package controllers;

import com.google.inject.Inject;
import dealer.Dealer;
import mongoModels.Hand;
import mongoModels.Player;
import mongoModels.SessionState;
import org.mongodb.morphia.query.Query;
import play.mvc.Controller;
import play.mvc.Result;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static com.mongodb.client.model.Filters.eq;

/**
 * Created by manish on 10/4/2017.
 */
public class MongoGameController extends Controller {

    private MongoHelper mongoHelper = null;
    private JacksonHelper jacksonHelper = null;

    /*All dealer functions*/
    //private Dealer dealer = new Dealer();
    @Inject
    public MongoGameController() {
        mongoHelper = new MongoHelper();
        jacksonHelper = new JacksonHelper();
    }

    public Result resetGame(){
        mongoHelper.resetDataBase();
        return ok();
    }


    public String joinGame(String playerName) {
        Query<SessionState> query = mongoHelper.datastore.createQuery(SessionState.class);
        List<SessionState> sessionStates = query.asList();

        SessionState sessionState = sessionStates.get(0);//get first
        if (!sessionState.isStarted()) {
            //game is already started
            //multiple games isnt supported yet
            if (sessionState.addNewHumanPlayer(playerName)) {
                mongoHelper.datastore.save(sessionState);
                return "JOINED";
            }
            return "ALREADYEXISTS";

        } //else
        return "ALREADYSTARTED";

    }

    //initiates a game session in the database
    public Result createSessionIfNotExists() {
        String name = "makeDynamic";

        Query<SessionState> query = mongoHelper.datastore.createQuery(SessionState.class);
        List<SessionState> sessionStates = query.asList();

        //checks if session already exist
        if (sessionStates.size() == 0) {
            //create new sessionState object
            SessionState sessionState = new SessionState();
            sessionState.initializeBots(2);//makeDynamic

            mongoHelper.datastore.save(sessionState);
            this.joinGame(name);
            return ok("JOINED");
        } else {
            return ok(joinGame(name));
        }
    }

    public Result getGameData() {
        SessionState sessionState = mongoHelper.getSessionStateByIndex(0);
        if(sessionState==null || sessionState.getState()==null) {
           return ok(jacksonHelper.pojoToJson(sessionState));
        }

        if(sessionState.getState().equals("ELIMINATION")){
            doRemainingMoves(sessionState);
            sessionState = mongoHelper.getSessionStateByIndex(0);
            sessionState = evaluateCards(sessionState); //evaluate cards
            //if everyone is done and there are winners
            if (sessionState.evaluateAndProcessWinners() ||
                    sessionState.allPlayersResultsEvaluated()) {
                //initiate new game
                sessionState.cleanPlayers();
                sessionState.setState("FIRSTDEAL");
            }else if(sessionState.isEveryoneReady()) {
                sessionState.everyOneCanHitAgain();
            }
            mongoHelper.datastore.save(sessionState);
        }

        return ok(jacksonHelper.pojoToJson(sessionState)); //converts object to json string
    }

    //starts game and returns new state
    public Result startGame() {
        SessionState sessionState = mongoHelper.getSessionStateByIndex(0);
        sessionState.startGame(true);//old way for states TODO:remove
        sessionState.setState("FIRSTDEAL"); //new way for states
        mongoHelper.datastore.save(sessionState);

        //initiate dealer
        Dealer dealer = new Dealer(sessionState.getPlayers().size());
        dealer.shuffleDecks();
        mongoHelper.datastore.save(dealer);

        return ok(jacksonHelper.pojoToJson(sessionState));
    }

    public Result dealCards(String playerId) {
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(this.getClass());
        logger.debug("dealcards ");

        //get sessionstate
        SessionState sessionState = mongoHelper.getSessionStateByIndex(0);
        sessionState.cleanWinner();

        //get dealer data
        //for each 3 players a new deck is needed
        Dealer dealer = mongoHelper.getDealerByIndex(0);
        int playerCountForDeck = 1;
        for (Player player : sessionState.getPlayers().values()) {
            logger.debug("handing over 1 card to 1 player count" + playerCountForDeck);
            //one card is dealt to everyone
            player.getHands().get(0).getCards().add(dealer.dealCard(playerCountForDeck));
            player.getHands().get(0).state="PLAYING";
            playerCountForDeck++;
        }

        //change sessionState
        sessionState.setState("ELIMINATION");

        sessionState = evaluateCards(sessionState);

        mongoHelper.datastore.save(sessionState);
        mongoHelper.datastore.save(dealer);
        return ok(jacksonHelper.pojoToJson(sessionState));
    }

    public Result hitCard(String playerId, int bet) {

        //get sessionState and dealer
        SessionState sessionState = this.hitCardAndUpdateBots(playerId, bet);
        //evaluate result of cards
        sessionState = evaluateCards(sessionState);
        mongoHelper.datastore.save(sessionState);

        //if everyone is done and there are winners
        if(sessionState.evaluateAndProcessWinners()){
            //initiate new game
            sessionState.setState("FIRSTDEAL");
        }
        //save data in database
        mongoHelper.datastore.save(sessionState);

        return ok(jacksonHelper.pojoToJson(sessionState));
    }

    public Result stand(String playerId, int bet) {

        //get sessionState and dealer
        SessionState sessionState = mongoHelper.getSessionStateByIndex(0);
        //evaluate result of cards
        sessionState = evaluateCards(sessionState);
        Player player = sessionState.getPlayers().get(playerId);
        player.isWaiting=true;

        //save data in database
        mongoHelper.datastore.save(sessionState);

        return ok(jacksonHelper.pojoToJson(sessionState));
    }

    private SessionState hitCardAndUpdateBots(String playerId, int bet){
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(this.getClass());
        logger.debug("hitCARD?");

        int handIndex=0;//makeDynamic

        //get session
        SessionState sessionState = mongoHelper.getSessionStateByIndex(0);
        Dealer dealer = mongoHelper.getDealerByIndex(0); //get dealer

        Player player = sessionState.getPlayers().get(playerId); //get player
        player.setBetAmount(bet, handIndex);        //set bet
        int playerIndex = sessionState.getPlayerIndex(playerId); //get index of player
        player.addCardToHand(dealer.dealCard(playerIndex), handIndex); //get card from dealer
        player.isWaiting=true;  //players turn is done

        logger.debug("new card should be added now" + playerId);
        mongoHelper.datastore.save(dealer);

        return sessionState;

    }

    private SessionState evaluateCards(SessionState sessionState) {
        for (Player player : sessionState.getPlayers().values()) {
            player.evaluateHands();
        }
        return sessionState;
    }

    // calls hitCard internally and saves data in database
    private SessionState doRemainingMoves(SessionState sessionState) {
        for (Player player : sessionState.getPlayers().values()) {
            if(!player.isWaiting  && (player.getType().equals("BOT") && !player.hasAllResults())){// first bot
                this.doBotMove(player);
                //break;
                return sessionState;
            }
            if(!player.isWaiting && player.getType().equals("HUMAN") && !player.hasAllResults()){
                //dont do anything because player needs to move first
                return sessionState;
            }
        }
        for (Player player : sessionState.getPlayers().values()) {
            if(!player.isWaiting  && (player.getType().equals("BANK"))){ // than bank
                this.doBotMove(player);
                //break;
            }
        }
        return sessionState;
    }

    //* moves always according to the same procedure. *//
    private void doBotMove(Player player){
        player.evaluateHands();
        for(Hand hand : player.getHands()){
            if(!hand.state.equals("DISABLED") && hand.winner==null){
                int betAmount = ThreadLocalRandom.current().nextInt(0, (int)player.getMoney() + 1);
                hand.setBetAmount(betAmount);
                if(hand.getHandSum()<=16){
                    //hit with random bet
                    hitCard(player.getId(), betAmount);//saves data in db
                }else if(hand.getHandSum()>16 && player.getType().equals("BANK")){
                    //stand
                    stand(player.getId(), betAmount);
                } else if(hand.getHandSum()<=21 && player.getType().equals("BOT")){
                    hitCard(player.getId(), betAmount);//saves data in db
                }
            }
        }
    }

}
package mongoModels;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.mongodb.morphia.annotations.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Created by manish on 9/30/2017.
 */
@Entity("SessionState")
public class SessionState {

    public SessionState(){
        this.turn=0;
        this.hand=0;
        initializeBank();
    }

    //TODO: make sessionId dynamic
    @Id
    public String _id = "firstSession";

    @Property
    @JsonProperty
    private String state; //FIRSTDEAL, ELIMINATION,

    @Property
    private int turn;

    @Property
    private int hand;

    @Embedded("players")
    private LinkedHashMap<String, Player> players = new LinkedHashMap<>();

    @JsonProperty("turn")
    public String getTurn() {
        //return id of the players turn
        String key = (String)players.keySet().toArray()[turn];
        return players.get(key).getId();
    }

    @JsonProperty("turn")
    public void setTurn(int turn) {
        this.turn = turn;
    }

    public int getHand() {
        return hand;
    }

    public void setHand(int hand) {
        this.hand = hand;
    }

    public LinkedHashMap<String, Player> getPlayers() {
        return players;
    }

    public void setPlayers(LinkedHashMap<String, Player> players) {
        this.players = players;
    }


    @JsonProperty
    public boolean isStarted() {
        return this.state!=null;
    }

    public void startGame(boolean started) {

        this.state = "FIRSTDEAL";
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getState() { return this.state;}

    //setter functions with behaviour
    public int getPlayerIndex(String playerId){
       return new ArrayList<String>(this.getPlayers().keySet()).indexOf(playerId);
    }

    //initializes the bots that play in the session
    public void initializeBots(int amountOfBots){
        for(int x=0; x<amountOfBots; x++){
            Player player = new Player();
            player.setId(Integer.toString(x));
            player.setType("BOT");
            addNewPlayer(player);
        }
    }

    //initializes the bots that play in the session
    public boolean initializeBank(){
        Player player = new Player();
        player.setId(Integer.toString(this.players.size()));
        player.setType("BANK");

        return addNewPlayer(player);
    }

    //initializes the human that play in the session
    //returns true if initialization succeeds
    //returns false if initialization fails
    public boolean addNewHumanPlayer(String name){
        Player player = new Player();
        player.setId(name);
        player.setType("HUMAN");

        return addNewPlayer(player);
    }

    private boolean addNewPlayer(Player player){
        //add player if key is not present
        if(!players.containsKey(player.getId())){

            player.getHands().add(0, new Hand(0,0,"PLAYING"));
            player.getHands().add(1, new Hand(1,0,"DISABLED"));
            players.put(player.getId(), player);
            return true;
        }
        return false;
    }

    /* Returns true if all players are winners or losers.
    *  Also changes the variables for the next state to be able to initiated.
    *  returns false if there are still people playing.
    * */
    public boolean evaluateAndProcessWinners(){
        boolean gameEnd=false;

        //if bank has one because of bust or blackjack
        //game ends directly
        for(Player player : this.players.values()){
            if(player.getType().equals("BANK")){ //did bank win?
                //if bank is bust
                if(player.getHands().get(0).state.equals("BUST")){
                    //every hand wins except for the bust hands
                    everyHandWinsExceptBusts();
                    return true;
                } else if(player.getHands().get(0).state.equals("BLACKJACK")){
                    //every hand loses
                    bankWinsEverything();
                    return true;
                }
            }
        }
        for(Player player : this.players.values()){
            if(!player.getType().equals("BANK")) {
                for (Hand hand : player.getHands()) {
                    if (!hand.state.equals("DISABLED") && hand.winner!=null) {
                        //there are still people playing
                        //cannot end game
                        //return false;
                        gameEnd = false;
                    } else if (handVsBank(hand)) { //player won
                        hand.setWinner("You", "because you had higher cards");
                        player.addMoney(hand.getBetAmount() * 2);
                    } else if(hand.getHandSum()>21) { //bank won
                        hand.setWinner("Bank", "because you got bust");
                    } else if(hand.getHandSum()==21) { //you got blackjack
                        hand.setWinner("You", "because you got blackjack");
                    }
                }
            }
        }

        //No one is playiing
        return gameEnd;
    }

    public void cleanPlayers() {
        for (Player player : this.players.values()) {
            for (Hand hand : player.getHands()) {
                hand.reset();//resets hand for a new game
                player.isWaiting = true;
            }
        }
    }

    public boolean isEveryoneReady(){
        for(Player player : this.players.values()) {
            if(!player.isWaiting){
                return false;
            }
        }
        return true;
    }

    public boolean allPlayersResultsEvaluated(){
        for(Player player : this.players.values()) {
            //if player is not a bank and dousnt have all results
            if(!player.getType().equals("BANK") && !player.hasAllResults()){
                return false;
            }
        }
        return true;
    }


    public void everyOneCanHitAgain(){
        for(Player player : this.players.values()) {
            player.isWaiting=false;
        }
    }

    //every hand wins except for the bust hands
    //because bank got busted
    private void everyHandWinsExceptBusts() {
        for (Player player : this.players.values()) {
            for(Hand hand : player.getHands()){
                if(!hand.state.equals("BUST") && !hand.state.equals("DISABLED")){
                    //wins are 1 by 1
                    hand.setWinner("You", "because the bank got busted.");
                    player.addMoney(hand.getBetAmount()*2);
                }
            }
        }
    }

    //bank got blackjack
    private void bankWinsEverything() {
        for (Player player : this.players.values()) {
            for(Hand hand : player.getHands()){
                if(!hand.state.equals("DISABLED")){
                    hand.setWinner("Bank", "because the bank got a blackjack.");
                }
            }
        }
    }

    /* Returns true if player hand wins
    * returns false if player loses
     */
    private boolean handVsBank(Hand hand) {
        //get total value of banks cards
        int bankHandTotal = getBankHandTotal();
        if((bankHandTotal>16 && hand.getHandSum()>bankHandTotal && hand.getHandSum()<=21)){ // player wins
            return true;
        }
        return false;

    }

    private int getBankHandTotal(){
        Player bank = null;// get bank
        for (Player bankSearch : this.players.values()) {
            if (bankSearch.getType().equals("BANK")) {
                bank = bankSearch;
            }
        }
        return bank.getHands().get(0).getHandSum();
    }


    //
    public void cleanWinner() {
        for (Player player : this.players.values()) {
            for(Hand hand : player.getHands()){
               hand.winner=null;
            }
        }
    }


}


package dealer;

import mongoModels.SessionState;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Property;
import play.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by manish on 10/3/2017.
 */
@Entity("DealerState")
public class Dealer {

    //TODO: make dealerId dynamic
    @Id
    public String _id = "firstDealer";

    @Embedded("decks")
    private List<Deck> decks= new ArrayList<Deck>();

    private int amountOfPlayers=0;

    public Dealer(int amountOfPlayers){
        //json datastream.
        //get amount of players
        this.amountOfPlayers=amountOfPlayers;
        initiateDecks(amountOfPlayers);
    }

    public Dealer(){
    }

    public int addNewPlayer(){
        Logger.debug("added new player new amount: " + amountOfPlayers);

        this.amountOfPlayers++;
        return amountOfPlayers;
    }

    public int getAmountOfPlayers(){
       return this.amountOfPlayers;
    }

    private void initiateDecks(int amountOfPlayers){
        amountOfPlayers++;//dealer
        int amountOfDecks = (int) Math.ceil(amountOfPlayers/3D);
        amountOfDecks++;//can't hurt
        for(int x=0; x<amountOfDecks; x++){
            Deck deck = new Deck(x);
            decks.add(deck);
        }
    }

    //input: playerId so the dealer knows which deck to use
    //the dealer can only deal 3 people each deck
    public int dealCard(int playerId){
        int deckIndex = (int) Math.ceil(playerId/3D);
        //deckIndex--;//because deck one is stored in index 0
        return decks.get(deckIndex).dealCard().getValue();
    }

    public void shuffleDecks(){
        for(Deck deck : decks){
            deck.shuffle();
        }
    }



//    private Deck[] getDecksFromDataBase(){
//        //get decks from mongodb
//
//    }

//    public Card dealNewCardByPlayerId(){
//
//    }



}

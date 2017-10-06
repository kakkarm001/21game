package mongoModels;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Property;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by manish on 10/5/2017.
 */
@Entity
public class Hand {

    public Hand(int handIndex, double betAmount, String state) {
        this.handIndex = handIndex;
        this.betAmount = betAmount;
        this.state = state;
        this.id= UUID.randomUUID().toString();
    }

    public Hand() {
    }

    @Id
    private String id;

    @Property
    private int handIndex; //0 or 1

    @Property
    private ArrayList<Integer> cards = new ArrayList<>();

    @Property
    private double betAmount;

    @Property
    public String state;//BUST, BLACKJACK

    @Property
    public String winner;//BUST, BLACKJACK

    public String getWinner() {
        return winner;
    }

    public void setWinner(String winner, String comment) {
        this.winner = winner + " won the hand " + comment;
    }

    public void evaluateHand(){
        if(winner==null) {
            this.state = evaluateHand(getHandSum());
        }
    }

    public int getHandSum(){
        return this.cards.stream().mapToInt(Integer::intValue).sum();
    }

    private String evaluateHand(int sum){
        if(sum>21){
            return "BUST";
        } else if(sum==21){
            return "BLACKJACK";
        } else if(sum==0 && handIndex==1){
            return "DISABLED";
        } else {
            return "PLAYING";
        }
    }

    public boolean isWinner(){
        return (winner!=null && !state.equals("DISABLED"));
    }

    //new round is initiated
    public void reset(){
        this.setBetAmount(0);//reset
        this.cards=new ArrayList<>();
        //first hand is default enabled

        if(this.handIndex==0){
            this.state="PLAYING";
        }
        else if(this.handIndex==1){ //bugfix
            this.state="DISABLED"; //new round is initiated
        }

    }

    //getters and setters

    @JsonProperty
    public double getBetAmount() {
        return betAmount;
    }

    @JsonProperty
    public void setBetAmount(double betAmount) {
        this.betAmount = betAmount;
    }

    @JsonProperty
    public int getHandIndex() {
        return handIndex;
    }

    @JsonProperty
    public void setHandIndex(int handIndex) {
        this.handIndex = handIndex;
    }

    @JsonProperty
    public ArrayList<Integer> getCards() {
        return cards;
    }

    @JsonProperty
    public void setHand(ArrayList<Integer> cards) {
        this.cards = cards;
    }

}

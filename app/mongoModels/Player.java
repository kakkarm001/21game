package mongoModels;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Property;

import java.util.ArrayList;

/**
 * Created by manish on 10/2/2017.
 */

@Entity
public class Player {

    public Player(){
        money=100D;//initial amount
    }

    @Id()
    private String id;

    @Embedded("hands")
    private ArrayList<Hand> hands = new ArrayList<>();

    @Property
    private String type; //human, bank or bot

    @Property
    private double money;

    // is used in many areas
    // but recomended use is hand.state
    @Property
    public boolean isWaiting;

    @JsonProperty("_id")
    public String getId() {
        return id;
    }

    @JsonProperty("_id")
    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @JsonProperty
    public double getMoney() {
        return money;
    }

    @JsonProperty
    public void setMoney(double money) {
        this.money = money;
    }

    public void addMoney(double money) {
        this.money += money;
    }

    @JsonProperty
    public ArrayList<Hand> getHands() {
        return hands;
    }

    @JsonProperty
    public void setHands(ArrayList<Hand> hands) {
        this.hands = hands;
    }

    public double getBetAmountByHand(int hand) {
        return this.hands.get(hand).getBetAmount();
    }

    /*  hand 0 is first hand
    *   hand 1 is second hand
     */
    public void addCardToHand(int card, int hand){
        this.hands.get(hand).getCards().add(card);
    }

    public void setBetAmount(double betAmount, int handIndex) {
        //banks dont bet money
        if(this.getType().equals("BANK")){
            this.hands.get(handIndex).setBetAmount(0);
            return;
        }
        //checks is bet amount is not higher than money available
        if(this.getMoney()<betAmount){
            this.hands.get(handIndex).setBetAmount((int)this.getMoney());
        }else{
            this.hands.get(handIndex).setBetAmount(betAmount);
        }
        this.setMoney(this.getMoney()-this.hands.get(handIndex).getBetAmount());
    }

    public void evaluateHands(){
        for(Hand hand : hands){
            hand.evaluateHand();
        }
    }

    //checks if the player got all his results
    //sa result is when a hand won or lost
    // returns true if all results are evaluated
    public boolean hasAllResults(){
        for(Hand hand : hands){
            //if the hand has no winner
            if(!hand.isWinner()){
                return false;
            }
        }
        return true;
    }




}

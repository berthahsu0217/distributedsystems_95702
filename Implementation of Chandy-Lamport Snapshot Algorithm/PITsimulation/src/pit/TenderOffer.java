package pit;

import java.io.Serializable;

/* 
 * A TenderOffer message carries an offer of a commodity card from one Player 
 * to another.
 */
public class TenderOffer implements Serializable {
    private static final long serialVersionUID = 1L;
    // The Player originating the offer
    public int sourcePlayer;
    
    // The commodity being offerred
    public String tradeCard;    
}

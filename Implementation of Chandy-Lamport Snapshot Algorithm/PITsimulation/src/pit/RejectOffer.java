package pit;

import java.io.Serializable;

/* 
 * A RejectOffer message indicates that an offer was rejected, and the
 * commodity card is being returned.
 */

public class RejectOffer implements Serializable {
    private static final long serialVersionUID = 1L;
    // The Player rejecting the offer
    public int sourcePlayer;
    
    // The commodity being returned
    public String tradeCard;    
}

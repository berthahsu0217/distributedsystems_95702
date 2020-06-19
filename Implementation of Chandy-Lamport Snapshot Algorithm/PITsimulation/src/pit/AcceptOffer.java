package pit;

import java.io.Serializable;

/* 
 * An AcceptOffer message carries a commodity card from one Player to another
 * in payment for an accepted offer.
 */
public class AcceptOffer implements Serializable {
    private static final long serialVersionUID = 1L;
    // The Player who accepted the offer is is returning payment.
    public int sourcePlayer;
    
    // The commodity being returned as payment.
    public String tradeCard;    
}

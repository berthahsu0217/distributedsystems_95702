package pit;

import java.io.Serializable;
import java.util.ArrayList;

/* 
 * A representation of a new hand of commodities to be sent from
 * PITsnapshot to each Player
 */
public class NewHand implements Serializable{
    private static final long serialVersionUID = 1L;
    // A list of the new commodity cards
    public ArrayList newHand = new ArrayList();
    
    // The total number of players who will be trading
    public int numPlayers;
}

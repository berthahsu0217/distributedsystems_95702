package pit;

import java.io.Serializable;

/* 
 * A Reset object is passed from PITsnapshot to each Player to signify
 * resetting the Player's state.  This is done in two stages, HALT, then
 * CLEAR.  The Player replies acknowledging each.
 */
public class Reset implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final int HALT = 1;
    public static final int CLEAR = 2;
    public int action;
    public Reset (int setAction) {
        action = setAction;
    }
}

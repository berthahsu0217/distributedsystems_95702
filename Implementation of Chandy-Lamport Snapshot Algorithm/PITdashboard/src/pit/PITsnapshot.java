package pit;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

@WebServlet(name = "PITsnapshot", urlPatterns = {"/PITsnapshot"})
public class PITsnapshot extends HttpServlet {

    // Number of players in the simulation (a PITplayer MDB must be created for each)
    final int numPlayers = 6;
    // Number of copies of each commodity for each player
    final int commodityCopiesPerPlayer = 2;
    // The list of commodities used in the simulation.
    // Should be the same number as numPlayers.  Actual commodities added in init()
    LinkedList<String> commodities = new LinkedList<String>();

    @Override
    public void init() {
        // Add the commodities.  
        // Each commodity should be unique and the number should equal numPlayers
        commodities.add("Wheat");
        commodities.add("Corn");
        commodities.add("Coffee");
        commodities.add("Soybeans");
        commodities.add("Oats");
        commodities.add("Barley");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        try {
            // Gather necessary JMS resources
            Context ctx = new InitialContext();
            ConnectionFactory cf = (ConnectionFactory) ctx.lookup("openejb:Resource/myConnectionFactory");
            Connection con = cf.createConnection();
            con.start(); // don't forget to start the connection
            QueueSession session = (QueueSession) con.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // The PITsnapshot Queue is used for responses from the Players to this serverlet
            Queue q = (Queue) ctx.lookup("openejb:Resource/PITsnapshot");
            MessageConsumer reader = session.createConsumer(q);

            /*
             * Throw out old PITsnapshot messages that may have been left from past
             * snapshots that did not complete (because of some error).
             */
            ObjectMessage m = null;
            while ((m = (ObjectMessage) reader.receiveNoWait()) != null) {
                System.out.println("Servlet found an orphaned PITsnapshot message");
            }

            // Initialize the snapshot by sending a marker to a Player
            sendInitSnapshot();

            /*
             * Receive the snapshot messages from all Players.
             * Each snapshot is a HashMap.  Put them into an array of HashMaps
             */
            LinkedList<HashMap> state = new LinkedList<HashMap>();
            int stateResponses = 0;
            int failures = 0;
            while (stateResponses < numPlayers) {
                if ((m = (ObjectMessage) reader.receive(2000)) == null) {
                    if (++failures > 5) {
                        System.out.println("Servlet: Not all players reported, giving up after " + stateResponses);
                        out.print("Snapshot Failed");
                        con.close();
                        return;
                    }
                    System.out.println("Servlet: Timeout number "+ failures + " without a player reporting.");
                    continue;
                }
                stateResponses++;
                state.add((HashMap) m.getObject());
            }
            request.setAttribute("commodity", commodities);
            request.setAttribute("state", state);

            // Close the connection
            con.close();

            request.getRequestDispatcher("snapshotResult.jsp").forward(request, response);

        } catch (Exception e) {
            System.out.println("Servlet threw exception " + e);
            e.printStackTrace();
        } finally {
            out.close();
        }
    }

    /*
     * Initiate the snapshot by sending a Marker message to one of the Players (Player0)
     * Any Player could have been used to initiate the snapshot.
     */
    private void sendInitSnapshot() {
        try {
            // Gather necessary JMS resources
            Context ctx = new InitialContext();
            ConnectionFactory cf = (ConnectionFactory) ctx.lookup("openejb:Resource/myConnectionFactory");
            // Which PITplayer should be sent the snapshot marker
            int snapshotStarter = Math.round((float) Math.random() * (numPlayers - 1));
            Queue q = (Queue) ctx.lookup("openejb:Resource/PITplayer"+snapshotStarter);
            Connection con = cf.createConnection();
            Session session = con.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer writer = session.createProducer(q);

            /*
             * As part of the snapshot algorithm, players need to record 
             * what other Players they receive markers from.
             * "-1" indicates to the PITplayer0 that this marker is coming from
             * the monitor, not another Player.
             */
            Marker m = new Marker(-1);
            ObjectMessage msg = session.createObjectMessage(m);
            System.out.println("Servlet Initiating Snapshot via PITplayer"+snapshotStarter);
            writer.send(msg);
            con.close();
        } catch (JMSException e) {
            System.out.println("Servlet JMS Exception thrown" + e);
        } catch (Throwable e) {
            System.out.println("Servlet Throwable thrown" + e);
            e.printStackTrace();
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");

        resetAllPlayers(numPlayers, Reset.HALT);
        resetAllPlayers(numPlayers, Reset.CLEAR);
        for (int player = 0; player < numPlayers ; player++) {
            sendInit(player);
        }

        PrintWriter out = response.getWriter();
        try {
            out.print("{\"message\": \"PIT has been initiated\",");
            String commoditiesString = "";
            String arraySeparator = "";
            for (String commodity: commodities) {
                commoditiesString += arraySeparator + "\"" + commodity + "\"";
                arraySeparator = ",";
            }
            out.println("\"commodities\": [" + commoditiesString + "]}");
        } finally {
            out.close();
        }
    }
    @Override
    public void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");

        resetAllPlayers(numPlayers, Reset.HALT);


        PrintWriter out = response.getWriter();
        try {
            out.print("{\"message\": \"PIT has been halted\"}");
        } finally {
            out.close();
        }
    }

    private void sendInit(int playerNumber) {

        try {
            // Gather necessary JMS resources
            Context ctx = new InitialContext();
            ConnectionFactory cf = (ConnectionFactory) ctx.lookup("openejb:Resource/myConnectionFactory");
            Queue q = (Queue) ctx.lookup("openejb:Resource/PITplayer" + playerNumber);
            Connection con = cf.createConnection();
            Session session = con.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer writer = session.createProducer(q);

            // Create a new hand to send to the Player
            NewHand hand = new NewHand();
            hand.numPlayers = numPlayers;
            // Give each player copies of each commodity
            for (int i = 0; i < commodityCopiesPerPlayer; i++) {
                for (String commodity: commodities) { 
                    hand.newHand.add(commodity);
                }
            }

            // Send the hand to the Player
            ObjectMessage msg = session.createObjectMessage(hand);
            System.out.println("Servlet sending newhand to " + playerNumber);
            writer.send(msg);
            con.close();
        } catch (JMSException e) {
            System.out.println("Servlet JMS Exception thrown" + e);
        } catch (Throwable e) {
            System.out.println("Servlet Throwable thrown" + e);
        }
    }

    private void resetAllPlayers(int numPlayers, int action) {
        String actionString = ((action == Reset.HALT) ? "HALT" : "CLEAR");
        try {
            // Gather necessary JMS resources
            Context ctx = new InitialContext();
            ConnectionFactory cf = (ConnectionFactory) ctx.lookup("openejb:Resource/myConnectionFactory");
            Connection con = cf.createConnection();
            Session session = con.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // Set up to read the PITmonitor Queue for the Reset acknowledgements
            Queue rq = (Queue) ctx.lookup("openejb:Resource/PITmonitor");
            MessageConsumer reader = session.createConsumer(rq);

            // Always remember to start a connection when receiving from it!
            con.start();

            /*
             * For each player, send a Reset message, and wait if its reply.
             * We need to wait for a reply, for the NewHands cannot be distributed
             * until every Player is in a reset state.
             */
            for (int player = 0; player < numPlayers; player++) {
                System.out.println("Servlet sending Reset "+actionString+" to PITplayer" + player);
                Queue q = (Queue) ctx.lookup("openejb:Resource/PITplayer" + player);
                MessageProducer writer = session.createProducer(q);

                /*
                 * A Reset is an object passed back and forth to initiate and 
                 * acknowledge an reset operation
                 */
                Reset reset = new Reset(action);
                ObjectMessage resetMessage = session.createObjectMessage(reset);

                writer.send(resetMessage);

                // Give a very long wait.  It should not take that long, but fail if it does not come back by then
                ObjectMessage m = (ObjectMessage) reader.receive(10000);

                if (m == null) {
                    System.out.println("Servlet ERROR:  Receive of reset acknowledgement time out from PITplayer" + player);
                    throw new Throwable("ERROR:  Receive of reset acknowledgement time out from PITplayer" + player);
                }
                if (!(((Reset) m.getObject()) instanceof Reset)) {
                    System.out.println("Servlet ERROR:  Bad reset acknowledgement back from PITplayer" + player);
                    throw new Throwable("ERROR:  Bad reset acknowledgement back from PITplayer" + player);
                }
                System.out.println("Servlet Reset "+actionString+" from PITplayer" + player + " ACKNOWLEDGED");
            }
            session.close();
            con.close();
        } catch (JMSException e) {
            System.out.println("Servlet JMS Exception thrown" + e);
        } catch (Throwable e) {
            System.out.println("Servlet Throwable thrown" + e);
        }
    }
}

/*
 * @author Bertha Hsu
 * This script is used to launch a TCP server.
 * It contains code to create a server socket for listening and sending messages.
 * When the server receives a request from the client, it performs the operation
 * specified by the client (using the value it sent).
 */

import java.math.BigInteger;
import java.net.*;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Server {

    /**
     * Method connect contains the socket communication code.
     * It also performs whichever operation requested by the client.
     */
    public static void connect(){

        //initialize a map to store values
        Map<String, Integer> data = new HashMap<>();
        Socket clientSocket = null;
        try {
            //allocate a port to the server
            int serverPort = 7777;
            //create a new server socket
            ServerSocket listenSocket = new ServerSocket(serverPort);

            /*
             * Block waiting for a new connection request from a client.
             * When the request is received, "accept" it, and the rest
             * the tcp protocol handshake will then take place, making
             * the socket ready for reading and writing.
             */
            while(true) {

                //accept a connection on the server socket
                clientSocket = listenSocket.accept();

                // Set up "in" to read from the client socket
                Scanner in;
                in = new Scanner(clientSocket.getInputStream());

                // Set up "out" to write to the client socket
                PrintWriter out;
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream())));

                //read request data from the client
                String ans = in.nextLine();
                String[] strings = ans.split(" ");
                String sol;

                //check user id and signature
                boolean verify = check(strings);

                //if valid, perform operation sepcified
                if (verify){
                    String id = strings[0];
                    String operation = strings[3];

                    //if the operation is ADD or SUBTRACT, performs related operation
                    if(operation.equals("add") || operation.equals("subtract")){
                        int value = Integer.parseInt(strings[4]);
                        if(data.containsKey(id)){
                            int sum = data.get(id);
                            if (operation.equals("add")) data.put(id, sum+value);
                            else data.put(id, sum-value);
                        }else{
                            if (operation.equals("add")) data.put(id, value);
                            else data.put(id, (-1)*value);

                        }
                        sol = "OK";
                        System.out.println("sum:"+data.get(id));
                     //if the operation is VIEW, return the stored value
                    }else{
                        if(data.containsKey(id)){
                            int sum = data.get(id);
                            sol = String.valueOf(sum);
                            System.out.println("sum:"+data.get(id));
                        }else{
                            sol = String.valueOf(0);
                            System.out.println("sum:"+0);
                        }

                    }
                }else{
                    sol = "Error in Request";
                }
                //write data to the socket
                out.println(sol);
                out.flush();
            }

        // Handle exceptions
        } catch (IOException error) {
            System.out.println("IO Exception:" + error.getMessage());
        // If quitting (typically by you sending quit signal) clean up sockets
        } finally {
            try {
                if (clientSocket != null) {
                    clientSocket.close();
                }
            } catch (IOException error) {
                // ignore exception on close
            }
        }


    }

    /**
     * Method SHA256Hash hashes a given string using "SHA-256"
     * @param text a String
     * @return byte array of the hash
     */
    public static byte[] SHA256Hash(String text) {

        try {
            // Create a SHA256 digest
            MessageDigest digest;
            digest = MessageDigest.getInstance("SHA-256");
            // allocate room for the result of the hash
            byte[] hashBytes;
            // perform the hash
            digest.update(text.getBytes("UTF-8"), 0, text.length());
            // collect result
            hashBytes = digest.digest();
            return hashBytes;
        }
        catch (NoSuchAlgorithmException nsa) {
            System.out.println("No such algorithm exception thrown " + nsa);
        }
        catch (UnsupportedEncodingException uee ) {
            System.out.println("Unsupported encoding exception thrown " + uee);
        }
        return null;
    }

    /**
     * Method byteArrayToString converts a byte array to a hex string
     * @param data byte array
     * @return a hex String
     */
    public static String byteArrayToString(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9))
                    buf.append((char) ('0' + halfbyte));
                else
                    buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[i] & 0x0F;
            } while(two_halfs++ < 1);
        }
        return buf.toString();
    }

    /**
     * Method generateID uses public key to generate an user ID
     * @param e public key
     * @param n public key
     * @return user ID
     */
    public static String generateID(BigInteger e, BigInteger n){

        String str = e.toString() + n.toString();
        byte[] hashedStr = SHA256Hash(str);
        String id = byteArrayToString(Arrays.copyOfRange(hashedStr,hashedStr.length-20,hashedStr.length));
        return id;
    }

    /**
     * Method check if the user ID is hashed properly, and if the request is properly signed
     * @param strings tokens of message recieved by the server
     * @return whether operation is valid to resume
     */
    public static boolean check(String[] strings){

        //retrieve id, public keys
        String id = strings[0];
        BigInteger e = new BigInteger(strings[1]);
        BigInteger n = new BigInteger(strings[2]);
        String operation = strings[3];

        //recreate the user id with public keys
        String IDtoCheck = generateID(e,n);
        //System.out.println(IDtoCheck);
        //System.out.println(id);
        //System.out.println(id.equals(IDtoCheck));

        //take the encrypted string and make it a big integer
        BigInteger encryptedHash = new BigInteger(strings[strings.length-1]);
        //decrypt it
        BigInteger decryptedHash = encryptedHash.modPow(e, n);

        //retrieve the origin hashed message
        String messageToCheck = strings[0];
        for(int i = 1; i < strings.length-1; i++){
            messageToCheck = messageToCheck+ " "+strings[i];
        }
        byte[] bytes = SHA256Hash(messageToCheck);
        //retrieve the BigInteger created by the hash
        BigInteger m = new BigInteger(bytes);
        //System.out.println(m);
        BigInteger bigIntegerToCheck = new BigInteger(1,bytes);

        //checks if the id and signature are valid, return the result
        if((bigIntegerToCheck.compareTo(decryptedHash) == 0) && (IDtoCheck.equals(id))){
            System.out.println("Valid id and signature.");
            return true;
        }
        else {
            System.out.println("Invalid id and signature.");
            return false;
        }
    }

    public static void main(String args[]) {

        System.out.println("Server running.");
        //launch a server socket
        connect();

    }
}

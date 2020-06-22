/*
 * @author Bertha Hsu
 * This script is used to launch a TCP client.
 * It contains code to create a client socket for sending and receiving messages from the server.
 * It lets users choose an operation and value to be performed in the server.
 */

import java.math.BigInteger;
import java.net.*;
import java.io.*;
import java.util.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Client {

    //generate private and public keys

    /**
     * Method RSA generate a pair of private and public keys for encryption and decryption
     * @return a map containing keys
     */
    public static Map RSA(){

        //initalize a map to store the keys
        Map<String, BigInteger> key = new HashMap<>();
        // Each public and private key consists of an exponent and a modulus
        BigInteger n; // n is the modulus for both the private and public keys
        BigInteger e; // e is the exponent of the public key
        BigInteger d; // d is the exponent of the private key
        Random rnd = new Random();
        // Step 1: Generate two large random primes.
        // We use 400 bits here, but best practice for security is 2048 bits.
        // Change 400 to 2048, recompile, and run the program again and you will
        // notice it takes much longer to do the math with that many bits.
        BigInteger p = new BigInteger(400,100,rnd);
        BigInteger q = new BigInteger(400,100,rnd);
        // Step 2: Compute n by the equation n = p * q.
        n = p.multiply(q);
        // Step 3: Compute phi(n) = (p-1) * (q-1)
        BigInteger phi = (p.subtract(BigInteger.ONE)).multiply(q.subtract(BigInteger.ONE));
        // Step 4: Select a small odd integer e that is relatively prime to phi(n).
        // By convention the prime 65537 is used as the public exponent.
        e = new BigInteger ("65537");
        // Step 5: Compute d as the multiplicative inverse of e modulo phi(n).
        d = e.modInverse(phi);

        //System.out.println(" e = " + e);  // Step 6: (e,n) is the RSA public key
        //System.out.println(" d = " + d);  // Step 7: (d,n) is the RSA private key
        //System.out.println(" n = " + n);  // Modulus for both keys

        //store keys in the map
        key.put("e",e);
        key.put("d",d);
        key.put("n",n);
        return key;
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
     * Method sign uses privates keys to encrypt a message
     * @param bytes byte array of the hashed message
     * @param d private key
     * @param n private key
     * @return a encrypted String
     */
    public static String sign(byte[] bytes, BigInteger d, BigInteger n){
        //make sure the hash generates a positive BigInteger
        BigInteger m = new BigInteger(1,bytes);
        //System.out.println("bigint:"+m);
        BigInteger c = m.modPow(d, n);
        return c.toString();
    }

    /**
     * Method sendInput contains socket communication code.
     * It sends a informational message to server.
     * @param message String containing useful information
     */
    public static void sendInput(String message) {

        Socket clientSocket = null;
        try {
            //allocate a server port to the client
            int serverPort = 7777;
            //create a TCP client Socket
            clientSocket = new Socket("localhost", serverPort);

            //set up "in" to read from the client socket
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            //set up "out" to write to the client socket
            PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream())));

            //write data to the socket
            out.println(message);
            out.flush();
            //read data from the socket
            String data = in.readLine();
            System.out.println("Received: " + data);

            // Handle exceptions
        } catch (IOException e) {
            System.out.println("IO Exception:" + e.getMessage());
            // If quitting (typically by you sending quit signal) clean up sockets
        } finally {
            try {
                if (clientSocket != null) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                // ignore exception on close
            }
        }
    }

    public static void main(String args[]) {

        System.out.println("Welcome to Project2 Task 5. Type EXIT if you want to exit the client.");
        System.out.println();

        String message;

        //create private and public keys
        Map key = RSA();

        String id = generateID((BigInteger)key.get("e"), (BigInteger)key.get("n"));
        //System.out.println("id: "+id);
        String e = key.get("e").toString();
        //System.out.println("e: "+e);
        String n = key.get("n").toString();
        //System.out.println("n: "+n);

        Scanner input = new Scanner(System.in);

        //create a client side menu
        while (true) {
            message = id + " " + e + " " + n;

            String operation;
            int value = 0;

            //entering operation
            while (true) {
                System.out.println("Please select an operation: add, subtract, or view:");
                operation = input.nextLine();
                //if enter "EXIT" then exits
                if (operation.equals("EXIT")) {
                    System.out.println("Exits client.");
                    System.exit(1);
                }
                //checking if operation is either ADD, SUBTRACT or VIEW
                if ((operation.equals("add")) || (operation.equals("subtract")) || (operation.equals("view"))) {
                    message += " "+operation;
                    break;
                } else {
                    System.out.println("You have not typed a valid operation. Try again.");
                }
            }
            //entering value
            if (operation.equals("add") || operation.equals("subtract")) {
                while (true) {
                    System.out.println("Please enter an value");
                    String s_value = input.nextLine();
                    //if enter "EXIT" then exits
                    if (s_value.equals("EXIT")) {
                        System.out.println("Exits client.");
                        System.exit(1);
                    }
                    //checking if the value is an integer
                    try {
                        value = Integer.parseInt(s_value);
                        message += " " + s_value;
                        break;
                    } catch (NumberFormatException error) {
                        System.out.println("You entered a value that is not an integer. Try again.");
                    }
                }
            }

            //hash message
            byte[] bytes = SHA256Hash(message);
            //encrytpe hashed message
            String signature = sign(bytes, (BigInteger)key.get("d"), (BigInteger)key.get("n"));
            message += " "+signature;

            //sends final message to socket
            sendInput(message);
            System.out.println();
        }
    }
}
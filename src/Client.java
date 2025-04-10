import utils.CryptoUtils;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.Socket;
import java.util.Base64;
import java.util.Scanner;

import org.json.*;

public class Client {

    private static final String DB_FILE = "data/userdb.json";

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter your username: ");
        String username = scanner.nextLine();
        JSONObject db = new JSONObject(new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of(DB_FILE))));
        if (!db.has(username)) {
            System.out.println("User not found.");
            return;
        }

        SecretKey userKey = new SecretKeySpec(Base64.getDecoder().decode(db.getString(username)), "AES");

        // Step 1: Talk to AS
        JSONObject asRequest = new JSONObject().put("username", username);
        JSONObject asResponse = talkToServer(5000, asRequest);

        if (asResponse.has("error")) {
            System.out.println("AS Error: " + asResponse.getString("error"));
            return;
        }

        String decryptedSessionKeyB64 = CryptoUtils.decrypt(asResponse.getString("session_key"), userKey);
        SecretKey sessionKey = new SecretKeySpec(Base64.getDecoder().decode(decryptedSessionKeyB64), "AES");
        String tgt = asResponse.getString("tgt");

        // Step 2: Talk to TGS
        JSONObject tgsRequest = new JSONObject().put("tgt", tgt).put("service", "app");
        JSONObject tgsResponse = talkToServer(5001, tgsRequest);

        String decryptedServiceSessionKeyB64 = CryptoUtils.decrypt(tgsResponse.getString("service_session_key"), sessionKey);
        String serviceTicket = tgsResponse.getString("service_ticket");

        // Step 3: Talk to Server
        JSONObject serverRequest = new JSONObject().put("service_ticket", serviceTicket);
        JSONObject serverResponse = talkToServer(5002, serverRequest);

        System.out.println("Server Response: " + serverResponse.getString("status"));
    }

    private static JSONObject talkToServer(int port, JSONObject payload) throws Exception {
        try (Socket socket = new Socket("localhost", port);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {

            out.writeUTF(payload.toString());
            return new JSONObject(in.readUTF());
        }
    }
}

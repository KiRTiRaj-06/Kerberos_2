package server;

import utils.CryptoUtils;

import javax.crypto.SecretKey;
import java.io.*;
import java.net.*;
import java.util.Scanner;

import org.json.*;

public class ApplicationServer {

    private static final int PORT = 5002;
    private static final String SERVER_KEY_FILE = "data/server.key";

    public static void main(String[] args) throws Exception {
        SecretKey serverKey = CryptoUtils.loadOrCreateKey(SERVER_KEY_FILE);
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server Listening on port " + PORT);

        while (true) {
            Socket socket = serverSocket.accept();
            new Thread(() -> {
                try (socket; DataInputStream in = new DataInputStream(socket.getInputStream());
                     DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

                    JSONObject input = new JSONObject(in.readUTF());
                    String encryptedTicket = input.getString("service_ticket");

                    JSONObject ticket = new JSONObject(CryptoUtils.decrypt(encryptedTicket, serverKey));
                    String username = ticket.getString("username");

                    System.out.print("Grant access to '" + username + "'? (yes/no): ");
                    Scanner scanner = new Scanner(System.in);
                    String decision = scanner.nextLine();

                    if (decision.equalsIgnoreCase("yes")) {
                        out.writeUTF(new JSONObject().put("status", "Access Granted").toString());
                    } else {
                        out.writeUTF(new JSONObject().put("status", "Access Denied").toString());
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}

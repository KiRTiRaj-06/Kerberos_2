package server;

import utils.CryptoUtils;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.*;
import java.util.Base64;

import org.json.*;

public class TGS {
    private static final int PORT = 5001;
    private static final String TGS_KEY_FILE = "data/tgs.key";
    private static final String SERVER_KEY_FILE = "data/server.key";

    public static void main(String[] args) throws Exception {
        SecretKey tgsKey = CryptoUtils.loadOrCreateKey(TGS_KEY_FILE);
        SecretKey serverKey = CryptoUtils.loadOrCreateKey(SERVER_KEY_FILE);

        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("TGS Listening on port " + PORT);

        while (true) {
            Socket socket = serverSocket.accept();
            new Thread(() -> {
                try (socket; DataInputStream in = new DataInputStream(socket.getInputStream());
                     DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

                    JSONObject input = new JSONObject(in.readUTF());
                    String encryptedTGT = input.getString("tgt");

                    JSONObject tgt = new JSONObject(CryptoUtils.decrypt(encryptedTGT, tgsKey));
                    String sessionKeyB64 = tgt.getString("session_key");
                    String username = tgt.getString("username");

                    SecretKey sessionKey = new SecretKeySpec(Base64.getDecoder().decode(sessionKeyB64), "AES");
                    SecretKey serviceSessionKey = CryptoUtils.generateKey();

                    JSONObject serviceTicket = new JSONObject()
                            .put("username", username)
                            .put("service_session_key", Base64.getEncoder().encodeToString(serviceSessionKey.getEncoded()));

                    JSONObject response = new JSONObject()
                            .put("service_ticket", CryptoUtils.encrypt(serviceTicket.toString(), serverKey))
                            .put("service_session_key", CryptoUtils.encrypt(Base64.getEncoder().encodeToString(serviceSessionKey.getEncoded()), sessionKey));

                    out.writeUTF(response.toString());

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}

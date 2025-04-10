package server;

import utils.CryptoUtils;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import org.json.*;

public class AUth_Server {
    private static final int PORT = 5000;
    private static final String DB_FILE = "data/userdb.json";
    private static final String TGS_KEY_FILE = "data/tgs.key";
    private static Map<String, SecretKey> userKeys = new HashMap<>();
    private static SecretKey tgsKey;

    public static void main(String[] args) throws Exception {
        File dataDir = new File("data");
        if (!dataDir.exists()) dataDir.mkdirs();

        tgsKey = CryptoUtils.loadOrCreateKey(TGS_KEY_FILE);
        loadUserDB();

        Scanner scanner = new Scanner(System.in);
        System.out.print("Register new users? (y/n): ");
        if (scanner.nextLine().equalsIgnoreCase("y")) {
            while (true) {
                System.out.print("Username (or 'done' to exit): ");
                String username = scanner.nextLine();
                if (username.equalsIgnoreCase("done")) break;
                if (userKeys.containsKey(username)) {
                    System.out.println("⚠️ Already exists.");
                    continue;
                }
                SecretKey key = CryptoUtils.generateKey();
                userKeys.put(username, key);
                System.out.println("✅ Registered " + username);
            }
            saveUserDB();
        }

        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("AS Listening on port " + PORT);

        while (true) {
            Socket socket = serverSocket.accept();
            new Thread(() -> handleClient(socket)).start();
        }
    }

    private static void handleClient(Socket socket) {
        try (socket; DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

            String request = in.readUTF();
            JSONObject input = new JSONObject(request);
            String username = input.getString("username");

            if (!userKeys.containsKey(username)) {
                out.writeUTF(new JSONObject().put("error", "Unknown user").toString());
                return;
            }

            SecretKey userKey = userKeys.get(username);
            SecretKey sessionKey = CryptoUtils.generateKey();

            JSONObject tgt = new JSONObject()
                    .put("username", username)
                    .put("session_key", Base64.getEncoder().encodeToString(sessionKey.getEncoded()));

            String encryptedTGT = CryptoUtils.encrypt(tgt.toString(), tgsKey);
            String encryptedSessionKey = CryptoUtils.encrypt(Base64.getEncoder().encodeToString(sessionKey.getEncoded()), userKey);

            JSONObject response = new JSONObject()
                    .put("tgt", encryptedTGT)
                    .put("session_key", encryptedSessionKey);

            out.writeUTF(response.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void loadUserDB() throws Exception {
        File file = new File(DB_FILE);
        if (!file.exists()) return;
        String json = Files.readString(file.toPath());
        JSONObject db = new JSONObject(json);
        for (String user : db.keySet()) {
            byte[] decoded = Base64.getDecoder().decode(db.getString(user));
            userKeys.put(user, new SecretKeySpec(decoded, "AES"));
        }
    }

    private static void saveUserDB() throws Exception {
        JSONObject db = new JSONObject();
        for (Map.Entry<String, SecretKey> entry : userKeys.entrySet()) {
            db.put(entry.getKey(), Base64.getEncoder().encodeToString(entry.getValue().getEncoded()));
        }
        Files.write(Path.of(DB_FILE), db.toString(4).getBytes());
    }
}

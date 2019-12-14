package chat.client.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

import chat.client.services.ServerMessage.MessageStatus;
import chat.client.services.ServerMessage.MessageType;

public class ChatService implements Runnable {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private ArrayList<ServerMessageListener> listeners;
    private String username;
    private boolean isAlive;
    private Thread thread;

    public ChatService(String hostname, int port) throws IOException {
        socket = new Socket(hostname, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(socket.getOutputStream(), true);
        listeners = new ArrayList<>();
        username = "";
        isAlive = true;
        thread = new Thread(this);
        thread.start();
    }

    public void registerMessageListener(ServerMessageListener messageListener) {
        this.listeners.add(messageListener);
    }

    public void unregisterMessageListener(ServerMessageListener messageListener) {
        this.listeners.remove(messageListener);
    }

    public void registerUser(String username) {
        if (!username.isEmpty()) {
            this.username = username;
            this.writer.println("IDEN " + username);
        }
    }

    public void listAllUser() {
        this.writer.println("LIST");
    }

    public void getUserStat() {
        this.writer.println("STAT");
    }

    public void broadcastMsg(String msg) {
        this.writer.println("HAIL " + msg);
    }

    public void privateMsg(String username, String msg) {
        this.writer.println("MESG " + username + " " + msg);
    }

    public void quit() {
        this.writer.println("QUIT");
    }

    public void stopService() {
        this.quit();
        this.isAlive = false;
    }

    public String getUsername() {
        return username;
    }


    /**
     * Run method.
     * Constantly listens for any new messages from the server and
     * process the messages into ServerMessage instances.
     */
    @Override
    public void run() {
        while (isAlive) {
            // Create a internal error message as placeholder.
            ServerMessage serverMessage = null;
            try {
                String rawRes = reader.readLine();
                if (rawRes != null && !rawRes.isEmpty()) {
                    serverMessage = new ServerMessage(rawRes);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (serverMessage != null) {
                for (ServerMessageListener listener : listeners) {
                    MessageType type = serverMessage.getType();
                    if (type.equals(MessageType.BROADCAST) || type.equals(MessageType.PM)) {
                        listener.onIncomingMessage(serverMessage);
                    }else {
                        if (serverMessage.getStatus() == MessageStatus.OK) {
                            listener.onServerSuccessResponse(serverMessage);
                        } else if (serverMessage.getStatus() == MessageStatus.BAD) {
                            listener.onServerErrorResponse(serverMessage);
                        }
                    }
                }
            }
        }
    }

}

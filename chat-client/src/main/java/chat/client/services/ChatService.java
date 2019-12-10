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
    private ArrayList<MessageListener> listeners;
    private String username;
    private boolean isAlive;
    private Thread thread;

    private enum State {
        IDEN, LIST, STAT, BROADCAST, PM, QUIT, NONE
    }

    private volatile State state;

    public ChatService(String hostname, int port) {
        try {
            socket = new Socket(hostname, port);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        listeners = new ArrayList<>();
        username = "";
        isAlive = true;
        state = State.NONE;
        thread = new Thread(this);
        thread.start();
    }

    public void registerMessageListener(MessageListener messageListener) {
        this.listeners.add(messageListener);
    }

    public void unregisterMessageListener(MessageListener messageListener) {
        this.listeners.remove(messageListener);
    }

    public void registerUser(String username) {
        if (!username.isEmpty()) {
            this.username = username;
            this.writer.println("IDEN " + username);
            this.state = State.IDEN;
        }
    }

    public void listAllUser() {
        this.writer.println("LIST");
        this.state = State.LIST;
    }

    public void getUserStat() {
        this.writer.println("STAT");
        this.state = State.STAT;
    }

    public void broadcastMsg(String msg) {
        this.writer.println("HAIL " + msg);
        this.state = State.BROADCAST;
    }

    public void privateMsg(String username, String msg) {
        this.writer.println("MESG " + username + " " + msg);
        this.state = State.PM;
    }

    public void quit() {
        this.writer.println("QUIT");
        this.state = State.QUIT;
    }

    public void stopService() {
        this.isAlive = false;
    }

    public State getCurrentState() {
        return this.state;
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
                for (MessageListener listener : listeners) {
                    if (serverMessage.getType().equals(MessageType.SERVER)) {
                        if (serverMessage.getStatus() == MessageStatus.OK) {
                            listener.onServerSuccessResponse(serverMessage.getMsg());
                        } else if (serverMessage.getStatus() == MessageStatus.BAD) {
                            listener.onServerErrorResponse(serverMessage.getMsg());
                        }
                        this.state = state.NONE;
                    } else if (serverMessage.getType().equals(MessageType.BROADCAST)) {
                        listener.onBroadcastResponse(serverMessage.getSourceUsername(), serverMessage.getMsg());
                    } else if (serverMessage.getType().equals(MessageType.PM)) {
                        listener.onPMResponse(serverMessage.getSourceUsername(), serverMessage.getMsg());
                    }
                }
            }
        }
    }
}

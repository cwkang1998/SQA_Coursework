package g53sqm.chat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Iterator;

public class Server {

    private ServerSocket server;
    private volatile ArrayList<Connection> list;
    protected boolean isListening;

    public Server(int port) {
        try {
            server = new ServerSocket(port);
            System.out.println("Server has been initialised on port " + server.getLocalPort());
        } catch (IOException e) {
            System.err.println("error initialising server");
            e.printStackTrace();
        }
        list = new ArrayList<Connection>();
    }

    public void listen() {
        if (!this.isListening) {
            this.isListening = true;
            while (this.isListening) {
                Connection c = null;
                try {
                    c = new Connection(server.accept(), this);
                } catch (IOException e) {
                    System.err.println("error setting up new client connection.");
                    e.printStackTrace();
                }
                Thread t = new Thread(c);
                t.start();
                synchronized (this) {
                    this.list.add(c);
                }
            }
        }
    }

    public void stopListening() {
        this.isListening = false;
    }

    public ArrayList<String> getUserList() {
        ArrayList<String> userList = new ArrayList<String>();
        Iterator<Connection> it = list.iterator();
        while (it.hasNext()) {
            Connection clientThread = it.next();
            if (clientThread.getState() == Connection.STATE_REGISTERED) {
                userList.add(clientThread.getUserName());
            }
        }
        return userList;
    }

    public boolean doesUserExist(String newUser) {
        for (Connection clientThread : list) {
            if (clientThread.getState() == Connection.STATE_REGISTERED) {
                if (clientThread.getUserName().equals(newUser)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void broadcastMessage(String theMessage) {
        System.out.println(theMessage);
        for (Connection clientThread : list) {
            clientThread.messageForConnection(theMessage + System.lineSeparator());
        }
    }

    public boolean sendPrivateMessage(String message, String user) {
        for (Connection clientThread : list) {
            if (clientThread.getState() == Connection.STATE_REGISTERED) {
                if (clientThread.getUserName().compareTo(user) == 0) {
                    clientThread.messageForConnection(message + System.lineSeparator());
                    return true;
                }
            }
        }
        return false;
    }

    public synchronized void removeDeadUsers() {
        Iterator<Connection> it = list.iterator();
        while (it.hasNext()) {
            Connection c = it.next();
            if (c != null && !c.isRunning())
                it.remove();
        }
    }

    public int getNumberOfUsers() {
        return list.size();
    }

    public boolean isListening() {
        return isListening;
    }

    public int getServerPort() {
        return server.getLocalPort();
    }

    protected void finalize() throws IOException {
        server.close();
    }

}

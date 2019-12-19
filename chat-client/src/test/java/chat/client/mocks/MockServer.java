package chat.client.mocks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class MockServer {

    private ServerSocket server;
    private Socket client;
    private BufferedReader in;
    private PrintWriter out;
    protected boolean isListening;
    private String lastReceivedMessage;

    public MockServer(int port) {
        try {
            server = new ServerSocket(port);
            System.out.println("Mock server has been initialised on port " + server.getLocalPort());
        } catch (IOException e) {
            System.err.println("error initialising mock server");
            e.printStackTrace();
        }
        lastReceivedMessage = "";
    }

    public void listen() {
        if (!this.isListening) {
            this.isListening = true;
            try {
                client = server.accept();
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                out = new PrintWriter(client.getOutputStream(), true);
            } catch (IOException e) {
                System.err.println("error setting up new client connection.");
                e.printStackTrace();
            }
            while (this.isListening) {
                try {
                    String line = in.readLine();
                    if (line != null) {
                        lastReceivedMessage = line;
                    } else {
                        stopListening();
                    }

                } catch (IOException e) {
                    stopListening();
                    System.out.println("Read failed");
                }
            }
        }
    }

    public void sendMessage(String msg) {
        out.println(msg);
    }

    public String getReceivedMessage() {
        String msg = lastReceivedMessage;
        lastReceivedMessage = "";
        return msg;
    }

    public void stopListening() {
        this.isListening = false;
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

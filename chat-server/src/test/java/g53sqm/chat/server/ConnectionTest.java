package g53sqm.chat.server;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class ConnectionTest {
    private static Server server;
    private static int serverPort;
    private static Thread serverThread;

    @BeforeClass
    public static void setup() {
        server = new Server(0);
        serverPort = server.getServerPort();
        serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("Start Server Thread.");
                server.listen();
            }
        });
        // Start the thread as daemon so it would stop when JVM stop
        serverThread.setDaemon(true);
        serverThread.start();
    }

    @AfterClass
    public static void cleanUp() {
        server.stopListening();
    }
}
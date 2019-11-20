package g53sqm.chat.server;

import org.junit.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

import static org.junit.Assert.assertArrayEquals;

public class TestServer {

    private static Server server;
    private static Socket client1;
    private static Socket client2;
    final static private int TEST_PORT = 9001;


    /**
     * Initialize Server and mock clients for the unit tests
     *
     * @throws Exception
     */
    @BeforeClass
    public static void init() throws Exception {
        // Set up test server
        server = new Server(TEST_PORT);
        Thread serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("Start Server Thread.");
                server.listen();
            }
        });
        // Start the thread as daemon so it would stop when JVM stop
        serverThread.setDaemon(true);
        serverThread.start();

        // Set up mock test clients
        try {
            // Set up 2 test client
            client1 = new Socket("localhost", TEST_PORT);
            PrintWriter client1Out = new PrintWriter(client1.getOutputStream(), true);
            client1Out.println("IDEN client1");
            client2 = new Socket("localhost", TEST_PORT);
            PrintWriter client2Out = new PrintWriter(client2.getOutputStream(), true);
            client2Out.println("IDEN client2");

        } catch (IOException e) {
            throw new Exception("Failed to setup.");
        }
    }

    @Test
    public void testGetUserListsReturn2CorrectUser() {
        ArrayList<String> actual = server.getUserList();
        String[] expected = new String[]{"client1", "client2"};

        // Check if the arrays have the same elements in the same order
        assertArrayEquals(actual.toArray(), expected);
    }


    @AfterClass
    public static void cleanUp() {
        server.stopListening();
        client1 = null;
        client2 = null;
    }

}
package g53sqm.chat.server;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class ServerTest {

    private Server server;
    private int serverPort;
    private Thread serverThread;

    @Before
    public void setupServer() {
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

        // Let it sleep for 1 second to ensure thread executed
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void isListening_ServerCreated_ReturnsFalse() {
        Server server = new Server(0);
        assertFalse(server.isListening());
        server.stopListening();
    }

    @Test
    public void isListening_ServerStartAndStop_ReturnsCorrectState() {
        Server server = new Server(0);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("Start Server Thread.");
                server.listen();
            }
        });
        // Start the thread as daemon so it would stop when JVM stop
        thread.setDaemon(true);
        thread.start();

        // Let it sleep for 1 second to ensure thread executed
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertTrue(server.isListening());

        server.stopListening();
        // Let it sleep for another second to ensure server died
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertFalse(server.isListening());
    }

    @Test
    public void getUserList_NoUsersConnected_ReturnsEmptyList() {
        ArrayList<String> actual = server.getUserList();
        assertArrayEquals(new String[]{}, actual.toArray());
    }

    @Test
    public void getUserList_2UsersConnected_ReturnsListWithCorrectUsernames() {
        // Create users
        Socket user1 = createMockUsers("user1", serverPort);
        Socket user2 = createMockUsers("user2", serverPort);

        String[] expected = new String[]{"user1", "user2"};
        ArrayList<String> actual = server.getUserList();

        assertEquals(expected[0], actual.get(0));
        assertEquals(expected[1], actual.get(1));
        assertArrayEquals(expected, actual.toArray());
    }

    @Test
    public void getUserList_ConnectedUsersQuit_ReturnsCorrectList() {
        // Create users
        Socket user1 = createMockUsers("user1", serverPort);
        Socket user2 = createMockUsers("user2", serverPort);

        // user1 quit
        userSendMessage(user1, "QUIT");
        ArrayList<String> actual = server.getUserList();
        assertEquals("user2", actual.get(0));
        assertArrayEquals(new String[]{"user2"}, actual.toArray());

        // user2 quit
        userSendMessage(user2, "QUIT");
        actual = server.getUserList();
        assertArrayEquals(new String[]{}, actual.toArray());
    }

    @Test
    public void getUserList_ConnectedUsersDisconnected_ReturnsCorrectList() {
        // Create users
        Socket user1 = createMockUsers("user1", serverPort);
        Socket user2 = createMockUsers("user2", serverPort);

        // Close user1 connection
        try {
            user1.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        ArrayList<String> actual = server.getUserList();
        assertEquals("user2", actual.get(0));
        assertArrayEquals(new String[]{"user2"}, actual.toArray());

        // Close user2 connection
        try {
            user2.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        actual = server.getUserList();
        assertArrayEquals(new String[]{}, actual.toArray());
    }

    @Test
    public void getNumberOfUsers_NoUsersConnected_ReturnsZero() {
        int numberOfUsers = server.getNumberOfUsers();
        assertEquals(0, numberOfUsers);
    }

    @Test
    public void getNumberOfUsers_3UsersConnected_Returns3() {
        // Create users
        Socket user1 = createMockUsers("user1", serverPort);
        Socket user2 = createMockUsers("user2", serverPort);
        Socket user3 = createMockUsers("user3", serverPort);

        int numberOfUsers = server.getNumberOfUsers();
        assertEquals(3, numberOfUsers);
    }

    @Test
    public void getNumberOfUsers_ConnectedUsersQuit_ReturnsCorrectNumber() {
        // Create users
        Socket user1 = createMockUsers("user1", serverPort);
        Socket user2 = createMockUsers("user2", serverPort);

        // user1 quit
        userSendMessage(user1, "QUIT");
        assertEquals(1, server.getNumberOfUsers());

        // user2 quit
        userSendMessage(user2, "QUIT");
        assertEquals(0, server.getNumberOfUsers());
    }

    @Test
    public void getNumberOfUsers_ConnectedUsersDisconnected_ReturnsCorrectNumber() {
        // Create users
        Socket user1 = createMockUsers("user1", serverPort);
        Socket user2 = createMockUsers("user2", serverPort);

        // Close user1 connection
        try {
            user1.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        assertEquals(1, server.getNumberOfUsers());

        // Close user2 connection
        try {
            user2.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        assertEquals(0, server.getNumberOfUsers());
    }

    @Test
    public void doesUserExists_NoUserExists_ReturnsFalse() {
        // Test a random name
        boolean gotUser = server.doesUserExist("someName");
        assertFalse(gotUser);
    }

    @Test
    public void doesUserExists_UserExists_ReturnsTrue() {
        Socket user1 = createMockUsers("existingUser", serverPort);

        boolean gotUser = server.doesUserExist("existingUser");
        assertTrue(gotUser);
    }

    @Test
    public void doesUserExists_MultipleUsersExists_ReturnsTrue() {
        Socket user1 = createMockUsers("existingUser", serverPort);
        Socket user2 = createMockUsers("existingUser2", serverPort);

        boolean gotUser = server.doesUserExist("existingUser");
        boolean gotUser2 = server.doesUserExist("existingUser2");
        assertTrue(gotUser);
        assertTrue(gotUser2);
    }

    @Test
    public void doesUserExists_MultipleUsersConnectAndQuit_ReturnsCorrectBoolean() {
        Socket user1 = createMockUsers("existingUser", serverPort);
        Socket user2 = createMockUsers("existingUser2", serverPort);

        boolean gotUser = server.doesUserExist("existingUser");
        boolean gotUser2 = server.doesUserExist("existingUser2");
        assertTrue(gotUser);
        assertTrue(gotUser2);

        userSendMessage(user1, "QUIT");
        gotUser = server.doesUserExist("existingUser");
        gotUser2 = server.doesUserExist("existingUser2");
        assertFalse(gotUser);
        assertTrue(gotUser2);

        userSendMessage(user2, "QUIT");
        gotUser = server.doesUserExist("existingUser");
        gotUser2 = server.doesUserExist("existingUser2");
        assertFalse(gotUser);
        assertFalse(gotUser2);
    }

    @Test
    public void doesUserExists_MultipleUsersConnectAndDisconnect_ReturnsCorrectBoolean() {
        Socket user1 = createMockUsers("existingUser", serverPort);
        Socket user2 = createMockUsers("existingUser2", serverPort);

        boolean gotUser = server.doesUserExist("existingUser");
        boolean gotUser2 = server.doesUserExist("existingUser2");
        assertTrue(gotUser);
        assertTrue(gotUser2);

        // Close user1 connection
        try {
            user1.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        gotUser = server.doesUserExist("existingUser");
        gotUser2 = server.doesUserExist("existingUser2");
        assertFalse(gotUser);
        assertTrue(gotUser2);

        // Close user2 connection
        try {
            user2.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        gotUser = server.doesUserExist("existingUser");
        gotUser2 = server.doesUserExist("existingUser2");
        assertFalse(gotUser);
        assertFalse(gotUser2);
    }

    @Test
    public void broadcastMessage_SingleUserSingleMessage_UserReceivesMessage() {
        Socket user = createMockUsers("user1", serverPort);
        userWaitForMessage(user); // Clean first line of buffer, the login message

        String msg = "Hello!";
        server.broadcastMessage(msg);
        String actual = userWaitForMessage(user);

        assertEquals(msg, actual);
    }

    @Test
    public void broadcastMessage_SingleUserMultipleMessages_UserReceivesMultipleMessages() {
        Socket user = createMockUsers("user1", serverPort);
        userWaitForMessage(user); // Clean first line of buffer, the login message

        String msg1 = "Hello!";
        String msg2 = "World!";
        server.broadcastMessage(msg1);
        String actual1 = userWaitForMessage(user);
        server.broadcastMessage(msg2);
        String actual2 = userWaitForMessage(user);

        assertEquals(msg1, actual1);
        assertEquals(msg2, actual2);
    }

    @Test
    public void broadcastMessage_MultipleUsers_UsersReceiveMessage() {
        Socket user1 = createMockUsers("user1", serverPort);
        Socket user2 = createMockUsers("user2", serverPort);
        // Clean first line of buffer, the login message
        userWaitForMessage(user1);
        userWaitForMessage(user2);

        String msg = "Hello!";
        server.broadcastMessage(msg);
        String actualUser1 = userWaitForMessage(user1);
        String actualUser2 = userWaitForMessage(user2);

        assertEquals(msg, actualUser1);
        assertEquals(msg, actualUser2);
    }

    @Test
    public void sendPrivateMessage_SingleUserCorrectUsername_UserReceivesMessage() {
        Socket user = createMockUsers("user", serverPort);
        userWaitForMessage(user); // Clean first line of buffer, the login message

        String msg = "Hello!";
        server.sendPrivateMessage(msg, "user");
        String actual = userWaitForMessage(user);

        assertEquals(msg, actual);
    }

    @Test
    public void sendPrivateMessage_SingleUserIncorrectUsername_UserReceivesMessage() {
        Socket user = createMockUsers("user", serverPort);
        userWaitForMessage(user); // Clean first line of buffer, the login message

        String msg = "Hello!";
        server.sendPrivateMessage(msg, "user1");
        server.broadcastMessage(""); // Use this to ensure the next line doesn't block.
        String actual = userWaitForMessage(user);

        assertNotEquals(msg, actual);
    }

    @Test
    public void sendPrivateMessage_SingleUserMultipleMessages_UserReceivesMessage() {
        Socket user = createMockUsers("user", serverPort);
        userWaitForMessage(user); // Clean first line of buffer, the login message

        String msg = "Hello!";
        String msg2 = "Yo!";
        server.sendPrivateMessage(msg, "user");
        String actual1 = userWaitForMessage(user);
        server.sendPrivateMessage(msg2, "user");
        String actual2 = userWaitForMessage(user);

        assertEquals(msg, actual1);
        assertEquals(msg2, actual2);
    }

    @Test
    public void sendPrivateMessage_MultipleUsers_OnlySpecifiedUserReceivesMessage() {
        Socket user1 = createMockUsers("user1", serverPort);
        Socket user2 = createMockUsers("user2", serverPort);
        // Clean first line of buffer, the login message
        userWaitForMessage(user1);
        userWaitForMessage(user2);

        String msg = "Hello!";
        server.sendPrivateMessage(msg, "user1");
        server.broadcastMessage(""); // Use this to ensure the others doesn't block.
        String actualUser1 = userWaitForMessage(user1);
        String actualUser2 = userWaitForMessage(user2);

        assertEquals(msg, actualUser1);
        assertNotEquals(msg, actualUser2);

        // Send to another user
        server.sendPrivateMessage(msg, "user2");
        server.broadcastMessage(""); // Use this to ensure the others doesn't block.
        actualUser1 = userWaitForMessage(user1);
        actualUser2 = userWaitForMessage(user2);

        assertNotEquals(msg, actualUser1);
        assertEquals(msg, actualUser2);
    }

    @Test
    public void removeDeadUser_OnlineUser_UserNotRemoved() {
        Socket user = createMockUsers("user", serverPort);
        int noUsers = server.getNumberOfUsers();

        server.removeDeadUsers();
        int noUsersAfterRemove = server.getNumberOfUsers();
        assertEquals(noUsers, noUsersAfterRemove);
    }

    @Test
    public void removeDeadUser_QuitDeadUser_UserRemoved() {
        Socket user = createMockUsers("user", serverPort);

        // user quit
        userSendMessage(user, "QUIT");
        server.removeDeadUsers();
        int noUsersAfterRemove = server.getNumberOfUsers();

        assertEquals(0, noUsersAfterRemove);
    }

    @Test
    public void removeDeadUser_DisconnectedDeadUser_UserRemoved() {
        Socket user = createMockUsers("user", serverPort);

        // Close user connection
        try {
            user.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        server.removeDeadUsers();
        int noUsersAfterRemove = server.getNumberOfUsers();

        assertEquals(0, noUsersAfterRemove);
    }

    @Test
    public void removeDeadUser_OneQuitFromMultipleUser_OneUserRemoved() {
        Socket user1 = createMockUsers("user1", serverPort);
        Socket user2 = createMockUsers("user2", serverPort);

        // user1 quit
        userSendMessage(user1, "QUIT");

        server.removeDeadUsers();
        assertEquals(1, server.getNumberOfUsers());
        assertEquals("user2", server.getUserList().get(0));
    }

    @Test
    public void removeDeadUser_OneDisconnectFromMultipleUser_OneUserRemoved() {
        Socket user1 = createMockUsers("user1", serverPort);
        Socket user2 = createMockUsers("user2", serverPort);

        // Close user1 connection
        try {
            user1.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        server.removeDeadUsers();
        assertEquals(1, server.getNumberOfUsers());
        assertEquals("user2", server.getUserList().get(0));

        // Close user2 connection
        try {
            user2.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @After
    public void stopServer() {
        server.stopListening();
    }

    /**
     * Create user that connects to the server based on the given user name.
     * Wait for a second after creation to ensure that the user actually connects.
     *
     * @param userName Name of the user created.
     * @param port     Port that you want to connect to
     * @return true if creation of user is successful, and false otherwise
     */
    private Socket createMockUsers(String userName, int port) {
        Socket user = null;
        try {
            // crete new socket for test user
            user = new Socket("localhost", port);
            userSendMessage(user, "IDEN " + userName);
        } catch (IOException e) {
            Assert.fail("Fail to create mock user.");
        }
        return user;
    }

    /**
     * Sends a given message through the given user socket.
     *
     * @param user Socket acting as a user that you want to send message with
     * @param msg  the string you want to send as message
     */
    private void userSendMessage(Socket user, String msg) {
        try {
            PrintWriter userOut = new PrintWriter(user.getOutputStream(), true);
            userOut.println(msg);
            Thread.sleep(1000);
        } catch (IOException | InterruptedException e) {
            Assert.fail("Fail to send message");
        }
    }

    /**
     * @param user User that is waiting for a message
     * @return the received message string
     */
    private String userWaitForMessage(Socket user) {
        String line = "";
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(user.getInputStream()));
            line = reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return line;
    }

}
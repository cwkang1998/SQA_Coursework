package g53sqm.chat.server;

import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

import static org.junit.Assert.*;

public class ServerTest {

    @Test
    public void isListening_ServerCreated_ReturnsFalse() {
        Server server = new Server(10001);
        assertFalse(server.isListening());
        server.stopListening();
    }

    @Test
    public void isListening_ServerStartAndStop_ReturnsCorrectState() {
        Server server = new Server(10002);
        Thread thread = runServerInThread(server);
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
        Server server = new Server(10003);
        Thread thread = runServerInThread(server);
        ArrayList<String> actual = server.getUserList();

        assertArrayEquals(new String[]{}, actual.toArray());
    }

    @Test
    public void getUserList_2UsersConnected_ReturnsListWithCorrectUsernames() {
        Server server = new Server(10004);
        Thread thread = runServerInThread(server);
        // Create users
        Socket user1 = createMockUsers("user1", 10004);
        Socket user2 = createMockUsers("user2", 10004);

        String[] expected = new String[]{"user1", "user2"};
        ArrayList<String> actual = server.getUserList();

        // Close user connection
        try {
            user1.close();
            user2.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        server.stopListening();

        assertEquals(expected[0], actual.get(0));
        assertEquals(expected[1], actual.get(1));
        assertArrayEquals(expected, actual.toArray());
    }

    @Test
    public void getUserList_ConnectedUsersQuit_ReturnsCorrectList() {
        Server server = new Server(10005);
        Thread thread = runServerInThread(server);
        // Create users
        Socket user1 = createMockUsers("user1", 10005);
        Socket user2 = createMockUsers("user2", 10005);

        // user1 quit
        userSendMessage(user1, "QUIT");
        ArrayList<String> actual = server.getUserList();
        assertEquals("user2", actual.get(0));
        assertArrayEquals(new String[]{"user2"}, actual.toArray());

        // user2 quit
        userSendMessage(user2, "QUIT");
        actual = server.getUserList();
        assertArrayEquals(new String[]{}, actual.toArray());


        // Close user connection
        try {
            user1.close();
            user2.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        server.stopListening();
    }

    @Test
    public void getUserList_ConnectedUsersDisconnected_ReturnsCorrectList() {
        Server server = new Server(10006);
        Thread thread = runServerInThread(server);
        // Create users
        Socket user1 = createMockUsers("user1", 10006);
        Socket user2 = createMockUsers("user2", 10006);

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

        server.stopListening();
    }

    @Test
    public void getNumberOfUsers_NoUsersConnected_ReturnsZero() {
        Server server = new Server(10007);
        int numberOfUsers = server.getNumberOfUsers();
        assertEquals(0, numberOfUsers);
        server.stopListening();
    }

    @Test
    public void getNumberOfUsers_3UsersConnected_Returns3() {
        Server server = new Server(10008);
        Thread thread = runServerInThread(server);
        // Create users
        Socket user1 = createMockUsers("user1", 10008);
        Socket user2 = createMockUsers("user2", 10008);
        Socket user3 = createMockUsers("user3", 10008);

        int numberOfUsers = server.getNumberOfUsers();
        assertEquals(3, numberOfUsers);

        server.stopListening();
        // Close user connection
        try {
            user1.close();
            user2.close();
            user3.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getNumberOfUsers_ConnectedUsersQuit_ReturnsCorrectNumber() {
        Server server = new Server(10009);
        Thread thread = runServerInThread(server);
        // Create users
        Socket user1 = createMockUsers("user1", 10009);
        Socket user2 = createMockUsers("user2", 10009);

        // user1 quit
        userSendMessage(user1, "QUIT");
        assertEquals(1, server.getNumberOfUsers());

        // user2 quit
        userSendMessage(user2, "QUIT");
        assertEquals(0, server.getNumberOfUsers());

        server.stopListening();
        // Close user connection
        try {
            user1.close();
            user2.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getNumberOfUsers_ConnectedUsersDisconnected_ReturnsCorrectNumber() {
        Server server = new Server(10010);
        Thread thread = runServerInThread(server);
        // Create users
        Socket user1 = createMockUsers("user1", 10010);
        Socket user2 = createMockUsers("user2", 10010);

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

        server.stopListening();
    }

    @Test
    public void doesUserExists_NoUserExists_ReturnsFalse() {
        Server server = new Server(10011);
        Thread thread = runServerInThread(server);

        // Test a random name
        boolean gotUser = server.doesUserExist("someName");
        assertFalse(gotUser);

        server.stopListening();
    }

    @Test
    public void doesUserExists_UserExists_ReturnsTrue() {
        Server server = new Server(10012);
        Thread thread = runServerInThread(server);
        Socket user1 = createMockUsers("existingUser", 10012);

        boolean gotUser = server.doesUserExist("existingUser");
        assertTrue(gotUser);

        server.stopListening();
        // Close user connection
        try {
            user1.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void doesUserExists_MultipleUsersExists_ReturnsTrue() {
        Server server = new Server(10013);
        Thread thread = runServerInThread(server);
        Socket user1 = createMockUsers("existingUser", 10013);
        Socket user2 = createMockUsers("existingUser2", 10013);

        boolean gotUser = server.doesUserExist("existingUser");
        boolean gotUser2 = server.doesUserExist("existingUser2");
        assertTrue(gotUser);
        assertTrue(gotUser2);

        server.stopListening();
        // Close user connection
        try {
            user1.close();
            user2.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void doesUserExists_MultipleUsersConnectAndQuit_ReturnsCorrectBoolean() {
        Server server = new Server(10014);
        Thread thread = runServerInThread(server);
        Socket user1 = createMockUsers("existingUser", 10014);
        Socket user2 = createMockUsers("existingUser2", 10014);

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

        server.stopListening();
        // Close user connection
        try {
            user1.close();
            user2.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void doesUserExists_MultipleUsersConnectAndDisconnect_ReturnsCorrectBoolean() {
        Server server = new Server(10015);
        Thread thread = runServerInThread(server);
        Socket user1 = createMockUsers("existingUser", 10015);
        Socket user2 = createMockUsers("existingUser2", 10015);

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

        server.stopListening();
    }

    @Test
    public void broadcastMessage_SingleUserSingleMessage_UserReceivesMessage() {
        Server server = new Server(10016);
        Thread thread = runServerInThread(server);
        Socket user = createMockUsers("user1", 10016);
        userWaitForMessage(user); // Clean first line of buffer, the login message

        String msg = "Hello!";
        server.broadcastMessage(msg);
        String actual = userWaitForMessage(user);

        assertEquals(msg, actual);

        server.stopListening();
        // Close user connection
        try {
            user.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void broadcastMessage_SingleUserMultipleMessages_UserReceivesMultipleMessages() {
        Server server = new Server(10017);
        Thread thread = runServerInThread(server);
        Socket user = createMockUsers("user1", 10017);
        userWaitForMessage(user); // Clean first line of buffer, the login message

        String msg1 = "Hello!";
        String msg2 = "World!";
        server.broadcastMessage(msg1);
        String actual1 = userWaitForMessage(user);
        server.broadcastMessage(msg2);
        String actual2 = userWaitForMessage(user);

        assertEquals(msg1, actual1);
        assertEquals(msg2, actual2);

        server.stopListening();
        // Close user connection
        try {
            user.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void broadcastMessage_MultipleUsers_UsersReceiveMessage() {
        Server server = new Server(10018);
        Thread thread = runServerInThread(server);
        Socket user1 = createMockUsers("user1", 10018);
        Socket user2 = createMockUsers("user2", 10018);
        // Clean first line of buffer, the login message
        userWaitForMessage(user1);
        userWaitForMessage(user2);

        String msg = "Hello!";
        server.broadcastMessage(msg);
        String actualUser1 = userWaitForMessage(user1);
        String actualUser2 = userWaitForMessage(user2);

        assertEquals(msg, actualUser1);
        assertEquals(msg, actualUser2);

        server.stopListening();
        // Close user connection
        try {
            user1.close();
            user2.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void sendPrivateMessage_SingleUserCorrectUsername_UserReceivesMessage() {
        Server server = new Server(10019);
        Thread thread = runServerInThread(server);
        Socket user = createMockUsers("user", 10019);
        userWaitForMessage(user); // Clean first line of buffer, the login message

        String msg = "Hello!";
        server.sendPrivateMessage(msg, "user");
        String actual = userWaitForMessage(user);

        assertEquals(msg, actual);

        server.stopListening();
        // Close user connection
        try {
            user.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void sendPrivateMessage_SingleUserIncorrectUsername_UserReceivesMessage() {
        Server server = new Server(10020);
        Thread thread = runServerInThread(server);
        Socket user = createMockUsers("user", 10020);
        userWaitForMessage(user); // Clean first line of buffer, the login message

        String msg = "Hello!";
        server.sendPrivateMessage(msg, "user1");
        server.broadcastMessage(""); // Use this to ensure the next line doesn't block.
        String actual = userWaitForMessage(user);

        assertNotEquals(msg, actual);

        server.stopListening();
        // Close user connection
        try {
            user.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void sendPrivateMessage_SingleUserMultipleMessages_UserReceivesMessage() {
        Server server = new Server(10021);
        Thread thread = runServerInThread(server);
        Socket user = createMockUsers("user", 10021);
        userWaitForMessage(user); // Clean first line of buffer, the login message

        String msg = "Hello!";
        String msg2 = "Yo!";
        server.sendPrivateMessage(msg, "user");
        String actual1 = userWaitForMessage(user);
        server.sendPrivateMessage(msg2, "user");
        String actual2 = userWaitForMessage(user);

        assertEquals(msg, actual1);
        assertEquals(msg2, actual2);

        server.stopListening();
        // Close user connection
        try {
            user.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void sendPrivateMessage_MultipleUsers_OnlySpecifiedUserReceivesMessage() {
        Server server = new Server(10022);
        Thread thread = runServerInThread(server);
        Socket user1 = createMockUsers("user1", 10022);
        Socket user2 = createMockUsers("user2", 10022);
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

        server.stopListening();
        // Close user connection
        try {
            user1.close();
            user2.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void removeDeadUser_OnlineUser_UserNotRemoved() {
        Server server = new Server(10023);
        Thread thread = runServerInThread(server);
        Socket user = createMockUsers("user", 10023);
        int noUsers = server.getNumberOfUsers();

        server.removeDeadUsers();
        int noUsersAfterRemove = server.getNumberOfUsers();
        assertEquals(noUsers, noUsersAfterRemove);

        server.stopListening();
        // Close user connection
        try {
            user.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void removeDeadUser_QuitDeadUser_UserRemoved() {
        Server server = new Server(10023);
        Thread thread = runServerInThread(server);
        Socket user = createMockUsers("user", 10023);

        // user quit
        userSendMessage(user, "QUIT");
        server.removeDeadUsers();
        int noUsersAfterRemove = server.getNumberOfUsers();

        assertEquals(0, noUsersAfterRemove);

        server.stopListening();
        // Close user connection
        try {
            user.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void removeDeadUser_DisconnectedDeadUser_UserRemoved() {
        Server server = new Server(10024);
        Thread thread = runServerInThread(server);
        Socket user = createMockUsers("user", 10024);

        // Close user connection
        try {
            user.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        server.removeDeadUsers();
        int noUsersAfterRemove = server.getNumberOfUsers();

        assertEquals(0, noUsersAfterRemove);

        server.stopListening();
    }

    @Test
    public void removeDeadUser_OneQuitFromMultipleUser_OneUserRemoved() {
        Server server = new Server(10025);
        Thread thread = runServerInThread(server);
        Socket user1 = createMockUsers("user1", 10025);
        Socket user2 = createMockUsers("user2", 10025);

        // user1 quit
        userSendMessage(user1, "QUIT");

        server.removeDeadUsers();
        assertEquals(1, server.getNumberOfUsers());
        assertEquals("user2", server.getUserList().get(0));

        server.stopListening();
        // Close user connection
        try {
            user1.close();
            user2.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void removeDeadUser_OneDisconnectFromMultipleUser_OneUserRemoved() {
        Server server = new Server(10026);
        Thread thread = runServerInThread(server);
        Socket user1 = createMockUsers("user1", 10026);
        Socket user2 = createMockUsers("user2", 10026);

        // Close user connection
        try {
            user1.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        server.removeDeadUsers();
        assertEquals(1, server.getNumberOfUsers());
        assertEquals("user2", server.getUserList().get(0));

        server.stopListening();
        // Close user connection
        try {
            user2.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Run the given server on a new thread
     *
     * @param serverInstance the given server instance
     * @return thread with running server instance in it
     */
    private static Thread runServerInThread(Server serverInstance) {
        Thread serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("Start Server Thread.");
                serverInstance.listen();
            }
        });
        // Start the thread as daemon so it would stop when JVM stop
        serverThread.setDaemon(true);
        serverThread.start();

        return serverThread;
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
package g53sqm.chat.server;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.net.Socket;
import java.util.ArrayList;

import static g53sqm.chat.server.Connection.STATE_REGISTERED;
import static g53sqm.chat.server.Connection.STATE_UNREGISTERED;
import static org.junit.Assert.*;

public class ConnectionTest {
    private Server server;
    private int serverPort;
    private Thread serverThread;
    private ArrayList<Connection> connectionList;


    @Before
    public void setup() {
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

        // Let it sleep for 0.5 second to ensure thread executed
        sleep(500);
    }

    @Test
    public void socket_AfterFirstConnect_ReceiveWelcomeMessage() {
        Socket socket = createNewConnection(serverPort);
        Connection connection = connectionList.get(0);
        assertTrue(connection.isRunning());
        String expectedMsg = "OK Welcome to the chat server, there are currently 1 user(s) online";
        String msg = waitAndRetrieveNextMessage(socket);
        assertEquals(expectedMsg, msg);
    }

    @Test
    public void connection_AfterFirstConnect_StateNotRegisteredAndNoUsername() {
        createNewConnection(serverPort);
        Connection connection = connectionList.get(0);
        assertTrue(connection.isRunning());
        assertEquals(STATE_UNREGISTERED, connection.getState());
        assertNull(connection.getUserName());
    }

    @Test
    public void getState_UnregisteredUser_ReturnsUnregisteredState() {
        createNewConnection(serverPort);
        Connection connection = connectionList.get(0);
        assertEquals(STATE_UNREGISTERED, connection.getState());
    }

    @Test
    public void getState_RegisteredUser_ReturnsRegisteredState() {
        Socket socket = createNewConnection(serverPort);
        socketSendMessage(socket, "IDEN user");
        Connection connection = connectionList.get(0);
        assertEquals(STATE_REGISTERED, connection.getState());
    }

    @Test
    public void getUserName_UnregisteredUser_ReturnsNull() {
        createNewConnection(serverPort);
        Connection connection = connectionList.get(0);
        assertNull(connection.getUserName());
    }

    @Test
    public void getUserName_RegisteredUser_ReturnsCorrectUserName() {
        Socket socket = createNewConnection(serverPort);
        String username = "myusername";
        socketSendMessage(socket, "IDEN " + username);
        Connection connection = connectionList.get(0);
        assertEquals(username, connection.getUserName());
    }

    @Test
    public void messageForConnection_SendMessages_SocketReceiveCorrectMessage() {
        Socket socket = createNewConnection(serverPort);
        Connection connection = connectionList.get(0);

        // Clear first welcome message
        waitAndRetrieveNextMessage(socket);

        connection.messageForConnection("My Message");
        String msg = waitAndRetrieveNextMessage(socket);

        assertEquals("My Message", msg);

        connection.messageForConnection("Second Message");
        msg = waitAndRetrieveNextMessage(socket);

        assertEquals("Second Message", msg);
    }

    @Test
    public void messageForConnection_SendEmptyStringMessage_SocketReceiveEmptyString() {
        Socket socket = createNewConnection(serverPort);
        Connection connection = connectionList.get(0);

        // Clear first welcome message
        waitAndRetrieveNextMessage(socket);

        connection.messageForConnection("");
        String msg = waitAndRetrieveNextMessage(socket);

        assertEquals("", msg);
    }

    @Test
    public void messageForConnection_SendMessageToMultipleConnections_CorrectSocketReceiveMessage() {
        Socket socket1 = createNewConnection(serverPort);
        Socket socket2 = createNewConnection(serverPort);
        Connection connection1 = connectionList.get(0);
        Connection connection2 = connectionList.get(1);

        // Clear first welcome message
        waitAndRetrieveNextMessage(socket1);
        waitAndRetrieveNextMessage(socket2);

        String con1Msg = "C1 msg";
        String con2Msg = "C2 msg";

        connection1.messageForConnection(con1Msg);
        connection2.messageForConnection(con2Msg);
        String con1Reply = waitAndRetrieveNextMessage(socket1);
        String con2Reply = waitAndRetrieveNextMessage(socket2);
        assertEquals(con1Msg, con1Reply);
        assertNotEquals(con2Msg, con1Reply);
        assertEquals(con2Msg, con2Reply);
        assertNotEquals(con1Msg, con2Reply);
    }

    @Test
    public void validateMessage_InvalidCommand_SendsBackErrorMessage() {
        Socket socket = createNewConnection(serverPort);

        String shortErrMsg = "BAD invalid command to server";
        String longErrMsg = "BAD command not recognised";
        String actualMsg = "";

        // Clear first welcome message
        waitAndRetrieveNextMessage(socket);

        // Empty Message
        socketSendMessage(socket, "");
        actualMsg = waitAndRetrieveNextMessage(socket);
        assertEquals(shortErrMsg, actualMsg);

        // Short Invalid Message
        socketSendMessage(socket, "ABC");
        actualMsg = waitAndRetrieveNextMessage(socket);
        assertEquals(shortErrMsg, actualMsg);

        //Exact Invalid Message
        socketSendMessage(socket, "NOPE");
        actualMsg = waitAndRetrieveNextMessage(socket);
        assertEquals(longErrMsg, actualMsg);

        // Long Invalid Message
        socketSendMessage(socket, "ABCDE");
        actualMsg = waitAndRetrieveNextMessage(socket);
        assertEquals(longErrMsg, actualMsg);
    }

//    @Test
//    public void iden_NoUsername_ReturnsErrorMsg() {
//        Socket socket = createNewConnection(serverPort);
//        // Clear first welcome message
//        waitAndRetrieveNextMessage(socket);
//
//        String expectedMsg = "BAD username not given";
//        socketSendMessage(socket, "IDEN");
//        String actualMsg = waitAndRetrieveNextMessage(socket);
//        assertEquals(expectedMsg, actualMsg);
//    }
//
//    @Test
//    public void iden_EmptyUsername_ReturnsErrorMsg() {
//        Socket socket = createNewConnection(serverPort);
//        Connection connection = connectionList.get(0);
//        // Clear first welcome message
//        waitAndRetrieveNextMessage(socket);
//
//        String expectedMsg = "BAD username not given";
//        socketSendMessage(socket, "IDEN    ");
//        String actualMsg = waitAndRetrieveNextMessage(socket);
//        assertEquals(expectedMsg, actualMsg);
//    }

    @Test
    public void iden_UsernameGivenWhenUnregistered_ReturnsWelcomeMsg() {
        Socket socket = createNewConnection(serverPort);
        // Clear first welcome message
        waitAndRetrieveNextMessage(socket);

        String username = "user1";
        String expectedMsg = "OK Welcome to the chat server " + username;
        socketSendMessage(socket, "IDEN " + username);
        String actualMsg = waitAndRetrieveNextMessage(socket);
        assertEquals(expectedMsg, actualMsg);
    }

    @Test
    public void iden_ExistingUsernameGivenWhenUnregistered_ReturnsErrorMsg() {
        Socket socket1 = createNewConnection(serverPort);
        Socket socket2 = createNewConnection(serverPort);
        // Clear first welcome message
        waitAndRetrieveNextMessage(socket1);
        waitAndRetrieveNextMessage(socket2);

        String username = "user1";
        String expectedMsg = "BAD username is already taken";
        socketSendMessage(socket1, "IDEN " + username);
        socketSendMessage(socket2, "IDEN " + username);
        String actualMsg = waitAndRetrieveNextMessage(socket2);
        assertEquals(expectedMsg, actualMsg);
    }

    @Test
    public void iden_ExistingUsernameGivenMultipleTimesWhenUnregistered_ReturnsErrorMsg() {
        Socket socket1 = createNewConnection(serverPort);
        Socket socket2 = createNewConnection(serverPort);
        Socket socket3 = createNewConnection(serverPort);
        // Clear first welcome message
        waitAndRetrieveNextMessage(socket1);
        waitAndRetrieveNextMessage(socket2);
        waitAndRetrieveNextMessage(socket3);

        String username = "user1";
        String expectedMsg = "BAD username is already taken";
        socketSendMessage(socket1, "IDEN " + username);
        socketSendMessage(socket2, "IDEN " + username);
        socketSendMessage(socket3, "IDEN " + username);

        String actualMsg = waitAndRetrieveNextMessage(socket2);
        assertEquals(expectedMsg, actualMsg);
        actualMsg = waitAndRetrieveNextMessage(socket3);
        assertEquals(expectedMsg, actualMsg);

        socketSendMessage(socket2, "IDEN " + username);
        actualMsg = waitAndRetrieveNextMessage(socket2);
        assertEquals(expectedMsg, actualMsg);
    }

    @Test
    public void iden_UsernameGivenWhenRegistered_ReturnsErrorMsg() {
        Socket socket = createNewConnection(serverPort);
        // Clear first welcome message
        waitAndRetrieveNextMessage(socket);

        String username = "user1";
        String expectedMsg = "BAD you are already registered with username " + username;

        // Register and Remove first welcome message
        socketSendMessage(socket, "IDEN " + username);
        waitAndRetrieveNextMessage(socket);

        socketSendMessage(socket, "IDEN " + username);
        String actualMsg = waitAndRetrieveNextMessage(socket);
        assertEquals(expectedMsg, actualMsg);
    }

    @Test
    public void stat_UnregisteredUser_ReturnsCorrectMsg() {
        Socket socket = createNewConnection(serverPort);

        // Clear first welcome message
        waitAndRetrieveNextMessage(socket);

        String expectedMsg = "OK There are currently 1 user(s) on the server You have not logged in yet";
        socketSendMessage(socket, "STAT");
        String actualMsg = waitAndRetrieveNextMessage(socket);
        assertEquals(expectedMsg, actualMsg);
    }

    @Test
    public void stat_RegisteredUserNoMsgSent_ReturnsCorrectMsg() {
        Socket socket = createNewConnection(serverPort);

        // Clear welcome and iden msg
        socketSendMessage(socket, "IDEN user1");
        waitAndRetrieveNextMessage(socket);

        socketSendMessage(socket, "STAT");
        String expectedMsg = "OK There are currently 1 user(s) on the server You are logged in and have sent 0 message(s)";
        String actualMsg = waitAndRetrieveNextMessage(socket);
        assertEquals(expectedMsg, actualMsg);
    }

    @Test
    public void stat_RegisteredUserOneMsgSent_ReturnsCorrectMsgWithCorrectMsgCount() {
        Socket socket = createNewConnection(serverPort);

        // Clear welcome and iden msg
        socketSendMessage(socket, "IDEN user1");
        socketSendMessage(socket, "HAIL TESTMSG");
        waitAndRetrieveNextMessage(socket);

        socketSendMessage(socket, "STAT");
        String expectedMsg = "OK There are currently 1 user(s) on the server You are logged in and have sent 1 message(s)";
        String actualMsg = waitAndRetrieveNextMessage(socket);
        assertEquals(expectedMsg, actualMsg);
    }

    @Test
    public void list_UnregisteredUser_ReturnsErrorMsg() {
        Socket socket = createNewConnection(serverPort);

        //Clear welcome msg
        waitAndRetrieveNextMessage(socket);

        socketSendMessage(socket, "LIST");
        String expectedMsg = "BAD You have not logged in yet";
        String actualMsg = waitAndRetrieveNextMessage(socket);
        assertEquals(expectedMsg, actualMsg);
    }

    @Test
    public void list_OneRegisteredUser_ReturnsOneUserInList() {
        Socket socket = createNewConnection(serverPort);

        // Clear welcome and iden msg
        String username = "user1";
        socketSendMessage(socket, "IDEN " + username);
        waitAndRetrieveNextMessage(socket);

        socketSendMessage(socket, "LIST");
        String expectedMsg = "OK " + username + ", ";
        String actualMsg = waitAndRetrieveNextMessage(socket);
        assertEquals(expectedMsg, actualMsg);
    }

    @Test
    public void list_MultipleRegisteredUser_ReturnsMultipleUserInList() {
        Socket socket1 = createNewConnection(serverPort);
        Socket socket2 = createNewConnection(serverPort);

        // Clear welcome and iden msg
        String username1 = "user1";
        String username2 = "user2";
        socketSendMessage(socket1, "IDEN " + username1);
        socketSendMessage(socket2, "IDEN " + username2);
        waitAndRetrieveNextMessage(socket1);
        waitAndRetrieveNextMessage(socket2);

        socketSendMessage(socket1, "LIST");
        socketSendMessage(socket2, "LIST");
        String expectedMsg = "OK " + username1 + ", " + username2 + ", ";
        String actualMsg = waitAndRetrieveNextMessage(socket1);
        assertEquals(expectedMsg, actualMsg);
        actualMsg = waitAndRetrieveNextMessage(socket2);
        assertEquals(expectedMsg, actualMsg);
    }

    @Test
    public void hail_UnregisteredUser_ReturnsErrorMsg() {
        Socket socket = createNewConnection(serverPort);

        // Clear welcome msg
        waitAndRetrieveNextMessage(socket);

        String msg = "Testing";
        socketSendMessage(socket, "HAIL " + msg);
        String expectedMsg = "BAD You have not logged in yet";
        String actualMsg = waitAndRetrieveNextMessage(socket);
        assertEquals(expectedMsg, actualMsg);
    }

    @Test
    public void hail_OneRegisteredUserSendOneHailMsg_ReturnsCorrectMsg() {
        Socket socket = createNewConnection(serverPort);

        // Clear welcome and iden msg
        String username = "user1";
        socketSendMessage(socket, "IDEN " + username);
        waitAndRetrieveNextMessage(socket);

        String msg = "Testing";
        socketSendMessage(socket, "HAIL " + msg);
        String expectedMsg = "Broadcast from " + username + ": " + msg;
        String actualMsg = waitAndRetrieveNextMessage(socket);
        assertEquals(expectedMsg, actualMsg);
    }

    @Test
    public void hail_OneRegisteredUserSendMultipleHailMsg_ReturnsCorrectMsg() {
        Socket socket = createNewConnection(serverPort);

        // Clear welcome and iden msg
        String username = "user1";
        socketSendMessage(socket, "IDEN " + username);
        waitAndRetrieveNextMessage(socket);

        String msg = "Testing";
        String msg2 = "Second Testing";
        socketSendMessage(socket, "HAIL " + msg);
        String expectedMsg = "Broadcast from " + username + ": " + msg;
        String actualMsg = waitAndRetrieveNextMessage(socket);
        assertEquals(expectedMsg, actualMsg);

        socketSendMessage(socket, "HAIL " + msg2);
        expectedMsg = "Broadcast from " + username + ": " + msg;
        actualMsg = waitAndRetrieveNextMessage(socket);
        assertEquals(expectedMsg, actualMsg);
    }


    @Test
    public void hail_MultipleRegisteredUserSendHailMsg_ReturnsCorrectMsg() {
        Socket socket1 = createNewConnection(serverPort);
        Socket socket2 = createNewConnection(serverPort);

        // Clear welcome and iden msg
        String username1 = "user1";
        String username2 = "user2";
        socketSendMessage(socket1, "IDEN " + username1);
        socketSendMessage(socket2, "IDEN " + username2);
        waitAndRetrieveNextMessage(socket1);
        waitAndRetrieveNextMessage(socket2);

        String msg = "Testing from sock1";
        String msg2 = "Testing from sock2";
        socketSendMessage(socket1, "HAIL " + msg);
        String expectedMsg = "Broadcast from " + username1 + ": " + msg;
        String actualMsgSock1 = waitAndRetrieveNextMessage(socket1);
        String actualMsgSock2 = waitAndRetrieveNextMessage(socket2);
        assertEquals(expectedMsg, actualMsgSock1);
        assertEquals(expectedMsg, actualMsgSock2);

        socketSendMessage(socket2, "HAIL " + msg2);
        expectedMsg = "Broadcast from " + username2 + ": " + msg2;
        actualMsgSock1 = waitAndRetrieveNextMessage(socket1);
        actualMsgSock2 = waitAndRetrieveNextMessage(socket2);
        assertEquals(expectedMsg, actualMsgSock1);
        assertEquals(expectedMsg, actualMsgSock2);
    }

    @Test
    public void mesg_UnregisteredUserSendPrivateMsg_ReturnsErrorMsg() {
        Socket socket1 = createNewConnection(serverPort);
        Socket socket2 = createNewConnection(serverPort);

        // Clear welcome and iden msg
        String targetUsername = "user1";
        socketSendMessage(socket1, "IDEN " + targetUsername);
        waitAndRetrieveNextMessage(socket1);
        waitAndRetrieveNextMessage(socket2);

        String msg = "Testing";
        socketSendMessage(socket2, "MESG " + targetUsername + " " + msg);
        String expectedMsg = "BAD You have not logged in yet";
        String actualMsg = waitAndRetrieveNextMessage(socket2);
        assertEquals(expectedMsg, actualMsg);
    }

    @Test
    public void mesg_RegisteredUserSendPrivateMsgToFakeUser_ReturnsErrorMsg() {
        Socket socket1 = createNewConnection(serverPort);
        Socket socket2 = createNewConnection(serverPort);

        // Clear welcome and iden msg
        String username = "user1";
        String fakeUsername = "fakeuser";
        socketSendMessage(socket1, "IDEN " + username);
        waitAndRetrieveNextMessage(socket1);
        waitAndRetrieveNextMessage(socket2);

        String msg = "Testing";
        socketSendMessage(socket1, "MESG " + fakeUsername + " " + msg);
        String expectedMsg = "BAD the user does not exist";
        String actualMsg = waitAndRetrieveNextMessage(socket1);
        assertEquals(expectedMsg, actualMsg);
    }

    @Test
    public void mesg_RegisteredUserSendBadFormatPrivateMsgToExistingUser_ReturnsErrorMsg() {
        Socket socket1 = createNewConnection(serverPort);
        Socket socket2 = createNewConnection(serverPort);

        // Clear welcome and iden msg
        String targetUsername = "user1";
        String senderUsername = "user2";
        socketSendMessage(socket1, "IDEN " + targetUsername);
        socketSendMessage(socket2, "IDEN " + senderUsername);
        waitAndRetrieveNextMessage(socket1);
        waitAndRetrieveNextMessage(socket2);

        String msg = "Testing";
        socketSendMessage(socket2, "MESG " + targetUsername + msg);
        String expectedSenderMsg = "BAD Your message is badly formatted";
        String actualSenderMsg = waitAndRetrieveNextMessage(socket2);
        assertEquals(expectedSenderMsg, actualSenderMsg);
    }

    @Test
    public void mesg_RegisteredUserSendPrivateMsgToExistingUser_ReturnsAndReceiveCorrectMsg() {
        Socket socket1 = createNewConnection(serverPort);
        Socket socket2 = createNewConnection(serverPort);

        // Clear welcome and iden msg
        String targetUsername = "user1";
        String senderUsername = "user2";
        socketSendMessage(socket1, "IDEN " + targetUsername);
        socketSendMessage(socket2, "IDEN " + senderUsername);
        waitAndRetrieveNextMessage(socket1);
        waitAndRetrieveNextMessage(socket2);

        String msg = "Testing";
        socketSendMessage(socket2, "MESG " + targetUsername + " " + msg);
        String expectedReceiverMsg = "PM from " + senderUsername + ":" + msg;
        String expectedSenderMsg = "OK your message has been sent";
        String actualReceiverMsg = waitAndRetrieveNextMessage(socket1);
        String actualSenderMsg = waitAndRetrieveNextMessage(socket2);
        assertEquals(expectedReceiverMsg, actualReceiverMsg);
        assertEquals(expectedSenderMsg, actualSenderMsg);
    }

    @Test
    public void quit_UnregisteredUserQuit_ReturnsCorrectMsg() {
        Socket socket = createNewConnection(serverPort);

        //Clear welcome msg
        waitAndRetrieveNextMessage(socket);

        socketSendMessage(socket, "QUIT");
        String expectedMsg = "OK goodbye";
        String actualMsg = waitAndRetrieveNextMessage(socket);
        assertEquals(expectedMsg, actualMsg);
    }

    @Test
    public void quit_RegisteredUserQuit_ReturnsCorrectMsg() {
        Socket socket = createNewConnection(serverPort);

        //Clear welcome msg
        String username = "user1";
        socketSendMessage(socket, "IDEN " + username);
        waitAndRetrieveNextMessage(socket);

        socketSendMessage(socket, "QUIT");
        String expectedMsg = "OK thank you for sending 0 message(s) with the chat service, goodbye. ";
        String actualMsg = waitAndRetrieveNextMessage(socket);
        assertEquals(expectedMsg, actualMsg);
    }


    @After
    public void cleanUp() {
        server.stopListening();
    }

    /**
     * Create a socket that connects to the given port
     *
     * @param port Port that you want to connect to
     * @return the created socket
     */
    private Socket createNewConnection(int port) {
        Socket socket = null;
        try {
            // crete new socket for test user
            socket = new Socket("localhost", port);
        } catch (IOException e) {
            Assert.fail("Fail to create mock user.");
        }

        // Update connection array
        try {
            Field field = Server.class.getDeclaredField("list");
            field.setAccessible(true);
            connectionList = (ArrayList<Connection>) field.get(server);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            System.err.println("Unable to access connection object.");
            e.printStackTrace();
        }
        sleep(500);
        return socket;
    }

    /**
     * Sends a given message through the given socket.
     *
     * @param socket Socket acting as a user that you want to send message with
     * @param msg    the string you want to send as message
     */
    private void socketSendMessage(Socket socket, String msg) {
        try {
            PrintWriter userOut = new PrintWriter(socket.getOutputStream(), true);
            userOut.println(msg);
            sleep(500);
        } catch (IOException e) {
            Assert.fail("Fail to send message");
        }
    }

    /**
     * Wait and Retrieve for the next message that is received
     *
     * @param socket The client socket that wants to receive the message
     * @return the message line received
     */
    private String waitAndRetrieveNextMessage(Socket socket) {
        String line = "";
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            line = reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return line;
    }

    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
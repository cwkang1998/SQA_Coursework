package chat.client.services;

import chat.client.mocks.MockMessageListener;
import chat.client.mocks.MockServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class ChatServiceTest {

    private static final String HOSTNAME = "localhost";
    private MockServer mockServer;
    private int serverPort;
    private Thread serverThread;
    private ChatService chatService;
    private MockMessageListener mockMessageListener;

    @Before
    public void setupMockServer() {
        mockServer = new MockServer(0);
        serverPort = mockServer.getServerPort();
        serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("Start Server Thread.");
                mockServer.listen();
            }
        });
        // Start the thread as daemon so it would stop when JVM stop
        serverThread.setDaemon(true);
        serverThread.start();

        // Let it sleep for 1 second to ensure thread executed
        sleep(1000);
        try {
            chatService = new ChatService(HOSTNAME, serverPort);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mockMessageListener = new MockMessageListener();
        chatService.registerMessageListener(mockMessageListener);
        sleep(500);
    }

    @Test
    public void registerUser_EmptyUsername_NoCommandSent() {
        chatService.registerUser("");
        sleep(500);
        String msg = mockServer.getReceivedMessage();
        assertEquals("", msg);
    }

    @Test
    public void registerUser_NonEmptyUsername_CommandSent() {
        String username = "user1";
        chatService.registerUser(username);
        sleep(500);
        String actualMsg = mockServer.getReceivedMessage();
        assertEquals("IDEN " + username, actualMsg);
    }

    @Test
    public void listAllUser_Invoked_CommandSent() {
        chatService.listAllUser();
        sleep(500);
        String actualMsg = mockServer.getReceivedMessage();
        assertEquals("LIST", actualMsg);
    }

    @Test
    public void getUserStat_Invoked_CommandSent() {
        chatService.getUserStat();
        sleep(500);
        String actualMsg = mockServer.getReceivedMessage();
        assertEquals("STAT", actualMsg);
    }

    @Test
    public void broadcastMsg_EmptyMsg_CommandSent() {
        chatService.broadcastMsg("");
        sleep(500);
        String actualMsg = mockServer.getReceivedMessage();
        assertEquals("HAIL ", actualMsg);
    }

    @Test
    public void broadcastMsg_NonEmptyMsg_CommandSent() {
        String msg = "Hello";
        chatService.broadcastMsg(msg);
        sleep(500);
        String actualMsg = mockServer.getReceivedMessage();
        assertEquals("HAIL " + msg, actualMsg);
    }

    @Test
    public void privateMsg_NonEmptyUsernameAndEmptyMsg_CommandSent() {
        String username = "user1";
        String msg = "";
        chatService.privateMsg(username, msg);
        sleep(500);
        String actualMsg = mockServer.getReceivedMessage();
        assertEquals("MESG " + username + " " + msg, actualMsg);
    }

    @Test
    public void privateMsg_EmptyUsernameAndNonEmptyMsg_CommandSent() {
        String username = "";
        String msg = "messages";
        chatService.privateMsg(username, msg);
        sleep(500);
        String actualMsg = mockServer.getReceivedMessage();
        assertEquals("MESG " + username + " " + msg, actualMsg);
    }

    @Test
    public void privateMsg_EmptyUsernameAndEmptyMsg_CommandSent() {
        String username = "";
        String msg = "";
        chatService.privateMsg(username, msg);
        sleep(500);
        String actualMsg = mockServer.getReceivedMessage();
        assertEquals("MESG " + username + " " + msg, actualMsg);
    }

    @Test
    public void privateMsg_NonEmptyUsernameAndNonEmptyMsg_CommandSent() {
        String username = "user1";
        String msg = "hello";
        chatService.privateMsg(username, msg);
        sleep(500);
        String actualMsg = mockServer.getReceivedMessage();
        assertEquals("MESG " + username + " " + msg, actualMsg);
    }

    @Test
    public void quit_Invoked_CommandSent() {
        chatService.quit();
        sleep(500);
        String actualMsg = mockServer.getReceivedMessage();
        assertEquals("QUIT", actualMsg);
    }

    @Test
    public void stopService_Invoked_QuitCommandSentAndIsAliveFalse() {
        assertTrue(chatService.isAlive());
        chatService.stopService();
        sleep(500);
        String actualMsg = mockServer.getReceivedMessage();
        assertEquals("QUIT", actualMsg);
        assertFalse(chatService.isAlive());
    }

    @Test
    public void run_UserBroadcastMessage_InvokeOnIncomingMessage() {
        String mockMessage = "Broadcast from user: mymessage";
        mockServer.sendMessage(mockMessage);
        sleep(500);
        assertEquals("onIncomingMessage", mockMessageListener.getLastInvokedMethodName());
        ServerMessage serverMessage = mockMessageListener.getLastReceivedMessage();
        assertEquals("user", serverMessage.getSourceUsername());
        assertEquals("mymessage", serverMessage.getMsg());
        assertEquals(ServerMessage.MessageType.BROADCAST, serverMessage.getType());
        assertEquals(ServerMessage.MessageStatus.OK, serverMessage.getStatus());
    }

    @Test
    public void run_UserPrivateMessage_InvokeOnIncomingMessage() {
        String mockMessage = "PM from user: mymessage";
        mockServer.sendMessage(mockMessage);
        sleep(500);
        assertEquals("onIncomingMessage", mockMessageListener.getLastInvokedMethodName());
        ServerMessage serverMessage = mockMessageListener.getLastReceivedMessage();
        assertEquals("user", serverMessage.getSourceUsername());
        assertEquals("mymessage", serverMessage.getMsg());
        assertEquals(ServerMessage.MessageType.PM, serverMessage.getType());
        assertEquals(ServerMessage.MessageStatus.OK, serverMessage.getStatus());
    }

    @Test
    public void run_ServerOKResponse_InvokeOnServerSuccessResponse() {
        String mockMessage = "OK MESG your message has been sent";
        mockServer.sendMessage(mockMessage);
        sleep(500);
        assertEquals("onServerSuccessResponse", mockMessageListener.getLastInvokedMethodName());
        ServerMessage serverMessage = mockMessageListener.getLastReceivedMessage();
        assertNull(serverMessage.getSourceUsername());
        assertEquals("your message has been sent", serverMessage.getMsg());
        assertEquals(ServerMessage.MessageType.MESG, serverMessage.getType());
        assertEquals(ServerMessage.MessageStatus.OK, serverMessage.getStatus());
    }

    @Test
    public void run_ServerBADResponse_InvokeOnServerErrorResponse() {
        String mockMessage = "BAD MESG the user does not exist";
        mockServer.sendMessage(mockMessage);
        sleep(500);
        assertEquals("onServerErrorResponse", mockMessageListener.getLastInvokedMethodName());
        ServerMessage serverMessage = mockMessageListener.getLastReceivedMessage();
        assertNull(serverMessage.getSourceUsername());
        assertEquals("the user does not exist", serverMessage.getMsg());
        assertEquals(ServerMessage.MessageType.MESG, serverMessage.getType());
        assertEquals(ServerMessage.MessageStatus.BAD, serverMessage.getStatus());
    }


    @After
    public void cleanUp() {
        mockServer.stopListening();
        chatService.unregisterMessageListener(mockMessageListener);
        chatService.stopService();
    }

    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

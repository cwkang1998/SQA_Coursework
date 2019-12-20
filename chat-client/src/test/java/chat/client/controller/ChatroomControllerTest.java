package chat.client.controller;

import chat.client.mocks.MockServer;
import chat.client.scene.SceneManager;
import chat.client.services.ChatService;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;

public class ChatroomControllerTest extends ApplicationTest {

    private static final String HOSTNAME = "localhost";
    private MockServer mockServer;
    private Stage primaryStage;
    private ChatService chatService;
    private SceneManager sceneManager;
    private int serverPort;
    private Thread serverThread;
    private String username;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        stage.show();
        stage.toFront();
        username = "user1";
    }

    @Before
    public void setUp() throws Exception {
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
        WaitForAsyncUtils.sleep(1000, TimeUnit.MILLISECONDS);
        chatService = new ChatService(HOSTNAME, serverPort);
        Semaphore semaphore = new Semaphore(0);
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                sceneManager = new SceneManager(primaryStage, chatService);

                // Simulate login to chatroom
                clickOn("#usernameInput");
                write(username);
                clickOn("#btnLogin");
                semaphore.release();
            }
        });
        semaphore.acquire();
        // Capture all events that are queued as of now.
        WaitForAsyncUtils.waitForFxEvents();
        WaitForAsyncUtils.sleep(1000, TimeUnit.MILLISECONDS);

        // Simulate server response
        mockServer.sendMessage("OK IDEN Welcome to the chat server " + username);
        WaitForAsyncUtils.waitForFxEvents();

        // Simulate user login, since user login took long enough in setup even
        Field field = ChatService.class.getDeclaredField("username");
        field.setAccessible(true);
        field.set(chatService, username);

        // Turn off the controller polling LIST, for isolated testing purposes
        field = SceneManager.class.getDeclaredField("controllers");
        field.setAccessible(true);
        ArrayList<BaseController> baseControllers = (ArrayList<BaseController>) field.get(sceneManager);
        ChatroomController controller = (ChatroomController) baseControllers.get(1);
        field = ChatroomController.class.getDeclaredField("isPolling");
        field.setAccessible(true);
        field.set(controller, false);

        WaitForAsyncUtils.waitForFxEvents();
        WaitForAsyncUtils.sleep(500, TimeUnit.MILLISECONDS);

        mockServer.sendMessage("OK LIST " + username + ", ");
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    public void inPublicChatroom_AfterLogin_CanSeeWelcomeMessageAndOnlineUserlist() {
        // Check UI for changes
        VBox chatBox = lookup("#vbox_public_chat").query();
        Label welcomeMsg = (Label) chatBox.getChildren().get(0);
        VBox contactBox = lookup("#chatContacts").query();
        ObservableList<Node> list = contactBox.getChildren();
        Hyperlink publicRoomName = (Hyperlink) list.get(0);

        assertEquals("Chatroom: Public Chatroom", primaryStage.getTitle());
        assertEquals("Welcome to the chat server " + username, welcomeMsg.getText());
        assertEquals(1, list.size());
        assertEquals("Public Chatroom", publicRoomName.getText());
    }

    @Test
    public void sendBtnClicked_InPublicChatroomWithNonEmptyMessage_CanSendMessage() {
        // Check UI for changes
        clickOn("#msgTextArea");
        write("Hello");
        clickOn("#btnSendMsg");
        WaitForAsyncUtils.waitForFxEvents();

        // Check from server if the command is sent
        String msg = mockServer.getReceivedMessage();
        assertEquals("HAIL Hello", msg);
        mockServer.sendMessage("Broadcast from " + username + ": " + "Hello");
        WaitForAsyncUtils.waitForFxEvents();

        VBox chatBox = lookup("#vbox_public_chat").query();
        Label broadcastMsg = (Label) chatBox.getChildren().get(1);
        assertEquals(username + ": " + "Hello", broadcastMsg.getText());
    }

    @Test
    public void sendBtnClicked_InPublicChatroomWithEmptyMessage_GetErrorMessage() {
        // Check UI for changes
        clickOn("#msgTextArea");
        write(" ");
        clickOn("#btnSendMsg");
        WaitForAsyncUtils.waitForFxEvents();

        VBox chatBox = lookup("#vbox_public_chat").query();
        Label errorMsg = (Label) chatBox.getChildren().get(1);
        assertEquals("message must not be empty", errorMsg.getText());
    }


    @Test
    public void enterKeyPressed_InPublicChatroomWithNonEmptyMessage_CanSendMessage() {
        // Check UI for changes
        clickOn("#msgTextArea");
        write("Hello");
        press(KeyCode.ENTER);
        WaitForAsyncUtils.waitForFxEvents();

        // Check from server if the command is sent
        String msg = mockServer.getReceivedMessage();
        assertEquals("HAIL Hello", msg);
        mockServer.sendMessage("Broadcast from " + username + ": " + "Hello");
        WaitForAsyncUtils.waitForFxEvents();

        VBox chatBox = lookup("#vbox_public_chat").query();
        Label broadcastMsg = (Label) chatBox.getChildren().get(1);
        assertEquals(username + ": " + "Hello", broadcastMsg.getText());
    }

    @Test
    public void enterKeyPressed_InPublicChatroomWithEmptyMessage_GetErrorMessage() {
        // Check UI for changes
        clickOn("#msgTextArea");
        write(" ");
        press(KeyCode.ENTER);
        WaitForAsyncUtils.waitForFxEvents();

        VBox chatBox = lookup("#vbox_public_chat").query();
        Label errorMsg = (Label) chatBox.getChildren().get(1);
        assertEquals("message must not be empty", errorMsg.getText());
    }

    @Test
    public void sendBtnClicked_InPrivateChatWithNonEmptyMessage_CanSendMessage() throws IOException {
        // Add new user via mocking in list
        String username2 = "user2";
        mockServer.sendMessage("OK LIST " + username + ", " + username2 + ", ");
        WaitForAsyncUtils.waitForFxEvents();

        // Switch to user2 chatroom
        VBox contactBox = lookup("#chatContacts").query();
        ObservableList<Node> list = contactBox.getChildren();
        Hyperlink user2Room = (Hyperlink) list.get(1);
        clickOn(user2Room);

        // Check UI for changes
        clickOn("#msgTextArea");
        write("Hello");
        clickOn("#btnSendMsg");
        WaitForAsyncUtils.waitForFxEvents();

        // Check from server if the command is sent
        String msg = mockServer.getReceivedMessage();
        assertEquals("MESG " + username2 + " Hello", msg);
        mockServer.sendMessage("OK MESG your message has been sent");
        WaitForAsyncUtils.waitForFxEvents();

        VBox chatBox = lookup("#vbox_" + username2).query();
        Label pmMsg = (Label) chatBox.getChildren().get(0);
        assertEquals(username + ": " + "Hello", pmMsg.getText());
    }

    @Test
    public void sendBtnClicked_InPrivateChatWithEmptyMessage_GetErrorMessage() {
        // Add new user via mocking in list
        String username2 = "user2";
        mockServer.sendMessage("OK LIST " + username + ", " + username2 + ", ");
        WaitForAsyncUtils.waitForFxEvents();

        // Switch to user2 chatroom
        VBox contactBox = lookup("#chatContacts").query();
        ObservableList<Node> list = contactBox.getChildren();
        Hyperlink user2Room = (Hyperlink) list.get(1);
        clickOn(user2Room);

        // Check UI for changes
        clickOn("#msgTextArea");
        write(" ");
        clickOn("#btnSendMsg");
        WaitForAsyncUtils.waitForFxEvents();

        VBox chatBox = lookup("#vbox_" + username2).query();
        Label errorMsg = (Label) chatBox.getChildren().get(0);
        assertEquals("message must not be empty", errorMsg.getText());
    }


    @Test
    public void enterKeyPressed_InPrivateChatWithNonEmptyMessage_CanSendMessage() {
        // Add new user via mocking in list
        String username2 = "user2";
        mockServer.sendMessage("OK LIST " + username + ", " + username2 + ", ");
        WaitForAsyncUtils.waitForFxEvents();

        // Switch to user2 chatroom
        VBox contactBox = lookup("#chatContacts").query();
        ObservableList<Node> list = contactBox.getChildren();
        Hyperlink user2Room = (Hyperlink) list.get(1);
        clickOn(user2Room);

        // Check UI for changes
        clickOn("#msgTextArea");
        write("Hello");
        press(KeyCode.ENTER);
        WaitForAsyncUtils.waitForFxEvents();

        // Check from server if the command is sent
        String msg = mockServer.getReceivedMessage();
        assertEquals("MESG " + username2 + " Hello", msg);
        mockServer.sendMessage("OK MESG your message has been sent");
        WaitForAsyncUtils.waitForFxEvents();

        VBox chatBox = lookup("#vbox_" + username2).query();
        Label broadcastMsg = (Label) chatBox.getChildren().get(0);
        assertEquals(username + ": " + "Hello", broadcastMsg.getText());
    }

    @Test
    public void enterKeyPressed_InPrivateChatWithEmptyMessage_GetErrorMessage() {
        // Add new user via mocking in list
        String username2 = "user2";
        mockServer.sendMessage("OK LIST " + username + ", " + username2 + ", ");
        WaitForAsyncUtils.waitForFxEvents();

        // Switch to user2 chatroom
        VBox contactBox = lookup("#chatContacts").query();
        ObservableList<Node> list = contactBox.getChildren();
        Hyperlink user2Room = (Hyperlink) list.get(1);
        clickOn(user2Room);

        // Check UI for changes
        clickOn("#msgTextArea");
        write(" ");
        press(KeyCode.ENTER);
        WaitForAsyncUtils.waitForFxEvents();

        VBox chatBox = lookup("#vbox_" + username2).query();
        Label errorMsg = (Label) chatBox.getChildren().get(0);
        assertEquals("message must not be empty", errorMsg.getText());
    }

    @Test
    public void inPublicChatroom_WhenReceiveBroadcastMsg_PublicChatIsUpdated() {
        // Add new user via mocking in list
        String username2 = "user2";
        mockServer.sendMessage("OK LIST " + username + ", " + username2 + ", ");
        WaitForAsyncUtils.waitForFxEvents();

        // Simulate broadcast from user2
        String message = "Hello Broadcast!";
        mockServer.sendMessage("Broadcast from " + username2 + ": " + message);
        WaitForAsyncUtils.waitForFxEvents();

        // Check UI for changes
        // Public chat
        VBox publicChatBox = lookup("#vbox_public_chat").query();
        ObservableList<Node> publicChatLabels = publicChatBox.getChildren();
        Label broadcastMsg = (Label) publicChatLabels.get(1);
        assertEquals(2, publicChatLabels.size());
        assertEquals(username2 + ": " + message, broadcastMsg.getText());

        //Go to private chatroom
        VBox contactBox = lookup("#chatContacts").query();
        ObservableList<Node> list = contactBox.getChildren();
        Hyperlink user2Room = (Hyperlink) list.get(1);
        clickOn(user2Room);

        //Private Chat
        VBox privateChatBox = lookup("#vbox_" + username2).query();
        ObservableList<Node> privateChatLabels = privateChatBox.getChildren();
        assertEquals(0, privateChatLabels.size());
    }

    @Test
    public void inPublicChatroom_WhenReceivePrivateMsg_PrivateChatIsUpdated() {
        // Add new user via mocking in list
        String username2 = "user2";
        mockServer.sendMessage("OK LIST " + username + ", " + username2 + ", ");
        WaitForAsyncUtils.waitForFxEvents();

        // Simulate PM from user2
        String message = "Hello PM!";
        mockServer.sendMessage("PM from " + username2 + ": " + message);
        WaitForAsyncUtils.waitForFxEvents();

        // Check UI for changes
        // Public chat
        VBox publicChatBox = lookup("#vbox_public_chat").query();
        ObservableList<Node> publicChatLabels = publicChatBox.getChildren();
        assertEquals(1, publicChatLabels.size());

        //Go to private chatroom
        VBox contactBox = lookup("#chatContacts").query();
        ObservableList<Node> list = contactBox.getChildren();
        Hyperlink user2Room = (Hyperlink) list.get(1);
        clickOn(user2Room);

        //Private Chat
        VBox privateChatBox = lookup("#vbox_" + username2).query();
        ObservableList<Node> privateChatLabels = privateChatBox.getChildren();
        Label privateMsg = (Label) privateChatLabels.get(0);
        assertEquals(1, privateChatLabels.size());
        assertEquals(username2 + ": " + message, privateMsg.getText());
    }

    @Test
    public void inPrivateChatroom_WhenReceiveBroadcastMsg_PublicChatIsUpdated() {
        // Add new user via mocking in list
        String username2 = "user2";
        mockServer.sendMessage("OK LIST " + username + ", " + username2 + ", ");
        WaitForAsyncUtils.waitForFxEvents();

        //Go to private chatroom
        VBox contactBox = lookup("#chatContacts").query();
        ObservableList<Node> list = contactBox.getChildren();
        Hyperlink user2Room = (Hyperlink) list.get(1);
        clickOn(user2Room);

        // Simulate broadcast from user2
        String message = "Hello Broadcast!";
        mockServer.sendMessage("Broadcast from " + username2 + ": " + message);
        WaitForAsyncUtils.waitForFxEvents();

        // Check UI for changes
        //Private Chat
        VBox privateChatBox = lookup("#vbox_" + username2).query();
        ObservableList<Node> privateChatLabels = privateChatBox.getChildren();
        assertEquals(0, privateChatLabels.size());

        //Go to public chatroom
        Hyperlink publicRoom = (Hyperlink) list.get(0);
        clickOn(publicRoom);

        // Public chat
        VBox publicChatBox = lookup("#vbox_public_chat").query();
        ObservableList<Node> publicChatLabels = publicChatBox.getChildren();
        Label broadcastMsg = (Label) publicChatLabels.get(1);
        assertEquals(2, publicChatLabels.size());
        assertEquals(username2 + ": " + message, broadcastMsg.getText());
    }

    @Test
    public void inPrivateChatroom_WhenReceivePrivateMsg_PrivateChatIsUpdated() {
        // Add new user via mocking in list
        String username2 = "user2";
        mockServer.sendMessage("OK LIST " + username + ", " + username2 + ", ");
        WaitForAsyncUtils.waitForFxEvents();

        //Go to private chatroom
        VBox contactBox = lookup("#chatContacts").query();
        ObservableList<Node> list = contactBox.getChildren();
        Hyperlink user2Room = (Hyperlink) list.get(1);
        clickOn(user2Room);

        // Simulate broadcast from user2
        String message = "Hello PM!";
        mockServer.sendMessage("PM from " + username2 + ": " + message);
        WaitForAsyncUtils.waitForFxEvents();

        // Check UI for changes
        //Private Chat
        VBox privateChatBox = lookup("#vbox_" + username2).query();
        ObservableList<Node> privateChatLabels = privateChatBox.getChildren();
        Label privateMsg = (Label) privateChatLabels.get(0);
        assertEquals(1, privateChatLabels.size());
        assertEquals(username2 + ": " + message, privateMsg.getText());

        //Go to public chatroom
        Hyperlink publicRoom = (Hyperlink) list.get(0);
        clickOn(publicRoom);

        // Public chat
        VBox publicChatBox = lookup("#vbox_public_chat").query();
        ObservableList<Node> publicChatLabels = publicChatBox.getChildren();
        assertEquals(1, publicChatLabels.size());
    }


    @After
    public void tearDown() throws TimeoutException {
        FxToolkit.hideStage();
        chatService.stopService();
        mockServer.stopListening();
    }
}

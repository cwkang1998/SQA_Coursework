package chat.client.controller;

import chat.client.Client;
import chat.client.mocks.MockServer;
import chat.client.scene.SceneManager;
import chat.client.services.ChatService;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.testfx.api.FxAssert;
import org.testfx.api.FxRobot;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.matcher.control.LabeledMatchers;
import org.testfx.util.WaitForAsyncUtils;

import java.awt.*;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;

public class LoginControllerTest extends ApplicationTest {

    private static final String HOSTNAME = "localhost";
    private MockServer mockServer;
    private Stage primaryStage;
    private ChatService chatService;
    private SceneManager sceneManager;
    private int serverPort;
    private Thread serverThread;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        stage.show();
        stage.toFront();
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
        sleep(1000);
        chatService = new ChatService(HOSTNAME, serverPort);
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                sceneManager = new SceneManager(primaryStage, chatService);
                sceneManager.navigateToScene("Login");
            }
        });
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    public void errorMsgLabel_InitialLogin_IsNotVisible() {
        Label errLabel = lookup("#msgErrorLogin").query();
        assertFalse(errLabel.isVisible());
    }

    @Test
    public void loginBtnClicked_EmptyUsername_ShowErrorMessage() {
        clickOn("#usernameInput");
        write(" ");
        clickOn("#btnLogin");
        FxAssert.verifyThat("#msgErrorLogin", LabeledMatchers.hasText("Username cannot be empty."));
        FxAssert.verifyThat("#msgErrorLogin", Node::isVisible);
    }

    @Test
    public void loginBtnClicked_ValidUsername_NavigateToNewPage() {
        clickOn("#usernameInput");
        write("user1");
        clickOn("#btnLogin");

        sleep(500);
        String sentMsg = mockServer.getReceivedMessage();
        assertEquals("IDEN user1", sentMsg);

        mockServer.sendMessage("OK IDEN Welcome to the chat server user1");
        sleep(500);
        assertEquals("Chatroom", primaryStage.getTitle());
    }

    @After
    public void tearDown() throws TimeoutException {
        FxToolkit.hideStage();
        chatService.stopService();
        mockServer.stopListening();
    }

    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

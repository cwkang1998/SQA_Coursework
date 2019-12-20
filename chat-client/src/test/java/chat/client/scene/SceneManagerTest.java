package chat.client.scene;

import chat.client.mocks.MockServer;
import chat.client.services.ChatService;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;

public class SceneManagerTest extends ApplicationTest {

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
        primaryStage.show();
        primaryStage.toFront();
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
            }
        });
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    public void navigateToScene_LoginScene_Successful() {
        sceneManager.navigateToScene("Login");
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals("Login", primaryStage.getTitle());
    }

    @Test
    public void navigateToScene_ChatroomScene_Successful() {
        sceneManager.navigateToScene("Chatroom");
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals("Chatroom", primaryStage.getTitle());
    }

    @Test
    public void navigateToScene_NonExistentScene_Fails() {
        sceneManager.navigateToScene("Nope");
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals("Login", primaryStage.getTitle());
    }

    @Test
    public void setChatroomSceneTitleDescp_SetInLoginScene_Fails() {
        sceneManager.navigateToScene("Login");
        WaitForAsyncUtils.waitForFxEvents();
        sceneManager.setChatroomSceneTitleDescp("Change");
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals("Login", primaryStage.getTitle());
    }

    @Test
    public void setChatroomSceneTitleDescp_SetInChatroomScene_TitleChange() {
        sceneManager.navigateToScene("Chatroom");
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals("Chatroom", primaryStage.getTitle());

        sceneManager.setChatroomSceneTitleDescp("Change");
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals("Chatroom: Change", primaryStage.getTitle());
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

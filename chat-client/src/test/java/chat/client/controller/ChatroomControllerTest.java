package chat.client.controller;

import chat.client.Client;
import chat.client.mocks.MockServer;
import chat.client.scene.SceneManager;
import chat.client.services.ChatService;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.After;
import org.junit.Before;
import org.testfx.api.FxToolkit;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.TimeoutException;

public class ChatroomControllerTest extends ApplicationTest {

    private static final String HOSTNAME = "localhost";
    private MockServer mockServer;
    private Stage primaryStage;
    private BaseController controller;
    private ChatService chatService;
    private SceneManager sceneManager;
    private int serverPort;
    private Thread serverThread;

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(Client.class.getResource("/chat/client/Chatroom.fxml"));
        Parent mainNode = loader.load();
        controller = loader.getController();
        primaryStage = stage;
        stage.setScene(new Scene(mainNode));
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
        Thread.sleep(1000);
        chatService = new ChatService(HOSTNAME, serverPort);
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                sceneManager = new SceneManager(primaryStage, chatService);
                controller.registerChatService(chatService);
                controller.registerSceneManager(sceneManager);
            }
        });
        WaitForAsyncUtils.waitForFxEvents();
    }

    @After
    public void tearDown() throws TimeoutException {
        FxToolkit.hideStage();
        controller.unregisterSceneManager();
        controller.unregisterChatService();
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

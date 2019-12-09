package chat.client;

import chat.client.scene.SceneManager;
import chat.client.services.ChatService;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.Group;

public class Client extends Application {

    private SceneManager sceneManager;
    private ChatService chatService;

    @Override
    public void start(Stage primaryStage) throws Exception {
        chatService = new ChatService("localhost", 9000);
        sceneManager = new SceneManager(primaryStage, chatService);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
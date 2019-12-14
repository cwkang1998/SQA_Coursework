package chat.client;

import chat.client.scene.SceneManager;
import chat.client.services.ChatService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import java.io.IOException;

public class Client extends Application {

    private SceneManager sceneManager;
    private ChatService chatService;

    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            chatService = new ChatService("localhost", 9000);
            sceneManager = new SceneManager(primaryStage, chatService);
            primaryStage.setResizable(false);
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Cannot Connect To Server.", ButtonType.OK);
                    alert.getDialogPane().setPrefHeight(Region.USE_PREF_SIZE);
                    alert.show();
                }
            });
        }
    }

    @Override
    public void stop() throws Exception {
        chatService.stopService();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
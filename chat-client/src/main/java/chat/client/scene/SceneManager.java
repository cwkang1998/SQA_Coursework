package chat.client.scene;

import chat.client.services.ChatService;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;

import java.io.IOException;
import java.util.ArrayList;

public class SceneManager {
    private ChatService chatService;
    private ArrayList<Scene> scenes;

    public SceneManager(ChatService chatService) {
        this.chatService = chatService;
        this.scenes = new ArrayList<>();
        initializeScene();
    }

    public void initializeScene() {
    }
}

package chat.client.scene;

import chat.client.controller.BaseController;
import chat.client.services.ChatService;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;

public class SceneManager {
    private Stage primaryStage;
    private ChatService chatService;
    private ArrayList<Scene> scenes;
    private ArrayList<BaseController> controllers;
    private String defaultSceneName;
    private String currentSceneName;

    public SceneManager(Stage stage, ChatService chatService) {
        this.primaryStage = stage;
        this.chatService = chatService;
        this.scenes = new ArrayList<>();
        this.controllers = new ArrayList<>();
        initialize();
        this.defaultSceneName = "Login";
        this.currentSceneName = this.defaultSceneName;
        primaryStage.setTitle(defaultSceneName);
        primaryStage.setScene(getSceneByName(defaultSceneName));
    }

    private void initialize() {
        FXMLLoader loginLoader = new FXMLLoader(getClass().getResource("/chat/client/Login.fxml"));
        FXMLLoader ChatroomLoader = new FXMLLoader(getClass().getResource("/chat/client/Chatroom.fxml"));

        // Initialize Scenes
        try {
            scenes.add(new Scene(loginLoader.load()));
            scenes.add(new Scene(ChatroomLoader.load()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Initialize Controller
        BaseController loginController = loginLoader.getController();
        BaseController chatroomController = ChatroomLoader.getController();
        controllers.add(loginController);
        controllers.add(chatroomController);

        for (BaseController controller : controllers) {
            controller.registerSceneManager(this);
            controller.registerChatService(chatService);
        }
    }

    /**
     * Gets a initialized scene by its name from the scene array list
     *
     * @param sceneName Name of the scenes that you want to switch to
     * @return The scene object corresponding to the sceneName provided,
     * if the corresponding scene object does not exists, return null instead
     */
    private Scene getSceneByName(String sceneName) {
        switch (sceneName) {
            case "Login":
                return scenes.get(0);
            case "Chatroom":
                return scenes.get(1);
        }
        return null;
    }

    public void navigateToScene(String sceneName) {
        Scene scene = getSceneByName(sceneName);
        currentSceneName = sceneName;
        if (scene != null) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    primaryStage.setTitle(sceneName);
                    primaryStage.setScene(scene);
                }
            });
        }
    }

    public void setChatroomSceneTitleDescp(String titleDescp) {
        if (currentSceneName.equals("Chatroom")) {
            if (titleDescp == null) {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        primaryStage.setTitle(currentSceneName);
                    }
                });

            } else {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        primaryStage.setTitle(currentSceneName + ": " + titleDescp);
                    }
                });
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        for (BaseController controller : controllers) {
            controller.unregisterChatService();
            controller.unregisterSceneManager();
        }
    }
}

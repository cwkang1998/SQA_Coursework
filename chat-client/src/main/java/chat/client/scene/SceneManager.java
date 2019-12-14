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
    private ArrayList<Stage> openedWindows;
    private String defaultSceneName;
    private String currentSceneName;

    public SceneManager(Stage stage, ChatService chatService) {
        this.primaryStage = stage;
        this.chatService = chatService;
        this.scenes = new ArrayList<>();
        this.controllers = new ArrayList<>();
        this.openedWindows = new ArrayList<>();
        initialize();
        this.defaultSceneName = "Login";
        this.currentSceneName = this.defaultSceneName;
        primaryStage.setTitle(defaultSceneName);
        primaryStage.setScene(this.getDefaultScene());
    }

    private void initialize() {
        FXMLLoader loginLoader = new FXMLLoader(getClass().getResource("/chat/client/Login.fxml"));
        FXMLLoader mainChatLoader = new FXMLLoader(getClass().getResource("/chat/client/MainChat.fxml"));
        FXMLLoader privateChatLoader = new FXMLLoader(getClass().getResource("/chat/client/PrivateChat.fxml"));

        // Initialize Scenes
        try {
            scenes.add(new Scene(loginLoader.load()));
            scenes.add(new Scene(mainChatLoader.load()));
            scenes.add(new Scene(privateChatLoader.load()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Initialize Controller
        BaseController loginController = loginLoader.getController();
        BaseController mainChatController = mainChatLoader.getController();
        BaseController privateChatController = privateChatLoader.getController();
        controllers.add(loginController);
        controllers.add(mainChatController);
        controllers.add(privateChatController);

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
            case "MainChat":
                return scenes.get(1);
            case "PrivateChat":
                return scenes.get(2);
        }
        return null;
    }

    public void navigateToScene(String sceneName) {
        Scene scene = getSceneByName(sceneName);
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

    public void startNewWindow(String sceneName, String title) {
        Stage stage = new Stage();
        Scene scene = getSceneByName(sceneName);
        if (scene != null) {
            stage.setScene(scene);
            stage.setTitle(currentSceneName);
            openedWindows.add(stage);
            stage.show();
        }
    }

    private Scene getDefaultScene() {
        return getSceneByName(defaultSceneName);
    }

    public void setDefaultScene(String defaultSceneName) {
        this.defaultSceneName = defaultSceneName;
    }

    public String getCurrentSceneName() {
        return currentSceneName;
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

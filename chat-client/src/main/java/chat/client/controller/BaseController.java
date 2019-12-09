package chat.client.controller;

import chat.client.scene.SceneManager;
import chat.client.services.ChatService;

/**
 * Base Controller
 * Consists of methods to register ChatService and SceneManager
 */
public class BaseController {
    protected ChatService chatService;
    protected SceneManager manager;

    public void registerChatService(ChatService chatService) {
        this.chatService = chatService;
    }

    public void unregisterChatService() {
        this.chatService = null;
    }

    public void registerSceneManager(SceneManager manager) {
        this.manager = manager;
    }

    public void unregisterSceneManager() {
        this.manager = null;
    }
}

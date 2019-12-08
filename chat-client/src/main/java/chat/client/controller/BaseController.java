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

    protected void registerChatService(ChatService chatService) {
        this.chatService = chatService;
    }

    protected void unregisterChatService() {
        this.chatService = null;
    }

    protected void registerSceneManager(SceneManager manager) {
        this.manager = manager;
    }

    protected void unregisterSceneManager() {
        this.manager = null;
    }
}

package chat.client.controller;

import chat.client.scene.SceneManager;
import chat.client.services.ChatService;
import chat.client.services.ServerMessage;
import chat.client.services.ServerMessageListener;

/**
 * Base Controller
 * Consists of methods to register ChatService and SceneManager
 */
public class BaseController implements ServerMessageListener {
    protected ChatService chatService;
    protected SceneManager manager;

    public void registerChatService(ChatService chatService) {
        this.chatService = chatService;
        this.chatService.registerMessageListener(this);
    }

    public void unregisterChatService() {
        this.chatService.unregisterMessageListener(this);
        this.chatService = null;
    }

    public void registerSceneManager(SceneManager manager) {
        this.manager = manager;
    }

    public void unregisterSceneManager() {
        this.manager = null;
    }

    @Override
    public void onServerSuccessResponse(ServerMessage message) {
    }

    @Override
    public void onServerErrorResponse(ServerMessage message) {
    }

    @Override
    public void onIncomingMessage(ServerMessage message) {
    }
}

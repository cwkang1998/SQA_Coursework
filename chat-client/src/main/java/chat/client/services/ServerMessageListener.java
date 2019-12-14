package chat.client.services;

public interface ServerMessageListener {
    void onServerSuccessResponse(ServerMessage message);

    void onServerErrorResponse(ServerMessage message);

    void onIncomingMessage(ServerMessage message);
}

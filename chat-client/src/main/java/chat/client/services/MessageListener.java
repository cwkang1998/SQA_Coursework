package chat.client.services;

public interface MessageListener {
    void onServerSuccessResponse(String msg);

    void onServerErrorResponse(String msg);

    void onBroadcastResponse(String source, String msg);

    void onPMResponse(String source, String msg);
}

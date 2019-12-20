package chat.client.mocks;

import chat.client.services.ServerMessage;
import chat.client.services.ServerMessageListener;

public class MockMessageListener implements ServerMessageListener {

    private ServerMessage lastReceivedMessage;
    private String lastInvokeMethodName;

    public MockMessageListener() {
        lastReceivedMessage = null;
        lastInvokeMethodName = "";
    }

    @Override
    public void onServerSuccessResponse(ServerMessage message) {
        lastReceivedMessage = message;
        lastInvokeMethodName = "onServerSuccessResponse";
    }

    @Override
    public void onServerErrorResponse(ServerMessage message) {
        lastReceivedMessage = message;
        lastInvokeMethodName = "onServerErrorResponse";
    }

    @Override
    public void onIncomingMessage(ServerMessage message) {
        lastReceivedMessage = message;
        lastInvokeMethodName = "onIncomingMessage";
    }

    public ServerMessage getLastReceivedMessage() {
        ServerMessage msg = lastReceivedMessage;
        lastReceivedMessage = null;
        return msg;
    }

    public String getLastInvokedMethodName(){
        String method = lastInvokeMethodName;
        lastInvokeMethodName = "";
        return method;
    }
}

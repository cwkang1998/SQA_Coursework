package chat.client.services;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ServerMessageTest {

    @Test
    public void ServerMessage_InvalidMessageFormat_HaveInvalidStatus() {
        String rawMsg = "WeirdBadlyFormatted Message";
        ServerMessage serverMessage = new ServerMessage(rawMsg);
        assertEquals(serverMessage.getStatus(), ServerMessage.MessageStatus.INVALID);
    }

    @Test
    public void ServerMessage_InvalidMessageStatus_HaveInvalidStatus() {
        String rawMsg = "STATUS CONNECT Message";
        ServerMessage serverMessage = new ServerMessage(rawMsg);
        assertEquals(serverMessage.getStatus(), ServerMessage.MessageStatus.INVALID);
    }

    @Test
    public void ServerMessage_InvalidMessageType_HaveInvalidStatus() {
        String rawMsg = "OK WEIRD Message";
        ServerMessage serverMessage = new ServerMessage(rawMsg);
        assertEquals(serverMessage.getStatus(), ServerMessage.MessageStatus.INVALID);
    }

    @Test
    public void ServerMessage_EmptyMessage_HaveInvalidStatus() {
        String rawMsg = "OK WEIRD   ";
        ServerMessage serverMessage = new ServerMessage(rawMsg);
        assertEquals(serverMessage.getStatus(), ServerMessage.MessageStatus.INVALID);
    }

    @Test
    public void ServerMessage_CorrectServerMessageFormat_IsCorrectlyParsed() {
        String rawMsg = "OK CONNECT Some Message";
        ServerMessage serverMessage = new ServerMessage(rawMsg);
        assertEquals(serverMessage.getStatus(), ServerMessage.MessageStatus.OK);
        assertEquals(serverMessage.getType(), ServerMessage.MessageType.CONNECT);
        assertNull(serverMessage.getSourceUsername());
        assertEquals(serverMessage.getMsg(), "Some Message");
    }

    @Test
    public void ServerMessage_CorrectUserMessageFormat_IsCorrectlyParsed() {
        String rawMsg = "Broadcast from someuser: Hello";
        ServerMessage serverMessage = new ServerMessage(rawMsg);
        assertEquals(serverMessage.getStatus(), ServerMessage.MessageStatus.OK);
        assertEquals(serverMessage.getType(), ServerMessage.MessageType.BROADCAST);
        assertEquals(serverMessage.getSourceUsername(), "someuser");
        assertEquals(serverMessage.getMsg(), "Hello");
    }

}

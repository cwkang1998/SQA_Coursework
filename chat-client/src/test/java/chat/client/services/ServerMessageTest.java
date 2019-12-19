package chat.client.services;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ServerMessageTest {

    @Test
    public void ServerMessage_BadMessageFormat_HaveBadStatus() {
        String rawMsg = "WeirdBadlyFormatted Message";
        ServerMessage serverMessage = new ServerMessage(rawMsg);
        assertEquals(ServerMessage.MessageStatus.BAD, serverMessage.getStatus());
        assertEquals(ServerMessage.MessageType.VALD, serverMessage.getType());
        assertNull(serverMessage.getSourceUsername());
        assertEquals("parsing error due to invalid message format.", serverMessage.getMsg());
    }

    @Test
    public void ServerMessage_BadMessageStatus_HaveBadStatus() {
        String rawMsg = "STATUS CONNECT Message";
        ServerMessage serverMessage = new ServerMessage(rawMsg);
        assertEquals(ServerMessage.MessageStatus.BAD, serverMessage.getStatus());
        assertEquals(ServerMessage.MessageType.VALD, serverMessage.getType());
        assertNull(serverMessage.getSourceUsername());
        assertEquals("parsing error due to invalid message format.", serverMessage.getMsg());
    }

    @Test
    public void ServerMessage_BadMessageType_HaveBadStatus() {
        String rawMsg = "OK WEIRD Message";
        ServerMessage serverMessage = new ServerMessage(rawMsg);
        assertEquals(ServerMessage.MessageStatus.BAD, serverMessage.getStatus());
        assertEquals(ServerMessage.MessageType.VALD, serverMessage.getType());
        assertNull(serverMessage.getSourceUsername());
        assertEquals("parsing error due to invalid message format.", serverMessage.getMsg());
    }

    @Test
    public void ServerMessage_EmptyMessage_HaveBadStatus() {
        String rawMsg = "OK WEIRD   ";
        ServerMessage serverMessage = new ServerMessage(rawMsg);
        assertEquals(ServerMessage.MessageStatus.BAD, serverMessage.getStatus());
        assertEquals(ServerMessage.MessageType.VALD, serverMessage.getType());
        assertNull(serverMessage.getSourceUsername());
        assertEquals("parsing error due to invalid message format.", serverMessage.getMsg());
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

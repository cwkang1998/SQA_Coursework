package chat.client.services;

public class ServerMessage {

    public enum MessageStatus {
        OK("OK"), BAD("BAD"), INVALID("INVALID");

        MessageStatus(String status) {
        }
    }

    public enum MessageType {
        // Request response
        CONNECT("CONNECT"), VALD("VALD"), IDEN("IDEN"), HAIL("HAIL"), MESG("MESG"),
        LIST("LIST"), STAT("STAT"), QUIT("QUIT"),

        // User message response
        BROADCAST("BROADCAST"), PM("PM");

        MessageType(String type) {
        }
    }

    private MessageStatus status;
    private MessageType type;
    private String msg;
    private String sourceUsername;


    /**
     * The constructor takes in a raw server message and check for its type,
     * the raw message would then be parsed according to its evaluated type,
     * adding information to each message to allow for easier evaluation.
     *
     * @param rawMsg The raw message from the server
     */
    public ServerMessage(String rawMsg) {
        // Check for the first three letters, if they consists of the main two status code,
        // They are of a server type message, otherwise user messages.
        String checkString = rawMsg.substring(0, 3);
        checkString = checkString.trim();
        try {
            if (checkString.equals(MessageStatus.OK.toString()) || checkString.equals(MessageStatus.BAD.toString())) {
                String[] parsed = parseServerMessage(rawMsg);
                this.status = MessageStatus.valueOf(parsed[0]);
                this.type = MessageType.valueOf(parsed[1]);
                this.sourceUsername = null;
                this.msg = parsed[2];
            } else {
                String[] parsed = parseUserMessage(rawMsg);
                this.status = MessageStatus.OK;
                this.type = MessageType.valueOf(parsed[0].toUpperCase());
                this.sourceUsername = parsed[1];
                this.msg = parsed[2];
            }
        } catch (IndexOutOfBoundsException | IllegalArgumentException e) {
            this.status = MessageStatus.INVALID;
        }
    }

    /**
     * Parse the message from the server and split them into the status of the message
     * and the actual message itself.
     *
     * @param rawMsg The raw message returned from the server.
     * @return A string array, containing the status of the message (OK or BAD) at index 0,
     * and the actual message that comes along with the message at index 1
     */
    private String[] parseServerMessage(String rawMsg) {
        rawMsg = rawMsg.trim();
        return rawMsg.split(" ", 3);
    }

    /**
     * Parse the message from the server sent by other users and split them into suitable parts
     * containing information regarding the message
     *
     * @param rawMsg The raw message returned from the server.
     * @return A string array, containing the type of the message(broadcast, pm) at index 0,
     * source of the message(username) at index 1, and the actual message at index 2
     */
    private String[] parseUserMessage(String rawMsg) {
        String[] userMsgData = new String[3];
        rawMsg = rawMsg.trim();
        String[] userAndMsg = rawMsg.split(":", 2);
        String[] msgTypeAndUser = userAndMsg[0].split(" ", 3);
        userMsgData[0] = msgTypeAndUser[0];
        userMsgData[1] = msgTypeAndUser[2];
        userMsgData[2] = userAndMsg[1].trim();
        return userMsgData;
    }

    public MessageStatus getStatus() {
        return status;
    }

    public String getMsg() {
        return msg;
    }

    public String getSourceUsername() {
        return sourceUsername;
    }

    public MessageType getType() {
        return type;
    }

}

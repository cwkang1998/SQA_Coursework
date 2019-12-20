package chat.client.controller;

import chat.client.services.ChatService;
import chat.client.services.ServerMessage;
import chat.client.services.ServerMessage.MessageType;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;


public class ChatroomController extends BaseController {

    @FXML
    private ScrollPane chatScroll;

    @FXML
    private VBox chatContacts;

    @FXML
    private Button btnSendMsg;

    @FXML
    private TextArea msgTextArea;

    private HashMap<String, VBox> chatRoomContent;
    private boolean isPolling;
    private Thread pollingThread;
    private final String PUBLIC_CHAT_NAME = "Public Chatroom";
    private String currentChatroom;

    @Override
    public void registerChatService(ChatService chatService) {
        super.registerChatService(chatService);
        currentChatroom = PUBLIC_CHAT_NAME;
        chatRoomContent = new HashMap<>(2);
        VBox publicChatBox = new VBox();
        publicChatBox.setId("vbox_public_chat");
        publicChatBox.setPadding(new Insets(12, 12, 12, 12));
        chatRoomContent.put(PUBLIC_CHAT_NAME, publicChatBox);
        chatScroll.setContent(chatRoomContent.get(PUBLIC_CHAT_NAME));
    }

    public void initialize() {
        btnSendMsg.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                handleSendMessage();
            }
        });
        msgTextArea.setWrapText(true);
        msgTextArea.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (event.getCode().equals(KeyCode.ENTER)) {
                    handleSendMessage();
                    event.consume();
                }
            }
        });
    }

    private void handleSendMessage() {
        String msg = msgTextArea.getText().trim();
        if (!msg.isEmpty()) {
            if (currentChatroom.equals(PUBLIC_CHAT_NAME)) {
                chatService.broadcastMsg(msg);
            } else {
                chatService.privateMsg(currentChatroom, msg);
                addChatMessages(currentChatroom, chatService.getUsername() + ": " + msg, false);
            }

        } else {
            addChatMessages(currentChatroom, "message must not be empty", true);
        }
        msgTextArea.clear();
    }

    private void addChatMessages(String user, String msg, boolean error) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                Label newMsg = new Label();
                newMsg.setText(msg);
                if (error) {
                    newMsg.setTextFill(Color.web("#FF3232"));
                }
                VBox chatBox = chatRoomContent.get(user);
                chatBox.getChildren().add(newMsg);
                chatBox.heightProperty().addListener(new InvalidationListener() {
                    @Override
                    public void invalidated(Observable observable) {
                        if (currentChatroom.equals(user)) {
                            chatScroll.setVvalue(1.0);
                        }
                    }
                });
            }
        });
    }

    private void pollOnlineUsers() {
        pollingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (isPolling) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    chatService.listAllUser();
                }
            }
        });
        pollingThread.setDaemon(true);
        pollingThread.start();
    }

    private synchronized void populateOnlineUsers(ArrayList<String> users) {
        if (users.size() + 1 != chatRoomContent.size()) {
            for (String currentUser : chatRoomContent.keySet()) {
                if (!users.contains(currentUser) && !currentUser.equals(PUBLIC_CHAT_NAME)) {
                    chatRoomContent.remove(currentUser);
                }
            }
        }
        for (String user : users) {
            if (!user.isEmpty()) {
                VBox chatBox = new VBox();
                chatBox.setId("vbox_" + user);
                chatBox.setPadding(new Insets(12, 12, 12, 12));
                chatRoomContent.putIfAbsent(user, chatBox);
            }
        }
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                //First load all chatrooms for all users
                chatContacts.getChildren().clear();
                for (String user : users) {
                    if (!user.isEmpty()) {
                        Hyperlink userLink = new Hyperlink(user);
                        userLink.setVisited(false);
                        userLink.setOnAction(new EventHandler<ActionEvent>() {
                            @Override
                            public void handle(ActionEvent event) {
                                currentChatroom = user;
                                userLink.setVisited(true);
                                chatScroll.setContent(chatRoomContent.get(user));
                                chatScroll.setVvalue(1.0);
                                populateOnlineUsers(users);
                            }
                        });
                        if (currentChatroom.equals(user)) {
                            userLink.setVisited(true);
                        }
                        chatContacts.getChildren().add(userLink);
                    }
                }
                chatContacts.getChildren().sorted();

                // Now onto the public chat room
                Hyperlink publicRoomLink = new Hyperlink(PUBLIC_CHAT_NAME);
                publicRoomLink.setVisited(false);
                publicRoomLink.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        currentChatroom = PUBLIC_CHAT_NAME;
                        publicRoomLink.setVisited(true);
                        chatScroll.setContent(chatRoomContent.get(PUBLIC_CHAT_NAME));
                        chatScroll.setVvalue(1.0);
                        populateOnlineUsers(users);
                    }
                });
                if (currentChatroom.equals(PUBLIC_CHAT_NAME)) {
                    publicRoomLink.setVisited(true);
                }
                chatContacts.getChildren().add(0, publicRoomLink);

                // Set the title desc to the chatroom you are in
                manager.setChatroomSceneTitleDescp(currentChatroom);
            }
        });
    }

    @Override
    public void onServerSuccessResponse(ServerMessage message) {
        if (message.getType().equals(MessageType.IDEN)) {
            addChatMessages(PUBLIC_CHAT_NAME, message.getMsg(), false);
            this.isPolling = true;
            pollOnlineUsers();
        } else if (message.getType().equals(MessageType.LIST)) {
            String[] users = message.getMsg().split(",");
            for (int i = 0; i < users.length; i++) {
                users[i] = users[i].trim();
            }
            ArrayList<String> usersList = new ArrayList<String>(Arrays.asList(users));
            usersList.remove(chatService.getUsername());
            populateOnlineUsers(usersList);
        } else if (message.getType().equals(MessageType.QUIT)) {
            addChatMessages(currentChatroom, message.getMsg(), false);
            synchronized (this) {
                this.isPolling = false;
            }
        }
    }

    @Override
    public void onServerErrorResponse(ServerMessage message) {
        if (!message.getType().equals(MessageType.IDEN)) {
            addChatMessages(currentChatroom, message.getMsg(), true);
        }
    }

    @Override
    public void onIncomingMessage(ServerMessage message) {
        if (message.getType().equals(MessageType.BROADCAST)) {
            addChatMessages(PUBLIC_CHAT_NAME, message.getSourceUsername() + ": " + message.getMsg(), false);
        } else if (message.getType().equals(MessageType.PM)) {
            addChatMessages(message.getSourceUsername(), message.getSourceUsername() + ": " + message.getMsg(), false);
        }
    }
}

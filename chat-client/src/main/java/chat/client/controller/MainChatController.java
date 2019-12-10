package chat.client.controller;

import chat.client.services.MessageListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

import java.util.ArrayList;


public class MainChatController extends BaseController {

    @FXML
    private VBox groupChatList;

    @FXML
    private VBox groupChatContacts;

    @FXML
    private Button btnSendMsg;

    @FXML
    private TextArea msgTextArea;

    private MessageListener listener;

    public void initialize() {
        btnSendMsg.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                String msg = msgTextArea.getText();
                chatService.broadcastMsg(msg);
            }
        });
    }

    private void addChatMessages(String user, String msg) {
        Label newMsg = new Label();
        newMsg.setText(user + ": " + msg);
        groupChatList.getChildren().add(newMsg);
    }

    private void populateOnlineUsers(ArrayList<String> users) {
        groupChatContacts.getChildren().clear();
        for (String user : users) {
            Label label = new Label(user);
            groupChatContacts.getChildren().add(label);
        }
        groupChatContacts.getChildren().sorted();
    }


}

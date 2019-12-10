package chat.client.controller;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;


public class PrivateChatController extends BaseController {
    @FXML
    private VBox privateChatList;

    @FXML
    private Button btnSendMsg;

    @FXML
    private TextArea msgTextArea;

    @FXML
    private String targetUsername;

    public void initialize() {
        btnSendMsg.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                String msg = msgTextArea.getText();
                chatService.privateMsg(targetUsername, msg);
            }
        });
    }

    private void addChatMessages(String user, String msg) {
        Label newMsg = new Label();
        newMsg.setText(user + ": " + msg);
        privateChatList.getChildren().add(newMsg);
    }

}

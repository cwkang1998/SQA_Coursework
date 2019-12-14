package chat.client.controller;

import chat.client.services.ServerMessage;
import chat.client.services.ServerMessage.MessageType;
import com.sun.prism.paint.Paint;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;


public class LoginController extends BaseController {
    @FXML
    private TextField usernameInput;

    @FXML
    private Label msgErrorLogin;

    @FXML
    private Button btnLogin;

    @FXML
    private Button btnCancel;

    public void initialize() {
        msgErrorLogin.setTextFill(Color.web("#FF0000"));
        msgErrorLogin.setVisible(false);
        btnLogin.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                msgErrorLogin.setVisible(false);
                String inputString = usernameInput.getText();
                inputString = inputString.trim();
                if (!inputString.isEmpty()) {
                    chatService.registerUser(inputString);
                } else {
                    msgErrorLogin.setText("Username cannot be empty.");
                    msgErrorLogin.setVisible(true);
                }
                usernameInput.setText("");
            }
        });

        btnCancel.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                Platform.exit();
            }
        });
    }

    @Override
    public void onServerSuccessResponse(ServerMessage message) {
        if (message.getType().equals(MessageType.IDEN)) {
            this.manager.navigateToScene("MainChat");
        }
    }

    @Override
    public void onServerErrorResponse(ServerMessage message) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                if (message.getType().equals(MessageType.IDEN)) {
                    msgErrorLogin.setText(Character.toUpperCase(message.getMsg().charAt(0)) + message.getMsg().substring(1));
                    msgErrorLogin.setVisible(true);
                } else if (message.getType().equals(MessageType.VALD)) {
                    msgErrorLogin.setText(Character.toUpperCase(message.getMsg().charAt(0)) + message.getMsg().substring(1));
                    msgErrorLogin.setVisible(true);
                }
            }
        });
    }
}



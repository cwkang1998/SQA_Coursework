package chat.client.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;


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
        msgErrorLogin.setVisible(false);
        btnLogin.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                String inputString = usernameInput.getText();
                chatService.registerUser(inputString);
            }
        });
        btnCancel.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                Platform.exit();
            }
        });
    }
}



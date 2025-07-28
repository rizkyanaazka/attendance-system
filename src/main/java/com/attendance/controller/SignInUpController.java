package com.attendance.controller;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class SignInUpController implements Initializable {

    @FXML
    private VBox vbox;
    private Parent fxml;


    @Override
    public void initialize(URL url, ResourceBundle rb) {
        TranslateTransition t = new TranslateTransition(Duration.seconds(0.5), vbox);
        t.setToX(0);
        t.play();
        t.setOnFinished(e -> loadFXML("fxml/SignUp.fxml"));
    }

    private void loadFXML(String path) {
        try {
            System.out.println("Loading FXML: " + path);
            fxml = FXMLLoader.load(getClass().getResource("/" + path));
            vbox.getChildren().setAll(fxml);
        } catch (IOException ex) {
            System.err.println("Failed to load: " + path);
            ex.printStackTrace();
        }
    }


    @FXML
    private void open_signin(ActionEvent event) {
        TranslateTransition t = new TranslateTransition(Duration.seconds(0.5), vbox);
        t.setToX(300);
        t.play();
        t.setOnFinished(e -> loadFXML("fxml/SignIn.fxml"));
    }

    @FXML
    private void open_signup(ActionEvent event) {
        TranslateTransition t = new TranslateTransition(Duration.seconds(0.5), vbox);
        t.setToX(0);
        t.play();
        t.setOnFinished(e -> loadFXML("fxml/SignUp.fxml"));
    }
}

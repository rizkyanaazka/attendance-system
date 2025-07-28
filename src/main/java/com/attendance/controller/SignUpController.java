package com.attendance.controller;

import com.attendance.DatabaseManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class SignUpController implements Initializable {

    public TextField emailField;
    @FXML private TextField nameField;
    @FXML private TextField passwordField;
    @FXML private Button signUpButton;
    @FXML private Label messageLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        messageLabel.setText("");
    }

    @FXML
    private void handleSignUp(ActionEvent e) {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String pw = passwordField.getText().trim();

        if (name.isEmpty() || email.isEmpty() || pw.isEmpty()) {
            showMessage("Semua field harus diisi!", false);
            return;
        }

        // Menonaktifkan tombol untuk mencegah klik ganda
        signUpButton.setDisable(true);
        signUpButton.setText("Mendaftarkan...");

        // Membuat dan menjalankan proses di background thread
        new Thread(() -> {
            try (Connection c = DatabaseManager.getConnection()) {
                PreparedStatement ps = c.prepareStatement("""
                INSERT INTO users (name, email, password, role, nim, class_name, subject, profile_image)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """);
                ps.setString(1, name);
                ps.setString(2, email);
                ps.setString(3, pw);
                ps.setString(4, "student");
                ps.setString(5, DatabaseManager.generateNim());
                ps.setString(6, "");  // Kolom class_name
                ps.setString(7, "-"); // Kolom subject
                ps.setBytes(8, null); // Kolom profile_image
                ps.executeUpdate();

                // Update UI setelah berhasil, kembali ke JavaFX Thread
                Platform.runLater(() -> {
                    showMessage("Pendaftaran berhasil!", true);
                    clearFields();
                    signUpButton.setDisable(false);
                    signUpButton.setText("Sign Up");
                });

            } catch (SQLException ex) {
                // Update UI jika gagal, kembali ke JavaFX Thread
                Platform.runLater(() -> {
                    showMessage("Terjadi kesalahan saat mendaftar!", false);
                    signUpButton.setDisable(false);
                    signUpButton.setText("Sign Up");
                });
                ex.printStackTrace(); // untuk debugging
            }
        }).start();
    }

    private void clearFields() {
        nameField.clear();
        emailField.clear();
        passwordField.clear();
    }

    private void showMessage(String message, boolean success) {
        messageLabel.setText(message);
        messageLabel.setStyle(success ? "-fx-text-fill: green;" : "-fx-text-fill: red;");

        new Thread(() -> {
            try {
                Thread.sleep(5000);
                Platform.runLater(() -> {
                    if (messageLabel.getText().equals(message)) {
                        messageLabel.setText("");
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

}


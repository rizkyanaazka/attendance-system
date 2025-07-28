package com.attendance.controller;

import com.attendance.DatabaseManager;
import com.attendance.model.User;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.awt.event.KeyEvent;
import java.net.URL;
import java.util.ResourceBundle;

public class SignInController implements Initializable {

    public TextField passwordField;
    public TextField nimField;
    public Button loginButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("LoginController initialized");
    }

    public void handleLogin() {
        String nim = nimField.getText().trim();
        String password = passwordField.getText();

        if (nim.isEmpty() || password.isEmpty()) {
            alert("NIM dan password harus diisi.");
            return;
        }

        loginButton.setDisable(true);
        loginButton.setText("Login...");

        new Thread(() -> {
            try {
                User user = DatabaseManager.authenticateUser(nim, password);

                Platform.runLater(() -> {
                    Stage stage = (Stage) loginButton.getScene().getWindow();

                    if (user != null) {
                        // Validasi siswa yang belum di aprove
                        if (user.getRole().equalsIgnoreCase("student") &&
                                (user.getStatus() == null || !user.getStatus().equalsIgnoreCase("approved"))) {
                            alert("Akun Anda belum disetujui oleh admin.");
                            loginButton.setDisable(false);
                            loginButton.setText("Login");
                            return;
                        }

                        try {
                            FXMLLoader loader;
                            Parent root;

                            switch (user.getRole().toLowerCase()) {
                                case "student" -> {
                                    loader = new FXMLLoader(getClass().getResource("/fxml/student_dashboard.fxml"));
                                    root = loader.load();
                                    StudentDashboardController controller = loader.getController();
                                    controller.setCurrentUser(user);
                                    showScene(stage, root, "Dashboard - " + user.getName());
                                }
                                case "admin" -> {
                                    loader = new FXMLLoader(getClass().getResource("/fxml/admin_dashboard.fxml"));
                                    root = loader.load();
                                    AdminDashboardController controller = loader.getController();
                                    controller.setCurrentUser(user);
                                    showScene(stage, root, "Dashboard Admin - " + user.getName());
                                }
                                case "tentor" -> {
                                    loader = new FXMLLoader(getClass().getResource("/fxml/tentor_dashboard.fxml"));
                                    root = loader.load();
                                    TentorDashboardController controller = loader.getController();
                                    controller.setCurrentUser(user);
                                    showScene(stage, root, "Dashboard Tentor - " + user.getName());
                                }
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                            alert("Gagal memuat dashboard.");
                        }

                    } else {
                        alert("NIM atau password salah.");
                    }

                    loginButton.setDisable(false);
                    loginButton.setText("Login");
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    alert("Terjadi kesalahan saat login.");
                    loginButton.setDisable(false);
                    loginButton.setText("Login");
                });
            }
        }).start();
    }

    private void showScene(Stage stage, Parent root, String title) {
        stage.setTitle(title);
        stage.setScene(new Scene(root, 900, 700));
        stage.centerOnScreen();
    }

    private void alert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Login");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleKeyPressed(javafx.scene.input.KeyEvent event) {
        if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
            handleLogin();
        }
    }
}

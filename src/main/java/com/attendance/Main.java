package com.attendance;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import javafx.stage.StageStyle;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            System.out.println("Testing database connection...");
            if (!DatabaseManager.testConnection()) {
                showDatabaseError();
                return;
            }

            System.out.println("Initializing database...");
            DatabaseManager.initializeDatabase();

            System.out.println("Loading login interface...");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SignInUp.fxml")); //anda bisa merubah menjadi Login.fxml
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            primaryStage.initStyle(StageStyle.TRANSPARENT);

            primaryStage.setTitle("Sistem Absensi Siswa - Login");
            primaryStage.setScene(scene); //Scene(root)
            primaryStage.setResizable(false);
            //primaryStage.sizeToScene(); //Menyesuaikan ukuran dari FXML
            primaryStage.show();

            System.out.println("Application started successfully!");

        } catch (Exception e) {
            System.err.println("Error starting application: " + e.getMessage());
            e.printStackTrace();
            showApplicationError(e);
        }
    }

    private void showDatabaseError() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Database Error");
        alert.setHeaderText("Tidak dapat terhubung ke database MySQL");
        alert.setContentText("Pastikan MySQL server berjalan dan konfigurasi database sudah benar.\n\n" +
                "Host: localhost:3306\n" +
                "Database: attendance_db\n" +
                "User: root\n" +
                "Password: (kosong)");
        alert.showAndWait();
        System.exit(1);
    }

    private void showApplicationError(Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Application Error");
        alert.setHeaderText("Terjadi kesalahan saat memulai aplikasi");
        alert.setContentText("Error: " + e.getMessage());
        alert.showAndWait();
        System.exit(1);
    }

    public static void main(String[] args) {
        System.out.println("Starting Student Attendance System...");
        launch(args);
    }
}

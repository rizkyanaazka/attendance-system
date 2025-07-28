package com.attendance;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.Objects;

public class SceneManager {

    public static void handleLogout(Stage stage) {
        try {
            Parent loginRoot = FXMLLoader.load(Objects.requireNonNull(SceneManager.class.getResource("/fxml/SignInUp.fxml")));
            Scene loginScene = new Scene(loginRoot);

            // Ganti scene pada stage yang ada
            stage.setScene(loginScene);
            stage.sizeToScene();
            stage.centerOnScreen();
            loginScene.setFill(Color.TRANSPARENT);
            stage.show();

        } catch (IOException e) {
            System.err.println("Gagal memuat halaman login!");
            e.printStackTrace();
        }
    }
}
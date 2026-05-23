package com.outline.client;

import com.outline.client.network.ApiClient;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class OutlineClientApplication extends Application {
    private final ApiClient apiClient = new ApiClient(System.getProperty("outline.server", "http://localhost:8080"));

    @Override
    public void start(Stage stage) throws Exception {
        showLogin(stage);
    }

    public void showLogin(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/outline/client/auth/login.fxml"));
        loader.setControllerFactory(type -> new com.outline.client.auth.LoginController(apiClient, stage, this));
        Scene scene = new Scene(loader.load(), 1180, 760);
        scene.getStylesheets().add(getClass().getResource("/styles/outline.css").toExternalForm());
        stage.setTitle("Outline Next");
        stage.setMinWidth(980);
        stage.setMinHeight(680);
        stage.setScene(scene);
        stage.setOnShown(event -> System.out.println("Outline Next login window opened."));
        stage.show();
        stage.centerOnScreen();
        stage.toFront();
        stage.requestFocus();
    }

    public void showMain(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/outline/client/ui/main.fxml"));
        loader.setControllerFactory(type -> new com.outline.client.ui.MainController(apiClient, stage, this));
        Scene scene = new Scene(loader.load(), 1360, 860);
        scene.getStylesheets().add(getClass().getResource("/styles/outline.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

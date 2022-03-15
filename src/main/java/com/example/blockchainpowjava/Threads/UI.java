package com.example.blockchainpowjava.Threads;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class UI extends Application {

    @Override
    public void start(Stage stage)  throws IOException  {

            FXMLLoader fxmlLoader = new FXMLLoader(UI.class.getResource("MainWindow.fxml"));

            Scene scene = new Scene(fxmlLoader.load(), 900, 700);
            stage.setTitle("Blockchain Viewer : E-Coin");
            stage.setScene(scene);
            stage.show();

    }
}
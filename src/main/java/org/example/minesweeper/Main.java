package org.example.minesweeper;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {
    private final int rows = 30, cols = 30, bombs = 100;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("page.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), Page.boxSize * cols, Page.boxSize * (rows + 2) + 20);
        Page ctrl = fxmlLoader.getController();
        ctrl.build(rows, cols, bombs);
        stage.setTitle("Minesweeper");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
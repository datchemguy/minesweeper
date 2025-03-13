module org.example.minesweeper {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;


    opens org.example.minesweeper to javafx.fxml;
    exports org.example.minesweeper;
}
package app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Login.fxml"));
        Scene scene = new Scene(loader.load());
        scene.getStylesheets().add(
                getClass().getResource("/css/style.css").toExternalForm()
        );

        primaryStage.setTitle("InventoryFX");
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);

        // 👉 plein écran
        primaryStage.setMaximized(true);

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

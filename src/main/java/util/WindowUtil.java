package util;

import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class WindowUtil {

    public static void switchScene(Stage stage, Scene scene) {
        stage.setScene(scene);

        // 👉 Récupérer la taille de l'écran utilisable
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();

        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());

        // En plus, on dit au stage d'être maximisé
        stage.setMaximized(true);

        stage.centerOnScreen();
    }
}

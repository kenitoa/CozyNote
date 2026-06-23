package com.cozynote;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import com.cozynote.app.AppCommand;
import com.cozynote.ui.shell.MainController;

public class CozyNoteApp extends Application {
    private static final double DEFAULT_START_WIDTH = 1568;
    private static final double DEFAULT_START_HEIGHT = 880;
    private static final double MIN_START_WIDTH = 1180;
    private static final double MIN_START_HEIGHT = 760;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cozynote/views/main.fxml"));
        BorderPane root = loader.load();
        MainController controller = loader.getController();
        Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
        double startWidth = Math.min(DEFAULT_START_WIDTH, visualBounds.getWidth());
        double startHeight = Math.min(DEFAULT_START_HEIGHT, visualBounds.getHeight());
        Scene scene = new Scene(root, startWidth, startHeight);
        installShortcuts(scene, controller);

        stage.setTitle("CozyNote");
        stage.setMinWidth(Math.min(MIN_START_WIDTH, visualBounds.getWidth()));
        stage.setMinHeight(Math.min(MIN_START_HEIGHT, visualBounds.getHeight()));
        stage.setX(visualBounds.getMinX());
        stage.setY(visualBounds.getMinY());
        stage.setWidth(startWidth);
        stage.setHeight(startHeight);
        stage.setScene(scene);
        controller.applyRuntimeSettings();
        controller.fitInitialLayoutToWidth(startWidth);
        stage.setOnCloseRequest(event -> controller.shutdown());
        if (visualBounds.getWidth() <= DEFAULT_START_WIDTH || visualBounds.getHeight() <= DEFAULT_START_HEIGHT) {
            stage.setMaximized(true);
        }
        stage.show();
    }

    private void installShortcuts(Scene scene, MainController controller) {
        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (new KeyCodeCombination(KeyCode.B, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN).match(event)) {
                rootStatus(scene, "Ctrl+Shift+B: 목록 블록");
                event.consume();
            } else if (new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN).match(event)) {
                rootStatus(scene, "Ctrl+Shift+C: 체크리스트 블록");
                event.consume();
            } else if (new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN).match(event)) {
                controller.executeCommand(AppCommand.NEW_NOTE);
                event.consume();
            } else if (new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN).match(event)) {
                controller.executeCommand(AppCommand.SAVE_NOTE);
                event.consume();
            } else if (new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN).match(event)) {
                controller.executeCommand(AppCommand.UNDO);
                event.consume();
            } else if (new KeyCodeCombination(KeyCode.Y, KeyCombination.CONTROL_DOWN).match(event)) {
                controller.executeCommand(AppCommand.REDO);
                event.consume();
            } else if (new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN).match(event)
                    || new KeyCodeCombination(KeyCode.X, KeyCombination.CONTROL_DOWN).match(event)
                    || new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN).match(event)
                    || new KeyCodeCombination(KeyCode.V, KeyCombination.CONTROL_DOWN).match(event)
                    || new KeyCodeCombination(KeyCode.EQUALS, KeyCombination.CONTROL_DOWN).match(event)
                    || new KeyCodeCombination(KeyCode.PLUS, KeyCombination.CONTROL_DOWN).match(event)
                    || new KeyCodeCombination(KeyCode.MINUS, KeyCombination.CONTROL_DOWN).match(event)
                    || new KeyCodeCombination(KeyCode.SUBTRACT, KeyCombination.CONTROL_DOWN).match(event)) {
                event.consume();
            } else if (new KeyCodeCombination(KeyCode.B, KeyCombination.CONTROL_DOWN).match(event)) {
                rootStatus(scene, "Ctrl+B: 굵게");
                event.consume();
            } else if (new KeyCodeCombination(KeyCode.I, KeyCombination.CONTROL_DOWN).match(event)) {
                rootStatus(scene, "Ctrl+I: 기울임");
                event.consume();
            } else if (new KeyCodeCombination(KeyCode.U, KeyCombination.CONTROL_DOWN).match(event)) {
                rootStatus(scene, "Ctrl+U: 밑줄");
                event.consume();
            } else if (new KeyCodeCombination(KeyCode.DIGIT1, KeyCombination.CONTROL_DOWN).match(event)) {
                rootStatus(scene, "Ctrl+1: 제목 블록");
                event.consume();
            } else if (new KeyCodeCombination(KeyCode.ESCAPE).match(event)) {
                rootStatus(scene, "Esc: 포커스 닫기");
                event.consume();
            }
        });
    }

    private void rootStatus(Scene scene, String text) {
        javafx.scene.Node node = scene.lookup("#statusLabel");
        if (node instanceof javafx.scene.control.Label label) {
            label.setText(text);
        }
    }
}

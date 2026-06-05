package com.cozynote.ui.settings;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.cozynote.app.AppState;
import com.cozynote.service.SettingsService;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;

public final class SettingsController {
    @FXML private CheckBox autoSaveCheckBox;
    @FXML private CheckBox reopenLastMemoCheckBox;
    @FXML private CheckBox showSidebarCheckBox;
    @FXML private CheckBox showRightPanelCheckBox;
    @FXML private CheckBox autoPlayMusicCheckBox;
    @FXML private ChoiceBox<String> themeChoiceBox;
    @FXML private ChoiceBox<String> fontSizeChoiceBox;
    @FXML private Button exportBackupButton;
    @FXML private Button importBackupButton;
    @FXML private Button openDataFolderButton;

    private AppState appState;
    private SettingsService settingsService;
    private Runnable exportBackupAction;
    private Runnable importBackupAction;
    private Runnable applySettingsAction;

    @FXML
    private void initialize() {
        themeChoiceBox.getItems().setAll("밝은 모드", "어두운 모드");
        fontSizeChoiceBox.getItems().setAll("작게", "보통", "크게");
        exportBackupButton.setOnAction(event -> {
            if (exportBackupAction != null) {
                exportBackupAction.run();
            }
        });
        importBackupButton.setOnAction(event -> {
            if (importBackupAction != null) {
                importBackupAction.run();
            }
        });
        openDataFolderButton.setOnAction(event -> showDataFolder());
        applyFallbackValues();
    }

    public void setContext(AppState appState, SettingsService settingsService,
            Runnable exportBackupAction, Runnable importBackupAction, Runnable applySettingsAction) {
        this.appState = appState;
        this.settingsService = settingsService;
        this.exportBackupAction = exportBackupAction;
        this.importBackupAction = importBackupAction;
        this.applySettingsAction = applySettingsAction;
        loadSettings();
    }

    @FXML
    private void saveSettings(ActionEvent event) {
        if (settingsService != null) {
            settingsService.setBoolean("editor.autoSave", autoSaveCheckBox.isSelected());
            settingsService.setBoolean("app.reopenLastMemo", reopenLastMemoCheckBox.isSelected());
            settingsService.setBoolean("layout.leftSidebarVisible", showSidebarCheckBox.isSelected());
            settingsService.setBoolean("layout.rightPanelVisible", showRightPanelCheckBox.isSelected());
            settingsService.setBoolean("music.autoPlay", autoPlayMusicCheckBox.isSelected());
            settingsService.setString("theme.name", themeChoiceBox.getValue());
            settingsService.setString("editor.fontScale", fontSizeChoiceBox.getValue());
        }
        if (appState != null) {
            appState.leftSidebarVisibleProperty().set(showSidebarCheckBox.isSelected());
            appState.rightPanelVisibleProperty().set(showRightPanelCheckBox.isSelected());
        }
        if (applySettingsAction != null) {
            applySettingsAction.run();
        }
        closeWindow(event);
    }

    @FXML
    private void closeWindow(ActionEvent event) {
        ((Node) event.getSource()).getScene().getWindow().hide();
    }

    private void loadSettings() {
        if (settingsService == null) {
            applyFallbackValues();
            return;
        }
        autoSaveCheckBox.setSelected(settingsService.getBoolean("editor.autoSave", true));
        reopenLastMemoCheckBox.setSelected(settingsService.getBoolean("app.reopenLastMemo", true));
        showSidebarCheckBox.setSelected(settingsService.getBoolean("layout.leftSidebarVisible", true));
        showRightPanelCheckBox.setSelected(settingsService.getBoolean("layout.rightPanelVisible", true));
        autoPlayMusicCheckBox.setSelected(settingsService.getBoolean("music.autoPlay", false));
        themeChoiceBox.setValue(settingsService.getString("theme.name", "밝은 모드"));
        fontSizeChoiceBox.setValue(settingsService.getString("editor.fontScale", "보통"));
    }

    private void applyFallbackValues() {
        autoSaveCheckBox.setSelected(true);
        reopenLastMemoCheckBox.setSelected(true);
        showSidebarCheckBox.setSelected(true);
        showRightPanelCheckBox.setSelected(true);
        autoPlayMusicCheckBox.setSelected(false);
        themeChoiceBox.setValue("밝은 모드");
        fontSizeChoiceBox.setValue("보통");
    }

    private void showDataFolder() {
        Path dataFolder = Path.of(System.getProperty("user.home"), ".cozynote");
        try {
            Files.createDirectories(dataFolder);
            Desktop.getDesktop().open(dataFolder.toFile());
        } catch (IOException | RuntimeException exception) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("저장 위치");
            alert.setHeaderText(null);
            alert.setContentText(dataFolder.toString());
            alert.showAndWait();
        }
    }
}

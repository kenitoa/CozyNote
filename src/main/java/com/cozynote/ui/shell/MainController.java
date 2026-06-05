package com.cozynote.ui.shell;

import java.io.IOException;
import java.util.List;

import com.cozynote.app.AppCommand;
import com.cozynote.app.AppContext;
import com.cozynote.app.AppState;
import com.cozynote.app.CommandRegistry;
import com.cozynote.domain.Note;
import com.cozynote.service.BackupService;
import com.cozynote.service.NoteService;
import com.cozynote.service.SettingsService;
import com.cozynote.ui.editor.EditorController;
import com.cozynote.ui.settings.SettingsController;
import com.cozynote.ui.sidebar.SidebarController;
import com.cozynote.ui.widgets.MusicPlayerController;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

public final class MainController {
    @FXML private MenuItem toggleLeftSidebarMenuItem;
    @FXML private MenuItem toggleRightPanelMenuItem;
    @FXML private MenuItem settingsMenuItem;
    @FXML private MenuItem helpMenuItem;
    @FXML private MenuItem exportMenuItem;
    @FXML private MenuItem importMenuItem;
    @FXML private MenuItem closeMenuItem;
    @FXML private Label statusLabel;
    @FXML private Button leftPanelToggleButton;
    @FXML private Button rightPanelToggleButton;
    @FXML private HBox appBody;
    @FXML private Region sidebarPane;
    @FXML private Region editorPane;
    @FXML private Region rightPanelPane;
    @FXML private SidebarController sidebarPaneController;
    @FXML private EditorController editorPaneController;
    @FXML private MusicPlayerController rightPanelPaneController;

    private final AppContext context = new AppContext();
    private final BackupService backupService = context.backupService();
    private final NoteService noteService = context.noteService();
    private final SettingsService settingsService = context.settingsService();
    private final CommandRegistry commands = context.commands();
    private final AppState appState = context.state();
    private static final double SIDE_PANEL_WIDTH = 420;
    private static final double EDITOR_MIN_WIDTH = 760;

    @FXML
    private void initialize() {
        statusLabel.setText("CozyNote 준비됨");
        loadPersistedSettings();
        registerCommands();
        toggleLeftSidebarMenuItem.setOnAction(event -> commands.execute(AppCommand.TOGGLE_LEFT_SIDEBAR));
        toggleRightPanelMenuItem.setOnAction(event -> commands.execute(AppCommand.TOGGLE_RIGHT_PANEL));
        settingsMenuItem.setOnAction(event -> commands.execute(AppCommand.OPEN_SETTINGS));
        helpMenuItem.setOnAction(event -> showHelp());
        exportMenuItem.setOnAction(event -> commands.execute(AppCommand.EXPORT_BACKUP));
        importMenuItem.setOnAction(event -> commands.execute(AppCommand.IMPORT_BACKUP));
        closeMenuItem.setOnAction(event -> commands.execute(AppCommand.EXIT));
        leftPanelToggleButton.setOnAction(event -> commands.execute(AppCommand.TOGGLE_LEFT_SIDEBAR));
        rightPanelToggleButton.setOnAction(event -> commands.execute(AppCommand.TOGGLE_RIGHT_PANEL));
        sidebarPaneController.setNewMemoCommand(() -> commands.execute(AppCommand.NEW_NOTE));
        editorPaneController.setServices(context.noteService(), context.progressService(),
                context.rewardService(), settingsService);
        editorPaneController.setCommandRegistry(commands);
        editorPaneController.setAppState(appState);
        sidebarPaneController.setAppState(appState);
        sidebarPaneController.setServices(context.noteService(), context.categoryService());
        rightPanelPaneController.setSettingsService(settingsService);
        selectStartupMemo();
        applyRuntimeSettings();
        appState.currentNoteProperty().addListener((observable, oldNote, note) -> {
            if (note != null) {
                settingsService.setString("app.lastMemoId", note.id());
                statusLabel.setText("메모 열림: " + note.title());
            }
        });
        if (appState.currentNoteProperty().get() == null && !appState.notes().isEmpty()) {
            appState.currentNoteProperty().set(appState.notes().get(0));
        }
        appState.leftSidebarVisibleProperty().addListener((observable, oldValue, showing) ->
                {
                    settingsService.setBoolean("layout.leftSidebarVisible", showing);
                    updatePanelVisibility(sidebarPane, leftPanelToggleButton, "사이드바", showing);
                });
        appState.rightPanelVisibleProperty().addListener((observable, oldValue, showing) ->
                {
                    settingsService.setBoolean("layout.rightPanelVisible", showing);
                    updatePanelVisibility(rightPanelPane, rightPanelToggleButton, "오른쪽 패널", showing);
                });
        editorPaneController.setSavedNoteHandler(sidebarPaneController::showSavedNote);
        sidebarPaneController.setStatusHandler(statusLabel::setText);
        updatePanelVisibility(sidebarPane, leftPanelToggleButton, "사이드바", appState.leftSidebarVisibleProperty().get());
        updatePanelVisibility(rightPanelPane, rightPanelToggleButton, "오른쪽 패널", appState.rightPanelVisibleProperty().get());
        Platform.runLater(this::installResponsiveLayout);
    }

    public void shutdown() {
        if (rightPanelPaneController != null) {
            rightPanelPaneController.dispose();
        }
    }

    private void loadPersistedSettings() {
        appState.leftSidebarVisibleProperty().set(settingsService.getBoolean("layout.leftSidebarVisible", true));
        appState.rightPanelVisibleProperty().set(settingsService.getBoolean("layout.rightPanelVisible", true));
    }

    private void registerCommands() {
        commands.register(AppCommand.NEW_NOTE, this::createNewMemo);
        commands.register(AppCommand.SAVE_NOTE, this::saveCurrentMemo);
        commands.register(AppCommand.CLEAR_NOTE, () -> editorPaneController.performCommand(AppCommand.CLEAR_NOTE));
        commands.register(AppCommand.UNDO, () -> editorPaneController.performCommand(AppCommand.UNDO));
        commands.register(AppCommand.REDO, () -> editorPaneController.performCommand(AppCommand.REDO));
        commands.register(AppCommand.TOGGLE_UNDERLINE, () -> editorPaneController.performCommand(AppCommand.TOGGLE_UNDERLINE));
        commands.register(AppCommand.TOGGLE_STRIKETHROUGH, () -> editorPaneController.performCommand(AppCommand.TOGGLE_STRIKETHROUGH));
        commands.register(AppCommand.TOGGLE_SUBSCRIPT, () -> editorPaneController.performCommand(AppCommand.TOGGLE_SUBSCRIPT));
        commands.register(AppCommand.TOGGLE_SUPERSCRIPT, () -> editorPaneController.performCommand(AppCommand.TOGGLE_SUPERSCRIPT));
        commands.register(AppCommand.INSERT_NUMBERED_LIST, () -> editorPaneController.performCommand(AppCommand.INSERT_NUMBERED_LIST));
        commands.register(AppCommand.INDENT, () -> editorPaneController.performCommand(AppCommand.INDENT));
        commands.register(AppCommand.OUTDENT, () -> editorPaneController.performCommand(AppCommand.OUTDENT));
        commands.register(AppCommand.TOGGLE_CHECKBOX_LINES, () -> editorPaneController.performCommand(AppCommand.TOGGLE_CHECKBOX_LINES));
        commands.register(AppCommand.INSERT_DIVIDER, () -> editorPaneController.performCommand(AppCommand.INSERT_DIVIDER));
        commands.register(AppCommand.TOGGLE_LEFT_SIDEBAR,
                () -> appState.leftSidebarVisibleProperty().set(!appState.leftSidebarVisibleProperty().get()));
        commands.register(AppCommand.TOGGLE_RIGHT_PANEL,
                () -> appState.rightPanelVisibleProperty().set(!appState.rightPanelVisibleProperty().get()));
        commands.register(AppCommand.OPEN_SETTINGS, this::openSettings);
        commands.register(AppCommand.EXPORT_BACKUP, this::exportBackup);
        commands.register(AppCommand.IMPORT_BACKUP, this::importBackup);
        commands.register(AppCommand.EXIT, () -> {
            shutdown();
            Platform.exit();
        });
    }

    public void executeCommand(AppCommand command) {
        commands.execute(command);
    }

    private void createNewMemo() {
        sidebarPaneController.createNewMemo();
        statusLabel.setText("새 메모를 만들었습니다.");
    }

    private void saveCurrentMemo() {
        editorPaneController.saveNow();
        statusLabel.setText("메모를 저장했습니다.");
    }

    private void updatePanelVisibility(Node panel, Button button, String label, boolean showing) {
        panel.setVisible(showing);
        panel.setManaged(showing);
        button.setText(showing ? label + " 숨김" : label + " 표시");
        statusLabel.setText(label + (showing ? " 표시" : " 숨김"));
        updateResponsiveLayout(appBody.getWidth());
    }

    private void installResponsiveLayout() {
        updateResponsiveLayout(appBody.getWidth());
        appBody.widthProperty().addListener((observable, oldValue, newValue) ->
                updateResponsiveLayout(newValue.doubleValue()));
    }

    private void updateResponsiveLayout(double bodyWidth) {
        appBody.setSpacing(16);
        appBody.setPadding(new Insets(18));
        resizeSidePanel(sidebarPane, SIDE_PANEL_WIDTH);
        resizeSidePanel(rightPanelPane, SIDE_PANEL_WIDTH);
        editorPane.setMinWidth(EDITOR_MIN_WIDTH);
        editorPane.setMaxWidth(Double.MAX_VALUE);
    }

    private void resizeSidePanel(Region panel, double width) {
        panel.setMinWidth(width);
        panel.setPrefWidth(width);
        panel.setMaxWidth(width);
    }

    private void openSettings() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/cozynote/views/settings.fxml"));
            Parent root = loader.load();
            if ("어두운 모드".equals(settingsService.getString("theme.name", "밝은 모드"))) {
                root.getStyleClass().add("dark-theme");
            }
            SettingsController controller = loader.getController();
            controller.setContext(appState, settingsService, this::exportBackup, this::importBackup, this::applyRuntimeSettings);
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("설정");
            dialog.setScene(new Scene(root, 560, 680));
            dialog.showAndWait();
            applyRuntimeSettings();
        } catch (IOException exception) {
            showError("설정 화면을 열지 못했습니다.", exception.getMessage());
        }
    }

    private void selectStartupMemo() {
        if (appState.notes().isEmpty()) {
            return;
        }
        if (!settingsService.getBoolean("app.reopenLastMemo", true)) {
            appState.currentNoteProperty().set(appState.notes().get(0));
            return;
        }
        String lastMemoId = settingsService.getString("app.lastMemoId", "");
        appState.notes().stream()
                .filter(note -> note.id().equals(lastMemoId))
                .findFirst()
                .ifPresentOrElse(
                        note -> appState.currentNoteProperty().set(note),
                        () -> appState.currentNoteProperty().set(appState.notes().get(0)));
    }

    public void applyRuntimeSettings() {
        applyTheme();
        editorPaneController.applySettings();
        rightPanelPaneController.applySettings();
    }

    private void applyTheme() {
        Scene scene = appBody == null ? null : appBody.getScene();
        if (scene == null) {
            return;
        }
        boolean dark = "어두운 모드".equals(settingsService.getString("theme.name", "밝은 모드"));
        scene.getRoot().getStyleClass().remove("dark-theme");
        if (dark) {
            scene.getRoot().getStyleClass().add("dark-theme");
        }
    }

    private void showHelp() {
        VBox guide = new VBox(12,
                helpSection("메모 작성",
                        "새 메모", "왼쪽 사이드바의 + 새 메모를 누르면 빈 메모가 만들어지고 최근 메모 맨 위에 추가됩니다.",
                        "가이드라인", "새 메모가 비어 있으면 안내 문구가 보입니다. 메모 영역을 클릭하거나 입력을 시작하면 실제 내용으로 저장되지 않고 사라집니다.",
                        "입력 위치", "일반 메모장처럼 실제 텍스트 흐름 안에서만 커서가 이동합니다. 빈 공간 클릭으로 새 줄이 자동 생성되지는 않습니다.",
                        "줄바꿈", "Enter로 새 줄을 만들고, Tab은 현재 커서 위치에 공백 4칸만 넣습니다.",
                        "수정", "내용을 입력하면 하단 상태가 대기/저장 중으로 바뀌고, 자동 저장이 켜져 있으면 잠시 뒤 저장됩니다."),
                helpSection("저장과 복원",
                        "즉시 저장", "Ctrl+S를 누르면 현재 제목과 본문이 즉시 저장됩니다.",
                        "자동 저장", "설정에서 자동 저장을 켜면 입력 후 잠시 멈췄을 때 저장됩니다. 끄면 Ctrl+S로 직접 저장해야 합니다.",
                        "최근 메모", "저장된 메모는 최근 메모 목록에 표시되고, 선택하면 중앙 에디터가 해당 메모로 바뀝니다.",
                        "마지막 메모", "설정에서 시작 시 마지막 메모 열기를 켜면 종료 전 마지막으로 보던 메모를 다음 실행 때 다시 엽니다.",
                        "상태바", "하단에는 글자 수, 공백 제외 글자 수, 줄 수, 포인트, 확대율, 저장 시간이 표시됩니다."),
                helpSection("서식 도구",
                        "글꼴 크기", "글자를 드래그한 상태에서 크기를 바꾸면 선택 영역만 바뀝니다. 선택이 없으면 이후 입력 기준 크기가 바뀝니다.",
                        "색상", "글꼴 색상은 선택한 글자의 색을 바꾸고, 하이라이트는 선택한 글자의 배경색을 바꿉니다.",
                        "기본 꾸밈", "U는 밑줄, S는 취소선, X₁은 아래첨자, X²는 위첨자를 적용합니다.",
                        "정렬", "정렬 콤보에서 왼쪽, 중앙, 오른쪽을 선택하면 현재 줄 또는 선택한 줄에 적용됩니다.",
                        "번호", "여러 줄을 선택하고 1. 버튼을 누르면 들여쓰기 깊이별로 번호가 따로 매겨집니다.",
                        "체크박스", "□ 버튼은 현재 줄 또는 선택한 줄을 체크박스 형식으로 바꾸고, 다시 누르면 체크 상태를 전환합니다.",
                        "수평선", "― 버튼은 현재 커서 위치에 구분선을 삽입합니다."),
                helpSection("사이드바와 목록",
                        "검색", "검색창에 입력하면 제목과 본문에 해당 단어가 들어간 메모만 표시됩니다.",
                        "최근 메모", "최근 메모는 저장/수정된 메모를 중심으로 표시됩니다. 항목을 클릭하면 메모가 열립니다.",
                        "즐겨찾기", "메모 항목을 우클릭해 즐겨찾기에 추가하거나 해제할 수 있습니다.",
                        "카테고리", "카테고리를 선택하면 해당 카테고리의 메모만 볼 수 있습니다.",
                        "패널 표시", "상단의 사이드바 숨김/표시 버튼으로 왼쪽 패널을 접거나 다시 펼칩니다."),
                helpSection("음악 플레이어",
                        "재생", "재생 버튼으로 현재 트랙을 재생하거나 일시정지합니다.",
                        "이전/다음", "이전, 다음 버튼으로 트랙을 이동합니다.",
                        "진행바", "진행 슬라이더를 움직이면 원하는 위치로 이동합니다.",
                        "볼륨", "아래 볼륨 슬라이더는 실제 음악 볼륨과 연결됩니다.",
                        "자동 재생", "설정에서 앱 시작 시 음악 자동 재생을 켜면 실행 시 자동으로 재생됩니다.",
                        "패널 표시", "오른쪽 패널 숨김/표시 버튼으로 음악 플레이어를 접거나 펼칩니다."),
                helpSection("도구 > 설정",
                        "테마", "밝은 모드와 어두운 모드를 전환합니다. 어두운 모드는 배경을 검은 계열로, 일반 글자를 흰색으로 표시합니다.",
                        "글자 크기", "설정의 글자 크기는 에디터 기본 글자 크기에 반영됩니다.",
                        "화면 구성", "사이드바 표시와 오른쪽 위젯 표시를 저장하면 즉시 화면에 반영되고 다음 실행에도 유지됩니다.",
                        "데이터 관리", "백업 내보내기는 현재 메모를 JSON 파일로 저장하고, 백업 불러오기는 JSON 백업에서 메모를 복원합니다.",
                        "저장 위치", "저장 위치 열기는 CozyNote가 사용하는 데이터 폴더를 파일 탐색기로 엽니다.",
                        "닫기", "닫기는 변경 없이 설정 창만 닫고, 저장은 선택한 값을 저장한 뒤 창을 닫습니다."));
        guide.setPadding(new Insets(14));

        ScrollPane scrollPane = new ScrollPane(guide);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setPrefViewportWidth(680);
        scrollPane.setPrefViewportHeight(520);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("CozyNote 사용 설명서");
        alert.setHeaderText("자주 쓰는 기능을 한눈에 확인하세요.");
        alert.getDialogPane().setContent(scrollPane);
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/com/cozynote/styles/cozy.css").toExternalForm());
        if ("어두운 모드".equals(settingsService.getString("theme.name", "밝은 모드"))) {
            alert.getDialogPane().getStyleClass().add("dark-theme");
        }
        alert.setResizable(true);
        alert.showAndWait();
    }

    private VBox helpSection(String title, String... entries) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: #2b2f33;");

        VBox rows = new VBox(6);
        for (int i = 0; i + 1 < entries.length; i += 2) {
            Label key = new Label(entries[i]);
            key.setMinWidth(92);
            key.setStyle("-fx-font-weight: 700; -fx-text-fill: #3f7eb3;");
            Label value = new Label(entries[i + 1]);
            value.setWrapText(true);
            value.setStyle("-fx-text-fill: #5f5145;");
            HBox row = new HBox(10, key, value);
            rows.getChildren().add(row);
        }

        VBox section = new VBox(8, titleLabel, rows);
        section.setPadding(new Insets(12));
        section.setStyle("-fx-background-color: #fffaf4; -fx-border-color: #e4d4c3; -fx-border-radius: 8px; -fx-background-radius: 8px;");
        return section;
    }

    private void exportBackup() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("백업 내보내기");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
        java.io.File target = chooser.showSaveDialog(null);
        if (target == null) {
            return;
        }
        try {
            List<Note> notes = noteService.listNotes();
            backupService.exportJson(target.toPath(), notes, "{}", "{}");
            statusLabel.setText("백업 완료: " + target.getName());
        } catch (IOException exception) {
            showError("백업 실패", exception.getMessage());
        }
    }

    private void importBackup() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("백업 불러오기");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
        java.io.File source = chooser.showOpenDialog(null);
        if (source == null) {
            return;
        }
        try {
            List<Note> importedNotes = backupService.readNotes(source.toPath());
            noteService.saveAll(importedNotes);
            sidebarPaneController.reloadNotes();
            if (!importedNotes.isEmpty()) {
                appState.currentNoteProperty().set(importedNotes.get(0));
            }
            statusLabel.setText("백업 불러오기 완료: " + importedNotes.size() + "개");
        } catch (IOException exception) {
            showError("백업 불러오기 실패", exception.getMessage());
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

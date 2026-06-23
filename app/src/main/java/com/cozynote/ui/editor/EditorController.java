package com.cozynote.ui.editor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import com.cozynote.app.AppCommand;
import com.cozynote.app.AppState;
import com.cozynote.app.CommandRegistry;
import com.cozynote.domain.BlockType;
import com.cozynote.domain.Category;
import com.cozynote.domain.Note;
import com.cozynote.domain.NoteBlock;
import com.cozynote.service.AutoSaveService;
import com.cozynote.service.EditorStats;
import com.cozynote.service.NoteService;
import com.cozynote.service.ProgressService;
import com.cozynote.service.RewardService;
import com.cozynote.service.SettingsService;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

public final class EditorController {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("a h:mm");
    private static final double BASE_FONT_SIZE = 16.0;
    private static final double MONOSPACE_CHAR_WIDTH_RATIO = 0.62;
    private static final Color DEFAULT_TEXT_COLOR = Color.web("#161a1d");
    private static final Color DEFAULT_HIGHLIGHT_COLOR = Color.web("#fffaf4");
    private static final String INDENT_TEXT = "    ";

    @FXML private TextField titleField;
    @FXML private WebView memoWebView;
    @FXML private CheckBox wrapTextCheckBox;
    @FXML private ComboBox<Integer> fontSizeComboBox;
    @FXML private ComboBox<String> alignmentComboBox;
    @FXML private ColorPicker fontColorPicker;
    @FXML private ColorPicker highlightColorPicker;
    @FXML private Pane visualBlockContainer;
    @FXML private Label characterCountLabel;
    @FXML private Label noSpaceCountLabel;
    @FXML private Label lineCountLabel;
    @FXML private Label zoomLabel;
    @FXML private Label pointLabel;
    @FXML private Label savedStateLabel;

    private AutoSaveService autoSaveService;
    private NoteService noteService;
    private ProgressService progressService;
    private RewardService rewardService = new RewardService();
    private SettingsService settingsService;
    private AppState appState;
    private CommandRegistry commandRegistry;
    private Note currentNote;
    private Consumer<Note> savedNoteHandler;
    private double zoom = 1.0;
    private int fontSize = (int) BASE_FONT_SIZE;
    private Color textColor = DEFAULT_TEXT_COLOR;
    private Color highlightColor = DEFAULT_HIGHLIGHT_COLOR;
    private boolean darkTheme;
    private boolean rendering;
    private boolean shortcutsInstalled;
    private boolean editorLoaded;
    private boolean executingCommand;

    @FXML
    private void initialize() {
        fontSizeComboBox.getItems().setAll(10, 11, 12, 14, 16, 18, 20, 22, 24, 28, 32, 36);
        fontSizeComboBox.setValue(fontSize);
        alignmentComboBox.getItems().setAll("왼쪽", "중앙", "오른쪽");
        fontColorPicker.setValue(textColor);
        highlightColorPicker.setValue(highlightColor);
        titleField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (rendering || currentNote == null) {
                return;
            }
            currentNote.setTitle(newValue.isBlank() ? "제목 없음" : newValue);
            markDirty();
        });
        wrapTextCheckBox.selectedProperty().addListener((observable, oldValue, selected) ->
                {
                    setEditorWrap(selected);
                    if (settingsService != null) {
                        settingsService.setBoolean("editor.wrapText", selected);
                    }
                });
        memoWebView.sceneProperty().addListener((observable, oldScene, newScene) -> installShortcuts(newScene));
        installVisualLayerBounds();
        installVisualLayerPassThrough();
        installRichEditor();
        applyZoom();
        renderNote(emptyDraftNote());
    }

    public void setServices(NoteService noteService, ProgressService progressService,
            RewardService rewardService, SettingsService settingsService) {
        this.noteService = noteService;
        this.progressService = progressService;
        this.rewardService = rewardService;
        this.settingsService = settingsService;
        this.autoSaveService = new AutoSaveService(noteService, this::updateSaveStatus);
        fontSize = fontSizeFromScale(settingsService.getString("editor.fontScale", "보통"));
        fontSizeComboBox.setValue(fontSize);
        darkTheme = "어두운 모드".equals(settingsService.getString("theme.name", "밝은 모드"));
        applyEditorStyle();
        wrapTextCheckBox.setSelected(settingsService.getBoolean("editor.wrapText", true));
    }

    public void setAppState(AppState appState) {
        this.appState = appState;
        zoom = appState.editorZoomProperty().get();
        applyZoom();
        appState.currentNoteProperty().addListener((observable, oldNote, note) -> {
            if (note != null && note != currentNote) {
                renderNote(note);
            }
        });
        appState.dirtyProperty().addListener((observable, oldValue, dirty) -> {
            if (dirty) {
                savedStateLabel.setText("… 대기");
            }
        });
        Note note = appState.currentNoteProperty().get();
        if (note != null && note != currentNote) {
            renderNote(note);
        }
    }

    public void setCommandRegistry(CommandRegistry commandRegistry) {
        this.commandRegistry = commandRegistry;
    }

    public void applySettings() {
        if (settingsService == null) {
            return;
        }
        fontSize = fontSizeFromScale(settingsService.getString("editor.fontScale", "보통"));
        fontSizeComboBox.setValue(fontSize);
        darkTheme = "어두운 모드".equals(settingsService.getString("theme.name", "밝은 모드"));
        applyEditorStyle();
        wrapTextCheckBox.setSelected(settingsService.getBoolean("editor.wrapText", true));
    }

    public void performCommand(AppCommand command) {
        executingCommand = true;
        try {
            switch (command) {
                case CLEAR_NOTE -> clearMemoContentNow();
                case SAVE_NOTE -> saveNowNow();
                case UNDO -> undoNow();
                case REDO -> redoNow();
                case TOGGLE_UNDERLINE -> applyUnderlineNow();
                case TOGGLE_STRIKETHROUGH -> applyStrikeNow();
                case TOGGLE_SUBSCRIPT -> applySubscriptNow();
                case TOGGLE_SUPERSCRIPT -> applySuperscriptNow();
                case INSERT_NUMBERED_LIST -> applyNumberingNow();
                case INDENT -> indentLinesNow();
                case OUTDENT -> outdentLinesNow();
                case TOGGLE_CHECKBOX_LINES -> toggleCheckboxLinesNow();
                case INSERT_DIVIDER -> insertHorizontalRuleNow();
                default -> { }
            }
        } finally {
            executingCommand = false;
        }
    }

    public void renderNote(Note note) {
        rendering = true;
        currentNote = note;
        if (appState != null && appState.currentNoteProperty().get() != note) {
            appState.currentNoteProperty().set(note);
        }
        removeTableAndChartBlocks();
        ensureLeadingTextBlock();
        titleField.setText(note.title());
        setEditorHtml(htmlFromNote(note));
        renderVisualBlocks();
        refreshStats();
        savedStateLabel.setText("✓ " + LocalDateTime.now().format(TIME_FORMAT));
        rendering = false;
    }

    public void setSavedNoteHandler(Consumer<Note> savedNoteHandler) {
        this.savedNoteHandler = savedNoteHandler;
    }

    @FXML private void undo() {
        runEditorCommand(AppCommand.UNDO, this::undoNow);
    }

    @FXML private void redo() {
        runEditorCommand(AppCommand.REDO, this::redoNow);
    }

    @FXML private void clearMemoContent() {
        runEditorCommand(AppCommand.CLEAR_NOTE, this::clearMemoContentNow);
    }

    private void clearMemoContentNow() {
        if (!editorText().isBlank() && !confirm("내용 비우기", "메모장 안의 모든 내용을 지울까요?")) {
            return;
        }
        setEditorHtml("");
        syncEditorToNote();
        markDirty();
        focusEditor();
    }

    @FXML public void saveNow() {
        runEditorCommand(AppCommand.SAVE_NOTE, this::saveNowNow);
    }

    private void saveNowNow() {
        if (currentNote == null) {
            return;
        }
        saveAndRenderStatus("✓ " + LocalDateTime.now().format(TIME_FORMAT));
    }

    @FXML private void changeFontSize() {
        Integer selectedSize = fontSizeComboBox.getValue();
        if (selectedSize == null) {
            return;
        }
        fontSize = selectedSize;
        if (settingsService != null) {
            settingsService.setString("editor.fontScale", scaleFromFontSize(fontSize));
        }
        applyFontSizeToSelection(fontSize);
        savedStateLabel.setText("글꼴 크기 " + fontSize);
        focusEditor();
    }

    @FXML private void changeFontColor() {
        textColor = fontColorPicker.getValue() == null ? DEFAULT_TEXT_COLOR : fontColorPicker.getValue();
        execEditorCommand("foreColor", toRgb(textColor));
        savedStateLabel.setText("글꼴 색상 변경");
    }

    @FXML private void changeHighlight() {
        highlightColor = highlightColorPicker.getValue() == null ? DEFAULT_HIGHLIGHT_COLOR : highlightColorPicker.getValue();
        execEditorCommand("backColor", toRgb(highlightColor));
        savedStateLabel.setText("하이라이트 변경");
    }

    @FXML private void applyUnderline() {
        runEditorCommand(AppCommand.TOGGLE_UNDERLINE, this::applyUnderlineNow);
    }

    @FXML private void applyStrike() {
        runEditorCommand(AppCommand.TOGGLE_STRIKETHROUGH, this::applyStrikeNow);
    }

    @FXML private void applySubscript() {
        runEditorCommand(AppCommand.TOGGLE_SUBSCRIPT, this::applySubscriptNow);
    }

    @FXML private void applySuperscript() {
        runEditorCommand(AppCommand.TOGGLE_SUPERSCRIPT, this::applySuperscriptNow);
    }

    @FXML private void changeAlignment() {
        String alignment = alignmentComboBox.getValue();
        if (alignment == null) {
            return;
        }
        switch (alignment) {
            case "왼쪽" -> alignLeft();
            case "중앙" -> alignCenter();
            case "오른쪽" -> alignRight();
            default -> { }
        }
        alignmentComboBox.setValue(null);
    }

    @FXML private void alignLeft() {
        transformSelectedLines(line -> line.stripLeading(), "왼쪽 정렬");
    }

    @FXML private void alignCenter() {
        transformSelectedLines(this::centerLine, "중앙 정렬");
    }

    @FXML private void alignRight() {
        transformSelectedLines(this::rightLine, "오른쪽 정렬");
    }

    @FXML private void applyNumbering() {
        runEditorCommand(AppCommand.INSERT_NUMBERED_LIST, this::applyNumberingNow);
    }

    private void applyNumberingNow() {
        if (editorText().isBlank() && editorSelectionStart() == editorSelectionEnd()) {
            replaceEditorSelection("1. ");
            syncEditorToNote();
            markDirty();
            savedStateLabel.setText("번호 매기기");
            focusEditor();
            return;
        }
        numberSelectedLines();
    }

    @FXML private void indentLines() {
        runEditorCommand(AppCommand.INDENT, this::indentLinesNow);
    }

    @FXML private void outdentLines() {
        runEditorCommand(AppCommand.OUTDENT, this::outdentLinesNow);
    }

    private void outdentLinesNow() {
        transformSelectedLines(line -> {
            if (line.startsWith(INDENT_TEXT)) {
                return line.substring(INDENT_TEXT.length());
            }
            if (line.startsWith("\t")) {
                return line.replaceFirst("^\\t", INDENT_TEXT).substring(INDENT_TEXT.length());
            }
            return line.stripLeading();
        }, "내어쓰기");
    }

    @FXML private void toggleCheckboxLines() {
        runEditorCommand(AppCommand.TOGGLE_CHECKBOX_LINES, this::toggleCheckboxLinesNow);
    }

    @FXML private void insertHorizontalRule() {
        runEditorCommand(AppCommand.INSERT_DIVIDER, this::insertHorizontalRuleNow);
    }

    private void insertHorizontalRuleNow() {
        String text = editorText();
        int caret = editorCaretOffset();
        String prefix = caret > 0 && text.charAt(caret - 1) != '\n' ? "\n" : "";
        String suffix = caret < text.length() && text.charAt(caret) != '\n' ? "\n" : "";
        String rule = prefix + "────────────────────────" + suffix;
        replaceEditorSelection(rule);
        syncEditorToNote();
        markDirty();
        savedStateLabel.setText("수평선 삽입");
        focusEditor();
    }

    private void undoNow() {
        execEditorCommand("undo", null);
    }

    private void redoNow() {
        execEditorCommand("redo", null);
    }

    private void applyUnderlineNow() {
        execEditorCommand("underline", null);
        savedStateLabel.setText("밑줄 적용");
    }

    private void applyStrikeNow() {
        execEditorCommand("strikeThrough", null);
        savedStateLabel.setText("취소선 적용");
    }

    private void applySubscriptNow() {
        transformSelection(this::subscriptText, "아래첨자 적용");
    }

    private void applySuperscriptNow() {
        transformSelection(this::superscriptText, "위첨자 적용");
    }

    private void indentLinesNow() {
        transformSelectedLines(line -> line.isBlank() ? line : INDENT_TEXT + line, "들여쓰기");
    }

    private void insertIndentAtCaret() {
        replaceEditorSelection(INDENT_TEXT);
        syncEditorToNote();
        markDirty();
        savedStateLabel.setText("공백 4칸");
        focusEditor();
    }

    private void removeIndentAtCaret() {
        String text = editorText();
        int caret = editorCaretOffset();
        int lineStart = lineStart(text, caret);
        int removable = Math.min(INDENT_TEXT.length(), caret - lineStart);
        if (removable <= 0) {
            focusEditor();
            return;
        }
        int removeStart = caret - removable;
        String beforeCaret = text.substring(removeStart, caret);
        if (!beforeCaret.isBlank()) {
            focusEditor();
            return;
        }
        selectEditorRange(removeStart, caret);
        replaceEditorSelection("");
        syncEditorToNote();
        markDirty();
        savedStateLabel.setText("공백 4칸 제거");
        focusEditor();
    }

    private void toggleCheckboxLinesNow() {
        transformSelectedLines(this::toggleCheckboxLine, "체크박스 변경");
    }

    private void runEditorCommand(AppCommand command, Runnable directAction) {
        if (commandRegistry != null && !executingCommand) {
            commandRegistry.execute(command);
            return;
        }
        directAction.run();
    }

    private void installShortcuts(Scene scene) {
        if (scene == null || shortcutsInstalled) {
            return;
        }
        shortcutsInstalled = true;
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.TAB && memoWebView.isFocused()) {
                if (event.isShiftDown()) {
                    removeIndentAtCaret();
                } else {
                    insertIndentAtCaret();
                }
                event.consume();
                return;
            }
            if (!event.isControlDown()) {
                return;
            }
            if (event.getCode() == KeyCode.S) {
                saveNow();
                event.consume();
            }
        });
    }

    private void ensureLeadingTextBlock() {
        if (currentNote == null) {
            return;
        }
        if (currentNote.blocks().isEmpty() || currentNote.blocks().get(0).type() != BlockType.TEXT) {
            currentNote.blocks().add(0, new NoteBlock(BlockType.TEXT, "", 0));
        }
        normalizeOrder();
    }

    private boolean isVisualBlock(NoteBlock block) {
        return block.type() == BlockType.TABLE || block.type() == BlockType.CHART;
    }

    private void removeTableAndChartBlocks() {
        currentNote.blocks().removeIf(this::isVisualBlock);
        normalizeOrder();
    }

    private void persistCurrentNote() throws java.io.IOException {
        syncEditorToNote();
        currentNote.refreshBodyFromBlocks();
        ensureServicesReady();
        noteService.save(currentNote);
    }

    private void requestAutoSave() {
        syncEditorToNote();
        currentNote.refreshBodyFromBlocks();
        if (appState != null) {
            appState.currentNoteProperty().set(currentNote);
            appState.dirtyProperty().set(true);
            appState.savingProperty().set(true);
        }
        boolean autoSaveEnabled = settingsService == null || settingsService.getBoolean("editor.autoSave", true);
        if (autoSaveService != null && autoSaveEnabled) {
            autoSaveService.requestSave(currentNote);
        } else if (appState != null) {
            appState.savingProperty().set(false);
        }
    }

    private void saveAndRenderStatus(String successStatus) {
        try {
            persistCurrentNote();
            if (appState != null) {
                appState.currentNoteProperty().set(currentNote);
                appState.dirtyProperty().set(false);
                appState.savingProperty().set(false);
            }
            if (savedNoteHandler != null) {
                savedNoteHandler.accept(currentNote);
            }
            savedStateLabel.setText(successStatus);
        } catch (java.io.IOException exception) {
            savedStateLabel.setText("! 실패");
            showError("저장 실패", exception.getMessage());
        }
    }

    private void renderVisualBlocks() {
        if (visualBlockContainer == null || currentNote == null) {
            return;
        }
        visualBlockContainer.getChildren().clear();
        visualBlockContainer.setVisible(false);
        visualBlockContainer.setManaged(false);
    }

    private void installVisualLayerBounds() {
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(visualBlockContainer.widthProperty());
        clip.heightProperty().bind(visualBlockContainer.heightProperty());
        visualBlockContainer.setClip(clip);
    }

    private void installVisualLayerPassThrough() {
        visualBlockContainer.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getTarget() == visualBlockContainer) {
                clearVisualSelection();
                focusEditor();
                event.consume();
            }
        });
    }

    private void clearVisualSelection() {
        for (Node child : visualBlockContainer.getChildren()) {
            child.getStyleClass().remove("selected-block");
        }
    }

    private String textBodyFromBlocks(List<NoteBlock> blocks) {
        return blocks.stream()
                .filter(block -> block.type() != BlockType.TABLE && block.type() != BlockType.CHART)
                .map(NoteBlock::text)
                .reduce("", (left, right) -> left.isEmpty() ? right : left + System.lineSeparator() + right);
    }

    private String htmlFromNote(Note note) {
        for (NoteBlock block : note.blocks()) {
            if (block.type() == BlockType.TEXT && block.richTextHtml() != null && !block.richTextHtml().isBlank()) {
                return block.richTextHtml();
            }
        }
        return escapeHtml(textBodyFromBlocks(note.blocks())).replace("\n", "<br>");
    }

    private void installRichEditor() {
        WebEngine engine = memoWebView.getEngine();
        engine.getLoadWorker().stateProperty().addListener((observable, oldValue, state) -> {
            if (state == javafx.concurrent.Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("cozyBridge", new Bridge());
                editorLoaded = true;
                applyEditorStyle();
            }
        });
        engine.loadContent(editorDocument(""));
    }

    private String editorDocument(String body) {
        return """
                <!doctype html>
                <html>
                <head>
                <meta charset="UTF-8">
                <style>
                html, body {
                    margin: 0;
                    padding: 0;
                    height: 100%%;
                    background: #fffaf4;
                    color: #161a1d;
                    font-family: "Malgun Gothic", "Noto Sans KR", "Segoe UI", sans-serif;
                    font-size: 16px;
                    line-height: 1.65;
                }
                ::-webkit-scrollbar {
                    width: 0;
                    height: 0;
                    background: transparent;
                }
                #editor {
                    box-sizing: border-box;
                    min-height: 520px;
                    width: 100%%;
                    padding: 18px 22px;
                    outline: none;
                    white-space: pre-wrap;
                    overflow-wrap: anywhere;
                    background: #fffaf4;
                    position: relative;
                }
                #editor.nowrap {
                    white-space: pre;
                    overflow-wrap: normal;
                }
                #editor[data-guide-visible="true"]::before {
                    content: "오늘의 메모를 시작해보세요.\\A\\A- 떠오른 생각을 바로 적기\\A- 해야 할 일은 한 줄씩 정리하기\\A- 중요한 내용은 밑줄이나 하이라이트로 표시하기";
                    white-space: pre-wrap;
                    color: var(--guide-color, #9a8b7a);
                    pointer-events: none;
                }
                ::selection {
                    background: #cfe0ec;
                }
                </style>
                </head>
                <body>
                <div id="editor" contenteditable="true">%s</div>
                <script>
                let cozySavedRange = null;
                function editor() { return document.getElementById('editor'); }
                function saveSelection() {
                    const selection = window.getSelection();
                    if (selection && selection.rangeCount > 0 && editor().contains(selection.anchorNode)) {
                        cozySavedRange = selection.getRangeAt(0).cloneRange();
                    }
                }
                function restoreSelection() {
                    editor().focus();
                    if (!cozySavedRange) return;
                    const selection = window.getSelection();
                    selection.removeAllRanges();
                    selection.addRange(cozySavedRange);
                }
                function sync() {
                    saveSelection();
                    if (window.cozyBridge) {
                        window.cozyBridge.changed(editor().innerHTML, editor().innerText);
                    }
                }
                function textNodeWalker() {
                    const walker = document.createTreeWalker(editor(), NodeFilter.SHOW_TEXT);
                    const nodes = [];
                    while (walker.nextNode()) nodes.push(walker.currentNode);
                    return nodes;
                }
                function rangeAt(start, end) {
                    const range = document.createRange();
                    const nodes = textNodeWalker();
                    let offset = 0;
                    let started = false;
                    for (const node of nodes) {
                        const next = offset + node.textContent.length;
                        if (!started && start <= next) {
                            range.setStart(node, Math.max(0, start - offset));
                            started = true;
                        }
                        if (started && end <= next) {
                            range.setEnd(node, Math.max(0, end - offset));
                            return range;
                        }
                        offset = next;
                    }
                    if (nodes.length === 0) {
                        const node = document.createTextNode('');
                        editor().appendChild(node);
                        range.setStart(node, 0);
                        range.setEnd(node, 0);
                    } else {
                        const node = nodes[nodes.length - 1];
                        range.setStart(node, node.textContent.length);
                        range.setEnd(node, node.textContent.length);
                    }
                    return range;
                }
                function plainText() {
                    return editor().innerText.replace(/\\r/g, '');
                }
                function shouldShowGuide(html) {
                    const probe = document.createElement('div');
                    probe.innerHTML = html || '';
                    return probe.innerText.replace(/\\s/g, '').length === 0;
                }
                function showGuideIfEmpty() {
                    if (shouldShowGuide(editor().innerHTML)) {
                        editor().setAttribute('data-guide-visible', 'true');
                    } else {
                        hideGuide();
                    }
                }
                function hideGuide() {
                    editor().removeAttribute('data-guide-visible');
                }
                function selectionOffset(which) {
                    const selection = window.getSelection();
                    if (!selection || selection.rangeCount === 0) return 0;
                    const range = selection.getRangeAt(0);
                    const probe = document.createRange();
                    probe.selectNodeContents(editor());
                    probe.setEnd(which === 'end' ? range.endContainer : range.startContainer,
                        which === 'end' ? range.endOffset : range.startOffset);
                    return probe.toString().length;
                }
                window.cozySetHtml = function(html) {
                    editor().innerHTML = html || '';
                    showGuideIfEmpty();
                    sync();
                };
                window.cozyHtml = function() { return editor().innerHTML; };
                window.cozyText = function() { return plainText(); };
                window.cozyCaret = function() { return selectionOffset('end'); };
                window.cozySelectionStart = function() { return Math.min(selectionOffset('start'), selectionOffset('end')); };
                window.cozySelectionEnd = function() { return Math.max(selectionOffset('start'), selectionOffset('end')); };
                window.cozySelect = function(start, end) {
                    const selection = window.getSelection();
                    selection.removeAllRanges();
                    const range = rangeAt(start, end);
                    selection.addRange(range);
                    cozySavedRange = range.cloneRange();
                    editor().focus();
                };
                window.cozyReplaceSelection = function(text) {
                    restoreSelection();
                    document.execCommand('insertText', false, text || '');
                    sync();
                };
                window.cozyReplacePlainRange = function(start, end, replacement) {
                    const text = plainText();
                    const safeStart = Math.max(0, Math.min(start, text.length));
                    const safeEnd = Math.max(safeStart, Math.min(end, text.length));
                    editor().innerText = text.substring(0, safeStart) + (replacement || '') + text.substring(safeEnd);
                    window.cozySelect(safeStart, safeStart + (replacement || '').length);
                    sync();
                };
                window.cozyCommand = function(command, value) {
                    restoreSelection();
                    document.execCommand(command, false, value || null);
                    sync();
                };
                window.cozyApplyFontSize = function(size) {
                    restoreSelection();
                    const selection = window.getSelection();
                    if (!selection || selection.rangeCount === 0 || selection.isCollapsed) {
                        editor().style.fontSize = size + 'px';
                        sync();
                        return;
                    }
                    document.execCommand('fontSize', false, '7');
                    editor().querySelectorAll('font[size="7"]').forEach(function(node) {
                        const span = document.createElement('span');
                        span.style.fontSize = size + 'px';
                        span.innerHTML = node.innerHTML;
                        node.replaceWith(span);
                    });
                    sync();
                };
                window.cozySetEditorStyle = function(size, background, foreground, guideColor) {
                    editor().style.fontSize = size + 'px';
                    document.documentElement.style.background = background;
                    document.body.style.background = background;
                    document.body.style.color = foreground;
                    editor().style.background = background;
                    editor().style.color = foreground;
                    editor().style.setProperty('--guide-color', guideColor);
                };
                window.cozySetWrap = function(wrap) {
                    editor().classList.toggle('nowrap', !wrap);
                };
                showGuideIfEmpty();
                editor().addEventListener('keydown', function(event) {
                    if (event.key === 'Tab') {
                        event.preventDefault();
                    }
                });
                editor().addEventListener('mousedown', hideGuide);
                editor().addEventListener('focus', hideGuide);
                editor().addEventListener('input', function() {
                    hideGuide();
                    sync();
                });
                editor().addEventListener('keyup', saveSelection);
                editor().addEventListener('mouseup', saveSelection);
                editor().addEventListener('focus', saveSelection);
                </script>
                </body>
                </html>
                """.formatted(body);
    }

    private Object execute(String script) {
        try {
            return memoWebView.getEngine().executeScript(script);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private void setEditorHtml(String html) {
        String safeHtml = html == null ? "" : html;
        if (!editorLoaded) {
            memoWebView.getEngine().loadContent(editorDocument(safeHtml));
            return;
        }
        execute("window.cozySetHtml && window.cozySetHtml('" + escapeJs(safeHtml) + "');");
    }

    private String editorHtml() {
        Object value = execute("window.cozyHtml && window.cozyHtml();");
        return value instanceof String text ? text : "";
    }

    private String editorText() {
        Object value = execute("window.cozyText && window.cozyText();");
        return value instanceof String text ? text : "";
    }

    private int editorCaretOffset() {
        return intFromScript("window.cozyCaret && window.cozyCaret();");
    }

    private int editorSelectionStart() {
        return intFromScript("window.cozySelectionStart && window.cozySelectionStart();");
    }

    private int editorSelectionEnd() {
        return intFromScript("window.cozySelectionEnd && window.cozySelectionEnd();");
    }

    private int intFromScript(String script) {
        Object value = execute(script);
        return value instanceof Number number ? number.intValue() : 0;
    }

    private void selectEditorRange(int start, int end) {
        execute("window.cozySelect && window.cozySelect(" + Math.max(0, start) + ", " + Math.max(0, end) + ");");
    }

    private void replaceEditorSelection(String text) {
        execute("window.cozyReplaceSelection && window.cozyReplaceSelection('" + escapeJs(text) + "');");
    }

    private void replaceEditorPlainRange(int start, int end, String text) {
        execute("window.cozyReplacePlainRange && window.cozyReplacePlainRange("
                + Math.max(0, start) + ", " + Math.max(0, end) + ", '" + escapeJs(text) + "');");
    }

    private void execEditorCommand(String command, String value) {
        String jsValue = value == null ? "null" : "'" + escapeJs(value) + "'";
        execute("window.cozyCommand && window.cozyCommand('" + escapeJs(command) + "', " + jsValue + ");");
        syncEditorToNote();
        markDirty();
        focusEditor();
    }

    private void syncEditorToNote() {
        NoteBlock textBlock = currentTextBlock();
        textBlock.setText(editorText());
        textBlock.setRichTextHtml(editorHtml());
        normalizeOrder();
    }

    private NoteBlock currentTextBlock() {
        for (NoteBlock block : currentNote.blocks()) {
            if (block.type() == BlockType.TEXT) {
                return block;
            }
        }
        NoteBlock block = new NoteBlock(BlockType.TEXT, "", 0);
        currentNote.blocks().add(0, block);
        normalizeOrder();
        return block;
    }

    private void setEditorWrap(boolean wrap) {
        execute("window.cozySetWrap && window.cozySetWrap(" + wrap + ");");
    }

    private void focusEditor() {
        memoWebView.requestFocus();
        execute("document.getElementById('editor').focus();");
        Platform.runLater(() -> {
            memoWebView.requestFocus();
            execute("document.getElementById('editor').focus();");
        });
    }

    private void normalizeOrder() {
        for (int i = 0; i < currentNote.blocks().size(); i++) {
            currentNote.blocks().get(i).setOrderIndex(i);
        }
    }

    private void transformSelection(java.util.function.Function<String, String> transformer, String status) {
        int start = editorSelectionStart();
        int end = editorSelectionEnd();
        if (end <= start) {
            savedStateLabel.setText("선택 필요");
            focusEditor();
            return;
        }
        String selectedText = editorText().substring(start, Math.min(end, editorText().length()));
        String transformedText = transformer.apply(selectedText);
        selectEditorRange(start, end);
        replaceEditorSelection(transformedText);
        selectEditorRange(start, start + transformedText.length());
        syncEditorToNote();
        markDirty();
        savedStateLabel.setText(status);
        focusEditor();
    }

    private void transformSelectedLines(java.util.function.Function<String, String> transformer, String status) {
        String text = editorText();
        int anchor = editorSelectionStart();
        int caret = editorSelectionEnd();
        int start = Math.min(anchor, caret);
        int end = Math.max(anchor, caret);
        if (start == end) {
            start = lineStart(text, editorCaretOffset());
            end = lineEnd(text, editorCaretOffset());
        } else {
            start = lineStart(text, start);
            end = lineEnd(text, end);
        }

        String target = text.substring(start, end);
        String[] lines = target.split("\\R", -1);
        String lineSeparator = target.contains("\r\n") ? "\r\n" : "\n";
        List<String> transformed = new ArrayList<>();
        for (String line : lines) {
            transformed.add(transformer.apply(line));
        }
        String replacement = String.join(lineSeparator, transformed);
        selectEditorRange(start, end);
        replaceEditorSelection(replacement);
        selectEditorRange(start, start + replacement.length());
        syncEditorToNote();
        markDirty();
        savedStateLabel.setText(status);
        focusEditor();
    }

    private void numberSelectedLines() {
        String text = editorText();
        int anchor = editorSelectionStart();
        int caret = editorSelectionEnd();
        int start = Math.min(anchor, caret);
        int end = Math.max(anchor, caret);
        if (start == end) {
            start = lineStart(text, editorCaretOffset());
            end = lineEnd(text, editorCaretOffset());
        } else {
            start = lineStart(text, start);
            end = lineEnd(text, end);
        }

        String target = text.substring(start, end);
        String[] lines = target.split("\\R", -1);
        String lineSeparator = target.contains("\r\n") ? "\r\n" : "\n";
        Map<Integer, Integer> counters = new HashMap<>();
        List<String> transformed = new ArrayList<>();

        for (String line : lines) {
            if (line.isBlank()) {
                transformed.add(line);
                continue;
            }

            String leading = leadingWhitespace(line);
            int level = indentLevel(leading);
            int number = counters.getOrDefault(level, 0) + 1;
            counters.put(level, number);
            counters.keySet().removeIf(existingLevel -> existingLevel > level);

            String content = line.substring(leading.length()).replaceFirst("^\\d+\\.\\s*", "");
            transformed.add(leading + number + ". " + content);
        }

        String replacement = String.join(lineSeparator, transformed);
        replaceEditorPlainRange(start, end, replacement);
        selectEditorRange(start, start + replacement.length());
        syncEditorToNote();
        markDirty();
        savedStateLabel.setText("번호 매기기");
        focusEditor();
    }

    private int lineStart(String text, int index) {
        int safeIndex = Math.max(0, Math.min(index, text.length()));
        int lineStart = text.lastIndexOf('\n', Math.max(0, safeIndex - 1));
        return lineStart < 0 ? 0 : lineStart + 1;
    }

    private int lineEnd(String text, int index) {
        int safeIndex = Math.max(0, Math.min(index, text.length()));
        int lineEnd = text.indexOf('\n', safeIndex);
        return lineEnd < 0 ? text.length() : lineEnd;
    }

    private String centerLine(String line) {
        String stripped = line.strip();
        if (stripped.isEmpty()) {
            return "";
        }
        int alignWidth = currentAlignmentWidth();
        int padding = Math.max(0, (alignWidth - stripped.length()) / 2);
        return " ".repeat(padding) + stripped;
    }

    private String rightLine(String line) {
        String stripped = line.strip();
        if (stripped.isEmpty()) {
            return "";
        }
        int padding = Math.max(0, currentAlignmentWidth() - stripped.length());
        return " ".repeat(padding) + stripped;
    }

    private int currentAlignmentWidth() {
        double availableWidth = memoWebView == null ? 0 : memoWebView.getWidth() - 44;
        int renderedSize = (int) Math.round(fontSize * zoom);
        double characterWidth = Math.max(1.0, renderedSize * MONOSPACE_CHAR_WIDTH_RATIO);
        int width = (int) Math.floor(availableWidth / characterWidth);
        return Math.max(20, width);
    }

    private String toggleCheckboxLine(String line) {
        String leading = leadingWhitespace(line);
        String content = line.substring(leading.length());
        if (content.startsWith("☐ ")) {
            return leading + "☑ " + content.substring(2);
        }
        if (content.startsWith("☑ ")) {
            return leading + "☐ " + content.substring(2);
        }
        if (content.startsWith("[ ] ")) {
            return leading + "[x] " + content.substring(4);
        }
        if (content.startsWith("[x] ") || content.startsWith("[X] ")) {
            return leading + "[ ] " + content.substring(4);
        }
        if (content.isBlank()) {
            return leading + "☐ ";
        }
        return leading + "☐ " + content;
    }

    private String leadingWhitespace(String line) {
        int index = 0;
        while (index < line.length() && Character.isWhitespace(line.charAt(index))) {
            index++;
        }
        return line.substring(0, index);
    }

    private int indentLevel(String leading) {
        int width = 0;
        for (int i = 0; i < leading.length(); i++) {
            width += leading.charAt(i) == '\t' ? INDENT_TEXT.length() : 1;
        }
        return width / INDENT_TEXT.length();
    }

    private String subscriptText(String text) {
        return mapCharacters(text, "0123456789+-=()aeioruvxhklmnpst", "₀₁₂₃₄₅₆₇₈₉₊₋₌₍₎ₐₑᵢₒᵣᵤᵥₓₕₖₗₘₙₚₛₜ");
    }

    private String superscriptText(String text) {
        return mapCharacters(text, "0123456789+-=()abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ",
                "⁰¹²³⁴⁵⁶⁷⁸⁹⁺⁻⁼⁽⁾ᵃᵇᶜᵈᵉᶠᵍʰᶦʲᵏˡᵐⁿᵒᵖᵠʳˢᵗᵘᵛʷˣʸᶻᴬᴮᶜᴰᴱᶠᴳᴴᴵᴶᴷᴸᴹᴺᴼᴾQᴿˢᵀᵁⱽᵂˣʸᶻ");
    }

    private String mapCharacters(String text, String from, String to) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char value = text.charAt(i);
            int index = from.indexOf(value);
            builder.append(index >= 0 && index < to.length() ? to.charAt(index) : value);
        }
        return builder.toString();
    }

    private void markDirty() {
        refreshStats();
        requestAutoSave();
    }

    private void refreshStats() {
        String text = editorText();
        int chars = text.length();
        int charsNoSpaces = text.replaceAll("\\s+", "").length();
        int lines = text.isEmpty() ? 1 : text.split("\\R", -1).length;
        EditorStats stats = new EditorStats(chars, charsNoSpaces, lines, zoom);
        characterCountLabel.setText("글 " + stats.characterCount());
        noSpaceCountLabel.setText("공백X " + stats.characterCountWithoutSpaces());
        lineCountLabel.setText("줄 " + stats.lineCount());
        zoomLabel.setText(Math.round(stats.zoom() * 100) + "%");
        RewardService.Summary rewardSummary = rewardService.summarize(currentNote);
        pointLabel.setText("P " + rewardSummary.totalPoints());
        try {
            if (progressService != null) {
                progressService.saveProgress("total_points", String.valueOf(rewardSummary.totalPoints()));
                progressService.saveProgress("completed_todos", String.valueOf(rewardSummary.completedTodos()));
            }
        } catch (java.io.IOException ignored) {
            pointLabel.setText("P !");
        }
    }

    private void ensureServicesReady() {
        if (noteService == null) {
            throw new IllegalStateException("Editor services were not initialized.");
        }
    }

    private void applyZoom() {
        applyEditorStyle();
    }

    private void applyEditorStyle() {
        if (memoWebView != null) {
            int renderedSize = (int) Math.round(fontSize * zoom);
            String background = darkTheme ? "#202124" : "#fffaf4";
            String foreground = darkTheme ? "#ffffff" : "#161a1d";
            String guide = darkTheme ? "#c9cdd2" : "#9a8b7a";
            execute("window.cozySetEditorStyle && window.cozySetEditorStyle(" + renderedSize
                    + ", '" + background + "', '" + foreground + "', '" + guide + "');");
        }
    }

    private void applyFontSizeToSelection(int size) {
        int renderedSize = (int) Math.round(size * zoom);
        execute("window.cozyApplyFontSize && window.cozyApplyFontSize(" + renderedSize + ");");
        syncEditorToNote();
        markDirty();
    }

    private int fontSizeFromScale(String scale) {
        if ("작게".equals(scale)) {
            return 14;
        }
        if ("크게".equals(scale)) {
            return 20;
        }
        return 16;
    }

    private String scaleFromFontSize(int size) {
        if (size <= 14) {
            return "작게";
        }
        if (size >= 20) {
            return "크게";
        }
        return "보통";
    }

    private String toRgb(Color color) {
        int red = (int) Math.round(color.getRed() * 255);
        int green = (int) Math.round(color.getGreen() * 255);
        int blue = (int) Math.round(color.getBlue() * 255);
        return String.format("#%02x%02x%02x", red, green, blue);
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String escapeJs(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\r", "")
                .replace("\n", "\\n");
    }

    private void updateSaveStatus(String status) {
        if (status.startsWith("저장 완료: ")) {
            savedStateLabel.setText("✓ " + status.substring("저장 완료: ".length()));
            if (appState != null) {
                appState.dirtyProperty().set(false);
                appState.savingProperty().set(false);
            }
        } else if (status.startsWith("입력 중")) {
            savedStateLabel.setText("… 대기");
            if (appState != null) {
                appState.dirtyProperty().set(true);
                appState.savingProperty().set(true);
            }
        } else if (status.startsWith("저장 중")) {
            savedStateLabel.setText("↻ 저장");
            if (appState != null) {
                appState.savingProperty().set(true);
            }
        } else if (status.contains("실패")) {
            savedStateLabel.setText("! 실패");
            if (appState != null) {
                appState.savingProperty().set(false);
            }
        } else {
            savedStateLabel.setText(status);
        }
    }

    private boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message == null ? "알 수 없는 오류가 발생했습니다." : message);
        alert.showAndWait();
    }

    public final class Bridge {
        public void changed(String html, String text) {
            if (rendering || currentNote == null) {
                return;
            }
            NoteBlock textBlock = currentTextBlock();
            textBlock.setText(text == null ? "" : text.replace("\r", ""));
            textBlock.setRichTextHtml(html == null ? "" : html);
            normalizeOrder();
            markDirty();
        }
    }

    private Note emptyDraftNote() {
        List<NoteBlock> blocks = new ArrayList<>();
        blocks.add(new NoteBlock(BlockType.TEXT, "", 0));
        return new Note(UUID.randomUUID().toString(), "오늘의 메모", "", new Category("모든 메모", 0),
                false, false, LocalDateTime.now(), LocalDateTime.now(), blocks);
    }
}

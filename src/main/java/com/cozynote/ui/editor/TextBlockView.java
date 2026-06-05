package com.cozynote.ui.editor;

import com.cozynote.domain.NoteBlock;

import javafx.concurrent.Worker;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

public final class TextBlockView extends HBox {
    private final NoteBlock block;
    private final Runnable onChanged;
    private final WebView webView = new WebView();
    private boolean loaded;

    public TextBlockView(NoteBlock block, Runnable onChanged) {
        this.block = block;
        this.onChanged = onChanged;

        webView.getStyleClass().add("rich-editor-webview");
        webView.setContextMenuEnabled(false);
        webView.setMinHeight(96);
        webView.setPrefHeight(160);
        webView.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(webView, Priority.ALWAYS);

        WebEngine engine = webView.getEngine();
        engine.getLoadWorker().stateProperty().addListener((observable, oldValue, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("cozyBridge", new Bridge());
                engine.executeScript("window.cozyInstallBridge && window.cozyInstallBridge();");
                loaded = true;
            }
        });
        engine.loadContent(htmlDocument(initialHtml()));

        getChildren().add(webView);
        getStyleClass().addAll("block-row", "text-block", "typing-workspace-row");
        BlockStyle.applyIndent(this, block);
    }

    public boolean ownsFocus() {
        return webView.isFocused() || Boolean.TRUE.equals(execute("document.activeElement && document.activeElement.id === 'editor'"));
    }

    public void applyBold() {
        executeEditorCommand("bold", null);
    }

    public void applyItalic() {
        executeEditorCommand("italic", null);
    }

    public void applyHighlight() {
        executeEditorCommand("backColor", "#fff3a3");
    }

    public void focusEditor() {
        execute("document.getElementById('editor').focus();");
        webView.requestFocus();
    }

    private void executeEditorCommand(String command, String value) {
        if (!loaded) {
            return;
        }
        String jsValue = value == null ? "null" : "'" + escapeJs(value) + "'";
        execute("window.cozyApplyCommand && window.cozyApplyCommand('" + command + "', " + jsValue + ");");
        syncFromDom();
        focusEditor();
    }

    private Object execute(String script) {
        try {
            return webView.getEngine().executeScript(script);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private void syncFromDom() {
        Object html = execute("document.getElementById('editor').innerHTML");
        Object text = execute("document.getElementById('editor').innerText");
        if (html instanceof String htmlValue) {
            block.setRichTextHtml(htmlValue);
        }
        if (text instanceof String textValue) {
            block.setText(textValue.replace("\r", ""));
        }
        onChanged.run();
    }

    private String initialHtml() {
        if (block.richTextHtml() != null && !block.richTextHtml().isBlank()) {
            return block.richTextHtml();
        }
        return escapeHtml(block.text()).replace("\n", "<br>");
    }

    private String htmlDocument(String body) {
        return """
                <!doctype html>
                <html>
                <head>
                <meta charset="UTF-8">
                <style>
                html, body {
                    margin: 0;
                    padding: 0;
                    background: #fffaf4;
                    color: #1f2933;
                    font-family: "Malgun Gothic", "Noto Sans KR", "Segoe UI", sans-serif;
                    font-size: 16px;
                    line-height: 1.65;
                    height: 100%%;
                }
                #editor {
                    box-sizing: border-box;
                    min-height: 128px;
                    width: 100%%;
                    padding: 18px 22px;
                    outline: none;
                    white-space: pre-wrap;
                    overflow-wrap: anywhere;
                    background: #fffaf4;
                }
                #editor:focus {
                    background: #eef6fb;
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
                function cozySaveSelection() {
                    const selection = window.getSelection();
                    if (selection && selection.rangeCount > 0) {
                        cozySavedRange = selection.getRangeAt(0).cloneRange();
                    }
                }
                function cozyRestoreSelection() {
                    const editor = document.getElementById('editor');
                    editor.focus();
                    if (!cozySavedRange) return;
                    const selection = window.getSelection();
                    selection.removeAllRanges();
                    selection.addRange(cozySavedRange);
                }
                function cozySync() {
                    cozySaveSelection();
                    if (window.cozyBridge) {
                        window.cozyBridge.changed(
                            document.getElementById('editor').innerHTML,
                            document.getElementById('editor').innerText
                        );
                    }
                }
                window.cozyApplyCommand = function(command, value) {
                    cozyRestoreSelection();
                    document.execCommand(command, false, value || null);
                    cozySync();
                };
                window.cozyInstallBridge = function() {
                    const editor = document.getElementById('editor');
                    editor.addEventListener('input', cozySync);
                    editor.addEventListener('keyup', cozySaveSelection);
                    editor.addEventListener('mouseup', cozySaveSelection);
                    editor.addEventListener('selectstart', cozySaveSelection);
                    editor.addEventListener('focus', cozySaveSelection);
                };
                </script>
                </body>
                </html>
                """.formatted(body);
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
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }

    public final class Bridge {
        public void changed(String html, String text) {
            block.setRichTextHtml(html == null ? "" : html);
            block.setText(text == null ? "" : text.replace("\r", ""));
            onChanged.run();
        }
    }
}

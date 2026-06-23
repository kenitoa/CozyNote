package com.cozynote.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.cozynote.domain.Category;
import com.cozynote.domain.BlockType;
import com.cozynote.domain.Note;
import com.cozynote.domain.NoteBlock;

public final class BackupService {
    public void exportJson(Path target, List<Note> notes, String settingsJson, String roomLayoutJson) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        builder.append("  \"version\": 1,\n");
        builder.append("  \"exportedAt\": \"").append(Instant.now()).append("\",\n");
        builder.append("  \"notes\": [\n");
        for (int i = 0; i < notes.size(); i++) {
            Note note = notes.get(i);
            if (i > 0) {
                builder.append(",\n");
            }
            builder.append("    {")
                    .append("\"id\":\"").append(escape(note.id())).append("\",")
                    .append("\"title\":\"").append(escape(note.title())).append("\",")
                    .append("\"category\":\"").append(escape(note.category().name())).append("\",")
                    .append("\"favorite\":").append(note.favorite()).append(',')
                    .append("\"pinned\":").append(note.pinned()).append(',')
                    .append("\"body\":\"").append(escape(note.body())).append("\",")
                    .append("\"blocks\":").append(blocksToJson(note.blocks()))
                    .append("}");
        }
        builder.append("\n  ],\n");
        builder.append("  \"categories\": [],\n");
        builder.append("  \"settings\": ").append(settingsJson == null ? "{}" : settingsJson).append(",\n");
        builder.append("  \"roomLayout\": ").append(roomLayoutJson == null ? "{}" : roomLayoutJson).append("\n");
        builder.append("}\n");
        Files.writeString(target, builder.toString());
    }

    public String readImportFile(Path source) throws IOException {
        return Files.readString(source);
    }

    public List<Note> readNotes(Path source) throws IOException {
        String json = readImportFile(source);
        String notesArray = arrayValue(json, "notes");
        if (notesArray.isBlank()) {
            return List.of();
        }
        List<Note> notes = new ArrayList<>();
        for (String object : splitObjects(notesArray)) {
            String id = jsonValue(object, "id", "");
            String title = jsonValue(object, "title", "제목 없음");
            String category = jsonValue(object, "category", "모든 메모");
            boolean favorite = Boolean.parseBoolean(jsonValue(object, "favorite", "false"));
            boolean pinned = Boolean.parseBoolean(jsonValue(object, "pinned", "false"));
            String body = jsonValue(object, "body", "");
            String blocksJson = arrayValue(object, "blocks");
            List<NoteBlock> blocks = blocksJson.isBlank()
                    ? Note.blocksFromBody(body)
                    : blocksFromJson("[" + blocksJson + "]");
            notes.add(new Note(id.isBlank() ? java.util.UUID.randomUUID().toString() : id,
                    title,
                    Note.bodyFromBlocks(blocks),
                    new Category(category, 0),
                    favorite,
                    pinned,
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    blocks));
        }
        return notes;
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private String blocksToJson(List<NoteBlock> blocks) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < blocks.size(); i++) {
            NoteBlock block = blocks.get(i);
            if (i > 0) {
                builder.append(',');
            }
            builder.append('{')
                    .append("\"id\":\"").append(escape(block.id())).append("\",")
                    .append("\"type\":\"").append(block.type()).append("\",")
                    .append("\"text\":\"").append(escape(block.text())).append("\",")
                    .append("\"richTextHtml\":\"").append(escape(block.richTextHtml())).append("\",")
                    .append("\"layoutX\":").append(block.layoutX()).append(',')
                    .append("\"layoutY\":").append(block.layoutY()).append(',')
                    .append("\"layoutWidth\":").append(block.layoutWidth()).append(',')
                    .append("\"layoutHeight\":").append(block.layoutHeight()).append(',')
                    .append("\"checked\":").append(block.checked()).append(',')
                    .append("\"bold\":").append(block.bold()).append(',')
                    .append("\"italic\":").append(block.italic()).append(',')
                    .append("\"underline\":").append(block.underline()).append(',')
                    .append("\"strike\":").append(block.strike()).append(',')
                    .append("\"subscript\":").append(block.subscript()).append(',')
                    .append("\"superscript\":").append(block.superscript()).append(',')
                    .append("\"indentLevel\":").append(block.indentLevel()).append(',')
                    .append("\"textColor\":\"").append(escape(block.textColor())).append("\",")
                    .append("\"highlightColor\":\"").append(escape(block.highlightColor())).append("\",")
                    .append("\"inlineStyles\":\"").append(escape(block.encodeInlineStyles())).append("\",")
                    .append("\"alignment\":\"").append(escape(block.alignment())).append("\",")
                    .append("\"orderIndex\":").append(i)
                    .append('}');
        }
        return builder.append(']').toString();
    }

    private List<NoteBlock> blocksFromJson(String json) {
        String body = json == null || json.length() < 2 ? "" : json.substring(1, json.length() - 1).trim();
        if (body.isBlank()) {
            return Note.blocksFromBody("");
        }
        List<NoteBlock> blocks = new ArrayList<>();
        for (String object : splitObjects(body)) {
            String id = jsonValue(object, "id", "");
            BlockType type = parseBlockType(jsonValue(object, "type", "TEXT"));
            String text = jsonValue(object, "text", "");
            String richTextHtml = jsonValue(object, "richTextHtml", "");
            double layoutX = parseDouble(jsonValue(object, "layoutX", "-1"), -1);
            double layoutY = parseDouble(jsonValue(object, "layoutY", "-1"), -1);
            double layoutWidth = parseDouble(jsonValue(object, "layoutWidth", "-1"), -1);
            double layoutHeight = parseDouble(jsonValue(object, "layoutHeight", "-1"), -1);
            boolean checked = Boolean.parseBoolean(jsonValue(object, "checked", "false"));
            boolean bold = Boolean.parseBoolean(jsonValue(object, "bold", "false"));
            boolean italic = Boolean.parseBoolean(jsonValue(object, "italic", "false"));
            boolean underline = Boolean.parseBoolean(jsonValue(object, "underline", "false"));
            boolean strike = Boolean.parseBoolean(jsonValue(object, "strike", "false"));
            boolean subscript = Boolean.parseBoolean(jsonValue(object, "subscript", "false"));
            boolean superscript = Boolean.parseBoolean(jsonValue(object, "superscript", "false"));
            int indentLevel = parseInt(jsonValue(object, "indentLevel", "0"), 0);
            String textColor = jsonValue(object, "textColor", "#1f2933");
            String highlightColor = jsonValue(object, "highlightColor", "transparent");
            String inlineStyles = jsonValue(object, "inlineStyles", "");
            String alignment = jsonValue(object, "alignment", "LEFT");
            int orderIndex = parseInt(jsonValue(object, "orderIndex", String.valueOf(blocks.size())), blocks.size());
            NoteBlock block = new NoteBlock(id.isBlank() ? java.util.UUID.randomUUID().toString() : id,
                    type, text, checked, orderIndex, bold, italic, underline, strike,
                    subscript, superscript, indentLevel, textColor, highlightColor, alignment);
            block.setRichTextHtml(richTextHtml);
            block.setLayout(layoutX, layoutY, layoutWidth, layoutHeight);
            block.setInlineStyles(NoteBlock.decodeInlineStyles(inlineStyles));
            blocks.add(block);
        }
        return blocks.isEmpty() ? Note.blocksFromBody("") : blocks;
    }

    private BlockType parseBlockType(String value) {
        try {
            return BlockType.valueOf(value);
        } catch (RuntimeException exception) {
            return BlockType.TEXT;
        }
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private double parseDouble(String value, double fallback) {
        try {
            return Double.parseDouble(value);
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private String arrayValue(String json, String key) {
        String pattern = "\"" + key + "\":";
        int start = json.indexOf(pattern);
        if (start < 0) {
            return "";
        }
        start = json.indexOf('[', start + pattern.length());
        if (start < 0) {
            return "";
        }
        int depth = 0;
        boolean inString = false;
        boolean escaping = false;
        for (int i = start; i < json.length(); i++) {
            char value = json.charAt(i);
            if (value == '"' && !escaping) {
                inString = !inString;
            }
            if (!inString) {
                if (value == '[') {
                    depth++;
                } else if (value == ']') {
                    depth--;
                    if (depth == 0) {
                        return json.substring(start + 1, i);
                    }
                }
            }
            escaping = value == '\\' && !escaping;
            if (value != '\\') {
                escaping = false;
            }
        }
        return "";
    }

    private List<String> splitObjects(String arrayBody) {
        List<String> objects = new ArrayList<>();
        int start = -1;
        int depth = 0;
        boolean inString = false;
        boolean escaping = false;
        for (int i = 0; i < arrayBody.length(); i++) {
            char value = arrayBody.charAt(i);
            if (value == '"' && !escaping) {
                inString = !inString;
            }
            if (!inString) {
                if (value == '{') {
                    if (depth == 0) {
                        start = i;
                    }
                    depth++;
                } else if (value == '}') {
                    depth--;
                    if (depth == 0 && start >= 0) {
                        objects.add(arrayBody.substring(start, i + 1));
                        start = -1;
                    }
                }
            }
            escaping = value == '\\' && !escaping;
            if (value != '\\') {
                escaping = false;
            }
        }
        return objects;
    }

    private String jsonValue(String object, String key, String fallback) {
        String pattern = "\"" + key + "\":";
        int start = object.indexOf(pattern);
        if (start < 0) {
            return fallback;
        }
        start += pattern.length();
        if (start >= object.length()) {
            return fallback;
        }
        if (object.charAt(start) == '"') {
            int end = start + 1;
            boolean escaping = false;
            while (end < object.length()) {
                char value = object.charAt(end);
                if (value == '"' && !escaping) {
                    break;
                }
                escaping = value == '\\' && !escaping;
                if (value != '\\') {
                    escaping = false;
                }
                end++;
            }
            return unescape(object.substring(start + 1, Math.min(end, object.length())));
        }
        int end = object.indexOf(',', start);
        if (end < 0) {
            end = object.indexOf('}', start);
        }
        return end < 0 ? fallback : object.substring(start, end).trim();
    }

    private String unescape(String value) {
        return value.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
    }
}

package com.cozynote.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class NoteBlock {
    private final String id;
    private BlockType type;
    private String text;
    private boolean checked;
    private int orderIndex;
    private boolean bold;
    private boolean italic;
    private boolean underline;
    private boolean strike;
    private boolean subscript;
    private boolean superscript;
    private int indentLevel;
    private String textColor = "#1f2933";
    private String highlightColor = "transparent";
    private String alignment = "LEFT";
    private String richTextHtml = "";
    private double layoutX = -1;
    private double layoutY = -1;
    private double layoutWidth = -1;
    private double layoutHeight = -1;
    private final List<InlineStyleRange> inlineStyles = new ArrayList<>();

    public NoteBlock(BlockType type, String text, int orderIndex) {
        this(UUID.randomUUID().toString(), type, text, false, orderIndex);
    }

    public NoteBlock(String id, BlockType type, String text, boolean checked, int orderIndex) {
        this(id, type, text, checked, orderIndex, false, false, false, "LEFT");
    }

    public NoteBlock(String id, BlockType type, String text, boolean checked, int orderIndex,
            boolean bold, boolean italic, boolean underline, String alignment) {
        this(id, type, text, checked, orderIndex, bold, italic, underline, false, false, false, 0,
                "#1f2933", "transparent", alignment);
    }

    public NoteBlock(String id, BlockType type, String text, boolean checked, int orderIndex,
            boolean bold, boolean italic, boolean underline, boolean strike, boolean subscript,
            boolean superscript, int indentLevel, String textColor, String highlightColor, String alignment) {
        this.id = id;
        this.type = type;
        this.text = text;
        this.checked = checked;
        this.orderIndex = orderIndex;
        this.bold = bold;
        this.italic = italic;
        this.underline = underline;
        this.strike = strike;
        this.subscript = subscript;
        this.superscript = superscript;
        this.indentLevel = Math.max(0, indentLevel);
        this.textColor = textColor == null || textColor.isBlank() ? "#1f2933" : textColor;
        this.highlightColor = highlightColor == null || highlightColor.isBlank() ? "transparent" : highlightColor;
        this.alignment = alignment == null || alignment.isBlank() ? "LEFT" : alignment;
    }

    public String id() {
        return id;
    }

    public BlockType type() {
        return type;
    }

    public void setType(BlockType type) {
        this.type = type;
    }

    public String text() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
        trimInlineStyles();
    }

    public boolean checked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public int orderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }

    public boolean bold() {
        return bold;
    }

    public void setBold(boolean bold) {
        this.bold = bold;
    }

    public boolean italic() {
        return italic;
    }

    public void setItalic(boolean italic) {
        this.italic = italic;
    }

    public boolean underline() {
        return underline;
    }

    public void setUnderline(boolean underline) {
        this.underline = underline;
    }

    public boolean strike() {
        return strike;
    }

    public void setStrike(boolean strike) {
        this.strike = strike;
    }

    public boolean subscript() {
        return subscript;
    }

    public void setSubscript(boolean subscript) {
        this.subscript = subscript;
        if (subscript) {
            superscript = false;
        }
    }

    public boolean superscript() {
        return superscript;
    }

    public void setSuperscript(boolean superscript) {
        this.superscript = superscript;
        if (superscript) {
            subscript = false;
        }
    }

    public int indentLevel() {
        return indentLevel;
    }

    public void setIndentLevel(int indentLevel) {
        this.indentLevel = Math.max(0, Math.min(6, indentLevel));
    }

    public String textColor() {
        return textColor;
    }

    public void setTextColor(String textColor) {
        this.textColor = textColor == null || textColor.isBlank() ? "#1f2933" : textColor;
    }

    public String highlightColor() {
        return highlightColor;
    }

    public void setHighlightColor(String highlightColor) {
        this.highlightColor = highlightColor == null || highlightColor.isBlank() ? "transparent" : highlightColor;
    }

    public String alignment() {
        return alignment;
    }

    public void setAlignment(String alignment) {
        this.alignment = alignment == null || alignment.isBlank() ? "LEFT" : alignment;
    }

    public String richTextHtml() {
        return richTextHtml;
    }

    public void setRichTextHtml(String richTextHtml) {
        this.richTextHtml = richTextHtml == null ? "" : richTextHtml;
    }

    public double layoutX() {
        return layoutX;
    }

    public void setLayoutX(double layoutX) {
        this.layoutX = layoutX;
    }

    public double layoutY() {
        return layoutY;
    }

    public void setLayoutY(double layoutY) {
        this.layoutY = layoutY;
    }

    public double layoutWidth() {
        return layoutWidth;
    }

    public void setLayoutWidth(double layoutWidth) {
        this.layoutWidth = layoutWidth;
    }

    public double layoutHeight() {
        return layoutHeight;
    }

    public void setLayoutHeight(double layoutHeight) {
        this.layoutHeight = layoutHeight;
    }

    public void setLayout(double x, double y, double width, double height) {
        layoutX = x;
        layoutY = y;
        layoutWidth = width;
        layoutHeight = height;
    }

    public List<InlineStyleRange> inlineStyles() {
        return inlineStyles;
    }

    public void setInlineStyles(List<InlineStyleRange> ranges) {
        inlineStyles.clear();
        if (ranges != null) {
            inlineStyles.addAll(ranges.stream()
                    .filter(range -> range.end() > range.start())
                    .toList());
        }
        trimInlineStyles();
    }

    public void addInlineStyle(int start, int end, boolean bold, boolean italic, String highlightColor) {
        int safeStart = Math.max(0, Math.min(start, text == null ? 0 : text.length()));
        int safeEnd = Math.max(safeStart, Math.min(end, text == null ? 0 : text.length()));
        if (safeEnd <= safeStart) {
            return;
        }
        inlineStyles.add(new InlineStyleRange(safeStart, safeEnd, bold, italic,
                highlightColor == null || highlightColor.isBlank() ? "transparent" : highlightColor));
    }

    public String encodeInlineStyles() {
        StringBuilder builder = new StringBuilder();
        for (InlineStyleRange range : inlineStyles) {
            if (builder.length() > 0) {
                builder.append(';');
            }
            builder.append(range.start()).append(':')
                    .append(range.end()).append(':')
                    .append(range.bold() ? '1' : '0').append(':')
                    .append(range.italic() ? '1' : '0').append(':')
                    .append(range.highlightColor().replace(";", "").replace(":", ""));
        }
        return builder.toString();
    }

    public static List<InlineStyleRange> decodeInlineStyles(String encoded) {
        List<InlineStyleRange> ranges = new ArrayList<>();
        if (encoded == null || encoded.isBlank()) {
            return ranges;
        }
        for (String item : encoded.split(";")) {
            String[] parts = item.split(":", 5);
            if (parts.length < 5) {
                continue;
            }
            try {
                ranges.add(new InlineStyleRange(
                        Integer.parseInt(parts[0]),
                        Integer.parseInt(parts[1]),
                        "1".equals(parts[2]),
                        "1".equals(parts[3]),
                        parts[4].isBlank() ? "transparent" : parts[4]));
            } catch (NumberFormatException ignored) {
                // Ignore malformed legacy style ranges.
            }
        }
        return ranges;
    }

    private void trimInlineStyles() {
        int length = text == null ? 0 : text.length();
        inlineStyles.removeIf(range -> range.start() >= length || range.end() <= range.start());
        for (int i = 0; i < inlineStyles.size(); i++) {
            InlineStyleRange range = inlineStyles.get(i);
            if (range.end() > length) {
                inlineStyles.set(i, new InlineStyleRange(range.start(), length,
                        range.bold(), range.italic(), range.highlightColor()));
            }
        }
    }

    public record InlineStyleRange(int start, int end, boolean bold, boolean italic, String highlightColor) {
    }
}

package com.cozynote.service;

import java.util.List;

import com.cozynote.domain.Note;
import com.cozynote.domain.NoteBlock;

public record DocumentStats(int characterCount, int noSpaceCount, int lineCount) {
    public static DocumentStats from(String text) {
        String value = text == null ? "" : text;
        int lines = value.isEmpty() ? 1 : value.split("\\R", -1).length;
        return new DocumentStats(value.length(), value.replaceAll("\\s+", "").length(), lines);
    }

    public static DocumentStats fromBlocks(List<NoteBlock> blocks) {
        return from(Note.bodyFromBlocks(blocks));
    }
}

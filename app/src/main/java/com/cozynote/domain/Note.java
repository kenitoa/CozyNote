package com.cozynote.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class Note {
    private final String id;
    private String title;
    private String body;
    private Category category;
    private boolean favorite;
    private boolean pinned;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private final List<NoteBlock> blocks;

    public Note(String title, String body, Category category, boolean favorite) {
        this(UUID.randomUUID().toString(), title, body, category, favorite, false, LocalDateTime.now(),
                LocalDateTime.now(), blocksFromBody(body));
    }

    public Note(String id, String title, String body, Category category, boolean favorite, LocalDateTime updatedAt) {
        this(id, title, body, category, favorite, false, updatedAt, updatedAt, blocksFromBody(body));
    }

    public Note(String id, String title, String body, Category category, boolean favorite, boolean pinned,
            LocalDateTime createdAt, LocalDateTime updatedAt, List<NoteBlock> blocks) {
        this.id = id;
        this.title = title;
        this.body = body == null ? "" : body;
        this.category = category;
        this.favorite = favorite;
        this.pinned = pinned;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.blocks = new ArrayList<>(blocks);
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        touch();
    }

    public String body() {
        return body;
    }

    public void setBody(String body) {
        this.body = body == null ? "" : body;
        replaceBlocks(blocksFromBody(this.body));
    }

    public Category category() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
        touch();
    }

    public boolean favorite() {
        return favorite;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
        touch();
    }

    public boolean pinned() {
        return pinned;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
        touch();
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public LocalDateTime updatedAt() {
        return updatedAt;
    }

    public List<NoteBlock> blocks() {
        return blocks;
    }

    public void replaceBlocks(List<NoteBlock> newBlocks) {
        List<NoteBlock> copy = new ArrayList<>(newBlocks);
        blocks.clear();
        blocks.addAll(copy);
        body = bodyFromBlocks(blocks);
        touch();
    }

    public void refreshBodyFromBlocks() {
        body = bodyFromBlocks(blocks);
        touch();
    }

    private void touch() {
        updatedAt = LocalDateTime.now();
    }

    public static List<NoteBlock> blocksFromBody(String body) {
        List<NoteBlock> result = new ArrayList<>();
        String[] lines = (body == null || body.isBlank() ? "" : body).split("\\R", -1);
        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }
            BlockType type = BlockType.TEXT;
            String text = line;
            boolean checked = false;
            if (line.startsWith("# ")) {
                type = BlockType.HEADING;
                text = line.substring(2);
            } else if (line.startsWith("□ ") || line.startsWith("[ ] ")) {
                type = BlockType.TODO;
                text = line.substring(line.indexOf(' ') + 1);
            } else if (line.startsWith("☑ ") || line.startsWith("[x] ")) {
                type = BlockType.TODO;
                checked = true;
                text = line.substring(line.indexOf(' ') + 1);
            } else if (line.startsWith("- ")) {
                type = BlockType.BULLET;
                text = line.substring(2);
            } else if (line.startsWith("> ")) {
                type = BlockType.QUOTE;
                text = line.substring(2);
            } else if (line.matches("-{3,}")) {
                type = BlockType.DIVIDER;
                text = "";
            }
            result.add(new NoteBlock(UUID.randomUUID().toString(), type, text, checked, result.size()));
        }
        if (result.isEmpty()) {
            result.add(new NoteBlock(BlockType.TEXT, "", 0));
        }
        return result;
    }

    public static String bodyFromBlocks(List<NoteBlock> blocks) {
        StringBuilder builder = new StringBuilder();
        for (NoteBlock block : blocks) {
            if (builder.length() > 0) {
                builder.append(System.lineSeparator());
            }
            switch (block.type()) {
                case HEADING -> builder.append("# ").append(block.text());
                case TODO -> builder.append(block.checked() ? "☑ " : "□ ").append(block.text());
                case BULLET -> builder.append("- ").append(block.text());
                case NUMBERED -> builder.append(block.orderIndex() + 1).append(". ").append(block.text());
                case QUOTE -> builder.append("> ").append(block.text());
                case TABLE -> builder.append(block.text());
                case CHART -> builder.append(block.text());
                case IMAGE -> builder.append("[이미지] ").append(block.text());
                case LINK -> builder.append("[링크] ").append(block.text());
                case ATTACHMENT -> builder.append("[첨부] ").append(block.text());
                case AUDIO -> builder.append("[오디오] ").append(block.text());
                case DIVIDER -> builder.append("------------------------");
                case TEXT -> builder.append(block.text());
            }
        }
        return builder.toString();
    }
}

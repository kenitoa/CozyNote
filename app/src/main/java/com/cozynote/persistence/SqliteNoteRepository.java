package com.cozynote.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.cozynote.domain.BlockType;
import com.cozynote.domain.Category;
import com.cozynote.domain.Note;
import com.cozynote.domain.NoteBlock;

public final class SqliteNoteRepository implements NoteRepository {
    private final Path databasePath;
    private final Object schemaLock = new Object();
    private volatile boolean schemaReady;

    public SqliteNoteRepository(Path databasePath) {
        this.databasePath = databasePath;
    }

    @Override
    public List<Note> findAll() throws IOException {
        try (Connection connection = connect();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("""
                        SELECT id, title, category_id, content_json, favorite, pinned, created_at, updated_at
                        FROM notes
                        ORDER BY pinned DESC, updated_at DESC
                        """)) {
            List<Note> notes = new ArrayList<>();
            while (resultSet.next()) {
                notes.add(mapNote(resultSet));
            }
            return notes;
        } catch (RuntimeException | SQLException exception) {
            throw new IOException("SQLite 메모 목록을 읽지 못했습니다: " + exception.getMessage(), exception);
        }
    }

    @Override
    public Optional<Note> findById(String id) throws IOException {
        try (Connection connection = connect();
                PreparedStatement statement = connection.prepareStatement("""
                        SELECT id, title, category_id, content_json, favorite, pinned, created_at, updated_at
                        FROM notes
                        WHERE id = ?
                        """)) {
            statement.setString(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapNote(resultSet)) : Optional.empty();
            }
        } catch (RuntimeException | SQLException exception) {
            throw new IOException("SQLite 메모를 읽지 못했습니다: " + exception.getMessage(), exception);
        }
    }

    @Override
    public void save(Note note) throws IOException {
        try (Connection connection = connect();
                PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO notes (
                            id, title, category_id, content_json, favorite, pinned, created_at, updated_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT(id) DO UPDATE SET
                            title = excluded.title,
                            category_id = excluded.category_id,
                            content_json = excluded.content_json,
                            favorite = excluded.favorite,
                            pinned = excluded.pinned,
                            updated_at = excluded.updated_at
                        """)) {
            statement.setString(1, note.id());
            statement.setString(2, note.title());
            statement.setString(3, note.category().name());
            statement.setString(4, blocksToJson(note.blocks()));
            statement.setInt(5, note.favorite() ? 1 : 0);
            statement.setInt(6, note.pinned() ? 1 : 0);
            statement.setString(7, note.createdAt().toString());
            statement.setString(8, note.updatedAt().toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IOException("SQLite 메모를 저장하지 못했습니다: " + exception.getMessage(), exception);
        }
    }

    @Override
    public void deleteById(String id) throws IOException {
        try (Connection connection = connect();
                PreparedStatement statement = connection.prepareStatement("DELETE FROM notes WHERE id = ?")) {
            statement.setString(1, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IOException("SQLite 메모를 삭제하지 못했습니다: " + exception.getMessage(), exception);
        }
    }

    public List<Category> findCategories() throws IOException {
        try (Connection connection = connect();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("""
                        SELECT c.name, c.level, COUNT(n.id) AS note_count
                        FROM categories c
                        LEFT JOIN notes n ON n.category_id = c.name
                        GROUP BY c.id, c.name, c.level, c.sort_order
                        ORDER BY c.sort_order, c.name
                        """)) {
            List<Category> categories = new ArrayList<>();
            while (resultSet.next()) {
                categories.add(new Category(
                        resultSet.getString("name"),
                        resultSet.getInt("note_count"),
                        resultSet.getInt("level")));
            }
            return categories;
        } catch (SQLException exception) {
            throw new IOException("SQLite 카테고리를 읽지 못했습니다: " + exception.getMessage(), exception);
        }
    }

    public void saveCategory(Category category, int sortOrder) throws IOException {
        try (Connection connection = connect();
                PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO categories (id, name, level, sort_order)
                        VALUES (?, ?, ?, ?)
                        ON CONFLICT(id) DO UPDATE SET
                            name = excluded.name,
                            level = excluded.level,
                            sort_order = excluded.sort_order
                        """)) {
            statement.setString(1, category.name());
            statement.setString(2, category.name());
            statement.setInt(3, category.level());
            statement.setInt(4, sortOrder);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IOException("SQLite 카테고리를 저장하지 못했습니다: " + exception.getMessage(), exception);
        }
    }

    public void deleteCategory(String name) throws IOException {
        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            try (PreparedStatement updateNotes =
                    connection.prepareStatement("UPDATE notes SET category_id = ? WHERE category_id = ?");
                    PreparedStatement deleteCategory =
                            connection.prepareStatement("DELETE FROM categories WHERE name = ?")) {
                updateNotes.setString(1, "모든 메모");
                updateNotes.setString(2, name);
                updateNotes.executeUpdate();
                deleteCategory.setString(1, name);
                deleteCategory.executeUpdate();
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new IOException("SQLite 카테고리를 삭제하지 못했습니다: " + exception.getMessage(), exception);
        }
    }

    public String getSetting(String key, String fallback) throws IOException {
        return readKeyValue("app_settings", key, fallback);
    }

    public void saveSetting(String key, String value) throws IOException {
        saveKeyValue("app_settings", key, value);
    }

    public String getProgress(String key, String fallback) throws IOException {
        return readKeyValue("user_progress", key, fallback);
    }

    public void saveProgress(String key, String value) throws IOException {
        saveKeyValue("user_progress", key, value);
    }

    private Connection connect() throws SQLException, IOException {
        Files.createDirectories(databasePath.getParent());
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
        ensureSchemaInitialized(connection);
        return connection;
    }

    private void ensureSchemaInitialized(Connection connection) throws SQLException {
        if (schemaReady) {
            return;
        }
        synchronized (schemaLock) {
            if (schemaReady) {
                return;
            }
            ensureSchema(connection);
            schemaReady = true;
        }
    }

    private void ensureSchema(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS notes (
                        id TEXT PRIMARY KEY,
                        title TEXT NOT NULL,
                        category_id TEXT,
                        content_json TEXT NOT NULL DEFAULT '[]',
                        favorite INTEGER NOT NULL DEFAULT 0,
                        pinned INTEGER NOT NULL DEFAULT 0,
                        created_at TEXT,
                        updated_at TEXT
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS categories (
                        id TEXT PRIMARY KEY,
                        name TEXT NOT NULL,
                        icon TEXT,
                        color TEXT,
                        level INTEGER NOT NULL DEFAULT 0,
                        sort_order INTEGER NOT NULL DEFAULT 0
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS app_settings (
                        key TEXT PRIMARY KEY,
                        value TEXT NOT NULL
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS user_progress (
                        key TEXT PRIMARY KEY,
                        value TEXT NOT NULL
                    )
                    """);
        }
        ensureColumn(connection, "notes", "category_id", "TEXT");
        ensureColumn(connection, "notes", "content_json", "TEXT NOT NULL DEFAULT '[]'");
        ensureColumn(connection, "notes", "favorite", "INTEGER NOT NULL DEFAULT 0");
        ensureColumn(connection, "notes", "pinned", "INTEGER NOT NULL DEFAULT 0");
        ensureColumn(connection, "notes", "created_at", "TEXT");
        ensureColumn(connection, "notes", "updated_at", "TEXT");
        ensureColumn(connection, "categories", "level", "INTEGER NOT NULL DEFAULT 0");
        backfillNotes(connection);
        seedDefaultCategories(connection);
    }

    private void ensureColumn(Connection connection, String tableName, String columnName, String definition)
            throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("PRAGMA table_info(" + tableName + ")")) {
            while (resultSet.next()) {
                if (columnName.equalsIgnoreCase(resultSet.getString("name"))) {
                    return;
                }
            }
        }
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
        }
    }

    private void backfillNotes(Connection connection) throws SQLException {
        String now = LocalDateTime.now().toString();
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE notes
                SET content_json = COALESCE(content_json, '[]'),
                    favorite = COALESCE(favorite, 0),
                    pinned = COALESCE(pinned, 0),
                    created_at = COALESCE(created_at, ?),
                    updated_at = COALESCE(updated_at, ?)
                """)) {
            statement.setString(1, now);
            statement.setString(2, now);
            statement.executeUpdate();
        }
    }

    private void seedDefaultCategories(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE notes SET category_id = '모든 메모' WHERE category_id IS NULL OR category_id = '' OR category_id = '일상'");
            statement.executeUpdate("DELETE FROM categories WHERE id = '일상' OR name = '일상'");
        }
        boolean hasAnyCategory = false;
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM categories")) {
            hasAnyCategory = resultSet.next() && resultSet.getInt(1) > 0;
        }
        if (hasAnyCategory) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT OR IGNORE INTO categories (id, name, level, sort_order)
                    VALUES (?, ?, 0, 0)
                    """)) {
                statement.setString(1, "모든 메모");
                statement.setString(2, "모든 메모");
                statement.executeUpdate();
            }
            return;
        }
        String[] defaults = {"모든 메모", "업무", "아이디어", "독서", "여행", "기타"};
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT OR IGNORE INTO categories (id, name, level, sort_order)
                VALUES (?, ?, 0, ?)
                """)) {
            for (int i = 0; i < defaults.length; i++) {
                statement.setString(1, defaults[i]);
                statement.setString(2, defaults[i]);
                statement.setInt(3, i);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private String readKeyValue(String tableName, String key, String fallback) throws IOException {
        try (Connection connection = connect();
                PreparedStatement statement =
                        connection.prepareStatement("SELECT value FROM " + tableName + " WHERE key = ?")) {
            statement.setString(1, key);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getString("value") : fallback;
            }
        } catch (SQLException exception) {
            throw new IOException("설정 값을 읽지 못했습니다: " + exception.getMessage(), exception);
        }
    }

    private void saveKeyValue(String tableName, String key, String value) throws IOException {
        try (Connection connection = connect();
                PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO %s (key, value)
                        VALUES (?, ?)
                        ON CONFLICT(key) DO UPDATE SET value = excluded.value
                        """.formatted(tableName))) {
            statement.setString(1, key);
            statement.setString(2, value);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IOException("설정 값을 저장하지 못했습니다: " + exception.getMessage(), exception);
        }
    }

    private Note mapNote(ResultSet resultSet) throws SQLException {
        List<NoteBlock> blocks = blocksFromJson(resultSet.getString("content_json"));
        return new Note(resultSet.getString("id"),
                resultSet.getString("title"),
                Note.bodyFromBlocks(blocks),
                new Category(valueOrDefault(resultSet.getString("category_id"), "모든 메모"), 0),
                resultSet.getInt("favorite") == 1,
                resultSet.getInt("pinned") == 1,
                parseDate(resultSet.getString("created_at")),
                parseDate(resultSet.getString("updated_at")),
                blocks);
    }

    private LocalDateTime parseDate(String value) {
        if (value == null || value.isBlank()) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(value);
        } catch (RuntimeException exception) {
            return LocalDateTime.now();
        }
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
        if (json == null || json.isBlank()) {
            return Note.blocksFromBody("");
        }
        List<NoteBlock> blocks = new ArrayList<>();
        String trimmed = json.trim();
        if (trimmed.length() < 2 || !trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            return Note.blocksFromBody("");
        }
        String body = trimmed.substring(1, trimmed.length() - 1).trim();
        if (body.isEmpty()) {
            return Note.blocksFromBody("");
        }
        String[] objects = body.split("(?<=\\}),\\s*(?=\\{)");
        for (String object : objects) {
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
            NoteBlock block = new NoteBlock(id, type, text, checked, orderIndex, bold, italic, underline, strike,
                    subscript, superscript, indentLevel, textColor, highlightColor, alignment);
            block.setRichTextHtml(richTextHtml);
            block.setLayout(layoutX, layoutY, layoutWidth, layoutHeight);
            block.setInlineStyles(NoteBlock.decodeInlineStyles(inlineStyles));
            blocks.add(block);
        }
        return blocks;
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

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private String unescape(String value) {
        return value.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
    }
}

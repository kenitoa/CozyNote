package com.cozynote.ui.editor;

import java.util.function.Consumer;

import com.cozynote.domain.BlockType;
import com.cozynote.domain.NoteBlock;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.MouseEvent;

public final class BlockViewFactory {
    private BlockViewFactory() {
    }

    public static Node create(NoteBlock block, Runnable onChanged, Consumer<NoteBlock> onSelected) {
        Node view = switch (block.type()) {
            case HEADING -> new HeadingBlockView(block, onChanged);
            case TODO -> new TodoBlockView(block, onChanged);
            case BULLET -> new BulletBlockView(block, onChanged);
            case NUMBERED -> new NumberedBlockView(block, onChanged);
            case QUOTE -> new QuoteBlockView(block, onChanged);
            case TABLE -> new TableBlockView(block, onChanged);
            case CHART -> new ChartBlockView(block, onChanged);
            case IMAGE, LINK, ATTACHMENT, AUDIO -> new ResourceBlockView(block, onChanged);
            case DIVIDER -> new DividerBlockView();
            case TEXT -> new TextBlockView(block, onChanged);
        };
        stampBlock(view, block);
        view.setOnMousePressed(event -> {
            if (!isTextInputTarget(event)) {
                onSelected.accept(block);
            }
        });
        return view;
    }

    private static void stampBlock(Node node, NoteBlock block) {
        node.getProperties().put("noteBlock", block);
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                stampBlock(child, block);
            }
        }
    }

    private static boolean isTextInputTarget(MouseEvent event) {
        if (!(event.getTarget() instanceof Node node)) {
            return false;
        }
        while (node != null) {
            if (node instanceof TextInputControl) {
                return true;
            }
            node = node.getParent();
        }
        return false;
    }

    public static NoteBlock emptyBlock(BlockType type, int orderIndex) {
        String text = switch (type) {
            case HEADING -> "새 제목";
            case TODO -> "새 할 일";
            case BULLET -> "새 항목";
            case NUMBERED -> "새 번호 항목";
            case QUOTE -> "인용";
            case TABLE -> "항목\t값\n\t";
            case CHART -> "기획=4\n회의=2\n검토=3";
            case IMAGE -> "";
            case LINK -> "https://";
            case ATTACHMENT -> "";
            case AUDIO -> "음성 메모";
            case DIVIDER -> "";
            case TEXT -> "";
        };
        return new NoteBlock(type, text, orderIndex);
    }
}

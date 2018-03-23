package treeml;

import java.util.List;

public interface Listener {

    default void onStart() {
    }

    default void onEnd() {
    }

    default void onCharacter(char c, int indent, int index, int line, int lineIndex, Parser2.Group currentGroup) {
    }

    default void onTag(String tag, int index, int line, int lineIndex, Parser2.Group currentGroup) {
    }

    default void onAddNode(String name, int indent, int index, int line, int lineIndex, Parser2.Group currentGroup) {
    }

    default void onAddValue(Object value, int indent, int index, int line, int lineIndex, Parser2.Group currentGroup) {
    }

}

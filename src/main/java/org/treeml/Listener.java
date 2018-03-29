package org.treeml;

public interface Listener {

    default void onStart() {
    }

    default void onEnd() {
    }

    default void onCharacter(char c, int indent, int index, int line, int lineIndex, Parser2.Group currentGroup) {
    }

    default void onTag(String tag, int index, int line, int lineIndex, Parser2.Group currentGroup) {
    }

    default void onAddNode(int indent, int index, int line, int lineIndex, Parser2.Group currentGroup) {
    }

    default void onAddValue(Object value, Parser2.Types type, int indent, int index, int line, int lineIndex, Parser2.Group currentGroup) {
    }

    default void onDeclareList(int indent, int index, int line, int lineIndex, Parser2.Group currentGroup) {
    }

    default void onNodeName(String name, int indent, int index, int lineNumber, int lineIndex, Parser2.Group name1) {

    }
}

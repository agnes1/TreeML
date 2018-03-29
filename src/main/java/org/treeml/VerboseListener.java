package org.treeml;

public class VerboseListener implements Listener {

    @Override
    public void onTag(String tag, int index, int line, int lineIndex, Parser2.Group currentGroup) {
        System.out.println("#" + tag);
    }

    @Override
    public void onAddNode(int indent, int index, int line, int lineIndex, Parser2.Group currentGroup) {
        System.out.println("Add node at indent [" + indent + "]");
    }

    @Override
    public void onAddValue(Object value, Parser2.Types type, int indent, int index, int line, int lineIndex, Parser2.Group currentGroup) {
        System.out.println("Add value: " + value + " [" + indent + "]");
    }

    @Override
    public void onStart() {

    }

    @Override
    public void onDeclareList(int indent, int index, int line, int lineIndex, Parser2.Group currentGroup) {
        System.out.println("Declare list");
    }

    @Override
    public void onNodeName(String name, int indent, int index, int lineNumber, int lineIndex, Parser2.Group name1) {
        System.out.println("Strat node: " + name);
    }
}

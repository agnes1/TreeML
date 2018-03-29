package org.treeml;

import java.util.concurrent.atomic.AtomicBoolean;

public class DomListener implements Listener {

    private RootNode root;
    private Node currentNode;
    private AtomicBoolean running = new AtomicBoolean(false);

    @SuppressWarnings("unused")
    public RootNode getDocument() {
        return root;
    }

    @Override
    public void onStart() {
        if (running.getAndSet(true)) {
            throw new IllegalStateException("Listener can only handle one document at a time. Create one parser and DomListener for each document.");
        }
        root = new RootNode();
    }

    @Override
    public void onEnd() {
        running.set(false);
    }

    @Override
    public void onTag(String tag, int index, int line, int lineIndex, Parser2.Group currentGroup) {
        root.tags.add(tag);
    }

    @Override
    public void onAddNode(int indent, int index, int line, int lineIndex, Parser2.Group currentGroup) {
        root.append(indent, currentNode);
        currentNode = null;
    }

    @Override
    public void onAddValue(Object value, Parser2.Types type, int indent, int index, int line, int lineIndex, Parser2.Group currentGroup) {
        currentNode.addValue(value);
    }

    @Override
    public void onDeclareList(int indent, int index, int line, int lineIndex, Parser2.Group currentGroup) {

    }

    @Override
    public void onNodeName(String name, int indent, int index, int lineNumber, int lineIndex, Parser2.Group name1) {
        currentNode = new Node(name, null);
    }
}

package org.treeml;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("WeakerAccess")
public class DomListener implements Listener {

    private RootNode root;
    private Node currentNode;
    private AtomicBoolean running = new AtomicBoolean(false);
    private boolean valueSet;

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
        valueSet = false;
    }

    @Override
    public void onAddValue(Object value, Parser2.Types type, int indent, int index, int line, int lineIndex, Parser2.Group currentGroup) {
        valueSet = true;
        if (currentNode.value != null && currentNode.value instanceof List<?>) {
            //noinspection unchecked
            ((List<Object>) currentNode.value).add(value);
        } else {
            currentNode.value = value;
        }
    }

    @Override
    public void onDeclareList(int indent, int index, int line, int lineIndex, Parser2.Group currentGroup) {
        //noinspection StatementWithEmptyBody
        if (currentNode.value != null && currentNode.value instanceof List<?>) {
            //do nothing
        } else {
            List<Object> list = new ArrayList<>();
            if (valueSet) {
                list.add(currentNode.value);
            }
            currentNode.value = list;
        }
    }

    @Override
    public void onNodeName(String name, int indent, int index, int lineNumber, int lineIndex, Parser2.Group name1) {
        currentNode = new Node(name, null);
        currentNode.line = lineNumber;
    }
}

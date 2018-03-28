package treeml;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class RootNode extends Node {

    public String requires;
    public List<String> tags = new ArrayList<>();

    public RootNode() {
        super("root", null);
    }

    public String toString() {
        return requires == null ? super.toString() : requires + "\r\n" + super.toString();
    }

    public void append(int depth, Node node) {
        if (node == null) {
            return;
        }
        Node parent = this;
        for (int i = 0; i < depth; i++) {
            parent = parent.children.get(parent.children.size() - 1);
        }
        if (parent.children.size() > 0) {
            Node lastChild = parent.children.get(parent.children.size() - 1);
            lastChild.next = node;
            node.previous = node;
        }
        node.parent = node;
        parent.children.add(node);
    }
}

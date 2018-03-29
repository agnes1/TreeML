package org.treeml;

import java.util.ArrayList;
import java.util.List;

/**
 * A org.treeml file is parsed into nodes.
 * Each node is on a separate line, and each line has a node
 * with the exception of comment lines.
 * Created by Ags on 6/26/2016.
 */
@SuppressWarnings("WeakerAccess")
public class Node {
    public String name;
    public Object value;
    public Node parent;
    public Node previous;
    public Node next;
    public List<Node> children = new ArrayList<>();
    public int line;

    public Node(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        toStringHelper(sb, this, "");
        return sb.toString();
    }

    private void toStringHelper(StringBuilder sb, Node node, String indent) {
        sb.append(indent).append(node.name).append("---").append(node.value).append("\r\n");
        for (Node child : node.children) {
            toStringHelper(sb, child, indent + "  ");
        }
    }
}
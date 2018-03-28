package treeml;

import java.util.ArrayList;
import java.util.List;

/**
 * A treeml file is parsed into nodes.
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
    private int valueSets = 0;

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

    public List<Node> getNodes(String name) {
        List<Node> result = new ArrayList<>();
        for (Node aChildren : children) {
            if (name.equals(aChildren.name)) {
                result.add(aChildren);
            }
        }
        return result;
    }

    public <T> T getValueAt(String name, T defaultValue) {
        T t = this.getValueAt(name);
        return t == null ? defaultValue : t;
    }

    public <T> T getValueAt(String name) {
        String[] steps = name.split("\\.");
        Node node = this;
        for (String step : steps) {
            List<Node> nodes = node.getNodes(step);
            if (nodes.isEmpty()) return null;
            node = nodes.get(0);
        }
        //noinspection unchecked
        return (T) node.value;
    }

    public Node getNode(String nameForNode) {
        for (int i = 0; i < children.size(); i++) {
            if (nameForNode.equals(children.get(i).name)) {
                return children.get(i);
            }
        }
        return null;
    }

    public String toTreeML() {
        StringBuilder sb = new StringBuilder();
        toTreeMLImpl(sb, 0);
        return sb.toString();
    }

    public void toTreeMLImpl(StringBuilder sb, int indent) {
        for (int i = 0; i < indent; i++) {
            sb.append('\t');
        }
        sb.append(this.name).append(" : ").append(val(this.value)).append('\n');
        for (Node child : this.children) {
            child.toTreeMLImpl(sb, indent + 1);
        }

    }

    private String val(Object v) {
        if (v instanceof String) {
            String s = (String) v;
            s = s.replace("\r", "\\r").replace("\n", "\\n").replace("\"", "\"\"");
            if (s.contains(" ") || !s.equals(v)) {
                return '"' + s + '"';
            } else {
                return (String) v;
            }
        } else if (v instanceof Double) {
            Double d = (Double) v;
            if (Math.abs(d) < 0.001 || Math.abs(d) > 999999) {
                return (Parser.DECIMAL_FORMAT.format(v));
            }
        } else if (v instanceof Long) {
            Long lo = (Long) v;
            if (Math.abs(lo) > 999999) {
                return (Parser.LONG_FORMAT.format(lo));
            }
        } else if (v instanceof List<?>) {
            boolean b = true;
            StringBuilder sb2 = new StringBuilder();
            for (Object o : (List<?>) v) {
                if (b) {
                    b = false;
                } else {
                    sb2.append(", ");
                }
                sb2.append(val(o));
            }
            return sb2.toString();
        }
        return String.valueOf(v);

    }

    public void addValue(Object v) {
        valueSets++;
        if (value == null && valueSets == 1) {
            value = v;
        } else if (valueSets > 2) {
            //noinspection unchecked
            ((List)value).add(v);
        } else {
            List<Object> list = new ArrayList<>();
            list.add(value);
            list.add(v);
            value = list;
        }
    }
}
package treeml;

import java.util.*;

/**
 * Basic TreeML schema language.
 * Created by Ags on 6/25/2016.
 */
@SuppressWarnings({"Convert2streamapi", "WeakerAccess"})
public class Schema {
    public Map<Node, SchemaNode> validated = new HashMap<>();

    static final Schema PASS = new Schema(new RootNode()) {
        @Override
        public void validateNode(List<Node> nodeStack, Node parent, Schema schema) {
        }
    };
    SchemaNode start = new SchemaNode(this);

    public Schema(Node schemaDocument) {
        //there is only one child of root in a schema
        //allow zero for PASS
        if (schemaDocument.children.size() > 0) {
            start.next = makeSchemaNode(schemaDocument.children.get(0));
            start.children.add(start.next);
        }
    }

    /**
     * Verify that the node matches the definition, and apply the schema type to the value.
     */
    public void validateNode(List<Node> nodeStack, Node node, Schema schema) {
        Node parent = nodeStack.get(nodeStack.size() - 1);
        SchemaNode ancestrySn = validated.get(parent);
        if (ancestrySn != null) {
            boolean matched = false;
            for (int i = nodeStack.size() - 1; i >= 1; i--) {
                final Node ancestor = nodeStack.get(i);
                if (nameMatch(ancestor, ancestrySn)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                throw new RuntimeException("Ancestor does not match. Expected: " + nodeStack + " got " + ancestrySn.name);
            }
        }
        SchemaNode matchedSn = null;
        if (ancestrySn == null) {
            matchedSn = schema.start.next;
        } else {
            boolean matched = false;
            for (SchemaNode childSn : ancestrySn.children) {
                if (nameMatch(node, childSn)) {
                    matched = true;
                    matchedSn = childSn;
                    break;
                }
            }
            if (!matched) {
                List<String> ls = new ArrayList<>();
                for (SchemaNode n : ancestrySn.children) {
                    ls.add(n.name);
                }
                throw new RuntimeException("Node does not match. Expected: " + ls + ", got: " + node.name);
            }
        }
        validated.put(node, matchedSn);
        previouslyValidated = node;
    }

    Node previouslyValidated = null;

    void refineType(Node node) {
        SchemaNode cursor = validated.get(node);
        if (cursor == null) {
            return;
        }
        if (cursor.list) {
            if (!(node.value instanceof List)) {
                String type = "string";
                try {
                    Object refined;
                    if (cursor.integer) {
                        type = "integer";
                        refined = Collections.singletonList((Long) node.value);
                    } else if (cursor.bool) {
                        type = "boolean";
                        refined = Collections.singletonList((Boolean) node.value);
                    } else if (cursor.decimal) {
                        type = "decimal";
                        refined = Collections.singletonList((Double) node.value);
                    } else {
                        type = "stringlike";
                        refined = Collections.singletonList((String) node.value);
                    }
                    node.value = refined;
                } catch (Exception e) {
                    throw new RuntimeException("Type mismatch: node " + node.name + " in typed " + type + " but has value of " + node.value.getClass().getSimpleName());
                }
            }
        } else {
            if (cursor.integer && !(node.value instanceof Long)) {
                throw new RuntimeException("Type mismatch: node " + node.name + " is typed integer but has value of " + node.value.getClass().getSimpleName());
            } else if (cursor.bool && !(node.value instanceof Boolean)) {
                throw new RuntimeException("Type mismatch: node " + node.name + " is typed boolean but has value of " + node.value.getClass().getSimpleName());
            } else if (cursor.decimal && !(node.value instanceof Double)) {
                throw new RuntimeException("Type mismatch: node " + node.name + " is typed double but has value of " + node.value.getClass().getSimpleName());
            } else if (node.value instanceof String && !(cursor.string || cursor.token || cursor.tokenid || cursor.tokenidref)) {
                throw new RuntimeException("Type mismatch: node " + node.name + " is typed stringlike but has value of " + node.value.getClass().getSimpleName());
            }
        }
    }

    static boolean nameMatch(Node node, SchemaNode sn) {
        String nodeName = node.name;
        String snName = sn.name;
        return nodeName.equals(snName) || "token".equals(snName);
    }

    SchemaNode makeSchemaNode(Node node) {
        SchemaNode result = new SchemaNode(this);
        result.name = node.name;
        @SuppressWarnings("unchecked")
        List<String> values = (List<String>) node.value;
        result.single = values.contains("single");
        result.optional = values.contains("optional");
        result.token = values.contains("token");
        result.string = values.contains("string");
        result.tokenid = values.contains("tokenid");
        result.tokenidref = values.contains("tokenidref");
        result.integer = values.contains("integer");
        result.decimal = values.contains("decimal");
        result.bool = values.contains("boolean");
        result.empty = values.contains("empty");
        result.list = values.contains("list");
        result.set = values.contains("set");
        if (values.contains("enum")) {
            result.hasEnum = true;
            boolean copy = false;
            for (String value : values) {
                if (copy) {
                    result.enumVals.add(value);
                }
                copy = copy || "enum".equals(value);
            }
        }
        SchemaNode prev = null;
        for (Node child : node.children) {
            final SchemaNode n = makeSchemaNode(child);
            n.parent = result;
            n.previous = prev;
            if (prev != null) {
                prev.next = n;
            }
            result.children.add(n);
            prev = n;
        }

        return result;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        int depth = 0;
        pl(this.start.next, sb, depth);
        return sb.toString();
    }

    private void pl(SchemaNode n, StringBuilder sb, int depth) {
        for (int i = 0; i < depth; i++) {
            sb.append("\t");
        }
        sb.append(n.name).append(" - ").append(parent(n)).append(" - ").append(next(n)).append('\n');
        for (SchemaNode child : n.children) {
            pl(child, sb, depth + 1);
        }

    }

    String parent(SchemaNode n) {
        return n.parent == null ? null : n.parent.name;
    }

    String next(SchemaNode n) {
        return n.next == null ? null : n.next.name;
    }
}
package org.treeml;

import java.util.*;

/**
 * Basic TreeML schema language.
 * Created by Ags on 6/25/2016.
 */
@SuppressWarnings({"Convert2streamapi", "WeakerAccess"})
public class Schema {

    Set<String> tokenids = new HashSet<>();
    static final Schema PASS = new Schema(new RootNode()) {
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

    static boolean nameMatch(Node node, SchemaNode sn) {
        String nodeName = node.name;
        String snName = sn.name;
        return nodeName.equals(snName) || "token".equals(snName);
    }

    SchemaNode makeSchemaNode(Node node) {
        SchemaNode result = new SchemaNode(this);
        result.name = node.name;
        @SuppressWarnings("unchecked")
        List<Object> values = (List<Object>) node.value;
        String mode = "default";
        for (Object value : values) {
            String s = String.valueOf(value);
            if (mode.equals("default")) {
                if (Arrays.asList(
                        "single", "optional", "token", "string", "tokenid", "tokenidref",
                        "integer", "decimal", "dateTime", "duration", "boolean", "empty",
                        "list", "set").contains(s)) {
                    setBoolean(result, value);
                } else if (s.startsWith("id") && result.id == null) {
                    result.id = s;
                } else if (s.startsWith("id")) {
                    throw new RuntimeException("Duplicate ID declared for schema node {line: " + node.line + "}");
                } else if (s.startsWith("choice") && result.choice == null) {
                    result.choice = s;
                } else if (s.startsWith("choice")) {
                    throw new RuntimeException("Duplicate choice group declared for schema node {line: " + node.line + "}");
                } else if (s.startsWith("enum")) {
                    mode = "enum";//todo:
                } else if (s.startsWith("range")) {
                    mode = "range";//todo:
                } else if (s.startsWith("items")) {
                    result.list = true;
                    mode = "items";//todo:
                }
            }
        }
//        if (values.contains("enum")) {
//            result.hasEnum = true;
//            boolean copy = false;
//            for (String value : values) {
//                if (copy) {
//                    result.enumVals.add(value);
//                }
//                copy = copy || "enum".equals(value);
//            }
//        }
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

    private void setBoolean(SchemaNode result, Object value) {
        if (value.equals("single")) {
            result.single = true;
        } else if (value.equals("optional")) {
            result.optional = true;
        } else if (value.equals("token")) {
            result.token = true;
        } else if (value.equals("string")) {
            result.string = true;
        } else if (value.equals("tokenid")) {
            result.tokenid = true;
        } else if (value.equals("tokenidref")) {
            result.tokenidref = true;
        } else if (value.equals("integer")) {
            result.integer = true;
        } else if (value.equals("decimal")) {
            result.decimal = true;
        } else if (value.equals("dateTime")) {
            result.dateTime = true;
        } else if (value.equals("duration")) {
            result.duration = true;
        } else if (value.equals("boolean")) {
            result.bool = true;
        } else if (value.equals("empty")) {
            result.empty = true;
        } else if (value.equals("list")) {
            result.list = true;
        } else if (value.equals("set")) {
            result.set = true;
        }
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
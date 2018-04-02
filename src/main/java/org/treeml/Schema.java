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
        List<String> values = (List<String>) node.value;
        result.single = values.contains("single");
        result.optional = values.contains("optional");
        result.token = values.contains("token");
        result.string = values.contains("string");
        result.tokenid = values.contains("tokenid");
        result.tokenidref = values.contains("tokenidref");
        result.integer = values.contains("integer");
        result.decimal = values.contains("decimal");
        result.dateTime = values.contains("dateTime");
        result.duration = values.contains("duration");
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
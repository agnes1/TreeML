package org.treeml;

import java.util.ArrayList;
import java.util.List;

/**
 * What goes in a schema node:
 * single|repeats, optional?, string|token|tokenid|tokenidref|integer|decimal|boolean|empty, (positive|negative|id)?, (list|set)?, enum
 * Created by Ags on 6/25/2016.
 */
class SchemaNode {
    @SuppressWarnings("FieldCanBeLocal")
    Schema schema;
    String name;
    SchemaNode next;
    SchemaNode previous;
    SchemaNode parent;
    List<SchemaNode> children = new ArrayList<>();
    boolean single;
    boolean optional;
    boolean string;
    boolean token;
    boolean tokenid;
    boolean tokenidref;
    boolean integer;
    boolean decimal;
    boolean bool;
    boolean empty;
    boolean list;
    boolean set;
    boolean hasEnum;
    List<String> enumVals = new ArrayList<>();
    boolean dateTime;
    boolean duration;

    SchemaNode(Schema schema) {
        this.schema = schema;
    }

    boolean hasMandatoryChildren() {
        for (SchemaNode child : children) {
            if (!child.optional) {
                return true;
            }
        }
        return false;
    }
}
package org.treeml;

import java.io.*;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Abstracts wot the parser does.
 * Created by Ags on 6/26/2016.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class ParserBase {
    public final Node parse(String inputClassPath, String inputSchemaClassPath) {
        try {
            return parse(
                    new InputStreamReader(this.getClass().getResourceAsStream(inputClassPath)),
                    new InputStreamReader(this.getClass().getResourceAsStream(inputSchemaClassPath))
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public final Node parse(String inputClassPath, Schema schema) {
        return parse(
                new InputStreamReader(this.getClass().getResourceAsStream(inputClassPath)),
                schema
        );
    }

    public final Node parse(InputStreamReader inputStreamReader, Schema schema) {
        try {
            return doParse(inputStreamReader, schema);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public final Node parse(File inputFile, File inputSchemaFile) {
        try {
            return parse(
                    new FileReader(inputFile),
                    new FileReader(inputSchemaFile)
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public final Node parse(Reader input, Reader inputSchema) throws IOException {
        Schema schema = parseSchema(inputSchema);
        return doParse(input, schema);
    }

    public final Schema parseSchema(File inputSchemaFile) {
        try {
            return parseSchema(
                    new FileReader(inputSchemaFile)
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public final Schema parseSchema(String inputSchemaClassPath) {
        try {
            return parseSchema(
                    new InputStreamReader(this.getClass().getResourceAsStream(inputSchemaClassPath))
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public final Schema parseSchema(Reader inputSchema) throws IOException {
        //final InputStream schemaSchemaStream = Schema.class.getResourceAsStream("/org/org.treeml/schema-schema.org.treeml");
        //Reader ssr = new InputStreamReader(schemaSchemaStream);
        //Node schemaSchemaDocument = parse(ssr);
        //Schema schema = new Schema(schemaSchemaDocument);
        final Node node = doParse(inputSchema, Schema.PASS);
        return new Schema(node);
    }

    protected abstract Node doParse(Reader inputSchema, Schema schema) throws IOException;

    public final Node parse(String inputClassPath) throws IOException {
        return doParse(
                new InputStreamReader(this.getClass().getResourceAsStream(inputClassPath))
                , Schema.PASS);
    }

    public final Node parse(File inputFile) throws IOException {
        return doParse(
                new FileReader(inputFile)
                , Schema.PASS);
    }

    public final Node parse(Reader input) throws IOException {
        return doParse(input, Schema.PASS);
    }

    protected void validate(RootNode document, Schema schema) {
        if (schema.equals(Schema.PASS)) {
            return;
        }
        if (document == null) {
            throw new RuntimeException("Cannot validate document in streaming mode.");
        }
        List<SchemaNode> schemaNodes = schema.start.children;
        List<Node> docNodes = document.children;
        final TreeMap<Integer, String> validationResults = validate(docNodes, schemaNodes);
        if (validationResults.size() > 0) {
            validationResults.values().forEach(System.out::println);
            throw new RuntimeException("Validation failed with " + validationResults.size() + " errors.");
        }
    }

    private TreeMap<Integer, String> validate(List<Node> docNodes, List<SchemaNode> schemaNodes) {
        TreeMap<Integer, String> errors = new TreeMap<>();
        SchemaNode schemaNode = schemaNodes.get(0);
        int i = 0;
        boolean secondOrMore = false;
        while (i < docNodes.size()) {
            Node docNode = docNodes.get(i);
            if (Schema.nameMatch(docNode, schemaNode)) {
                if ( ! validateType(docNode, schemaNode)) {
                    errors.put(docNode.line, "Validation error V004: " + docNode.name + " has value of wrong type.");
                }
                if (docNode.children.size() > 0) {
                    errors.putAll(validate(docNode.children, schemaNode.children));
                } else if (schemaNode.hasMandatoryChildren()) {
                    errors.put(docNode.line, "Validation error V003: " + docNode.name + " requires children.");
                }
                i++;
                if (schemaNode.single) {
                    schemaNode = schemaNode.next;
                    secondOrMore = false;
                } else {
                    secondOrMore = true;
                }
            } else {
                if (!schemaNode.optional && !secondOrMore) {
                    errors.put(docNode.line, "Validation error V001: " + docNode.name + " not expected at line " + docNode.line + "; expected = " + schemaNode.name);
                    return errors;
                } else {
                    schemaNode = schemaNode.next;
                    if (schemaNode == null) {
                        errors.put(docNode.line, "Validation error V002: " + docNode.name + " not expected at line " + docNode.line);
                        return errors;
                    }
                }
            }
        }
        return errors;
    }

    private boolean validateType(Node docNode, SchemaNode schemaNode) {
        if (docNode.value == null) {
            return true;
        }
        Object value = docNode.value;
        if (value instanceof String) {
            return (schemaNode.tokenid && isTokenId((String) value, schemaNode.schema))
                    || schemaNode.string
                    || (schemaNode.token && isToken((String) value));
        } else if (value instanceof Long) {
            return schemaNode.integer;
        } else if (value instanceof Double) {
            return schemaNode.decimal;
        } else if (value instanceof Boolean) {
            return schemaNode.bool;
        } else if (value instanceof List) {
            return schemaNode.list;
        } else if (value instanceof Duration) {
            return schemaNode.duration;
        } else if (value instanceof DateTime) {
            return schemaNode.dateTime;
        }
        return false;
    }

    private static final Pattern TOKEN = Pattern.compile("[a-z][a-zA-Z0-9_]*");
    private boolean isToken(String value) {
        return TOKEN.matcher(value).matches();
    }

    private boolean isTokenId(String value, Schema schema) {
        return TOKEN.matcher(value).matches() && schema.tokenids.add(value);
    }
}
package org.treeml;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
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


    @SuppressWarnings("UnusedReturnValue")
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
        final List<String> validationResults = validate(docNodes, schemaNodes);
        if (validationResults.size() > 0) {
            validationResults.forEach(System.out::println);
            throw new ValidationException("Validation failed with " + validationResults.size() + " errors.", validationResults);
        }
    }

    private List<String> validate(List<Node> docNodes, List<SchemaNode> schemaNodes) {
        List<String> errors = new ArrayList<>();
        SchemaNode schemaNode = schemaNodes.get(0);
        int i = 0;
        boolean secondOrMore = false;
        while (i < docNodes.size()) {
            Node docNode = docNodes.get(i);
            if (Schema.nameMatch(docNode, schemaNode)) {
                validateType(docNode, schemaNode, errors);
                if (docNode.children.size() > 0) {
                    errors.addAll(validate(docNode.children, schemaNode.children));
                } else if (schemaNode.hasMandatoryChildren()) {
                    errors.add("Validation error V003: [" + docNode.name + "] requires children {line: " + docNode.line + "}");
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
                    errors.add("Validation error V001: [" + docNode.name + "] not expected; expected = " + schemaNode.name + " {line: " + docNode.line + "}");
                    return errors;
                } else {
                    schemaNode = schemaNode.next;
                    if (schemaNode == null) {
                        errors.add("Validation error V002: [" + docNode.name + "] not expected {line: " + docNode.line + "}");
                        return errors;
                    }
                }
            }
        }
        return errors;
    }

    @SuppressWarnings("UnusedReturnValue")
    private boolean validateType(Node docNode, SchemaNode schemaNode, List<String> errors) {
        if (docNode.value == null) {
            return true;
        }
        Object value = docNode.value;
        boolean result = false;
        if (value instanceof String) {
            return (schemaNode.tokenid && isTokenId((String) value, docNode, schemaNode.schema, errors))
                    || schemaNode.string
                    || (schemaNode.tokenidref && refersToDeclaredId(docNode, schemaNode, (String)value, errors))
                    || (schemaNode.token && isToken((String) value, docNode, errors));
        } else if (value instanceof Long) {
            result = schemaNode.integer;
        } else if (value instanceof Double) {
            result = schemaNode.decimal;
        } else if (value instanceof Boolean) {
            result = schemaNode.bool;
        } else if (value instanceof List) {
            result = schemaNode.list;
        } else if (value instanceof Duration) {
            result = schemaNode.duration;
        } else if (value instanceof DateTime) {
            result = schemaNode.dateTime;
        }
        if (!result) {
            errors.add("Validation error V004: [" + docNode.name + "] has value of wrong type {line: " + docNode.line + "}");
        }
        return result;
    }

    private boolean refersToDeclaredId(Node docNode, SchemaNode schemaNode, String value, List<String> errors) {
        if (schemaNode.schema.tokenids.contains(value)) {
            return true;
        }
        errors.add("Validation error V008: [" + docNode.name + ':' + value + "] tokenidref does not refer to a preceding tokenid {line: " + docNode.line + "}");
        return false;
    }

    private static final Pattern TOKEN = Pattern.compile("[a-z][a-zA-Z0-9_]*");
    private boolean isToken(String value, Node docNode, List<String> errors) {
        if ( ! TOKEN.matcher(value).matches() ) {
            errors.add("Validation error V007: [" + docNode.name + ':' + value + "] token is not a valid token {line: " + docNode.line + "}");
        }
        return true;
    }

    private boolean isTokenId(String value, Node docNode, Schema schema, List<String> errors) {
        if (TOKEN.matcher(value).matches()) {
            if (schema.tokenids.add(value)) {
                return true;
            } else {
                errors.add("Validation error V005: [" + docNode.name + ':' + value + "] token ID is not unique {line: " + docNode.line + "}");
            }
        } else {
            errors.add("Validation error V006: [" + docNode.name + ':' + value + "] token ID is not a valid token {line: " + docNode.line + "}");
        }
        return false;
    }
}
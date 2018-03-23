package treeml;

import java.io.*;
import java.util.List;
import java.util.TreeMap;

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
        final InputStream schemaSchemaStream = Schema.class.getResourceAsStream("/org/treeml/schema-schema.treeml");
        Reader ssr = new InputStreamReader(schemaSchemaStream);
        Node schemaSchemaDocument = parse(ssr);
        Schema schema = new Schema(schemaSchemaDocument);
        final Node node = doParse(inputSchema, schema);
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
}
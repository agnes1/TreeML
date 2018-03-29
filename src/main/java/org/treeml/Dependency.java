package org.treeml;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Evaluates that a document (the referrer) makes references existing values in another document
 * or group of documents (the source).
 * Created by Ags on 6/25/2016.
 */
public class Dependency {

    private final static Logger logger = Logger.getLogger(Dependency.class.getSimpleName());

    private ParserBase parser;

    @SuppressWarnings({"unused", "WeakerAccess"})
    public Dependency() {
        parser = new Parser2();
    }

    @SuppressWarnings("unused")
    public Dependency(ParserBase parserIf) {
        parser = parserIf;
    }

    public static void main(String[] args) throws IOException {
        final String items = "/encyclopedia/items.org.treeml";
        final String itemsAnimalParts = "/encyclopedia/items-animal-parts.org.treeml";
        DocumentGroup dg = new DocumentGroup();
        dg.documents.add(items);
        dg.documents.add(itemsAnimalParts);
        dg.path = Arrays.asList("item", "id", "nodeValue");
        Dependency dp = new Dependency();
        final Map<Integer, String> brokenReferences = dp.checkReferences(
                "/encyclopedia/creatures.org.treeml",
                Arrays.asList("creature", "parts", "token", "nodeName"),
                dg
        );
        for (Map.Entry<Integer, String> entry : brokenReferences.entrySet()) {
            logger.warning(entry.getKey() + " : " + entry.getValue());
        }
        logger.warning("Found " + brokenReferences.size() + " errors.");
    }

    @SuppressWarnings("WeakerAccess")
    public Map<Integer, String> checkReferences(String referrer, List<String> referrerPath, DocumentGroup source) throws IOException {
        Comparer comparer = new Comparer();
        comparer.result = new TreeMap<>();
        comparer.values = source.eval(true, parser);
        walkTree(false, comparer, parser.parse(referrer), referrerPath);
        return comparer.result;
    }

    public interface TreeWalker {
        void walk(boolean unique, List<Node> found, String finalStep);
    }

    public static class Collector implements TreeWalker {
        Set<Object> result = new HashSet<>();
        @Override
        public void walk(boolean unique, List<Node> found, String finalStep) {
            collectValues(unique, result, found, finalStep);
        }
    }

    public static class Comparer implements TreeWalker {
        Map<Integer,String> result = new HashMap<>();
        Set<Object> values = new HashSet<>();
        @Override
        public void walk(boolean unique, List<Node> found, String finalStep) {
            compareValues(result, values, found, finalStep);
        }
    }

    private static void walkTree(boolean unique, TreeWalker walker, Node doc, List<String> path) {
        List<Node> found = new ArrayList<>();
        found.add(doc);
        for (int i = 0; i < path.size() - 1; i++) {
            String s = path.get(i);
            List<Node> temp = new ArrayList<>();
            for (Node foundLevel : found) {
                for (Node nextLevel : foundLevel.children) {
                    final SchemaNode sn = new SchemaNode(null);
                    sn.name = s;
                    if (Schema.nameMatch(nextLevel, sn)) {
                        temp.add(nextLevel);
                    }
                }
            }
            found = temp;
        }
        String finalStep = path.get(path.size() - 1);
        walker.walk(unique, found, finalStep);
    }

    @SuppressWarnings("WeakerAccess")
    public static void collectValues(boolean unique, Set<Object> result, List<Node> found, String finalStep) {
        if ("nodeName".equals(finalStep)) {
            for (Node node : found) {
                if (unique && result.contains(node.name)) {
                    throw new RuntimeException("L0001: Node name not unique: " + node.name + " at line " + node.line);
                }
                result.add(node.name);
            }
        } else if ("nodeValue".equals(finalStep)) {
            for (Node node : found) {
                if (unique && result.contains(node.value)) {
                    throw new RuntimeException("L0002: Node value not unique: " + node.value + " at line " + node.line);
                }
                result.add(node.value);
            }
        } else {
            throw new RuntimeException("Final step in path must be nodeName or nodeValue.");
        }
    }


    static void compareValues(Map<Integer, String> result, Set<Object> values, List<Node> found, String finalStep) {
        if ("nodeName".equals(finalStep)) {
            found.stream().filter(node -> !values.contains(node.name)).forEach(node -> result.put(node.line, "L0003: Node name not in source: " + node.name));
        } else if ("nodeValue".equals(finalStep)) {
            found.stream().filter(node -> !values.contains(node.value)).forEach(node -> result.put(node.line, "L0004: Node value not in source: " + node.value));
        } else {
            throw new RuntimeException("Final step in path must be nodeName or nodeValue.");
        }
    }

    public static class DocumentGroup {
        List<String> documents = new ArrayList<>();
        List<String> path = new ArrayList<>();

        Set<Object> eval(@SuppressWarnings("SameParameterValue") boolean unique, ParserBase parser) {
            Collector colly = new Collector();
            for (String document : documents) {
                try {
                    final Node doc = parser.parse(document);
                    walkTree(unique, colly, doc, path);

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return colly.result;
        }

    }

}
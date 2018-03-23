package treeml;

import java.io.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.logging.Logger;

/**
 * Parses a tab-indented or curly-indented file into a tree of Nodes.
 * @author agnes.clarke
 */
@SuppressWarnings("WeakerAccess")
public class Parser extends ParserBase {

    public static final String NADA = "nada", NULL = "null", TRUE = "true", FALSE = "false";
    private int indentOfLastLine = 0;
    public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
    public static final DecimalFormat LONG_FORMAT = new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));

    static {
        DECIMAL_FORMAT.setMaximumFractionDigits(18);
        DECIMAL_FORMAT.setMinimumFractionDigits(1);
        LONG_FORMAT.setMaximumFractionDigits(0);
    }

    public static void main(String[] args) throws IOException {
        System.out.println("Usage: pathToTreeMLFile optionalPathToTreeMLSchema");
        Reader fileReader = new FileReader(args[0]);
        Node node = new Parser().parse(fileReader);
        System.out.println(node);
    }

    protected Node doParse(Reader input, Schema schema) throws IOException {
        int lineNumber = 1;
        List<Node> nodeStack = new ArrayList<>();
        RootNode root = new RootNode();
        nodeStack.add(root);
        BufferedReader reader = new BufferedReader(input);
        String line = reader.readLine();
        while (line != null) {
            doLine(schema, root, line, nodeStack, lineNumber++, -1);
            line = reader.readLine();
        }
        validate(root, schema);
        return root;
    }


    private final List<String> valueList = new ArrayList<>();

    private int curlyStackPointer = 0;
    private int curlyStackPointerIncrement = 0;

    private static class Flags {
        int stackPointer = 0; // node to append to
        boolean startOfLine = true;
        boolean insideValue = false;
        boolean insideString = false;
        boolean valueSeparatedByWhitespace = false;
        StringBuilder nameOrValue = new StringBuilder();
    }

    private void doLine(Schema schema, RootNode root, String line, List<Node> nodeStack, int lineNumber, int skip) {
        line = preconditions(root, line, lineNumber, skip);
        if (line == null) return;
        Node newNode = new Node(null, null);
        newNode.line = lineNumber;
        int stackSize = nodeStack.size();
        Flags f = new Flags();
        for (int index = 0; index < line.length(); index++) {
            char c = line.charAt(index);
            if (c == '\t' && f.startOfLine) {
                f.stackPointer++;
            } else if (f.startOfLine && (c == '{' || c == '}')) {
                curlyStackPointerIncrement = c == '{' ? 1 : -1;
                doLine(schema, root, line, nodeStack, lineNumber, index + 1);
                return;
            } else if (c == ' ' && f.startOfLine && curlyStackPointer == 0) {
                Logger.getGlobal().warning("Mixed tabs and space at start of line: [" + line + ']');
            } else {
                if (f.startOfLine) {
                    if (startOfLine(line, nodeStack, lineNumber, stackSize, f, index, c)) return;
                }
                if (!f.insideString && c == '/' && nextCharEquals(line, index, '/')) {
                    break;
                }
                if (!f.insideString && (c == ':' || c == ',')) {
                    stringTeminatedByOperator(newNode, f);
                } else if (!f.insideString && (c == ' ' || c == '\t')) {
                    valueSeparatedByWhitespace(f);

                } else if (!f.insideString && (c == '{' || c == '}')) {
                    curlyEndsLine(schema, root, line, nodeStack, lineNumber, newNode, f, index, c);
                    return;
                } else {
                    if (c == '"') {
                        quotedValue(lineNumber, f, index);
                    } else {
                        index = insideValue(line, lineNumber, f, index, c);
                    }
                }
            }
        }
        endOfLine(schema, root, nodeStack, lineNumber, newNode, f.stackPointer, f.nameOrValue, -1, line);
    }

    private boolean startOfLine(String line, List<Node> nodeStack, int lineNumber, int stackSize, Flags f, int index, char c) {
        if (c == '/' && nextCharEquals(line, index, '/')) {
            // disregard line - it is a comment
            return true;
        }
        f.startOfLine = false;
        // drop nodes higher than current stackPointer
        int actualStackPointer = curlyStackPointer > 0 ? curlyStackPointer - 1 : f.stackPointer;
        for (int i = stackSize - 1; i > actualStackPointer; i--) {
            nodeStack.remove(i);
        }
        if (curlyStackPointer == 0 && actualStackPointer > indentOfLastLine + 1) {
            throw new RuntimeException("Line " + lineNumber + ": illegal indentation");
        } else {
            indentOfLastLine = actualStackPointer;
        }
        return false;
    }

    private int insideValue(String line, int lineNumber, Flags f, int index, char c) {
        f.insideValue = true;
        if (f.valueSeparatedByWhitespace) {
            throw new RuntimeException("Line " + lineNumber + ", char " + index
                    + ": Illegal whitespace");
        }
        if (f.insideString && (c == '\\' && nextCharEquals(line, index, 'n'))) {
            f.nameOrValue.append('\n');
            index++;
        } else if (f.insideString && (c == '\\' && nextCharEquals(line, index, 'r'))) {
            f.nameOrValue.append('\r');
            index++;
        } else if (f.insideString && (c == '\\' && nextCharEquals(line, index, '"'))) {
            f.nameOrValue.append('"');
            index++;
        } else if (f.insideString && (c == '\\' && nextCharEquals(line, index, '\\'))) {
            f.nameOrValue.append('\\');
            index++;
        } else {
            f.nameOrValue.append(c);
        }
        return index;
    }

    private void quotedValue(int lineNumber, Flags f, int index) {
        if (!f.insideString && f.insideValue) {
            throw new RuntimeException("Line " + lineNumber + ", char " + index
                    + ": Illegal quote");
        }
        f.insideString = !f.insideString;
        f.insideValue = true;
    }

    private void curlyEndsLine(Schema schema, RootNode root, String line, List<Node> nodeStack, int lineNumber, Node newNode, Flags f, int index, char c) {
        endOfLine(schema, root, nodeStack, lineNumber, newNode, f.stackPointer, f.nameOrValue, index, line);
        curlyStackPointerIncrement = c == '{' ? 1 : -1;
    }

    private void valueSeparatedByWhitespace(Flags f) {
        //noinspection ConstantConditions
        if (f.insideValue && !f.insideString) {
            f.valueSeparatedByWhitespace = true;
        }
    }

    private void stringTeminatedByOperator(Node newNode, Flags f) {
        if (newNode.name != null) {
            // add values to list
            valueList.add(f.nameOrValue.toString());
        } else {
            newNode.name = f.nameOrValue.toString();
        }
        f.insideValue = false;
        f.nameOrValue = new StringBuilder();
        f.valueSeparatedByWhitespace = false;
    }

    private String preconditions(RootNode root, String line, int lineNumber, int skip) {
        if (skip > 0) {
            line = line.substring(skip);
        }
        if (lineNumber == 1 && line.startsWith("#")) {
            root.value = line.substring(1).trim();
            return null;
        }
        if (lineNumber == 2 && line.startsWith("#")) {
            root.requires = line.substring(1).trim();
            return null;
        }
        String trim = line.trim();
        curlyStackPointer += curlyStackPointerIncrement;
        curlyStackPointerIncrement = 0;
        if (trim.equals("{") || trim.equals("}")) {
            curlyStackPointerIncrement = trim.equals("{") ? 1 : -1;
            return null;
        }
        if (trim.isEmpty()) {
            return null;
        }
        valueList.clear();
        return line;
    }

    private void endOfLine(Schema schema, RootNode root, List<Node> nodeStack, int lineNumber, Node newNode,
                           int stackPointer, StringBuilder nameOrValue, int skip, String line) {
        Object value;
        if (valueList.isEmpty()) {
            value = nameOrValue.toString();
        } else {
            if ( ! nameOrValue.toString().isEmpty()) {
                valueList.add(nameOrValue.toString());
            }
            value = new ArrayList<>(valueList);
        }
        newNode.value = value;
        typifyNode(newNode, nodeStack);
        try {
            schema.validateNode(nodeStack, newNode, schema);
            schema.refineType(newNode);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage() + " at line " + lineNumber, e);
        }
        int actualStackPointer = curlyStackPointer > 0 ? curlyStackPointer - 1 : stackPointer;
        nodeStack.get(actualStackPointer).children.add(newNode);
        nodeStack.add(newNode);
        if (skip > 0) {
            doLine(schema, root, line, nodeStack, lineNumber, skip);
        }
    }

    // Get the types of values right, nada the nada elements
    private void typifyNode(Node newNode, List<Node> nodeStack) {
        Object val = newNode.value;
        if (NADA.equals(val)) {
            dropNewNode(newNode, nodeStack);
        } else if ("".equals(val)) {
            newNode.value = null;
        } else if (!(val instanceof List<?>)) {
            newNode.value = typifyValue(null, val);
        } else {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) val;
            if (list.removeAll(Collections.singletonList(NADA)) && list.isEmpty()) {
                dropNewNode(newNode, nodeStack);
            } else {
                List<Object> newList = new ArrayList<>();
                list.forEach(untypified -> typifyValue(newList, untypified));
                newNode.value = newList;
            }
        }
    }

    private Object typifyValue(List<Object> valueList, Object untypified) {

        if (NULL.equals(untypified)) {
            return setValue(null, valueList);
        } else if (TRUE.equals(untypified)) {
            return setValue(true, valueList);
        } else if (FALSE.equals(untypified)) {
            return setValue(false, valueList);
        } else if (untypified.toString().matches("[+-]?[0-9]+")) {
            return setValue(Long.parseLong(untypified.toString()), valueList);
        } else if (untypified.toString().matches("[+-]?[0-9]+(\\.[0-9]+)")) {
            return setValue(Double.parseDouble(untypified.toString()), valueList);
        }
        return setValue(untypified.toString(), valueList);

    }

    private Object setValue(Object value, List<Object> valueList) {
        if (valueList != null) {
            valueList.add(value);
        }
        return value;
    }

    private void dropNewNode(Node newNode, List<Node> nodeStack) {
        nodeStack.remove(newNode);
        nodeStack.get(nodeStack.size() - 1).children.remove(newNode);
    }

    private boolean nextCharEquals(String line, int index, char... cs) {
        boolean result = true;
        int max = line.length();
        for (int i = 0; i < cs.length; i++) {
            char c = cs[i];
            //noinspection StatementWithEmptyBody
            if (index + 1 + i < max && line.charAt(index + 1 + i) == c) {
                // match, do nothing
            } else {
                result = false;
            }
        }
        return result;
    }

}
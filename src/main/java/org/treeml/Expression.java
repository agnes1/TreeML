package org.treeml;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class Expression {

    public static Object eval(Node node, String expression) {
        //token[1].token[1].*[:"a value"]()
        try {
            Object x = getLiteral(expression, false);
            if (x != null) return x;
            String[] functionSplit = expression.split("\\(");
            String function = functionSplit.length == 2 ? '(' + functionSplit[1] : null;
            String[] steps = functionSplit[0].split("\\.");
            for (String step : steps) {
                Node nextNode = null;
                ParsedStep parsedStep = new ParsedStep(step).invoke();
                Integer index = parsedStep.getIndex();
                Object testValue = parsedStep.getTestValue();
                boolean hasTestValue = parsedStep.hasTestValue();
                String nodeName = parsedStep.getNodeName();

                int currentIndex = 0;
                for (Node child : node.children) {
                    if (nodeName.equals("*") || nodeName.equals(child.name)) {
                        if ((index == null && !hasTestValue) || (index != null && index == currentIndex) || matches(child.value, testValue, hasTestValue)) {
                            nextNode = child;
                            break;
                        } else {
                            currentIndex++;
                        }
                    }
                }
                if (nextNode != null) {
                    node = nextNode;
                } else {
                    return null;
                }
            }
            try {
                if (function == null) {
                    return node.value;
                } else if ("()".equals(function)) {
                    return node.value;
                } else if ("(name)".equals(function)) {
                    return node.name;
                } else if ("(integer)".equals(function)) {
                    return node.value instanceof Long;
                } else if ("(double)".equals(function)) {
                    return node.value instanceof Double;
                } else if ("(string)".equals(function)) {
                    return node.value instanceof String;
                } else if ("(boolean)".equals(function)) {
                    return node.value instanceof Boolean;
                } else if ("(list)".equals(function)) {
                    return node.value instanceof List<?>;
                } else if (function.matches("\\([0-9]+\\)")) {
                    if (! (node.value instanceof List<?>) ) {
                        throw new RuntimeException("List function used on non-list value: " + node.value);
                    }
                    return ((List<?>)node.value).get(Integer.parseInt(function.replaceAll("\\(?\\)?", "")));
                }
            } catch (NumberFormatException e) {
                throw new RuntimeException("Could not evaluate function: " + function, e);
            }
        } catch (RuntimeException e) {
            throw new RuntimeException("Syntax error: " + expression, e);
        }
        return null;
    }

    private static boolean matches(Object value, Object testValue, boolean hasTestValue) {
        if ( ! hasTestValue) {
            return false;
        }
        if (value instanceof List<?>) {
            return ((List<?>)value).contains(testValue);
        }
        return testValue == null ? value == null : testValue.equals(value);
    }

    private static Object getLiteral(String expression, boolean knownToBeLiteral) {
        try {
            if ("null".equals(expression)) {
                return null;
            } else if ("true".equals(expression)) {
                return true;
            } else if ("false".equals(expression)) {
                return false;
            } else if (expression.startsWith("'")) {
                if (expression.endsWith("'")) {
                    return expression.substring(1, expression.length() - 1);
                } else {
                    throw new RuntimeException("Unclosed string literal.");
                }
            } else if ("0123456789.+-".contains("" + expression.charAt(0))) {
                try {
                    return Long.parseLong(expression);
                } catch (Exception e) {
                    return Double.parseDouble(expression);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Literal value syntax error: " + expression, e);
        }
        if (knownToBeLiteral) {
            throw new RuntimeException("Literal value syntax error: " + expression);
        }
        return null;
    }

    @SuppressWarnings("WeakerAccess")
    private static class ParsedStep {
        private String step;
        private Integer index;
        private Object testValue;
        private String nodeName;
        private boolean hasTestValue;

        public ParsedStep(String step) {
            this.step = step;
        }

        public Integer getIndex() {
            return index;
        }

        public Object getTestValue() {
            return testValue;
        }

        public String getNodeName() {
            return nodeName;
        }

        public ParsedStep invoke() {
            String[] predicateSplit = step.split("\\[");
            String predicate = predicateSplit.length == 2 ? predicateSplit[1] : null;
            index = null;
            testValue = null;
            if (predicate != null) {
                if (predicate.endsWith("]")) {
                    predicate = predicate.substring(0, predicate.length() - 1);
                } else {
                    throw new RuntimeException("Unclosed predicate.");
                }
                hasTestValue = predicate.startsWith(":");
                index = !hasTestValue ? Integer.parseInt(predicate) : null;
                testValue = hasTestValue ? getLiteral(predicate.substring(1), true) : null;

            }
            if (predicateSplit.length > 2) {
                throw new RuntimeException("Multiple predicates forbidden.");
            }
            nodeName = predicateSplit[0];
            return this;
        }

        public boolean hasTestValue() {
            return hasTestValue;
        }
    }

    public static void main(String[] args) throws IOException {
        Parser2 parser = new Parser2(new Parser2.Options(false, false, false, false), Collections.emptyList());
        Node doc = parser.parse(new File("C:\\Users\\agnes.clarke\\Desktop\\mongouni\\lesson4\\blog\\untitled\\src\\org\\org.treeml\\test\\simple3.tree"));
        System.out.println(Expression.eval(doc, "nm[:'urf'].name[1](2)"));
    }
}

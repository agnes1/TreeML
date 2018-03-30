package org.treeml;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses a tab-indented or curly-indented file into a tree of name-value Nodes.
 *
 * @author agnes.clarke
 */
@SuppressWarnings("WeakerAccess")
public class Parser2 extends ParserBase {

    @SuppressWarnings("unused")
    public static final String NULL = "null", TRUE = "true", FALSE = "false";
    public final Options options;
    public final List<Listener> listeners = new ArrayList<>(1);

    enum Types {
        tokenValue, stringValue, longValue, timeValue, doubleValue
    }

    public static class Options {
        final boolean verbose, timed, streaming, character;

        public Options(boolean verbose, boolean timed, boolean streaming, boolean character) {
            this.verbose = verbose;
            this.timed = timed;
            this.streaming = streaming;
            this.character = character;
        }
    }

    @SuppressWarnings("unused")
    public Parser2() {
        options = new Options(false, false, false, false);
    }

    public Parser2(Options options, List<Listener> listeners) {
        this.options = options;
        if (options.timed) {
            this.listeners.add(new TimerListener());
        }
        if (options.verbose) {
            this.listeners.add(new VerboseListener());
        }
        this.listeners.addAll(listeners);
    }

    public static void main(String[] args) throws IOException {
        System.out.println("Usage: pathToTreeMLFile optionalPathToTreeMLSchema");
        boolean verbose = false, timed = false, streaming = false, character = false;
        for (int i = 1; i < args.length; i++) {
            if ("verbose".equals(args[i])) {
                verbose = true;
            }
            if ("timed".equals(args[i])) {
                timed = true;
            }
            if ("streaming".equals(args[i])) {
                streaming = true;
            }
            if ("character".equals(args[i])) {
                character = true;
            }
        }
        Parser2 parser = new Parser2(new Options(verbose, timed, streaming, character), new ArrayList<>(1));
        Node node = parser.parse(new FileReader(args[0]));
        System.out.println(node);
    }

    protected Node doParse(Reader input, Schema schema) throws IOException {
        State state = new State(listeners);
        DomListener dom = new DomListener();
        CharacterListener characterListener = new CharacterListener();
        if (!options.streaming) {
            state.listeners.add(dom);
        }
        if (options.character) {
            state.listeners.add(characterListener);
        }
        for (Listener listener : state.listeners) {
            listener.onStart();
        }
        BufferedReader reader = new BufferedReader(input);
        int c = (char) reader.read();
        Group group = Prolog.I;
        while (c != -1) {
            location(state);
            if ((char) c != '\r') {
                group = group.read((char) c, state);
                for (Listener listener : state.listeners) {
                    listener.onCharacter((char) c, state.indent, state.index, state.lineNumber, state.lineIndex, group);
                }
                if (" \t\n".indexOf(c) < 0) {
                    state.lastSignificantChar = (char)c;
                }
            }
            newline((char) c, state);
            c = reader.read();
        }
        group = group.read('\n', state);
        if ((state.curlySyntax && state.indent != -1) || (!state.curlySyntax && !group.equals(StartOfLine.I))) {
            throw new RuntimeException(String.format("Unterminated {line %s, position %s}", state.lineNumber, state.lineIndex));
        }
        validate(dom.getDocument(), schema);
        for (Listener listener : state.listeners) {
            listener.onEnd();
        }
        return dom.getDocument();
    }

    private void location(State state) {
        state.index++;
        state.lineIndex++;
    }

    private void newline(char c, State state) {
        if (c == '\n') {
            state.lineNumber++;
            state.lineIndex = 0;
            if (!state.curlySyntax) {
                state.indent = 0;
            }
        }
    }

    private static void ignore() {
    }


    private static void addNode(State state, Group group) {
        if (state.indent < 0 || state.indent > (state.previousIndent + 1)) {
            throw new RuntimeException(String.format("Illegal indent: %s --> %s {line %s, position %s}", state.previousIndent, state.indent, state.lineNumber, state.lineIndex));
        }
        state.previousIndent = state.indent;
        for (Listener listener : state.listeners) {
            listener.onAddNode(state.indent, state.index, state.lineNumber, state.lineIndex, group);
        }
        state.endStatement();
    }

    private static void ensureCleanCurly(char c, char c2, int line, int position) {
        if ((c == '{' && c2 == '}') || (c2 == '{' && c == '}')) {
            throw new RuntimeException(String.format("Illogical curly braces {line %s, position %s}", line, position));
        }
    }

    public class State {
        private final List<Listener> listeners;
        int lineNumber = 1;
        public int index;
        public int lineIndex;
        public StringBuilder buffer;
        public boolean curlySyntax;
        public int indent, previousIndent = -1;
        public boolean escape;
        public Types type;
        public char lastSignificantChar;

        public State(List<Listener> listeners) {
            this.listeners = listeners;
        }

        public void endStatement() {
            if (!curlySyntax) {
                indent = 0;
            }
            buffer = null;
        }
    }

    public interface Group {
        Group read(char c, State state);

        static Group getGroup(char c, State state, Group instance) {
            state.buffer = null;
            state.escape = false;
            return instance.read(c, state);
        }

    }

    public static class Prolog implements Group {

        public static final Prolog I = new Prolog();

        @Override
        public Group read(char c, State state) {
            if (c == '#' && state.buffer == null) {
                state.buffer = new StringBuilder();
            } else if (c == '\n' || c == '\\') {
                if (state.buffer != null) {
                    String tag = state.buffer.toString();
                    for (Listener listener : state.listeners) {
                        listener.onTag(tag, state.index, state.lineNumber, state.lineIndex, this);
                    }
                }
                state.endStatement();
            } else if (c == '{' && state.buffer == null) {
                state.curlySyntax = true;
            } else if (state.buffer != null) {
                state.buffer.append(c);
            } else {
                return Group.getGroup(c, state, StartOfLine.I);
            }
            return this;
        }
    }

    public static class StartOfLine implements Group {

        public static final StartOfLine I = new StartOfLine();

        @Override
        public Group read(char c, State state) {
            if (c == ' ') {
                ignore();
            } else if (c == '\t' && !state.curlySyntax) {
                state.indent++;
            } else if (c == '{' && state.curlySyntax) {
                ensureCleanCurly(c, state.lastSignificantChar, state.lineNumber, state.lineIndex);
                state.indent++;
            } else if (c == '}' && state.curlySyntax) {
                ensureCleanCurly(c, state.lastSignificantChar, state.lineNumber, state.lineIndex);
                state.indent--;
            } else if (c == '\n' || c == '\\') {
                state.endStatement();
            } else if (c == '/') {
                state.endStatement();
                return Group.getGroup(c, state, Comment.I);
            } else if (Name.allowed.indexOf(c) > -1) {
                return Group.getGroup(c, state, Name.I);
            } else {
                throw new RuntimeException(String.format("Illegal start of a name: %s {line %s, position %s}", c, state.lineNumber, state.lineIndex));
            }
            return this;
        }
    }

    public static class Name implements Group {
        public static final Name I = new Name();
        public static final String allowed = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_";

        @Override
        public Group read(char c, State state) {
            if (allowed.indexOf(c) > -1) {
                if (state.buffer == null) {
                    state.buffer = new StringBuilder();
                }
                state.buffer.append(c);
            } else {
                String name = state.buffer.toString();
                for (Listener listener : state.listeners) {
                    listener.onNodeName(name, state.indent, state.index, state.lineNumber, state.lineIndex, this);
                }
                return Group.getGroup(c, state, InterStage.I);
            }
            return this;
        }
    }

    public static class InterStage implements Group {
        public static final InterStage I = new InterStage();

        @Override
        public Group read(char c, State state) {
            if (c == ' ' || c == '\t') {
                ignore();
                return this;
            } else if (c == ':') {
                return Group.getGroup(' ', state, BeforeValue.I);
            }
            throw new RuntimeException(String.format("Name:Value not separated by legal character: %s {line %s, position %s}", c, state.lineNumber, state.lineIndex));
        }
    }

    public static class Token implements Group {
        public static final Token I = new Token();
        public static final String allowedFirstCharacter = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz_";
        public static final String allowed = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_";

        @Override
        public Group read(char c, State state) {
            if (state.buffer == null) {
                state.buffer = new StringBuilder();
            }
            if (allowed.indexOf(c) > -1) {
                state.buffer.append(c);
                return this;
            } else {
                return addValue(deriveValue(state), state, c, this);
            }
        }

        private Object deriveValue(State state) {
            String v = state.buffer.toString();
            if ("null".equals(v)) {
                return null;
            } else if ("true".equals(v)) {
                return true;
            } else if ("false".equals(v)) {
                return false;
            } else {
                return v;
            }
        }
    }

    public static class NumberValue implements Group {
        public static final NumberValue I = new NumberValue();
        public static final String allowedFirstCharacter = ".0123456789-";
        public static final String allowed = ".0123456789-_e";

        @Override
        public Group read(char c, State state) {
            if (state.buffer == null) {
                state.buffer = new StringBuilder();
            }
            if (c == '.' || c == 'e') {
                if (state.buffer.indexOf("" + c) > -1) {
                    throw new RuntimeException(String.format("Invalid number, two \"%s\": \\%s. {line %s, position %s}", c, state.buffer, state.lineNumber, state.lineIndex));
                }
                state.type = Types.doubleValue;
                state.buffer.append(c);
                return this;
            } else if (c == '-') {
                if (state.buffer.length() != 0 && state.buffer.charAt(state.buffer.length() - 1) != 'e') {
                    throw new RuntimeException(String.format("Invalid number, ,misplaced -: \\%s- {line %s, position %s}", state.buffer, state.lineNumber, state.lineIndex));
                }
                state.buffer.append(c);
                return this;
            } else if (c == '_') {
                return this;
            } else if (allowed.indexOf(c) > -1) {
                state.buffer.append(c);
                return this;
            } else {
                Object value;
                if (state.type == Types.longValue) {
                    value = Long.valueOf(state.buffer.toString());
                } else {
                    value = Double.valueOf(state.buffer.toString());
                }
                return addValue(value, state, c, this);
            }
        }
    }

    public static class StringValue implements Group {
        public static final StringValue I = new StringValue();

        @SuppressWarnings("ConstantConditions")
        @Override
        public Group read(char c, State state) {
            if (state.buffer == null) {
                state.buffer = new StringBuilder();
                //throw away opening quote
                return this;
            } else if (c == '\\' && !state.escape) {
                state.escape = true;
                return this;
            } else if (c == '\\' && state.escape) {
                state.escape = false;
                state.buffer.append(c);
                return this;
            } else if (c == '"' && !state.escape) {
                return addValue(state.buffer.toString(), state, ' ', this);
            } else if (c == '"' && state.escape) {
                state.escape = false;
                state.buffer.append(c);
                return this;
            } else if (c == 'r' && state.escape) {
                state.escape = false;
                state.buffer.append('\r');
                return this;
            } else if (c == 'n' && state.escape) {
                state.escape = false;
                state.buffer.append('\n');
                return this;
            } else if (state.escape) {
                throw new RuntimeException(String.format("Invalid escape sequence: \\%s {line %s, position %s}", c, state.lineNumber, state.lineIndex));
            } else {
                state.buffer.append(c);
                return this;
            }
        }
    }

    public static class TimeValue implements Group {
        public static final TimeValue I = new TimeValue();
        public static final String allowed = "0123456789+-THMSPZ:.";

        @SuppressWarnings("ConstantConditions")
        @Override
        public Group read(char c, State state) {
            if (state.buffer == null) {
                state.buffer = new StringBuilder();
                //throw away opening @
                return this;
            } else if (allowed.indexOf(c) > -1) {
                state.buffer.append(c);
                return this;
            } else {
                String value0 = state.buffer.toString();
                Object value;
                if (value0.startsWith("P")) {
                    value = new Duration(value0);
                } else {
                    value = new DateTime(value0);
                }
                return addValue(value, state, c, this);
            }
        }
    }

    private static Group addValue(Object value, State state, char c, Group group) {
        for (Listener listener : state.listeners) {
            listener.onAddValue(value, state.type, state.indent, state.index, state.lineNumber, state.lineIndex, group);
        }

        return Group.getGroup(c, state, AfterValue.I);
    }

public static class BeforeValue implements Group {
    public static final BeforeValue I = new BeforeValue();

    @Override
    public Group read(char c, State state) {
        if (Token.allowedFirstCharacter.indexOf(c) > -1) {
            state.type = Types.tokenValue;
            return Group.getGroup(c, state, Token.I);
        } else if (" \t".indexOf(c) > -1) {
            return this;
        } else if (c == '"') {
            state.type = Types.stringValue;
            return Group.getGroup(c, state, StringValue.I);
        } else if (c == '@') {
            state.type = Types.timeValue;
            return Group.getGroup(c, state, TimeValue.I);
        } else if (c == '\\') {
            return Group.getGroup(c, state, Continuation.I);
        } else if (NumberValue.allowedFirstCharacter.indexOf(c) > -1) {
            state.type = Types.longValue;
            return Group.getGroup(c, state, NumberValue.I);
        } else if (c == '\n') {
            return Group.getGroup(c, state, AfterValue.I);
        } else if (c == ',') {
            if (state.lastSignificantChar == ',') {
                throw new RuntimeException(String.format("Repeated comma {line %s, position %s}", state.lineNumber, state.lineIndex));
            }
            for (Listener listener : state.listeners) {
                listener.onDeclareList(state.indent, state.index, state.lineNumber, state.lineIndex, this);
            }
            return Group.getGroup(c, state, AfterValue.I);
        } else if (c == '{' && state.curlySyntax) {
            ensureCleanCurly(c, state.lastSignificantChar, state.lineNumber, state.lineIndex);
            return addValue(null, state, c, this);
        } else if (c == '}' && state.curlySyntax) {
            return addValue(null, state, c, this);
        } else {
            throw new RuntimeException(String.format("Value invalid start character: %s {line %s, position %s}", c, state.lineNumber, state.lineIndex));
        }
    }
}

public static class AfterValue implements Group {
    public static final AfterValue I = new AfterValue();

    @Override
    public Group read(char c, State state) {
        if (c == ' ' || c == '\t') {
            return this;
        } else if (state.curlySyntax && c == '{') {
            addNode(state, this);
            state.indent++;
            return Group.getGroup(' ', state, StartOfLine.I);
        } else if (state.curlySyntax && c == '}') {
            addNode(state, this);
            state.indent--;
            return Group.getGroup(' ', state, StartOfLine.I);
        } else if (c == '\\') {
            return Group.getGroup(c, state, Continuation.I);
        } else if (c == ',') {
            if (state.lastSignificantChar == ',') {
                throw new RuntimeException(String.format("Repeated comma {line %s, position %s}", state.lineNumber, state.lineIndex));
            }
            for (Listener listener : state.listeners) {
                listener.onDeclareList(state.indent, state.index, state.lineNumber, state.lineIndex, this);
            }
            return Group.getGroup(' ', state, BeforeValue.I);
        } else if (c == '\n' || c == '/') {
            addNode(state, this);
            return Group.getGroup(c, state, StartOfLine.I);
        }
        throw new RuntimeException(String.format("AfterValue not separated by legal character: %s {line %s, position %s}", c, state.lineNumber, state.lineIndex));
    }
}

public static class Continuation implements Group {
    public static final Continuation I = new Continuation();

    @Override
    public Group read(char c, State state) {
        if (c == '\\') {
            state.buffer = new StringBuilder();
            return this;
        } else if (c == '\n') {
            return Group.getGroup(' ', state, BeforeValue.I);
        } else if (c == ' ') {
            return this;
        } else {
            if (!state.curlySyntax && c == '\t' && state.buffer != null) {
                state.buffer.append(c);
                return this;
            }
            if (" \t".indexOf(c) == -1) {
                addNode(state, this);
                if (!state.curlySyntax) {
                    state.indent = state.buffer.length();
                }
                return Group.getGroup(c, state, StartOfLine.I);
            }
        }
        throw new RuntimeException(String.format("Continuation not legal character: %s {line %s, position %s}", c, state.lineNumber, state.lineIndex));
    }
}

public static class Comment implements Group {
    public static final Comment I = new Comment();

    @Override
    public Group read(char c, State state) {
        if (state.buffer != null) {
            if (state.buffer.length() == 1 && '/' != c) {
                throw new RuntimeException(String.format("Illegal start of a comment: /%s {line %s, position %s}", c, state.lineNumber, state.lineIndex));
            } else if (state.buffer.length() == 1) {
                state.buffer.append(c);
            } else if (c == '\n') {
                state.endStatement();
                return Group.getGroup(c, state, StartOfLine.I);
            }
        } else {
            if (c != '/')
                throw new RuntimeException(String.format("Illegal start of a comment: %s {line %s, position %s}", c, state.lineNumber, state.lineIndex));
            state.buffer = new StringBuilder().append(c);
        }
        return this;
    }
}

}
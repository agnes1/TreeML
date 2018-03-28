package treeml;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Test {

    public static void main(String[] args) throws IOException {
        File testDir = new File(args[0]);
        assert testDir.listFiles() != null;
        TagListener tl = new TagListener();
        Parser2 parser = new Parser2(new Parser2.Options(false, false, false), Collections.singletonList(tl));
        int ok=0, notok=0;
        //noinspection ConstantConditions
        for (File f : testDir.listFiles()) {
            tl.tags.clear();
            RootNode doc = null;
            try {
                doc = (RootNode)parser.parse(f);
                boolean failIntended = "fail".equals(tl.tags.get("result"));
                if (failIntended) {
                    System.out.println("FAILURE - PARSE RESULT - " + f.getName());
                    notok++;
                } else {
                    System.out.println("PASS    - PARSE RESULT - " + f.getName());
                    ok++;
                }
            } catch (Exception e) {
                boolean fail = "fail".equals(tl.tags.get("result"));
                if ( fail && e.getMessage().equals(tl.tags.get("error")) ) {
                    System.out.println("PASS    - PARSE RESULT - " + f.getName());
                    ok++;
                } else {
                    System.out.println("FAILURE Expected " + tl.tags.get("result") + ", got fail - " + f.getName());
                    System.out.println("---" + e.getMessage() + "---");
                    e.printStackTrace();
                    notok++;
                }
            }
            if (doc != null) {
                StringBuilder sb = new StringBuilder();
                int indent = 1;
                hashStructure(doc, sb, indent);
                String structure = tl.tags.get("structure");
                if (structure != null) {
                    boolean structureMatch = structure.equals(sb.toString());
                    if (structureMatch) {
                        System.out.println("PASS    - STRUCTURE    - " + f.getName());
                        ok++;
                    } else {
                        System.out.println("FAILURE - STRUCTURE    - " + f.getName());
                        System.out.println(" - expected: " + structure);
                        System.out.println(" - actual  : " + sb.toString());
                        notok++;
                    }
                }
                for (String tag : doc.tags) {
                    String[] nv = tag.split("::");
                    if (nv[0].equals("eval")) {
                        String[] v = nv[1].split("=");
                        String exp1 = v[0];
                        String exp2 = v[1];
                        Object value1 = Expression.eval(doc, exp1);
                        Object value2 = Expression.eval(doc, exp2);
                        if (value1 == null ? value2 == null : value1.equals(value2)) {
                            System.out.println("PASS    - EXPRESSION   - " + f.getName());
                            ok++;
                        } else {
                            System.out.println("FAILURE - EXPRESSION   - " + tag);
                            notok++;
                        }
                    }
                }


            }
        }
        if (notok > 0) {
            System.out.print("TEST FAIL: ");
        } else {
            System.out.print("TEST PASS: ");
        }
        System.out.println(String.format(" {pass: %s; fail: %s}", ok, notok));
    }

    private static void hashStructure(Node doc, StringBuilder sb, int indent) {
        for (Node child : doc.children) {
            sb.append(indent).append(":").append((child.value == null) ? 0 : (child.value instanceof List<?>) ? 2 : 1).append(';');
            hashStructure(child, sb, indent + 1);
        }
    }

    public static class TagListener implements Listener {

        Map<String, String> tags = new HashMap<>();

        @Override
        public void onTag(String tag, int index, int line, int lineIndex, Parser2.Group currentGroup) {
            String[] split = tag.split("::");
            if (split.length == 2) {
                tags.put(split[0], split[1]);
            }
        }

    }

}

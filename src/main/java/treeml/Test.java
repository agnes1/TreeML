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
            try {
                Node doc = parser.parse(f);
                StringBuilder sb = new StringBuilder();
                int indent = 1;
                hashStructure(doc, sb, indent);
                String structure = tl.tags.get("structure");
                boolean structureMatch = (structure == null || structure.equals(sb.toString()));
                boolean failIntended = "fail".equals(tl.tags.get("result"));
                if (failIntended) {
                    System.out.println("FAILURE Expected fail, got ok - " + f.getName());
                    notok++;
                } else if ( ! structureMatch) {
                    System.out.println("FAILURE Expected ok, got bad structure [" + sb.toString() + "] - " + f.getName());
                    notok++;
                } else {
                    System.out.println("PASS Expected ok, got ok - " + f.getName());
                    ok++;
                }
            } catch (Exception e) {
                boolean fail = "fail".equals(tl.tags.get("result"));
                if ( fail
                        && e.getMessage().equals(tl.tags.get("error")) ) {
                    System.out.println("PASS Expected fail, got fail - " + f.getName());
                    ok++;
                } else {
                    System.out.println("---" + e.getMessage() + "---");
                    System.out.println("FAILURE Expected " + tl.tags.get("result") + ", got fail - " + f.getName());
                    e.printStackTrace();
                    notok++;
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

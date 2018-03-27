package treeml;

public class VerboseListener implements Listener {
    @Override
    public void onCharacter(char c, int indent, int index, int line, int lineIndex, Parser2.Group currentGroup) {
        String s = ("" + c).replace(" ", "\\s").replace("\n", "\\n");
        System.out.println(s + '\t' + indent + "\t:\t" + currentGroup.getClass().getSimpleName());
    }

    @Override
    public void onTag(String tag, int index, int line, int lineIndex, Parser2.Group currentGroup) {
        System.out.println("#" + tag);
    }

    @Override
    public void onAddNode(String name, int indent, int index, int line, int lineIndex, Parser2.Group currentGroup) {
        System.out.println("Add node: " + name + " [" + indent + "]");
    }

    @Override
    public void onAddValue(Object value, Parser2.Types type, int indent, int index, int line, int lineIndex, Parser2.Group currentGroup) {
        System.out.println("Add value: " + value + " [" + indent + "]");
    }
}

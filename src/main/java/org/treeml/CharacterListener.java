package org.treeml;

public class CharacterListener implements Listener {
    @Override
    public void onCharacter(char c, int indent, int index, int line, int lineIndex, Parser2.Group currentGroup) {
        String s = ("" + c).replace(" ", "\\s").replace("\n", "\\n");
        System.out.println(s + '\t' + indent + "\t:\t" + currentGroup.getClass().getSimpleName());
    }

}

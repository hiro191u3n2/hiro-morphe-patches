package android.content;
public class ClipData {
    public final CharSequence label,text;
    private ClipData(CharSequence label,CharSequence text){this.label=label;this.text=text;}
    public static ClipData newPlainText(CharSequence label,CharSequence text){return new ClipData(label,text);}
    public Item getItemAt(int index){if(index!=0)throw new IndexOutOfBoundsException();return new Item(text);}
    public static class Item {private final CharSequence text;Item(CharSequence text){this.text=text;}public CharSequence getText(){return text;}}
}

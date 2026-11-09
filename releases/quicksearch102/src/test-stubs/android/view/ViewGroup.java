package android.view;
import android.content.Context;
import java.util.ArrayList;
import java.util.List;
public class ViewGroup extends View {
    public static class LayoutParams {
        public static final int MATCH_PARENT=-1, WRAP_CONTENT=-2;
        public int width,height;
        public LayoutParams(int width,int height) {this.width=width;this.height=height;}
    }
    public static class MarginLayoutParams extends LayoutParams {
        public int leftMargin,topMargin,rightMargin,bottomMargin;
        private int startMargin=Integer.MIN_VALUE,endMargin=Integer.MIN_VALUE;
        public MarginLayoutParams(int width,int height) {super(width,height);}
        public void setMargins(int left,int top,int right,int bottom) {leftMargin=left;topMargin=top;rightMargin=right;bottomMargin=bottom;}
        public void setMarginStart(int value) {startMargin=value;}
        public void setMarginEnd(int value) {endMargin=value;}
        public int getMarginStart() {return startMargin==Integer.MIN_VALUE?leftMargin:startMargin;}
        public int getMarginEnd() {return endMargin==Integer.MIN_VALUE?rightMargin:endMargin;}
        public boolean isMarginRelative() {return startMargin!=Integer.MIN_VALUE||endMargin!=Integer.MIN_VALUE;}
    }
    private final List<View> children=new ArrayList<>();
    public ViewGroup(Context c) {super(c);}
    public void addView(View v) {children.add(v);v.parent=this;}
    public int getChildCount() {return children.size();}
    public View getChildAt(int index) {return children.get(index);}
    @Override public <T extends View> T findViewById(int id) {
        T own=super.findViewById(id);if(own!=null)return own;
        for(View child:children){T result=child.findViewById(id);if(result!=null)return result;}return null;
    }
}

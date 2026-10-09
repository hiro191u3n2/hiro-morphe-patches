package android.graphics;
public final class Rect {
    public int left,top,right,bottom;
    public Rect(int l,int t,int r,int b){left=l;top=t;right=r;bottom=b;}
    public boolean equals(Object o){if(!(o instanceof Rect))return false;Rect r=(Rect)o;return left==r.left&&top==r.top&&right==r.right&&bottom==r.bottom;}
    public int hashCode(){return left*3+top*5+right*7+bottom*11;}
}


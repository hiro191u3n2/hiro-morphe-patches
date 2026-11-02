package android.view;
import java.util.ArrayList;
import android.content.Context;
public class View implements ViewParent {
    public static final int VISIBLE=0, INVISIBLE=4, GONE=8;
    public Context context;
    public boolean attached=true,shown=true,layoutRequested;
    public int visibility=VISIBLE,width=400,height=800,measuredWidth=400,measuredHeight=800,left,top;
    public int paddingLeft,paddingTop,paddingRight,paddingBottom,scrollX,scrollY;
    public int invalidations,layoutRequests;
    public ViewParent parent;
    public ViewGroup.LayoutParams layoutParams=new ViewGroup.LayoutParams(-1,-1);
    public long now=android.os.SystemClock.uptimeMillis();
    public OnClickListener click;
    public CharSequence contentDescription;
    public final ArrayList<OnAttachStateChangeListener> attachListeners=new ArrayList<OnAttachStateChangeListener>();
    public final ArrayList<Runnable> posts=new ArrayList<Runnable>();
    public static final class Timer { public Runnable r; public long due; Timer(Runnable r,long due){this.r=r;this.due=due;} }
    public final ArrayList<Timer> timers=new ArrayList<Timer>();
    public ViewTreeObserver observer=new ViewTreeObserver();
    public View(){this(new Context());}
    public View(Context context){this.context=context;}
    public Context getContext(){return context;}
    public int getWidth(){return width;} public int getHeight(){return height;}
    public int getMeasuredWidth(){return measuredWidth;} public int getMeasuredHeight(){return measuredHeight;}
    public int getLeft(){return left;} public int getTop(){return top;}
    public int getRight(){return left+width;} public int getBottom(){return top+height;}
    public int getPaddingLeft(){return paddingLeft;} public int getPaddingTop(){return paddingTop;}
    public int getPaddingRight(){return paddingRight;} public int getPaddingBottom(){return paddingBottom;}
    public int getScrollX(){return scrollX;} public int getScrollY(){return scrollY;}
    public ViewParent getParent(){return parent;}
    public ViewGroup.LayoutParams getLayoutParams(){return layoutParams;}
    public boolean isLayoutRequested(){return layoutRequested;}
    public void requestLayout(){ViewTreeObserver.assertNotDrawing();layoutRequested=true;layoutRequests++;}
    public void completeLayout(int w,int h){width=w;height=h;measuredWidth=w;measuredHeight=h;layoutRequested=false;}
    public void setPadding(int l,int t,int r,int b){ViewTreeObserver.assertNotDrawing();paddingLeft=l;paddingTop=t;paddingRight=r;paddingBottom=b;}
    public void setContentDescription(CharSequence text){contentDescription=text;}
    public void setBackgroundColor(int color){ViewTreeObserver.assertNotDrawing();}
    public void bringToFront(){ViewTreeObserver.assertNotDrawing();}
    public void setVisibility(int visibility){ViewTreeObserver.assertNotDrawing();this.visibility=visibility;shown=visibility==VISIBLE;}
    public int getVisibility(){return visibility;}
    public void setOnClickListener(OnClickListener click){this.click=click;}
    public boolean performClick(){if(click==null)return false;click.onClick(this);return true;}
    public interface OnClickListener {void onClick(View view);}
    public interface OnAttachStateChangeListener {void onViewAttachedToWindow(View view);void onViewDetachedFromWindow(View view);}
    public void addOnAttachStateChangeListener(OnAttachStateChangeListener listener){if(!attachListeners.contains(listener))attachListeners.add(listener);}
    public void removeOnAttachStateChangeListener(OnAttachStateChangeListener listener){attachListeners.remove(listener);}
    public ViewTreeObserver getViewTreeObserver(){return observer;}
    public void detach(){attached=false;for(OnAttachStateChangeListener l:new ArrayList<OnAttachStateChangeListener>(attachListeners))l.onViewDetachedFromWindow(this);observer.alive=false;}
    public void attach(){attached=true;observer=new ViewTreeObserver();for(OnAttachStateChangeListener l:new ArrayList<OnAttachStateChangeListener>(attachListeners))l.onViewAttachedToWindow(this);}
    public boolean post(Runnable runnable){posts.add(runnable);return true;}
    public boolean postDelayed(Runnable runnable,long delay){now=Math.max(now,android.os.SystemClock.uptimeMillis());timers.add(new Timer(runnable,now+delay));return true;}
    public boolean removeCallbacks(Runnable runnable){posts.removeIf(r->r==runnable);timers.removeIf(t->t.r==runnable);return true;}
    public void flushPosts(){android.os.Looper.background=false;int guard=1000;while(!posts.isEmpty()){if(--guard==0)throw new AssertionError("post spin");posts.remove(0).run();}}
    public void advance(long delta){
        now=Math.max(now,android.os.SystemClock.uptimeMillis());long target=now+delta;int guard=1000;
        for(;;){int pick=-1;long min=Long.MAX_VALUE;
            for(int i=0;i<timers.size();i++)if(timers.get(i).due<=target&&timers.get(i).due<min){pick=i;min=timers.get(i).due;}
            if(pick<0)break;if(--guard==0)throw new AssertionError("timer spin");
            Timer t=timers.remove(pick);now=Math.max(min,android.os.SystemClock.uptimeMillis());android.os.SystemClock.now=now;t.r.run();}
        now=target;android.os.SystemClock.now=target;
    }
    public boolean isAttachedToWindow(){return attached;}
    public boolean isShown(){return attached&&shown&&visibility==VISIBLE;}
    public void postInvalidateOnAnimation(){invalidations++;}
}

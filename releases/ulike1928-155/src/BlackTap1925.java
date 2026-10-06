package com.hiro.ulike;

import android.graphics.RectF;
import android.os.Looper;
import android.os.SystemClock;
import android.view.MotionEvent;
import com.bytedance.corecamera.ui.view.CameraShadeView;
import com.bytedance.corecamera.ui.view.GestureBgLayout;
import java.util.WeakHashMap;
import i.o.a.b1.a.g.y;

/** Background-only gesture routing. Child buttons retain normal View dispatch.
 * A first tap on a black bar is not dispatched as a native single tap until
 * GestureDetector confirms that it was not part of a double tap. No timers,
 * retained MotionEvents, synthetic clicks or direct camera-session manipulation.
 */
public final class BlackTap1925 {
    private BlackTap1925() {}
    private static final WeakHashMap<GestureBgLayout, Long> pending = new WeakHashMap<>();
    private static final WeakHashMap<GestureBgLayout, Long> lastSwitch = new WeakHashMap<>();
    private static boolean ui(){return Looper.myLooper()==Looper.getMainLooper();}
    private static boolean live(GestureBgLayout v){
        return v!=null && v.isAttachedToWindow() && v.isShown() && v.isEnabled() && v.hasWindowFocus();
    }
    private static boolean camera(GestureBgLayout v){return v!=null && v.c instanceof y.h;}
    static boolean outside(RectF r,float x,float yy,int width,int height){
        return r!=null && width>0 && height>0 && !Float.isNaN(x) && !Float.isNaN(yy)
            && r.left>=0 && r.top>=0 && r.right<=width && r.bottom<=height
            && r.right>r.left && r.bottom>r.top && x>=0 && x<width && yy>=0 && yy<height
            && (x<r.left || x>=r.right || yy<r.top || yy>=r.bottom);
    }
    static boolean black(GestureBgLayout bg,MotionEvent event){
        if(event==null || event.getPointerCount()!=1 || !live(bg) || !camera(bg))return false;
        CameraShadeView v=PreviewLayout1922.gestureView1925();
        if(v==null || !v.isAttachedToWindow() || !v.isShown()
                || v.getRootView()!=bg.getRootView() || v.getWidth()!=v.c || v.getHeight()!=v.j)return false;
        RectF r=PreviewLayout1922.viewport();
        // During a transition use drawn mask bounds, not its future target.
        if(v.w!=null && v.n!=null && v.o!=null && r!=null)
            r=new RectF(r.left,v.n.bottom,r.right,v.o.top);
        int[] origin=new int[2];v.getLocationOnScreen(origin);
        return outside(r,event.getRawX()-origin[0],event.getRawY()-origin[1],v.getWidth(),v.getHeight());
    }
    public static boolean singleUp(GestureBgLayout view,MotionEvent event){
        if(!ui())return false;
        pending.remove(view);
        if(black(view,event)){
            pending.put(view,event.getDownTime());return true;
        }
        if(view!=null && view.c!=null)view.c.a(event);
        return false; // original OnGestureListener return value
    }
    public static boolean confirmed(GestureBgLayout view,MotionEvent event){
        if(!ui())return false;
        Long first=pending.remove(view);
        if(first==null || event==null || first.longValue()!=event.getDownTime())return false;
        if(black(view,event) && view.c!=null)view.c.a(event);
        return true;
    }
    public static boolean doubleTap(GestureBgLayout view,MotionEvent event){
        if(!ui())return false;
        Long first=pending.remove(view);
        // Matched pending tap also prevents falling into the old blank handler if
        // the view's rectangle changed between the two fingers-up/down events.
        boolean owned=first!=null && event!=null && first.longValue()==event.getDownTime();
        if(!owned && !black(view,event)){
            if(view==null || view.c==null)return false;
            view.c.onDoubleTap(event);return true;
        }
        if(!live(view) || !camera(view) || event==null || event.getPointerCount()!=1 || event.getActionMasked()!=MotionEvent.ACTION_DOWN)return true;
        long now=SystemClock.uptimeMillis();
        Long before=lastSwitch.get(view);
        if(before!=null && now-before.longValue()<800L)return true;
        y owner=((y.h)view.c).a;
        // Preserve recording, still-capture and pending high-quality save guards.
        if(owner==null || owner.c1() || owner.l || ExitBusy1921.captureBusy(false))return true;
        lastSwitch.put(view,now);
        owner.switchCamera(); // native switch keeps its own 800ms and ready-state guards
        return true;
    }
    public static void reset(GestureBgLayout view,MotionEvent event){
        if(!ui() || view==null || event==null)return;
        int action=event.getActionMasked();
        if(action==MotionEvent.ACTION_CANCEL || action==MotionEvent.ACTION_POINTER_DOWN
                || event.getPointerCount()!=1)pending.remove(view);
    }
    public static void detached(GestureBgLayout view){
        if(!ui())return;
        pending.remove(view);lastSwitch.remove(view);
    }
}

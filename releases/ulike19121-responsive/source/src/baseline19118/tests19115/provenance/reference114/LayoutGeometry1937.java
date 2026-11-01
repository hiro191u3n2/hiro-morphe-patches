package com.hiro.ulike;

import android.graphics.RectF;
import android.widget.RelativeLayout;
import com.bytedance.corecamera.camera.basic.PureCameraFragment;
import com.bytedance.corecamera.ui.view.CameraShadeView;
import com.bytedance.corecamera.ui.view.CameraView;
import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/** UI-thread preview geometry only. Never changes capture, renderer resolution or saved pixels. */
public final class LayoutGeometry1937 {
    private LayoutGeometry1937() {}
    private static final WeakHashMap<PureCameraFragment, Snapshot> delivered = new WeakHashMap<>();
    private static final WeakHashMap<PureCameraFragment, Snapshot> pending = new WeakHashMap<>();
    private static CameraShadeView shade(i.o.a.b1.a.g.y api) {
        if (api == null || !(api.e instanceof i.o.a.b1.a.d.b)) return null;
        i.o.a.b1.a.d.b owner = (i.o.a.b1.a.d.b) api.e;
        return owner.d == api ? owner.b : null;
    }
    private static int measured(CameraShadeView view, boolean height) {
        if (!PreviewLayout1922.current(view)) return 0;
        // Both dimensions must belong to a completed measurement, not constructor screen caches.
        int width = view.getWidth(), tall = view.getHeight();
        return width > 0 && tall > 0 ? (height ? tall : width) : 0;
    }
    public static int cameraWidth(i.o.a.b1.a.g.y api) {
        int result=measured(shade(api),false);
        return result>0 ? result : i.n.c.a.o.w.e.p();
    }
    public static int cameraHeight(i.o.a.b1.a.g.y api) {
        int result=measured(shade(api),true);
        return result>0 ? result : i.n.c.a.o.w.e.m();
    }
    public static int previewWidth(i.o.a.m.j.f owner) {
        int result=measured(owner==null?null:owner.a,false);
        return result>0 ? result : i.n.c.a.o.w.e.p();
    }
    public static int previewHeight(i.o.a.m.j.f owner) {
        int result=measured(owner==null?null:owner.a,true);
        return result>0 ? result : i.n.c.a.o.w.e.m();
    }
    private static CameraShadeView shade(PureCameraFragment owner) {
        CameraView camera=owner==null?null:owner.z2();
        CameraShadeView view=camera==null?null:camera.getCameraShaderView();
        return measured(view,false)>0 ? view : null;
    }
    /** Return true only when native LP equality AND the last successful renderer delivery agree. */
    public static boolean equivalent(PureCameraFragment owner, RelativeLayout.LayoutParams previous,
            RelativeLayout.LayoutParams next, RectF rect) {
        if (!owner.M2(previous,next)) return false;
        CameraShadeView view=shade(owner);
        if (view==null) return true; // Preserve native policy outside the currently owned camera view.
        Snapshot current=Snapshot.capture(owner,view,rect);
        Snapshot last=delivered.get(owner);
        return current!=null && last!=null && last.same(current);
    }
    /** Snapshot BEFORE delivery; callbacks may synchronously replace the camera session. */
    public static void beginDelivery(PureCameraFragment owner, RectF rect) {
        CameraShadeView view=shade(owner);
        Snapshot value=view==null?null:Snapshot.capture(owner,view,rect);
        delivered.remove(owner);
        if(value==null)pending.remove(owner);else pending.put(owner,value);
    }
    /** Hooked immediately AFTER cameraHelper.o0 returns; rejected/throwing calls never populate cache. */
    public static void markDelivery(PureCameraFragment owner, RectF rect) {
        CameraShadeView view=shade(owner);
        Snapshot value=view==null?null:Snapshot.capture(owner,view,rect);
        Snapshot started=pending.remove(owner);
        if(value==null || started==null || !started.same(value))delivered.remove(owner);else delivered.put(owner,started);
    }
    private static final class Snapshot {
        final WeakReference<Object>[] identities;
        final long epoch;
        final float left,top,right,bottom;
        final int ratio,width,height,present,previewWidth,previewHeight;
        final boolean mode,helperMode;
        @SuppressWarnings("unchecked")
        Snapshot(Object[] owners, CameraShadeView view, RectF rect, PureCameraFragment fragment) {
            identities=(WeakReference<Object>[])new WeakReference<?>[owners.length];
            int bits=0;for(int i=0;i<owners.length;i++){identities[i]=new WeakReference<>(owners[i]);if(owners[i]!=null)bits|=1<<i;}present=bits;
            epoch=PreviewLayout1922.generation(view);
            left=rect.left;top=rect.top;right=rect.right;bottom=rect.bottom;
            ratio=fragment.r;mode=fragment.I;helperMode=fragment.y.v;width=view.getWidth();height=view.getHeight();
            previewWidth=fragment.y.r.a.p.getWidth();previewHeight=fragment.y.r.a.p.getHeight();
        }
        static Snapshot capture(PureCameraFragment fragment, CameraShadeView view, RectF rect) {
            if(rect==null || rect.right<=rect.left || rect.bottom<=rect.top || !finite(rect.left)||!finite(rect.top)||!finite(rect.right)||!finite(rect.bottom))return null;
            i.f.l.n.q.y.c helper=fragment.y;
            if(helper==null || helper.r==null || helper.r.a==null || helper.b==null)return null;
            i.f.l.h manager=helper.r.a;
            // o0 is void even when its scene/property is missing. Manager.E also
            // returns before rendering without a PreviewView, or merely posts when s is true.
            i.f.l.u.g scene=helper.r.g(helper.b);
            i.f.l.u.p<?> property=scene==null?null:scene.d();
            if(property==null || manager.p==null || manager.b==null || manager.s
                    || manager.p.getWidth()<=0 || manager.p.getHeight()<=0)return null;
            com.ss.android.vesdk.VERecorder recorder=manager.e;
            if(recorder==null || fragment.o==null || helper.s==null || helper.s.getCameraShaderView()!=view)return null;
            // A missing render view is legitimate in SurfaceView mode; keep its identity as null.
            Object[] owners={view,helper,helper.r,manager,recorder,recorder.getRenderView(),
                    recorder.getCurrentCameraCapture(),helper.g,helper.s,fragment.o,scene,property,manager.p,manager.b};
            return new Snapshot(owners,view,rect,fragment);
        }
        private static boolean finite(float f){return !Float.isNaN(f)&&!Float.isInfinite(f);}
        boolean same(Snapshot that) {
            if(present!=that.present || epoch!=that.epoch || ratio!=that.ratio || mode!=that.mode || helperMode!=that.helperMode || width!=that.width || height!=that.height
                    || previewWidth!=that.previewWidth || previewHeight!=that.previewHeight
                    || Float.compare(left,that.left)!=0 || Float.compare(top,that.top)!=0
                    || Float.compare(right,that.right)!=0 || Float.compare(bottom,that.bottom)!=0)return false;
            for(int i=0;i<identities.length;i++)if(identities[i].get()!=that.identities[i].get())return false;
            return true;
        }
    }
}

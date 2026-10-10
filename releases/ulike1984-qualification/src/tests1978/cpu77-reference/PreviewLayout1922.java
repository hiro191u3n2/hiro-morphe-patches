package com.hiro.ulike;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.os.SystemClock;
import com.bytedance.corecamera.ui.view.CameraShadeView;
import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/** Own each shade transition until completion. No camera, bitmap or save-path changes.
 * Called on the native View/UI thread. Delayed starts capture an exact animator,
 * so a late first-frame task can never start a newer layout's animation.
 */
public final class PreviewLayout1922 {
    private static final long SETTLE_LIMIT_MS = 1200L;
    private static final WeakHashMap<CameraShadeView, Pending> pending = new WeakHashMap<>();
    private static final WeakHashMap<CameraShadeView, Long> generations = new WeakHashMap<>();
    private static final WeakHashMap<CameraShadeView, Long> ownershipGenerations = new WeakHashMap<>();
    private PreviewLayout1922() {}
    // A rendering rectangle must use the same coordinate system as its shade view.
    private static volatile WeakReference<CameraShadeView> active = new WeakReference<>(null);
    private static volatile android.graphics.RectF snapshot;
    private static final WeakHashMap<CameraShadeView, android.graphics.RectF> delivered = new WeakHashMap<>();
    // At most two scalar hierarchy snapshots per attachment. This is not an
    // onDraw observer; it never samples pixels, labels or other View content.
    private static final WeakHashMap<CameraShadeView, LayoutProbe> layoutProbes = new WeakHashMap<>();
    private static void trace(String phase,CameraShadeView view,String fields) {
        try {
            long epoch=0L;try{epoch=ManualLens170.number("epoch");}catch(Throwable ignored){}
            CameraTrace1965.event(phase,epoch,"view="+System.identityHashCode(view)+" "+fields);
        }catch(Throwable ignored){}
    }
    public static void prepare(CameraShadeView view) {
        cancel(view);
        advanceGeneration(view);
        delivered.remove(view);
    }
    private static void advanceGeneration(CameraShadeView view) {
        if(view!=null)synchronized(generations){generations.put(view,generation(view)+1L);}
    }
    public static long generation(CameraShadeView view) {
        synchronized(generations){Long value=generations.get(view);return value==null?0L:value.longValue();}
    }
    /** Camera-controller work may intentionally wait across first measurement.
     * Only detach/replacement invalidates that ownership; ordinary geometry does not.
     */
    public static long ownershipGeneration(CameraShadeView view) {
        synchronized(ownershipGenerations){Long value=ownershipGenerations.get(view);return value==null?0L:value.longValue();}
    }
    private static void advanceOwnership(CameraShadeView view) {
        if(view!=null)synchronized(ownershipGenerations){ownershipGenerations.put(view,ownershipGeneration(view)+1L);}
    }
    public static boolean current(CameraShadeView view) {
        return view!=null && active.get()==view && view.isAttachedToWindow();
    }
    public static boolean current(CameraShadeView view,long generation) {
        return current(view) && generation(view)==generation;
    }
    public static boolean owned(CameraShadeView view,long ownership) {
        return current(view) && ownershipGeneration(view)==ownership;
    }
    public static int localWidth(CameraShadeView view) {
        int width=view.getWidth();
        if(width<=0) width=view.c;
        return width>0?width:view.getResources().getDisplayMetrics().widthPixels;
    }
    public static int localHeight(CameraShadeView view) {
        int height=view.getHeight();
        if(height<=0) height=view.j;
        return height>0?height:view.getResources().getDisplayMetrics().heightPixels;
    }
    // Renderer threads read a published immutable rectangle, not live mutable Views.
    public static android.graphics.RectF viewport() {
        android.graphics.RectF rect=snapshot;
        return rect==null?null:new android.graphics.RectF(rect);
    }
    /** UI-thread gesture hit testing uses this same currently attached shade. */
    public static CameraShadeView gestureView1925() {
        CameraShadeView view=active.get();return current(view)?view:null;
    }
    private static void invalidateViewport(CameraShadeView view) {
        if(active.get()==view)snapshot=null;
        delivered.remove(view);
    }
    private static void publish(CameraShadeView view) {
        if(!current(view))return;
        // A failed measurement/target replaces the old viewport with unknown.
        // Retaining the previous rectangle would let renderer/feedback threads
        // crop a resized or temporarily unmeasured view using an obsolete size.
        if(view.v==null || view.getWidth()<=0 || view.getHeight()<=0){invalidateViewport(view);return;}
        int left=CameraShadeView.d0,right=CameraShadeView.e0;
        if(view.u<0 || view.t<0 || (long)view.u+view.t>=view.j || left<0 || right<0 || (long)left+right>=view.c){invalidateViewport(view);return;}
        android.graphics.RectF previous=snapshot;
        if(previous!=null && previous.left==left && previous.top==view.u
                && previous.right==view.c-right && previous.bottom==view.j-view.t)return;
        snapshot=new android.graphics.RectF(left,view.u,view.c-right,view.j-view.t);
        trace("layout_geometry",view,"width="+view.c+" height="+view.j+" top="+view.u+" bottom="+view.t
            +" ratio="+view.v.name());
    }
    /** Refresh even when the window is the same size but the camera/renderer is new. */
    public static void attached(CameraShadeView view) {
        if(view==null)return;
        CameraShadeView old=active.get();
        if(old!=null && old!=view){cancel(old);stopLayoutProbe(old);advanceOwnership(old);}
        // Native setup may already have installed this view's own animator.
        // Keep it and supervise it under the new attachment generation.
        removePending(view);stopLayoutProbe(view);advanceGeneration(view);
        active=new WeakReference<>(view);snapshot=null;delivered.remove(view);
        trace("layout_attached",view,"width="+view.getWidth()+" height="+view.getHeight());
        afterLayout(view);
    }
    private static boolean targetRepair(CameraShadeView view) {
        int width=view.getWidth(),height=view.getHeight();
        if(width<=0 || height<=0)return false;
        String ratio=view.v.name();
        boolean dimensions=view.c!=width || view.j!=height;
        // Never use physical screen dimensions for a laid-out preview container.
        if(dimensions){cancel(view);view.c=width;view.j=height;}
        if(!ratio.equals("RADIO_1_1") && !ratio.equals("RADIO_ROUND")) {
            CameraShadeView.d0=0;CameraShadeView.e0=0;
            CameraShadeView.b0=0;CameraShadeView.c0=width;
            if(view.p!=null)view.p.right=0;
            if(view.q!=null)view.q.left=width;
        }
        boolean changed=dimensions;
        if(ratio.equals("RADIO_3_4")) {
            int content=(int)(((long)width*4)/3);
            // Do not invent a crop for a container too small to fit the selected ratio.
            if(content<=height) {
                int top=Math.max(0,Math.min(height-content,view.getRatio34TopMargin()));
                int bottom=height-content-top;
                if(view.u!=top || view.t!=bottom) {
                    cancel(view);view.u=top;view.t=bottom;changed=true;
                }
            }
        }else if(dimensions) {
            // Other ratios retain the original native geometry policy.
            view.m(view.v,false,view.G,false);
        }
        // Rect extents must be refreshed even when only the container size changes.
        view.n.left=0;view.n.top=0;view.n.right=width;
        view.o.left=0;view.o.right=width;view.o.bottom=height;
        if(changed){advanceGeneration(view);delivered.remove(view);}
        return changed;
    }
    public static void detached(CameraShadeView view) {
        cancel(view);stopLayoutProbe(view); advanceGeneration(view);advanceOwnership(view);delivered.remove(view);
        if(active.get()==view){active=new WeakReference<>(null);snapshot=null;}
        if(view!=null)trace("layout_detached",view,"");
    }
    public static void afterLayout(CameraShadeView view) {
        if(!current(view))return;
        if(view.getWidth()<=0 || view.getHeight()<=0){invalidateViewport(view);return;}
        reconcile(view); ready(view);
    }
    private static void notifySettled(CameraShadeView view) {
        if(!current(view) || view.w!=null || view.v==null || view.c<=0 || view.j<=0)return;
        android.graphics.RectF rect=viewport();
        if(rect==null)return;
        android.graphics.RectF previous=delivered.get(view);
        if(rect.equals(previous))return;
        delivered.put(view,rect); // Mark before native callbacks, which may re-enter.
        trace("layout_settled",view,"width="+view.c+" height="+view.j+" visible_confirmed=false");
        view.b();
        probeSettledLayout(view);
    }
    /** Registration may occur after the first settled delivery, or off the UI thread.
     * Deliver after the native registration call has returned; never touch a View on
     * that caller thread, retain an old listener, or revive a removed registration.
     */
    public static void listenerAdded(Object listener) {
        final CameraShadeView owner=active.get();
        if(owner==null || listener==null)return;
        final WeakReference<CameraShadeView> view=new WeakReference<>(owner);
        final WeakReference<Object> target=new WeakReference<>(listener);
        final long token=generation(owner);
        owner.post(new Runnable(){public void run(){
            CameraShadeView currentView=view.get();Object currentListener=target.get();
            if(!current(currentView,token) || currentListener==null || CameraShadeView.V==null
                    || !CameraShadeView.V.contains(currentListener))return;
            delivered.remove(currentView);
            ready(currentView);
        }});
    }


    private static final class Pending {
        final WeakReference<CameraShadeView> view;
        final WeakReference<ValueAnimator> animation;
        final Runnable fallback;
        final Runnable firstFrame;
        final Runnable settle;
        final long generation,deadline;
        boolean framePosted;
        Pending(CameraShadeView v, ValueAnimator a) {
            view = new WeakReference<>(v);
            animation = new WeakReference<>(a);
            generation=PreviewLayout1922.generation(v);
            deadline=SystemClock.uptimeMillis()+SETTLE_LIMIT_MS;
            fallback = new Runnable() { public void run() { fire(Pending.this); } };
            firstFrame = new Runnable() { public void run() { fire(Pending.this); } };
            settle = new Runnable() { public void run() { settleExpired(Pending.this); } };
        }
    }
    private static void removePending(CameraShadeView view) {
        Pending p;
        synchronized (pending) { p = pending.remove(view); }
        if (p != null) {
            view.removeCallbacks(p.fallback);
            view.removeCallbacks(p.firstFrame);
            view.removeCallbacks(p.settle);
        }
    }
    /** Runs before native target calculation and when the shade detaches. */
    public static void cancel(CameraShadeView view) {
        if (view == null) return;
        removePending(view);
        ValueAnimator a = view.w;
        view.w = null; // invalidate ownership BEFORE any cancel callback
        if (a != null) {
            a.removeAllUpdateListeners();
            a.removeAllListeners();
            a.cancel();
        }
    }
    /** Supervise every transition, including one already started by native code. */
    public static void ready(CameraShadeView view) {
        if (!current(view)) return;
        if(view.getWidth()<=0 || view.getHeight()<=0) {
            // Constructor screen caches do not establish a measured shade.
            // Keep its exact native animator for the first real layout, but do
            // not start/end it or notify camera clients using guessed geometry.
            removePending(view);invalidateViewport(view);
            trace("layout_wait_measurement",view,"width="+view.getWidth()+" height="+view.getHeight());
            return;
        }
        if(view.v!=null && view.n!=null && view.o!=null && view.c>0 && view.j>0) {
            targetRepair(view);publish(view);
        }else invalidateViewport(view);
        if (view.w == null) { reconcile(view); notifySettled(view); return; }
        synchronized (pending) {
            Pending p = pending.get(view);
            if (p != null && p.animation.get() == view.w && p.generation==generation(view)) return;
        }
        removePending(view);
        Pending p = new Pending(view, view.w);
        synchronized (pending) { pending.put(view, p); }
        if(!view.w.isStarted())view.postDelayed(p.fallback, 500L);
        view.postDelayed(p.settle, SETTLE_LIMIT_MS);
    }
    /** Replace the native anonymous, unbound 200ms first-frame callback. */
    public static void schedule(CameraShadeView view) {
        if(!current(view))return;
        trace("shade_first_frame_notice",view,"visible_confirmed=false");
        delivered.remove(view); // The first-frame callback identifies a new rendering session.
        ready(view);
        Pending p;
        synchronized (pending) {
            p = pending.get(view);
            if (p == null || p.framePosted || view.w==null || view.w.isStarted()) return;
            p.framePosted = true;
        }
        view.postDelayed(p.firstFrame, 200L);
    }
    private static void fire(Pending p) {
        CameraShadeView view = p.view.get();
        ValueAnimator a = p.animation.get();
        if (!current(view,p.generation) || a == null) return;
        synchronized (pending) { if (pending.get(view) != p) return; }
        if (view.w != a) { removePending(view); return; }
        startCurrent(view);
    }
    public static void startCurrent(CameraShadeView view) {
        if (!current(view)) return;
        if(view.getWidth()<=0 || view.getHeight()<=0){ready(view);return;}
        ValueAnimator a = view.w;
        ready(view);
        if(view.w!=a)return;
        Pending p=pending.get(view);
        if(p!=null){view.removeCallbacks(p.fallback);view.removeCallbacks(p.firstFrame);}
        if (a != null && !a.isStarted()) a.start();
        // Do NOT clear view.w here; native code formerly lost the cancellation handle.
    }
    public static boolean owns(CameraShadeView view, ValueAnimator a) {
        return current(view) && a != null && view.w == a;
    }
    /** Stale end events cannot notify camera clients or modify the current target. */
    public static boolean finish(CameraShadeView view, Animator a) {
        if (!current(view) || a == null || view.w != a) return false;
        if(view.getWidth()<=0 || view.getHeight()<=0) {
            removePending(view);invalidateViewport(view);
            trace("layout_wait_measurement",view,"native_completion_deferred=true");
            return false;
        }
        removePending(view);
        view.w = null;
        reconcile(view,false);
        trace("layout_settled",view,"width="+view.getWidth()+" height="+view.getHeight()+" visible_confirmed=false");
        probeSettledLayout(view);
        return true; // keep the original native completion/listener notifications
    }
    /** A started animator can be paused by lifecycle or lose its final callback.
     * Finish the exact owned animation on the UI thread; preserve native listeners.
     * This watchdog is never renewed by onDraw or repeated first-frame callbacks.
     */
    private static void settleExpired(Pending p) {
        CameraShadeView view=p.view.get();ValueAnimator a=p.animation.get();
        if(!current(view,p.generation) || a==null || pending.get(view)!=p || view.w!=a)return;
        if(view.getWidth()<=0 || view.getHeight()<=0) {
            removePending(view);invalidateViewport(view);
            trace("layout_wait_measurement",view,"watchdog_deferred=true");
            return;
        }
        try { a.end(); } finally {
            if(view.w==a){cancel(view);reconcile(view);}
        }
    }
    /** Repair invalid measured targets immediately and settle stalled transitions.
     * Valid live interpolation remains native until its bounded completion.
     */
    public static void reconcile(CameraShadeView view) {
        reconcile(view,true);
    }
    private static void reconcile(CameraShadeView view,boolean notify) {
        if (!current(view)) return;
        if (view.v == null || view.getWidth() <= 0 || view.getHeight() <= 0
                || view.n == null || view.o == null) { invalidateViewport(view); return; }
        targetRepair(view);
        if(view.w!=null) {
            ready(view);
            Pending p=pending.get(view);
            if(p!=null && SystemClock.uptimeMillis()>=p.deadline)settleExpired(p);
            if(view.w!=null)return;
        }
        if (active.get()==view) {
            CameraShadeView.Q=view.u; CameraShadeView.R=view.t; CameraShadeView.S=view.j;
            CameraShadeView.U=view.v;
        }
        float top = view.u;
        String ratio = view.v.name();
        if (ratio.equals("RADIO_1_1") || ratio.equals("RADIO_ROUND")) top += 1f;
        if (view.r != view.u || view.s != view.t || view.n.bottom != top
                || view.o.top != view.j - view.t) {
            view.r = view.u;
            view.s = view.t;
            view.i();
        }
        publish(view);
        if(notify)notifySettled(view);
        else {android.graphics.RectF rect=viewport();if(rect!=null)delivered.put(view,rect);}
    }
    /** Inset changes must invalidate the target, not merely store A. */
    public static void offsetChanged(CameraShadeView view) {
        if (!current(view) || view.getWidth() <= 0 || view.getHeight() <= 0 || view.v == null
                || view.n == null || view.o == null) return;
        view.m(view.v, false, view.G, false);
    }

    private static final class LayoutProbe {
        final long ownership,epoch;
        Runnable follow;
        LayoutProbe(CameraShadeView view){ownership=ownershipGeneration(view);epoch=layoutEpoch();}
    }
    private static void stopLayoutProbe(CameraShadeView view) {
        if(view==null)return;
        LayoutProbe probe=layoutProbes.remove(view);
        try{if(probe!=null && probe.follow!=null)view.removeCallbacks(probe.follow);}catch(Throwable unavailable){}
    }
    private static long layoutEpoch() {
        try{return ManualLens170.number("epoch");}catch(Throwable unavailable){return 0L;}
    }
    /** Optional observation only: ordinary measurement/ratio generations may change
     * between startup and the final sample. The same attached view and foreground
     * camera epoch must still own the work. Never renew the two-sample budget.
     */
    private static void probeSettledLayout(CameraShadeView view) {
        try {
        if(!current(view) || view.w!=null || viewport()==null || layoutProbes.containsKey(view))return;
        final LayoutProbe probe=new LayoutProbe(view);
        layoutProbes.put(view,probe); // Mark before any optional View reads.
        if(!layoutSnapshot(view,1))return;
        final WeakReference<CameraShadeView> owner=new WeakReference<>(view);
        probe.follow=new Runnable(){public void run(){
            CameraShadeView attached=owner.get();probe.follow=null;
            if(!owned(attached,probe.ownership) || layoutProbes.get(attached)!=probe)return;
            if(layoutEpoch()!=probe.epoch){trace("layout_probe_stop",attached,"pass=2 reason=camera_epoch_changed");return;}
            layoutSnapshot(attached,2);
        }};
        if(!view.postDelayed(probe.follow,1200L))probe.follow=null;
        }catch(Throwable unavailable){}
    }
    private static final class LayoutNode {
        final Object view;
        final int parent,depth;
        LayoutNode(Object view,int parent,int depth){this.view=view;this.parent=parent;this.depth=depth;}
    }
    private static Object layoutRead(Object view,String method)throws ReflectiveOperationException {
        return view.getClass().getMethod(method).invoke(view);
    }
    private static Object optionalLayoutRead(Object view,String method) {
        try{return layoutRead(view,method);}catch(Throwable unavailable){return null;}
    }
    private static int layoutInt(Object view,String method)throws ReflectiveOperationException {
        Object value=layoutRead(view,method);
        if(!(value instanceof Number))throw new IllegalArgumentException("non_scalar_view_geometry");
        return ((Number)value).intValue();
    }
    private static float layoutFloat(Object view,String method,float fallback) {
        Object value=optionalLayoutRead(view,method);
        return value instanceof Number?((Number)value).floatValue():fallback;
    }
    private static int layoutFlag(Object view,String method) {
        Object value=optionalLayoutRead(view,method);
        return value instanceof Boolean?(((Boolean)value).booleanValue()?1:0):-1;
    }
    private static int layoutRenderKind(Object view) {
        Class<?> type=view.getClass();
        for(int i=0;i<8 && type!=null;i++,type=type.getSuperclass()) {
            if("android.view.TextureView".equals(type.getName()))return 1;
            if("android.view.SurfaceView".equals(type.getName()))return 2;
        }
        return 0;
    }
    private static int layoutSurfaceValid(Object view) {
        Object holder=optionalLayoutRead(view,"getHolder");
        if(holder==null)return -1;
        try {
            // SurfaceView's holder is often a non-public implementation. Invoke
            // through the public Android interface, never bypass access checks.
            Object surface=Class.forName("android.view.SurfaceHolder").getMethod("getSurface").invoke(holder);
            return surface==null?-1:layoutFlag(surface,"isValid");
        }catch(Throwable unavailable){return -1;}
    }
    /** Only public scalar geometry/lifecycle and bounded hierarchy APIs are read.
     * Deep rendering leaves and clickable visible controls take priority over generic
     * containers, while the emitted geometry budget remains 16 nodes per sample.
     * Every object belongs to this synchronous local traversal; no content/pixels,
     * layout setters, persistent draw observer or camera work is introduced.
     */
    private static boolean layoutSnapshot(CameraShadeView shade,int pass) {
        try {
            Object root=optionalLayoutRead(shade,"getRootView");
            if(root==null || !(optionalLayoutRead(root,"getWidth") instanceof Number)) {
                root=shade;
                for(int i=0;i<4;i++) {
                    Object parent=optionalLayoutRead(root,"getParent");
                    if(parent==null || parent==root || !(optionalLayoutRead(parent,"getWidth") instanceof Number))break;
                    root=parent;
                }
            }
            java.util.ArrayList<LayoutNode> nodes=new java.util.ArrayList<>();
            java.util.ArrayDeque<LayoutNode> queue=new java.util.ArrayDeque<>();
            java.util.IdentityHashMap<Object,Boolean> seen=new java.util.IdentityHashMap<>();
            queue.add(new LayoutNode(root,-1,0));seen.put(root,Boolean.TRUE);
            boolean scanTruncated=false;
            while(!queue.isEmpty() && nodes.size()<128) {
                LayoutNode node=queue.removeFirst();Object v=node.view;int index=nodes.size();nodes.add(node);
                Object childCount=optionalLayoutRead(v,"getChildCount");
                int children=childCount instanceof Number?Math.max(0,((Number)childCount).intValue()):0;
                if(node.depth>=10){if(children>0)scanTruncated=true;continue;}
                int inspect=Math.min(children,32);if(children>inspect)scanTruncated=true;
                for(int child=0;child<inspect;child++) {
                    if(nodes.size()+queue.size()>=128){scanTruncated=true;break;}
                    Object value=v.getClass().getMethod("getChildAt",Integer.TYPE).invoke(v,Integer.valueOf(child));
                    if(value==null || seen.containsKey(value))continue;
                    seen.put(value,Boolean.TRUE);queue.addLast(new LayoutNode(value,index,node.depth+1));
                }
            }
            boolean[] selected=new boolean[nodes.size()];java.util.ArrayList<Integer> chosen=new java.util.ArrayList<>();
            // Priority 3 preserves the outer root/shade. Rendering leaves and then
            // currently visible controls include their ancestor geometry when space
            // permits, so a collapsed parent is distinguishable from its leaf.
            for(int priority=3;priority>=0 && chosen.size()<16;priority--) {
                for(int i=0;i<nodes.size() && chosen.size()<16;i++) {
                    Object v=nodes.get(i).view;
                    int actual=(i==0 || v==shade)?3:layoutRenderKind(v)!=0?2:
                        (layoutFlag(v,"isClickable")==1 && layoutInt(v,"getVisibility")==0)?1:0;
                    if(actual!=priority || selected[i])continue;
                    java.util.ArrayList<Integer> ancestors=new java.util.ArrayList<>();
                    for(int at=i;at>=0;at=nodes.get(at).parent)if(!selected[at])ancestors.add(Integer.valueOf(at));
                    // Reserve the leaf itself even if an unusually deep branch
                    // cannot fit every ancestor into the remaining emission slots.
                    if(ancestors.size()>16-chosen.size())ancestors.clear();
                    if(ancestors.isEmpty())ancestors.add(Integer.valueOf(i));
                    for(int at=ancestors.size()-1;at>=0 && chosen.size()<16;at--){int n=ancestors.get(at).intValue();if(!selected[n]){selected[n]=true;chosen.add(Integer.valueOf(n));}}
                }
            }
            // Geometry plus optional state/transform events remain within 64 total
            // records per sample, including the final summary (same prior ceiling).
            int details=63-chosen.size();
            for(Integer selectedIndex:chosen) {
                int index=selectedIndex.intValue();LayoutNode node=nodes.get(index);Object v=node.view;
                String name=v.getClass().getSimpleName();if(name.length()>12)name=name.substring(0,12);
                trace("layout_node",shade,"pass="+pass+" n="+index+" p="+node.parent+" id="+layoutInt(v,"getId")
                    +" c="+name+" b="+layoutInt(v,"getLeft")+","+layoutInt(v,"getTop")+","+layoutInt(v,"getRight")+","+layoutInt(v,"getBottom")
                    +" s="+layoutInt(v,"getWidth")+","+layoutInt(v,"getHeight")+" v="+layoutInt(v,"getVisibility"));
                int render=layoutRenderKind(v),clickable=layoutFlag(v,"isClickable");
                if((render!=0 || clickable==1) && details-->0)trace("layout_view_state",shade,"pass="+pass+" n="+index+" render="+render+" clickable="+clickable
                    +" shown="+layoutFlag(v,"isShown")+" attached="+layoutFlag(v,"isAttachedToWindow")
                    +" available="+(render==1?layoutFlag(v,"isAvailable"):-1)+" surface_valid="+(render==2?layoutSurfaceValid(v):-1));
                float sx=layoutFloat(v,"getScaleX",1f),sy=layoutFloat(v,"getScaleY",1f);
                float tx=layoutFloat(v,"getTranslationX",0f),ty=layoutFloat(v,"getTranslationY",0f);
                float rotation=layoutFloat(v,"getRotation",0f),alpha=layoutFloat(v,"getAlpha",1f);
                boolean scaled=sx!=1f||sy!=1f,rotated=rotation!=0f;
                if((scaled||tx!=0f||ty!=0f) && details-->0)trace("layout_transform",shade,"pass="+pass+" n="+index+" sx="+sx+" sy="+sy+" tx="+tx+" ty="+ty);
                if((rotated||alpha!=1f) && details-->0)trace("layout_effect",shade,"pass="+pass+" n="+index+" rotation="+rotation+" alpha="+alpha);
                if((scaled||rotated) && details-->0)trace("layout_pivot",shade,"pass="+pass+" n="+index+" x="+layoutFloat(v,"getPivotX",0f)+" y="+layoutFloat(v,"getPivotY",0f));
            }
            trace("layout_probe",shade,"pass="+pass+" nodes="+chosen.size()+" scanned="+nodes.size()+" max_nodes=16 max_scan=128 max_depth=10 truncated="+(scanTruncated||nodes.size()>chosen.size())+" settled="+(shade.w==null));
            return true;
        }catch(Throwable unavailable){trace("layout_probe",shade,"pass="+pass+" unavailable="+unavailable.getClass().getSimpleName());return false;}
    }
}

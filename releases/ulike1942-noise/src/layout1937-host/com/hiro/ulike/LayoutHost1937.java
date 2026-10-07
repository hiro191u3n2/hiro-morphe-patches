package com.hiro.ulike;
import java.util.*;
import android.animation.*;
import android.graphics.RectF;
import android.os.SystemClock;
import com.bytedance.corecamera.ui.view.CameraShadeView;
import com.ss.android.vesdk.VEPreviewRadio;

/** Actual production coordinator; native shade writer/Animator behavior is modeled explicitly. */
public final class LayoutHost1937 {
    static int assertions;
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);assertions++;}
    static CameraShadeView view(){
        SystemClock.now=0;
        CameraShadeView.V.clear();
        CameraShadeView v=new CameraShadeView();v.c=742;v.j=1536;v.margin=131;
        v.u=v.r=131;v.t=v.s=416;v.i();PreviewLayout1922.attached(v);return v;
    }
    static ValueAnimator animation(final CameraShadeView v,boolean started){
        final ValueAnimator a=new ValueAnimator();v.w=a;
        a.ending=()->{if(PreviewLayout1922.owns(v,a)){v.r=v.u;v.s=v.t;v.i();}
            if(PreviewLayout1922.finish(v,a))v.b();};
        if(started)a.start();PreviewLayout1922.ready(v);return a;
    }
    static void badMask(CameraShadeView v){v.r=309;v.s=416;v.i();}
    static void correct(CameraShadeView v,String message){check(v.n.bottom==131 && v.o.top==1120 && v.o.top-v.n.bottom==989,message);}
    static void detach(CameraShadeView v){PreviewLayout1922.detached(v);v.attached=false;}
    static void added(Object listener){try{PreviewLayout1922.class.getMethod("listenerAdded",Object.class).invoke(null,listener);}catch(Exception e){throw new AssertionError(e);}}
    static long ownership(CameraShadeView view){try{return (Long)PreviewLayout1922.class.getMethod("ownershipGeneration",CameraShadeView.class).invoke(null,view);}catch(Exception e){throw new AssertionError(e);}}
    static boolean owned(CameraShadeView view,long epoch){try{return (Boolean)PreviewLayout1922.class.getMethod("owned",CameraShadeView.class,long.class).invoke(null,view,epoch);}catch(Exception e){throw new AssertionError(e);}}
    public static void main(String[] args){
        if(args.length>0 && args[0].equals("baseline")){
            CameraShadeView v=view();ValueAnimator a=animation(v,true);badMask(v);
            v.advance(10000);PreviewLayout1922.reconcile(v);
            check(v.n.bottom==309 && v.o.top==1120 && v.w==a,"1.9.36 reproduces persistent 811px mask despite correct target");
            System.out.println("BASELINE1936_STALLED_MASK_REPRODUCED=811");return;
        }
        CameraShadeView v=view();correct(v,"normal screenshot geometry preserved");
        ValueAnimator a=animation(v,true);badMask(v);
        PreviewLayout1922.reconcile(v);check(v.n.bottom==309,"live animation is allowed to interpolate");
        v.advance(1199);check(v.w==a,"watchdog allows native 200ms animation");
        int notifications=v.notifications;v.advance(1200);correct(v,"started orphan is settled within bound");
        check(v.w==null && a.ends==1 && v.notifications==notifications+1,"one native completion notification");
        int redraws=v.redraws;PreviewLayout1922.reconcile(v);PreviewLayout1922.ready(v);
        check(v.redraws==redraws && v.notifications==notifications+1 && v.queue.isEmpty(),"settled traversal converges without callback loop");detach(v);

        v=view();a=animation(v,false);PreviewLayout1922.schedule(v);
        check(v.queue.size()==3,"first frame, missing-frame start and settlement deadlines all owned");
        for(int i=0;i<30;i++){PreviewLayout1922.schedule(v);PreviewLayout1922.ready(v);PreviewLayout1922.reconcile(v);}
        check(v.queue.size()==3,"repeated callbacks do not renew or duplicate bounds");
        v.advance(199);check(a.starts==0,"first-frame delay maintained");v.advance(200);
        check(a.starts==1 && v.queue.size()==1,"start keeps settlement watchdog");badMask(v);v.advance(1200);correct(v,"pending-first-frame animation settles");detach(v);

        v=view();a=animation(v,false);v.advance(500);check(a.starts==1,"missing first frame receives bounded start");badMask(v);v.advance(1200);correct(v,"missing end after fallback settles");detach(v);
        v=view();a=animation(v,true);a.started=false;badMask(v);v.advance(1200);correct(v,"paused animator settles");detach(v);
        v=view();a=animation(v,true);badMask(v);PreviewLayout1922.attached(v);
        check(v.w==a && !a.cancelled,"native animator installed before attachment retains completion listeners");
        v.advance(1200);correct(v,"reattached native animator receives fresh bounded owner");detach(v);
        v=view();a=animation(v,true);badMask(v);SystemClock.now=1300;
        PreviewLayout1922.reconcile(v);correct(v,"draw repairs before paint if UI queue resumes after deadline");check(v.queue.isEmpty(),"draw settlement removes overdue runnable");detach(v);

        v=view();a=animation(v,true);badMask(v);List<Runnable> stale=new ArrayList<>();for(CameraShadeView.Task t:v.queue)stale.add(t.action);
        CameraShadeView old=v;detach(v);v=view();ValueAnimator newer=animation(v,true);
        for(Runnable r:stale)r.run();PreviewLayout1922.ready(old);PreviewLayout1922.afterLayout(old);PreviewLayout1922.reconcile(old);PreviewLayout1922.schedule(old);PreviewLayout1922.prepare(old);
        check(PreviewLayout1922.gestureView1925()==v && PreviewLayout1922.viewport().top==131,"old detached callbacks cannot reclaim active viewport");
        check(v.w==newer && newer.ends==0 && a.ends==0,"old callbacks cannot settle next instance");detach(v);

        v=view();a=animation(v,true);badMask(v);stale=new ArrayList<>();for(CameraShadeView.Task t:v.queue)stale.add(t.action);
        PreviewLayout1922.prepare(v);newer=animation(v,true);for(Runnable r:stale)r.run();
        check(v.w==newer && newer.ends==0 && a.cancelled,"old generation cannot terminate newer animation on same view");
        check(!PreviewLayout1922.finish(v,a) && !PreviewLayout1922.owns(v,a),"stale native end/update ignored");detach(v);

        v=view();a=animation(v,false);final CameraShadeView instantView=v;final ValueAnimator instantAnimator=a;
        a.instant=()->{if(PreviewLayout1922.finish(instantView,instantAnimator))instantView.b();};
        PreviewLayout1922.startCurrent(v);check(v.w==null && v.queue.isEmpty(),"zero-duration synchronous finish leaves no ownership or watchdog");detach(v);

        v=view();final CameraShadeView listenerView=v;final int[] callbackCount={0};
        java.util.function.Consumer<RectF> listener=rect->{callbackCount[0]++;check(rect.top==131 && rect.bottom==1120,"late native listener receives current viewport");
            PreviewLayout1922.reconcile(listenerView);PreviewLayout1922.ready(listenerView);};
        CameraShadeView.V.add(listener);notifications=v.notifications;added(listener);
        check(callbackCount[0]==0,"registration callback is posted after native initialization");v.advance(0);
        check(callbackCount[0]==1 && v.notifications==notifications+1,"late listener receives one handoff without recursive callback loop");detach(v);
        v=view();Object removed=new Object();CameraShadeView.V.add(removed);notifications=v.notifications;added(removed);CameraShadeView.V.remove(removed);v.advance(0);
        check(v.notifications==notifications,"removed listener cannot revive a geometry delivery");detach(v);
        v=view();a=animation(v,true);Object pendingListener=new Object();CameraShadeView.V.add(pendingListener);notifications=v.notifications;added(pendingListener);v.advance(0);
        check(v.notifications==notifications,"late listener waits for pending transition completion");v.advance(1200);
        check(v.notifications==notifications+1,"pending transition hands current geometry to new listener once");detach(v);
        v=view();Object epochListener=new Object();CameraShadeView.V.add(epochListener);added(epochListener);PreviewLayout1922.prepare(v);PreviewLayout1922.ready(v);notifications=v.notifications;v.advance(0);
        check(v.notifications==notifications,"old registration epoch is ignored after current epoch delivered geometry");detach(v);
        v=view();Object oldListener=new Object();CameraShadeView.V.add(oldListener);added(oldListener);CameraShadeView oldListenerView=v;detach(v);v=view();notifications=v.notifications;oldListenerView.advance(0);
        check(v.notifications==notifications && PreviewLayout1922.gestureView1925()==v,"old listener cannot hand off to a replacement view");detach(v);
        v=new CameraShadeView();v.attached=false;long beforeAttach=ownership(v);
        check(!owned(v,beforeAttach),"pre-attachment callback cannot execute before view is attached");
        v.attached=true;PreviewLayout1922.attached(v);
        check(owned(v,beforeAttach),"first attachment preserves valid onCreate controller work");
        PreviewLayout1922.prepare(v);PreviewLayout1922.ready(v);v.measuredWidth=742;v.measuredHeight=1536;PreviewLayout1922.afterLayout(v);
        check(owned(v,beforeAttach),"measurement-only geometry changes preserve pending native controller layout");
        detach(v);v.attached=true;PreviewLayout1922.attached(v);
        check(!owned(v,beforeAttach),"detach and reattach invalidate previous binding");
        long rebound=ownership(v);check(owned(v,rebound),"new callbacks bind reattached view");
        CameraShadeView displaced=v;v=view();check(!owned(displaced,rebound),"replacement invalidates old still-attached owner");detach(v);

        v=view();a=animation(v,true);v.u=v.r=309;v.t=v.s=416;v.i();PreviewLayout1922.reconcile(v);
        correct(v,"invalid target repaired even with running handle");check(a.cancelled,"obsolete animation cancelled before target rewrite");detach(v);
        v=view();a=animation(v,true);v.margin=150;PreviewLayout1922.reconcile(v);
        check(v.u==150 && v.t==397 && v.o.top-v.n.bottom==989,"late inset change repairs active transition");detach(v);
        v=view();a=animation(v,true);v.measuredWidth=1080;v.measuredHeight=2340;PreviewLayout1922.afterLayout(v);
        check(v.c==1080 && v.j==2340 && v.o.top-v.n.bottom==1440 && v.n.right==1080,"actual measured size wins over previous display cache");detach(v);
        v=view();v.measuredWidth=0;v.measuredHeight=0;PreviewLayout1922.attached(v);
        check(PreviewLayout1922.viewport()==null,"unlaid-out view does not publish guessed display rectangle");
        v.measuredWidth=742;v.measuredHeight=1536;PreviewLayout1922.afterLayout(v);correct(v,"first layout publishes actual geometry");detach(v);

        for(VEPreviewRadio ratio:VEPreviewRadio.values()){
            if(ratio==VEPreviewRadio.RADIO_3_4)continue;
            v=view();v.v=ratio;v.u=v.r=80;v.t=v.s=333;v.i();a=animation(v,true);v.r=309;v.i();v.advance(1200);
            check(v.u==80 && v.t==333,"native target retained for "+ratio);
            check(v.n.bottom==80+((ratio==VEPreviewRadio.RADIO_1_1||ratio==VEPreviewRadio.RADIO_ROUND)?1:0),"native square seam retained "+ratio);detach(v);
        }
        v=view();v.c=v.measuredWidth=1200;v.j=v.measuredHeight=700;v.u=v.r=10;v.t=v.s=10;v.i();PreviewLayout1922.afterLayout(v);
        check(v.u==10 && v.t==10,"landscape container never assigned impossible portrait crop");detach(v);
        for(int width:new int[]{320,360,742,778,1080,1440})for(int top:new int[]{0,40,61,131,150}){
            v=view();v.c=v.measuredWidth=width;v.j=v.measuredHeight=width*2+100;v.margin=top;
            v.u=v.r=309;v.t=v.s=416;v.i();PreviewLayout1922.afterLayout(v);RectF r=PreviewLayout1922.viewport();
            check(r.top==top && r.right==width && (int)(r.bottom-r.top)==(int)((long)width*4/3),"3:4 invariant width="+width+" top="+top);detach(v);
        }
        for(int i=0;i<100;i++){
            v=view();a=animation(v,(i&1)==0);badMask(v);
            if(i%3==0)PreviewLayout1922.schedule(v);
            if(i%4==0){v.advance(150);PreviewLayout1922.prepare(v);a=animation(v,true);badMask(v);}
            v.advance(2000);correct(v,"repeat startup permutation "+i);detach(v);
            check(PreviewLayout1922.viewport()==null && v.queue.isEmpty(),"repeat detached state cleared "+i);
        }
        PreviewLayout1922.ready(null);PreviewLayout1922.schedule(null);PreviewLayout1922.reconcile(null);PreviewLayout1922.offsetChanged(null);PreviewLayout1922.detached(null);
        System.out.println("HOST_LAYOUT1937_ASSERTIONS="+assertions);
        System.out.println("MOCKED_ANDROID_NOT_A_DEVICE_TEST");
    }
}

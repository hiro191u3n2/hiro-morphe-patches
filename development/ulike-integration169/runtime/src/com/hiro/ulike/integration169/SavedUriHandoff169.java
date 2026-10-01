package com.hiro.ulike.integration169;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import com.hiro.ulike.hdr.gainmap.GainmapSave;
import com.hiro.ulike.hdr.photo.*;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;

/** Actual pinned ULike automatic-save UI adapter. The Bitmap editor is explicitly unsupported.
 * No Bitmap is invented and the old Bitmap holder/save chain is not started for an owned shot.
 * New capture gate remains disabled independently of this callable boundary implementation. */
public final class SavedUriHandoff169 {
    private SavedUriHandoff169() {}
    private static final Map<Object,Token> ACTIVE=new IdentityHashMap<>();
    private static final ArrayList<WeakReference<Object>> RETIRED=new ArrayList<>();
    /** Runs on main when that Handler accepts work; if it is unavailable, runs on the calling
     * worker with an explicit UI-unavailable failure so a committed URI is never lost. */
    public interface Completion { void finished(Uri committedPhoto,Exception uiFailure); }
    public static final class Token {
        final Object bitmapCallback,takeCallback,scene,controller,bridge,autoSave,identity;
        final long epoch,startedMs;
        boolean dimensions,terminal,saving;
        volatile Uri committed;
        Token(Object callback,Object take,Object scene,Object controller,Object bridge,Object autoSave,Object identity,long epoch){
            bitmapCallback=callback;takeCallback=take;this.scene=scene;this.controller=controller;this.bridge=bridge;this.autoSave=autoSave;this.identity=identity;this.epoch=epoch;startedMs=SystemClock.elapsedRealtime();
        }
    }
    static synchronized Token prepare(Object callback,Object identity,long epoch)throws Exception {
        exact(callback,"i.f.l.r.v$a");Object take=AppHook169.publicField(callback,"c");exact(take,"i.f.l.r.s");
        Object ui=call(AppHook169.publicField(take,"g"),"l");
        if(!Boolean.TRUE.equals(call(call(ui,"a"),"a")))throw new UnsupportedOperationException("The original Bitmap editor has no HDR URI handoff; automatic save is required");
        Object listener=AppHook169.publicField(take,"i"),scene;
        if(listener.getClass().getName().equals("i.f.l.n.q.y.k"))scene=AppHook169.publicField(listener,"b");
        else if(listener.getClass().getName().equals("i.f.l.n.q.y.l"))scene=AppHook169.publicField(listener,"c");
        else throw new UnsupportedOperationException("Unreviewed picture listener");
        exact(scene,"i.o.a.b1.a.g.y$k");Object controller=AppHook169.publicField(scene,"b"),bridge=call(controller,"A0"),autoSave=call(bridge,"c");
        Class<?> base=Class.forName("i.o.a.b1.a.e.f",false,bridge.getClass().getClassLoader());
        if(!base.isInstance(bridge))throw new UnsupportedOperationException("Unreviewed application bridge");
        exact(autoSave,"i.o.a.b1.a.b.c");
        if(AppHook169.publicField(take,"h")!=null || !ACTIVE.isEmpty())throw new IllegalStateException("Legacy image holder already started or another handoff active");
        Token token=new Token(callback,take,scene,controller,bridge,autoSave,identity,epoch);ACTIVE.put(take,token);return token;
    }
    /** Hook at the real TakePictureCallbackImpl.c(width,height), before it creates a Bitmap holder. */
    public static synchronized boolean dimensions(Object take,int width,int height) {
        if(!CandidateGate169.enabled())return false;
        Token token=ACTIVE.get(take);
        if(token==null){for(int i=RETIRED.size()-1;i>=0;i--){Object old=RETIRED.get(i).get();if(old==null)RETIRED.remove(i);else if(old==take)return true;}return false;}
        if(width<1 || height<1)throw new IllegalArgumentException("Invalid capture dimensions");
        token.dimensions=true;return true;
    }
    /** Synchronous worker save; original UI success is posted only after committed URI verification.
     * The completion callback distinguishes a stored photograph from a later UI failure. */
    public static Uri saveAndComplete(CameraBridge169.OwnedShot shot,AndroidPhotoTransaction transaction,
            PairedStore.Pair processedPair,GainmapSave.QualityLimits limits,GainmapSave.Codec codec,Completion completion)throws Exception {
        if(shot==null || transaction==null || completion==null)throw new NullPointerException();
        if(Looper.myLooper()==Looper.getMainLooper())throw new IllegalStateException("Photo encoding cannot run on UI thread");
        final Token token=shot.handoff;
        synchronized(SavedUriHandoff169.class){
            if(token==null || ACTIVE.get(token.takeCallback)!=token || token.identity!=shot.shotIdentity || token.epoch!=shot.shotEpoch || token.terminal || token.saving || !token.dimensions)throw new IllegalStateException("No exact live capture handoff");
            if(transaction.staging.identity.exactSource!=shot.pixels || processedPair.identity!=transaction.staging.identity)throw new IllegalArgumentException("Transaction belongs to another captured image");
            token.saving=true;
        }
        final Uri uri;
        try {
            uri=transaction.savePair(processedPair,limits,codec);token.committed=uri;
            PendingPhotoStore.Entry stored=new MediaStorePhotos(AppHook169.app).inspect(uri.toString(),transaction.staging.plan);
            if(stored==null || !stored.owned || !stored.matching || stored.pending || stored.bytes<1)throw new IOException("Returned URI is not the committed owned transaction");
        } catch(Exception failure) {
            if(token.committed!=null){retire(token);postCommittedFailure(token,failure,completion);}
            else fail(token,"save",failure);
            throw failure;
        }
        retire(token);
        final int width=transaction.staging.identity.frame.width,height=transaction.staging.identity.frame.height;
        Handler main=new Handler(Looper.getMainLooper());
        if(!main.post(()->{
            Exception failure=null;
            // Each action is attempted once; one failed listener cannot prevent shutter cleanup.
            failure=attempt(failure,()->call(call(token.autoSave,"i"),"m",new Class<?>[]{String.class},uri.toString()));
            failure=attempt(failure,()->call(token.autoSave,"k"));
            int duration=(int)Math.min(Integer.MAX_VALUE,Math.max(0,SystemClock.elapsedRealtime()-token.startedMs));
            failure=attempt(failure,()->call(token.autoSave,"l",new Class<?>[]{boolean.class,int.class,String.class,String.class,org.json.JSONObject.class},true,duration,uri.toString(),"",null));
            failure=attempt(failure,()->{
                Intent intent=new Intent("com.lemon.faceu.action.scan_file");intent.setPackage(AppHook169.app.getPackageName());intent.putExtra("com.lemon.faceu.action.scan_file_key",uri.toString());AppHook169.app.sendBroadcast(intent);
            });
            failure=attempt(failure,()->cleanupSuccess(token,width,height));
            completion.finished(uri,failure);
        }))completion.finished(uri,new IOException("Photo is committed but UI handler is unavailable; UI cleanup not executed"));
        return uri;
    }
    private static void cleanupSuccess(Token t,int width,int height)throws Exception {
        Exception error=null;
        error=attempt(error,()->call(t.controller,"R1",new Class<?>[]{boolean.class},false));
        error=attempt(error,()->call(t.controller,"Q1",new Class<?>[]{long.class},SystemClock.uptimeMillis()));
        error=attempt(error,()->call(call(t.controller,"P0"),"c"));
        error=attempt(error,()->{
            if(Boolean.TRUE.equals(AppHook169.publicField(t.scene,"g"))){Object state=AppHook169.publicField(t.scene,"h");if(state!=null){Object value=call(state,"q");Class<?> type=Class.forName("i.f.l.u.p",false,value.getClass().getClassLoader());invoke(type.getMethod("h",type,Object.class,boolean.class,int.class,Object.class),null,value,Boolean.TRUE,false,2,null);}}
        });
        error=attempt(error,()->{Object record=AppHook169.publicField(t.scene,"f");call(record,"setGenWidth",new Class<?>[]{int.class},width);call(record,"setGenHeight",new Class<?>[]{int.class},height);});
        error=attempt(error,()->call(t.scene,"b"));
        error=attempt(error,()->call(AppHook169.publicField(t.takeCallback,"d"),"I",new Class<?>[]{int.class},1));
        error=attempt(error,()->{Class<?> base=Class.forName("i.o.a.b1.a.e.f",false,t.bridge.getClass().getClassLoader());invoke(base.getMethod("t",base),null,t.bridge);});
        if(error!=null)throw error;
    }
    static void rejectUnprepared(Object callback,Exception failure){
        if(callback==null || !callback.getClass().getName().equals("i.f.l.r.v$a"))return;
        new Handler(Looper.getMainLooper()).post(()->{try{call(callback,"onImageError",new Class<?>[]{int.class,int.class},-1,-1);}catch(Exception error){failure.addSuppressed(error);android.util.Log.e("ULike169","Rejected capture cleanup failed",failure);}});
    }
    static void fail(Token token,String stage,Exception failure) {
        if(token==null)return;
        synchronized(SavedUriHandoff169.class){if(token.terminal || token.committed!=null)return;retire(token);}
        new Handler(Looper.getMainLooper()).post(()->{
            try {call(token.bitmapCallback,"onImageError",new Class<?>[]{int.class,int.class},-1,-1);}
            catch(Exception callback){failure.addSuppressed(callback);android.util.Log.e("ULike169","Original capture failure cleanup failed at "+stage,failure);}
        });
    }
    private static void postCommittedFailure(Token token,Exception failure,Completion completion){
        if(!new Handler(Looper.getMainLooper()).post(()->{
            Exception combined=attempt(failure,()->cleanupUiWithoutSuccess(token));completion.finished(token.committed,combined);
        })){failure.addSuppressed(new IOException("UI handler unavailable; UI cleanup not executed"));completion.finished(token.committed,failure);}
    }
    /** Restore camera/shutter controls without asserting successful photo inspection or firing save-success listeners. */
    private static void cleanupUiWithoutSuccess(Token t)throws Exception {
        Exception error=null;
        error=attempt(error,()->call(t.controller,"R1",new Class<?>[]{boolean.class},false));
        error=attempt(error,()->call(t.controller,"Q1",new Class<?>[]{long.class},SystemClock.uptimeMillis()));
        error=attempt(error,()->call(call(t.controller,"P0"),"c"));
        error=attempt(error,()->call(t.scene,"a"));
        error=attempt(error,()->{if(Boolean.TRUE.equals(AppHook169.publicField(t.scene,"d"))){Class<?> controller=Class.forName("i.o.a.b1.a.g.y",false,t.scene.getClass().getClassLoader());invoke(t.scene.getClass().getMethod("e",controller),null,t.controller);}});
        error=attempt(error,()->call(AppHook169.publicField(t.takeCallback,"d"),"I",new Class<?>[]{int.class},1));
        error=attempt(error,()->{Class<?> base=Class.forName("i.o.a.b1.a.e.f",false,t.bridge.getClass().getClassLoader());invoke(base.getMethod("t",base),null,t.bridge);});
        if(error!=null)throw error;
    }
    private static synchronized void retire(Token token){if(ACTIVE.get(token.takeCallback)==token)ACTIVE.remove(token.takeCallback);token.terminal=true;RETIRED.removeIf(r->r.get()==null);RETIRED.add(new WeakReference<>(token.takeCallback));}
    private interface Action {void run()throws Exception;}
    private static Exception attempt(Exception before,Action action){try{action.run();}catch(Exception failure){if(before==null)return failure;before.addSuppressed(failure);}return before;}
    private static void exact(Object value,String name){if(value==null || !value.getClass().getName().equals(name))throw new IllegalArgumentException("Unreviewed callback class: "+name);}
    private static Object call(Object target,String name)throws Exception{return call(target,name,new Class<?>[0]);}
    private static Object call(Object target,String name,Class<?>[] signature,Object...args)throws Exception{return invoke(target.getClass().getMethod(name,signature),target,args);}
    private static Object invoke(Method method,Object target,Object...args)throws Exception{try{return method.invoke(target,args);}catch(InvocationTargetException e){Throwable cause=e.getCause();if(cause instanceof Exception)throw (Exception)cause;if(cause instanceof Error)throw (Error)cause;throw e;}}
}

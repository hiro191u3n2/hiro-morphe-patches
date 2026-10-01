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
    public interface Completion {
        void finished(Uri committedPhoto,Exception uiFailure);
        /** Separate from finished: candidateUri must never be treated as a committed photograph.
         * The handoff also posts an explicit user-visible message; no implicit retry is allowed.
         * Implementations may offer a read-only recovery check with PhotoTransaction.reconcile. */
        default void publicationUncertain(PublicationOutcome169.UnconfirmedPhoto photo,Exception failure) {
            android.util.Log.e("ULike169","Photo publication is unconfirmed; transaction="+photo.transactionId,failure);
        }
    }
    public static final class Token {
        final Object bitmapCallback,takeCallback,scene,controller,bridge,autoSave,identity;
        final long epoch,startedMs;
        boolean dimensions,terminal,saving;
        final PublicationOutcome169 publication=new PublicationOutcome169();
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
        final PublicationOutcome169.SaveAttempt attempt;
        synchronized(SavedUriHandoff169.class){
            if(token==null || ACTIVE.get(token.takeCallback)!=token || token.identity!=shot.shotIdentity || token.epoch!=shot.shotEpoch || token.terminal || token.saving || !token.dimensions)throw new IllegalStateException("No exact live capture handoff");
            if(transaction.staging.identity.exactSource!=shot.pixels || processedPair.identity!=transaction.staging.identity)throw new IllegalArgumentException("Transaction belongs to another captured image");
            attempt=token.publication.beginSave();token.saving=true;
        }
        final Uri uri;
        try {
            uri=transaction.savePair(processedPair,limits,codec);
            // The core sets this only after verifying exact URI, ownership, bytes and visible
            // status. Read it before any Android URI conversion that could itself fail.
            String receipt=transaction.staging.confirmedPublishedUri();
            if(receipt==null)throw new IOException("No confirmed publication receipt returned");
            token.publication.committed(attempt,receipt);
            if(!receipt.equals(uri.toString()))throw new IOException("Returned URI differs from the confirmed transaction");
        } catch(PhotoTransaction.PublicationUncertainException failure) {
            PublicationOutcome169.UnconfirmedPhoto unconfirmed=new PublicationOutcome169.UnconfirmedPhoto(
                failure.transactionId,failure.candidateUri,failure.expectedBytes,failure.identitySha256);
            token.publication.uncertain(attempt,unconfirmed);retire(token);
            postUncertain(token,unconfirmed,failure,completion);
            throw failure;
        } catch(Exception failure) {
            try{reportSaveFailure(token,attempt,transaction,failure,completion);}
            catch(Exception|Error reporting){if(reporting!=failure)failure.addSuppressed(reporting);}
            throw failure;
        } catch(Error error) {
            Exception failure=new IOException("Photo save worker terminated",error);
            try{reportSaveFailure(token,attempt,transaction,failure,completion);}
            catch(Exception|Error reporting){if(reporting!=error)error.addSuppressed(reporting);}
            throw error;
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
    private static void reportSaveFailure(Token token,PublicationOutcome169.SaveAttempt attempt,
            AndroidPhotoTransaction transaction,Exception failure,Completion completion){
        String receipt=transaction.staging.confirmedPublishedUri();
        if(receipt!=null){
            if(token.publication.snapshot().kind==PublicationOutcome169.Kind.PUBLISHING)
                token.publication.committed(attempt,receipt);
            retire(token);postCommittedFailure(token,failure,completion);
        }else failSave(token,attempt,"save",failure);
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
        synchronized(SavedUriHandoff169.class){if(token.terminal || !token.publication.failBeforePublication())return;retire(token);}
        postImageError(token,stage,failure);
    }
    private static void failSave(Token token,PublicationOutcome169.SaveAttempt attempt,String stage,Exception failure){
        synchronized(SavedUriHandoff169.class){
            if(token.terminal)return;
            token.publication.failedSave(attempt);retire(token);
        }
        postImageError(token,stage,failure);
    }
    private static void postImageError(Token token,String stage,Exception failure){
        new Handler(Looper.getMainLooper()).post(()->{
            try {call(token.bitmapCallback,"onImageError",new Class<?>[]{int.class,int.class},-1,-1);}
            catch(Exception callback){failure.addSuppressed(callback);android.util.Log.e("ULike169","Original capture failure cleanup failed at "+stage,failure);}
        });
    }
    private static void postCommittedFailure(Token token,Exception failure,Completion completion){
        if(!new Handler(Looper.getMainLooper()).post(()->{
            Exception combined=attempt(failure,()->cleanupUiWithoutSuccess(token));completion.finished(Uri.parse(token.publication.snapshot().committedUri),combined);
        })){failure.addSuppressed(new IOException("UI handler unavailable; UI cleanup not executed"));completion.finished(Uri.parse(token.publication.snapshot().committedUri),failure);}
    }
    private static void postUncertain(Token token,PublicationOutcome169.UnconfirmedPhoto photo,
            PhotoTransaction.PublicationUncertainException failure,Completion completion){
        // No saved listener, last-photo assignment, media scan, or onImageError is emitted.
        // The transaction journal retains the same URI so a later check cannot insert a duplicate.
        try{
            if(new Handler(Looper.getMainLooper()).post(()->{
                try{
                    attempt(failure,()->cleanupUiWithoutSuccess(token));
                    attempt(failure,()->android.widget.Toast.makeText(AppHook169.app,
                        "保存結果を確認できません。保存済みの可能性があります。ギャラリーを確認してください。",
                        android.widget.Toast.LENGTH_LONG).show());
                }catch(Error uiError){
                    failure.addSuppressed(uiError);throw uiError;
                }finally{deliverUncertainty(completion,photo,failure);}
            }))return;
            failure.addSuppressed(new IOException("Publication is unconfirmed and the UI handler is unavailable; UI cleanup/message not executed"));
        }catch(RuntimeException|Error dispatch){if(dispatch!=failure)failure.addSuppressed(dispatch);}
        deliverUncertainty(completion,photo,failure);
    }
    private static void deliverUncertainty(Completion completion,PublicationOutcome169.UnconfirmedPhoto photo,Exception failure){
        try{completion.publicationUncertain(photo,failure);}
        catch(Exception|Error callback){
            if(callback!=failure)failure.addSuppressed(callback);
            android.util.Log.e("ULike169","Unconfirmed publication callback failed; recovery journal remains available",failure);
        }
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
    private static Exception attempt(Exception before,Action action){try{action.run();}catch(Exception failure){if(before==null)return failure;if(before!=failure)before.addSuppressed(failure);}return before;}
    private static void exact(Object value,String name){if(value==null || !value.getClass().getName().equals(name))throw new IllegalArgumentException("Unreviewed callback class: "+name);}
    private static Object call(Object target,String name)throws Exception{return call(target,name,new Class<?>[0]);}
    private static Object call(Object target,String name,Class<?>[] signature,Object...args)throws Exception{return invoke(target.getClass().getMethod(name,signature),target,args);}
    private static Object invoke(Method method,Object target,Object...args)throws Exception{try{return method.invoke(target,args);}catch(InvocationTargetException e){Throwable cause=e.getCause();if(cause instanceof Exception)throw (Exception)cause;if(cause instanceof Error)throw (Error)cause;throw e;}}
}

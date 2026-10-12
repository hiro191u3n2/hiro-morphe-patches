package com.hiro.ulike;

import android.graphics.SurfaceTexture;
import android.os.Build;
import android.util.Log;
import android.view.Surface;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;

/** The provider belongs to one teardown generation, not to a mutable server slot. */
public final class ProviderLifecycle1929 {
    private static final Map<Object, Release> releases = new WeakHashMap<>();
    private ProviderLifecycle1929() {}
    static Object get(Object o, String n) throws ReflectiveOperationException {
        return o == null ? null : OpticalZoom.field(o, n);
    }
    static void put(Object o, String n, Object v) throws ReflectiveOperationException {
        for (Class<?> c=o.getClass(); c!=null; c=c.getSuperclass()) {
            try { Field f=c.getDeclaredField(n); f.setAccessible(true); f.set(o,v); return; }
            catch (NoSuchFieldException absent) { /* inherited native field */ }
        }
        throw new NoSuchFieldException(n);
    }
    static Object call(Object o, String n) throws ReflectiveOperationException {
        if(o==null)return null;
        Method m=o.getClass().getMethod(n);m.setAccessible(true);return m.invoke(o);
    }
    /** Copy every identity field, including Surface, pixel format and primary status.
     * Freeze the mutable size value; an in-place size change must invalidate the cache. */
    public static void copy(Object destination, Object source) {
        i.s.a.w.l0.c.a d=(i.s.a.w.l0.c.a)destination, s=(i.s.a.w.l0.c.a)source;
        d.a=s.a;d.b=s.b==null?null:new i.s.a.w.w(s.b.c,s.b.j);
        d.c=s.c;d.d=s.d;d.e=s.e;d.f=s.f;d.g=s.g;d.h=s.h;d.i=s.i;
    }
    public static boolean same(Object first, Object second) {
        if(!(first instanceof i.s.a.w.l0.c.a)||!(second instanceof i.s.a.w.l0.c.a))return false;
        i.s.a.w.l0.c.a a=(i.s.a.w.l0.c.a)first,b=(i.s.a.w.l0.c.a)second;
        return a.b!=null&&b.b!=null&&a.a==b.a&&a.b.c==b.b.c&&a.b.j==b.b.j
                &&a.c==b.c&&a.d==b.d&&a.e==b.e&&a.f==b.f&&a.g==b.g&&a.h==b.h&&a.i==b.i;
    }
    /** Only used by addCameraProvider's reuse check, not by capture/recording code. */
    public static i.s.a.w.l0.c liveManager(Object camera) {
        try {
            i.s.a.w.l0.c manager=(i.s.a.w.l0.c)call(camera,"U");
            Object provider=call(manager,"h");
            if(provider==null||get(provider,"d")!=camera)return null;
            Object format=get(provider,"b");String name=format instanceof Enum?((Enum<?>)format).name():"";
            if(name.equals("PIXEL_FORMAT_OpenGL_OES")||name.equals("PIXEL_FORMAT_Recorder")) {
                Object surface=call(provider,"d"),texture=call(provider,"f");
                if(!(surface instanceof Surface)||!((Surface)surface).isValid())return null;
                if(Build.VERSION.SDK_INT>=26&&texture instanceof SurfaceTexture&&((SurfaceTexture)texture).isReleased())return null;
            }
            return manager;
        } catch(ReflectiveOperationException|RuntimeException|LinkageError e) {
            Log.w("ULikeProvider1929","Unusable cached provider will be rebuilt",e);return null;
        }
    }
    private static final class Release {
        final Object manager,provider,lock;final boolean releaseTexture;
        Release(Object manager,Object provider,Object lock,boolean releaseTexture){
            this.manager=manager;this.provider=provider;this.lock=lock;this.releaseTexture=releaseTexture;
        }
    }
    /** Called in the teardown Runnable's constructor, before it enters the queue. */
    public static void captureRelease(Object task,Object server) {
        try {
            Object manager=get(server,"mProviderManager"), lock=get(server,"mSurfaceTextureLock");
            if(lock==null)throw new IllegalStateException("Missing native texture lock");
            synchronized(lock) {
                Release r=new Release(manager,call(manager,"h"),lock,Boolean.TRUE.equals(get(server,"mbNeedReleaseSurfaceTexture")));
                synchronized(releases){releases.put(task,r);}
            }
        } catch(ReflectiveOperationException|RuntimeException|LinkageError e) {
            Log.w("ULikeProvider1929","Teardown ownership could not be captured; live provider is protected",e);
        }
    }
    /** Never re-read the mutable server slot as the object to release. */
    public static void release(Object task,Object server) {
        Release r; synchronized(releases){r=releases.remove(task);}
        if(r==null||r.provider==null)return;
        try {
            synchronized(r.lock) {
                Object liveManager=get(server,"mProviderManager"), liveProvider=call(liveManager,"h");
                if(liveManager!=r.manager&&liveProvider==r.provider)return; // adopted by the new generation
                boolean same=liveManager==r.manager&&liveProvider==r.provider;
                Object oldTexture=call(r.provider,"f"),liveTexture=call(liveProvider,"f");
                boolean shared=!same&&oldTexture!=null&&oldTexture==liveTexture;
                Object oldSurface=call(r.provider,"d"), liveSurface=call(liveProvider,"d");
                if(!same&&oldSurface!=null&&oldSurface==liveSurface){
                    // A supplied Surface can be adopted without a new wrapper.
                    // Detach the old listener but never release the shared target.
                    put(r.provider,"a",get(r.provider,"j"));
                    if(call(r.manager,"h")==r.provider)put(r.manager,"a",null);
                    return;
                }
                if(same)put(server,"mbNeedReleaseSurfaceTexture",false);
                if(r.releaseTexture&&!shared)call(r.provider,"p");
                // The old manager may itself now contain another provider: detach only
                // the captured object in that case, never its replacement.
                if(call(r.manager,"h")==r.provider)call(r.manager,"n");else call(r.provider,"o");
            }
            Log.i("ULikeProvider1929","Released the captured camera provider only");
        } catch(ReflectiveOperationException|RuntimeException|LinkageError e) {
            Log.w("ULikeProvider1929","Captured provider cleanup failed; no global cleanup attempted",e);
        }
    }
}

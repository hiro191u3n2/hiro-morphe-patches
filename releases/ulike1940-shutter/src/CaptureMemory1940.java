package com.hiro.ulike;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;
import android.os.Build;
import java.lang.reflect.Method;

/** Read process/system pressure for Android 8+ native-backed Bitmap ownership.
 * No camera operation is issued by a budget check. Missing metrics keep serial saving. */
public final class CaptureMemory1940 {
    private CaptureMemory1940() {}
    private static volatile Context application;
    static long javaAvailable(){Runtime r=Runtime.getRuntime();return Math.max(0,r.maxMemory()-(r.totalMemory()-r.freeMemory()));}
    static long nativeAvailable(){
        if(Build.VERSION.SDK_INT<26)return javaAvailable();
        try{
            Context context=application;
            if(context==null){
                Class<?> core=Class.forName("i.n.c.a.b.g");Method get=core.getMethod("j");get.setAccessible(true);
                Object singleton=get.invoke(null);Method current=core.getMethod("i");current.setAccessible(true);
                Object value=current.invoke(singleton);if(!(value instanceof Context))return 0;
                context=((Context)value).getApplicationContext();if(context==null)context=(Context)value;application=context;
            }
            Object service=context.getSystemService(Context.ACTIVITY_SERVICE);if(!(service instanceof ActivityManager))return 0;
            ActivityManager.MemoryInfo memory=new ActivityManager.MemoryInfo();((ActivityManager)service).getMemoryInfo(memory);
            return MemoryBudget1940.nativeHeadroom(memory.availMem,memory.threshold,memory.totalMem,
                (long)Debug.getPss()*1024L,Debug.getNativeHeapAllocatedSize(),memory.lowMemory);
        }catch(Exception unavailable){return 0;}catch(LinkageError unavailable){return 0;}catch(OutOfMemoryError unavailable){return 0;}
    }
    static boolean allows(long pixels,long outputPixels,int bytesPerPixel,long qualityFloor){
        if(Build.VERSION.SDK_INT<26)
            return SaveQueue1935.processingMemoryAllows(javaAvailable(),pixels,outputPixels)
                && MemoryBudget1940.javaAllows(javaAvailable(),pixels,qualityFloor);
        return MemoryBudget1940.permits(javaAvailable(),nativeAvailable(),pixels,outputPixels,bytesPerPixel,qualityFloor);
    }
}

package com.hiro.ulike;
import android.os.*;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
public final class OpticalZoom {
    public static Handler MAIN=new Handler(Looper.getMainLooper());
    static Object field(Object owner,String name)throws ReflectiveOperationException {
        for(Class<?> type=owner.getClass();type!=null;type=type.getSuperclass()) {
            try{Field field=type.getDeclaredField(name);field.setAccessible(true);return field.get(owner);}
            catch(NoSuchFieldException absent){}
        }
        throw new NoSuchFieldException(name);
    }
    static final class Route {
        long epoch=7;boolean rear,failed,configured;int frames;
        WeakReference<Object> mode;
        Route(Object mode){this.mode=new WeakReference<>(mode);}
    }
}

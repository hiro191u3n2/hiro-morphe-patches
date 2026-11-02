package com.hiro.ulike;
import java.util.HashMap;
import java.util.Map;
/** Explicit retained ownership API double; no native/device behavior inferred. */
public final class ManualLens170 {
    public static final Map<String,Object> values=new HashMap<>();
    public static int reads;
    public static boolean numberUnavailable,getUnavailable,yesUnavailable;
    public static long number(String name)throws ReflectiveOperationException {
        reads++;if(numberUnavailable)throw new ReflectiveOperationException("unavailable");
        return ((Number)values.get(name)).longValue();
    }
    public static Object get(String name)throws ReflectiveOperationException {
        reads++;if(getUnavailable)throw new ReflectiveOperationException("unavailable");
        return values.get(name);
    }
    public static boolean yes(String name)throws ReflectiveOperationException {
        reads++;if(yesUnavailable)throw new ReflectiveOperationException("unavailable");
        return Boolean.TRUE.equals(values.get(name));
    }
    public static void reset(){values.clear();reads=0;numberUnavailable=getUnavailable=yesUnavailable=false;}
}

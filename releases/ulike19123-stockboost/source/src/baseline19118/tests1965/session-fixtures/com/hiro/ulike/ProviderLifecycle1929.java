package com.hiro.ulike;
/** Counts the expensive native/provider discoveries separately from ownership reads. */
public final class ProviderLifecycle1929 {
    public interface ForbiddenFrame {}
    public static int reads,calls,frameReads;
    public static boolean unavailable;
    public static Object get(Object owner,String name)throws ReflectiveOperationException {
        reads++;guard(owner);
        return owner.getClass().getField(name).get(owner);
    }
    public static Object call(Object owner,String name)throws ReflectiveOperationException {
        calls++;guard(owner);
        return owner.getClass().getMethod(name).invoke(owner);
    }
    private static void guard(Object owner)throws ReflectiveOperationException {
        if(owner instanceof ForbiddenFrame){frameReads++;throw new AssertionError("frame inspected");}
        if(unavailable)throw new ReflectiveOperationException("unavailable");
    }
    public static void reset(){reads=calls=frameReads=0;unavailable=false;}
}

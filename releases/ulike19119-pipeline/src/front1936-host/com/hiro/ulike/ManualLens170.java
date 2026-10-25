package com.hiro.ulike;
import java.util.*;
public final class ManualLens170 {
    public static final Map<String,Object> f=new HashMap<>();
    static Object get(String key)throws ReflectiveOperationException{return f.get(key);}
    static void put(String key,Object value)throws ReflectiveOperationException{f.put(key,value);}
    static long number(String key)throws ReflectiveOperationException{return ((Number)get(key)).longValue();}
    static boolean yes(String key)throws ReflectiveOperationException{return Boolean.TRUE.equals(get(key));}
}

package com.hiro.ulike;
import java.util.*;
public class ManualLens170 {
 static final Map<String,Object> values=new HashMap<>();
 static Object get(String name)throws ReflectiveOperationException{return values.get(name);}
 static boolean yes(String name)throws ReflectiveOperationException{return Boolean.TRUE.equals(get(name));}
 static long number(String name)throws ReflectiveOperationException{return ((Number)get(name)).longValue();}
}

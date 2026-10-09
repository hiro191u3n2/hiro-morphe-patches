package com.hiro.ulike;
public class ProviderLifecycle1929 {
 static Object get(Object o,String n)throws ReflectiveOperationException {return o.getClass().getField(n).get(o);}
 static Object call(Object o,String n)throws ReflectiveOperationException{return o.getClass().getMethod(n).invoke(o);}
}

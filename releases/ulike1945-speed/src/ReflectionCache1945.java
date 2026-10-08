package com.hiro.ulike;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/** P20: retain reflection metadata, never a target, result or camera setting. */
public final class ReflectionCache1945 {
    private static final int CLASS_LIMIT=64, MEMBER_LIMIT=192;
    private static final Class<?>[] NO_PARAMETERS=new Class<?>[0];
    private static final Map<String,Class<?>> CLASSES=new Bounded<String,Class<?>>(CLASS_LIMIT);
    private static final Map<MemberKey,Field> FIELDS=new Bounded<MemberKey,Field>(MEMBER_LIMIT);
    private static final Map<MemberKey,Method> METHODS=new Bounded<MemberKey,Method>(MEMBER_LIMIT);
    private ReflectionCache1945(){}

    private static final class Bounded<K,V> extends LinkedHashMap<K,V>{
        private final int limit;
        Bounded(int limit){super(16,.75f,true);this.limit=limit;}
        protected boolean removeEldestEntry(Map.Entry<K,V> entry){return size()>limit;}
    }
    private static final class MemberKey{
        final Class<?> owner;final String name;final Class<?>[] parameters;final boolean declared;
        final int hash;
        MemberKey(Class<?> owner,String name,Class<?>[] parameters,boolean declared){
            // Keep the old null-name failure and do not retain a caller-owned array.
            this.owner=owner;this.name=name;this.declared=declared;
            this.parameters=parameters==null||parameters.length==0?NO_PARAMETERS:parameters.clone();
            int h=31*System.identityHashCode(owner)+name.hashCode();h=31*h+(declared?1:0);
            for(Class<?> parameter:this.parameters)h=31*h+System.identityHashCode(parameter);
            hash=h;
        }
        public int hashCode(){return hash;}
        public boolean equals(Object other){
            if(this==other)return true;if(!(other instanceof MemberKey))return false;
            MemberKey key=(MemberKey)other;
            if(owner!=key.owner||declared!=key.declared||!name.equals(key.name)||parameters.length!=key.parameters.length)return false;
            for(int i=0;i<parameters.length;i++)if(parameters[i]!=key.parameters[i])return false;
            return true;
        }
    }
    public static Class<?> type(String name)throws ClassNotFoundException{
        synchronized(CLASSES){Class<?> cached=CLASSES.get(name);if(cached!=null)return cached;}
        // Class initialization may execute application code; never run it under
        // a cache lock. Errors and failed lookups are deliberately not retained.
        Class<?> resolved=Class.forName(name);
        synchronized(CLASSES){Class<?> cached=CLASSES.get(name);if(cached!=null)return cached;CLASSES.put(name,resolved);}
        return resolved;
    }
    /** Static-field lookup retains getDeclaredField's exact-owner semantics. */
    public static Field declaredField(Class<?> owner,String name)throws NoSuchFieldException{
        return resolveField(owner,name,true);
    }
    /** Instance fields retain the existing nearest declaration in the hierarchy. */
    public static Field field(Class<?> owner,String name)throws NoSuchFieldException{
        return resolveField(owner,name,false);
    }
    private static Field resolveField(Class<?> owner,String name,boolean declared)throws NoSuchFieldException{
        MemberKey key=new MemberKey(owner,name,null,declared);
        synchronized(FIELDS){Field cached=FIELDS.get(key);if(cached!=null)return cached;}
        Field resolved=null;
        if(declared)resolved=owner.getDeclaredField(name);
        else for(Class<?> c=owner;c!=null;c=c.getSuperclass()){
            try{resolved=c.getDeclaredField(name);break;}catch(NoSuchFieldException absent){}
        }
        if(resolved==null)throw new NoSuchFieldException(name);
        resolved.setAccessible(true);
        synchronized(FIELDS){Field cached=FIELDS.get(key);if(cached!=null)return cached;FIELDS.put(key,resolved);}
        return resolved;
    }
    /** Method lookup stays public-only, including inherited and interface methods. */
    public static Method method(Class<?> owner,String name,Class<?>[] parameters)throws NoSuchMethodException{
        MemberKey key=new MemberKey(owner,name,parameters,false);
        synchronized(METHODS){Method cached=METHODS.get(key);if(cached!=null)return cached;}
        Method resolved=owner.getMethod(name,key.parameters);
        resolved.setAccessible(true);
        synchronized(METHODS){Method cached=METHODS.get(key);if(cached!=null)return cached;METHODS.put(key,resolved);}
        return resolved;
    }
}

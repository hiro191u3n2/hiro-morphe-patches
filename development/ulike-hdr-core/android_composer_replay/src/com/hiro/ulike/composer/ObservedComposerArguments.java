package com.hiro.ulike.composer;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Owns the pinned public generic composer argument before native side effects.
 * Partial counts and nondefault fields are rejected, not dropped or inferred. */
public final class ObservedComposerArguments {
    private static final Set<String> FIELDS=new HashSet<>(Arrays.asList("TYPE","boolArrayValue","boolValueOne","boolValueThree","boolValueTwo","floatArrayValue","floatValueOne","floatValueThree","floatValueTwo","intArrayValue","intValueOne","intValueThree","intValueTwo","stringArrayOne","stringArrayThree","stringArrayTwo","stringValueOne","stringValueThree","stringValueTwo"));
    private ObservedComposerArguments(){}
    public static ComposerCommand nodes(ComposerCommand.Kind kind,String[] p,int n,String[] q,int m,String[] tags) {
        if(p==null||n!=p.length||(q==null?m!=0:m!=q.length))throw new IllegalArgumentException("partial/invalid count unsupported");
        return ComposerCommand.nodes(kind,p,q,tags);
    }
    public static ComposerCommand updates(int n,String[] p,String[] k,float[] v) {
        if(p==null||k==null||v==null||n!=p.length||n!=k.length||n!=v.length)throw new IllegalArgumentException("partial/invalid batch count unsupported");
        return ComposerCommand.updates(p,k,v);
    }
    public static ComposerCommand effectParams(Object p)throws Exception {
        if(p==null||!p.getClass().getName().equals("com.ss.android.vesdk.VEEffectParams"))throw new IllegalArgumentException("exact generic parameter class required");
        Class<?> t=p.getClass();Set<String> observed=new HashSet<>();
        for(Field f:t.getFields())if(!Modifier.isStatic(f.getModifiers()))observed.add(f.getName());
        if(!observed.equals(FIELDS))throw new IllegalArgumentException("generic parameter field inventory changed");
        // These noncomposer fields are initialized exactly this way by the pinned
        // default constructor. Nondefault values cannot be replayed by our adapter.
        for(String name:new String[]{"boolValueOne","boolValueTwo","boolValueThree"})if(t.getField(name).getBoolean(p))throw new IllegalArgumentException("nondefault boolean field");
        for(String name:new String[]{"floatValueOne","floatValueTwo","floatValueThree"})if(Float.floatToRawIntBits(t.getField(name).getFloat(p))!=0)throw new IllegalArgumentException("nondefault float field");
        if(t.getField("intValueThree").getInt(p)!=0)throw new IllegalArgumentException("nondefault integer field");
        for(String name:new String[]{"stringValueOne","stringValueTwo","stringValueThree"})if(!"".equals(t.getField(name).get(p)))throw new IllegalArgumentException("nondefault string field");
        for(String name:new String[]{"boolArrayValue","floatArrayValue","intArrayValue"})emptyList(t.getField(name).get(p));
        int type=t.getField("TYPE").getInt(p),n=t.getField("intValueOne").getInt(p),m=t.getField("intValueTwo").getInt(p);
        String[] a=strings(t.getField("stringArrayOne").get(p)),b=strings(t.getField("stringArrayTwo").get(p)),c=strings(t.getField("stringArrayThree").get(p));
        ComposerCommand.Kind kind;String constant;
        switch(type){case 0:kind=ComposerCommand.Kind.SET_TAG;constant="SET";break;case 1:kind=ComposerCommand.Kind.RELOAD_TAG;constant="RELOAD";break;case 2:kind=ComposerCommand.Kind.APPEND_TAG;constant="APPEND";break;case 3:kind=ComposerCommand.Kind.REPLACE_TAG;constant="REPLACE";break;default:throw new IllegalArgumentException("noncomposer generic operation");}
        if(t.getField("EFFECT_TYPE_"+constant+"_COMPOSER_WITH_TAG").getInt(null)!=type)throw new IllegalArgumentException("SDK opcode changed");
        if(type!=3 && (m!=0 || c.length!=0))throw new IllegalArgumentException("unexpected replacement state");
        return nodes(kind,a,n,type==3?b:null,type==3?m:0,type==3?c:b);
    }
    private static void emptyList(Object value){if(value==null||value.getClass()!=ArrayList.class||!((ArrayList<?>)value).isEmpty())throw new IllegalArgumentException("nondefault array field");}
    private static String[] strings(Object value) {
        if(value==null||value.getClass()!=ArrayList.class)throw new IllegalArgumentException("pinned list shape required");
        ArrayList<?> list=(ArrayList<?>)value;if(list.size()>256)throw new IllegalArgumentException("list budget");
        Object[] snapshot=list.toArray();if(snapshot.length>256)throw new IllegalArgumentException("list budget");String[] out=new String[snapshot.length];
        for(int i=0;i<out.length;i++){if(!(snapshot[i] instanceof String))throw new IllegalArgumentException("string element required");out[i]=(String)snapshot[i];}return out;
    }
}

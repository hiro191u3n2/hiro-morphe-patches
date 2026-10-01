package com.hiro.ulike.composer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

/** Ordinary public SDK command adapter. This is NOT ReplayTarget: it does not
 * pretend to prove ownership, initialization, graph coverage or a queue barrier.
 * The supplied instance must already be an owned normally initialized RecordInvoker.
 */
public final class RecordInvokerCommands {
    private final Object invoker;
    private final Class<?> type,effectType;
    public RecordInvokerCommands(Object invoker,ClassLoader sdkLoader)throws Exception {
        if(invoker==null || sdkLoader==null)throw new NullPointerException();
        type=Class.forName("com.ss.android.medialib.RecordInvoker",false,sdkLoader);
        effectType=Class.forName("com.ss.android.vesdk.VEEffectParams",false,sdkLoader);
        if(invoker.getClass()!=type)throw new IllegalArgumentException("exact stock RecordInvoker required");
        this.invoker=invoker;
    }
    public Object identity(){return invoker;}
    public int apply(ComposerCommand c)throws Exception {
        if(c==null)throw new NullPointerException();
        String[] p=c.paths(),q=c.replacement(),tags=c.tags(),keys=c.keys();float[] values=c.values();
        switch(c.kind) {
            case MODE:return call("setComposerMode",new Class<?>[]{int.class,int.class},c.modeOne,c.modeTwo);
            case RESOURCE:return call("setComposerResourcePath",new Class<?>[]{String.class},p[0]);
            case SET:return nodes("setComposerNodes",p);
            case APPEND:return nodes("appendComposerNodes",p);
            case REMOVE:return nodes("removeComposerNodes",p);
            case RELOAD:return nodes("reloadComposerNodes",p);
            case REPLACE:return call("replaceComposerNodes",new Class<?>[]{String[].class,int.class,String[].class,int.class},p,p.length,q,q.length);
            case UPDATE:return call("updateComposerNode",new Class<?>[]{String.class,String.class,float.class},p[0],keys[0],values[0]);
            case UPDATES:return call("updateMultiComposerNodes",new Class<?>[]{int.class,String[].class,String[].class,float[].class},p.length,p,keys,values);
            case SET_TAG:case APPEND_TAG:case RELOAD_TAG:case REPLACE_TAG:
                Object params=effectType.getConstructor().newInstance();String constant;int expected;
                switch(c.kind) {
                    case SET_TAG:constant="EFFECT_TYPE_SET_COMPOSER_WITH_TAG";expected=0;break;
                    case APPEND_TAG:constant="EFFECT_TYPE_APPEND_COMPOSER_WITH_TAG";expected=2;break;
                    case RELOAD_TAG:constant="EFFECT_TYPE_RELOAD_COMPOSER_WITH_TAG";expected=1;break;
                    default:constant="EFFECT_TYPE_REPLACE_COMPOSER_WITH_TAG";expected=3;
                }
                int observedType=effectType.getField(constant).getInt(null);
                if(observedType!=expected)throw new IllegalStateException("Pinned SDK composer constant changed");
                effectType.getField("TYPE").setInt(params,observedType);
                effectType.getField("intValueOne").setInt(params,p.length);
                effectType.getField("stringArrayOne").set(params,new ArrayList<>(Arrays.asList(p)));
                effectType.getField("stringArrayTwo").set(params,new ArrayList<>(Arrays.asList(q==null?tags:q)));
                if(q!=null){effectType.getField("intValueTwo").setInt(params,q.length);effectType.getField("stringArrayThree").set(params,new ArrayList<>(Arrays.asList(tags)));}
                return call("setVEEffectParams",new Class<?>[]{effectType},params);
            default:throw new IllegalArgumentException("unimplemented composer opcode");
        }
    }
    private int nodes(String name,String[] paths)throws Exception {return call(name,new Class<?>[]{String[].class,int.class},paths,paths.length);}
    private int call(String name,Class<?>[] signature,Object... args)throws Exception {
        Method method=type.getMethod(name,signature);
        try {Object result=method.invoke(invoker,args);if(!(result instanceof Integer))throw new IllegalStateException("SDK command return type");return (Integer)result;}
        catch(InvocationTargetException e){Throwable cause=e.getCause();if(cause instanceof Error)throw (Error)cause;if(cause instanceof Exception)throw (Exception)cause;throw e;}
    }
}

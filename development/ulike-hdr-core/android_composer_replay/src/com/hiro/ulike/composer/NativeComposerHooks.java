package com.hiro.ulike.composer;

/** Compile-time disabled entry points for the separately verified private DEX
 * delta. No setter, registration or production barrier implementation exists. */
public final class NativeComposerHooks {
    private static final boolean ENABLED=false;
    private static final NativeComposerBoundary BOUNDARY=new NativeComposerBoundary(new NativeComposerBoundary.HandlerReader(){
        @Override public long read(Object r)throws Exception {
            if(!r.getClass().getName().equals("com.ss.android.medialib.RecordInvoker"))throw new IllegalArgumentException("stock receiver required");
            return ((Long)r.getClass().getMethod("getHandler").invoke(r)).longValue();
        }
    });
    private NativeComposerHooks(){}
    public static void beforeInit(Object r,int w,int h,String p,int a,int b,String q,int c,boolean x,boolean y,boolean z){if(ENABLED)BOUNDARY.beforeInit(r,w,h,p,a,b,q,c,x,y,z);}
    public static void beforeUnsupportedInit(Object r,Object settings){if(ENABLED)BOUNDARY.beforeUnsupportedInit(r);}
    public static void beforeUninit(Object r){if(ENABLED)BOUNDARY.beforeUninit(r);}
    public static void beforeMode(Object r,int a,int b){if(ENABLED)BOUNDARY.before(r,ComposerCommand.mode(a,b));}
    public static void beforeResource(Object r,String p){if(ENABLED)try{BOUNDARY.before(r,ComposerCommand.resource(p));}catch(IllegalArgumentException e){BOUNDARY.malformed(r,"invalid resource argument");}}
    public static void beforeSet(Object r,String[] p,int n){nodes(r,ComposerCommand.Kind.SET,p,n,null,0,null);}
    public static void beforeAppend(Object r,String[] p,int n){nodes(r,ComposerCommand.Kind.APPEND,p,n,null,0,null);}
    public static void beforeRemove(Object r,String[] p,int n){nodes(r,ComposerCommand.Kind.REMOVE,p,n,null,0,null);}
    public static void beforeReload(Object r,String[] p,int n){nodes(r,ComposerCommand.Kind.RELOAD,p,n,null,0,null);}
    public static void beforeReplace(Object r,String[] p,int n,String[] q,int m){nodes(r,ComposerCommand.Kind.REPLACE,p,n,q,m,null);}
    public static void beforeUpdate(Object r,String p,String k,float v){if(ENABLED)try{BOUNDARY.before(r,ComposerCommand.update(p,k,v));}catch(IllegalArgumentException e){BOUNDARY.malformed(r,"invalid update argument");}}
    public static void beforeUpdates(Object r,int n,String[] p,String[] k,float[] v){if(ENABLED)try{BOUNDARY.before(r,ObservedComposerArguments.updates(n,p,k,v));}catch(IllegalArgumentException e){BOUNDARY.malformed(r,"noncanonical count or invalid batch argument");}}
    public static void beforeEffectParams(Object r,Object p) {
        if(!ENABLED)return;
        ComposerCommand command;
        try {command=ObservedComposerArguments.effectParams(p);}
        catch(Exception e){BOUNDARY.malformed(r,"unsupported or malformed generic effect parameters");return;}
        BOUNDARY.before(r,command);
    }
    private static void nodes(Object r,ComposerCommand.Kind kind,String[] p,int n,String[] q,int m,String[] tags) {
        if(!ENABLED)return;
        try{BOUNDARY.before(r,ObservedComposerArguments.nodes(kind,p,n,q,m,tags));}
        catch(IllegalArgumentException e){BOUNDARY.malformed(r,"noncanonical count or invalid node argument");}
    }
    public static void returned(int status){if(ENABLED)BOUNDARY.returned(status);}
    public static void failed(Object recorder){if(ENABLED)BOUNDARY.failed(recorder);}
}

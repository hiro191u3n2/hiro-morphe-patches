package com.ss.android.medialib;
import com.ss.android.vesdk.VEEffectParams;
/** Original host test double; no SDK code or native work. */
public final class RecordInvoker {
 public String method; public Object[] args; public int status; public boolean fail;
 private int called(String name,Object... a){if(fail)throw new IllegalStateException("native test failure");method=name;args=a;return status;}
 public int setComposerMode(int a,int b){return called("mode",a,b);}
 public int setComposerResourcePath(String p){return called("resource",p);}
 public int setComposerNodes(String[] p,int n){return called("set",p,n);}
 public int appendComposerNodes(String[] p,int n){return called("append",p,n);}
 public int removeComposerNodes(String[] p,int n){return called("remove",p,n);}
 public int reloadComposerNodes(String[] p,int n){return called("reload",p,n);}
 public int replaceComposerNodes(String[] p,int n,String[] q,int m){return called("replace",p,n,q,m);}
 public int updateComposerNode(String p,String k,float v){return called("update",p,k,v);}
 public int updateMultiComposerNodes(int n,String[] p,String[] k,float[] v){return called("updates",n,p,k,v);}
 public int setVEEffectParams(VEEffectParams p){return called("tags",p);}
}

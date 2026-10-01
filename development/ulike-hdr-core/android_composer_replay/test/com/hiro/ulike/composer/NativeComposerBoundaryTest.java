package com.hiro.ulike.composer;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import com.ss.android.vesdk.VEEffectParams;
public final class NativeComposerBoundaryTest {
 static int checks;static final String STYLE="7306041792770609665";static class Recorder{long handle;}
 interface Work{void run()throws Exception;}
 static void ok(boolean x){checks++;if(!x)throw new AssertionError("check "+checks);}
 static void bad(Work w)throws Exception{try{w.run();throw new AssertionError("accepted invalid request");}catch(IllegalArgumentException|IllegalStateException expected){checks++;}}
 static NativeComposerBoundary boundary(){return new NativeComposerBoundary(r->((Recorder)r).handle);}
 static Recorder initialize(NativeComposerBoundary b){Recorder r=new Recorder();init(b,r);return r;}
 static void init(NativeComposerBoundary b,Recorder r){b.beforeInit(r,4080,3060,"/owned",0,0,"/model",0,false,false,false);r.handle=17;b.returned(0);}
 static ComposerJournal.Snapshot snap(NativeComposerBoundary b,Recorder r){return b.snapshot(r,new Object(),1,STYLE);}
 static VEEffectParams tagged(int type){VEEffectParams p=new VEEffectParams();p.TYPE=type;p.stringArrayOne.add("/first");p.intValueOne=1;p.stringArrayTwo.add("tag");if(type==3){p.stringArrayTwo.set(0,"/second");p.intValueTwo=1;p.stringArrayThree.add("tag");}return p;}
 public static void main(String[] args)throws Exception{
  NativeComposerBoundary b=boundary();Recorder r=initialize(b);ok(snap(b,r).commands.isEmpty());ok(!snap(b,r).allMutationHooksProven&&!snap(b,r).nativeQueueBarrierProven);
  String[] paths={"/one"};ComposerCommand c=ObservedComposerArguments.nodes(ComposerCommand.Kind.SET,paths,1,null,0,null);b.before(r,c);paths[0]="/mutated";bad(()->snap(b,r));b.returned(0);ok(snap(b,r).commands.get(0).paths()[0].equals("/one"));
  ComposerJournal.Snapshot old=snap(b,r);b.before(r,ComposerCommand.update("/one","x",.5f));b.returned(0);bad(old::requireCurrent);ok(snap(b,r).commands.size()==2);
  b.beforeUninit(r);r.handle=0;bad(()->snap(b,r));b.returned(-1);ok(b.invalidReason(r)!=null);bad(()->snap(b,r));
  NativeComposerBoundary b2=boundary();Recorder r2=initialize(b2);b2.beforeUninit(r2);r2.handle=0;b2.returned(0);init(b2,r2);ok(snap(b2,r2).commands.isEmpty());
  NativeComposerBoundary changed=boundary();Recorder cr=initialize(changed);cr.handle=99;bad(()->snap(changed,cr));ok(changed.invalidReason(cr)!=null);
  NativeComposerBoundary during=boundary();Recorder dr=initialize(during);during.before(dr,ComposerCommand.mode(1,0));dr.handle=19;during.returned(0);bad(()->snap(during,dr));
  NativeComposerBoundary failed=boundary();Recorder fr=initialize(failed);failed.before(fr,ComposerCommand.mode(1,0));failed.returned(-105);bad(()->snap(failed,fr));
  NativeComposerBoundary late=boundary();Recorder lr=new Recorder();lr.handle=3;late.before(lr,ComposerCommand.mode(1,0));late.returned(0);bad(()->snap(late,lr));
  NativeComposerBoundary overlap=boundary();Recorder or=initialize(overlap);overlap.before(or,ComposerCommand.mode(1,0));overlap.before(or,ComposerCommand.mode(1,0));overlap.returned(0);overlap.returned(0);bad(()->snap(overlap,or));
  NativeComposerBoundary ex=boundary();Recorder er=initialize(ex);ex.before(er,ComposerCommand.mode(1,0));ex.failed(er);bad(()->snap(ex,er));bad(()->ex.returned(0));
  NativeComposerBoundary unsupported=boundary();Recorder ur=new Recorder();unsupported.beforeUnsupportedInit(ur);ur.handle=1;unsupported.returned(0);bad(()->snap(unsupported,ur));
  NativeComposerBoundary brokenInit=boundary();Recorder ir=new Recorder();brokenInit.beforeInit(ir,4080,3060,null,0,0,"/model",0,false,false,false);ir.handle=3;brokenInit.returned(0);bad(()->snap(brokenInit,ir));
  NativeComposerBoundary repeated=boundary();Recorder rr=initialize(repeated);init(repeated,rr);bad(()->snap(repeated,rr));
  NativeComposerBoundary concurrent=boundary();Recorder tr=initialize(concurrent);CountDownLatch entered=new CountDownLatch(1),finish=new CountDownLatch(1);Throwable[] failure={null};
  Thread first=new Thread(()->{try{concurrent.before(tr,ComposerCommand.mode(1,0));entered.countDown();finish.await();concurrent.returned(0);}catch(Throwable t){failure[0]=t;}});first.start();ok(entered.await(10,TimeUnit.SECONDS));concurrent.before(tr,ComposerCommand.mode(1,0));concurrent.returned(0);finish.countDown();first.join(10000);ok(!first.isAlive()&&failure[0]==null);bad(()->snap(concurrent,tr));
  // Exact generic field defaults and tags; every mutable input is owned.
  for(int type=0;type<4;type++){VEEffectParams p=tagged(type);ComposerCommand command=ObservedComposerArguments.effectParams(p);p.stringArrayOne.set(0,"/changed");p.stringArrayTwo.clear();ok(command.paths()[0].equals("/first"));ok(command.tags()[0].equals("tag"));}
  for(Field field:VEEffectParams.class.getFields())if(!Modifier.isStatic(field.getModifiers()) && !Arrays.asList("TYPE","intValueOne","intValueTwo","stringArrayOne","stringArrayTwo","stringArrayThree").contains(field.getName())){
   VEEffectParams p=tagged(0);if(field.getType()==boolean.class)field.setBoolean(p,true);else if(field.getType()==int.class)field.setInt(p,1);else if(field.getType()==float.class)field.setFloat(p,-0.0f);else if(field.getType()==String.class)field.set(p,"different");else field.set(p,null);bad(()->ObservedComposerArguments.effectParams(p));
  }
  for(int type:new int[]{-1,4,15,Integer.MAX_VALUE}){VEEffectParams p=tagged(0);p.TYPE=type;bad(()->ObservedComposerArguments.effectParams(p));}
  VEEffectParams partial=tagged(0);partial.intValueOne=0;bad(()->ObservedComposerArguments.effectParams(partial));
  VEEffectParams extra=tagged(0);extra.stringArrayThree.add("ignored");bad(()->ObservedComposerArguments.effectParams(extra));
  VEEffectParams zero=tagged(0);zero.intValueTwo=1;bad(()->ObservedComposerArguments.effectParams(zero));
  bad(()->ObservedComposerArguments.nodes(ComposerCommand.Kind.SET,new String[]{"a","b"},1,null,0,null));
  bad(()->ObservedComposerArguments.updates(0,new String[]{"a"},new String[]{"b"},new float[]{0}));
  for(int n=0;n<=256;n++){String[] p=new String[n],k=new String[n];float[] v=new float[n];Arrays.fill(p,"/r");Arrays.fill(k,"x");ComposerCommand owned=ObservedComposerArguments.updates(n,p,k,v);if(n>0){p[0]="mutate";k[0]="mutate";v[0]=1;}ok(owned.paths().length==n);if(n>0)ok(owned.paths()[0].equals("/r")&&owned.keys()[0].equals("x")&&owned.values()[0]==0);}
  // Compile-time disabled hooks must be callable before any SDK object exists,
  // including malformed fields, unmatched exits and every typed entry point.
  NativeComposerHooks.beforeInit(null,0,0,null,0,0,null,0,false,false,false);NativeComposerHooks.beforeUnsupportedInit(null,null);NativeComposerHooks.beforeUninit(null);
  NativeComposerHooks.beforeMode(null,0,0);NativeComposerHooks.beforeResource(null,null);NativeComposerHooks.beforeSet(null,null,-1);NativeComposerHooks.beforeAppend(null,null,-1);NativeComposerHooks.beforeRemove(null,null,-1);NativeComposerHooks.beforeReload(null,null,-1);NativeComposerHooks.beforeReplace(null,null,-1,null,-1);NativeComposerHooks.beforeUpdate(null,null,null,Float.NaN);NativeComposerHooks.beforeUpdates(null,-1,null,null,null);NativeComposerHooks.beforeEffectParams(null,null);NativeComposerHooks.returned(0);NativeComposerHooks.failed(null);ok(true);
  System.out.println("PASS "+checks+" native composer boundary/owned argument checks; live barriers unavailable");
 }
}

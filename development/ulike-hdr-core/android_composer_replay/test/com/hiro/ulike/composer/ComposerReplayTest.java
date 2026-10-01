package com.hiro.ulike.composer;

import java.util.*;
import com.ss.android.medialib.RecordInvoker;
import com.ss.android.vesdk.VEEffectParams;
import static com.hiro.ulike.composer.ComposerCommand.Kind.*;

public final class ComposerReplayTest {
 static int checks;
 static final String INIT=String.join("",Collections.nCopies(64,"1")),STYLE="7306041792770609665";
 interface Checked{void run()throws Exception;}
 static void ok(boolean v,String reason){checks++;if(!v)throw new AssertionError(reason);}
 static void bad(Checked f){checks++;try{f.run();throw new AssertionError("accepted unsupported/unsafe state");}catch(Exception expected){}}
 static ComposerJournal journal(){ComposerJournal j=new ComposerJournal(new Object());j.beforeNativeInit(INIT);j.nativeInitResult(0,91);return j;}
 static void record(ComposerJournal j,ComposerCommand c){j.result(j.begin(c),0);}
 static ComposerJournal.Snapshot snap(ComposerJournal j){return j.snapshot(new Object(),7,STYLE);}
 static final ReplayPlan.Preconditions TEST_PROOF=s->s.requireCurrent(); // TEST ONLY: no production coverage/barrier claim.
 static final ReplayPlan.ResourceMapping SAME=(p,d)->p;
 static class Target implements ReplayPlan.ReplayTarget {
  Object id=new Object();int calls,aborted,fresh,barriers,status=-1,badReceipt=-1;ComposerJournal mutate;int mutateAt=-1;boolean throwApply,throwAbort;
  List<ComposerCommand> seen=new ArrayList<>();
  public Object identity(){return id;}
  public void requireFresh(String init){ok(init.equals(INIT),"init fingerprint retained");fresh++;}
  public int apply(ComposerCommand c){calls++;seen.add(c);if(mutate!=null && calls==mutateAt)record(mutate,ComposerCommand.mode(9,9));if(throwApply)throw new IllegalStateException("test exception");return calls==status?-99:0;}
  public ReplayPlan.BarrierReceipt awaitSetupBarrier(ReplayPlan.BarrierRequest r){barriers++;return new ReplayPlan.BarrierReceipt(badReceipt==0?new Object():r.targetIdentity,badReceipt==1?new Object():r.shotIdentity,badReceipt==2?r.shotEpoch+1:r.shotEpoch,badReceipt==3?r.nonce+1:r.nonce,badReceipt==4?INIT:r.settingsSha256,badReceipt==5?INIT:r.mappedCommandsSha256);}
  public void abort(){aborted++;if(throwAbort)throw new IllegalStateException("cleanup also failed");}
 }
 static void journalSafety()throws Exception {
  ComposerJournal never=new ComposerJournal(new Object());ok(never.begin(ComposerCommand.mode(1,0))==null,"pre-init rejected");bad(()->snap(never));
  ComposerJournal init=new ComposerJournal(new Object());init.beforeNativeInit(INIT);init.nativeInitResult(-1,0);bad(()->snap(init));
  ComposerJournal repeated=journal();bad(()->repeated.nativeInitResult(0,91));bad(()->snap(repeated));
  ComposerJournal pending=journal();ComposerJournal.Ticket t=pending.begin(ComposerCommand.mode(1,0));bad(()->snap(pending));pending.result(t,0);ok(snap(pending).commands.size()==1,"successful request retained");
  bad(()->pending.result(t,0));bad(()->snap(pending));
  ComposerJournal overlap=journal();ComposerJournal.Ticket first=overlap.begin(ComposerCommand.mode(1,0));ok(overlap.begin(ComposerCommand.mode(2,0))==null,"overlap rejected");overlap.result(first,0);bad(()->snap(overlap));
  ComposerJournal failure=journal();failure.result(failure.begin(ComposerCommand.resource("/r")),-7);bad(()->snap(failure));
  ComposerJournal throwing=journal();throwing.threw(throwing.begin(ComposerCommand.resource("/r")));bad(()->snap(throwing));
  ComposerJournal external=journal();external.unrecordedMutation("sendEffectMsg");bad(()->snap(external));
  ComposerJournal end=journal();ComposerJournal.Snapshot before=snap(end);end.disposed();bad(before::requireCurrent);bad(()->snap(end));
  ComposerJournal large=journal();for(int i=0;i<4097;i++)record(large,ComposerCommand.mode(i,0));bad(()->snap(large));
  ComposerJournal byteBound=journal();String big=String.join("",Collections.nCopies(4000,"x"));String[] sixteen=new String[16];Arrays.fill(sixteen,big);for(int i=0;i<34;i++)record(byteBound,ComposerCommand.nodes(SET,sixteen,null,null));bad(()->snap(byteBound));
 }
 static void ownershipCanonical()throws Exception {
  String[] paths={"/r:slider:-0.0","/q"},tags={"first","second"};
  ComposerCommand c=ComposerCommand.nodes(SET_TAG,paths,null,tags);paths[0]="changed";tags[0]="changed";
  ok(c.paths()[0].equals("/r:slider:-0.0") && c.tags()[0].equals("first"),"request arrays owned");
  c.paths()[0]="another";ok(c.paths()[0].equals("/r:slider:-0.0"),"returned arrays owned");
  float[] f={-0.0f};ComposerCommand u=ComposerCommand.updates(new String[]{"/r"},new String[]{"k"},f);f[0]=1;
  ok(Float.floatToRawIntBits(u.values()[0])==0x80000000,"exact negative zero captured");
  ComposerJournal j=journal();record(j,c);record(j,u);ComposerJournal.Snapshot s=snap(j);
  byte[] canonical=s.canonicalBytes();canonical[0]++;ok(!Arrays.equals(canonical,s.canonicalBytes()),"canonical bytes owned");bad(()->s.commands.clear());
  String same=s.sha256;ok(same.equals(snapWith(j,s.shotIdentity).sha256),"same settings/shot epoch canonical identity");
  record(j,ComposerCommand.mode(1,0));bad(s::requireCurrent);ok(s.commands.size()==2,"snapshot immutable on subsequent requests");
  bad(()->ComposerCommand.resource("/r"+((char)0xd800)));bad(()->ComposerCommand.resource("/r"+((char)0xd801)));bad(()->ComposerCommand.update("/r","k"+((char)0xdc00),.5f));
  ComposerCommand.resource("/r\ud83d\ude00");ok(true,"valid surrogate pair accepted without UTF-8 collision");
  Set<String> hashes=new HashSet<>();
  for(int i=0;i<1024;i++) {ComposerJournal a=journal();record(a,ComposerCommand.update("/r","k",Float.intBitsToFloat(0x3f000000+i)));ok(hashes.add(snap(a).sha256),"raw float setting changes canonical SHA");}
  ComposerJournal a=journal(),b=journal();record(a,ComposerCommand.update("/r","k",0));record(b,ComposerCommand.updates(new String[]{"/r"},new String[]{"k"},new float[]{0}));ok(!snap(a).sha256.equals(snap(b).sha256),"single vs batch call preserved");
  bad(()->ComposerCommand.nodes(SET_TAG,new String[]{"/r"},null,new String[0]));bad(()->ComposerCommand.update("/r","k",Float.NaN));bad(()->ComposerCommand.nodes(SET,new String[257],null,null));
 }
 static ComposerJournal.Snapshot snapWith(ComposerJournal j,Object shot){return j.snapshot(shot,7,STYLE);}
 static void replaySafety()throws Exception {
  ComposerJournal j=journal();record(j,ComposerCommand.mode(1,0));record(j,ComposerCommand.resource("/r"));record(j,ComposerCommand.nodes(APPEND_TAG,new String[]{"/r:slider:-0.0"},null,new String[]{"tag"}));
  record(j,ComposerCommand.nodes(REPLACE_TAG,new String[]{"/old"},new String[]{"/new"},new String[]{"replacement"}));record(j,ComposerCommand.update("/r","exact",.7f));
  ComposerJournal.Snapshot s=snap(j);bad(()->ReplayPlan.authorize(s,ReplayPlan.UNAVAILABLE));
  Map<String,String> roots=new LinkedHashMap<>();roots.put("/r","/owned/r");roots.put("/old","/owned/old");roots.put("/new","/owned/new");ExactResourceMap mapping=new ExactResourceMap(roots);roots.clear();
  Target target=new Target();ReplayPlan plan=ReplayPlan.authorize(s,TEST_PROOF);plan.execute(target,mapping);
  ok(target.calls==5 && target.barriers==1 && target.aborted==0,"complete exact transcript and setup receipt");
  ok(target.seen.get(2).paths()[0].equals("/owned/r:slider:-0.0"),"inline suffix preserved literally");
  ok(target.seen.get(3).kind==REPLACE_TAG && target.seen.get(3).replacement()[0].equals("/owned/new"),"replace not flattened");bad(()->plan.execute(new Target(),mapping));
  Target incomplete=new Target();bad(()->ReplayPlan.authorize(s,TEST_PROOF).execute(incomplete,new ExactResourceMap(Collections.singletonMap("/r","/owned/r"))));ok(incomplete.calls==0 && incomplete.fresh==0,"all historical paths preflight before native mutation");
  Target changedSuffix=new Target();bad(()->ReplayPlan.authorize(s,TEST_PROOF).execute(changedSuffix,(p,d)->p.replace(":-0.0",":0.0")));ok(changedSuffix.calls==0,"mapping cannot alter inline exact parameter");
  Target original=new Target();original.id=s.recorderIdentity;bad(()->ReplayPlan.authorize(s,TEST_PROOF).execute(original,SAME));ok(original.calls==0 && original.aborted==0,"source never mutated or destroyed");
  for(int n=1;n<=5;n++){Target fail=new Target();fail.status=n;bad(()->ReplayPlan.authorize(s,TEST_PROOF).execute(fail,SAME));ok(fail.calls==n && fail.aborted==1 && fail.barriers==0,"native failure aborts exact prefix");}
  for(int n=0;n<6;n++){Target stale=new Target();stale.badReceipt=n;bad(()->ReplayPlan.authorize(s,TEST_PROOF).execute(stale,SAME));ok(stale.aborted==1,"all setup barrier identities checked");}
  Target thrown=new Target();thrown.throwApply=true;thrown.throwAbort=true;try{ReplayPlan.authorize(s,TEST_PROOF).execute(thrown,SAME);throw new AssertionError();}catch(IllegalStateException ex){ok(ex.getSuppressed().length==1,"cleanup error retained without replacing primary failure");}
  ComposerJournal dynamic=journal();record(dynamic,ComposerCommand.mode(1,0));Target race=new Target();race.mutate=dynamic;race.mutateAt=1;bad(()->ReplayPlan.authorize(snap(dynamic),TEST_PROOF).execute(race,SAME));ok(race.aborted==1 && race.barriers==0,"source change during final command aborts before barrier");
  Target removedProof=new Target();int[] checked={0};ReplayPlan gone=ReplayPlan.authorize(s,snapshot->{if(++checked[0]>1)throw new IllegalStateException("coverage lost");});bad(()->gone.execute(removedProof,SAME));ok(removedProof.calls==0,"coverage reevaluated before execute");
 }
 static void sdkDispatch()throws Exception {
  RecordInvoker spy=new RecordInvoker();RecordInvokerCommands target=new RecordInvokerCommands(spy,ComposerReplayTest.class.getClassLoader());
  target.apply(ComposerCommand.mode(8,-3));ok(spy.method.equals("mode") && spy.args[1].equals(-3),"mode unchanged");
  target.apply(ComposerCommand.resource("/root"));ok(spy.method.equals("resource"),"resource API");
  for(ComposerCommand.Kind kind:new ComposerCommand.Kind[]{SET,APPEND,REMOVE,RELOAD}){target.apply(ComposerCommand.nodes(kind,new String[]{"/a","/b"},null,null));ok(spy.method.equals(kind.name().toLowerCase(Locale.ROOT)) && spy.args[1].equals(2),"ordinary exact node API");}
  target.apply(ComposerCommand.nodes(REPLACE,new String[]{"/old"},new String[]{"/new","/more"},null));ok(spy.method.equals("replace") && spy.args[3].equals(2),"plain replace count/order");
  target.apply(ComposerCommand.update("/r","k",.8f));ok(spy.method.equals("update"),"single update not converted to batch");
  target.apply(ComposerCommand.updates(new String[]{"/r"},new String[]{"k"},new float[]{.8f}));ok(spy.method.equals("updates"),"batch count one remains batch");
  int index=0;for(ComposerCommand.Kind kind:new ComposerCommand.Kind[]{SET_TAG,APPEND_TAG,RELOAD_TAG,REPLACE_TAG}) {
   String[] q=kind==REPLACE_TAG?new String[]{"/new","/other"}:null;
   target.apply(ComposerCommand.nodes(kind,new String[]{"/r"},q,q==null?new String[]{"t"}:new String[]{"t1","t2"}));
   VEEffectParams p=(VEEffectParams)spy.args[0];ok(spy.method.equals("tags") && p.TYPE==new int[]{0,2,1,3}[index++],"actual reflected tag constant");
   ok(p.intValueOne==1 && p.stringArrayOne.equals(Arrays.asList("/r")),"tag original paths");
   if(q==null)ok(p.stringArrayTwo.equals(Arrays.asList("t")),"ordinary tags in array two");
   else ok(p.intValueTwo==2 && p.stringArrayTwo.equals(Arrays.asList(q)) && p.stringArrayThree.equals(Arrays.asList("t1","t2")),"replacement paths and tags retain distinct arrays");
  }
  VEEffectParams.EFFECT_TYPE_SET_COMPOSER_WITH_TAG=99;bad(()->target.apply(ComposerCommand.nodes(SET_TAG,new String[]{"/r"},null,new String[]{"t"})));VEEffectParams.EFFECT_TYPE_SET_COMPOSER_WITH_TAG=0;
  spy.status=-47;ok(target.apply(ComposerCommand.mode(0,0))==-47,"SDK status propagated");spy.fail=true;bad(()->target.apply(ComposerCommand.mode(0,0)));bad(()->new RecordInvokerCommands(new Object(),ComposerReplayTest.class.getClassLoader()));
 }
 static void legacy()throws Exception {
  Map<String,Object> manifest=new LinkedHashMap<>();manifest.put("schema","ulike-style-materials-164");manifest.put("completeness",Collections.singletonMap("ordered_api_model_complete",true));
  manifest.put("mode_one",1);manifest.put("mode_two",0);manifest.put("composer_resource_path","/r");
  Map<String,Object> e=new LinkedHashMap<>();e.put("return_code",0);e.put("operation","set");manifest.put("api_events",Arrays.asList(e));
  V164ReplayAudit a=V164ReplayAudit.inspect(manifest);ok(a.orderedApiModelComplete && !a.replayAuthorized,"legacy approximate true cannot grant replay");bad(a::requireReplayable);
 }
 public static void main(String[] args)throws Exception {journalSafety();ownershipCanonical();replaySafety();sdkDispatch();legacy();System.out.println("PASS "+checks+" composer replay checks");}
}

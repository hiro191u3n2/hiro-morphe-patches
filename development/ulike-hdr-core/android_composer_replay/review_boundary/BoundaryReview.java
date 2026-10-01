package com.hiro.ulike.composer;

import com.ss.android.vesdk.VEEffectParams;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

/** Independent observer protocol review; no native SDK or Android code executed. */
public final class BoundaryReview {
    static int checks;
    static final String STYLE="7307549491547083266";
    static class Recorder {volatile long handle;volatile boolean failRead;}
    interface Task {void run()throws Exception;}
    static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    static void reject(Task task,String why)throws Exception{try{task.run();throw new AssertionError("accepted "+why);}catch(IllegalStateException|IllegalArgumentException expected){checks++;}}
    static NativeComposerBoundary boundary(){return new NativeComposerBoundary(r->{Recorder x=(Recorder)r;if(x.failRead)throw new ReflectiveOperationException("injected getHandler failure");return x.handle;});}
    static void start(NativeComposerBoundary b,Recorder r){b.beforeInit(r,4080,3060,"/owned/東京",13,-7,"/owned/model",9,true,false,true);}
    static Recorder ready(NativeComposerBoundary b,int number){Recorder r=new Recorder();start(b,r);r.handle=number;b.returned(0);return r;}
    static ComposerJournal.Snapshot snapshot(NativeComposerBoundary b,Recorder r){return b.snapshot(r,new Object(),19,STYLE);}
    static String independentInitHash()throws Exception{
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        putString(out,"ulike-native-init-request-171-v1");putInt(out,4080);putInt(out,3060);putString(out,"/owned/東京");putInt(out,13);putInt(out,-7);putString(out,"/owned/model");putInt(out,9);out.write(1);out.write(0);out.write(1);
        StringBuilder hex=new StringBuilder();for(byte v:MessageDigest.getInstance("SHA-256").digest(out.toByteArray())){hex.append("0123456789abcdef".charAt((v&255)>>>4));hex.append("0123456789abcdef".charAt(v&15));}return hex.toString();
    }
    static void putInt(ByteArrayOutputStream out,int n){byte[] bytes=ByteBuffer.allocate(4).putInt(n).array();out.write(bytes,0,bytes.length);}
    static void putString(ByteArrayOutputStream out,String s){byte[] bytes=s.getBytes(StandardCharsets.UTF_8);putInt(out,bytes.length);out.write(bytes,0,bytes.length);}
    static void initAndFailures()throws Exception{
        for(int code:new int[]{0,-1,1,Integer.MIN_VALUE,Integer.MAX_VALUE})for(long handle:new long[]{0,1,-1,Long.MAX_VALUE,Long.MIN_VALUE}){
            NativeComposerBoundary b=boundary();Recorder r=new Recorder();start(b,r);r.handle=handle;b.returned(code);
            if(code==0&&handle!=0){ComposerJournal.Snapshot s=snapshot(b,r);check(s.nativeHandler==handle,"full-width native handle preserved");check(s.initConfigurationSha256.equals(independentInitHash()),"independent init fingerprint including UTF8/signed params");check(!s.allMutationHooksProven&&!s.nativeQueueBarrierProven,"snapshot is not coverage/barrier proof");}
            else reject(()->snapshot(b,r),"failed/no-handle initialization");
        }
        for(int stage=0;stage<4;stage++){
            NativeComposerBoundary b=boundary();Recorder r=new Recorder();
            if(stage==0){r.handle=81;start(b,r);b.returned(0);}
            if(stage==1){start(b,r);r.handle=81;r.failRead=true;b.returned(0);r.failRead=false;}
            if(stage==2){r=ready(b,81);r.failRead=true;b.before(r,ComposerCommand.mode(1,0));r.failRead=false;b.returned(0);}
            if(stage==3){r=ready(b,81);b.before(r,ComposerCommand.mode(1,0));r.failRead=true;b.returned(0);r.failRead=false;}
            final Recorder target=r;reject(()->snapshot(b,target),"late/read-failed observation at stage "+stage);
        }
        NativeComposerBoundary b=boundary();Recorder r=ready(b,55);ComposerJournal.Snapshot old=snapshot(b,r);
        b.before(r,ComposerCommand.mode(1,0));b.failed(r);reject(old::requireCurrent,"exception invalidates prior snapshot");
        for(int code:new int[]{0,-105,1}){b.before(r,ComposerCommand.update("/x","v",.2f));b.returned(code);reject(()->snapshot(b,r),"invalid state sticky across later returns");}
        b.beforeUninit(r);r.handle=0;b.returned(0);start(b,r);r.handle=55;b.returned(0);check(snapshot(b,r).commands.isEmpty(),"verified teardown permits fresh empty journal despite address reuse");reject(old::requireCurrent,"old snapshot remains disposed after address reuse");
    }
    static void nestedAndThreads()throws Exception{
        NativeComposerBoundary b=boundary();Recorder[] rs=new Recorder[32];for(int i=0;i<rs.length;i++)rs[i]=ready(b,i+1);
        for(int i=0;i<16;i++)b.before(rs[i],ComposerCommand.mode(i,-i));
        reject(()->b.before(rs[16],ComposerCommand.mode(16,0)),"17th nested hook entry");
        for(int i=15;i>=0;i--)b.returned(0);
        for(int i=0;i<16;i++){ComposerJournal.Snapshot s=snapshot(b,rs[i]);check(s.commands.size()==1&&s.commands.get(0).modeOne==i,"nested independent recorder calls unwind exact stack");}
        reject(()->snapshot(b,rs[16]),"overflow target quarantined");reject(()->start(b,new Recorder()),"33rd tracked recorder rejected");
        b.beforeUninit(rs[0]);rs[0].handle=0;b.returned(0);Recorder replacement=ready(b,110);check(snapshot(b,replacement).commands.isEmpty(),"successful teardown releases recorder budget");
        NativeComposerBoundary same=boundary();Recorder sr=ready(same,99);same.before(sr,ComposerCommand.mode(1,0));same.before(sr,ComposerCommand.resource("/p"));same.returned(0);same.returned(0);reject(()->snapshot(same,sr),"reentrant same-recorder mutation cannot form valid transcript");
        NativeComposerBoundary mismatch=boundary();Recorder a=ready(mismatch,1),c=ready(mismatch,2);mismatch.before(a,ComposerCommand.mode(1,2));mismatch.failed(c);reject(()->snapshot(mismatch,a),"exception mismatch poisons actual receiver");reject(()->snapshot(mismatch,c),"exception mismatch poisons declared receiver");
        for(boolean shared:new boolean[]{false,true})for(int round=0;round<20;round++){
            NativeComposerBoundary concurrent=boundary();Recorder x=ready(concurrent,11),y=shared?x:ready(concurrent,12);
            CountDownLatch entered=new CountDownLatch(1),otherCompleted=new CountDownLatch(1);Throwable[] failed={null};
            Thread t=new Thread(()->{try{concurrent.before(x,ComposerCommand.update("/x","first",.125f));entered.countDown();if(!otherCompleted.await(10,TimeUnit.SECONDS))throw new AssertionError("timeout");concurrent.returned(0);}catch(Throwable failure){failed[0]=failure;}});
            t.start();check(entered.await(10,TimeUnit.SECONDS),"first thread entered");concurrent.before(y,ComposerCommand.update("/x","second",.5f));concurrent.returned(0);otherCompleted.countDown();t.join(10000);check(!t.isAlive()&&failed[0]==null,"bounded thread completion");
            if(shared)reject(()->snapshot(concurrent,x),"cross-thread shared recorder overlapping mutation");
            else{check(snapshot(concurrent,x).commands.get(0).keys()[0].equals("first"),"per-thread stack first recorder");check(snapshot(concurrent,y).commands.get(0).keys()[0].equals("second"),"per-thread stack second recorder");}
        }
    }
    static VEEffectParams params(int type){VEEffectParams p=new VEEffectParams();p.TYPE=type;p.intValueOne=1;p.stringArrayOne.add("/old");p.stringArrayTwo.add(type==3?"/new":"tag");if(type==3){p.intValueTwo=1;p.stringArrayThree.add("tag");}return p;}
    static void argumentOwnership()throws Exception{
        for(int type=0;type<4;type++){
            VEEffectParams p=params(type);ComposerCommand command=ObservedComposerArguments.effectParams(p);
            p.TYPE=77;p.intValueOne=0;p.stringArrayOne.clear();p.stringArrayTwo.clear();p.stringArrayThree.clear();
            check(command.paths()[0].equals("/old")&&command.tags()[0].equals("tag"),"generic args snapshot owns exact list values");
            if(type==3)check(command.replacement()[0].equals("/new"),"replacement route distinct from tags");
            String[] out=command.paths();out[0]="/evil";check(command.paths()[0].equals("/old"),"path getter cannot mutate recorded command");
        }
        for(int n:new int[]{0,1,2,127,255,256}){
            String[] p=new String[n],k=new String[n];float[] v=new float[n];for(int i=0;i<n;i++){p[i]="/p"+i;k[i]="key"+i;v[i]=Float.intBitsToFloat(i%2==0?0x80000000:0x3eaaaaab);}
            ComposerCommand owned=ObservedComposerArguments.updates(n,p,k,v);Arrays.fill(p,"/changed");Arrays.fill(k,"changed");Arrays.fill(v,1);
            for(int i=0;i<n;i++)check(owned.paths()[i].equals("/p"+i)&&owned.keys()[i].equals("key"+i)&&Float.floatToRawIntBits(owned.values()[i])==(i%2==0?0x80000000:0x3eaaaaab),"exact owned order and raw finite float bits");
            reject(()->ObservedComposerArguments.updates(n-1,p,k,v),"partial or negative count");
        }
        for(int type=0;type<4;type++)for(String name:new String[]{"boolArrayValue","floatArrayValue","intArrayValue","stringArrayOne","stringArrayTwo","stringArrayThree"}){
            VEEffectParams p=params(type);Field f=VEEffectParams.class.getField(name);f.set(p,new ArrayList<Object>(){});reject(()->ObservedComposerArguments.effectParams(p),"nonexact list class "+name);
        }
        for(String bad:new String[]{"\u0000","\ud800","\udfff"}){VEEffectParams p=params(0);p.stringArrayOne.set(0,bad);reject(()->ObservedComposerArguments.effectParams(p),"ambiguous/unbounded string encoding");}
        for(float bad:new float[]{Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY})reject(()->ObservedComposerArguments.updates(1,new String[]{"/x"},new String[]{"k"},new float[]{bad}),"nonfinite batch scalar");
        NativeComposerBoundary b=boundary();Recorder r=ready(b,11);String[] p={"/entry-snapshot"};b.before(r,ObservedComposerArguments.nodes(ComposerCommand.Kind.SET,p,1,null,0,null));p[0]="/concurrent-caller-value";b.returned(0);
        ComposerJournal.Snapshot snapshot=snapshot(b,r);check(snapshot.commands.get(0).paths()[0].equals("/entry-snapshot"),"entry transcript immutable");
        check(!snapshot.allMutationHooksProven&&!snapshot.nativeQueueBarrierProven,"entry ownership must not claim native consumed mutable caller value");reject(()->ReplayPlan.authorize(snapshot,ReplayPlan.UNAVAILABLE),"uninstalled coverage cannot authorize replay");
    }
    static void disabled()throws Exception{
        for(Field f:NativeComposerHooks.class.getDeclaredFields())if(f.getName().equals("ENABLED")){f.setAccessible(true);check(java.lang.reflect.Modifier.isFinal(f.getModifiers())&&!f.getBoolean(null),"observer remains compile-time disabled");}
        NativeComposerHooks.beforeInit(null,0,0,null,0,0,null,0,false,false,false);NativeComposerHooks.beforeUnsupportedInit(null,null);NativeComposerHooks.beforeUninit(null);
        NativeComposerHooks.beforeMode(null,0,0);NativeComposerHooks.beforeResource(null,null);NativeComposerHooks.beforeSet(null,null,-1);NativeComposerHooks.beforeAppend(null,null,-1);NativeComposerHooks.beforeRemove(null,null,-1);NativeComposerHooks.beforeReload(null,null,-1);NativeComposerHooks.beforeReplace(null,null,-1,null,-1);NativeComposerHooks.beforeUpdate(null,null,null,Float.NaN);NativeComposerHooks.beforeUpdates(null,-1,null,null,null);NativeComposerHooks.beforeEffectParams(null,null);NativeComposerHooks.returned(-1);NativeComposerHooks.failed(null);check(true,"all disabled entry/exit methods do not touch SDK/null objects");
    }
    public static void main(String[] args)throws Exception{
        initAndFailures();nestedAndThreads();argumentOwnership();disabled();
        System.out.println("{\"checks\":"+checks+",\"concurrent_cases\":40,\"initialization_outcomes\":25,\"native_sdk_execution\":false,\"android_device_execution\":false}");
    }
}

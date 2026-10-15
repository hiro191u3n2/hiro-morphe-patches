package com.hiro.ulike;

import android.content.Context;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Independent asynchronous/storage boundary tests. No GPU peer, admission
 * record or synthetic success is used as evidence of a hardware result. */
public final class JournalAdversarial19101Test {
    static int assertions;
    static void check(boolean okay,String why){assertions++;if(!okay)throw new AssertionError(why);}
    interface Condition {boolean test()throws Exception;}
    static void until(Condition c,String why)throws Exception{
        long deadline=System.nanoTime()+6000000000L;
        while(!c.test()){if(System.nanoTime()>deadline)throw new AssertionError("timeout: "+why);Thread.sleep(2);}
        check(true,why);
    }
    static long[] values(long id,int state){long[] v=new long[VerificationJournal19101.VALUES];v[0]=id;v[1]=700+id;v[2]=3;v[3]=state;v[4]=2;
        v[5]=7;v[6]=state==2?11:0;v[7]=1;v[8]=2;v[15]=1700000000000L;v[16]=1700000001000L;
        v[18]=2;v[19]=1;v[20]=1;v[21]=1;v[29]=-1;v[30]=-1;return v;}
    static String[] labels(){return new String[]{"連結","連結の画質比較","速度条件を満たさず","モデル64スレッド","CPU基準"};}
    static VerificationJournal19101.Entry entry(String process,long id,int state){return new VerificationJournal19101.Entry(process,"1.9.101",values(id,state),labels());}
    static VerificationJournal19101.Entry find(String raw,String process,long id)throws Exception{
        for(VerificationJournal19101.Entry e:VerificationJournal19101.decode(raw))if(e.process.equals(process)&&e.v[0]==id)return e;
        return null;
    }
    static final class ControlledStore implements VerificationJournal19101.Store {
        volatile String raw;volatile int reads,writes;volatile boolean reject;
        volatile CountDownLatch reading,releaseRead,writing,releaseWrite;
        volatile Thread ioThread;volatile VerificationJournal19101.Engine owner;
        ControlledStore(String raw){this.raw=raw;}
        void io(){ioThread=Thread.currentThread();if(owner!=null&&Thread.holdsLock(owner.lock))throw new AssertionError("journal monitor held during I/O");}
        public String read()throws Exception{io();reads++;CountDownLatch latch=reading;if(latch!=null)latch.countDown();latch=releaseRead;if(latch!=null&&!latch.await(5,TimeUnit.SECONDS))throw new AssertionError("read release");return raw;}
        public boolean write(String text)throws Exception{io();writes++;CountDownLatch latch=writing;if(latch!=null)latch.countDown();latch=releaseWrite;if(latch!=null&&!latch.await(5,TimeUnit.SECONDS))throw new AssertionError("write release");if(reject)return false;raw=text;return true;}
    }
    static void loaded(VerificationJournal19101.Engine e)throws Exception{until(()->{synchronized(e.lock){return e.loaded;}},"asynchronous load completes");}
    static void saved(VerificationJournal19101.Engine e)throws Exception{until(()->{synchronized(e.lock){return e.savedGeneration==e.generation;}},"latest generation committed");}
    static void stopped(VerificationJournal19101.Engine e)throws Exception{e.stopForTest();e.writer.join(1000);check(!e.writer.isAlive(),"writer exits without caller I/O");}
    static void races()throws Exception{
        ControlledStore store=new ControlledStore(VerificationJournal19101.encode(Arrays.asList(entry("old-process",1,1))));
        store.reading=new CountDownLatch(1);store.releaseRead=new CountDownLatch(1);
        VerificationJournal19101.Engine e=new VerificationJournal19101.Engine("new-process");store.owner=e;e.start(store);
        check(store.reading.await(1,TimeUnit.SECONDS),"writer entered delayed disk load");
        FutureTask<Void> offer=new FutureTask<Void>(()->{e.offer(values(2,1),labels());e.offer(values(2,2),labels());return null;});
        Thread producer=new Thread(offer,"capture-producer");producer.start();offer.get(1,TimeUnit.SECONDS);
        check(e.summary().contains("読込み中")&&store.writes==0,"producer and summary remain independent of delayed load");
        store.releaseRead.countDown();loaded(e);saved(e);
        check(find(store.raw,"old-process",1)!=null&&find(store.raw,"new-process",2).v[3]==2,"startup merge retains old disk and newest pre-load terminal event");
        check(store.ioThread!=Thread.currentThread()&&store.ioThread!=producer,"all storage belongs to dedicated writer");
        String previous=e.summary();check(previous.contains("前回実行中・結果不明")&&!previous.contains("前回完了"),"old unfinished state never becomes completion");
        check(!previous.contains("#2 連結"),"current-process rows are not rendered as historical");
        store.writing=new CountDownLatch(1);store.releaseWrite=new CountDownLatch(1);
        e.offer(values(3,1),labels());check(store.writing.await(1,TimeUnit.SECONDS),"older generation writing is delayed");
        long oldGeneration; synchronized(e.lock){oldGeneration=e.generation;}
        e.offer(values(3,2),labels());check(e.summary().contains("保存待ち"),"pending newer generation is visible while commit blocks");
        synchronized(e.lock){check(e.generation>oldGeneration&&e.savedGeneration<e.generation,"old in-flight commit cannot acknowledge newer transition");}
        store.releaseWrite.countDown();saved(e);
        check(find(store.raw,"new-process",3).v[3]==2,"follow-up write persists latest terminal state");
        stopped(e);

        ControlledStore failed=new ControlledStore("");failed.reject=true;
        VerificationJournal19101.Engine retry=new VerificationJournal19101.Engine("retry-process");failed.owner=retry;retry.start(failed);loaded(retry);
        retry.offer(values(1,1),labels());until(()->retry.summary().contains("保存失敗"),"false commit is reported as failure");
        synchronized(retry.lock){check(retry.savedGeneration==0&&retry.failedGeneration>0,"false commit does not advance durable generation");}
        check(failed.raw.isEmpty(),"failed write leaves disk unchanged");
        failed.reject=false;retry.offer(values(1,2),labels());saved(retry);
        check(find(failed.raw,"retry-process",1).v[3]==2&&!retry.summary().contains("保存失敗"),"later update retries and clears actual failure only after success");
        stopped(retry);
    }
    static void rejection(String raw)throws Exception{
        boolean rejected=false;try{VerificationJournal19101.decode(raw);}catch(Exception expected){rejected=true;}
        check(rejected,"corrupt/oversized/unknown journal rejected");
    }
    static void corruptAndBounds()throws Exception{
        String good=VerificationJournal19101.encode(Arrays.asList(entry("old-process",1,1)));
        rejection("VJ9:"+good.substring(4));rejection(good.substring(0,4)+(good.charAt(4)=='0'?"1":"0")+good.substring(5));
        rejection(good.substring(0,good.length()-3));rejection(good+"AAAA");
        rejection(String.join("",Collections.nCopies(32769,"x")));
        rejection(VerificationJournal19101.encode(Arrays.asList(entry("duplicate",1,1),entry("duplicate",1,2))));
        ControlledStore store=new ControlledStore(good.substring(0,20));
        VerificationJournal19101.Engine e=new VerificationJournal19101.Engine("bounds-process");store.owner=e;e.start(store);loaded(e);
        check(e.summary().contains("有無は不明")&&!e.summary().contains("保存された記録なし"),"corrupt storage is not reported as absent history");
        for(int i=1;i<=80;i++)e.offer(values(i,i%3),labels());saved(e);
        List<VerificationJournal19101.Entry> rows=VerificationJournal19101.decode(store.raw);
        check(rows.size()==16&&rows.get(0).v[0]==65&&rows.get(15).v[0]==80,"retained journal bound is exact with oldest eviction");
        check(store.raw.length()<=32768,"encoded persistent byte envelope stays bounded");
        long[] mutable=values(81,1);String[] names=labels();e.offer(mutable,names);mutable[3]=2;names[0]="mutated";saved(e);
        VerificationJournal19101.Entry durable=find(store.raw,"bounds-process",81);
        check(durable.v[3]==1&&durable.labels[0].equals("連結"),"caller mutation cannot rewrite immutable pending snapshot");
        stopped(e);
    }
    static String oldEnvelope(VerificationJournal19101.Entry e,int schema)throws Exception{
        java.io.ByteArrayOutputStream raw=new java.io.ByteArrayOutputStream();java.io.DataOutputStream out=new java.io.DataOutputStream(raw);
        out.writeInt(schema==1?0x564a0101:schema==2?0x564a0103:0x564a0104);out.writeInt(1);out.writeUTF(e.process);out.writeUTF(e.version);
        for(int i=0;i<(schema==1?37:schema==2?47:59);i++)out.writeLong(e.v[i]);for(String s:e.labels)out.writeUTF(s);out.close();
        byte[] bytes=raw.toByteArray();StringBuilder hash=new StringBuilder();for(byte b:java.security.MessageDigest.getInstance("SHA-256").digest(bytes))hash.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
        return "VJ"+schema+":"+hash+":"+Base64.getEncoder().encodeToString(bytes);
    }
    static String render(long[] v){
        VerificationJournal19101.Engine engine=new VerificationJournal19101.Engine("display-process");
        VerificationJournal19101.Entry e=new VerificationJournal19101.Entry("previous-process","1.9.104",v,labels());
        engine.rows.put(e.key(),e);engine.loaded=true;return engine.summary();
    }
    static void invalidStage(long[] good,int index,long value){
        long[] v=good.clone();v[index]=value;boolean refused=false;
        try{new VerificationJournal19101.Entry("bad-process","1.9.104",v,labels());}catch(IllegalArgumentException expected){refused=true;}
        check(refused,"invalid stage scalar rejected index="+index+" value="+value);
    }
    static void schema104()throws Exception{
        long[] v=values(101,2);
        v[47]=1L|(1L<<2)|(2L<<10)|(2L<<26);v[48]=6;v[49]=4;v[50]=7;v[51]=0xff112233L;v[52]=0xff112234L;
        v[53]=0;v[54]=1;v[55]=256;v[56]=256;v[57]=64;
        VerificationJournal19101.Entry current=new VerificationJournal19101.Entry("previous-process","1.9.104",v,labels());
        String encoded=VerificationJournal19101.encode(Arrays.asList(current));
        check(encoded.startsWith("VJ5:"),"stage scalars written with VJ5 envelope");
        java.io.DataInputStream wire=new java.io.DataInputStream(new java.io.ByteArrayInputStream(Base64.getDecoder().decode(encoded.substring(69))));
        check(wire.readInt()==0x564a0106,"VJ5 wire magic is distinct from older layouts");
        VerificationJournal19101.Entry restored=VerificationJournal19101.decode(encoded).get(0);
        check(Arrays.equals(v,restored.v)&&restored.version.equals("1.9.104"),"all bounded scalars roundtrip exactly");
        VerificationJournal19101.Entry prior104=VerificationJournal19101.decode(oldEnvelope(current,3)).get(0);
        check(Arrays.equals(Arrays.copyOf(v,59),Arrays.copyOf(prior104.v,59))&&prior104.v[59]==0&&prior104.v[60]==0,"VJ3 stage scalars restored exactly with no fabricated later evidence");
        String text=render(v);
        check(text.contains("half入力=署名差未検出")&&text.contains("Strong出力=署名差あり")&&text.contains("最終出力=厳密比較不一致"),"row signatures and final exact comparison remain distinct");
        check(text.contains("最初に署名差を観測した工程: Strong出力")&&text.contains("(x,y)=(6,3)")&&text.contains("期待 0xff112233・実値 0xff112234"),"stable raw raster difference has true coordinates");
        check(text.contains("署名は認定に不使用")&&text.contains("絶対的な原因証明ではありません"),"stage observation never claims qualification or causal proof");
        for(int kind=1;kind<=3;kind++){
            long[] flat=v.clone();flat[53]=kind;String flatText=render(flat);
            check(flatText.contains("(列,行)=(6,3)")&&flatText.contains("平坦化語位置 198")&&!flatText.contains("(x,y)="),"nonraster layouts never invent image coordinates");
            check(flatText.contains(kind==3?"ARGB期待":"生ビット期待"),"word interpretation follows evidence kind");
        }
        long[] unstable=v.clone();unstable[50]=0;unstable[54]=0;unstable[58]=1;String unconfirmed=render(unstable);
        check(unconfirmed.contains("CPU再生成の安定性未確認・GPU原因とは未判定")&&!unconfirmed.contains("期待 0x"),"unverified replay hides values and does not attribute GPU cause");
        long[] shape=v.clone();shape[49]=shape[50]=shape[54]=shape[57]=0;shape[55]=13;shape[56]=99;
        check(render(shape).contains("語数: 基準 13・比較先 99"),"shape mismatch preserves both differing word counts");
        long[] oldValues=values(9,2);oldValues[37]=1;oldValues[38]=1;oldValues[39]=oldValues[40]=4;oldValues[45]=oldValues[46]=2;
        VerificationJournal19101.Entry old=new VerificationJournal19101.Entry("old-process","1.9.103",oldValues,labels());
        for(int schema=1;schema<=2;schema++){
            VerificationJournal19101.Entry previous=VerificationJournal19101.decode(oldEnvelope(old,schema)).get(0);
            check(previous.version.equals("1.9.103"),"restoring legacy schema never rewrites recorded build version");
            for(int i=schema==1?37:47;i<previous.v.length;i++)check(previous.v[i]==0,"missing historical evidence stays unobserved");
            check(!render(previous.v).contains("保存後の工程診断"),"legacy zeros do not invent stage diagnostic output");
            check(schema==1?previous.v[37]==0:previous.v[39]==4,"VJ2 comparison suffix remains intact");
        }
        for(int i=47;i<59;i++)invalidStage(v,i,-1);
        invalidStage(v,47,1L<<28);invalidStage(v,48,14);invalidStage(v,49,(long)Integer.MAX_VALUE+1);
        invalidStage(v,50,8193);invalidStage(v,51,0x100000000L);invalidStage(v,52,0x100000000L);
        invalidStage(v,53,4);invalidStage(v,54,2);invalidStage(v,55,Long.MAX_VALUE);invalidStage(v,56,Long.MAX_VALUE);
        invalidStage(v,57,8193);invalidStage(v,58,2);invalidStage(v,48,0);invalidStage(v,49,0);invalidStage(v,49,5);
        invalidStage(v,50,65);invalidStage(v,52,v[51]);invalidStage(v,54,0);invalidStage(v,55,198);invalidStage(v,56,257);invalidStage(v,57,0);
    }
    static void seed(Path dir)throws Exception{
        Context context=new Context();VerificationJournal19101.initialize(context);
        VerificationJournal19101.offer(values(1,1),labels());VerificationJournal19101.offer(values(2,2),labels());
        until(()->VerificationJournal19101.summary().contains("直近の反映を確認"),"real public singleton writes journal");
        Files.write(dir.resolve("first-process.txt"),VerificationJournal19101.processId().getBytes("UTF-8"));
        check(Context.WRONG_THREAD==0&&Context.IO_COUNT>0,"Android preferences only touched by journal writer");
        check(Files.readAllBytes(dir.resolve("proof-store.bin"))[0]==71,"separate proof store remains untouched");
    }
    static void restart(Path dir)throws Exception{
        String original=new String(Files.readAllBytes(dir.resolve("first-process.txt")),"UTF-8");
        VerificationJournal19101.initialize(new Context());
        until(()->VerificationJournal19101.summary().contains("復元完了"),"separate JVM restores persisted journal");
        String text=VerificationJournal19101.summary();
        check(!VerificationJournal19101.processId().equals(original),"actual process identity differs after JVM restart");
        check(text.contains("プロセス "+original)&&text.contains("#1 連結")&&text.contains("前回実行中・結果不明"),"old running attempt keeps owner and unknown result");
        check(text.contains("#2 連結")&&text.contains("前回完了 / 速度条件を満たさず"),"old terminal observation remains the recorded failure");
        check(!text.contains("現在実行中")&&Context.IO_COUNT==1&&Context.WRONG_THREAD==0,"reading history does not resume work or rewrite storage");
        check(Files.readAllBytes(dir.resolve("proof-store.bin"))[0]==71,"restored diagnostics do not touch proof store");
    }
    public static void main(String[] args)throws Exception{
        if(args[0].equals("races")){races();corruptAndBounds();schema104();}
        else if(args[0].equals("seed"))seed(Paths.get(args[1]));else restart(Paths.get(args[1]));
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"physical_android_tested\":false,\"device_speedup_verified\":false}");
    }
}

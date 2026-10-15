package com.hiro.ulike;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.CancellationException;

/** Exercises the production observer; the scripted GPU checks ownership only. */
public final class ReasonDiagnostic19105Test {
 static int checks,cases;
 static void check(boolean yes,String why){checks++;if(!yes)throw new AssertionError(why);}
 static void begin(){cases++;ResidentStages19104.clear();GpuNoise1960.reset();Thread.interrupted();ResidentStages19104.begin(new StageDiagnostic19104Test.Cancel());}
 static int[] data(){return StageDiagnostic19104Test.data(12);}
 static void prepare(){int[] cpu=data(),gpu=cpu.clone();gpu[6]++;ResidentStages19104.observeWords(5,cpu,4);ResidentStages19104.baseline();ResidentStages19104.observeWords(5,gpu,4);}
 static String summary(long[] v){check(ResidentStages19104.valid(v),"diagnostic scalar payload valid");StringBuilder s=new StringBuilder();ResidentStages19104.appendSummary(s,v);return s.toString();}
 static void reason(long[] v,long expected,String label){check((v[12]&expected)!=0&&v[11]==1,"reason bit measured: "+label);check(summary(v).contains(label),"reason rendered: "+label);check((v[12]&ResidentStages19104.CPU_REPLAY_UNSTABLE)==0,"unrelated failure does not claim CPU instability");}
 static void expired()throws Exception{Field active=ResidentStages19104.class.getDeclaredField("ACTIVE");active.setAccessible(true);Object context=((ThreadLocal<?>)active.get(null)).get();Field d=context.getClass().getDeclaredField("deadline");d.setAccessible(true);d.setLong(context,System.nanoTime()-1);}
 static void reasons()throws Exception {
  begin();GpuNoise1960.memory=false;ResidentStages19104.observeWords(0,data(),4);reason(ResidentStages19104.finish(-1),ResidentStages19104.WORKSPACE,"診断用メモリ予算不足");
  begin();ResidentStages19104.observeWords(0,new int[8193],8193);reason(ResidentStages19104.finish(-1),ResidentStages19104.INPUT_SHAPE,"診断入力の寸法・容量が対象外");
  begin();expired();ResidentStages19104.observeWords(0,data(),4);reason(ResidentStages19104.finish(-1),ResidentStages19104.DEADLINE,"診断時間上限");
  begin();ResidentStages19104.baseline();ResidentStages19104.observeWords(0,data(),4);reason(ResidentStages19104.finish(-1),ResidentStages19104.MISSING_REFERENCE,"比較用の基準署名なし");
  begin();ResidentStages19104.failed();reason(ResidentStages19104.finish(-1),ResidentStages19104.UNKNOWN_FAILURE,"詳細理由未記録");
  begin();Thread.currentThread().interrupt();boolean stopped=false;try{ResidentStages19104.observeWords(0,data(),4);}catch(CancellationException expected){stopped=true;}finally{Thread.interrupted();}check(stopped,"external interruption propagates");reason(ResidentStages19104.finish(-1),ResidentStages19104.CANCELLED,"撮影優先または割込みによる中断");
  for(int mode=0;mode<5;mode++){
   begin();GpuNoise1960.Session gpu=StageDiagnostic19104Test.source(4,3);ResidentStages19104.observeWords(5,data(),4);ResidentStages19104.baseline();
   if(mode==0)GpuNoise1960.supported=false;if(mode==1)gpu.deny=true;if(mode==2)gpu.failDispatch=true;if(mode==3)gpu.throwRead=true;if(mode==4)gpu.failRelease=true;
   ResidentStages19104.observeGpu(5,gpu,4,4,3);long[] row=ResidentStages19104.finish(-1);
   long[] bits={ResidentStages19104.GPU_UNSUPPORTED,ResidentStages19104.GPU_RESERVATION,ResidentStages19104.GPU_TRANSFER,ResidentStages19104.OBSERVER_EXCEPTION,ResidentStages19104.GPU_RELEASE};
   String[] labels={"診断用GPU処理未対応","診断用GPU領域の予約不可","診断用GPU転送・読戻し失敗","診断処理例外","診断用GPU領域の解放失敗"};reason(row,bits[mode],labels[mode]);check(gpu.liveLeases==0,"failure releases all leases "+mode);
  }
 }
 static class ParentMask {private byte[] skin={1,2,3};}
 static final class ChildMask extends ParentMask {private byte[] detail={4,5};}
 static final class ZeroMask {private byte[] skin=new byte[0];}
 static void masksAndReplay(){
  begin();QualityPixels1932.Plan p=new QualityPixels1932.Plan();p.faceRegions=new FaceRegions1934.Mask(true);ResidentStages19104.observe(7,p);ResidentStages19104.baseline();ResidentStages19104.observe(7,p);long[] row=ResidentStages19104.finish(-1);check(StageDiagnostic19104Test.state(row,8)==1&&row[11]==0,"known empty mask is observed, not diagnostic failure");
  begin();p=new QualityPixels1932.Plan();p.faceRegions=new ChildMask();ResidentStages19104.observe(7,p);ResidentStages19104.baseline();ResidentStages19104.observe(7,p);row=ResidentStages19104.finish(-1);check(StageDiagnostic19104Test.state(row,8)==1&&row[11]==0,"inherited mask planes can be observed");
  begin();p=new QualityPixels1932.Plan();p.faceRegions=new ZeroMask();ResidentStages19104.observe(7,p);row=ResidentStages19104.finish(-1);reason(row,ResidentStages19104.MASK_UNSUPPORTED,"顔・肌マスク形式未対応");
  begin();p=new QualityPixels1932.Plan();p.faceRegions=new Object();ResidentStages19104.observe(7,p);prepare();ResidentStages19104.replay();try{ResidentStages19104.observeWords(5,data(),4);}catch(CancellationException done){}row=ResidentStages19104.finish(-1);
  check(row[7]==1&&row[3]==3&&row[11]==1,"stable target row can coexist with incomplete unrelated mask");reason(row,ResidentStages19104.MASK_UNSUPPORTED,"顔・肌マスク形式未対応");check(summary(row).contains("同じ行の厳密差")&&!summary(row).contains("CPU再生成の対象行署名不一致を検出"),"stable CPU evidence remains precise despite mask omission");
  begin();prepare();ResidentStages19104.replay();int[] unstable=data();unstable[5]++;try{ResidentStages19104.observeWords(5,unstable,4);}catch(CancellationException done){}row=ResidentStages19104.finish(-1);check(row[12]==ResidentStages19104.CPU_REPLAY_UNSTABLE&&row[7]==0&&row[3]==0,"actual replay instability records only instability");check(summary(row).contains("CPU再生成の対象行署名不一致を検出")&&!summary(row).contains("同じ行の厳密差"),"unstable row never supplies exact pixel claim");
 }
 static void nestedAttribution(){
  begin();QualityPixels1932.Plan p=new QualityPixels1932.Plan();p.faceRegions=new ChildMask();ResidentStages19104.observe(7,p);ResidentStages19104.baseline();p.faceRegions=new Object();ResidentStages19104.observe(7,p);long[] row=ResidentStages19104.finish(-1);
  check(StageDiagnostic19104Test.state(row,7)==1&&StageDiagnostic19104Test.state(row,8)==3,"unavailable face child does not erase observed source coefficients");reason(row,ResidentStages19104.MASK_UNSUPPORTED,"顔・肌マスク形式未対応");
  begin();p=new QualityPixels1932.Plan();ResidentStages19104.observe(11,p);ResidentStages19104.baseline();p.localNoise.optionalWeights=new float[8193];ResidentStages19104.observe(11,p);row=ResidentStages19104.finish(-1);
  check(StageDiagnostic19104Test.state(row,11)==1&&StageDiagnostic19104Test.state(row,12)==3,"oversized output-noise child does not erase observed finish coefficients");reason(row,ResidentStages19104.INPUT_SHAPE,"診断入力の寸法・容量が対象外");
  for(int bad=1;bad<=3;bad++){
   begin();StrongNoise1958.Model initial=new StrongNoise1958.Model(8,new int[][]{data(),data(),data()},new float[]{.5f},new float[]{.25f});ResidentStages19104.observe(1,initial);ResidentStages19104.baseline();int[][] maps={data(),data(),data()};if(bad<3)maps[bad]=null;StrongNoise1958.Model broken=new StrongNoise1958.Model(8,maps,bad==3?null:new float[]{.5f},new float[]{.25f});ResidentStages19104.observe(1,broken);row=ResidentStages19104.finish(-1);
   for(int stage=1;stage<=4;stage++)check(StageDiagnostic19104Test.state(row,stage)==(stage==bad+1?3:1),"model child failure attributed only to child "+bad+" stage "+stage);
   check(row[12]==ResidentStages19104.OBSERVER_EXCEPTION,"model child exception reason preserved without unstable claim");
  }
  for(int parent:new int[]{7,11}){
   begin();final int[] counter={0},limit={Integer.MAX_VALUE};ResidentStages19104.begin(new GpuQualification1961.Cancellation(){public boolean cancelled(){return ++counter[0]>=limit[0];}});p=new QualityPixels1932.Plan();p.faceRegions=new ChildMask();ResidentStages19104.observe(parent,p);ResidentStages19104.baseline();counter[0]=0;limit[0]=3;boolean stopped=false;try{ResidentStages19104.observe(parent,p);}catch(CancellationException expected){stopped=true;}row=ResidentStages19104.finish(-1);
   check(stopped&&StageDiagnostic19104Test.state(row,parent)==1,"nested cancellation propagates without erasing successful parent "+parent);check(row[12]==ResidentStages19104.CANCELLED,"nested cancellation is not classified as child exception "+parent);
  }
 }
 static void fixedAPI()throws Exception {
  begin();check(ResidentStages19104.fixedTarget19105()==null,"fixed replay absent before target");prepare();check(ResidentStages19104.fixedTarget19105()==null,"fixed target unavailable during GPU baseline");ResidentStages19104.replay();Object[] target=ResidentStages19104.fixedTarget19105();check(((Integer)target[0])==1&&((int[])target[1]).length==4,"fixed diagnostic receives target row only");((int[])target[1])[0]=0;check(((int[])ResidentStages19104.fixedTarget19105()[1])[0]!=0,"target row is defensive copy");
  int[] row=Arrays.copyOfRange(data(),4,8);check(ResidentStages19104.fixedCpuMatches19105(row),"fixed CPU row verifies against original CPU signature");row[1]++;check(!ResidentStages19104.fixedCpuMatches19105(row),"fixed CPU instability not accepted");check(!ResidentStages19104.fixedCpuMatches19105(new int[5]),"wrong width is not row agreement");
  long[] frozen={1,2,1,0,1,1,3,0xff123456L,0xff123456L,0xff123457L,1,1,4,4};ResidentStages19104.fixedReport19105(frozen);frozen[3]=9;long[] report=ResidentStages19104.finish(-1);check(report.length==27&&report[16]==0&&report[17]==1,"fixed payload copied, no pixels retained in report");check(ResidentStages19104.valid(report),"fixed output is bounded scalar payload");
  begin();prepare();ResidentStages19104.replay();expired();boolean stopped=false;try{ResidentStages19104.checkFixed19105();}catch(CancellationException expected){stopped=true;}check(stopped,"fixed replay shares original cooperative deadline");reason(ResidentStages19104.finish(-1),ResidentStages19104.DEADLINE,"診断時間上限");
  long[] invalid=report.clone();invalid[15]=6;check(!ResidentStages19104.valid(invalid),"unknown native backend rejected");invalid=report.clone();invalid[17]=5;check(!ResidentStages19104.valid(invalid),"diff count beyond target row rejected");invalid=report.clone();invalid[20]=0x100000000L;check(!ResidentStages19104.valid(invalid),"fixed ARGB overflow rejected");invalid=report.clone();invalid[13]=0;check(!ResidentStages19104.valid(invalid),"not-run status cannot carry fabricated observations");
 }
 public static void main(String[] args)throws Exception {reasons();masksAndReplay();nestedAttribution();fixedAPI();System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+checks+",\"cases\":"+cases+",\"diagnostic_reasons_separated\":true,\"legacy_ambiguity_preserved\":true,\"known_empty_mask_observed\":true,\"fixed_replay_api_bounded\":true,\"physical_android_tested\":false}");}
}
final class FaceRegions1934 {public static final class Mask {private final boolean empty;private byte[] skin,detail;public Mask(boolean value){empty=value;}public boolean empty19103(){return empty;}}}

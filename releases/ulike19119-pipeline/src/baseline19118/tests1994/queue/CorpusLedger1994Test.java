package com.hiro.ulike;
import android.content.Context;
import android.content.SharedPreferences;
import java.lang.reflect.*;
import java.util.*;

/** Real finite-corpus store and queue ledger; Android preferences/save-idle
 * boundaries are controlled fixtures, with no invented GPU proof output. */
public final class CorpusLedger1994Test {
 static int checks;static volatile Throwable fault;static Object lastAttempt;static int caseId;
 static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
 static Field field(Class<?> type,String name)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}
 static int value(String name)throws Exception{return field(lastAttempt.getClass(),name).getInt(lastAttempt);}
 static final class Prefs implements SharedPreferences {
  final Context.Memory delegate=new Context.Memory();boolean fail;
  public String getString(String key,String fallback){return delegate.getString(key,fallback);}
  public Map<String,?> getAll(){return delegate.getAll();}
  public Editor edit(){final Editor actual=delegate.edit();return new Editor(){
   public Editor putString(String k,String v){actual.putString(k,v);return this;}
   public Editor remove(String k){actual.remove(k);return this;}
   public void apply(){if(fail)throw new IllegalStateException("controlled preference failure");actual.apply();}
  };}
 }
 static final class App extends Context {final Prefs prefs=new Prefs();public SharedPreferences getSharedPreferences(String name,int mode){return prefs;}}
 interface Body {void run()throws Exception;}
 static void job(final Body body)throws Exception {
  QueueModel1994Test.reset();fault=null;lastAttempt=null;
  check(GpuQualification1961.schedule("corpus-ledger1994:"+(++caseId),512,new GpuQualification1961.Probe(){
   public void run(GpuQualification1961.Cancellation cancellation){try{
    Object job=((ThreadLocal<?>)field(GpuQualification1961.class,"CURRENT").get(null)).get();
    lastAttempt=field(job.getClass(),"attempt1984").get(job);body.run();
   }catch(Throwable failed){fault=failed;}}
   public void close(){}
  }),"corpus bookkeeping probe queued");
  QueuePriority1993Test.drain();if(fault!=null)throw new AssertionError("probe",fault);
  check(lastAttempt!=null,"real active attempt observed");
 }
 static boolean pass(String suite){return GpuCertificates1990.passed(suite,32,100,100,100);}
 public static void main(String[] args)throws Exception {
  QueueModel1994Test.reset();App app=new App();GpuCertificates1990.initialize(app);
  CameraTrace1965.corpusLock=field(GpuCertificates1990.class,"LOCK").get(null);
  int before=field(GpuQualification1961.class,"attemptCount1984").getInt(null);
  check(pass("outside"),"startup corpus can write outside queue");GpuQualification1961.corpusCertificate1994();
  check(field(GpuQualification1961.class,"attemptCount1984").getInt(null)==before,"startup write creates no photo attempt");
  job(new Body(){public void run(){check(pass("success"),"first corpus write persists");check(pass("success"),"same suite rewrite persists");}});
  check(value("corpusWrites1994")==2&&value("certificates")==0,"corpus updates count separately from photo records");
  check(value("outcome")==1,"corpus-only successful job is recorded");
  check(CameraTrace1965.corpusEvents>0&&!CameraTrace1965.insideStoreLock,"queue notification occurs outside certificate lock");
  String summary=GpuQualification1961.attemptSummary1985();
  check(summary.contains("写真条件の認定書込 0回 / 固定テストの認定書込 2回"),"independent ledger labels displayed");
  check(summary.contains("固定テストの書込だけでは速度条件"),"quality writes not represented as speed admission");
  job(new Body(){public void run(){check(pass("slow-quality"),"quality writes before speed fail");GpuQualification1961.outcome1984("speed_condition");}});
  check(value("corpusWrites1994")==1&&value("outcome")==11,"speed failure takes precedence over quality write");
  job(new Body(){public void run(){check(pass("before-cancel"),"completed corpus write before cancel");Thread.currentThread().interrupt();check(!pass("cancelled"),"cancelled store write declined");GpuQualification1961.corpusCertificate1994();GpuQualification1961.outcome1984("qualified");}});
  check(value("corpusWrites1994")==1&&value("outcome")==4,"cancelled callbacks ignored and cancellation wins");
  final App broken=new App();GpuCertificates1990.initialize(broken);
  GpuCertificates1990.reject("rejected","complete controlled mismatch");
  job(new Body(){public void run(){
   check(!GpuCertificates1990.passed("invalid",0,1,1,1),"invalid proof not saved");
   check(!pass("rejected"),"sticky rejection not rewritten");
   broken.prefs.fail=true;check(!pass("failed-write"),"preference failure not saved");broken.prefs.fail=false;
   GpuQualification1961.outcome1984("qualified");
  }});
  check(value("corpusWrites1994")==0&&value("outcome")==3,"failed/invalid/rejected writes do not create success");
  int previous=value("corpusWrites1994");GpuQualification1961.corpusCertificate1994();
  check(value("corpusWrites1994")==previous,"finished attempt immutable outside active thread");
  job(new Body(){public void run(){GpuQualification1961.outcome1984("cached_preparation");}});
  check(value("corpusWrites1994")==0&&value("certificates")==0&&value("outcome")==16,
   "restored GPU preparation succeeds without inventing a new comparison or certificate");
  check(GpuQualification1961.attemptSummary1985().contains("既存認定を復元・GPU準備完了（再比較なし）"),
   "restored preparation gets an explicit result label");
  job(new Body(){public void run(){GpuQualification1961.outcome1984("cached_preparation");Thread.currentThread().interrupt();}});
  check(value("outcome")==4&&value("corpusWrites1994")==0,"cancellation overrides restored-preparation outcome");
  System.out.println("{\"status\":\"passed\",\"assertions\":"+checks+",\"actual_corpus_store_and_queue\":true,\"physical_android_tested\":false}");
 }
}
final class CameraTrace1965 {
 static Object corpusLock;static int corpusEvents;static boolean insideStoreLock;
 public static void event(String name,long capture,String data){if("gpu_proof94_corpus".equals(name)){corpusEvents++;insideStoreLock|=Thread.holdsLock(corpusLock);}}
}

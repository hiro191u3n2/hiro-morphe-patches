package com.hiro.ulike;
import java.lang.reflect.Field;

/** Diagnostic cache restore uses controlled host certificates only; inherited
 * finish recovery independently proves the real 28-case GPU corpus. */
public final class WideRestore1998Test {
 static int assertions;
 static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
 static void set(String name,String value)throws Exception{Field f=GpuFinishDefault1990.class.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
 public static void main(String[] args)throws Exception{
  FinishPolicy1992Test.certificateStore();
  check(GpuNoise1960.supports(GpuNoise1960.FINISH1961),"initialize controlled host driver identity before storing certificates");
  set("wideStatus1995","128行の検証待ち");set("firstWideFailure1995","");set("lastWideFailure1995","");
  check(GpuFinishDefault1990.summaryWide1995().equals("128行未認定 / 128行の検証待ち"),"unverified process preserves waiting state");
  String narrow=GpuFinishDefault1990.activeSuite1992(),wide=GpuFinishDefault1990.wideSuite1993();
  check(GpuCertificates1990.passed(narrow,28,3000000,1000,1000),"install controlled host narrow certificate");
  check(GpuFinishDefault1990.summaryWide1995().startsWith("128行未認定"),"narrow certificate never certifies wide schedule");
  check(GpuCertificates1990.passed(wide,28,3000000,1000,1000),"install controlled matching host wide certificate");
  int before32=FinishSchedule1993Test.attempts32,before128=FinishSchedule1993Test.attempts128;
  String restored=GpuFinishDefault1990.summaryWide1995();
  check(restored.equals("128行認定あり / 端末・処理版が一致する128行記録を復元（今回の再検証なし）"),"restored admission does not contradict untouched per-process status");
  check(!restored.contains("検証待ち"),"startup/live snapshots cannot advertise a pending corpus after restore");
  check(restored.equals(GpuFinishDefault1990.summaryWide1995()),"repeated live rendering is stable");
  check(before32==FinishSchedule1993Test.attempts32&&before128==FinishSchedule1993Test.attempts128,"restored diagnostic performs no extra finish GPU dispatch");
  set("wideStatus1995","28例の検証完了（全画素一致26例・透過対象外2例）");
  check(GpuFinishDefault1990.summaryWide1995().contains("28例の検証完了"),"actual fresh corpus status retained");
  set("wideStatus1995","case=3 検証メモリ不足");set("firstWideFailure1995","case=1 実行・転送を完了できず");set("lastWideFailure1995","case=3 検証メモリ不足");
  String history=GpuFinishDefault1990.summaryWide1995();
  check(history.contains("case=3 検証メモリ不足")&&history.contains("最初の失敗: case=1"),"real same-process failure history is never hidden by formatting fix");
  set("wideStatus1995","128行の検証待ち");GpuCertificates1990.reject(wide,"controlled rejected certificate");
  String rejected=GpuFinishDefault1990.summaryWide1995();
  check(rejected.startsWith("128行は画質不一致で拒否")&&!rejected.contains("記録を復元"),"sticky rejection overrides restored success");
  check(before32==FinishSchedule1993Test.attempts32&&before128==FinishSchedule1993Test.attempts128,"all diagnostic reads avoid GPU finish execution");
  System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"wide_restore_status_verified\":true,\"no_extra_gpu_dispatch_verified\":true,\"controlled_host_certificate_only\":true}");
 }
}

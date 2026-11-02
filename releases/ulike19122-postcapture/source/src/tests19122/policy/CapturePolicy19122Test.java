package com.hiro.ulike;
import android.preference.*;import android.app.AlertDialog;
public final class CapturePolicy19122Test {
 static int assertions;
 static void check(boolean b,String why){assertions++;if(!b)throw new AssertionError(why);}
 static void normal(){check(CapturePolicy19122.cpuWorkerLimit()==2,"normal CPU2");check(CapturePolicy19122.gpuBanks()==1,"normal one GPU bank");check(CapturePolicy19122.gpuSubmissionGapNanos()==2000000L,"normal submit gap");check(!CapturePolicy19122.earlyPreviewEnabled(),"normal early preview off");check(CapturePolicy19122.cleanupEnabled(),"normal cleanup on");check(!CapturePolicy19122.bypassProcessing()&&!CapturePolicy19122.diagnostic19122(),"normal full quality");}
 public static void main(String[] args){
  normal();check(!CapturePolicy19122.arm19122(-1)&&!CapturePolicy19122.arm19122(7),"invalid selector rejected");
  for(int mode=1;mode<=6;mode++){
   check(CapturePolicy19122.arm19122(mode),"idle comparison accepted");normal();
   long id=100+mode;CapturePolicy19122.begin19122(id);
   check(CapturePolicy19122.diagnostic19122(),"active diagnostic");
   check(CapturePolicy19122.cpuWorkerLimit()==(mode==1||mode==2?4:2),"CPU one-variable control");
   check(CapturePolicy19122.gpuBanks()==(mode==1||mode==3?2:1),"GPU one-variable control");
   check(CapturePolicy19122.gpuSubmissionGapNanos()==(mode==1||mode==3?0:2000000L),"gap control");
   check(CapturePolicy19122.earlyPreviewEnabled()==(mode==1||mode==4),"preview control");
   check(CapturePolicy19122.cleanupEnabled()==(mode!=1&&mode!=5),"cleanup control");
   check(CapturePolicy19122.bypassProcessing()==(mode==6),"bypass explicit only");
   check(!CapturePolicy19122.arm19122(0),"running save cannot be modified");
   CapturePolicy19122.end19122(id+1000);check(CapturePolicy19122.diagnostic19122(),"stale finish does not reset active");
   CapturePolicy19122.begin19122(id);CapturePolicy19122.end19122(id);normal();
   CapturePolicy19122.begin19122(id+2000);normal();CapturePolicy19122.end19122(id+2000);normal();
  }
  PreferenceActivity activity=new PreferenceActivity();CapturePolicy19122.install19122(activity);CapturePolicy19122.install19122(activity);
  check(activity.screen.rows.size()==1,"settings row idempotent");Preference row=activity.screen.rows.get(0);
  check(row.listener.onPreferenceClick(row)&&AlertDialog.shows==1&&AlertDialog.items.length==7,"all comparison choices accessible");
  check(AlertDialog.items[6].toString().contains("画質が変わります"),"bypass disclosure visible before selection");
  AlertDialog.selected.onClick(null,6);check(row.summary.toString().contains("診断用"),"armed mode visible");normal();CapturePolicy19122.begin19122(9999);check(CapturePolicy19122.bypassProcessing(),"selection consumed next capture");
  AlertDialog.selected.onClick(null,2);check(row.summary.toString().contains("保存処理中"),"busy selection explained");check(CapturePolicy19122.bypassProcessing(),"busy selection preserves active snapshot");CapturePolicy19122.end19122(9999);normal();
  activity.destroyed=true;row.listener.onPreferenceClick(row);check(AlertDialog.shows==1,"destroyed activity no dialog");
  CapturePolicy19122.install19122(null);PreferenceActivity missing=new PreferenceActivity();missing.screen=null;CapturePolicy19122.install19122(missing);normal();
  System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"one_shot_policy_executed\":true}");
 }
}

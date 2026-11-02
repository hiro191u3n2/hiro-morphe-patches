package com.hiro.ulike;
import android.app.*;import android.content.*;import android.widget.*;
public final class CopyLogs1993Test {
 static int assertions;static void check(boolean value,String label){assertions++;if(!value)throw new AssertionError(label);}
 public static void main(String[] args)throws Exception{
  Activity app=new Activity();StringBuilder longText=new StringBuilder("ULike v1.9.93 / 撮影 #93 / 完了\n");
  for(int i=0;i<1500;i++)longText.append("工程 ").append(i).append(" 日本語 🐱 GPU 16区間 / CPU 0区間\n");
  longText.append("表示時点の検証予約: 3件 / 実行中: 1件\n末尾の診断情報も省略しない");
  ProcessingTiming1947.value=longText.toString();ProcessingTiming1947.calls=0;
  CameraTrace1965.details1969(app);AlertDialog dialog=AlertDialog.last;
  check(dialog!=null&&dialog.message.length()>40000,"full long log displayed");
  check(dialog.messageView.selectable,"native message text is long-press selectable");
  check("全文をコピー".equals(dialog.labels.get("neutral")),"dedicated full-copy action labeled");
  check("閉じる".equals(dialog.labels.get("negative")),"close action preserved");
  check(app.clipboard.writes==0,"opening does not touch clipboard");
  String displayed=dialog.message;
  ProcessingTiming1947.value="ULike v1.9.93 / 撮影 #94 / 処理中\n後から変わった検証状態";
  check(dialog.neutral.performClick(),"copy listener is installed after show");
  check(displayed.contentEquals(app.clipboard.clip.text),"copy is exact frozen displayed snapshot including tail and live-state snapshot");
  check(app.clipboard.clip.text.toString().contains("末尾の診断情報も省略しない"),"full log tail survives without clipboard truncation");
  check(app.clipboard.clip.text.toString().contains("🐱"),"Unicode surrogate pair preserved");
  check("ULike 撮影ログ v1.9.93".contentEquals(app.clipboard.clip.label),"clipboard metadata carries actual release");
  check(ProcessingTiming1947.calls==1,"copy never refetches newer photo or queue state");
  check(!dialog.dismissed,"copy leaves dialog and scroll position open");
  check("ログ全文をコピーしました。".equals(Toast.last),"success toast follows actual clipboard write");
  dialog.neutral.performClick();check(app.clipboard.writes==2,"repeat copy works without reopening");
  app.clipboard.fail=true;dialog.neutral.performClick();
  check(app.clipboard.writes==2&&displayed.contentEquals(app.clipboard.clip.text),"failed write cannot overwrite existing clipboard");
  check("コピーできませんでした。長押しでログを選択できます。".equals(Toast.last),"write failure is visible without false success");
  check(!dialog.dismissed&&dialog.messageView.selectable,"clipboard failure leaves manual selection accessible");
  app.clipboard=null;dialog.neutral.performClick();
  check("コピーできませんでした。長押しでログを選択できます。".equals(Toast.last),"missing clipboard service has same safe fallback");
  CameraTrace1965.details1969(app);check(AlertDialog.last.message.startsWith("ULike v1.9.93 / 撮影 #94"),"reopening deliberately refreshes displayed snapshot");
  check(CameraTrace1965.VERSION.equals("1.9.93"),"camera recorder header matches release");
  System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"physical_android_tested\":false}");
 }
}

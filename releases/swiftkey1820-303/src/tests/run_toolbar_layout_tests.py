#!/usr/bin/env python3
"""Host geometry, native-controller and access-path tests for the real bridge."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess

STUBS = {
    "android/os/Looper.java": "package android.os; public class Looper {public static final Looper MAIN=new Looper();public static boolean main=true;public static Looper getMainLooper(){return MAIN;}public static Looper myLooper(){return main?MAIN:null;}}",
    "android/os/Handler.java": "package android.os; public class Handler {public static java.util.List<Runnable> pending=new java.util.ArrayList<>();public Handler(Looper l){}public boolean post(Runnable r){pending.add(r);return true;}public static void drain(){for(Runnable r:new java.util.ArrayList<>(pending))r.run();pending.clear();}}",
    "android/content/Context.java": "package android.content; public class Context {}",
    "android/content/ContextWrapper.java": "package android.content; public class ContextWrapper extends Context { public Context base; public ContextWrapper(Context b){base=b;} public Context getBaseContext(){return base;} }",
    "android/view/ViewParent.java": "package android.view; public interface ViewParent { ViewParent getParent(); }",
    "android/view/Gravity.java": "package android.view; public class Gravity { public static final int LEFT=3,TOP=48,END=8388613; }",
    "android/view/View.java": r'''package android.view;
import android.content.Context;
public class View implements ViewParent {
 public static final int GONE=8,VISIBLE=0,IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS=4,LAYOUT_DIRECTION_RTL=1;
 public Context context; public int id,visibility,access,direction,measured=32,paramSets,clicks; public boolean attached,enabled=true,clickable=true,focusable=true; public float alpha=1; public ViewParent parent; public ViewGroup.LayoutParams params; public OnClickListener listener;
 public View(Context c){context=c;} public Context getContext(){return context;} public ViewParent getParent(){return parent;}
 public void setId(int i){id=i;} public View findViewById(int i){return id==i?this:null;}
 public void setAlpha(float f){alpha=f;} public float getAlpha(){return alpha;} public boolean isEnabled(){return enabled;} public void setEnabled(boolean v){enabled=v;}
 public void setOnClickListener(OnClickListener v){listener=v;} public void setClickable(boolean v){clickable=v;} public void setFocusable(boolean v){focusable=v;}
 public void setImportantForAccessibility(int i){access=i;} public int getImportantForAccessibility(){return access;}
 public boolean isAttachedToWindow(){return attached;} public int getVisibility(){return visibility;} public void setVisibility(int i){visibility=i;}
 public ViewGroup.LayoutParams getLayoutParams(){return params;} public void setLayoutParams(ViewGroup.LayoutParams p){params=p;paramSets++;}
 public int getMeasuredWidth(){return measured;} public int getLayoutDirection(){return direction;}
 public boolean performTouchClick(){if(!enabled||!clickable||visibility!=0||listener==null)return false;listener.onClick(this);return true;}
 public interface OnClickListener { void onClick(View v); }
}''',
    "android/view/ViewGroup.java": r'''package android.view;
import android.content.Context; import java.util.*;
public class ViewGroup extends View {
 public List<View> children=new ArrayList<>(); public ViewGroup(Context c){super(c);} public int getChildCount(){return children.size();} public View getChildAt(int i){return children.get(i);}
 public void addView(View v){children.add(v);v.parent=this;} public View findViewById(int id){View v=super.findViewById(id);if(v!=null)return v;for(View c:children){v=c.findViewById(id);if(v!=null)return v;}return null;}
 public static class LayoutParams { public int width,height; public LayoutParams(int w,int h){width=w;height=h;} }
 public static class MarginLayoutParams extends LayoutParams { public int leftMargin,rightMargin,topMargin,bottomMargin,start=Integer.MIN_VALUE,end=Integer.MIN_VALUE;public MarginLayoutParams(int w,int h){super(w,h);}public void setMarginStart(int x){start=x;}public void setMarginEnd(int x){end=x;}public int getMarginStart(){return start==Integer.MIN_VALUE?leftMargin:start;}public int getMarginEnd(){return end==Integer.MIN_VALUE?rightMargin:end;} }
}''',
    "android/widget/FrameLayout.java": "package android.widget; public class FrameLayout extends android.view.ViewGroup { public FrameLayout(android.content.Context c){super(c);} public static class LayoutParams extends android.view.ViewGroup.MarginLayoutParams { public int gravity; public LayoutParams(int w,int h){super(w,h);} } }",
    "androidx/recyclerview/widget/RecyclerView.java": "package androidx.recyclerview.widget; public class RecyclerView extends android.view.ViewGroup {public RecyclerView(android.content.Context c){super(c);}}",
    "com/touchtype/KeyboardService.java": "package com.touchtype; public class KeyboardService extends android.content.Context {}",
    "ah0/w0.java": "package ah0; public class w0 {public boolean dedicated=true; public boolean G(){return dedicated;} }",
    "kd0/d0.java": "package kd0; public class d0 {public int a=2;public boolean b=true;}",
    "kd0/e0.java": "package kd0; public class e0 {public d0 g=new d0();public ah0.w0 b=new ah0.w0();public int notifications,writes;public boolean persisted,failAfter,failBefore;public static void n(e0 m,int s){if(m.failBefore)throw new RuntimeException();m.g.a=s;if(m.failAfter)throw new RuntimeException();m.writes++;m.persisted=s==0;m.notifications++;}}",
    "ob0/b1.java": "package ob0; public class b1 extends android.view.View {public kd0.e0 c;public b1(android.content.Context c,kd0.e0 m){super(c);this.c=m;}}",
    "ob0/d1.java": "package ob0; public class d1 extends android.view.ViewGroup {public b1 button;public d1(android.content.Context c,kd0.e0 m){super(c);button=new b1(c,m);addView(button);}public b1 getButton(){return button;}}",
    "ui0/k.java": "package ui0; public class k {public int size=48;public int d(){return size;}}",
    "com/touchtype/keyboard/expandedcandidate/ExpandedResultsCloseButton.java": "package com.touchtype.keyboard.expandedcandidate;public class ExpandedResultsCloseButton extends android.view.View {public ui0.k a=new ui0.k();public ExpandedResultsCloseButton(android.content.Context c){super(c);}}",
}

HARNESS = r'''import android.content.*; import android.view.*; import android.widget.*;
import androidx.recyclerview.widget.RecyclerView; import com.touchtype.KeyboardService;
import com.touchtype.keyboard.expandedcandidate.ExpandedResultsCloseButton;
import hiro.swiftkey.CandidateToolbar;
public class ToolbarLayoutTests {
 static int assertions,scenarios;
 static void check(boolean value,String label){assertions++;if(!value)throw new AssertionError(label);}
 static FrameLayout.LayoutParams lp(int w,int h){return new FrameLayout.LayoutParams(w,h);}
 static FrameLayout makeBar(Context c,int width,int dir){FrameLayout b=new FrameLayout(c);b.direction=dir;View list=new View(c);list.id=0x7f0a0473;list.direction=dir;FrameLayout.LayoutParams l=lp(-1,-1);l.setMarginEnd(width);list.params=l;b.addView(list);View button=new View(c);button.id=0x7f0a0474;FrameLayout.LayoutParams p=lp(width,-1);p.gravity=Gravity.END;button.params=p;button.listener=v->v.clicks++;b.addView(button);return b;}
 static void geometry(){
  for(int dir=0;dir<2;dir++)for(int width:new int[]{24,32,48,64,96,144})for(int screen:new int[]{320,720,1440}){
   scenarios++; FrameLayout bar=makeBar(new Context(),width,dir);View list=bar.getChildAt(0),button=bar.getChildAt(1);int oldDir=list.direction;
   CandidateToolbar.layoutCollapsed(bar);FrameLayout.LayoutParams bp=(FrameLayout.LayoutParams)button.params, p=(FrameLayout.LayoutParams)list.params;
   check(bp.gravity==(Gravity.LEFT|Gravity.TOP),"real expand button goes physical left");check(p.leftMargin==width&&p.rightMargin==0,"candidate rectangle clears left control");
   check(p.getMarginStart()==(dir==1?0:width)&&p.getMarginEnd()==(dir==1?width:0),"relative margins resolve physical left in RTL");
   int buttonLeft=0,buttonRight=width,listLeft=p.leftMargin,listRight=screen-p.rightMargin;
   check(buttonLeft==0&&buttonRight<=listLeft&&listRight==screen,"button/list hit rectangles are disjoint and use right edge");
   check(list.direction==oldDir&&bar.children.get(0)==list&&bar.children.get(1)==button,"text and child order preserved");
   check(button.performTouchClick()&&button.clicks==1,"moved native expand listener still works");
   int count=list.paramSets+button.paramSets;CandidateToolbar.layoutCollapsed(bar);check(count==list.paramSets+button.paramSets,"idempotent measure does not schedule endless relayout");
   button.visibility=View.GONE;CandidateToolbar.layoutCollapsed(bar);check(button.visibility==View.GONE&&!button.performTouchClick(),"empty candidate state stays hidden");
   bar.children.remove(button);CandidateToolbar.layoutCollapsed(bar);check(p.leftMargin==0&&p.rightMargin==0,"layout without more button releases reserved space");
  }
  for(int dir=0;dir<2;dir++)for(int size:new int[]{28,40,56,72,112}){
   scenarios++;Context c=new Context();FrameLayout outer=new FrameLayout(c),header=new FrameLayout(c);RecyclerView grid=new RecyclerView(c);grid.direction=dir;grid.params=lp(-1,-1);outer.addView(grid);outer.addView(header);
   ExpandedResultsCloseButton close=new ExpandedResultsCloseButton(c);close.a.size=size;close.params=lp(32,-2);close.listener=v->v.clicks++;header.addView(close);
   CandidateToolbar.layoutExpandedClose(close);FrameLayout.LayoutParams p=(FrameLayout.LayoutParams)close.params,g=(FrameLayout.LayoutParams)grid.params;
   check(p.gravity==(Gravity.LEFT|Gravity.TOP)&&p.width==size&&p.height==size,"close geometry uses native keyboard size");check(g.leftMargin==size&&g.rightMargin==0,"first expanded candidate cannot hide under close");check(grid.direction==dir,"expanded order preserved");check(close.performTouchClick()&&close.clicks==1,"native close listener retained");
   int count=close.paramSets+grid.paramSets;CandidateToolbar.layoutExpandedClose(close);check(count==close.paramSets+grid.paramSets,"expanded layout idempotent");
   close.a.size=size+7;CandidateToolbar.layoutExpandedClose(close);check(g.leftMargin==size+7&&p.width==size+7,"rotation/key resize updates actual hit reserve");
  }
  scenarios++;CandidateToolbar.layoutCollapsed(null);CandidateToolbar.layoutContainer(null);CandidateToolbar.layoutExpandedClose(null);CandidateToolbar.bindToolbar(null);CandidateToolbar.layoutCollapsed(new View(new Context()));check(true,"foreign/missing view paths safely ignored");
 }
 static void accessAndState(){
  scenarios++;KeyboardService service=new KeyboardService();kd0.e0 model=new kd0.e0();ContextWrapper c=new ContextWrapper(new ContextWrapper(service));ob0.d1 wrapper=new ob0.d1(c,model);wrapper.attached=true;wrapper.listener=v->v.clicks++;wrapper.button.listener=v->v.clicks++;
  FrameLayout ribbon=new FrameLayout(c),slot=new FrameLayout(c);slot.id=0x7f0a04b8;ribbon.addView(slot);slot.addView(wrapper);CandidateToolbar.bindToolbar(wrapper);CandidateToolbar.layoutContainer(ribbon);
  check(wrapper.alpha==1&&slot.visibility==0,"toolbar access remains until emoji hook succeeds");check(!CandidateToolbar.toggle(service),"unconfigured emoji cannot toggle");
  CandidateToolbar.setEmojiAccess(service,true);check(wrapper.alpha==0&&!wrapper.enabled&&!wrapper.button.enabled&&wrapper.access==4,"ready emoji route hides and disables original marker");check(slot.visibility==View.GONE,"old left slot reclaimed");check(wrapper.listener!=null&&wrapper.button.listener!=null,"native listeners retained for fallback");
  wrapper.visibility=0;check(wrapper.alpha==0&&!wrapper.performTouchClick()&&!wrapper.button.performTouchClick(),"native visibility animations cannot revive ghost hit target");
  check(CandidateToolbar.toggle(service)&&model.g.a==0&&model.persisted&&model.notifications==1,"longpress opens native model and persists");
  check(CandidateToolbar.closeIfOpen(service)&&model.g.a==2&&!model.persisted&&model.notifications==2,"shortpress closes native model without emoji");
  check(!CandidateToolbar.closeIfOpen(service)&&model.notifications==2,"normal shortpress remains available when closed");
  check(CandidateToolbar.toggle(service)&&CandidateToolbar.toggle(service)&&model.g.a==2,"longpress toggles both directions");
  CandidateToolbar.setEmojiAccess(service,false);check(wrapper.alpha==1&&wrapper.enabled&&wrapper.button.enabled&&slot.visibility==0,"combined mic mode restores arrow and slot");check(wrapper.performTouchClick()&&wrapper.button.performTouchClick(),"restored native controls still callable");
  CandidateToolbar.setEmojiAccess(service,true);model.b.dedicated=false;CandidateToolbar.layoutCollapsed(makeBar(c,32,0));check(wrapper.alpha==1&&slot.visibility==0,"native dedicated-key preference off restores access");check(!CandidateToolbar.toggle(service),"preference off does not hijack toolbar");
  model.b.dedicated=true;CandidateToolbar.layoutCollapsed(makeBar(c,32,0));check(wrapper.alpha==0&&slot.visibility==8,"same-service native layout recreation restores requested placement");
  scenarios++;KeyboardService another=new KeyboardService();CandidateToolbar.setEmojiAccess(another,true);check(!CandidateToolbar.toggle(another)&&model.g.a==2,"another service cannot control stale keyboard");
  wrapper.attached=false;check(!CandidateToolbar.toggle(service),"detached ribbon ignored");wrapper.attached=true;
  ob0.d1 detached=new ob0.d1(c,new kd0.e0());CandidateToolbar.bindToolbar(detached);check(CandidateToolbar.toggle(service)&&model.g.a==0,"detached newer wrapper does not shadow live one");
  model.failAfter=true;check(CandidateToolbar.closeIfOpen(service)&&model.g.a==2,"close consumed if native state changed before persistence error");model.failAfter=false;
  model.g.a=0;model.failBefore=true;check(!CandidateToolbar.closeIfOpen(service)&&model.g.a==0,"failed transition not reported as complete");model.failBefore=false;model.g.a=1;check(!CandidateToolbar.toggle(service),"unrecognized native state untouched");
  scenarios++;ob0.d1 foreign=new ob0.d1(new Context(),new kd0.e0());CandidateToolbar.bindToolbar(foreign);check(foreign.alpha==1&&foreign.enabled,"unresolvable service leaves original access");ContextWrapper cycle=new ContextWrapper(null);cycle.base=cycle;ob0.d1 cyclic=new ob0.d1(cycle,new kd0.e0());CandidateToolbar.bindToolbar(cyclic);check(cyclic.alpha==1,"context cycle terminates without disabling control");
  scenarios++;KeyboardService early=new KeyboardService();CandidateToolbar.setEmojiAccess(early,true);ob0.d1 late=new ob0.d1(early,new kd0.e0());CandidateToolbar.bindToolbar(late);check(late.alpha==0,"emoji-before-ribbon construction order supported");
  scenarios++;android.os.Looper.main=false;CandidateToolbar.setEmojiAccess(early,false);check(late.alpha==0,"off-main model factory does not mutate views");android.os.Looper.main=true;android.os.Handler.drain();check(late.alpha==1,"queued main-thread update restores controls");
 }
 public static void main(String[] args){geometry();accessAndState();System.out.println("{\"status\":\"PASSED\",\"assertions\":"+assertions+",\"scenarios\":"+scenarios+"}");}
}'''


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--source-dir", required=True, type=Path)
    parser.add_argument("--work", required=True, type=Path)
    args = parser.parse_args()
    args.work.mkdir(parents=True, exist_ok=True)
    src = args.work / "src"
    out = args.work / "classes"
    out.mkdir(exist_ok=True)
    files = []
    for name, content in STUBS.items():
        path = src / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding="utf-8")
        files.append(str(path))
    harness = src / "ToolbarLayoutTests.java"
    harness.write_text(HARNESS, encoding="utf-8")
    implementation = args.source_dir / "src/hiro/swiftkey/CandidateToolbar.java"
    subprocess.run(["java", "com.sun.tools.javac.Main", "--release", "8", "-encoding", "UTF-8", "-d", str(out), *files, str(implementation), str(harness)], check=True)
    result = subprocess.run(["java", "-cp", str(out), "ToolbarLayoutTests"], check=True, capture_output=True, text=True)
    report = json.loads(result.stdout)
    report["source_sha256"] = hashlib.sha256(implementation.read_bytes()).hexdigest()
    report["production_source_sha256"] = report["source_sha256"]
    report["device_tested"] = False
    report["android_instrumentation_tested"] = False
    report["scope"] = "Host geometry/controller tests; actual Android rendering and handset input not measured"
    (args.work / "report.json").write_text(json.dumps(report, ensure_ascii=False, indent=2)+"\n")
    print(json.dumps(report, ensure_ascii=False))


if __name__ == "__main__":
    main()

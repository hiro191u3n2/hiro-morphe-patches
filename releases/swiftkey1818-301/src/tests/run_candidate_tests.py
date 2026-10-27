#!/usr/bin/env python3
"""Production CandidateScroll behavior on explicit native-grid/RecyclerView host fakes."""
import argparse
import hashlib
import json
import shutil
import subprocess
from pathlib import Path

SOURCES = {
    'android/os/Looper.java': '''package android.os; public final class Looper {
 private static final Looper MAIN=new Looper();public static boolean isMain=true;
 public static Looper getMainLooper(){return MAIN;}public static Looper myLooper(){return isMain?MAIN:null;}
}''',
    'android/view/View.java': '''package android.view; public class View {
 public boolean attached=true, shown=true; public int width=780,height=600,top;
 public boolean isAttachedToWindow(){return attached;} public boolean isShown(){return shown;}
 public int getWidth(){return width;} public int getHeight(){return height;}
}''',
    'androidx/recyclerview/widget/RecyclerView.java': '''package androidx.recyclerview.widget;
public class RecyclerView extends android.view.View {
 public ra.q1 adapter; public ra.a2 layout; public boolean computing;
 public ra.x1 animator=new ra.x1(); public int animatorSets;
 public int scrollPosition=7,scrollOffset=431,scrollState=2,stopCalls;
 public boolean failAnimatorRead,failAnimatorWrite;
 public final java.util.ArrayList<String> events=new java.util.ArrayList<>();
 public boolean layoutPending,layoutWithAnimator;public int layoutFrames;
 public boolean c0(){return computing;} public ra.q1 getAdapter(){return adapter;}
 public ra.a2 getLayoutManager(){return layout;}
 public ra.x1 getItemAnimator(){
  if(failAnimatorRead)throw new NoSuchMethodError("test native animator ABI failure");
  return animator;
 }
 public void setItemAnimator(ra.x1 value){
  if(failAnimatorWrite)throw new IllegalStateException("test native animator failure");
  if(animator!=null){events.add("endAnimations");animator.d();}
  animator=value;animatorSets++;events.add(value==null?"animator:null":"animator:set");
 }
 // Native observers queue requestLayout/postOnAnimation; they do not perform
 // synchronous layout inside notify. This host model records that boundary.
 public void queueAdapterLayout(String notification){events.add(notification);layoutPending=true;}
 public void runDeferredLayout(){
  if(!layoutPending)return;
  events.add("layout");layoutPending=false;layoutFrames++;layoutWithAnimator=animator!=null;
  if(layout instanceof LinearLayoutManager){LinearLayoutManager manager=(LinearLayoutManager)layout;
   if(manager.x>=0){scrollPosition=manager.x;scrollOffset=manager.y==Integer.MIN_VALUE?0:manager.y;}
   manager.x=-1;manager.y=Integer.MIN_VALUE;
  }
 }
 public void w0(int position){stopCalls++;scrollState=0;events.add("nativeScrollToStart");
  LinearLayoutManager manager=(LinearLayoutManager)layout;manager.x=position;manager.y=Integer.MIN_VALUE;layoutPending=true;}
}''',
    'com/touchtype/ui/AccessibilityEmptyRecyclerView.java': '''package com.touchtype.ui;
public class AccessibilityEmptyRecyclerView extends androidx.recyclerview.widget.RecyclerView {}''',
    'androidx/recyclerview/widget/LinearLayoutManager.java': '''package androidx.recyclerview.widget;
public class LinearLayoutManager extends ra.a2 {
 public int x=-1,y=Integer.MIN_VALUE,first=-1,last=-1,anchorCalls;public ra.n1 r=new ra.n1();
 public final java.util.Map<Integer,android.view.View> children=new java.util.HashMap<>();
 public int V0(){return first;}public int W0(){return last;}public android.view.View q(int position){return children.get(position);}
 public void l1(int position,int offset){x=position;y=offset;anchorCalls++;b.layoutPending=true;b.events.add("anchor");}
}''',
    'androidx/recyclerview/widget/GridLayoutManager.java': '''package androidx.recyclerview.widget;
public class GridLayoutManager extends LinearLayoutManager { public int G=8; public ce0.a L; }''',
    'ra/a2.java': '''package ra; public abstract class a2 { public androidx.recyclerview.widget.RecyclerView b; }''',
    'ra/n1.java': '''package ra; public class n1 {
 public int padding=9;public int e(android.view.View child){return child.top;}public int k(){return padding;}
}''',
    'ra/x1.java': '''package ra; public class x1 {
 public int endCalls;public boolean running=true;
 public void d(){endCalls++;running=false;}
}''',
    'ra/q1.java': '''package ra; public abstract class q1 {
 public r1 a=new r1(); public void q(int start,int count){a.insert(start,count);}
}''',
    'ra/r1.java': '''package ra;
public final class r1 {
 public rb0.h owner;
 public int size,full,changes,inserts,changeStart=-1,changeCount,insertStart=-1,insertCount;
 public boolean changeWithAnimator,insertWithAnimator,resetWithAnimator;
 public Object payload;
 public void d(int start,int count,Object payload){
  if(start<0||count<=0||start+count>size)throw new AssertionError("Invalid old-row update");
  this.changes++;this.changeStart=start;this.changeCount=count;this.payload=payload;
  changeWithAnimator=owner!=null&&owner.l.b.animator!=null;
  if(owner!=null){owner.l.b.queueAdapterLayout("notifyChange");owner.rebind(start,count);}
 }
 public void insert(int start,int count){
  if(start!=size||count<=0)throw new AssertionError("Invalid append notification");
  this.inserts++;this.insertStart=start;this.insertCount=count;size+=count;
  insertWithAnimator=owner!=null&&owner.l.b.animator!=null;
  if(owner!=null)owner.l.b.queueAdapterLayout("notifyInsert");
 }
 public void reset(int count){full++;size=count;resetWithAnimator=owner!=null&&owner.l.b.animator!=null;
  if(owner!=null){owner.l.b.queueAdapterLayout("notifyFull");owner.rebind(0,count);}}
 public void clear(){full=changes=inserts=changeCount=insertCount=0;changeStart=insertStart=-1;payload=null;}
}''',
    'ce0/a.java': '''package ce0; public abstract class a {}''',
    'rb0/g.java': '''package rb0; public final class g extends ce0.a {
 public java.util.ArrayList e; public g(java.util.ArrayList values){e=values;}
}''',
    'fb0/v.java': '''package fb0; public interface v { int d(); }''',
    'fb0/s.java': '''package fb0; public interface s { void d(boolean reset); }''',
    'fb0/u.java': '''package fb0; public final class u {
 public v d;public int dispatches;public Runnable before,after;public boolean failBefore,failAfter;
 public final java.util.List<s> listeners=new java.util.ArrayList<>();
 public void h(boolean reset){dispatches++;if(failBefore)throw new IllegalStateException("native dispatch before listeners");
  if(before!=null)before.run();for(s listener:new java.util.ArrayList<s>(listeners))listener.d(reset);
  if(after!=null)after.run();if(failAfter)throw new IllegalStateException("native dispatch after listeners");}
}''',
    'rb0/j.java': '''package rb0; public final class j implements fb0.v {
 public java.util.List b=new java.util.ArrayList();
 public fb0.u d;
 public final java.util.ArrayList<Integer> widths=new java.util.ArrayList<>();
 public final java.util.ArrayList<Object> keys=new java.util.ArrayList<>();
 public void add(int... values){for(int v:values){widths.add(v);keys.add(new Object());}}
 public int d(){return widths.size();}
 public void load(java.util.List values){b=new java.util.ArrayList(values);widths.clear();keys.clear();
  for(Object value:values){widths.add(2);keys.add(value);}}
 public void a(ib0.a incoming){
  incoming=hiro.swiftkey.CandidateScroll.stabilize(this,incoming);
  java.util.List fresh=new java.util.ArrayList(incoming.a);boolean prefix=fresh.size()>=b.size();
  for(int i=0;prefix&&i<b.size();i++)prefix=java.util.Objects.equals(
   ((m40.b)b.get(i)).getCorrectionSpanReplacementText(),((m40.b)fresh.get(i)).getCorrectionSpanReplacementText());
  if(!b.isEmpty()&&fresh.size()==b.size()&&prefix){testutil.NativeBridge.finish(this);return;}
  boolean reset=!(incoming.b==ib0.m.g&&!b.isEmpty()&&prefix);
  load(fresh);if(d!=null)testutil.NativeBridge.dispatch(this,d,reset);testutil.NativeBridge.finish(this);
 }
}''',
    'rb0/k.java': '''package rb0; public final class k implements fb0.s {
 public com.touchtype.ui.AccessibilityEmptyRecyclerView l;public rb0.h m;public rb0.l n;public int resets;
 public k(com.touchtype.ui.AccessibilityEmptyRecyclerView view,rb0.h adapter){l=view;m=adapter;n=new rb0.l(adapter.l);}
 public void d(boolean reset){if(reset)a();}
 public void a(){resets++;m.n=true;m.o=false;m.p=m.q=0;m.H();n.c=0;testutil.NativeBridge.panel(this,l,0);}
}''',
    'rb0/l.java': '''package rb0; public final class l {
 public androidx.recyclerview.widget.GridLayoutManager b;public int c,fetches;
 public l(androidx.recyclerview.widget.GridLayoutManager layout){b=layout;}
 public void b(androidx.recyclerview.widget.RecyclerView view,int dx,int dy){
  testutil.NativeBridge.watermark(this,view,dx,dy);int last=b.W0();
  if(last>c){fetches++;c=last;}
 }
}''',
    'testutil/NativeBridge.java': '''package testutil;
public final class NativeBridge {
 // Missing 1.8.18 hooks use the exact former native calls, so the same host
 // regression tests can execute against the previous production helper.
 private static boolean call(String name,Class<?>[] types,Object... args){
  try{hiro.swiftkey.CandidateScroll.class.getMethod(name,types).invoke(null,args);return true;}
  catch(NoSuchMethodException absent){return false;}
  catch(java.lang.reflect.InvocationTargetException wrapped){Throwable cause=wrapped.getCause();
   if(cause instanceof RuntimeException)throw(RuntimeException)cause;if(cause instanceof Error)throw(Error)cause;
   throw new AssertionError(cause);}
  catch(ReflectiveOperationException failure){throw new AssertionError(failure);}
 }
 public static void dispatch(rb0.j provider,fb0.u notifier,boolean reset){
  if(!call("dispatchUpdate",new Class<?>[]{rb0.j.class,fb0.u.class,boolean.class},provider,notifier,reset))notifier.h(reset);}
 public static void panel(rb0.k panel,androidx.recyclerview.widget.RecyclerView view,int position){
  if(!call("scrollPanelToStart",new Class<?>[]{rb0.k.class,androidx.recyclerview.widget.RecyclerView.class,int.class},panel,view,position))view.w0(position);}
 public static void finish(rb0.j provider){call("finishUpdate",new Class<?>[]{rb0.j.class},provider);}
 public static void watermark(rb0.l listener,androidx.recyclerview.widget.RecyclerView view,int dx,int dy){
  call("rebaseScrollWatermark",new Class<?>[]{rb0.l.class,androidx.recyclerview.widget.RecyclerView.class,int.class,int.class},listener,view,dx,dy);}
}''',
    'ib0/a.java': '''package ib0; public final class a {
 public java.util.List a; public ib0.m b; public l40.c c; public lh0.a d;
 public a(lh0.a breadcrumb,java.util.List candidates,ib0.m type,l40.c context){
  a=candidates;b=type;c=context;d=breadcrumb;
 }
}''',
    'ib0/m.java': '''package ib0; public enum m { g, other }''',
    'lh0/a.java': '''package lh0; public class a {}''',
    'l40/c.java': '''package l40; public final class c {
 public com.microsoft.fluency.ResultsFilter.PredictionSearchType d;
 public Object a,b,c,e,h,p;public Object[] j;public int g;
 public boolean f,n,o; public String k,l,m,i,q;
}''',
    'm40/b.java': '''package m40; public interface b {
 String getUserFacingText();String getCorrectionSpanReplacementText();String getPredictionInput();
 String getTrailingSeparator();l40.c subrequest();
}''',
    'com/microsoft/fluency/ResultsFilter.java': '''package com.microsoft.fluency;
public final class ResultsFilter { public enum PredictionSearchType { JAPANESE, OTHER } }''',
    'dd0/e0.java': '''package dd0; public class e0 {}''',
    'gd0/b.java': '''package gd0; public interface b { dd0.e0 b(); }''',
    'ah0/b0.java': '''package ah0; public final class b0 {
 public int height=80; public Object get(){return Integer.valueOf(height);}
}''',
    'rb0/h.java': '''package rb0;
public final class h extends ra.q1 implements fb0.s {
 public gd0.b e;public fb0.u g=new fb0.u();public ah0.b0 h=new ah0.b0();
 public androidx.recyclerview.widget.GridLayoutManager l=new androidx.recyclerview.widget.GridLayoutManager();
 public boolean o=true,r=true,n=true;public int p,q;public boolean failG;public int[] forcedSpans;public int fullCalls;
 public boolean measureWithAnimator;
 public final java.util.ArrayList<Object> bound=new java.util.ArrayList<>();
 public h(){a.owner=this;}
 public void rebind(int start,int count){
  rb0.j provider=(rb0.j)g.d;
  for(int i=start;i<start+count;i++){
   while(bound.size()<=i)bound.add(null);
   if(n||bound.get(i)==null)bound.set(i,provider.keys.get(i));
  }
 }
 public int j(){return r?g.d.d():0;}
 public void G(){
  measureWithAnimator=l.b!=null&&l.b.animator!=null;
  if(l.b!=null)l.b.events.add("measure");
  if(failG)throw new IllegalStateException("native width calculation failed");
  java.util.ArrayList<Integer> spans=new java.util.ArrayList<>();
  if(forcedSpans!=null){for(int s:forcedSpans)spans.add(s);l.L=new rb0.g(spans);return;}
  rb0.j provider=(rb0.j)g.d;int columns=l.G;int used=0;
  for(int size:provider.widths){
   int width=Math.min(columns,Math.max(1,size));
   if(used+width>columns){int last=spans.size()-1;spans.set(last,spans.get(last)+columns-used);used=0;}
   spans.add(width);used+=width;if(used==columns)used=0;
  }
  if(used!=0){int last=spans.size()-1;spans.set(last,spans.get(last)+columns-used);}
  l.L=new rb0.g(spans);
 }
 public void H(){fullCalls++;G();if(!o){o=true;p=0;q=0;}a.reset(j());hiro.swiftkey.CandidateScroll.snapshot(this);}
 public void d(boolean reset){
  // CandidateScrollPatch's leading if-nez bypasses the helper for true/reset.
  if(!reset&&hiro.swiftkey.CandidateScroll.tryAppend(this,reset))return;
  if(reset){n=true;o=false;p=q=0;}H();
 }
}''',
    'CandidateScrollHostTest.java': r'''import hiro.swiftkey.CandidateScroll;
import java.lang.reflect.Field;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Random;
import java.util.WeakHashMap;

public final class CandidateScrollHostTest {
 static int assertions,scenarios;
 static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
 static final class Theme implements gd0.b {dd0.e0 value=new dd0.e0();public dd0.e0 b(){return value;}}
 static final class Grid {
  final rb0.h adapter=new rb0.h();final rb0.j provider=new rb0.j();final Theme theme=new Theme();
  final androidx.recyclerview.widget.RecyclerView view=new androidx.recyclerview.widget.RecyclerView();
  Grid(int... widths){adapter.g.d=provider;adapter.e=theme;provider.add(widths);view.adapter=adapter;
   view.layout=adapter.l;adapter.l.b=view;adapter.H();view.runDeferredLayout();adapter.a.clear();adapter.fullCalls=0;}
  void append(int... values){provider.add(values);}
  void update(){adapter.d(false);}
 }
 static int[] spans(Grid g){ArrayList<?> raw=((rb0.g)g.adapter.l.L).e;int[] r=new int[raw.size()];
  for(int i=0;i<r.length;i++)r[i]=(Integer)raw.get(i);return r;}
 static void array(int[] actual,int... expected){check(java.util.Arrays.equals(actual,expected),
  "Span layout differs: "+java.util.Arrays.toString(actual));}
 static void fallback(Grid g,String message){check(!CandidateScroll.tryAppend(g.adapter,false),message);
  check(g.adapter.a.changes==0&&g.adapter.a.inserts==0,"Rejected append emitted notifications");}
 static void scrollingUnchanged(Grid g){
  check(g.view.scrollPosition==7&&g.view.scrollOffset==431&&g.view.scrollState==2,
        "Item animation policy changed native scroll position or fling state");
 }
 static void animationPolicy()throws Exception{
  // This test observes real production calls. The fake only supplies the native
  // setter's documented end-animations contract; it does not render Android UI.
  Grid g=new Grid(4,4);scenarios++;
  check(g.view.animator==null&&g.view.animatorSets==1,
        "Initial candidate snapshot left item animations enabled");
  check(g.adapter.measureWithAnimator&&g.adapter.a.resetWithAnimator,
        "Initial native measurement/notification was incorrectly modeled after snapshot");
  check(g.view.events.indexOf("notifyFull")<g.view.events.indexOf("animator:null")
        &&g.view.events.indexOf("animator:null")<g.view.events.indexOf("layout")
        &&g.view.layoutFrames==1&&!g.view.layoutWithAnimator,
        "Initial snapshot did not suppress item animation before deferred layout");
  int setters=g.view.animatorSets;CandidateScroll.snapshot(g.adapter);
  check(g.view.animatorSets==setters,"Already-disabled candidate view was needlessly reset");
  scrollingUnchanged(g);

  // A late native animator replacement can still be active during the next
  // append. It must end before measurement and either range notification.
  ra.x1 active=new ra.x1();g.view.animator=active;g.adapter.p=1;g.adapter.q=6;
  Object freshActor=new Object();g.provider.keys.set(0,freshActor);
  g.append(4);g.update();scenarios++;
  check(active.endCalls==1&&!active.running&&g.view.animator==null,
        "Running item animations were not ended before append");
  check(!g.adapter.measureWithAnimator&&!g.adapter.a.changeWithAnimator&&!g.adapter.a.insertWithAnimator,
        "New width or position update was emitted while item animation remained enabled");
  check(g.adapter.a.full==0&&g.adapter.a.changes==1&&g.adapter.a.inserts==1,
        "Animation suppression changed incremental notification behavior");
  check(g.adapter.bound.get(0)==freshActor&&g.adapter.p==1&&g.adapter.q==6,
        "Animation suppression changed latest commit actor or shortcut navigation");
  scrollingUnchanged(g);

  // The native reset hook bypasses tryAppend. Original G()+notify run first;
  // H's snapshot then ends animations before the deferred layout can use them.
  g=new Grid(4,4);g.view.events.clear();active=new ra.x1();g.view.animator=active;
  g.append(4);g.adapter.d(true);scenarios++;
  check(active.endCalls==1&&g.view.animator==null&&g.adapter.measureWithAnimator&&g.adapter.a.resetWithAnimator,
        "Native reset did not bypass tryAppend and disable via post-notify snapshot");
  check(g.view.layoutPending&&g.view.events.indexOf("notifyFull")<g.view.events.indexOf("endAnimations")
        &&g.view.events.indexOf("endAnimations")<g.view.events.indexOf("animator:null")
        &&!g.view.events.contains("layout"),
        "Reset snapshot ran outside the native notify-to-layout boundary");
  g.view.runDeferredLayout();
  check(!g.view.layoutWithAnimator&&g.view.events.indexOf("animator:null")<g.view.events.indexOf("layout"),
        "Reset reached deferred layout with item animation still enabled");
  check(g.adapter.a.full==1&&g.adapter.a.inserts==0,"Reset no longer uses native full refresh");
  scrollingUnchanged(g);
  // Append fallback does enter tryAppend, so suppression precedes native H.
  g=new Grid(2,2,4,2);active=new ra.x1();g.view.animator=active;
  g.provider.widths.set(0,4);g.append(2);g.update();scenarios++;
  check(active.endCalls==1&&g.adapter.a.full==1&&!g.adapter.a.resetWithAnimator,
        "Earlier-row reflow fallback retained item animations");
  scrollingUnchanged(g);
  g=new Grid(4,4);active=new ra.x1();g.view.animator=active;g.view.width++;
  g.append(4);g.update();scenarios++;
  check(active.endCalls==1&&g.adapter.a.full==1&&!g.adapter.a.resetWithAnimator,
        "First-size-change fallback retained item animations");
  scrollingUnchanged(g);

  g=new Grid(4,4);Field cache=CandidateScroll.class.getDeclaredField("snapshots");cache.setAccessible(true);
  ((WeakHashMap<?,?>)cache.get(null)).remove(g.adapter);
  active=new ra.x1();g.view.animator=active;g.append(4);g.update();scenarios++;
  check(active.endCalls==1&&g.adapter.a.full==1&&!g.adapter.a.resetWithAnimator,
        "Missing-snapshot fallback retained item animations");
  scrollingUnchanged(g);

  // snapshot is also reached after H()/onAttachedToWindow's original notify.
  // It must apply even before a usable geometry snapshot can be recorded.
  g=new Grid();scenarios++;
  check(g.view.animator==null,"Empty initial candidate view retained item animations");
  active=new ra.x1();g.view.animator=active;g.adapter.r=false;CandidateScroll.snapshot(g.adapter);
  check(active.endCalls==1&&g.view.animator==null,"Inactive attach snapshot skipped animation suppression");
  g=new Grid(4,4);g.view.shown=false;g.view.attached=false;active=new ra.x1();g.view.animator=active;
  g.append(4);fallback(g,"Hidden/detached append was accepted");scenarios++;
  check(active.endCalls==1&&g.view.animator==null,"Hidden candidate fallback retained animations for reopening");

  // Do not touch another RecyclerView, or an old candidate view that has been
  // rebound to a different adapter/layout. No process-wide animation setting.
  g=new Grid(4,4);androidx.recyclerview.widget.RecyclerView other=new androidx.recyclerview.widget.RecyclerView();
  ra.x1 otherAnimator=other.animator;active=new ra.x1();g.view.animator=active;
  g.view.adapter=new ra.q1(){};g.append(4);fallback(g,"Replaced adapter accepted");scenarios++;
  check(g.view.animator==active&&active.endCalls==0&&other.animator==otherAnimator&&otherAnimator.endCalls==0,
        "Animator of an unrelated/rebound view was modified");
  g=new Grid(4,4);active=new ra.x1();g.view.animator=active;g.view.layout=new androidx.recyclerview.widget.GridLayoutManager();
  CandidateScroll.snapshot(g.adapter);scenarios++;
  check(g.view.animator==active&&active.endCalls==0,"Mismatched layout animator was modified");
  rb0.h unconnected=new rb0.h();CandidateScroll.snapshot(unconnected);
  check(!CandidateScroll.tryAppend(unconnected,false),"Unconnected candidate adapter accepted");scenarios++;

  // Native ABI/runtime failures remain non-fatal and preserve native refresh.
  g=new Grid(4,4);g.view.failAnimatorRead=true;g.append(4);
  fallback(g,"Unavailable native animator API did not fall back");CandidateScroll.snapshot(g.adapter);scenarios++;
  g=new Grid(4,4);g.view.animator=new ra.x1();g.view.failAnimatorWrite=true;g.append(4);
  fallback(g,"Throwing native animator setter did not fall back");CandidateScroll.snapshot(g.adapter);scenarios++;
 }
 static l40.c query(String input){l40.c c=new l40.c();c.k=input;c.l="before";c.m="after";
  c.i="ja";c.q="";c.d=com.microsoft.fluency.ResultsFilter.PredictionSearchType.JAPANESE;return c;}
 static final class C implements m40.b {
  final String text,commit,separator;final l40.c context;final int metadata;
  C(String text,l40.c context,int metadata){this(text,text,"",context,metadata);}
  C(String text,String commit,String separator,l40.c context,int metadata){
   this.text=text;this.commit=commit;this.separator=separator;this.context=context;this.metadata=metadata;}
  public String getUserFacingText(){return text;}public String getCorrectionSpanReplacementText(){return commit;}
  public String getPredictionInput(){return context.k;}public String getTrailingSeparator(){return separator;}
  public l40.c subrequest(){return context;}
 }
 static ArrayList<m40.b> candidates(l40.c context,int metadata,String... words){
  ArrayList<m40.b> out=new ArrayList<>();for(String word:words)out.add(new C(word,context,metadata));return out;}
 static boolean nativeAppend(java.util.List old,java.util.List next){
  if(next.size()<=old.size())return false;
  for(int i=0;i<old.size();i++)if(!((m40.b)old.get(i)).getCorrectionSpanReplacementText()
     .equals(((m40.b)next.get(i)).getCorrectionSpanReplacementText()))return false;
  return true;
 }
 static void stabilization(){
  // Exact 21-emoji あい entry from the published v1.8.13 emoji_ja.tsv. The
  // supplement inserts its first emoji early and leaves the other 20 at tail.
  String[] emoji={"❤️‍🔥","❤️","🩷","💛","🩵","💜","🩶","🫰","🫶","\ud83e\ude89","ℹ️",
   "🫰🏻","🫰🏼","🫰🏽","🫰🏾","🫰🏿","🫶🏻","🫶🏼","🫶🏽","🫶🏾","🫶🏿"};
  String[] words={"会","あ",emoji[0],"eye","愛依","安威","會","阿井","愛良","吾唯","安居","aye"};
  l40.c oldContext=query("あい"),nextContext=query("あい");oldContext.g=12;nextContext.g=24;
  ArrayList<m40.b> old=candidates(oldContext,1,words),fresh=candidates(nextContext,2,words);
  for(int i=1;i<emoji.length;i++)old.add(new C(emoji[i],oldContext,1));
  fresh.add(new C("空い",nextContext,2));fresh.add(new C("逢い",nextContext,2));
  for(int i=1;i<emoji.length;i++)fresh.add(new C(emoji[i],nextContext,2));
  rb0.j provider=new rb0.j();provider.b=old;lh0.a breadcrumb=new lh0.a();
  ib0.a incoming=new ib0.a(breadcrumb,fresh,ib0.m.g,nextContext);
  check(!nativeAppend(old,fresh),"Actual emoji tail reorder must reproduce native reset");
  ib0.a stable=CandidateScroll.stabilize(provider,incoming);scenarios++;
  check(stable!=incoming&&nativeAppend(old,stable.a),"Emoji-tail result did not become native append");
  check(stable.a.size()==fresh.size()&&fresh.size()==old.size()+2,"Stabilization lost candidates");
  check(stable.b==incoming.b&&stable.c==nextContext&&stable.d==breadcrumb,"Container metadata changed");
  java.util.IdentityHashMap<Object,Boolean> identities=new java.util.IdentityHashMap<>();
  for(Object value:stable.a){check(fresh.contains(value),"Reused stale native candidate");
   check(((C)value).metadata==2,"Commit/long-press metadata was stale");identities.put(value,true);}
  check(identities.size()==fresh.size(),"Fresh candidate repeated or dropped");
  check(((C)stable.a.get(old.size())).text.equals("空い")&&((C)stable.a.get(old.size()+1)).text.equals("逢い"),
       "New incoming order changed");
  for(int i=0;i<old.size();i++)check(((C)old.get(i)).text.equals(((C)stable.a.get(i)).text),"Displayed prefix moved");

  // Feed that normalized container through the same native stable-prefix rule
  // and production append updater, rather than testing the normalizer alone.
  int[] widths=new int[old.size()];java.util.Arrays.fill(widths,2);Grid grid=new Grid(widths);
  grid.provider.b=old;grid.provider.keys.clear();grid.provider.keys.addAll(old);grid.adapter.H();
  grid.adapter.a.clear();grid.adapter.fullCalls=0;grid.provider.b=stable.a;
  grid.provider.keys.clear();grid.provider.keys.addAll(stable.a);grid.provider.widths.add(2);grid.provider.widths.add(2);
  grid.adapter.d(!nativeAppend(old,stable.a));scenarios++;
  check(grid.adapter.a.full==0&&grid.adapter.a.inserts==1&&grid.adapter.a.insertCount==2,
       "Actual emoji dictionary route missed append optimization");
  for(int i=0;i<old.size();i++)check(grid.adapter.bound.get(i)==stable.a.get(i),"Existing grid key kept old metadata");

  // Duplicate labels retain multiplicity and FIFO choice of latest objects.
  provider=new rb0.j();provider.b=candidates(oldContext,1,"x","x","y");
  fresh=candidates(nextContext,2,"x","y","x","z");incoming=new ib0.a(breadcrumb,fresh,ib0.m.g,nextContext);
  stable=CandidateScroll.stabilize(provider,incoming);scenarios++;
  check(stable.a.get(0)==fresh.get(0)&&stable.a.get(1)==fresh.get(2)&&stable.a.get(2)==fresh.get(1)
     &&stable.a.get(3)==fresh.get(3),"Duplicate candidate queue lost identity/order");

  provider.b=candidates(oldContext,1,"x","y");fresh=candidates(nextContext,2,"x","y","z");
  incoming=new ib0.a(breadcrumb,fresh,ib0.m.g,nextContext);scenarios++;
  check(CandidateScroll.stabilize(provider,incoming)==incoming,"Ordinary stable append needlessly allocated a container");
  provider.b=candidates(oldContext,1,"x","y");fresh=candidates(nextContext,2,"y","x","z");
  incoming=new ib0.a(breadcrumb,fresh,ib0.m.other,nextContext);scenarios++;
  check(CandidateScroll.stabilize(provider,incoming)==incoming,"Initial/nonexpanded result reordered");
  incoming=new ib0.a(breadcrumb,fresh,ib0.m.g,query("あお"));scenarios++;
  check(CandidateScroll.stabilize(provider,incoming)==incoming,"New composing input kept old ordering");
  l40.c changed=query("あい");changed.l="different prefix";incoming=new ib0.a(breadcrumb,fresh,ib0.m.g,changed);scenarios++;
  check(CandidateScroll.stabilize(provider,incoming)==incoming,"Changed surrounding editor context reordered");
  changed=query("あい");changed.d=com.microsoft.fluency.ResultsFilter.PredictionSearchType.OTHER;
  incoming=new ib0.a(breadcrumb,fresh,ib0.m.g,changed);scenarios++;
  check(CandidateScroll.stabilize(provider,incoming)==incoming,"Other language/search type reordered");
  for(String field:new String[]{"a","b","c","e","h","p","j"}){
   changed=query("あい");
   try{l40.c.class.getField(field).set(changed,field.equals("j")?new Object[]{new Object()}:new Object());}
   catch(Exception error){throw new AssertionError(error);}
   incoming=new ib0.a(breadcrumb,fresh,ib0.m.g,changed);scenarios++;
   check(CandidateScroll.stabilize(provider,incoming)==incoming,"Changed native query field accepted: "+field);
  }
  fresh=candidates(nextContext,2,"x","z","w");incoming=new ib0.a(breadcrumb,fresh,ib0.m.g,nextContext);scenarios++;
  check(CandidateScroll.stabilize(provider,incoming)==incoming,"Removed prior candidate was resurrected");
  fresh=candidates(nextContext,2,"y");incoming=new ib0.a(breadcrumb,fresh,ib0.m.g,nextContext);scenarios++;
  check(CandidateScroll.stabilize(provider,incoming)==incoming,"Shrinking/deleting result reordered");
  fresh=candidates(nextContext,2,"y","x");incoming=new ib0.a(breadcrumb,fresh,ib0.m.g,nextContext);scenarios++;
  check(CandidateScroll.stabilize(provider,incoming)==incoming,"Same-count result treated as append");
  provider.b=candidates(oldContext,1,"x","x");fresh=candidates(nextContext,2,"x","y","z");
  incoming=new ib0.a(breadcrumb,fresh,ib0.m.g,nextContext);scenarios++;
  check(CandidateScroll.stabilize(provider,incoming)==incoming,"Missing duplicate occurrence silently dropped");
  provider.b=candidates(oldContext,1,"x","y");fresh=candidates(nextContext,2,"y","z");
  fresh.add(new C("x","different commit","",nextContext,2));incoming=new ib0.a(breadcrumb,fresh,ib0.m.g,nextContext);scenarios++;
  check(CandidateScroll.stabilize(provider,incoming)==incoming,"Different commit text matched only visible label");
  fresh=candidates(nextContext,2,"y","z");fresh.add(new C("x","x"," ",nextContext,2));
  incoming=new ib0.a(breadcrumb,fresh,ib0.m.g,nextContext);scenarios++;
  check(CandidateScroll.stabilize(provider,incoming)==incoming,"Different trailing separator silently matched");
  check(CandidateScroll.stabilize(null,incoming)==incoming&&CandidateScroll.stabilize(provider,null)==null,
       "Null stabilization arguments changed native behavior");scenarios++;
 }
 public static void main(String[] args)throws Exception {
  animationPolicy();
  stabilization();
  Grid g=new Grid(2,2);g.adapter.p=1;g.adapter.q=7;
  Object first=g.provider.keys.get(0),second=g.provider.keys.get(1);
  array(spans(g),2,6);g.append(2);g.update();scenarios++;
  array(spans(g),2,2,4);check(g.adapter.fullCalls==0&&g.adapter.a.full==0,"Append used full refresh");
  check(g.adapter.a.changes==1&&g.adapter.a.changeStart==0&&g.adapter.a.changeCount==2,"Old metadata range not rebound");
  check(g.adapter.a.payload!=null,"Changed row must reuse holders using a payload");
  check(g.adapter.a.insertStart==2&&g.adapter.a.insertCount==1&&g.adapter.a.size==3,"Append notification incorrect");
  check(g.provider.keys.get(0)==first&&g.provider.keys.get(1)==second,"Candidate selection mapping changed");
  check(g.adapter.p==1&&g.adapter.q==7,"Shortcut navigation range changed");

  g=new Grid(4,4);Object oldActor=g.adapter.bound.get(0),newActor=new Object();
  g.provider.keys.set(0,newActor);g.append(4);g.update();scenarios++;
  check(g.adapter.a.full==0&&g.adapter.bound.get(0)==newActor&&g.adapter.bound.get(0)!=oldActor,
       "Recreated native commit/long-press metadata was left stale");
  g=new Grid(4,4);oldActor=g.adapter.bound.get(0);g.adapter.n=false;
  g.provider.keys.set(0,new Object());g.append(4);g.update();scenarios++;
  check(!g.adapter.n&&g.adapter.bound.get(0)==oldActor,"Native binding condition was overridden");

  g=new Grid(4,4);g.append(4,4);g.update();scenarios++;
  array(spans(g),4,4,4,4);check(g.adapter.a.changes==1&&g.adapter.a.changeCount==2&&g.adapter.a.inserts==1,"Old native metadata must rebind even with unchanged widths");
  g.adapter.a.clear();g.append(1);g.update();scenarios++;
  check(g.adapter.a.changes==1&&g.adapter.a.changeCount==4&&g.adapter.a.insertStart==4,"Second append snapshot stale");
  g.adapter.a.clear();g.append(2);g.update();scenarios++;
  check(g.adapter.a.changeStart==0&&g.adapter.a.changeCount==5&&g.adapter.a.insertStart==5,"All old metadata must refresh without a full invalidation");

  g=new Grid(2,2);g.append(2);g.adapter.d(true);scenarios++;
  check(g.adapter.fullCalls==1&&g.adapter.a.full==1&&g.adapter.a.inserts==0,"Reset must retain native full refresh");
  g.adapter.a.clear();g.append(1);g.update();check(g.adapter.a.inserts==1,"Reset did not rebaseline append state");

  g=new Grid(2,2,4,2);g.provider.widths.set(0,4);g.append(2);g.update();scenarios++;
  check(g.adapter.fullCalls==1&&g.adapter.a.full==1&&g.adapter.a.inserts==0,"Changed earlier row must fall back");
  array(spans(g),4,4,4,2,2);

  g=new Grid(2,2);g.append(2);g.view.shown=false;fallback(g,"Hidden view accepted");scenarios++;
  g=new Grid(2,2);g.append(2);g.view.attached=false;fallback(g,"Detached view accepted");scenarios++;
  g=new Grid(2,2);g.append(2);g.view.computing=true;fallback(g,"Layout recursion accepted");scenarios++;
  g=new Grid(2,2);g.append(2);g.view.adapter=null;fallback(g,"Replaced adapter accepted");scenarios++;
  g=new Grid(2,2);g.append(2);g.view.layout=null;fallback(g,"Replaced layout accepted");scenarios++;
  g=new Grid(2,2);g.append(2);g.view.width++;fallback(g,"Changed width accepted");scenarios++;
  g=new Grid(2,2);g.append(2);g.view.height++;fallback(g,"Changed height accepted");scenarios++;
  g=new Grid(2,2);g.append(2);g.adapter.l.G++;fallback(g,"Changed columns accepted");scenarios++;
  g=new Grid(2,2);g.append(2);g.adapter.h.height++;fallback(g,"Changed key size accepted");scenarios++;
  g=new Grid(2,2);g.append(2);g.theme.value=new dd0.e0();fallback(g,"Changed theme accepted");scenarios++;
  g=new Grid(2,2);g.append(2);g.adapter.o=false;fallback(g,"Uninitialized navigation accepted");scenarios++;
  g=new Grid(2,2);g.append(2);g.adapter.r=false;fallback(g,"Inactive adapter accepted");scenarios++;
  g=new Grid(2,2);g.append(2);g.adapter.G();fallback(g,"Changed lookup accepted");scenarios++;
  g=new Grid(2,2);g.append(2);rb0.j replacement=new rb0.j();replacement.add(2,2,2);
  g.adapter.g.d=replacement;fallback(g,"Replaced provider accepted");scenarios++;
  g=new Grid(2,2);g.append(2);g.adapter.g.d=new fb0.v(){public int d(){return 3;}};
  fallback(g,"Unsupported provider accepted");scenarios++;
  g=new Grid(2,2);fallback(g,"Same-count notification accepted as insert");scenarios++;
  g=new Grid(2,2);g.provider.widths.remove(1);fallback(g,"Shrink accepted as insert");scenarios++;
  g=new Grid(2,2);g.append(2);g.adapter.forcedSpans=new int[]{2,2,2};
  fallback(g,"Unpadded native span list accepted");scenarios++;
  g=new Grid(2,2);g.append(2);g.adapter.failG=true;fallback(g,"Throwing native measurement did not fail safe");scenarios++;

  // Derive expected notification boundaries from the native row model across
  // different widths, not from helper internals. Contents/indices never move.
  Random random=new Random(1814);
  for(int run=0;run<200;run++){
   int[] widths=new int[1+random.nextInt(25)];for(int i=0;i<widths.length;i++)widths[i]=1+random.nextInt(12);
   g=new Grid(widths);Object[] keys=g.provider.keys.toArray();int[] before=spans(g);int oldCount=before.length;
   int[] extra=new int[1+random.nextInt(8)];for(int i=0;i<extra.length;i++)extra[i]=1+random.nextInt(12);
   g.append(extra);g.update();scenarios++;
   check(g.adapter.a.full==0&&g.adapter.a.inserts==1,"Native stable-prefix append rejected, sample "+run);
   check(g.adapter.a.insertStart==oldCount&&g.adapter.a.insertCount==extra.length,"Wrong suffix, sample "+run);
   check(g.adapter.a.size==g.provider.d(),"Observer count differs from provider");
   for(int i=0;i<oldCount;i++)check(g.provider.keys.get(i)==keys[i],"Selection identity moved");
   int[] after=spans(g);int used=0,rowStart=0;
   for(int i=0;i<oldCount;i++){if(used==0)rowStart=i;used+=before[i];if(used==8)used=0;}
   for(int i=0;i<rowStart;i++)check(before[i]==after[i],"A completed old row changed");
   check(g.adapter.a.changes==1&&g.adapter.a.changeStart==0&&g.adapter.a.changeCount==oldCount&&g.adapter.a.payload!=null,
      "Old keys were not rebound through a reusable payload");
  }

  // Snapshot cannot keep a keyboard or its candidate contents alive via the
  // WeakHashMap value; only weak references and primitive geometry are stored.
  for(Field field:Class.forName("hiro.swiftkey.CandidateScroll$Snapshot").getDeclaredFields()){
   Class<?> t=field.getType();check(t.isPrimitive()||t==int[].class||t==WeakReference.class,
       "Snapshot retains a native/input object strongly: "+field.getName());
  }scenarios++;
  CandidateScroll.snapshot(null);check(!CandidateScroll.tryAppend(null,false),"Null adapter accepted");scenarios++;
  System.out.println("PASS CandidateScroll "+scenarios+" scenarios, "+assertions+" assertions");
  System.out.println("REPORT {\"status\":\"PASSED\",\"scenarios\":"+scenarios+",\"assertions\":"+assertions+"}");
 }
}'''
}

SOURCES['CandidateViewportHostTest.java'] = r'''import hiro.swiftkey.CandidateScroll;
import testutil.NativeBridge;
import java.util.*;
import java.lang.reflect.Field;

public final class CandidateViewportHostTest {
 static int assertions,scenarios;
 static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
 static l40.c query(){return CandidateScrollHostTest.query("あい");}
 static ArrayList<m40.b> words(l40.c context,int version,String... words){
  return CandidateScrollHostTest.candidates(context,version,words);
 }
 static final class Theme implements gd0.b {dd0.e0 value=new dd0.e0();public dd0.e0 b(){return value;}}
 static final class Grid {
  final rb0.j provider=new rb0.j();final rb0.h adapter=new rb0.h();final Theme theme=new Theme();
  final com.touchtype.ui.AccessibilityEmptyRecyclerView view=new com.touchtype.ui.AccessibilityEmptyRecyclerView();
  final rb0.k panel=new rb0.k(view,adapter);final fb0.u notifier=adapter.g;final l40.c context=query();
  Grid(boolean panelFirst,String... initial){
   provider.d=notifier;notifier.d=provider;adapter.e=theme;provider.load(words(context,1,initial));
   view.adapter=adapter;view.layout=adapter.l;adapter.l.b=view;
   if(panelFirst){notifier.listeners.add(panel);notifier.listeners.add(adapter);}
   else{notifier.listeners.add(adapter);notifier.listeners.add(panel);}
   adapter.H();view.runDeferredLayout();adapter.a.clear();adapter.fullCalls=0;
   view.scrollState=2;view.stopCalls=0;adapter.l.anchorCalls=0;
  }
  void visible(int first,int count,int offset){
   adapter.l.children.clear();adapter.l.first=first;adapter.l.last=first+count-1;
   for(int i=first;i<first+count;i++){android.view.View child=new android.view.View();
    child.top=adapter.l.r.padding+offset+((i-first)/2)*80;adapter.l.children.put(i,child);}
   panel.n.c=adapter.l.last;
  }
  void update(l40.c next,int version,String... values){update(words(next,version,values),next,ib0.m.g);}
  void update(List<m40.b> values,l40.c next,ib0.m type){provider.a(new ib0.a(new lh0.a(),values,type,next));}
  void next(int version,String... values){update(query(),version,values);}
 }
 static void anchor(Grid g,int index,int offset,String why){
  check(g.adapter.l.x==index&&g.adapter.l.y==offset,why+": wrong pending candidate/pixel offset ("
    +g.adapter.l.x+","+g.adapter.l.y+")");
  check(g.view.stopCalls==0&&g.view.scrollState==2,why+": native reset stopped fling");
  check(g.view.animator==null,why+": item animations were restored");
 }
 static void nativeReset(Grid g,String why){
  check(g.view.stopCalls>0&&g.adapter.l.x==0&&g.adapter.l.y==Integer.MIN_VALUE,why+": native reset was suppressed");
 }
 static void latest(Grid g,int version){
  latestPrefix(g,version,g.provider.b.size());
 }
 static void latestPrefix(Grid g,int version,int reboundCount){
  check(g.adapter.n,"Native fresh-key binding flag was bypassed");
  for(int i=0;i<g.provider.b.size();i++){
   Object candidate=g.provider.b.get(i);
   if(i<reboundCount)check(g.adapter.bound.get(i)==candidate,
     "Candidate commit/long-press object was stale at "+i+" for version "+version);
   check(((CandidateScrollHostTest.C)candidate).metadata==version,"Fresh native metadata was not retained");
  }
 }
 static void scopeCleared(Grid g)throws Exception{
  Field field=CandidateScroll.class.getDeclaredField("activeUpdate");field.setAccessible(true);
  check(((ThreadLocal<?>)field.get(null)).get()==null,"Active dispatch escaped its finally block");
  field=CandidateScroll.class.getDeclaredField("preparedUpdates");field.setAccessible(true);
  check(!((Map<?,?>)field.get(null)).containsKey(g.provider),"Prepared update survived dispatch/normal return");
 }
 static Grid basic(boolean order){Grid g=new Grid(order,"A","B","C","D","E","F");g.visible(2,4,-17);return g;}

 public static void main(String[] args)throws Exception{
  // This mirrors the video's disappearing candidate set, and executes BOTH
  // valid native WeakHashMap listener orders. The older helper uses native
  // w0(0) through NativeBridge and fails the anchor/stop-fling assertions here.
  for(boolean panelFirst:new boolean[]{true,false}){
   Grid g=new Grid(panelFirst,"あ","愛","あい","会い","heart","eye","亜衣","亜依","愛衣","愛依",
    "安威","會","艾","璦","阿井","愛良","吾唯","安居","空い");
   g.visible(6,10,-17);
   g.next(2,"会","あ","上","愛","A","あい","会い","heart","eye","愛依","安威","會","阿井",
    "愛良","吾唯","安居","aye","空い","アイ");scenarios++;
   anchor(g,9,63,"Same-query result replacement");latest(g,2);scopeCleared(g);
   check(g.panel.resets==1&&g.adapter.a.full==2,"Native panel and adapter replacement updates were bypassed");
  }
  Grid g=basic(true);g.next(2,"C","A","B","E","D","F");scenarios++;
  anchor(g,0,-17,"Same-size reordered result");latest(g,2);
  g=basic(false);g.next(2,"A","D","F");scenarios++;
  anchor(g,1,-17,"Removed primary anchor uses next surviving visible candidate");latest(g,2);

  g=new Grid(true,"A","X","X","B","C");g.visible(2,2,-11);
  g.next(2,"X","A","B","X","C");scenarios++;
  anchor(g,3,-11,"Duplicate occurrences map in order");latest(g,2);
  g=basic(true);g.next(2,"A","B","Z");scenarios++;
  nativeReset(g,"No surviving visible candidate");latest(g,2);
  g=new Grid(true,"A","X","B");g.visible(1,1,-9);
  l40.c changed=query();ArrayList<m40.b> changedCommit=words(changed,2,"A","B");
  changedCommit.add(1,new CandidateScrollHostTest.C("X","different commit","",changed,2));
  g.update(changedCommit,changed,ib0.m.g);scenarios++;
  nativeReset(g,"Visible label alone cannot identify a commit");

  g=basic(true);Object oldList=g.provider.b;g.next(2,"A","B","C","D","E","F");scenarios++;
  check(g.provider.b==oldList&&g.notifier.dispatches==0,"Native identical-result early return changed");
  scopeCleared(g);g.panel.a();nativeReset(g,"Identical result left a reset suppression plan");
  g=basic(true);changed=CandidateScrollHostTest.query("うえ");g.update(changed,2,"C","D","A");scenarios++;
  nativeReset(g,"Changed composing query");
  g=basic(false);changed=query();changed.c=new Object();g.update(changed,2,"C","D","A");scenarios++;
  nativeReset(g,"Changed surrounding context with same spelling");
  g=basic(true);changed=query();g.update(words(changed,2,"C","D","A"),changed,ib0.m.other);scenarios++;
  nativeReset(g,"Non-expanded result type");
  g=basic(false);android.os.Looper.isMain=false;
  try{g.next(2,"C","D","A");}finally{android.os.Looper.isMain=true;}scenarios++;
  nativeReset(g,"Off-main-thread update");

  // Fresh candidate identity and all UI ownership/environment checks are
  // reevaluated inside the native callback, not only at preparation time.
  for(int kind=0;kind<6;kind++){
   final Grid current=basic((kind&1)==0);final int mutation=kind;
   current.notifier.before=new Runnable(){public void run(){
    if(mutation==0)current.view.shown=false;
    if(mutation==1)current.view.attached=false;
    if(mutation==2)current.view.width++;
    if(mutation==3)current.theme.value=new dd0.e0();
    if(mutation==4)current.view.adapter=new ra.q1(){};
    if(mutation==5)current.provider.load(words(query(),3,"C","D","A"));
   }};
   current.next(2,"C","D","A");scenarios++;
   nativeReset(current,"Changed callback environment "+kind);scopeCleared(current);
  }

  final Grid unrelated=basic(true),target=basic(false);
  target.notifier.before=new Runnable(){public void run(){unrelated.panel.a();}};
  target.next(2,"D","C","A");scenarios++;
  anchor(target,1,-17,"Scoped target panel");nativeReset(unrelated,"Unrelated panel during dispatch");
  g=basic(true);CandidateScroll.stabilize(g.provider,new ib0.a(new lh0.a(),words(query(),2,"D","C","A"),ib0.m.g,query()));
  g.panel.a();NativeBridge.finish(g.provider);scenarios++;
  nativeReset(g,"Prepared geometry alone cannot suppress a reset");scopeCleared(g);
  g=basic(true);g.next(2,"D","C","A");g.panel.a();scenarios++;
  nativeReset(g,"Completed dispatch cannot suppress a later theme/native reset");scopeCleared(g);

  for(boolean after:new boolean[]{false,true}){
   g=basic(after);g.notifier.failBefore=!after;g.notifier.failAfter=after;boolean thrown=false;
   try{g.next(2,"D","C","A");}catch(IllegalStateException expected){thrown=true;}scenarios++;
   check(thrown,"Native dispatch exception was swallowed");scopeCleared(g);
   g.panel.a();nativeReset(g,"Throwing dispatch leaked active reset suppression");
  }
  final Grid nested=basic(true);
  nested.notifier.before=new Runnable(){public void run(){nested.notifier.before=null;
   nested.update(CandidateScrollHostTest.query("new input"),3,"Z","A","D");}};
  nested.next(2,"D","C","A");scenarios++;
  nativeReset(nested,"Nested query invalidates outer epoch");latest(nested,3);scopeCleared(nested);
  final Grid inner=basic(true),outer=basic(false);
  outer.notifier.before=new Runnable(){public void run(){inner.next(2,"D","C","A");}};
  outer.next(2,"E","C","A","D");scenarios++;
  anchor(inner,1,-17,"Nested independent provider");anchor(outer,1,-17,"Outer scope restored after nested provider");
  scopeCleared(inner);scopeCleared(outer);

  // Multiple results can arrive before RecyclerView lays out any new holders.
  // Keep logical visible candidates, never index the newest list with those
  // still-old child positions. The first preserved candidate then disappears.
  g=basic(true);g.next(2,"Z","C","A","E","D","F");anchor(g,1,-17,"First pending replacement");
  g.next(3,"A","F","Z","E","D");scenarios++;
  anchor(g,4,-17,"Second pending replacement after primary removal");latest(g,3);
  g.next(4,"A","F","Z","E","D","N");scenarios++;
  anchor(g,4,-17,"Ordinary append rebases pending list identity");
  latestPrefix(g,4,5); // Newly inserted row has not reached the next layout yet.
  g.next(5,"N","D","E","A");scenarios++;
  anchor(g,1,-17,"Replacement after pending append");latest(g,5);scopeCleared(g);

  // Once the layout has completed, capture the actual viewport again.
  g.view.runDeferredLayout();g.visible(2,2,-33);g.next(6,"A","E","D");scenarios++;
  anchor(g,1,-33,"Completed layout discarded old logical viewport");latest(g,6);
  g=basic(true);g.adapter.l.l1(5,12);g.next(2,"D","C","A");scenarios++;
  nativeReset(g,"Unrelated native pending scroll is not borrowed");
  final Grid explicit=basic(false);explicit.notifier.before=new Runnable(){public void run(){explicit.adapter.l.l1(2,55);}};
  explicit.next(2,"D","C","A");scenarios++;
  nativeReset(explicit,"New native pending scroll during dispatch is not overwritten");

  // The native scroll listener ignores dx/dy and would request more data after
  // a reset-to-zero watermark. Rebase ONLY the restored layout's first 0/0
  // callback, using its real final visible range rather than the old range.
  g=basic(true);g.next(2,"D","C","A","E","F","Z","N");
  NativeBridge.watermark(g.panel.n,g.view,0,0);
  check(g.panel.n.c==0,"Watermark rebased before pending layout completed");
  g.view.runDeferredLayout();g.adapter.l.first=1;g.adapter.l.last=5;
  rb0.l foreign=new rb0.l(g.adapter.l);NativeBridge.watermark(foreign,g.view,0,0);
  check(foreign.c==0,"A different listener consumed the panel token");
  g.panel.n.b(g.view,0,0);scenarios++;
  check(g.panel.n.c==5&&g.panel.n.fetches==0,"Restored layout triggered another candidate fetch");
  g.panel.n.b(g.view,0,0);check(g.panel.n.fetches==0,"Unchanged range refetched after consuming token");
  g.adapter.l.last=6;g.panel.n.b(g.view,0,12);
  check(g.panel.n.fetches==1&&g.panel.n.c==6,"Real row advance no longer requested more candidates");

  g=basic(false);g.next(2,"D","C","A","E","F","Z");
  g.next(3,"D","C","A","E","F","Z"); // native no-op before pending frame
  g.view.runDeferredLayout();g.adapter.l.last=4;g.panel.n.b(g.view,0,0);scenarios++;
  check(g.panel.n.fetches==0&&g.panel.n.c==4,"Identical intermediate result invalidated layout watermark");
  g=basic(true);g.next(2,"D","C","A","E","F","Z");
  g.view.runDeferredLayout();g.adapter.l.last=4;g.panel.n.b(g.view,0,8);scenarios++;
  check(g.panel.n.fetches==1&&g.panel.n.c==4,"First nonzero motion was suppressed by layout token");
  g.panel.n.c=0;g.panel.n.b(g.view,0,0);
  check(g.panel.n.fetches==2,"Consumed nonzero-motion token affected a later unrelated reset");
  g=basic(true);g.next(2,"D","C","A","E","F","Z");
  g.view.runDeferredLayout();g.provider.load(words(query(),9,"D","C","A","E","F","Z"));
  g.adapter.l.last=4;g.panel.n.b(g.view,0,0);scenarios++;
  check(g.panel.n.fetches==1,"Stale fresh-list identity suppressed native fetching");

  // Long-lived geometry contains weak references/primitive arrays only; no
  // saved spelling, candidate list, candidate actor or strong UI ownership.
  for(String name:new String[]{"Viewport","UpdatePlan"}){
   for(Field field:Class.forName("hiro.swiftkey.CandidateScroll$"+name).getDeclaredFields()){
    Class<?> type=field.getType();check(type!=String.class&&!List.class.isAssignableFrom(type)
      &&!m40.b.class.isAssignableFrom(type)&&!android.view.View.class.isAssignableFrom(type),
      "Persistent viewport field retains input/UI strongly: "+field.getName());
   }
  }scenarios++;
  System.out.println("PASS CandidateViewport "+scenarios+" scenarios, "+assertions+" assertions");
  System.out.println("REPORT {\"status\":\"PASSED\",\"scenarios\":"+scenarios+",\"assertions\":"+assertions+"}");
 }
}'''


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--source-dir', type=Path, required=True)
    parser.add_argument('--work', type=Path, required=True)
    args = parser.parse_args()
    root, work = args.source_dir.resolve(), args.work.resolve()
    production = root / 'src/hiro/swiftkey/CandidateScroll.java'
    work.mkdir(parents=True, exist_ok=True)
    classes, source = work / 'classes', work / 'generated'
    for directory in (classes, source):
        if directory.exists():
            shutil.rmtree(directory)
        directory.mkdir()
    for name, content in SOURCES.items():
        path = source / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content + '\n')
    commands = [
        ('compile', ['java', 'com.sun.tools.javac.Main', '--release', '8', '-encoding', 'UTF-8',
                     '-g:none', '-d', str(classes), str(production),
                     *map(str, sorted(source.rglob('*.java')))]),
        ('candidate', ['java', '-ea', '-cp', str(classes), 'CandidateScrollHostTest']),
        ('viewport', ['java', '-ea', '-cp', str(classes), 'CandidateViewportHostTest']),
    ]
    result_text = ''
    reports = []
    for name, command in commands:
        result = subprocess.run(command, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        (work / (name + '.log')).write_text(result.stdout)
        print(result.stdout, end='', flush=True)
        if result.returncode:
            raise SystemExit(result.returncode)
        result_text = result.stdout
        reports.extend(json.loads(line[7:]) for line in result_text.splitlines() if line.startswith('REPORT '))
    report = dict(status='PASSED', scenarios=sum(item['scenarios'] for item in reports),
                  assertions=sum(item['assertions'] for item in reports))
    report.update(production_source_sha256=hashlib.sha256(production.read_bytes()).hexdigest(),
                  device_tested=False, android_instrumentation_tested=False,
                  scope='Production CandidateScroll with explicit host native-grid, adapter-observer and deferred-layout fakes; reset hook bypass matches native loader, native G row packing replayed, no ART/device/FPS measurement',
                  full_width_measurement_preserved=True, native_candidate_mapping_preserved=True,
                  reset_full_refresh_preserved=True, snapshot_contains_no_input_text=True,
                  candidate_only_item_animations_disabled=True,
                  active_item_animations_ended_before_append=True,
                  reset_and_geometry_fallback_animation_policy_verified=True,
                  native_reset_hook_bypasses_append_helper=True,
                  reset_post_notify_snapshot_precedes_deferred_layout=True,
                  candidate_same_query_scroll_position_preserved=True,
                  candidate_reset_does_not_stop_scroll=True,
                  candidate_listener_orders_verified=True,
                  candidate_fresh_commit_metadata_preserved=True,
                  candidate_layout_refresh_does_not_refetch=True,
                  repeated_pre_layout_results_verified=True,
                  nested_and_throwing_dispatch_cleanup_verified=True,
                  native_scroll_state_and_fresh_commit_metadata_preserved=True,
                  actual_ai_dictionary_emoji_tail_reproduced=True,
                  expanded_same_query_order_stabilized_using_fresh_native_candidates=True)
    (work / 'report.json').write_text(json.dumps(report, ensure_ascii=False, sort_keys=True, indent=2) + '\n')


if __name__ == '__main__':
    main()

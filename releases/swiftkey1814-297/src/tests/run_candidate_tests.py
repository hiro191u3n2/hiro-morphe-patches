#!/usr/bin/env python3
"""Production CandidateScroll behavior on explicit native-grid/RecyclerView host fakes."""
import argparse
import hashlib
import json
import shutil
import subprocess
from pathlib import Path

SOURCES = {
    'android/view/View.java': '''package android.view; public class View {
 public boolean attached=true, shown=true; public int width=780,height=600;
 public boolean isAttachedToWindow(){return attached;} public boolean isShown(){return shown;}
 public int getWidth(){return width;} public int getHeight(){return height;}
}''',
    'androidx/recyclerview/widget/RecyclerView.java': '''package androidx.recyclerview.widget;
public class RecyclerView extends android.view.View {
 public ra.q1 adapter; public ra.a2 layout; public boolean computing;
 public boolean c0(){return computing;} public ra.q1 getAdapter(){return adapter;}
 public ra.a2 getLayoutManager(){return layout;}
}''',
    'androidx/recyclerview/widget/LinearLayoutManager.java': '''package androidx.recyclerview.widget;
public class LinearLayoutManager extends ra.a2 {}''',
    'androidx/recyclerview/widget/GridLayoutManager.java': '''package androidx.recyclerview.widget;
public class GridLayoutManager extends LinearLayoutManager { public int G=8; public ce0.a L; }''',
    'ra/a2.java': '''package ra; public abstract class a2 { public androidx.recyclerview.widget.RecyclerView b; }''',
    'ra/q1.java': '''package ra; public abstract class q1 {
 public r1 a=new r1(); public void q(int start,int count){a.insert(start,count);}
}''',
    'ra/r1.java': '''package ra;
public final class r1 {
 public rb0.h owner;
 public int size,full,changes,inserts,changeStart=-1,changeCount,insertStart=-1,insertCount;
 public Object payload;
 public void d(int start,int count,Object payload){
  if(start<0||count<=0||start+count>size)throw new AssertionError("Invalid old-row update");
  this.changes++;this.changeStart=start;this.changeCount=count;this.payload=payload;
  if(owner!=null)owner.rebind(start,count);
 }
 public void insert(int start,int count){
  if(start!=size||count<=0)throw new AssertionError("Invalid append notification");
  this.inserts++;this.insertStart=start;this.insertCount=count;size+=count;
 }
 public void reset(int count){full++;size=count;if(owner!=null)owner.rebind(0,count);}
 public void clear(){full=changes=inserts=changeCount=insertCount=0;changeStart=insertStart=-1;payload=null;}
}''',
    'ce0/a.java': '''package ce0; public abstract class a {}''',
    'rb0/g.java': '''package rb0; public final class g extends ce0.a {
 public java.util.ArrayList e; public g(java.util.ArrayList values){e=values;}
}''',
    'fb0/v.java': '''package fb0; public interface v { int d(); }''',
    'fb0/u.java': '''package fb0; public final class u { public v d; }''',
    'rb0/j.java': '''package rb0; public final class j implements fb0.v {
 public java.util.List b=new java.util.ArrayList();
 public final java.util.ArrayList<Integer> widths=new java.util.ArrayList<>();
 public final java.util.ArrayList<Object> keys=new java.util.ArrayList<>();
 public void add(int... values){for(int v:values){widths.add(v);keys.add(new Object());}}
 public int d(){return widths.size();}
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
public final class h extends ra.q1 {
 public gd0.b e;public fb0.u g=new fb0.u();public ah0.b0 h=new ah0.b0();
 public androidx.recyclerview.widget.GridLayoutManager l=new androidx.recyclerview.widget.GridLayoutManager();
 public boolean o=true,r=true,n=true;public int p,q;public boolean failG;public int[] forcedSpans;public int fullCalls;
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
  if(hiro.swiftkey.CandidateScroll.tryAppend(this,reset))return;
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
   view.layout=adapter.l;adapter.l.b=view;adapter.H();adapter.a.clear();adapter.fullCalls=0;}
  void append(int... values){provider.add(values);}
  void update(){adapter.d(false);}
 }
 static int[] spans(Grid g){ArrayList<?> raw=((rb0.g)g.adapter.l.L).e;int[] r=new int[raw.size()];
  for(int i=0;i<r.length;i++)r[i]=(Integer)raw.get(i);return r;}
 static void array(int[] actual,int... expected){check(java.util.Arrays.equals(actual,expected),
  "Span layout differs: "+java.util.Arrays.toString(actual));}
 static void fallback(Grid g,String message){check(!CandidateScroll.tryAppend(g.adapter,false),message);
  check(g.adapter.a.changes==0&&g.adapter.a.inserts==0,"Rejected append emitted notifications");}
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
    ]
    result_text = ''
    for name, command in commands:
        result = subprocess.run(command, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        (work / (name + '.log')).write_text(result.stdout)
        print(result.stdout, end='', flush=True)
        if result.returncode:
            raise SystemExit(result.returncode)
        result_text = result.stdout
    report_line = next(line[7:] for line in result_text.splitlines() if line.startswith('REPORT '))
    report = json.loads(report_line)
    report.update(production_source_sha256=hashlib.sha256(production.read_bytes()).hexdigest(),
                  device_tested=False, android_instrumentation_tested=False,
                  scope='Production CandidateScroll with explicit host native-grid and adapter-observer fakes; native G row packing replayed, no ART/device/FPS measurement',
                  full_width_measurement_preserved=True, native_candidate_mapping_preserved=True,
                  reset_full_refresh_preserved=True, snapshot_contains_no_input_text=True,
                  actual_ai_dictionary_emoji_tail_reproduced=True,
                  expanded_same_query_order_stabilized_using_fresh_native_candidates=True)
    (work / 'report.json').write_text(json.dumps(report, ensure_ascii=False, sort_keys=True, indent=2) + '\n')


if __name__ == '__main__':
    main()

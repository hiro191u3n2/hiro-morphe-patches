#!/usr/bin/env python3
"""Execute production pipeline with pixel-capable host Android fixtures.

Generated stubs/classes stay under --out. This tests pixel order, ownership and
concurrency; it does not emulate Android/ART, Camera2, or Galaxy save performance.
"""
import argparse
import json
from pathlib import Path
import subprocess


STUBS = {
    "android/os/Build.java": """package android.os;
public final class Build {public static final class VERSION {public static int SDK_INT=36;}}
""",
    "android/graphics/ColorSpace.java": """package android.graphics;
public final class ColorSpace {private final boolean srgb;
 public ColorSpace(boolean value){srgb=value;} public boolean isSrgb(){return srgb;}}
""",
    "android/graphics/Bitmap.java": r"""package android.graphics;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import com.hiro.ulike.HostAudit1932;
public final class Bitmap {
 public enum Config {ARGB_8888,RGB_565,RGBA_F16,HARDWARE,ALPHA_8}
 public static final List<Bitmap> ALL=Collections.synchronizedList(new ArrayList<Bitmap>());
 public static volatile boolean failCreateOnce,failCopyOnce,failNextDensity,failNextCreatedWrite,failNextCopyWrite;
 public static final AtomicInteger writesAfterRecycle=new AtomicInteger();
 private final int width,height;private final Config config;private final int[] pixels;
 private final boolean mutable;private volatile boolean recycled;private int density=160;
 private boolean alpha=true,gainmap;private ColorSpace colorSpace=new ColorSpace(true);
 private boolean densityFailure,writeFailure;
 public int reads,writes;
 private Bitmap(int w,int h,Config c,boolean m){
  if(w<1||h<1||(long)w*h>Integer.MAX_VALUE)throw new IllegalArgumentException("dimensions");
  width=w;height=h;config=c;mutable=m;pixels=new int[w*h];ALL.add(this);
 }
 public static Bitmap from(int w,int h,int[] p,Config c,boolean mutable){
  Bitmap b=new Bitmap(w,h,c,mutable);System.arraycopy(p,0,b.pixels,0,p.length);return b;
 }
 public static Bitmap createBitmap(int w,int h,Config c){
  if(failCreateOnce){failCreateOnce=false;throw new OutOfMemoryError("injected create");}
  Bitmap b=new Bitmap(w,h,c,true);b.densityFailure=failNextDensity;failNextDensity=false;
  b.writeFailure=failNextCreatedWrite;failNextCreatedWrite=false;
  HostAudit1932.event("create:"+w+"x"+h);return b;
 }
 public Bitmap copy(Config c,boolean m){
  check();if(failCopyOnce){failCopyOnce=false;throw new IllegalStateException("injected copy");}
  Bitmap b=new Bitmap(width,height,c,m);System.arraycopy(pixels,0,b.pixels,0,pixels.length);
  b.density=density;b.alpha=alpha;b.colorSpace=colorSpace;b.gainmap=gainmap;
  b.writeFailure=failNextCopyWrite;failNextCopyWrite=false;
  HostAudit1932.event("copy:"+width+"x"+height);return b;
 }
 private void check(){if(recycled)throw new IllegalStateException("recycled bitmap");}
 public int getWidth(){check();return width;} public int getHeight(){check();return height;}
 public Config getConfig(){check();return config;} public boolean isMutable(){check();return mutable;}
 public boolean isRecycled(){return recycled;}
 public void recycle(){recycled=true;HostAudit1932.event("recycle:"+width+"x"+height);}
 public int getDensity(){check();return density;}
 public void setDensity(int d){check();if(densityFailure){densityFailure=false;throw new IllegalStateException("injected density");}density=d;}
 public boolean hasAlpha(){check();return alpha;}public void setHasAlpha(boolean value){check();alpha=value;}
 public boolean hasGainmap(){check();return gainmap;}public void setGainmapForTest(boolean value){gainmap=value;}
 public ColorSpace getColorSpace(){check();return colorSpace;}public void setColorSpaceForTest(ColorSpace value){colorSpace=value;}
 public synchronized void getPixels(int[] out,int offset,int stride,int x,int y,int w,int h){
  check();bounds(out,offset,stride,x,y,w,h);reads++;
  for(int row=0;row<h;row++)System.arraycopy(pixels,(y+row)*width+x,out,offset+row*stride,w);
 }
 public synchronized void setPixels(int[] in,int offset,int stride,int x,int y,int w,int h){
  if(recycled)writesAfterRecycle.incrementAndGet();check();
  if(!mutable)throw new IllegalStateException("immutable");
  if(writeFailure){writeFailure=false;throw new IllegalStateException("injected write");}
  bounds(in,offset,stride,x,y,w,h);writes++;
  for(int row=0;row<h;row++)System.arraycopy(in,offset+row*stride,pixels,(y+row)*width+x,w);
 }
 private void bounds(int[] data,int offset,int stride,int x,int y,int w,int h){
  if(w<0||h<0||x<0||y<0||x+w>width||y+h>height||stride<w||offset<0||
    (h>0&&(long)offset+(h-1)*stride+w>data.length))throw new IllegalArgumentException("pixel bounds");
 }
 public synchronized int[] snapshot(){check();return pixels.clone();}
}
""",
    "com/hiro/ulike/HostAudit1932.java": r"""package com.hiro.ulike;
import java.util.*;import java.lang.reflect.*;
public final class HostAudit1932 {
 public static final ThreadLocal<List<String>> EVENTS=new ThreadLocal<List<String>>(){protected List<String> initialValue(){return new ArrayList<String>();}};
 public static void event(String value){EVENTS.get().add(value);}
 @SuppressWarnings("unchecked")public static <T>ThreadLocal<T> local(String name){
  try{Field f=QualityPipeline1932.class.getDeclaredField(name);f.setAccessible(true);return (ThreadLocal<T>)f.get(null);}
  catch(Exception e){throw new RuntimeException(e);}}
 @SuppressWarnings("unchecked")public static Map<Object,Object> map(String name){
  try{Field f=QualityPipeline1932.class.getDeclaredField(name);f.setAccessible(true);return (Map<Object,Object>)f.get(null);}
  catch(Exception e){throw new RuntimeException(e);}}
 public static QualityPixels1932.Plan current(){return HostAudit1932.<QualityPixels1932.Plan>local("CURRENT").get();}
}
""",
    "com/hiro/ulike/PhotoDetail.java": r"""package com.hiro.ulike;
public final class PhotoDetail {
 public static final ThreadLocal<Settings> REQUEST=new ThreadLocal<Settings>();
 public static Settings snapshot1932(){Settings s=REQUEST.get();return s==null?new Settings(false,2,false,2,true,true,true):s;}
 public static final class Settings {
  public final boolean noiseOn,sharpOn,texturePriority,haloSuppression,shadowPriority;
  public final int noiseLevel,sharpLevel;
  public Settings(boolean n,int nl,boolean s,int sl){this(n,nl,s,sl,true,true,true);}
  public Settings(boolean n,int nl,boolean s,int sl,boolean t,boolean h,boolean d){
   noiseOn=n;noiseLevel=nl;sharpOn=s;sharpLevel=sl;texturePriority=t;haloSuppression=h;shadowPriority=d;}
 }
}
""",
    "com/hiro/ulike/ChromaPipeline186.java": r"""package com.hiro.ulike;
public final class ChromaPipeline186 {
 public static final class State {public Runnable action;public QualityPixels1932.Plan expected;
  public State(){QualityPipeline1932.stateCreated(this);}}
 public static final class Buffer {}
}
""",
    "com/hiro/ulike/ShadowDetail1923.java": r"""package com.hiro.ulike;
public final class ShadowDetail1923 {
 public static void run(ChromaPipeline186.State state,ChromaPipeline186.Buffer buffer){
  if(state.action!=null)state.action.run();}
}
""",
    "com/hiro/ulike/ChromaPipeline177.java": r"""package com.hiro.ulike;
import android.graphics.Bitmap;
public final class ChromaPipeline177 {
 public interface Observer {void apply(Config c,ChromaPipeline186.State state);}
 public static final class Config {
  public boolean chroma,replace,denoise,useLegacy,fail;
  public int calls,width,height;public Bitmap bitmap,original,ownedResult;
  public PhotoDetail.Settings passed,legacyObserved;
  public int[] before,after;public QualityPixels1932.Plan plan;
  public ChromaPipeline186.State state;public Observer observer;
 }
 public static final ThreadLocal<Config> CONFIG=new ThreadLocal<Config>(){protected Config initialValue(){return new Config();}};
 public static boolean enabled1932(){return CONFIG.get().chroma;}
 public static Bitmap apply(Bitmap bitmap,Bitmap original,PhotoDetail.Settings settings){
  Config c=CONFIG.get();c.calls++;c.bitmap=bitmap;c.original=original;c.passed=settings;
  if(bitmap==null||bitmap.isRecycled())return bitmap;
  c.width=bitmap.getWidth();c.height=bitmap.getHeight();c.before=bitmap.snapshot();
  c.plan=HostAudit1932.current();HostAudit1932.event("apply:"+c.width+"x"+c.height);
  c.state=new ChromaPipeline186.State();c.state.expected=c.plan;
  if(c.useLegacy)c.legacyObserved=QualityPipeline1932.settingsForLegacy(new PhotoDetail.Settings(true,4,true,4,false,false,false));
  if(c.observer!=null)c.observer.apply(c,c.state);
  if(c.fail)throw new IllegalStateException("injected NR");
  Bitmap out=bitmap;
  if(c.replace||(c.denoise&&settings.noiseOn)){
   out=bitmap.copy(Bitmap.Config.ARGB_8888,true);c.ownedResult=out;
   if(c.denoise&&settings.noiseOn){
    int[] p=c.before.clone();
    for(int y=1;y<c.height-1;y++)for(int x=1;x<c.width-1;x++){
     int i=y*c.width+x,center=c.before[i];if((center>>>24)!=255)continue;
     int rr=0,gg=0,bb=0;
     for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++){
      int q=c.before[(y+dy)*c.width+x+dx];rr+=(q>>>16)&255;gg+=(q>>>8)&255;bb+=q&255;}
     p[i]=0xff000000|(((rr+4)/9)<<16)|(((gg+4)/9)<<8)|((bb+4)/9);
    }
    out.setPixels(p,0,c.width,0,0,c.width,c.height);
   }
  }
  c.after=out.snapshot();return out;
 }
}
""",
    "com/hiro/ulike/SaveQuality2.java": r"""package com.hiro.ulike;
import android.graphics.Bitmap;
public final class SaveQuality2 {
 public static final ThreadLocal<int[]> SIZE=new ThreadLocal<int[]>();
 public static final ThreadLocal<Integer> FALLBACK=new ThreadLocal<Integer>(){protected Integer initialValue(){return 0;}};
 public static int[] output186(int w,int h,int rotation,boolean fixed){
  int[] s=SIZE.get();if(s!=null)return s.clone();
  return rotation==90||rotation==270?new int[]{h,w}:new int[]{w,h};}
 public static Bitmap normalize186(Bitmap bitmap,int rotation,boolean fixed){
  FALLBACK.set(FALLBACK.get()+1);HostAudit1932.event("fallback");
  if(bitmap==null||bitmap.isRecycled())return bitmap;
  return bitmap.copy(Bitmap.Config.ARGB_8888,true);}
}
""",
    "com/hiro/ulike/ShotContext1932.java": r"""package com.hiro.ulike;
import android.graphics.Bitmap;import java.util.*;
public final class ShotContext1932 {
 public static final int LENS_UNKNOWN=0,LENS_FRONT=1,LENS_BACK=2,LENS_ULTRAWIDE=3,LENS_TELEPHOTO=4;
 public static final class Snapshot {
  public final int iso,lensKind;public final long exposureNanos;public final boolean metadataReliable;
  public final float beautyStrength;
  public Snapshot(int iso,long exposure,int lens,boolean reliable,float beauty){
   this.iso=iso;exposureNanos=exposure;lensKind=lens;metadataReliable=reliable;beautyStrength=beauty;}
 }
 public static final Snapshot UNKNOWN=new Snapshot(0,0,0,false,-1);
 public static final Map<Bitmap,Snapshot> SHOTS=Collections.synchronizedMap(new WeakHashMap<Bitmap,Snapshot>());
 public static volatile boolean failCopyOnce;
 public static Snapshot forBitmap(Bitmap b){Snapshot s=SHOTS.get(b);return s==null?UNKNOWN:s;}
 public static void copy(Bitmap from,Bitmap to){
  if(failCopyOnce){failCopyOnce=false;throw new IllegalStateException("injected metadata copy");}
  SHOTS.put(to,forBitmap(from));}
 public static void forget(Bitmap bitmap){SHOTS.remove(bitmap);}
}
""",
}


TEST = r"""package com.hiro.ulike;
import android.graphics.*;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
public final class PipelineHost1932 {
 private static final AtomicInteger checks=new AtomicInteger();
 private static final Map<String,String> outcomes=new LinkedHashMap<String,String>();
 private interface Scenario {void run() throws Exception;}
 private static void check(boolean value,String detail){checks.incrementAndGet();if(!value)throw new AssertionError(detail);}
 private static int rgb(int r,int g,int b){return 0xff000000|(clamp(r)<<16)|(clamp(g)<<8)|clamp(b);}
 private static int clamp(int n){return Math.max(0,Math.min(255,n));}
 private static int red(int p){return (p>>>16)&255;}private static int green(int p){return (p>>>8)&255;}
 private static void reset(){
  PhotoDetail.REQUEST.remove();ChromaPipeline177.CONFIG.remove();SaveQuality2.SIZE.remove();SaveQuality2.FALLBACK.set(0);
  HostAudit1932.EVENTS.remove();HostAudit1932.local("CURRENT").remove();HostAudit1932.local("LEGACY").remove();
  HostAudit1932.map("PREPARED").clear();HostAudit1932.map("STATES").clear();
  Bitmap.ALL.clear();Bitmap.failCreateOnce=false;Bitmap.failCopyOnce=false;Bitmap.failNextDensity=false;
  Bitmap.failNextCreatedWrite=false;Bitmap.failNextCopyWrite=false;Bitmap.writesAfterRecycle.set(0);
  ShotContext1932.SHOTS.clear();ShotContext1932.failCopyOnce=false;
 }
 private static void scenario(String name,Scenario scenario){
  reset();try{scenario.run();outcomes.put(name,"passed");}
  catch(Throwable t){outcomes.put(name,"FAILED: "+t.toString());t.printStackTrace(System.err);}
 }
 private static Bitmap image(int w,int h,long seed){
  Random random=new Random(seed);int[] pixels=new int[w*h];
  for(int i=0;i<pixels.length;i++){
   int n=(int)Math.round(random.nextGaussian()*9),v=110+n;
   pixels[i]=rgb(v+16,v,v-8);
  }
  return Bitmap.from(w,h,pixels,Bitmap.Config.ARGB_8888,true);
 }
 private static void settings(boolean noise,boolean sharp){PhotoDetail.REQUEST.set(new PhotoDetail.Settings(noise,3,sharp,4,true,true,true));}
 private static QualityPixels1932.Plan basePlan(int[] pixels,int w,int h){
  return QualityPixels1932.plan(QualityPixels1932.estimate(pixels,w,h),800,20000000,2,0.6f,3,4,true,true,1);}
 private static int[] pureResize(final int[] in,final int sw,final int sh,final int dw,final int dh){
  final int[] out=new int[dw*dh];double scale=Math.max((double)dw/sw,(double)dh/sh);
  double cw=dw/scale,ch=dh/scale;
  QualityPixels1932.resizeCrop(new QualityPixels1932.RowSource(){public void readRow(int y,int[] p){System.arraycopy(in,y*sw,p,0,sw);}},sw,sh,
   new QualityPixels1932.RowSink(){public void writeRow(int y,int[] p){System.arraycopy(p,0,out,y*dw,dw);}},dw,dh,(sw-cw)/2,(sh-ch)/2,cw,ch);
  return out;
 }
 private static void noLeakedOwned(Bitmap input,Bitmap returned){
  synchronized(Bitmap.ALL){for(Bitmap b:Bitmap.ALL)if(b!=input&&b!=returned)
   check(b.isRecycled(),"failed preparation must recycle every owned output");}
  check(!input.isRecycled(),"caller input remains live");
  check(Bitmap.writesAfterRecycle.get()==0,"workers joined before recycling output");
 }
 private static void sourceOrder() {
  settings(true,true);Bitmap in=image(61,43,82931);int[] before=in.snapshot();
  SaveQuality2.SIZE.set(new int[]{122,86});ChromaPipeline177.Config c=ChromaPipeline177.CONFIG.get();c.denoise=true;
  ShotContext1932.Snapshot shot=new ShotContext1932.Snapshot(1600,20000000,2,true,0.6f);ShotContext1932.SHOTS.put(in,shot);
  Bitmap out=QualityPipeline1932.normalize(in,0,true);
  check(SaveQuality2.FALLBACK.get()==0,"normal enlargement succeeds");
  check(c.width==61&&c.height==43,"NR before enlargement uses input dimensions");
  check(c.passed.noiseOn&&!c.passed.sharpOn,"source stage receives NR and no sharp");
  check(c.plan.haloSuppression,"default halo protection reaches output plan");
  check(c.bitmap==c.original,"legacy pipeline cannot take ownership of caller source");
  check(Arrays.equals(before,in.snapshot())&&!in.isRecycled(),"source pixels/ownership preserved");
  check(out.getWidth()==122&&out.getHeight()==86,"enlarged output dimensions");
  int[] enlarged=pureResize(c.after,61,43,122,86),expected=new int[enlarged.length];
  QualityPixels1932.finishStrip(enlarged,expected,122,86,0,86,c.plan,false,true);
  check(Arrays.equals(expected,out.snapshot()),"exact output equals NR then resample then final sharp");
  check(ShotContext1932.forBitmap(out)==shot,"shot snapshot propagated to output");
  check(c.ownedResult.isRecycled(),"intermediate denoised bitmap recycled after resize");
 }
 private static void downsampleFirst(){
  settings(true,true);Bitmap in=image(192,144,91011);int[] original=in.snapshot();
  SaveQuality2.SIZE.set(new int[]{96,64});ChromaPipeline177.Config c=ChromaPipeline177.CONFIG.get();c.denoise=true;
  Bitmap out=QualityPipeline1932.normalize(in,0,true);
  check(c.width==96&&c.height==64,"large input reduced before NR");
  check(Arrays.equals(c.before,pureResize(original,192,144,96,64)),"reduction includes correct center crop before NR");
  check(c.plan.outputScale==1f,"final sharpening planned in reduced dimensions");
  int[] expected=new int[c.after.length];
  QualityPixels1932.finishStrip(c.after,expected,96,64,0,64,c.plan,false,true);
  check(Arrays.equals(expected,out.snapshot()),"reduced image receives NR then sharp without second resize");
  check(Arrays.equals(original,in.snapshot()),"large source remains immutable");
 }
 private static void chromaBeforeResize(){
  settings(false,true);int w=63,h=47;int[] input=new int[w*h];
  for(int y=0;y<h;y++)for(int x=0;x<w;x++){
   int c=((x+y)&1)==0?18:-18;input[y*w+x]=rgb(150+c,150,150-3*c);
  }
  Bitmap in=Bitmap.from(w,h,input,Bitmap.Config.ARGB_8888,true);
  SaveQuality2.SIZE.set(new int[]{126,94});ChromaPipeline177.Config c=ChromaPipeline177.CONFIG.get();c.chroma=true;
  Bitmap out=QualityPipeline1932.normalize(in,0,true);
  int[] clean=new int[input.length];
  QualityPixels1932.finishStrip(input,clean,w,h,0,h,c.plan,true,false);
  check(!Arrays.equals(input,clean),"fixture contains detectable source-domain periodic chroma");
  int[] resized=pureResize(clean,w,h,126,94),expected=new int[126*94];
  QualityPixels1932.finishStrip(resized,expected,126,94,0,94,c.plan,false,true);
  check(Arrays.equals(expected,out.snapshot()),"periodic chroma before resize; final sharp after resize");
  check(Arrays.equals(input,in.snapshot())&&!in.isRecycled(),"chroma correction owns a separate bitmap");
 }
 private static void rotations(){
  int sw=5,sh=3;int[] original=new int[sw*sh];
  for(int i=0;i<original.length;i++)original[i]=rgb(20+i*7,40+i*5,60+i*3);
  for(int rot:new int[]{0,90,180,270,-90,-1}){
   settings(false,false);ChromaPipeline177.CONFIG.set(new ChromaPipeline177.Config());
   Bitmap in=Bitmap.from(sw,sh,original,Bitmap.Config.ARGB_8888,true);in.setDensity(420);in.setHasAlpha(false);
   Bitmap out=QualityPipeline1932.normalize(in,rot,false);int turn=rot==-1?0:(rot+360)%360;
   int dw=turn==90||turn==270?sh:sw,dh=turn==90||turn==270?sw:sh;
   int[] expected=new int[dw*dh];
   for(int y=0;y<dh;y++)for(int x=0;x<dw;x++){
    int sx=x,sy=y;
    if(turn==90){sx=y;sy=sh-1-x;}else if(turn==180){sx=sw-1-x;sy=sh-1-y;}else if(turn==270){sx=sw-1-y;sy=x;}
    expected[y*dw+x]=original[sy*sw+sx];
   }
   check(Arrays.equals(expected,out.snapshot()),"exact rotation "+rot);
   check(Arrays.equals(original,in.snapshot())&&!in.isRecycled(),"rotation input preserved "+rot);
   check(out.getDensity()==420&&!out.hasAlpha(),"rotation density/alpha tags "+rot);
  }
 }
 private static void fractionalCrop(){
  int sw=83,sh=61;int[] original=new int[sw*sh];
  for(int y=0;y<sh;y++)for(int x=0;x<sw;x++)original[y*sw+x]=rgb(20+2*x,30+2*y,50+x+y);
  for(int rot:new int[]{0,90}){
   settings(false,false);ChromaPipeline177.CONFIG.set(new ChromaPipeline177.Config());
   Bitmap in=Bitmap.from(sw,sh,original,Bitmap.Config.ARGB_8888,true);
   int dw=rot==0?47:40,dh=rot==0?40:47;SaveQuality2.SIZE.set(new int[]{dw,dh});
   Bitmap out=QualityPipeline1932.normalize(in,rot,true);
   check(out.getWidth()==dw&&out.getHeight()==dh&&SaveQuality2.FALLBACK.get()==0,"fractional crop must not silently fall back");
   int[] p=out.snapshot();
   int rw=rot==90?sh:sw,rh=rot==90?sw:sh;
   double scale=Math.max((double)dw/rw,(double)dh/rh),cw=dw/scale,ch=dh/scale;
   for(int y=4;y<dh-4;y++)for(int x=4;x<dw-4;x++){
    double rx=(rw-cw)/2+(x+0.5)/scale-0.5,ry=(rh-ch)/2+(y+0.5)/scale-0.5;
    double sx=rot==90?ry:rx,sy=rot==90?sh-1-rx:ry;
    check(Math.abs(red(p[y*dw+x])-(int)Math.round(20+2*sx))<=1,"fractional cropped red ramp rot="+rot+" x="+x+" y="+y+" actual="+red(p[y*dw+x])+" expected="+Math.round(20+2*sx));
    check(Math.abs(green(p[y*dw+x])-(int)Math.round(30+2*sy))<=1,"fractional cropped green ramp rot="+rot+" x="+x+" y="+y+" actual="+green(p[y*dw+x])+" expected="+Math.round(30+2*sy));
   }
  }
 }
 private static void offAndMarkers() throws Exception {
  settings(false,false);Bitmap in=image(61,43,13);int[] before=in.snapshot();
  ChromaPipeline177.Config c=ChromaPipeline177.CONFIG.get();Bitmap prepared=QualityPipeline1932.normalize(in,0,false);
  check(prepared==in,"all disabled and identity geometry require no copy");
  check(!c.passed.noiseOn&&!c.passed.sharpOn,"off settings passed to legacy NR");
  check(Arrays.equals(before,in.snapshot()),"all disabled leaves exact pixels");
  int calls=c.calls;PhotoDetail.Settings s=PhotoDetail.snapshot1932();
  check(QualityPipeline1932.applyDetail(prepared,in,s)==prepared&&c.calls==calls,"identity marker prevents double processing");
  QualityPipeline1932.applyDetail(prepared,in,s);check(c.calls==calls+1,"completion marker consumed exactly once");
  QualityPipeline1932.normalize(in,0,false);calls=c.calls;
  Bitmap different=image(61,43,17);QualityPipeline1932.applyDetail(prepared,different,s);
  check(c.calls==calls+1,"different original cannot consume a false success");
  QualityPipeline1932.normalize(in,0,false);calls=c.calls;
  Object marker=HostAudit1932.map("PREPARED").get(in);Field field=marker.getClass().getDeclaredField("source");field.setAccessible(true);
  ((WeakReference<?>)field.get(marker)).clear();QualityPipeline1932.applyDetail(in,null,s);
  check(c.calls==calls+1,"cleared weak original must not equal null caller");
 }
 private static void legacyAndRestoration(){
  PhotoDetail.Settings requested=new PhotoDetail.Settings(true,2,true,3,false,false,true);PhotoDetail.REQUEST.set(requested);
  ChromaPipeline177.Config c=ChromaPipeline177.CONFIG.get();c.useLegacy=true;
  QualityPixels1932.Plan previous=basePlan(image(13,13,18).snapshot(),13,13);
  PhotoDetail.Settings previousLegacy=new PhotoDetail.Settings(false,1,true,4,true,true,false);
  HostAudit1932.<QualityPixels1932.Plan>local("CURRENT").set(previous);
  HostAudit1932.<PhotoDetail.Settings>local("LEGACY").set(previousLegacy);
  Bitmap in=image(83,67,9294);QualityPipeline1932.normalize(in,0,false);
  check(c.legacyObserved==c.passed,"legacy fallback receives exact source settings");
  check(c.passed.noiseLevel<=2&&!c.passed.sharpOn,"legacy NR upper bound with deferred sharp");
  check(!c.passed.texturePriority&&!c.passed.haloSuppression&&c.passed.shadowPriority,"legacy option flags retained");
  check(!c.plan.haloSuppression,"disabled halo option reaches final sharp plan");
  check(HostAudit1932.current()==previous,"nested current plan restored");
  check(HostAudit1932.<PhotoDetail.Settings>local("LEGACY").get()==previousLegacy,"nested legacy snapshot restored");
  c.fail=true;QualityPipeline1932.normalize(in,0,false);
  check(HostAudit1932.current()==previous&&HostAudit1932.<PhotoDetail.Settings>local("LEGACY").get()==previousLegacy,
   "fallback exception restores both thread locals");
 }
 private static void concurrentPlans() throws Exception {
  final ChromaPipeline186.State[] states=new ChromaPipeline186.State[4];
  final QualityPixels1932.Plan[] plans=new QualityPixels1932.Plan[4];
  final CyclicBarrier barrier=new CyclicBarrier(4);
  final AtomicReference<Throwable> failure=new AtomicReference<Throwable>();
  Thread[] threads=new Thread[4];
  for(int k=0;k<4;k++){final int id=k;threads[k]=new Thread(new Runnable(){public void run(){
   try{
    settings(true,false);final Bitmap in=image(83,67,812+id);
    ShotContext1932.SHOTS.put(in,new ShotContext1932.Snapshot(100+id*600,10000000,2,true,id/3f));
    ChromaPipeline177.Config config=ChromaPipeline177.CONFIG.get();
    config.observer=new ChromaPipeline177.Observer(){public void apply(ChromaPipeline177.Config c,ChromaPipeline186.State state){
     try{
      states[id]=state;plans[id]=c.plan;state.action=new Runnable(){public void run(){
       check(HostAudit1932.current()==plans[id],"worker receives corresponding state's plan");
       int expected=QualityPixels1932.shadowBudgetQ8(rgb(190,145,120),1,plans[id]);
       check(QualityPipeline1932.shadowBudgetQ8(rgb(190,145,120),1)==expected,"worker local skin budget");
      }};
      barrier.await();QualityPipeline1932.run(states[(id+1)%4],new ChromaPipeline186.Buffer());
      check(HostAudit1932.current()==plans[id],"cross-state worker restores caller plan");
      ChromaPipeline186.State absent=new ChromaPipeline186.State();HostAudit1932.map("STATES").remove(absent);
      absent.action=new Runnable(){public void run(){check(HostAudit1932.current()==null,"unbound worker has no stale plan");
       check(QualityPipeline1932.shadowBudgetQ8(rgb(190,145,120),1)==256,"unbound budget defaults full");}};
      QualityPipeline1932.run(absent,new ChromaPipeline186.Buffer());
      check(HostAudit1932.current()==plans[id],"unbound worker restores caller plan");
      ChromaPipeline186.State throwing=new ChromaPipeline186.State();throwing.action=new Runnable(){public void run(){throw new IllegalStateException("worker failure");}};
      try{QualityPipeline1932.run(throwing,new ChromaPipeline186.Buffer());throw new AssertionError("expected worker failure");}
      catch(IllegalStateException expected){check(HostAudit1932.current()==plans[id],"worker exception restores plan");}
     }catch(Exception e){throw new RuntimeException(e);}
    }};
    QualityPipeline1932.normalize(in,0,false);
    check(SaveQuality2.FALLBACK.get()==0,"interleaved normalization succeeds");
    check(HostAudit1932.current()==null,"normalization clears thread plan");
   }catch(Throwable t){failure.compareAndSet(null,t);barrier.reset();}
  }});threads[k].start();}
  for(Thread t:threads)t.join();if(failure.get()!=null)throw new AssertionError(failure.get());
  check(plans[0]!=plans[1]&&plans[1]!=plans[2],"shots keep distinct immutable plans");
 }
 private static void stripEquivalence(){
  for(int h:new int[]{127,128,129,513,617}){
   int w=131;Bitmap bitmap=image(w,h,1932+h);int[] input=bitmap.snapshot();
   for(int y=4;y<Math.min(h-4,128);y++)for(int x=4;x<w-4;x++){
    int c=((x+y)&1)==0?18:-18;input[y*w+x]=rgb(150+c,150,150-3*c);
   }
   for(int y=138;y<Math.min(h,164);y++)for(int x=7;x<19;x++)input[y*w+x]=0x80784625;
   bitmap.setPixels(input,0,w,0,0,w,h);QualityPixels1932.Plan plan=basePlan(input,w,h);
   int[] reference=new int[input.length];QualityPixels1932.finishStrip(input,reference,w,h,0,h,plan,true,true);
   QualityPipeline1932.finishInPlace(bitmap,plan,true,true);
   check(Arrays.equals(reference,bitmap.snapshot()),"parallel in-place equals immutable reference h="+h);
   check(Bitmap.writesAfterRecycle.get()==0,"no writes after recycle in strip runner");
  }
 }
 private static void unsupportedFallback(){
  for(int kind=0;kind<3;kind++){
   settings(true,true);ChromaPipeline177.CONFIG.set(new ChromaPipeline177.Config());SaveQuality2.FALLBACK.set(0);
   Bitmap in=image(23,19,912+kind);if(kind==0)in.setColorSpaceForTest(new ColorSpace(false));
   if(kind==1)in=Bitmap.from(23,19,in.snapshot(),Bitmap.Config.HARDWARE,false);
   if(kind==2)in.setGainmapForTest(true);
   int[] original=in.snapshot();Bitmap out=QualityPipeline1932.normalize(in,90,true);
   check(SaveQuality2.FALLBACK.get()==1,"unsupported input uses original normalization kind="+kind);
   check(ChromaPipeline177.CONFIG.get().calls==0,"unsupported input bypasses new filters kind="+kind);
   check(in.reads==0,"unsupported input not probed by CPU helper");
   check(Arrays.equals(original,in.snapshot())&&!in.isRecycled(),"unsupported input retained");
   QualityPipeline1932.applyDetail(out,in,PhotoDetail.snapshot1932());
   check(ChromaPipeline177.CONFIG.get().calls==1,"fallback continues existing detail path");
  }
 }
 private static void failure(String name){
  settings(false,false);Bitmap in=image(73,57,844);int[] original=in.snapshot();
  ChromaPipeline177.Config c=ChromaPipeline177.CONFIG.get();
  if(name.equals("create")){c.replace=true;SaveQuality2.SIZE.set(new int[]{146,114});Bitmap.failCreateOnce=true;}
  else if(name.equals("write")){c.replace=true;SaveQuality2.SIZE.set(new int[]{146,114});Bitmap.failNextCreatedWrite=true;}
  else if(name.equals("density")){c.replace=true;SaveQuality2.SIZE.set(new int[]{146,114});Bitmap.failNextDensity=true;}
  else if(name.equals("copy")){c.chroma=true;Bitmap.failCopyOnce=true;}
  else if(name.equals("copy_metadata")){c.chroma=true;ShotContext1932.failCopyOnce=true;}
  else if(name.equals("final_metadata")){c.replace=true;ShotContext1932.failCopyOnce=true;}
  else if(name.equals("worker_write")){c.chroma=true;Bitmap.failNextCopyWrite=true;}
  else if(name.equals("nr")){c.fail=true;}
  else throw new AssertionError(name);
  Bitmap result=QualityPipeline1932.normalize(in,0,true);
  check(SaveQuality2.FALLBACK.get()==1,"fault delegates once: "+name);
  check(result!=null&&!result.isRecycled(),"fallback result owned by caller: "+name);
  check(Arrays.equals(original,in.snapshot()),"fault preserves original pixels: "+name);
  check(HostAudit1932.current()==null&&HostAudit1932.local("LEGACY").get()==null,"fault clears contexts: "+name);
  check(HostAudit1932.map("PREPARED").isEmpty(),"fault creates no completion marker: "+name);
  noLeakedOwned(in,result);
 }
 private static void localBeautyBudget(){
  QualityPixels1932.NoiseStats stats=new QualityPixels1932.NoiseStats(8,10,120,0,1000);
  QualityPixels1932.Plan bare=QualityPixels1932.plan(stats,800,10000000,2,0,3,3,true,true,1);
  QualityPixels1932.Plan beauty=QualityPixels1932.plan(stats,800,10000000,2,1,3,3,true,true,1);
  check(bare.noiseLevel==beauty.noiseLevel,"beauty does not globally lower main noise setting");
  HostAudit1932.<QualityPixels1932.Plan>local("CURRENT").set(beauty);
  int skin=QualityPipeline1932.shadowBudgetQ8(rgb(190,145,120),1);
  int textured=QualityPipeline1932.shadowBudgetQ8(rgb(190,145,120),90);
  int background=QualityPipeline1932.shadowBudgetQ8(rgb(60,130,190),1);
  check(skin<textured&&textured==background,"smooth skin alone gets overlap reduction");
  HostAudit1932.local("CURRENT").remove();check(QualityPipeline1932.shadowBudgetQ8(rgb(190,145,120),1)==256,"outside worker budget unchanged");
 }
 private static void haloSetting(){
  int w=65,h=33;int[] input=new int[w*h];
  for(int y=0;y<h;y++)for(int x=0;x<w;x++){int value=x<w/2?40:200;input[y*w+x]=rgb(value,value,value);}
  int[] on=null,off=null;
  for(boolean halo:new boolean[]{true,false}){
   PhotoDetail.REQUEST.set(new PhotoDetail.Settings(false,3,true,4,true,halo,true));
   ChromaPipeline177.CONFIG.set(new ChromaPipeline177.Config());
   Bitmap in=Bitmap.from(w,h,input,Bitmap.Config.ARGB_8888,true);
   Bitmap out=QualityPipeline1932.normalize(in,0,false);
   check(SaveQuality2.FALLBACK.get()==0,"halo setting uses quality output path");
   check(ChromaPipeline177.CONFIG.get().plan.haloSuppression==halo,"requested halo flag reaches final sharp");
   check(Arrays.equals(input,in.snapshot()),"halo option preserves source ownership");
   if(halo)on=out.snapshot();else off=out.snapshot();
  }
  check(Arrays.equals(input,on),"pipeline halo ON retains hard-step bounds");
  int edge=(h/2)*w+w/2;
  check(QualityPixels1932.luma(off[edge])-QualityPixels1932.luma(off[edge-1])>
   QualityPixels1932.luma(on[edge])-QualityPixels1932.luma(on[edge-1]),"pipeline halo OFF visibly affects final sharp step");
 }
 private static void interruptionCleanup(){
  settings(false,false);Bitmap in=image(193,513,471);int[] original=in.snapshot();
  ChromaPipeline177.CONFIG.get().chroma=true;
  Thread.currentThread().interrupt();
  try{
   Bitmap out=QualityPipeline1932.normalize(in,0,false);
   check(SaveQuality2.FALLBACK.get()==1,"interrupted preparation uses established fallback");
   check(Thread.currentThread().isInterrupted(),"interruption status preserved");
   check(Arrays.equals(original,in.snapshot()),"interruption leaves caller pixels intact");
   noLeakedOwned(in,out);
  }finally{Thread.interrupted();}
 }
 private static String quote(String text){return "\""+text.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n")+"\"";}
 public static void main(String[] args){
  long started=System.nanoTime();
  scenario("source_NR_before_enlarge_final_sharp",new Scenario(){public void run(){sourceOrder();}});
  scenario("large_input_downsample_before_NR",new Scenario(){public void run(){downsampleFirst();}});
  scenario("periodic_chroma_before_resize",new Scenario(){public void run(){chromaBeforeResize();}});
  scenario("exact_rotation_and_output_tags",new Scenario(){public void run(){rotations();}});
  scenario("fractional_center_crop_with_rotation",new Scenario(){public void run(){fractionalCrop();}});
  scenario("OFF_and_identity_completion_markers",new Scenario(){public void run()throws Exception{offAndMarkers();}});
  scenario("legacy_snapshot_and_nested_context_restore",new Scenario(){public void run(){legacyAndRestoration();}});
  scenario("concurrent_state_plan_isolation",new Scenario(){public void run()throws Exception{concurrentPlans();}});
  scenario("parallel_strips_match_immutable_whole_frame",new Scenario(){public void run(){stripEquivalence();}});
  scenario("non_sRGB_hardware_gainmap_fallback",new Scenario(){public void run(){unsupportedFallback();}});
  scenario("local_beauty_shadow_budget",new Scenario(){public void run(){localBeautyBudget();}});
  scenario("halo_setting_reaches_final_sharpen",new Scenario(){public void run(){haloSetting();}});
  scenario("interrupted_worker_cleanup",new Scenario(){public void run(){interruptionCleanup();}});
  for(final String fault:new String[]{"create","write","density","copy","copy_metadata","final_metadata","worker_write","nr"})
   scenario("failure_cleanup_"+fault,new Scenario(){public void run(){failure(fault);}});
  scenario("null_and_recycled_fallback",new Scenario(){public void run(){
   check(QualityPipeline1932.normalize(null,0,false)==null,"null fallback");
   Bitmap recycled=image(7,5,1);recycled.recycle();check(QualityPipeline1932.normalize(recycled,0,false)==recycled,"recycled delegated unchanged");
   check(SaveQuality2.FALLBACK.get()==2,"each invalid input delegated once");}});
  boolean passed=true;for(String result:outcomes.values())if(!result.equals("passed"))passed=false;
  StringBuilder json=new StringBuilder("{\"status\":\"").append(passed?"passed":"failed").append("\",\"assertions\":").append(checks.get())
   .append(",\"physical_device_verified\":false,\"host_seconds\":").append((System.nanoTime()-started)/1e9).append(",\"scenarios\":{");
  int n=0;for(Map.Entry<String,String> result:outcomes.entrySet()){if(n++>0)json.append(',');json.append(quote(result.getKey())).append(':').append(quote(result.getValue()));}
  json.append("}}");System.out.println(json.toString());if(!passed)System.exit(1);
 }
}
"""


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--javac", required=True)
    parser.add_argument("--java", required=True)
    parser.add_argument("--out", required=True)
    args = parser.parse_args()
    out = Path(args.out).resolve()
    fixtures = out / "fixtures"
    classes = out / "classes"
    source = Path(__file__).resolve().parent
    files = []
    for relative, contents in STUBS.items():
        path = fixtures / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(contents, encoding="utf-8")
        files.append(str(path))
    test = fixtures / "com/hiro/ulike/PipelineHost1932.java"
    test.write_text(TEST, encoding="utf-8")
    files.extend([str(test), str(source / "QualityPixels1932.java"), str(source / "QualityPipeline1932.java")])
    classes.mkdir(parents=True, exist_ok=True)
    compile_run = subprocess.run([args.javac, "-source", "8", "-target", "8", "-Xlint:-options",
                                  "-d", str(classes), *files], text=True, capture_output=True)
    (out / "compile.log").write_text(compile_run.stdout + compile_run.stderr, encoding="utf-8")
    if compile_run.returncode:
        raise SystemExit(compile_run.stdout + compile_run.stderr)
    run = subprocess.run([args.java, "-Xmx256m", "-cp", str(classes), "com.hiro.ulike.PipelineHost1932"],
                         text=True, capture_output=True, timeout=120)
    (out / "execution.log").write_text(run.stdout + run.stderr, encoding="utf-8")
    try:
        result = json.loads(run.stdout.strip().splitlines()[-1])
    except (ValueError, IndexError):
        raise SystemExit(run.stdout + run.stderr)
    result["scope"] = "Production pixel pipeline with host Bitmap fixtures; stubbed collaborators, not Android/ART"
    (out / "host-pipeline1932.json").write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(result, indent=2))
    raise SystemExit(run.returncode)


if __name__ == "__main__":
    main()

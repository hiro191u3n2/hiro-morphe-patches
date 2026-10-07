package com.hiro.ulike;
import android.graphics.Bitmap;
import java.util.*;

public final class Fast1933Test {
 static int assertions,scenarios;
 static void require(boolean ok,String why){assertions++;if(!ok)throw new AssertionError(why);}
 static int[] pixels(int w,int h,boolean alpha){Random r=new Random(w*9187L+h);int[]p=new int[w*h];
  for(int i=0;i<p.length;i++)p[i]=alpha?r.nextInt():(0xff000000|r.nextInt(0x1000000));return p;}
 static void same(int[]a,int[]b,String why){require(a.length==b.length,why+" length");for(int i=0;i<a.length;i++)require(a[i]==b[i],why+" pixel "+i);}
 static void kernel(){
  final int[][]dims={{1,1,9,9},{9,9,1,1},{41,29,71,53},{127,93,18,11},{13,41,17,99},{251,89,181,347}};
  for(int[]d:dims)for(boolean crop:new boolean[]{false,true}){
   final int sw=d[0],sh=d[1],w=d[2],h=d[3];final int[]input=pixels(sw,sh,false),old=new int[w*h],fast=new int[w*h];
   double left=crop&&sw>2?0.37:0,top=crop&&sh>2?0.19:0,cw=sw-left*2,ch=sh-top*2;
   QualityPixels1932.resizeCrop(new QualityPixels1932.RowSource(){public void readRow(int y,int[]p){System.arraycopy(input,y*sw,p,0,sw);}},sw,sh,
    new QualityPixels1932.RowSink(){public void writeRow(int y,int[]p){System.arraycopy(p,0,old,y*w,w);}},w,h,left,top,cw,ch);
   FastPixels1933.Plan plan=FastPixels1933.prepare(sw,sh,w,h,left,top,cw,ch);
   FastPixels1933.RowSource source=new FastPixels1933.RowSource(){public void readRow(int y,int[]p){System.arraycopy(input,y*sw,p,0,sw);}};
   FastPixels1933.RowSink sink=new FastPixels1933.RowSink(){public void writeRow(int y,int[]p){System.arraycopy(p,0,fast,y*w,w);}};
   // Reverse partition order makes any cache/order dependency visible.
   int split=h/3;FastPixels1933.runRows(plan,source,sink,split,h);FastPixels1933.runRows(plan,source,sink,0,split);
   same(old,fast,"opaque exact crop="+crop);scenarios++;
  }
 }
 static void bitmap(){
  for(int rotation:new int[]{0,90,180,270})for(boolean alpha:new boolean[]{false,true})for(boolean enlarged:new boolean[]{false,true}){
   int sw=129,sh=97,w=enlarged?341:53,h=enlarged?519:77;
   int[]p=pixels(sw,sh,alpha);Bitmap input=Bitmap.from(sw,sh,p);input.setDensity(320);
   int before=QualityPipeline1932.fallbacks;
   Bitmap actual=FastResize1933.resample(input,rotation,w,h);
   require(QualityPipeline1932.fallbacks==before+(alpha?1:0),"exact alpha dispatch");
   Bitmap expected=QualityPipeline1932.resample(input,rotation,w,h);
   same(expected.pixels(),actual.pixels(),"bitmap rotation="+rotation+" alpha="+alpha);
   same(p,input.pixels(),"source preserved");
   require(actual.getDensity()==320&&actual.hasAlpha()==input.hasAlpha(),"output tags");
   require(!input.isRecycled(),"input ownership");
   if(!alpha&&(rotation==90||rotation==270))require(input.reads<sw,"block transposed reads");
   actual.recycle();expected.recycle();input.recycle();scenarios++;
  }
  Bitmap in=Bitmap.from(80,60,pixels(80,60,false));in.setHasAlpha(false);
  require(FastResize1933.resample(in,0,80,60)==in,"identity bitmap");
  Bitmap out=FastResize1933.resample(in,0,160,120);require(!out.hasAlpha(),"opaque flag preserved");out.recycle();in.recycle();scenarios++;
 }
 static void failures(){
  for(int mode=0;mode<3;mode++){
   Bitmap in=Bitmap.from(130,97,pixels(130,97,false));int[] before=in.pixels();int first=Bitmap.ALL.size();
   if(mode==0)Bitmap.failCreate=true;if(mode==1)Bitmap.failDensity=true;if(mode==2)Bitmap.failWrite=true;
   boolean failed=false;try{FastResize1933.resample(in,90,341,519);}catch(RuntimeException e){failed=true;}catch(OutOfMemoryError e){failed=true;}
   require(failed,"fault surfaced "+mode);require(!in.isRecycled(),"fault input owned");same(before,in.pixels(),"fault source exact");
   for(int i=first;i<Bitmap.ALL.size();i++)require(Bitmap.ALL.get(i).isRecycled(),"partial output recycled");
   require(Bitmap.writesAfterRecycle==0,"workers joined before recycle");in.recycle();scenarios++;
  }
  Bitmap in=Bitmap.from(50,30,pixels(50,30,false));Thread.currentThread().interrupt();boolean failed=false;
  try{FastResize1933.resample(in,90,70,50);}catch(IllegalStateException e){failed=true;}finally{Thread.interrupted();}
  require(failed&&!in.isRecycled(),"interruption preserves input");in.recycle();scenarios++;
  final int[] transparent={0x80ffffff};boolean rejected=false;
  try{FastPixels1933.resizeCrop(new FastPixels1933.RowSource(){public void readRow(int y,int[]p){p[0]=transparent[0];}},1,1,
   new FastPixels1933.RowSink(){public void writeRow(int y,int[]p){}},2,2,0,0,1,1);}catch(IllegalArgumentException e){rejected=true;}
  require(rejected,"opaque kernel rejects transparent input");scenarios++;
 }
 public static void main(String[]args){kernel();bitmap();failures();System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+
  ",\"scenarios\":"+scenarios+",\"bit_identical_to_1932\":true,\"alpha_fallback_verified\":true,\"block_rotation_verified\":true,\"worker_failure_cleanup_verified\":true,\"device_tested\":false}");}
}

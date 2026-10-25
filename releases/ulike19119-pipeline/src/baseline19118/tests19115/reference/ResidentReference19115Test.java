package com.hiro.ulike;
import android.graphics.Bitmap;
import java.lang.reflect.Method;

/** Real key construction and foreground preflight against controlled peers. */
public final class ResidentReference19115Test {
 static int assertions,cases;
 static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
 static String key(int w,int h,boolean regional,int profile,int outputW,int outputH)throws Exception{
  GpuStrongDefault1990.regional=regional;GpuStrongDefault1990.legacyProfile=GpuStrongDefault1990.testedProfile=profile;GpuChain1961.preferred=128;
  return keyForCurrent(w,h,outputW,outputH);
 }
 static String keyForCurrent(int w,int h,int outputW,int outputH)throws Exception{
  Bitmap source=new Bitmap(w,h);QualityPixels1932.Plan plan=new QualityPixels1932.Plan();Method m=GpuResident1976.class.getDeclaredMethod("key",Bitmap.class,QualityPixels1932.Plan.class,Integer.TYPE,Boolean.TYPE,Integer.TYPE,Integer.TYPE,Integer.TYPE,QualityPixels1932.Plan.class,Boolean.TYPE);m.setAccessible(true);
  String value=(String)m.invoke(null,source,plan,4,true,0,outputW,outputH,plan,false);source.recycle();return value;
 }
 static void foreground(int width,int height,boolean before,boolean after,int oldProfile,int newProfile,boolean expected){
  GpuStrongDefault1990.regional=before;GpuStrongDefault1990.afterRegional=after;GpuStrongDefault1990.legacyProfile=GpuStrongDefault1990.testedProfile=oldProfile;GpuStrongDefault1990.afterLegacy=GpuStrongDefault1990.afterTested=newProfile;
  GpuChain1961.preferred=GpuChain1961.afterPreferred=GpuChain1961.planned=128;GpuChain1961.created=GpuChain1961.closed=0;GpuChain1961.active=null;
  GpuQualification1961.proofRows=128;GpuQualification1961.restores=GpuQualification1961.writes=0;QualityPipeline1932.calls=QualityPipeline1932.executedRows=0;PipelineDiagnostics1982.exitReason=ProcessingTiming1947.route=0;
  Bitmap source=new Bitmap(width,height);QualityPixels1932.Plan plan=new QualityPixels1932.Plan();Bitmap result=GpuResident1976.finish1978(source,plan,4,true,new int[]{1},0,width,height,plan,true,new ProcessingTiming1947.Trace());
  check(GpuQualification1961.restores==1,"whole proof restored once");check(GpuChain1961.created==1&&GpuChain1961.closed==1&&GpuChain1961.active==null,"changed route closes preflight exactly once");
  check((result!=null)==expected,"foreground result rejects changed regional route");check(QualityPipeline1932.calls==(expected?1:0),"only stable actual route enters producer");
  check(PipelineDiagnostics1982.exitReason==(expected?1:13)&&ProcessingTiming1947.route==(expected?4:0),"fallback does not claim adopted resident route");
  check(!source.isRecycled()&&source.pixels[0]==0xff123456,"caller source preserved for fallback");if(result!=null)result.recycle();source.recycle();cases++;
 }
 public static void main(String[] args)throws Exception{
  boolean baseline=args.length>0&&args[0].equals("baseline");StringBuilder keys=new StringBuilder();
  int[][] sizes={{5,7},{3060,4080},{4080,3060},{3060,4080},{4080,3060}};boolean[] flags={true,false,false,true,true};int[] profiles={4,4,4,6,7};
  for(int i=0;i<sizes.length;i++){String k=key(sizes[i][0],sizes[i][1],flags[i],profiles[i],5,7);if(i>0)keys.append(',');keys.append('"').append(k).append('"');}
  if(!baseline){
   String old=key(3060,4080,false,4,5,7),regional=key(3060,4080,true,4,5,7);check(!old.contains("regional=115")&&regional.contains("strong=4;regional=115;"),"tested input geometry records actual regional baseline");
   check(key(4080,3060,true,4,5,7).contains("regional=115"),"reverse orientation records regional baseline");
   check(!key(3060,3060,true,4,5,7).contains("regional=115"),"unmeasured source shape preserves old key");
   check(!key(3060,4080,true,6,5,7).contains("regional=115")&&!key(4080,3060,true,7,5,7).contains("regional=115"),"nonregional adaptive profile does not borrow regional identity");
   GpuStrongDefault1990.regional=true;GpuStrongDefault1990.legacyProfile=7;GpuStrongDefault1990.testedProfile=4;
   check(keyForCurrent(3060,4080,5,7).contains("strong=4;regional=115;"),"resident profile lookup uses tested source shape");
   String outside=keyForCurrent(5,7,3060,4080);check(outside.contains("strong=7;")&&!outside.contains("regional=115"),"resident profile lookup preserves outside source shape");
   foreground(3060,4080,false,false,4,4,true);foreground(3060,4080,true,true,4,4,true);foreground(4080,3060,true,true,4,4,true);
   foreground(3060,4080,false,true,4,4,false);foreground(3060,4080,true,false,4,4,false);foreground(4080,3060,true,false,4,4,false);
   foreground(5,7,true,false,4,4,true);foreground(3060,3060,false,true,4,4,true);foreground(3060,4080,true,false,6,6,true);foreground(3060,4080,true,true,4,6,false);
  }
  System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"cases\":"+cases+",\"unchanged_keys\":["+keys+"]}");
 }
}

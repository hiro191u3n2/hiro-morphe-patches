"""Exercise the production finishing worker with deterministic mock upstream stages.
The mocks test range/workspace/cancellation contracts, not the native denoiser or ART.
"""
from pathlib import Path
import re,subprocess

def test(root,work):
 src=work/'pipeline-host/src';classes=work/'pipeline-host/classes';src.mkdir(parents=True);classes.mkdir()
 files={
 'com/hiro/ulike/DetailPixels.java':(root/'noise-stubs/com/hiro/ulike/DetailPixels.java').read_text(),
 'com/hiro/ulike/DetailSerial186.java':'''package com.hiro.ulike;public final class DetailSerial186 {
 static int calls;static boolean fail;
 public static void filter(DetailPixels.Work w,int width,int rows,int start,int count,int noise,int sharp,boolean texture,boolean halos,boolean shadows){
  if(fail)throw new IllegalStateException("mock stage failure");calls++;
  if(start<0||count<0||start+count>rows)throw new AssertionError("detail bounds");
  // Stage has deliberately changed luma, proving finishing cannot return to raw luma.
  for(int row=start;row<start+count;row++)for(int col=0;col<width;col++)w.output[row*width+col]=0xff282828;
 }
}''',
 'com/hiro/ulike/Chroma186.java':'''package com.hiro.ulike;public final class Chroma186 {
 static int calls;static boolean fail,interrupt;
 public static void finishWorkspace(int[] raw,int[] base,int[] out,int width,int rows,int begin,int end,int radius,int[] cols,int[] cov,boolean nativeAllowed){
  if(fail)throw new IllegalStateException("mock chroma failure");calls++;
  if(begin<0||end>rows||end<begin||raw==out||base==out)throw new AssertionError("chroma bounds/alias");
  System.arraycopy(base,0,out,0,width*rows);
  for(int row=begin;row<end;row++)for(int col=0;col<width;col++)out[row*width+col]=0xff1a2a1a;
  if(interrupt)Thread.currentThread().interrupt();
 }
}''',
 'com/hiro/ulike/ChromaPipeline186.java':'''package com.hiro.ulike;import java.util.*;public final class ChromaPipeline186 {
 static final class Buffer {DetailPixels.Work pixels;int[] columns,covariance;Buffer(int width,int rows){pixels=new DetailPixels.Work(width*rows);columns=new int[width*7];covariance=new int[width*2];}}
 static final class State {
  volatile boolean failed;int core,height,radius,width,noise,sharp,next,writes;boolean texture,halos,shadows,nativeAllowed;int[] output;
  State(int w,int h,int c,int r,int n,boolean shadow,boolean nat){width=w;height=h;core=c;radius=r;noise=n;sharp=2;shadows=shadow;nativeAllowed=nat;output=new int[w*h];}
  synchronized int read(Buffer b){if(next>=height)return -1;int first=next;next+=core;int count=Math.min(core,height-first),top=Math.max(0,first-radius-7),rows=Math.min(height,first+count+radius+7)-top;
   Arrays.fill(b.pixels.source,0xff646464);Arrays.fill(b.pixels.output,0xffeeeeee);Arrays.fill(b.pixels.horizontal,0xffdddddd);Arrays.fill(b.pixels.denoised,0xffcccccc);return first;
  }
  synchronized void write(int[] pixels,int local,int first,int count){
   if(local<0||first<0||first+count>height||pixels.length<(local+count)*width)throw new AssertionError("write bounds");
   System.arraycopy(pixels,local*width,output,first*width,count*width);writes++;
  }
 }
}''',
 'com/hiro/ulike/PipelineTests1924.java':'''package com.hiro.ulike;import java.util.*;public class PipelineTests1924 {
 static int count;static void check(String n,boolean ok){if(!ok)throw new AssertionError(n);count++;System.out.println("PASS\\t"+n);}
 public static void main(String[] args){
  for(int radius:new int[]{1,2,3,12})for(int core:new int[]{1,7,32})for(int noise:new int[]{0,1,4})for(boolean shadow:new boolean[]{false,true})for(boolean nat:new boolean[]{false,true}){
   ChromaPipeline186.State s=new ChromaPipeline186.State(17,25,core,radius,noise,shadow,nat);
   ChromaPipeline186.Buffer b=new ChromaPipeline186.Buffer(17,core+2*(radius+7));
   ShadowDetail1923.run(s,b);
   check("processed-domain flat output r="+radius+" core="+core+" n="+noise+" shadow="+shadow+" native="+nat,!s.failed&&s.writes==(25+core-1)/core&&Arrays.stream(s.output).allMatch(p->p==0xff1a2a1a));
   check("immutable original source preserved",Arrays.stream(b.pixels.source).allMatch(p->p==0xff646464));
  }
  ChromaPipeline186.State s=new ChromaPipeline186.State(17,25,7,3,4,true,true);
  ChromaPipeline186.Buffer b=new ChromaPipeline186.Buffer(17,27);DetailSerial186.fail=true;ShadowDetail1923.run(s,b);
  check("detail failure marks state and publishes no partial core",s.failed&&s.writes==0);DetailSerial186.fail=false;
  s=new ChromaPipeline186.State(17,25,7,3,4,true,true);Chroma186.fail=true;ShadowDetail1923.run(s,b);
  check("chroma failure marks state and publishes no core",s.failed&&s.writes==0);Chroma186.fail=false;
  s=new ChromaPipeline186.State(17,25,7,3,4,true,true);Chroma186.interrupt=true;ShadowDetail1923.run(s,b);
  check("interruption propagates and no incomplete core published",s.failed&&s.writes==0&&Thread.currentThread().isInterrupted());Chroma186.interrupt=false;Thread.interrupted();
  for(int begin:new int[]{0,3})for(int end:new int[]{10,13}){
   DetailPixels.Work w=new DetailPixels.Work(17*13);Arrays.fill(w.source,0xffcccccc);Arrays.fill(w.output,0xff151515);
   ShadowDetail1923.legacy(w,17,13,begin,end-begin,4,4,true,true,true);
   check("legacy preserves processed DC and untouched rows "+begin+" "+end,Arrays.stream(w.output).allMatch(p->p==0xff151515));
   check("legacy does not overwrite original source",Arrays.stream(w.source).allMatch(p->p==0xffcccccc));
  }
  System.out.println("HOST_PIPELINE_ASSERTIONS="+count);System.out.println("PRODUCTION_WORKER_WITH_MOCK_UPSTREAM_STAGES_NOT_DEVICE_EXECUTION");
 }
}'''
 }
 for n,s in files.items():p=src/n;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s)
 result=subprocess.run(['javac','--release','8','-encoding','UTF-8','-d',str(classes),*[str(p) for p in src.rglob('*.java')],str(root/'ShadowDetail1923.java')],stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
 (work/'pipeline-host/compile.log').write_text(result.stdout);result.check_returncode()
 result=subprocess.run(['java','-cp',str(classes),'com.hiro.ulike.PipelineTests1924'],stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
 (work/'host-pipeline1924.txt').write_text(result.stdout);result.check_returncode();print(result.stdout[-250:])
 return int(re.search(r'HOST_PIPELINE_ASSERTIONS=(\d+)',result.stdout)[1])
if __name__=='__main__':
 import sys
 print(test(Path(__file__).resolve().parent,Path(sys.argv[1])))

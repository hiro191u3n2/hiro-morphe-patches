package com.hiro.ulike;
public final class ChromaPipeline186 {
 static final class Buffer { DetailPixels.Work pixels;int[] columns=new int[1],covariance=new int[1];Buffer(State s){pixels=new DetailPixels.Work(s.width*(s.core+2*(s.radius+7)));} }
 static final class State {
  volatile boolean failed;int core,height,radius=3,width,noise,sharp;boolean texture=true,halos=true,shadows=true,nativeAllowed;
  final int[] input,result;int next;
  State(int[] input,int width,int height,int core,int noise){this.input=input;this.width=width;this.height=height;this.core=core;this.noise=noise;result=new int[input.length];QualityPipeline1932.stateCreated(this);}
  synchronized int read(Buffer b){if(next>=height)return -1;int first=next,count=Math.min(core,height-first),top=Math.max(0,first-radius-7),bottom=Math.min(height,first+count+radius+7);
   System.arraycopy(input,top*width,b.pixels.source,0,(bottom-top)*width);next+=count;return first;}
  synchronized void write(int[] p,int start,int first,int count){System.arraycopy(p,start*width,result,first*width,count*width);}
 }
}

package com.hiro.ulike;
public final class ChromaPipeline186 {
 static final class Buffer { DetailPixels.Work pixels; int[] columns,covariance; }
 static final class State {
  volatile boolean failed; int core,height,radius,width,noise,sharp; boolean texture,halos,shadows,nativeAllowed;
  synchronized int read(Buffer b){return -1;} synchronized void write(int[] p,int a,int b,int c){}
 }
}

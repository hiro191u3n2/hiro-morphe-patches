package com.hiro.ulike;
import java.util.*;import java.lang.reflect.*;
public final class CoreNative1950Test {
 static int assertions,cases;static long pixels;
 static void check(boolean x,String message){assertions++;if(!x)throw new AssertionError(message);}
 public static void main(String[]a)throws Exception{
  CoreDexOracle1950.init(a[0]);
  Field l=CorePixels1950.class.getDeclaredField("LUMA"),c=CorePixels1950.class.getDeclaredField("COLOUR");l.setAccessible(true);c.setAccessible(true);
  int[]lr=(int[])l.get(null),cc=(int[])c.get(null);
  for(int level=1;level<=4;level++)for(int i=0;i<256;i++){check(lr[level*256+i]==CoreDexOracle1950.table("LUMA",level)[i],"baseline luma table");check(cc[level*256+i]==CoreDexOracle1950.table("COLOUR",level)[i],"baseline colour table");}
  Random random=new Random(195049L);
  int[][]dims={{1,1},{2,7},{7,2},{17,13},{31,23},{7,69}};
  for(int[]size:dims)for(int pattern=0;pattern<4;pattern++){
   int width=size[0],rows=size[1],n=width*rows;int[]g=new int[n],v=new int[n];
   for(int i=0;i<n;i++){
    int x=i%width,y=i/width,k=random.nextInt(31)-15,r,b,z;
    if(pattern==0){r=clamp(22+k);z=clamp(18+k);b=clamp(15+k);}
    else if(pattern==1){r=clamp(189+k);z=clamp(155+k);b=clamp(124+k);}
    else if(pattern==2){r=(x*83+y*41)&255;z=(x*23+y*17)&255;b=(x*31+y*71)&255;}
    else{r=random.nextInt(256);z=random.nextInt(256);b=random.nextInt(256);}
    g[i]=0xff000000|r<<16|z<<8|b;v[i]=0xff000000|random.nextInt(1<<24);
    if(pattern==3 && i%11==0)g[i]&=0x7fffffff;
   }
   for(int level=1;level<=4;level++)for(int flags=0;flags<8;flags++){
    boolean h=(flags&1)!=0,t=(flags&2)!=0,s=(flags&4)!=0;int first=(rows>3 && flags%3==0)?1:0,last=(rows>3 && flags%3==0)?rows-1:rows;
    int[]expected=new int[n],actual=new int[n];Arrays.fill(expected,0x13579bdf);Arrays.fill(actual,0x13579bdf);
    CoreDexOracle1950.bilateral(g,v,expected,width,rows,level,h,t,s,first,last);
    check(CorePixels1950.run(g,v,actual,width,rows,level,h,t,s,first,last),"native JNI ready");
    for(int i=0;i<n;i++)check(actual[i]==expected[i],"DEX/native differs size="+width+"x"+rows+" pattern="+pattern+" level="+level+" flags="+flags+" i="+i+" expected="+Integer.toHexString(expected[i])+" actual="+Integer.toHexString(actual[i]));
    cases++;pixels+=(long)width*(last-first);
   }
  }
  int[]g=new int[221],v=new int[221],o=new int[221];Arrays.fill(g,0xff37322c);Arrays.fill(v,0xff38302d);Arrays.fill(o,0x13579bdf);
  check(!CorePixels1950.run(g,v,g,17,13,1,true,true,true,0,13),"alias rejection");check(!CorePixels1950.run(g,v,o,17,13,5,true,true,true,0,13),"bad level");check(!CorePixels1950.run(g,v,o,17,13,1,true,true,true,0,14),"bad range");check(!CorePixels1950.run(g,v,o,Integer.MAX_VALUE,13,1,true,true,true,0,13),"overflow rejection");
  int[]expected=o.clone();CoreDexOracle1950.bilateral(g,v,expected,17,13,2,false,true,true,0,13);CorePixels1950.bilateral(g,v,o,17,13,2,false,true,true,0,13);check(Arrays.equals(expected,o),"production wrapper exact runtime selfcheck");
  Field verified=CorePixels1950.class.getDeclaredField("verified");verified.setAccessible(true);check(verified.getInt(null)==1,"native enabled after independent real-bytecode selfcheck");
  Arrays.fill(o,0);Arrays.fill(expected,0);CoreDexOracle1950.bilateral(g,g,expected,17,13,4,true,true,true,0,13);check(CorePixels1950.run(g,g,o,17,13,4,true,true,true,0,13),"shared guide/value JNI arrays supported");check(Arrays.equals(expected,o),"normal horizontal shared-input exactness");
  verified.setInt(null,-1);Arrays.fill(o,0);CorePixels1950.bilateral(g,g,o,17,13,4,true,true,true,0,13);check(Arrays.equals(expected,o),"original DEX fallback exactness");verified.setInt(null,1);
  final int pw=7,pr=69;final int[]pg=new int[pw*pr],pv=new int[pw*pr],po=new int[pw*pr];int[]pe=new int[pw*pr];
  for(int i=0;i<pg.length;i++){pg[i]=0xff000000|((i*17+31)&255)<<16|((i*7+29)&255)<<8|((i*3+11)&255);pv[i]=0xff000000|((i*13+7)&255)<<16|((i*11+19)&255)<<8|((i*5+37)&255);}
  CoreDexOracle1950.bilateral(pg,pv,pe,pw,pr,3,false,true,true,0,pr);
  final Throwable[]failure=new Throwable[2];Thread[]threads=new Thread[2];
  for(int k=0;k<2;k++){final int index=k;threads[k]=new Thread(new Runnable(){public void run(){try{if(!CorePixels1950.run(pg,pv,po,pw,pr,3,false,true,true,index==0?0:34,index==0?34:pr))throw new AssertionError("disjoint native call declined");}catch(Throwable e){failure[index]=e;}}});threads[k].start();}
  for(Thread thread:threads)thread.join();check(failure[0]==null&&failure[1]==null,"concurrent native runs");check(Arrays.equals(pe,po),"concurrent disjoint rows match production DEX");
  System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"cases\":"+cases+",\"compared_pixels\":"+pixels+",\"production_dex_oracle_executed\":true,\"pixel_equivalence_to_baseline\":true,\"production_main_denoise\":true,\"production_c_jni_executed\":true,\"runtime_native_selfcheck_passed\":true,\"physical_android_tested\":false}");
 }
 static int clamp(int x){return Math.max(0,Math.min(255,x));}
}

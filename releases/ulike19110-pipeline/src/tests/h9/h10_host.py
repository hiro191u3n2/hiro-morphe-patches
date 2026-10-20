#!/usr/bin/env python3
"""Exercise two-buffer ownership, overlap and CPU fallback with real CPU pixels."""
import pathlib
import os
import subprocess

root=pathlib.Path(__file__).resolve().parents[2]
work=pathlib.Path(os.environ.get('ULIKE_H9_WORK',root.parent/'build'))/'h9h11-overlap'
work.mkdir(parents=True,exist_ok=True)
source=(root/'QualityShadow1932.java').read_text()
head,tail=source.split('private static boolean prepareSavedTile',1)
needle='        for(int row=start;row<limit;row++)for(int col=0;col<width;col++) {'
assert tail.count(needle)==1
source=head+'private static boolean prepareSavedTile'+tail.replace(needle,'''
        if(start==128) {
            try {
                if(!GpuInteger1949.entered.await(5,java.util.concurrent.TimeUnit.SECONDS))
                    throw new AssertionError("GPU worker did not start");
                GpuInteger1949.overlap=GpuInteger1949.active;
                GpuInteger1949.release.countDown();
            } catch(InterruptedException e){Thread.currentThread().interrupt();}
        }
'''+needle)
(work/'QualityShadow1932.java').write_text(source)
(work/'GpuInteger1949.java').write_text('''package com.hiro.ulike;
import java.util.concurrent.CountDownLatch;
final class GpuInteger1949 {
 static volatile boolean active,overlap,background;
 static final CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
 static int calls;
 interface CpuFinishInto{boolean run(int[] output,int offset);}
 interface CpuAggregate{boolean run(int[] output);}
 static boolean available(){return true;}
 static boolean aggregateAvailable(int w,int rows,int begin,int end,int lo,int hi,int radius){return false;}
 static boolean aggregate(int[] input,int[] meta,int[] out,int w,int rows,int begin,
     int end,int lo,int hi,int radius,int[] range,CpuAggregate cpu){return false;}
 static boolean finishSavedInto(int[] input,int[] meta,int[] policy,int[] out,int offset,
     int w,int rows,int begin,int end,int lo,int hi,int radius,int[] range,int noise,
     boolean shadows,int global,int beauty,int smoothLimit,CpuFinishInto cpu){
   background|=Thread.currentThread().getName().equals("ulike-residual-gpu");
   int call=++calls;
   if(call==1){
     int[] m=meta.clone(),p=policy.clone();active=true;entered.countDown();
     try {if(!release.await(5,java.util.concurrent.TimeUnit.SECONDS))
       throw new AssertionError("preparation did not overlap");}
     catch(InterruptedException e){Thread.currentThread().interrupt();return false;}
     finally {active=false;}
     if(!java.util.Arrays.equals(m,meta)||!java.util.Arrays.equals(p,policy))
       throw new AssertionError("GPU-owned tile mutated during next tile preparation");
   }
   // Force the middle tile to take the per-tile CPU fallback.
   return call==2?false:cpu.run(out,offset);
 }
 static boolean finishInto(int[] input,int[] meta,int[] policy,int[] out,int offset,
     int w,int rows,int begin,int end,int lo,int hi,int radius,int[] range,int noise,
     boolean shadows,int global,int beauty,int smoothLimit,CpuFinishInto cpu){return false;}
}''')
(work/'H10OverlapTest.java').write_text('''package com.hiro.ulike;
import java.util.Arrays;
import java.util.Random;
public final class H10OverlapTest {
 public static void main(String[] args){
   int w=53,h=270;int[] input=new int[w*h];Random random=new Random(1951);
   for(int i=0;i<input.length;i++){
     int c=80+random.nextInt(60);input[i]=0xff000000|(c<<16)|((c+2)<<8)|(c+1);
   }
   QualityPixels1932.Plan plan=QualityPixels1932.plan(
     new QualityPixels1932.NoiseStats(7,7,128,0,4096),400,10000000L,2,0,4,0,true,true,1)
     .withLocalNoise(null,4);
   int[] expected=input.clone(),actual=input.clone();
   QualityShadowReference1944.smoothRange(input,expected,w,h,0,h,0,h,4,true,4,plan,0);
   QualityShadow1932.smoothSavedRange1951(input,actual,w,h,0,h,0,h,4,true,4,plan,0);
   if(!Arrays.equals(expected,actual)){
     for(int i=0;i<actual.length;i++)if(expected[i]!=actual[i])
       throw new AssertionError("saved CPU fallback pixel changed at "+i);
   }
   if(GpuInteger1949.calls!=3||!GpuInteger1949.background||!GpuInteger1949.overlap)
     throw new AssertionError("two-buffer overlap/fallback "+GpuInteger1949.calls+"/"+
       GpuInteger1949.background+"/"+GpuInteger1949.overlap);
   System.out.println("H10 overlap, buffer ownership, per-tile CPU fallback, pixel equality: "+actual.length);
 }
}''')
classes=work/'classes';classes.mkdir(exist_ok=True)
java=os.environ.get('ULIKE_JDK_HOME','')
java=str(pathlib.Path(java)/'bin/java') if java else 'java'
sources=[work/name for name in ('QualityShadow1932.java','GpuInteger1949.java','H10OverlapTest.java')]
sources += [root/name for name in ('QualityPixels1932.java','NativeMoire1951.java','PolicyCache1945.java','NoiseCache1944.java',
    'NativeSpeed1944.java','NativeSpeed1935.java','SpatialNoise1934.java','LongMoire1934.java',
    'SpeedWorkers1935.java','cache1945-reference/QualityShadowReference1944.java',
    'tests/noise-fixtures/com/hiro/ulike/QualityPipeline1932.java')]
subprocess.run([java,'-m','jdk.compiler/com.sun.tools.javac.Main','-d',str(classes),
    *(str(p) for p in sources)],check=True)
prebuilt=pathlib.Path(os.environ.get('ULIKE_H9_PREBUILT',root.parent/'build'/'gpu-finish1950-host'))/'fallback-fixture'
subprocess.run([java,'-Xcheck:jni','-Djava.library.path='+str(prebuilt),'-cp',str(classes),
    'com.hiro.ulike.H10OverlapTest'],check=True)

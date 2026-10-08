package com.hiro.ulike;

import android.graphics.Bitmap;
import android.graphics.ColorSpace;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.concurrent.CancellationException;

/** Real production adapters and stage ownership. Golden pixels are read from
 * the separately compiled published1960 oracle, never made by the candidate. */
public final class Facade1961Test {
    private static long assertions,pixels;
    private static native long failedUnmaps();
    private static int strongCases,singleCases,geometryCases,probes;
    private static boolean gpu,faultDone,cancelDone;
    private static void check(boolean yes,String why){assertions++;if(!yes)throw new AssertionError(why);}
    private static void released(String why){check(!GpuNoise1960.sessionBusy(),why+" owner closed");check(GpuNoise1960.retainedBytes()<=128L*1024*1024,why+" bounded reusable pool");GpuNoise1960.trimIdle();check(GpuNoise1960.retainedBytes()==0,why+" idle trim releases pool");}
    private static void awaitBackground()throws Exception {if(!gpu)return;long end=System.nanoTime()+10000000000L;while(GpuQualification1961.retainedBytes()>0&&System.nanoTime()<end)Thread.sleep(10);check(GpuQualification1961.retainedBytes()==0,"actual idle qualification probe drained");}
    private static int[] ints(DataInputStream in)throws Exception{int n=in.readInt();int[] a=new int[n];for(int i=0;i<n;i++)a[i]=in.readInt();return a;}
    private static Object field(Object o,String name)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
    private static void same(int[] expected,int[] actual,String why){check(actual!=null&&actual.length>=expected.length,why+" length");for(int i=0;i<expected.length;i++){assertions++;pixels++;if(expected[i]!=actual[i])throw new AssertionError(why+" pixel"+i+" expected "+Integer.toHexString(expected[i])+" actual "+Integer.toHexString(actual[i]));}}
    private static void strong(final int[] u,final int[][] b,int[] expected,int[] confidence,boolean injected)throws Exception{
        final int w=u[0],h=u[1];final int[] source=b[0],before=source.clone();
        long beforeDispatch=gpu?Native1960Test.faultFacts()[3]:0;
        if(injected)Native1960Test.setFault(2);
        StrongNoise1958.Model model=StrongNoise1958.prepare(new StrongNoise1958.Patches(){public void read(int[] a,int x,int y,int width,int height){for(int row=0;row<height;row++)System.arraycopy(source,(y+row)*w+x,a,row*width,width);}},w,h,u[8],u[9]!=0);
        int[][] maps=StrongNoise1958.gpuMaps1960(model);for(int i=0;i<3;i++)same(b[4+i],maps[i],"prepared GPU/CPU map");
        float[] evidence=StrongNoise1958.gpuEvidence1960(model);int[] raw=new int[evidence.length];for(int i=0;i<raw.length;i++)raw[i]=Float.floatToRawIntBits(evidence[i]);same(b[2],raw,"prepared region evidence");
        boolean plain=true;for(int i=0;i<b[3].length;i+=2)if(b[3][i]!=256||b[3][i+1]!=0){plain=false;break;}
        StrongNoise1958.Protection protection=plain?new GpuPolicy1960.Protection(null,true):new StrongNoise1958.Protection(){public int budgetQ8(int x,int y){return b[3][(y*w+x)*2];}public int detailQ8(int x,int y){return b[3][(y*w+x)*2+1];}};
        int[] actual=source.clone();StrongNoise1958.Workspace workspace=new StrongNoise1958.Workspace(true);
        GpuStrong1960.beginStage(model);
        try{if(injected)Native1960Test.setFault(2);StrongNoise1958.processRange(source,actual,w,h,0,h,0,h,0,u[8],u[9]!=0,model,protection,workspace);
            same(expected,actual,"strong production facade");same(confidence,workspace.confidence(),"strong facade confidence");awaitBackground();
        }finally{if(injected)Native1960Test.setFault(0);GpuStrong1960.endStage(model);workspace.close();}
        same(before,source,"strong immutable source");released("strong stage releases all GPU buffers");
        if(gpu&&Native1960Test.faultFacts()[3]>beforeDispatch)probes++;
        if(!cancelDone){int[] sentinel=new int[source.length];Arrays.fill(sentinel,0x76543210);GpuStrong1960.beginStage(model);boolean cancelled=false;Thread.currentThread().interrupt();
            try{StrongNoise1958.processRange(source,sentinel,w,h,0,h,0,h,0,u[8],u[9]!=0,model,protection);}catch(CancellationException stop){cancelled=true;}finally{GpuStrong1960.endStage(model);Thread.interrupted();}
            check(cancelled,"cancellation propagated");for(int v:sentinel)check(v==0x76543210,"cancellation cannot publish partial stage");released("cancelled stage closes");cancelDone=true;}
        strongCases++;
    }
    private static void single(final int[] u,final int[][] b,int[] expected)throws Exception{
        final int w=u[1],h=u[2];final int[] source=b[0];SingleNoise1955.Model model=SingleNoise1955.probe(new SingleNoise1955.Patches(){public void read(int[] a,int x,int y,int width,int height){for(int row=0;row<height;row++)System.arraycopy(source,(y+row)*w+x,a,row*width,width);}},w,h);
        SingleNoise1955.Protection p=new SingleNoise1955.Protection(){public int budgetQ8(int x,int y){return b[3][(y*w+x)*2];}public int detailQ8(int x,int y){return b[3][(y*w+x)*2+1];}};
        int[] actual=source.clone();SingleNoise1955.processRange(source,actual,w,h,0,h,0,h,0,u[13],u[14]!=0,model,p);same(expected,actual,"single production unsupported precision fallback");awaitBackground();released("single release");singleCases++;
    }
    private static void geometry(final int[] u,int[][] b,final int[] expected)throws Exception{
        Bitmap input=Bitmap.from(u[1],u[2],b[0],Bitmap.Config.ARGB_8888,true);final int[] calls={0};
        long beforeDispatch=gpu?Native1960Test.faultFacts()[3]:0;
        Bitmap actual=GpuGeometry1960.resample(input,u[3],u[4],u[5],new GpuGeometry1960.Cpu(){public Bitmap run(){calls[0]++;return Bitmap.from(u[4],u[5],expected,Bitmap.Config.ARGB_8888,true);}});
        same(expected,actual.snapshot(),"geometry production facade");same(b[0],input.snapshot(),"geometry original immutable");awaitBackground();check(calls[0]>=1,"qualification publishes independent CPU reference");
        if(gpu&&Native1960Test.faultFacts()[3]>beforeDispatch)probes++;
        if(actual!=input)actual.recycle();input.recycle();released("geometry release");geometryCases++;
    }
    private static void unsupportedGeometry(){
        for(int kind=0;kind<4;kind++){Bitmap.Config config=kind==0?Bitmap.Config.RGBA_F16:Bitmap.Config.ARGB_8888;final Bitmap b=Bitmap.from(3,5,new int[15],config,true);if(kind==1)b.setColorSpaceForTest(new ColorSpace(false));if(kind==2)b.setGainmapForTest(true);
            final int[] called={0};Bitmap result=GpuGeometry1960.resample(b,90,5,3,new GpuGeometry1960.Cpu(){public Bitmap run(){called[0]++;return b;}});
            check(result==b&&called[0]==1,"unsupported format/color/HDR/alpha CPU preserved "+kind);check(!b.isRecycled(),"fallback source alive");b.recycle();}
    }
    public static void main(String[] args)throws Exception{
        gpu=Boolean.parseBoolean(args[1]);boolean fault=args.length>2;
        check(GpuNoise1960.available()==gpu,"expected native capability");check(!GpuNoise1960.workspaceFits(Long.MAX_VALUE),"oversized workspace rejected");
        try(DataInputStream in=new DataInputStream(new FileInputStream(args[0]))){check(in.readInt()==1960001,"frozen record version");for(;;){String name=in.readUTF();if(name.isEmpty())break;
            int shader=in.readInt();int[] u=ints(in);in.readInt();int nb=in.readInt();int[][] b=new int[nb][];for(int i=0;i<nb;i++)b[i]=ints(in);int[] expected=ints(in),confidence=ints(in);
            if(shader==0&&u[10]==3){if(fault&&(faultDone||u[0]<65||u[8]<3))continue;strong(u,b,expected,confidence,fault);if(fault)faultDone=true;}
            else if(shader==1&&!fault)single(u,b,expected);
            else if(shader==3&&!fault)geometry(u,b,expected);
        }}
        unsupportedGeometry();awaitBackground();if(fault)check(failedUnmaps()>0,"actual private candidate unmap failed before publication");check(!gpu||probes>0,"production facade issued actual compute work");
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"pixels\":"+pixels+",\"strongCases\":"+strongCases+",\"singleCases\":"+singleCases+",\"geometryCases\":"+geometryCases+",\"actualGpuStageProbes\":"+probes+",\"nativeAvailable\":"+gpu+",\"lateFailureFallback\":"+fault+",\"actualInjectedFailedUnmaps\":"+(fault?failedUnmaps():0)+",\"physicalAndroidTested\":false}");
    }
}

package com.hiro.ulike;

import android.graphics.Bitmap;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Random;

/** .62 and candidate execute independently against identical owned fixtures. */
public final class PipelineAudit1963Test {
    private static long assertions;
    private static int masks,pipelines;
    private static void check(boolean ok,String reason){assertions++;if(!ok)throw new AssertionError(reason);}
    private static void rejects(Runnable action,String reason){boolean rejected=false;try{action.run();}catch(IllegalArgumentException expected){rejected=true;}check(rejected,reason);}
    private static int[] pixels(int w,int h,int kind) {
        Random rng=new Random(1963L+kind);int[] p=new int[w*h];
        for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
            int v=60+(x/7+y/11)%23,n=rng.nextInt(13)-6,c=rng.nextInt(9)-4;
            p[y*w+x]=0xff000000|((v+n+c)<<16)|((v+n)<<8)|(v+n-c);
            if(kind==1&&x%11<4)p[y*w+x]=0xff164095;
            if(kind==2&&(x+3*y)%19==0)p[y*w+x]&=0x7fffffff;
        }
        return p;
    }
    private static void normal(String path)throws Exception {
        DataOutputStream out=new DataOutputStream(new FileOutputStream(path));
        try {
            for(int type=0;type<7;type++)for(int kind=0;kind<3;kind++) {
                int w=96,h=112;int[] p=pixels(w,h,kind);
                FaceRegions1934Pixels.Anchor good=new FaceRegions1934Pixels.Anchor(48,38,22,0,0,.9f);
                FaceRegions1934Pixels.Anchor roll=new FaceRegions1934Pixels.Anchor(42,40,19,22,18,.7f);
                FaceRegions1934Pixels.Anchor[] faces=type==0?new FaceRegions1934Pixels.Anchor[]{good}:
                    type==1?new FaceRegions1934Pixels.Anchor[]{roll}:type==2?new FaceRegions1934Pixels.Anchor[]{good,roll}:
                    type==3?new FaceRegions1934Pixels.Anchor[]{null,good}:type==4?new FaceRegions1934Pixels.Anchor[0]:
                    type==5?new FaceRegions1934Pixels.Anchor[]{new FaceRegions1934Pixels.Anchor(Float.NaN,38,22,0,0,.9f)}:null;
                FaceRegions1934Pixels.Raster mask=FaceRegions1934Pixels.buildCpu1961(p,w,h,faces);
                out.writeBoolean(mask.reliable);out.writeInt(mask.skin.length);out.write(mask.skin);out.write(mask.detail);
                for(int i=0;i<p.length;i++)check(mask.skin[i]==0||(mask.detail[i]&255)<255,"mask bound");masks++;
            }
            StringBuilder counts=new StringBuilder();
            for(int mode=0;mode<6;mode++) {
                final int w=40,h=48;int[] p=pixels(w,h,mode%3);Bitmap input=Bitmap.from(w,h,p,Bitmap.Config.ARGB_8888,true);
                input.setHasAlpha(mode%3==2);input.setPremultiplied(false);
                PhotoDetail.REQUEST.set(new PhotoDetail.Settings(true,3,true,4,true,true,true));
                AsyncSave1935.CAPTURED.remove();AsyncSave1935.COLOUR.remove();
                ChromaPipeline177.enabled=(mode&1)!=0;
                int turn=mode<2?0:90,ow=mode<4?40:32,oh=mode<4?48:36;
                SaveQuality2.SIZE.set(new int[]{ow,oh});SaveQuality2.FALLBACK.set(0);
                Bitmap.spatialProbeReads=0;
                Bitmap result=QualityPipeline1932.normalize(input,turn,false);
                check(SaveQuality2.FALLBACK.get()==0,"normal audit must stay in quality route");
                check(!input.isRecycled()&&Arrays.equals(input.snapshot(),p),"normal immutable source");
                out.writeInt(result.getWidth());out.writeInt(result.getHeight());int[] saved=result.snapshot();out.writeInt(saved.length);for(int value:saved)out.writeInt(value);
                QualityPipeline1932.applyDetail(result,input,PhotoDetail.REQUEST.get());check(Arrays.equals(saved,result.snapshot()),"completion suppresses duplicate filtering");
                if(mode>0)counts.append(',');counts.append(Bitmap.spatialProbeReads);
                if(result!=input)result.recycle();input.recycle();pipelines++;
            }
            System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"face_cases\":"+masks+",\"pipeline_cases\":"+pipelines+",\"spatial_probe_reads\":["+counts+"]}");
        } finally {out.close();}
    }
    private static void invalid() {
        final StrongNoise1958.Model model58=StrongNoise1958.prepareJava(new StrongNoise1958.Patches(){public void read(int[] p,int x,int y,int w,int h){Arrays.fill(p,0xff807060);}},1,1,0,false);
        final StrongNoise1957.Model model57=StrongNoise1957.prepareJava(new StrongNoise1957.Patches(){public void read(int[] p,int x,int y,int w,int h){Arrays.fill(p,0xff807060);}},1,1,0,false);
        final int[] input=new int[128],output=new int[128];Arrays.fill(input,0xff807060);Arrays.fill(output,0x12345678);
        rejects(new Runnable(){public void run(){StrongNoise1958.processJavaRange(input,output,1,128,64,128,0,128,Integer.MAX_VALUE,0,false,model58,null);}},"overflowing active strip origin rejected before copy");
        rejects(new Runnable(){public void run(){StrongNoise1957.processJavaRange(input,output,1,128,64,128,0,128,Integer.MAX_VALUE,0,false,model57,null);}},"overflowing compatibility strip origin rejected");
        rejects(new Runnable(){public void run(){model58.smoothingQ8(input,1,128,0,64,0,128,Integer.MAX_VALUE,new float[2]);}},"overflowing confidence origin rejected before clean shortcut");
        for(int value:output)check(value==0x12345678,"invalid origin has no output commit");
        StrongNoise1958.processJavaRange(input,output,1,3,1,2,1,2,-1,0,false,model58,null);
        check(output[0]==0x12345678&&output[1]==input[1]&&output[2]==0x12345678,"valid negative local origin retains bounded copy");
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"overflow_regressions\":3,\"transactional_rejection\":true}");
    }
    @SuppressWarnings("removal")
    private static void deniedNative() {
        System.setSecurityManager(new SecurityManager(){public void checkPermission(java.security.Permission p){}public void checkLink(String library){throw new SecurityException("controlled optional native denial");}});
        try {
            check(!SingleNoise1955.nativeAvailable(),"denied single NR native uses CPU fallback");
            check(!StrongNoise1957.nativeAvailable(),"denied compatibility NR native uses CPU fallback");
            check(!StrongNoise1958.nativeAvailable(),"denied active NR native uses CPU fallback");
            check(!SingleNoise1955.nativeAvailable()&&!StrongNoise1957.nativeAvailable()&&!StrongNoise1958.nativeAvailable(),"denial remains stable without retry");
        } finally {System.setSecurityManager(null);}
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"native_denial_cpu_fallback\":true}");
    }
    public static void main(String[] args)throws Exception {
        if("invalid".equals(args[0]))invalid();else if("denied".equals(args[0]))deniedNative();else normal(args[0]);
    }
}

package com.hiro.ulike;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/** Synthetic sensor-input checks of the production NV21 kernel, without Android. */
public final class FusionPixels1933Test {
    static int assertions;
    static final Map<String,Integer> cases=new LinkedHashMap<String,Integer>();
    static final Map<String,Long> metrics=new LinkedHashMap<String,Long>();
    interface Case { void run() throws Exception; }
    static void check(boolean b,String message){assertions++;if(!b)throw new AssertionError(message);}
    static void test(String name,Case body)throws Exception{
        int before=assertions;body.run();cases.put(name,assertions-before);
    }
    static long[] times(int n){long[] a=new long[n];for(int i=0;i<n;i++)a[i]=1000000000L+i*33000000L;return a;}
    static FusionPixels1933.Result fuse(byte[][] frames,int w,int h,int level,boolean night,int workers){
        return FusionPixels1933.fuse(frames,w,h,times(frames.length),level,night,workers);
    }
    static int clip(double a){return Math.max(0,Math.min(255,(int)Math.round(a)));}
    static int u(byte a){return a&255;}
    // Fixed analytic chart: smooth shading, several edges, fine lines and uneven texture.
    static double chart(double x,double y,int kind){
        if(kind==0)return 82;
        if(kind==1)return 92+20*Math.sin(x*.081)+16*Math.cos(y*.063)+12*Math.sin((x+y)*.153)+
            ((x>76&&x<114&&y>38&&y<166)?44:0)+((x>180&&y>100)?-22:0);
        if(kind==2)return 48+14*Math.sin(x*.072)+11*Math.cos(y*.091)+((x>120&&x<145)?42:0);
        if(kind==3)return (x<128?44:187)+((y>66&&y<69)?30:0);
        return 16;
    }
    static byte[] frame(int w,int h,int kind,double dx,double dy,double noise,long seed){
        Random rng=new Random(seed);byte[] a=new byte[w*h*3/2];
        for(int y=0;y<h;y++)for(int x=0;x<w;x++)
            a[y*w+x]=(byte)clip(chart(x-dx,y-dy,kind)+rng.nextGaussian()*noise);
        for(int y=0;y<h/2;y++)for(int x=0;x<w/2;x++){
            int at=w*h+y*w+x*2;
            a[at]=(byte)clip(153+(kind==1?8*Math.sin((x-dx/2)*.071):0)+rng.nextGaussian()*noise*.65);
            a[at+1]=(byte)clip(104+(kind==1?6*Math.cos((y-dy/2)*.09):0)+rng.nextGaussian()*noise*.65);
        }return a;
    }
    static byte[][] captures(int w,int h,int kind,double sigma,int count){
        byte[][] f=new byte[count][];for(int i=0;i<count;i++)f[i]=frame(w,h,kind,0,0,sigma,12341+i*323);return f;
    }
    static double mse(byte[] actual,byte[] truth,int w,int h,int margin){
        double sum=0;int n=0;for(int y=margin;y<h-margin;y++)for(int x=margin;x<w-margin;x++){
            int d=u(actual[y*w+x])-u(truth[y*w+x]);sum+=d*d;n++;}return sum/n;
    }
    static double uvMse(byte[] actual,byte[] truth,int w,int h,int margin){
        double sum=0;int n=0;for(int y=margin;y<h/2-margin;y++)for(int x=margin*2;x<w-margin*2;x++){
            int i=w*h+y*w+x,d=u(actual[i])-u(truth[i]);sum+=d*d;n++;}return sum/n;
    }
    static byte[][] cloneFrames(byte[][] frames){byte[][] copy=new byte[frames.length][];
        for(int i=0;i<copy.length;i++)copy[i]=frames[i]==null?null:frames[i].clone();return copy;}
    static void unchanged(byte[][] a,byte[][] b){for(int i=0;i<a.length;i++)check(Arrays.equals(a[i],b[i]),"source mutated "+i);}
    static void addObject(byte[] a,int w,int h,int left,int top,int right,int bottom,int yValue,int v,int u){
        for(int y=top;y<bottom;y++)for(int x=left;x<right;x++)a[y*w+x]=(byte)yValue;
        for(int y=top/2;y<bottom/2;y++)for(int x=left&~1;x<right;x+=2){int at=w*h+y*w+x;
            a[at]=(byte)v;a[at+1]=(byte)u;}
    }
    static void exactBox(byte[] a,byte[] b,int w,int left,int top,int right,int bottom){
        for(int y=top;y<bottom;y++)for(int x=left;x<right;x++)check(a[y*w+x]==b[y*w+x],"moving region changed "+x+","+y);
    }
    public static void main(String[] ignored)throws Exception {
        test("off_invalid_and_reference_ownership",()->{
            int w=128,h=96;byte[][] f=captures(w,h,1,7,3);byte[][] copy=cloneFrames(f);
            FusionPixels1933.Result off=fuse(f,w,h,0,false,4);
            check(off.nv21==f[0]&&!off.actualFusion&&off.acceptedCount==1,"OFF");
            check(fuse(f,w,h,-7,false,4).nv21==f[0],"negative level");
            check(fuse(new byte[][]{f[0]},w,h,4,false,4).nv21==f[0],"single frame");
            check(FusionPixels1933.fuse(f,w,h,null,4,false,4).nv21==f[0],"missing timestamps");
            check(FusionPixels1933.fuse(f,w,h,new long[]{0,1,2},4,false,4).nv21==f[0],"invalid reference timestamp");
            check(fuse(new byte[][]{f[0],null,new byte[3]},w,h,4,false,4).nv21==f[0],"invalid second frames");
            boolean rejected=false;try{fuse(f,w-1,h,4,false,1);}catch(IllegalArgumentException e){rejected=true;}
            check(rejected,"odd dimension reject");
            rejected=false;try{FusionPixels1933.fuse(new byte[0][],w,h,times(0),4,false,1);}catch(IllegalArgumentException e){rejected=true;}
            check(rejected,"missing reference reject");
            unchanged(f,copy);
        });
        test("real_distinct_capture_requirement",()->{
            int w=160,h=128;byte[][] f=captures(w,h,1,8,3);
            check(fuse(new byte[][]{f[0],f[0],f[0].clone()},w,h,4,false,4).acceptedCount==1,"synthetic duplicates rejected");
            check(FusionPixels1933.fuse(f,w,h,new long[]{1,1,1},4,false,4).acceptedCount==1,"duplicate timestamps rejected");
            FusionPixels1933.Result result=fuse(new byte[][]{f[0],f[1],f[1].clone()},w,h,4,false,1);
            check(result.acceptedCount==2,"duplicate extra capture counted twice");
            FusionPixels1933.Motion motion=FusionPixels1933.probeMotion(f[0],f[0].clone(),w,h,800,20000000);
            check(motion.duplicate&&motion.score==0,"duplicate probe tagged");
        });
        test("same_exposure_metadata_gate",()->{
            int w=128,h=96;byte[][] f=captures(w,h,1,8,3);long[] ts=times(3);
            check(FusionPixels1933.fuse(f,w,h,ts,new long[]{100,200,200},new int[]{800,400,400},4,true,4).acceptedCount==1,"HDR exposures rejected");
            check(FusionPixels1933.fuse(f,w,h,ts,new long[]{100,100,100},new int[]{800,1600,1600},4,true,4).acceptedCount==1,"ISO mismatch rejected");
            check(FusionPixels1933.fuse(f,w,h,ts,new long[]{100,101,100},new int[]{800,810,800},4,true,4).actualFusion,"minor metadata rounding accepted");
            check(FusionPixels1933.fuse(f,w,h,ts,new long[]{100},new int[]{800,800,800},4,true,4).acceptedCount==1,"short metadata rejected");
        });
        test("static_noise_and_nv21_chroma_gain",()->{
            int w=256,h=192;byte[][] f=captures(w,h,1,9,4);byte[][] copies=cloneFrames(f);
            byte[] clean=frame(w,h,1,0,0,0,0);FusionPixels1933.Result r=fuse(f,w,h,4,false,4);
            double before=mse(f[0],clean,w,h,12),after=mse(r.nv21,clean,w,h,12);
            double cbefore=uvMse(f[0],clean,w,h,8),cafter=uvMse(r.nv21,clean,w,h,8);
            metrics.put("static_y_mse_ratio_ppm",Math.round(after/before*1000000));
            metrics.put("static_uv_mse_ratio_ppm",Math.round(cafter/cbefore*1000000));
            check(r.actualFusion&&r.acceptedCount==4,"all real static frames accepted");
            check(after<before*.75,"static noise gain "+after+"/"+before);
            check(cafter<cbefore*.75,"chroma noise gain "+cafter+"/"+cbefore);
            check(r.referenceIndex==0&&r.nv21.length==f[0].length&&r.nv21!=f[0],"output ownership/geometry");
            check(r.fusedPixels>0&&r.fusedPixels<=(long)w*h,"fused count bounded");unchanged(f,copies);
        });
        test("flat_noise_and_level_bounds",()->{
            int w=256,h=192;byte[][] f=captures(w,h,0,10,4);byte[] clean=frame(w,h,0,0,0,0,0);
            double before=mse(f[0],clean,w,h,16),previous=before;
            for(int level=1;level<=4;level++){
                FusionPixels1933.Result r=fuse(f,w,h,level,false,2);double after=mse(r.nv21,clean,w,h,16);
                check(r.actualFusion,"flat scene fusion");check(after<previous,"strength ordering "+level);previous=after;
            }
            metrics.put("flat_y_mse_ratio_ppm",Math.round(previous/before*1000000));
            check(Arrays.equals(fuse(f,w,h,4,false,1).nv21,fuse(f,w,h,99,false,1).nv21),"max level capped");
            FusionPixels1933.Motion probe=FusionPixels1933.probeMotion(f[0],f[1],w,h,1600,50000000);
            check(!probe.reliable&&probe.score>=.45f,"flat probe uncertainty conservative");
        });
        test("integer_camera_translation",()->{
            int w=320,h=256;byte[] ref=frame(w,h,1,0,0,7,45),shift=frame(w,h,1,5,-3,7,76);
            byte[] clean=frame(w,h,1,0,0,0,0);byte[][] f={ref,shift,frame(w,h,1,-3,2,7,777)};
            FusionPixels1933.Motion p=FusionPixels1933.probeMotion(ref,shift,w,h,800,20000000);
            check(p.reliable&&!p.duplicate,"textured shift reliable");
            check(Math.abs(p.globalShiftPixels-Math.sqrt(34))<1.1,"translation recovered "+p.globalShiftPixels);
            FusionPixels1933.Result r=fuse(f,w,h,4,false,4);
            double before=mse(ref,clean,w,h,20),after=mse(r.nv21,clean,w,h,20);
            metrics.put("translation_y_mse_ratio_ppm",Math.round(after/before*1000000));
            check(r.actualFusion&&after<before*.85,"aligned shift improves error "+after+"/"+before);
        });
        test("fractional_translation_and_warm_hue",()->{
            int w=320,h=256;byte[] ref=frame(w,h,1,0,0,6,431),b=frame(w,h,1,2.4,-1.6,6,751);
            byte[] clean=frame(w,h,1,0,0,0,0);FusionPixels1933.Result r=fuse(new byte[][]{ref,b},w,h,4,false,3);
            check(r.actualFusion,"subpixel accepted");check(mse(r.nv21,clean,w,h,20)<mse(ref,clean,w,h,20)*.93,"subpixel improves error");
            double mv=0,mu=0;int n=0;for(int i=w*h;i<r.nv21.length;i+=2){mv+=u(r.nv21[i])-u(clean[i]);mu+=u(r.nv21[i+1])-u(clean[i+1]);n++;}
            check(Math.abs(mv/n)<.7&&Math.abs(mu/n)<.7,"NV21 VU warm hue preservation");
        });
        test("foreground_motion_reference_and_background_gain",()->{
            int w=320,h=256;byte[][] f=captures(w,h,2,5,3);byte[] clean=frame(w,h,2,0,0,0,0);
            addObject(f[0],w,h,72,80,100,148,160,170,85);
            addObject(f[1],w,h,112,80,140,148,160,170,85);addObject(f[2],w,h,152,80,180,148,160,170,85);
            FusionPixels1933.Result r=fuse(f,w,h,4,true,4);
            check(r.actualFusion,"static background still fused");
            exactBox(r.nv21,f[0],w,74,82,98,146);
            FusionPixels1933.Result two=fuse(new byte[][]{f[0],f[1]},w,h,4,true,4);
            exactBox(two.nv21,f[0],w,114,82,138,146);
            // The third frame sees the original background here and may safely
            // reduce its noise; the foreground from frame one must not leak in.
            for(int y=82;y<146;y++)for(int x=114;x<138;x++)
                check(u(r.nv21[y*w+x])<u(f[0][y*w+x])+15,"no secondary foreground ghost");
        });
        test("large_motion_scene_flash_and_colour_mismatch",()->{
            int w=256,h=192;byte[][] f=captures(w,h,1,4,2);
            byte[] flash=f[1].clone();for(int i=0;i<w*h;i++)flash[i]=(byte)clip(u(flash[i])+35);
            check(fuse(new byte[][]{f[0],flash},w,h,4,true,4).nv21==f[0],"flash rejected");
            byte[] changed=frame(w,h,0,0,0,3,666);
            check(fuse(new byte[][]{f[0],changed},w,h,4,true,4).nv21==f[0],"scene change rejected");
            byte[] colour=f[1].clone();for(int i=w*h;i<colour.length;i++)colour[i]=(byte)clip(u(colour[i])+25);
            check(fuse(new byte[][]{f[0],colour},w,h,4,true,4).nv21==f[0],"WB change rejected");
            check(FusionPixels1933.probeMotion(f[0],flash,w,h,800,10000000).score==1,"unsafe probe conservative");
        });
        test("small_local_warp_and_uncertain_rotation",()->{
            int w=384,h=256;byte[] ref=frame(w,h,1,0,0,6,919),warp=frame(w,h,1,0,0,6,920);
            byte[] clean=frame(w,h,1,0,0,0,0);Random random=new Random(920);
            for(int y=0;y<h;y++)for(int x=0;x<w;x++)
                warp[y*w+x]=(byte)clip(chart(x-(y-h*.5)*.008,y,1)+random.nextGaussian()*6);
            FusionPixels1933.Result r=fuse(new byte[][]{ref,warp},w,h,4,false,4);
            check(r.actualFusion,"small smooth warp permits selective fusion");
            check(mse(r.nv21,clean,w,h,20)<mse(ref,clean,w,h,20)*.96,"local warp error improves");
            byte[] rotated=warp.clone();double angle=.08;
            for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
                double cx=x-w*.5,cy=y-h*.5;
                rotated[y*w+x]=(byte)clip(chart(cx*Math.cos(angle)-cy*Math.sin(angle)+w*.5,
                    cx*Math.sin(angle)+cy*Math.cos(angle)+h*.5,1)+random.nextGaussian()*6);
            }
            FusionPixels1933.Result unsafe=fuse(new byte[][]{ref,rotated},w,h,4,false,4);
            check(mse(unsafe.nv21,clean,w,h,20)<=mse(ref,clean,w,h,20)*1.1,"large unmodelled rotation safely limited");
        });
        test("night_low_contrast_motion_and_vu_only_motion",()->{
            int w=320,h=256;byte[][] f=captures(w,h,2,10,2);
            // A broad, dim moving object has lower contrast than the daylight test.
            for(int y=64;y<160;y++)for(int x=66;x<110;x++)f[0][y*w+x]=(byte)clip(u(f[0][y*w+x])+32);
            for(int y=64;y<160;y++)for(int x=166;x<210;x++)f[1][y*w+x]=(byte)clip(u(f[1][y*w+x])+32);
            FusionPixels1933.Result r=fuse(f,w,h,4,true,4);double difference=0;int n=0;
            for(int y=68;y<156;y++)for(int x=70;x<106;x++){difference+=Math.abs(u(r.nv21[y*w+x])-u(f[0][y*w+x]));n++;}
            check(difference/n<2,"dark moving foreground stays near reference "+difference/n);
            byte[][] colour=captures(w,h,1,2,2);
            for(int y=48;y<80;y++)for(int x=84;x<132;x+=2){int p=w*h+y*w+x;colour[1][p]=(byte)190;colour[1][p+1]=(byte)60;}
            FusionPixels1933.Result cr=fuse(colour,w,h,4,false,2);
            check(cr.actualFusion,"colour-only motion still permits background fusion");
            for(int y=51;y<77;y++)for(int x=88;x<128;x++)
                check(cr.nv21[w*h+y*w+x]==colour[0][w*h+y*w+x],"moving VU preserves reference");
        });
        test("edges_lines_and_no_overshoot",()->{
            int w=256,h=192;byte[][] f=captures(w,h,3,1,3);byte[] clean=frame(w,h,3,0,0,0,0);
            FusionPixels1933.Result r=fuse(f,w,h,4,false,4);check(r.actualFusion,"low-noise chart accepted");
            double loss=0;int n=0;for(int y=20;y<h-20;y++)for(int x=125;x<=130;x++){
                loss+=Math.abs(u(r.nv21[y*w+x])-u(clean[y*w+x]));n++;}
            check(loss/n<1.5,"sharp step retained without halo "+loss/n);
            for(int y=66;y<69;y++)for(int x=20;x<110;x++)check(Math.abs(u(r.nv21[y*w+x])-u(clean[y*w+x]))<5,"thin line retained");
        });
        test("worker_equivalence_seams_and_immutable_inputs",()->{
            int w=258,h=226;byte[][] f=captures(w,h,1,8,4),copy=cloneFrames(f);
            FusionPixels1933.Result one=fuse(f,w,h,4,true,1),four=fuse(f,w,h,4,true,4);
            check(one.actualFusion,"seam test actually fuses");
            check(Arrays.equals(one.nv21,four.nv21),"parallel whole image output identical");
            check(one.acceptedCount==four.acceptedCount&&one.fusedPixels==four.fusedPixels,"parallel evidence identical");
            check(Arrays.equals(one.nv21,fuse(f,w,h,4,true,32).nv21),"worker cap");
            for(int y=32;y<h;y+=32)for(int x=0;x<w;x++)check(one.nv21[y*w+x]==four.nv21[y*w+x],"row seam");
            unchanged(f,copy);
        });
        test("cancellation_and_probe_immutability",()->{
            int w=160,h=128;byte[][] f=captures(w,h,1,6,3),copy=cloneFrames(f);
            Thread.currentThread().interrupt();FusionPixels1933.Result r=fuse(f,w,h,4,false,4);
            check(r.cancelled&&r.nv21==f[0]&&!r.actualFusion,"pre-cancel fallback");
            check(Thread.interrupted(),"interruption retained");
            FusionPixels1933.probeMotion(f[0],f[1],w,h,800,10000000);unchanged(f,copy);
            FusionPixels1933.Motion invalid=FusionPixels1933.probeMotion(f[0],null,w,h,800,10000000);
            check(!invalid.reliable&&invalid.score==1,"missing probe conservative");
        });
        StringBuilder json=new StringBuilder("{\"status\":\"passed\",\"assertions\":").append(assertions).append(",\"scenarios\":{");
        boolean first=true;for(Map.Entry<String,Integer> e:cases.entrySet()){if(!first)json.append(',');first=false;
            json.append('"').append(e.getKey()).append("\":").append(e.getValue());}
        json.append("},\"metrics\":{");first=true;for(Map.Entry<String,Long> e:metrics.entrySet()){if(!first)json.append(',');first=false;
            json.append('"').append(e.getKey()).append("\":").append(e.getValue());}
        System.out.println(json.append("}}"));
    }
}

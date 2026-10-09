package com.hiro.ulike;

import java.util.Arrays;

/** Same-photo model analysis reuse. Captures every original analysis patch while
 * it is already being read; neither qualification nor reuse calls an external
 * source twice or stores a model for another photograph. */
final class CpuModel1978 {
    private CpuModel1978() {}
    static String spectralKey(int width,int height){return "cpu-model1978-spectral-exact2-time5-v1:"+width+":"+height;}
    static String regionKey(int width,int height){return "cpu-model1978-region-exact2-time5-v1:"+width+":"+height;}
    static void evidence(int[] src,final int width,final int height,float[] dst,int offset) {
        final String key=spectralKey(width,height);boolean reused=CpuExact1978.enabled(key);
        int nx=Math.min(17,Math.max(1,width/16)),ny=Math.min(17,Math.max(1,height/16));
        final long retained=4L*nx*ny*64+4096,peak=128L*1024;
        int[] capture=null;
        if(!reused&&width>=8&&height>=8&&CpuExact1978.canCapture(key,retained,peak))
            try{capture=new int[nx*ny*64];}catch(OutOfMemoryError optional){}
        StrongNoise1958.evidenceCpu1978(src,width,height,dst,offset,reused,capture);
        final int[] patches=capture;
        if(patches!=null)CpuExact1978.offer(key,retained,peak,new CpuExact1978.Factory(){
            public CpuExact1978.Work create(){return new Spectral(patches,width,height);}
        });
    }
    static float[] regions(StrongNoise1958.Patches source,final int width,final int height,final float[] ev) {
        final String key=regionKey(width,height);boolean reused=CpuExact1978.enabled(key);
        int pw=Math.min(32,width),ph=Math.min(32,height),rw=(width+63)/64,rh=(height+63)/64;
        long count=(long)rw*rh*pw*ph;final long retained=4L*count+4096,peak=48L*rw*rh+128L*1024;
        int[] capture=null;
        if(!reused&&count<=Integer.MAX_VALUE&&CpuExact1978.canCapture(key,retained,peak))
            try{capture=new int[(int)count];}catch(OutOfMemoryError optional){}
        float[] result=StrongNoise1958.regionsSnapshot1978(source,width,height,ev,reused,capture);
        final int[] patches=capture;
        if(patches!=null)CpuExact1978.offer(key,retained,peak,new CpuExact1978.Factory(){
            public CpuExact1978.Work create(){return new Regions(patches,width,height,Arrays.copyOf(ev,16));}
        });
        return result;
    }
    private static final class Spectral implements CpuExact1978.Work {
        int[] patches;final int width,height;
        Spectral(int[] patches,int width,int height){this.patches=patches;this.width=width;this.height=height;}
        public Object run(boolean reused){return patches==null?null:StrongNoise1958.spectralSnapshot1978(patches,width,height,reused);}
        public void close(){patches=null;}
    }
    private static final class Regions implements CpuExact1978.Work {
        int[] patches;float[] ev;final int width,height;
        Regions(int[] patches,int width,int height,float[] ev){this.patches=patches;this.width=width;this.height=height;this.ev=ev;}
        public Object run(boolean reused) {
            if(patches==null)return null;
            final int pw=Math.min(32,width),ph=Math.min(32,height),rw=(width+63)/64;
            StrongNoise1958.Patches reader=new StrongNoise1958.Patches(){int at;
                public void read(int[] out,int x,int y,int w,int h) {
                    int gx=at%rw,gy=at/rw;
                    int px=Math.max(0,Math.min(width-pw,gx*64+32-pw/2));
                    int py=Math.max(0,Math.min(height-ph,gy*64+32-ph/2));
                    if(w!=pw||h!=ph||x!=px||y!=py||out.length<pw*ph||(long)(at+1)*pw*ph>patches.length)
                        throw new IllegalStateException("CPU region snapshot traversal");
                    System.arraycopy(patches,at++*pw*ph,out,0,pw*ph);
                }
            };
            return StrongNoise1958.regionsSnapshot1978(reader,width,height,ev,reused,null);
        }
        public void close(){patches=null;ev=null;}
    }
}

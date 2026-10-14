package com.hiro.ulike;
import java.util.*;
public final class CpuMasks1976Test {
    static long assertions;static void check(boolean yes,String why){assertions++;if(!yes)throw new AssertionError(why);}
    static void data(GpuPolicy1960.PolicyData a,GpuPolicy1975Reference.PolicyData b){
        check(a.count==b.count&&Arrays.equals(a.masks,b.masks)&&Arrays.equals(a.u,b.u)&&Arrays.equals(a.grid,b.grid)&&Arrays.equals(a.f,b.f),"frozen75 descriptor exact");
    }
    static QualityPixels1932.Plan plan(QualityPixels1932.RegionMask mask){return QualityPixels1932.plan(new QualityPixels1932.NoiseStats(2.7f,3.8f,121f,.2f,100),200,10000000L,2,.83f,4,4,true,true,1f).withFaceRegions(mask);}
    static void sample() {
        Random random=new Random(197609);int[][] sizes={{1,1},{2,3},{7,9},{31,47},{131,127}};
        for(int[] size:sizes)for(int turn:new int[]{0,90,180,270})for(int mode=0;mode<5;mode++) {
            int w=size[0],h=size[1],rw=Math.max(1,w/3),rh=Math.max(1,h/3);
            byte[] skin=new byte[rw*rh],detail=new byte[skin.length];random.nextBytes(skin);random.nextBytes(detail);
            if(mode==1)Arrays.fill(skin,(byte)255);if(mode==2)Arrays.fill(detail,(byte)0);if(mode==3)Arrays.fill(detail,(byte)255);
            FaceRegions1934.Mask mask=FaceRegions1934.uprightRaster(w,h,rw,rh,skin.clone(),detail.clone(),turn);
            FaceRegions1975Reference.Mask old=FaceRegions1975Reference.uprightRaster(w,h,rw,rh,skin.clone(),detail.clone(),turn);
            for(int rotation:new int[]{0,90,180,270,-90,450}) {
                int ow=w+mode*3,oh=h+mode*2;
                FaceRegions1934.Mask a=mask.resample(rotation,ow,oh);
                FaceRegions1975Reference.Mask b=old.resample(rotation,ow,oh);
                for(int y=-2;y<oh+2;y++)for(int x=-2;x<ow+2;x++) {
                    int pair=a.samplePair1976(x,y);
                    check((pair&65535)==b.skinQ8(x,y),"paired skin exact affine/bound/round");
                    check((pair>>>16)==b.detailQ8(x,y),"paired detail exact affine/bound/round");
                }
                QualityPixels1932.Plan p=plan(a),q=plan(b);int first=Math.min(3,oh-1),rows=oh-first;
                for(boolean strong:new boolean[]{false,true})for(boolean descriptor:new boolean[]{false,true}) {
                    int count=ow*rows*2;int[] actual=new int[count+11],expected=new int[count+11];Arrays.fill(actual,789);Arrays.fill(expected,789);
                    GpuPolicy1960.Protection na=new GpuPolicy1960.Protection(p,strong).freezePolicy1964(ow,rows,first,actual,descriptor);
                    GpuPolicy1975Reference.Protection ob=new GpuPolicy1975Reference.Protection(q,strong).freezePolicy1964(ow,rows,first,expected,descriptor);
                    check(Arrays.equals(actual,expected),"frozen75 CPU oracle and untouched tail");data(na.data(ow,rows,first),ob.data(ow,rows,first));
                    PolicyCache1945 lease=PolicyCache1945.borrow(p,ow,first,oh);
                    try{if(lease!=null){int[] leased=new int[count];new GpuPolicy1960.Protection(p.withPolicyCache(lease),strong).freezePolicy1964(ow,rows,first,leased,false);check(Arrays.equals(leased,Arrays.copyOf(expected,count)),"same immutable result within existing policy lease");}}
                    finally{if(lease!=null)lease.close();}
                }
                GpuPolicy1960.PolicyData all=GpuPolicy1960.sourcePlan(p,ow,oh,0,true);
                GpuPolicy1960.PolicyData slice=GpuPolicy1960.slice1976(all,first,rows);
                data(slice,GpuPolicy1975Reference.sourcePlan(q,ow,rows,first,true));
                check(slice.masks!=all.masks&&slice.u!=all.u&&slice.grid!=all.grid&&slice.f!=all.f,"private slice no mutable aliases");
            }
        }
        FaceRegions1934.Mask empty=FaceRegions1934.empty(79,81);check(empty.samplePair1976(3,5)==0,"unreliable mask zero");
    }
    static final class Unknown implements QualityPixels1932.RegionMask,PairedRegions1976 {
        final ArrayList<String> calls=new ArrayList<String>();
        public int skinQ8(int x,int y){calls.add("s"+x+":"+y);return (x+y)%3==0?Integer.MAX_VALUE:x*19-y*123;}
        public int detailQ8(int x,int y){calls.add("d"+x+":"+y);return x*1976-y*713;}
        public int samplePair1976(int x,int y){throw new AssertionError("unknown callback pairing forbidden");}
    }
    static void unknown() {
        for(boolean strong:new boolean[]{false,true})for(boolean descriptor:new boolean[]{false,true}) {
            Unknown a=new Unknown(),b=new Unknown();int[] ap=new int[9*7*2],bp=ap.clone();
            new GpuPolicy1960.Protection(plan(a),strong).freezePolicy1964(9,7,5,ap,descriptor);
            new GpuPolicy1975Reference.Protection(plan(b),strong).freezePolicy1964(9,7,5,bp,descriptor);
            check(a.calls.equals(b.calls)&&Arrays.equals(ap,bp),"unknown callback exact calls, signed overflow and clamp");
        }
        Thread.currentThread().interrupt();boolean interrupted=false;
        try{new GpuPolicy1960.Protection(null,true).freezePolicy1964(5,7,0,new int[70],true);}catch(java.util.concurrent.CancellationException yes){interrupted=true;}finally{Thread.interrupted();}
        check(interrupted,"policy interruption retained");
    }
    public static void main(String[] ignored){sample();unknown();System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+"}");}
}

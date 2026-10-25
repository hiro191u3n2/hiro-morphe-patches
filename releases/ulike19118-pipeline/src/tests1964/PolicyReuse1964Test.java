package com.hiro.ulike;

import java.io.*;
import java.lang.reflect.Method;
import java.util.Arrays;

/** Executes identical custom double mask owners in the published .63 and new
 * JVM. Descriptor/policy bytes must match; only actual owner calls may fall. */
public final class PolicyReuse1964Test {
    private static long assertions,skinCalls,detailCalls,values;
    private static int cases;
    private static Method freeze;
    private static boolean candidate;
    private static void check(boolean condition,String label){assertions++;if(!condition)throw new AssertionError(label);}
    private static int clamp(int n){return Math.max(0,Math.min(256,n));}
    private static final class CountingMask implements QualityPixels1932.RegionMask {
        long skin,detail;
        final int phase;
        CountingMask(int phase){this.phase=phase;}
        int skinValue(int x,int y){
            if(phase==2)return (x+y)%5==0?Integer.MAX_VALUE:(x+y)%5==1?Integer.MIN_VALUE:(x*17+y*31)%257;
            double d=Math.sqrt((x-17.3)*(x-17.3)+(y-13.1)*(y-13.1));
            return clamp((int)Math.round(256.0*(1.0-Math.min(1.0,d/41.7))));
        }
        int detailValue(int x,int y){return phase==2?((x+y)%3==0?-41:(x+y)%3==1?391:256):(x*61+y*19)%257;}
        public int skinQ8(int x,int y){skin++;return skinValue(x,y);}
        public int detailQ8(int x,int y){detail++;return detailValue(x,y);}
    }
    private static int[] image(int w,int h){
        int[] out=new int[w*h];int seed=196431;
        for(int i=0;i<out.length;i++){seed^=seed<<13;seed^=seed>>>17;seed^=seed<<5;out[i]=0xff000000|(seed&0xffffff);}
        return out;
    }
    private static QualityPixels1932.Plan plan(final int width,int phase,CountingMask mask){
        QualityPixels1932.Plan plan=QualityPixels1932.plan(new QualityPixels1932.NoiseStats(4.2f,.91f,54f,.22f,96),
            700,10000000L,QualityPixels1932.LENS_WIDE,.8f,4,3,true,true,.73f).withFaceRegions(mask);
        if(phase==1){
            final int[] pixels=image(width,41);
            SpatialNoise1934 noise=SpatialNoise1934.probe(new SpatialNoise1934.Patches(){
                public void read(int[] out,int x,int y,int w,int h){for(int row=0;row<h;row++)System.arraycopy(pixels,(y+row)*width+x,out,row*w,w);}
            },width,41);
            plan=plan.withOutputNoise(noise);
        }
        return plan;
    }
    private static GpuPolicy1960.Protection fill(GpuPolicy1960.Protection p,int width,int rows,int first,int[] policy,boolean snapshot)throws Exception{
        if(candidate)return (GpuPolicy1960.Protection)freeze.invoke(p,width,rows,first,policy,snapshot);
        for(int y=0;y<rows;y++)for(int x=0;x<width;x++){
            int at=(y*width+x)*2;policy[at]=clamp(p.budgetQ8(x,y+first));policy[at+1]=clamp(p.detailQ8(x,y+first));
        }
        return p;
    }
    private static void ints(DataOutputStream out,int[] data)throws Exception{out.writeInt(data.length);for(int n:data)out.writeInt(n);}
    private static void floats(DataOutputStream out,float[] data)throws Exception{out.writeInt(data.length);for(float n:data)out.writeInt(Float.floatToRawIntBits(n));}
    private static void one(DataOutputStream out,int width,int rows,int first,int phase,boolean strong)throws Exception{
        CountingMask mask=phase<0?null:new CountingMask(phase);
        QualityPixels1932.Plan plan=phase<0?null:plan(width,phase,mask);
        GpuPolicy1960.Protection p=new GpuPolicy1960.Protection(plan,strong);
        int count=width*rows;int[] policy=new int[count*2+7];Arrays.fill(policy,-97531);
        GpuPolicy1960.Protection frozen=fill(p,width,rows,first,policy,true);
        GpuPolicy1960.PolicyData data=frozen.data(width,rows,first);
        long expected=mask==null?0:(candidate?count:2L*count);
        if(mask!=null){check(mask.skin==expected&&mask.detail==expected,"each exact mask computed once");skinCalls+=mask.skin;detailCalls+=mask.detail;}
        check(data.count==count,"descriptor count");
        for(int y=0;y<rows;y++)for(int x=0;x<width;x++){
            int at=(y*width+x)*2,skin=mask==null?0:mask.skinValue(x,y+first),detail=mask==null?0:mask.detailValue(x,y+first);
            int budget=plan==null?256:256-((skin*plan.beautyQ8)>>9);
            if(!strong&&plan!=null)budget=plan.shadowBudgetQ8*budget>>8;
            check(policy[at]==clamp(budget)&&policy[at+1]==clamp(detail),"published integer oracle");
            check(data.masks[at]==skin&&data.masks[at+1]==detail,"raw masks preserve double owner");values+=4;
        }
        for(int i=count*2;i<policy.length;i++)check(policy[i]==-97531,"workspace tail unchanged");
        int[] exact=Arrays.copyOf(policy,count*2),raw=data.masks.clone();
        ints(out,exact);ints(out,data.masks);ints(out,data.u);floats(out,data.grid);floats(out,data.f);
        if(candidate){
            check(frozen.data(width,rows,first)==data,"same coordinate descriptor reused");
            if(mask!=null)check(mask.skin==expected&&mask.detail==expected,"reuse has no mask reads");
            Arrays.fill(policy,0);check(Arrays.equals(raw,data.masks),"pooled CPU policy cannot mutate descriptor");
            GpuPolicy1960.PolicyData shifted=frozen.data(width,rows,first+1);
            check(shifted!=data&&shifted.u[3]==first+1,"different absolute origin bypasses snapshot");
            if(mask!=null)check(mask.skin==expected+count&&mask.detail==expected+count,"shifted origin recalculated exactly");
            int[] cpuOnly=new int[count*2];GpuPolicy1960.Protection plain=fill(p,width,rows,first,cpuOnly,false);
            check(plain==p&&Arrays.equals(exact,cpuOnly),"no GPU capability retains allocation-free CPU oracle");
            check(new GpuPolicy1960.Protection(plan,!strong).data(width,rows,first)!=data,"settings cannot reuse another policy snapshot");
        }
        cases++;
    }
    public static void main(String[] args)throws Exception{
        candidate=Boolean.parseBoolean(args[1]);
        if(candidate)freeze=GpuPolicy1960.Protection.class.getMethod("freezePolicy1964",int.class,int.class,int.class,int[].class,boolean.class);
        DataOutputStream out=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(args[0])));
        for(int width:new int[]{1,5,17,71})for(int rows:new int[]{1,3,9})for(int first:new int[]{0,3,19})for(int phase=-1;phase<3;phase++)for(boolean strong:new boolean[]{false,true})one(out,width,rows,first,phase,strong);
        out.close();
        if(candidate){
            for(int[] bad:new int[][]{{0,1,0},{1,0,0},{1,1,-1},{1,2,Integer.MAX_VALUE}}){
                try{fill(new GpuPolicy1960.Protection(null,true),bad[0],bad[1],bad[2],new int[4],true);throw new AssertionError("invalid policy geometry accepted");}
                catch(java.lang.reflect.InvocationTargetException expected){check(expected.getCause() instanceof IllegalArgumentException,"invalid geometry fails before writes");}
            }
            Thread.currentThread().interrupt();
            try{fill(new GpuPolicy1960.Protection(null,true),1,1,0,new int[2],true);throw new AssertionError("cancelled mask completed");}
            catch(java.lang.reflect.InvocationTargetException expected){check(expected.getCause() instanceof java.util.concurrent.CancellationException,"policy cancellation");}
            finally{Thread.interrupted();}
        }
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"cases\":"+cases+",\"values\":"+values+",\"skin_calls\":"+skinCalls+",\"detail_calls\":"+detailCalls+",\"candidate\":"+candidate+"}");
    }
}

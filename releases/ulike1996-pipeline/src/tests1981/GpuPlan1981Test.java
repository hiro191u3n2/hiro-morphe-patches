package com.hiro.ulike;

import android.graphics.Bitmap;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CancellationException;

/** Actual policy/Strong CPU kernels and resident planning, with resource faults.
 * The .80 policy and Strong source are replayed in a separate JVM for every
 * numerical output. Resource tests do not pretend to execute GPU arithmetic. */
public final class GpuPlan1981Test {
    static long assertions;
    static boolean baseline;
    static Method freeze;
    static void check(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
    static Field field(Class<?> type,String name)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);return f;}
    static int skin(int x,int y){return (x*37+y*19)&255;}
    static int detail(int x,int y){return (x*11+y*71)&255;}
    static final class Mask implements QualityPixels1932.RegionMask,PairedRegions1976 {
        int skins,details;
        public int skinQ8(int x,int y){skins++;return skin(x,y);}
        public int detailQ8(int x,int y){details++;return detail(x,y);}
        public int samplePair1976(int x,int y){throw new AssertionError("an unknown callback must retain the original separate methods");}
    }
    static QualityPixels1932.Plan plan(QualityPixels1932.RegionMask mask,float beauty,boolean shadows){
        return QualityPixels1932.plan(new QualityPixels1932.NoiseStats(2.7f,3.8f,121f,.2f,100),200,10000000L,2,beauty,4,4,true,shadows,1f).withFaceRegions(mask);
    }
    static GpuPolicy1960.Protection freeze(GpuPolicy1960.Protection p,int width,int rows,int first,int[] out,boolean raw)throws Exception{
        return (GpuPolicy1960.Protection)freeze.invoke(p,width,rows,first,out,raw);
    }
    static void ints(DataOutputStream out,int[] values)throws Exception{out.writeInt(values.length);for(int value:values)out.writeInt(value);}
    static void floats(DataOutputStream out,float[] values)throws Exception{out.writeInt(values.length);for(float value:values)out.writeInt(Float.floatToRawIntBits(value));}
    static void policy(DataOutputStream binary)throws Exception{
        freeze=GpuPolicy1960.Protection.class.getDeclaredMethod(baseline?"freezePolicy1964":"freezePolicy1981",int.class,int.class,int.class,int[].class,boolean.class);freeze.setAccessible(true);
        for(int width:new int[]{1,7,37})for(int rows:new int[]{1,3,17})for(int first:new int[]{0,11})
        for(float beauty:new float[]{0f,.31f,1f})for(boolean strong:new boolean[]{false,true})for(boolean raw:new boolean[]{false,true}){
            Mask mask=new Mask();QualityPixels1932.Plan p=plan(mask,beauty,true);int count=width*rows;
            int[] values=new int[count*2+13];Arrays.fill(values,0x56789abc);
            GpuPolicy1960.Protection original=new GpuPolicy1960.Protection(p,strong),owner=freeze(original,width,rows,first,values,raw);
            check(mask.skins==count&&mask.details==count,"one CPU policy traversal");
            if(!baseline)check((owner!=original)==raw,"only selected legacy GPU route retains raw mask descriptor");
            GpuPolicy1960.PolicyData data=owner.data(width,rows,first);
            int expected=(!baseline&&raw)?count:2*count;
            check(mask.skins==expected&&mask.details==expected,"selected legacy descriptor performs no second mask traversal");
            for(int i=0;i<count;i++){
                int x=i%width,y=i/width+first,s=skin(x,y),d=detail(x,y);
                int b=256-((s*p.beautyQ8)>>9);if(!strong)b=p.shadowBudgetQ8*b>>8;
                check(values[i*2]==Math.max(0,Math.min(256,b))&&values[i*2+1]==d,"original Q8 policy at exact absolute coordinate");
                check(data.masks[i*2]==s&&data.masks[i*2+1]==d,"raw skin is copied before lossy beauty attenuation");
            }
            for(int i=count*2;i<values.length;i++)check(values[i]==0x56789abc,"pooled policy tail remains untouched");
            ints(binary,values);ints(binary,data.masks);ints(binary,data.u);floats(binary,data.grid);floats(binary,data.f);
            // Coordinates are part of descriptor ownership; a neighbour strip
            // cannot accidentally reuse this strip's masks.
            GpuPolicy1960.PolicyData next=owner.data(width,rows,first+1);
            check(next.u[3]==first+1&&next.masks[0]==skin(0,first+1),"different image coordinates regenerate descriptor");
            ints(binary,next.masks);
        }
        Mask m=new Mask();QualityPixels1932.Plan p=plan(m,.63f,true);
        try(PolicyCache1945 cache=PolicyCache1945.borrow(p,17,9,18)){
            check(cache!=null,"real scoped policy lease is available");
            QualityPixels1932.Plan scoped=p.withPolicyCache(cache);int[] values=new int[17*9*2];
            GpuPolicy1960.Protection owner=freeze(new GpuPolicy1960.Protection(scoped,true),17,9,9,values,true);
            GpuPolicy1960.PolicyData data=owner.data(17,9,9);
            check(m.skins==17*9&&m.details==17*9,"existing scoped mask cache remains authoritative");
            ints(binary,values);ints(binary,data.masks);
        }
        int[] fallback=new int[8];Arrays.fill(fallback,7);
        boolean cancelled=false;
        try{Thread.currentThread().interrupt();freeze(new GpuPolicy1960.Protection(null,true),2,2,0,fallback,true);}
        catch(java.lang.reflect.InvocationTargetException failure){cancelled=failure.getCause() instanceof CancellationException;}
        finally{Thread.interrupted();}
        check(cancelled&&fallback[0]==7,"cancel before materialization cannot publish a policy");
    }
    static void strong(DataOutputStream binary)throws Exception{
        final int width=17,height=37;final int[] pixels=new int[width*height];Random random=new Random(1981);
        for(int i=0;i<pixels.length;i++){int v=60+random.nextInt(128);pixels[i]=0xff000000|v<<16|(v+11)<<8|v-13;}
        GpuNoise1960.enabled=false;
        for(int noise:new int[]{2,4})for(boolean shadows:new boolean[]{false,true}){
            StrongNoise1958.Model model=StrongNoise1958.prepareJava(new StrongNoise1958.Patches(){public void read(int[] out,int x,int y,int w,int h){for(int row=0;row<h;row++)System.arraycopy(pixels,(y+row)*width+x,out,row*w,w);}},width,height,noise,shadows);
            Mask mask=new Mask();QualityPixels1932.Plan p=plan(mask,.81f,shadows);GpuPolicy1960.Protection protection=new GpuPolicy1960.Protection(p,true);
            int[] actual=new int[pixels.length],expected=new int[pixels.length];int[] input=pixels.clone();
            try(StrongNoise1958.Workspace workspace=new StrongNoise1958.Workspace(true)){
                StrongNoise1958.processRange(pixels,actual,width,height,0,height,0,height,0,noise,shadows,model,protection,workspace);
                check(mask.skins==pixels.length&&mask.details==pixels.length,"real CPU fallback uses the single materialized policy");
                StrongNoise1958.processJavaRange(pixels,expected,width,height,0,height,0,height,0,noise,shadows,model,protection);
                check(Arrays.equals(actual,expected)&&Arrays.equals(pixels,input),"routed Strong equals independent serial CPU and preserves source");
                ints(binary,actual);ints(binary,workspace.policy());
                int[] cf=workspace.confidence();check(cf!=null,"aligned real production workspace keeps complete NR13 confidence");ints(binary,cf);
                if(!baseline)for(int profile=-1;profile<8;profile++){
                    int[] u=new int[32];GpuStrong1960.Route1981 route=new GpuStrong1960.Route1981(model,u,profile,0,"test",1,1);
                    check(route.rawPolicy()==(profile>=0&&profile<4),"raw descriptor only for legacy policy route");
                    check(route.matches(model,u),"route binds exact model and uniforms");u[0]=1;check(!route.matches(model,u),"route ticket cannot cross uniform geometry");
                }
            }
        }
    }
    static long capacity(GpuChain1961.Preflight1981 p,int slot){for(int i=0;i<p.slots.length;i++)if(p.slots[i]==slot)return p.capacities[i];return 0;}
    static Bitmap bitmap(int w,int h){int[] p=new int[w*h];for(int i=0;i<p.length;i++)p[i]=0xff000000|i*1961;return Bitmap.from(w,h,p,Bitmap.Config.ARGB_8888,true);}
    static void preflight()throws Exception{
        for(int rotation:new int[]{0,90,180,270})for(int[] size:new int[][]{{64,80},{39,57},{91,103}}){
            GpuNoise1960.resetPlan();Bitmap source=bitmap(64,80);int[] original=source.snapshot();
            int w=size[0],h=size[1];GpuChain1961.Preflight1981 p=GpuChain1961.preflightResident1981(source,rotation,w,h,0);
            check(p!=null,"admissible resident continuation has an explicit plan");
            try{
                check(GpuNoise1960.operations==0&&source.reads==0&&source.writes==0&&Arrays.equals(source.snapshot(),original),"preflight performs no pixel transfer or mutation");
                check(p.matches(source,rotation,w,h)&&!p.matches(source,(rotation+90)%360,w,h),"preflight identity includes exact geometry");
                check(GpuChain1961.preflightResident1981(source,rotation,w,h,0)==null,"nested ownership cannot replace an active plan");
                check(capacity(p,24)==4L*64*80&&capacity(p,1)==4L*64*80,"future Strong source and moire intermediate are reserved");
                check(capacity(p,3)==16L*w*Math.min(64,h)&&capacity(p,8)==capacity(p,3),"both worst-size finishing policy banks are forecast");
                check(capacity(p,17)==4L*w*Math.min(64,h)&&capacity(p,18)==capacity(p,17),"both finishing output banks are forecast");
                long existing=8L*w*h+48L*w*Math.min(64,h)+16L*1024*1024;
                if(rotation==0&&w==64&&h==80)check(p.geometry==null,"identity route allocates no unnecessary geometry axes");
                else{
                    GpuPolicy1960.GeometryData independent=GpuPolicy1960.geometry(64,80,rotation,w,h);
                    check(p.geometry!=null&&Arrays.equals(p.geometry.u,independent.u)&&Arrays.equals(p.geometry.tables,independent.tables)&&Arrays.equals(p.geometry.weights,independent.weights),"preplanned exact geometry uses unchanged CPU axes");
                    existing+=4L*(independent.tables.length+independent.weights.length);
                    check(capacity(p,2)==4L*w*h&&capacity(p,6)==4L*independent.weights.length&&capacity(p,7)==4L*independent.tables.length,"geometry output and immutable axes reserved");
                    if(!independent.exactCrop){int[] u=independent.u;
                        for(int first=0;first<h;first+=p.geometryRows){int lo=Integer.MAX_VALUE,hi=-1;
                            for(int y=first;y<Math.min(h,first+p.geometryRows);y++)for(int tap=independent.tables[u[10]+y];tap<independent.tables[u[10]+y+1];tap++){int row=independent.tables[u[11]+tap];lo=Math.min(lo,row);hi=Math.max(hi,row);}
                            check(capacity(p,4)>=12L*w*(hi-lo+1),"every exact future horizontal band fits the preplanned peak");
                        }
                    }
                }
                check(p.javaPeak>=existing,"forecast covers existing finish Java admission including axes");
                p.deadline1981=12345;check(GpuChain1961.residentDeadline1981()==12345,"absolute whole-chain deadline belongs to this scope");
                GpuNoise1960.enabled=true;GpuNoise1960.Session session=GpuNoise1960.open();check(session!=null,"test session ownership acquired");
                try{
                    check(GpuChain1961.admitResident1981(session)&&GpuNoise1960.leases==1&&GpuNoise1960.reserved>p.javaPeak,"future peak debt acquired before NR workers");
                    check(!GpuChain1961.admitResident1981(session),"forecast cannot bind twice");
                    p.finishAdmission(session);check(GpuNoise1960.leases==0&&GpuNoise1960.reserved==0,"forecast transfers to ordinary finish admission after join");
                }finally{session.close();}
            }finally{p.close();p.close();source.recycle();}
            check(GpuChain1961.residentDeadline1981()==0&&GpuNoise1960.leases==0&&GpuNoise1960.reserved==0,"idempotent scope cleanup leaves no forecast debt");
        }
        for(int fault=0;fault<4;fault++){
            GpuNoise1960.resetPlan();Bitmap source=bitmap(64,80);GpuChain1961.Preflight1981 p=GpuChain1961.preflightResident1981(source,0,64,80,0);check(p!=null,"failure test plan created");
            GpuNoise1960.enabled=true;GpuNoise1960.Session s=GpuNoise1960.open();GpuNoise1960.revalidation=fault;
            boolean got=false;Throwable failure=null;
            try{got=GpuChain1961.admitResident1981(s);}catch(Throwable t){failure=t;}
            if(fault==0)check(got&&failure==null,"healthy forecast binds");
            if(fault==1)check(!got&&failure==null&&GpuNoise1960.leases==0,"resource revalidation refusal closes tentative debt");
            if(fault>=2)check(!got&&failure!=null&&GpuNoise1960.leases==0,"cancellation or exception closes tentative debt");
            p.close();s.close();source.recycle();check(GpuNoise1960.reserved==0&&GpuNoise1960.leases==0,"failure cleanup leaves no forecast ownership");
        }
        GpuNoise1960.resetPlan();Bitmap source=bitmap(64,80);GpuNoise1960.plannedMaximum=1024;
        check(GpuChain1961.preflightResident1981(source,0,64,80,0)==null&&GpuNoise1960.planCalls==6,"per-buffer limit rejects before Strong and exhausts bounded band choices");
        GpuNoise1960.resetPlan();GpuNoise1960.budget=false;
        check(GpuChain1961.preflightResident1981(source,0,64,80,0)==null&&GpuNoise1960.planCalls==0,"Java peak refusal precedes native planning and NR");
        GpuNoise1960.resetPlan();check(GpuChain1961.preflightResident1981(source,1,64,80,0)==null,"unsupported rotation never enters resident work");
        check(GpuChain1961.preflightResident1981(source,0,64,80,Integer.MAX_VALUE)==null,"impossible carried half input is declined");
        source.recycle();check(GpuChain1961.preflightResident1981(source,0,64,80,0)==null,"recycled source is rejected before planning");
    }
    public static void main(String[] args)throws Exception{
        baseline=Boolean.parseBoolean(args[1]);
        try(DataOutputStream binary=new DataOutputStream(new FileOutputStream(args[0]))){policy(binary);strong(binary);}
        if(!baseline)preflight();
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"baseline\":"+baseline+",\"route_policy_single_pass1981_verified\":"+!baseline+",\"resident_preflight1981_verified\":"+!baseline+",\"physical_android_tested\":false}");
    }
}

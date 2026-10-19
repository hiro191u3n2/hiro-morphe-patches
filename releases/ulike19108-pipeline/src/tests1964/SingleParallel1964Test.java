package com.hiro.ulike;

import java.util.Arrays;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** GX30 actual bounded parallel CPU outputs, including frozen global-coordinate
 * policies and cuts which do not begin on the DCT block grid. */
public final class SingleParallel1964Test {
    private static long assertions;private static int cases;
    private static final GpuQualification1961.Cancellation NEVER=new GpuQualification1961.Cancellation(){public boolean cancelled(){return false;}};
    private static void check(boolean condition,String label){assertions++;if(!condition)throw new AssertionError(label);}
    private static int[] image(int width,int height){
        int[] out=new int[width*height];int seed=196430;
        for(int i=0;i<out.length;i++){seed^=seed<<13;seed^=seed>>>17;seed^=seed<<5;int y=(seed>>>1)%21-10,r=128+y+(seed>>>6)%7-3,g=128+y,b=128+y+(seed>>>13)%9-4;out[i]=0xff000000|(r<<16)|(g<<8)|b;}
        return out;
    }
    /** Call the actual production residual proof's timing oracle, rather than a
     * test-owned reproduction of its parallel adapter. The fixture exercises
     * its frozen policy constructor, retained-reference comparison and reject.
     */
    private static void residualProof(int[] input,int[] serial,int width,int rows,int begin,int end,int origin,
            SingleNoise1955.Model model,SingleNoise1955.Protection protection)throws Exception {
        Class<?> gateType=Class.forName("com.hiro.ulike.GpuResidual1961$Gate");
        Constructor<?> gateConstructor=gateType.getDeclaredConstructor();gateConstructor.setAccessible(true);Object gate=gateConstructor.newInstance();
        Class<?> probeType=Class.forName("com.hiro.ulike.GpuResidual1961$ResidualProbe");
        Constructor<?> constructor=probeType.getDeclaredConstructor(String.class,gateType,int[].class,int[].class,
            int.class,int.class,int.class,int.class,int.class,int.class,int.class,int.class,boolean.class,
            SingleNoise1955.Model.class,SingleNoise1955.Protection.class);constructor.setAccessible(true);
        Object probe=constructor.newInstance("test1964-residual-cpu-"+width+"-"+origin+"-"+begin,gate,input,serial,
            width,rows,begin,end,0,rows,origin,4,true,model,protection);
        Method cpu=probeType.getDeclaredMethod("cpu1964",int[].class,GpuQualification1961.Cancellation.class);cpu.setAccessible(true);
        Method close=probeType.getDeclaredMethod("close");close.setAccessible(true);
        int[] output=new int[input.length];Arrays.fill(output,-111);
        try {
            long measured=(Long)cpu.invoke(probe,output,NEVER);check(measured>0,"production residual uses real CPU wall time");
            for(int i=0;i<output.length;i++)check(output[i]==serial[i],"production residual CPU parallel pixels/halos "+i);
            final GpuQualification1961.Cancellation cancelled=new GpuQualification1961.Cancellation(){public boolean cancelled(){return true;}};
            try{cpu.invoke(probe,output,cancelled);throw new AssertionError("residual cancelled proof ran");}
            catch(InvocationTargetException okay){check(okay.getCause() instanceof java.util.concurrent.CancellationException,"residual CPU timing cancellation");}
            Field reference=probeType.getDeclaredField("reference");reference.setAccessible(true);((int[])reference.get(probe))[0]^=1;
            check((Long)cpu.invoke(probe,output,NEVER)==0,"mismatched retained CPU output cannot qualify");
            Field rejected=gateType.getDeclaredField("rejected");rejected.setAccessible(true);check(rejected.getBoolean(gate),"mismatch rejected exactly");
        } finally {close.invoke(probe);}
        for(String name:new String[]{"input","reference","model","protection"}){Field field=probeType.getDeclaredField(name);field.setAccessible(true);check(field.get(probe)==null,"residual proof releases "+name);}
    }
    private static void one(final int width,int fullHeight,final int origin,final int rows,final int begin,final int end)throws Exception{
        final int[] full=image(width,fullHeight),input=new int[width*rows];System.arraycopy(full,origin*width,input,0,input.length);
        final SingleNoise1955.Model model=SingleNoise1955.probe(new SingleNoise1955.Patches(){public void read(int[] out,int x,int y,int w,int h){for(int row=0;row<h;row++)System.arraycopy(full,(y+row)*width+x,out,row*w,w);}},width,fullHeight);
        check(model.meanSigma()>=.3f||model.meanChromaSigma()>=.3f,"active noise oracle");
        final int[] policy=new int[width*(end-begin)*2];for(int y=begin;y<end;y++)for(int x=0;x<width;x++){int at=((y-begin)*width+x)*2;policy[at]=(x*17+(origin+y)*19)%257;policy[at+1]=(x*31+(origin+y)*5)%257;}
        final SingleNoise1955.Protection protection=new SingleNoise1955.Protection(){
            public int budgetQ8(int x,int y){return policy[((y-origin-begin)*width+x)*2];}
            public int detailQ8(int x,int y){return policy[((y-origin-begin)*width+x)*2+1];}
        };
        final int[] serial=new int[input.length],parallel=new int[input.length];Arrays.fill(serial,-111);Arrays.fill(parallel,-111);
        SingleNoise1955.processCpuRange(input,serial,width,rows,begin,end,0,rows,origin,4,true,model,protection);
        long elapsed=GpuQualification1961.parallelRows1964(begin,end,4,new GpuQualification1961.Cancellation(){public boolean cancelled(){return false;}},new GpuQualification1961.RowTask1964(){
            public boolean run(int first,int last){SingleNoise1955.processCpuRange(input,parallel,width,rows,first,last,0,rows,origin,4,true,model,protection);return true;}
        });
        check(elapsed>0,"actual pool wall time");
        for(int i=0;i<input.length;i++)check(serial[i]==parallel[i],"parallel CPU exact with untouched halo "+i);
        residualProof(input,serial,width,rows,begin,end,origin,model,protection);
        cases++;
    }
    public static void main(String[] args)throws Exception{
        boolean nativeExpected=args.length>0&&args[0].equals("native");
        check(SingleNoise1955.nativeAvailable()==nativeExpected,"actual production CPU JNI mode");
        for(int width:new int[]{9,13,31}){one(width,29,0,29,0,29);one(width,61,11,39,7,32);one(width,61,11,39,8,31);one(width,61,11,39,9,30);}
        one(65,131,0,131,0,131);
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"cases\":"+cases+",\"parallel_cpu_pixel_equivalence\":true,\"frozen_global_policy\":true,\"residual_production_parallel_cases\":"+cases+",\"residual_parallel_pixel_equivalence\":true,\"native_cpu\":"+nativeExpected+"}");
    }
}

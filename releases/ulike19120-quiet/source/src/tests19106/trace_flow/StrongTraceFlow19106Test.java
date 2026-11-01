package com.hiro.ulike;

import java.util.Arrays;
import java.util.concurrent.CancellationException;

public final class StrongTraceFlow19106Test {
    private static int assertions,cases;
    private static int[] input,u,policy,cpu,java,gpu;
    private static long[] fixed;
    private static void ok(boolean b,String m){assertions++;if(!b)throw new AssertionError(m);}
    private static void setup(){cases++;TraceFixture19106.reset();input=new int[8*3];u=new int[32];u[0]=8;u[1]=3;u[2]=1;u[3]=2;u[4]=0;u[5]=3;u[7]=3;u[8]=4;u[10]=3;u[11]=1;policy=new int[16];cpu=new int[8];Arrays.fill(cpu,0xff123456);java=cpu.clone();gpu=cpu.clone();gpu[3]=0xff123455;
        fixed=new long[]{1,2,1,0,1,1,4,0xff123456L,0xff123456L,0xff123455L,1,1,8,8};}
    private static long[] run(){StrongTrace19106.run(input,u,new StrongNoise1958.Model(),policy,cpu,java,gpu,fixed);return TraceFixture19106.reported;}
    private static void status(long[] r,int wanted){ok(r!=null,"report");ok(r[1]==wanted,"status "+r[1]+" != "+wanted);ok(StrongTrace19106.valid(r),"valid report");}
    private static void point(int id,int a,int b){TraceFixture19106.javaTrace[id]=a;TraceFixture19106.gpuTrace[id]=b;TraceFixture19106.javaTrace[2048+id]=TraceFixture19106.gpuTrace[2048+id]=1;}
    public static void main(String[] args){
        setup();long[] r=run();status(r,1);ok(r[4]==94,"packed checkpoint");ok(r[14]==5,"both outputs reproduced");ok(r[15]==3,"RGB observed");
        for(int field:new int[]{0,2,4,5,6,10,11,12,13}){setup();fixed[field]=0;run();ok(TraceFixture19106.javaCalls==0&&TraceFixture19106.gpuCalls==0&&TraceFixture19106.reports==0,"eligibility field "+field);}
        setup();fixed[3]=1;run();ok(TraceFixture19106.javaCalls==0&&TraceFixture19106.reports==0,"nativeJava difference blocks");
        for(int route=2;route<=5;route++){setup();fixed[2]=route;run();ok(TraceFixture19106.javaCalls==0&&TraceFixture19106.reports==0,"native unavailable "+route);}
        setup();TraceFixture19106.fits=false;status(run(),5);ok(TraceFixture19106.javaCalls==0&&TraceFixture19106.gpuCalls==0,"budget before work");
        setup();java[0]^=1;status(run(),3);ok(TraceFixture19106.javaCalls==0,"row identity before work");
        setup();u[6]=1;status(run(),3);ok(TraceFixture19106.javaCalls==0,"absolute row identity");
        setup();TraceFixture19106.javaTrace[93]^=2;status(run(),6);
        setup();TraceFixture19106.gpuOutput[7]^=1;status(run(),6);ok(TraceFixture19106.reported[14]==9,"whole GPU row required");
        setup();TraceFixture19106.gpuTrace[93]^=1;status(run(),6);
        setup();TraceFixture19106.mutate=true;status(run(),3);
        setup();TraceFixture19106.throwGpu=true;status(run(),3);
        setup();TraceFixture19106.javaTrace[2048+20]=0;status(run(),3);ok(TraceFixture19106.reported[4]==21,"missing presence explicit");
        setup();TraceFixture19106.gpuTrace[2048+20]=2;status(run(),3);
        setup();TraceFixture19106.javaTrace[2048+93]=0;status(run(),3);
        setup();TraceFixture19106.gpuTrace[2048+93]=0;status(run(),3);
        setup();TraceFixture19106.javaTrace[2048+90]=0;point(16,1,2);status(run(),3);
        setup();point(16,1,2);point(128+5,3,4);r=run();status(r,1);ok(r[4]==134,"local refinement");
        setup();point(6,1,2);point(1472+6,3,4);r=run();status(r,1);ok(r[4]==1479,"region refinement");
        setup();point(9,1,2);point(1425,3,4);point(16,1,2);r=run();status(r,1);ok(r[4]==1426,"guide before locals");
        setup();point(48,1,2);point(1520+6,3,4);r=run();status(r,1);ok(r[4]==1527,"map refinement");
        setup();point(31,1,1);point(32,1,2);point(544+4,0,0);point(544+8,3,4);r=run();status(r,1);ok(r[4]==33,"discarded NLM samples excluded");
        setup();point(31,1,1);point(32,1,2);point(544+4,1,1);point(544+8+6,3,4);r=run();status(r,1);ok(r[4]==559,"accepted NLM sample refinement");
        setup();point(45,Float.floatToIntBits(3),Float.floatToIntBits(4));point(42,Float.floatToIntBits(1),Float.floatToIntBits(1));point(43,Float.floatToIntBits(2),Float.floatToIntBits(2));r=run();status(r,1);ok(r[7]==1&&r[8]==r[10]&&r[9]==r[11],"direct binary operands");
        setup();point(11,1,2);point(12,1,2);r=run();status(r,1);ok(r[4]==94,"mask auxiliary excluded");
        for(int at=1;at<=3;at++){setup();TraceFixture19106.cancelAt=at;boolean cancelled=false;try{run();}catch(CancellationException expected){cancelled=true;}ok(cancelled,"cancellation propagated "+at);status(TraceFixture19106.reported,4);}
        setup();r=run();StringBuilder rendered=new StringBuilder();StrongTrace19106.appendSummary(rendered,r);ok(rendered.toString().contains("Java基準対GPU"),"honest Java label");ok(rendered.toString().contains("nativeは先行する行出力比較のみ"),"native observation scope");ok(rendered.toString().contains("両経路で記録あり"),"presence count label");
        System.out.println("{\"suite\":\"trace_flow19106\",\"cases\":"+cases+",\"assertions\":"+assertions+",\"status\":\"passed\"}");
    }
}

package com.hiro.ulike;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.concurrent.CancellationException;

/** Real production fixture/comparison code; native numerical paths remain unchanged. */
public final class StrongCancellation19112Test {
    static long assertions;static int fixtures,cancelled;
    static void check(boolean value,String reason){assertions++;if(!value)throw new AssertionError(reason);}
    static final class Token implements GpuQualification1961.Cancellation {
        int checks;final int stop;Token(int stop){this.stop=stop;}
        public boolean cancelled(){return ++checks==stop;}
    }
    static void words(MessageDigest d,int[] values){if(values==null){words(d,new int[]{-1});return;}for(int v:values){d.update((byte)v);d.update((byte)(v>>>8));d.update((byte)(v>>>16));d.update((byte)(v>>>24));}}
    static void fixtureHash(MessageDigest d,GpuStrongDefault1990.Fixture f){
        words(d,f.u);words(d,f.source);words(d,f.policy);
        for(int[] map:StrongNoise1958.gpuMaps1960(f.model))words(d,map);
        float[] ev=StrongNoise1958.gpuEvidence1960(f.model);int[] bits=new int[ev.length];for(int i=0;i<ev.length;i++)bits[i]=Float.floatToRawIntBits(ev[i]);words(d,bits);fixtures++;
    }
    static String corpus()throws Exception{
        MessageDigest d=MessageDigest.getInstance("SHA-256");
        for(int id=0;id<GpuStrongDefault1990.CASES;id++)fixtureHash(d,GpuStrongDefault1990.fixture(id));
        for(int id=64;id<66;id++)fixtureHash(d,GpuStrongDefault1990.Adaptive1994.benchmark(id));
        StringBuilder text=new StringBuilder();for(byte b:d.digest())text.append(String.format("%02x",b&255));return text.toString();
    }
    static void cancellation(){
        for(int stop:new int[]{1,2,10,70,80,120,400,800}){
            Token token=new Token(stop);boolean thrown=false;
            try{GpuStrongDefault1990.fixture19112(64,token);}catch(CancellationException expected){thrown=true;}
            check(thrown&&token.checks==stop,"fixture stops at requested source/policy/map checkpoint "+stop);cancelled++;
        }
        Token counted=new Token(-1);GpuStrongDefault1990.fixture19112(64,counted);int base=counted.checks;
        for(int stop:new int[]{base+10,base+300,base+500}){
            Token token=new Token(stop);boolean thrown=false;
            try{GpuStrongDefault1990.Adaptive1994.benchmark19112(64,token);}catch(CancellationException expected){thrown=true;}
            check(thrown&&token.checks==stop,"benchmark source/policy copies bounded "+stop);cancelled++;
        }
        int[] expected=new int[10009],actual=new int[10000],mask=new int[10000],otherMask=new int[10000];Arrays.fill(expected,4);Arrays.fill(actual,4);
        for(int stop:new int[]{1,2,3,5,6,7}){
            Token token=new Token(stop);boolean thrown=false;
            try{GpuStrongDefault1990.difference19112(actual,mask,expected,otherMask,9,10000,token);}catch(CancellationException correct){thrown=true;}
            check(thrown&&token.checks==stop,"ARGB/confidence compare cancellation "+stop);cancelled++;
        }
        check(actual[9999]==4&&expected[10008]==4&&mask[9999]==0,"cancelled compare never mutates inputs");
        Thread.currentThread().interrupt();boolean thrown=false;
        try{GpuStrongDefault1990.fixture19112(0,new Token(-1));}catch(CancellationException expectedCancel){thrown=true;}finally{Thread.interrupted();}
        check(thrown,"thread interruption remains authoritative");cancelled++;
        check(GpuStrongDefault1990.fixture19112(0,new Token(-1))!=null,"new optional attempt recovers without stale cancellation");
    }
    static String comparisons()throws Exception {
        MessageDigest d=MessageDigest.getInstance("SHA-256");
        int[] expected=new int[10009],actual=new int[10000],cf=new int[31],ecf=new int[31];Arrays.fill(expected,4);Arrays.fill(actual,4);
        for(int id=0;id<8;id++){
            int[] a=actual.clone(),b=cf.clone(),e=ecf.clone();int count=10000,offset=9;
            if(id==1)a=null;if(id==2)count=9999;if(id==3)a[4096]^=1;if(id==4)b=null;if(id==5)b=new int[30];if(id==6)b[30]=7;if(id==7){b=null;e=null;}
            String result=GpuStrongDefault1990.difference(a,b,expected,e,offset,count);d.update(String.valueOf(result).getBytes("UTF-8"));assertions++;
        }
        StringBuilder out=new StringBuilder();for(byte b:d.digest())out.append(String.format("%02x",b&255));return out.toString();
    }
    static void legacyComparisonBridge()throws Exception{
        GpuStrongDefault1990.Fixture f=GpuStrongDefault1990.fixture(0);
        int[] expected=f.source.clone(),actual=Arrays.copyOfRange(expected,f.u[0]*f.u[2],f.u[0]*f.u[3]);
        GpuStrong1960.Result output=new GpuStrong1960.Result(actual,null);
        Class<?> readType=Class.forName("com.hiro.ulike.GpuStrongDefault1990$Adaptive1994$Read");
        java.lang.reflect.Constructor<?> ctor=readType.getDeclaredConstructor(GpuStrong1960.Result.class,Long.TYPE);ctor.setAccessible(true);
        Object read=ctor.newInstance(output,1L);
        java.lang.reflect.Method compare=GpuStrongDefault1990.Adaptive1994.class.getDeclaredMethod("compare",GpuStrongDefault1990.Fixture.class,readType,int[].class,int[].class);compare.setAccessible(true);
        check(compare.invoke(null,f,read,expected,null)==null,"published 4-argument comparison still accepts exact result");
        actual[0]^=1;check(((String)compare.invoke(null,f,read,expected,null)).startsWith("argb index=0 "),"published comparison still rejects mismatch");
        check("output unavailable".equals(compare.invoke(null,f,null,expected,null)),"published comparison preserves unavailable result");
    }
    public static void main(String[] args)throws Exception{
        String corpus=corpus(),comparisons=comparisons();legacyComparisonBridge();if(args.length==0)cancellation();
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"fixtures\":"+fixtures+",\"cancelled_cases\":"+cancelled+",\"corpus_sha256\":\""+corpus+"\",\"comparison_sha256\":\""+comparisons+"\"}");
    }
}

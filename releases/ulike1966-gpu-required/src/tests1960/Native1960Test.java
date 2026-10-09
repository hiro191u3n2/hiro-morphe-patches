package com.hiro.ulike;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.util.Arrays;

/** Calls unchanged production Java/JNI/shader bytes against frozen CPU records. */
public final class Native1960Test {
    private static long assertions,pixels,confidencePixels,differences,confidenceDifferences;
    private static int cases,skipped;
    private static String firstDifference="";
    private static void check(boolean yes,String why){assertions++;if(!yes)throw new AssertionError(why);}
    private static int[] ints(DataInputStream in)throws Exception{int n=in.readInt();check(n>=0&&n<10000000,"bounded fixture");int[] a=new int[n];for(int i=0;i<n;i++)a[i]=in.readInt();return a;}
    private static void compare(int[] expected,int[] actual,boolean confidence,String name){
        check(actual!=null&&actual.length==expected.length,"readback shape "+name);
        for(int i=0;i<expected.length;i++){assertions++;if(confidence)confidencePixels++;else pixels++;
            if(expected[i]!=actual[i]){if(confidence)confidenceDifferences++;else differences++;
                if(firstDifference.isEmpty())firstDifference=name+" index="+i+" expected="+Integer.toHexString(expected[i])+" actual="+Integer.toHexString(actual[i]);}}
    }
    private static void lifecycle(){
        check(GpuNoise1960.available(),"library ABI");
        Thread.currentThread().interrupt();try{check(GpuNoise1960.open()==null,"preinterrupt open");check(Thread.currentThread().isInterrupted(),"interrupt preserved");}finally{Thread.interrupted();}
        GpuNoise1960.Session a=GpuNoise1960.open();check(a!=null,"lifecycle open");
        check(GpuNoise1960.open()==null,"single active owner");
        check(a.upload(0,new int[]{1,2,3,4}),"upload lifecycle");check(GpuNoise1960.retainedBytes()>0,"resident accounting");
        check(Arrays.equals(a.readInts(0,4),new int[]{1,2,3,4}),"lossless upload readback");
        Thread.currentThread().interrupt();try{check(!a.upload(0,new int[]{4}),"interrupt prevents candidate mutation");a.close();check(Thread.currentThread().isInterrupted(),"drained close preserves interrupt");}finally{Thread.interrupted();}
        check(!a.upload(0,new int[]{8}),"closed upload rejected");check(GpuNoise1960.retainedBytes()==0,"buffers released after fence");a.close();
        GpuNoise1960.Session b=GpuNoise1960.open();check(b!=null,"reopen after cancellation");b.close();
    }
    private static void records(String file)throws Exception{
        boolean[] supported=new boolean[4];for(int i=0;i<4;i++)supported[i]=GpuNoise1960.supports(i);
        check(supported[0],"actual GLES strong compile required");check(supported[2],"actual GLES analysis compile required");check(supported[3],"actual GLES geometry compile required");
        System.out.println("ENV "+GpuNoise1960.fingerprint());
        System.out.println("SUPPORTED "+Arrays.toString(supported));
        try(DataInputStream in=new DataInputStream(new FileInputStream(file))){
            check(in.readInt()==1960001,"frozen record version");for(;;){String name=in.readUTF();if(name.isEmpty())break;
                int shader=in.readInt();int[] u=ints(in);int invocations=in.readInt(),nb=in.readInt();int[][] input=new int[nb][];for(int i=0;i<nb;i++)input[i]=ints(in);
                int[] expected=ints(in),confidence=ints(in);
                if(!supported[shader]){skipped++;continue;}
                GpuNoise1960.Session s=GpuNoise1960.open();check(s!=null,"record session "+name);
                try{int[] slots=new int[nb];for(int i=0;i<nb;i++){slots[i]=i;check(s.upload(i,input[i].length==0?new int[1]:input[i]),"upload "+name+" binding"+i);}
                    if(shader==1){u[0]=0;check(s.dispatch(shader,slots,u,null,u[17]*u[18]),"single block");u[0]=1;}
                    if(shader==3&&u[0]==1){check(s.dispatch(shader,slots,u,null,u[4]*u[14]),"geometry horizontal");u[0]=2;}
                    float[] f=null;if(shader==2&&u[0]==4){f=new float[32];for(int i=0;i<32;i++)f[i]=Float.intBitsToFloat(input[6][i]);}
                    check(s.dispatch(shader,slots,u,f,invocations),"real dispatch "+name);
                    int outputSlot=1;
                    if(shader==2&&(u[0]==1||u[0]==4))outputSlot=3;
                    if(shader==2&&u[0]==2){u[0]=3;check(s.dispatch(shader,slots,u,null,16),"spectral median");outputSlot=2;}
                    compare(expected,s.readInts(outputSlot,expected.length),false,name);
                    if(confidence.length>0)compare(confidence,s.readInts(shader==2?2:7,confidence.length),true,name+" confidence");
                    check(Arrays.equals(input[0],s.readInts(0,input[0].length)),"GPU immutable input "+name);cases++;
                }finally{s.close();}
                check(GpuNoise1960.retainedBytes()==0,"record releases resident buffers");
            }
        }
        System.out.println("RESULT {\"status\":\""+(differences+confidenceDifferences==0?"passed":"failed")+"\",\"assertions\":"+assertions+",\"cases\":"+cases+",\"unsupportedShaderCases\":"+skipped+",\"pixels\":"+pixels+",\"confidencePixels\":"+confidencePixels+",\"pixelDifferences\":"+differences+",\"confidenceDifferences\":"+confidenceDifferences+",\"firstDifference\":\""+firstDifference+"\",\"singleFp64Supported\":"+supported[1]+"}");
        check(differences+confidenceDifferences==0,"pixel equality "+firstDifference);
    }
    static native void setFault(int mode);
    private static native boolean beginExternal();
    private static native boolean externalUnchanged();
    private static native boolean endExternal();
    static native long[] faultFacts();
    private static void fault(String mode){
        check(GpuNoise1960.available(),"fault library");
        if(mode.equals("context")){check(beginExternal(),"foreign EGL context");check(GpuNoise1960.supports(0),"owner initializes isolated context");lifecycle();check(externalUnchanged(),"foreign EGL context unchanged");check(endExternal(),"foreign EGL released");}
        else {GpuNoise1960.Session s=GpuNoise1960.open();check(s!=null,"fault session");check(s.upload(0,new int[]{7,8,9,10}),"fault input");long before=GpuNoise1960.retainedBytes();
            setFault(mode.equals("timeout")?1:2);check(s.readInts(0,4)==null,"failed readback remains unpublished");setFault(0);s.close();
            if(mode.equals("timeout")){check(GpuNoise1960.retainedBytes()==before,"unknown completion quarantined");check(GpuNoise1960.open()==null,"timed-out context not reused");}
            else {check(GpuNoise1960.retainedBytes()==0,"unmap failure drained then released");GpuNoise1960.Session fresh=GpuNoise1960.open();check(fresh!=null,"known-complete failure reusable");fresh.close();}}
        long[] facts=faultFacts();System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"mode\":\""+mode+"\",\"fenceCalls\":"+facts[0]+",\"mapCalls\":"+facts[1]+",\"lastFenceTimeoutNs\":"+facts[2]+"}");
    }
    public static void main(String[] args)throws Exception{if(args.length==2&&args[0].equals("fault")){fault(args[1]);return;}lifecycle();records(args[0]);}
}

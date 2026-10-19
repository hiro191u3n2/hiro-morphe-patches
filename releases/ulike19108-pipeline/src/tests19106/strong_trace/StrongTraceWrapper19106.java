package com.hiro.ulike;
import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CancellationException;
/** Actual production wrapper and JNI; only replay ownership is installed by this host fixture. */
public final class StrongTraceWrapper19106 {
    static int assertions;
    static void check(boolean ok,String why){assertions++;if(!ok)throw new AssertionError(why);}
    static int[] ints(DataInputStream in)throws Exception{int n=in.readInt();int[] a=new int[n];for(int x=0;x<n;x++)a[x]=in.readInt();return a;}
    @SuppressWarnings("unchecked") public static void main(String[] args)throws Exception{
        int[] u,source,policy,ev,expected,trace;int[][] maps=new int[3][];
        try(DataInputStream in=new DataInputStream(new BufferedInputStream(new FileInputStream(args[0])))){
            check(in.readInt()==1910601,"corpus magic");in.readUTF();u=ints(in);source=ints(in);policy=ints(in);ev=ints(in);for(int n=0;n<3;n++)maps[n]=ints(in);expected=ints(in);trace=ints(in);
        }
        float[] evidence=new float[ev.length];for(int n=0;n<ev.length;n++)evidence[n]=Float.intBitsToFloat(ev[n]);int[] original=source.clone(),oldPolicy=policy.clone(),oldUniforms=u.clone();
        check(GpuStrong1960.trace19106(source,evidence,maps,policy,u,u[30])==null,"foreground inert");
        Field f=GpuResident1976.class.getDeclaredField("CPU");f.setAccessible(true);ThreadLocal<Boolean> cpu=(ThreadLocal<Boolean>)f.get(null);cpu.set(Boolean.TRUE);
        try{
            boolean inactive=false;try{GpuStrong1960.trace19106(source,evidence,maps,policy,u,u[30]);}catch(CancellationException e){inactive=true;}check(inactive,"inactive replay rejected");
            ResidentStages19104.begin(null);ResidentStages19104.replay();
            check(GpuStrong1960.trace19106(source,evidence,maps,policy,u,-1)==null,"negative x rejected");
            Thread.currentThread().interrupt();boolean cancelled=false;try{GpuStrong1960.trace19106(source,evidence,maps,policy,u,u[30]);}catch(CancellationException e){cancelled=true;}finally{Thread.interrupted();}check(cancelled,"cancel before GPU open");
            GpuStrong1960.Trace19106 result=GpuStrong1960.trace19106(source,evidence,maps,policy,u,u[30]);check(result!=null,"real wrapper trace complete");
            check(Arrays.equals(expected,result.pixels),"wrapper row exact");check(Arrays.equals(trace,result.words),"wrapper raw trace exact");
            check(Arrays.equals(source,original)&&Arrays.equals(policy,oldPolicy)&&Arrays.equals(u,oldUniforms),"wrapper input ownership");check(!GpuNoise1960.sessionBusy(),"GPU session released");
        }finally{cpu.remove();ResidentStages19104.clear();}
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"actual_wrapper_jni\":true,\"physical_android_tested\":false}");
    }
}

package com.hiro.ulike;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CancellationException;

/** Execute the actual Java -> native fp64 preparation -> batched GLES route.
 * The private test entry bypasses admission only to compare every candidate
 * against the independently compiled frozen1960 Java pixel fixtures. */
public final class SingleResidual1961Test {
    private static int assertions,cases,pixels;
    private static final Method ROUTE;
    static {
        try {
            ROUTE=GpuResidual1961.class.getDeclaredMethod("run",int[].class,int.class,int.class,int.class,
                int.class,int.class,int.class,int.class,int.class,boolean.class,SingleNoise1955.Model.class,
                SingleNoise1955.Protection.class,int.class,GpuQualification1961.Cancellation.class);
            ROUTE.setAccessible(true);
        } catch(Exception error){throw new ExceptionInInitializerError(error);}
    }
    private static void check(boolean okay,String message){assertions++;if(!okay)throw new AssertionError(message);}
    private static int[] ints(DataInputStream in)throws Exception {int n=in.readInt();int[] a=new int[n];for(int i=0;i<n;i++)a[i]=in.readInt();return a;}
    private static float[] floats(int[] bits){float[] a=new float[bits.length];for(int i=0;i<a.length;i++)a[i]=Float.intBitsToFloat(bits[i]);return a;}
    private static SingleNoise1955.Model model(int[] u,float[] data)throws Exception {
        Constructor<SingleNoise1955.Model> c=SingleNoise1955.Model.class.getDeclaredConstructor(
            int.class,int.class,int.class,int.class,float[].class,float[].class,float[].class,
            float[].class,float[].class,float[].class,int.class,int.class,int.class);
        c.setAccessible(true);int cells=u[9]*u[10];
        return c.newInstance(u[1],u[2],u[9],u[10],Arrays.copyOfRange(data,0,cells),
            Arrays.copyOfRange(data,cells,cells*2),Arrays.copyOfRange(data,cells*2,cells*3),
            Arrays.copyOfRange(data,cells*3,cells*3+8),Arrays.copyOfRange(data,cells*3+8,cells*3+16),
            Arrays.copyOfRange(data,cells*3+16,data.length),0,u[11],u[12]);
    }
    private static SingleNoise1955.Protection protection(final int[] u,final int[] policy) {
        return new SingleNoise1955.Protection(){public int budgetQ8(int x,int y){return policy[((y-u[6]-u[4])*u[1]+x)*2];}public int detailQ8(int x,int y){return policy[((y-u[6]-u[4])*u[1]+x)*2+1];}};
    }
    private static int[] route(int[] source,int[] u,SingleNoise1955.Model model,SingleNoise1955.Protection policy,
            GpuQualification1961.Cancellation cancellation)throws Exception {
        try{return (int[])ROUTE.invoke(null,source,u[1],u[3],u[4],u[5],u[7],u[8],u[6],u[13],u[14]!=0,
            model,policy,u[1]*(u[5]-u[4]),cancellation);}
        catch(InvocationTargetException failed){if(failed.getCause() instanceof CancellationException)throw (CancellationException)failed.getCause();throw failed;}
    }
    private static void fixtures(String filename)throws Exception {
        DataInputStream in=new DataInputStream(new FileInputStream(filename));
        int magic=in.readInt();check(magic==1960001||magic==19610013,"fixture magic");
        for(;;) {
            String name=in.readUTF();if(name.isEmpty())break;
            int[] u,expected;int[][] buffers;int count;
            if(magic==19610013) {
                u=ints(in);buffers=new int[4][];buffers[0]=ints(in);buffers[2]=ints(in);buffers[3]=ints(in);
                expected=ints(in);count=expected.length;
            } else {
                int shader=in.readInt();u=ints(in);count=in.readInt();int bindings=in.readInt();
                buffers=new int[bindings][];for(int i=0;i<bindings;i++)buffers[i]=ints(in);
                expected=ints(in);ints(in);if(shader!=1)continue;
            }
            SingleNoise1955.Model model=model(u,floats(buffers[2]));int[] before=buffers[0].clone();
            int[] actual=route(buffers[0],u,model,protection(u,buffers[3]),null);
            check(actual!=null&&actual.length==count,"complete actual residual route "+name);
            for(int i=0;i<count;i++)check(actual[i]==expected[i],"frozen1960 pixel "+name+"/"+i);
            check(Arrays.equals(before,buffers[0]),"immutable source "+name);
            check(!GpuNoise1960.sessionBusy()&&GpuNoise1960.retainedBytes()<=128L*1024*1024,"closed bounded native workspace "+name);
            cases++;pixels+=count;
        }in.close();
    }
    private static void largeAndCancellation()throws Exception {
        final int w=65,h=131;final int[] source=new int[w*h];Random random=new Random(19610013);
        for(int i=0;i<source.length;i++){int g=90+random.nextInt(31);source[i]=0xff000000|((g+random.nextInt(9)-4)<<16)|(g<<8)|(g+random.nextInt(9)-4);}
        SingleNoise1955.Model model=SingleNoise1955.probe(new SingleNoise1955.Patches(){public void read(int[] a,int x,int y,int width,int height){for(int row=0;row<height;row++)System.arraycopy(source,(y+row)*w+x,a,row*width,width);}},w,h);
        int[] u=new int[32];u[1]=w;u[2]=h;u[3]=h;u[5]=h;u[8]=h;u[13]=4;u[14]=1;
        SingleNoise1955.Protection p=new SingleNoise1955.Protection(){public int budgetQ8(int x,int y){return 173;}public int detailQ8(int x,int y){return (x+y)%257;}};
        int[] expected=new int[source.length];SingleNoise1955.processCpuRange(source,expected,w,h,0,h,0,h,0,4,true,model,p);
        int[] actual=route(source,u,model,p,null);check(Arrays.equals(expected,actual),"three64-row JNI/graph batches");cases++;pixels+=source.length;
        boolean cancelled=false;try{route(source,u,model,p,new GpuQualification1961.Cancellation(){public boolean cancelled(){return true;}});}catch(CancellationException okay){cancelled=true;}
        check(cancelled,"proof cancellation throws before publication");
        check(!GpuNoise1960.sessionBusy()&&GpuNoise1960.retainedBytes()==0,"cancelled proof drains and closes");
        check(SingleResidual1961.prepare(source,w,h,0,h,1,h,0,4,true,model)==null,"JNI rejects missing upper halo/core row");
        check(SingleResidual1961.prepare(source,w,h,0,h,0,h,0,0,true,model)==null,"JNI rejects invalid noise request");
    }
    public static void main(String[] args)throws Exception {
        check(GpuNoise1960.supports(GpuNoise1960.RESIDUAL1961),"real FP32 residual GLES capability");
        fixtures(args[0]);largeAndCancellation();check(cases>=17,"independent fixture coverage");
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"cases\":"+cases+",\"pixels\":"+pixels+",\"physical_android_tested\":false,\"binary64_preparation_preserved\":true}");
    }
}

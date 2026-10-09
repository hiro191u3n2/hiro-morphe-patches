package com.hiro.ulike;
import java.util.*;
/** Actual production Java/JNI; only analysis shader replaced by a raw-bit helper probe. */
public strictfp final class DivisionGpu1973Test {
    static int assertions; static long cases; static int rng=0x91ce83f5;
    static native int[] scalarSnapshot1973();
    static void check(boolean b,String s){assertions++;if(!b)throw new AssertionError(s);}
    static int next(){rng^=rng<<13;rng^=rng>>>17;rng^=rng<<5;return rng;}
    static void add(List<Integer> p,int a,int b){p.add(a);p.add(b);}
    public static void main(String[] args)throws Exception {
        check(GpuNoise1960.available(),"actual JNI ABI loads");
        check(GpuNoise1960.fingerprint().length()==0,"cold cached identity starts empty");
        check(GpuNoise1960.warmEnvironment1973(),"cold private context identity warms");
        int[] state=scalarSnapshot1973();
        check(state[0]==1&&state[1]==0&&state[2]==0&&state[3]==0&&state[4]==0,"warming compiles no program and allocates no SSBO/staging/session");
        String fingerprint=GpuNoise1960.fingerprint();check(fingerprint.contains("source="),"warmed identity binds actual source");
        check(GpuNoise1960.warmEnvironment1973()&&fingerprint.equals(GpuNoise1960.fingerprint()),"warmed identity stable without reset");
        Thread.currentThread().interrupt();check(!GpuNoise1960.warmEnvironment1973(),"interrupted warming refuses");Thread.interrupted();
        check(GpuNoise1960.PROGRAMS==42&&GpuNoise1960.strongExactProgram1973(-1)==-1&&GpuNoise1960.strongExactProgram1973(3)==-1,"appended program bounds");
        for(int v=0;v<3;v++){
            check(GpuNoise1960.variant(GpuNoise1960.STRONG,v)==v*9,"legacy generic mapping preserved");
            for(int m=0;m<4;m++)check(GpuNoise1960.strongProgram(m,v)==27+m+4*v,"legacy tile mapping preserved");
            int id=GpuNoise1960.strongExactProgram1973(v);check(id==39+v&&GpuNoise1960.supports(id),"real exact full shader compiles "+id);
            check(GpuNoise1960.workgroup(id)==(v==1?32:v==2?128:64),"exact generic workgroup mapping "+id);
        }
        for(int id:new int[]{0,9,18,27,28,29,30,31,32,33,34,35,36,37,38})check(GpuNoise1960.supports(id),"retained old Strong program compiles "+id);
        check(GpuNoise1960.supports(GpuNoise1960.ANALYSIS),"helper GPU probe compiles");
        List<Integer> values=new ArrayList<Integer>();
        int[] edges={0,0x80000000,1,2,3,0x007fffff,0x00800000,0x00800001,0x00ffffff,0x3f000000,0x3f000001,0x3f7fffff,0x3f800000,0x3f800001,0x3fc00000,0x40000000,0x4b7fffff,0x7f000000,0x7f7ffe,0x7f7fff,0x7f7ffffe,0x7f7fffff,0x7f800000,0xff800000,0x7fc00001};
        for(int a:edges)for(int b:edges){add(values,a,b);add(values,a^0x80000000,b);}
        for(int m=1;m<=10000;m++){add(values,m,0x40000000);add(values,m^0x80000000,0x40000000);add(values,0x007fffff-m,0x3f7fffff);}
        for(int i=0;i<50000;i++){int a=next(),b=next();if((a&0x7fffffff)>=0x7f800000)a&=0xff7fffff;if((b&0x7fffffff)>=0x7f800000)b&=0xff7fffff;add(values,a,b);}
        int[] pairs=new int[values.size()];for(int i=0;i<pairs.length;i++)pairs[i]=values.get(i);int n=pairs.length/2;
        GpuNoise1960.Session s=GpuNoise1960.open();check(s!=null,"owned real session opens");
        GpuNoise1960.Lease1971 lease=s.reserveCapacity1971(new int[]{0,1},new long[]{pairs.length*4L,n*4L},n*4L);check(lease!=null,"readback and native growth are reserved");
        check(s.upload(0,pairs)&&s.allocate(1,n*4L),"raw bits retain subnormal inputs without float load");
        for(int perturb:new int[]{0,-8,8,100}){
            check(lease.revalidate1971(),"actual physical quota revalidates");
            int[] u=new int[32];u[0]=n;u[1]=perturb;
            check(s.dispatch(GpuNoise1960.ANALYSIS,new int[]{0,1},u,null,n),"real GPU helper dispatch "+perturb);
            int[] actual=s.readInts(1,n);check(actual!=null&&actual.length==n,"complete GPU helper readback");
            for(int i=0;i<n;i++){
                float cpu=Float.intBitsToFloat(pairs[2*i])/Float.intBitsToFloat(pairs[2*i+1]);int expect=Float.floatToRawIntBits(cpu);cases++;
                if(Float.isNaN(cpu)){if((actual[i]&0x7fffffff)<=0x7f800000)throw new AssertionError("NaN class");}
                else if(actual[i]!=expect)throw new AssertionError("exact GPU division a="+Integer.toHexString(pairs[2*i])+" b="+Integer.toHexString(pairs[2*i+1])+" gpu="+Integer.toHexString(actual[i])+" cpu="+Integer.toHexString(expect)+" perturb="+perturb);
            }
        }
        int[] u=new int[32];u[10]=0;check(!s.dispatch(39,new int[]{0,1},u,null,1),"exact mode3 rejects wrong preparation descriptor");
        WholeRoute1953.retained=Long.MAX_VALUE;check(!lease.revalidate1971(),"future CPU workspace exhaustion refuses submission");WholeRoute1953.retained=0;
        lease.close();s.close();check(GpuNoise1960.reservedBytes1971()==0&&!GpuNoise1960.sessionBusy(),"all owned quota/session state releases");
        check(GpuNoise1960.warmEnvironment1973()&&fingerprint.equals(GpuNoise1960.fingerprint()),"close and warm preserve source identity");
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"cases\":"+cases+"}");
    }
}
final class WholeRoute1953 {static volatile long retained;static long retainedBytes(){return retained;}}
final class GpuFinish1953 {static long retainedBytes(){return 0;}}
final class SpeedWorkers1935 {static long nativeRetainedBytes1956(){return 0;}}

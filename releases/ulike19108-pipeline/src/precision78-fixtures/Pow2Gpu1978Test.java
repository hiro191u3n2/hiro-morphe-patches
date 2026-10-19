package com.hiro.ulike;
import java.util.*;
/** Raw binary32 operands avoid incidental subnormal flushing by float loads. */
public strictfp final class Pow2Gpu1978Test {
    static int assertions,rng=0x18f779ae;static long cases;
    static void check(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
    static int next(){rng^=rng<<13;rng^=rng>>>17;rng^=rng<<5;return rng;}
    static void add(List<Integer> data,int bits,int shift){data.add(bits);data.add(shift);}
    public static void main(String[] args){
        check(GpuNoise1960.available()&&GpuNoise1960.warmEnvironment1973(),"actual .78 JNI context");
        ArrayList<Integer> values=new ArrayList<Integer>();
        int[] edges={0,0x80000000,1,2,3,0x007fffff,0x00800000,0x00800001,0x3f000000,0x3f800000,0x3f800001,0x40000000,0x4b7fffff,0x7f7fffff,0x7f800000,0xff800000,0x7fc00001};
        for(int shift=0;shift<=127;shift++){
            for(int bits:edges){add(values,bits,shift);add(values,bits^0x80000000,shift);}
            int boundary=(shift+1)<<23;
            for(int d=-32;d<=32;d++){add(values,boundary+d,shift);add(values,(boundary+d)^0x80000000,shift);}
        }
        for(int i=0;i<300000;i++)add(values,next(),next()&127);
        int[] pairs=new int[values.size()];for(int i=0;i<pairs.length;i++)pairs[i]=values.get(i);
        int n=pairs.length/2,normal=0,fallback=0;
        GpuNoise1960.Session session=GpuNoise1960.open();check(session!=null,"owned raw-bit session");
        GpuNoise1960.Lease1971 lease=session.reserveCapacity1971(new int[]{0,1},new long[]{pairs.length*4L,n*4L},n*4L);
        try{
            check(lease!=null&&lease.revalidate1971(),"pow2 outputs and upload reserved");
            check(session.upload(0,pairs)&&session.allocate(1,n*4L),"raw pow2 upload");
            int[] u=new int[32];u[0]=n;u[2]=1;
            check(session.dispatch(GpuNoise1960.ANALYSIS,new int[]{0,1},u,null,n),"actual production pow2 helper dispatch");
            int[] actual=session.readInts(1,n);check(actual!=null&&actual.length==n,"all pow2 results returned");
            for(int i=0;i<n;i++){
                int bits=pairs[i*2],shift=pairs[i*2+1],exponent=(bits>>>23)&255;
                if(exponent>shift&&exponent<255)normal++;else fallback++;
                float expected=Float.intBitsToFloat(bits)/Float.intBitsToFloat((shift+127)<<23);int want=Float.floatToRawIntBits(expected);cases++;
                if(Float.isNaN(expected)){if((actual[i]&0x7fffffff)<=0x7f800000)throw new AssertionError("pow2 NaN class");}
                else if(actual[i]!=want)throw new AssertionError("pow2 bits="+Integer.toHexString(bits)+" shift="+shift+" gpu="+Integer.toHexString(actual[i])+" cpu="+Integer.toHexString(want));
            }
            check(normal>1000&&fallback>1000,"normal scaling and original boundary fallback both exercised");
        }finally{if(lease!=null)lease.close();session.close();}
        check(!GpuNoise1960.sessionBusy()&&GpuNoise1960.reservedBytes1971()==0,"all pow2 leases and native tickets released");
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"cases\":"+cases+",\"normal\":"+normal+",\"fallback\":"+fallback+"}");
    }
}

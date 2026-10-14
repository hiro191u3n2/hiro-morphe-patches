package com.hiro.ulike;
import java.io.*;
import java.util.Arrays;
/** Actual current production JNI/batched shader graph versus complete binary
 * expected outputs exported by Analysis1961Oracle in a separate frozen60 JVM. */
public final class Analysis1961Test {
    static long words;static int cases;
    static int[] ints(DataInputStream in)throws Exception{int[] a=new int[in.readInt()];for(int i=0;i<a.length;i++)a[i]=in.readInt();return a;}
    static void same(int[] expected,int[] actual,String label){if(actual==null||!Arrays.equals(expected,actual)){int at=0;if(actual!=null)while(at<Math.min(expected.length,actual.length)&&expected[at]==actual[at])at++;throw new AssertionError(label+" unequal at "+at+(actual!=null&&at<expected.length&&at<actual.length?" expected="+Integer.toHexString(expected[at])+" actual="+Integer.toHexString(actual[at]):" length"));}words+=expected.length;}
    static float[] floats(int[] bits){float[] a=new float[bits.length];for(int i=0;i<bits.length;i++)a[i]=Float.intBitsToFloat(bits[i]);return a;}
    /** Observe the aggregate's full intermediate output, which the final grid
     * does not consume when patches have sufficient samples. Original order and
     * stop semantics must still match the independently frozen NoiseProbe. */
    static int[] prefixCandidate(int[] packed,int w,int h,int variant){
        int pw=Math.min(64,w),ph=Math.min(64,h),count=packed.length/(pw*ph),per=(pw-1)*(ph-1),samples=count*per,chunks=(samples+255)/256,hist=2051*(count+1),values=5*(count+1);int shader=GpuNoise1960.variant(GpuNoise1960.ANALYSIS1961,variant);
        if(!GpuNoise1960.supports(shader))return null;GpuNoise1960.Session session=GpuNoise1960.open();if(session==null)return null;
        try{
            GpuNoise1960.Batch b=new GpuNoise1960.Batch().upload(0,packed).allocate(1,4L*samples).allocate(2,4L*values).allocate(3,4L*hist).allocate(4,4).allocate(5,4L*samples).allocate(6,4L*chunks).allocate(7,4);
            int[] u=new int[32],bindings={0,1,2,3,4,5,6,7};u[1]=pw;u[2]=ph;u[3]=count;u[4]=per;u[5]=samples;u[6]=chunks;u[7]=hist;
            u[0]=6;b.dispatch(shader,bindings,u,null,hist);u[0]=0;b.dispatch(shader,bindings,u,null,samples);u[0]=1;b.dispatch(shader,bindings,u,null,chunks);u[0]=2;b.dispatch(shader,bindings,u,null,1);u[0]=3;b.dispatch(shader,bindings,u,null,samples);u[0]=4;b.dispatch(shader,bindings,u,null,count+1);
            int[][] result=session.execute(b,new int[]{2},new int[]{values});return result==null?null:Arrays.copyOfRange(result[0],count*5,count*5+5);
        }finally{session.close();}
    }
    public static void main(String[] args)throws Exception{
        DataInputStream in=new DataInputStream(new BufferedInputStream(new FileInputStream(args[0])));if(in.readInt()!=19611)throw new AssertionError("independent frozen60 records header");int records=in.readInt();
        for(int record=0;record<records;record++){
            String type=in.readUTF();int w=in.readInt(),h=in.readInt(),pattern=in.readInt();int[] image=Analysis1961Oracle.image(w,h,pattern);
            if(type.equals("spatial")){
                int[] expected=ints(in),packed=Analysis1961Oracle.packed(image,w,h,false);
                for(int v=0;v<3;v++){SpatialNoise1934 result=GpuAnalysis1961.spatialCandidate1961(packed,w,h,v);same(expected,result==null?null:Analysis1961Oracle.spatialValues(result),"spatial "+w+"x"+h+" variant"+v);cases++;}
            }else if(type.equals("region")){
                int[] expected=ints(in),packed=Analysis1961Oracle.packed(image,w,h,true);
                for(int v=0;v<3;v++){float[] result=GpuAnalysis1961.regionsCandidate1961(packed,w,h,Analysis1961Oracle.evidence(),v);same(expected,result==null?null:Analysis1961Oracle.bits(result),"region "+w+"x"+h+" variant"+v);cases++;}
            }else if(type.equals("resident")){
                int[] a=ints(in),b=ints(in),c=ints(in),ev=ints(in),rt=ints(in),half=Analysis1961Oracle.half(image,w,h);
                for(int v=0;v<3;v++){
                    StrongNoise1958.Model result=GpuAnalysis1961.residentCandidate1961(half,w,h,4,true,floats(ev),floats(rt),v);
                    try{if(result==null)throw new AssertionError("resident candidate unavailable");same(a,(int[])Analysis1961Oracle.field(result,"halfMap"),"half "+w+"x"+h+" variant"+v);same(b,(int[])Analysis1961Oracle.field(result,"quarterMap"),"quarter "+w+"x"+h+" variant"+v);same(c,(int[])Analysis1961Oracle.field(result,"eighthMap"),"eighth "+w+"x"+h+" variant"+v);same(ev,Analysis1961Oracle.bits((float[])Analysis1961Oracle.field(result,"evidence")),"D8bins "+w+"x"+h+" variant"+v);same(rt,Analysis1961Oracle.bits((float[])Analysis1961Oracle.field(result,"runtimeEvidence")),"runtime "+w+"x"+h+" variant"+v);cases++;}
                    finally{StrongNoise1958.discardResident1961(result);}
                }
            }else throw new AssertionError(type);
        }
        if(!in.readUTF().equals("spatialPrefix32768"))throw new AssertionError("full aggregate prefix oracle missing");int prefixRecords=in.readInt(),prefixCases=0;
        for(int p=0;p<prefixRecords;p++){int w=in.readInt(),h=in.readInt(),pattern=in.readInt();int[] expected=ints(in),packed=Analysis1961Oracle.packed(Analysis1961Oracle.image(w,h,pattern),w,h,false);if(expected[4]!=32768)throw new AssertionError("prefix fixture must reach original cap");for(int v=0;v<3;v++){same(expected,prefixCandidate(packed,w,h,v),"aggregate32768 "+pattern+" variant"+v);prefixCases++;}}
        in.close();System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+words+",\"cases\":"+cases+",\"prefixCases\":"+prefixCases+",\"exactWords\":"+words+",\"actualVariants\":[64,32,128],\"spatialPrefix32768\":true,\"regionalLag4Periodic\":true,\"residentGraph\":true,\"physicalAndroidTested\":false}");
    }
}

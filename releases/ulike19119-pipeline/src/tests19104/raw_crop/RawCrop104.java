package com.hiro.ulike;
import java.util.*;

/** Actual GEOMETRY mode0; bounded raw ARGB crop, no colour arithmetic. */
public final class RawCrop104 {
  static long words,guardWords;static int cases,assertions;
  static void check(boolean v,String why){assertions++;if(!v)throw new AssertionError(why);}
  static void test(int width,int height,int sourceSlot){
    int[] input=new int[width*height];Random random=new Random(104000L+width*79L+height);
    for(int i=0;i<input.length;i++)input[i]=i%7==0?i:i%7==1?~i:random.nextInt();
    int capacity=width*Math.min(32,height),poison=0x5a3927e1;
    GpuNoise1960.Session session=GpuNoise1960.open();check(session!=null,"open");
    try{
      check(session.upload(sourceSlot,input),"source upload");
      check(session.allocate(22,4L),"scratch slot22 fourbytes");
      check(session.allocate(23,4L*capacity),"output slot23 bounded32rows");
      for(int first=0;first<height;first+=32){
        int rows=Math.min(32,height-first),count=width*rows;
        int[] u=new int[32];u[1]=width;u[2]=height;u[4]=width;u[5]=height;u[15]=first;u[16]=first+rows;u[20]=1;
        int[] poisonBuffer=new int[capacity];Arrays.fill(poisonBuffer,poison);
        GpuNoise1960.Batch batch=new GpuNoise1960.Batch().upload(23,poisonBuffer)
          .dispatch(GpuNoise1960.GEOMETRY,new int[]{sourceSlot,23,22,22,22,22},u,null,count);
        int[][] returned=session.execute(batch,new int[]{23},new int[]{capacity});
        check(returned!=null&&returned.length==1&&returned[0].length==capacity,"actual GPU readback");
        int[] actual=returned[0];
        for(int i=0;i<count;i++)check(actual[i]==input[first*width+i],"raw crop mismatch width="+width+" height="+height+" sourceSlot="+sourceSlot+" first="+first+" x="+(i%width)+" row="+(i/width)+" wanted="+Integer.toHexString(input[first*width+i])+" got="+Integer.toHexString(actual[i]));
        for(int i=count;i<capacity;i++)check(actual[i]==poison,"tail overwrite "+i);
        words+=count;guardWords+=capacity-count;cases++;
        System.out.println("CASE width="+width+" height="+height+" sourceSlot="+sourceSlot+" first="+first+" rows="+rows+" compared="+count+" guard="+(capacity-count)+" exact=true");
      }
      int[] unchanged=session.readInts(sourceSlot,input.length);check(Arrays.equals(input,unchanged),"source preserved");
    }finally{session.close();}
    check(!GpuNoise1960.sessionBusy(),"session released");
  }
  public static void main(String[] args){
    check(GpuNoise1960.supports(GpuNoise1960.GEOMETRY),"actual production GEOMETRY available");
    for(int[] shape:new int[][]{{1,1},{37,63},{4284,65}})for(int slot:new int[]{0,1,2,24})test(shape[0],shape[1],slot);
    System.out.println("ENV "+GpuNoise1960.fingerprint());
    System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"cases\":"+cases+",\"compared_argb_words\":"+words+",\"tail_guard_words\":"+guardWords+",\"actual_jni_gpu\":true,\"geometry_mode\":0,\"max_rows\":32,\"output_slot\":23,\"scratch_slot\":22,\"scratch_bytes\":4,\"source_slots\":[0,1,2,24],\"physical_android_tested\":false,\"device_speedup_verified\":false}");
  }
}

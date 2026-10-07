package com.hiro.ulike;
import android.media.Image;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
public final class Native1935Test {
 static int checks;static synchronized void require(boolean ok,String msg){checks++;if(!ok)throw new AssertionError(msg);}
 static void image(Random random,boolean direct){
  int w=2*(1+random.nextInt(64)),h=2*(1+random.nextInt(48));Image.Plane[] planes=new Image.Plane[3];byte[] want=new byte[w*h*3/2];int[] positions=new int[3],limits=new int[3];
  for(int c=0;c<3;c++){int cw=c==0?w:w/2,ch=c==0?h:h/2,step=1+random.nextInt(4),pad=random.nextInt(15),start=random.nextInt(21),stride=(cw-1)*step+1+pad,len=start+(ch-1)*stride+(cw-1)*step+1;
   ByteBuffer b=direct?ByteBuffer.allocateDirect(len+11):ByteBuffer.allocate(len+11);
   for(int y=0;y<ch;y++)for(int x=0;x<cw;x++){byte value=(byte)random.nextInt(256);b.put(start+y*stride+x*step,value);if(c==0)want[y*w+x]=value;else want[w*h+y*w+2*x+(c==1?1:0)]=value;}
   b.position(start);b.limit(len);if(random.nextBoolean())b=b.asReadOnlyBuffer();planes[c]=new Image.Plane(b,stride,step);positions[c]=start;limits[c]=len;
  }
  Image im=new Image(w,h,35,1,planes);byte[] out=new byte[want.length+17];Arrays.fill(out,(byte)71);
  require(YuvPlanes1934.copy(im,out),"valid full image");require(Arrays.equals(want,Arrays.copyOf(out,want.length)),"exact NV21");for(int i=want.length;i<out.length;i++)require(out[i]==71,"trailing destination sentinel");
  for(int c=0;c<3;c++){require(planes[c].getBuffer().position()==positions[c],"source position");require(planes[c].getBuffer().limit()==limits[c],"source limit");}
  byte[] before=out.clone();planes[2].getBuffer().limit(limits[2]-1);require(!YuvPlanes1934.copy(im,out),"truncated final sample");require(Arrays.equals(before,out),"invalid no mutation");
 }
 static void contracts(){
  float[] dest={8,9,10};require(!NativeSpeed1935.horizontal(new int[]{0xff000000},new int[]{0,1},new int[]{1},new float[]{1},dest,1),"invalid index false");require(Arrays.equals(dest,new float[]{8,9,10}),"horizontal invalid no mutation");
  require(!NativeSpeed1935.horizontal(new int[]{0xff000000},new int[]{0,1},new int[]{0},new float[]{Float.NaN},dest,1),"NaN false");
  require(!NativeSpeed1935.verticalAdd(dest,dest,new float[3],new float[3],1,3),"alias false");require(Arrays.equals(dest,new float[]{8,9,10}),"vertical invalid no mutation");
  ByteBuffer b=ByteBuffer.allocateDirect(32);byte[] out=new byte[6];Arrays.fill(out,(byte)77);
  require(!NativeSpeed1935.pack(b,0,3,2,1,b,0,1,1,1,b,0,1,1,1,2,2,out),"bad direct limit false");for(byte v:out)require(v==77,"native bad limit no mutation");
  require(!NativeSpeed1935.pack(b,0,32,Integer.MAX_VALUE,1,b,0,32,Integer.MAX_VALUE,1,b,0,32,Integer.MAX_VALUE,1,Integer.MAX_VALUE-1,Integer.MAX_VALUE-1,out),"oversized product rejected before arithmetic overflow");for(byte v:out)require(v==77,"huge dimensions no mutation");
 }
 public static void main(String[] args)throws Exception{
  boolean expected=args.length>0&&args[0].equals("native");require(NativeSpeed1935.available()==expected,"library availability mode");contracts();
  Random random=new Random(1935);for(int i=0;i<1200;i++)image(random,(i&1)==0);
  final AtomicReference<Throwable> failure=new AtomicReference<Throwable>();Thread[] threads=new Thread[6];for(int t=0;t<threads.length;t++){final int seed=t;threads[t]=new Thread(new Runnable(){public void run(){try{Random r=new Random(12345+seed);for(int i=0;i<100;i++)image(r,(i&1)==0);}catch(Throwable bad){failure.compareAndSet(null,bad);}}});threads[t].start();}for(Thread t:threads)t.join();if(failure.get()!=null)throw new AssertionError(failure.get());
  System.out.println("{\"status\":\"passed\",\"native_available\":"+NativeSpeed1935.available()+",\"checks\":"+checks+",\"image_cases\":1800,\"physical_android_tested\":false}");
 }
}

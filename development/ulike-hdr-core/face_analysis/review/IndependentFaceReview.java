package com.hiro.ulike.hdr.input;
import com.hiro.ulike.hdr.face.StillFaceAnalysis;
import java.nio.*;
public final class IndependentFaceReview {
 static int n;
 static void ok(boolean b){n++;if(!b)throw new AssertionError("check "+n);}
 interface Check{void run()throws Exception;}
 static void reject(Check c)throws Exception{boolean r=false;try{c.run();}catch(IllegalArgumentException|NullPointerException e){r=true;}ok(r);}
 static HdrFrame frame(int w,int h){Object o=new Object();CaptureMatch.Context c=new CaptureMatch.Context(o,o,o,o,o,7,"0",null);CaptureMatch.Result r=new CaptureMatch.Result(c,o,444,12,null,"5",200L,100);return new HdrFrame(w,h,HdrFrame.Encoding.BT2020_NCL_HLG_LIMITED,c,r,new short[][]{new short[w*h],new short[w*h/4],new short[w*h/4]},w*h*3L);}
 static StillFaceAnalysis.ProxyRenderer renderer(final int value,final String policy){return new StillFaceAnalysis.ProxyRenderer(){public String policyName(){return policy;}public int rgb8(HdrFrame f,int x,int y){return value;}};}
 public static void main(String[] args)throws Exception{
  HdrFrame source=frame(514,270);
  for(int turn=0;turn<4;turn++)for(int mirror=0;mirror<2;mirror++){
   StillFaceAnalysis.Image im=StillFaceAnalysis.prepare(source,90*turn,mirror==1,128,renderer(0xabcdef,"review"));
   int ow=turn%2==0?514:270,oh=turn%2==0?270:514;
   for(double x:new double[]{0,0.25,257.33,513})for(double y:new double[]{0,0.5,139.37,269}){
    double u=0,v=0;
    switch(turn){case 0:u=x;v=y;break;case 1:u=269-y;v=x;break;case 2:u=513-x;v=269-y;break;case 3:u=y;v=513-x;break;}
    if(mirror==1)u=ow-1-u;
    double px=(u+0.5)*im.width/ow-0.5,py=(v+0.5)*im.height/oh-0.5;
    double[] back=im.sourcePoint(px,py);ok(Math.abs(back[0]-x)<1e-10&&Math.abs(back[1]-y)<1e-10);
   }
   ByteBuffer a=im.rgb8();ok(a.isReadOnly()&&a.get(0)==(byte)0xab&&a.get(1)==(byte)0xcd&&a.get(2)==(byte)0xef);
   a.position(a.limit());ok(im.rgb8().position()==0);
   float[] points=new float[212];points[0]=im.width/3.25f;points[1]=im.height/2.25f;
   StillFaceAnalysis.Face face=new StillFaceAnalysis.Face(points,.5f);points[0]=99999;
   StillFaceAnalysis.Result result=StillFaceAnalysis.analyse(im,image->{ok(image==im);return new StillFaceAnalysis.Face[]{face};});
   reject(()->result.requireSource(frame(514,270)));
   StillFaceAnalysis.Face[] faces=result.requireSource(source);float before=faces[0].points()[0];faces[0]=null;ok(result.requireSource(source)[0].points()[0]==before);
   reject(()->im.sourcePoint(Double.NaN,0));reject(()->im.sourcePoint(0,Double.POSITIVE_INFINITY));
  }
  reject(()->StillFaceAnalysis.prepare(source,1,false,128,renderer(0,"review")));
  reject(()->StillFaceAnalysis.prepare(source,0,false,127,renderer(0,"review")));
  reject(()->StillFaceAnalysis.prepare(source,0,false,4097,renderer(0,"review")));
  reject(()->StillFaceAnalysis.prepare(source,0,false,128,renderer(0,"  ")));
  reject(()->StillFaceAnalysis.prepare(source,0,false,128,renderer(0,null)));
  reject(()->StillFaceAnalysis.prepare(source,0,false,128,renderer(0x1000000,"review")));
  reject(()->StillFaceAnalysis.prepare(source,0,false,128,renderer(-1,"review")));
  reject(()->new StillFaceAnalysis.Face(new float[212],1.01f));reject(()->new StillFaceAnalysis.Face(new float[212],-.01f));
  reject(()->new StillFaceAnalysis.Face(new float[214],.5f));reject(()->new StillFaceAnalysis.Face(null,.5f));
  StillFaceAnalysis.Image im=StillFaceAnalysis.prepare(source,0,false,128,renderer(0,"review"));
  float[] huge=new float[212];huge[0]=Float.MAX_VALUE;StillFaceAnalysis.Face enormous=new StillFaceAnalysis.Face(huge,.5f);
  reject(()->StillFaceAnalysis.analyse(im,image->new StillFaceAnalysis.Face[]{enormous}));
  Exception failure=new Exception("native failed");try{StillFaceAnalysis.analyse(im,image->{throw failure;});throw new AssertionError();}catch(Exception e){ok(e==failure);}
  System.out.println("{\"checks\":"+n+",\"status\":\"PASS\"}");
 }
}

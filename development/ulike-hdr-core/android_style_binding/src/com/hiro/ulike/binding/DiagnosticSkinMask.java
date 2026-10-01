package com.hiro.ulike.binding;
import com.hiro.ulike.style.SampledMakeupPipeline;

/** Validates diagnostic UV ramps under an explicit callback interpretation.
 * Does not auto-infer/invert a transfer function. Keeps ONLY the sampled 8-bit
 * mask, not RGB photo pixels. GPU filtering then callback quantization can lose
 * fractional native mask precision: this is never advertised as exact readback.
 */
public final class DiagnosticSkinMask {
 public enum Channels { ARGB(16,8,0,24),ABGR(0,8,16,24),RGBA(24,16,8,0),BGRA(8,16,24,0);
  final int r,g,b,a;Channels(int r,int g,int b,int a){this.r=r;this.g=g;this.b=b;this.a=a;}}
 public enum RowOrigin { UV_V_ZERO,UV_V_ONE }
 public final Object frameIdentity;public final String captureId;public final long sensorTimestampNs;
 public final int width,height;public final boolean exactNativeMask=false;
 public final double maximumRampError;
 private final byte[] topLeftMask;
 private DiagnosticSkinMask(Object identity,String id,long time,int w,int h,byte[] mask,double error){frameIdentity=identity;captureId=id;sensorTimestampNs=time;width=w;height=h;topLeftMask=mask;maximumRampError=error;}
 private static void require(boolean b,String s){if(!b)throw new IllegalArgumentException(s);}
 /** Interpretation must be explicitly selected from observed callback evidence.
  * The complete G/B raster must agree with linear pixel-center UV ramps within
  * one 8-bit code. Mismatch rejects; no guessed gamma/channel repair occurs.
  * This check cannot by itself prove that R came from the intended live mask.
  */
 public static DiagnosticSkinMask validate(Object exactFrameIdentity,String captureId,long timestamp,int width,int height,
    int[] raw,Channels channels,RowOrigin rowOrigin,boolean horizontalMirror){
  require(exactFrameIdentity!=null&&captureId!=null&&!captureId.isEmpty()&&captureId.length()<=256&&timestamp>=0&&width>=2&&height>=2&&(long)width*height<=25000000&&raw!=null&&raw.length==(long)width*height&&channels!=null&&rowOrigin!=null,"owned bounded diagnostic image required");
  byte[] mask=new byte[raw.length];double maximum=0;
  for(int y=0;y<height;y++)for(int x=0;x<width;x++){int p=raw[y*width+x];int cx=horizontalMirror?width-1-x:x,cy=rowOrigin==RowOrigin.UV_V_ZERO?height-1-y:y;
   double expectedU=(cx+.5)/width,expectedV=1-(cy+.5)/height;
   double error=Math.max(Math.abs(((p>>>channels.g)&255)/255.0-expectedU),Math.abs(((p>>>channels.b)&255)/255.0-expectedV));
   require(((p>>>channels.a)&255)==255&&error<=1.000001/255.0,"diagnostic channel/orientation/linear-transfer calibration mismatch");maximum=Math.max(maximum,error);mask[cy*width+cx]=(byte)(p>>>channels.r);
  }
  return new DiagnosticSkinMask(exactFrameIdentity,captureId,timestamp,width,height,mask,maximum);
 }
 public double[] sampleTile(Object exactFrameIdentity,SampledMakeupPipeline.FrameTile tile){
  require(exactFrameIdentity==frameIdentity&&tile!=null&&tile.captureId.equals(captureId)&&tile.sensorTimestampNs==sensorTimestampNs&&tile.imageWidth==width&&tile.imageHeight==height,"diagnostic mask belongs to a different still or geometry");
  double[] result=new double[tile.pixels()];for(int y=0;y<tile.height;y++)for(int x=0;x<tile.width;x++)result[y*tile.width+x]=(topLeftMask[(tile.y+y)*width+tile.x+x]&255)/255.0;return result;
 }
}

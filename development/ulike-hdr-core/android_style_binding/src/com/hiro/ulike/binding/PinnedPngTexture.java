package com.hiro.ulike.binding;
import java.io.*;import java.security.MessageDigest;import java.util.zip.*;

/** Literal authored PNG samples. No ICC/gamma conversion and no photo quantization.
 * PNG stores straight samples; sampling can explicitly premultiply BEFORE
 * bilinear interpolation, matching the makeup equation's required input.
 * This decoder accepts the audited RGB/RGBA/palette 8-bit noninterlaced layouts.
 */
public final class PinnedPngTexture {
 public enum Rows { FIRST_DECODED_ROW_AT_V_ZERO,FIRST_DECODED_ROW_AT_V_ONE }
 public final String sourceSha256;public final int width,height;private final byte[] rgba;
 private PinnedPngTexture(String sha,int w,int h,byte[] bytes){sourceSha256=sha;width=w;height=h;rgba=bytes;}
 private static void require(boolean b,String s){if(!b)throw new IllegalArgumentException(s);}
 private static String sha(byte[] b)throws Exception{byte[]h=MessageDigest.getInstance("SHA-256").digest(b);StringBuilder s=new StringBuilder();for(byte v:h)s.append(String.format(java.util.Locale.ROOT,"%02x",v&255));return s.toString();}
 public byte[] copyStraightRgba(){return rgba.clone();}
 public static PinnedPngTexture readPinned(byte[] png,String expectedSha)throws Exception{
  require(png!=null && png.length>=57 && png.length<=4*1024*1024 && expectedSha!=null && expectedSha.matches("[0-9a-f]{64}"),"bounded pinned PNG required");png=png.clone();require(sha(png).equals(expectedSha),"PNG SHA-256 mismatch");
  DataInputStream in=new DataInputStream(new ByteArrayInputStream(png));require(in.readLong()==0x89504e470d0a1a0aL,"PNG signature (JPEG LUT is not accepted)");
  ByteArrayOutputStream compressed=new ByteArrayOutputStream();byte[] palette=null,transparency=null;boolean header=false,end=false,idat=false,afterIdat=false;int w=0,h=0,channels=0,color=-1;
  while(in.available()>0){int n=in.readInt();require(n>=0 && n<=in.available()-8,"PNG chunk bounds");byte[] type=new byte[4],data=new byte[n];in.readFully(type);in.readFully(data);long crcValue=Integer.toUnsignedLong(in.readInt());CRC32 crc=new CRC32();crc.update(type);crc.update(data);require(crcValue==crc.getValue(),"PNG CRC");String key=new String(type,"US-ASCII");
   if(!header)require(key.equals("IHDR"),"PNG header first");
   if(key.equals("IHDR")){require(!header && n==13,"PNG header count");DataInputStream b=new DataInputStream(new ByteArrayInputStream(data));w=b.readInt();h=b.readInt();int depth=b.readUnsignedByte();color=b.readUnsignedByte();require(w>0&&h>0&&w<=2048&&h<=2048&&(long)w*h<=1048576&&depth==8&&(color==2||color==3||color==6)&&b.readUnsignedByte()==0&&b.readUnsignedByte()==0&&b.readUnsignedByte()==0,"PNG layout");channels=color==2?3:color==6?4:1;header=true;}
   else if(key.equals("PLTE")){require(!idat&&palette==null&&n>=3&&n<=768&&n%3==0,"PNG palette");palette=data;}
   else if(key.equals("tRNS")){require(!idat&&transparency==null&&color==3&&palette!=null&&n<=palette.length/3,"PNG palette alpha");transparency=data;}
   else if(key.equals("IDAT")){require(!afterIdat&&(color!=3||palette!=null),"PNG IDAT order");idat=true;compressed.write(data);}
   else if(key.equals("IEND")){require(n==0&&idat&&in.available()==0,"PNG IEND");end=true;}
   else {require((type[0]&32)!=0,"unsupported critical PNG chunk");if(idat)afterIdat=true;}
  }
  require(end,"incomplete PNG");int stride=w*channels;byte[] raw=new byte[(stride+1)*h];Inflater z=new Inflater();
  try{z.setInput(compressed.toByteArray());int filled=0;while(filled<raw.length){int n=z.inflate(raw,filled,raw.length-filled);require(n>0,"PNG zlib truncated/stalled");filled+=n;}byte[] extra=new byte[1];require(z.inflate(extra)==0&&z.finished()&&z.getRemaining()==0,"PNG decompressed length/trailing zlib");}finally{z.end();}
  byte[] out=new byte[w*h*4],previous=new byte[stride],row=new byte[stride];int p=0;
  for(int y=0;y<h;y++){int filter=raw[p++]&255;require(filter<=4,"PNG filter");for(int x=0;x<stride;x++){int a=x>=channels?row[x-channels]&255:0,b=previous[x]&255,c=x>=channels?previous[x-channels]&255:0,v=raw[p++]&255;int predictor=filter==0?0:filter==1?a:filter==2?b:filter==3?(a+b)/2:paeth(a,b,c);row[x]=(byte)(v+predictor);}
   for(int x=0;x<w;x++){int to=(y*w+x)*4,from=x*channels;if(color==3){int i=row[x]&255;require(i<palette.length/3,"palette sample outside table");System.arraycopy(palette,i*3,out,to,3);out[to+3]=transparency!=null&&i<transparency.length?transparency[i]:(byte)255;}else{System.arraycopy(row,from,out,to,3);out[to+3]=color==6?row[from+3]:(byte)255;}}
   byte[] temp=previous;previous=row;row=temp;
  }
  return new PinnedPngTexture(expectedSha,w,h,out);
 }
 private static int paeth(int a,int b,int c){int p=a+b-c,pa=Math.abs(p-a),pb=Math.abs(p-b),pc=Math.abs(p-c);return pa<=pb&&pa<=pc?a:pb<=pc?b:c;}
 /** Clamp-to-edge sampling is an explicit replacement sampler contract. The
  * original GPU's upload orientation/sampler state still needs device evidence.
  */
 public void sample(double u,double v,Rows rows,boolean premultiply,double[] out){
  require(Double.isFinite(u)&&Double.isFinite(v)&&rows!=null&&out!=null&&out.length>=4,"texture sample arguments");if(rows==Rows.FIRST_DECODED_ROW_AT_V_ONE)v=1-v;
  double px=Math.max(0,Math.min(width-1,u*width-0.5)),py=Math.max(0,Math.min(height-1,v*height-0.5));int x0=(int)Math.floor(px),y0=(int)Math.floor(py),x1=Math.min(x0+1,width-1),y1=Math.min(y0+1,height-1);double fx=px-x0,fy=py-y0;
  for(int c=0;c<4;c++){double a=value(x0,y0,c,premultiply)*(1-fx)+value(x1,y0,c,premultiply)*fx,b=value(x0,y1,c,premultiply)*(1-fx)+value(x1,y1,c,premultiply)*fx;out[c]=a*(1-fy)+b*fy;}
 }
 private double value(int x,int y,int c,boolean pm){int i=(y*width+x)*4;double v=(rgba[i+c]&255)/255.0;return pm&&c<3?v*((rgba[i+3]&255)/255.0):v;}
}

package com.hiro.ulike.hdr.gainmapcodec;
import java.nio.*;
import java.nio.file.*;
import java.security.*;
import java.io.*;
import java.util.*;
public final class IndependentCodecProbe {
 static int checks;
 static void yes(boolean v){checks++;if(!v)throw new AssertionError("check "+checks);}
 static void bad(Runnable r){checks++;try{r.run();}catch(IllegalArgumentException good){return;}throw new AssertionError("accepted invalid input "+checks);}
 static ColorP010.Identity id(int w,int h){return new ColorP010.Identity(w,h,"shot-1","geometry-1","process-1");}
 static void pack(ByteBuffer b,int at,int code){int x=code<<6;b.put(at,(byte)x);b.put(at+1,(byte)(x>>>8));}
 static byte[] stream(List<byte[]> units){ByteArrayOutputStream b=new ByteArrayOutputStream();for(byte[]n:units){b.write(0);b.write(0);b.write(1);b.write(n,0,n.length);}return b.toByteArray();}
 public static void main(String[] args)throws Exception{
  // Manually pack a padded 10x8 image; exact last address and nonzero origin.
  int w=10,h=8,ys=28,uvs=24,origin=5;
  ByteBuffer y=ByteBuffer.allocate(origin+(h-1)*ys+w*2),uv=ByteBuffer.allocate(origin+(h/2-1)*uvs+(w/2-1)*4+4);
  for(int row=0;row<h;row++)for(int x=0;x<w;x++)pack(y,origin+row*ys+x*2,(row*131+x*23)%1024);
  for(int row=0;row<h/2;row++)for(int x=0;x<w/2;x++){pack(uv,origin+row*uvs+x*4,10+row*51+x*7);pack(uv,origin+row*uvs+x*4+2,1000-row*73-x*11);}
  y.position(origin);uv.position(origin);ByteBuffer v=uv.duplicate();v.position(origin+2);
  ColorP010.Plane py=new ColorP010.Plane(y.asReadOnlyBuffer(),ys,2),pu=new ColorP010.Plane(uv.asReadOnlyBuffer(),uvs,4),pv=new ColorP010.Plane(v.asReadOnlyBuffer(),uvs,4);
  ColorP010 frame=ColorP010.read(id(4,4),ColorP010.Role.SDR_BASE,4,2,py,pu,pv);
  for(int row=0;row<4;row++)for(int x=0;x<4;x++)yes(frame.samples(0).get(row*4+x)==((row+2)*131+(x+4)*23)%1024);
  for(int row=0;row<2;row++)for(int x=0;x<2;x++){yes(frame.samples(1).get(row*2+x)==10+(row+1)*51+(x+2)*7);yes(frame.samples(2).get(row*2+x)==1000-(row+1)*73-(x+2)*11);}
  yes(y.position()==origin&&uv.position()==origin&&v.position()==origin+2);
  bad(()->ColorP010.read(id(4,4),ColorP010.Role.SDR_BASE,0,Integer.MAX_VALUE-1,py,pu,pv));
  bad(()->ColorP010.read(id(4,4),ColorP010.Role.SDR_BASE,Integer.MAX_VALUE-1,0,py,pu,pv));
  bad(()->ColorP010.read(id(4,4),ColorP010.Role.SDR_BASE,0,1,py,pu,pv));
  bad(()->ColorP010.read(id(4,4),ColorP010.Role.SDR_BASE,0,0,new ColorP010.Plane(y,ys,1),pu,pv));
  bad(()->ColorP010.read(id(4,4),ColorP010.Role.SDR_BASE,0,0,new ColorP010.Plane(y,ys-1,2),pu,pv));
  ByteBuffer shortY=y.duplicate();shortY.limit(origin+1);bad(()->ColorP010.read(id(4,4),ColorP010.Role.SDR_BASE,0,0,new ColorP010.Plane(shortY,ys,2),pu,pv));
  byte[] dest=new byte[128];Arrays.fill(dest,(byte)91);ByteBuffer dy=ByteBuffer.wrap(dest);ByteBuffer du=ByteBuffer.allocate(32),dv=ByteBuffer.allocate(32);byte[] before=dest.clone();
  bad(()->frame.copyTo(new ColorP010.Plane(dy,16,2),new ColorP010.Plane(du,16,4),new ColorP010.Plane(dv.asReadOnlyBuffer(),16,4)));yes(Arrays.equals(before,dest));
  Thread.currentThread().interrupt();try{ColorP010.read(id(4,4),ColorP010.Role.SDR_BASE,0,0,py,pu,pv);throw new AssertionError();}catch(java.util.concurrent.CancellationException expected){checks++;}finally{Thread.interrupted();}
  byte[] base=Files.readAllBytes(Path.of(args[0],"base.hevc")),map=Files.readAllBytes(Path.of(args[0],"map.hevc"));
  RoleHevcProof.Sps bp=RoleHevcProof.inspect(base,96,64,ColorP010.Role.SDR_BASE);yes(bp.profile==2&&bp.lumaBits==10&&bp.chromaBits==10&&bp.chroma==1&&bp.primaries==9&&bp.matrix==9&&bp.fullRange&&bp.transfer==1);
  yes(RoleHevcProof.inspect(map,96,64,ColorP010.Role.NUMERICAL_GAINMAP).transfer==8);
  bad(()->RoleHevcProof.inspect(base,96,64,ColorP010.Role.NUMERICAL_GAINMAP));bad(()->RoleHevcProof.inspect(map,96,64,ColorP010.Role.SDR_BASE));
  Main10Codec.Encoded encoded=Main10Codec.Encoded.verify(base,id(96,64),ColorP010.Role.SDR_BASE);
  StringBuilder sha=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(base))sha.append(String.format("%02x",b&255));yes(encoded.sha256.equals(sha.toString()));
  byte[] copy=encoded.copyAnnexB();Arrays.fill(copy,(byte)9);yes(Arrays.equals(encoded.copyAnnexB(),base));base[base.length-2]^=1;yes(!Arrays.equals(encoded.copyAnnexB(),base));
  byte[] stable=encoded.copyAnnexB();List<byte[]> ns=RoleHevcProof.annexB(stable);List<byte[]> both=new ArrayList<>(ns);both.addAll(ns);bad(()->RoleHevcProof.inspect(stream(both),96,64,ColorP010.Role.SDR_BASE));
  List<byte[]> aud=new ArrayList<>(ns);aud.add(new byte[]{70,1,(byte)0xf0});bad(()->RoleHevcProof.inspect(stream(aud),96,64,ColorP010.Role.SDR_BASE));
  List<byte[]> sei=new ArrayList<>(ns);sei.add(new byte[]{78,1,1,5,7,(byte)128});bad(()->RoleHevcProof.inspect(stream(sei),96,64,ColorP010.Role.SDR_BASE));
  List<byte[]> refs=new ArrayList<>();for(byte[] n:ns){n=n.clone();if(((n[0]>>>1)&63)==32)n[2]^=0x10;refs.add(n);}bad(()->RoleHevcProof.inspect(stream(refs),96,64,ColorP010.Role.SDR_BASE));
  List<byte[]> temporal=new ArrayList<>();for(byte[] n:ns){n=n.clone();n[1]=(byte)((n[1]&248)|2);temporal.add(n);}bad(()->RoleHevcProof.inspect(stream(temporal),96,64,ColorP010.Role.SDR_BASE));
  bad(()->CodecSelection.select(Arrays.asList(new CodecSelection.Candidate("real8",true,false,true,true)),CodecSelection.Policy.ANY,null));
  bad(()->CodecSelection.select(Arrays.asList(new CodecSelection.Candidate("roleMismatch",true,true,true,false)),CodecSelection.Policy.REQUIRE_HARDWARE,null));
  System.out.println("{\"independent_assertions\":"+checks+",\"status\":\"PASS\",\"android_executed\":false}");
 }
}

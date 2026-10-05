package com.hiro.ulike;
import java.util.*;import java.nio.file.*;import java.nio.*;
public class ShadowTests1923 {
 static int count; static void check(String name,boolean value){if(!value)throw new AssertionError(name);count++;System.out.println("PASS\t"+name);}
 static int pixel(int v){v=Math.max(0,Math.min(255,v));return 0xff000000|v<<16|v<<8|v;}
 static double variance(int[] a){double sum=0,sq=0;for(int p:a){int y=ShadowDetail1923.y(p);sum+=y;sq+=(double)y*y;}return sq/a.length-Math.pow(sum/a.length,2);}
 public static void main(String[] args)throws Exception {
  int w=128,h=128;Random r=new Random(1923);int[] a=new int[w*h];for(int i=0;i<a.length;i++)a[i]=pixel(42+(int)Math.round(r.nextGaussian()*12));
  int[] b=a.clone();double before=variance(a);long t=System.nanoTime();ShadowDetail1923.smooth(a,b,w,h,0,h,3,true);long elapsed=System.nanoTime()-t;double after=variance(b);
  check("synthetic dark flat field variance reduced at least 50 percent",after<before*0.5);
  System.out.println("SYNTHETIC_VARIANCE_BEFORE="+before+" AFTER="+after+" MS="+(elapsed/1e6));
  int[] reference=b.clone();
  for(int step:new int[]{1,7,31,64,128}) {
   b=a.clone();for(int y=0;y<h;y+=step){int start=Math.max(0,y-3),end=Math.min(h,y+step+3);int[] src=Arrays.copyOfRange(a,start*w,end*w);int[] dst=src.clone();ShadowDetail1923.smooth(src,dst,w,end-start,y-start,Math.min(h,y+step)-start,3,true);System.arraycopy(dst,(y-start)*w,b,y*w,Math.min(step,h-y)*w);}check("strip halo equality step "+step,Arrays.equals(reference,b));
  }
  b=a.clone();ShadowDetail1923.smooth(a,b,w,h,0,h,0,true);check("noise off byte exact",Arrays.equals(a,b));
  b=a.clone();ShadowDetail1923.smooth(a,b,w,h,0,h,4,false);check("shadow priority off byte exact",Arrays.equals(a,b));
  for(int v:new int[]{0,2,42,80,120,144,180,255}) {Arrays.fill(a,pixel(v));b=a.clone();ShadowDetail1923.smooth(a,b,w,h,0,h,4,true);check("constant flat field preserved "+v,Arrays.equals(a,b));}
  for(int i=0;i<a.length;i++)a[i]=pixel(150+r.nextInt(105));b=a.clone();ShadowDetail1923.smooth(a,b,w,h,0,h,4,true);check("bright areas exact including texture",Arrays.equals(a,b));
  for(int y=0;y<h;y++)for(int x=0;x<w;x++)a[y*w+x]=pixel(x<w/2?20:220);b=a.clone();ShadowDetail1923.smooth(a,b,w,h,0,h,4,true);check("strong step edge preserved within one level",Arrays.stream(b).allMatch(p->ShadowDetail1923.y(p)<=21||ShadowDetail1923.y(p)>=219));
  Arrays.fill(a,0x70403020);b=a.clone();ShadowDetail1923.smooth(a,b,w,h,0,h,4,true);check("translucent alpha and RGB exact",Arrays.equals(a,b));
  Arrays.fill(a,0xff202020);a[w*64+64]=0x00ffffff;b=a.clone();ShadowDetail1923.smooth(a,b,w,h,0,h,4,true);check("transparent neighbor cannot introduce white fringe",b[w*64+64]==0x00ffffff&&b[w*64+63]==0xff202020);
  // Feed the actual post-chroma domain, not a pre-correction raw frame.
  Arrays.fill(a,0xff234323);b=a.clone();ShadowDetail1923.smooth(a,b,w,h,0,h,4,true);
  check("processed constant chroma and luminance both exact",Arrays.equals(a,b));
  for(int level=1;level<=4;level++)for(int v=0;v<144;v+=9){Arrays.fill(a,pixel(v));b=a.clone();ShadowDetail1923.smooth(a,b,w,h,0,h,level,true);check("no black lift or haze constant "+v+" level "+level,Arrays.equals(a,b));}
  for(int v:new int[]{24,48,80}){
   double meanIn=0,meanOut=0;for(int i=0;i<a.length;i++){a[i]=pixel(v+(int)Math.round(r.nextGaussian()*5));meanIn+=ShadowDetail1923.y(a[i]);}
   b=a.clone();ShadowDetail1923.smooth(a,b,w,h,0,h,4,true);for(int p:b)meanOut+=ShadowDetail1923.y(p);
   check("no systematic flat-field luminance lift "+v,Math.abs(meanIn-meanOut)/a.length<0.7);
  }
  for(int i=0;i<a.length;i++){int n=r.nextInt(13)-6;a[i]=0xff000000|(ShadowDetail1923.clamp(42+n)<<16)|(42<<8)|ShadowDetail1923.clamp(42-n);}
  b=a.clone();ShadowDetail1923.smooth(a,b,w,h,0,h,4,true);double cin=0,cout=0;for(int i=0;i<a.length;i++){cin+=Math.abs(((a[i]>>>16)&255)-(a[i]&255));cout+=Math.abs(((b[i]>>>16)&255)-(b[i]&255));}
  check("dark chroma noise also reduced",cout<cin*0.8);
  for(int ww:new int[]{1,2,3,5})for(int hh:new int[]{1,2,3,5}){int[] s=new int[ww*hh];Arrays.fill(s,0xff191919);int[] d=s.clone();ShadowDetail1923.smooth(s,d,ww,hh,0,hh,3,true);check("tiny strip bounds "+ww+"x"+hh,Arrays.equals(s,d));}
  int[] bad=new int[2];boolean rejected=false;try{ShadowDetail1923.smooth(bad,bad,2,1,0,1,3,true);}catch(IllegalArgumentException e){rejected=true;}check("reject source output alias",rejected);
  Arrays.fill(a,0xff282828);b=a.clone();Thread.currentThread().interrupt();ShadowDetail1923.smooth(a,b,w,h,0,h,4,true);check("interruption preserved and no write at first row",Thread.currentThread().isInterrupted()&&Arrays.equals(a,b));Thread.interrupted();
  if(args.length==2){byte[] raw=Files.readAllBytes(Paths.get(args[0]));ByteBuffer in=ByteBuffer.wrap(raw).order(ByteOrder.BIG_ENDIAN);int width=in.getInt(),height=in.getInt();int[] image=new int[width*height];for(int i=0;i<image.length;i++)image[i]=in.getInt();int[] out=image.clone();long now=System.nanoTime();ShadowDetail1923.smooth(image,out,width,height,0,height,3,true);System.out.println("HOST_RASTER_PROCESS_MS="+((System.nanoTime()-now)/1e6));ByteBuffer bb=ByteBuffer.allocate(8+4*out.length).order(ByteOrder.BIG_ENDIAN);bb.putInt(width);bb.putInt(height);for(int p:out)bb.putInt(p);Files.write(Paths.get(args[1]),bb.array());}
  System.out.println("HOST_SHADOW_ASSERTIONS="+count);System.out.println("SYNTHETIC_AND_HOST_RASTER_ONLY_NOT_DEVICE_TEST");
 }
}

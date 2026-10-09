package com.hiro.ulike;
import java.io.*;import java.util.*;
/** Test-only inputs and recorded actual retained DEX output. */
public final class ChromaOracle1965 {
 static int fixtures,pixels,changed;
 static void words(DataOutputStream out,int[] a)throws IOException{out.writeInt(a.length);for(int p:a)out.writeInt(p);}
 static int rgb(int r,int g,int b){return 0xff000000|(Math.max(0,Math.min(255,r))<<16)|(Math.max(0,Math.min(255,g))<<8)|Math.max(0,Math.min(255,b));}
 static int[] image(int width,int height,int mode){
  int[] a=new int[width*height];Random random=new Random(19650000+mode*10000+width*100+height);
  for(int y=0;y<height;y++)for(int x=0;x<width;x++){
   int base=mode==0?150:mode==1?90+((x+y*3)%17):mode==2?155:mode==3?120+((x/3+y/4)%9):mode==4?random.nextInt(256):mode==5?((x+y)%2)*255:mode==6?120:mode==7?150:mode==8?150:150;
   int noise=mode==0||mode==2||mode==7?random.nextInt(13)-6:random.nextInt(7)-3;
   int p;
   if(mode==0)p=rgb(base+noise,base,base+random.nextInt(11)-5);
   else if(mode==1)p=rgb(base+28+noise,base+8,base-30+noise);
   else if(mode==2)p=rgb(base+18+noise,base+8,base-30+random.nextInt(31)-15);
   else if(mode==3)p=rgb(base+noise,base,base+random.nextInt(13)-6);
   else if(mode==4)p=rgb(base,random.nextInt(256),random.nextInt(256));
   else if(mode==5)p=rgb(base,base,base);
   else if(mode==6)p=rgb(base,base,base);
   else if(mode==7)p=rgb(base+noise,base,base+random.nextInt(11)-5);
   else if(mode==8)p=rgb(base+80+noise,base,base-80+noise);
   else p=rgb(base+30+noise,base+10,base-25+noise);
   if(mode==7&&(x==width/2&&y==height/2))p=(p&0xffffff)|0xfe000000;
   a[y*width+x]=p;
  }
  return a;
 }
 static void emit(DataOutputStream out,String name,int w,int h,int mode,int begin,int end,int radius)throws Exception{
  int[] input=image(w,h,mode),result=input.clone();ChromaDexOracle1965.chroma(input,result,w,h,begin,end,radius);
  int changes=0;for(int i=0;i<input.length;i++)if(input[i]!=result[i])changes++;
  out.writeUTF(name);for(int v:new int[]{w,h,begin,end,radius,changes})out.writeInt(v);words(out,input);words(out,result);
  fixtures++;pixels+=w*(end-begin);changed+=changes;
 }
 public static void main(String[] args)throws Exception{
  ChromaDexOracle1965.init(args[0]);
  try(DataOutputStream out=new DataOutputStream(new FileOutputStream(args[1]))){
   out.writeInt(1965002);
   for(int mode=0;mode<10;mode++)for(int radius:new int[]{1,3,8})emit(out,"mode"+mode+"-r"+radius,37,29,mode,0,29,radius);
   for(int radius:new int[]{1,8,32,128}){emit(out,"radius"+radius,41,33,2,0,33,radius);emit(out,"band-r"+radius,41,33,9,7,25,radius);}
   for(int[] dims:new int[][]{{1,1},{1,17},{17,1},{2,2},{3,9},{9,3}})emit(out,"border"+dims[0]+"x"+dims[1],dims[0],dims[1],0,0,dims[1],8);
   emit(out,"large-radius128",129,131,2,0,131,128);
   emit(out,"empty-band",17,11,2,4,4,3);
   out.writeUTF("");
  }
  System.out.println("{\"fixtures\":"+fixtures+",\"pixels\":"+pixels+",\"changed\":"+changed+",\"actual_dex_oracle\":true}");
 }
}

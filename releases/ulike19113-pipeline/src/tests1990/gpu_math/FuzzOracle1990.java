package com.hiro.ulike;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Random;

/** Compiled only against byte-frozen published 1.9.59 production classes.
 * Records original CPU outputs; this file contains no denoising algorithm. */
public final class FuzzOracle1990 {
    private static DataOutputStream out;
    private static int records;
    private static long pixels;
    private static int clip(int v){return Math.max(0,Math.min(255,v));}
    private static int[] image(int w,int h,int kind,int seed){
        Random r=new Random(seed);int[] a=new int[w*h];
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){
            int base=kind==0?36:kind==1?(x<w/2?48:174):kind==2?113:70;
            int n=kind==2?0:r.nextInt(101)-50,c=kind==2?0:r.nextInt(85)-42;
            int v=base+n+(kind==0?(int)Math.round(5*Math.sin(x*.17)+4*Math.cos(y*.21)):0);
            a[y*w+x]=0xff000000|clip(v+c)<<16|clip(v)<<8|clip(v-c);
            if(kind==3&&(x+3*y)%17==0)a[y*w+x]=(a[y*w+x]&0xffffff)|((x+y)%255)<<24;
        }return a;
    }
    private static Object field(Object o,String name)throws Exception{Field f=(o instanceof Class?(Class<?>)o:o.getClass()).getDeclaredField(name);f.setAccessible(true);return f.get(o instanceof Class?null:o);}
    private static int[] bits(float[] a){int[] b=new int[a.length];for(int i=0;i<a.length;i++)b[i]=Float.floatToRawIntBits(a[i]);return b;}
    private static void ints(int[] a)throws Exception{out.writeInt(a.length);for(int v:a)out.writeInt(v);}
    private static void record(String name,int shader,int[] u,int invocations,int[][] bindings,int[] expected,int[] confidence)throws Exception{
        out.writeUTF(name);out.writeInt(shader);ints(u);out.writeInt(invocations);out.writeInt(bindings.length);
        for(int[] binding:bindings)ints(binding);ints(expected);ints(confidence);records++;pixels+=expected.length;
    }
    private static int[] uniforms(int w,int h,int noise,boolean shadows,int mode){int[] u=new int[32];
        u[0]=w;u[1]=h;u[3]=h;u[5]=h;u[7]=h;u[8]=noise;u[9]=shadows?1:0;u[10]=mode;return u;}
    private static void preparation(int w,int h,int kind,int noise,boolean shadows,int mode)throws Exception{
        int[] p=image(w,h,kind,19000+records),expected=new int[p.length];float[] ev=new float[16];
        Random er=new Random(90100+records); for(int i=0;i<16;i++)ev[i]=kind==2?0f:.39f+er.nextFloat()*30f;
        StrongNoise1958.prepareJavaRange(p,expected,w,h,0,h,noise,shadows,ev,mode);
        record("strong-prep-"+mode+"-"+w+"x"+h+"-"+kind,0,uniforms(w,h,noise,shadows,mode),p.length,
            new int[][]{p,new int[p.length],bits(ev),new int[1],new int[1],new int[1],new int[1],new int[1]},expected,new int[0]);
    }
    private static void strong(final int w,final int h,int kind,int noise,boolean shadows,final int policyKind)throws Exception{
        final int[] p=image(w,h,kind,29000+records);
        StrongNoise1958.Model model=StrongNoise1958.prepareJava(new StrongNoise1958.Patches(){public void read(int[] a,int x,int y,int width,int height){for(int row=0;row<height;row++)System.arraycopy(p,(y+row)*w+x,a,row*width,width);}},w,h,noise,shadows);
        final int[] policy=new int[w*h*2];for(int y=0;y<h;y++)for(int x=0;x<w;x++){int i=(y*w+x)*2;policy[i]=policyKind==0?256:policyKind==1?0:(x+y)%3==0?0:173;policy[i+1]=policyKind==2?(x*7+y*11)%257:0;}
        StrongNoise1958.Protection protection=new StrongNoise1958.Protection(){public int budgetQ8(int x,int y){return policy[(y*w+x)*2];}public int detailQ8(int x,int y){return policy[(y*w+x)*2+1];}};
        int[] expected=p.clone();StrongNoise1958.processRange(p,expected,w,h,0,h,0,h,0,noise,shadows,model,protection);
        int cw=(w+3)/4,ch=(h+3)/4;int[] confidence=new int[cw*ch];
        for(int y=0;y<ch;y++)for(int x=0;x<cw;x++){int sx=Math.min(w-1,x*4+1),sy=Math.min(h-1,y*4+1);if(policy[(sy*w+sx)*2]!=0)confidence[y*cw+x]=model.smoothingQ8(p,w,h,sx,sy,0,h,0);}
        int[] u=uniforms(w,h,noise,shadows,3);u[11]=u[12]=1;
        record("strong-full-"+w+"x"+h+"-"+kind+"-policy"+policyKind,0,u,p.length,
            new int[][]{p,new int[p.length],bits((float[])field(model,"runtimeEvidence")),policy,(int[])field(model,"halfMap"),(int[])field(model,"quarterMap"),(int[])field(model,"eighthMap"),new int[confidence.length]},expected,confidence);
    }
    public static void main(String[] args)throws Exception{
        out=new DataOutputStream(new FileOutputStream(args[0]));out.writeInt(1960001);
        for(int sample=0;sample<240;sample++)for(int mode=0;mode<3;mode++)preparation(63+(sample%7),61+(sample%5),sample%4,1+sample%4,(sample&1)==0,mode);
        for(int sample=0;sample<240;sample++)strong(63+(sample%7),61+(sample%5),sample%4,1+sample%4,(sample&1)==0,sample%3);
        for(int[] size:new int[][]{{1,1},{1,2},{2,1},{3,5},{4,7},{5,8},{7,9},{31,33},{32,34},{33,35},{65,131},{3060,37},{4080,41},{4284,43}})
            for(int kind=0;kind<4;kind++)strong(size[0],size[1],kind,1+kind,(kind&1)==0,kind%3);
        out.writeUTF("");out.close();System.out.println("{\"records\":"+records+",\"pixels\":"+pixels+"}");
    }
}

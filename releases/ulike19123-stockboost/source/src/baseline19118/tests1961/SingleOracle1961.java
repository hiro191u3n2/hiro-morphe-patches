package com.hiro.ulike;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.util.Random;

/** Independent fixtures compiled exclusively against frozen published1960 CPU.
 * Includes alpha, full and partial protection, odd dimensions, >64-row batch
 * transitions, and cropped immutable halo with nonzero origin/phase. */
public final class SingleOracle1961 {
    private static DataOutputStream out;
    private static int cases;
    private static void ints(int[] a)throws Exception{out.writeInt(a.length);for(int v:a)out.writeInt(v);}
    private static void floats(float[] a)throws Exception{out.writeInt(a.length);for(float v:a)out.writeInt(Float.floatToRawIntBits(v));}
    private static Object field(Object a,String name)throws Exception{Field f=a.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(a);}
    private static int[] image(int w,int h,int kind) {
        Random random=new Random(61000+cases);int[] p=new int[w*h];
        for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
            int g=kind==2?80:kind==1?(x<w/2?64:176)+random.nextInt(17)-8:96+random.nextInt(41)-20;
            int r=kind==2?80:Math.max(0,Math.min(255,g+random.nextInt(17)-8));
            int b=kind==2?80:Math.max(0,Math.min(255,g+random.nextInt(17)-8));
            p[y*w+x]=((kind==3&&(x+y)%13==0)?0x80000000:0xff000000)|(r<<16)|(g<<8)|b;
        }return p;
    }
    private static void one(final int w,final int h,int kind,boolean strip)throws Exception {
        final int[] full=image(w,h,kind);
        SingleNoise1955.Model model=SingleNoise1955.probe(new SingleNoise1955.Patches(){public void read(int[] a,int x,int y,int pw,int ph){for(int row=0;row<ph;row++)System.arraycopy(full,(y+row)*w+x,a,row*pw,pw);}},w,h);
        final int origin=strip?21:0,rows=strip?49:h,begin=strip?7:0,end=strip?42:h;
        int[] input=new int[w*rows];System.arraycopy(full,origin*w,input,0,input.length);
        final int[] policy=new int[w*(end-begin)*2];
        for(int y=begin;y<end;y++)for(int x=0;x<w;x++) {
            int at=((y-begin)*w+x)*2;
            policy[at]=kind==1?0:kind==2?173:256;
            policy[at+1]=kind==2?(x*7+(y+origin)*11)%257:0;
        }
        SingleNoise1955.Protection protection=new SingleNoise1955.Protection(){public int budgetQ8(int x,int y){return policy[((y-origin-begin)*w+x)*2];}public int detailQ8(int x,int y){return policy[((y-origin-begin)*w+x)*2+1];}};
        int noise=(kind&1)==0?4:1;boolean shadows=(kind&2)==0;
        int[] expected=new int[input.length];SingleNoise1955.processRange(input,expected,w,rows,begin,end,0,rows,origin,noise,shadows,model,protection);
        int[] core=new int[w*(end-begin)];System.arraycopy(expected,begin*w,core,0,core.length);
        int[] u=new int[32];u[1]=w;u[2]=h;u[3]=rows;u[4]=begin;u[5]=end;u[6]=origin;u[8]=rows;
        u[9]=model.columns;u[10]=model.rows;u[11]=(Integer)field(model,"patchWidth");u[12]=(Integer)field(model,"patchHeight");
        u[13]=noise;u[14]=shadows?1:0;u[15]=1;u[20]=begin;
        out.writeUTF("single61-"+w+"x"+h+"-"+kind+"-strip"+strip);ints(u);ints(input);floats((float[])field(model,"nativeData"));ints(policy);ints(core);cases++;
    }
    public static void main(String[] args)throws Exception {
        out=new DataOutputStream(new FileOutputStream(args[0]));out.writeInt(19610013);
        for(int[] size:new int[][]{{1,1},{7,9},{17,19},{33,35},{65,77},{257,131}})for(int kind=0;kind<4;kind++)one(size[0],size[1],kind,false);
        one(65,77,0,true);one(65,77,3,true);out.writeUTF("");out.close();
        System.out.println("SINGLE61_ORACLE cases="+cases);
    }
}

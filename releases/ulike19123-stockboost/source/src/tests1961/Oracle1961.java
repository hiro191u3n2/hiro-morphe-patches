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
public final class Oracle1961 {
    private static DataOutputStream out;
    private static int records;
    private static long pixels;
    private static int clip(int v){return Math.max(0,Math.min(255,v));}
    private static int[] image(int w,int h,int kind,int seed){
        Random r=new Random(seed);int[] a=new int[w*h];
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){
            int base=kind==0?36:kind==1?(x<w/2?48:174):kind==2?113:70;
            int n=kind==2?0:r.nextInt(21)-10,c=kind==2?0:r.nextInt(15)-7;
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
        for(int i=0;i<16;i++)ev[i]=kind==2?0f:(i<8?4f+i*.5f:3f+i*.2f);
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
    private static void single(final int w,final int h,int kind,int noise,boolean shadows,final int policyKind)throws Exception{
        final int[] p=image(w,h,kind,39000+records);
        SingleNoise1955.Model model=SingleNoise1955.probe(new SingleNoise1955.Patches(){public void read(int[] a,int x,int y,int width,int height){for(int row=0;row<height;row++)System.arraycopy(p,(y+row)*w+x,a,row*width,width);}},w,h);
        final int[] policy=new int[p.length*2];for(int y=0;y<h;y++)for(int x=0;x<w;x++){int i=(y*w+x)*2;policy[i]=policyKind==0?256:policyKind==1?0:173;policy[i+1]=policyKind==2?(x*7+y*11)%257:0;}
        SingleNoise1955.Protection protection=new SingleNoise1955.Protection(){public int budgetQ8(int x,int y){return policy[(y*w+x)*2];}public int detailQ8(int x,int y){return policy[(y*w+x)*2+1];}};
        int[] expected=p.clone();SingleNoise1955.processRange(p,expected,w,h,0,h,0,h,0,noise,shadows,model,protection);
        int[] u=new int[32];u[1]=w;u[2]=h;u[3]=h;u[5]=h;u[8]=h;u[9]=model.columns;u[10]=model.rows;u[11]=(Integer)field(model,"patchWidth");u[12]=(Integer)field(model,"patchHeight");u[13]=noise;u[14]=shadows?1:0;u[15]=1;u[16]=-8;u[17]=(w+3)/4+1;u[18]=(h+8+3)/4;
        record("single-"+w+"x"+h+"-"+kind+"-policy"+policyKind,1,u,w*h,new int[][]{p,new int[p.length],bits((float[])field(model,"nativeData")),policy,new int[u[17]*u[18]*248]},expected,new int[0]);
    }
    private static void analysis(int w,int h,int kind,int mode)throws Exception{
        int[] p=image(w,h,kind,49000+records),u=new int[32],expected;u[0]=mode;u[1]=w;u[2]=h;
        int invocations;float[] basis=new float[64];float[][] rows=(float[][])field(StrongNoise1958.class,"D8");for(int i=0;i<8;i++)System.arraycopy(rows[i],0,basis,i*8,8);
        if(mode==0){u[3]=(w+1)/2;u[4]=(h+1)/2;invocations=u[3]*u[4];Method m=StrongNoise1958.class.getDeclaredMethod("downsample",int[].class,int.class,int.class,boolean.class);m.setAccessible(true);expected=(int[])m.invoke(null,p,w,h,false);}
        else if(mode==1){int step=kind%2+1,lag=kind%2==0?1:4;u[3]=step;u[4]=lag;u[7]=w;u[8]=h;invocations=((w-lag+step-1)/step)*((h-lag+step-1)/step);
            int[][] hist=new int[16][128];int[] counts=new int[16];Method m=StrongNoise1958.class.getDeclaredMethod("estimateLag",int[].class,int.class,int.class,int[][].class,int[].class,int.class,int.class);m.setAccessible(true);m.invoke(null,p,w,h,hist,counts,step,lag);
            expected=new int[2064];for(int i=0;i<16;i++)System.arraycopy(hist[i],0,expected,i*128,128);System.arraycopy(counts,0,expected,2048,16);
        }else{u[3]=Math.min(17,Math.max(1,w/16));u[4]=Math.min(17,Math.max(1,h/16));invocations=u[3]*u[4];float[] ev=new float[16];Method m=StrongNoise1958.class.getDeclaredMethod("estimateSpectral",int[].class,int.class,int.class,float[].class,int.class);m.setAccessible(true);m.invoke(null,p,w,h,ev,0);expected=bits(ev);}
        record("analysis-"+mode+"-"+w+"x"+h+"-"+kind,2,u,invocations,new int[][]{p,new int[Math.max(1,((w+1)/2)*((h+1)/2))],new int[16],new int[2064],bits(basis),new int[1]},expected,new int[0]);
    }
    private static void geometry(final int w,final int h,final int turn,int ow,int oh)throws Exception{
        final int[] p=image(w,h,0,59000+records);final int rw=turn%180==0?w:h,rh=turn%180==0?h:w;
        double scale=Math.max((double)ow/rw,(double)oh/rh),cw=Math.min((double)rw,ow/scale),ch=Math.min((double)rh,oh/scale),left=Math.max(0,(rw-cw)*.5),top=Math.max(0,(rh-ch)*.5);
        FastPixels1933.Plan plan=FastPixels1933.prepare(rw,rh,ow,oh,left,top,cw,ch);final int[] expected=new int[ow*oh];final int dw=ow;
        FastPixels1933.runRows(plan,new FastPixels1933.RowSource(){public void readRow(int y,int[] a){for(int x=0;x<rw;x++){int sx=x,sy=y;if(turn==90){sx=y;sy=h-1-x;}else if(turn==180){sx=w-1-x;sy=h-1-y;}else if(turn==270){sx=w-1-y;sy=x;}a[x]=p[sy*w+sx];}}},new FastPixels1933.RowSink(){public void writeRow(int y,int[] a){System.arraycopy(a,0,expected,y*dw,dw);}},0,oh);
        int[] u=new int[32];u[1]=w;u[2]=h;u[3]=turn;u[4]=ow;u[5]=oh;u[6]=plan.cropLeft;u[7]=plan.cropTop;u[14]=rh;u[16]=oh;
        int[] table=new int[1];float[] weights=new float[1];u[0]=plan.exactCrop?0:1;
        if(!plan.exactCrop){Object ha=plan.horizontal,va=plan.vertical;int[] ho=(int[])field(ha,"offset"),hi=(int[])field(ha,"indices"),vo=(int[])field(va,"offset"),vi=(int[])field(va,"indices");float[] hw=(float[])field(ha,"weights"),vw=(float[])field(va,"weights");
            table=new int[ho.length+hi.length+vo.length+vi.length];weights=new float[hw.length+vw.length];u[8]=0;u[9]=ho.length;u[10]=u[9]+hi.length;u[11]=u[10]+vo.length;u[13]=hw.length;
            System.arraycopy(ho,0,table,u[8],ho.length);System.arraycopy(hi,0,table,u[9],hi.length);System.arraycopy(vo,0,table,u[10],vo.length);System.arraycopy(vi,0,table,u[11],vi.length);System.arraycopy(hw,0,weights,0,hw.length);System.arraycopy(vw,0,weights,hw.length,vw.length);
        }
        record("geometry-"+w+"x"+h+"-"+turn+"-"+ow+"x"+oh,3,u,ow*oh,new int[][]{p,new int[ow*oh],new int[plan.exactCrop?1:ow*rh*3],new int[1],bits(weights),table},expected,new int[0]);
    }
    private static void policy(boolean strong,boolean spatial,float beauty)throws Exception{
        int w=17,h=19,first=3,rows=13;float[] grid={0f,.30000004f,.6f,2.00001f,5.25f,8f,10.3f,13f,32f};
        QualityPixels1932.Plan p=QualityPixels1932.plan(null,800,30000000L,ShotContext1932.LENS_FRONT,beauty,3,4,true,true,1f);
        if(spatial){Constructor<SpatialNoise1934> c=SpatialNoise1934.class.getDeclaredConstructor(int.class,int.class,int.class,int.class,float.class,float.class,float.class,float.class,float[].class,QualityPixels1932.NoiseStats.class);c.setAccessible(true);
            SpatialNoise1934 n=c.newInstance(w,h,3,3,.5f,.75f,7.25f,8.5f,grid,null);p=p.withLocalNoise(n,3);}
        p=p.withFaceRegions(new QualityPixels1932.RegionMask(){public int skinQ8(int x,int y){return new int[]{0,1,128,255,256}[(x+y)%5];}public int detailQ8(int x,int y){return (x*7+y*11)%257;}});
        int[] u=new int[32];float[] f=new float[32];u[0]=4;u[1]=w;u[2]=rows;u[3]=first;u[6]=p.shadowBudgetQ8;u[7]=p.beautyQ8;u[8]=strong?1:0;u[9]=3;u[10]=3;u[11]=spatial?1:0;f[0]=p.sourceSigma;f[1]=.5f;f[2]=.75f;f[3]=7.25f;f[4]=8.5f;
        int[] masks=new int[w*rows*2],expected=new int[masks.length];float[] sigma=new float[w*rows];
        for(int y=0;y<rows;y++)for(int x=0;x<w;x++){int i=y*w+x;masks[i*2]=p.skinAt(x,y+first);masks[i*2+1]=p.detailAt(x,y+first);
            // Exact published .60 QualityPipeline Worker policy, before any GX adapter.
            int budget=256-((p.skinAt(x,y+first)*p.beautyQ8)>>9);if(!strong)budget=p.shadowBudgetQ8*budget>>8;
            expected[i*2]=Math.max(0,Math.min(256,budget));expected[i*2+1]=Math.max(0,Math.min(256,p.detailAt(x,y+first)));sigma[i]=p.localSigmaAt(x,y+first);}
        record("policy-"+strong+"-"+spatial+"-"+beauty,2,u,w*rows,new int[][]{new int[1],new int[1],new int[w*rows],new int[w*rows*2],bits(grid),masks,bits(f)},expected,bits(sigma));
    }
    public static void main(String[] args)throws Exception{
        out=new DataOutputStream(new FileOutputStream(args[0]));out.writeInt(1960001);
        for(int[] size:new int[][]{{1,1},{2,3},{7,9},{31,33},{65,67}})for(int mode=0;mode<3;mode++)for(int kind=0;kind<4;kind++)preparation(size[0],size[1],kind,kind%2==0?4:1,kind%2==0,mode);
        for(int[] size:new int[][]{{7,9},{31,33},{65,67},{67,131}})for(int kind=0;kind<4;kind++)strong(size[0],size[1],kind,kind%2==0?4:1,kind%2==0,kind%3);
        for(int[] size:new int[][]{{1,1},{7,9},{17,19},{33,35}})for(int kind=0;kind<4;kind++)single(size[0],size[1],kind,kind%2==0?4:1,kind%2==0,kind%3);
        for(int[] size:new int[][]{{9,11},{33,35},{65,67}})for(int kind=0;kind<4;kind++)for(int mode=0;mode<3;mode++)analysis(size[0],size[1],kind,mode);
        for(int turn:new int[]{0,90,180,270}){geometry(17,19,turn,turn%180==0?17:19,turn%180==0?19:17);geometry(31,33,turn,23,27);geometry(17,19,turn,37,41);}
        for(boolean strong:new boolean[]{false,true})for(boolean spatial:new boolean[]{false,true})for(float beauty:new float[]{0f,.4f,1f})policy(strong,spatial,beauty);
        out.writeUTF("");out.close();System.out.println("{\"status\":\"passed\",\"records\":"+records+",\"referencePixels\":"+pixels+",\"referenceNativeEnabled\":false}");
    }
}

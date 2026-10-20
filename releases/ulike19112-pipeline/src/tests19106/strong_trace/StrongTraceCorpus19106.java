package com.hiro.ulike;
import java.io.*;
import java.util.*;
import java.util.concurrent.CancellationException;
/** Synthetic model/input corpus: instrumentation proof, not a physical photo certificate. */
public final class StrongTraceCorpus19106 {
    static long assertions,checkpoints,pixels;static int records,nlmOn,nlmOff,sparse,nearHalf;
    static void check(boolean ok,String why){assertions++;if(!ok)throw new AssertionError(why);}
    static int clip(int n){return Math.max(0,Math.min(255,n));}
    static int[] image(int w,int h,int kind){Random r=new Random(1910600+kind);int[] a=new int[w*h];for(int y=0;y<h;y++)for(int x=0;x<w;x++){
        int n=r.nextInt(7)-3,c=r.nextInt(5)-2,edge=kind%2==0?0:((x/32+y/32)%2)*9;
        int p=0xff000000|clip(104+n+c+edge)<<16|clip(86+n+edge)<<8|clip(60+n-c+edge);
        if(kind==3&&(x+7*y)%113==0)p&=0x7fffffff;a[y*w+x]=p;
    }return a;}
    static int[] map(int w,int h,int scale,int kind){int mw=(w+scale-1)/scale,mh=(h+scale-1)/scale;int[] a=new int[mw*mh];Random r=new Random(341+kind*17+scale);for(int y=0;y<mh;y++)for(int x=0;x<mw;x++)a[y*mw+x]=(80+(x+y)%31)<<24|((r.nextInt(7)-3)&255)<<16|((r.nextInt(7)-3)&255)<<8|((r.nextInt(7)-3)&255);return a;}
    static void ints(DataOutputStream out,int[] a)throws Exception{out.writeInt(a.length);for(int v:a)out.writeInt(v);}
    static void record(DataOutputStream out,String name,int[] full,int w,int h,int y,int x,int kind,int[][] maps,float[] ev)throws Exception {
        int lo=Math.max(0,y-18),hi=Math.min(h,y+19);int[] band=Arrays.copyOfRange(full,lo*w,hi*w),original=band.clone(),policy=new int[w*2];
        for(int px=0;px<w;px++){policy[2*px]=kind==3&&(px+y)%11==0?0:kind==1?173:256;policy[2*px+1]=kind==1?(px*7+y*11)%257:0;}
        int[] oldPolicy=policy.clone(),u=new int[32];u[0]=w;u[1]=hi-lo;u[2]=y-lo;u[3]=u[2]+1;u[5]=hi-lo;u[6]=lo;u[7]=h;u[8]=4;u[9]=kind%2;u[10]=3;u[11]=1;u[30]=x;
        StrongNoise1958.Model model=StrongNoise1958.model1961(w,h,maps[0],maps[1],maps[2],Arrays.copyOf(ev,64),ev);
        int[] javaRow=new int[band.length],nativeRow=new int[band.length];Arrays.fill(javaRow,0x12345678);Arrays.fill(nativeRow,0x12345678);
        check(StrongNoise1958.diagnosticCpu19105(band,javaRow,u,model,policy,true)==2,"explicit Java");
        check(StrongNoise1958.diagnosticCpu19105(band,nativeRow,u,model,policy,false)==1,"actual native");
        check(Arrays.equals(javaRow,nativeRow),"native / Java row mismatch "+name);pixels+=w;
        int[] trace=StrongNoise1958.diagnosticTrace19106(band,u,model,policy,x);
        check(trace.length==4096&&trace[2048+93]==1,"complete final checkpoint");check(trace[93]==javaRow[u[2]*w+x],"trace / ordinary Java output "+name);
        check(Arrays.equals(band,original)&&Arrays.equals(policy,oldPolicy),"immutable inputs");
        for(int id=0;id<2048;id++){check(trace[2048+id]==0||trace[2048+id]==1,"boolean presence");if(trace[2048+id]==1)checkpoints++;}
        if(trace[2048+31]==1){if(trace[31]==1)nlmOn++;else nlmOff++;}else sparse++;
        if(trace[2048+90]==1)for(int id=90;id<=92;id++){float v=Float.intBitsToFloat(trace[id]);if(Math.abs((v-(float)Math.floor(v))-.5f)<.01f)nearHalf++;}
        if(records==0){
            int[] invalid=u.clone();invalid[3]++;boolean rejected=false;try{StrongNoise1958.diagnosticTrace19106(band,invalid,model,policy,x);}catch(IllegalArgumentException e){rejected=true;}check(rejected,"multiple rows rejected");
            Thread.currentThread().interrupt();boolean cancelled=false;try{StrongNoise1958.diagnosticTrace19106(band,u,model,policy,x);}catch(CancellationException e){cancelled=true;}finally{Thread.interrupted();}check(cancelled,"trace cancellation");
        }
        int[] evidence=new int[ev.length];for(int n=0;n<ev.length;n++)evidence[n]=Float.floatToRawIntBits(ev[n]);
        out.writeUTF(name);ints(out,u);ints(out,band);ints(out,policy);ints(out,evidence);for(int[] m:maps)ints(out,m);ints(out,Arrays.copyOfRange(javaRow,u[2]*w,u[3]*w));ints(out,trace);records++;
    }
    public static void main(String[] args)throws Exception {
        check(StrongNoise1958.nativeAvailable(),"native JNI loaded");
        try(DataOutputStream out=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(args[0])))){
            out.writeInt(1910601);
            for(int kind=0;kind<4;kind++){
                int w=kind<2?65:3060,h=kind<2?81:540;int[] full=image(w,h,kind);int[][] maps={map(w,h,2,kind),map(w,h,4,kind),map(w,h,8,kind)};
                float[] ev=new float[16+3*((w+63)/64)*((h+63)/64)];Arrays.fill(ev,0,16,3f);
                for(int i=16;i<ev.length;i+=3){ev[i]=2.2f+(i%11)*.13f;ev[i+1]=2.7f+(i%7)*.17f;ev[i+2]=kind==1?.25f:0f;}
                int[] ys=kind<2?new int[]{0,1,31,32,63,80}:new int[]{0,94,255,256,278,520,539};
                for(int y:ys)for(int x:new int[]{0,Math.min(w-1,32),w-1})record(out,"kind"+kind+" y"+y+" x"+x,full,w,h,y,x,kind,maps,ev);
                if(kind==0){
                    // Deterministic scan selects a genuinely near-half final value, without changing expected arithmetic.
                    int bestX=0;float best=1;int y=40,lo=y-18;int[] band=Arrays.copyOfRange(full,lo*w,(y+19)*w),policy=new int[w*2],u=new int[32];for(int x=0;x<w;x++)policy[x*2]=256;
                    u[0]=w;u[1]=37;u[2]=18;u[3]=19;u[5]=37;u[6]=lo;u[7]=h;u[8]=4;u[10]=3;u[11]=1;StrongNoise1958.Model model=StrongNoise1958.model1961(w,h,maps[0],maps[1],maps[2],Arrays.copyOf(ev,64),ev);
                    for(int x=0;x<w;x++){int[] trace=StrongNoise1958.diagnosticTrace19106(band,u,model,policy,x);for(int id=90;id<=92;id++){float v=Float.intBitsToFloat(trace[id]),d=Math.abs((v-(float)Math.floor(v))-.5f);if(d<best){best=d;bestX=x;}}}
                    check(best<.01f,"near-half fixture exists");record(out,"near-half x"+bestX,full,w,h,y,bestX,kind,maps,ev);
                }
            }
            out.writeUTF("");
        }
        check(nlmOn>0&&nlmOff>0&&sparse>0&&nearHalf>0,"branch and rounding coverage");
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"records\":"+records+",\"cpu_row_pixels\":"+pixels+",\"checkpoints\":"+checkpoints+",\"nlm_on\":"+nlmOn+",\"nlm_off\":"+nlmOff+",\"sparse\":"+sparse+",\"near_half\":"+nearHalf+"}");
    }
}

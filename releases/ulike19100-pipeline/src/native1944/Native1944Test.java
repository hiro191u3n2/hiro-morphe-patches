package com.hiro.ulike;
import java.util.*;

/** Independent direct-source oracle, preserving the v1943 integer loop order. */
public final class Native1944Test {
    static final int[] OFF={-4,-2,-1,0,1,2,4},WIDE={1,3,5,6,5,3,1},SP={1,2,3,4,3,2,1};
    static int comparisons,cases;
    static int y(int p){return (77*((p>>>16)&255)+150*((p>>>8)&255)+29*(p&255)+128)>>>8;}
    static int texture(int[] src,int w,int x,int row,int lo,int hi,int threshold,int radius){
        if(radius<3 || x<radius || x>=w-radius || row<lo+radius || row>=hi-radius)return 0;
        int[] samples=new int[9];int best=0,len=radius*2+1,at=row*w+x;
        for(int dir=0;dir<4;dir++){
            int step=dir==0?1:dir==1?w:dir==2?w+1:w-1;
            boolean opaque=true;int min=255,max=0,adj=0;
            for(int i=0;i<len;i++){
                int p=src[at+(i-radius)*step];if((p>>>24)!=255){opaque=false;break;}
                int v=samples[i]=y(p);min=Math.min(min,v);max=Math.max(max,v);if(i>0)adj+=Math.abs(v-samples[i-1]);
            }
            if(!opaque || max-min<threshold*2 || adj<(len-1)*threshold)continue;
            int avg=(adj+(len-2)/2)/(len-1);
            for(int period=2;period<=Math.min(4,len-4);period++){
                int err=0,count=len-period;
                for(int i=0;i<count;i++)err+=Math.abs(samples[i]-samples[i+period]);
                err=(err+count/2)/count;
                int c=Math.max(0,Math.min(256,(avg*4-err*9)*256/Math.max(1,avg*4)));c=c*c>>8;best=Math.max(best,c);
            }
        }
        return best;
    }
    static int[] oracle(int[] src,int[] meta,int w,int begin,int end,int lo,int hi,int radius,int[] range){
        int[] out=new int[w*(end-begin)*3];int taps=radius==4?7:radius*2+1;
        for(int row=begin;row<end;row++)for(int col=0;col<w;col++){
            int i=(row-begin)*w+col;if(meta[i]>=0)continue;
            int c=src[row*w+col];if((c>>>24)!=255)continue;
            int yc=y(c),cr=(c>>>16)&255,cg=(c>>>8)&255,cb=c&255,sigma=meta[i]&63;
            int weight=0,rr=0,gg=0,bb=0,nw=0,nr=0,ng=0,nb=0;
            int l=0,r=0,u=0,d=0,lc=0,rc=0,uc=0,dc=0,min=yc,max=yc;
            for(int yi=0;yi<taps;yi++){
                int dy=radius==4?OFF[yi]:yi-radius,sy=Math.max(lo,Math.min(hi-1,row+dy));
                for(int xi=0;xi<taps;xi++){
                    int dx=radius==4?OFF[xi]:xi-radius,sx=Math.max(0,Math.min(w-1,col+dx));
                    int p=src[sy*w+sx];if((p>>>24)!=255)continue;
                    int yy=y(p),red=(p>>>16)&255,green=(p>>>8)&255,blue=p&255;
                    if(dx<0){l+=yy;lc++;}else if(dx>0){r+=yy;rc++;}
                    if(dy<0){u+=yy;uc++;}else if(dy>0){d+=yy;dc++;}
                    min=Math.min(min,yy);max=Math.max(max,yy);
                    int ld=Math.abs(yy-yc),cd=Math.max(Math.abs((red-green)-(cr-cg)),Math.abs((blue-green)-(cb-cg)));
                    if(ld>Math.min(56,8+sigma*3) || cd>Math.min(48,8+sigma*2))continue;
                    int wt=(radius==4?WIDE[xi]*WIDE[yi]:SP[dx+3]*SP[dy+3])*range[sigma*256+ld];
                    if(cd>8)wt=wt*8/cd;if(wt==0)continue;
                    weight+=wt;rr+=wt*red;gg+=wt*green;bb+=wt*blue;
                    if(Math.abs(dx)<=1 && Math.abs(dy)<=1){nw+=wt;nr+=wt*red;ng+=wt*green;nb+=wt*blue;}
                }
            }
            if(weight==0 || nw==0)continue;
            int edge=Math.max(lc==0||rc==0?0:Math.abs(l/lc-r/rc),uc==0||dc==0?0:Math.abs(u/uc-d/dc));
            int periodic=texture(src,w,col,row,lo,hi,(meta[i]>>>6)&63,radius);
            out[i*3]=((nr+nw/2)/nw)<<16|((ng+nw/2)/nw)<<8|(nb+nw/2)/nw;
            out[i*3+1]=((rr+weight/2)/weight)<<16|((gg+weight/2)/weight)<<8|(bb+weight/2)/weight;
            out[i*3+2]=0x80000000|periodic<<16|edge<<8|(max-min);
        }
        return out;
    }
    static void req(boolean b,String why){if(!b)throw new AssertionError(why);}
    static int testVerticalBatches(Random random){
        int checks=0;
        for(int fixture=0;fixture<240;fixture++){
            int count=1+random.nextInt(119),taps=1+random.nextInt(128),first=random.nextInt(7);
            float[][] rows=new float[taps][];float[] weights=new float[first+taps];
            float[] a=new float[count],min=new float[count],max=new float[count];
            Arrays.fill(min,Float.POSITIVE_INFINITY);Arrays.fill(max,Float.NEGATIVE_INFINITY);
            float[] ea=a.clone(),emin=min.clone(),emax=max.clone();
            float[] oldA=a.clone(),oldMin=min.clone(),oldMax=max.clone();
            for(int t=0;t<taps;t++){
                weights[first+t]=(random.nextFloat()-.23f)*.17f;
                rows[t]=new float[count];
                for(int i=0;i<count;i++)rows[t][i]=fixture%7==0?Float.MIN_VALUE*(1+random.nextInt(8)):random.nextFloat()*255f;
                float weight=weights[first+t];
                for(int i=0;i<count;i++){
                    float v=rows[t][i];ea[i]+=v*weight;if(v<emin[i])emin[i]=v;if(v>emax[i])emax[i]=v;
                }
                req(NativeSpeed1935.verticalAdd(oldA,oldMin,oldMax,rows[t],weight,count),"retained vertical JNI");
            }
            req(NativeSpeed1944.verticalBatch(rows,weights,first,taps,a,min,max,count),"batched JNI availability");
            for(int i=0;i<count;i++){
                req(Float.floatToRawIntBits(a[i])==Float.floatToRawIntBits(ea[i]) &&
                    Float.floatToRawIntBits(a[i])==Float.floatToRawIntBits(oldA[i]),"batch sum bits fixture="+fixture+" index="+i);
                req(Float.floatToRawIntBits(min[i])==Float.floatToRawIntBits(emin[i]) &&
                    Float.floatToRawIntBits(max[i])==Float.floatToRawIntBits(emax[i]),"batch extrema bits");
                checks+=3;
            }
        }
        float[] a={1,2},min={3,4},max={5,6};
        req(!NativeSpeed1944.verticalBatch(new float[][]{{1,2}},new float[]{Float.NaN},0,1,a,min,max,2),"nonfinite weight accepted");
        req(Arrays.equals(a,new float[]{1,2}) && Arrays.equals(min,new float[]{3,4}) && Arrays.equals(max,new float[]{5,6}),"invalidbatch mutated");
        return checks;
    }
    static int testFullResize(Random random){
        int checks=0;
        for(int fixture=0;fixture<240;fixture++){
            final int sw=1+random.nextInt(91),sh=1+random.nextInt(79),w=1+random.nextInt(109),h=1+random.nextInt(93);
            double left=random.nextDouble()*(sw-.25)*.17,top=random.nextDouble()*(sh-.25)*.17;
            double cw=(sw-left)*(.21+random.nextDouble()*.79),ch=(sh-top)*(.21+random.nextDouble()*.79);
            final int[] src=new int[sw*sh],actual=new int[w*h],expected=new int[w*h];
            for(int i=0;i<src.length;i++)src[i]=0xff000000|random.nextInt(0x1000000);
            FastPixels1933.Plan plan=FastPixels1933.prepare(sw,sh,w,h,left,top,cw,ch);
            FastPixels1933Oracle1944.Plan original=FastPixels1933Oracle1944.prepare(sw,sh,w,h,left,top,cw,ch);
            if(fixture%3==0){plan=FastPixels1933.limitCache(plan,1);original=FastPixels1933Oracle1944.limitCache(original,1);}
            FastPixels1933.runRows(plan,(row,pixels)->System.arraycopy(src,row*sw,pixels,0,sw),
                (row,pixels)->System.arraycopy(pixels,0,actual,row*w,w),0,h);
            FastPixels1933Oracle1944.runRows(original,(row,pixels)->System.arraycopy(src,row*sw,pixels,0,sw),
                (row,pixels)->System.arraycopy(pixels,0,expected,row*w,w),0,h);
            for(int i=0;i<actual.length;i++){req(actual[i]==expected[i],"fullresize fixture="+fixture+" pixel="+i);checks++;}
        }
        return checks;
    }
    static void verify(int[] src,int[] meta,int w,int rows,int begin,int end,int lo,int hi,int radius,int[] range){
        int[] expected=oracle(src,meta,w,begin,end,lo,hi,radius,range),out=new int[expected.length];
        int[] before=src.clone();req(NativeSpeed1944.aggregate(src,meta,out,w,rows,begin,end,lo,hi,radius,range),"native availability");
        req(Arrays.equals(src,before),"input mutated");
        for(int i=0;i<out.length;i++){
            if(i%3!=2 && expected[i/3*3+2]==0)continue;
            req(out[i]==expected[i],"case="+cases+" index="+i+" expected="+expected[i]+" actual="+out[i]+" w="+w+" radius="+radius);
            comparisons++;
        }
        cases++;
    }
    public static void main(String[] args)throws Exception{
        req(NativeSpeed1944.available(),"new native library failed to load");
        int[] range=new int[33*256];for(int sigma=1;sigma<=32;sigma++)for(int d=0;d<256;d++)range[sigma*256+d]=(int)Math.round(256.0*Math.exp(-(double)d*d/(2.0*sigma*sigma)));
        Random random=new Random(19441943L);
        int batchChecks=testVerticalBatches(random);
        int resizeChecks=testFullResize(random);
        for(int fixture=0;fixture<120;fixture++)for(int radius=1;radius<=4;radius++){
            int w=fixture%10==0?1:1+random.nextInt(73),rows=1+random.nextInt(45);
            int lo=random.nextInt(rows),hi=lo+1+random.nextInt(rows-lo),begin=lo+random.nextInt(hi-lo),end=begin+1+random.nextInt(hi-begin);
            int[] src=new int[w*rows],meta=new int[w*(end-begin)];
            for(int i=0;i<src.length;i++){
                int x=i%w,b=fixture%5==0?(x%2==0?65:82):fixture%5==1?65+random.nextInt(20):random.nextInt(256);
                src[i]=fixture%5<=1?0xff000000|b<<16|b<<8|b:0xff000000|random.nextInt(0x1000000);
                if(fixture%7==0 && random.nextInt(8)==0)src[i]&=0x00ffffff;
            }
            for(int i=0;i<meta.length;i++)meta[i]=(random.nextInt(9)==0?0:0x80000000)|(2+random.nextInt(31))|(3+random.nextInt(30))<<6;
            verify(src,meta,w,rows,begin,end,lo,hi,radius,range);
        }
        int w=4080,rows=40;int[] src=new int[w*rows];
        for(int i=0;i<src.length;i++){int b=48+(i%w+(i/w))*7%29;src[i]=0xff000000|b<<16|b<<8|b;}
        for(int begin=0;begin<rows;begin+=8){int end=Math.min(rows,begin+8),n=w*(end-begin);int[] meta=new int[n];Arrays.fill(meta,0x80000000|18|(7<<6));verify(src,meta,w,rows,begin,end,0,rows,4,range);}
        int[] source={0xff020304},meta={0x80000000|1|(3<<6)},out={1,2,3};
        req(!NativeSpeed1944.aggregate(source,meta,out,1,1,0,1,0,1,1,range),"invalid sigma accepted");
        req(Arrays.equals(out,new int[]{1,2,3}),"invalid call changed summaries");
        Throwable[] errors=new Throwable[4];Thread[] threads=new Thread[4];
        for(int t=0;t<threads.length;t++){final int k=t;threads[t]=new Thread(()->{
            try{int[] s=new int[37*17],m=new int[37*13];Arrays.fill(s,0xff414141);Arrays.fill(m,0x80000000|9|(4<<6));
                for(int j=0;j<20;j++){int[] o=new int[m.length*3];req(NativeSpeed1944.aggregate(s,m,o,37,17,2,15,0,17,4,range),"parallelcall");for(int i=0;i<m.length;i++)req(o[i*3]==0x414141 && o[i*3+1]==0x414141 && o[i*3+2]==0x80000000,"parallel corruption");}}
            catch(Throwable e){errors[k]=e;}
        });threads[t].start();}
        for(Thread thread:threads)thread.join();for(Throwable err:errors)if(err!=null)throw new AssertionError(err);
        System.out.println("PASS native1944 JNI vs independent Java aggregate: cases="+cases+" exact_int_comparisons="+comparisons+" vertical_batches=240 exact_float_comparisons="+batchChecks+" full_resize_fixtures=240 exact_resize_pixels="+resizeChecks+" parallel=80 invalid_no_write=true input_immutable=true");
    }
}

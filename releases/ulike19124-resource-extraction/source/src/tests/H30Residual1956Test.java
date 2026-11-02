package com.hiro.ulike;
import java.util.Arrays;
import java.util.Random;

/** Published .55 source is the oracle; counters live only in generated test copies. */
public final class H30Residual1956Test {
    static long pixels,changed,protectedPixels,partialPixels;static int cases;
    static final Random RANDOM=new Random(19561955L);
    static void require(boolean b,String why){if(!b)throw new AssertionError(why);}
    static int[] image(int w,int h,int mode){
        int[] out=new int[w*h];
        for(int row=0;row<h;row++)for(int col=0;col<w;col++){
            int v=mode==0?40+RANDOM.nextInt(21):mode==1?(((col+row)&1)==0?38:68):
                mode==2?(((col+row)&1)==0?38:68)+RANDOM.nextInt(3):
                mode==3?(col<w/2?40:160):mode==4?127:40+RANDOM.nextInt(21);
            int alpha=mode==5?(col%4==0?128:255):mode==6?0:255;
            out[row*w+col]=(alpha<<24)|(v<<16)|(v<<8)|v;
        }
        return out;
    }
    static void compare(int w,int h,int mode,int radius,int kind,boolean saved){
        int[] source=image(w,h,mode),original=source.clone(),seed=source.clone();
        if(!saved && kind==1)Arrays.fill(seed,0x61473521);
        int[] expected=seed.clone(),actual=seed.clone();
        int lo=h>12?1:0,hi=h>12?h-1:h,begin=kind==2?lo+(hi-lo)/3:lo,end=kind==2?hi-(hi-lo)/4:hi;
        int noise=kind==3?0:4;
        QualityPixels1932.Plan plan=kind==0?null:QualityPixels1932.plan(
            new QualityPixels1932.NoiseStats(6,8,88,.2f,2048),1600,33000000L,
            QualityPixels1932.LENS_FRONT,kind==2?0:1,noise,3,true,true,1);
        QualityShadowH30Oracle.smoothRange(source,expected,w,h,begin,end,lo,hi,noise,true,radius,plan,0);
        if(saved)QualityShadow1932.smoothSavedRange1951(source,actual,w,h,begin,end,lo,hi,noise,true,radius,plan,0);
        else QualityShadow1932.smoothRange(source,actual,w,h,begin,end,lo,hi,noise,true,radius,plan,0);
        require(Arrays.equals(source,original),"source mutation");
        for(int i=0;i<source.length;i++){
            require(expected[i]==actual[i],"pixel mismatch case="+cases+" i="+i+" radius="+radius+" kind="+kind);
            if(expected[i]!=seed[i])changed++;
            int row=i/w,col=i%w;
            if(row<begin||row>=end||(source[i]>>>24)!=255||noise==0)
                require(actual[i]==seed[i],"untouched seed changed");
            if(row>=begin&&row<end&&(source[i]>>>24)==255&&noise>0){
                float sigma=plan==null?3f+noise*1.8f:plan.localSigmaAt(col,row);
                int gate=QualityShadowH30Oracle.textureQ8(source,w,h,col,row,lo,hi,sigma,radius);
                if(gate==256){protectedPixels++;require(actual[i]==seed[i],"protected pixel seed changed");}
                else if(gate>0)partialPixels++;
            }
            pixels++;
        }
        cases++;
    }
    public static void main(String[] args){
        int[][] sizes={{1,1},{7,9},{17,19},{33,31},{65,49}};
        for(int[] size:sizes)for(int mode=0;mode<7;mode++)for(int radius=1;radius<=4;radius++)for(int kind=0;kind<4;kind++)
            compare(size[0],size[1],mode,radius,kind,(mode+kind)%2==0);
        require(changed>0&&protectedPixels>0&&partialPixels>0,"fixtures lack changing/protected/partial pixels");
        if(!NativeSpeed1944.available())require(QualityShadow1932.H30_SKIPPED>0&&QualityShadow1932.H30_TAPS<QualityShadowH30Oracle.H30_TAPS,"Java weighted work was not reduced");
        System.out.println("{\"status\":\"passed\",\"cases\":"+cases+",\"pixels\":"+pixels+",\"changed\":"+changed+
            ",\"protected\":"+protectedPixels+",\"partiallyProtected\":"+partialPixels+",\"jniExecuted\":"+NativeSpeed1944.available()+
            ",\"baselineTaps\":"+QualityShadowH30Oracle.H30_TAPS+",\"candidateJavaTaps\":"+QualityShadow1932.H30_TAPS+
            ",\"candidateJavaSkipped\":"+QualityShadow1932.H30_SKIPPED+"}");
    }
}

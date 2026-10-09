package com.hiro.ulike;

import java.io.DataOutputStream;
import java.io.FileOutputStream;

/** Independent CPU fixture generator compiled against frozen published .63.
 * It is test-only and never included in the distributed Android patch. */
public final class ModelOracle1965 {
    private static int bound(int v){return Math.max(0,Math.min(255,v));}
    private static int pixel(int x,int y,int seed,int kind) {
        int bits=(x+13)*0x1f123bb5^(y+29)*0x5f356495^seed;
        bits^=bits>>>16;bits*=0x7feb352d;bits^=bits>>>15;
        int alpha=255,level=kind==0?128:kind==1?12+(x/64+y/64)%8*30:kind==2?126:kind==3?2+(x/32)%8*35:120;
        int noise=kind==0?0:((bits&31)-15),r=bound(level+noise),g=bound(level+((bits>>>5&31)-15)),b=bound(level+((bits>>>10&31)-15));
        if(kind==0)r=g=b=level;
        if(kind==2){int edge=(x/8&1)*22;r=bound(r+edge);g=bound(g+edge);b=bound(b+edge);}
        if(kind==3&&(x+y)%23==0)alpha=127;
        if(kind==4){r=bound(r+((bits>>>15&63)-31));b=bound(b+((bits>>>21&63)-31));}
        return alpha<<24|r<<16|g<<8|b;
    }
    private static void words(DataOutputStream out,int[] values)throws Exception {
        out.writeInt(values.length);for(int value:values)out.writeInt(value);
    }
    private static void record(DataOutputStream out,int width,int height,final int seed,final int kind)throws Exception {
        SingleNoise1955.Patches patches=new SingleNoise1955.Patches(){public void read(int[] destination,int x,int y,int w,int h){for(int yy=0;yy<h;yy++)for(int xx=0;xx<w;xx++)destination[yy*w+xx]=pixel(x+xx,y+yy,seed,kind);}};
        SingleNoise1955.Model model=SingleNoise1955.probe(patches,width,height);
        int pw=Math.min(48,width),ph=Math.min(48,height),cols=model.columns,rows=model.rows;
        int[] pixels=new int[pw*ph*cols*rows],patch=new int[pw*ph];
        for(int gy=0;gy<rows;gy++)for(int gx=0;gx<cols;gx++) {
            int px=cols==1?(width-pw)/2:Math.round((float)(width-pw)*gx/(cols-1));
            int py=rows==1?(height-ph)/2:Math.round((float)(height-ph)*gy/(rows-1));
            patches.read(patch,px,py,pw,ph);System.arraycopy(patch,0,pixels,(gy*cols+gx)*patch.length,patch.length);
        }
        out.writeUTF(width+"x"+height+":kind"+kind+":seed"+seed);
        out.writeInt(width);out.writeInt(height);out.writeInt(cols);out.writeInt(rows);out.writeInt(pw);out.writeInt(ph);
        words(out,pixels);float[] data=model.gpuData1960();int[] expected=new int[data.length+3];
        for(int i=0;i<data.length;i++)expected[i]=Float.floatToRawIntBits(data[i]);
        expected[data.length]=Float.floatToRawIntBits(model.meanSigma());expected[data.length+1]=Float.floatToRawIntBits(model.meanChromaSigma());expected[data.length+2]=model.samples;
        words(out,expected);
        if(width<=65&&height<=65) {
            int[] source=new int[width*height],result=new int[source.length];patches.read(source,0,0,width,height);
            int noise=kind%4+1;boolean shadows=(seed&1)!=0;
            SingleNoise1955.processJavaRange(source,result,width,height,0,height,0,height,0,noise,shadows,model,null);
            out.writeInt(noise);out.writeInt(shadows?1:0);words(out,source);words(out,result);
        } else {out.writeInt(0);out.writeInt(0);words(out,new int[0]);words(out,new int[0]);}
    }
    public static void main(String[] args)throws Exception {
        int[][] sizes={{1,1},{7,9},{8,8},{13,19},{48,48},{49,47},{65,33},{257,259},{513,519},{4080,3060}};
        int records=0;
        try(DataOutputStream out=new DataOutputStream(new FileOutputStream(args[0]))) {
            out.writeInt(1965001);
            for(int[] size:sizes)for(int kind=0;kind<5;kind++){record(out,size[0],size[1],1965+kind,kind);records++;}
            out.writeUTF("");
        }
        System.out.println("{\"status\":\"passed\",\"records\":"+records+",\"referenceNativeEnabled\":"+SingleNoise1955.nativeAvailable()+"}");
    }
}

package com.hiro.ulike;
import java.io.*;
import java.lang.reflect.*;
import java.util.Arrays;
/** Compile this class against the independently frozen published .60 sources.
 * Binary records contain full outputs, not a digest or our new CPU helper. */
public final class Analysis1961Oracle {
    static final int[][] SPATIAL={{2,2,0},{63,65,1},{129,257,2},{1665,1665,1},{1665,1665,3},{65,63,4}};
    static final int[][] REGIONAL={{17,19,0},{31,33,1},{65,67,2},{127,129,3},{129,65,4}};
    static final int[][] RESIDENT={{17,19,1},{65,67,2},{96,98,3}};
    static int[] image(int w,int h,int pattern){
        int[] out=new int[w*h];int seed=0x695ac301;
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){
            seed^=seed<<13;seed^=seed>>>17;seed^=seed<<5;int n=(seed>>>1)%25-12;
            int r=128,g=128,b=128,a=255;
            if(pattern==1){r=128+n;g=128+((seed>>>6)&15)-8;b=128+((seed>>>12)&31)-16;}
            if(pattern==2){r=(x*3+y+n+8192)&255;g=(y*2+x+n+8192)&255;b=(x+y*3-n+8192)&255;if((seed&63)==0)a=127;}
            if(pattern==3){int k=((x/4+y/4)&1)==0?52:204;r=k+n;g=k-n;b=k+((seed>>>8)&7)-4;}
            if(pattern==4){r=g=b=((x+y)&3)==0?0:255;if((seed&7)==0)a=0;}
            out[y*w+x]=(a<<24)|(r<<16)|(g<<8)|b;
        }return out;
    }
    static Object field(Object o,String n)throws Exception{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
    static int[] bits(float[] f){int[] out=new int[f.length];for(int i=0;i<f.length;i++)out[i]=Float.floatToRawIntBits(f[i]);return out;}
    static int[] spatialValues(SpatialNoise1934 n)throws Exception{
        float[] grid=(float[])field(n,"sigma");int[] out=new int[5+grid.length];out[0]=Float.floatToRawIntBits(n.global.lumaSigma);out[1]=Float.floatToRawIntBits(n.global.chromaSigma);out[2]=Float.floatToRawIntBits(n.global.meanLuma);out[3]=Float.floatToRawIntBits(n.global.detailFraction);out[4]=n.global.samples;System.arraycopy(bits(grid),0,out,5,grid.length);return out;
    }
    static int[] stats(QualityPixels1932.NoiseStats s){return new int[]{Float.floatToRawIntBits(s.lumaSigma),Float.floatToRawIntBits(s.chromaSigma),Float.floatToRawIntBits(s.meanLuma),Float.floatToRawIntBits(s.detailFraction),s.samples};}
    static StrongNoise1958.Patches strong(final int[] image,final int width){return new StrongNoise1958.Patches(){public void read(int[] dst,int x,int y,int w,int h){for(int row=0;row<h;row++)System.arraycopy(image,(y+row)*width+x,dst,row*w,w);}};}
    static SpatialNoise1934.Patches spatial(final int[] image,final int width){return new SpatialNoise1934.Patches(){public void read(int[] dst,int x,int y,int w,int h){for(int row=0;row<h;row++)System.arraycopy(image,(y+row)*width+x,dst,row*w,w);}};}
    static float[] evidence(){float[] e=new float[16];for(int i=0;i<16;i++)e[i]=.25f+i*.1875f;return e;}
    static int[] packed(int[] image,int width,int height,boolean region){
        int pw=Math.min(region?32:64,width),ph=Math.min(region?32:64,height),nx=region?(width+63)/64:Math.max(1,Math.min(13,(width+127)/128)),ny=region?(height+63)/64:Math.max(1,Math.min(13,(height+127)/128));int[] out=new int[nx*ny*pw*ph];
        for(int gy=0;gy<ny;gy++)for(int gx=0;gx<nx;gx++){
            int x=region?Math.max(0,Math.min(width-pw,gx*64+32-pw/2)):nx==1?(width-pw)/2:Math.round((float)(width-pw)*gx/(nx-1));
            int y=region?Math.max(0,Math.min(height-ph,gy*64+32-ph/2)):ny==1?(height-ph)/2:Math.round((float)(height-ph)*gy/(ny-1));
            for(int row=0;row<ph;row++)System.arraycopy(image,(y+row)*width+x,out,(gy*nx+gx)*pw*ph+row*pw,pw);
        }return out;
    }
    static int[] half(int[] src,int w,int h){int ow=(w+1)/2,oh=(h+1)/2;int[] out=new int[ow*oh];for(int y=0;y<oh;y++)for(int x=0;x<ow;x++){
        int r=0,g=0,b=0,n=0;boolean opaque=true;for(int yy=y*2;yy<Math.min(h,y*2+2);yy++)for(int xx=x*2;xx<Math.min(w,x*2+2);xx++){int p=src[yy*w+xx];opaque&=(p>>>24)==255;r+=(p>>>16)&255;g+=(p>>>8)&255;b+=p&255;n++;}out[y*ow+x]=(opaque?0xff000000:0)|((r+n/2)/n<<16)|((g+n/2)/n<<8)|(b+n/2)/n;
    }return out;}
    static void ints(DataOutputStream out,int[] a)throws Exception{out.writeInt(a.length);for(int n:a)out.writeInt(n);}
    public static void main(String[] args)throws Exception{
        DataOutputStream out=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(args[0])));out.writeInt(19611);out.writeInt(SPATIAL.length+REGIONAL.length+RESIDENT.length);
        for(int[] c:SPATIAL){out.writeUTF("spatial");for(int i:c)out.writeInt(i);int[] img=image(c[0],c[1],c[2]);ints(out,spatialValues(SpatialNoise1934.probe(spatial(img,c[0]),c[0],c[1])));}
        Method region=StrongNoise1958.class.getDeclaredMethod("estimateRegions",StrongNoise1958.Patches.class,int.class,int.class,float[].class,float[].class);region.setAccessible(true);
        for(int[] c:REGIONAL){out.writeUTF("region");for(int i:c)out.writeInt(i);int[] img=image(c[0],c[1],c[2]);float[] rt=new float[16+3*((c[0]+63)/64)*((c[1]+63)/64)];region.invoke(null,strong(img,c[0]),c[0],c[1],rt,evidence());ints(out,bits(Arrays.copyOfRange(rt,16,rt.length)));}
        for(int[] c:RESIDENT){out.writeUTF("resident");for(int i:c)out.writeInt(i);int[] img=image(c[0],c[1],c[2]);StrongNoise1958.Model m=StrongNoise1958.prepareJava(strong(img,c[0]),c[0],c[1],4,true);ints(out,(int[])field(m,"halfMap"));ints(out,(int[])field(m,"quarterMap"));ints(out,(int[])field(m,"eighthMap"));ints(out,bits((float[])field(m,"evidence")));ints(out,bits((float[])field(m,"runtimeEvidence")));}
        out.writeUTF("spatialPrefix32768");out.writeInt(2);
        for(int pattern:new int[]{1,3}){
            int w=1665,h=1665;out.writeInt(w);out.writeInt(h);out.writeInt(pattern);int[] packed=packed(image(w,h,pattern),w,h,false),patch=new int[64*64];QualityPixels1932.NoiseProbe aggregate=new QualityPixels1932.NoiseProbe();
            for(int p=0;p<packed.length/(64*64);p++){System.arraycopy(packed,p*64*64,patch,0,patch.length);aggregate.add(patch,64,64);}ints(out,stats(aggregate.finish()));
        }
        out.close();System.out.println("Analysis1961Oracle frozen60 records="+(SPATIAL.length+REGIONAL.length+RESIDENT.length)+" prefixRecords=2");
    }
}

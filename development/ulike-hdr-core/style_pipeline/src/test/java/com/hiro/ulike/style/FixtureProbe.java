package com.hiro.ulike.style;
import java.io.*;

/** Private test protocol: caller-owned fixtures, not a production file reader. */
public final class FixtureProbe {
    private static double[] a(DataInputStream in) throws IOException {
        int n=in.readInt(); if(n<0 || n>4000000) throw new IOException("fixture budget");
        double[] a=new double[n];for(int i=0;i<n;i++)a[i]=in.readDouble();return a;
    }
    private static StyleLutPipeline.Texture tex(DataInputStream in) throws IOException {
        int w=in.readInt(),h=in.readInt();return new StyleLutPipeline.Texture(w,h,a(in));
    }
    public static void main(String[] args) throws Exception {
        try(DataInputStream in=new DataInputStream(new BufferedInputStream(new FileInputStream(args[0])));
            DataOutputStream out=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(args[1])))) {
            if(in.readInt()!=0x53545931) throw new IOException("magic");
            int count=in.readInt();if(count<1 || count>100)throw new IOException("case count");out.writeInt(count);
            for(int c=0;c<count;c++) {
                int kind=in.readInt(),n=in.readInt();
                SampledMakeupPipeline.FrameTile tile=new SampledMakeupPipeline.FrameTile("fixture",1,n,1,0,0,n,1);
                double[] photo=a(in),result;
                if(kind==0) {
                    int index=in.readInt(); double[] rgba=a(in),coverage=a(in);
                    double intensity=in.readDouble(),opacity=in.readDouble();
                    double[] color=in.readBoolean()?a(in):null;
                    double[] seg=null;boolean[] inside=null;
                    if(in.readBoolean()) {seg=a(in);inside=new boolean[n];for(int i=0;i<n;i++)inside[i]=in.readBoolean();}
                    double[] shaderBase=in.readBoolean()?a(in):null;
                    SampledMakeupPipeline.ResolvedPass p=new SampledMakeupPipeline.ResolvedPass(
                        SampledMakeupPipeline.Pass.values()[index],tile,rgba,coverage,intensity,opacity,color,seg,inside,shaderBase);
                    result=SampledMakeupPipeline.apply(SampledMakeupPipeline.Domain.ENCODED_SDR_FULL_RANGE,tile,photo,p);
                } else if(kind==1) {
                    StyleLutPipeline.Texture bg=tex(in),skin=tex(in); double[] mask=a(in);double alpha=in.readDouble();
                    result=StyleLutPipeline.skin(SampledMakeupPipeline.Domain.ENCODED_SDR_FULL_RANGE,tile,photo,bg,skin,mask,alpha);
                } else if(kind==2) {
                    StyleLutPipeline.Texture lut=tex(in);double alpha=in.readDouble();
                    result=StyleLutPipeline.finalLut(SampledMakeupPipeline.Domain.ENCODED_SDR_FULL_RANGE,tile,photo,lut,alpha);
                } else throw new IOException("unknown fixture kind");
                out.writeInt(result.length);for(double d:result)out.writeDouble(d);
            }
            if(in.read()!=-1)throw new IOException("trailing fixture bytes");
        }
    }
}

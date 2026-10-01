package com.hiro.ulike.hdr.gainmap;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** ISO 21496-1 version 0, three-channel, SDR base, separate-rational subset.
 * Map values are numerical gain (encoding gamma=1), not color/HLG samples. */
public final class IsoMetadata {
    private final byte[] payload;
    private final double[][] channels=new double[3][5];
    public final double baseHeadroomLog2,alternateHeadroomLog2;
    public IsoMetadata(GainmapMath.Metadata metadata) {
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        put(out,new byte[]{0,0,0,0,(byte)0xc0});
        byte[] base=rational(0,false),alternate=rational(metadata.alternateHeadroomLog2(),false);
        baseHeadroomLog2=value(base,false); alternateHeadroomLog2=value(alternate,false);
        if(baseHeadroomLog2>=alternateHeadroomLog2) GainmapMath.fail("headroom collapses at ISO precision");
        put(out,base); put(out,alternate);
        for(int c=0;c<3;c++) {
            double[] values={metadata.low(c),metadata.high(c),1,GainmapMath.OFFSET,GainmapMath.OFFSET};
            for(int j=0;j<5;j++) {
                byte[] encoded=rational(values[j],j!=2); put(out,encoded); channels[c][j]=value(encoded,j!=2);
            }
            if(metadata.low(c)!=metadata.high(c) && channels[c][0]==channels[c][1])
                GainmapMath.fail("gain range collapses at ISO precision");
        }
        payload=out.toByteArray();
    }
    public byte[] copyPayload() { return payload.clone(); }
    public double channel(int c,int parameter) { return channels[c][parameter]; }
    public double reconstruct(double base,int mapCode,int channel) {
        if(!GainmapMath.finite(base) || base<0 || base>1 || mapCode<0 || mapCode>1023)
            GainmapMath.fail("invalid actual-decoded reconstruction sample");
        double[] c=channels[channel];
        double logGain=c[0]+(c[1]-c[0])*StrictMath.pow(mapCode/1023.0,1.0/c[2]);
        return (base+c[3])*StrictMath.pow(2,logGain)-c[4];
    }
    private static byte[] rational(double value,boolean signed) {
        if(!GainmapMath.finite(value) || (!signed && value<0)) GainmapMath.fail("invalid ISO rational");
        long denominator=1<<24,maximum=signed?Integer.MAX_VALUE:0xffff_ffffL;
        while(denominator>1 && StrictMath.abs(value)*denominator>maximum) denominator>>=1;
        double rounded=StrictMath.rint(value*denominator);
        if(StrictMath.abs(rounded)>maximum) GainmapMath.fail("ISO rational overflow");
        long numerator=(long)rounded,g=gcd(StrictMath.abs(numerator),denominator);
        return ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN)
                .putInt((int)(numerator/g)).putInt((int)(denominator/g)).array();
    }
    private static long gcd(long a,long b) { while(b!=0) { long r=a%b; a=b; b=r; } return a; }
    private static double value(byte[] bytes,boolean signed) {
        ByteBuffer b=ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN); int n=b.getInt();
        long d=b.getInt()&0xffff_ffffL; return (signed?n:(n&0xffff_ffffL))/(double)d;
    }
    private static void put(ByteArrayOutputStream out,byte[] bytes) { out.write(bytes,0,bytes.length); }
}

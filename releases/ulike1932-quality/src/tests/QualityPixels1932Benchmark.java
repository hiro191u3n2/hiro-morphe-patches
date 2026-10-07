package com.hiro.ulike;

/** Host-only smoke measurement. These timings are not predictions for Galaxy hardware. */
public final class QualityPixels1932Benchmark {
    public static void main(String[] args) {
        final int sw=4080, sh=3060, dw=5712, dh=4284;
        final long[] checksum={0};
        final int[] rowReads={0};
        long started=System.nanoTime();
        QualityPixels1932.resize(new QualityPixels1932.RowSource() {
            public void readRow(int y,int[] pixels) {
                rowReads[0]++;
                for(int x=0;x<sw;x++) {
                    int g=48+(x*3+y*7)%136;
                    pixels[x]=0xff000000|((g+20)<<16)|(g<<8)|(g-10);
                }
            }
        },sw,sh,new QualityPixels1932.RowSink() {
            public void writeRow(int y,int[] pixels) {checksum[0]+=pixels[y%pixels.length];}
        },dw,dh);
        double seconds=(System.nanoTime()-started)/1e9;
        System.out.println("{\"kind\":\"host_only_not_device\",\"source_pixels\":"+(sw*sh)+
            ",\"output_pixels\":"+(dw*dh)+",\"row_reads\":"+rowReads[0]+
            ",\"resize_seconds\":"+seconds+",\"checksum\":"+checksum[0]+"}");
    }
}

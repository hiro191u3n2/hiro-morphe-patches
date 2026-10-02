package com.hiro.ulike;

/** Pixel-exact row accumulator with the old luma call inlined. */
public final class ChromaFast179 {
    private ChromaFast179(){}
    public static void row(int[] source,int width,int row,int[] sums,int weight){
        int base=row*width;
        for(int x=0;x<width;x++){
            int p=source[base+x];
            if((p>>>24)!=255) continue;
            int r=(p>>>16)&255,g=(p>>>8)&255,b=p&255;
            int y=(77*r+150*g+29*b+128)>>8;
            int cb=b-y, cr=r-y, k=x*7;
            int wy=weight*y;
            sums[k]+=wy;
            sums[k+1]+=wy*y;
            int wcb=weight*cb,wcr=weight*cr;
            sums[k+2]+=wcb;
            sums[k+3]+=wcr;
            sums[k+4]+=wcb*cb;
            sums[k+5]+=wcr*cr;
            sums[k+6]+=weight;
        }
    }
}

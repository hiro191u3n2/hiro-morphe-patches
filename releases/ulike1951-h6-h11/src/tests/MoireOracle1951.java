package com.hiro.ulike;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Random;

/** Binary oracle for the original published Java algorithm. */
public final class MoireOracle1951 {
    private MoireOracle1951() {}
    private static void write(DataOutputStream file,int width,int rows,int first,int last,int[] src) throws Exception {
        int[] reference=new int[src.length];
        Arrays.fill(reference,0x13579bdf);
        QualityPixels1932.finishStripAtBefore1951(src,reference,width,rows,first,last,null,true,false,0);
        file.writeInt(width);file.writeInt(rows);file.writeInt(first);file.writeInt(last);
        for(int pixel:src)file.writeInt(pixel);
        for(int pixel:reference)file.writeInt(pixel);
    }
    public static void main(String[] args) throws Exception {
        Random random=new Random(0x195135516L);
        try(DataOutputStream file=new DataOutputStream(new FileOutputStream(args[0]))) {
            for(int mode=0;mode<14;mode++) {
                int width=mode==0?7:mode==1?40:mode==2?65:mode==3?91:119;
                int rows=mode==0?9:mode==1?52:mode==2?66:mode==3?93:103;
                int[] src=new int[width*rows];
                for(int y=0;y<rows;y++)for(int x=0;x<width;x++) {
                    int red,green,blue,randomValue=random.nextInt(256);
                    switch(mode%7) {
                    case 0: red=randomValue;green=random.nextInt(256);blue=random.nextInt(256);break;
                    case 1: green=121;red=green+((x&1)==0?24:-24);blue=green-((x&1)==0?24:-24);break;
                    case 2: green=129;red=green+((y&3)<2?26:-26);blue=green-((y&3)<2?26:-26);break;
                    case 3: green=122;red=green+(int)Math.round(19*Math.sin(2*Math.PI*x/16));
                            blue=green-(red-green);break;
                    case 4: green=148;red=green+(int)Math.round(22*Math.sin(2*Math.PI*y/12));
                            blue=green-(red-green);break;
                    case 5: green=128;red=green+((x+y)%8<4?14:-14);blue=green+(x%3-1)*4;break;
                    default: green=red=blue=127+random.nextInt(5);break;
                    }
                    int alpha=random.nextInt(67)==0?random.nextInt(255):255;
                    src[y*width+x]=(alpha<<24)|(red<<16)|(green<<8)|blue;
                }
                write(file,width,rows,0,rows,src);
                if(rows>11)write(file,width,rows,5,rows-6,src);
                write(file,width,rows,Math.min(32,rows),Math.min(rows,63),src);
            }
        }
    }
}

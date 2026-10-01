package com.hiro.ulike.hdr.faceprobe;
import java.util.Arrays;
public final class OwnedRenderPixelsTest {
    private static int checks;
    private static void check(boolean value){checks++;if(!value)throw new AssertionError();}
    private static void reject(int[] p,int w,int h,int ew,int eh){checks++;try{OwnedRenderPixels.copy(p,w,h,ew,eh);throw new AssertionError();}catch(IllegalArgumentException expected){}}
    public static void main(String[] args){
        int[] source={0x01020304,0x12345678,0,0xffffffff,0x87654321,7};
        int[] copied=OwnedRenderPixels.copy(source,3,2,3,2);check(Arrays.equals(copied,source));check(copied!=source);
        source[0]=8;check(copied[0]==0x01020304);copied[1]=9;check(source[1]==0x12345678);
        reject(source,2,3,3,2);reject(null,3,2,3,2);reject(new int[5],3,2,3,2);reject(source,-3,-2,-3,-2);
        reject(source,Integer.MAX_VALUE,2,Integer.MAX_VALUE,2);reject(source,4096,4096,4096,4096);reject(new int[0],0,1,0,1);
        System.out.println("PASS owned diagnostic pixels "+checks+" checks; packed bits preserved, no channel/transfer calibration");
    }
}

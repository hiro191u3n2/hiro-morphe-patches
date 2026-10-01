package com.hiro.ulike.hdr.encoder;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Bounded HEVC Main10 single-IDR contract inspector, not a general HEVC decoder. */
public final class HevcProof {
    private HevcProof() {}
    public static final class Sps {
        public final int width, height, lumaBits, chromaBits, chroma, profile;
        public final int primaries, transfer, matrix;
        public final boolean fullRange;
        Sps(int w, int h, int l, int c, int chroma, int profile,
            int primaries, int transfer, int matrix, boolean full) {
            this.width=w; this.height=h; this.lumaBits=l; this.chromaBits=c;
            this.chroma=chroma; this.profile=profile; this.primaries=primaries;
            this.transfer=transfer; this.matrix=matrix; this.fullRange=full;
        }
    }
    static final class Bits {
        final byte[] bytes; int position;
        Bits(byte[] bytes) { this.bytes=bytes; }
        int get(int n) {
            if (n<0 || n>31 || (long) position+n > bytes.length*8L) fail("truncated SPS");
            int value=0;
            for(int i=0;i<n;i++,position++) value=(value<<1)|((bytes[position/8] >>> (7-position%8))&1);
            return value;
        }
        void skip(int n) { while(n>0) { int k=Math.min(24,n); get(k); n-=k; } }
        int ue(int maximum) {
            int z=0; while(get(1)==0) { if(++z>29) fail("oversized Exp-Golomb"); }
            long v=((1L<<z)-1)+get(z);
            if(v>maximum) fail("SPS field outside supported bound");
            return (int) v;
        }
        void se() { ue(1<<20); }
    }
    private static void fail(String why) { throw new IllegalArgumentException(why); }
    public static List<byte[]> annexB(byte[] data) {
        if(data==null || data.length<6 || data.length>64*1024*1024) fail("HEVC stream size");
        List<byte[]> nals=new ArrayList<>(); int start=-1;
        for(int i=0;i+2<data.length;i++) {
            int length=data[i]==0 && data[i+1]==0 && data[i+2]==1 ? 3 :
                (i+3<data.length && data[i]==0 && data[i+1]==0 && data[i+2]==0 && data[i+3]==1 ? 4 : 0);
            if(length==0) continue;
            if(start<0) { for(int z=0;z<i;z++) if(data[z]!=0) fail("expected Annex B"); }
            else addNal(nals,data,start,i);
            start=i+length; i+=length-1;
        }
        if(start<0) fail("expected Annex B start code");
        addNal(nals,data,start,data.length); return nals;
    }
    private static void addNal(List<byte[]> nals,byte[] data,int start,int end) {
        while(end>start && data[end-1]==0) end--;
        if(end-start<3 || nals.size()>=4096) fail("empty or excessive NAL units");
        byte[] n=Arrays.copyOfRange(data,start,end);
        if((n[0]&128)!=0 || ((n[0]&1)<<5 | ((n[1]&255)>>>3))!=0 || (n[1]&7)!=1)
            fail("only single-layer temporal-id-zero HEVC supported");
        nals.add(n);
    }
    static byte[] rbsp(byte[] n) {
        ByteArrayOutputStream out=new ByteArrayOutputStream(); int zeros=0;
        for(int i=2;i<n.length;i++) {
            int v=n[i]&255;
            if(zeros>=2 && v==3) {
                if(i+1>=n.length || (n[i+1]&255)>3) fail("invalid emulation prevention");
                zeros=0; continue;
            }
            if(zeros>=2 && v<=2) fail("unescaped NAL byte sequence");
            out.write(v); zeros=v==0 ? zeros+1 : 0;
        }
        return out.toByteArray();
    }
    public static Sps inspect(byte[] annexB,int width,int height,boolean fullRange) {
        byte[][] parameters=new byte[3][]; int firstSlices=0, vcl=0;
        for(byte[] nal:annexB(annexB)) {
            int type=(nal[0]>>>1)&63;
            if(type>=32 && type<=34) {
                int p=type-32;
                if(parameters[p]!=null && !Arrays.equals(parameters[p],nal)) fail("changing parameter sets");
                parameters[p]=nal;
            } else if(type<=31) {
                if(type!=19 && type!=20) fail("only IDR still images accepted");
                vcl++; if((rbsp(nal)[0]&128)!=0) firstSlices++;
            } else if(type!=35 && type!=39 && type!=40) fail("unsupported HEVC NAL type");
        }
        if(parameters[0]==null || parameters[1]==null || parameters[2]==null || vcl==0 || firstSlices!=1)
            fail("one IDR first-slice marker and VPS/SPS/PPS required");
        Sps s=parseSps(parameters[1]);
        if(s.width!=width || s.height!=height || s.lumaBits!=10 || s.chromaBits!=10
                || s.chroma!=1 || s.profile!=2 || s.primaries!=9 || s.transfer!=18
                || s.matrix!=9 || s.fullRange!=fullRange)
            fail("encoded dimensions, Main10 depth, BT2020 HLG or range do not match input");
        return s;
    }
    static Sps parseSps(byte[] nal) {
        if(nal.length>65536 || ((nal[0]>>>1)&63)!=33) fail("invalid SPS");
        Bits b=new Bits(rbsp(nal)); b.skip(4); int layers=b.get(3); b.skip(1);
        if(layers>6) fail("invalid HEVC sublayer count");
        int profileSpace=b.get(2); b.skip(1); int profile=b.get(5); b.skip(88);
        if(profileSpace!=0) fail("unsupported profile space");
        int[] ps=new int[layers], ls=new int[layers];
        for(int i=0;i<layers;i++) { ps[i]=b.get(1); ls[i]=b.get(1); }
        if(layers>0) b.skip((8-layers)*2);
        for(int i=0;i<layers;i++) { if(ps[i]!=0) b.skip(88); if(ls[i]!=0) b.skip(8); }
        b.ue(15); int chroma=b.ue(3); if(chroma==3 && b.get(1)!=0) fail("separate planes unsupported");
        int w=b.ue(32768),h=b.ue(32768);
        int l=0,r=0,t=0,bt=0;
        if(b.get(1)!=0) { l=b.ue(32768); r=b.ue(32768); t=b.ue(32768); bt=b.ue(32768); }
        int depthL=b.ue(8)+8,depthC=b.ue(8)+8,poc=b.ue(12)+4;
        int from=b.get(1)!=0 ? 0 : layers;
        for(int i=from;i<=layers;i++) { b.ue(16); b.ue(16); b.ue(1<<20); }
        for(int i=0;i<6;i++) b.ue(16);
        if(b.get(1)!=0 && b.get(1)!=0) scalingList(b);
        b.skip(2);
        if(b.get(1)!=0) { b.skip(8); b.ue(16); b.ue(16); b.skip(1); }
        int count=b.ue(64); int[] deltas=new int[count];
        for(int i=0;i<count;i++) {
            if(i!=0 && b.get(1)!=0) {
                b.skip(1); b.ue(1<<20); int n=0;
                for(int j=0;j<=deltas[i-1];j++) { int used=b.get(1); if(used!=0 || b.get(1)!=0) n++; }
                if(n>64) fail("excess reference pictures"); deltas[i]=n;
            } else {
                int negatives=b.ue(64),positives=b.ue(64);
                if(negatives+positives>64) fail("excess reference pictures");
                deltas[i]=negatives+positives;
                for(int j=0;j<deltas[i];j++) { b.ue(1<<20); b.skip(1); }
            }
        }
        if(b.get(1)!=0) { int n=b.ue(32); for(int i=0;i<n;i++) b.skip(poc+1); }
        b.skip(2);
        if(b.get(1)==0) fail("SPS has no VUI colour contract");
        if(b.get(1)!=0 && b.get(8)==255) b.skip(32);
        if(b.get(1)!=0) b.skip(1);
        if(b.get(1)==0) fail("missing video signal type");
        b.skip(3); boolean full=b.get(1)!=0;
        if(b.get(1)==0) fail("missing colour description");
        int primaries=b.get(8),transfer=b.get(8),matrix=b.get(8);
        int subW=chroma==1 || chroma==2 ? 2 : 1, subH=chroma==1 ? 2 : 1;
        w-=(l+r)*subW; h-=(t+bt)*subH;
        if(w<=0 || h<=0) fail("invalid conformance window");
        return new Sps(w,h,depthL,depthC,chroma,profile,primaries,transfer,matrix,full);
    }
    private static void scalingList(Bits b) {
        for(int size=0;size<4;size++) for(int matrix=0;matrix<6;matrix+=(size==3?3:1)) {
            if(b.get(1)==0) b.ue(6);
            else { if(size>1) b.se(); for(int i=0;i<Math.min(64,1<<(4+(size<<1)));i++) b.se(); }
        }
    }
}

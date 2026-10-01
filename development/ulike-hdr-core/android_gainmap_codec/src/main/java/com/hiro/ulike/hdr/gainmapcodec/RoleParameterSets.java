package com.hiro.ulike.hdr.gainmapcodec;

import java.io.ByteArrayOutputStream;
import java.util.List;

/** Restricted single-layer VPS/PPS syntax and ID linkage. Slice entropy is not decoded. */
final class RoleParameterSets {
    private RoleParameterSets() {}
    static final class Bits {
        final byte[] bytes; int position;
        Bits(byte[] bytes) { this.bytes=bytes; }
        int read(int n) {
            if(n<0 || n>30 || position+(long)n>bytes.length*8L) fail("truncated parameter/slice header");
            int v=0; for(int i=0;i<n;i++,position++) v=(v<<1)|((bytes[position/8]>>>(7-position%8))&1);
            return v;
        }
        void skip(int n) { while(n>0) { int k=Math.min(n,24); read(k); n-=k; } }
        int ue(int max) {
            int zeros=0; while(read(1)==0) if(++zeros>29) fail("Exp-Golomb overflow");
            long value=(1L<<zeros)-1+read(zeros);
            if(value>max) fail("parameter outside supported bounds"); return (int)value;
        }
        int se(int maximumAbsolute) { int n=ue(maximumAbsolute*2); return (n&1)!=0?(n+1)/2:-n/2; }
        void end() {
            if(read(1)!=1) fail("missing RBSP stop bit");
            while(position<bytes.length*8) if(read(1)!=0) fail("data after RBSP stop bit");
        }
    }
    private static void fail(String why) { throw new IllegalArgumentException(why); }
    static byte[] rbsp(byte[] nal) {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream(); int zeros=0;
        for(int i=2;i<nal.length;i++) {
            int value=nal[i]&255;
            if(zeros>=2 && value==3) {
                if(i+1>=nal.length || (nal[i+1]&255)>3) fail("malformed NAL emulation-prevention byte");
                zeros=0; continue;
            }
            if(zeros>=2 && value<=2) fail("unescaped NAL byte sequence");
            bytes.write(value); zeros=value==0?zeros+1:0;
        }
        return bytes.toByteArray();
    }
    static void validate(List<byte[]> nals,byte[][] params) {
        for(byte[] nal:nals) rbsp(nal); // Every accepted NAL, including VPS/PPS/AUD/SEI.
        Bits sps=new Bits(rbsp(params[1])); int spsVps=sps.read(4),spsLayers=sps.read(3); sps.read(1);
        profile(sps,spsLayers); int spsId=sps.ue(15);
        int[] vps=parseVps(params[0]),pps=parsePps(params[2]);
        if(spsVps!=vps[0] || spsLayers>vps[1] || pps[1]!=spsId) fail("VPS/SPS/PPS reference mismatch");
        for(byte[] nal:nals) {
            int type=(nal[0]>>>1)&63;
            if(type==19 || type==20) {
                Bits slice=new Bits(rbsp(nal)); slice.read(1); slice.read(1); // first_slice, no_output_of_prior_pics
                int ppsId=slice.ue(63);
                if(ppsId!=pps[0]) fail("slice references missing PPS");
                if(slice.position+1>=slice.bytes.length*8) fail("missing coded slice data");
            } else if(type==35) {
                Bits aud=new Bits(rbsp(nal)); if(aud.read(3)>2) fail("invalid AUD pic_type"); aud.end();
            } else if(type==39 || type==40) validateSei(rbsp(nal));
        }
    }
    private static void profile(Bits bits,int layers) {
        if(layers!=0) fail("only no-sublayer still VPS supported");
        bits.skip(96); int[] p=new int[layers],l=new int[layers];
        for(int i=0;i<layers;i++) { p[i]=bits.read(1); l[i]=bits.read(1); }
        if(layers!=0) bits.skip((8-layers)*2);
        for(int i=0;i<layers;i++) { if(p[i]!=0) bits.skip(88); if(l[i]!=0) bits.skip(8); }
    }
    private static int[] parseVps(byte[] nal) {
        Bits b=new Bits(rbsp(nal)); int id=b.read(4); b.skip(2);
        if(b.read(6)!=0) fail("multilayer VPS unsupported");
        int layers=b.read(3); b.skip(1);
        if(b.read(16)!=65535) fail("invalid VPS reserved bits");
        profile(b,layers);
        int from=b.read(1)!=0?0:layers;
        for(int i=from;i<=layers;i++) { b.ue(16); b.ue(16); b.ue(1<<20); }
        if(b.read(6)!=0 || b.ue(1023)!=0) fail("additional VPS layer sets unsupported");
        if(b.read(1)!=0) {
            int unitsHi=b.read(16),unitsLo=b.read(16),scaleHi=b.read(16),scaleLo=b.read(16);
            if((unitsHi|unitsLo)==0 || (scaleHi|scaleLo)==0) fail("invalid VPS timing");
            if(b.read(1)!=0) b.ue(1<<20);
            if(b.ue(1024)!=0) fail("VPS HRD parameters unsupported in this still mux");
        }
        if(b.read(1)!=0) fail("VPS extensions unsupported");
        b.end(); return new int[]{id,layers};
    }
    private static int[] parsePps(byte[] nal) {
        Bits b=new Bits(rbsp(nal)); int id=b.ue(63),sps=b.ue(15);
        b.skip(7); b.ue(14); b.ue(14); b.se(51); b.skip(2);
        if(b.read(1)!=0) b.ue(6);
        b.se(12); b.se(12); b.skip(4);
        int tiles=b.read(1); b.skip(1);
        if(tiles!=0) {
            int columns=b.ue(19),rows=b.ue(21);
            if(b.read(1)==0) { for(int i=0;i<columns;i++) b.ue(32768); for(int i=0;i<rows;i++) b.ue(32768); }
            b.skip(1);
        }
        b.skip(1);
        if(b.read(1)!=0) { b.skip(1); if(b.read(1)==0) { b.se(6); b.se(6); } }
        if(b.read(1)!=0) scalingList(b);
        b.skip(1); b.ue(4); b.skip(1);
        if(b.read(1)!=0 && b.read(8)!=0) fail("PPS extensions unsupported");
        b.end(); return new int[]{id,sps};
    }
    private static void scalingList(Bits b) {
        for(int size=0;size<4;size++) for(int matrix=0;matrix<6;matrix+=(size==3?3:1)) {
            if(b.read(1)==0) b.ue(6);
            else { if(size>1) b.se(255); for(int i=0;i<Math.min(64,1<<(4+2*size));i++) b.se(255); }
        }
    }
    private static void validateSei(byte[] data) {
        int offset=0;
        while(offset<data.length) {
            if(offset==data.length-1 && (data[offset]&255)==128) return;
            int value;
            do { if(offset>=data.length) fail("truncated SEI payload type"); value=data[offset++]&255; } while(value==255);
            long size=0;
            do { if(offset>=data.length) fail("truncated SEI payload size"); value=data[offset++]&255; size+=value; } while(value==255);
            if(size>data.length-offset) fail("truncated SEI payload");
            offset+=(int)size;
        }
        fail("missing SEI RBSP trailing bits");
    }
}

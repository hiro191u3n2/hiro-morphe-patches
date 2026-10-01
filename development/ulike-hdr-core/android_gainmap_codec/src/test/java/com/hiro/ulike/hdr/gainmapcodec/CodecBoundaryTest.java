package com.hiro.ulike.hdr.gainmapcodec;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public final class CodecBoundaryTest {
    private static int checks;
    private static void yes(boolean condition,String message) { checks++;if(!condition)throw new AssertionError(message); }
    private static void rejects(Runnable work,String label) { checks++;try { work.run(); } catch(IllegalArgumentException expected) {return;}throw new AssertionError("accepted "+label); }
    private static ColorP010.Identity id(int width,int height) { return new ColorP010.Identity(width,height,"capture","geometry","revision"); }
    public static void main(String[] args) throws Exception {
        short[] y=new short[1024],cb=new short[256],cr=new short[256];
        for(int i=0;i<y.length;i++)y[i]=(short)i;
        for(int i=0;i<cb.length;i++){cb[i]=(short)(i*4);cr[i]=(short)(1023-i*4);}
        ColorP010 source=new ColorP010(id(32,32),ColorP010.Role.SDR_BASE,ShortBuffer.wrap(y),ShortBuffer.wrap(cb),ShortBuffer.wrap(cr));
        ByteBuffer packedY=ByteBuffer.allocate(32*72+8),packedUv=ByteBuffer.allocate(16*72+8);
        packedY.position(8);packedUv.position(8);ByteBuffer crView=packedUv.duplicate();crView.position(10);
        ColorP010.Plane py=new ColorP010.Plane(packedY,72,2),pu=new ColorP010.Plane(packedUv,72,4),pv=new ColorP010.Plane(crView,72,4);
        source.copyTo(py,pu,pv);
        ColorP010 decoded=ColorP010.read(source.identity,source.role,0,0,py,pu,pv);
        for(int n=0;n<3;n++) {
            ShortBuffer expected=source.samples(n),actual=decoded.samples(n);
            while(expected.hasRemaining())yes(expected.get()==actual.get(),"10bit sample preservation");
        }
        yes(packedY.position()==8 && packedUv.position()==8 && crView.position()==10,"buffer positions changed");
        yes(source.samples(0).isReadOnly(),"samples mutable");
        y[0]=1023;yes(source.samples(0).get(0)==0,"constructor retained source array");
        ColorP010 crop=ColorP010.read(id(4,4),ColorP010.Role.NUMERICAL_GAINMAP,4,2,py,pu,pv);
        for(int row=0;row<4;row++)for(int x=0;x<4;x++)yes(crop.samples(0).get(row*4+x)==(row+2)*32+x+4,"crop coordinate");
        for(int row=0;row<2;row++)for(int x=0;x<2;x++)yes(crop.samples(1).get(row*2+x)==((row+1)*16+x+2)*4,"crop chroma coordinate");
        byte[] before=packedY.array().clone();
        rejects(()->source.copyTo(py,pu,new ColorP010.Plane(ByteBuffer.allocate(2),72,4)),"short chroma");
        yes(Arrays.equals(before,packedY.array()),"partial mutation before validation");
        rejects(()->source.copyTo(new ColorP010.Plane(packedY.asReadOnlyBuffer(),72,2),pu,pv),"readonly destination");
        ColorP010 readOnly=ColorP010.read(source.identity,source.role,0,0,new ColorP010.Plane(packedY.asReadOnlyBuffer(),72,2),pu,pv);
        yes(readOnly.samples(0).get(1023)==1023,"readonly source");
        rejects(()->ColorP010.read(id(4,4),source.role,1,0,py,pu,pv),"odd crop");
        rejects(()->ColorP010.read(id(4,4),source.role,Integer.MAX_VALUE-1,0,py,pu,pv),"crop overflow");
        rejects(()->ColorP010.read(id(4,4),source.role,0,Integer.MAX_VALUE-1,py,pu,pv),"crop row overflow");
        packedY.put(8,(byte)1);rejects(()->ColorP010.read(source.identity,source.role,0,0,py,pu,pv),"unused low bits");packedY.put(8,(byte)0);
        rejects(()->new ColorP010.Identity(5712,4285,"a","b","c"),"odd geometry");
        rejects(()->new ColorP010.Identity(8192,8192,"a","b","c"),"memory budget");
        rejects(()->new ColorP010.Identity(32,32," ","b","c"),"missing provenance");
        short[] bad={1024,0,0,0};rejects(()->new ColorP010(id(2,2),source.role,ShortBuffer.wrap(bad),ShortBuffer.wrap(new short[1]),ShortBuffer.wrap(new short[1])),"11bit");
        List<CodecSelection.Candidate> choices=Arrays.asList(
            new CodecSelection.Candidate("hw8",true,false,false,true),
            new CodecSelection.Candidate("hwWrongRole",true,true,true,false),
            new CodecSelection.Candidate("sw",false,true,true,true),
            new CodecSelection.Candidate("hw",true,true,true,true));
        yes(CodecSelection.select(choices,CodecSelection.Policy.PREFER_HARDWARE,null).name.equals("hw"),"hardware preference");
        yes(CodecSelection.select(choices,CodecSelection.Policy.ANY,"sw").name.equals("sw"),"exact software option");
        rejects(()->CodecSelection.select(choices,CodecSelection.Policy.ANY,"hw8"),"8bit codec");
        rejects(()->CodecSelection.select(choices,CodecSelection.Policy.ANY,"hwWrongRole"),"wrong role capabilities");
        rejects(()->CodecSelection.select(choices,CodecSelection.Policy.REQUIRE_HARDWARE,"sw"),"required hardware");
        yes(ColorP010.Role.SDR_BASE.isoTransfer==1 && ColorP010.Role.SDR_BASE.androidTransfer==3,"SDR transfer mapping");
        yes(ColorP010.Role.NUMERICAL_GAINMAP.isoTransfer==8 && ColorP010.Role.NUMERICAL_GAINMAP.androidTransfer==1,"map transfer mapping");
        Path directory=Path.of(args[0]);
        byte[] base=Files.readAllBytes(directory.resolve("base.hevc")),map=Files.readAllBytes(directory.resolve("gainmap.hevc"));
        RoleHevcProof.Sps bp=RoleHevcProof.inspect(base,64,32,ColorP010.Role.SDR_BASE);
        RoleHevcProof.Sps mp=RoleHevcProof.inspect(map,64,32,ColorP010.Role.NUMERICAL_GAINMAP);
        yes(bp.lumaBits==10 && bp.chromaBits==10 && bp.transfer==1,"real SDR Main10 proof");
        yes(mp.transfer==8,"real numerical map proof");
        rejects(()->RoleHevcProof.inspect(base,64,32,ColorP010.Role.NUMERICAL_GAINMAP),"base as map");
        rejects(()->RoleHevcProof.inspect(map,64,32,ColorP010.Role.SDR_BASE),"map as base");
        rejects(()->RoleHevcProof.inspect(base,32,64,ColorP010.Role.SDR_BASE),"wrong dimensions");
        for(String name:new String[]{"hlg","sdr8","limited","smpte170m","bt709matrix"}) {
            byte[] wrong=Files.readAllBytes(directory.resolve(name+".hevc"));
            rejects(()->RoleHevcProof.inspect(wrong,64,32,ColorP010.Role.SDR_BASE),name);
        }
        rejects(()->RoleHevcProof.inspect(changeSpsTail(base),64,32,ColorP010.Role.SDR_BASE),"SPS appended non-trailing data");
        rejects(()->RoleHevcProof.inspect(removeParameter(base,34),64,32,ColorP010.Role.SDR_BASE),"PPS missing");
        rejects(()->RoleHevcProof.inspect(truncateSps(base),64,32,ColorP010.Role.SDR_BASE),"SPS truncated tail");
        Main10Codec.Encoded encoded=Main10Codec.Encoded.verify(base,id(64,32),ColorP010.Role.SDR_BASE);
        byte[] owned=encoded.copyAnnexB();owned[0]=99;yes(encoded.copyAnnexB()[0]==base[0],"immutable encoded bytes");
        yes(encoded.sha256.length()==64 && encoded.encoderName.equals("external-unattested"),"import provenance explicit");
        byte[] original=base.clone();base[0]=88;yes(Arrays.equals(encoded.copyAnnexB(),original),"encoded import owns source bytes");
        System.out.println("{\"checks\":"+checks+",\"status\":\"PASS\",\"android_device_executed\":false}");
    }
    static byte[] changeSpsTail(byte[] input) {
        return rewrite(input,1);
    }
    static byte[] truncateSps(byte[] input) { return rewrite(input,2); }
    static byte[] rewrite(byte[] input,int mode) {
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        for(byte[] nal:RoleHevcProof.annexB(input)) {
            out.write(0);out.write(0);out.write(0);out.write(1);
            if(((nal[0]>>>1)&63)==33) {
                if(mode==1){out.write(nal,0,nal.length);out.write(1);}
                else out.write(nal,0,nal.length-1);
            } else out.write(nal,0,nal.length);
        }
        return out.toByteArray();
    }
    static byte[] removeParameter(byte[] input,int type) {
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        for(byte[] nal:RoleHevcProof.annexB(input)) if(((nal[0]>>>1)&63)!=type) {
            out.write(0);out.write(0);out.write(0);out.write(1);out.write(nal,0,nal.length);
        }
        return out.toByteArray();
    }
}

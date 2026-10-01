package com.hiro.ulike.hdr.encoder;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public final class EncoderBoundaryTest {
    private static int checks;
    private static void check(boolean value) { checks++; if(!value) throw new AssertionError("check "+checks); }
    private static void rejects(Runnable call) {
        checks++; try { call.run(); } catch(IllegalArgumentException expected) { return; }
        throw new AssertionError("expected rejection at "+checks);
    }
    private static P010Frame frame(short[] y) {
        return new P010Frame(32,32,P010Frame.Encoding.BT2020_NCL_HLG_LIMITED,"capture","geometry","processing",
                ShortBuffer.wrap(y),ShortBuffer.wrap(new short[256]),ShortBuffer.wrap(new short[256]));
    }
    public static void main(String[] args) throws Exception {
        short[] codes=new short[1024]; for(short i=0;i<1024;i++) codes[i]=i;
        P010Frame frame=frame(codes); codes[4]=0;
        check(frame.samples(0).get(4)==4); check(frame.samples(0).isReadOnly());
        ByteBuffer y=ByteBuffer.allocate(80*32+10),uv=ByteBuffer.allocate(80*16+10);
        Arrays.fill(y.array(),(byte)0x55); Arrays.fill(uv.array(),(byte)0x33);
        y.position(4); uv.position(4); ByteBuffer cr=uv.duplicate(); cr.position(6);
        frame.copyTo(new P010Frame.Plane(y,80,2),new P010Frame.Plane(uv,80,4),new P010Frame.Plane(cr,80,4));
        for(int row=0;row<32;row++) {
            for(int col=0;col<32;col++) {
                int offset=4+row*80+col*2, packed=(y.get(offset)&255)|((y.get(offset+1)&255)<<8);
                check(packed>>>6==row*32+col); check((packed&63)==0);
            }
            check(y.get(4+row*80+64)==0x55);
        }
        for(int row=0;row<16;row++) for(int col=0;col<16;col++) {
            int offset=4+row*80+col*4;
            check(uv.get(offset)==0 && uv.get(offset+1)==0 && uv.get(offset+2)==0 && uv.get(offset+3)==0);
        }
        check(y.position()==4 && uv.position()==4 && cr.position()==6);
        byte[] before=y.array().clone();
        rejects(()->frame.copyTo(new P010Frame.Plane(y,80,2),new P010Frame.Plane(uv,80,4),new P010Frame.Plane(ByteBuffer.allocate(2),80,4)));
        check(Arrays.equals(before,y.array()));
        rejects(()->frame.copyTo(new P010Frame.Plane(y,80,1),new P010Frame.Plane(uv,80,4),new P010Frame.Plane(cr,80,4)));
        rejects(()->frame.copyTo(new P010Frame.Plane(y.asReadOnlyBuffer(),80,2),new P010Frame.Plane(uv,80,4),new P010Frame.Plane(cr,80,4)));
        P010Frame small=new P010Frame(4,4,P010Frame.Encoding.BT2020_NCL_HLG_LIMITED,"a","b","c",
                ShortBuffer.allocate(16),ShortBuffer.allocate(4),ShortBuffer.allocate(4));
        ByteBuffer smallY=ByteBuffer.allocate(32), shared=ByteBuffer.allocate(16);
        Arrays.fill(smallY.array(),(byte)0x55); Arrays.fill(shared.array(),(byte)0x33);
        ByteBuffer sharedCr=shared.duplicate(); sharedCr.position(2);
        rejects(()->small.copyTo(new P010Frame.Plane(smallY,8,2),new P010Frame.Plane(shared,6,4),new P010Frame.Plane(sharedCr,6,4)));
        check(smallY.get(0)==0x55 && shared.get(0)==0x33);
        rejects(()->small.copyTo(new P010Frame.Plane(smallY,8,2),new P010Frame.Plane(shared,8,4),new P010Frame.Plane(sharedCr,10,4)));
        check(smallY.get(0)==0x55 && shared.get(0)==0x33);
        short[] bad=new short[1024]; bad[100]=1024; rejects(()->frame(bad));
        bad[100]=-1; rejects(()->frame(bad));
        rejects(()->new P010Frame(3,2,P010Frame.Encoding.BT2020_NCL_HLG_FULL,"a","b","c",ShortBuffer.allocate(6),ShortBuffer.allocate(1),ShortBuffer.allocate(1)));
        EncoderChoice.Candidate hardware=new EncoderChoice.Candidate("hardware",true,true,true,true,true);
        EncoderChoice.Candidate software=new EncoderChoice.Candidate("aSoftware",false,true,true,true,true);
        EncoderChoice.Candidate fake=new EncoderChoice.Candidate("a8bitHardware",true,true,false,true,true);
        List<EncoderChoice.Candidate> choices=Arrays.asList(fake,software,hardware);
        check(EncoderChoice.select(choices,EncoderChoice.Policy.HARDWARE_PREFERRED,null)==hardware);
        check(EncoderChoice.select(choices,EncoderChoice.Policy.ANY,null)==software);
        check(EncoderChoice.select(choices,EncoderChoice.Policy.HARDWARE_PREFERRED,"aSoftware")==software);
        rejects(()->EncoderChoice.select(choices,EncoderChoice.Policy.HARDWARE_ONLY,"aSoftware"));
        rejects(()->EncoderChoice.select(Arrays.asList(fake),EncoderChoice.Policy.ANY,null));
        rejects(()->EncoderChoice.select(choices,EncoderChoice.Policy.ANY,"missing"));
        for(int failure=0;failure<4;failure++) {
            EncoderChoice.Candidate c=new EncoderChoice.Candidate("bad",true,failure!=0,failure!=1,failure!=2,failure!=3);
            rejects(()->EncoderChoice.select(Arrays.asList(c),EncoderChoice.Policy.ANY,null));
        }
        Path fixtures=Path.of(args[0]);
        byte[] hlg=Files.readAllBytes(fixtures.resolve("hlg_limited.hevc"));
        byte[] full=Files.readAllBytes(fixtures.resolve("hlg_full.hevc"));
        byte[] sdr=Files.readAllBytes(fixtures.resolve("sdr8.hevc"));
        byte[] pq=Files.readAllBytes(fixtures.resolve("pq10.hevc"));
        HevcProof.Sps limited=HevcProof.inspect(hlg,64,32,false);
        check(limited.lumaBits==10 && limited.chromaBits==10 && limited.profile==2);
        check(limited.primaries==9 && limited.transfer==18 && limited.matrix==9);
        check(HevcProof.inspect(full,64,32,true).fullRange);
        rejects(()->HevcProof.inspect(hlg,32,64,false));
        rejects(()->HevcProof.inspect(hlg,64,32,true));
        rejects(()->HevcProof.inspect(full,64,32,false));
        rejects(()->HevcProof.inspect(sdr,64,32,false));
        rejects(()->HevcProof.inspect(pq,64,32,false));
        rejects(()->HevcProof.inspect(new byte[6],64,32,false));
        byte[] twice=new byte[hlg.length*2]; System.arraycopy(hlg,0,twice,0,hlg.length); System.arraycopy(hlg,0,twice,hlg.length,hlg.length);
        rejects(()->HevcProof.inspect(twice,64,32,false));
        byte[] noVcl=HevcProof.annexB(hlg).stream().filter(n->((n[0]>>>1)&63)==33).findFirst().get();
        // This is a bounded SPS contract inspector through colour_description,
        // not a validator of every later SPS/VUI extension or coded slice.
        for(int n=2;n<24;n++) {
            final byte[] truncated=Arrays.copyOf(noVcl,n);
            rejects(()->HevcProof.parseSps(truncated));
        }
        System.out.println("{\"checks\":"+checks+",\"status\":\"PASS\",\"android_device_executed\":false}");
    }
}

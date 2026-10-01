package com.hiro.ulike.hdr.gainmap;

import com.hiro.ulike.hdr.gainmapcodec.ColorP010;
import com.hiro.ulike.hdr.gainmapcodec.Main10Codec;
import java.io.IOException;
import java.util.Objects;

/** Actual API33+ MediaCodec bridge. No software test fixture, 8-bit fallback or HLG map tagging.
 * Phone codec support and device execution are unverified until called on that device. */
public final class AndroidGainmapCodec implements GainmapSave.Codec {
    private final Main10Codec.Options options;
    public AndroidGainmapCodec(Main10Codec.Options options) { this.options=Objects.requireNonNull(options); }
    private static ColorP010.Role role(GainmapSave.Role role) {
        Objects.requireNonNull(role);
        return role==GainmapSave.Role.SDR_BASE?ColorP010.Role.SDR_BASE:ColorP010.Role.NUMERICAL_GAINMAP;
    }
    private static ColorP010.Identity identity(GainmapMath.Frame f) {
        return new ColorP010.Identity(f.width,f.height,f.captureId,f.geometryId,f.processingId);
    }
    @Override public byte[] encode(GainmapMath.Codes input,GainmapSave.Role role) throws IOException {
        Yuv42010 source=Yuv42010.fromRgb(input);
        ColorP010 actual=new ColorP010(identity(input.frame),role(role),source.plane(0),source.plane(1),source.plane(2));
        return Main10Codec.encode(actual,options).copyAnnexB();
    }
    @Override public GainmapSave.Decoded decode(byte[] actualAnnexB,GainmapMath.Frame frame,GainmapSave.Role role) throws IOException {
        Main10Codec.Encoded owned=Main10Codec.Encoded.verify(actualAnnexB,identity(frame),role(role));
        Main10Codec.Decoded decoded=Main10Codec.decode(owned,options);
        if(!owned.sha256.equals(decoded.encodedSha256) || !owned.identity.equals(decoded.frame.identity) || owned.role!=decoded.frame.role)
            throw new IOException("decoded output lost encoded-byte or frame binding");
        ColorP010 p010=decoded.frame;
        Yuv42010 actual=new Yuv42010(frame,p010.samples(0),p010.samples(1),p010.samples(2));
        GainmapMath.Codes codes=actual.rgb();
        return new GainmapSave.Decoded() {
            private boolean closed;
            @Override public GainmapMath.Codes codes() {
                if(closed) throw new IllegalStateException("decoded source closed"); return codes;
            }
            @Override public void close() { closed=true; }
        };
    }
}

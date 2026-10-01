package com.hiro.ulike.hdr.photo;

import com.hiro.ulike.hdr.gainmap.GainmapMath;
import com.hiro.ulike.hdr.input.HdrFrame;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/** Immutable shutter-time capture/settings/geometry identity. No latest-preview state is read. */
public final class PhotoIdentity {
    public final Object exactSource;
    public final GainmapMath.Frame frame;
    public final String settingsSha256,identitySha256;
    public final long dateTakenMs;
    public PhotoIdentity(Object exactSource,int width,int height,String captureId,String geometryId,
            String processingRevision,byte[] exactSettingsSnapshot,long dateTakenMs) {
        this.exactSource=Objects.requireNonNull(exactSource,"exact source token");
        if(exactSettingsSnapshot==null || exactSettingsSnapshot.length<1 || exactSettingsSnapshot.length>64*1024
                || dateTakenMs<=0 || processingRevision==null || processingRevision.trim().isEmpty()
                || processingRevision.length()>256) throw new IllegalArgumentException("bounded shutter settings/revision/time required");
        settingsSha256=hash(exactSettingsSnapshot.clone()); this.dateTakenMs=dateTakenMs;
        String processing=hash(fields(processingRevision,settingsSha256));
        frame=new GainmapMath.Frame(width,height,captureId,geometryId,processing);
        identitySha256=hash(fields(Integer.toString(width),Integer.toString(height),captureId,geometryId,processing,settingsSha256));
    }
    public static PhotoIdentity fromCapture(HdrFrame source,PhotoGeometry geometry,String processingRevision,
            byte[] exactSettingsSnapshot,long dateTakenMs) {
        Objects.requireNonNull(source); Objects.requireNonNull(geometry);
        if(source.width!=geometry.sourceWidth || source.height!=geometry.sourceHeight)
            throw new IllegalArgumentException("geometry does not describe exact capture");
        String capture=hash(fields(source.cameraId,String.valueOf(source.physicalId),String.valueOf(source.activePhysicalId),
                Long.toString(source.timestampNs),Long.toString(source.frameNumber),Long.toString(source.shot)));
        return new PhotoIdentity(source,geometry.width,geometry.height,capture,geometry.id(),processingRevision,exactSettingsSnapshot,dateTakenMs);
    }
    private PhotoIdentity(PhotoIdentity parent,int width,int height){
        exactSource=parent.exactSource;settingsSha256=parent.settingsSha256;dateTakenMs=parent.dateTakenMs;
        frame=new GainmapMath.Frame(width,height,parent.frame.captureId,parent.frame.geometryId,parent.frame.processingId);
        identitySha256=hash(fields(Integer.toString(width),Integer.toString(height),frame.captureId,frame.geometryId,frame.processingId,settingsSha256));
    }
    PhotoIdentity sourceRaster(int width,int height){return new PhotoIdentity(this,width,height);}
    static byte[] fields(String... fields) {
        try { ByteArrayOutputStream bytes=new ByteArrayOutputStream(); DataOutputStream out=new DataOutputStream(bytes);
            for(String field:fields) out.writeUTF(Objects.requireNonNull(field)); out.flush(); return bytes.toByteArray();
        } catch(IOException e) { throw new AssertionError(e); }
    }
    static MessageDigest digest() { try { return MessageDigest.getInstance("SHA-256"); } catch(NoSuchAlgorithmException e) { throw new AssertionError(e); } }
    static String hash(byte[] bytes) {
        byte[] digest=digest().digest(bytes); StringBuilder text=new StringBuilder(64);
        for(byte b:digest) text.append(Character.forDigit((b>>>4)&15,16)).append(Character.forDigit(b&15,16)); return text.toString();
    }
}

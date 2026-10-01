package com.hiro.ulike.hdr.faceprobe;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/** Exactly the original arm64 SDK or the already released four-byte NV21 variant. */
public final class StockSdkIdentity {
    public static final String ORIGINAL = "67f1b97b92859630ae200cafad41f7e3db3d8d2fbcac3ffd5c7bab0683471095";
    public static final String RELEASED_NV21 = "eac57bcaa613860c7ca03ea516c4370520f9775cdfe700688d74e223bd59c2bb";
    private static final long LIMIT = 64L * 1024 * 1024;
    private static final int OFFSET = 0x40b864;
    private static final byte[] BEFORE = {0x26, 0, (byte)0x80, 0x52};
    private static final byte[] AFTER = {(byte)0xa6, (byte)0xe3, 0x40, 0x39};
    public final String installedSha256, canonicalSha256, variant;
    private StockSdkIdentity(String installed, String canonical, String variant) {
        this.installedSha256=installed;this.canonicalSha256=canonical;this.variant=variant;
    }

    /** Caller owns the stream. Hashes the entire library, using a bounded streaming buffer. */
    public static StockSdkIdentity verify(InputStream input) throws IOException {
        if(input==null)throw new NullPointerException("SDK stream");
        MessageDigest actual=sha(), canonical=sha();byte[] buffer=new byte[65536], observed=new byte[4];long total=0;
        for(;;){
            int count=input.read(buffer);
            if(count<0)break;
            if(count==0){int one=input.read();if(one<0)break;buffer[0]=(byte)one;count=1;}
            if(count>LIMIT-total)throw new IOException("SDK exceeds 64 MiB verification bound");
            actual.update(buffer,0,count);
            long start=Math.max(total,OFFSET),end=Math.min(total+count,OFFSET+4L);
            for(long position=start;position<end;position++){
                int patch=(int)(position-OFFSET),index=(int)(position-total);
                observed[patch]=buffer[index];buffer[index]=BEFORE[patch];
            }
            canonical.update(buffer,0,count);total+=count;
        }
        if(total<OFFSET+4L)throw new IOException("Truncated SDK library");
        String installed=hex(actual.digest()), normalized=hex(canonical.digest());
        if(!ORIGINAL.equals(normalized))throw new IOException("Unsupported SDK library (canonical SHA-256)");
        if(ORIGINAL.equals(installed)&&Arrays.equals(observed,BEFORE))
            return new StockSdkIdentity(installed,normalized,"STOCK_5_6_2_740_ARM64");
        if(RELEASED_NV21.equals(installed)&&Arrays.equals(observed,AFTER))
            return new StockSdkIdentity(installed,normalized,"PUBLISHED_NV21_FOUR_BYTE_VARIANT");
        throw new IOException("Unsupported SDK variant");
    }
    private static MessageDigest sha(){try{return MessageDigest.getInstance("SHA-256");}catch(NoSuchAlgorithmException e){throw new AssertionError(e);}}
    private static String hex(byte[] data){StringBuilder s=new StringBuilder();for(byte b:data)s.append(Character.forDigit((b&255)>>>4,16)).append(Character.forDigit(b&15,16));return s.toString();}
}

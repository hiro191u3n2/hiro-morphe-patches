package com.hiro.ulike.hdr.gainmap;

import com.hiro.ulike.hdr.gainmapcodec.ColorP010;
import com.hiro.ulike.hdr.gainmapcodec.RoleHevcProof;
import java.util.List;

/** Uses the same strict, complete SPS/VPS/PPS role proof as the Android codec boundary. */
public final class GainmapHevcProof {
    private GainmapHevcProof() {}
    public static RoleHevcProof.Sps inspect(byte[] bytes,int width,int height,GainmapSave.Role role) {
        if(role==null) throw new NullPointerException("role");
        return RoleHevcProof.inspect(bytes,width,height,role==GainmapSave.Role.SDR_BASE?
                ColorP010.Role.SDR_BASE:ColorP010.Role.NUMERICAL_GAINMAP);
    }
    public static List<byte[]> annexB(byte[] bytes) { return RoleHevcProof.annexB(bytes); }
}

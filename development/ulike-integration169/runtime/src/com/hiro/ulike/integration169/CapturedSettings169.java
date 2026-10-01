package com.hiro.ulike.integration169;

import com.hiro.ulike.binding.ShotStyleSettings;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

/** Canonical same-choice requests plus the independently verified native replay transcript.
 * Serializing a transcript does not establish its completeness or native execution. */
public final class CapturedSettings169 {
    private CapturedSettings169(){}
    public static byte[] encode(ShotStyleSettings.Snapshot shot,byte[] completeReplayTranscript)throws Exception {
        if(shot==null || completeReplayTranscript==null || completeReplayTranscript.length<1 || completeReplayTranscript.length>512*1024)
            throw new IllegalArgumentException("Exact bounded shot and replay transcript required");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
        out.writeUTF("ulike-same-shot-requests-and-replay-v1");out.writeUTF(shot.styleId);out.writeLong(shot.shotEpoch);out.writeLong(shot.observerRevision);out.writeBoolean(shot.requestedGraphComplete);
        nullable(out,shot.composerResourcePath);number(out,shot.modeOne);number(out,shot.modeTwo);
        out.writeInt(shot.requestedNodes.size());
        if(shot.requestedNodeTags.size()!=shot.requestedNodes.size())throw new IllegalArgumentException("Node/tag extent mismatch");
        for(int i=0;i<shot.requestedNodes.size();i++){out.writeUTF(shot.requestedNodes.get(i));nullable(out,shot.requestedNodeTags.get(i));}
        parameters(out,shot.inlineParameters);parameters(out,shot.updateParameters);
        out.writeInt(completeReplayTranscript.length);out.write(completeReplayTranscript.clone());out.flush();
        byte[] result=bytes.toByteArray();if(result.length>1024*1024)throw new IllegalArgumentException("Canonical settings budget");return result;
    }
    private static void nullable(DataOutputStream out,String value)throws Exception{out.writeBoolean(value!=null);if(value!=null)out.writeUTF(value);}
    private static void number(DataOutputStream out,Integer value)throws Exception{out.writeBoolean(value!=null);if(value!=null)out.writeInt(value);}
    private static void parameters(DataOutputStream out,java.util.List<ShotStyleSettings.Parameter> values)throws Exception{
        out.writeInt(values.size());for(ShotStyleSettings.Parameter p:values){out.writeUTF(p.resource);out.writeUTF(p.key);out.writeInt(p.valueBits);out.writeUTF(p.source);}
    }
}

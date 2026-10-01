package com.hiro.ulike.binding;

import com.hiro.ulike.integration169.CapturedSettings169;
import java.io.*;
import java.util.*;

/** Independent canonical settings serialization review using immutable request snapshots. */
public final class CapturedSettingsReview {
    static int checks;
    static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    static Map<String,Object> map(Object... values){Map<String,Object> out=new LinkedHashMap<>();for(int i=0;i<values.length;i+=2)out.put((String)values[i],values[i+1]);return out;}
    static Map<String,Object> manifest(boolean complete){return map("schema","ulike-style-materials-164","style_id",ShotStyleSettings.NATURAL,"observer_revision",9L,
        "composer_resource_path","素材/🙂/composer","mode_one",2,"mode_two",null,
        "completeness",map("pending_api_calls",0L,"dropped_events",0L,"overlapping_api_calls",false,"malformed_or_bounded_arguments",false,"ordered_api_model_complete",complete),
        "ordered_requested_nodes",Arrays.asList(map("path","node-A","tag",null,"descriptor_kind","plain"),map("path","node-B","tag","tag-B","descriptor_kind","path_parameter_float","resource_path","material","inline_parameter_key","strength","inline_value_float","-0.0")),
        "parameter_updates_observed_since_node_reset",Arrays.asList(map("resource_path","material","key","makeup","value_float","0.625")));}
    static ShotStyleSettings.Snapshot snapshot(Map<String,Object> manifest){return ShotStyleSettings.copy(new Object(),new Object(),4,ShotStyleSettings.NATURAL,manifest,new Object());}
    static void nullable(DataInputStream in,String expected)throws Exception{boolean present=in.readBoolean();check(present==(expected!=null),"nullable presence");if(present)check(expected.equals(in.readUTF()),"nullable UTF");}
    public static void main(String[] args)throws Exception{
        Map<String,Object> original=manifest(false);ShotStyleSettings.Snapshot shot=snapshot(original);byte[] replay={0,1,2,(byte)255};byte[] encoded=CapturedSettings169.encode(shot,replay);
        DataInputStream in=new DataInputStream(new ByteArrayInputStream(encoded));
        check(in.readUTF().equals("ulike-same-shot-requests-and-replay-v1"),"version tag");check(in.readUTF().equals(ShotStyleSettings.NATURAL),"style");check(in.readLong()==4&&in.readLong()==9,"shot epoch and observer revision");
        check(!in.readBoolean(),"incomplete request model is serialized truthfully, never upgraded to native proof");
        nullable(in,"素材/🙂/composer");check(in.readBoolean()&&in.readInt()==2&&!in.readBoolean(),"nullable modes");
        check(in.readInt()==2&&in.readUTF().equals("node-A"),"ordered nodes extent and first");nullable(in,null);check(in.readUTF().equals("node-B"),"second ordered node");nullable(in,"tag-B");
        check(in.readInt()==1&&in.readUTF().equals("material")&&in.readUTF().equals("strength")&&in.readInt()==Float.floatToRawIntBits(-0.0f)&&in.readUTF().equals("inline_node"),"inline float exact bits and source");
        check(in.readInt()==1&&in.readUTF().equals("material")&&in.readUTF().equals("makeup")&&in.readInt()==Float.floatToRawIntBits(.625f)&&in.readUTF().equals("successful_api_update"),"resolved request update exact bits and source");
        check(in.readInt()==4,"replay length");byte[] body=new byte[4];in.readFully(body);check(Arrays.equals(body,replay)&&in.available()==0,"complete replay transcript and no trailing bytes");
        original.clear();replay[0]=99;check(Arrays.equals(encoded,CapturedSettings169.encode(shot,new byte[]{0,1,2,(byte)255})),"snapshot/replay later mutation cannot alter serialized result");
        check(!Arrays.equals(encoded,CapturedSettings169.encode(shot,replay)),"actual changed transcript affects canonical bytes");
        check(!Arrays.equals(encoded,CapturedSettings169.encode(snapshot(manifest(true)),new byte[]{0,1,2,(byte)255})),"completeness flag binds canonical settings without claiming validation");
        for(byte[] invalid:Arrays.asList(null,new byte[0],new byte[512*1024+1])){boolean rejected=false;try{CapturedSettings169.encode(shot,invalid);}catch(IllegalArgumentException expected){rejected=true;}check(rejected,"missing/oversized transcript rejected");}
        boolean rejected=false;try{CapturedSettings169.encode(null,new byte[]{1});}catch(IllegalArgumentException expected){rejected=true;}check(rejected,"null snapshot rejected");
        check(CapturedSettings169.encode(shot,new byte[512*1024]).length>512*1024,"exact transcript budget accepted with bounded framing");
        System.out.println("{\"checks\":"+checks+",\"native_replay_completeness_proven\":false,\"android_execution\":false}");
    }
}

package com.hiro.ulike.binding;

import com.hiro.ulike.integration169.CapturedSettings169;
import java.util.*;

/** Exact serialization of existing immutable observer request settings; not native execution QA. */
public final class CapturedSettings169Test {
    static int checks;
    static void ok(boolean condition){checks++;if(!condition)throw new AssertionError("check "+checks);}
    static Map<String,Object> map(Object...pairs){Map<String,Object> m=new LinkedHashMap<>();for(int i=0;i<pairs.length;i+=2)m.put((String)pairs[i],pairs[i+1]);return m;}
    static Map<String,Object> fixture(){
        Map<String,Object> completeness=map("pending_api_calls",0,"dropped_events",0,"overlapping_api_calls",false,"malformed_or_bounded_arguments",false,"ordered_api_model_complete",false);
        Map<String,Object> node=map("path","/private/material","tag",null);
        Map<String,Object> update=map("resource_path","/private/2000_5_d","key","opacity","value_float","-0.0");
        return map("schema","ulike-style-materials-164","style_id",ShotStyleSettings.NATURAL,"completeness",completeness,
            "ordered_requested_nodes",Arrays.asList(node),"parameter_updates_observed_since_node_reset",Arrays.asList(update),
            "observer_revision",7L,"composer_resource_path",null,"mode_one",null,"mode_two",null);
    }
    static byte[] encode(Map<String,Object> m,long epoch,byte[] transcript)throws Exception{
        ShotStyleSettings.Snapshot snapshot=ShotStyleSettings.copy(new Object(),new Object(),epoch,ShotStyleSettings.NATURAL,m,null);
        return CapturedSettings169.encode(snapshot,transcript);
    }
    @SuppressWarnings("unchecked") public static void main(String[] args)throws Exception{
        byte[] transcript={1,2,3};byte[] original=encode(fixture(),1,transcript);ok(Arrays.equals(original,encode(fixture(),1,transcript)));
        ok(!Arrays.equals(original,encode(fixture(),2,transcript)));ok(!Arrays.equals(original,encode(fixture(),1,new byte[]{1,2,4})));
        for(String key:Arrays.asList("observer_revision","composer_resource_path","mode_one","mode_two")){
            Map<String,Object> m=fixture();m.put(key,key.equals("composer_resource_path")?"/private/actual-root":Integer.valueOf(2));
            ok(!Arrays.equals(original,encode(m,1,transcript)));
        }
        Map<String,Object> m=fixture();((Map<String,Object>)m.get("completeness")).put("ordered_api_model_complete",true);ok(!Arrays.equals(original,encode(m,1,transcript)));
        for(String field:Arrays.asList("path","tag")){
            m=fixture();((Map<String,Object>)((List<?>)m.get("ordered_requested_nodes")).get(0)).put(field,"changed");ok(!Arrays.equals(original,encode(m,1,transcript)));
        }
        for(String field:Arrays.asList("resource_path","key","value_float")){
            m=fixture();((Map<String,Object>)((List<?>)m.get("parameter_updates_observed_since_node_reset")).get(0)).put(field,field.equals("value_float")?"0.0":"changed");
            ok(!Arrays.equals(original,encode(m,1,transcript)));
        }
        m=fixture();ShotStyleSettings.Snapshot snapshot=ShotStyleSettings.copy(new Object(),new Object(),1,ShotStyleSettings.NATURAL,m,null);
        ((Map<String,Object>)((List<?>)m.get("parameter_updates_observed_since_node_reset")).get(0)).put("key","mutated-after-freeze");
        ok(Arrays.equals(original,CapturedSettings169.encode(snapshot,transcript)));
        for(byte[] invalid:new byte[][]{null,new byte[0],new byte[512*1024+1]})try{CapturedSettings169.encode(snapshot,invalid);throw new AssertionError("unbounded transcript");}catch(IllegalArgumentException expected){checks++;}
        System.out.println("captured-settings-checks="+checks);
    }
}

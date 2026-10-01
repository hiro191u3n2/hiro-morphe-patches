package com.hiro.ulike.binding;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Freezes the existing v164 observer's actual successful composer requests at
 * the photo-choice boundary. These are immutable request settings, NOT a claim
 * that native per-face uniforms, geometry or asynchronous effect work resolved.
 */
public final class ShotStyleSettings {
    private ShotStyleSettings() {}
    public static final String NATURAL="7306041792770609665",PURITY="7307549491547083266";
    public static final class Parameter {
        public final String resource,key,source;
        public final float value;
        public final int valueBits;
        Parameter(String resource,String key,String number,String source) {
            this.resource=text(resource);this.key=text(key);this.source=source;
            require(number!=null && number.length()<=64,"invalid parameter number");
            value=Float.parseFloat(number);require(Float.isFinite(value),"nonfinite parameter");
            valueBits=Float.floatToRawIntBits(value);
        }
    }
    public static final class Snapshot {
        public final Object recorderIdentity,shotIdentity;
        public final long shotEpoch,observerRevision;
        public final String styleId,composerResourcePath;
        public final Integer modeOne,modeTwo;
        public final List<String> requestedNodeTags;
        public final boolean requestedGraphComplete;
        public final boolean nativeExecutionConfirmed=false,nativeQueueBarrierPerformed=false;
        public final List<String> requestedNodes;
        public final List<Parameter> inlineParameters,updateParameters;
        private final Object observerSnapshot;
        private Snapshot(Object recorder,Object shot,long epoch,String style,long revision,boolean complete,
                         List<String> nodes,List<String> tags,List<Parameter> inline,List<Parameter> updates,Object original,String resource,Integer one,Integer two) {
            recorderIdentity=recorder;shotIdentity=shot;shotEpoch=epoch;styleId=style;
            observerRevision=revision;requestedGraphComplete=complete;composerResourcePath=resource;modeOne=one;modeTwo=two;
            requestedNodeTags=Collections.unmodifiableList(tags);
            requestedNodes=Collections.unmodifiableList(nodes);
            inlineParameters=Collections.unmodifiableList(inline);updateParameters=Collections.unmodifiableList(updates);
            observerSnapshot=original;
        }
        /** Only returns an observed update when exactly one resource owns this
         * key. It does not assume the selected material root equals composer
         * event routing (the supplied exports route sliders through 2000_5_d).
         */
        public Parameter uniqueObservedUpdate(String key) {
            Parameter found=null;
            for(Parameter p:updateParameters)if(p.key.equals(key)){
                require(found==null,"ambiguous composer resource for "+key);found=p;
            }
            require(found!=null,"unobserved requested setting: "+key);return found;
        }
    }
    private static void require(boolean ok,String message){if(!ok)throw new IllegalArgumentException(message);}
    private static String text(String s){require(s!=null && !s.isEmpty() && s.length()<=4096 && s.indexOf('\0')<0,"invalid observer string");return s;}
    private static Map<?,?> map(Object value){require(value instanceof Map,"observer map required");return (Map<?,?>)value;}
    private static List<?> list(Object value,int bound){require(value instanceof List && ((List<?>)value).size()<=bound,"observer list bound");return (List<?>)value;}
    private static String string(Map<?,?> m,String key){Object v=m.get(key);require(v instanceof String,"missing observer string "+key);return text((String)v);}
    private static long number(Map<?,?> m,String key){Object v=m.get(key);require(v instanceof Number,"missing observer count "+key);return ((Number)v).longValue();}
    private static boolean flag(Map<?,?> m,String key){Object v=m.get(key);require(v instanceof Boolean,"missing observer flag "+key);return (Boolean)v;}
    private static Object call(Object obj,String method)throws Exception{return obj.getClass().getMethod(method).invoke(obj);}
    private static Object field(Object obj,String field)throws Exception{
        Field f=obj.getClass().getDeclaredField(field);f.setAccessible(true);return f.get(obj);
    }
    public static Snapshot freezeAtChoice(Object state,Object recorder,Object shotIdentity,long shotEpoch)throws Exception {
        require(state!=null,"photo-choice state required");
        Object values=call(call(call(call(state,"h"),"g"),"a"),"b");
        Object selected=map(values).get(Integer.valueOf(15));
        require(selected instanceof Number,"selected style ID unavailable");
        return freeze(recorder,Long.toString(((Number)selected).longValue()),shotIdentity,shotEpoch);
    }
    public static Snapshot freeze(Object recorder,String styleId,Object shotIdentity,long shotEpoch)throws Exception {
        require(recorder!=null && shotIdentity!=null && shotEpoch>0,"exact photo request identities required");
        require(NATURAL.equals(styleId) || PURITY.equals(styleId),"unsupported selected style");
        Class<?> observer=Class.forName("com.hiro.ulike.StyleSnapshot164");
        Method snapshot=observer.getDeclaredMethod("snapshot",Object.class,String.class,String.class);snapshot.setAccessible(true);
        Object original=snapshot.invoke(null,recorder,styleId,NATURAL.equals(styleId)?"Natural_blush":"Purity2");
        Map<?,?> manifest=map(field(original,"manifest"));
        Snapshot copy=copy(recorder,shotIdentity,shotEpoch,styleId,manifest,original);
        requireCurrent(copy);return copy;
    }
    /** Recheck on the matching recorder/camera request boundary, before applying
     * the frozen settings. A change fails closed; no global-latest fallback.
     */
    public static void requireCurrent(Snapshot snapshot)throws Exception {
        require(snapshot!=null && snapshot.observerSnapshot!=null,"live observer snapshot required");
        Object raw=snapshot.observerSnapshot;
        Method unchanged=Class.forName("com.hiro.ulike.StyleSnapshot164").getDeclaredMethod("unchanged",raw.getClass());
        unchanged.setAccessible(true);
        require(Boolean.TRUE.equals(unchanged.invoke(null,raw)),"composer changed or has pending work since photo choice");
    }
    static Snapshot copy(Object recorder,Object shot,long epoch,String style,Map<?,?> manifest,Object original) {
        require("ulike-style-materials-164".equals(manifest.get("schema")) && style.equals(manifest.get("style_id")),"observer schema/style mismatch");
        Map<?,?> c=map(manifest.get("completeness"));
        require(number(c,"pending_api_calls")==0 && number(c,"dropped_events")==0
                && !flag(c,"overlapping_api_calls") && !flag(c,"malformed_or_bounded_arguments"),
                "composer request history is pending, overlapping or incomplete");
        ArrayList<String> nodes=new ArrayList<>(),tags=new ArrayList<>();ArrayList<Parameter> inline=new ArrayList<>(),updates=new ArrayList<>();
        long chars=0;
        for(Object item:list(manifest.get("ordered_requested_nodes"),256)) {
            Map<?,?> n=map(item);String path=string(n,"path");nodes.add(path);Object tag=n.get("tag");require(tag==null || tag instanceof String,"node tag type");tags.add(tag==null?null:text((String)tag));chars+=path.length()+(tag==null?0:((String)tag).length());
            if("path_parameter_float".equals(n.get("descriptor_kind")))inline.add(new Parameter(
                string(n,"resource_path"),string(n,"inline_parameter_key"),string(n,"inline_value_float"),"inline_node"));
        }
        for(Object item:list(manifest.get("parameter_updates_observed_since_node_reset"),2048)) {
            Map<?,?> p=map(item);Parameter value=new Parameter(string(p,"resource_path"),string(p,"key"),string(p,"value_float"),"successful_api_update");
            updates.add(value);chars+=value.resource.length()+value.key.length()+64;
        }
        require(chars<=1048576,"snapshot character budget");
        long revision=number(manifest,"observer_revision");require(revision>0,"no observed composer state");
        Object resource=manifest.get("composer_resource_path"),one=manifest.get("mode_one"),two=manifest.get("mode_two");
        require(resource==null || resource instanceof String,"composer resource type");
        require((one==null || one instanceof Integer) && (two==null || two instanceof Integer),"composer mode type");
        return new Snapshot(recorder,shot,epoch,style,revision,flag(c,"ordered_api_model_complete"),nodes,tags,inline,updates,original,resource==null?null:text((String)resource),(Integer)one,(Integer)two);
    }
}

package com.hiro.ulike;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.Size;
import android.view.View;
import java.util.*;

/** Capability enumeration is not a capture failure. Keep fixed-lens eligibility
 * strict, but do not poison a healthy preview merely by inspecting another format.
 * A zero-length answer advertises no sizes; it never restores logical-only sizes.
 */
public final class Preview177 {
    private static final Map<OpticalZoom.Route,Set<String>> NOTICES=new WeakHashMap<>();
    private Preview177(){}
    public static void attached(View previewHost){
        // Register before the optional lens row checks preview geometry/overlap.
        // Readiness still requires the live host to be shown and window-focused.
        if(previewHost!=null)OpticalZoom.host(previewHost);
    }
    private static OpticalZoom.Route mapped(StreamConfigurationMap map){
        try{synchronized(ManualLens170.get("LOCK")){
            @SuppressWarnings("unchecked") Map<StreamConfigurationMap,OpticalZoom.Route> maps=(Map<StreamConfigurationMap,OpticalZoom.Route>)ManualLens170.get("maps");
            OpticalZoom.Route r=maps.get(map);
            return r!=null&&r==ManualLens170.get("active")&&r.epoch==ManualLens170.number("epoch")?r:null;
        }}catch(ReflectiveOperationException|RuntimeException error){return null;}
    }
    private static StreamConfigurationMap physical(OpticalZoom.Route r){return r.lens.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);}
    static Size[] intersect(Size[] logical,Size[] physical){
        if(logical==null||physical==null||logical.length==0||physical.length==0)return new Size[0];
        Set<Size> allowed=new HashSet<>(Arrays.asList(physical));
        List<Size> result=new ArrayList<>();
        for(Size s:logical)if(s!=null&&allowed.contains(s))result.add(s);
        return result.toArray(new Size[result.size()]);
    }
    private static Size[] checked(OpticalZoom.Route r,Size[] logical,Size[] physical,String query){
        Size[] sizes=intersect(logical,physical);
        if(sizes.length==0)notice(r,query);
        return sizes;
    }
    private static void notice(OpticalZoom.Route r,String query){
        synchronized(NOTICES){
            Set<String> seen=NOTICES.get(r);if(seen==null){seen=new HashSet<>();NOTICES.put(r,seen);}
            if(!seen.add(query))return;
        }
        try{synchronized(ManualLens170.get("LOCK")){
            if(r!=ManualLens170.get("active")||r.epoch!=ManualLens170.number("epoch"))return;
            ManualLens170.put("detail",(String)ManualLens170.get("detail")+"\n出力能力照会: "+query+"は固定レンズの共通サイズなし（カメラ停止は行いません）");
        }}catch(ReflectiveOperationException|RuntimeException ignored){}
    }
    public static Size[] outputSizes(StreamConfigurationMap map,int format){
        OpticalZoom.Route r=mapped(map);
        if(r==null||!r.physical())return map.getOutputSizes(format);
        try{
            StreamConfigurationMap lens=physical(r);
            return checked(r,map.getOutputSizes(format),lens==null?null:lens.getOutputSizes(format),"format="+format);
        }catch(IllegalArgumentException unsupported){notice(r,"format="+format);return new Size[0];}
    }
    public static Size[] outputSizes(StreamConfigurationMap map,Class<?> surfaceClass){
        OpticalZoom.Route r=mapped(map);
        if(r==null||!r.physical())return map.getOutputSizes(surfaceClass);
        try{
            StreamConfigurationMap lens=physical(r);
            return checked(r,map.getOutputSizes(surfaceClass),lens==null?null:lens.getOutputSizes(surfaceClass),"surface="+(surfaceClass==null?"null":surfaceClass.getSimpleName()));
        }catch(IllegalArgumentException unsupported){notice(r,"unsupported surface class");return new Size[0];}
    }
}

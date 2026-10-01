package com.hiro.ulike.composer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Read-only diagnosis of the released observer. Never upgrades an incomplete
 * export into a replay plan by trusting its approximate current-node list.
 */
public final class V164ReplayAudit {
    public final int events,failedEvents,replaceEvents,reloadEvents,singleCountUpdateEvents;
    public final boolean orderedApiModelComplete,observedResourcePath,observedMode,hasSetEvent;
    public final boolean replayAuthorized=false;
    public final List<String> missingEvidence;
    private V164ReplayAudit(Map<?,?> manifest) {
        if(!"ulike-style-materials-164".equals(manifest.get("schema")))throw new IllegalArgumentException("unsupported observer schema");
        Object completeness=manifest.get("completeness"),history=manifest.get("api_events");
        if(!(completeness instanceof Map) || !(history instanceof List) || ((List<?>)history).size()>512)throw new IllegalArgumentException("bounded observer history required");
        Map<?,?> c=(Map<?,?>)completeness;orderedApiModelComplete=Boolean.TRUE.equals(c.get("ordered_api_model_complete"));
        observedResourcePath=manifest.get("composer_resource_path") instanceof String;
        observedMode=manifest.get("mode_one") instanceof Number && manifest.get("mode_two") instanceof Number;
        int failed=0,replace=0,reload=0,updates=0;boolean set=false;
        for(Object item:(List<?>)history) {
            if(!(item instanceof Map))throw new IllegalArgumentException("malformed observer event");Map<?,?> e=(Map<?,?>)item;
            Object rc=e.get("return_code");if(!(rc instanceof Number))throw new IllegalArgumentException("missing result");
            if(((Number)rc).intValue()!=0)failed++;
            String op=String.valueOf(e.get("operation"));
            if("set".equals(op))set=true;if("replace".equals(op))replace++;if("reload".equals(op))reload++;
            if("update".equals(op) && e.get("count") instanceof Number && ((Number)e.get("count")).intValue()==1)updates++;
        }
        events=((List<?>)history).size();failedEvents=failed;replaceEvents=replace;reloadEvents=reload;singleCountUpdateEvents=updates;hasSetEvent=set;
        ArrayList<String> missing=new ArrayList<>();
        if(!orderedApiModelComplete)missing.add("ordered_api_model_complete=false");
        if(!observedResourcePath)missing.add("composer resource path is unobserved; null does not prove an empty/default path");
        if(!observedMode)missing.add("composer mode is unobserved");
        if(!hasSetEvent)missing.add("no graph reset/set event in retained history");
        if(failed>0)missing.add("failed native requests may have partially mutated state");
        if(updates>0)missing.add("single update and one-item multi-update APIs were collapsed to the same event opcode");
        // These cannot be reconstructed even when the legacy approximate graph flag is true.
        missing.add("native initialization/configuration baseline and lifecycle not recorded");
        missing.add("all SDK state mutation hook coverage not established");
        missing.add("same-shot native queue barrier and external texture/message/tracking state not established");
        missing.add("complete content identity of all historical replay resources is unproved; active roots alone are insufficient");
        missingEvidence=Collections.unmodifiableList(missing);
    }
    public static V164ReplayAudit inspect(Map<?,?> manifest){if(manifest==null)throw new NullPointerException();return new V164ReplayAudit(manifest);}
    public void requireReplayable(){throw new IllegalStateException("v164 exports cannot authorize complete composer replay: "+missingEvidence);}
}

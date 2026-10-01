package com.hiro.ulike.composer;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Exact path relocation after a separate asset installer has verified ownership
 * and content. This class proves string mapping only, not filesystem provenance.
 */
public final class ExactResourceMap implements ReplayPlan.ResourceMapping {
    private final Map<String,String> resources;
    public ExactResourceMap(Map<String,String> verifiedResourceRoots) {
        if(verifiedResourceRoots==null || verifiedResourceRoots.size()>4096)throw new IllegalArgumentException("resource map bound");
        Map<String,String> copy=new LinkedHashMap<>();long bytes=0;
        for(Map.Entry<String,String> e:verifiedResourceRoots.entrySet()) {
            ComposerCommand.text(e.getKey());ComposerCommand.text(e.getValue());
            if(e.getKey().isEmpty() || e.getValue().isEmpty())throw new IllegalArgumentException("empty resource root");
            bytes+=(long)e.getKey().length()+e.getValue().length();if(bytes>1048576)throw new IllegalArgumentException("resource character bound");
            copy.put(e.getKey(),e.getValue());
        }resources=Collections.unmodifiableMap(copy);
    }
    @Override public String mapExactArgument(String original,boolean descriptor)throws Exception {
        ComposerCommand.text(original);String suffix="",root=original;
        if(descriptor) {
            int last=original.lastIndexOf(':'),prev=last<0?-1:original.lastIndexOf(':',last-1);
            if(prev>=0 && prev+1<last && last+1<original.length())try {
                float value=Float.parseFloat(original.substring(last+1));
                if(Float.isFinite(value)){root=original.substring(0,prev);suffix=original.substring(prev);}
            }catch(NumberFormatException ignored){}
        }
        String replacement=resources.get(root);
        if(replacement==null)throw new IllegalStateException("A replay resource has no exact verified mapping");
        return replacement+suffix;
    }
}

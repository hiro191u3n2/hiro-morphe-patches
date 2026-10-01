package com.hiro.ulike.composer;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** One owned successful request. No flattening of reload/replace order or tags. */
public final class ComposerCommand {
    public enum Kind { MODE, RESOURCE, SET, SET_TAG, APPEND, APPEND_TAG, REMOVE,
        RELOAD, RELOAD_TAG, REPLACE, REPLACE_TAG, UPDATE, UPDATES }
    public final Kind kind;
    public final int modeOne, modeTwo;
    private final String[] paths, replacement, tags, keys;
    private final float[] values;
    private ComposerCommand(Kind kind,String[] paths,String[] replacement,String[] tags,String[] keys,float[] values,int one,int two) {
        if(kind==null)throw new NullPointerException("kind");
        this.kind=kind;this.paths=copy(paths);this.replacement=copy(replacement);this.tags=copy(tags);this.keys=copy(keys);
        this.values=values==null?null:values.clone();modeOne=one;modeTwo=two;
        if(this.values!=null){if(this.values.length>256)throw new IllegalArgumentException("values bound");for(float v:this.values)if(!Float.isFinite(v))throw new IllegalArgumentException("nonfinite value");}
        switch(kind) {
            case MODE: require(paths==null && replacement==null && tags==null && keys==null && values==null);break;
            case RESOURCE: require(paths!=null && paths.length==1 && replacement==null && tags==null && keys==null && values==null);break;
            case UPDATE: case UPDATES:
                require(paths!=null && keys!=null && values!=null && paths.length==keys.length && paths.length==values.length && replacement==null && tags==null);
                if(kind==Kind.UPDATE)require(paths.length==1);break;
            default:
                require(paths!=null && keys==null && values==null);
                boolean replace=kind==Kind.REPLACE || kind==Kind.REPLACE_TAG;
                require(replace==(replacement!=null));
                boolean tagged=kind==Kind.SET_TAG || kind==Kind.APPEND_TAG || kind==Kind.RELOAD_TAG || kind==Kind.REPLACE_TAG;
                require(tagged==(tags!=null));
                if(tagged)require(tags.length==(replace?replacement.length:paths.length));
        }
        if(encodedSize()>65536)throw new IllegalArgumentException("command byte budget");
    }
    public static ComposerCommand mode(int one,int two){return new ComposerCommand(Kind.MODE,null,null,null,null,null,one,two);}
    public static ComposerCommand resource(String path){return new ComposerCommand(Kind.RESOURCE,new String[]{path},null,null,null,null,0,0);}
    public static ComposerCommand nodes(Kind kind,String[] paths,String[] replacement,String[] tags){
        if(kind==Kind.MODE || kind==Kind.RESOURCE || kind==Kind.UPDATE || kind==Kind.UPDATES)throw new IllegalArgumentException("node operation");
        return new ComposerCommand(kind,paths,replacement,tags,null,null,0,0);
    }
    public static ComposerCommand update(String path,String key,float value){return new ComposerCommand(Kind.UPDATE,new String[]{path},null,null,new String[]{key},new float[]{value},0,0);}
    public static ComposerCommand updates(String[] paths,String[] keys,float[] values){return new ComposerCommand(Kind.UPDATES,paths,null,null,keys,values,0,0);}
    public String[] paths(){return clone(paths);} public String[] replacement(){return clone(replacement);}
    public String[] tags(){return clone(tags);} public String[] keys(){return clone(keys);} public float[] values(){return values==null?null:values.clone();}
    private static String[] clone(String[] a){return a==null?null:a.clone();}
    private static String[] copy(String[] a){
        if(a==null)return null;if(a.length>256)throw new IllegalArgumentException("item bound");String[] b=a.clone();
        for(String s:b)text(s);return b;
    }
    static void text(String s){
        if(s==null || s.length()>4096 || s.indexOf('\0')>=0)throw new IllegalArgumentException("bounded nonnull text required");
        // UTF-8 must be injective for our accepted Java strings. The default
        // encoder silently replaces distinct lone surrogates with the same byte.
        for(int i=0;i<s.length();i++) {
            char c=s.charAt(i);
            if(Character.isHighSurrogate(c)) {
                if(i+1>=s.length() || !Character.isLowSurrogate(s.charAt(i+1)))throw new IllegalArgumentException("unpaired high surrogate");
                i++;
            }else if(Character.isLowSurrogate(c))throw new IllegalArgumentException("unpaired low surrogate");
        }
    }
    private static void require(boolean ok){if(!ok)throw new IllegalArgumentException("operation argument shape");}
    int encodedSize(){int n=32;for(String[] a:new String[][]{paths,replacement,tags,keys})if(a!=null)for(String s:a)n+=4+s.getBytes(StandardCharsets.UTF_8).length;return n+(values==null?0:values.length*4);}
    void encode(DataOutputStream out)throws IOException {
        out.writeInt(kind.ordinal());out.writeInt(modeOne);out.writeInt(modeTwo);
        strings(out,paths);strings(out,replacement);strings(out,tags);strings(out,keys);
        out.writeInt(values==null?-1:values.length);if(values!=null)for(float v:values)out.writeInt(Float.floatToRawIntBits(v));
    }
    static void string(DataOutputStream out,String s)throws IOException {byte[] b=s.getBytes(StandardCharsets.UTF_8);out.writeInt(b.length);out.write(b);}
    private static void strings(DataOutputStream out,String[] values)throws IOException {out.writeInt(values==null?-1:values.length);if(values!=null)for(String s:values)string(out,s);}
    public ComposerCommand remap(ReplayPlan.ResourceMapping mapping)throws Exception {
        String[] mapped=map(paths,mapping),other=map(replacement,mapping);
        return new ComposerCommand(kind,mapped,other,tags,keys,values,modeOne,modeTwo);
    }
    private String[] map(String[] original,ReplayPlan.ResourceMapping mapping)throws Exception {
        if(original==null)return null;String[] out=new String[original.length];
        for(int i=0;i<out.length;i++) {
            // A node descriptor may end in :parameter:float. Mapping receives the
            // whole exact argument and must preserve that suffix, not its decimal round-trip.
            out[i]=mapping.mapExactArgument(original[i],kind!=Kind.RESOURCE);
            text(out[i]);if(kind!=Kind.RESOURCE && !sameSuffix(original[i],out[i]))throw new IllegalArgumentException("Changed inline composer parameter");
        }return out;
    }
    private static String suffix(String s) {
        int last=s.lastIndexOf(':'),prev=last<=0?-1:s.lastIndexOf(':',last-1);
        if(prev<0 || prev+1==last || last+1==s.length())return "";
        try{float v=Float.parseFloat(s.substring(last+1));return Float.isFinite(v)?s.substring(prev):"";}catch(NumberFormatException e){return "";}
    }
    private static boolean sameSuffix(String a,String b){return suffix(a).equals(suffix(b));}
}

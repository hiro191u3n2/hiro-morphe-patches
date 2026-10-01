package com.hiro.ulike.hdr.stillanalysis;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Owned values copied inside the stock FaceResultCallback; no SDK object escapes. */
public final class SdkFaceSnapshot {
    private final Face[] faces;
    public final boolean sourceCoordinateContractVerified = false;
    private SdkFaceSnapshot(Face[] faces) { this.faces = faces; }
    public Face[] faces() { return faces.clone(); }
    public static SdkFaceSnapshot copy(Object detectInfo) throws ReflectiveOperationException {
        if (detectInfo == null) throw new IllegalArgumentException("Missing face info");
        Object rawFaces = get(detectInfo, "getInfo");
        int count = length(rawFaces, 10, false);
        Face[] faces = new Face[count];
        for (int i = 0; i < count; i++) faces[i] = copyFace(Array.get(rawFaces, i));
        return new SdkFaceSnapshot(faces);
    }
    private static Face copyFace(Object f) throws ReflectiveOperationException {
        float[] points = points(get(f, "getPoints"), 106, false);
        if (points.length != 212) throw new IllegalArgumentException("Expected 106 points");
        float[] visibility = floats(get(f, "getPointVisibility"), 106, true);
        if (visibility.length != 0 && visibility.length != 106) throw new IllegalArgumentException("Visibility count");
        Object r = get(f, "getRect");
        int[] rect = r == null ? null : new int[]{integerField(r,"left"),integerField(r,"top"),integerField(r,"right"),integerField(r,"bottom")};
        if (rect != null && (rect[2] < rect[0] || rect[3] < rect[1])) throw new IllegalArgumentException("Invalid face rect");
        float score = number(get(f,"getScore"));
        if (score < 0 || score > 1) throw new IllegalArgumentException("Score range");
        Object ext = get(f,"getFaceExtInfo");
        LinkedHashMap<String,float[]> extra = new LinkedHashMap<>();
        LinkedHashMap<String,Integer> counts = new LinkedHashMap<>();
        if (ext != null) {
            for (String name : new String[]{"eyeCount","eyebrowCount","irisCount","lipCount"}) {
                int n = integerField(ext,name);
                if (n < 0 || n > 512) throw new IllegalArgumentException("Extra point count");
                counts.put(name,n);
            }
            for (String name : new String[]{"eyeLeftPoints","eyeRightPoints","eyeBrowLeftPoints","eyeBrowRightPoints","irisLeftPoints","irisRightPoints","lipPoints"})
                extra.put(name,points(field(ext,name),512,true));
        }
        return new Face(integer(get(f,"getFaceID")),integer(get(f,"getTrackCount")),integer(get(f,"getAction")),score,
                number(get(f,"getEyeDistance")),number(get(f,"getYaw")),number(get(f,"getPitch")),number(get(f,"getRoll")),
                points,visibility,rect,extra,counts,ext != null);
    }
    private static Object get(Object object,String name) throws ReflectiveOperationException {
        if (object == null) throw new IllegalArgumentException("Null SDK element");
        return object.getClass().getMethod(name).invoke(object);
    }
    private static Object field(Object object,String name) throws ReflectiveOperationException {
        if (object == null) throw new IllegalArgumentException("Null SDK element");
        Field field = object.getClass().getField(name); return field.get(object);
    }
    private static int integerField(Object o,String n) throws ReflectiveOperationException { return integer(field(o,n)); }
    private static int integer(Object value) {
        if (!(value instanceof Integer)) throw new IllegalArgumentException("Expected SDK int");
        return (Integer)value;
    }
    private static float number(Object value) {
        if (!(value instanceof Float) || !Float.isFinite((Float)value)) throw new IllegalArgumentException("Expected finite SDK float");
        return (Float)value;
    }
    private static int length(Object array,int maximum,boolean nullable) {
        if (array == null) { if (nullable) return 0; throw new IllegalArgumentException("Missing SDK array"); }
        if (!array.getClass().isArray()) throw new IllegalArgumentException("Expected SDK array");
        int n = Array.getLength(array);
        if (n > maximum) throw new IllegalArgumentException("Oversized SDK array");
        return n;
    }
    private static float[] floats(Object array,int maximum,boolean nullable) {
        int n = length(array,maximum,nullable); float[] out = new float[n];
        for (int i=0;i<n;i++) out[i]=number(Array.get(array,i));
        return out;
    }
    private static float[] points(Object array,int maximum,boolean nullable) throws ReflectiveOperationException {
        int n=length(array,maximum,nullable); float[] out=new float[n*2];
        for(int i=0;i<n;i++) { Object point=Array.get(array,i); out[2*i]=number(field(point,"x")); out[2*i+1]=number(field(point,"y")); }
        return out;
    }
    public static final class Face {
        public final int faceId,trackCount,action;
        public final float score,eyeDistance,yaw,pitch,roll;
        public final boolean extraInfoPresent;
        private final float[] points,visibility;
        private final int[] rect;
        private final Map<String,float[]> extra;
        private final Map<String,Integer> counts;
        private Face(int id,int track,int action,float score,float eye,float yaw,float pitch,float roll,float[] points,float[] visibility,int[] rect,Map<String,float[]> extra,Map<String,Integer> counts,boolean present) {
            faceId=id;trackCount=track;this.action=action;this.score=score;eyeDistance=eye;this.yaw=yaw;this.pitch=pitch;this.roll=roll;
            this.points=points;this.visibility=visibility;this.rect=rect;this.extra=extra;this.counts=Collections.unmodifiableMap(new LinkedHashMap<>(counts));extraInfoPresent=present;
        }
        public float[] points106() { return points.clone(); }
        public float[] visibility() { return visibility.clone(); }
        public int[] rect() { return rect==null ? null : rect.clone(); }
        public Map<String,Integer> extraCounts() { return counts; }
        /** Raw named SDK arrays. No UV topology / coordinate scaling is invented. */
        public Map<String,float[]> extraPoints() { LinkedHashMap<String,float[]> out=new LinkedHashMap<>();for(Map.Entry<String,float[]> e:extra.entrySet())out.put(e.getKey(),e.getValue().clone());return Collections.unmodifiableMap(out); }
    }
}

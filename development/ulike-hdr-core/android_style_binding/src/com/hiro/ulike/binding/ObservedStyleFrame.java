package com.hiro.ulike.binding;

import com.hiro.ulike.hdr.stillanalysis.StillMessageCollector;
import java.util.*;

/** Parses the owned still's actual Lua observations. A completed transport is
 * evidence of setter arguments/material values and available 3D algorithm data;
 * it is NOT proof of GPU draw execution, native 2D vertices or segmentation pixels.
 */
public final class ObservedStyleFrame {
    public final Object frameIdentity;
    public final int nonce;
    public final String styleId;
    public final boolean native2dGeometryAvailable=false,skinMaskPixelsAvailable=false,completeStyleBinding=false;
    private final Map<String,Double> uniforms;
    private final Map<Integer,Mesh3d> meshes;
    public static final class Mesh3d {
        public final int faceIndex,vertexCount;
        private final double[] xyz,mvp;
        private Mesh3d(int face,double[] xyz,double[] mvp){faceIndex=face;vertexCount=xyz.length/3;this.xyz=xyz.clone();this.mvp=mvp.clone();}
        public double[] copyVertices(){return xyz.clone();}
        /** Four successive GetRow results, each x/y/z/w; no transpose inferred. */
        public double[] copyMvpRows(){return mvp.clone();}
    }
    private ObservedStyleFrame(Object id,int nonce,String style,Map<String,Double> uniforms,Map<Integer,Mesh3d> meshes){
        frameIdentity=id;this.nonce=nonce;styleId=style;this.uniforms=Collections.unmodifiableMap(uniforms);this.meshes=Collections.unmodifiableMap(meshes);
    }
    public Set<Integer> observed3dFaces(){return meshes.keySet();}
    public Mesh3d mesh3d(int face){Mesh3d m=meshes.get(face);require(m!=null,"3D face was not observed");return m;}
    /** Returns the last post-setter/material value within this one update. */
    public double uniform(String feature,String component,int face,String key){Double v=uniforms.get(key(feature,component,face,key));require(v!=null,"uniform not observed for this face/component");return v;}
    public static Set<String> expectedFeatures(String style){
        if(ShotStyleSettings.NATURAL.equals(style))return Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList("AmazingFeature1","AmazingFeature0","AmazingFeature2")));
        if(ShotStyleSettings.PURITY.equals(style))return Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList("AmazingFeature1","AmazingFeature3","AmazingFeature5","AmazingFeature6","AmazingFeature7","3dmakeup4","AmazingFeature0","AmazingFeature8","AmazingFeature9")));
        throw new IllegalArgumentException("unsupported style");
    }
    private static String key(String f,String c,int face,String k){return f+"\0"+c+"\0"+face+"\0"+k;}
    private static void require(boolean yes,String m){if(!yes)throw new IllegalArgumentException(m);}
    private static double number(String s){require(s.matches("[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?"),"numeric grammar");double n=Double.parseDouble(s);require(Double.isFinite(n),"nonfinite observation");return n;}
    private static double[] numbers(String s,int n){String[] a=s.split(",",-1);require(a.length==n,"observation number count");double[] out=new double[n];for(int i=0;i<n;i++)out[i]=number(a[i]);return out;}
    private static int index(String s){require(s.matches("0|[1-9][0-9]{0,4}"),"index grammar");return Integer.parseInt(s);}
    private static final class MeshBuilder {final double[] xyz=new double[1427*3];double[] mvp;int next;}
    public static ObservedStyleFrame parse(StillMessageCollector.Snapshot source,Object exactFrameIdentity,int expectedNonce,String style){
        require(source!=null && source.failure==null && source.allExpectedExportsObserved && expectedNonce>0 && source.nonce==expectedNonce && exactFrameIdentity!=null,"completed same-submission observation required");
        require(source.records().keySet().equals(expectedFeatures(style)),"selected-style feature set mismatch");
        Map<String,Double> uniforms=new LinkedHashMap<>();Map<Integer,MeshBuilder> builders=new LinkedHashMap<>();
        for(Map.Entry<String,List<StillMessageCollector.Record>> feature:source.records().entrySet()){
            String name=feature.getKey();
            for(StillMessageCollector.Record r:feature.getValue()){
                if(r.kind.equals("END"))continue;
                if(r.kind.equals("uniform")){
                    String[] p=r.payload.split(",",-1);require(p.length==3 && p[0].matches("[A-Za-z0-9_-]{1,128}"),"uniform payload");
                    boolean global=name.equals("AmazingFeature0") || (ShotStyleSettings.NATURAL.equals(style)?name.equals("AmazingFeature2"):name.equals("AmazingFeature8")||name.equals("AmazingFeature9"));
                    require(global==(r.faceIndex==-1),"uniform face/global provenance");
                    require(p[1].equals(global && !name.equals("AmazingFeature0")?"uniAlpha":"intensity"),"unexpected observed uniform");
                    uniforms.put(key(name,p[0],r.faceIndex,p[1]),number(p[2]));continue;
                }
                require(ShotStyleSettings.PURITY.equals(style) && name.equals("3dmakeup4") && r.faceIndex>=0 && r.faceIndex<=9,"mesh data in wrong feature");
                MeshBuilder b=builders.get(r.faceIndex);if(b==null){b=new MeshBuilder();builders.put(r.faceIndex,b);}
                if(r.kind.equals("mvp")){require(b.mvp==null,"duplicate MVP");b.mvp=numbers(r.payload,16);}
                else if(r.kind.equals("vertices")){
                    String[] p=r.payload.split(";",-1);require(p.length==2,"vertex chunk fields");String[] h=p[0].split(",",-1);require(h.length==3,"vertex chunk header");
                    int first=index(h[0]),count=index(h[1]),total=index(h[2]);require(total==1427 && first==b.next && count>=1 && count<=64 && first+count<=total,"vertex chunk order/count");
                    double[] values=numbers(p[1],count*3);System.arraycopy(values,0,b.xyz,first*3,values.length);b.next+=count;
                } else throw new IllegalArgumentException("unsupported observed data");
            }
        }
        Map<Integer,Mesh3d> meshes=new LinkedHashMap<>();
        for(Map.Entry<Integer,MeshBuilder> e:builders.entrySet()){MeshBuilder b=e.getValue();require(b.next==1427 && b.mvp!=null,"partial 3D face export");meshes.put(e.getKey(),new Mesh3d(e.getKey(),b.xyz,b.mvp));}
        return new ObservedStyleFrame(exactFrameIdentity,expectedNonce,style,uniforms,meshes);
    }
}

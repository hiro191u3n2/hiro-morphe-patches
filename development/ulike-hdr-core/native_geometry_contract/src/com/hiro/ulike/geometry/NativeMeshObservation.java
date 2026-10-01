package com.hiro.ulike.geometry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Owned diagnostics from a pinned late-update observer; never a production face binding. */
public final class NativeMeshObservation {
    public static final int MESSAGE_ID = 0x554c4701;
    private NativeMeshObservation() {}
    public static final class Mesh {
        public final String entityNameHex;
        public final int componentOrdinal, luaUserdataAliasOrdinal;
        private final int[] declaredFaceIds;
        private final double[] positions;
        private final int semantic, width;
        private Mesh(int component, int shared, String name, int[] ids, double[] xy,int semantic,int width) {
            componentOrdinal=component; luaUserdataAliasOrdinal=shared; entityNameHex=name;
            declaredFaceIds=ids.clone(); positions=xy.clone(); this.semantic=semantic;this.width=width;
        }
        public int[] declaredFaceIds() { return declaredFaceIds.clone(); }
        public double[] positions() { return positions.clone(); }
        public int positionSemantic() { return semantic; }
        public int positionComponents() { return width; }
        public int vertexCount() { return positions.length/width; }
        /** This is a Lua userdata alias observation, not a final active face/submesh map. */
        public boolean perFaceRoutingVerified() { return false; }
    }
    public static final class Feature {
        public final String name;
        public final int algorithmFaceCount;
        public final List<Mesh> meshes;
        private Feature(String n, int count, List<Mesh> m) {
            name=n; algorithmFaceCount=count;
            meshes=Collections.unmodifiableList(new ArrayList<Mesh>(m));
        }
    }
    public static final class Frame {
        private final Object owner;
        public final int nonce;
        public final List<Feature> features;
        private Frame(Object id,int n,List<Feature> f) {
            owner=id;nonce=n;features=Collections.unmodifiableList(new ArrayList<Feature>(f));
        }
        public boolean belongsTo(Object identity) { return identity!=null && identity==owner; }
        public boolean productionGeometryVerified() { return false; }
        public boolean legacyV2OpacityObserved() { return false; }
    }
    private static final class PendingMesh {
        int ordinal, shared, next, semantic, width;
        String name;
        int[] ids;
        double[] xy;
        Mesh finish() {
            require(next*width==xy.length,"incomplete mesh chunks");
            return new Mesh(ordinal,shared,name,ids,xy,semantic,width);
        }
    }
    private static final class Pending {
        int sequence, faceCount=-1, componentCount=-1, vertices;
        boolean ended;
        PendingMesh mesh;
        List<Mesh> meshes=new ArrayList<Mesh>();
    }
    public static final class Collector {
        private final Object owner;
        private final int nonce;
        private final Map<String,Pending> expected=new LinkedHashMap<String,Pending>();
        private int chars;
        private boolean failed, finished;
        public Collector(Object exactAnalysisIdentity,int n,List<String> featureNames) {
            require(exactAnalysisIdentity!=null && n>0,"analysis identity and positive nonce required");
            owner=exactAnalysisIdentity;nonce=n;
            require(featureNames!=null && featureNames.size()>0 && featureNames.size()<=5,"feature budget");
            for(String name:featureNames) {
                require(name!=null && name.matches("AmazingFeature[13567]"),"unknown feature");
                require(!expected.containsKey(name),"duplicate feature");expected.put(name,new Pending());
            }
        }
        /** Returns false only for another protocol. Any malformed G1 poisons this collector. */
        public synchronized boolean accept(int id,long requestNonce,long ordinal,String text) {
            if(id!=MESSAGE_ID)return false;
            try {
                require(!failed && !finished,"collector unavailable");
                require(requestNonce==nonce && ordinal>=1 && ordinal<=600,"nonce/ordinal mismatch");
                require(text!=null && text.length()<=8192,"packet budget");
                chars=Math.addExact(chars,text.length());require(chars<=8*1024*1024,"frame text budget");
                String[] fields=text.split("\\|",-1);
                require(fields.length==5 && fields[0].equals("G1"),"packet grammar");
                Pending p=expected.get(fields[1]);require(p!=null && !p.ended,"feature unavailable");
                require(ordinal==p.sequence+1,"noncontiguous packet");p.sequence++;
                int component=integer(fields[3],-1,31);
                String payload=fields[4], kind=fields[2];
                if(kind.equals("ERROR"))throw new IllegalArgumentException("native geometry unavailable");
                if(kind.equals("BEGIN")) {
                    require(p.faceCount<0 && component==-1 && p.sequence==1,"unexpected BEGIN");
                    String[] a=payload.split(",",-1);require(a.length==2,"BEGIN grammar");
                    p.faceCount=integer(a[0],0,10);p.componentCount=integer(a[1],0,32);
                } else {
                    require(p.faceCount>=0,"BEGIN required");
                    if(kind.equals("MESH")) {
                        if(p.mesh!=null){p.meshes.add(p.mesh.finish());p.mesh=null;}
                        require(component==p.meshes.size() && component<p.componentCount,"component order");
                        String[] a=payload.split(",",-1);require(a.length==4,"MESH grammar");
                        require(a[0].matches("[0-9a-f]{2,256}") && a[0].length()%2==0,"name hex");
                        PendingMesh m=new PendingMesh();m.ordinal=component;m.name=a[0];m.shared=integer(a[1],0,component);
                        int count=integer(a[2],1,8192);p.vertices+=count;require(p.vertices<=32768,"feature vertex budget");
                        String[] ids=a[3].isEmpty()?new String[0]:a[3].split(":",-1);
                        require(ids.length<=32,"face slot budget");m.ids=new int[ids.length];
                        for(int i=0;i<ids.length;i++)m.ids[i]=integer(ids[i],0,255);
                        m.semantic=fields[1].equals("AmazingFeature6")?0:13;
                        m.width=m.semantic==0?3:2;
                        m.xy=new double[count*m.width];p.mesh=m;
                    } else if(kind.equals("POS")) {
                        PendingMesh m=p.mesh;require(m!=null && component==m.ordinal,"mesh required");
                        String[] a=payload.split(";",-1);require(a.length==2,"POS grammar");
                        String[] head=a[0].split(",",-1);require(head.length==2,"chunk grammar");
                        int start=integer(head[0],0,8191),count=integer(head[1],1,64);
                        require(start==m.next && start+count<=m.xy.length/m.width,"chunk extent/order");
                        String[] values=a[1].split(",",-1);require(values.length==count*m.width,"component count");
                        for(int i=0;i<values.length;i++)m.xy[start*m.width+i]=number(values[i]);m.next+=count;
                    } else if(kind.equals("END")) {
                        require(component==-1 && integer(payload,1,599)==p.sequence-1,"END count");
                        if(p.mesh!=null){p.meshes.add(p.mesh.finish());p.mesh=null;}
                        require(p.meshes.size()==p.componentCount,"missing component");p.ended=true;
                    } else throw new IllegalArgumentException("unknown observation kind");
                }
                return true;
            } catch(RuntimeException e) { failed=true;throw e; }
        }
        public synchronized Frame finish(Object exactAnalysisIdentity) {
            try {
                require(!failed && !finished && exactAnalysisIdentity==owner,"wrong/incomplete analysis owner");
                List<Feature> out=new ArrayList<Feature>();int faceCount=-1;
                for(Map.Entry<String,Pending> e:expected.entrySet()) {
                    Pending p=e.getValue();require(p.ended,"missing late update");
                    if(faceCount<0)faceCount=p.faceCount;else require(faceCount==p.faceCount,"different face counts");
                    out.add(new Feature(e.getKey(),p.faceCount,p.meshes));
                }
                finished=true;return new Frame(owner,nonce,out);
            } catch(RuntimeException e) { failed=true;throw e; }
        }
    }
    private static int integer(String s,int lo,int hi) {
        require(s.matches("-?(0|[1-9][0-9]{0,9})"),"integer grammar");
        long v=Long.parseLong(s);require(v>=lo && v<=hi,"integer range");return (int)v;
    }
    private static double number(String s) {
        require(s.length()<=32 && s.matches("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?"),"float grammar");
        double v=Double.parseDouble(s);
        require(v!=0 || s.split("[eE]",-1)[0].matches("-?0(\\.0+)?"),"decimal underflow");
        require(!Double.isNaN(v) && !Double.isInfinite(v) && (double)(float)v==v,"native float32 value required");
        return v;
    }
    private static void require(boolean v,String message) { if(!v)throw new IllegalArgumentException(message); }
}

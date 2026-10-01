package com.hiro.ulike.geometry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Current CPU mesh topology, never inferred face routing or a GPU draw-completion receipt. */
public final class NativeDrawObservation {
    public static final int MESSAGE_ID=0x554c4401;
    private NativeDrawObservation() {}
    public static final class Submesh {
        public final int ordinal, primitive, storedIndexCount;
        public final boolean indices32;
        private final int[] indices;
        private Submesh(int ordinal,int primitive,int stored,boolean wide,int[] indices) {
            this.ordinal=ordinal;this.primitive=primitive;storedIndexCount=stored;indices32=wide;this.indices=indices.clone();
        }
        /** Only the current indicesCount prefix; unused backing storage is deliberately not exported. */
        public int[] indices() { return indices.clone(); }
        public int requestedIndexCount() { return indices.length; }
    }
    public static final class Mesh {
        public final String entityNameHex;
        public final int componentOrdinal,generatedRendererMaterialCount;
        public final List<Submesh> submeshes;
        private final int[] declaredFaceIds;
        private final double[] positions,uv;
        private final int width;
        private Mesh(PendingMesh p) {
            entityNameHex=p.name;componentOrdinal=p.ordinal;generatedRendererMaterialCount=p.materials;width=p.width;
            declaredFaceIds=p.ids.clone();positions=p.pos.clone();uv=p.uv.clone();
            submeshes=Collections.unmodifiableList(new ArrayList<Submesh>(p.submeshes));
        }
        public int positionComponents(){return width;}
        public int positionSemantic(){return width==3?0:13;}
        public int uvSemantic(){return 6;}
        public int vertexCount(){return uv.length/2;}
        public int[] declaredFaceIds(){return declaredFaceIds.clone();}
        public double[] positions(){return positions.clone();}
        public double[] uv(){return uv.clone();}
        public boolean perFaceRoutingVerified(){return false;}
        public boolean materialToSubmeshMappingVerified(){return false;}
    }
    public static final class Feature {
        public final String name;
        public final int algorithmFaceCount;
        public final List<Mesh> meshes;
        private Feature(String name,Pending p){this.name=name;algorithmFaceCount=p.faceCount;meshes=Collections.unmodifiableList(new ArrayList<Mesh>(p.meshes));}
    }
    public static final class Frame {
        private final Object owner;
        public final int nonce;
        public final List<Feature> features;
        private Frame(Object owner,int nonce,List<Feature> features){this.owner=owner;this.nonce=nonce;this.features=Collections.unmodifiableList(features);}
        public boolean belongsTo(Object identity){return identity!=null && identity==owner;}
        public boolean productionGeometryVerified(){return false;}
        public boolean finalUniformMatricesObserved(){return false;}
        public boolean legacyV2OpacityObserved(){return false;}
        /** Cross-check two read-only snapshots; equality does not establish native frame attribution. */
        public void requireSamePositions(NativeMeshObservation.Frame other) {
            require(other!=null && other.belongsTo(owner) && other.nonce==nonce && other.features.size()==features.size(),"mesh snapshot identity");
            for(int f=0;f<features.size();f++) {
                Feature a=features.get(f);NativeMeshObservation.Feature b=other.features.get(f);
                require(a.name.equals(b.name) && a.algorithmFaceCount==b.algorithmFaceCount && a.meshes.size()==b.meshes.size(),"feature snapshot mismatch");
                for(int m=0;m<a.meshes.size();m++) {
                    Mesh x=a.meshes.get(m);NativeMeshObservation.Mesh y=b.meshes.get(m);
                    require(x.entityNameHex.equals(y.entityNameHex) && x.componentOrdinal==y.componentOrdinal && x.positionSemantic()==y.positionSemantic(),"component snapshot mismatch");
                    require(java.util.Arrays.equals(x.declaredFaceIds,y.declaredFaceIds()) && java.util.Arrays.equals(x.positions,y.positions()),"geometry changed between snapshots");
                }
            }
        }
    }
    private static final class PendingSub {
        int ordinal,primitive,stored,next;boolean wide;int[] indices;
        Submesh finish(){require(next==indices.length,"incomplete index stream");return new Submesh(ordinal,primitive,stored,wide,indices);}
    }
    private static final class PendingMesh {
        int ordinal,width,materials,subCount,next;String name;int[] ids;double[] pos,uv;PendingSub sub;
        final List<Submesh> submeshes=new ArrayList<Submesh>();
        void finishSub(){if(sub!=null){submeshes.add(sub.finish());sub=null;}}
        Mesh finish(){require(next==uv.length/2,"incomplete vertices");finishSub();require(submeshes.size()==subCount,"missing submesh");return new Mesh(this);}
    }
    private static final class Pending {
        int sequence,faceCount=-1,componentCount,vertices,indices;boolean ended;PendingMesh mesh;
        final List<Mesh> meshes=new ArrayList<Mesh>();
        void finishMesh(){if(mesh!=null){meshes.add(mesh.finish());mesh=null;}}
    }
    public static final class Collector {
        private final Object owner;private final int nonce;
        private final Map<String,Pending> expected=new LinkedHashMap<String,Pending>();
        private int chars;private boolean failed,finished;
        public Collector(Object exactAnalysisIdentity,int nonce,List<String> features) {
            require(exactAnalysisIdentity!=null && nonce>0,"analysis owner/nonce");owner=exactAnalysisIdentity;this.nonce=nonce;
            require(features!=null && !features.isEmpty() && features.size()<=5,"feature budget");
            for(String feature:features){require(feature!=null && feature.matches("AmazingFeature[13567]") && !expected.containsKey(feature),"feature identity");expected.put(feature,new Pending());}
        }
        public synchronized boolean accept(int id,long requestNonce,long ordinal,String text) {
            if(id!=MESSAGE_ID)return false;
            try {
                require(!failed && !finished,"collector unavailable");require(requestNonce==nonce && ordinal>=1 && ordinal<=8192,"nonce/ordinal");
                require(text!=null && text.length()<=8192,"packet budget");chars=Math.addExact(chars,text.length());require(chars<=24*1024*1024,"frame budget");
                String[] f=text.split("\\|",-1);require(f.length==5 && f[0].equals("D1"),"packet grammar");
                Pending p=expected.get(f[1]);require(p!=null && !p.ended && ordinal==p.sequence+1,"feature/sequence");p.sequence++;
                int component=integer(f[3],-1,31);String kind=f[2],payload=f[4];
                if(kind.equals("ERROR"))throw new IllegalArgumentException("native topology unavailable");
                if(kind.equals("BEGIN")){
                    require(p.faceCount<0 && component==-1 && p.sequence==1,"unexpected BEGIN");String[] a=fields(payload,",",2);
                    p.faceCount=integer(a[0],0,10);p.componentCount=integer(a[1],0,32);
                }else{
                    require(p.faceCount>=0,"BEGIN required");
                    if(kind.equals("MESH")){
                        p.finishMesh();require(component==p.meshes.size() && component<p.componentCount,"component order");String[] a=fields(payload,",",5);
                        PendingMesh m=new PendingMesh();m.ordinal=component;m.name=a[0];require(m.name.matches("[0-9a-f]{2,256}") && m.name.length()%2==0,"name hex");
                        int count=integer(a[1],1,8192);p.vertices+=count;require(p.vertices<=32768,"vertex budget");m.width=f[1].equals("AmazingFeature6")?3:2;
                        m.subCount=integer(a[2],0,64);m.materials=integer(a[3],-1,64);
                        String[] ids=a[4].isEmpty()?new String[0]:a[4].split(":",-1);require(ids.length<=32,"face eligibility budget");m.ids=new int[ids.length];
                        for(int i=0;i<ids.length;i++)m.ids[i]=integer(ids[i],0,255);m.pos=new double[count*m.width];m.uv=new double[count*2];p.mesh=m;
                    }else if(kind.equals("VTX")){
                        PendingMesh m=mesh(p,component);require(m.sub==null && m.submeshes.isEmpty(),"vertices after topology");String[] a=fields(payload,";",2),head=fields(a[0],",",2);
                        int first=integer(head[0],0,8191),count=integer(head[1],1,32);require(first==m.next && first+count<=m.uv.length/2,"vertex order/extent");
                        String[] v=a[1].split(",",-1);require(v.length==count*(m.width+2),"attribute count");
                        for(int i=0;i<count;i++){for(int j=0;j<m.width;j++)m.pos[(first+i)*m.width+j]=number(v[i*(m.width+2)+j]);for(int j=0;j<2;j++)m.uv[(first+i)*2+j]=number(v[i*(m.width+2)+m.width+j]);}m.next+=count;
                    }else if(kind.equals("SUB")){
                        PendingMesh m=mesh(p,component);require(m.next==m.uv.length/2,"vertices required");m.finishSub();String[] a=fields(payload,",",5);PendingSub s=new PendingSub();
                        s.ordinal=integer(a[0],0,63);require(s.ordinal==m.submeshes.size() && s.ordinal<m.subCount,"submesh order");s.wide=integer(a[1],0,1)==1;s.primitive=integer(a[2],0,255);
                        int count=integer(a[3],0,65536);s.stored=integer(a[4],count,65536);p.indices+=count;require(p.indices<=262144,"index budget");s.indices=new int[count];m.sub=s;
                    }else if(kind.equals("IDX")){
                        PendingMesh m=mesh(p,component);PendingSub s=m.sub;require(s!=null,"submesh required");String[] a=fields(payload,";",2),head=fields(a[0],",",3);
                        require(integer(head[0],0,63)==s.ordinal,"index submesh");int first=integer(head[1],0,65535),count=integer(head[2],1,128);
                        require(first==s.next && first+count<=s.indices.length,"index order/extent");String[] v=a[1].split(",",-1);require(v.length==count,"index count");
                        for(int i=0;i<count;i++)s.indices[first+i]=integer(v[i],0,m.uv.length/2-1);s.next+=count;
                    }else if(kind.equals("END")){
                        require(component==-1 && integer(payload,1,8191)==p.sequence-1,"END count");p.finishMesh();require(p.meshes.size()==p.componentCount,"missing component");p.ended=true;
                    }else throw new IllegalArgumentException("unknown record");
                }
                return true;
            }catch(RuntimeException e){failed=true;throw e;}
        }
        public synchronized Frame finish(Object exactAnalysisIdentity) {
            try{
                require(!failed && !finished && exactAnalysisIdentity==owner,"wrong/incomplete owner");List<Feature> out=new ArrayList<Feature>();int faces=-1;
                for(Map.Entry<String,Pending> e:expected.entrySet()){Pending p=e.getValue();require(p.ended,"missing END");if(faces<0)faces=p.faceCount;else require(faces==p.faceCount,"face count changed");out.add(new Feature(e.getKey(),p));}
                finished=true;return new Frame(owner,nonce,out);
            }catch(RuntimeException e){failed=true;throw e;}
        }
    }
    private static PendingMesh mesh(Pending p,int component){require(p.mesh!=null && p.mesh.ordinal==component,"mesh required");return p.mesh;}
    private static String[] fields(String text,String delimiter,int count){String[] out=text.split(delimiter,-1);require(out.length==count,"record grammar");return out;}
    private static int integer(String s,int lo,int hi){require(s.matches("-?(0|[1-9][0-9]{0,9})"),"integer grammar");long n=Long.parseLong(s);require(n>=lo && n<=hi,"integer range");return (int)n;}
    private static double number(String s){require(s.length()<=32 && s.matches("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?"),"float grammar");double n=Double.parseDouble(s);require(n!=0 || s.split("[eE]",-1)[0].matches("-?0(\\.0+)?"),"decimal underflow");require(!Double.isNaN(n) && !Double.isInfinite(n) && (double)(float)n==n,"native float32 value required");return n;}
    private static void require(boolean condition,String message){if(!condition)throw new IllegalArgumentException(message);}
}

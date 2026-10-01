package com.hiro.ulike.binding;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reads the pinned style's real UVs/topology. Authored position samples are
 * deliberately not exposed as live face geometry. No 106-point remapping is
 * invented. Position semantic/UV relationships are checked against the actual
 * selected xshader attribute bindings by the accompanying asset audit.
 */
public final class AuthoredMesh {
    public final String sourceSha256;
    public final int vertexCount,strideBytes,positionSemantic,positionComponents;
    public final List<Submesh> submeshes;
    private final double[] uv;
    public static final class Submesh {
        public final String name;public final int primitive;
        private final int[] triangles;
        Submesh(String name,int primitive,int[] triangles){this.name=name;this.primitive=primitive;this.triangles=triangles;}
        public int[] copyTriangleIndices(){return triangles.clone();}
        public int triangleCount(){return triangles.length/3;}
    }
    private AuthoredMesh(String sha,int count,int stride,int semantic,int components,double[] uv,List<Submesh> submeshes) {
        sourceSha256=sha;vertexCount=count;strideBytes=stride;positionSemantic=semantic;positionComponents=components;
        this.uv=uv;this.submeshes=java.util.Collections.unmodifiableList(submeshes);
    }
    public double[] copyUv(){return uv.clone();}
    private static void require(boolean value,String text){if(!value)throw new IllegalArgumentException(text);}
    private static int hash(String key){int h=5381;for(int i=0;i<key.length();i++)h=h*33+key.charAt(i);return h;}
    private static String sha(byte[] b)throws Exception {byte[] d=MessageDigest.getInstance("SHA-256").digest(b);StringBuilder s=new StringBuilder();for(byte x:d)s.append(String.format(java.util.Locale.ROOT,"%02x",x&255));return s.toString();}
    private static final class Cursor {
        final ByteBuffer b;final int end;
        Cursor(byte[] bytes,int start,int end){b=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);b.position(start);this.end=end;}
        int u32(){require(b.position()+4<=end,"truncated mesh field");return b.getInt();}
        byte[] bytes(int n){require(n>=0 && (long)b.position()+n<=end,"mesh field bounds");byte[] v=new byte[n];b.get(v);return v;}
    }
    private static Map<Integer,List<byte[]>> object(Cursor c,int expectedType) {
        require(c.u32()==expectedType && c.u32()==2,"mesh object type/version");int count=c.u32();require(count>=0 && count<=32,"mesh property count");
        Map<Integer,List<byte[]>> result=new LinkedHashMap<>();
        for(int i=0;i<count;i++){int key=c.u32(),n=c.u32();byte[] payload=c.bytes(n);List<byte[]> found=result.get(key);if(found==null){found=new ArrayList<>();result.put(key,found);}found.add(payload);}
        return result;
    }
    private static byte[] field(Map<Integer,List<byte[]>> m,String key){List<byte[]> v=m.get(hash(key));require(v!=null && v.size()==1,"missing/ambiguous mesh field "+key);return v.get(0);}
    private static byte[] field(Map<Integer,List<byte[]>> m,int key){List<byte[]> v=m.get(key);require(v!=null && v.size()==1,"missing/ambiguous mesh field");return v.get(0);}
    private static int integer(byte[] p){Cursor c=new Cursor(p,0,p.length);require(c.u32()==0x0d66433a && c.u32()==3 && p.length==16,"mesh integer layout");int n=c.u32();require(c.u32()==0,"mesh integer high bits");return n;}
    private static int enumeration(byte[] p,int type){Cursor c=new Cursor(p,0,p.length);require(c.u32()==type && c.u32()==1 && p.length==12,"mesh enum layout");return c.u32();}
    private static String firstName(Map<Integer,List<byte[]>> m)throws Exception {
        List<byte[]> values=m.get(hash("name"));require(values!=null && !values.isEmpty(),"mesh object name");byte[] p=values.get(0);Cursor c=new Cursor(p,0,p.length);
        require(c.u32()==0xd1ee9bdc && c.u32()==0,"mesh string layout");int n=c.u32();require(n<=256 && n==p.length-12,"mesh name bound");return new String(c.bytes(n),"UTF-8");
    }
    private static Cursor vector(byte[] p){Cursor c=new Cursor(p,0,p.length);require(c.u32()==0xd7d69b78 && c.u32()==34,"mesh object vector");return c;}
    public static AuthoredMesh readPinned(byte[] bytes,String expectedSha256)throws Exception {
        require(bytes!=null && bytes.length>=88 && bytes.length<=4*1024*1024 && expectedSha256!=null && expectedSha256.matches("[0-9a-f]{64}"),"bounded pinned mesh required");
        bytes=bytes.clone();require(sha(bytes).equals(expectedSha256),"mesh SHA-256 mismatch");
        byte[] signature="%SerializedFormat%@\n".getBytes("US-ASCII");for(int i=0;i<signature.length;i++)require(bytes[i]==signature[i],"mesh signature");
        Cursor root=new Cursor(bytes,64,bytes.length);require(root.u32()==1 && root.u32()==0x7c890592 && root.u32()==bytes.length-76,"mesh root count/length");
        Map<Integer,List<byte[]>> fields=object(root,0x7c890592);require(root.b.position()==root.end,"trailing mesh fields");
        byte[] data=field(fields,"vertices");Cursor floats=new Cursor(data,0,data.length);require(floats.u32()==0xcf7a586e && floats.u32()==24,"mesh float vector");int nf=floats.u32();require(nf>=1 && nf<=1000000 && (long)nf*4==data.length-12,"mesh float count");
        float[] raw=new float[nf];for(int i=0;i<nf;i++){raw[i]=Float.intBitsToFloat(floats.u32());require(Float.isFinite(raw[i]),"nonfinite authored vertex");}
        Cursor attrs=vector(field(fields,"vertexAttribs"));int na=attrs.u32();require(na>0 && na<=8,"vertex attribute count");
        int stride=0,uvOffset=-1,position=-1,components=0;
        for(int i=0;i<na;i++){
            Map<Integer,List<byte[]>> a=object(attrs,0x904eef68);
            int semantic=enumeration(field(a,"semantic"),0x90580a2b),offset=integer(field(a,"offset")),size=integer(field(a,"componentCount"));
            require(integer(field(a,0xf62a2538))==0 && offset>=0 && offset<=128 && offset%4==0 && size>=1 && size<=4,"unsupported vertex format");
            stride=Math.max(stride,offset+size*4);
            if(semantic==6){require(uvOffset<0 && size==2,"UV attribute");uvOffset=offset/4;}
            if(semantic==0 || semantic==13){require(position<0 && ((semantic==0 && size==3)||(semantic==13 && size==2)),"position attribute");position=semantic;components=size;}
        }
        require(attrs.b.position()==attrs.end && stride>0 && uvOffset>=0 && position>=0 && (long)nf*4%stride==0,"vertex attribute layout");
        int nv=nf*4/stride;double[] uv=new double[nv*2];for(int i=0;i<nv;i++){uv[2*i]=raw[i*(stride/4)+uvOffset];uv[2*i+1]=raw[i*(stride/4)+uvOffset+1];}
        Cursor subs=vector(field(fields,"submeshes"));int ns=subs.u32();require(ns>=1 && ns<=8,"submesh bound");List<Submesh> out=new ArrayList<>();
        for(int i=0;i<ns;i++){
            Map<Integer,List<byte[]>> sub=object(subs,0x10e638fc);String name=firstName(sub);
            int primitive=enumeration(field(sub,"primitive"),0x26e355fe);require(primitive==4,"only triangle-list topology is supported");
            byte[] ib=field(sub,"indices16");Cursor idx=new Cursor(ib,0,ib.length);require(idx.u32()==0x6b282c5f && idx.u32()==22,"16-bit index vector");int ni=idx.u32();require(ni>0 && ni%3==0 && ni<=1000000 && (long)ni*2==ib.length-12,"triangle index count");
            require(integer(field(sub,"indicesCount"))==ni,"submesh draw count");
            byte[] i32=field(sub,"indices32");Cursor empty=new Cursor(i32,0,i32.length);require(i32.length==12 && empty.u32()==0x0fc2e31d && empty.u32()==23 && empty.u32()==0,"unsupported 32-bit index vector");
            int[] tri=new int[ni];for(int k=0;k<ni;k++){tri[k]=Short.toUnsignedInt(idx.b.getShort());require(tri[k]<nv,"index outside vertex buffer");}
            out.add(new Submesh(name,primitive,tri));
        }
        require(subs.b.position()==subs.end,"trailing submesh data");return new AuthoredMesh(expectedSha256,nv,stride,position,components,uv,out);
    }
}

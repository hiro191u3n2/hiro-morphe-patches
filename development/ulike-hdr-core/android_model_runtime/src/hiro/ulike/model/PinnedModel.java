package hiro.ulike.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/** Reads only the two audited models. No model, loader material or vendor code is bundled. */
public final class PinnedModel {
    public enum Style { NATURAL_BLUSH, PURITY2 }
    static final String BAOMAN = "e64f0772bb857e4d990789237c1007b62fb86020a7edcfb0c3a61a7bedc6e2c0";
    static final String GOODLIKE = "0d60ea7e684f32628daac031cb37fd32c50bf898aa3fb62bcbc25673b5725cad";
    static final String BYTENN = "cedda347b55c03de82cc22b7f003777f40750d4f341494a29587d7b821ad7853";
    static final String EFFECT = "d40af10b250b91cf8f30f4a265ac1d3b7b5b88a82bbf63da332c3f47a415d48e";
    static final String BAOMAN_GRAPH = "40e9f7491cc3be27fa63e5aef589bd9359b934403a092b75f2f2e5f3ef0e41a9";
    static final String GOODLIKE_GRAPH = "0a5c6692f3361a0c5247c95065e446ca8245db7aa632f26ef227253f06254a26";
    private static final int MAX_INPUT = 128 * 1024 * 1024;
    private PinnedModel() {}

    /** Streams remain caller-owned. Returned bytes contain caller model data; never publish them. */
    public static CompiledModel compile(Style style, InputStream model, InputStream bytenn,
                                        InputStream effect) throws IOException, GeneralSecurityException {
        require(style != null && model != null && bytenn != null, "missing required input");
        byte[] modelBytes = readPinned(model, style == Style.NATURAL_BLUSH ? BAOMAN : GOODLIKE);
        byte[] libraryBytes = readPinned(bytenn, BYTENN);
        Decoded decoded;
        if (style == Style.NATURAL_BLUSH) decoded = baoman(modelBytes, libraryBytes);
        else {
            require(effect != null, "Purity loader library required");
            byte[] effectBytes = readPinned(effect, EFFECT);
            decoded = goodlike(modelBytes, effectBytes);
        }
        return GraphOnnx.compile(style, decoded);
    }

    /** Immutable handles expose ONNX only as a defensive copy. No camera/channel semantics implied. */
    public static final class CompiledModel {
        public final Style style;
        public final String inputName, outputName, modelSha256, graphSha256, weightsSha256;
        public final int operatorCount, weightCount;
        private final byte[] onnx;
        CompiledModel(Style style, String input, String output, int operators, Decoded decoded, byte[] onnx) {
            this.style=style; this.inputName=input; this.outputName=output;
            this.operatorCount=operators; this.weightCount=decoded.weights.length/4;
            this.modelSha256=style == Style.NATURAL_BLUSH ? BAOMAN : GOODLIKE;
            this.graphSha256=style == Style.NATURAL_BLUSH ? BAOMAN_GRAPH : GOODLIKE_GRAPH;
            this.weightsSha256=sha(decoded.weights); this.onnx=onnx;
        }
        public byte[] copyOnnx() { return onnx.clone(); }
        public long[] inputShape() { return new long[]{1,3,256,256}; }
        public long[] outputShape() { return new long[]{1,4,256,256}; }
    }
    static final class Decoded {
        final String graph; final byte[] weights;
        Decoded(String graph, byte[] weights) { this.graph=graph; this.weights=weights; }
    }
    static void require(boolean ok, String error) {
        if (!ok) throw new IllegalArgumentException(error);
    }
    static String sha(byte[] bytes) {
        try {
            byte[] hash=MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder s=new StringBuilder(64);
            for (byte b:hash) s.append(Character.forDigit((b&255)>>>4,16)).append(Character.forDigit(b&15,16));
            return s.toString();
        } catch (GeneralSecurityException e) { throw new AssertionError(e); }
    }
    static byte[] readPinned(InputStream input, String expected) throws IOException {
        ByteArrayOutputStream out=new ByteArrayOutputStream(); byte[] chunk=new byte[16384];
        int n;
        while ((n=input.read(chunk))!=-1) {
            if (n==0) { int value=input.read(); if(value==-1) break; require(out.size()<MAX_INPUT,"input too large"); out.write(value); }
            else { require(n<=MAX_INPUT-out.size(),"input too large"); out.write(chunk,0,n); }
        }
        byte[] bytes=out.toByteArray(); require(bytes.length>0 && sha(bytes).equals(expected),"unsupported input SHA-256");
        return bytes;
    }
    static void span(byte[] data,int start,int size) { require(start>=0 && size>=0 && start<=data.length && size<=data.length-start,"invalid span"); }
    static long u32(byte[] b,int p) { span(b,p,4); return ((long)b[p]&255)|(((long)b[p+1]&255)<<8)|(((long)b[p+2]&255)<<16)|(((long)b[p+3]&255)<<24); }
    static int length(byte[] b,int p) { long n=u32(b,p); require(n<=Integer.MAX_VALUE,"length overflow"); return (int)n; }
    static byte[] slice(byte[] b,int p,int n) { span(b,p,n); return Arrays.copyOfRange(b,p,p+n); }
    static String ascii(byte[] b,int p,int n) {
        span(b,p,n); for(int i=p;i<p+n;i++) require(b[i]>=32 && b[i]<127,"invalid ASCII name");
        return new String(b,p,n,StandardCharsets.US_ASCII);
    }
    static String name(byte[] b,int p) {
        span(b,p,256); int end=p; while(end<p+256 && b[end]!=0) end++;
        require(end>p && end<p+256,"name terminator"); return ascii(b,p,end-p);
    }
    private static Decoded baoman(byte[] b,byte[] library) {
        require(length(b,0)==b.length && length(b,36)==2,"Bach header");
        int groupStart=48; byte[] inner=null;
        for(int i=0;i<2;i++) {
            int size=length(b,40+4*i); span(b,groupStart,size); require(size>=292,"Bach group");
            int groupEnd=groupStart+size; String groupName=name(b,groupStart);
            if(b[4]!=0) { int xor=0; for(int j=groupStart+292;j<groupEnd;j++) xor^=b[j]&255; require(xor==(b[groupStart+256]&255),"Bach checksum"); }
            int count=length(b,groupStart+288); require(count<=64 && count*4<=size-292,"Bach count");
            int cursor=groupStart+292+count*4;
            for(int j=0;j<count;j++) {
                int entry=length(b,groupStart+292+j*4); require(entry>=296 && entry<=groupEnd-cursor,"Bach entry");
                String key=name(b,cursor); int type=length(b,cursor+288), n=length(b,cursor+292);
                require(entry==296+n,"Bach entry size");
                if(groupName.equals("inference_model") && key.equals("model_name")) {
                    require(inner==null && type==3,"duplicate/mismatched model"); inner=slice(b,cursor+296,n);
                }
                cursor+=entry;
            }
            require(cursor==groupEnd,"Bach trailing group bytes"); groupStart=groupEnd;
        }
        require(groupStart==b.length && inner!=null,"Bach payload missing");
        require(inner.length>=44 && inner[0]=='B' && inner[1]=='M' && inner[2]==0 && inner[3]==2,"BM variant");
        require(length(inner,4)==inner.length && length(inner,8)==3,"BM header");
        int nt=length(inner,12), pt=length(inner,16), nw=length(inner,20), pw=length(inner,24), ps=length(inner,32);
        require(nt>0 && nt<=1048576 && nw>=4 && nw%4==0 && pt==36 && pw==pt+nt && ps==pw+nw && ps+8==inner.length,"BM segments");
        byte[] table=slice(library,0x1c5d31,256); boolean[] seen=new boolean[256];
        for(byte v:table) { require(!seen[v&255],"BM substitution permutation"); seen[v&255]=true; }
        byte[] graph=new byte[nt];
        for(int j=0;j<nt;j++) graph[j]=table[(inner[pt+j]^inner[ps+j%8])&255];
        require(sha(graph).equals(BAOMAN_GRAPH) && graph[nt-1]==0,"BM graph SHA-256");
        String raw=new String(graph,0,nt-1,StandardCharsets.US_ASCII); StringBuilder normalized=new StringBuilder();
        for(String row:raw.split("\\r?\\n")) { if(row.length()==0) continue; require(row.endsWith("\\n"),"BM delimiter"); normalized.append(row,0,row.length()-2).append('\n'); }
        String text=normalized.toString(); require(text.startsWith("1 97 "),"BM graph count");
        long marker=Long.parseLong(text.substring(0,text.indexOf('\n')).split(" ")[2]);
        require(marker==u32(inner,pw+nw-4),"BM graph/weight marker");
        byte[] weights=slice(inner,pw,nw-4);
        require(sha(weights).equals("ba99e1be002e46c1f6109fa88bb6f732058c0501d3834ef700acdfda4e47d4b3"),"BM weights SHA-256");
        return new Decoded(text,weights);
    }
    private static final class Cursor {
        final byte[] b; int p;
        Cursor(byte[] b,int p) {this.b=b;this.p=p;}
        int word() {int n=length(b,p);p+=4;return n;}
        long uint() {long n=u32(b,p);p+=4;return n;}
        byte[] blob() {int n=word();byte[] out=slice(b,p,n);p+=n;return out;}
        String text() {int n=word();require(n>0 && n<=4096,"legacy name length");String s=ascii(b,p,n);p+=n;return s;}
    }
    private static Decoded goodlike(byte[] b,byte[] effect) throws GeneralSecurityException {
        require(length(b,16)==b.length,"legacy size");
        byte[] version=new byte[8]; for(int i=0;i<8;i++) version[i]=(byte)(b[i]+b[i+8]);
        require(Arrays.equals(version,new byte[]{'v','3',0,0,0,0,0,0}),"legacy version");
        Cursor c=new Cursor(b,20); require(c.text().equals("tt_goodlike_v1.0") && c.word()==2,"legacy model/groups");
        byte[] graph=null, weights=null;
        for(int i=0;i<2;i++) {
            require(c.text().equals(i==0?"v0":"face_point"),"legacy group order");
            byte[] auxiliary=c.blob();long checksum=c.uint();byte[] encoded=c.blob();c.uint();byte[] tail=c.blob();
            if(i==0) {
                byte[] loaderMaterial=loaderMaterial(effect),material=null;
                try { material=decrypt(auxiliary,loaderMaterial); require(material.length==32,"legacy material length");graph=decrypt(encoded,material); }
                finally {Arrays.fill(loaderMaterial,(byte)0);if(material!=null)Arrays.fill(material,(byte)0);}
                require(sha(graph).equals(GOODLIKE_GRAPH),"legacy graph SHA-256");
                long h=0x4e67c6a7L;for(byte v:graph)h=(h^((h<<5)+(h>>>2)+(v&255)))&0xffffffffL;
                require(h==checksum,"legacy graph checksum");
                require(tail.length==1000020 && u32(tail,tail.length-4)==1614766077L,"legacy weight marker");
                weights=slice(tail,0,tail.length-4);
                require(sha(weights).equals("98ac5b9c28d57d02831e2bcb771e2732f2eb6be77285a6dde875b7b4c636cda0"),"legacy weights SHA-256");
            }
        }
        for(int pass=0;pass<2;pass++) {int n=c.word();require(n<=64,"legacy parameter count");for(int j=0;j<n;j++){c.text();c.uint();}}
        require(c.p==b.length && graph!=null && weights!=null,"legacy trailing data");
        return new Decoded(new String(graph,StandardCharsets.US_ASCII),weights);
    }
    static byte[] decrypt(byte[] encoded,byte[] key) throws GeneralSecurityException {
        require(key.length==16 || key.length==24 || key.length==32,"AES material size");
        require(encoded.length>=20 && encoded.length<=1048596 && (encoded.length-4)%16==0,"AES block length");
        int n=length(encoded,encoded.length-4), padded=encoded.length-4;
        require(n>0 && n<=padded && padded-n<16,"AES plaintext length");
        Cipher cipher=Cipher.getInstance("AES/ECB/NoPadding");cipher.init(Cipher.DECRYPT_MODE,new SecretKeySpec(key,"AES"));
        byte[] full=cipher.doFinal(encoded,0,padded);try{return Arrays.copyOf(full,n);}finally{Arrays.fill(full,(byte)0);}
    }
    private static byte[] loaderMaterial(byte[] library) {
        Map<Integer,Integer> regs=new HashMap<>(),stack=new HashMap<>();regs.put(31,0);
        int[][] ranges={{0xc9f460,0xc9f468},{0xe48ee8,0xe48fb4},{0xc9f470,0xc9f48c},{0xc9f490,0xc9f4d0}};
        for(int[] range:ranges)for(int p=range[0];p<range[1];p+=4){
            int word=(int)u32(library,p);
            if((word&0xff800000)==0x52800000){int shift=(word>>>21)&3;require(shift<=1,"MOVZ shift");regs.put(word&31,((word>>>5)&0xffff)<<(16*shift));}
            else if((word&0xffc00000)==0xb9000000){int reg=word&31,offset=((word>>>10)&0xfff)*4;require(((word>>>5)&31)==31 && regs.containsKey(reg) && offset<=0x148 && offset%8==0,"STR argument");stack.put(offset,regs.get(reg));}
            else throw new IllegalArgumentException("unsupported loader immediate instruction");
        }
        byte[] args=new byte[49];int p=0;
        for(int i=1;i<8;i++){require(regs.containsKey(i),"missing loader register");int v=regs.get(i);require(v>0 && v<128,"loader register byte");args[p++]=(byte)v;}
        for(int i=0;i<0x150;i+=8){require(stack.containsKey(i),"missing loader stack slot");int v=stack.get(i);require(v>=0 && v<128 && (p==48?v==0:v!=0),"loader stack byte");args[p++]=(byte)v;}
        try{return Arrays.copyOf(args,32);}finally{Arrays.fill(args,(byte)0);}
    }
}

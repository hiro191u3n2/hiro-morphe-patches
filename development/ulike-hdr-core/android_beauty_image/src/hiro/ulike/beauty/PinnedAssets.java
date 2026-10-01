package hiro.ulike.beauty;

import hiro.ulike.model.PinnedModel;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.zip.*;

/** Reads caller-owned audited assets. No vendor image/template bytes are bundled. */
public final class PinnedAssets {
    private PinnedAssets() {}
    static void require(boolean value,String message) {if(!value)throw new IllegalArgumentException(message);}
    private static MessageDigest sha() {try{return MessageDigest.getInstance("SHA-256");}catch(NoSuchAlgorithmException e){throw new AssertionError(e);}}
    private static String hex(byte[] data){StringBuilder s=new StringBuilder();for(byte b:data)s.append(Character.forDigit((b&255)>>>4,16)).append(Character.forDigit(b&15,16));return s.toString();}
    private static byte[] exact(InputStream in,int length)throws IOException{
        byte[] bytes=new byte[length];int p=0;while(p<length){int n=in.read(bytes,p,length-p);if(n<0)throw new EOFException();if(n==0){int c=in.read();if(c<0)throw new EOFException();bytes[p++]=(byte)c;}else p+=n;}
        require(in.read()==-1,"trailing asset data");return bytes;
    }
    public static final class FaceTemplate {
        private final double[] points;
        private FaceTemplate(double[] p){points=p;}
        double[] copy(){return points.clone();}
    }
    public static final class NeuralMask {
        public final PinnedModel.Style style;
        public final boolean verticalFlip;
        private final byte[] red;
        private NeuralMask(PinnedModel.Style style,boolean flip,byte[] red){this.style=style;this.verticalFlip=flip;this.red=red;}
        double at(int x,int y){return (red[y*320+x]&255)/255.0;}
    }
    /** Streams the whole library hash, retaining only the 848-byte template. */
    public static FaceTemplate loadTemplate(File library)throws IOException{
        require(library!=null && library.isFile() && library.length()<=128L*1024*1024,"bounded library required");
        final int offset=0x116d7a0;byte[] selected=new byte[848],chunk=new byte[16384];int total=0;MessageDigest hash=sha();
        try(InputStream in=new FileInputStream(library)){int n;while((n=in.read(chunk))!=-1){if(n==0)continue;require(n<=128*1024*1024-total,"library too large");hash.update(chunk,0,n);int lo=Math.max(total,offset),hi=Math.min(total+n,offset+848);if(lo<hi)System.arraycopy(chunk,lo-total,selected,lo-offset,hi-lo);total+=n;}}
        require(total>=offset+848 && hex(hash.digest()).equals("d40af10b250b91cf8f30f4a265ac1d3b7b5b88a82bbf63da332c3f47a415d48e"),"library SHA-256");
        require(hex(sha().digest(selected)).equals("3c68979e89af5a444cc3902a8f232b29e9285a024ad696a1dc4ee0268ef54f12"),"template SHA-256");
        ByteBuffer b=ByteBuffer.wrap(selected).order(ByteOrder.LITTLE_ENDIAN);double[] out=new double[212];for(int i=0;i<out.length;i++){out[i]=b.getFloat();require(Double.isFinite(out[i]),"template finite");}return new FaceTemplate(out);
    }
    /** Orientation is mandatory and means decoded first PNG row -> crop top when false. */
    public static NeuralMask loadMask(File archive,PinnedModel.Style style,boolean verticalFlip)throws IOException{
        require(archive!=null && archive.isFile() && archive.length()<=32L*1024*1024 && style!=null,"bounded style ZIP required");
        boolean natural=style==PinnedModel.Style.NATURAL_BLUSH;
        String member="materials/016/AmazingFeature0/image/"+(natural?"fusion_mask.png":"mask.png");
        int size=natural?41691:37816;String expected=natural?"e438c9ec4336a387a0bba52982b4259fb9bff3cde60b7695b6efbc6451ef45c1":"e0d9a8ccce809ec53f3691b495f65fa59eda516844c0f8a77f9be80cfd035067";byte[] png;
        try(ZipFile zip=new ZipFile(archive)){Enumeration<? extends ZipEntry> entries=zip.entries();ZipEntry chosen=null;int count=0;while(entries.hasMoreElements()){ZipEntry e=entries.nextElement();require(++count<=10000,"ZIP entry budget");if(e.getName().equals(member)){require(chosen==null && e.getSize()==size,"duplicate or invalid mask member");chosen=e;}}require(chosen!=null,"mask absent");try(InputStream in=zip.getInputStream(chosen)){png=exact(in,size);}}
        require(hex(sha().digest(png)).equals(expected),"mask SHA-256");byte[] red=decodeRed(png,natural?3:4);
        if(verticalFlip){for(int y=0;y<160;y++)for(int x=0;x<320;x++){int a=y*320+x,b=(319-y)*320+x;byte v=red[a];red[a]=red[b];red[b]=v;}}
        return new NeuralMask(style,verticalFlip,red);
    }
    // Decode pinned 8-bit, noninterlaced truecolor PNG samples directly. ICC/gamma
    // conversion would change shader mask values, so no image colour API is used.
    private static byte[] decodeRed(byte[] png,int channels)throws IOException{
        DataInputStream in=new DataInputStream(new ByteArrayInputStream(png));require(in.readLong()==0x89504e470d0a1a0aL,"PNG signature");ByteArrayOutputStream compressed=new ByteArrayOutputStream();boolean header=false,end=false;
        while(in.available()>0){int n=in.readInt();require(n>=0 && n<=in.available()-8,"PNG chunk length");byte[] type=new byte[4];in.readFully(type);byte[] data=new byte[n];in.readFully(data);long check=Integer.toUnsignedLong(in.readInt());CRC32 crc=new CRC32();crc.update(type);crc.update(data);require(check==crc.getValue(),"PNG CRC");String key=new String(type,"US-ASCII");
            if(key.equals("IHDR")){require(!header && n==13,"PNG IHDR");DataInputStream h=new DataInputStream(new ByteArrayInputStream(data));require(h.readInt()==320 && h.readInt()==320 && h.readUnsignedByte()==8 && h.readUnsignedByte()==(channels==3?2:6) && h.readUnsignedByte()==0 && h.readUnsignedByte()==0 && h.readUnsignedByte()==0,"PNG layout");header=true;}
            else if(key.equals("IDAT")){require(header && !end,"PNG IDAT order");compressed.write(data);}
            else if(key.equals("IEND")){require(n==0 && header && in.available()==0,"PNG IEND");end=true;}
        }
        require(header && end,"PNG incomplete");int stride=320*channels;byte[] raw;try(InputStream z=new InflaterInputStream(new ByteArrayInputStream(compressed.toByteArray()))){raw=exact(z,(stride+1)*320);}
        byte[] red=new byte[320*320],previous=new byte[stride],row=new byte[stride];int p=0;
        for(int y=0;y<320;y++){int filter=raw[p++]&255;require(filter<=4,"PNG filter");for(int x=0;x<stride;x++){int a=x>=channels?row[x-channels]&255:0,b=previous[x]&255,c=x>=channels?previous[x-channels]&255:0,v=raw[p++]&255;
                int predict=filter==0?0:filter==1?a:filter==2?b:filter==3?(a+b)/2:paeth(a,b,c);row[x]=(byte)(v+predict);}
            for(int x=0;x<320;x++)red[y*320+x]=row[x*channels];byte[] temp=previous;previous=row;row=temp;}
        return red;
    }
    private static int paeth(int a,int b,int c){int p=a+b-c,pa=Math.abs(p-a),pb=Math.abs(p-b),pc=Math.abs(p-c);return pa<=pb && pa<=pc?a:pb<=pc?b:c;}
}

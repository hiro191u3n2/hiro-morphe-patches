package com.hiro.ulike.hdr.heif;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/** Independent local review adapter. Does not invoke Android MediaCodec. */
public final class HeifReviewBridge {
    private static Main10Heif.Crop crop(String[] args) {
        return args.length==10?new Main10Heif.Crop(Integer.parseInt(args[6]),Integer.parseInt(args[7]),Integer.parseInt(args[8]),Integer.parseInt(args[9])):null;
    }
    public static void main(String[] args) throws Exception {
        if(args.length<6)throw new IllegalArgumentException("mode input output width height full-range [left top width height]");
        byte[] source=Files.readAllBytes(Paths.get(args[1]));
        int width=Integer.parseInt(args[3]),height=Integer.parseInt(args[4]);
        boolean full=Boolean.parseBoolean(args[5]);
        if(args[0].equals("reject")) {
            try {Main10Heif.prepare(source,width,height,full,crop(args));}
            catch(IllegalArgumentException expected){System.out.println("REJECTED "+expected.getMessage());return;}
            throw new AssertionError("hostile stream accepted");
        }
        Main10Heif.Crop crop=crop(args);
        Main10Heif.Prepared prepared=Main10Heif.prepare(source,width,height,full,crop);
        ByteArrayOutputStream first=new ByteArrayOutputStream();prepared.writeTo(first);
        byte[] expected=first.toByteArray();
        if(expected.length!=prepared.fileBytes)throw new AssertionError("length metadata");
        Arrays.fill(source,(byte)17);
        ByteArrayOutputStream again=new ByteArrayOutputStream();prepared.writeTo(again);
        if(!Arrays.equals(expected,again.toByteArray()))throw new AssertionError("caller mutated saved bytes");
        Path target=Paths.get(args[2]);Files.write(target,new byte[]{10,11,12});
        prepared.saveAtomic(target);
        if(!Arrays.equals(expected,Files.readAllBytes(target)))throw new AssertionError("atomic replacement mismatch");
        IOException sentinel=new IOException("review sink failure");
        boolean[] closed={false},flushed={false};int[] count={0};
        OutputStream sink=new OutputStream(){
            public void write(int b)throws IOException{if(++count[0]>9)throw sentinel;}
            public void close(){closed[0]=true;}
            public void flush(){flushed[0]=true;}
        };
        try{prepared.writeTo(sink);throw new AssertionError("sink failure lost");}
        catch(IOException got){if(got!=sentinel)throw new AssertionError("sink error replaced");}
        if(closed[0]||flushed[0])throw new AssertionError("caller stream closed/flushed");
        Path dir=target.resolveSibling(target.getFileName()+".directory");Files.createDirectories(dir);
        try{prepared.saveAtomic(dir);throw new AssertionError("directory replaced");}
        catch(IOException expectedFailure){if(!Files.isDirectory(dir))throw new AssertionError("directory changed");}
        try{prepared.saveAtomic(target.resolve("invalid-child"));throw new AssertionError("non-directory parent accepted");}
        catch(IOException expectedFailure){if(!Arrays.equals(expected,Files.readAllBytes(target)))throw new AssertionError("old destination changed on failure");}
        try(java.util.stream.Stream<Path> children=Files.list(target.toAbsolutePath().getParent())){
            if(children.anyMatch(p->p.getFileName().toString().startsWith(".ulike-hlg-")))throw new AssertionError("temporary file left");
        }
        System.out.println("PASS width="+prepared.displayWidth+" height="+prepared.displayHeight+" bytes="+prepared.fileBytes);
    }
}

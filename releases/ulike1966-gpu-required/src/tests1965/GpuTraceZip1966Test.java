package com.hiro.ulike;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.*;

/** Actual .66 ZIP exporter with an explicit scalar GPU-log delivery fixture. */
public final class GpuTraceZip1966Test {
    static int assertions;
    static void check(boolean value,String reason){assertions++;if(!value)throw new AssertionError(reason);}
    static Map<String,byte[]> zip(CameraTrace1965.Storage storage)throws Exception {
        ByteArrayOutputStream destination=new ByteArrayOutputStream();storage.exportZip(destination);
        Map<String,byte[]> entries=new LinkedHashMap<String,byte[]>();
        try(ZipInputStream input=new ZipInputStream(new ByteArrayInputStream(destination.toByteArray()))){
            for(ZipEntry entry;(entry=input.getNextEntry())!=null;){ByteArrayOutputStream data=new ByteArrayOutputStream();byte[] buffer=new byte[4096];for(int count;(count=input.read(buffer))!=-1;)data.write(buffer,0,count);check(entries.put(entry.getName(),data.toByteArray())==null,"ZIP entries remain unique");}
        }return entries;
    }
    public static void main(String[] args)throws Exception {
        File directory=new File(args[0]);directory.mkdirs();CameraTrace1965.Storage storage=new CameraTrace1965.Storage(directory,"gpu-zip-focus");
        try {
            storage.append("{\"sequence\":1,\"phase\":\"camera_start\"}\n",true);File snapshot=storage.freeze("user","share_requested",1);byte[] frozen=Files.readAllBytes(snapshot.toPath());
            String expected=GpuRequired1965.text;Map<String,byte[]> ordinary=zip(storage);
            check(Arrays.equals(expected.getBytes(StandardCharsets.UTF_8),ordinary.get("gpu-required1965.txt")),"existing diagnostic ZIP contains exact bounded GPU scalar log");
            String manifest=new String(ordinary.get("manifest.json"),StandardCharsets.UTF_8);check(manifest.contains("\"version\":\"1.9.66\"")&&manifest.contains("\"gpu_log_max_bytes\":262144"),"ZIP metadata identifies .66 and explicit GPU cap");
            check(manifest.contains("\"includes_photos_or_frames\":false"),"diagnostics remain scalar metadata only");
            check(Arrays.equals(frozen,ordinary.get("protected/"+snapshot.getName())),"GPU export addition preserves exact frozen camera evidence");
            StringBuilder large=new StringBuilder();for(int i=0;i<100000;i++)large.append('\u65e5');large.append("\nGPU_LATEST_END\n");GpuRequired1965.text=large.toString();Map<String,byte[]> bounded=zip(storage);byte[] gpu=bounded.get("gpu-required1965.txt");
            check(gpu.length<=256*1024&&gpu.length>256*1024-4,"large GPU export has a hard 256KiB byte bound");
            String decoded=new String(gpu,StandardCharsets.UTF_8);check(Arrays.equals(gpu,decoded.getBytes(StandardCharsets.UTF_8)),"UTF-8 boundary stays valid after bounded export");check(decoded.endsWith("GPU_LATEST_END\n"),"bounded export retains latest diagnostic events");
            check(Arrays.equals(frozen,Files.readAllBytes(snapshot.toPath()))&&Arrays.equals(frozen,bounded.get("protected/"+snapshot.getName())),"large GPU log never mutates protected camera snapshot");
            GpuRequired1965.text="";check(zip(storage).containsKey("gpu-required1965.txt"),"empty GPU log has an explicit ZIP entry");
        }finally{storage.close();}
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"actualProductionZipExporter\":true,\"boundedGpuDiagnosticsIncluded\":true,\"frozenCameraSnapshotPreserved\":true,\"physicalAndroidTested\":false}");
    }
}

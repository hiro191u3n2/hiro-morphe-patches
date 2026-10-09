package com.hiro.ulike;
import android.app.*;
import android.content.*;
import android.os.*;
import android.preference.Preference;
import android.widget.Toast;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.zip.*;
/** Execute real trace storage/export/queue APIs; Android provider/UI boundaries
 * are controlled inherited fixtures. No physical Android coverage is claimed. */
public final class CameraExport1967Test {
    static int checks;
    interface Condition {boolean ok()throws Exception;}
    static void check(boolean yes,String name){checks++;if(!yes)throw new AssertionError(name);}
    static void until(Condition c,String label)throws Exception{long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);while(System.nanoTime()<end){if(c.ok())return;Thread.sleep(10);}throw new AssertionError("timeout: "+label);}
    static Object field(String name)throws Exception{Field f=CameraTrace1965.class.getDeclaredField(name);f.setAccessible(true);return f.get(null);}
    static String line(String marker,int n){return "{\"marker\":\""+marker+"\",\"seq\":"+n+",\"fields\":\""+repeat('x',160)+"\"}\n";}
    static String repeat(char c,int n){char[] a=new char[n];Arrays.fill(a,c);return new String(a);}
    static List<File> jsonFiles(File directory)throws Exception{List<File> out=new ArrayList<File>();if(directory.exists())try(java.util.stream.Stream<Path> paths=Files.walk(directory.toPath())){paths.filter(p->p.toString().endsWith(".jsonl")).forEach(p->out.add(p.toFile()));}Collections.sort(out,Comparator.comparing(File::getPath));return out;}
    static String text(File f)throws Exception{return new String(Files.readAllBytes(f.toPath()),StandardCharsets.UTF_8);}
    static String contents(File root)throws Exception{StringBuilder out=new StringBuilder();for(File f:jsonFiles(root))out.append(text(f));return out.toString();}
    static Map<String,byte[]> zip(byte[] archive)throws Exception{Map<String,byte[]> out=new LinkedHashMap<String,byte[]>();try(ZipInputStream z=new ZipInputStream(new ByteArrayInputStream(archive))){for(ZipEntry e;(e=z.getNextEntry())!=null;){ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] block=new byte[8192];for(int n;(n=z.read(block))!=-1;)b.write(block,0,n);check(out.put(e.getName(),b.toByteArray())==null,"archive paths unique");check(!e.getName().startsWith("/")&&!e.getName().contains(".."),"archive paths are safe and relative");}}return out;}
    static String zipText(Map<String,byte[]> entries){StringBuilder out=new StringBuilder();for(byte[] b:entries.values())out.append(new String(b,StandardCharsets.UTF_8));return out.toString();}
    static void ownedHold(File root)throws Exception{
        CameraTrace1965.Storage s=CameraTrace1965.Storage.openOwned1967(root,"cross-process-holder");
        try{for(int i=0;i<400;i++)s.append(line("cross-process-holder",i),false);s.flush(true);System.out.println("READY "+s.directory.getAbsolutePath());System.out.flush();System.in.read();}
        finally{s.close();}
    }
    static void ownership(File root)throws Exception{
        CameraTrace1965.Storage[] stores=new CameraTrace1965.Storage[3];
        try{
            Set<String> dirs=new HashSet<String>();
            for(int i=0;i<3;i++){stores[i]=CameraTrace1965.Storage.openOwned1967(root,"local-owner-"+i);check(dirs.add(stores[i].directory.getCanonicalPath()),"independent owners get separate directories");for(int n=0;n<400;n++)stores[i].append(line("local-owner-"+i,n),false);stores[i].flush(true);}
            long started=System.nanoTime();boolean refused=false;try{CameraTrace1965.Storage extra=CameraTrace1965.Storage.openOwned1967(root,"fifth-owner");if(extra!=null)extra.close();else refused=true;}catch(IOException expected){refused=true;}
            check(refused,"four active owners enforce fixed storage ownership cap including other JVM");
            check(System.nanoTime()-started<TimeUnit.SECONDS.toNanos(1),"busy owner slots cannot block camera writer admission");
            for(File f:jsonFiles(root)){check(f.length()<=128*1024,"isolated owner rolling files obey hard bound");check(text(f).endsWith("\n"),"isolated owner ends at complete record");}
            File reused=stores[0].directory;stores[0].close();stores[0]=CameraTrace1965.Storage.openOwned1967(root,"replacement-owner");
            check(reused.equals(stores[0].directory),"closed inactive owner slot can be reused");
            check(contents(reused).contains("local-owner-0"),"reused owner keeps earlier diagnostics");
            stores[0].append(line("replacement-owner",500),true);
            CameraTrace1965.Storage legacy=new CameraTrace1965.Storage(root,"legacy-owner");try{legacy.append(line("legacy-retained-evidence",1),true);}finally{legacy.close();}
            File other=jsonFiles(stores[1].directory).get(0);
            Files.write(other.toPath(),"{\"partial_other_owner\"".getBytes(StandardCharsets.UTF_8),StandardOpenOption.APPEND);
            byte[] before=Files.readAllBytes(other.toPath());
            ByteArrayOutputStream archive=new ByteArrayOutputStream();stores[0].exportZip(archive);
            Map<String,byte[]> entries=zip(archive.toByteArray());String all=zipText(entries);
            check(all.contains("cross-process-holder"),"archive includes live evidence from another JVM owner");
            check(all.contains("local-owner-1")&&all.contains("local-owner-2"),"archive includes other retained owners");
            check(all.contains("legacy-retained-evidence"),"archive includes previous version legacy root logs");
            check(all.contains("replacement-owner"),"archive includes current owner evidence");
            check(Arrays.equals(before,Files.readAllBytes(other.toPath())),"export never repairs or mutates another active owner's files");
            check(!all.contains("partial_other_owner"),"aggregate export drops incomplete foreign final record");
            for(Map.Entry<String,byte[]> e:entries.entrySet())if(e.getKey().endsWith(".jsonl")){check(e.getValue().length<= (e.getKey().contains("snapshot-")?256:128)*1024,"aggregate archive file remains bounded");check(e.getValue().length==0||e.getValue()[e.getValue().length-1]=='\n',"aggregate complete JSONL records only");}
            check(archive.size()<10*1024*1024,"aggregate export remains bounded across four owners and legacy");
        }finally{for(CameraTrace1965.Storage s:stores)if(s!=null)s.close();}
    }
    static void oversized(File root)throws Exception{
        File rolling=new File(root,"rolling");rolling.mkdirs();File file=new File(rolling,"segment-00000000000000000001.jsonl");
        File older=new File(rolling,"segment-00000000000000000000.jsonl");
        try(FileOutputStream output=new FileOutputStream(older)){for(int i=0;i<1500;i++)output.write(line("older-oversized-segment",i).getBytes(StandardCharsets.UTF_8));output.write(line("older-newest-complete-evidence",1501).getBytes(StandardCharsets.UTF_8));}
        try(FileOutputStream output=new FileOutputStream(file)){for(int i=0;i<1500;i++)output.write(line("older-history",i).getBytes(StandardCharsets.UTF_8));output.write(line("newest-complete-evidence",1501).getBytes(StandardCharsets.UTF_8));output.write("{\"cut_off\"".getBytes(StandardCharsets.UTF_8));}
        File protectedDir=new File(root,"protected");protectedDir.mkdirs();File snapshot=new File(protectedDir,"snapshot-00000000000000000001.jsonl");
        String snapshotHeader="{\"phase\":\"protected_snapshot\",\"fields\":\"kind=anomaly reason=intermittent_display\"}\n";
        try(FileOutputStream output=new FileOutputStream(snapshot)){output.write(snapshotHeader.getBytes(StandardCharsets.UTF_8));for(int i=0;i<2000;i++)output.write(line("protected-history",i).getBytes(StandardCharsets.UTF_8));output.write(line("protected-newest-complete",2001).getBytes(StandardCharsets.UTF_8));output.write("{\"protected_cut_off\"".getBytes(StandardCharsets.UTF_8));}
        long original=file.length();check(original>128*1024,"real previously oversized rolling file prepared");
        CameraTrace1965.Storage s=new CameraTrace1965.Storage(root,"recovered-owner");
        try{
            check(file.length()<=128*1024,"oversized rolling recovery enforces bound");
            check(older.length()<=128*1024&&text(older).contains("older-newest-complete-evidence"),"older retained oversized segment is recovered as well as current segment");
            check(text(file).contains("newest-complete-evidence"),"oversized recovery preserves newest complete evidence");
            check(!text(file).contains("cut_off"),"oversized recovery drops partial final record");
            check(snapshot.length()<=256*1024,"oversized protected snapshot stays bounded");
            check(text(snapshot).startsWith(snapshotHeader),"protected oversized recovery preserves original incident header");
            check(text(snapshot).contains("protected-newest-complete"),"protected oversized recovery preserves newest complete evidence");
            check(!text(snapshot).contains("protected_cut_off"),"protected oversized recovery drops incomplete tail");
            check(s.recoveredTailBytes>0,"oversized recovery reports dropped bytes");
            s.append(line("new-record-after-recovery",1600),true);ByteArrayOutputStream b=new ByteArrayOutputStream();s.exportZip(b);
            check(zipText(zip(b.toByteArray())).contains("new-record-after-recovery"),"recovered storage continues to append and export");
        }finally{s.close();}
    }
    static void menu(Activity app)throws Exception{AlertDialog.last=null;CameraTrace1965.open(new Preference(app));Handler.dispatchAll();until(()->AlertDialog.last!=null,"diagnostic menu");}
    static String exportStatus()throws Exception{
        try{Field f=CameraTrace1965.class.getDeclaredField("exportStatus1967");f.setAccessible(true);return String.valueOf(f.get(null));}
        catch(NoSuchFieldException missing){try{Method m=CameraTrace1965.class.getDeclaredMethod("exportStatus1967");m.setAccessible(true);return String.valueOf(m.invoke(null));}catch(NoSuchMethodException absent){return "";}}
    }
    static void directSave(Activity app)throws Exception{
        menu(app);check(AlertDialog.last.items!=null&&AlertDialog.last.items.length==3,"menu offers explicit snapshot share and direct ZIP save actions");
        check(AlertDialog.last.items[2].toString().contains("ZIP"),"direct save clearly names ZIP deliverable");
        check(AlertDialog.last.items[0].toString().contains("アプリ内"),"private snapshot action clearly identifies internal storage");
        int previous=app.resolver.updates;app.started=null;AlertDialog.last.clickItem(2);
        until(()->app.resolver.updates>previous&&exportStatus().contains("保存済み"),"direct ZIP pending publication");Handler.dispatchAll();
        check(app.started==null,"direct save does not launch a share chooser");
        check("application/zip".equals(app.resolver.insertedValues.get(app.resolver.insertedValues.size()-1).get("mime_type")),"direct export ZIP MIME metadata");
        check("Download/ULike".equals(app.resolver.insertedValues.get(app.resolver.insertedValues.size()-1).get("relative_path")),"direct export visible in Download/ULike");
        check(Integer.valueOf(0).equals(app.resolver.updatedValues.get(app.resolver.updatedValues.size()-1).get("is_pending")),"direct export clears pending after successful output");
        check(!app.resolver.mainIo&&!app.filesOnMain,"provider and disk work remains off the UI thread");
        byte[] bytes=app.resolver.data.get(app.resolver.last);check(bytes!=null&&bytes.length>0,"direct save produces real ZIP bytes");
        String all=zipText(zip(bytes));check(all.contains("1.9.67")&&all.contains("process_start"),"direct ZIP contains current version and usable evidence");
    }
    static void ui(File base)throws Exception{
        Looper.getMainLooper();base.mkdirs();Activity app=new Activity(base);
        menu(app);until(()->field("STORE")!=null,"worker initialized");directSave(app);
        ContentResolver r=app.resolver;
        for(int fault=0;fault<3;fault++){
            r.failOpen=fault==0;r.failWrite=fault==1;r.failPublish=fault==2;app.started=null;int deleted=r.deletes;
            menu(app);AlertDialog.last.clickItem(2);until(()->r.deletes>deleted,"pending entry cleanup on provider fault");Handler.dispatchAll();
            check(app.started==null,"provider failure never shares incomplete archive");
            check(((Thread)field("WRITER")).isAlive(),"writer survives provider failure");
            String stage=fault==0?"open":fault==1?"zip":"publish";
            check(exportStatus().contains("保存失敗")&&exportStatus().contains(stage),"provider failure reports its exact export stage");
            check(contents(((CameraTrace1965.Storage)field("STORE")).directory).contains("diagnostic_export_failed"),"provider failure persists a scalar diagnostic record");
            r.failOpen=r.failWrite=r.failPublish=false;directSave(app);
        }
        menu(app);int previous=r.updates;AlertDialog.last.clickItem(1);until(()->r.updates>previous,"share export publication");
        until(()->{Handler.dispatchAll();return app.started!=null;},"share chooser after publication");
        Intent share=app.started.target==null?app.started:app.started.target;
        check(Intent.ACTION_SEND.equals(share.action),"existing Android share remains available");
        check((share.flags&Intent.FLAG_GRANT_READ_URI_PERMISSION)!=0&&(share.flags&Intent.FLAG_GRANT_WRITE_URI_PERMISSION)==0,"share grants read access only");
    }
    static void initRetry(File base)throws Exception{
        Looper.getMainLooper();base.getParentFile().mkdirs();Files.write(base.toPath(),new byte[]{1});Activity app=new Activity(base);
        CameraTrace1965.init(app);until(()->app.filesDirCalls.get()>0&&field("WRITER")==null,"failed storage writer cleanup");
        check(field("STORE")==null,"failed writer releases store");
        Files.delete(base.toPath());base.mkdirs();menu(app);
        until(()->field("STORE")!=null&&field("WRITER")!=null,"public menu retries dead writer");
        check(((Thread)field("WRITER")).isAlive(),"retry creates live writer");
        directSave(app);
    }
    static int openRecordDescriptors(File root)throws Exception{
        int count=0;try(DirectoryStream<Path> fds=Files.newDirectoryStream(Paths.get("/proc/self/fd"))){for(Path fd:fds)try{String target=Files.readSymbolicLink(fd).toString();if(target.startsWith(root.getAbsolutePath())&&target.contains(".jsonl"))count++;}catch(IOException closedDuringScan){}}
        return count;
    }
    static void startupFault(File root)throws Exception{
        root.mkdirs();int before=openRecordDescriptors(root);
        Field fault=SystemClock.class.getDeclaredField("startupFault1967");fault.setBoolean(null,true);
        boolean failed=false;try{CameraTrace1965.Storage unexpected=CameraTrace1965.Storage.openOwned1967(root,"constructor-fault");unexpected.close();}catch(IllegalStateException expected){failed=true;}
        check(failed,"real constructor failure after raw stream open injected at first uptime call");
        check(openRecordDescriptors(root)==before,"constructor failure closes raw JSONL file descriptor before releasing ownership");
        CameraTrace1965.Storage retry=CameraTrace1965.Storage.openOwned1967(root,"constructor-retry");
        try{check(retry.directory.getName().equals("owner-0"),"constructor failure releases first owner slot for retry");retry.append(line("after-constructor-fault",1),true);ByteArrayOutputStream archive=new ByteArrayOutputStream();retry.exportZip(archive);check(zipText(zip(archive.toByteArray())).contains("after-constructor-fault"),"writer can append and export after constructor failure");}
        finally{retry.close();}
        check(openRecordDescriptors(root)==before,"successful retry closes its raw descriptor too");
    }
    public static void main(String[] args)throws Exception{
        File base=new File(args[1]);String mode=args[0];
        if("hold".equals(mode)){ownedHold(base);return;}
        if("ownership".equals(mode))ownership(base);
        else if("oversized".equals(mode))oversized(base);
        else if("ui".equals(mode))ui(base);
        else if("retry".equals(mode))initRetry(base);
        else if("startup".equals(mode))startupFault(base);
        else throw new IllegalArgumentException(mode);
        System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+checks+",\"mode\":\""+mode+"\",\"physical_android_tested\":false}");
    }
}


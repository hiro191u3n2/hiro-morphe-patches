package com.hiro.ulike;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;
import android.preference.Preference;
import android.provider.MediaStore;
import android.widget.Toast;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Bounded scalar lifecycle evidence. Camera hooks only enqueue small records;
 * files, rotation, snapshots and ZIP creation all run on the single writer. */
public final class CameraTrace1965 {
    public static final String VERSION="1.9.66";
    private static final Charset UTF8=Charset.forName("UTF-8");
    private static final Object LOCK=new Object();
    private static final int MAX_QUEUE=256;
    private static final long FLUSH_MS=500L;
    private static final ArrayDeque<Message> QUEUE=new ArrayDeque<Message>();
    private static final LinkedHashMap<String,Boolean> ANOMALIES=new LinkedHashMap<String,Boolean>();
    private static final String SESSION=Long.toHexString(System.currentTimeMillis())+"-"+UUID.randomUUID().toString().substring(0,8);
    private static Context APP;
    private static Thread WRITER;
    private static volatile Storage STORE;
    private static volatile boolean UNAVAILABLE;
    private static long sequence,dropped,totalDropped;
    private CameraTrace1965() {}

    private static final class Message {
        final int kind; // 0 event, 1 anomaly, 2 user snapshot, 3 share
        final long sequence,uptime,epoch;
        final String phase,fields;
        final boolean critical;
        final WeakReference<Activity> activity;
        Message(int kind,String phase,long epoch,String fields,Activity activity){
            this.kind=kind;this.sequence=++CameraTrace1965.sequence;
            uptime=SystemClock.uptimeMillis();this.epoch=epoch;
            this.phase=bounded(phase,64);this.fields=bounded(fields,160);
            critical=kind!=0||critical(this.phase);
            this.activity=activity==null?null:new WeakReference<Activity>(activity);
        }
        String line(){return record(sequence,uptime,epoch,phase,fields);}
    }
    public static void init(Context context) {
        if(context==null)return;
        try {
            Context application=context.getApplicationContext();
            synchronized(LOCK){
                if(APP==null)APP=application==null?context:application;
                if(WRITER!=null)return;
                Thread thread=new Thread(new Runnable(){public void run(){writeLoop();}},"ULike-camera-trace1965");
                thread.setDaemon(true);WRITER=thread;UNAVAILABLE=false;
                try{thread.start();}catch(Throwable unavailable){WRITER=null;UNAVAILABLE=true;totalDropped+=QUEUE.size();QUEUE.clear();}
            }
        }catch(Throwable optional){}
    }
    public static void event(String phase,long epoch,String fields) {
        try {enqueue(0,phase,epoch,fields,null);}catch(Throwable optional){}
    }
    public static void event(String phase,Object owner,long epoch,String fields) {
        event(phase,epoch,"owner="+(owner==null?0:System.identityHashCode(owner))+" "+bounded(fields,130));
    }
    public static void anomaly(String reason,long epoch,String fields) {
        try {
            String key=bounded(reason,64)+"@"+epoch;
            synchronized(LOCK){
                if(ANOMALIES.containsKey(key))return;
                ANOMALIES.put(key,Boolean.TRUE);
                while(ANOMALIES.size()>128)ANOMALIES.remove(ANOMALIES.keySet().iterator().next());
            }
            if(!enqueue(1,reason,epoch,fields,null))synchronized(LOCK){ANOMALIES.remove(key);}
        }catch(Throwable optional){}
    }
    private static boolean enqueue(int kind,String phase,long epoch,String fields,Activity activity) {
        synchronized(LOCK){
            Message message=new Message(kind,phase,epoch,fields,activity);
            if(QUEUE.size()>=MAX_QUEUE){
                // Keep pending snapshots/share actions; ordinary telemetry can
                // be discarded without waiting for disk or blocking a camera.
                Message remove=null;
                for(Message queued:QUEUE)if(queued.kind==0&&!queued.critical){remove=queued;break;}
                if(remove==null&&message.critical)for(Message queued:QUEUE)if(queued.kind==0){remove=queued;break;}
                if(remove!=null)QUEUE.remove(remove);
                else {dropped++;totalDropped++;LOCK.notifyAll();return false;}
                dropped++;totalDropped++;
            }
            QUEUE.addLast(message);LOCK.notifyAll();return true;
        }
    }
    private static boolean critical(String phase){
        return phase.contains("start")||phase.contains("stop")||phase.contains("open")||phase.contains("close")
            ||phase.contains("switch")||phase.contains("attach")||phase.contains("detach")||phase.contains("surface")
            ||phase.contains("error")||phase.contains("pause")||phase.contains("resume")||phase.contains("destroy")
            ||phase.contains("session")||phase.equals("first_frame");
    }
    private static void writeLoop(){
        Storage storage=null;
        try {
            // getFilesDir can create its directory; it also belongs on this worker.
            final Context context; synchronized(LOCK){context=APP;}
            storage=new Storage(new File(context.getFilesDir(),"camera-trace1965"),SESSION);STORE=storage;
            String boot="version="+VERSION+" api="+Build.VERSION.SDK_INT+" model="+bounded(Build.MODEL,40)
                +" pid="+Process.myPid()+" prior="+bounded(storage.priorSession,32)
                +" prior_close_missing="+storage.priorOpen+" repaired_tail="+storage.recoveredTailBytes;
            storage.append(record(0,SystemClock.uptimeMillis(),0,"process_start",boot),true);
            final Storage closing=storage;
            try{Runtime.getRuntime().addShutdownHook(new Thread(new Runnable(){public void run(){try{closing.close();}catch(Throwable ignored){}}},"ULike-camera-trace-close1965"));}catch(Throwable optional){}
            for(;;){
                Message message;long lost;
                synchronized(LOCK){
                    if(QUEUE.isEmpty())try{LOCK.wait(storage.nextFlushDelay());}catch(InterruptedException ignored){}
                    message=QUEUE.pollFirst();lost=dropped;dropped=0;
                }
                try {
                    if(lost>0){storage.append(record(0,SystemClock.uptimeMillis(),0,"queue_dropped","count="+lost+" total="+totalDropped),true);}
                    if(message==null){storage.flush(false);continue;}
                    storage.append(message.line(),message.critical);
                    if(message.kind==1)storage.freeze("anomaly",message.phase,message.epoch);
                    else if(message.kind==2){storage.freeze("user",message.phase,message.epoch);toast("不具合が起きた記録を保存しました。");}
                    else if(message.kind==3){
                        // This frozen incident survives future ordinary rotations.
                        storage.freeze("user","share_requested",message.epoch);
                        share(storage,message.activity==null?null:message.activity.get());
                    }
                    if(message.phase.equals("process_stop"))storage.markClean();
                }catch(Throwable optional){
                    synchronized(LOCK){totalDropped++;if(message!=null&&message.kind==1)ANOMALIES.remove(message.phase+"@"+message.epoch);}
                    if(message!=null&&message.kind>=2)toast("診断記録を保存できませんでした。");
                }
            }
        }catch(Throwable unavailable){
            boolean userPending=false;
            synchronized(LOCK){for(Message queued:QUEUE)if(queued.kind>=2)userPending=true;totalDropped+=QUEUE.size();QUEUE.clear();ANOMALIES.clear();UNAVAILABLE=true;}
            if(userPending)toast("診断記録を保存できませんでした。");
        }finally{
            // The writer remains the initialization owner through close/fsync.
            // A retry cannot open the same rolling files while this owner closes.
            if(storage!=null)try{storage.close();}catch(Throwable ignored){}
            synchronized(LOCK){
                if(STORE==storage)STORE=null;
                if(WRITER==Thread.currentThread())WRITER=null;
            }
        }
    }
    /** Opened only through the existing timing result row; no deleted setting returns. */
    public static void open(final Preference preference) {
        if(preference==null)return;
        try {
            final Context context=preference.getContext();init(context);
            final Activity activity=activity(context);
            new AlertDialog.Builder(context).setTitle("カメラの診断記録")
                .setItems(new CharSequence[]{"不具合が起きた記録を保存","診断記録を共有"},new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog,int which){
                        if(which==0)request(2,"user_snapshot",activity);
                        else if(which==1)request(3,"user_share",activity);
                    }
                }).setNegativeButton("閉じる",new DialogInterface.OnClickListener(){public void onClick(DialogInterface dialog,int which){
                    try{Object summary=Class.forName("com.hiro.ulike.ProcessingTiming1947").getMethod("summary").invoke(null);if(summary instanceof String)preference.setSummary((String)summary);}catch(Throwable optional){}
                }}).show();
        }catch(Throwable optional){toast("診断記録の画面を開けませんでした。");}
    }
    private static void request(int kind,String phase,Activity activity){
        synchronized(LOCK){if(WRITER==null||UNAVAILABLE){toast("診断記録を保存できませんでした。");return;}}
        if(!enqueue(kind,phase,0,"requested_from_timing_row",activity))toast("診断記録が混雑しています。少し待ってからお試しください。");
    }
    private static Activity activity(Context context){
        for(int n=0;n<8&&context!=null;n++){
            if(context instanceof Activity)return (Activity)context;
            if(!(context instanceof ContextWrapper))break;
            Context next=((ContextWrapper)context).getBaseContext();if(next==context)break;context=next;
        }
        return null;
    }
    private static void share(Storage storage,final Activity activity)throws IOException {
        final Context context; synchronized(LOCK){context=APP;}
        if(Build.VERSION.SDK_INT<29)throw new IOException("Downloads export requires Android 10");
        ContentResolver resolver=context.getContentResolver();Uri destination=null;boolean published=false;
        try {
            ContentValues values=new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME,"ULike_camera_trace_"+System.currentTimeMillis()+".zip");
            values.put(MediaStore.MediaColumns.MIME_TYPE,"application/zip");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH,"Download/ULike");
            values.put(MediaStore.MediaColumns.IS_PENDING,Integer.valueOf(1));
            destination=resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);
            if(destination==null)throw new IOException("Downloads entry unavailable");
            try(OutputStream output=resolver.openOutputStream(destination,"w")){
                if(output==null)throw new IOException("Downloads output unavailable");storage.exportZip(output);
            }
            ContentValues ready=new ContentValues();ready.put(MediaStore.MediaColumns.IS_PENDING,Integer.valueOf(0));
            if(resolver.update(destination,ready,null,null)<=0)throw new IOException("Downloads publication failed");
            published=true;final Uri shared=destination;
            if(!main(new Runnable(){public void run(){
                if(activity==null||activity.isFinishing()||activity.isDestroyed()){toast("診断ZIPをダウンロードのULikeフォルダに保存しました。");return;}
                try {
                    Intent intent=new Intent(Intent.ACTION_SEND).setType("application/zip").putExtra(Intent.EXTRA_STREAM,shared)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.setClipData(ClipData.newRawUri("ULike camera trace",shared));
                    activity.startActivity(Intent.createChooser(intent,"診断記録を共有"));
                }catch(Throwable optional){toast("診断ZIPをダウンロードのULikeフォルダに保存しました。");}
            }}))toast("診断ZIPをダウンロードのULikeフォルダに保存しました。");
        }finally{if(!published&&destination!=null)try{resolver.delete(destination,null,null);}catch(Throwable ignored){}}
    }
    private static boolean main(Runnable task){try{return new Handler(Looper.getMainLooper()).post(task);}catch(Throwable optional){return false;}}
    private static void toast(final String text){main(new Runnable(){public void run(){try{Context context; synchronized(LOCK){context=APP;}if(context!=null)Toast.makeText(context,text,Toast.LENGTH_SHORT).show();}catch(Throwable optional){}}});}
    private static String bounded(String text,int maximum){
        if(text==null)return "";int end=Math.min(text.length(),maximum);
        if(end>0&&end<text.length()&&Character.isHighSurrogate(text.charAt(end-1)))end--;
        return text.substring(0,end).replace('\r',' ').replace('\n',' ');
    }
    private static String quote(String text){
        StringBuilder escaped=new StringBuilder(text.length()+8);escaped.append('"');
        for(int i=0;i<text.length();i++){
            char c=text.charAt(i);
            if(c=='"'||c=='\\')escaped.append('\\').append(c);
            else if(c<32)escaped.append(String.format("\\u%04x",(int)c));
            else escaped.append(c);
        }
        return escaped.append('"').toString();
    }
    private static String record(long seq,long uptime,long epoch,String phase,String fields){
        return record(SESSION,seq,uptime,epoch,phase,fields);
    }
    private static String record(String session,long seq,long uptime,long epoch,String phase,String fields){
        return "{\"session\":"+quote(session)+",\"seq\":"+seq+",\"uptime_ms\":"+uptime+",\"epoch\":"+epoch
            +",\"phase\":"+quote(bounded(phase,64))+",\"fields\":"+quote(bounded(fields,160))+"}\n";
    }

    /** Only this worker-owned store touches files. Package scope permits actual
     * production retention/recovery/export tests without replacing its logic. */
    static final class Storage {
        static final int ROLL_COUNT=8,ROLL_BYTES=128*1024,SNAPSHOT_COUNT=3,SNAPSHOT_BYTES=256*1024;
        final File directory,rolling,protectedDirectory;
        final String session,priorSession;
        final boolean priorOpen;
        final long recoveredTailBytes;
        private long segmentId,snapshotId,currentBytes,lastFlush;
        private File current;
        private FileOutputStream raw;
        private BufferedOutputStream output;
        private boolean closed;
        private final ArrayDeque<Incident> incidents=new ArrayDeque<Incident>();
        private static final long POST_TRIGGER_MS=5000L;
        private static final class Incident {
            final File file;final long deadline;long bytes;
            Incident(File file,long now){this.file=file;deadline=now+POST_TRIGGER_MS;bytes=file.length();}
        }
        Storage(File directory,String session)throws IOException {
            this.directory=directory;this.session=bounded(session,64);
            rolling=new File(directory,"rolling");protectedDirectory=new File(directory,"protected");
            mkdir(directory);mkdir(rolling);mkdir(protectedDirectory);
            removePending(protectedDirectory);
            String previous=readText(new File(directory,"session-state.txt"),512);
            String[] marker=previous.split("\\n");priorSession=marker.length>0?bounded(marker[0],64):"";
            priorOpen=marker.length>1&&marker[1].equals("open");
            File[] segments=files(rolling,"segment-");long repaired=0;
            if(segments.length>0){
                current=segments[segments.length-1];segmentId=id(current,"segment-");
                repaired+=repairTail(current,ROLL_BYTES);
                currentBytes=current.length();
            }
            File[] snapshots=files(protectedDirectory,"snapshot-");
            for(File snapshot:snapshots)repaired+=repairTail(snapshot,SNAPSHOT_BYTES);
            if(snapshots.length>0)snapshotId=id(snapshots[snapshots.length-1],"snapshot-");
            recoveredTailBytes=repaired;trim(rolling,"segment-",ROLL_COUNT);trim(protectedDirectory,"snapshot-",SNAPSHOT_COUNT);
            marker("open");openSegment();
        }
        private static long repairTail(File file,int maximum)throws IOException{
            byte[] bytes=read(file,maximum);int valid=bytes.length;
            while(valid>0&&bytes[valid-1]!='\n')valid--;
            if(valid==bytes.length)return 0;
            try(RandomAccessFile repair=new RandomAccessFile(file,"rw")){repair.setLength(valid);repair.getFD().sync();}
            return bytes.length-valid;
        }
        private static void mkdir(File directory)throws IOException{if(!directory.isDirectory()&&!directory.mkdirs()&&!directory.isDirectory())throw new IOException("trace directory unavailable");}
        private static long id(File file,String prefix){try{return Long.parseLong(file.getName().substring(prefix.length(),file.getName().length()-6));}catch(RuntimeException ignored){return 0;}}
        private static File[] files(File directory,String prefix){
            File[] found=directory.listFiles();if(found==null)return new File[0];List<File> accepted=new ArrayList<File>();
            for(File file:found)if(file.isFile()&&file.getName().matches(prefix+"[0-9]{20}\\.jsonl"))accepted.add(file);
            File[] result=accepted.toArray(new File[accepted.size()]);Arrays.sort(result,new Comparator<File>(){public int compare(File a,File b){return a.getName().compareTo(b.getName());}});return result;
        }
        private static void removePending(File directory){
            File[] found=directory.listFiles();if(found==null)return;
            for(File file:found)if(file.isFile()&&file.getName().matches("snapshot-[0-9]{20}\\.jsonl\\.pending"))file.delete();
        }
        private static void trim(File directory,String prefix,int count)throws IOException{
            File[] found=files(directory,prefix);for(int n=0;n<found.length-count;n++)if(!found[n].delete()&&found[n].exists())throw new IOException("trace rotation unavailable");
        }
        private void openSegment()throws IOException{
            if(current==null||currentBytes>=ROLL_BYTES){current=new File(rolling,String.format(Locale.ROOT,"segment-%020d.jsonl",++segmentId));currentBytes=0;}
            raw=new FileOutputStream(current,true);output=new BufferedOutputStream(raw,8192);lastFlush=SystemClock.uptimeMillis();trim(rolling,"segment-",ROLL_COUNT);
        }
        synchronized void append(String jsonLine,boolean sync)throws IOException{
            if(closed)throw new IOException("trace closed");byte[] bytes=jsonLine.getBytes(UTF8);
            if(bytes.length>ROLL_BYTES)throw new IOException("trace record too large");
            if(currentBytes+bytes.length>ROLL_BYTES){flush(true);output.close();output=null;raw=null;current=null;currentBytes=0;openSegment();}
            output.write(bytes);currentBytes+=bytes.length;appendPostTrigger(bytes,sync);if(sync)flush(true);else flush(false);
        }
        private void appendPostTrigger(byte[] bytes,boolean sync)throws IOException{
            long now=SystemClock.uptimeMillis();
            for(java.util.Iterator<Incident> it=incidents.iterator();it.hasNext();){
                Incident incident=it.next();
                if(now>incident.deadline||!incident.file.exists()){it.remove();continue;}
                if(incident.bytes+bytes.length>SNAPSHOT_BYTES)continue;
                try{
                    try(FileOutputStream target=new FileOutputStream(incident.file,true)){target.write(bytes);if(sync)target.getFD().sync();}
                    incident.bytes+=bytes.length;
                }catch(IOException failed){
                    it.remove(); // Never append after a possibly partial record.
                    try{repairTail(incident.file,SNAPSHOT_BYTES);}catch(IOException ignored){}
                    throw failed;
                }
            }
        }
        synchronized long nextFlushDelay(){return Math.max(1L,FLUSH_MS-(SystemClock.uptimeMillis()-lastFlush));}
        synchronized void flush(boolean sync)throws IOException{
            if(output==null)return;long now=SystemClock.uptimeMillis();
            if(sync||now-lastFlush>=FLUSH_MS){output.flush();if(sync)raw.getFD().sync();lastFlush=now;}
        }
        synchronized File freeze(String kind,String reason,long epoch)throws IOException{
            flush(true);
            byte[] header=record(session,0,SystemClock.uptimeMillis(),epoch,"protected_snapshot","kind="+bounded(kind,16)+" reason="+bounded(reason,64)).getBytes(UTF8);
            boolean anomaly="anomaly".equals(kind);
            // Persist pre-trigger evidence immediately, leaving a separately
            // bounded half for five seconds of follow-up lifecycle events.
            int remaining=(anomaly?SNAPSHOT_BYTES/2:SNAPSHOT_BYTES)-header.length;ArrayDeque<byte[]> portions=new ArrayDeque<byte[]>();
            File[] found=files(rolling,"segment-");
            for(int n=found.length-1;n>=0&&remaining>0;n--){
                byte[] bytes=read(found[n],ROLL_BYTES);int start=Math.max(0,bytes.length-remaining);
                if(start>0)while(start<bytes.length&&bytes[start-1]!='\n')start++;
                byte[] portion=Arrays.copyOfRange(bytes,start,bytes.length);portions.addFirst(portion);remaining-=portion.length;
            }
            File destination=new File(protectedDirectory,String.format(Locale.ROOT,"snapshot-%020d.jsonl",++snapshotId));
            File temporary=new File(protectedDirectory,destination.getName()+".pending");
            boolean committed=false;
            try{
                try(FileOutputStream target=new FileOutputStream(temporary)){target.write(header);for(byte[] portion:portions)target.write(portion);target.getFD().sync();}
                if(!temporary.renameTo(destination))throw new IOException("protected snapshot commit unavailable");
                committed=true;trim(protectedDirectory,"snapshot-",SNAPSHOT_COUNT);
                if(anomaly){while(incidents.size()>=SNAPSHOT_COUNT)incidents.removeFirst();incidents.addLast(new Incident(destination,SystemClock.uptimeMillis()));}
                return destination;
            }finally{if(!committed)temporary.delete();}
        }
        synchronized void exportZip(OutputStream destination)throws IOException{
            flush(true);
            try(ZipOutputStream zip=new ZipOutputStream(destination)){
                String manifest="{\"version\":"+quote(VERSION)+",\"session\":"+quote(session)+",\"prior_session\":"+quote(priorSession)
                    +",\"prior_session_close_not_recorded\":"+priorOpen+",\"recovered_tail_bytes\":"+recoveredTailBytes
                    +",\"dropped_records\":"+totalDropped+",\"rolling_files\":8,\"rolling_bytes_per_file\":131072,\"protected_files\":3,\"protected_bytes_per_file\":262144,\"anomaly_followup_window_ms\":5000,\"gpu_log_max_bytes\":262144,\"includes_photos_or_frames\":false}\n";
                zip.putNextEntry(new ZipEntry("manifest.json"));zip.write(manifest.getBytes(UTF8));zip.closeEntry();
                zip.putNextEntry(new ZipEntry("README.txt"));zip.write(("Camera lifecycle and GPU-stage scalar diagnostics only. No photos, video or frame pixels.\nA prior session without a close record does not by itself prove a crash.\nRoutine rolling log rotation never replaces protected incident snapshots.\n").getBytes(UTF8));zip.closeEntry();
                add(zip,rolling,"segment-","rolling/");add(zip,protectedDirectory,"snapshot-","protected/");
                byte[] gpu=GpuRequired1965.diagnosticText1965().getBytes(UTF8);
                int start=Math.max(0,gpu.length-256*1024);
                // Retain the newest bounded diagnostics without splitting UTF-8.
                while(start<gpu.length&&(gpu[start]&0xc0)==0x80)start++;
                zip.putNextEntry(new ZipEntry("gpu-required1965.txt"));zip.write(gpu,start,gpu.length-start);zip.closeEntry();
            }
        }
        private static void add(ZipOutputStream zip,File directory,String prefix,String path)throws IOException{
            for(File file:files(directory,prefix)){zip.putNextEntry(new ZipEntry(path+file.getName()));byte[] bytes=read(file,prefix.equals("segment-")?ROLL_BYTES:SNAPSHOT_BYTES);
                int valid=bytes.length;while(valid>0&&bytes[valid-1]!='\n')valid--;zip.write(bytes,0,valid);zip.closeEntry();}
        }
        private void marker(String state)throws IOException{
            File destination=new File(directory,"session-state.txt"),temporary=new File(directory,"session-state.pending");
            try(FileOutputStream marker=new FileOutputStream(temporary)){marker.write((session+"\n"+state+"\n").getBytes(UTF8));marker.getFD().sync();}
            if(!temporary.renameTo(destination)){temporary.delete();throw new IOException("session marker unavailable");}
        }
        synchronized void markClean()throws IOException{marker("closed");}
        synchronized void close()throws IOException{
            if(closed)return;closed=true;
            try{flush(true);}finally{try{if(output!=null)output.close();}finally{output=null;raw=null;marker("closed");}}
        }
        private static String readText(File file,int maximum)throws IOException{return file.exists()?new String(read(file,maximum),UTF8):"";}
        private static byte[] read(File file,int maximum)throws IOException{
            if(file.length()>maximum)throw new IOException("trace retained size exceeded");
            try(FileInputStream input=new FileInputStream(file);ByteArrayOutputStream bytes=new ByteArrayOutputStream()){
                byte[] buffer=new byte[4096];for(int count;(count=input.read(buffer))>=0;){bytes.write(buffer,0,count);if(bytes.size()>maximum)throw new IOException("trace retained size exceeded");}return bytes.toByteArray();
            }
        }
    }
}

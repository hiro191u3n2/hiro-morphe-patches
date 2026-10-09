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
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
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
    public static final String VERSION="1.9.72";
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
    private static volatile String EXPORT_STATUS="診断ZIP: 未保存";
    private static long sequence,dropped,totalDropped;
    private CameraTrace1965() {}

    private static final class Message {
        final int kind; // 0 event, 1 anomaly, 2 private snapshot, 3 share, 4 Downloads ZIP
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
            storage=Storage.openOwned1967(new File(context.getFilesDir(),"camera-trace1965"),SESSION);STORE=storage;
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
                    else if(message.kind==2){storage.freeze("user",message.phase,message.epoch);toast("不具合時点をアプリ内に記録しました。診断ZIPの保存・共有で取り出せます。");}
                    else if(message.kind==3||message.kind==4){
                        EXPORT_STATUS="診断ZIP: 保存中 / snapshot";
                        // This frozen incident survives future ordinary rotations.
                        storage.freeze("user",message.kind==3?"share_requested":"downloads_requested",message.epoch);
                        export(storage,message.activity==null?null:message.activity.get(),message.kind==3);
                    }
                    if(message.phase.equals("process_stop"))storage.markClean();
                }catch(Throwable optional){
                    synchronized(LOCK){totalDropped++;if(message!=null&&message.kind==1)ANOMALIES.remove(message.phase+"@"+message.epoch);}
                    if(message!=null&&message.kind>=2){
                        String reason=bounded(optional.getClass().getSimpleName()+": "+optional.getMessage(),110);
                        if(message.kind==2)EXPORT_STATUS="診断ZIP: 記録失敗 / "+reason;
                        else if(!EXPORT_STATUS.startsWith("診断ZIP: 保存失敗"))EXPORT_STATUS="診断ZIP: 保存失敗 / snapshot / "+bounded(optional.getClass().getSimpleName(),32);
                        try{storage.append(record(0,SystemClock.uptimeMillis(),0,"diagnostic_action_failed","action="+message.kind+" reason="+reason),true);}catch(Throwable ignored){}
                        toast((message.kind>=3?EXPORT_STATUS:"診断記録を保存できませんでした（"+bounded(optional.getClass().getSimpleName(),32)+"）")+"。もう一度保存をお試しください。");
                    }
                }
            }
        }catch(Throwable unavailable){
            EXPORT_STATUS="診断記録: 初期化失敗 / "+bounded(unavailable.getClass().getSimpleName()+": "+unavailable.getMessage(),96);
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
                .setItems(new CharSequence[]{"直前の不具合を記録（アプリ内）","診断記録を共有","診断ZIPを保存（ダウンロード/ULike）","処理時間・GPU使用状況を表示"},new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog,int which){
                        if(which==0)request(2,"user_snapshot",activity);
                        else if(which==1)request(3,"user_share",activity);
                        else if(which==2)request(4,"user_download",activity);
                        else if(which==3)details1969(context);
                    }
                }).setNegativeButton("閉じる",new DialogInterface.OnClickListener(){public void onClick(DialogInterface dialog,int which){
                    try{Object summary=Class.forName("com.hiro.ulike.ProcessingTiming1947").getMethod("summary").invoke(null);if(summary instanceof String)preference.setSummary((String)summary);}catch(Throwable optional){}
                }}).show();
        }catch(Throwable optional){toast("診断記録の画面を開けませんでした。");}
    }
    static void details1969(Context context){
        try{
            Object summary=Class.forName("com.hiro.ulike.ProcessingTiming1947").getMethod("summary").invoke(null);
            if(!(summary instanceof String)){toast("処理時間・GPU使用状況を表示できませんでした。");return;}
            new AlertDialog.Builder(context).setTitle("撮影の処理時間・GPU使用状況")
                .setMessage((String)summary+"\n\nGPU/CPUの区間数は、この写真の強ノイズ処理だけの記録です。未観測はGPU使用の有無を判定できていない状態です。")
                .setNegativeButton("閉じる",null)
                .show();
        }catch(Throwable optional){toast("処理時間・GPU使用状況を表示できませんでした。");}
    }
    public static String exportStatus1967(){return EXPORT_STATUS;}
    private static void request(int kind,String phase,Activity activity){
        // A prior initialization failure may have finished its cleanup since
        // this menu opened. Retry the writer without requiring another screen.
        final Context context;synchronized(LOCK){context=APP;}init(context);
        synchronized(LOCK){if(WRITER==null||UNAVAILABLE){toast("診断記録を保存できませんでした。");return;}}
        if(kind>=3)EXPORT_STATUS="診断ZIP: 保存待ち";
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
    // Keep the reviewed share entry for callers/tests; direct save owns exactly
    // the same pending/commit/delete transaction and suppresses only the chooser.
    private static void share(Storage storage,final Activity activity)throws IOException {export(storage,activity,true);}
    private static void export(Storage storage,final Activity activity,final boolean chooser)throws IOException {
        final Context context; synchronized(LOCK){context=APP;}
        if(Build.VERSION.SDK_INT<29){EXPORT_STATUS="診断ZIP: 保存失敗 / unsupported_api";throw new IOException("Downloads export requires Android 10");}
        ContentResolver resolver=null;Uri destination=null;boolean published=false;
        final String filename="ULike_camera_trace_"+System.currentTimeMillis()+"_"+Process.myPid()+".zip";
        String stage="record";EXPORT_STATUS="診断ZIP: 保存中";
        try {
            storage.append(record(0,SystemClock.uptimeMillis(),0,"diagnostic_export_start","action="+(chooser?"share":"download")),true);
            stage="resolver";resolver=context.getContentResolver();stage="insert";
            ContentValues values=new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME,filename);
            values.put(MediaStore.MediaColumns.MIME_TYPE,"application/zip");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH,"Download/ULike");
            values.put(MediaStore.MediaColumns.IS_PENDING,Integer.valueOf(1));
            destination=resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);
            if(destination==null)throw new IOException("Downloads entry unavailable");
            stage="open";try(OutputStream output=resolver.openOutputStream(destination,"w")){
                if(output==null)throw new IOException("Downloads output unavailable");stage="zip";storage.exportZip(output);
            }
            stage="publish";ContentValues ready=new ContentValues();ready.put(MediaStore.MediaColumns.IS_PENDING,Integer.valueOf(0));
            if(resolver.update(destination,ready,null,null)<=0)throw new IOException("Downloads publication failed");
            published=true;final Uri shared=destination;
            EXPORT_STATUS="診断ZIP: 保存済み / Download/ULike/"+filename;
            try{storage.append(record(0,SystemClock.uptimeMillis(),0,"diagnostic_export_complete","action="+(chooser?"share":"download")+" stage=published"),true);}catch(Throwable ignored){}
            if(!chooser){toast("Download/ULike/"+filename+" に保存しました。");return;}
            if(!main(new Runnable(){public void run(){
                if(activity==null||activity.isFinishing()||activity.isDestroyed()){toast("診断ZIPをダウンロードのULikeフォルダに保存しました。");return;}
                try {
                    Intent intent=new Intent(Intent.ACTION_SEND).setType("application/zip").putExtra(Intent.EXTRA_STREAM,shared)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.setClipData(ClipData.newRawUri("ULike camera trace",shared));
                    activity.startActivity(Intent.createChooser(intent,"診断記録を共有"));
                }catch(Throwable optional){toast("診断ZIPをダウンロードのULikeフォルダに保存しました。");}
            }}))toast("診断ZIPをダウンロードのULikeフォルダに保存しました。");
        }catch(IOException failure){
            EXPORT_STATUS="診断ZIP: 保存失敗 / "+stage+" / "+bounded(failure.getClass().getSimpleName(),32);
            try{storage.append(record(0,SystemClock.uptimeMillis(),0,"diagnostic_export_failed","stage="+stage+" reason="+bounded(failure.getClass().getSimpleName()+": "+failure.getMessage(),100)),true);}catch(Throwable ignored){}
            throw failure;
        }catch(RuntimeException failure){
            EXPORT_STATUS="診断ZIP: 保存失敗 / "+stage+" / "+bounded(failure.getClass().getSimpleName(),32);
            try{storage.append(record(0,SystemClock.uptimeMillis(),0,"diagnostic_export_failed","stage="+stage+" reason="+bounded(failure.getClass().getSimpleName()+": "+failure.getMessage(),100)),true);}catch(Throwable ignored){}
            throw failure;
        }finally{if(!published&&destination!=null&&resolver!=null)try{resolver.delete(destination,null,null);}catch(Throwable ignored){}}
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
        static final int ROLL_COUNT=8,ROLL_BYTES=128*1024,SNAPSHOT_COUNT=3,SNAPSHOT_BYTES=256*1024,OWNER_COUNT=4;
        final File directory,rolling,protectedDirectory;
        final String session,priorSession;
        final boolean priorOpen;
        final long recoveredTailBytes;
        private long segmentId,snapshotId,currentBytes,lastFlush;
        private File current;
        private FileOutputStream raw;
        private BufferedOutputStream output;
        private boolean closed;
        private File aggregateRoot;
        private RandomAccessFile ownerFile;
        private FileLock ownerLock;
        private final ArrayDeque<Incident> incidents=new ArrayDeque<Incident>();
        private static final long POST_TRIGGER_MS=5000L;
        private static final class Incident {
            final File file;final long deadline;long bytes;
            Incident(File file,long now){this.file=file;deadline=now+POST_TRIGGER_MS;bytes=file.length();}
        }
        /** Four fixed stores cap disk retention across processes. A lifetime
         * OS file lock prevents independent processes from rotating/repairing
         * the same files. All acquisition/recovery occurs on the log writer. */
        static Storage openOwned1967(File root,String session)throws IOException {
            mkdir(root);
            for(int slot=0;slot<OWNER_COUNT;slot++){
                RandomAccessFile claim=null;FileLock lock=null;boolean transferred=false;
                try{
                    claim=new RandomAccessFile(new File(root,"owner-"+slot+".lock"),"rw");
                    try{lock=claim.getChannel().tryLock();}catch(OverlappingFileLockException busy){continue;}
                    if(lock==null)continue;
                    Storage made=new Storage(new File(root,"owner-"+slot),session);
                    made.aggregateRoot=root;made.ownerFile=claim;made.ownerLock=lock;transferred=true;return made;
                }finally{if(!transferred){try{if(lock!=null)lock.release();}finally{if(claim!=null)claim.close();}}}
            }
            throw new IOException("trace writer capacity exhausted");
        }
        Storage(File directory,String session)throws IOException {
            this.directory=directory;this.session=bounded(session,64);
            rolling=new File(directory,"rolling");protectedDirectory=new File(directory,"protected");
            try{
                mkdir(directory);mkdir(rolling);mkdir(protectedDirectory);
                removePending(protectedDirectory);
                String previous=readText(new File(directory,"session-state.txt"),512);
                String[] marker=previous.split("\\n");priorSession=marker.length>0?bounded(marker[0],64):"";
                priorOpen=marker.length>1&&marker[1].equals("open");
                // Obsolete extra files are removed before bounded recovery.
                // Every retained rolling segment may have been enlarged by
                // competing old writers; freeze() will read all of them.
                trim(rolling,"segment-",ROLL_COUNT);trim(protectedDirectory,"snapshot-",SNAPSHOT_COUNT);
                File[] segments=files(rolling,"segment-");long repaired=0;
                for(File segment:segments)repaired+=repairTail(segment,ROLL_BYTES);
                if(segments.length>0){
                    current=segments[segments.length-1];segmentId=id(current,"segment-");currentBytes=current.length();
                }
                File[] snapshots=files(protectedDirectory,"snapshot-");
                for(File snapshot:snapshots)repaired+=repairTail(snapshot,SNAPSHOT_BYTES);
                if(snapshots.length>0)snapshotId=id(snapshots[snapshots.length-1],"snapshot-");
                recoveredTailBytes=repaired;marker("open");openSegment();
            }catch(IOException failure){closeStartup1967();throw failure;}
             catch(RuntimeException failure){closeStartup1967();throw failure;}
             catch(Error failure){closeStartup1967();throw failure;}
        }
        private void closeStartup1967(){
            // An initialization failure after opening the stream must release
            // it before openOwned1967 releases the process ownership lock.
            try{if(output!=null)output.close();else if(raw!=null)raw.close();}catch(Throwable ignored){}
            finally{output=null;raw=null;}
        }
        private static long repairTail(File file,int maximum)throws IOException{
            long original=file.length();byte[] valid=file.getName().startsWith("snapshot-")?readProtectedTail1967(file,maximum):readTail1967(file,maximum);
            if(original==valid.length)return 0;
            // Recover the newest complete bounded records even if earlier
            // competing writers made an old retained file exceed its limit.
            try(RandomAccessFile repair=new RandomAccessFile(file,"rw")){
                repair.seek(0);repair.write(valid);repair.setLength(valid.length);repair.getFD().sync();
            }
            return Math.max(0L,original-valid.length);
        }
        private static byte[] readProtectedTail1967(File file,int maximum)throws IOException{
            if(file.length()<=maximum)return readTail1967(file,maximum);
            byte[] header=null;
            try(RandomAccessFile input=new RandomAccessFile(file,"r")){
                byte[] first=new byte[(int)Math.min(input.length(),1024L)];input.readFully(first);
                for(int n=0;n<first.length;n++)if(first[n]=='\n'){
                    String line=new String(first,0,n+1,UTF8);
                    if(line.contains("\"phase\":\"protected_snapshot\""))header=Arrays.copyOf(first,n+1);
                    break;
                }
            }
            if(header==null||header.length>=maximum)return readTail1967(file,maximum);
            byte[] tail=readTail1967(file,maximum-header.length),result=new byte[header.length+tail.length];
            System.arraycopy(header,0,result,0,header.length);System.arraycopy(tail,0,result,header.length,tail.length);return result;
        }
        private static byte[] readTail1967(File file,int maximum)throws IOException{
            try(RandomAccessFile input=new RandomAccessFile(file,"r")){
                long length=input.length(),start=Math.max(0L,length-maximum);
                byte[] bytes=new byte[(int)(length-start)];input.seek(start);input.readFully(bytes);
                int first=0,last=bytes.length;
                if(start>0)while(first<last&&bytes[first++]!='\n'){}
                while(last>first&&bytes[last-1]!='\n')last--;
                return first==0&&last==bytes.length?bytes:Arrays.copyOfRange(bytes,first,last);
            }
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
        private static final class ExportEntry1967 {
            final String path;final byte[] bytes;
            ExportEntry1967(String path,byte[] bytes){this.path=path;this.bytes=bytes;}
        }
        private static int collect1967(ArrayList<ExportEntry1967> entries,File directory,String prefix,String path,int count,int maximum){
            File[] found=files(directory,prefix);int skipped=0;
            for(int n=Math.max(0,found.length-count);n<found.length;n++)try{
                // Another active owner may append/rotate after enumeration.
                // Read only a bounded complete tail; never repair its live file.
                entries.add(new ExportEntry1967(path+found[n].getName(),prefix.equals("snapshot-")?readProtectedTail1967(found[n],maximum):readTail1967(found[n],maximum)));
            }catch(IOException rotated){skipped++;}
            return skipped;
        }
        private static int collectStore1967(ArrayList<ExportEntry1967> entries,File directory,String path){
            return collect1967(entries,new File(directory,"rolling"),"segment-",path+"rolling/",ROLL_COUNT,ROLL_BYTES)
                +collect1967(entries,new File(directory,"protected"),"snapshot-",path+"protected/",SNAPSHOT_COUNT,SNAPSHOT_BYTES);
        }
        synchronized void exportZip(OutputStream destination)throws IOException{
            flush(true);ArrayList<ExportEntry1967> entries=new ArrayList<ExportEntry1967>();
            int skipped=collectStore1967(entries,directory,"");
            if(aggregateRoot!=null){
                for(int slot=0;slot<OWNER_COUNT;slot++){
                    File other=new File(aggregateRoot,"owner-"+slot);
                    if(!other.equals(directory)&&other.isDirectory())skipped+=collectStore1967(entries,other,"owners/owner-"+slot+"/");
                }
                // The upgrade must preserve access to the original .65 logs.
                skipped+=collectStore1967(entries,aggregateRoot,"legacy/");
            }
            try(ZipOutputStream zip=new ZipOutputStream(destination)){
                String manifest="{\"version\":"+quote(VERSION)+",\"session\":"+quote(session)+",\"prior_session\":"+quote(priorSession)
                    +",\"prior_session_close_not_recorded\":"+priorOpen+",\"recovered_tail_bytes\":"+recoveredTailBytes
                    +",\"dropped_records\":"+totalDropped+",\"rolling_files\":8,\"rolling_bytes_per_file\":131072,\"protected_files\":3,\"protected_bytes_per_file\":262144,\"anomaly_followup_window_ms\":5000,\"writer_slots\":4,\"aggregate_export_max_bytes\":9175040,\"files_skipped_during_export\":"+skipped+",\"includes_photos_or_frames\":false}\n";
                zip.putNextEntry(new ZipEntry("manifest.json"));zip.write(manifest.getBytes(UTF8));zip.closeEntry();
                zip.putNextEntry(new ZipEntry("README.txt"));zip.write(("Camera lifecycle scalar diagnostics only. No photos, video or frame pixels.\nA prior session without a close record does not by itself prove a crash.\nRoutine rolling log rotation never replaces protected incident snapshots.\nEach process owns a locked fixed slot. Export includes complete bounded tails from other slots and legacy logs; actively rotating files may be skipped and counted in manifest.json.\n").getBytes(UTF8));zip.closeEntry();
                for(ExportEntry1967 entry:entries){zip.putNextEntry(new ZipEntry(entry.path));zip.write(entry.bytes);zip.closeEntry();}
            }
        }
        private void marker(String state)throws IOException{
            File destination=new File(directory,"session-state.txt"),temporary=new File(directory,"session-state.pending");
            try(FileOutputStream marker=new FileOutputStream(temporary)){marker.write((session+"\n"+state+"\n").getBytes(UTF8));marker.getFD().sync();}
            if(!temporary.renameTo(destination)){temporary.delete();throw new IOException("session marker unavailable");}
        }
        synchronized void markClean()throws IOException{marker("closed");}
        synchronized void close()throws IOException{
            if(closed)return;closed=true;
            try{
                try{flush(true);}finally{try{if(output!=null)output.close();else if(raw!=null)raw.close();}finally{output=null;raw=null;marker("closed");}}
            }finally{
                try{if(ownerLock!=null)ownerLock.release();}finally{ownerLock=null;if(ownerFile!=null)ownerFile.close();ownerFile=null;}
            }
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

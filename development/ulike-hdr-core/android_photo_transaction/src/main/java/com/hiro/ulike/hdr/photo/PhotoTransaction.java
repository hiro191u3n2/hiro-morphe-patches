package com.hiro.ulike.hdr.photo;

import com.hiro.ulike.hdr.gainmap.GainmapMath;
import com.hiro.ulike.hdr.gainmap.GainmapSave;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** One photo, private staging then MediaStore pending-write/publish. Never overwrites an existing photo. */
public final class PhotoTransaction implements AutoCloseable {
    private static final Object MANAGER_MONITOR=new Object();
    public final PhotoIdentity identity;public final PendingPhotoStore.Plan plan;
    private final PendingPhotoStore store;private final Path root,directory;private final long diskBudget;
    private final RandomAccessFile lockFile;private final FileLock lock;
    private final List<AutoCloseable> resources=new ArrayList<>();
    private long reserved;private int files;private String uri="",phase="NEW";private boolean committed,closed;
    private PhotoTransaction(Path root,PhotoIdentity identity,PendingPhotoStore store,long diskBudget)throws IOException{
        if(identity==null || store==null || diskBudget<1 || diskBudget>8L*1024*1024*1024)throw new IllegalArgumentException("photo transaction contract");
        if(!Files.isDirectory(root,LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(root))throw new IOException("private transaction root must be a real directory");
        this.identity=identity;this.store=store;this.diskBudget=diskBudget;this.root=root;
        plan=new PendingPhotoStore.Plan(UUID.randomUUID().toString(),identity.identitySha256,identity.dateTakenMs);
        directory=Files.createDirectory(root.resolve("tx-"+plan.transactionId));
        RandomAccessFile opened=null;FileLock acquired=null;
        try{opened=new RandomAccessFile(directory.resolve("lock").toFile(),"rw");acquired=opened.getChannel().lock();}
        catch(IOException|RuntimeException e){if(opened!=null)try{opened.close();}catch(IOException close){e.addSuppressed(close);}deleteDirectory(directory);throw e;}
        lockFile=opened;lock=acquired;
        try{journal();}catch(IOException e){try{lock.release();lockFile.close();deleteDirectory(directory);}catch(IOException close){e.addSuppressed(close);}throw e;}
    }
    public static PhotoTransaction begin(Path privateRoot,PhotoIdentity identity,PendingPhotoStore store,long diskBudget)throws IOException{
        if(!Files.isDirectory(privateRoot,LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(privateRoot))throw new IOException("private transaction root must be a real directory");
        synchronized(MANAGER_MONITOR){try(RandomAccessFile manager=new RandomAccessFile(privateRoot.resolve("manager.lock").toFile(),"rw");FileLock held=manager.getChannel().lock()){
            return new PhotoTransaction(privateRoot,identity,store,diskBudget);
        }}
    }
    private void active()throws IOException{if(closed || committed)throw new IOException("photo transaction closed/committed");if(Thread.currentThread().isInterrupted())throw new IOException("photo transaction cancelled");}
    public synchronized StoredRgb.Writer createRgb(StoredRgb.Precision precision,StoredRgb.Domain domain,double headroom)throws IOException{
        return createRgbFor(identity,precision,domain,headroom);
    }
    private StoredRgb.Writer createRgbFor(PhotoIdentity raster,StoredRgb.Precision precision,StoredRgb.Domain domain,double headroom)throws IOException{
        active();long bytes=StoredRgb.storageBytes(raster,precision);
        if(bytes>diskBudget-reserved || bytes>Files.getFileStore(directory).getUsableSpace())throw new IOException("insufficient explicit RGB staging budget/storage");
        StoredRgb.Writer writer=new StoredRgb.Writer(directory.resolve("rgb-"+(files++)+".stage"),raster,precision,domain,headroom,bytes);
        reserved+=bytes;resources.add(writer);return writer;
    }
    public synchronized PairedStore.Writer createPair(double headroom)throws IOException{
        return createPairFor(identity,headroom);
    }
    synchronized PairedStore.Writer createPairFor(PhotoIdentity raster,double headroom)throws IOException{
        active();if(!Double.isFinite(headroom) || headroom<=1 || headroom>10000.0/203.0)throw new IllegalArgumentException("explicit gainmap headroom");
        StoredRgb.Writer sdr=createRgbFor(raster,StoredRgb.Precision.FP64,StoredRgb.Domain.DISPLAY_LINEAR_BT2020_SDR,1);
        try{return new PairedStore.Writer(raster,headroom,sdr,createRgbFor(raster,StoredRgb.Precision.FP64,StoredRgb.Domain.DISPLAY_LINEAR_BT2020_HDR,headroom));}
        catch(IOException|RuntimeException e){try{sdr.abort();}catch(IOException close){e.addSuppressed(close);}throw e;}
    }
    public synchronized GeometryPairWriter createTransformedPair(PhotoGeometry geometry,double headroom,long maxTileWorkspaceBytes)throws IOException{
        active();return new GeometryPairWriter(this,geometry,headroom,maxTileWorkspaceBytes);
    }
    public synchronized String savePair(PairedStore.Pair pair,GainmapSave.QualityLimits limits,GainmapSave.Codec codec)throws IOException{
        if(pair==null || pair.identity!=identity)throw new IllegalArgumentException("pair belongs to another capture/settings transaction");
        return savePair(pair.sdr(),pair.hdr(),pair.headroom,limits,codec);
    }
    public synchronized String savePair(GainmapMath.Image processedSdr,GainmapMath.Image processedHdr,double headroom,
            GainmapSave.QualityLimits limits,GainmapSave.Codec codec)throws IOException{
        active();if(!identity.frame.equals(processedSdr.frame) || !identity.frame.equals(processedHdr.frame))throw new IllegalArgumentException("processed pair identity/dimensions changed");
        GainmapSave.Result complete=GainmapSave.prepare(processedSdr,processedHdr,headroom,limits,codec,directory);
        active();phase="INSERTING";journal(); // durable planned name closes insert-before-URI-journal crash gap.
        uri=store.insertPending(plan);if(uri==null || uri.isEmpty() || uri.length()>2048)throw new IOException("no pending MediaStore URI");
        phase="INSERTED";journal();
        store.writeAndSync(uri,plan,complete.file.fileBytes,complete.file::writeTo);
        active();PendingPhotoStore.Entry entry=store.inspect(uri,plan);
        if(entry==null || !entry.owned || !entry.matching || !entry.pending || entry.bytes!=complete.file.fileBytes)throw new IOException("pending photo byte/ownership validation failed");
        IOException publicationFailure=null;try{store.publish(uri,plan);}catch(IOException failure){publicationFailure=failure;}
        try{entry=store.inspect(uri,plan);}catch(IOException failure){if(publicationFailure!=null)failure.addSuppressed(publicationFailure);throw failure;}
        if(entry==null || !entry.owned || !entry.matching || entry.pending || entry.bytes!=complete.file.fileBytes){IOException failure=new IOException("photo publication not confirmed");if(publicationFailure!=null)failure.addSuppressed(publicationFailure);throw failure;}
        // Commit linearization is IS_PENDING becoming zero. Cancellation after this point must not remove the photo.
        // The on-disk INSERTED journal is sufficient for recovery: a confirmed visible row is
        // preserved. Avoid a fallible journal write after the public commit linearization.
        committed=true;phase="PUBLISHED";return uri;
    }
    private void journal()throws IOException{
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
        out.writeInt(0x554c5458);out.writeInt(1);out.writeUTF(plan.transactionId);out.writeUTF(plan.identitySha256);out.writeLong(plan.dateTakenMs);out.writeUTF(phase);out.writeUTF(uri);out.flush();
        byte[] payload=bytes.toByteArray();Path temporary=directory.resolve("journal.new"),target=directory.resolve("journal");
        try(FileOutputStream file=new FileOutputStream(temporary.toFile())){file.write(payload);file.write(PhotoIdentity.digest().digest(payload));file.getFD().sync();}
        Files.move(temporary,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
    }
    private static PendingPhotoStore.Plan readPlan(Path directory)throws IOException{
        Path path=directory.resolve("journal");if(!Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS) || Files.size(path)>16384)throw new IOException("missing/bounded transaction journal");
        byte[] all=Files.readAllBytes(path);if(all.length<40)throw new IOException("truncated transaction journal");int payload=all.length-32;
        byte[] bytes=java.util.Arrays.copyOf(all,payload),hash=java.util.Arrays.copyOfRange(all,payload,all.length);
        if(!java.security.MessageDigest.isEqual(PhotoIdentity.digest().digest(bytes),hash))throw new IOException("transaction journal checksum");
        DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes));
        if(in.readInt()!=0x554c5458 || in.readInt()!=1)throw new IOException("unknown journal version");
        String id=in.readUTF(),identity=in.readUTF();long date=in.readLong();String phase=in.readUTF(),uri=in.readUTF();
        if(in.available()!=0 || !directory.getFileName().toString().equals("tx-"+id)
                || !(phase.equals("NEW") || phase.equals("INSERTING") || phase.equals("INSERTED") || phase.equals("PUBLISHED")) || uri.length()>2048)
            throw new IOException("invalid journal identity/state");
        try{return new PendingPhotoStore.Plan(id,identity,date);}catch(IllegalArgumentException e){throw new IOException("journal plan",e);}
    }
    /** Run on app start/worker. Skips active locks; deletes ONLY exact owned pending rows, never published media.
     * A corrupt journal is retained for diagnosis, not guessed/deleted. Returns recovered transaction count. */
    public static int recover(Path privateRoot,PendingPhotoStore store)throws IOException{
        if(!Files.isDirectory(privateRoot,LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(privateRoot))throw new IOException("invalid private recovery root");int recovered=0;IOException failures=null;
        synchronized(MANAGER_MONITOR){try(RandomAccessFile manager=new RandomAccessFile(privateRoot.resolve("manager.lock").toFile(),"rw");FileLock managerHeld=manager.getChannel().lock()){
        try(DirectoryStream<Path> entries=Files.newDirectoryStream(privateRoot,"tx-*")){
            for(Path directory:entries){if(!Files.isDirectory(directory,LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(directory))continue;
                try{
                boolean remove=false;
                try(RandomAccessFile file=new RandomAccessFile(directory.resolve("lock").toFile(),"rw")){
                    FileLock held;try{held=file.getChannel().tryLock();}catch(OverlappingFileLockException active){continue;}if(held==null)continue;
                    try{
                        if(Files.exists(directory.resolve("journal"),LinkOption.NOFOLLOW_LINKS)){PendingPhotoStore.Plan plan=readPlan(directory);cleanupPending(store,plan);}
                        else{
                            // In this process-crash protocol insertion cannot start before journal's
                            // first atomic rename. An uninitialized private directory has no media row.
                            try(DirectoryStream<Path> partial=Files.newDirectoryStream(directory)){
                                for(Path p:partial)if(!(p.getFileName().toString().equals("lock") || p.getFileName().toString().equals("journal.new")))throw new IOException("unrecognized uninitialized transaction");
                            }
                        }
                        remove=true;
                    }finally{held.release();}
                }
                if(remove){deleteDirectory(directory);recovered++;}
                }catch(IOException failure){if(failures==null)failures=new IOException("one or more abandoned photo transactions require recovery attention");failures.addSuppressed(failure);}
            }
        }if(failures!=null){IOException aggregate=new IOException("recovered "+recovered+" transaction(s); retained other invalid/unavailable journals",failures);throw aggregate;}return recovered;
        }}
    }
    private static void cleanupPending(PendingPhotoStore store,PendingPhotoStore.Plan plan)throws IOException{
        for(PendingPhotoStore.Entry entry:store.find(plan))if(entry.owned && entry.matching && entry.pending)store.deletePending(entry.uri,plan);
        for(PendingPhotoStore.Entry entry:store.find(plan))if(entry.owned && entry.matching && entry.pending)throw new IOException("owned pending photo cleanup incomplete");
    }
    @Override public synchronized void close()throws IOException{
        if(closed)return;boolean interrupted=Thread.interrupted();try{closed=true;IOException first=null;
        for(int i=resources.size()-1;i>=0;i--)try{resources.get(i).close();}catch(Exception e){if(first==null)first=e instanceof IOException?(IOException)e:new IOException(e);else first.addSuppressed(e);}
        synchronized(MANAGER_MONITOR){try(RandomAccessFile manager=new RandomAccessFile(root.resolve("manager.lock").toFile(),"rw");FileLock held=manager.getChannel().lock()){
            boolean cleanup=committed;try{if(!committed)cleanupPending(store,plan);cleanup=true;}catch(IOException e){if(first==null)first=e;else first.addSuppressed(e);}
            try{lock.release();lockFile.close();}catch(IOException e){if(first==null)first=e;else first.addSuppressed(e);}
            if(cleanup)try{deleteDirectory(directory);}catch(IOException e){if(first==null)first=e;else first.addSuppressed(e);}
        }catch(IOException e){if(first==null)first=e;else first.addSuppressed(e);try{if(lock.isValid())lock.release();lockFile.close();}catch(IOException release){first.addSuppressed(release);}}}
        if(first!=null)throw first;
        }finally{if(interrupted)Thread.currentThread().interrupt();}
    }
    private static void deleteDirectory(Path directory)throws IOException{
        List<Path> files=new ArrayList<>();
        try(DirectoryStream<Path> entries=Files.newDirectoryStream(directory)){
            for(Path entry:entries){if(Files.isDirectory(entry,LinkOption.NOFOLLOW_LINKS))throw new IOException("unexpected nested transaction directory");files.add(entry);}
        }
        // Keep a valid recovery plan until all potentially failing stage deletions finish.
        for(Path entry:files)if(!entry.getFileName().toString().equals("journal") && !entry.getFileName().toString().equals("lock"))Files.delete(entry);
        Files.deleteIfExists(directory.resolve("journal"));Files.deleteIfExists(directory.resolve("lock"));Files.delete(directory);
    }
}

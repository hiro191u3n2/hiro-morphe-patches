package com.hiro.ulike.hdr.photo;

import com.hiro.ulike.hdr.gainmap.GainmapSave;
import com.hiro.ulike.hdr.gainmap.PhotoTestCodecFactory;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class TransactionTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    interface Throwing {void run()throws Exception;}
    private static void reject(Throwing operation,String label)throws Exception{try{operation.run();throw new AssertionError("accepted "+label);}catch(IOException|IllegalArgumentException|IllegalStateException expected){checks++;}}
    static final class Store implements PendingPhotoStore {
        final Path root;String fault;boolean publicationAttempted;int publicationCalls;
        Store(Path root,String fault)throws IOException{this.root=root;this.fault=fault;Files.createDirectories(root);}
        private Path meta(String id){return root.resolve(id+".meta");}
        private Path image(String id){return root.resolve(id+".heic");}
        private String id(String uri)throws IOException{if(!uri.startsWith("test://"))throw new IOException("wrong URI");String id=uri.substring(7);if(!id.matches("[0-9a-f-]{36}"))throw new IOException("wrong ID");return id;}
        void record(Plan plan,boolean pending,boolean owned)throws IOException{
            try(DataOutputStream out=new DataOutputStream(Files.newOutputStream(meta(plan.transactionId)))){
                out.writeUTF(plan.transactionId);out.writeUTF(plan.identitySha256);out.writeLong(plan.dateTakenMs);out.writeBoolean(pending);out.writeBoolean(owned);
            }
        }
        public String insertPending(Plan plan)throws IOException{
            publicationAttempted=false;
            Files.createFile(image(plan.transactionId));record(plan,true,true);
            if("crash_insert".equals(fault))Runtime.getRuntime().halt(88);
            return "test://"+plan.transactionId;
        }
        public void writeAndSync(String uri,Plan plan,long expected,Write writer)throws IOException{
            String id=id(uri);Entry before=inspect(uri,plan);if(before==null || !before.pending || !before.owned || !before.matching)throw new IOException("not owned pending");
            try(FileOutputStream file=new FileOutputStream(image(id).toFile())){
                OutputStream target=file;
                if("write_fail".equals(fault))target=new OutputStream(){int count;public void write(int value)throws IOException{if(++count>100)throw new IOException("injected write failure");file.write(value);}};
                writer.to(target);file.getFD().sync();
            }
            if(Files.size(image(id))!=expected)throw new IOException("fixture byte mismatch");
            if("crash_write".equals(fault))Runtime.getRuntime().halt(88);
            if("cancel_write".equals(fault))Thread.currentThread().interrupt();
        }
        public void publish(String uri,Plan plan)throws IOException{
            Entry before=inspect(uri,plan);if(before==null || !before.pending || !before.owned || !before.matching)throw new IOException("not owned pending");
            publicationAttempted=true;publicationCalls++;
            if("pending_unknown".equals(fault))throw new IOException("publish failed before update; confirming query will also fail");
            record(plan,false,true);
            if("crash_publish".equals(fault))Runtime.getRuntime().halt(88);
            if("publish_throw".equals(fault))throw new IOException("provider error after committed update");
        }
        public Entry inspect(String uri,Plan plan)throws IOException{
            if(publicationAttempted && ("inspect_after_publish_io".equals(fault) || "pending_unknown".equals(fault)))throw new IOException("confirming query unavailable");
            if(publicationAttempted && "inspect_after_publish_runtime".equals(fault))throw new SecurityException("confirming query access lost");
            String id=id(uri);if(!Files.exists(meta(id)))return null;
            try(DataInputStream in=new DataInputStream(Files.newInputStream(meta(id)))){
                String storedId=in.readUTF(),identity=in.readUTF();long date=in.readLong();boolean pending=in.readBoolean(),owned=in.readBoolean();
                String observed=publicationAttempted && "inspect_after_publish_wrong_uri".equals(fault)?uri+"-different":uri;
                long bytes=Files.size(image(id));if(publicationAttempted && "inspect_after_publish_wrong_bytes".equals(fault))bytes++;
                return new Entry(observed,owned,storedId.equals(plan.transactionId)&&identity.equals(plan.identitySha256)&&date==plan.dateTakenMs,pending,bytes);
            }
        }
        public List<Entry> find(Plan plan)throws IOException{
            List<Entry> result=new ArrayList<>();Entry own=inspect("test://"+plan.transactionId,plan);if(own!=null)result.add(own);return result;
        }
        public void deletePending(String uri,Plan plan)throws IOException{
            Entry before=inspect(uri,plan);if(before!=null && before.owned && before.matching && before.pending){
                if("delete_fail".equals(fault))throw new IOException("injected cleanup failure");Files.delete(image(id(uri)));Files.delete(meta(id(uri)));
            }
        }
        int pending()throws IOException{int total=0;try(DirectoryStream<Path> files=Files.newDirectoryStream(root,"*.meta")){for(Path p:files)try(DataInputStream in=new DataInputStream(Files.newInputStream(p))){in.readUTF();in.readUTF();in.readLong();if(in.readBoolean())total++;}}return total;}
        int photos()throws IOException{int total=0;try(DirectoryStream<Path> files=Files.newDirectoryStream(root,"*.heic")){for(Path p:files)total++;}return total;}
    }
    private static PhotoIdentity identity(){return new PhotoIdentity(new Object(),64,48,"exact-capture","rotation0-fullcrop","hdr-appearance-v1",new byte[]{1,2,3},1790800000000L);}
    private static PairedStore.Pair pair(PhotoTransaction tx)throws IOException{
        PairedStore.Writer writer=tx.createPair(8);int w=tx.identity.frame.width;
        double[] sdr=new double[w*3],hdr=new double[w*3];
        for(int y=0;y<tx.identity.frame.height;y++){
            for(int x=0;x<w;x++)for(int c=0;c<3;c++){int i=x*3+c;double r=(x+.4*y)/(w-1+.4*(tx.identity.frame.height-1));sdr[i]=.02+.65*r;hdr[i]=sdr[i]*(1.3+3*r+.15*c);}
            writer.writeRows(y,1,sdr,hdr);
        }return writer.seal();
    }
    private static long transactions(Path root)throws IOException{try(java.util.stream.Stream<Path> entries=Files.list(root)){return entries.filter(p->p.getFileName().toString().startsWith("tx-")).count();}}
    public static void main(String[] args)throws Exception{
        Path work=Paths.get(args[0]);Files.createDirectories(work);Path root=work.resolve("private"),gallery=work.resolve("gallery"),codec=work.resolve("codec");
        Files.createDirectories(root);Files.createDirectories(codec);
        if(args.length>1 && args[1].equals("recover")){
            Store store=new Store(gallery,"");int before=store.photos(),pending=store.pending(),recovered=PhotoTransaction.recover(root,store);
            System.out.println("{\"photos_before\":"+before+",\"pending_before\":"+pending+",\"recovered\":"+recovered+",\"photos_after\":"+store.photos()+",\"pending_after\":"+store.pending()+",\"transactions_after\":"+transactions(root)+"}");return;
        }
        if(args.length>1){Store store=new Store(gallery,args[1]);try(PhotoTransaction tx=PhotoTransaction.begin(root,identity(),store,16*1024*1024)){
            tx.savePair(pair(tx),new GainmapSave.QualityLimits(.2,.4,.08),PhotoTestCodecFactory.open(codec,"crash"));
        }throw new AssertionError("crash fixture returned");}
        Store store=new Store(gallery,"");PhotoIdentity id=identity();
        try(PhotoTransaction tx=PhotoTransaction.begin(root,id,store,16*1024*1024)){
            check(PhotoTransaction.recover(root,store)==0,"recovery skips active process lock");
            StoredRgb.Writer s=tx.createRgb(StoredRgb.Precision.FP32,StoredRgb.Domain.ENCODED_SRGB,1);
            float[] row=new float[id.frame.width*3];for(int i=0;i<row.length;i++)row[i]=i/(float)(row.length-1);row[0]=-0.0f;
            for(int y=0;y<id.frame.height;y++)s.writeRows(y,1,row);
            StoredRgb.Reader r=s.seal();float[] pixel=new float[3];double[] actual=new double[row.length];
            for(int y=0;y<id.frame.height;y++){r.readRow(y,actual);for(int i=0;i<row.length;i++)check(Float.floatToRawIntBits((float)actual[i])==Float.floatToRawIntBits(row[i]),"FP32 file roundtrip bits");}
            r.readPixel(0,0,pixel);check(Float.floatToRawIntBits(pixel[0])==0x80000000,"negative-zero file preservation");
            reject(()->s.writeRows(0,1,row),"write after seal");
            StoredRgb.Writer incomplete=tx.createRgb(StoredRgb.Precision.FP64,StoredRgb.Domain.DISPLAY_LINEAR_BT2020_SDR,1);
            reject(incomplete::seal,"incomplete staged raster");
            StoredRgb.Writer corrupt=tx.createRgb(StoredRgb.Precision.FP64,StoredRgb.Domain.DISPLAY_LINEAR_BT2020_SDR,1);
            double[] thirds=new double[row.length];Arrays.fill(thirds,1.0/3);
            for(int y=0;y<id.frame.height;y++)corrupt.writeRows(y,1,thirds);StoredRgb.Reader corrupted=corrupt.seal();
            try(RandomAccessFile edit=new RandomAccessFile(corrupt.path.toFile(),"rw")){edit.seek(4096);edit.write(0);}
            reject(()->corrupted.readRow(0,new double[row.length]),"tampered staged row");
            StoredRgb.Writer beauty=tx.createRgb(StoredRgb.Precision.FP32,StoredRgb.Domain.ENCODED_SRGB,1);
            reject(()->beauty.asBeautySink().begin(id.frame.width,id.frame.height,new Object()),"preview source cannot enter still sink");
            reject(()->tx.createRgb(StoredRgb.Precision.FP64,StoredRgb.Domain.DISPLAY_LINEAR_BT2020_HDR,Double.NaN),"missing HDR domain");
        }
        check(transactions(root)==0 && store.photos()==0,"private stages cleanup without publishing");
        for(int angle:new int[]{0,90,180,270})for(boolean mirror:new boolean[]{false,true}){
            int w=angle%180==0?6:4,h=angle%180==0?4:6;PhotoGeometry g=new PhotoGeometry(6,4,angle,mirror,0,0,w,h);boolean[] visited=new boolean[24];int[] xy=new int[2];
            for(int y=0;y<h;y++)for(int x=0;x<w;x++){g.sourcePixel(x,y,xy);check(xy[0]>=0 && xy[0]<6 && xy[1]>=0 && xy[1]<4,"mapped pixel in exact input");int at=xy[1]*6+xy[0];check(!visited[at],"rotation/mirror never duplicates input pixel");visited[at]=true;}
        }
        long maxGeometryWorkspace=0;
        for(int angle:new int[]{0,90,180,270})for(boolean mirror:new boolean[]{false,true}){
            int sw=130,sh=98,uw=angle%180==0?sw:sh,uh=angle%180==0?sh:sw;
            PhotoGeometry geometry=new PhotoGeometry(sw,sh,angle,mirror,1,3,uw-4,uh-6);
            PhotoIdentity output=new PhotoIdentity(new Object(),geometry.width,geometry.height,"same-shot",geometry.id(),"same-hdr-appearance",new byte[]{9,7},1790800000000L);
            try(PhotoTransaction tx=PhotoTransaction.begin(root,output,store,16*1024*1024)){
                GeometryPairWriter transformed=tx.createTransformedPair(geometry,4,128*1024);maxGeometryWorkspace=Math.max(maxGeometryWorkspace,transformed.tileArrayWorkspaceBound);
                transformed.begin(output.exactSource,sw,sh,output.settingsSha256,geometry.id(),4);
                double[] s=new double[sw*3],h=new double[s.length];
                for(int y=0;y<sh;y++){for(int x=0;x<sw;x++)for(int c=0;c<3;c++){double v=((y*sw+x)*3+c)/(double)(sw*sh*3);s[x*3+c]=v;h[x*3+c]=v*2.5;}transformed.writeRows(y,1,s,h);}
                transformed.commit();PairedStore.Pair pair=transformed.pair();check(pair.identity==output,"final pair exact output identity");
                double[] sr=new double[geometry.width*3],hr=new double[sr.length];int[] p=new int[2];
                for(int y=0;y<geometry.height;y++){pair.sdr().read(y,sr);pair.hdr().read(y,hr);for(int x=0;x<geometry.width;x++){
                    geometry.sourcePixel(x,y,p);for(int c=0;c<3;c++){double expected=((p[1]*sw+p[0])*3+c)/(double)(sw*sh*3);
                        check(Double.doubleToRawLongBits(sr[x*3+c])==Double.doubleToRawLongBits(expected),"SDR disk rotation/crop bit exact");
                        check(Double.doubleToRawLongBits(hr[x*3+c])==Double.doubleToRawLongBits(expected*2.5),"HDR disk rotation/crop bit exact");}
                }}
                check(transformed.tileArrayWorkspaceBound<=128*1024,"bounded actual orientation workspace");
            }
        }
        String success;
        try(PhotoTransaction tx=PhotoTransaction.begin(root,identity(),store,16*1024*1024)){
            success=tx.savePair(pair(tx),new GainmapSave.QualityLimits(.2,.4,.08),PhotoTestCodecFactory.open(codec,"success"));
            check(!store.inspect(success,tx.plan).pending,"completed callback URI is visible only after full save");
        }
        check(store.photos()==1 && store.pending()==0 && transactions(root)==0,"success leaves one complete photo only");
        for(String fault:new String[]{"write_fail","cancel_write"}){
            store.fault=fault;
            reject(()->{try(PhotoTransaction tx=PhotoTransaction.begin(root,identity(),store,16*1024*1024)){
                tx.savePair(pair(tx),new GainmapSave.QualityLimits(.2,.4,.08),PhotoTestCodecFactory.open(codec,fault));
            }},fault);
            boolean interrupted=Thread.interrupted();if(fault.equals("cancel_write"))check(interrupted,"cancel status preserved after cleanup");
            check(store.photos()==1 && store.pending()==0 && transactions(root)==0,"failed/cancelled write cannot publish partial photo");
        }
        store.fault="publish_throw";
        try(PhotoTransaction tx=PhotoTransaction.begin(root,identity(),store,16*1024*1024)){
            String published=tx.savePair(pair(tx),new GainmapSave.QualityLimits(.2,.4,.08),PhotoTestCodecFactory.open(codec,"throw_after_commit"));
            check(!store.inspect(published,tx.plan).pending,"provider error after commit reconciled by actual row");
        }
        store.fault="";check(store.photos()==2 && store.pending()==0,"unique names preserve previous photo");
        int visible=2;
        for(String fault:new String[]{"inspect_after_publish_io","inspect_after_publish_runtime","inspect_after_publish_wrong_uri","inspect_after_publish_wrong_bytes","pending_unknown"}){
            store.fault=fault;PhotoTransaction.PublicationUncertainException uncertain=null;int calls=store.publicationCalls;
            try(PhotoTransaction tx=PhotoTransaction.begin(root,identity(),store,16*1024*1024)){
                try{tx.savePair(pair(tx),new GainmapSave.QualityLimits(.2,.4,.08),PhotoTestCodecFactory.open(codec,fault));throw new AssertionError("unknown publication returned success");}
                catch(PhotoTransaction.PublicationUncertainException failure){
                    uncertain=failure;check(failure.candidateUri.equals("test://"+tx.plan.transactionId),"uncertain exact candidate URI");
                    check(failure.transactionId.equals(tx.plan.transactionId) && failure.identitySha256.equals(tx.plan.identitySha256) && failure.expectedBytes>0,"uncertain exact identity and expected bytes");
                    reject(()->PhotoTransaction.reconcile(store,failure),"cannot reconcile while confirmation remains ambiguous");
                    reject(()->tx.createPair(8),"no restaging or re-publication of attempted transaction");
                }
            }
            check(uncertain!=null && store.publicationCalls==calls+1,"exactly one provider publication attempt");
            check(transactions(root)==1,"unknown outcome retains its durable journal after close");
            boolean pending=fault.equals("pending_unknown");if(!pending)visible++;
            check(store.photos()==visible+(pending?1:0) && store.pending()==(pending?1:0),"uncertain close cannot remove candidate or completed photograph");
            store.fault="";
            check(PhotoTransaction.reconcile(store,uncertain)==(pending?PhotoTransaction.PublicationState.PENDING:PhotoTransaction.PublicationState.PUBLISHED),"later exact read-only reconciliation");
            check(store.publicationCalls==calls+1,"reconciliation never republishes");
            check(PhotoTransaction.recover(root,store)==1 && transactions(root)==0,"startup recovers PUBLISHING journal");
            check(store.photos()==visible && store.pending()==0,"recovery preserves every visible photograph and deletes only exact pending row");
        }
        Path uninitialized=Files.createDirectory(root.resolve("tx-00000000-0000-0000-0000-000000000000"));Files.write(uninitialized.resolve("lock"),new byte[0]);Files.write(uninitialized.resolve("journal.new"),new byte[]{1,2});
        check(PhotoTransaction.recover(root,store)==1 && !Files.exists(uninitialized),"restart clears uninitialized private transaction");
        System.out.println("{\"checks\":"+checks+",\"successful_photo_files\":"+visible+",\"uncertain_publication_cases\":5,\"max_geometry_tile_workspace\":"+maxGeometryWorkspace+",\"published_first_uri\":\""+success+"\",\"android_media_store_executed\":false}");
    }
}

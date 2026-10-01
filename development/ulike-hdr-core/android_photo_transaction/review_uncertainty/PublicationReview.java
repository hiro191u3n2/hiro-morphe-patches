package com.hiro.ulike.hdr.photo;

import com.hiro.ulike.hdr.gainmap.GainmapMath;
import com.hiro.ulike.hdr.gainmap.GainmapSave;
import com.hiro.ulike.hdr.gainmap.PhotoTestCodecFactory;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Independent host provider-failure matrix. Only the provider is synthetic.
 * Codec memoization verifies exact RGB10 inputs and encoded bytes, after real
 * x265 encode / FFmpeg decode for each of the two image roles.
 */
public final class PublicationReview {
    static int checks,cases;
    static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    interface Action {void run()throws Exception;}
    static IOException io(Action action,String label)throws Exception{
        try{action.run();throw new AssertionError("accepted "+label);}catch(IOException e){checks++;return e;}
    }
    static final class MemoCodec implements GainmapSave.Codec {
        final GainmapSave.Codec real;
        final Map<GainmapSave.Role,int[]> inputs=new EnumMap<>(GainmapSave.Role.class),decoded=new EnumMap<>(GainmapSave.Role.class);
        final Map<GainmapSave.Role,byte[]> encoded=new EnumMap<>(GainmapSave.Role.class);
        int encodeCalls,realEncodes,realDecodes;
        MemoCodec(Path work)throws IOException{Files.createDirectories(work);real=PhotoTestCodecFactory.open(work,"independent");}
        static int[] raster(GainmapMath.Codes image){
            int[] all=new int[image.frame.width*image.frame.height*3],row=new int[image.frame.width*3];
            for(int y=0;y<image.frame.height;y++){image.read(y,row);System.arraycopy(row,0,all,y*row.length,row.length);}return all;
        }
        public byte[] encode(GainmapMath.Codes image,GainmapSave.Role role)throws IOException{
            encodeCalls++;int[] all=raster(image);
            if(!inputs.containsKey(role)){inputs.put(role,all);encoded.put(role,real.encode(image,role));realEncodes++;}
            else check(Arrays.equals(inputs.get(role),all),"memoized codec exact RGB10 input");
            return encoded.get(role).clone();
        }
        public GainmapSave.Decoded decode(byte[] bytes,GainmapMath.Frame frame,GainmapSave.Role role)throws IOException{
            check(Arrays.equals(encoded.get(role),bytes),"memoized decode exact HEVC bytes");
            if(!decoded.containsKey(role))try(GainmapSave.Decoded realResult=real.decode(bytes,frame,role)){
                decoded.put(role,raster(realResult.codes()));realDecodes++;
            }
            final int[] copy=decoded.get(role).clone();
            return new GainmapSave.Decoded(){
                public GainmapMath.Codes codes(){return new GainmapMath.Codes(frame,(y,row)->System.arraycopy(copy,y*row.length,row,0,row.length));}
                public void close(){Arrays.fill(copy,0);}
            };
        }
    }
    static final class Store implements PendingPhotoStore {
        final Path root;Plan exact;String uri;long bytes;boolean exists,pending=true,publishedCall;
        String publishFault="ok",inspectFault="ok";boolean writeFailure,cancelWrite,cancelPublish,recovering;
        int inserts,writes,publishes,inspects,finds,deletes;
        final IOException reused=new IOException("same provider exception reused");
        Store(Path root){this.root=root;}
        void same(Plan p){check(recovering?p.transactionId.equals(exact.transactionId)&&p.identitySha256.equals(exact.identitySha256)&&p.dateTakenMs==exact.dateTakenMs:p==exact,"same exact immutable plan passed to provider / recovered value identity");}
        public String insertPending(Plan p){if(exact==null)exact=p;else same(p);inserts++;uri="content://independent/images/"+inserts;exists=true;pending=true;return uri;}
        public void writeAndSync(String u,Plan p,long expected,Write writer)throws IOException{
            same(p);check(u.equals(uri),"write exact inserted URI");writes++;
            if(writeFailure)throw new IOException("provider write failed");
            ByteArrayOutputStream out=new ByteArrayOutputStream();writer.to(out);bytes=out.size();check(bytes==expected,"complete actual HEIF serialized");
            if(cancelWrite)Thread.currentThread().interrupt();
        }
        public void publish(String u,Plan p)throws IOException{
            same(p);check(u.equals(uri),"publish exact inserted URI");publishes++;publishedCall=true;
            try{readJournal(root.resolve("tx-"+p.transactionId),"PUBLISHING",u);}catch(Exception e){throw new IOException(e);}
            if(publishFault.equals("io_before"))throw reused;
            if(publishFault.equals("runtime_before"))throw new SecurityException("publish runtime before update");
            if(publishFault.equals("error_before"))throw new AssertionError("publish Error before update");
            pending=false;if(cancelPublish)Thread.currentThread().interrupt();
            if(publishFault.equals("io_after"))throw reused;
            if(publishFault.equals("runtime_after"))throw new SecurityException("publish runtime after update");
            if(publishFault.equals("error_after"))throw new AssertionError("publish Error after update");
        }
        public Entry inspect(String u,Plan p)throws IOException{
            same(p);check(u.equals(uri),"inspect exact inserted URI");inspects++;
            if(publishedCall){
                if(inspectFault.equals("io"))throw new IOException("confirming inspect unavailable");
                if(inspectFault.equals("shared_io"))throw reused;
                if(inspectFault.equals("runtime"))throw new SecurityException("confirming inspect denied");
                if(inspectFault.equals("error"))throw new AssertionError("confirming inspect Error");
                if(inspectFault.equals("null"))return null;
            }
            if(!exists)return null;
            return new Entry(publishedCall&&inspectFault.equals("uri")?u+"different":u,
                !(publishedCall&&inspectFault.equals("foreign")),!(publishedCall&&inspectFault.equals("mismatch")),pending,
                bytes+(publishedCall&&inspectFault.equals("bytes")?1:0));
        }
        public List<Entry> find(Plan p){same(p);finds++;return exists?Collections.singletonList(new Entry(uri,true,true,pending,bytes)):Collections.emptyList();}
        public void deletePending(String u,Plan p){same(p);check(u.equals(uri),"delete exact inserted URI");deletes++;if(pending)exists=false;}
    }
    static PhotoIdentity identity(){return new PhotoIdentity(new Object(),64,48,"independent-capture","full-native-grid","publication-review-v1",new byte[]{31,7,42},1790800000000L);}
    static PairedStore.Pair pair(PhotoTransaction tx)throws Exception{
        PairedStore.Writer writer=tx.createPair(4);double[] s=new double[64*3],h=new double[64*3];
        Arrays.fill(s,.25);Arrays.fill(h,.875);for(int y=0;y<48;y++)writer.writeRows(y,1,s,h);return writer.seal();
    }
    static final GainmapSave.QualityLimits LIMITS=new GainmapSave.QualityLimits(.05,.05,.02);
    static void readJournal(Path dir,String phase,String uri)throws Exception{
        byte[] all=Files.readAllBytes(dir.resolve("journal"));byte[] payload=Arrays.copyOf(all,all.length-32);
        check(MessageDigest.isEqual(MessageDigest.getInstance("SHA-256").digest(payload),Arrays.copyOfRange(all,all.length-32,all.length)),"durable journal checksum");
        try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(payload))){
            check(in.readInt()==0x554c5458&&in.readInt()==1,"v1 journal identity");
            check(dir.getFileName().toString().equals("tx-"+in.readUTF()),"journal transaction UUID matches directory");in.readUTF();in.readLong();
            check(in.readUTF().equals(phase)&&in.readUTF().equals(uri)&&in.available()==0,"durable exact PUBLISHING phase before fallible provider call");
        }
    }
    static void matrix(Path root,MemoCodec codec)throws Exception{
        for(String publish:new String[]{"ok","io_before","runtime_before","error_before","io_after","runtime_after","error_after"})
        for(String inspect:new String[]{"ok","io","shared_io","runtime","error","null","uri","bytes","foreign","mismatch"}){
            cases++;Path here=Files.createDirectory(root.resolve("matrix-"+cases));Store store=new Store(here);store.publishFault=publish;store.inspectFault=inspect;
            PhotoTransaction tx=PhotoTransaction.begin(here,identity(),store,1_000_000);PairedStore.Pair pair=pair(tx);
            check(tx.confirmedPublishedUri()==null,"receipt absent before any publication");
            String success=null;IOException failure=null;try{success=tx.savePair(pair,LIMITS,codec);}catch(IOException e){failure=e;}
            boolean unknown=!inspect.equals("ok"),visible=!publish.endsWith("before");
            check(Objects.equals(tx.confirmedPublishedUri(),!unknown&&visible?store.uri:null),"core receipt only for verified committed row");
            check(store.inserts==1&&store.writes==1&&store.publishes==1,"one insert/write/publication attempt");
            if(unknown){
                check(failure instanceof PhotoTransaction.PublicationUncertainException&&success==null,"unknown never classified as success or ordinary unsaved error");
                PhotoTransaction.PublicationUncertainException e=(PhotoTransaction.PublicationUncertainException)failure;
                check(e.candidateUri.equals(store.uri)&&e.transactionId.equals(tx.plan.transactionId)&&e.identitySha256.equals(tx.identity.identitySha256)&&e.expectedBytes==store.bytes,"uncertain receipt exact identity/size");
                int inserted=store.inserts,published=store.publishes,deleted=store.deletes;
                IOException again=io(()->PhotoTransaction.reconcile(store,e),"unresolved read-only reconciliation");
                check(again instanceof PhotoTransaction.PublicationUncertainException,"reconciliation preserves uncertainty type");
                check(store.inserts==inserted&&store.publishes==published&&store.deletes==deleted,"reconciliation never mutates provider");
                if(publish.startsWith("io")&&inspect.equals("shared_io"))check(e.getCause()==store.reused&&e.getSuppressed().length==0,"same exception reused as cause avoids self suppression");
            }else if(visible)check(success!=null&&success.equals(store.uri)&&failure==null,"confirmed visible success survives fallible publication return");
            else check(success==null&&failure!=null&&!(failure instanceof PhotoTransaction.PublicationUncertainException),"confirmed pending is known ordinary publication failure");
            int calls=codec.encodeCalls;io(()->tx.savePair(pair,LIMITS,codec),"repeat save after publication attempt");io(()->tx.createPair(4),"stage after publication attempt");
            check(codec.encodeCalls==calls&&store.inserts==1&&store.publishes==1,"repeat rejected before expensive codec/provider operations");
            int finds=store.finds;tx.close();tx.close();
            check(Objects.equals(tx.confirmedPublishedUri(),!unknown&&visible?store.uri:null),"receipt after close remains exact verified state");
            Path dir=here.resolve("tx-"+tx.plan.transactionId);
            if(unknown){
                check(store.exists&&Files.isRegularFile(dir.resolve("journal")),"uncertain close preserves media and recovery journal");
                check(store.deletes==0&&store.finds==finds,"uncertain close performs no media cleanup/query");
                store.inspectFault="ok";int mutations=store.inserts+store.publishes+store.deletes;
                check(PhotoTransaction.reconcile(store,(PhotoTransaction.PublicationUncertainException)failure)==(visible?PhotoTransaction.PublicationState.PUBLISHED:PhotoTransaction.PublicationState.PENDING),"later read-only state equals actual provider state");
                check(mutations==store.inserts+store.publishes+store.deletes,"resolved reconciliation is read-only");
                store.recovering=true;check(PhotoTransaction.recover(here,store)==1,"durable PUBLISHING journal recoverable after uncertain close");
            }
            check(!Files.exists(dir)&&store.exists==visible&&store.deletes==(visible?0:1),"recovery/close preserves exactly visible photo, deletes only pending");
        }
    }
    static void cancellation(Path root,MemoCodec codec)throws Exception{
        for(boolean after:new boolean[]{false,true}){
            Path here=Files.createDirectory(root.resolve("cancel-"+after));Store store=new Store(here);store.cancelWrite=!after;store.cancelPublish=after;
            PhotoTransaction tx=PhotoTransaction.begin(here,identity(),store,1_000_000);PairedStore.Pair pair=pair(tx);
            if(after)check(tx.savePair(pair,LIMITS,codec).equals(store.uri),"post-commit interrupt cannot relabel saved photo");
            else io(()->tx.savePair(pair,LIMITS,codec),"pre-publication cancel");
            check(Thread.currentThread().isInterrupted(),"cancel status before close");tx.close();check(Thread.interrupted(),"close preserves interrupt status");
            check(store.exists==after&&store.publishes==(after?1:0),"cancellation on proper side of commit");
        }
    }
    static void duplicateBeforePublication(Path root,MemoCodec codec)throws Exception{
        Path here=Files.createDirectory(root.resolve("repeat-before-publish"));Store store=new Store(here);store.writeFailure=true;
        try(PhotoTransaction tx=PhotoTransaction.begin(here,identity(),store,1_000_000)){
            PairedStore.Pair pair=pair(tx);io(()->tx.savePair(pair,LIMITS,codec),"injected write failure");
            check(tx.confirmedPublishedUri()==null,"write failure has no committed receipt");
            int calls=codec.encodeCalls;store.writeFailure=false;
            io(()->tx.savePair(pair,LIMITS,codec),"repeat transaction after provider write failure");
            check(store.inserts==1&&store.publishes==0&&codec.encodeCalls==calls,"failed save cannot reinsert/reencode/retry same UUID transaction");
        }
    }
    static void legacyRecovery(Path root)throws Exception{
        for(String phase:new String[]{"NEW","INSERTING","INSERTED","PUBLISHING","PUBLISHED"})for(boolean pending:new boolean[]{false,true}){
            Path here=Files.createDirectory(root.resolve("legacy-"+phase+"-"+pending));Store store=new Store(here);
            PendingPhotoStore.Plan plan=new PendingPhotoStore.Plan(UUID.randomUUID().toString(),String.join("",Collections.nCopies(64,"b")),1790800000000L);
            store.exact=plan;store.uri="content://independent/images/61";store.exists=true;store.pending=pending;store.bytes=817;
            Path dir=Files.createDirectory(here.resolve("tx-"+plan.transactionId));Files.createFile(dir.resolve("lock"));
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(DataOutputStream out=new DataOutputStream(bytes)){
                out.writeInt(0x554c5458);out.writeInt(1);out.writeUTF(plan.transactionId);out.writeUTF(plan.identitySha256);out.writeLong(plan.dateTakenMs);out.writeUTF(phase);out.writeUTF(phase.equals("NEW")?"":store.uri);
            }
            byte[] payload=bytes.toByteArray();try(OutputStream out=Files.newOutputStream(dir.resolve("journal"))){out.write(payload);out.write(MessageDigest.getInstance("SHA-256").digest(payload));}
            // Recovery reconstructs a value-equivalent immutable Plan after a process restart.
            PendingPhotoStore proxy=new PendingPhotoStore(){
                public String insertPending(Plan p){throw new AssertionError();}public void writeAndSync(String u,Plan p,long n,Write w){throw new AssertionError();}public void publish(String u,Plan p){throw new AssertionError();}public Entry inspect(String u,Plan p){throw new AssertionError();}
                public List<Entry> find(Plan p){check(p.transactionId.equals(plan.transactionId)&&p.identitySha256.equals(plan.identitySha256)&&p.dateTakenMs==plan.dateTakenMs,"legacy recovery immutable plan values");return store.exists?Collections.singletonList(new Entry(store.uri,true,true,store.pending,store.bytes)):Collections.emptyList();}
                public void deletePending(String u,Plan p){check(store.pending&&u.equals(store.uri),"legacy cleanup only exact pending URI");store.exists=false;store.deletes++;}
            };
            check(PhotoTransaction.recover(here,proxy)==1&&!Files.exists(dir),"old/new v1 journal phase recovery");
            check(store.exists!=pending&&store.deletes==(pending?1:0),"actual row visibility overrides stale phase name");
        }
    }
    public static void main(String[] args)throws Exception{
        Path root=Paths.get(args[0]);Files.createDirectories(root);MemoCodec codec=new MemoCodec(root.resolve("codec"));
        matrix(root,codec);cancellation(root,codec);legacyRecovery(root);duplicateBeforePublication(root,codec);
        check(codec.realEncodes==2&&codec.realDecodes==2,"both image roles independently exercised actual codec once");
        System.out.println("{\"checks\":"+checks+",\"provider_failure_cases\":"+cases+",\"actual_x265_encodes\":"+codec.realEncodes+",\"actual_ffmpeg_decodes\":"+codec.realDecodes+",\"legacy_v1_phase_cases\":10,\"android_device_execution\":false}");
    }
}

package com.hiro.ulike.hdr.photo;

import java.io.*;
import java.nio.channels.FileLock;
import java.nio.file.*;
import java.util.*;

/** Independent geometry oracle and adversarial recovery/storage review.
 * This executes real disk stages and locks, not Android MediaProvider.
 */
public final class PhotoReview {
    static int checks;
    static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    interface Throwing {void run()throws Exception;}
    static void reject(Throwing action,String why)throws Exception{
        boolean failed=false;try{action.run();}catch(IOException|IllegalArgumentException|IllegalStateException expected){failed=true;}
        check(failed,"accepted "+why);
    }
    static final class Store implements PendingPhotoStore {
        final Map<String,List<Entry>> entries=new HashMap<>();int deleted,finds;boolean failDelete;
        public String insertPending(Plan p){throw new AssertionError("review does not encode/save media");}
        public void writeAndSync(String u,Plan p,long bytes,Write write){throw new AssertionError();}
        public void publish(String u,Plan p){throw new AssertionError();}
        public Entry inspect(String u,Plan p){for(Entry e:find(p))if(e.uri.equals(u))return e;return null;}
        public List<Entry> find(Plan p){finds++;return new ArrayList<>(entries.getOrDefault(p.transactionId,Collections.emptyList()));}
        public void deletePending(String u,Plan p)throws IOException{
            if(failDelete)throw new IOException("injected provider cleanup unavailable");
            List<Entry> list=entries.get(p.transactionId);if(list==null)return;
            Iterator<Entry> i=list.iterator();while(i.hasNext()){Entry e=i.next();if(e.uri.equals(u)&&e.pending&&e.owned&&e.matching){i.remove();deleted++;}}
        }
        void add(Plan p,Entry... e){entries.put(p.transactionId,new ArrayList<>(Arrays.asList(e)));}
    }
    static PhotoIdentity identity(Object token,PhotoGeometry g){
        return new PhotoIdentity(token,g.width,g.height,"capture-exact",g.id(),"review-revision",new byte[]{5,7,9},1790800000000L);
    }
    static void journal(Path root,PendingPhotoStore.Plan plan,String phase,boolean corrupt)throws Exception{
        Path directory=Files.createDirectory(root.resolve("tx-"+plan.transactionId));Files.createFile(directory.resolve("lock"));
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream data=new DataOutputStream(bytes);
        data.writeInt(0x554c5458);data.writeInt(1);data.writeUTF(plan.transactionId);data.writeUTF(plan.identitySha256);data.writeLong(plan.dateTakenMs);data.writeUTF(phase);data.writeUTF("");data.flush();
        byte[] payload=bytes.toByteArray(),digest=java.security.MessageDigest.getInstance("SHA-256").digest(payload);if(corrupt)digest[0]^=64;
        try(OutputStream out=Files.newOutputStream(directory.resolve("journal"))){out.write(payload);out.write(digest);}
    }
    static PendingPhotoStore.Plan plan(){return new PendingPhotoStore.Plan(UUID.randomUUID().toString(),new String(new char[64]).replace('\0','a'),1790800000000L);}
    static void recovery(Path root)throws Exception{
        Store store=new Store();PendingPhotoStore.Plan corrupt=plan(),valid=plan(),published=plan(),foreign=plan();
        journal(root,corrupt,"INSERTING",true);journal(root,valid,"INSERTED",false);journal(root,published,"INSERTED",false);journal(root,foreign,"INSERTED",false);
        store.add(valid,new PendingPhotoStore.Entry("pending",true,true,true,123));
        store.add(published,new PendingPhotoStore.Entry("published",true,true,false,123));
        store.add(foreign,new PendingPhotoStore.Entry("foreign-owner",false,true,true,123),new PendingPhotoStore.Entry("wrong-plan",true,false,true,123));
        reject(()->PhotoTransaction.recover(root,store),"corrupt journal should report failure");
        check(Files.exists(root.resolve("tx-"+corrupt.transactionId).resolve("journal")),"corrupt journal retained");
        check(!Files.exists(root.resolve("tx-"+valid.transactionId)),"unrelated valid transaction recovered despite corrupt peer");
        check(store.find(valid).isEmpty()&&store.deleted==1,"only exact owned pending row deleted");
        check(store.find(published).size()==1&&!store.find(published).get(0).pending,"published image preserved");
        check(store.find(foreign).size()==2,"foreign/mismatched pending media preserved");
        Files.delete(root.resolve("tx-"+corrupt.transactionId).resolve("journal"));Files.delete(root.resolve("tx-"+corrupt.transactionId).resolve("lock"));Files.delete(root.resolve("tx-"+corrupt.transactionId));
        PendingPhotoStore.Plan active=plan();journal(root,active,"INSERTED",false);store.add(active,new PendingPhotoStore.Entry("active",true,true,true,123));
        try(RandomAccessFile file=new RandomAccessFile(root.resolve("tx-"+active.transactionId).resolve("lock").toFile(),"rw");FileLock held=file.getChannel().lock()){
            check(PhotoTransaction.recover(root,store)==0,"recovery skips an active transaction lock");check(store.find(active).size()==1,"active pending row retained");
        }
        check(PhotoTransaction.recover(root,store)==1&&store.find(active).isEmpty(),"same abandoned lock later recoverable");
        PendingPhotoStore.Plan fail=plan(),ok=plan();journal(root,fail,"INSERTED",false);journal(root,ok,"NEW",false);store.add(fail,new PendingPhotoStore.Entry("will-retry",true,true,true,123));
        store.failDelete=true;reject(()->PhotoTransaction.recover(root,store),"provider cleanup failure");
        check(Files.exists(root.resolve("tx-"+fail.transactionId).resolve("journal")),"cleanup failure retains recovery plan");
        check(!Files.exists(root.resolve("tx-"+ok.transactionId)),"unrelated empty transaction still cleaned");
        store.failDelete=false;check(PhotoTransaction.recover(root,store)==1,"provider recovery retry succeeds");
        PendingPhotoStore.Plan nested=plan();journal(root,nested,"NEW",false);Path d=root.resolve("tx-"+nested.transactionId);Files.createDirectory(d.resolve("unexpected"));
        reject(()->PhotoTransaction.recover(root,store),"unexpected nested stage directory");
        check(Files.exists(d.resolve("journal")),"failed private cleanup keeps journal until final deletion");
        Files.delete(d.resolve("unexpected"));check(PhotoTransaction.recover(root,store)==1,"private cleanup retry after diagnosis");
    }
    static double[][] forwardReference(int sw,int sh,int angle,boolean mirror){
        int w=angle%180==0?sw:sh,h=angle%180==0?sh:sw;
        double[][] values=new double[h][w];
        // Forward coordinate propagation is independent of sourcePixel's inverse mapping.
        for(int y=0;y<sh;y++)for(int x=0;x<sw;x++){
            int u=x,v=y;
            if(angle==90){u=sh-1-y;v=x;}else if(angle==180){u=sw-1-x;v=sh-1-y;}else if(angle==270){u=y;v=sw-1-x;}
            if(mirror)u=w-1-u;values[v][u]=(y*sw+x+1)/(double)(sw*sh+1);
        }return values;
    }
    static void geometry(Path root)throws Exception{
        Store store=new Store();int sw=130,sh=18;
        for(int angle:new int[]{0,90,180,270})for(boolean mirror:new boolean[]{false,true}){
            double[][] expected=forwardReference(sw,sh,angle,mirror);int outH=expected.length,outW=expected[0].length;
            PhotoGeometry g=new PhotoGeometry(sw,sh,angle,mirror,1,3,outW-4,outH-6);PhotoIdentity id=identity(new Object(),g);
            try(PhotoTransaction tx=PhotoTransaction.begin(root,id,store,2_000_000)){
                GeometryPairWriter writer=tx.createTransformedPair(g,4,24_000);check(writer.tileRows<g.height,"multi-tile geometry fixture");
                reject(()->writer.begin(new Object(),sw,sh,id.settingsSha256,g.id(),4),"foreign source token");
                writer.begin(id.exactSource,sw,sh,id.settingsSha256,g.id(),4);
                double[] s=new double[sw*3],h=new double[s.length];
                for(int y=0;y<sh;y++){
                    for(int x=0;x<sw;x++)for(int c=0;c<3;c++){
                        double value=(y*sw+x+1)/(double)(sw*sh+1);s[x*3+c]=value;h[x*3+c]=value*3;
                    }writer.writeRows(y,1,s,h);
                }
                writer.commit();PairedStore.Pair pair=writer.pair();check(pair.identity==id,"exact final identity, not source-raster staging identity");
                double[] sr=new double[g.width*3],hr=new double[sr.length];
                for(int y=0;y<g.height;y++){
                    pair.sdr().read(y,sr);pair.hdr().read(y,hr);
                    for(int x=0;x<g.width;x++)for(int c=0;c<3;c++){
                        double want=expected[y+g.top][x+g.left];
                        check(Double.doubleToRawLongBits(sr[x*3+c])==Double.doubleToRawLongBits(want),"forward-reference SDR rotation/mirror/crop");
                        check(Double.doubleToRawLongBits(hr[x*3+c])==Double.doubleToRawLongBits(want*3),"same mapping HDR pair");
                    }
                }
                reject(writer::commit,"repeated transformed commit");
            }
        }
        PhotoGeometry g=new PhotoGeometry(130,4,0,false,0,0,130,4);PhotoIdentity id=identity(new Object(),g);
        try(PhotoTransaction tx=PhotoTransaction.begin(root,id,store,200_000)){
            StoredRgb.Writer writer=tx.createRgb(StoredRgb.Precision.FP64,StoredRgb.Domain.DISPLAY_LINEAR_BT2020_SDR,1);
            double[] row=new double[390];Arrays.fill(row,1.0/3);for(int y=0;y<4;y++)writer.writeRows(y,1,row);StoredRgb.Reader reader=writer.seal();
            double[] full=new double[390];reader.readRow(0,full); // populate row cache first
            double[] span=new double[18];Arrays.fill(span,-999);reader.readSpan(0,63,4,span,3);
            for(int i=0;i<span.length;i++)check(span[i]==(i>=3&&i<15?1.0/3:-999),"cross-block bounded span leaves padding alone");
            try(RandomAccessFile mutate=new RandomAccessFile(writer.path.toFile(),"rw")){mutate.seek(4096+64L*3*8);mutate.write(0);}
            reject(()->reader.readSpan(0,63,4,span,3),"block tampering rejected even after full-row cache populated");
            reject(()->reader.readSpan(0,129,2,span,0),"span past row bounds");
        }
    }
    public static void main(String[] args)throws Exception{
        Path root=Paths.get(args[0]);Files.createDirectories(root);geometry(root);recovery(root);
        System.out.println("{\"checks\":"+checks+",\"forward_reference_geometries\":8,\"real_disk_and_lock_execution\":true,\"android_media_store_execution\":false}");
    }
}

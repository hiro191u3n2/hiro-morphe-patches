package com.hiro.ulike.hdr.photo;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/** Production implementation uses MediaStore IS_PENDING; test implementation exercises failures. */
public interface PendingPhotoStore {
    final class Plan {
        public final String transactionId,displayName,relativePath,identitySha256;
        public final long dateTakenMs;
        Plan(String id,String identitySha256,long dateTakenMs){
            if(id==null || !id.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
                    || identitySha256==null || !identitySha256.matches("[0-9a-f]{64}") || dateTakenMs<=0)throw new IllegalArgumentException("bounded transaction identity");
            transactionId=id;displayName="ULike_HDR_"+id+".heic";relativePath="DCIM/Camera/";
            this.identitySha256=identitySha256;this.dateTakenMs=dateTakenMs;
        }
    }
    final class Entry {
        public final String uri;public final boolean owned,matching,pending;public final long bytes;
        public Entry(String uri,boolean owned,boolean matching,boolean pending,long bytes){this.uri=uri;this.owned=owned;this.matching=matching;this.pending=pending;this.bytes=bytes;}
    }
    interface Write {void to(OutputStream output)throws IOException;}
    String insertPending(Plan plan)throws IOException;
    void writeAndSync(String uri,Plan plan,long expectedBytes,Write write)throws IOException;
    void publish(String uri,Plan plan)throws IOException;
    Entry inspect(String uri,Plan plan)throws IOException;
    List<Entry> find(Plan plan)throws IOException;
    /** Must recheck ownership, exact plan and pending state atomically in its deletion predicate. */
    void deletePending(String uri,Plan plan)throws IOException;
}

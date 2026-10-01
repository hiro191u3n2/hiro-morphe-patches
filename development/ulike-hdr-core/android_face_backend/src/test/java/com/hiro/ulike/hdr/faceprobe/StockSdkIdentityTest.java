package com.hiro.ulike.hdr.faceprobe;

import java.io.*;
import java.nio.file.*;

public final class StockSdkIdentityTest {
    private static int checks;
    private static void check(boolean ok,String what){checks++;if(!ok)throw new AssertionError(what);}
    private static void reject(byte[] b)throws Exception{try{StockSdkIdentity.verify(new ByteArrayInputStream(b));throw new AssertionError("unsupported binary accepted");}catch(IOException expected){checks++;}}
    public static void main(String[] args)throws Exception{
        byte[] stock=Files.readAllBytes(Paths.get(args[0]));
        StockSdkIdentity a=StockSdkIdentity.verify(new ByteArrayInputStream(stock));
        check(a.installedSha256.equals(StockSdkIdentity.ORIGINAL)&&a.canonicalSha256.equals(StockSdkIdentity.ORIGINAL),"original identity");
        byte[] patched=stock.clone();int at=0x40b864;byte[] delta={(byte)0xa6,(byte)0xe3,0x40,0x39};System.arraycopy(delta,0,patched,at,4);
        StockSdkIdentity b=StockSdkIdentity.verify(new ByteArrayInputStream(patched));
        check(b.installedSha256.equals(StockSdkIdentity.RELEASED_NV21)&&b.canonicalSha256.equals(a.installedSha256),"released patch canonical identity");
        // Split the known four-byte instruction across reads and exercise legal zero-length progress.
        InputStream split=new ByteArrayInputStream(patched){
            int calls;
            @Override public synchronized int read(byte[] out,int off,int length){
                if(++calls%11==0)return 0;
                int boundary=at+2;
                return super.read(out,off,pos<boundary?Math.min(length,boundary-pos):length);
            }
        };
        check(StockSdkIdentity.verify(split).installedSha256.equals(b.installedSha256),"split instruction and zero read");
        byte[] corrupt=patched.clone();corrupt[at+99]^=1;reject(corrupt);
        corrupt=stock.clone();corrupt[at]^=1;reject(corrupt);
        reject(new byte[16]);
        ProbeLedger ledger=new ProbeLedger(2,2);ledger.verifiedSdk(b);ledger.initialized(0);ledger.submit();
        ledger.face(new float[0][],new float[0]);ledger.rendered(2,2,4);
        ProbeLedger.Snapshot result=ledger.finish(true);
        check(result.callbacksObserved&&result.sdkLibrarySha256.equals(b.installedSha256)&&result.sdkLibraryVariant.equals(b.variant),"copied diagnostic native identity");
        check(!result.sourceCoordinateContractVerified,"native hash must not certify coordinates");
        try{ledger.verifiedSdk(a);throw new AssertionError("late identity accepted");}catch(IllegalStateException expected){checks++;}
        System.out.println("PASS StockSdkIdentity "+checks+" checks; original and published NV21 variant; no native execution");
    }
}

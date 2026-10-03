package com.hiro.ulike.composer;
import java.util.concurrent.*;
public final class InitReceiptReview182 {
 static int checks;static class Receiver{volatile long handle;}
 interface Work{void go()throws Exception;}
 static void ok(boolean x,String m){checks++;if(!x)throw new AssertionError(m);}
 static void rejects(Work w,String m)throws Exception{try{w.go();throw new AssertionError(m);}catch(IllegalStateException expected){checks++;}}
 static NativeComposerBoundary boundary(){return new NativeComposerBoundary(r->((Receiver)r).handle);}
 static void begin(NativeComposerBoundary b,Receiver r){b.beforeInit(r,4080,3060,"/test",1,2,"/resources",3,false,true,false);}
 static void finish(NativeComposerBoundary b,Receiver r){r.handle=71;b.returned(0);}
 static NativeComposerBoundary.InitializationReceipt good(NativeComposerBoundary b,Receiver r,int status)throws Exception{begin(b,r);finish(b,r);b.initCallback(r,status);return b.initializationReceipt(r);}
 public static void main(String[]a)throws Exception {
  NativeComposerBoundary b=boundary();Receiver r=new Receiver();begin(b,r);rejects(()->b.initializationReceipt(r),"entryonly");finish(b,r);rejects(()->b.initializationReceipt(r),"returnonly");b.initCallback(r,0);var receipt=b.initializationReceipt(r);receipt.requireCurrent();ok(receipt.recorderIdentity==r&&receipt.nativeHandler==71&&receipt.callbackStatus==0,"exactidentity");ok(!receipt.nativeQueueBarrierProven&&!receipt.listenerCompletionProven&&!receipt.restorationProven,"noinflatedbarrier");
  b.initCallback(r,0);rejects(receipt::requireCurrent,"duplicateinvalidatesheldreceipt");
  NativeComposerBoundary before=boundary();Receiver rb=new Receiver();begin(before,rb);rb.handle=71;Thread cb=new Thread(()->before.initCallback(rb,1));cb.start();cb.join(1000);ok(!cb.isAlive(),"callbackthreadbounded");rejects(()->before.initializationReceipt(rb),"callbackwithoutreturn");before.returned(0);ok(before.initializationReceipt(rb).callbackStatus==1,"SDKnonnegativebranchobserved");
  NativeComposerBoundary wrong=boundary();Receiver rw=new Receiver(),other=new Receiver();begin(wrong,rw);finish(wrong,rw);other.handle=71;wrong.initCallback(other,0);rejects(()->wrong.initializationReceipt(rw),"wrongreceivercannotack");rejects(()->wrong.initializationReceipt(other),"unobservedreceivercannotack");
  NativeComposerBoundary failed=boundary();Receiver rf=new Receiver();begin(failed,rf);finish(failed,rf);failed.initCallback(rf,-1);rejects(()->failed.initializationReceipt(rf),"negativecallback");
  NativeComposerBoundary mismatch=boundary();Receiver rm=new Receiver();begin(mismatch,rm);finish(mismatch,rm);rm.handle=72;mismatch.initCallback(rm,0);rejects(()->mismatch.initializationReceipt(rm),"differentcallbackhandle");
  NativeComposerBoundary zero=boundary();Receiver rz=new Receiver();begin(zero,rz);zero.initCallback(rz,0);finish(zero,rz);rejects(()->zero.initializationReceipt(rz),"zerocallbackhandle");
  NativeComposerBoundary changed=boundary();Receiver rc=new Receiver();var live=good(changed,rc,0);rc.handle=72;rejects(live::requireCurrent,"postreceiptchangedhandle");rc.handle=71;rejects(live::requireCurrent,"stickyinvalid");
  NativeComposerBoundary reuse=boundary();Receiver rr=new Receiver();var prior=good(reuse,rr,0);reuse.beforeUninit(rr);rr.handle=0;reuse.returned(0);rejects(prior::requireCurrent,"disposedreceipt");begin(reuse,rr);finish(reuse,rr);reuse.initCallback(rr,0);rejects(()->reuse.initializationReceipt(rr),"reusedreceiverneverack");
  NativeComposerBoundary unknown=boundary();Receiver ru=new Receiver();unknown.beforeUnsupportedInit(ru);ru.handle=71;unknown.returned(0);unknown.initCallback(ru,0);rejects(()->unknown.initializationReceipt(ru),"unsupportedinit");
  NativeComposerBoundary broken=boundary();Receiver re=new Receiver();begin(broken,re);re.handle=71;broken.failed(re);broken.initCallback(re,0);rejects(()->broken.initializationReceipt(re),"initexception");
  NativeComposerBoundary mutations=boundary();Receiver ra=new Receiver();var init=good(mutations,ra,0);mutations.unrecorded(ra,"unknown effect write");rejects(init::requireCurrent,"unknownmutationinvalidates");
  System.out.println("PASS InitReceiptReview182 "+checks+" bounded host checks; no native queue/render/restoration proof");
 }
}

package com.hiro.ulike.hdr.input;
import com.hiro.ulike.hdr.faceprobe.*;
import com.hiro.ulike.hdr.stillanalysis.StockStillAnalysis;
import java.lang.reflect.*;
import java.util.*;
public final class SubmissionOwnershipTest {
 private static int checks;
 private static void check(boolean v,String reason){checks++;if(!v)throw new AssertionError(reason);}
 private interface Task{void run()throws Exception;}
 private static void rejects(Task t,Class<? extends Throwable> kind)throws Exception{try{t.run();throw new AssertionError("Expected rejection");}catch(Throwable failure){if(!kind.isInstance(failure))throw failure;checks++;}}
 private static StockStillAnalysis.RenderedDiagnostic diagnostic(int w,int h,int[] pixels)throws Exception {
  Constructor<StockStillAnalysis.RenderedDiagnostic> c=StockStillAnalysis.RenderedDiagnostic.class.getDeclaredConstructor(int.class,int.class,int.class,int[].class,AnalysisCapacity.class);
  c.setAccessible(true);return c.newInstance(71,w,h,pixels,AnalysisCapacity.nativeSize4080x3060Candidate());
 }
 public static void main(String[] args)throws Exception {
  int[] released={0};Object r=new Object();SubmissionOwnership<Object> own=new SubmissionOwnership<>(r,x->{check(x==r,"exact release identity");released[0]++;});
  check(own.state()==SubmissionOwnership.State.CREATED && own.beforeTransfer()==r,"created ownership");
  SubmissionOwnership.Claim<Object> claim=own.transfer();own.close();check(released[0]==0 && claim.resource()==r,"caller close cannot free moved resource");
  rejects(own::transfer,IllegalStateException.class);rejects(own::beforeTransfer,IllegalStateException.class);
  claim.submitted();rejects(claim::submitted,IllegalStateException.class);claim.joined(true);own.close();
  check(released[0]==1 && own.state()==SubmissionOwnership.State.CLOSED,"one release after join");rejects(claim::resource,IllegalStateException.class);rejects(()->claim.joined(true),IllegalStateException.class);
  SubmissionOwnership<Object> unsafe=new SubmissionOwnership<>(r,x->{throw new AssertionError("Quarantined input must not free");});SubmissionOwnership.Claim<Object> live=unsafe.transfer();live.submitted();
  live.joined(false);unsafe.close();check(unsafe.state()==SubmissionOwnership.State.QUARANTINED,"failed native join quarantine");rejects(unsafe::transfer,IllegalStateException.class);rejects(()->live.joined(true),IllegalStateException.class);
  SubmissionOwnership<Object> unused=new SubmissionOwnership<>(r,x->released[0]++);unused.close();unused.close();check(released[0]==2,"untransferred failure frees once");
  SubmissionOwnership<Object> failedRelease=new SubmissionOwnership<>(r,x->{throw new IllegalStateException("recycle failed");});SubmissionOwnership.Claim<Object> failing=failedRelease.transfer();
  rejects(()->failing.joined(true),IllegalStateException.class);failedRelease.close();check(failedRelease.state()==SubmissionOwnership.State.QUARANTINED,"failed release quarantines");
  SubmissionOwnership<Object> beforeSubmit=new SubmissionOwnership<>(r,x->released[0]++);beforeSubmit.transfer().joined(true);check(released[0]==3,"init error before submit may join release");
  int[] sdk={1,2,3,4};int[] copied=OwnedRenderPixels.copy(sdk,2,2,2,2,AnalysisCapacity.nativeSize4080x3060Candidate());sdk[0]=8;check(copied[0]==1,"callback clone independent");
  StockStillAnalysis.RenderedDiagnostic d=diagnostic(2,2,copied);byte[] mask=d.consume((pixels,w,h)->{check(pixels==copied,"consuming conversion uses sole owned array");byte[] out=new byte[pixels.length];for(int i=0;i<out.length;i++)out[i]=(byte)pixels[i];return out;});
  check(mask[0]==1 && Arrays.stream(copied).allMatch(x->x==0),"converted owned mask with source wipe");rejects(d::pixels,IllegalStateException.class);rejects(()->d.consume((px,w,h)->null),IllegalStateException.class);
  int[] alias={1,2,3,4};StockStillAnalysis.RenderedDiagnostic aliasD=diagnostic(2,2,alias);rejects(()->aliasD.consume((px,w,h)->px),IllegalArgumentException.class);check(Arrays.stream(alias).allMatch(x->x==0),"alias rejection wipes");
  int[] failedPixels={1,2,3,4};StockStillAnalysis.RenderedDiagnostic failure=diagnostic(2,2,failedPixels);rejects(()->failure.consume((px,w,h)->{throw new OutOfMemoryError("test");}),OutOfMemoryError.class);check(Arrays.stream(failedPixels).allMatch(x->x==0),"OOM conversion wipe");
  int[] cancelled={1,2,3,4};StockStillAnalysis.RenderedDiagnostic cancelledD=diagnostic(2,2,cancelled);Thread.currentThread().interrupt();try{rejects(()->cancelledD.consume((px,w,h)->null),InterruptedException.class);check(Thread.currentThread().isInterrupted(),"cancel flag preserved");}finally{Thread.interrupted();}check(Arrays.stream(cancelled).allMatch(x->x==0),"cancel wipes");
  int[] nativePixels=new int[4080*3060];StockStillAnalysis.RenderedDiagnostic nativeD=diagnostic(4080,3060,nativePixels);rejects(nativeD::pixels,IllegalStateException.class);nativeD.consume((px,w,h)->{check(px==nativePixels && w==4080 && h==3060,"no 4MP clamp in consume");return null;});
  ProbeLedger ledger=new ProbeLedger(4080,3060,AnalysisCapacity.nativeSize4080x3060Candidate());ledger.initialized(0);ledger.submit();ledger.face(new float[0][],new float[0]);ledger.rendered(4080,3060,4080*3060);check(ledger.finish(true).callbacksObserved,"full grid callback ledger");
  ProbeLedger dup=new ProbeLedger(4080,3060,AnalysisCapacity.nativeSize4080x3060Candidate());dup.initialized(0);dup.submit();dup.face(new float[0][],new float[0]);dup.rendered(4080,3060,4080*3060);dup.rendered(4080,3060,4080*3060);check(!dup.finish(true).callbacksObserved,"duplicate full grid rejected");
  ProbeLedger stale=new ProbeLedger(4080,3060,AnalysisCapacity.nativeSize4080x3060Candidate());stale.rendered(4080,3060,4080*3060);check(!stale.finish(true).callbacksObserved,"pre-submit stale callback rejected");
  System.out.println("PASS "+checks+" submission ownership/callback consumption checks; device execution=false");
 }
}

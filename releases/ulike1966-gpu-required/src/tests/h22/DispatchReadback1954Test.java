package com.hiro.ulike;

import java.io.*;
import java.lang.reflect.Constructor;
import java.util.Arrays;

/** H22/H23 execute the production shader and JNI. Pixel fixtures are generated
 * by independently compiled, byte-pinned pre-GPU Java, never by this backend. */
public final class DispatchReadback1954Test {
 private DispatchReadback1954Test() {}
 private static long h22,h23,pixels;
 private static int cases;
 private static final int SENTINEL=0x13579bdf;
 private static final int[] DISPATCH={32,64,0};
 private static final Constructor<QualityPixels1932.Plan> PLAN;
 private static final Constructor<FinishPolicy1953.Band> BAND;
 static {try {
  PLAN=QualityPixels1932.Plan.class.getDeclaredConstructor(int.class,int.class,int.class,int.class,
   int.class,int.class,int.class,float.class,float.class,boolean.class,boolean.class,boolean.class,
   SpatialNoise1934.class,boolean.class,QualityPixels1932.RegionMask.class);PLAN.setAccessible(true);
  BAND=FinishPolicy1953.Band.class.getDeclaredConstructor(int.class,int[].class,int.class,boolean.class);BAND.setAccessible(true);
 }catch(Exception failed){throw new ExceptionInInitializerError(failed);}}
 private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 private static void check22(boolean ok,String message){check(ok,message);h22++;}
 private static void check23(boolean ok,String message){check(ok,message);h23++;}
 private static long number(String json,String key){
  java.util.regex.Matcher m=java.util.regex.Pattern.compile("\\\""+key+"\\\":([0-9]+)").matcher(json);
  if(!m.find())throw new AssertionError("Missing native counter "+key+" in "+json);return Long.parseLong(m.group(1));
 }
 private static int[] read(DataInputStream input,int n)throws Exception {
  int[] a=new int[n];for(int i=0;i<n;i++)a[i]=input.readInt();return a;
 }
 private static QualityPixels1932.Plan plan(int gain,int floor,int limit,boolean texture,boolean halo)throws Exception {
  return PLAN.newInstance(2,gain>0?1:0,0,0,gain,floor,limit,0f,1f,texture,false,halo,null,false,null);
 }
 private static FinishPolicy1953.Band policy(int[] values,int count,int variant)throws Exception {
  boolean uniform=true,fits=true;
  for(int i=0;i<count;i++)for(int c=0;c<4;c++){
   int value=values[i*4+c];fits&=(value&~65535)==0;
   if(i>0 && value!=values[c])uniform=false;
  }
  int mode=uniform && variant==2?2:fits && variant!=0?1:0;int[] words;
  if(mode==2)words=Arrays.copyOf(values,4);
  else if(mode==1){words=new int[count*2];for(int i=0;i<count;i++){
   words[i*2]=values[i*4]|(values[i*4+1]<<16);words[i*2+1]=values[i*4+2]|(values[i*4+3]<<16);
  }}else words=Arrays.copyOf(values,count*4);
  return BAND.newInstance(mode,words,count,mode==0);
 }
 private static int dispatchCount(int first,int last,int rows,boolean moire,boolean sharp,boolean sequential,int height){
  if(first==last)return 0;
  int count=0,block=rows==0?last-first:rows,corrected=sequential&&sharp?Math.max(0,first-4):first;
  for(int start=first;start<last;start+=block){
   int end=Math.min(last,start+block);count++;
   if(moire&&sharp){
    int needed=sequential?Math.min(height,end+4):end;
    if(needed>corrected)count++;
    corrected=needed;
   }
  }
  return count;
 }
 private static String fixtureRun(int[] source,int[] expected,int[] values,int w,int h,int first,int last,
        QualityPixels1932.Plan p,boolean moire,boolean sharp,boolean sequential,int dispatch,
        boolean direct,int[] actual)throws Exception {
  GpuFinish1953.Session session=GpuFinish1953.open(w,h,p,moire,sharp,sequential,256,dispatch);
  check(session!=null,"Dispatch session unavailable rows="+dispatch);
  if(direct)h23++;else h22++;
  Arrays.fill(actual,SENTINEL);int[] before=source.clone();String stats;
  try {
   if(first<last){
    int origin=Math.max(0,first-session.halo()),bottom=Math.min(h,last+session.halo());
    int[] lease=Arrays.copyOfRange(source,origin*w,bottom*w);
    FinishPolicy1953.Band band=policy(values,w*(last-first),cases%3);
    GpuFinish1953.Ticket ticket;
    try {ticket=session.submit(lease,origin,bottom-origin,first,last,band);}finally {band.close();}
    check(ticket!=null,"Dispatch fixture submit declined");if(direct)h23++;else h22++;
    Arrays.fill(lease,0x5a5a5a5a);
    boolean collected=direct?session.collectCandidate(ticket,actual,first*w):session.collect(ticket,actual,first*w);
    check(collected,"Dispatch fixture collect declined");if(direct)h23++;else h22++;
    int[] old=actual.clone();
    boolean duplicate=direct?session.collectCandidate(ticket,actual,first*w):session.collect(ticket,actual,first*w);
    check(!duplicate && Arrays.equals(old,actual),"Duplicate collect mutated destination");if(direct)h23++;else h22++;
   }
   stats=session.stats();
   check(number(stats,"dispatch_rows")==dispatch,"Requested dispatch size lost");if(direct)h23++;else h22++;
   check(number(stats,"dispatch_count")==dispatchCount(first,last,dispatch,moire,sharp,sequential,h),"Dispatch count differs case="+cases+" first="+first+" last="+last+" moire="+moire+" sharp="+sharp+" "+stats);if(direct)h23++;else h22++;
   check(number(stats,"direct_candidate_collections")==((direct&&first<last)?1:0),"Direct counter differs");if(direct)h23++;else h22++;
   check(number(stats,"staging_collection_memcpy_bytes")==((direct||first==last)?0L:(long)w*(last-first)*4L),"Staging memcpy counter differs");if(direct)h23++;else h22++;
   check(Arrays.equals(source,before),"Fixture source changed");if(direct)h23++;else h22++;
  }finally {session.close();}
  check(session.stats().contains("\"close_drained\":true"),"Fixture session not drained");if(direct)h23++;else h22++;
  return stats;
 }
 private static void fixtures(String filename,boolean seq)throws Exception {
  try(DataInputStream input=new DataInputStream(new BufferedInputStream(new FileInputStream(filename)))){
   while(input.available()>0){
    int w=input.readInt(),h=input.readInt(),first=input.readInt(),last=input.readInt(),moire=input.readInt(),sharp=input.readInt();
    int gain=input.readInt(),floor=input.readInt(),limit=input.readInt(),texture=input.readInt(),halo=input.readInt();input.readInt();
    int n=w*h,count=w*(last-first);int[] source=read(input,n),expected=read(input,n),values=read(input,Math.max(1,count)*4);
    if(seq&&moire==0)continue;
    QualityPixels1932.Plan p=plan(gain,floor,limit,texture!=0,halo!=0);
    for(int dispatch:DISPATCH){
     int[] staged=new int[n],direct=new int[n];
     fixtureRun(source,expected,values,w,h,first,last,p,moire!=0,sharp!=0,seq,dispatch,false,staged);
     fixtureRun(source,expected,values,w,h,first,last,p,moire!=0,sharp!=0,seq,dispatch,true,direct);
     for(int i=0;i<n;i++){
      check22(staged[i]==expected[i],"Dispatch oracle mismatch rows="+dispatch+" case="+cases+" pixel="+i);
      check23(direct[i]==expected[i]&&direct[i]==staged[i],"Direct candidate oracle mismatch rows="+dispatch+" case="+cases+" pixel="+i);
     }
     cases++;pixels+=n;
    }
   }
  }
 }
 private static int[] source(int w,int h,int seed){
  int[] a=new int[w*h];for(int y=0;y<h;y++)for(int x=0;x<w;x++){
   int g=123+(x+y+seed)%9,r=g+((x+seed)%4<2?23:-23),b=g-(r-g);
   int alpha=(x+y*13+seed)%101==0?73:255;a[y*w+x]=(alpha<<24)|(r<<16)|(g<<8)|b;
  }return a;
 }
 private static QualityPixels1932.Plan actualPlan(final int seed){
  return QualityPixels1932.plan(new QualityPixels1932.NoiseStats(1.3f,1.1f,128f,0f,144),
   200,10000000L,QualityPixels1932.LENS_WIDE,.7f,4,4,true,true,1f)
   .withFaceRegions(new QualityPixels1932.RegionMask(){
    public int skinQ8(int x,int y){return (x*7+y*11+seed)&255;}
    public int detailQ8(int x,int y){return 0;}
   });
 }
 private static int[] reference(int[] source,int w,int h,QualityPixels1932.Plan p){
  int[] mid=new int[source.length],out=new int[source.length];
  QualityPixels1932.finishStripAtBefore1951(source,mid,w,h,0,h,p,true,false,0);
  QualityPixels1932.finishStripAtBefore1951(mid,out,w,h,0,h,p,false,true,0);return out;
 }
 private static GpuFinish1953.Ticket submit(GpuFinish1953.Session session,int[] input,int w,int h,
        int first,QualityPixels1932.Plan p){
  int last=Math.min(h,first+session.coreRows()),origin=Math.max(0,first-session.halo()),bottom=Math.min(h,last+session.halo());
  FinishPolicy1953.Band policy=FinishPolicy1953.prepare(p,w,h,first,last,0);
  check(policy!=null,"Full-photo policy unavailable");
  int[] lease=Arrays.copyOfRange(input,origin*w,bottom*w);GpuFinish1953.Ticket ticket;
  try {ticket=session.submit(lease,origin,bottom-origin,first,last,policy);}finally {policy.close();}
  Arrays.fill(lease,0x5a5a5a5a);return ticket;
 }
 private static String photo(int preferred,int dispatch,boolean direct)throws Exception {
  int w=preferred==256?97:103,h=preferred==256?1103:1129;
  int[] input=source(w,h,7),before=input.clone();QualityPixels1932.Plan p=actualPlan(7);
  int[] expected=reference(input,w,h,p),output=new int[input.length];Arrays.fill(output,SENTINEL);
  GpuFinish1953.Session session=GpuFinish1953.open(w,h,p,true,true,true,preferred,dispatch);
  check(session!=null,"Full-photo session unavailable");if(direct)h23++;else h22++;
  check(session.halo()==36,"Sequential halo changed");if(direct)h23++;else h22++;
  GpuFinish1953.Ticket[] pending=new GpuFinish1953.Ticket[2];int submitted=0,collected=0,commands=0;
  String stats;
  try {
   for(int first=0;first<h;first+=session.coreRows()){
    if(submitted-collected==2){
     GpuFinish1953.Ticket old=pending[collected%2];
     check(direct?session.collectCandidate(old,output,old.first*w):session.collect(old,output,old.first*w),"Full-photo collect failed");
     if(direct)h23++;else h22++;collected++;
    }
    GpuFinish1953.Ticket ticket=submit(session,input,w,h,first,p);
    check(ticket!=null,"Full-photo submit failed");if(direct)h23++;else h22++;
    pending[submitted%2]=ticket;submitted++;
    commands+=dispatchCount(first,ticket.last,dispatch,true,true,true,h);
   }
   while(collected<submitted){
    GpuFinish1953.Ticket old=pending[collected%2];
    check(direct?session.collectCandidate(old,output,old.first*w):session.collect(old,output,old.first*w),"Tail collect failed");
    if(direct)h23++;else h22++;collected++;
   }
   for(int i=0;i<input.length;i++){
    check(output[i]==expected[i],"Sequential full-photo mismatch rows="+dispatch+" direct="+direct+" pixel="+i);
    if(direct)h23++;else h22++;
   }
   stats=session.stats();
   check(number(stats,"source_uploaded_pixels")==input.length,"Repeated source upload");if(direct)h23++;else h22++;
   check(number(stats,"dispatch_count")==commands,"Full-photo dispatch count differs");if(direct)h23++;else h22++;
   check(number(stats,"peak_inflight")==2&&number(stats,"submits_before_collect")>0&&number(stats,"submit_completion_waits")==0,
    "Bounded CPU/GPU overlap lost");if(direct)h23++;else h22++;
   check(number(stats,"direct_candidate_collections")== (direct?submitted:0),"Direct collection count differs");if(direct)h23++;else h22++;
   check(number(stats,"staging_collection_memcpy_bytes")== (direct?0L:(long)input.length*4L),"Photo staging bytes differ");if(direct)h23++;else h22++;
   check(Arrays.equals(input,before),"Photo source changed");if(direct)h23++;else h22++;
  }finally {session.close();}
  check(session.stats().contains("\"close_drained\":true"),"Photo close failed");if(direct)h23++;else h22++;
  return stats;
 }
 private static void cancellation(int dispatch)throws Exception {
  int w=73,h=1100;int[] input=source(w,h,13);QualityPixels1932.Plan p=actualPlan(13);
  GpuFinish1953.Session session=GpuFinish1953.open(w,h,p,true,true,true,256,dispatch);
  check22(session!=null,"Cancel session unavailable");
  GpuFinish1953.Ticket a=submit(session,input,w,h,0,p),b=submit(session,input,w,h,256,p);
  check22(a!=null&&b!=null,"Cancel submit failed");
  Thread.currentThread().interrupt();session.close();
  check22(Thread.currentThread().isInterrupted(),"Cancellation cleared caller interrupt");Thread.interrupted();
  check22(session.stats().contains("\"close_drained\":true")&&number(session.stats(),"forced_drains")==2,
   "Cancellation did not drain both slots");
  int[] candidate=new int[w*256];Arrays.fill(candidate,SENTINEL);int[] before=candidate.clone();
  check23(!session.collectCandidate(a,candidate,0)&&Arrays.equals(candidate,before),"Cancelled private ticket mutated output");
 }
 private static native void failNextUnmap();
 private static void lateFailure(boolean direct)throws Exception {
  int w=79,h=1100;int[] source=source(w,h,17),before=source.clone();QualityPixels1932.Plan p=actualPlan(17);
  GpuFinish1953.Session session=GpuFinish1953.open(w,h,p,true,true,true,256,64);
  check23(session!=null,"Failure session unavailable");
  GpuFinish1953.Ticket first=submit(session,source,w,h,0,p),second=submit(session,source,w,h,256,p);
  check23(first!=null&&second!=null,"Failure two-slot submit declined");
  int[] privateCandidate=new int[w*256+8];Arrays.fill(privateCandidate,SENTINEL);
  int[] clean=privateCandidate.clone();failNextUnmap();
  boolean success=direct?session.collectCandidate(first,privateCandidate,4):session.collect(first,privateCandidate,4);
  check23(!success,"Faulted unmap was reported successful");
  check23(Arrays.equals(source,before),"After-copy failure changed original photograph");
  if(direct){
   check23(!Arrays.equals(privateCandidate,clean),"Fault seam did not execute after direct Java-array copy");
   int[] expected=reference(source,w,h,p);
   for(int i=0;i<w*256;i++)check23(privateCandidate[i+4]==expected[i],"Private partial candidate was not copied exactly");
   for(int i=0;i<4;i++)check23(privateCandidate[i]==SENTINEL&&privateCandidate[privateCandidate.length-1-i]==SENTINEL,
    "Private failed copy crossed requested range");
  }else check23(Arrays.equals(privateCandidate,clean),"Legacy staged collect published on unmap failure");
  // This private array has no publication path. A failed return requires its
  // owner to discard it; the actual pipeline fallback is checked separately.
  session.close();String stats=session.stats();
  check23(stats.contains("\"close_drained\":false")&&number(stats,"forced_drains")==1,
   "Failure did not drain remaining ticket and quarantine uncertain slot");
  check23(!session.collectCandidate(second,privateCandidate,0),"Failed session published remaining private ticket");
  System.out.println("FAILURE1954_PASS direct="+direct+" h23="+h23+" "+stats);
 }
 public static void main(String[] args)throws Exception {
  if(args.length==1){lateFailure(args[0].equals("direct-failure"));return;}
  fixtures(args[0],false);fixtures(args[1],true);
  for(int preferred:new int[]{256,512})for(int rows:DISPATCH){
   String staged=photo(preferred,rows,false),direct=photo(preferred,rows,true);
   System.out.println("DISPATCH_COUNTERS core="+preferred+" rows="+rows+" direct=false "+staged);
   System.out.println("DISPATCH_COUNTERS core="+preferred+" rows="+rows+" direct=true "+direct);
  }
  for(int rows:DISPATCH)cancellation(rows);
  check22(GpuFinish1953.open(97,1103,actualPlan(1),true,true,true,256,31)==null,
   "Unreviewed dispatch option admitted");
  System.out.println("DISPATCH1954_PASS cases="+cases+" pixels="+pixels+" h22="+h22+" h23="+h23);
 }
}

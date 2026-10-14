package com.hiro.ulike;
import java.io.*;
import java.lang.reflect.Constructor;
import java.util.Arrays;

/** Executes production session JNI and GLSL against separately compiled .52 Java. */
public final class FinishSession1953Test {
 private FinishSession1953Test() {}
 private static long assertions16,pixels;
 private static final java.util.concurrent.atomic.AtomicLong assertions18=new java.util.concurrent.atomic.AtomicLong();
 private static final java.util.concurrent.atomic.AtomicLong assertions19=new java.util.concurrent.atomic.AtomicLong();
 private static int cases,dispatches,sequences,raw,packed,constant;
 private static final Constructor<QualityPixels1932.Plan> PLAN;
 private static final Constructor<FinishPolicy1953.Band> BAND;
 static {try {
  PLAN=QualityPixels1932.Plan.class.getDeclaredConstructor(int.class,int.class,int.class,int.class,
   int.class,int.class,int.class,float.class,float.class,boolean.class,boolean.class,boolean.class,
   SpatialNoise1934.class,boolean.class,QualityPixels1932.RegionMask.class);PLAN.setAccessible(true);
  BAND=FinishPolicy1953.Band.class.getDeclaredConstructor(int.class,int[].class,int.class,boolean.class);BAND.setAccessible(true);
 }catch(Exception failed){throw new ExceptionInInitializerError(failed);}}
 private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 private static int[] read(DataInputStream input,int n)throws Exception {int[] a=new int[n];for(int i=0;i<n;i++)a[i]=input.readInt();return a;}
 private static QualityPixels1932.Plan plan(int gain,int floor,int limit,boolean texture,boolean halo)throws Exception {
  return PLAN.newInstance(2,gain>0?1:0,0,0,gain,floor,limit,0f,1f,texture,false,halo,null,false,null);
 }
 private static FinishPolicy1953.Band policy(int[] values,int count,int variant)throws Exception {
  boolean uniform=true,fits=true;
  for(int i=0;i<count;i++)for(int c=0;c<4;c++){
   int value=values[i*4+c];fits&=(value&~65535)==0;
   if(i>0 && value!=values[c])uniform=false;
  }
  int mode=uniform && variant==2?2:fits && variant!=0?1:0;
  int[] words;
  if(mode==2){words=Arrays.copyOf(values,4);constant++;}
  else if(mode==1){words=new int[count*2];for(int i=0;i<count;i++){
   words[i*2]=values[i*4]|(values[i*4+1]<<16);words[i*2+1]=values[i*4+2]|(values[i*4+3]<<16);
  }packed++;}
  else {words=Arrays.copyOf(values,count*4);raw++;}
  return BAND.newInstance(mode,words,count,mode==0);
 }
 private static long number(String json,String key){
  java.util.regex.Matcher m=java.util.regex.Pattern.compile("\\\""+key+"\\\":([0-9]+)").matcher(json);
  if(!m.find())throw new AssertionError("Missing native counter "+key+" in "+json);return Long.parseLong(m.group(1));
 }
 private static void fixture(String filename,boolean seq)throws Exception {
  try(DataInputStream input=new DataInputStream(new BufferedInputStream(new FileInputStream(filename)))){
   while(input.available()>0){
    int w=input.readInt(),h=input.readInt(),first=input.readInt(),last=input.readInt(),moire=input.readInt(),sharp=input.readInt();
    int gain=input.readInt(),floor=input.readInt(),limit=input.readInt(),texture=input.readInt(),halo=input.readInt(),origin=input.readInt();
    int n=w*h,count=w*(last-first);int[] source=read(input,n),expected=read(input,n),values=read(input,Math.max(1,count)*4);
    if(seq && moire==0)continue;
    QualityPixels1932.Plan p=plan(gain,floor,limit,texture!=0,halo!=0);
    GpuFinish1953.Session session=GpuFinish1953.open(w,h,p,moire!=0,sharp!=0,seq,256);
    require(session!=null,"Session open failed fixture "+cases);
    int[] output=new int[n];Arrays.fill(output,0x13579bdf);int[] before=source.clone();
    try {
     if(first==last){
      // Empty consumed bands decline without a dispatch or destination write.
      FinishPolicy1953.Band band=policy(values,1,0);
      try {require(session.submit(new int[0],first,0,first,last,band)==null,"Empty submit accepted");assertions16++;}
      finally {band.close();}
     }else{
      int start=Math.max(0,first-session.halo()),end=Math.min(h,last+session.halo());
      int[] slice=Arrays.copyOfRange(source,start*w,end*w);
      FinishPolicy1953.Band band=policy(values,count,cases%3);
      GpuFinish1953.Ticket ticket;
      try {ticket=session.submit(slice,start,end-start,first,last,band);}
      finally {band.close();}
      require(ticket!=null && ticket.first==first && ticket.last==last,"Ticket metadata changed");assertions16++;
      // Submit copied all Java data before return. Immediate mutation/release
      // of the upload slice must not change in-flight GPU source pixels.
      Arrays.fill(slice,0x5a13579b);
      require(session.collect(ticket,output,first*w),"Session collect failed "+cases);assertions16++;
      require(!session.collect(ticket,output,first*w),"Already collected ticket accepted");assertions16++;
      dispatches++;
     }
     for(int i=0;i<n;i++){
      require(output[i]==expected[i],"Exact .52 fixture mismatch case="+cases+" seq="+seq+" x="+i%w+" y="+i/w+
       " actual="+Integer.toHexString(output[i])+" expected="+Integer.toHexString(expected[i]));assertions16++;
     }
     require(Arrays.equals(source,before),"Original source changed");assertions16++;
     String stats=session.stats();require(number(stats,"context_binds")==1,"Per-band/photo context rebind");assertions16++;
    }finally {session.close();}
    require(session.stats().contains("\"close_drained\":true"),"Photo close did not drain");assertions16++;
    cases++;pixels+=n;if(seq)sequences++;
   }
  }
 }
 private static int[] source(int w,int h,int seed){
  int[] pixels=new int[w*h];
  for(int y=0;y<h;y++)for(int x=0;x<w;x++){
   int green=123+(x+y+seed)%9,red=green+((x+seed)%4<2?23:-23),blue=green-(red-green);
   int alpha=(x+y*13+seed)%101==0?73:255;pixels[y*w+x]=(alpha<<24)|(red<<16)|(green<<8)|blue;
  }return pixels;
 }
 private static QualityPixels1932.Plan actualPlan(final int seed){
  return QualityPixels1932.plan(new QualityPixels1932.NoiseStats(1.3f+seed*.1f,1.1f,128f,0f,144),
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
 private static void write(int[] output,int w,GpuFinish1953.Ticket t,int[] band){
  System.arraycopy(band,0,output,t.first*w,w*(t.last-t.first));
 }
 private static String photo(int w,int h,int preferred,int seed)throws Exception {
  int[] input=source(w,h,seed),before=input.clone();QualityPixels1932.Plan p=actualPlan(seed);
  int[] expected=reference(input,w,h,p),output=new int[input.length];
  GpuFinish1953.Session session=GpuFinish1953.open(w,h,p,true,true,true,preferred);
  require(session!=null,"Photo session unavailable");assertions18.incrementAndGet();
  require(session.coreRows()==Math.min(h,preferred),"Unexpected adaptive core");assertions18.incrementAndGet();
  require(GpuFinish1953.retainedBytes()>=(long)w*session.coreRows()*16,"Native growth was not pre-reserved");assertions18.incrementAndGet();
  GpuFinish1953.Ticket[] pending=new GpuFinish1953.Ticket[2];int submitted=0,collected=0;
  long inputRows=0;String counters="";
  try {
   for(int first=0;first<h;first+=session.coreRows()){
    if(submitted-collected==2){
     GpuFinish1953.Ticket old=pending[collected%2];int[] band=new int[w*(old.last-old.first)];
     require(session.collect(old,band,0),"Two-slot collect failed");assertions19.incrementAndGet();
     write(output,w,old,band);pending[collected%2]=null;collected++;
    }
    int last=Math.min(h,first+session.coreRows()),origin=Math.max(0,first-session.halo()),bottom=Math.min(h,last+session.halo());
    inputRows+=bottom-origin;int[] bandSource=Arrays.copyOfRange(input,origin*w,bottom*w);
    FinishPolicy1953.Band policy=FinishPolicy1953.prepare(p,w,h,first,last,0);
    require(policy!=null,"Policy unavailable");assertions19.incrementAndGet();
    GpuFinish1953.Ticket t;
    try {t=session.submit(bandSource,origin,bottom-origin,first,last,policy);}
    finally {policy.close();}
    require(t!=null,"Two-slot submit failed");assertions19.incrementAndGet();
    Arrays.fill(bandSource,0x55555555);pending[submitted%2]=t;submitted++;
   }
   while(collected<submitted){
    GpuFinish1953.Ticket old=pending[collected%2];int[] band=new int[w*(old.last-old.first)];
    require(session.collect(old,band,0),"Tail collect failed");assertions19.incrementAndGet();
    write(output,w,old,band);collected++;
   }
   for(int i=0;i<output.length;i++){
    require(output[i]==expected[i],"Resident-halo/overlap pixels changed seed="+seed+" pixel="+i);assertions18.incrementAndGet();
   }
   require(Arrays.equals(input,before),"Photo source mutated");assertions18.incrementAndGet();
   counters=session.stats();
   require(number(counters,"source_uploaded_pixels")==input.length,"Repeated source rows still CPU uploaded");assertions18.incrementAndGet();
   require(number(counters,"halo_copied_pixels")==inputRows*w-input.length,"Incorrect halo copy counter");assertions18.incrementAndGet();
   require(number(counters,"cross_photo_halo_reuses")==0,"Cross-photo cache");assertions18.incrementAndGet();
   require(number(counters,"peak_inflight")==2,"Second bounded slot not exercised");assertions19.incrementAndGet();
   require(number(counters,"submits_before_collect")>0,"Submit waited for earlier collect");assertions19.incrementAndGet();
   require(number(counters,"submit_completion_waits")==0,"Submit performed fence completion wait");assertions19.incrementAndGet();
   require(number(counters,"fence_waits")==submitted,"Fence collected count incorrect");assertions19.incrementAndGet();
  }finally {session.close();}
  require(session.stats().contains("\"close_drained\":true"),"Tail close failed");assertions19.incrementAndGet();
  return counters;
 }
 private static void cancel()throws Exception {
  int w=101,h=1100;int[] input=source(w,h,13);QualityPixels1932.Plan p=actualPlan(13);
  GpuFinish1953.Session session=GpuFinish1953.open(w,h,p,true,true,true,256);
  require(session!=null,"Cancel session unavailable");assertions19.incrementAndGet();
  GpuFinish1953.Ticket[] tickets=new GpuFinish1953.Ticket[2];
  for(int band=0;band<2;band++){
   int first=band*session.coreRows(),last=first+session.coreRows(),origin=Math.max(0,first-session.halo()),bottom=last+session.halo();
   FinishPolicy1953.Band policy=FinishPolicy1953.prepare(p,w,h,first,last,0);
   try {tickets[band]=session.submit(Arrays.copyOfRange(input,origin*w,bottom*w),origin,bottom-origin,first,last,policy);}
   finally {policy.close();}
   require(tickets[band]!=null,"Cancel submit failed");assertions19.incrementAndGet();
  }
  Thread.currentThread().interrupt();session.close();
  require(Thread.currentThread().isInterrupted(),"Close lost caller interrupt");assertions19.incrementAndGet();
  Thread.interrupted();
  String report=session.stats();require(report.contains("\"close_drained\":true") && number(report,"forced_drains")==2,
   "Closing failed to drain every submitted slot");assertions19.incrementAndGet();
  int[] output=new int[w*256];Arrays.fill(output,0x14578abc);int[] old=output.clone();
  require(!session.collect(tickets[0],output,0) && Arrays.equals(output,old),"Closed ticket published pixels");assertions19.incrementAndGet();
 }
 public static void main(String[] args)throws Exception {
  require(GpuFinish1953.environment1953().isEmpty(),"Foreground query initialized context");assertions16++;
  require(GpuFinish1953.retainedBytes()==0,"Uninitialized native resources claimed allocated");assertions16++;
  fixture(args[0],false);fixture(args[1],true);
  require(raw>0 && packed>0 && constant>0,"Lossless policy GPU variants not executed");assertions16++;
  String env=GpuFinish1953.environment1953();
  require(env.contains("abi=19531") && env.contains("source=") && env.contains("vendor=") && env.contains("renderer=") &&
   env.contains("glsl=") && env.contains("storage="),"Actual strict GPU environment missing");assertions16++;
  String a=photo(97,1103,256,1),b=photo(103,1129,512,2);
  // Fresh photo source differs at every coordinate, while native pool storage is
  // retained. Full equality and exactly once source upload prove token reset.
  String c=photo(97,1103,256,9);
  require(number(a,"token")!=number(c,"token"),"Photo token reused");assertions18.incrementAndGet();
  cancel();
  final Throwable[] failures=new Throwable[2];Thread[] threads=new Thread[2];
  for(int i=0;i<2;i++){final int t=i;threads[i]=new Thread(new Runnable(){public void run(){
   try {photo(89+t*8,1071+t*29,256,21+t);}catch(Throwable failure){failures[t]=failure;}
  }});threads[i].start();}
  for(Thread t:threads)t.join();for(Throwable failure:failures)require(failure==null,"Concurrent photographs: "+failure);assertions19.addAndGet(2);
  System.out.println("SESSION1953_PASS cases="+cases+" dispatched="+dispatches+" sequential="+sequences+" pixels="+pixels+
   " raw="+raw+" packed="+packed+" constant="+constant+" h16="+assertions16+" h18="+assertions18.get()+" h19="+assertions19.get());
  System.out.println("COUNTERS256 "+a);System.out.println("COUNTERS512 "+b);System.out.println("ENV "+env);
  System.out.println("NATIVE_RETAINED "+GpuFinish1953.retainedBytes());
 }
}

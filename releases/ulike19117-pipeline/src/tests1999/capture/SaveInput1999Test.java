package com.hiro.ulike;
import android.graphics.Bitmap;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Execute actual submit/copy/queue/encode/receipt boundaries on reused SDK input. */
public final class SaveInput1999Test {
 static int assertions;
 static void check(boolean b,String why){assertions++;if(!b)throw new AssertionError(why);}
 static void until(java.util.function.BooleanSupplier condition,String reason)throws Exception{
  long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
  while(!condition.getAsBoolean()&&System.nanoTime()<end)Thread.yield();check(condition.getAsBoolean(),reason);
 }
 public static void main(String[] args)throws Exception{
  SaveQuality2.systemInfo=true;PhotoDetail.live=new PhotoDetail.Settings(false,1,false,1,true,true,true);
  final List<ProcessingTiming1947.Trace> traces=Collections.synchronizedList(new ArrayList<ProcessingTiming1947.Trace>());
  final List<Integer> metadata=Collections.synchronizedList(new ArrayList<Integer>());
  final List<int[]> pixels=Collections.synchronizedList(new ArrayList<int[]>());
  final AtomicInteger receipts=new AtomicInteger();final AtomicReference<Throwable> failure=new AtomicReference<Throwable>();
  final Bitmap source=new Bitmap(new int[]{17,18,19,20});ShotContext1932.bind(source,71);
  final ProcessingTiming1947.Trace first=ProcessingTiming1947.forKey(source);
  HostRouter1981.encoder=(controller,input,rotation,direction)->{
   try {
    check(input!=source&&!input.isRecycled(),"encoder owns independent source copy");
    ProcessingTiming1947.Trace trace=ProcessingTiming1947.forKey(input);traces.add(trace);
    metadata.add(AsyncSave1935.snapshotForPhoto1981(input).id);pixels.add(input.pixels.clone());
    check(trace.dimensions1999[2]==input.getWidth()&&trace.dimensions1999[3]==input.getHeight(),"actual retrieval size follows source even without camera metadata");
    check(trace.dimensions1999[4]==input.getWidth()&&trace.dimensions1999[5]==input.getHeight(),"save-owned size measured after ownership transition");
    check(trace.ownership1999==1&&trace.ticket1999>0,"one explicit ownership/ticket record per save");
    AsyncSave1935.encoding(input);AsyncSave1935.hardwareClosed1956();return "saved-"+traces.size();
   }catch(Throwable e){failure.compareAndSet(null,e);throw new RuntimeException(e);}
  };
  i.o.a.b1.a.b.f.a manager=new i.o.a.b1.a.b.f.a(new i.o.a.b1.a.b.f.a.c(){public void a(){}public void b(boolean ok,int n,String path,String error){if(!ok)failure.compareAndSet(null,new AssertionError("save failed: "+error));receipts.incrementAndGet();}});
  i.f.l.n.s.a.b().image=source;AsyncSave1935.submitAuto(manager,0,0);
  until(()->receipts.get()==1&&SaveQueue1935.idle1953(),"first actual save completes");
  check(traces.size()==1&&traces.get(0)==first&&first.finished,"first source identity preserved through save");
  check(metadata.get(0)==71&&first.binding1999==1,"first exact source metadata retained");
  int[] original=source.pixels.clone();AsyncSave1935.submitAuto(manager,0,0);
  until(()->receipts.get()==2&&SaveQueue1935.idle1953(),"second use of SDK holder completes independently");
  check(traces.size()==2&&traces.get(1)!=first&&traces.get(1).binding1999==3,"completed source record split into fresh save trace");
  check(metadata.get(1)==0&&traces.get(1).expected==0,"stale camera metadata is not copied into a new save");
  check(Arrays.equals(original,pixels.get(0))&&Arrays.equals(original,pixels.get(1))&&Arrays.equals(original,source.pixels),"trace repair never resizes or changes source pixels");
  Bitmap unknown=new Bitmap(new int[]{31,32,33,34,35,36});unknown.width=3;unknown.height=2;i.f.l.n.s.a.b().image=unknown;
  AsyncSave1935.submitAuto(manager,0,0);until(()->receipts.get()==3&&SaveQueue1935.idle1953(),"unobserved capture metadata still saves real pixels");
  check(traces.get(2).binding1999==2&&metadata.get(2)==0,"unknown source record explicitly identified without previous metadata fallback");
  check(Arrays.equals(unknown.pixels,pixels.get(2))&&!unknown.isRecycled()&&!source.isRecycled(),"SDK source remains pristine and unrecycled");
  check(traces.get(0).ticket1999<traces.get(1).ticket1999&&traces.get(1).ticket1999<traces.get(2).ticket1999,"save IDs remain distinct and ordered");
  check(failure.get()==null&&ExitJobs185.count()==0&&SaveQueue1935.count()==0,"all reservations, exits and receipts complete without failure: "+failure.get());
  System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"completed_bitmap_trace_isolated\":true,\"save_input_pixels_unchanged\":true,\"save_bitmap_dimensions_observed\":true}");
 }
}

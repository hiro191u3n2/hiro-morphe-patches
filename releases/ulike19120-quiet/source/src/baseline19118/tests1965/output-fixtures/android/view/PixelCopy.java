package android.view;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
public final class PixelCopy {
 public static final int SUCCESS=0,ERROR_UNKNOWN=1,ERROR_TIMEOUT=2,ERROR_SOURCE_NO_DATA=3,ERROR_SOURCE_INVALID=4,ERROR_DESTINATION_INVALID=5;
 public interface OnPixelCopyFinishedListener {void onPixelCopyFinished(int result);}
 public static final List<Request> REQUESTS=Collections.synchronizedList(new ArrayList<Request>());
 public static volatile boolean automatic=true,failRequest,throwAfterSubmit;public static int result=SUCCESS,luma=128;
 public static volatile CountDownLatch entered,release;
 public static final class Request {
  public final Surface surface;public final Bitmap bitmap;public final OnPixelCopyFinishedListener callback;public final Handler handler;
  Request(Surface surface,Bitmap bitmap,OnPixelCopyFinishedListener callback,Handler handler){this.surface=surface;this.bitmap=bitmap;this.callback=callback;this.handler=handler;}
 }
 public static void request(Surface surface,Bitmap bitmap,OnPixelCopyFinishedListener callback,Handler handler){
  if(Looper.myLooper()==Looper.getMainLooper())throw new AssertionError("PixelCopy request blocks MAIN");
  if(!Thread.currentThread().isDaemon())throw new AssertionError("observation worker must be daemon");
  if(failRequest)throw new IllegalArgumentException("injected PixelCopy validation error");
  if(bitmap.width!=16||bitmap.height!=16)throw new AssertionError("destination is not bounded");
  final Request request=new Request(surface,bitmap,callback,handler);REQUESTS.add(request);
  CountDownLatch started=entered,blocked=release;
  if(started!=null)started.countDown();if(blocked!=null)try{if(!blocked.await(5,TimeUnit.SECONDS))throw new AssertionError("worker was not released");}catch(InterruptedException e){throw new AssertionError(e);}
  if(automatic){final int status=result,value=luma;handler.postDelayed(new Runnable(){public void run(){deliver(request,status,value);}},30);}
  if(throwAfterSubmit)throw new RuntimeException("injected failure after native ownership");
 }
 public static void complete(int index,final int result,final int luma){final Request request=REQUESTS.get(index);request.handler.post(new Runnable(){public void run(){deliver(request,result,luma);}});}
 private static void deliver(Request request,int result,int luma){
  if(request.bitmap.recycled)throw new AssertionError("PixelCopy still owns recycled destination");
  for(int i=0;i<request.bitmap.pixels.length;i++)request.bitmap.pixels[i]=0xff000000|(luma<<16)|(luma<<8)|luma;
  request.callback.onPixelCopyFinished(result);
 }
 public static void reset(){REQUESTS.clear();automatic=true;failRequest=false;throwAfterSubmit=false;entered=null;release=null;result=SUCCESS;luma=128;}
}

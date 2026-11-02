package com.hiro.ulike;
import android.graphics.Bitmap;
import android.media.Image;
import android.hardware.camera2.*;
public final class CaptureBinding1991Test {
 static int assertions;
 static void check(boolean b,String why){assertions++;if(!b)throw new AssertionError(why);}
 public static final class Owner {public Object x0;public CameraDevice j=new CameraDevice("back");public CameraCharacteristics a=new CameraCharacteristics();Owner(Object callback){x0=callback;a.put(CameraCharacteristics.LENS_FACING,1);}}
 static TotalCaptureResult result(long time){TotalCaptureResult r=new TotalCaptureResult();r.put(CaptureResult.SENSOR_TIMESTAMP,time);r.put(CaptureResult.SENSOR_SENSITIVITY,100);r.put(CaptureResult.SENSOR_EXPOSURE_TIME,10000000L);r.put(CaptureResult.LENS_FOCAL_LENGTH,6.3f);return r;}
 static void start(Object cb){ShotContext1932.beginRecorder(new Object(),cb);ShotContext1932.begin(null,cb);}
 static ProcessingTiming1947.Trace trace(Object cb){return ProcessingTiming1947.forKey(cb);}
 public static void main(String[] args)throws Exception{
  Object cb=new Object();start(cb);ProcessingTiming1947.Trace first=trace(cb);
  check(first.requestOrigin1991==1&&first.events1991[0]>0,"production recorder sets exact trace origin");
  check(first.events1991[1]>=first.events1991[0],"native request follows same owner");
  Owner owner=new Owner(cb);ShotContext1932.received(owner,new Image(100,400,300),result(101));
  check(first.events1991[2]==0,"wrong image timestamp cannot advance timing");
  ShotContext1932.received(owner,new Image(100,400,300),result(100));
  check(first.events1991[2]>=first.events1991[1],"matched exact image records actual receipt");
  Bitmap image=new Bitmap(400,300);ShotContext1932.bindDelivery(cb,new Object(),image);
  check(first.events1991[3]>=first.events1991[2]&&ProcessingTiming1947.forKey(image)==first,"render callback event follows image to save");
  long old=first.events1991[3];start(cb);ProcessingTiming1947.Trace second=trace(cb);
  check(first!=second&&second.events1991[3]==0,"reused callback receives fresh timing");
  ShotContext1932.bindDelivery(cb,new Object(),new Bitmap(400,300));
  check(second.events1991[3]>0&&first.events1991[3]==old,"new delivery cannot rewrite old photo");
  Object nativeOnly=new Object();ShotContext1932.begin(null,nativeOnly);
  check(trace(nativeOnly).requestOrigin1991==2,"missing recorder uses explicitly named native origin");
  Object failed=new Object();start(failed);ProcessingTiming1947.Trace error=trace(failed);
  ShotContext1932.failed(failed,-1);ShotContext1932.bindDelivery(failed,new Object(),new Bitmap(2,2));
  check(error.state==2&&error.events1991[3]==0,"failed callback cannot manufacture delivery");
  System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+",\"actual_capture_binding_verified\":true}");
 }
}

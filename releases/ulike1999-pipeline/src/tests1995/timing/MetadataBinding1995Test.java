package com.hiro.ulike;
import android.media.Image;
import android.hardware.camera2.*;
public final class MetadataBinding1995Test {
 static int assertions;
 static void check(boolean b,String reason){assertions++;if(!b)throw new AssertionError(reason);}
 static void start(Object cb){ShotContext1932.beginRecorder(new Object(),cb);ShotContext1932.begin(null,cb);}
 static ProcessingTiming1947.Trace trace(Object cb){return ProcessingTiming1947.forKey(cb);}
 static String render(ProcessingTiming1947.Trace t)throws Exception{return Timing1982Test.render(t);}
 public static void main(String[] args)throws Exception{
  Object cb=new Object();start(cb);ProcessingTiming1947.Trace t=trace(cb);CaptureBinding1991Test.Owner owner=new CaptureBinding1991Test.Owner(cb);
  check(render(t).contains("一致確認済み入力の撮影条件: 未観測"),"unknown metadata cannot be estimated");
  ShotContext1932.received(owner,new Image(100,400,300),CaptureBinding1991Test.result(101));
  check(!t.captureMetadataObserved1995,"mismatched timestamp cannot populate metadata");
  ShotContext1932.received(owner,new Image(100,400,300),CaptureBinding1991Test.result(100));
  check(t.captureMetadataObserved1995&&t.captureIso1995==100&&t.captureExposureNanos1995==10000000L&&t.captureWidth1995==400&&t.captureHeight1995==300,"exact image binding supplies existing scalar values");
  check(render(t).contains("ISO 100 / 露光 10000000 ns / レンズ 背面 / 入力 400×300"),"exposure units and input dimensions labelled");
  ProcessingTiming1947.captureMetadata1995(t,400,1,1,1,1);
  check(t.captureIso1995==100&&t.captureWidth1995==400,"first exact observation cannot be overwritten");
  start(cb);ProcessingTiming1947.Trace second=trace(cb);
  check(second!=t&&!second.captureMetadataObserved1995,"reused callback does not inherit prior metadata");
  TotalCaptureResult unknown=new TotalCaptureResult();unknown.put(CaptureResult.SENSOR_TIMESTAMP,200L);
  check(ShotContext1932.receivedValues1933(owner,cb,200L,640,480,unknown),"burst metadata exact binding uses same scalar path");
  check(second.captureMetadataObserved1995&&second.captureIso1995==0&&second.captureExposureNanos1995==0,"missing sensor values remain unknown");
  check(render(second).contains("ISO 不明 / 露光 不明")&&render(second).contains("入力 640×480"),"missing exposure is not rendered as zero nanoseconds");
  Object another=new Object();start(another);ProcessingTiming1947.Trace third=trace(another);
  check(!ShotContext1932.receivedValues1933(owner,another,200L,640,480,unknown)&&!third.captureMetadataObserved1995,"wrong owner callback cannot assign values");
  ProcessingTiming1947.captureMetadata1995(third,-1,1,1,2,2);ProcessingTiming1947.captureMetadata1995(third,1,-1,1,2,2);ProcessingTiming1947.captureMetadata1995(third,1,1,5,2,2);ProcessingTiming1947.captureMetadata1995(third,1,1,1,0,2);
  check(!third.captureMetadataObserved1995,"invalid scalar tuples rejected atomically");
  ProcessingTiming1947.finish(t,true);String terminal=render(t);ShotContext1932.received(owner,new Image(200,800,600),unknown);check(render(t).equals(terminal),"later photo cannot rewrite saved metadata");
  System.out.println("RESULT {\"status\":\"passed\",\"assertions\":"+assertions+"}");
 }
}

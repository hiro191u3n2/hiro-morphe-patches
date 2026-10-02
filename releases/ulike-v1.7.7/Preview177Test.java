package com.hiro.ulike;
import android.hardware.camera2.*;import android.hardware.camera2.params.*;import android.util.Size;import android.view.View;import java.util.*;
public final class Preview177Test{
 static int tests;static void check(boolean yes,String name){tests++;if(!yes)throw new AssertionError(name);}
 static final Size HD=new Size(1920,1080),FULL=new Size(4000,3000),SMALL=new Size(1280,720);
 public static void main(String[] args)throws Exception{
  ManualLens170Test.Owner o=ManualLens170Test.setup();OpticalZoom.Route r=OpticalZoom.active;
  StreamConfigurationMap logical=r.logical.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP), physical=r.lens.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
  OpticalZoom.maps.put(logical,r);logical.values.put(256,new Size[]{FULL});logical.values.put(View.class,new Size[]{HD,SMALL});physical.values.put(View.class,new Size[]{HD,SMALL});
  long epoch=OpticalZoom.epoch;
  check(Preview177.outputSizes(logical,256).length==0,"unsupported JPEG advertises no physical sizes");
  check(!r.failed&&OpticalZoom.failures==0&&OpticalZoom.starts==0,"JPEG capability inquiry must not poison/restart camera");
  check(OpticalZoom.active==r&&OpticalZoom.epoch==epoch&&OpticalZoom.requested==1,"capability query leaves chosen lens/generation unchanged");
  check(Arrays.equals(Preview177.outputSizes(logical,View.class),new Size[]{HD,SMALL}),"preview sizes still available after absent JPEG");
  CameraDevice device=new CameraDevice(r.cameraId);OpticalZoom.devices.put(device,r);
  CaptureRequest.Builder request=ManualLens170.createRequest(device,1);check(request!=null,"actual request not rejected after optional inquiry");
  List<OutputConfiguration> outputs=Arrays.asList(new OutputConfiguration());ManualLens170.createSession(device,new SessionConfiguration(0,outputs,Runnable::run,null));
  check("M".equals(outputs.get(0).id),"same main physical output still required");CameraCaptureSession session=new CameraCaptureSession(device);device.config.getStateCallback().onConfigured(session);ManualLens170.observe(r,session,new TotalCaptureResult(0,"U"));
  check(r.configured&&r.frames>0&&OpticalZoom.confirmed==1&&!r.failed,"main can produce first frame after absent optional format");
  int frames=r.frames;String detail=OpticalZoom.detail;
  for(int i=0;i<100;i++)check(Preview177.outputSizes(logical,256).length==0,"repeat unsupported query "+i);
  check(detail.equals(OpticalZoom.detail),"repeated capability diagnostic bounded");check(r.frames==frames&&OpticalZoom.confirmed==1&&!r.failed,"query cannot break established preview");
  physical.values.put(256,new Size[]{SMALL});check(Preview177.outputSizes(logical,256).length==0,"no common sizes does not return logical-only JPEG size");
  physical.values.put(256,new Size[]{FULL});check(Arrays.equals(Preview177.outputSizes(logical,256),new Size[]{FULL}),"full supported resolution unchanged");
  physical.unsupported=true;check(Preview177.outputSizes(logical,256).length==0&&!r.failed,"unsupported physical format exception is nonfatal capability answer");check(Preview177.outputSizes(logical,View.class).length==0&&!r.failed,"unsupported physical class exception is nonfatal capability answer");physical.unsupported=false;
  check(Arrays.equals(Preview177.intersect(new Size[]{FULL,HD,SMALL},new Size[]{SMALL,FULL}),new Size[]{FULL,SMALL}),"only common sizes and original order");
  check(Preview177.intersect(null,new Size[]{HD}).length==0,"null query safe");check(Preview177.intersect(new Size[]{HD},null).length==0,"missing physical capability safe");
  check(Preview177.intersect(new Size[0],new Size[]{HD}).length==0,"empty capability safe");check(Preview177.intersect(new Size[]{null,HD},new Size[]{HD}).length==1,"invalid element ignored");
  StreamConfigurationMap unrelated=new StreamConfigurationMap();Size[] raw={FULL};unrelated.values.put(256,raw);check(Preview177.outputSizes(unrelated,256)==raw,"unmapped stock query remains untouched");
  check(Preview177.outputSizes(unrelated,View.class)==null,"unmapped stock null contract remains untouched");
  OpticalZoom.active=null;check(Preview177.outputSizes(logical,256)==logical.values.get(256),"stale mapping ignored");OpticalZoom.active=r;
  boolean denied=false;try{ManualLens170.createSession(device,new SessionConfiguration(1,outputs,Runnable::run,null));}catch(CameraAccessException expected){denied=true;}
  check(denied&&r.failed,"genuine unsupported session still rejected, never unpinned");
  o=ManualLens170Test.setup();OpticalZoom.realHostCheck=true;View host=new View();
  check(!ManualLens170.ready(),"before host attachment cannot restart");
  // No lens-row construction, rectangle, active-frame confirmation or button click.
  Preview177.attached(host);check(ManualLens170.ready(),"attached preview host allows startup recovery before optional row appears");
  host.focused=false;check(!ManualLens170.ready(),"unfocused host still blocked");host.focused=true;host.shown=false;check(!ManualLens170.ready(),"hidden host still blocked");host.shown=true;host.windowVisibility=8;check(!ManualLens170.ready(),"hidden window still blocked");host.windowVisibility=0;host.attached=false;check(!ManualLens170.ready(),"detached host still blocked");host.attached=true;
  Object strong=Lifecycle172Test.camera(0);LensLifecycle172.onForeground(strong);Runnable watch=Lifecycle172Test.lastTask();Lifecycle172Test.tick(7000);watch.run();check(OpticalZoom.attempt==1&&OpticalZoom.requested==1,"missing-frame watchdog retries main without lens-row display");
  o=ManualLens170Test.setup();OpticalZoom.realHostCheck=true;Preview177.attached(host);strong=Lifecycle172Test.camera(0);LensLifecycle172.onForeground(strong);watch=Lifecycle172Test.lastTask();OpticalZoom.foreground=false;Lifecycle172Test.tick(7000);watch.run();check(OpticalZoom.starts==0,"host registration cannot reopen background camera");
  System.out.println("PASS Preview177: "+tests+" assertions; optional capability inquiries do not poison preview; host registration precedes lens-row rendering. Host doubles, NOT Android/Galaxy execution.");
 }
}

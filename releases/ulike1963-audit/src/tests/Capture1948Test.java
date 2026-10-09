package com.hiro.ulike;

import android.graphics.*;
import android.hardware.camera2.*;
import android.hardware.camera2.params.RggbChannelVector;
import android.media.*;
import android.os.*;
import android.util.Range;
import com.ss.android.vesdk.TECameraVideoRecorder$60;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Arrays;

/** Executes the production capture coordinator against controllable Android
 * callback boundaries. It does not pretend to be an Android/ART camera test. */
public final class Capture1948Test {
    private static int assertions,scenarios;
    private static final int W=2000,H=2000;
    public static final class Owner {
        public Object x0,y0; public i.s.a.w.i g=new i.s.a.w.i();
        public CameraDevice j=new CameraDevice("front");
        public CameraCaptureSession d=new CameraCaptureSession();
        public CameraCharacteristics a=characteristics(true);
        public Handler k=new Handler(); public ImageReader e0=new ImageReader(W,H,35);public int m0=1;
        public TotalCaptureResult i0;public boolean listenerThrows;
    }
    public static final class ReaderState {
        public ImageReader yuv,original;public boolean closed,selected=true;
        public Image pending;public TotalCaptureResult result;
        public ReaderState(ImageReader r){yuv=r;original=r;}
    }
    public static final class Listener {
        public Owner a;public Listener(Owner o){a=o;}
        public void onImageAvailable(ImageReader reader){
            Image image=BurstCapture1933.acquireStill(this,reader);
            if(a.listenerThrows)throw new IllegalStateException("scripted stock listener failure");
            if(BurstCapture1933.stillImage(this,image))return;
            if(image!=null){originalQ0(a,image,a.i0);a.i0=null;image.close();}
        }
    }
    private static final class Env {
        final Owner o=new Owner(); TECameraVideoRecorder$60 callback=new TECameraVideoRecorder$60();
        final ReaderState reader=new ReaderState(o.e0);
        final Listener listener=new Listener(o);
        int iso,targetDelivery=1;long exposure;long time=1000000000L;boolean useStock;String physical="";
        Env(){o.x0=callback;}
        boolean seed(int i,long e)throws Exception {
            BurstCapture1933.canceled(o);
            ProcessingTiming1947.begin(callback);
            targetDelivery=callback.deliveries+1;
            iso=i;exposure=e;
            CaptureRequest.Builder b=new CaptureRequest.Builder();
            b.set(CaptureRequest.CONTROL_CAPTURE_INTENT,CaptureRequest.CONTROL_CAPTURE_INTENT_STILL_CAPTURE);
            b.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON);
            b.set(CaptureRequest.SENSOR_EXPOSURE_TIME,e);b.set(CaptureRequest.SENSOR_SENSITIVITY,i);
            TotalCaptureResult r=metadata(b.build(),time,i,e);
            Image image=image(W,H,time,7,false);
            check(!arrival(image),"first real still passes reader guard");
            boolean accepted=BurstCapture1933.beginImage(o,image,r);
            check(image.closes==0,"seed Image ownership remains with SDK caller");
            image.close(); drain(o.k);return accepted;
        }
        boolean arrival(Image image){
            o.e0.offer(image);
            if(useStock){Image acquired=BurstCapture1933.acquireStill(listener,o.e0);
                check(acquired==image,"stock hook preserves acquired Image identity");
                return BurstCapture1933.stillImage(listener,acquired);}
            Image acquired=BurstCapture1933.acquireYuv(o,reader,o.e0);
            check(acquired==image,"YUV hook preserves acquired Image identity");
            return BurstCapture1933.yuvImage(o,reader,acquired);
        }
        CameraCaptureSession.Call latest(){return o.d.calls.get(o.d.calls.size()-1);}
        void emit(boolean imageFirst)throws Exception {emit(latest(),imageFirst,0,false,false);}
        void emit(CameraCaptureSession.Call c,boolean imageFirst,long mismatch,boolean badColour,boolean badGeometry)throws Exception {
            time+=80000000L;
            Integer si=c.request.get(CaptureRequest.SENSOR_SENSITIVITY);Long se=c.request.get(CaptureRequest.SENSOR_EXPOSURE_TIME);
            int actualIso=si==null?iso:si;long actualExposure=se==null?exposure:se;
            TotalCaptureResult r=metadata(c.request,time+mismatch,actualIso,actualExposure);
            if(badColour)r.put(CaptureResult.COLOR_CORRECTION_GAINS,new RggbChannelVector(2,1,1,1));
            if(badGeometry)r.put(CaptureResult.SCALER_CROP_REGION,new Rect(4,0,W,H));
            c.callback.onCaptureStarted(o.d,c.request,time,1);
            Image image=image(W,H,time,11,false);
            if(!imageFirst)c.callback.onCaptureCompleted(o.d,c.request,r);
            check(arrival(image),"supplementary image intercepted at its real reader");
            check(image.closes==1,"supplementary image closed exactly once");
            if(imageFirst)c.callback.onCaptureCompleted(o.d,c.request,r);
            drain(o.k);
        }
        void finishAll()throws Exception {
            int previous=-1;
            for(int step=0;step<8&&callback.deliveries<targetDelivery;step++){
                int n=o.d.calls.size();if(n==previous)break;previous=n;
                emit((step&1)==0);
            }
            drain(o.k);
        }
        TotalCaptureResult metadata(CaptureRequest request,long time,int iso,long exposure){
            TotalCaptureResult r=result(request,time,iso,exposure);
            if(!physical.isEmpty()){
                r.put(CaptureResult.SENSOR_TIMESTAMP,time+1000000L);
                r.physical.put(physical,result(request,time,iso,exposure));
            }
            return r;
        }
    }

    /** The fixture owns Image just as the pinned Q0 callers do. */
    public static void originalQ0(Owner owner,Image image,TotalCaptureResult result){
        if(BurstCapture1933.beginImage(owner,image,result))return;
        i.s.a.w.m frame=new i.s.a.w.m(BurstCapture1933.copyNv21(image),
                i.s.a.w.m.d.PIXEL_FORMAT_NV21,image.getWidth(),image.getHeight(),owner.m0==1?270:90);
        ((TECameraVideoRecorder$60)owner.x0).onPictureTaken(frame,owner.g);
    }

    private static CaptureRequest ordinaryRequest(int iso,long exposure){
        CaptureRequest.Builder b=new CaptureRequest.Builder();
        b.set(CaptureRequest.CONTROL_CAPTURE_INTENT,CaptureRequest.CONTROL_CAPTURE_INTENT_STILL_CAPTURE);
        b.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON);
        b.set(CaptureRequest.SENSOR_EXPOSURE_TIME,exposure);b.set(CaptureRequest.SENSOR_SENSITIVITY,iso);return b.build();
    }
    private static TECameraVideoRecorder$60 fresh(Env e){
        TECameraVideoRecorder$60 old=e.callback;
        BurstCapture1933.canceled(e.o);e.callback=new TECameraVideoRecorder$60();e.o.x0=e.callback;
        e.targetDelivery=1;e.reader.selected=true;e.reader.pending=null;e.reader.result=null;e.o.i0=null;return old;
    }
    private static void ordinaryResult(Env e,TotalCaptureResult result){
        if(e.useStock){e.o.i0=result;BurstCapture1933.observedResult(e.o,result);}
        else CaptureYuv.result(e.o,e.reader,result);
    }
    private static int deferred()throws Exception{
        Field f=BurstCapture1933.class.getDeclaredField("DEFERRED");f.setAccessible(true);return ((java.util.List<?>)f.get(null)).size();
    }

    private static void check(boolean b,String label){assertions++;if(!b)throw new AssertionError(label);}
    private static void scenario(){scenarios++; reset();}
    private static void reset(){
        PhotoDetail.current=new PhotoDetail.Settings(true,2);ShotContext1932.calls=0;ShotContext1932.allow=true;
        ShotContext1932.timestamp=0;ShotContext1932.iso=0;FusionPixels1933.calls=0;FusionPixels1933.fused=0;
        FusionPixels1933.lastFrames=0;FusionPixels1933.throwFuse=false;FusionPixels1933.next=new FusionPixels1933.Motion();
        FusionPixels1933.chosenReference=0;FusionPixels1933.wrongTimestamp=false;FusionPixels1933.cancelResult=false;
        FusionPixels1933.blockProbe=false;FusionPixels1933.probeEntered=false;FusionPixels1933.probeInterrupted=false;
        check(FusionPixels1933.Analysis.retained==0,"previous shot analysis released retained frame references");
        FusionPixels1933.resetPreparation();CaptureYuv.failedCalls=0;OpticalZoom.routes.clear();ExitJobs185.sealed=false;
        check(ExitJobs185.count==0,"previous capture counter balanced");
        SystemClock.now+=10000;
    }
    private static CameraCharacteristics characteristics(boolean manual){
        CameraCharacteristics c=new CameraCharacteristics();
        c.put(CameraCharacteristics.CONTROL_AWB_LOCK_AVAILABLE,true);c.put(CameraCharacteristics.CONTROL_AE_LOCK_AVAILABLE,true);
        c.put(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES,manual?new int[]{1}:new int[]{});
        c.put(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES,new int[]{0,1});c.put(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES,new int[]{0,1});
        c.put(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE,new Range<Long>(100000L,1000000000L));
        c.put(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE,new Range<Integer>(50,6400));
        c.put(CameraCharacteristics.SENSOR_INFO_MAX_FRAME_DURATION,1000000000L);return c;
    }
    private static TotalCaptureResult result(CaptureRequest request,long time,int iso,long exposure){
        TotalCaptureResult r=new TotalCaptureResult();r.request=request;
        r.put(CaptureResult.SENSOR_TIMESTAMP,time);r.put(CaptureResult.SENSOR_SENSITIVITY,iso);r.put(CaptureResult.SENSOR_EXPOSURE_TIME,exposure);
        r.put(CaptureResult.SENSOR_FRAME_DURATION,Math.max(exposure,33333333L));r.put(CaptureResult.LENS_FOCAL_LENGTH,3.3f);
        r.put(CaptureResult.LENS_FOCUS_DISTANCE,0f);r.put(CaptureResult.LENS_STATE,CaptureResult.LENS_STATE_STATIONARY);
        r.put(CaptureResult.SCALER_CROP_REGION,new Rect(0,0,W,H));r.put(CaptureResult.CONTROL_ZOOM_RATIO,1f);
        r.put(CaptureResult.FLASH_MODE,CaptureResult.FLASH_MODE_OFF);r.put(CaptureResult.FLASH_STATE,0);
        r.put(CaptureResult.COLOR_CORRECTION_GAINS,new RggbChannelVector(1,1,1,1));return r;
    }
    private static Image image(int w,int h,long time,int value,boolean padded){
        int yr=w+(padded?7:0),uvr=w/2+(padded?3:0),pos=padded?5:0;
        ByteBuffer y=ByteBuffer.allocate(pos+yr*h),u=ByteBuffer.allocate(pos+uvr*(h/2)),v=ByteBuffer.allocate(pos+uvr*(h/2));
        Arrays.fill(y.array(),(byte)value);Arrays.fill(u.array(),(byte)101);Arrays.fill(v.array(),(byte)151);
        y.position(pos);u.position(pos);v.position(pos);
        return new Image(w,h,35,time,new Image.Plane[]{new Image.Plane(y,yr,1),new Image.Plane(u,uvr,1),new Image.Plane(v,uvr,1)});
    }
    private static boolean running()throws Exception{Field f=BurstCapture1933.class.getDeclaredField("fusionRunning");f.setAccessible(true);return f.getBoolean(null);}
    private static boolean finishing()throws Exception {
        Field f=BurstCapture1933.class.getDeclaredField("active");f.setAccessible(true);Object s=f.get(null);
        if(s==null)return false;Field closed=s.getClass().getDeclaredField("closed"),finishing=s.getClass().getDeclaredField("finishing");
        closed.setAccessible(true);finishing.setAccessible(true);return !closed.getBoolean(s)&&finishing.getBoolean(s);
    }
    private static void drain(Handler h)throws Exception {
        int quiet=0;
        for(int i=0;i<3000;i++){
            boolean ran=h.one();if(!running()&&!finishing()&&h.empty()&&!ran){if(++quiet==3)return;}else quiet=0;
            if(!ran)Thread.sleep(1);
        }
        throw new AssertionError("coordinator did not quiesce");
    }
    private static void waitPreparation(Env e)throws Exception {
        for(int i=0;i<3000&&!FusionPixels1933.preparationEntered;i++){e.o.k.one();Thread.sleep(1);}
        check(FusionPixels1933.preparationEntered,"scripted preparation entered its background worker");
    }
    private static void waitInterrupted()throws Exception {
        for(int i=0;i<3000&&!FusionPixels1933.preparationInterrupted;i++)Thread.sleep(1);
        check(FusionPixels1933.preparationInterrupted,"cancellation interrupts running preparation");
    }
    private static void prepared(int frames,int pairs){
        check(FusionPixels1933.prepareFrameCalls==frames&&FusionPixels1933.framePrepsExited==frames,"each accepted immutable frame prepared once");
        check(FusionPixels1933.preparePairCalls==pairs&&FusionPixels1933.pairPrepsExited==pairs,"each accepted shutter pair prepared once");
        check(FusionPixels1933.maxPreparationRunning==1,"capture preparation uses one bounded worker");
        check(FusionPixels1933.identityChecks>=frames,"preparation checked bytes and actual metadata identity");
        synchronized(FusionPixels1933.preparedFrames){
            for(FusionPixels1933.PreparedFrame f:FusionPixels1933.preparedFrames){
                check(f.w==W&&f.h==H&&f.time>0&&f.iso>0&&f.exposure>0,"full sensor preprocessing has actual capture metadata");
                check(Arrays.hashCode(f.bytes)==f.hash,"capture NV21 remains immutable through renderer handoff");
            }
        }
    }
    private static void delivered(Env e,int frames,long timestamp){
        check(e.callback.deliveries==1,"renderer receives exactly one final image");
        check(e.callback.failures==0,"successful capture has no duplicate failure callback");
        check(e.callback.output.width==W&&e.callback.output.height==H,"full sensor image dimensions retained");
        check(e.callback.output.rotation==270,"front orientation matches native Q0");
        check(ShotContext1932.calls==1&&ShotContext1932.timestamp==timestamp,"metadata binds selected reference exactly once");
        check(FusionPixels1933.lastFrames==frames,"expected number of distinct sensor frames");
        check(ExitJobs185.count==0,"capture job released after render callback");
    }

    /** Exercise the callback that synchronously reenters cancellation while its
     * CPU coordinator will acquire Burst's monitor during completion. */
    private static void deferredFailureWhileWorker(boolean rejectHandler)throws Exception {
        scenario();final Env e=new Env();check(e.seed(100,5000000L),"retired-request deferred failure fixture armed");fresh(e);
        e.time=1160000000L;TotalCaptureResult proof=e.metadata(ordinaryRequest(100,5000000L),e.time,100,5000000L);ordinaryResult(e,proof);
        Image shutter=image(W,H,e.time,17,false);check(!e.arrival(shutter),"new shutter proved without resolving old request timestamp");
        check(BurstCapture1933.beginImage(e.o,shutter,proof),"fresh capture starts with unresolved retired ticket");shutter.close();drain(e.o.k);
        FusionPixels1933.blockProbe=true;CameraCaptureSession.Call supplement=e.latest();long sensorTime=1240000000L;
        supplement.callback.onCaptureStarted(e.o.d,supplement.request,sensorTime,1);
        supplement.callback.onCaptureCompleted(e.o.d,supplement.request,e.metadata(supplement.request,sensorTime,100,5000000L));
        Image candidate=image(W,H,sensorTime,19,false);check(e.arrival(candidate)&&candidate.closes==1,"positively proved supplement reaches live worker despite old ambiguity");
        for(int i=0;i<3000&&!FusionPixels1933.probeEntered;i++){e.o.k.one();Thread.sleep(1);}
        check(FusionPixels1933.probeEntered&&running(),"deferred failure runs during active CPU coordinator");
        final Image[] held=new Image[rejectHandler?0:4];
        for(int i=0;i<held.length;i++){held[i]=image(W,H,1320000000L+i*80000000L,23+i,false);check(e.arrival(held[i])&&held[i].closes==0,"unproven image fills deferred queue while coordinator runs");}
        if(rejectHandler)e.o.k.reject=true;
        final Image failedImage=image(W,H,1720000000L,31,false);final Throwable[] failure=new Throwable[1];
        Thread failThread=new Thread(new Runnable(){public void run(){try{check(e.arrival(failedImage),"failed deferred image remains guard owned");}catch(Throwable t){failure[0]=t;}}});
        failThread.setDaemon(true);failThread.start();failThread.join(3000);
        check(!failThread.isAlive()&&failure[0]==null,"deferred failure callback reentry cannot hold coordinator monitor while joining worker");
        drain(e.o.k);check(CaptureYuv.failedCalls==1&&FusionPixels1933.probeInterrupted,"real CaptureYuv.failed reentered cancellation and interrupted active coordinator");
        check(failedImage.closes==1&&deferred()==0,"failed deferred frame closes once and reentrant cancellation empties queue");
        for(Image image:held)check(image.closes==1,"reentrant cancellation closes every previously held image exactly once");
        check(!running()&&ExitJobs185.count==0&&FusionPixels1933.Analysis.retained==0,"reentrant deferred failure releases worker, preprocessing and capture ownership");
        check(e.callback.deliveries==0&&e.callback.failures==1&&e.callback.noOpFailures==0,"deferred failure reports one real error without retired renderer delivery");
    }
    public static void main(String[] args)throws Exception {
        scenario();
        check(CapturePolicy1933.frameLimit(2000,2000,300000000L)==4,"memory permits four full frames");
        check(CapturePolicy1933.frameLimit(2000,2000,10000000L)==0,"low memory avoids burst");
        check(CapturePolicy1933.frameLimit(1920,1080,300000000L)==0,"preview-sized image not labelled HQ still");
        check(CapturePolicy1933.frameLimit(2001,2000,300000000L)==0,"odd YUV dimensions rejected");
        CapturePolicy1933.Decision still=CapturePolicy1933.choose(25000000L,1600,100000L,1000000000L,50,6400,4,true,true,true,0,0,0,80000000L);
        check(still.changedExposure&&still.exposureNanos==50000000L&&still.iso==800&&still.frames==4,"still night lengthens shutter and lowers ISO");
        CapturePolicy1933.Decision moving=CapturePolicy1933.choose(25000000L,1600,100000L,1000000000L,50,6400,4,true,true,true,0.8f,0.5f,4,80000000L);
        check(moving.changedExposure&&moving.exposureNanos==10000000L&&moving.iso==4000&&moving.frames==2,"motion shortens shutter and bounds frame count");
        CapturePolicy1933.Decision unknown=CapturePolicy1933.choose(25000000L,1600,100000L,1000000000L,50,6400,4,true,true,false,0,0,0,0);
        check(unknown.exposureNanos<=25000000L&&unknown.frames==2,"unknown motion never extends shutter");
        CapturePolicy1933.Decision locked=CapturePolicy1933.choose(25000000L,1600,100000L,1000000000L,50,6400,4,false,true,true,0,0,0,80000000L);
        check(!locked.changedExposure&&locked.iso==1600,"non-manual device stays at original exposure");
        check(!CapturePolicy1933.compatible(100,100,20000,20000,500,500),"duplicate sensor timestamp excluded");
        check(!CapturePolicy1933.compatible(100,200,20000,30000,500,500),"different exposure group excluded");

        scenario();
        Image padded=image(6,4,1,31,true);byte[] nv=BurstCapture1933.copyNv21(padded);
        check(nv.length==36&&nv[0]==31&&nv[24]==(byte)151&&nv[25]==101,"padded planes copy as NV21 V then U");
        for(Image.Plane p:padded.planes)check(p.getBuffer().position()==5,"source plane position unchanged");
        check(padded.closes==0,"copy never closes borrowed Image");
        padded.crop=new Rect(2,0,6,4);boolean threw=false;try{BurstCapture1933.copyNv21(padded);}catch(IllegalArgumentException x){threw=true;}check(threw,"unsupported crop is not silently misregistered");

        scenario();Env normal=new Env();check(normal.seed(100,5000000L),"ordinary HQ YUV capture armed");
        check(ExitJobs185.count==1,"one asynchronous capture job owns the burst");
        check(normal.latest().request.target==normal.o.e0.getSurface(),"same already-configured surface reused");
        check(Boolean.TRUE.equals(normal.latest().request.get(CaptureRequest.CONTROL_AWB_LOCK)),"AWB locked only on extra still");
        check(normal.latest().request.get(CaptureRequest.CONTROL_AE_MODE)==0,"manual same-exposure request");
        normal.finishAll();delivered(normal,3,1000000000L);

        scenario();Env stock=new Env();stock.useStock=true;check(stock.seed(100,5000000L),"stock full-resolution YUV reader supported");stock.finishAll();delivered(stock,3,1000000000L);

        scenario();FusionPixels1933.chosenReference=1;Env selected=new Env();check(selected.seed(100,5000000L),"selected reference fixture armed");
        selected.finishAll();delivered(selected,3,1000000000L);
        check(ShotContext1932.timestamp==1000000000L,"supplemental result cannot replace shutter metadata");
        check(selected.callback.output.bytes[0]==7 && selected.callback.output.metadata.d.get(CaptureResult.SENSOR_TIMESTAMP)==1000000000L,
                "untrusted future-reference result restores original shutter bytes and SDK metadata");

        scenario();FusionPixels1933.chosenReference=1;FusionPixels1933.wrongTimestamp=true;Env wrongStamp=new Env();check(wrongStamp.seed(100,5000000L),"wrong reference timestamp fixture armed");
        wrongStamp.finishAll();delivered(wrongStamp,3,1000000000L);
        check(wrongStamp.callback.output.bytes[0]==7 && wrongStamp.callback.output.metadata.d.get(CaptureResult.SENSOR_TIMESTAMP)==1000000000L,
                "wrong timestamp restores original bytes and SDK metadata");

        scenario();FusionPixels1933.chosenReference=1;FusionPixels1933.cancelResult=true;Env canceledFusion=new Env();check(canceledFusion.seed(100,5000000L),"canceled fusion fixture armed");
        canceledFusion.finishAll();delivered(canceledFusion,3,1000000000L);
        check(canceledFusion.callback.output.bytes[0]==7 && canceledFusion.callback.output.metadata.d.get(CaptureResult.SENSOR_TIMESTAMP)==1000000000L,
                "canceled result restores original bytes and SDK metadata");

        scenario();Env night=new Env();check(night.seed(1600,25000000L),"night first capture armed");night.emit(true);
        check(night.o.d.calls.size()==2,"motion probe permits another same-exposure denoise frame");
        check(night.latest().request.get(CaptureRequest.SENSOR_EXPOSURE_TIME)==25000000L,"quiet scene keeps original sensor exposure rather than restarting capture");
        check(night.latest().request.get(CaptureRequest.SENSOR_SENSITIVITY)==1600,"quiet scene keeps original ISO for one compatible denoise group");
        night.finishAll();delivered(night,4,1000000000L);
        check(ShotContext1932.iso==1600,"metadata describes actual original still");
        check(night.o.d.calls.size()==3,"stationary night uses original plus three supplements without a replacement group");
        check(night.callback.output.bytes[0]==7&&night.callback.output.metadata.d.get(CaptureResult.SENSOR_TIMESTAMP)==1000000000L,"stationary night retains shutter pixels and exact SDK timestamp");

        scenario();FusionPixels1933.next.score=0.8f;FusionPixels1933.next.movingFraction=0.5f;
        Env movingNight=new Env();check(movingNight.seed(1600,25000000L),"moving night armed");movingNight.emit(false);
        check(movingNight.o.d.calls.size()==1,"moving scene does not submit a later recapture with a different exposure");
        movingNight.finishAll();delivered(movingNight,1,1000000000L);
        check(movingNight.callback.output.bytes[0]==7,"moved-to pixels do not replace the original shutter image");

        scenario();Env aeOnly=new Env();aeOnly.o.a=characteristics(false);check(aeOnly.seed(1600,25000000L),"AE-lock camera still supports temporal NR");
        check(Boolean.TRUE.equals(aeOnly.latest().request.get(CaptureRequest.CONTROL_AE_LOCK)),"AE lock requested when manual unsupported");
        aeOnly.finishAll();delivered(aeOnly,4,1000000000L);
        for(CameraCaptureSession.Call c:aeOnly.o.d.calls)check(c.request.get(CaptureRequest.SENSOR_EXPOSURE_TIME)==25000000L,"AE-only route never claims dynamic shutter");

        scenario();Env mismatch=new Env();check(mismatch.seed(100,5000000L),"mismatch fixture armed");mismatch.emit(mismatch.latest(),true,1,false,false);delivered(mismatch,1,1000000000L);
        scenario();Env colour=new Env();check(colour.seed(100,5000000L),"colour fixture armed");colour.emit(colour.latest(),false,0,true,false);delivered(colour,1,1000000000L);
        scenario();Env geometry=new Env();check(geometry.seed(100,5000000L),"crop fixture armed");geometry.emit(geometry.latest(),true,0,false,true);delivered(geometry,1,1000000000L);

        scenario();Env failure=new Env();check(failure.seed(100,5000000L),"failure fixture armed");CameraCaptureSession.Call fc=failure.latest();fc.callback.onCaptureFailed(failure.o.d,fc.request,new CaptureFailure());drain(failure.o.k);delivered(failure,1,1000000000L);
        scenario();Env timeout=new Env();check(timeout.seed(100,5000000L),"timeout fixture armed");SystemClock.now+=1200;timeout.o.k.timers();drain(timeout.o.k);delivered(timeout,1,1000000000L);
        scenario();FusionPixels1933.throwFuse=true;Env kernelError=new Env();check(kernelError.seed(100,5000000L),"kernel exception fixture armed");kernelError.finishAll();delivered(kernelError,3,1000000000L);

        scenario();Env canceled=new Env();check(canceled.seed(100,5000000L),"cancel fixture armed");CameraCaptureSession.Call cc=canceled.latest();long lateTime=1080000000L;
        cc.callback.onCaptureStarted(canceled.o.d,cc.request,lateTime,1);BurstCapture1933.canceled(canceled.o);canceled.o.x0=new TECameraVideoRecorder$60();
        Image late=image(W,H,lateTime,41,false);check(canceled.arrival(late)&&late.closes==1,"late image of canceled request is discarded by exact timestamp");
        cc.callback.onCaptureCompleted(canceled.o.d,cc.request,result(cc.request,lateTime,100,5000000L));drain(canceled.o.k);
        check(canceled.callback.deliveries==0&&ShotContext1932.calls==0&&ExitJobs185.count==0,"canceled callback never receives new shot data");

        scenario();Env changedSession=new Env();check(changedSession.seed(100,5000000L),"session replacement fixture armed");CameraCaptureSession old=changedSession.o.d;CameraCaptureSession.Call sc=changedSession.latest();changedSession.o.d=new CameraCaptureSession();
        sc.callback.onCaptureCompleted(old,sc.request,result(sc.request,1080000000L,100,5000000L));SystemClock.now+=1200;changedSession.o.k.timers();drain(changedSession.o.k);
        check(changedSession.callback.deliveries==0&&ExitJobs185.count==0,"replaced session cannot deliver or leak job");

        scenario();PhotoDetail.current=new PhotoDetail.Settings(false,2);Env off=new Env();check(!off.seed(1600,25000000L)&&off.o.d.calls.isEmpty(),"NR OFF does not acquire extra frames");
        scenario();Env noLock=new Env();noLock.o.a.put(CameraCharacteristics.CONTROL_AWB_LOCK_AVAILABLE,false);check(!noLock.seed(100,5000000L),"unsupported white-balance lock preserves single path");
        scenario();Env noManualNoLock=new Env();noManualNoLock.o.a=characteristics(false);noManualNoLock.o.a.put(CameraCharacteristics.CONTROL_AE_LOCK_AVAILABLE,false);check(!noManualNoLock.seed(100,5000000L),"no exposure control preserves single path");
        scenario();Env sealed=new Env();ExitJobs185.sealed=true;check(!sealed.seed(100,5000000L)&&ExitJobs185.count==0,"exit sealing cannot leak capture ownership");ExitJobs185.sealed=false;
        scenario();Env reject=new Env();reject.o.k.reject=true;check(!reject.seed(100,5000000L)&&ExitJobs185.count==0,"rejected handler post rolls back ownership");

        scenario();Env unmarked=new Env();CaptureRequest.Builder ub=new CaptureRequest.Builder();ub.set(CaptureRequest.CONTROL_CAPTURE_INTENT,2);Image unmarkedImage=image(W,H,1,1,false);
        check(!BurstCapture1933.beginImage(unmarked.o,unmarkedImage,result(ub.build(),1,100,5000000L)),"unmarked preview or foreign Image cannot start HQ capture");
        scenario();Env duplicateMark=new Env();Image marked=image(W,H,1,1,false),other=image(W,H,2,2,false);duplicateMark.arrival(marked);
        check(!BurstCapture1933.beginImage(duplicateMark.o,other,result(ub.build(),2,100,5000000L)),"reader mark is bound to exact Image identity");

        scenario();
        CapturePolicy1933.Decision subMillisecond=CapturePolicy1933.choose(500000L,1600,100000L,1000000000L,50,6400,4,true,true,true,0.8f,0.5f,4,80000000L);
        check(!subMillisecond.changedExposure&&subMillisecond.exposureNanos==500000L,"moving sub-millisecond shutter is never lengthened by policy floor");

        scenario();final Env delayed=new Env();delayed.iso=100;delayed.exposure=5000000L;
        final Image delayedImage=image(W,H,delayed.time,17,false);delayed.o.e0.offer(delayedImage);
        Thread readerThread=new Thread(new Runnable(){public void run(){CaptureYuv.image(delayed.o,delayed.reader,delayed.o.e0);}});readerThread.start();readerThread.join();
        check(delayed.reader.pending==delayedImage&&delayedImage.closes==0,"custom receiver retains real Image before result");
        final TotalCaptureResult delayedResult=delayed.metadata(ordinaryRequest(100,5000000L),delayed.time,100,5000000L);
        Thread resultThread=new Thread(new Runnable(){public void run(){ordinaryResult(delayed,delayedResult);}});resultThread.start();resultThread.join();drain(delayed.o.k);
        check(delayed.o.d.calls.size()==1&&delayedImage.closes==1,"Q0 on another thread retains exact full-resolution provenance");
        delayed.finishAll();delivered(delayed,3,1000000000L);

        scenario();Env unknownOld=new Env();check(unknownOld.seed(100,5000000L),"unknown retired request fixture armed");
        CameraCaptureSession.Call oldUnknown=unknownOld.latest();TECameraVideoRecorder$60 abandonedCallback=fresh(unknownOld);
        Image unknownLate=image(W,H,1080000000L,71,false);
        check(unknownOld.arrival(unknownLate)&&unknownLate.closes==0,"old image before both timestamp callbacks is held");drain(unknownOld.o.k);
        oldUnknown.callback.onCaptureStarted(unknownOld.o.d,oldUnknown.request,1080000000L,1);drain(unknownOld.o.k);
        check(unknownLate.closes==1&&deferred()==0,"late start timestamp resolves and closes exactly the old image");
        oldUnknown.callback.onCaptureCompleted(unknownOld.o.d,oldUnknown.request,result(oldUnknown.request,1080000000L,100,5000000L));drain(unknownOld.o.k);
        check(abandonedCallback.deliveries==0&&unknownOld.callback.deliveries==0&&unknownOld.callback.failures==0,"late old frame never reaches fresh callback");

        scenario();Env interleaved=new Env();check(interleaved.seed(100,5000000L),"interleaved ownership fixture armed");
        CameraCaptureSession.Call interleavedOld=interleaved.latest();TECameraVideoRecorder$60 interleavedOldCallback=fresh(interleaved);
        Image retiredFirst=image(W,H,1080000000L,91,false),freshFirst=image(W,H,1160000000L,23,false);
        check(interleaved.arrival(retiredFirst)&&retiredFirst.closes==0,"unknown old image held before new shot");
        check(interleaved.arrival(freshFirst)&&freshFirst.closes==0,"fresh ordinary image also waits for positive result proof");
        interleaved.time=1160000000L;ordinaryResult(interleaved,interleaved.metadata(ordinaryRequest(100,5000000L),interleaved.time,100,5000000L));drain(interleaved.o.k);
        check(freshFirst.closes==1&&retiredFirst.closes==0&&interleaved.o.d.calls.size()==2,"fresh ordinary proof replays only its own full-resolution Image");
        CameraCaptureSession.Call freshSupplement=interleaved.latest();Image freshLeg=image(W,H,1240000000L,29,false);
        check(interleaved.arrival(freshLeg)&&freshLeg.closes==0,"new supplemental image before its callbacks is held while old request unresolved");drain(interleaved.o.k);
        freshSupplement.callback.onCaptureCompleted(interleaved.o.d,freshSupplement.request,interleaved.metadata(freshSupplement.request,1240000000L,100,5000000L));drain(interleaved.o.k);
        check(freshLeg.closes==1&&retiredFirst.closes==0&&interleaved.o.d.calls.size()==3,"pending result proof replays into active burst copy rather than original selected=false path");
        interleavedOld.callback.onCaptureCompleted(interleaved.o.d,interleavedOld.request,result(interleavedOld.request,1080000000L,100,5000000L));drain(interleaved.o.k);
        check(retiredFirst.closes==1&&deferred()==0,"old result retires only old timestamp after new burst leg");
        interleaved.time=1240000000L;interleaved.finishAll();delivered(interleaved,3,1160000000L);
        check(interleavedOldCallback.deliveries==0&&interleaved.callback.output.bytes[0]==23,"old pixels cannot become fresh reference");

        scenario();Env physicalLate=new Env();physicalLate.physical="inner-front";
        OpticalZoom.Route physicalRoute=new OpticalZoom.Route();physicalRoute.physicalId=physicalLate.physical;physicalRoute.lens=physicalLate.o.a;
        physicalLate.o.a.ids.add(physicalLate.physical);OpticalZoom.routes.put(physicalLate.o.j,physicalRoute);
        check(physicalLate.seed(100,5000000L),"explicit physical full-resolution sensor armed");
        CameraCaptureSession.Call physicalOld=physicalLate.latest();fresh(physicalLate);
        physicalOld.callback.onCaptureStarted(physicalLate.o.d,physicalOld.request,1081000000L,1);
        Image physicalImage=image(W,H,1080000000L,81,false);check(physicalLate.arrival(physicalImage)&&physicalImage.closes==0,"logical onStarted timestamp cannot prove physical image identity");
        physicalOld.callback.onCaptureCompleted(physicalLate.o.d,physicalOld.request,physicalLate.metadata(physicalOld.request,1080000000L,100,5000000L));drain(physicalLate.o.k);
        check(physicalImage.closes==1&&deferred()==0,"physical CaptureResult timestamp retires exact sensor image");

        scenario();Env noOwnershipResult=new Env();check(noOwnershipResult.seed(100,5000000L),"ownership timeout fixture armed");fresh(noOwnershipResult);
        Image noProofA=image(W,H,1080000000L,31,false),noProofB=image(W,H,1160000000L,33,false);
        check(noOwnershipResult.arrival(noProofA)&&noOwnershipResult.arrival(noProofB),"unproven old and fresh images held without mixing");drain(noOwnershipResult.o.k);
        SystemClock.now+=1200;noOwnershipResult.o.k.timers();drain(noOwnershipResult.o.k);
        check(noProofA.closes==1&&noProofB.closes==1&&deferred()==0,"ownership timeout reclaims all images of failed shutter once");
        check(noOwnershipResult.callback.failures==1&&noOwnershipResult.callback.noOpFailures==0&&noOwnershipResult.callback.deliveries==0,"timeout invokes real one-argument error callback once");

        scenario();Env paused=new Env();check(paused.seed(100,5000000L),"paused deferred fixture armed");fresh(paused);
        Image pausedImage=image(W,H,1160000000L,17,false);check(paused.arrival(pausedImage)&&pausedImage.closes==0,"paused fixture owns one held image");
        BurstCapture1933.canceled(paused.o);paused.o.k.reject=true;
        check(pausedImage.closes==1&&deferred()==0&&paused.callback.failures==0,"release synchronously reclaims deferred images with no active State or live handler");

        scenario();Env noHandler=new Env();check(noHandler.seed(100,5000000L),"rejected deferred handler fixture armed");fresh(noHandler);noHandler.o.k.reject=true;
        Image rejectedDeferred=image(W,H,1160000000L,17,false);check(noHandler.arrival(rejectedDeferred),"rejected post does not return an owned image to original listener");
        check(rejectedDeferred.closes==1&&noHandler.callback.failures==1&&noHandler.callback.noOpFailures==0&&deferred()==0,"rejected deferred post explicitly fails shutter and releases Image");

        scenario();Env replayThrows=new Env();replayThrows.useStock=true;check(replayThrows.seed(100,5000000L),"stock replay failure fixture armed");fresh(replayThrows);
        Image throwingImage=image(W,H,1160000000L,19,false);check(replayThrows.arrival(throwingImage)&&throwingImage.closes==0,"stock replay failure begins with owned deferred Image");
        replayThrows.o.listenerThrows=true;ordinaryResult(replayThrows,replayThrows.metadata(ordinaryRequest(100,5000000L),1160000000L,100,5000000L));drain(replayThrows.o.k);
        check(throwingImage.closes==1&&replayThrows.callback.failures==1&&replayThrows.callback.noOpFailures==0&&deferred()==0,"listener throwing after replay acquire cannot leak Image or shutter");

        scenario();Env replayPending=new Env();check(replayPending.seed(100,5000000L),"custom pending replay fixture armed");CameraCaptureSession.Call replayOld=replayPending.latest();fresh(replayPending);
        Image pendingReplay=image(W,H,1160000000L,37,false);check(replayPending.arrival(pendingReplay)&&pendingReplay.closes==0,"new image waits for retired request timestamp");
        replayOld.callback.onCaptureStarted(replayPending.o.d,replayOld.request,1080000000L,1);drain(replayPending.o.k);
        check(replayPending.reader.pending==pendingReplay&&pendingReplay.closes==0&&deferred()==0,"successful custom replay transfers to pending owner without premature close");
        replayPending.time=1160000000L;ordinaryResult(replayPending,replayPending.metadata(ordinaryRequest(100,5000000L),replayPending.time,100,5000000L));drain(replayPending.o.k);
        check(pendingReplay.closes==1&&replayPending.o.d.calls.size()==2,"later result consumes retained replay image once");replayPending.finishAll();delivered(replayPending,3,1160000000L);

        scenario();Env missingMetadata=new Env();check(missingMetadata.seed(100,5000000L),"missing metadata fixture armed");ShotContext1932.allow=false;missingMetadata.finishAll();
        check(missingMetadata.callback.deliveries==0&&missingMetadata.callback.failures==1&&missingMetadata.callback.noOpFailures==0&&ExitJobs185.count==0,"metadata association failure reports one real error and balances capture job");

        scenario();Env rejectedRenderer=new Env();check(rejectedRenderer.seed(100,5000000L),"renderer rejection fixture armed");rejectedRenderer.callback.throwDelivery=true;rejectedRenderer.finishAll();
        check(rejectedRenderer.callback.deliveries==0&&rejectedRenderer.callback.failures==1&&rejectedRenderer.callback.noOpFailures==0&&ExitJobs185.count==0,"renderer exception rolls back delivery claim and invokes actual error callback");

        scenario();Env reusedCallback=new Env();check(reusedCallback.seed(100,5000000L),"callback reuse first shot armed");reusedCallback.finishAll();reusedCallback.time+=200000000L;
        check(reusedCallback.seed(100,5000000L),"same native callback may be reused for a new shutter epoch");reusedCallback.finishAll();
        check(reusedCallback.callback.deliveries==2&&reusedCallback.callback.failures==0&&ShotContext1932.calls==2&&ExitJobs185.count==0,"completion guard is scoped to shot epoch, not permanent callback identity");

        scenario();Env readerReplaced=new Env();ImageReader obsoleteReader=readerReplaced.o.e0;readerReplaced.o.e0=new ImageReader(W,H,35);
        Image obsoleteImage=image(W,H,1,13,false);obsoleteReader.offer(obsoleteImage);Image acquiredObsolete=BurstCapture1933.acquireStill(readerReplaced.listener,obsoleteReader);
        check(BurstCapture1933.stillImage(readerReplaced.listener,acquiredObsolete)&&obsoleteImage.closes==1,"old listener reader cannot impersonate newly installed still reader");
        check(BurstCapture1933.acquireStill(readerReplaced.listener,obsoleteReader)==null,"empty original acquire preserves null semantics");

        scenario();Env fallbackWorker=new Env();check(fallbackWorker.seed(100,5000000L),"worker fallback continuation fixture armed");
        Field activeField=BurstCapture1933.class.getDeclaredField("active");activeField.setAccessible(true);Object activeState=activeField.get(null);
        java.lang.reflect.Method fallbackMethod=BurstCapture1933.class.getDeclaredMethod("deliverFallbackSoon",activeState.getClass());fallbackMethod.setAccessible(true);fallbackMethod.invoke(null,activeState);drain(fallbackWorker.o.k);
        check(fallbackWorker.callback.deliveries==1&&fallbackWorker.callback.failures==0&&ShotContext1932.calls==1&&FusionPixels1933.calls==0&&ExitJobs185.count==0,"worker-unavailable continuation delivers existing real frame through live handler");

        scenario();Env frameDuration=new Env();frameDuration.o.a.put(CameraCharacteristics.SENSOR_INFO_MAX_FRAME_DURATION,30000000L);
        check(frameDuration.seed(1600,25000000L),"sensor maximum frame duration fixture armed");frameDuration.emit(false);
        check(frameDuration.latest().request.get(CaptureRequest.SENSOR_EXPOSURE_TIME)<=30000000L
                &&frameDuration.latest().request.get(CaptureRequest.SENSOR_FRAME_DURATION)>=frameDuration.latest().request.get(CaptureRequest.SENSOR_EXPOSURE_TIME),"manual exposure never exceeds supported frame duration");BurstCapture1933.canceled(frameDuration.o);drain(frameDuration.o.k);

        scenario();Env interruptedWorker=new Env();check(interruptedWorker.seed(100,5000000L),"interruptible worker fixture armed");FusionPixels1933.blockProbe=true;
        CameraCaptureSession.Call interruptedCall=interruptedWorker.latest();Image interruptedImage=image(W,H,1080000000L,11,false);
        interruptedCall.callback.onCaptureStarted(interruptedWorker.o.d,interruptedCall.request,1080000000L,1);
        interruptedCall.callback.onCaptureCompleted(interruptedWorker.o.d,interruptedCall.request,result(interruptedCall.request,1080000000L,100,5000000L));
        check(interruptedWorker.arrival(interruptedImage)&&interruptedImage.closes==1,"probe source ownership released before CPU work");
        for(int i=0;i<1000&&!FusionPixels1933.probeEntered;i++){interruptedWorker.o.k.one();Thread.sleep(1);}
        check(FusionPixels1933.probeEntered&&running(),"probe worker is running before cancellation");BurstCapture1933.canceled(interruptedWorker.o);drain(interruptedWorker.o.k);
        check(FusionPixels1933.probeInterrupted&&!running()&&ExitJobs185.count==0&&interruptedWorker.callback.deliveries==0,"cancel interrupts active CPU worker and suppresses retired delivery");

        scenario();FusionPixels1933.blockProbe=true;final Env rejectedCancel=new Env();check(rejectedCancel.seed(100,5000000L),"cancel plus rejected handler fixture armed");
        CameraCaptureSession.Call rejectCall=rejectedCancel.latest();Image rejectImage=image(W,H,1080000000L,11,false);
        rejectCall.callback.onCaptureStarted(rejectedCancel.o.d,rejectCall.request,1080000000L,1);
        rejectCall.callback.onCaptureCompleted(rejectedCancel.o.d,rejectCall.request,result(rejectCall.request,1080000000L,100,5000000L));
        check(rejectedCancel.arrival(rejectImage)&&rejectImage.closes==1,"rejected-handler probe releases Image before work");
        for(int i=0;i<3000&&!FusionPixels1933.probeEntered;i++){rejectedCancel.o.k.one();Thread.sleep(1);}
        check(FusionPixels1933.probeEntered,"rejected-handler probe is running before cancellation");rejectedCancel.o.k.reject=true;
        final Throwable[] rejectedCancelFailure=new Throwable[1];Thread rejectedCancelThread=new Thread(new Runnable(){public void run(){try{BurstCapture1933.canceled(rejectedCancel.o);}catch(Throwable e){rejectedCancelFailure[0]=e;}}});
        rejectedCancelThread.start();rejectedCancelThread.join(3000);
        check(!rejectedCancelThread.isAlive()&&rejectedCancelFailure[0]==null,"cancellation and worker handler rejection cannot deadlock each other");
        check(rejectedCancel.o.k.postRejections>0&&FusionPixels1933.probeInterrupted,"interrupted worker actually attempts rejected continuation");
        check(!running()&&ExitJobs185.count==0&&FusionPixels1933.Analysis.retained==0,"rejected continuation releases worker, preparation and capture job");
        check(rejectedCancel.callback.deliveries==0&&rejectedCancel.callback.failures==0,"canceled rejected continuation cannot signal retired callback");

        deferredFailureWhileWorker(true);
        deferredFailureWhileWorker(false);

        scenario();FusionPixels1933.blockFrameTimestamp=1000000000L;FusionPixels1933.permitCooperativeInterrupt=false;
        Env overlap=new Env();check(overlap.seed(100,5000000L),"overlap preprocessing shot armed");waitPreparation(overlap);
        check(overlap.o.d.calls.size()==1&&overlap.callback.deliveries==0,"supplement camera request dispatched while first-frame preparation is blocked");
        check(ExitJobs185.count==1,"pending camera and preparation share one capture job");
        FusionPixels1933.releasePreparation=true;overlap.finishAll();delivered(overlap,3,1000000000L);prepared(3,2);

        scenario();Env preppedNight=new Env();check(preppedNight.seed(1600,25000000L),"four-frame preparation fixture armed");
        preppedNight.finishAll();delivered(preppedNight,4,1000000000L);prepared(4,3);

        scenario();FusionPixels1933.blockPairCandidateTimestamp=1080000000L;FusionPixels1933.permitCooperativeInterrupt=false;
        Env pairOverlap=new Env();check(pairOverlap.seed(1600,25000000L),"pair preprocessing overlap fixture armed");pairOverlap.emit(true);waitPreparation(pairOverlap);
        check(pairOverlap.o.d.calls.size()==2&&pairOverlap.callback.deliveries==0,"next sensor request dispatched while earlier pair preparation is blocked");
        FusionPixels1933.releasePreparation=true;pairOverlap.finishAll();delivered(pairOverlap,4,1000000000L);prepared(4,3);

        scenario();FusionPixels1933.blockPairCandidateTimestamp=1160000000L;FusionPixels1933.permitCooperativeInterrupt=false;
        final Env finalPair=new Env();check(finalPair.seed(100,5000000L),"final pair join fixture armed");finalPair.emit(false);
        final Throwable[] finalFailure=new Throwable[1];Thread finalEmitter=new Thread(new Runnable(){public void run(){try{finalPair.emit(true);}catch(Throwable e){finalFailure[0]=e;}}});finalEmitter.start();waitPreparation(finalPair);
        check(FusionPixels1933.calls==0&&finalPair.callback.deliveries==0&&ExitJobs185.count==1,"fuse and native renderer wait for queued final pair preparation");
        FusionPixels1933.releasePreparation=true;finalEmitter.join(3000);check(!finalEmitter.isAlive()&&finalFailure[0]==null,"final queued pair joins before completion");
        drain(finalPair.o.k);delivered(finalPair,3,1000000000L);prepared(3,2);

        scenario();Env preppedMismatch=new Env();check(preppedMismatch.seed(100,5000000L),"metadata mismatch preparation fixture armed");
        preppedMismatch.emit(preppedMismatch.latest(),true,1,false,false);delivered(preppedMismatch,1,1000000000L);prepared(1,0);

        scenario();FusionPixels1933.blockFrameTimestamp=1000000000L;FusionPixels1933.permitCooperativeInterrupt=false;
        final Env canceledPreparation=new Env();check(canceledPreparation.seed(100,5000000L),"noncooperative preparation cancellation fixture armed");waitPreparation(canceledPreparation);
        final Throwable[] cancelFailure=new Throwable[1];Thread cancelThread=new Thread(new Runnable(){public void run(){try{BurstCapture1933.canceled(canceledPreparation.o);}catch(Throwable e){cancelFailure[0]=e;}}});
        cancelThread.start();waitInterrupted();
        check(ExitJobs185.count==1,"cancel retains ExitJobs ownership until preparation actually exits");
        Env blockedNewShot=new Env();check(!blockedNewShot.seed(100,5000000L)&&blockedNewShot.o.d.calls.isEmpty(),"canceled running preparation keeps capture gate closed to another shot");
        check(canceledPreparation.callback.deliveries==0,"canceled preprocessing cannot deliver retired pixels");
        FusionPixels1933.releasePreparation=true;cancelThread.join(3000);check(!cancelThread.isAlive()&&cancelFailure[0]==null,"cancellation completes after preparation release without lifecycle error");
        drain(canceledPreparation.o.k);check(ExitJobs185.count==0&&FusionPixels1933.preparationRunning==0,"cancel releases capture ownership after running preparation has exited");
        check(canceledPreparation.callback.deliveries==0&&canceledPreparation.callback.failures==0,"canceled preprocessing sends no stale renderer/failure callback");
        Env afterPreparation=new Env();check(afterPreparation.seed(100,5000000L),"capture gate reopens after canceled preparation exits");afterPreparation.finishAll();delivered(afterPreparation,3,1000000000L);

        check(ExitJobs185.count==0,"all scenarios finish with balanced counter");
        check(FusionPixels1933.Analysis.retained==0,"all scenarios release capture analysis");
        System.out.println("CAPTURE_1948_PASS assertions="+assertions+" scenarios="+scenarios);
    }
}


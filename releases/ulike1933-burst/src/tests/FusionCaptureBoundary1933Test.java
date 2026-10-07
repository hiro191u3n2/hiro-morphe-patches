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

/** Production capture policy/coordinator AND real NV21 fusion, joined across
 * scripted Android callbacks. Acquisition/replay races live in Capture1933Test;
 * these two cases prove the CPU boundary actually delivers noise-reduced pixels
 * with the chosen group's metadata. This is not an Android/ART/device test. */
public final class FusionCaptureBoundary1933Test {
    private static int assertions,scenarios;
    private static final int W=2000,H=2000;
    public static final class Owner {
        public Object x0,y0; public i.s.a.w.i g=new i.s.a.w.i();
        public CameraDevice j=new CameraDevice("front");
        public CameraCaptureSession d=new CameraCaptureSession();
        public CameraCharacteristics a=characteristics(true);
        public Handler k=new Handler(); public ImageReader e0=new ImageReader(W,H,35);public int m0=1;
    }
    public static final class ReaderState {public ImageReader yuv;ReaderState(ImageReader r){yuv=r;}}
    public static final class Listener {public Owner a;Listener(Owner o){a=o;}}
    private static final class Env {
        final Owner o=new Owner(); final TECameraVideoRecorder$60 callback=new TECameraVideoRecorder$60();
        final ReaderState reader=new ReaderState(o.e0);
        int iso;long exposure;long time=1000000000L;boolean useStock;
        Env(){o.x0=callback;}
        boolean seed(int i,long e)throws Exception {
            iso=i;exposure=e;
            CaptureRequest.Builder b=new CaptureRequest.Builder();
            b.set(CaptureRequest.CONTROL_CAPTURE_INTENT,CaptureRequest.CONTROL_CAPTURE_INTENT_STILL_CAPTURE);
            b.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON);
            b.set(CaptureRequest.SENSOR_EXPOSURE_TIME,e);b.set(CaptureRequest.SENSOR_SENSITIVITY,i);
            TotalCaptureResult r=result(b.build(),time,i,e);
            Image image=image(W,H,time,7,false);
            check(!arrival(image),"first real still passes reader guard");
            boolean accepted=BurstCapture1933.beginImage(o,image,r);
            check(image.closes==0,"seed Image ownership remains with SDK caller");
            image.close(); drain(o.k);return accepted;
        }
        boolean arrival(Image image){return useStock?BurstCapture1933.stillImage(new Listener(o),image):BurstCapture1933.yuvImage(o,reader,image);}
        CameraCaptureSession.Call latest(){return o.d.calls.get(o.d.calls.size()-1);}
        void emit(boolean imageFirst)throws Exception {emit(latest(),imageFirst,0,false,false);}
        void emit(CameraCaptureSession.Call c,boolean imageFirst,long mismatch,boolean badColour,boolean badGeometry)throws Exception {
            time+=80000000L;
            Integer si=c.request.get(CaptureRequest.SENSOR_SENSITIVITY);Long se=c.request.get(CaptureRequest.SENSOR_EXPOSURE_TIME);
            int actualIso=si==null?iso:si;long actualExposure=se==null?exposure:se;
            TotalCaptureResult r=result(c.request,time+mismatch,actualIso,actualExposure);
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
            for(int step=0;step<8&&callback.deliveries==0;step++){
                int n=o.d.calls.size();if(n==previous)break;previous=n;
                emit((step&1)==0);
            }
            drain(o.k);
        }
    }

    private static void check(boolean b,String label){assertions++;if(!b)throw new AssertionError(label);}
    private static void scenario(){scenarios++; reset();}
    private static void reset(){
        PhotoDetail.current=new PhotoDetail.Settings(true,2);ShotContext1932.calls=0;ShotContext1932.allow=true;
        ShotContext1932.timestamp=0;ShotContext1932.iso=0;
        OpticalZoom.routes.clear();ExitJobs185.sealed=false;
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
        byte[] pixels=FusionPixels1933Test.frame(w,h,1,0,0,5,time);
        for(int yy=0;yy<h;yy++)System.arraycopy(pixels,yy*w,y.array(),pos+yy*yr,w);
        for(int yy=0;yy<h/2;yy++)for(int xx=0;xx<w/2;xx++){
            v.array()[pos+yy*uvr+xx]=pixels[w*h+yy*w+xx*2];
            u.array()[pos+yy*uvr+xx]=pixels[w*h+yy*w+xx*2+1];
        }
        y.position(pos);u.position(pos);v.position(pos);
        return new Image(w,h,35,time,new Image.Plane[]{new Image.Plane(y,yr,1),new Image.Plane(u,uvr,1),new Image.Plane(v,uvr,1)});
    }
    private static boolean running()throws Exception{Field f=BurstCapture1933.class.getDeclaredField("fusionRunning");f.setAccessible(true);return f.getBoolean(null);}
    private static void drain(Handler h)throws Exception {
        int quiet=0;
        for(int i=0;i<3000;i++){
            boolean ran=h.one();if(!running()&&h.empty()&&!ran){if(++quiet==3)return;}else quiet=0;
            if(!ran)Thread.sleep(1);
        }
        throw new AssertionError("coordinator did not quiesce");
    }
    private static void delivered(Env e,int frames,long timestamp){
        check(e.callback.deliveries==1,"renderer receives exactly one final image");
        check(e.callback.failures==0,"successful capture has no duplicate failure callback");
        check(e.callback.output.width==W&&e.callback.output.height==H,"full sensor image dimensions retained");
        check(e.callback.output.rotation==270,"front orientation matches native Q0");
        check(ShotContext1932.calls==1&&ShotContext1932.timestamp==timestamp,"metadata binds selected reference exactly once");
        check(ExitJobs185.count==0,"capture job released after render callback");
    }

    public static void main(String[] args)throws Exception {
        scenario();Env normal=new Env();check(normal.seed(100,5000000L),"real fusion normal start");
        normal.finishAll();delivered(normal,3,1000000000L);
        check(normal.o.d.calls.size()==2,"real normal requests probe plus third frame");
        byte[] clean=FusionPixels1933Test.frame(W,H,1,0,0,0,0);
        byte[] reference=FusionPixels1933Test.frame(W,H,1,0,0,5,1000000000L);
        double before=FusionPixels1933Test.mse(reference,clean,W,H,20);
        double after=FusionPixels1933Test.mse(normal.callback.output.bytes,clean,W,H,20);
        check(after<before*.8,"adapter delivered actual NR result");
        int normalAssertions=assertions;long normalRatio=Math.round(after/before*1000000);
        scenario();Env night=new Env();check(night.seed(1600,25000000L),"real night start");
        night.emit(true);
        check(night.latest().request.get(CaptureRequest.SENSOR_EXPOSURE_TIME)==50000000L,"real static probe selected longer shutter");
        night.finishAll();delivered(night,4,1160000000L);
        check(night.o.d.calls.size()==5,"night has initial probe plus four new group frames");
        reference=FusionPixels1933Test.frame(W,H,1,0,0,5,1160000000L);
        before=FusionPixels1933Test.mse(reference,clean,W,H,20);
        after=FusionPixels1933Test.mse(night.callback.output.bytes,clean,W,H,20);
        check(after<before*.8,"night delivers actual fused group with matching metadata");
        long nightRatio=Math.round(after/before*1000000);
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+
                ",\"scenarios\":{\"normal_three_real_frames_to_renderer\":"+normalAssertions+
                ",\"night_measured_probe_and_new_four_frame_group\":"+(assertions-normalAssertions)+
                "},\"metrics\":{\"normal_y_mse_ratio_ppm\":"+normalRatio+
                ",\"night_y_mse_ratio_ppm\":"+nightRatio+"},\"device_tested\":false}");
    }
}

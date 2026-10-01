package com.hiro.ulike.integration169;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.DataSpace;
import android.hardware.HardwareBuffer;
import android.hardware.camera2.*;
import android.hardware.camera2.params.*;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.Surface;
import com.hiro.ulike.hdr.capture.StillJoin;
import com.hiro.ulike.hdr.input.CaptureMatch;
import com.hiro.ulike.hdr.input.HdrFrame;
import com.hiro.ulike.hdr.input.P010FrameReader;
import com.hiro.ulike.hdr.input.android.AndroidP010FrameReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Actual Camera2 bridge for the pinned ULike app. Private candidate gate is deliberately off.
 * P010 output never enters stock Q0/NV21 or the stock Bitmap encoder. */
public final class CameraBridge169 {
    private CameraBridge169() {}
    private static final Map<Object,State> OWNERS=new IdentityHashMap<>();
    private static final Map<CameraCaptureSession,State> SESSIONS=new IdentityHashMap<>();
    private static final Map<CaptureRequest.Builder,State> BUILDERS=new IdentityHashMap<>();
    private static final Map<CaptureRequest,State> REQUESTS=new IdentityHashMap<>();
    private static final long OWNED_BUDGET=128L*1024*1024;
    /** Application coordinator must implement actual processed HDR+SDR and app URI completion.
     * No incomplete/default/no-beauty implementation is installed in this private candidate. */
    public interface Processor {
        void process(OwnedShot shot) throws Exception;
    }
    private static volatile Processor processor;
    public static void installProcessor(Processor value) {
        if(!CandidateGate169.enabled())throw new IllegalStateException("private candidate prerequisites incomplete");
        if(value==null)throw new NullPointerException("processor");processor=value;
    }
    public static final class OwnedShot {
        public final HdrFrame pixels;
        public final SavedUriHandoff169.Token handoff;
        public final com.hiro.ulike.binding.ShotStyleSettings.Snapshot style;
        public final Object recorder,bitmapCallback,shotIdentity;
        public final long shotEpoch,dateTakenMs;
        public final int sensorOrientation,lensFacing;
        private final Rect sensorCrop;
        public Rect sensorCrop(){return new Rect(sensorCrop);}
        private OwnedShot(HdrFrame f,AppHook169.Choice c,int orientation,int facing,Rect crop) {
            pixels=f;handoff=c.handoff;style=c.settings;recorder=c.recorder;bitmapCallback=c.bitmapCallback;shotIdentity=c.identity;shotEpoch=c.epoch;dateTakenMs=c.dateTakenMs;
            sensorOrientation=orientation;lensFacing=facing;sensorCrop=new Rect(crop);
        }
    }
    private static final class State {
        final Object owner;
        final CameraDevice device;
        final ImageReader original,p010;
        final String cameraId,physicalId;
        final Object opticalRoute;
        final CameraCharacteristics characteristics;
        final HandlerThread thread;
        final Handler handler;
        final StillJoin<Image,TotalCaptureResult> join=new StillJoin<>();
        CameraCaptureSession session;
        CaptureMatch.Context submitted;
        AppHook169.Choice choice;
        boolean ready,closed;
        long sequence;
        Runnable timeout;
        State(Object owner,CameraDevice device,ImageReader original,ImageReader p010,CameraCharacteristics c,String physicalId,Object route) {
            this.owner=owner;this.device=device;this.original=original;this.p010=p010;characteristics=c;this.physicalId=physicalId;opticalRoute=route;cameraId=device.getId();
            thread=new HandlerThread("ULikeP010-169");thread.start();handler=new Handler(thread.getLooper());
        }
    }
    /** Hook replaces only the still output while retaining existing preview/ZSL outputs verbatim.
     * No global HLG color space is forced on the old SDR preview. Actual still dataspace is verified. */
    public static boolean session(Object owner,CameraDevice device,SessionConfiguration original)throws CameraAccessException {
        if(!CandidateGate169.enabled())return false;
        if(processor==null || AppHook169.app==null)throw new IllegalStateException("complete application processor unavailable");
        if(Build.VERSION.SDK_INT<34)throw new IllegalStateException("explicit P010/HLG capture requires API34+");
        release(owner);
        State state=null;ImageReader readerOwner=null;
        try {
            ImageReader jpeg=(ImageReader)AppHook169.publicField(owner,"e0");
            if(jpeg==null || AppHook169.publicField(owner,"j")!=device || original.getSessionType()!=SessionConfiguration.SESSION_REGULAR)
                throw new IllegalArgumentException("unknown still owner/session");
            CameraCharacteristics logical=(CameraCharacteristics)AppHook169.publicField(owner,"a"),routeCharacteristics=logical;
            Object route=opticalRoute(device);String physicalId=null;
            if(route!=null) {
                if(!device.getId().equals(AppHook169.declaredField(route,"cameraId")) || Boolean.TRUE.equals(AppHook169.declaredField(route,"failed")))throw new IllegalStateException("stale lens route");
                Method physical=route.getClass().getDeclaredMethod("physical");physical.setAccessible(true);
                routeCharacteristics=(CameraCharacteristics)AppHook169.declaredField(route,"lens");
                if(Boolean.TRUE.equals(physical.invoke(route))) {
                    physicalId=(String)AppHook169.declaredField(route,"physicalId");
                    if(physicalId==null || !logical.getPhysicalCameraIds().contains(physicalId))throw new IllegalArgumentException("unknown physical route");
                }
            }
            requireProfile(logical);requireProfile(routeCharacteristics);
            int width=jpeg.getWidth(),height=jpeg.getHeight();
            if(width<2 || height<2 || (width&1)!=0 || (height&1)!=0 || (long)width*height*3>OWNED_BUDGET)throw new IllegalArgumentException("P010 sample budget");
            StreamConfigurationMap map=routeCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            boolean size=false;
            if(map!=null)for(Size s:orEmpty(map.getOutputSizes(ImageFormat.YCBCR_P010)))if(s.getWidth()==width&&s.getHeight()==height)size=true;
            if(!size)throw new IllegalArgumentException("exact current still size not advertised as normal P010; no scaling/JPEG fallback");
            ImageReader p010=ImageReader.newInstance(width,height,ImageFormat.YCBCR_P010,2,HardwareBuffer.USAGE_CPU_READ_OFTEN);readerOwner=p010;
            state=new State(owner,device,jpeg,p010,routeCharacteristics,physicalId,route);
            final State configuredState=state;
            ArrayList<OutputConfiguration> outputs=new ArrayList<>();int replaced=0;
            for(OutputConfiguration prior:original.getOutputConfigurations()) {
                if(prior.getSurfaces().contains(jpeg.getSurface())) {
                    if(prior.getSurfaces().size()!=1 || ++replaced!=1)throw new IllegalArgumentException("shared or duplicate still surface");
                    OutputConfiguration next=new OutputConfiguration(p010.getSurface());
                    next.setDynamicRangeProfile(DynamicRangeProfiles.HLG10);next.setTimestampBase(OutputConfiguration.TIMESTAMP_BASE_SENSOR);
                    next.setReadoutTimestampEnabled(false);next.setMirrorMode(OutputConfiguration.MIRROR_MODE_NONE);
                    if(physicalId!=null)next.setPhysicalCameraId(physicalId);outputs.add(next);
                } else outputs.add(prior);
            }
            if(replaced!=1)throw new IllegalArgumentException("exact original still surface absent");
            CameraCaptureSession.StateCallback originalCallback=original.getStateCallback();
            SessionConfiguration config=new SessionConfiguration(original.getSessionType(),outputs,original.getExecutor(),new CameraCaptureSession.StateCallback(){
                @Override public void onConfigured(CameraCaptureSession session) {
                    synchronized(CameraBridge169.class) {
                        if(configuredState.closed || OWNERS.get(owner)!=configuredState || session.getDevice()!=device) { session.close();return; }
                        configuredState.session=session;configuredState.ready=true;SESSIONS.put(session,configuredState);
                    }
                    originalCallback.onConfigured(session);
                }
                @Override public void onConfigureFailed(CameraCaptureSession failed) { releaseExact(owner,configuredState);originalCallback.onConfigureFailed(failed); }
                @Override public void onReady(CameraCaptureSession session) { originalCallback.onReady(session); }
                @Override public void onActive(CameraCaptureSession session) { originalCallback.onActive(session); }
                @Override public void onCaptureQueueEmpty(CameraCaptureSession session) { originalCallback.onCaptureQueueEmpty(session); }
                @Override public void onSurfacePrepared(CameraCaptureSession session,Surface surface) { originalCallback.onSurfacePrepared(session,surface); }
                @Override public void onClosed(CameraCaptureSession session) { releaseExact(owner,configuredState);originalCallback.onClosed(session); }
            });
            if(original.getInputConfiguration()!=null)config.setInputConfiguration(original.getInputConfiguration());
            if(original.getSessionParameters()!=null)config.setSessionParameters(original.getSessionParameters());
            if(!device.isSessionConfigurationSupported(config))throw new IllegalArgumentException("exact preview+P010 session rejected");
            p010.setOnImageAvailableListener(reader->image(configuredState,reader),configuredState.handler);
            synchronized(CameraBridge169.class) { OWNERS.put(owner,state); }
            device.createCaptureSession(config);return true;
        } catch(CameraAccessException | RuntimeException e) { if(state!=null)close(state);else if(readerOwner!=null)readerOwner.close();throw e; }
        catch(Exception e) { if(state!=null)close(state);else if(readerOwner!=null)readerOwner.close();throw new IllegalStateException("P010 session binding failed",e); }
    }
    private static Size[] orEmpty(Size[] values) { return values==null?new Size[0]:values; }
    private static void requireProfile(CameraCharacteristics characteristics) {
        if(characteristics==null)throw new IllegalArgumentException("camera characteristics missing");
        DynamicRangeProfiles ranges=characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES);
        if(ranges==null || !ranges.getSupportedProfiles().contains(DynamicRangeProfiles.HLG10))throw new IllegalArgumentException("HLG10 not advertised");
        if(characteristics.getAvailableCaptureRequestKeys()==null || !characteristics.getAvailableCaptureRequestKeys().contains(CaptureRequest.SCALER_ROTATE_AND_CROP))throw new IllegalArgumentException("cannot fix input orientation");
    }
    public static synchronized boolean ready() { if(!CandidateGate169.enabled())return false;for(State s:OWNERS.values())if(s.ready&&!s.closed)return true;return false; }
    /** null means private gate is disabled; enabled but invalid state throws, never silently returns JPEG. */
    public static ImageReader reader(Object owner,ImageReader original) {
        if(!CandidateGate169.enabled())return null;
        synchronized(CameraBridge169.class) {
            State s=OWNERS.get(owner);
            try {
                requireCurrent(s);
                if(!s.ready || s.original!=original || s.choice!=null || s.submitted!=null)throw new IllegalStateException("P010 reader not idle and ready");
                s.choice=AppHook169.forCamera(owner);return s.p010;
            } catch(Exception e) { throw new IllegalStateException("P010 shot rejected",e); }
        }
    }
    public static synchronized void configureRequest(Object owner,CaptureRequest.Builder builder) {
        if(!CandidateGate169.enabled())return;State s=OWNERS.get(owner);if(s==null || s.choice==null)return;
        if(builder==null)throw new IllegalArgumentException("still request builder missing");
        if(BUILDERS.containsKey(builder)){fail(s,"builder",new IllegalStateException("still builder reused before submission"));throw new IllegalStateException("still builder reused before submission");}
        BUILDERS.put(builder,s);
        try{builder.set(CaptureRequest.SCALER_ROTATE_AND_CROP,CaptureRequest.SCALER_ROTATE_AND_CROP_NONE);
        if(s.characteristics.getAvailableCaptureRequestKeys().contains(CaptureRequest.SENSOR_PIXEL_MODE))builder.set(CaptureRequest.SENSOR_PIXEL_MODE,CaptureRequest.SENSOR_PIXEL_MODE_DEFAULT);}
        catch(RuntimeException e){fail(s,"configure",e);throw e;}
    }
    /** Pinned J0/H0 add the reader returned by reader() before W0 invokes configureRequest.
     * Replacing the subsequent Builder.build call binds the ACTUAL request without hidden
     * CaptureRequest.getTargets APIs or modifying the application's request tag. */
    public static synchronized CaptureRequest build(CaptureRequest.Builder builder) {
        State state=BUILDERS.remove(builder);CaptureRequest request;
        try{request=com.hiro.ulike.OpticalZoom.build(builder);}catch(RuntimeException e){if(state!=null)fail(state,"build",e);throw e;}
        if(state!=null) {
            try { requireCurrent(state);if(state.choice==null)throw new IllegalStateException("still builder has no shot"); }
            catch(Exception e){fail(state,"build",e);throw new IllegalStateException("stale still builder",e);}
            REQUESTS.put(request,state);
        }
        return request;
    }
    /** Wraps the existing OpticalZoom.capture at pinned j/k sites; lens observers remain active. */
    public static int capture(CameraCaptureSession session,CaptureRequest request,CameraCaptureSession.CaptureCallback original,Handler callbackHandler)throws CameraAccessException {
        final State s;
        synchronized(CameraBridge169.class) { s=REQUESTS.remove(request); }
        if(!CandidateGate169.enabled() || s==null)return com.hiro.ulike.OpticalZoom.capture(session,request,original,callbackHandler);
        synchronized(CameraBridge169.class) { if(SESSIONS.get(session)!=s){fail(s,"submit",new IllegalStateException("still request used with another session"));throw new IllegalStateException("still request used with another session");} }
        if(request.isReprocess()){fail(s,"submit",new IllegalArgumentException("8-bit reprocessed input cannot become HDR"));throw new IllegalArgumentException("8-bit reprocessed input cannot become HDR");}
        final CameraCaptureSession.CaptureCallback wrapper=new CameraCaptureSession.CaptureCallback(){
            @Override public void onCaptureCompleted(CameraCaptureSession actual,CaptureRequest received,TotalCaptureResult result) {
                if(actual==session && received==request)post(s,request,session,()->result(s,result));
                if(original!=null)original.onCaptureCompleted(actual,received,result);
            }
            @Override public void onCaptureFailed(CameraCaptureSession actual,CaptureRequest received,CaptureFailure failure) {
                if(actual==session && received==request)post(s,request,session,()->failExact(s,request,session,"capture",new IllegalStateException("capture failure "+failure.getReason())));
                if(original!=null)original.onCaptureFailed(actual,received,failure);
            }
            @Override public void onCaptureBufferLost(CameraCaptureSession actual,CaptureRequest received,Surface target,long number) {
                if(actual==session && received==request && target==s.p010.getSurface())post(s,request,session,()->failExact(s,request,session,"buffer",new IllegalStateException("P010 buffer lost")));
                if(original!=null)original.onCaptureBufferLost(actual,received,target,number);
            }
            @Override public void onCaptureStarted(CameraCaptureSession actual,CaptureRequest received,long timestamp,long number) { if(original!=null)original.onCaptureStarted(actual,received,timestamp,number); }
            @Override public void onCaptureProgressed(CameraCaptureSession actual,CaptureRequest received,CaptureResult partial) { if(original!=null)original.onCaptureProgressed(actual,received,partial); }
            @Override public void onCaptureSequenceCompleted(CameraCaptureSession actual,int sequenceId,long frameNumber) { if(original!=null)original.onCaptureSequenceCompleted(actual,sequenceId,frameNumber); }
            @Override public void onCaptureSequenceAborted(CameraCaptureSession actual,int sequenceId) {
                if(actual==session)post(s,request,session,()->failExact(s,request,session,"sequence",new IllegalStateException("P010 capture sequence aborted")));
                if(original!=null)original.onCaptureSequenceAborted(actual,sequenceId);
            }
        };
        synchronized(CameraBridge169.class) {
            try { requireCurrent(s);if(s.choice==null || s.submitted!=null)throw new IllegalStateException("shot not uniquely armed"); }
            catch(Exception e){fail(s,"submit",e);throw new IllegalStateException("camera generation changed",e);}
            s.submitted=new CaptureMatch.Context(s.owner,session,s.p010,request,wrapper,++s.sequence,s.cameraId,s.physicalId);
            s.join.begin(request);
            final CaptureMatch.Context submitted=s.submitted;
            s.timeout=()->{synchronized(CameraBridge169.class){if(s.submitted==submitted)fail(s,"timeout",new IllegalStateException("P010 shot timed out"));}};
            if(!s.handler.postDelayed(s.timeout,15000)){fail(s,"handler",new IllegalStateException("capture handler stopped"));throw new IllegalStateException("capture handler stopped");}
        }
        try { return com.hiro.ulike.OpticalZoom.capture(session,request,wrapper,callbackHandler); }
        catch(CameraAccessException | RuntimeException e){failExact(s,request,session,"submit",e);throw e;}
    }
    public static int burst(CameraCaptureSession session,List<CaptureRequest> requests,CameraCaptureSession.CaptureCallback callback,Handler handler)throws CameraAccessException {
        synchronized(CameraBridge169.class) { if(CandidateGate169.enabled())for(CaptureRequest r:requests)if(REQUESTS.containsKey(r)){State rejected=REQUESTS.get(r);fail(rejected,"burst",new IllegalStateException("burst HDR processing is not integrated in candidate169"));throw new IllegalStateException("burst HDR processing is not integrated in candidate169");} }
        return com.hiro.ulike.OpticalZoom.burst(session,requests,callback,handler);
    }
    private static void post(State s,CaptureRequest request,CameraCaptureSession session,Runnable task) { if(!s.handler.post(task))failExact(s,request,session,"handler",new IllegalStateException("capture handler unavailable")); }
    private static synchronized void failExact(State s,CaptureRequest request,CameraCaptureSession session,String stage,Exception error){if(s.submitted!=null && s.submitted.request==request && s.session==session)fail(s,stage,error);}
    private static void image(State s,ImageReader actual) {
        synchronized(CameraBridge169.class) {
            if(s.closed || actual!=s.p010)return;
            try { Image image;while((image=actual.acquireNextImage())!=null) {
                long timestamp;
                try { timestamp=image.getTimestamp(); } catch(RuntimeException e) { try{image.close();}catch(RuntimeException close){e.addSuppressed(close);}throw e; }
                StillJoin.Pair<Image,TotalCaptureResult> pair=s.join.image(image,timestamp);if(pair!=null)finish(s,pair);
            } }
            catch(Exception e){fail(s,"image",e);}
        }
    }
    private static void result(State s,TotalCaptureResult result) {
        synchronized(CameraBridge169.class) {
            if(s.closed || s.submitted==null || result.getRequest()!=s.submitted.request)return;
            try { CaptureResult metadata=metadata(s,result);Long time=metadata.get(CaptureResult.SENSOR_TIMESTAMP);if(time==null)throw new IllegalStateException("sensor timestamp missing");
                StillJoin.Pair<Image,TotalCaptureResult> pair=s.join.result(result.getRequest(),result,time);if(pair!=null)finish(s,pair);
            } catch(Exception e){fail(s,"result",e);}
        }
    }
    private static CaptureResult metadata(State s,TotalCaptureResult result) {
        CaptureResult selected=s.physicalId==null?result:result.getPhysicalCameraResults().get(s.physicalId);
        if(selected==null)throw new IllegalStateException("selected physical result missing");return selected;
    }
    private static void finish(State s,StillJoin.Pair<Image,TotalCaptureResult> pair)throws Exception {
        OwnedShot shot;final AppHook169.Choice processingChoice=s.choice;
        try(StillJoin.Pair<Image,TotalCaptureResult> owned=pair) {
            requireCurrent(s);final CaptureMatch.Context context=s.submitted;
            if(context==null || context.request!=pair.request || s.choice==null)throw new IllegalStateException("stale image pair");
            CaptureResult metadata=metadata(s,pair.result);Rect crop=metadata.get(CaptureResult.SCALER_CROP_REGION);
            Integer rotation=pair.result.get(CaptureResult.SCALER_ROTATE_AND_CROP),pixel=pair.result.get(CaptureResult.SENSOR_PIXEL_MODE);
            if(rotation==null || rotation!=CaptureResult.SCALER_ROTATE_AND_CROP_NONE || (pixel!=null && pixel!=CaptureResult.SENSOR_PIXEL_MODE_DEFAULT) || crop==null)throw new IllegalStateException("unverified still geometry");
            int data=owned.image().getDataSpace();HdrFrame.Encoding encoding;
            if(data==(DataSpace.STANDARD_BT2020|DataSpace.TRANSFER_HLG|DataSpace.RANGE_FULL))encoding=HdrFrame.Encoding.BT2020_NCL_HLG_FULL;
            else if(data==(DataSpace.STANDARD_BT2020|DataSpace.TRANSFER_HLG|DataSpace.RANGE_LIMITED))encoding=HdrFrame.Encoding.BT2020_NCL_HLG_LIMITED;
            else throw new IllegalStateException("actual still dataspace is not explicit BT2020 HLG");
            HdrFrame frame=AndroidP010FrameReader.copyBorrowed(owned.image(),new P010FrameReader.Request(s.p010.getWidth(),s.p010.getHeight(),encoding,OWNED_BUDGET),context,
                new CaptureMatch.Source(s.owner,s.session,s.p010,s.cameraId,s.physicalId,CaptureMatch.TimestampConvention.SENSOR_START_OF_EXPOSURE),pair.result,context,()->s.closed?null:s.submitted);
            Integer orientation=s.characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION),facing=s.characteristics.get(CameraCharacteristics.LENS_FACING);
            if(orientation==null||facing==null)throw new IllegalStateException("sensor orientation/facing missing");
            shot=new OwnedShot(frame,s.choice,orientation,facing,crop);
        }
        AppHook169.processing(s.choice);s.choice=null;s.submitted=null;if(s.timeout!=null)s.handler.removeCallbacks(s.timeout);s.timeout=null;
        // Owned pixels are now independent of ImageReader/CameraDevice. Processor must obtain
        // its real exclusive SDK lease before still analysis, and publish only processed pairs.
        final Processor current=processor;
        try{new Thread(()->{try{if(current==null)throw new IllegalStateException("processor removed");current.process(shot);}catch(Exception e){SavedUriHandoff169.fail(shot.handoff,"process",e);android.util.Log.e("ULike169","Save processor failed",e);}finally{AppHook169.consumed(processingChoice);}},"ULikeSave169").start();}
        catch(RuntimeException | Error failure){AppHook169.consumed(processingChoice);SavedUriHandoff169.fail(shot.handoff,"worker",new IllegalStateException("Cannot start save worker",failure));throw failure;}
    }
    private static Object opticalRoute(CameraDevice device)throws Exception {
        Method method=Class.forName("com.hiro.ulike.OpticalZoom").getDeclaredMethod("route",CameraDevice.class);method.setAccessible(true);return method.invoke(null,device);
    }
    private static void requireCurrent(State s)throws Exception {
        if(s!=null && s.choice!=null)AppHook169.requireLive(s.choice);
        if(s==null || s.closed || OWNERS.get(s.owner)!=s || AppHook169.publicField(s.owner,"j")!=s.device
                || AppHook169.publicField(s.owner,"e0")!=s.original || opticalRoute(s.device)!=s.opticalRoute
                || (s.opticalRoute!=null && Boolean.TRUE.equals(AppHook169.declaredField(s.opticalRoute,"failed"))))throw new IllegalStateException("camera/lens generation changed");
    }
    static synchronized void cancelChoice(AppHook169.Choice choice){for(State state:new ArrayList<>(OWNERS.values()))if(state.choice==choice)fail(state,"request-timeout",new IllegalStateException("Photo choice expired before owned copy"));}
    private static synchronized void fail(State s,String stage,Exception error) {
        if(s==null)return;BUILDERS.values().removeIf(value->value==s);REQUESTS.values().removeIf(value->value==s);AppHook169.Choice choice=s.choice;s.choice=null;s.submitted=null;
        if(s.timeout!=null)s.handler.removeCallbacks(s.timeout);s.timeout=null;
        try{s.join.cancel();}catch(Exception e){error.addSuppressed(e);}AppHook169.consumed(choice);
        if(choice!=null)SavedUriHandoff169.fail(choice.handoff,stage,error);
        android.util.Log.e("ULike169","Capture failed at "+stage,error);
    }
    private static synchronized void releaseExact(Object owner,State expected) { if(OWNERS.get(owner)==expected)release(owner); }
    public static synchronized void release(Object owner) { State state=OWNERS.remove(owner);if(state!=null)close(state); }
    private static synchronized void close(State state) {
        if(state.closed)return;state.closed=true;state.ready=false;OWNERS.remove(state.owner,state);if(state.session!=null)SESSIONS.remove(state.session);
        BUILDERS.values().removeIf(value->value==state);REQUESTS.values().removeIf(value->value==state);
        fail(state,"lifecycle",new IllegalStateException("camera session released"));
        try{state.join.close();}catch(Exception ignored){}try{state.p010.close();}finally{state.thread.quitSafely();}
    }
}

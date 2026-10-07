package com.hiro.ulike;
import android.media.*;
import android.hardware.camera2.TotalCaptureResult;
import java.util.IdentityHashMap;

/** Scripted ownership-equivalent receive/result boundary of the pinned helper. */
public final class CaptureYuv {
    private static final IdentityHashMap<Object,Capture1933Test.ReaderState> states=new IdentityHashMap<Object,Capture1933Test.ReaderState>();
    private static void receive(Object object,Capture1933Test.ReaderState state,ImageReader reader){
        Capture1933Test.Owner owner=(Capture1933Test.Owner)object;
        states.put(owner,state);
        Image image=BurstCapture1933.acquireYuv(owner,state,reader);
        if(BurstCapture1933.yuvImage(owner,state,image))return;
        if(image==null)return;
        if(state.closed || !state.selected || state.yuv!=reader){image.close();return;}
        if(state.pending!=null)state.pending.close();
        state.pending=image;
        deliver(owner,state);
    }
    public static void image(Object owner,Capture1933Test.ReaderState state,ImageReader reader){receive(owner,state,reader);}
    public static void result(Capture1933Test.Owner owner,Capture1933Test.ReaderState state,TotalCaptureResult result){
        states.put(owner,state);BurstCapture1933.observedResult(owner,result);state.result=result;deliver(owner,state);
    }
    private static void deliver(Capture1933Test.Owner owner,Capture1933Test.ReaderState state){
        if(state.pending==null || state.result==null || !state.selected)return;
        Image image=state.pending;TotalCaptureResult result=state.result;state.pending=null;state.result=null;
        try{Capture1933Test.originalQ0(owner,image,result);}finally{image.close();state.selected=false;}
    }
    private static void failed(Object owner){
        BurstCapture1933.canceled(owner);
        Capture1933Test.ReaderState state=states.get(owner);
        if(state!=null){if(state.pending!=null)state.pending.close();state.pending=null;state.result=null;state.selected=false;}
    }
}

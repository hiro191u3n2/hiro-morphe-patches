package com.ss.android.vesdk;
import i.s.a.w.*;
public final class TECameraVideoRecorder$60 {
    public int deliveries,failures,noOpFailures;public m output;public boolean throwDelivery;
    public void onPictureTaken(m frame,i info){if(throwDelivery)throw new IllegalStateException("scripted renderer rejection");deliveries++;output=frame;}
    public void onTakenFail(Exception e){failures++;}
    // Pinned 5.6.2 $60 delegates this overload to r.b, whose body is RETURN_VOID.
    public void onTakenFail(Exception e,int f){noOpFailures++;}
}


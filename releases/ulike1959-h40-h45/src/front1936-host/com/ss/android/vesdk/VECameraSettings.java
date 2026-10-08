package com.ss.android.vesdk;
public class VECameraSettings {
    public enum CAMERA_FACING_ID { FACING_BACK,FACING_FRONT }
    public CAMERA_FACING_ID facing=CAMERA_FACING_ID.FACING_FRONT;
    public CAMERA_FACING_ID getCameraFacing(){return facing;}
}

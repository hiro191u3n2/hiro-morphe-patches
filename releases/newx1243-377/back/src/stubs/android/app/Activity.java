package android.app;
public class Activity { public boolean finishing, destroyed; public int finishes; public boolean isFinishing(){return finishing;} public boolean isDestroyed(){return destroyed;} public void finishAndRemoveTask(){finishes++; finishing=true;} }

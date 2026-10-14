package com.hiro.ulike;
/** Host-only trace collector; excluded from runtime production inventories. */
public final class CameraTrace1965 {
 public static final java.util.List<String> events=new java.util.ArrayList<>();
 public static void event(String phase,long epoch,String fields){events.add(phase+"|"+epoch+"|"+fields);}
 public static void anomaly(String reason,long epoch,String fields){events.add(reason+"|"+epoch+"|"+fields);}
}

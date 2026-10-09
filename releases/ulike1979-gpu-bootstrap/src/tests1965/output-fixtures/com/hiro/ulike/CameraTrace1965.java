package com.hiro.ulike;
import java.util.ArrayList;
import java.util.List;
public final class CameraTrace1965 {
 public static final List<String> EVENTS=new ArrayList<String>();
 public static boolean fail;
 public static void event(String event,long epoch,String scalars){if(fail)throw new IllegalStateException("injected logger error");if(scalars.length()>160)throw new AssertionError("probe fields truncated by real trace limit");EVENTS.add(event+" epoch="+epoch+" "+scalars);}
}

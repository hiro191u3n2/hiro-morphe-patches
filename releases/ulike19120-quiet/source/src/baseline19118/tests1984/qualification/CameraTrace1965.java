package com.hiro.ulike;
import java.util.*;
/** Host sink only; this file is never part of the released DEX. */
public final class CameraTrace1965 {
    public static volatile boolean fail;
    public static final List<String> events=Collections.synchronizedList(new ArrayList<String>());
    public static void event(String phase,long epoch,String fields){
        if(fail)throw new AssertionError("injected trace failure");
        events.add(phase+" "+epoch+" "+fields);
    }
}

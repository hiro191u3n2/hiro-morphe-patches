package com.hiro.ulike;
import android.content.Context;
import java.util.ArrayList;
import java.util.List;
/** Event sink spy: tests the session observer, not persistent storage or Android. */
public final class CameraTrace1965 {
    public static final class Entry {
        public final String phase,fields;
        public final long epoch;
        public final boolean anomaly;
        Entry(String phase,long epoch,String fields,boolean anomaly){this.phase=phase;this.epoch=epoch;this.fields=fields;this.anomaly=anomaly;}
    }
    public static final List<Entry> entries=new ArrayList<>();
    public static int inits;
    public static boolean failInit;
    public static int eventFault,anomalyFault,initFault,faults;
    private static void fault(int mode){
        if(mode==0)return;faults++;
        if(mode==1)throw new RuntimeException("optional diagnostic runtime failure");
        throw new AssertionError("optional diagnostic error");
    }
    public static void event(String phase,long epoch,String fields){fault(eventFault);entries.add(new Entry(phase,epoch,fields,false));}
    public static void anomaly(String phase,long epoch,String fields){fault(anomalyFault);entries.add(new Entry(phase,epoch,fields,true));}
    public static void init(Context context){inits++;fault(initFault);if(failInit)throw new AssertionError("optional storage unavailable");}
    public static void reset(){entries.clear();inits=0;failInit=false;eventFault=anomalyFault=initFault=faults=0;}
}

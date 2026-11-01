package com.hiro.ulike;
import android.content.Context;
import java.util.*;
public final class CameraTrace1965 {
    static final List<String> rows=new ArrayList<>();
    public static void init(Context c){}
    public static void event(String phase,long epoch,String detail){rows.add(phase+" "+epoch+" "+detail);}
    public static void anomaly(String phase,long epoch,String detail){rows.add("ANOMALY "+phase+" "+epoch+" "+detail);}
}

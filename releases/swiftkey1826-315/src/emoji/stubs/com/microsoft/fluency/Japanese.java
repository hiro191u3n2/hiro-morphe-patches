package com.microsoft.fluency;
import java.util.Map;
import java.util.HashMap;
/** Native converter double. Exact expected inputs are registered by the host test. */
public class Japanese {
 public static final Map<String,String> readings=new HashMap<String,String>();
 public static String lastInput;
 public static int calls;
 public static String romajiToHiragana(String value){lastInput=value;calls++;return readings.containsKey(value)?readings.get(value):value;}
}

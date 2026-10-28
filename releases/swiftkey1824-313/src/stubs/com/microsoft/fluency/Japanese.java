package com.microsoft.fluency;
public class Japanese {
    public static String lastInput;
    public static int calls;
    public static String romajiToHiragana(String value) {
        lastInput=value; calls++;
        if (value.equals("neko")) return "ねこ";
        if (value.equals("koーhiー")) return "こーひー";
        return value;
    }
}

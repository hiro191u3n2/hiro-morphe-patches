package com.microsoft.fluency;
/** Native boundary fixture: checks arguments and supplies known kana for host tests.
 * This does not execute or reimplement the Android JNI library. */
public class Japanese {
    public static String lastInput;
    public static int calls;
    public static String romajiToHiragana(String value) {
        lastInput=value; calls++;
        if(value.equals("airabuyuー") || value.equals("airabuyu-")) return "あいらぶゆー";
        if(value.equals("sankyuー") || value.equals("sankyu-")) return "さんきゅー";
        if(value.equals("guddomoーningu") || value.equals("guddomo-ningu")) return "ぐっどもーにんぐ";
        if(value.equals("kamera")) return "かめら";
        if(value.equals("sushi")) return "すし";
        return value;
    }
}

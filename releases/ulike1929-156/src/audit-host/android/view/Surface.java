package android.view;public class Surface{public boolean valid=true;public int releases;public boolean isValid(){return valid;}public void release(){valid=false;releases++;}}

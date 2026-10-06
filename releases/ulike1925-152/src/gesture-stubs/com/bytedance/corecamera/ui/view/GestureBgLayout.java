package com.bytedance.corecamera.ui.view;
public class GestureBgLayout extends android.widget.RelativeLayout {
 public d c;
 public GestureBgLayout(android.content.Context context){super(context);}
 public interface d {boolean a(android.view.MotionEvent e);void onDoubleTap(android.view.MotionEvent e);}
}
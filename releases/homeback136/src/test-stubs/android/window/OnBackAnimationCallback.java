package android.window;public interface OnBackAnimationCallback extends OnBackInvokedCallback {void onBackStarted(BackEvent e);void onBackProgressed(BackEvent e);void onBackCancelled();}

package android.view.inputmethod; public interface InputConnection {default CharSequence getTextBeforeCursor(int n,int flags){return "";}default CharSequence getTextAfterCursor(int n,int flags){return "";}}


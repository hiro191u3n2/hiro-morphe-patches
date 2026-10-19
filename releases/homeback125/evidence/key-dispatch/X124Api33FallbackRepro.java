package app.hiro.oneback.runtime;
import android.view.View;
import android.window.OnBackInvokedCallback;
public final class X124Api33FallbackRepro {
 public static void main(String[] args) throws Exception {
  for(boolean resumeBeforeBack:new boolean[]{false,true}) {
   try (Back124TwitterTest.Scene s=new Back124TwitterTest.Scene(Back124TwitterTest.SEARCH)) {
    View retainedEditor=new View(s.activity);retainedEditor.editor=true;retainedEditor.hidden=true;
    s.root.focusView=retainedEditor;
    int[] fallbacks={0};
    OnBackInvokedCallback original=()->{
     fallbacks[0]++;
     // Actual l0.d fallback reaches patched ComponentActivity's terminal bridge,
     // bypassing MainActivity's p.onBackPressed and l1.goBack.
     if(!OneBackExit.tryTerminalBack(s.activity,0,false))s.activity.finish();
    };
    NativeBack.register(s.activity.getWindow().dispatcher(),0,original);
    OnBackInvokedCallback decorated=s.activity.getWindow().dispatcher().top();
    if(resumeBeforeBack)OneBackExit.onResume(s.activity);
    boolean callbackChanged=s.activity.getWindow().dispatcher().top()!=decorated;
    try{
     s.activity.getWindow().dispatcher().invokeBack();
     System.out.println("resumeBeforeBack="+resumeBeforeBack+" callbackChangedOnResume="+callbackChanged+" inputReady="+HomeScreen.inputReady(s.activity)+" fallbackCalls="+fallbacks[0]+" nativeRootCalls="+s.hookCalls+" finishing="+s.activity.finishing+" excluded="+s.task.excluded+" removeCalls="+s.task.removeCalls+" homeMarker="+Back124TwitterTest.marker(s.app));
     if(callbackChanged||fallbacks[0]!=1||s.hookCalls!=0||!s.activity.finishing||s.task.excluded||Back124TwitterTest.marker(s.app))throw new AssertionError("Expected exact v124 omission not reproduced");
    }finally{NativeBack.unregister(s.activity.getWindow().dispatcher(),original);}
   }
  }
 }
}

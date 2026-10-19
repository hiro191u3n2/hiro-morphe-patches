package app.hiro.oneback.runtime;
public final class X124MissingObserverRepro {
 public static void main(String[] args)throws Exception{
  try(Back124TwitterTest.Scene s=new Back124TwitterTest.Scene(Back124TwitterTest.CHAT)){
   boolean callback=s.activity.getWindow().dispatcher().top()!=null;
   // Framework's default Back is not an app registration. The Android16
   // Activity callback directly asks ActivityClient to background/finish.
   if(callback)s.activity.getWindow().dispatcher().invokeBack();else s.activity.finish();
   System.out.println("appCallbackRegistered="+callback+" nativeRootCalls="+s.hookCalls+" finishing="+s.activity.finishing+" excluded="+s.task.excluded+" removeCalls="+s.task.removeCalls+" homeMarker="+Back124TwitterTest.marker(s.app));
   if(callback||s.hookCalls!=0||s.task.excluded||Back124TwitterTest.marker(s.app))throw new AssertionError("Expected v124 root-observer gap not reproduced");
  }
 }
}

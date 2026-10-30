package android.app;
public class FragmentManager {
 public int count,popCalls;public boolean saved;public Runnable popAction;
 public java.util.List<Object> fragments=new java.util.ArrayList<Object>();
 public int getBackStackEntryCount(){return count;}
 public java.util.List<Object> getFragments(){return fragments;}
 public boolean isStateSaved(){return saved;}
 public boolean popBackStackImmediate(){if(saved||count<=0)return false;popCalls++;count--;if(popAction!=null)popAction.run();return true;}
 public Object primary;public Object getPrimaryNavigationFragment(){return primary;}
}

import app.hiro.newx.back.RootExit;
import android.app.Activity;
import com.x.android.main.ea;
import com.x.android.main.landing.g;
import com.x.main.api.landing.d0;
import com.x.navigation.vd;
import kotlin.jvm.functions.Function0;
public class BackRuntimeTest {
 static int assertions;
 static void check(boolean ok,String m){assertions++;if(!ok)throw new AssertionError(m);}
 static class Landing implements g,d0 {vd tab; int calls; boolean async,fail; Function0<?> completion; Landing(vd t){tab=t;}public d0 f(){return this;}public vd s(){return tab;}public void j(vd target,Function0<?> cb){calls++;check(target==vd.HOME,"select native home");if(fail)throw new IllegalStateException();tab=target;completion=cb;if(!async)cb.invoke();}}
 public static void main(String[] args){
  for(vd tab:new vd[]{vd.HOME,vd.EXPLORE,vd.NOTIFICATIONS,vd.DM}){Activity a=new Activity();RootExit.bind(a);ea r=new ea();Landing l=new Landing(tab);r.X=l;check(RootExit.enabled(r),"target root "+tab);check(RootExit.rootBack(r),"root back consumed "+tab);check(l.tab==vd.HOME&&a.finishes==1,"home then remove task "+tab);l.completion.invoke();check(a.finishes==1,"callback idempotence");}
  for(vd tab:new vd[]{vd.HOME,vd.EXPLORE,vd.NOTIFICATIONS,vd.DM}){Activity a=new Activity();RootExit.bind(a);ea r=new ea();Landing l=new Landing(tab);r.X=l;r.root=false;check(!RootExit.enabled(r),"inner stack excluded");check(!RootExit.rootBack(r)&&a.finishes==0&&l.calls==0,"inner back preserved");}
  for(vd tab:new vd[]{vd.GROK,vd.COMMUNITIES,vd.SPACES}){Activity a=new Activity();RootExit.bind(a);ea r=new ea();r.X=new Landing(tab);check(!RootExit.enabled(r)&&!RootExit.rootBack(r)&&a.finishes==0,"other root preserves original native callback");}
  Activity a=new Activity();RootExit.bind(a);ea r=new ea();Landing l=new Landing(vd.DM);l.async=true;r.X=l;check(RootExit.rootBack(r)&&a.finishes==0,"wait native navigation completion");l.completion.invoke();check(a.finishes==1,"finish on completion");
  a=new Activity();RootExit.bind(a);l=new Landing(vd.HOME);l.fail=true;r.X=l;check(RootExit.rootBack(r)&&a.finishes==1,"native selection exception terminal fallback");
  a=new Activity();a.destroyed=true;RootExit.bind(a);l=new Landing(vd.HOME);r.X=l;check(!RootExit.rootBack(r)&&l.calls==0,"destroyed activity excluded");
  a=new Activity();a.finishing=true;RootExit.bind(a);check(!RootExit.rootBack(r),"already finishing excluded");
  RootExit.bind(null);check(!RootExit.rootBack(r),"missing activity preserves original callback");
  r.X=null;check(!RootExit.enabled(r),"missing landing excluded");check(!RootExit.enabled(null)&&!RootExit.enabled(new Object()),"wrong root excluded");
  System.out.println("PASS BackRuntimeTest: "+assertions+" assertions");
 }
}

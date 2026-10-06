import android.app.*;
import android.content.*;
import android.os.Build;
import android.view.*;
import android.widget.*;
import android.window.*;
import app.hiro.quicksearch.runtime.SearchTask;
import jp.ddo.sugihiro.quicksearch.activity.MainActivity;
import jp.ddo.sugihiro.quicksearch.activity.WebActivity;
import jp.ddo.sugihiro.quicksearch.activity.ConfigActivity;
import java.util.Objects;

/** Verifies public runtime decisions against host fakes; not an Android framework/device emulator. */
public final class SearchTaskTest {
    private static final String PKG="jp.ddo.sugihiro.quicksearch";
    private static final int X_ID=0x7f0900cf, HISTORY_CLOSE_ID=0x7f090065;
    private static int assertions;
    private static void check(boolean ok,String reason){if(!ok)throw new AssertionError(reason);assertions++;}
    private static void same(Object expected,Object actual,String reason){
        if(!Objects.equals(expected,actual))throw new AssertionError(reason+": expected "+expected+", got "+actual);
        assertions++;
    }
    private static <T extends Activity>T setup(T a,int id,boolean root){a.taskId=id;a.taskRoot=root;return a;}
    private static void release(Activity... activities){for(Activity a:activities){SearchTask.detach(a);a.destroyed=true;}}
    private static ActivityManager.AppTask task(int id,String pkg){
        ActivityManager.AppTask task=new ActivityManager.AppTask();
        task.info.id=id;task.info.taskId=id;task.info.persistentId=id;
        task.info.baseActivity=pkg==null?null:new ComponentName(pkg,pkg+".Root");
        return task;
    }
    private static void overlay(TestBackDispatcher dispatcher,String reason){
        same(1,dispatcher.callbacks.size(),reason+" callback count");
        same(OnBackInvokedDispatcher.PRIORITY_OVERLAY,dispatcher.callbacks.values().iterator().next(),reason+" priority");
    }
    private static ImageButton button(Context context,int id,ViewGroup.LayoutParams layout){
        ImageButton button=new ImageButton(context);button.setId(id);button.setLayoutParams(layout);return button;
    }

    private static void searchFlags(){
        for(Boolean exit:new Boolean[]{null,false,true})for(int initial:new int[]{0x08000001,0x14000000}){
            MainActivity a=new MainActivity();if(exit!=null)a.preferences.put("exit",exit);
            Intent intent=new Intent("android.intent.action.VIEW").setFlags(initial).putExtra("query","日本語 Pokémon & test");
            SearchTask.startExternal(a,intent);
            same(1,a.startCalls,"search launched once");
            same(intent,a.startedIntent,"original intent identity retained");
            same(initial|Intent.FLAG_ACTIVITY_NEW_TASK,intent.getFlags(),"all external searches isolated while preserving flags");
            same("android.intent.action.VIEW",intent.getAction(),"search action retained");
            same("日本語 Pokémon & test",intent.getStringExtra("query"),"search payload retained");
            same(0,a.finishCalls+a.removeCalls,"search helper does not override original exit preference branch");
        }
        MainActivity failing=new MainActivity();failing.startFailure=new IllegalStateException("No resolver");
        RuntimeException result=null;try{SearchTask.startExternal(failing,new Intent());}catch(RuntimeException e){result=e;}
        same(failing.startFailure,result,"original app error path still sees launch failures");
        same(1,failing.startCalls,"launch failure not retried");
    }

    private static void ownedStackAndDuplicateEvents(){
        Build.VERSION.SDK_INT=36;
        MainActivity root=setup(new MainActivity(),100,true),otherTask=setup(new MainActivity(),101,true);
        WebActivity web=setup(new WebActivity(),100,false);
        ConfigActivity config=setup(new ConfigActivity(),100,false);
        Activity foreign=setup(new Activity(),100,false);foreign.packageName="external.browser";
        SearchTask.attach(root);SearchTask.attach(web);SearchTask.attach(config);SearchTask.attach(otherTask);SearchTask.attach(foreign);
        overlay(root.window.back,"Main Activity");overlay(web.window.back,"Web Activity");overlay(config.window.back,"Config Activity");
        same(0,foreign.window.back.callbacks.size(),"foreign activity is never captured");
        SearchTask.attach(root);overlay(root.window.back,"repeat Main attach");
        AlertDialog dialog=new AlertDialog(root);dialog.show();SearchTask.bindMain(root,dialog,dialog.window.decor);
        overlay(dialog.window.back,"Main dialog");
        OnBackInvokedCallback duplicate=dialog.window.back.top();
        web.window.back.invoke();
        same(1,root.removeCalls,"child Back removes own root task");
        check(SearchTask.isClosing(root)&&SearchTask.isClosing(web)&&SearchTask.isClosing(config),"all own same-task activities enter closing state");
        same(1,root.cleanupCalls,"Main timer/handler cleanup bridge invoked once");
        same(1,dialog.dismissals,"Main overlay dismissed once");
        same(0,otherTask.finishCalls+otherTask.removeCalls+otherTask.cleanupCalls,"other task unaffected");
        same(0,foreign.finishCalls+foreign.removeCalls,"foreign activity untouched");
        SearchTask.finish(root);duplicate.onBackInvoked();SearchTask.handleBackKey(web,new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_BACK));
        same(1,root.removeCalls,"duplicate exit events do not repeat task removal");
        same(1,root.cleanupCalls,"duplicate exit events do not repeat cleanup");
        same(1,dialog.dismissals,"duplicate exit events do not repeat dismissal");
        release(root,web,config,otherTask,foreign);
        same(0,root.window.back.callbacks.size()+web.window.back.callbacks.size()+config.window.back.callbacks.size(),"activity callbacks unregistered on detach");
        same(0,dialog.window.back.callbacks.size(),"dialog callback unregistered when detached");
    }

    private static void missingRootAndForeignTask(){
        int id=200;
        for(String base:new String[]{PKG,"external.caller",null}){
            WebActivity web=setup(new WebActivity(),id++,false);
            ActivityManager manager=new ActivityManager();
            ActivityManager.AppTask current=task(web.taskId,base),unrelated=task(web.taskId+1000,PKG);
            manager.tasks.add(unrelated);manager.tasks.add(current);web.services.put(Context.ACTIVITY_SERVICE,manager);
            SearchTask.attach(web);SearchTask.finish(web);
            same(PKG.equals(base)?1:0,current.removals,"rootless task removal requires own base package");
            same(0,unrelated.removals,"different taskId not removed");
            check(SearchTask.isClosing(web),"rootless caller marked closing");
            if(!PKG.equals(base))same(1,web.finishCalls,"foreign/unknown root preserves task and finishes own screen");
            SearchTask.finish(web);same(PKG.equals(base)?1:0,current.removals,"rootless repeated exit idempotent");
            release(web);
        }
        MainActivity main=setup(new MainActivity(),250,false);ConfigActivity config=setup(new ConfigActivity(),250,false);
        ActivityManager manager=new ActivityManager();ActivityManager.AppTask external=task(250,"external.caller");manager.tasks.add(external);
        main.services.put(Context.ACTIVITY_SERVICE,manager);config.services.put(Context.ACTIVITY_SERVICE,manager);
        SearchTask.attach(main);SearchTask.attach(config);SearchTask.finish(config);
        same(0,external.removals,"foreign root never removed even when several app screens are tracked");
        same(1,main.finishCalls,"embedded Main finished");same(1,config.finishCalls,"embedded Config finished");
        same(0,main.removeCalls+config.removeCalls,"embedded activities never call remove-task");
        same(1,main.cleanupCalls,"embedded Main cleanup runs");release(main,config);
        WebActivity failure=setup(new WebActivity(),260,false);ActivityManager unavailable=new ActivityManager();
        unavailable.readFailure=new SecurityException("fixture task query denied");failure.services.put(Context.ACTIVITY_SERVICE,unavailable);
        SearchTask.attach(failure);SearchTask.finish(failure);same(1,failure.finishCalls,"task-query failure still finishes own screen safely");release(failure);
    }

    private static void dialogWindowsAndHitAreas(){
        MainActivity main=setup(new MainActivity(),300,true);SearchTask.attach(main);
        AlertDialog dialog=new AlertDialog(new ContextWrapper(new ContextWrapper(main)));dialog.show();
        LinearLayout.LayoutParams standard=new LinearLayout.LayoutParams(-2,-2);standard.leftMargin=-10;standard.rightMargin=7;
        ImageButton x=button(main,X_ID,standard);x.setEnabled(false);dialog.window.decor.addView(x);
        final int[] otherClicks={0};View other=new View(main);other.setId(123);other.setOnClickListener(v->otherClicks[0]++);dialog.window.decor.addView(other);
        SearchTask.bindMain(main,dialog,dialog.window.decor);SearchTask.bindDialog(dialog);
        overlay(dialog.window.back,"Dialog bind is idempotent");
        check(x.getMinimumWidth()>=96&&x.getMinimumHeight()>=96,"X minimum touch area is at least 48dp at density 2");
        check(standard.leftMargin>=0,"negative X left margin removed");same(7,standard.rightMargin,"positive margin preserved");
        check(x.isEnabled()&&x.isClickable(),"X is enabled and clickable");
        other.performClick();same(1,otherClicks[0],"unrelated button retains its listener");
        dialog.dismiss();same(0,dialog.window.back.callbacks.size(),"dialog detach unregisters callback");
        dialog.show();overlay(dialog.window.back,"dialog re-show re-registers callback once");
        check(x.performClick(),"X click handled");same(1,main.removeCalls,"X closes and removes own task");release(main);

        for(int width:new int[]{-1,0}){
            WebActivity web=setup(new WebActivity(),310+width,true);SearchTask.attach(web);
            LinearLayout.LayoutParams classic=new LinearLayout.LayoutParams(width,-1,1f);classic.leftMargin=-4;
            ImageButton classicX=button(web,X_ID,classic);web.window.decor.addView(classicX);SearchTask.attach(web);
            // The original classic style uses MATCH_PARENT with weight; also guard the usual width-zero case.
            Dialog classicDialog=new Dialog(web);classicDialog.window.decor.addView(classicX);classicDialog.show();SearchTask.bindDialog(classicDialog);
            same(width,classic.width,"classic weighted width preserved");same(1f,classic.weight,"classic weight preserved");
            check(classicX.getMinimumHeight()>=96,"classic X retains minimum touch height");
            check(classic.leftMargin>=0,"classic negative margin removed");release(web);
        }

        ConfigActivity owner=setup(new ConfigActivity(),320,true);SearchTask.attach(owner);
        Dialog history=new Dialog(owner);ImageButton close=button(owner,HISTORY_CLOSE_ID,new ViewGroup.MarginLayoutParams(-2,-2));
        history.window.decor.addView(close);SearchTask.showDialog(history);
        same(1,history.shows,"Dialog wrapper shows exactly once");overlay(history.window.back,"history dialog");
        close.performClick();same(1,owner.removeCalls,"history close button performs full exit");release(owner);
        MainActivity lateOwner=setup(new MainActivity(),325,true);SearchTask.attach(lateOwner);
        Dialog lateHistory=new Dialog(lateOwner);lateHistory.show();SearchTask.bindDialog(lateHistory);
        SearchTask.closeDialog(lateHistory);same(1,lateOwner.removeCalls,"original late-installed history listener direct hook closes task");
        SearchTask.closeDialog(lateHistory);same(1,lateOwner.removeCalls,"late history close hook idempotent");release(lateOwner);
        MainActivity builderOwner=setup(new MainActivity(),330,true);SearchTask.attach(builderOwner);
        AlertDialog shown=SearchTask.showPlatform(new AlertDialog.Builder(builderOwner));
        check(shown.isShowing()&&shown.shows==1,"platform Builder wrapper returns shown original dialog");
        overlay(shown.window.back,"platform Builder dialog");release(builderOwner);
        Activity foreign=new Activity();foreign.packageName="other.app";
        Dialog foreignDialog=new Dialog(foreign);SearchTask.showDialog(foreignDialog);
        same(1,foreignDialog.shows,"foreign dialog show preserved");same(0,foreignDialog.window.back.callbacks.size(),"foreign dialog back unchanged");
        SearchTask.closeDialog(foreignDialog);same(1,foreignDialog.cancels,"foreign closeDialog preserves original cancel action");
    }

    private static void legacyKeyBoundaries(){
        Build.VERSION.SDK_INT=32;
        MainActivity root=setup(new MainActivity(),400,true);SearchTask.attach(root);
        same(0,root.window.back.callbacks.size(),"API 32 never registers platform API 33 callback");
        check(!SearchTask.handleBackKey(root,new KeyEvent(KeyEvent.ACTION_UP,66)),"non-Back key passes through");
        check(SearchTask.handleBackKey(root,new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_BACK)),"Back DOWN consumed");
        same(0,root.removeCalls,"Back DOWN does not finish");
        KeyEvent canceled=new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_BACK);canceled.canceled=true;
        check(SearchTask.handleBackKey(root,canceled),"canceled Back UP consumed");same(0,root.removeCalls,"canceled Back does not finish");
        check(SearchTask.handleBackKey(root,new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_BACK)),"committed Back UP consumed");
        same(1,root.removeCalls,"committed Back finishes exactly once");release(root);
        MainActivity inputOwner=setup(new MainActivity(),410,true);SearchTask.attach(inputOwner);
        View input=new View(new ContextWrapper(inputOwner));
        check(!SearchTask.handleBackKeyFromView(input,new KeyEvent(KeyEvent.ACTION_UP,66)),"pre-IME non-Back passes through");
        check(SearchTask.handleBackKeyFromView(input,new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_BACK)),"pre-IME Back DOWN consumed");
        same(0,inputOwner.removeCalls,"pre-IME Back DOWN does not finish");
        check(SearchTask.handleBackKeyFromView(input,new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_BACK)),"pre-IME Back UP consumed");
        same(1,inputOwner.removeCalls,"pre-IME Back exits input owner before keyboard consumes it");
        Context foreignContext=new Context();foreignContext.packageName="foreign.app";
        check(!SearchTask.handleBackKeyFromView(new View(foreignContext),new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_BACK)),"pre-IME foreign context passes through");
        release(inputOwner);Build.VERSION.SDK_INT=36;
    }

    public static void main(String[] args){
        searchFlags();ownedStackAndDuplicateEvents();missingRootAndForeignTask();dialogWindowsAndHitAreas();legacyKeyBoundaries();
        System.out.println("PASS SearchTask host assertions="+assertions+"; task ownership/stack/duplicates, Window callbacks, old key boundaries, external flags, X dimensions. Android device behavior NOT tested.");
    }
}

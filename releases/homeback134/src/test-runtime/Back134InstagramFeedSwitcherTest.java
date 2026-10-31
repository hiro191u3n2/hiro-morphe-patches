package app.hiro.oneback.runtime;

import android.app.ActivityManager;
import android.os.Handler;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import static app.hiro.oneback.runtime.HomeBackHostTest.*;
import static app.hiro.oneback.runtime.InstagramCurrent452Fixture.*;
import static app.hiro.oneback.runtime.Back127Instagram452Test.*;

/** Native-shape host regressions for the FEED_SWITCHER tab omitted in 317.
 * These are explicit fixture inputs, not observations of the user's device. */
public final class Back134InstagramFeedSwitcherTest {
    static Node[] feed(Scene s,String outerType)throws Exception {
        Node outer=s.node(outerType,null,MAIN);s.select(outer,true);
        set(s.controller,"current",s.switcherTab());
        if(outerType.equals("X.00VS"))s.cacheTab(outer,"feed_switcher");
        Node switcher=s.node("X.0A4c",outer,outerType.equals("X.00VS")?BASE:MAIN);
        s.switcher(switcher,2,4,2,0);
        Node page=s.node("X.052y",switcher,1936);s.tag(page,"f0");s.merged(page,0);
        Node home=s.node("X.00nV",page,1940);s.tag(home,"merged_feed_tab_0");
        return new Node[]{outer,switcher,page,home};
    }
    static void rejected(Scene s,ActivityManager.AppTask task,String why) {
        check(!OneBackExit.tryBack(s.activity),why);preserved(s,task);
    }
    static Map<String,Test> scenarios() {
        Map<String,Test> tests=new LinkedHashMap<String,Test>();
        for(final String outer:new String[]{"X.02Wh","X.00VS"})
            tests.put("feed_switcher_under_"+outer,()->{try(Scene s=new Scene()){
                Node[] n=feed(s,outer);ActivityManager.AppTask task=own(s);
                InstagramCurrent.Result r=InstagramCurrent.inspect(s.activity);
                check(r.nativeRecognized&&r.stateClear&&r.stackClear&&r.kind==InstagramCurrent.HOME&&r.target==n[3],"stable ID f0 selected at pager position 2");
                check(r.path.contains(n[1])&&r.path.contains(n[2]),"both native wrappers retained in current path");
                check(OneBackExit.tryBack(s.activity),"native feed content in FEED_SWITCHER exits");removed(s,task);
            }});
        tests.put("inactive_noncontiguous_page_excluded",()->{try(Scene s=new Scene()){
            Node[] n=feed(s,"X.00VS");Node other=s.node("X.052y",n[1],1936);s.tag(other,"f4");s.merged(other,4);other.resumed=false;
            s.node("X.NativeProfile",other,1940);ActivityManager.AppTask task=own(s);
            check(InstagramCurrent.inspect(s.activity).backgroundRoots.contains(other.view),"known inactive stable ID excluded from layer scan");
            check(OneBackExit.tryBack(s.activity),"retained other page does not block selected Home");removed(s,task);
        }});
        for(final String fault:new String[]{"missing_pager","wrong_adapter","wrong_adapter_owner","out_of_range_position","invalid_model_id","duplicate_model_id","missing_page_tag","wrong_page_type","wrong_page_parent","wrong_page_manager","page_not_resumed","missing_page_cache","pending_switcher","executing_page","saved_page","home_back_stack","live_other_page","unlisted_other_page"})
            tests.put("feed_switcher_rejects_"+fault,()->{try(Scene s=new Scene()){
                Node[] n=feed(s,"X.00VS");Object pager=n[1].getClass().getField("A0B").get(n[1]);
                Object adapter=n[1].getClass().getField("A0G").get(n[1]);ActivityManager.AppTask task=own(s);
                if(fault.equals("missing_pager"))set(n[1],"A0B",null);
                else if(fault.equals("wrong_adapter"))set(pager,"adapter",s.create("X.0Clp"));
                else if(fault.equals("wrong_adapter_owner"))set(adapter,"A00",n[0]);
                else if(fault.equals("out_of_range_position"))set(pager,"A00",3);
                else if(fault.equals("invalid_model_id")||fault.equals("duplicate_model_id")) {
                    java.util.List<?> models=(java.util.List<?>)adapter.getClass().getField("A02").get(adapter);
                    ((SelectedState)models.get(0)).index=fault.equals("invalid_model_id")?5:0;
                } else if(fault.equals("missing_page_tag"))s.tag(n[2],"f1");
                else if(fault.equals("wrong_page_type")){s.tag(n[2],"f1");Node wrong=s.node("X.NativeProfile",n[1],1936);s.tag(wrong,"f0");}
                else if(fault.equals("wrong_page_parent"))set(n[2],"mParentFragment",n[0]);
                else if(fault.equals("wrong_page_manager"))set(n[2],"mFragmentManager",s.manager);
                else if(fault.equals("page_not_resumed"))n[2].resumed=false;
                else if(fault.equals("missing_page_cache"))s.merged(n[2],null);
                else if(fault.equals("pending_switcher"))((ArrayList<Object>)s.child(n[1]).getClass().getField("A0W").get(s.child(n[1]))).add(new Object());
                else if(fault.equals("executing_page"))set(s.child(n[2]),"mExecutingActions",true);
                else if(fault.equals("saved_page"))((ManagerState)s.child(n[2])).saved=true;
                else if(fault.equals("home_back_stack"))((ManagerState)s.child(n[2])).count=1;
                else {Node other=s.node("X.052y",n[1],1936);s.tag(other,fault.equals("live_other_page")?"f4":"f3");s.merged(other,4);other.resumed=fault.equals("live_other_page");s.node("X.NativeProfile",other,1940);}
                rejected(s,task,"uncertain or layered selected route remains native: "+fault);
            }});
        tests.put("feed_switcher_cancel_keeps_task",()->{try(Scene s=new Scene()){
            feed(s,"X.00VS");ActivityManager.AppTask task=own(s);Object d=bind(s);
            check(OneBackExit.startInstagramDispatcherGesture(d),"clear Home prediction suppresses native exit animation");
            check(OneBackExit.cancelInstagramDispatcherGesture(d),"cancellation remains handled");preserved(s,task);
            check(OneBackExit.tryInstagramDispatcherBack(d),"later independent Back exits");removed(s,task);
        }});
        tests.put("feed_switcher_changed_target_during_gesture",()->{try(Scene s=new Scene()){
            Node[] n=feed(s,"X.00VS");ActivityManager.AppTask task=own(s);Object d=bind(s);
            check(OneBackExit.startInstagramDispatcherGesture(d),"gesture started on Home");
            erase(s,n[3]);Node child=s.node("X.NativeProfile",n[2],1940);s.tag(child,"merged_feed_tab_0");
            check(OneBackExit.tryInstagramDispatcherBack(d),"suppressed gesture consumed without new pop");preserved(s,task);
        }});
        tests.put("feed_switcher_other_content_is_not_home",()->{try(Scene s=new Scene()){
            Node[] n=feed(s,"X.00VS");erase(s,n[3]);Node profile=s.node("X.NativeProfile",n[2],1940);s.tag(profile,"merged_feed_tab_0");
            ActivityManager.AppTask task=own(s);InstagramCurrent.Result r=InstagramCurrent.inspect(s.activity);
            check(r.nativeRecognized&&r.kind==InstagramCurrent.OTHER&&r.target==profile,"selected enum alone does not authorize Home");
            rejected(s,task,"profile content keeps native Back");
        }});
        tests.put("feed_switcher_profile_video_retains_origin",()->{try(Scene s=new Scene()){
            Node[] n=feed(s,"X.00VS");erase(s,n[3]);Node video=s.node("X.02Hr",n[2],1940);s.tag(video,"merged_feed_tab_0");
            video.config=new com.instagram.clips.intf.ClipsViewerConfig(com.instagram.clips.intf.ClipsViewerSource.PROFILE);footer(s,video);
            ActivityManager.AppTask task=own(s);
            check(Instagram452.content(s.activity,n[0])==video,"native read-only selector follows stable-ID page to video");
            check(InstagramProfileBack.current(s.activity),"selected profile-origin video remains recognized through switcher");
            check(Instagram452.deferReels(s.activity),"native Reels guard still runs first");
            check(!OneBackExit.tryInstagramReelsBack(video),"profile video returns natively rather than exiting");preserved(s,task);
        }});
        return tests;
    }
    public static void main(String[] args)throws Exception {
        int beforeCases=cases,beforeAssertions=assertions;
        for(Map.Entry<String,Test> item:scenarios().entrySet())test(item.getKey(),item.getValue());
        System.out.println("PASS Back134InstagramFeedSwitcherTest: "+(cases-beforeCases)+" cases, "+(assertions-beforeAssertions)+" assertions; native-shape host fixtures, no device execution");
    }
}

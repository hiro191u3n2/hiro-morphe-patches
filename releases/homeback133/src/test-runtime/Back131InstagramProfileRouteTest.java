package app.hiro.oneback.runtime;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import java.util.LinkedHashMap;
import java.util.Map;
import static app.hiro.oneback.runtime.HomeBackHostTest.*;
import static app.hiro.oneback.runtime.InstagramCurrent452Fixture.*;
import static app.hiro.oneback.runtime.Back130InstagramProfileLinkTest.*;

/** Original-452 native delegate/parser bundle shapes, followed by explicit
 * profile Back hooks. These models neither execute the native router nor prove
 * which URI a browser delivers on a handset. The custom and /_u/ parser branches
 * supply a parsed username without the canonical web branch's uri Parcelable.
 */
public final class Back131InstagramProfileRouteTest {
    private static final String NAME="yoasobi_staff_";
    private static final String CUSTOM="instagram://user?username="+NAME;
    private static final String ALIAS="https://www.instagram.com/_u/"+NAME+"/";
    private static final String ORIGINAL="original_url";
    private static final String ON_CREATE="com.instagram.url.extra.IS_ON_CREATE";
    private static final String MODULE="com.instagram.url.constants.ARGUMENTS_KEY_ANALYTICS_MODULE_NAME";
    private static final String INCLUDE_STACK="InstagramUrlLaunchConstants.EXTRA_SHOULD_INCLUDE_BACK_STACK";
    private static final String INTERNAL="InstagramUrlLaunchConstants.EXTRA_IS_INTERNAL_LINK";
    private static final String OPEN_CREATION="InstagramUrlLaunchConstants.EXTRA_USER_OPEN_CREATION_SHEET";

    /** Independent of the legacy route helper. Native custom and /_u/ cases
     * omit uri; consistency cases explicitly supply an additional payload. */
    private static Bundle envelope(String module,String url,String user,boolean nativeUri) {
        Bundle args=new Bundle();
        args.putString(ORIGINAL,url);args.putBoolean(ON_CREATE,true);args.putString(MODULE,module);
        args.putString(USER,user);
        args.putString("InstagramUrlLaunchConstants.EXTRA_STARTING_TAB",null);
        args.putBoolean(OPEN_CREATION,false);
        if(nativeUri)args.fixtureObject("uri",Uri.parse(url));
        return args;
    }
    private static String module(String host) {
        return host.equals("launcher")?"url_handler_launcher":host.startsWith("url")?"url_handler":"main_activity";
    }
    private static Bundle canonical(String host) {return envelope(module(host),URL,NAME,true);}
    private static Node register(Scene scene,Bundle args)throws Exception {
        Node profile=detached(scene);
        check(profile.activity==null&&!profile.added,"native factory registration happens before attachment");
        OneBackExit.rememberInstagramExternalProfile(scene.activity,profile,args);
        attach(scene,profile);scene.profileTabs(profile,true);
        return profile;
    }
    private static void accepted(Map<String,Test> tests,String name,final String host,
            final String url,final String user,final boolean nativeUri) {
        tests.put(name,()->{try(Scene scene=new Scene(host)) {
            Bundle args=envelope(module(host),url,user,nativeUri);
            check(args.containsKey("uri")==nativeUri,"fixture has the requested uri payload presence");
            Node profile=register(scene,args);ActivityManager.AppTask task=own(scene);
            check(!OneBackExit.tryBack(scene.activity),"registration leaves profile native Back first");
            check(back(profile),"native unconsumed registered profile Back exits");removed(scene,task);
        }});
    }
    private static void rejected(Map<String,Test> tests,String name,final Bundle args) {
        tests.put(name,()->{try(Scene scene=new Scene()) {
            Node profile=register(scene,new Bundle(args));ActivityManager.AppTask task=own(scene);
            check(!back(profile),"unverified route has no external-entry exit permission");preserved(scene,task);
        }});
    }

    static Map<String,Test> scenarios() {
        Map<String,Test> tests=new LinkedHashMap<String,Test>();
        for(final String host:new String[]{"","url","launcher"}) {
            String prefix=host.isEmpty()?"main":host;
            accepted(tests,prefix+"_canonical_native_envelope",host,URL,NAME,true);
            accepted(tests,prefix+"_custom_native_bundle_without_uri",host,CUSTOM,NAME,false);
            accepted(tests,prefix+"_alias_native_bundle_without_uri",host,ALIAS,NAME,false);
            tests.put(prefix+"_wrong_host_module_rejected",()->{try(Scene scene=new Scene(host)) {
                Bundle args=canonical(host);args.putString(MODULE,host.isEmpty()?"url_handler":"main_activity");
                Node profile=register(scene,args);ActivityManager.AppTask task=own(scene);
                check(!back(profile),"the delegate module must match its actual Activity host");preserved(scene,task);
            }});
        }
        accepted(tests,"custom_slash_path_and_tracking","url",CUSTOM.replace("user?","user/?")+"&igsh=abc&utm_source=browser",NAME,false);
        accepted(tests,"alias_mobile_http_tracking","url","http://m.instagram.com/_u/"+NAME+"?igshid=abc&utm_campaign=share",NAME,false);
        accepted(tests,"canonical_bare_host_no_trailing_slash","","http://instagram.com/"+NAME,NAME,true);
        accepted(tests,"canonical_dotted_username","","https://www.instagram.com/some.user/","some.user",true);
        accepted(tests,"custom_consistent_optional_uri","url",CUSTOM,NAME,true);
        accepted(tests,"alias_consistent_optional_uri","url",ALIAS,NAME,true);

        for(final String url:new String[]{URL,CUSTOM,ALIAS}) {
            final boolean nativeUri=url.equals(URL);
            tests.put("warm_modal_native_route_"+(nativeUri?"canonical":url.equals(CUSTOM)?"custom":"alias"),()->{
                Bundle args=envelope("main_activity",url,NAME,nativeUri);args.putBoolean(INCLUDE_STACK,true);
                try(ModalScene scene=new ModalScene(args)) {
                    set(scene.manager,"count",1);
                    check(back(scene.profile),"native includeBackStack retains the external-entry identity");removed(scene.scene,scene.task);
                }
            });
        }
        tests.put("custom_cold_native_consumed_then_separate_back",()->{try(Scene scene=new Scene("url")) {
            Node profile=register(scene,envelope("url_handler",CUSTOM,NAME,false));ActivityManager.AppTask task=own(scene);
            check(back(profile,true,null),"custom-route native true remains true");preserved(scene,task);
            Handler.drain();check(back(profile),"later unconsumed custom-route Back exits");removed(scene,task);
        }});
        tests.put("custom_warm_native_consumed_then_separate_back",()->{
            Bundle args=envelope("main_activity",CUSTOM,NAME,false);args.putBoolean(INCLUDE_STACK,true);
            try(ModalScene scene=new ModalScene(args)) {
                check(back(scene.profile,true,null),"warm custom-route native true remains true");preserved(scene.scene,scene.task);
                Handler.drain();check(back(scene.profile),"later warm custom-route Back exits");removed(scene.scene,scene.task);
            }
        });
        tests.put("custom_cold_internal_profile_return_requires_next_back",()->{try(Scene scene=new Scene("url")) {
            Node entry=register(scene,envelope("url_handler",CUSTOM,NAME,false));hide(entry);
            Node child=detached(scene);attach(scene,child);scene.profileTabs(child,true);ActivityManager.AppTask task=own(scene);
            check(!back(child),"unregistered UserDetail child preserves its native false result");preserved(scene,task);
            // Explicit native-pop model; no native caller implementation is inferred.
            erase(scene,child);reveal(scene,entry);
            check(back(entry),"same-dispatch entry callback preserves the completed child return");preserved(scene,task);
            Handler.drain();check(back(entry),"separate later entry Back exits");removed(scene,task);
        }});
        tests.put("custom_warm_internal_profile_return_requires_next_back",()->{
            Bundle args=envelope("main_activity",CUSTOM,NAME,false);args.putBoolean(INCLUDE_STACK,true);
            try(ModalScene scene=new ModalScene(args)) {
                Node child=scene.push(PROFILE);child.module="profile";scene.scene.profileTabs(child,true);
                check(!back(child),"warm internal UserDetail preserves native false");preserved(scene.scene,scene.task);
                scene.pop(child);
                check(back(scene.profile),"warm same-dispatch entry callback cannot exit");preserved(scene.scene,scene.task);
                Handler.drain();check(back(scene.profile),"warm separate entry Back exits");removed(scene.scene,scene.task);
            }
        });
        tests.put("custom_foreign_browser_task_preserved",()->{try(Scene scene=new Scene("url_nonroot")) {
            Node profile=register(scene,envelope("url_handler",CUSTOM,NAME,false));
            Activity browser=new Activity(new Application("com.android.chrome"),11,true);
            ActivityManager.AppTask caller=task(scene,"com.android.chrome",11,browser,scene.activity);
            Activity ownActivity=new Activity(scene.app,12,true);ActivityManager.AppTask ownTask=task(scene,"com.instagram.android",12,ownActivity);
            check(back(profile),"custom external entry exits its own Activity");
            check(!caller.excluded&&caller.excludeCalls==0&&caller.removeCalls==0,"caller task is preserved");
            check(!browser.finishing&&browser.finishCalls==0&&browser.removeCalls==0,"caller browser is preserved");
            check(scene.activity.finishCalls==1&&scene.activity.removeCalls==0,"only the Instagram child finishes in the caller task");
            check(ownTask.excluded&&ownTask.removeCalls==1,"separate Instagram task is removed");
        }});

        for(final String fault:new String[]{"parser_only","original_missing","original_not_string","on_create_missing",
                "on_create_false","on_create_string","module_missing","canonical_uri_missing","uri_string",
                "uri_mismatch","username_missing","username_mismatch","username_invalid","open_creation",
                "entrypoint_override","auto_launch_reel","starting_tab","internal_link"}) {
            Bundle args=canonical("");
            if(fault.equals("parser_only")){args.remove(ORIGINAL);args.remove(ON_CREATE);args.remove(MODULE);}
            else if(fault.equals("original_missing"))args.remove(ORIGINAL);
            else if(fault.equals("original_not_string"))args.fixtureObject(ORIGINAL,Uri.parse(URL));
            else if(fault.equals("on_create_missing"))args.remove(ON_CREATE);
            else if(fault.equals("on_create_false"))args.putBoolean(ON_CREATE,false);
            else if(fault.equals("on_create_string"))args.putString(ON_CREATE,"true");
            else if(fault.equals("module_missing"))args.remove(MODULE);
            else if(fault.equals("canonical_uri_missing"))args.remove("uri");
            else if(fault.equals("uri_string"))args.putString("uri",URL);
            else if(fault.equals("uri_mismatch"))args.fixtureObject("uri",Uri.parse(URL+"?igsh=different"));
            else if(fault.equals("username_missing"))args.remove(USER);
            else if(fault.equals("username_mismatch"))args.putString(USER,"other_user");
            else if(fault.equals("username_invalid"))args.putString(USER,"some..user");
            else if(fault.equals("open_creation"))args.putBoolean(OPEN_CREATION,true);
            else if(fault.equals("entrypoint_override"))args.putString("InstagramUrlLaunchConstants.EXTRA_ENTRYPOINT_OVERRIDE","");
            else if(fault.equals("auto_launch_reel"))args.fixtureObject("InstagramUrlLaunchConstants.EXTRA_AUTO_LAUNCH_REEL_PARAMS",new Object());
            else if(fault.equals("starting_tab"))args.putString("InstagramUrlLaunchConstants.EXTRA_STARTING_TAB","");
            else args.putBoolean(INTERNAL,true);
            rejected(tests,"rejected_envelope_"+fault,args);
        }
        for(final String[] fixture:new String[][]{
                {"custom_duplicate_username",CUSTOM+"&username="+NAME},
                {"custom_duplicate_encoded_username",CUSTOM+"&user%6Eame="+NAME},
                {"custom_conflicting_username",CUSTOM+"&username=other_user"},
                {"custom_missing_username","instagram://user?igsh=abc"},
                {"custom_mismatched_username","instagram://user?username=other_user"},
                {"custom_unknown_control",CUSTOM+"&launch_reel=1"},
                {"custom_nonroot_path","instagram://user/reels?username="+NAME},
                {"custom_unknown_host","instagram://profile?username="+NAME},
                {"custom_fragment",CUSTOM+"#reels"},
                {"alias_unknown_control",ALIAS+"?story=1"},
                {"alias_extra_path",ALIAS+"reels/"},
                {"canonical_username_query",URL+"?username="+NAME},
                {"canonical_unknown_control",URL+"?launch_reel=1"},
                {"canonical_fragment",URL+"#reels"},
                {"canonical_userinfo","https://somebody@www.instagram.com/"+NAME+"/"},
                {"canonical_port","https://www.instagram.com:443/"+NAME+"/"},
                {"canonical_wrong_host","https://instagram.com.example/"+NAME+"/"},
                {"unknown_scheme","ig://user?username="+NAME},
                {"qr_entry_control",URL+"?utm_source=qr"},
                {"custom_qr_entry_control",CUSTOM+"&utm_source=qr"}}) {
            String value=fixture[1];boolean nativeUri=fixture[0].startsWith("canonical_")||fixture[0].equals("qr_entry_control");
            rejected(tests,"rejected_"+fixture[0],envelope("main_activity",value,NAME,nativeUri));
        }
        for(final String kind:new String[]{"initial","new_intent"})tests.put("internal_"+kind+"_intent_rejected",()->{
            try(Scene scene=new Scene("url")) {
                Intent internal=new Intent().putExtra(INTERNAL,true);
                if(kind.equals("initial"))scene.activity.setIntent(internal);
                else {scene.activity.setIntent(new Intent());OneBackExit.onNewIntent(scene.activity,internal);}
                Node profile=register(scene,canonical("url"));ActivityManager.AppTask task=own(scene);
                check(!back(profile),"native internal-link Intent does not grant external-entry permission");preserved(scene,task);
            }
        });
        tests.put("fresh_external_intent_replaces_stale_internal_marker",()->{try(Scene scene=new Scene("url")) {
            Intent internal=new Intent().putExtra(INTERNAL,true);scene.activity.setIntent(internal);
            OneBackExit.onNewIntent(scene.activity,internal);
            Node blocked=register(scene,canonical("url"));ActivityManager.AppTask task=own(scene);
            check(!back(blocked),"latest internal Intent initially rejects registration");preserved(scene,task);
            erase(scene,blocked);Handler.drain();
            Intent external=new Intent().setData(Uri.parse(URL));OneBackExit.onNewIntent(scene.activity,external);
            check(external.getAction()==null,"native null-action URL fallback remains a valid envelope");
            check(scene.activity.getIntent()==internal,"Activity's old Intent can lag the onNewIntent hook");
            Node profile=register(scene,canonical("url"));
            check(back(profile),"new external Intent supersedes the stale internal marker");removed(scene,task);
        }});
        return tests;
    }

    public static void main(String[] args)throws Exception {
        int beforeCases=cases,beforeAssertions=assertions,failed=0;
        for(Map.Entry<String,Test> entry:scenarios().entrySet()) {
            try {test(entry.getKey(),entry.getValue());}
            catch(AssertionError error) {failed++;System.out.println("FAIL "+entry.getKey()+": "+error.getMessage());Handler.drain();}
        }
        if(failed!=0)throw new AssertionError("Back131InstagramProfileRouteTest failed="+failed);
        System.out.println("PASS Back131InstagramProfileRouteTest: "+(cases-beforeCases)
            +" cases, "+(assertions-beforeAssertions)+" assertions; host models, no device execution");
    }
}

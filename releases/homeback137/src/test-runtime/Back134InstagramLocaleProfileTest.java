package app.hiro.oneback.runtime;

import android.app.ActivityManager;
import android.os.Bundle;
import android.os.Handler;
import java.util.LinkedHashMap;
import java.util.Map;
import static app.hiro.oneback.runtime.HomeBackHostTest.*;
import static app.hiro.oneback.runtime.InstagramProfileReels137Fixture.*;
import static app.hiro.oneback.runtime.InstagramCurrent452Fixture.*;
import static app.hiro.oneback.runtime.Back130InstagramProfileLinkTest.*;

/** Native-created external profile envelopes with a web display language.
 * These model the runtime boundary, not Android/browser routing on a device.
 */
public final class Back134InstagramLocaleProfileTest {
    private static final String NAME="yoasobi_staff_";
    private static void reject(Map<String,Test> tests,String name,String url) {
        tests.put(name,()->{try(Scene s=new Scene()) {
            Node p=linked(s,route(s,url,NAME));ActivityManager.AppTask t=own(s);
            check(!OneBackExit.tryInstagramDispatcherBack(dispatcher(s)),"unverified locale/query retains native dispatch");
            check(p.profileBackCalls==0,"unregistered URI cannot invoke a profile through the bridge");
            check(!back(p),"native false remains false without a registered external entry");check(InstagramProfileReels.begin(s.activity)==null,"rejected locale/provenance cannot capture a Reels handoff");preserved(s,t);
        }});
    }
    static Map<String,Test> scenarios() {
        Map<String,Test> tests=new LinkedHashMap<String,Test>();
        for(final String language:new String[]{"ja","en","ja-JP","en-US","pt_BR","es-419","zh-Hant-TW","zh_Hans_CN","fil","JA-jp"})
            tests.put("web_language_"+language,()->{try(Scene s=new Scene()) {
                Node p=linked(s,route(s,URL+"?hl="+language,NAME));ActivityManager.AppTask t=own(s);
                check(!OneBackExit.tryInstagramDispatcherBack(dispatcher(s)),"language-only entry retains native dispatcher");
                check(p.profileBackCalls==0,"no synthetic native handler call");Handler.drain();toReels(s,p,t);
            }});
        for(final String host:new String[]{"url","launcher"})tests.put(host+"_language_tracking",()->{try(Scene s=new Scene(host)) {
            Node p=linked(s,route(s,URL+"?igsh=abc&hl=ja&utm_source=google",NAME));ActivityManager.AppTask t=own(s);
            check(!OneBackExit.tryInstagramDispatcherBack(dispatcher(s)),"language tracking entry retains native dispatcher");
            check(p.profileBackCalls==0,"no synthetic native handler call");Handler.drain();toReels(s,p,t);
        }});
        tests.put("web_alias_language",()->{try(Scene s=new Scene()) {
            String url="https://www.instagram.com/_u/"+NAME+"/?hl=ja";
            Bundle args=route(s,url,NAME);args.remove("uri");
            Node p=linked(s,args);ActivityManager.AppTask t=own(s);
            toReels(s,p,t);
        }});
        tests.put("encoded_key_and_language",()->{try(Scene s=new Scene()) {
            Node p=linked(s,route(s,URL+"?h%6c=en%2DUS",NAME));ActivityManager.AppTask t=own(s);
            toReels(s,p,t);
        }});
        for(final String query:new String[]{
                "hl", "hl=", "hl=j", "hl=japanese", "hl=ja+JP", "hl=%20ja", "hl=ja%00", "hl=ja%2FJP",
                "hl=ja%26launch_reel%3D1", "hl=ja=JP", "hl=ja--JP", "hl=ja-J", "hl=en-US-extra",
                "hl=en-u-ca-japanese", "hl=123", "hl=%FF", "hl=%ZZ", "hl=%", "hl=ja&hl=ja",
                "hl=ja&hl=en", "HL=ja&hl=en", "hl=ja&h%6c=en", "hl=ja&launch_reel=1",
                "hl=ja&open_create_flow=1", "hl=ja&utm_source=qr", "hl=ja&locale=en", "hl=ja&username="+NAME})
            reject(tests,"reject_"+query,URL+"?"+query);
        reject(tests,"native_scheme_language_not_expanded","instagram://user?username="+NAME+"&hl=ja");
        reject(tests,"language_does_not_allow_reels_path",URL+"reels/?hl=ja");
        reject(tests,"language_does_not_allow_fragment",URL+"?hl=ja#reels");
        tests.put("native_consumed_still_preserved",()->{try(Scene s=new Scene()) {
            Node p=linked(s,route(s,URL+"?hl=ja",NAME));ActivityManager.AppTask t=own(s);
            p.profileBackConsumed=true;
            check(profileBack(p),"native consumed result retained");
            check(p.profileBackCalls==1,"native consumed handler once");preserved(s,t);
            Handler.drain();p.profileBackConsumed=false;toReels(s,p,t);
        }});
        tests.put("language_does_not_grant_internal_entry",()->{try(Scene s=new Scene()) {
            Bundle args=route(s,URL+"?hl=ja",NAME);args.putBoolean("InstagramUrlLaunchConstants.EXTRA_IS_INTERNAL_LINK",true);
            Node p=linked(s,args);ActivityManager.AppTask t=own(s);
            check(!back(p),"internal link retains native false despite valid language");check(InstagramProfileReels.begin(s.activity)==null,"rejected locale/provenance cannot capture a Reels handoff");preserved(s,t);
        }});
        tests.put("language_does_not_grant_parser_only_entry",()->{try(Scene s=new Scene()) {
            Bundle args=route(s,URL+"?hl=ja",NAME);args.remove("com.instagram.url.extra.IS_ON_CREATE");
            Node p=linked(s,args);ActivityManager.AppTask t=own(s);
            check(!back(p),"native provenance remains required");check(InstagramProfileReels.begin(s.activity)==null,"rejected locale/provenance cannot capture a Reels handoff");preserved(s,t);
        }});
        return tests;
    }
    public static void main(String[] args)throws Exception {
        int beforeCases=cases,beforeAssertions=assertions,failed=0;
        for(Map.Entry<String,Test> item:scenarios().entrySet())try{test(item.getKey(),item.getValue());}
        catch(AssertionError failure){failed++;System.out.println("FAIL "+item.getKey()+": "+failure.getMessage());Handler.drain();}
        if(failed!=0)throw new AssertionError("Back134InstagramLocaleProfileTest failed="+failed);
        System.out.println("PASS Back134InstagramLocaleProfileTest: "+(cases-beforeCases)+" cases, "+(assertions-beforeAssertions)+" assertions; host models, no device execution");
    }
}

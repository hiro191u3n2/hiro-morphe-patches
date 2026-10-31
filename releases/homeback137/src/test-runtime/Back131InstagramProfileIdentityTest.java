package app.hiro.oneback.runtime;

import android.app.ActivityManager;
import android.os.Handler;
import java.util.LinkedHashMap;
import java.util.Map;
import static app.hiro.oneback.runtime.HomeBackHostTest.*;
import static app.hiro.oneback.runtime.InstagramProfileReels137Fixture.*;
import static app.hiro.oneback.runtime.InstagramCurrent452Fixture.*;
import static app.hiro.oneback.runtime.Back130InstagramProfileLinkTest.*;

/** A second UserDetailFragment is ordinary child navigation even while another
 * instance in the same Activity is the registered external-link entry. These
 * host models invoke the actual profile hook pair; they do not model an
 * unregistered Activity or replace the internal profile with X.NativeProfile.
 */
public final class Back131InstagramProfileIdentityTest {
    private static Node internalProfile(Scene scene,Node entry)throws Exception {
        hide(entry);
        Node child=detached(scene);
        attach(scene,child);
        scene.profileTabs(child,true);
        // Actual 0EIp uses FragmentPagerAdapter behavior 0. An offscreen
        // retained tab can remain RESUMED and is still ordinary page content.
        ((Node)profileCached(tabAdapter(child),1)).resumed=true;
        check(child!=entry&&PROFILE.equals(child.getClass().getName()),
            "internal destination is a different actual UserDetailFragment receiver");
        return child;
    }

    static Map<String,Test> scenarios() {
        Map<String,Test> tests=new LinkedHashMap<String,Test>();
        for(final String host:new String[]{"","url"}) {
            final String name=host.isEmpty()?"main":"url";
            tests.put(name+"_internal_profile_false_pops_then_separate_entry_back_exits",()->{
                try(Scene scene=new Scene(host)) {
                    Node entry=linked(scene),child=internalProfile(scene,entry);
                    ActivityManager.AppTask task=own(scene);
                    // This hook can be reached without an Activity wrapper.
                    // Its own begin guard must survive the native child's pop.
                    check(!back(child),"an internal profile's unconsumed Back reaches its native caller");
                    preserved(scene,task);
                    erase(scene,child);reveal(scene,entry);
                    check(back(entry),"same-dispatch initial-profile callback consumes the completed child return");
                    preserved(scene,task);
                    Handler.drain();
                    toReels(scene,entry,task);
                }
            });
            tests.put(name+"_internal_profile_native_true_keeps_child_and_task",()->{
                try(Scene scene=new Scene(host)) {
                    Node entry=linked(scene),child=internalProfile(scene,entry);
                    ActivityManager.AppTask task=own(scene);
                    check(back(child,true,null),"an internal profile's native consumed result remains true");
                    check(findId(scene.manager,MAIN)==child,"native consumption leaves the internal child selected");
                    preserved(scene,task);
                    Handler.drain();
                    check(!back(child),"a later unconsumed Back can still pop that internal profile");
                    preserved(scene,task);
                }
            });
        }
        return tests;
    }

    public static void main(String[] args)throws Exception {
        int beforeCases=cases,beforeAssertions=assertions,failed=0;
        for(Map.Entry<String,Test> entry:scenarios().entrySet()) {
            try {test(entry.getKey(),entry.getValue());}
            catch(AssertionError error) {
                failed++;
                System.out.println("FAIL "+entry.getKey()+": "+error.getMessage());
                Handler.drain();
            }
        }
        if(failed!=0)throw new AssertionError("Back131InstagramProfileIdentityTest failed="+failed);
        System.out.println("PASS Back131InstagramProfileIdentityTest: "+(cases-beforeCases)
            +" cases, "+(assertions-beforeAssertions)+" assertions; host models, no device execution");
    }
}

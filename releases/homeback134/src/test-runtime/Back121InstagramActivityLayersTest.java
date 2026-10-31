package app.hiro.oneback.runtime;

import android.app.Activity;
import android.app.Application;
import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;

/** Host contract tests; native descriptors and read-only getter bodies are
 * checked separately against the user's uploaded APK. */
public final class Back121InstagramActivityLayersTest {
    interface Test {void run()throws Exception;}
    private static int assertions,cases;
    private static void check(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
    static Map<String,Test> scenarios() {
        Map<String,Test> tests=new LinkedHashMap<String,Test>();
        tests.put("clear_native_main_with_null_guards",()->{
            NativeActivity121Fixture.Fixture f=NativeActivity121Fixture.create();
            check(InstagramActivityLayers.supports(f.activity),"exact Instagram Main hierarchy is supported");
            check(InstagramActivityLayers.clear(f.activity),"null native guards and no dialogs are clear");
            check(f.activity.nativeBackCalls==0&&f.activity.finishCalls==0&&f.activity.removeCalls==0,"observation never invokes Back or a terminal action");
        });
        tests.put("unsupported_hosts_stay_native",()->{
            check(!InstagramActivityLayers.supports(null)&&!InstagramActivityLayers.clear(null),"null is unsupported");
            Activity unrelated=new Activity(new Application("com.instagram.android"),11,true);
            check(!InstagramActivityLayers.supports(unrelated)&&!InstagramActivityLayers.clear(unrelated),"package alone cannot authorize the native adapter");
        });
        tests.put("known_layer_and_unknown_api_are_distinct",()->{
            NativeActivity121Fixture.Fixture f=NativeActivity121Fixture.create();
            check(InstagramActivityLayers.inspect(f.activity)==InstagramActivityLayers.CLEAR,"clear has an explicit state");
            f.modal(true);check(InstagramActivityLayers.inspect(f.activity)==InstagramActivityLayers.BLOCKED,"native added modal is known blocked");
            f=NativeActivity121Fixture.create("main_field_type");
            check(InstagramActivityLayers.inspect(f.activity)==InstagramActivityLayers.UNKNOWN,"mismatched shape is not claimed to be a native layer");
        });
        tests.put("null_or_collected_modal_reference_is_clear",()->{
            NativeActivity121Fixture.Fixture f=NativeActivity121Fixture.create();
            Object host=f.create("X.3MM");f.activity.getClass().getDeclaredField("A08").set(f.activity,host);
            check(InstagramActivityLayers.clear(f.activity),"null modal reference matches native non-consumption");
            f.modal(null);check(InstagramActivityLayers.clear(f.activity),"collected modal reference matches native non-consumption");
        });
        tests.put("added_modal_blocks_without_dismissing",()->{
            NativeActivity121Fixture.Fixture f=NativeActivity121Fixture.create();
            NativeActivity121Fixture.FragmentState page=(NativeActivity121Fixture.FragmentState)f.modal(true);
            check(!InstagramActivityLayers.clear(f.activity),"added 9ld consumes native Main Back");
            check(page.added&&f.activity.nativeBackCalls==0,"guard does not dismiss or mutate the modal");
        });
        tests.put("unadded_modal_does_not_block",()->{
            NativeActivity121Fixture.Fixture f=NativeActivity121Fixture.create();f.modal(false);
            check(InstagramActivityLayers.clear(f.activity),"stale unadded 9ld matches native Main non-consumption");
        });
        tests.put("main_layer_uses_a0u_not_reels_a15",()->{
            NativeActivity121Fixture.Fixture f=NativeActivity121Fixture.create();
            NativeActivity121Fixture.LayerState layer=f.layer(true,f.fragment(true));
            check(!InstagramActivityLayers.clear(f.activity),"Main A0U and current fragment block despite A15 being false");
            check(layer.lookups==1&&layer.current!=null,"only the observational current-fragment getter runs");
            check(f.activity.nativeBackCalls==0,"consuming A0p is never invoked");
        });
        tests.put("disabled_main_layer_does_not_invoke_getter",()->{
            NativeActivity121Fixture.Fixture f=NativeActivity121Fixture.create();
            NativeActivity121Fixture.LayerState layer=f.layer(false,f.fragment(true));layer.failLookup=true;
            layer.getClass().getField("A15").setBoolean(layer,true);
            check(InstagramActivityLayers.clear(f.activity),"A0U false matches native Main; separate Reels guard owns A15");
            check(layer.lookups==0,"native A0p also short-circuits before A0M when A0U is false");
        });
        tests.put("enabled_layer_without_fragment_is_clear",()->{
            NativeActivity121Fixture.Fixture f=NativeActivity121Fixture.create();NativeActivity121Fixture.LayerState layer=f.layer(true,null);
            check(InstagramActivityLayers.clear(f.activity)&&layer.lookups==1,"A0U alone does not falsely block an empty native container");
        });
        tests.put("layer_getter_failure_stays_native",()->{
            NativeActivity121Fixture.Fixture f=NativeActivity121Fixture.create();NativeActivity121Fixture.LayerState layer=f.layer(true,null);layer.failLookup=true;
            check(!InstagramActivityLayers.clear(f.activity)&&layer.lookups==1,"failed observational lookup is unknown");
        });
        tests.put("modal_getter_failure_stays_native",()->{
            NativeActivity121Fixture.Fixture f=NativeActivity121Fixture.create();((NativeActivity121Fixture.FragmentState)f.modal(false)).failAdded=true;
            check(!InstagramActivityLayers.clear(f.activity),"failed isAdded cannot prove native guards clear");
        });
        tests.put("rxr_shadowed_field_uses_07d_dialog",()->{
            NativeActivity121Fixture.Fixture f=NativeActivity121Fixture.create();NativeActivity121Fixture.ShowingDialog dialog=f.dialog(true,true);
            check(!InstagramActivityLayers.clear(f.activity),"showing 07d dialog blocks even though RxR.A00 is a non-Dialog");
            check(dialog.showingReads==1&&dialog.showing&&dialog.nativeBackCalls==0,"correct Dialog is observed and remains open");
        });
        tests.put("non_showing_dialog_does_not_block",()->{
            NativeActivity121Fixture.Fixture f=NativeActivity121Fixture.create();NativeActivity121Fixture.ShowingDialog dialog=f.dialog(true,false);
            check(InstagramActivityLayers.clear(f.activity)&&dialog.showingReads==1,"retained non-showing dialog matches native check");
        });
        tests.put("other_showing_dialog_fragment_stays_native",()->{
            NativeActivity121Fixture.Fixture f=NativeActivity121Fixture.create();NativeActivity121Fixture.ShowingDialog dialog=f.dialog(false,true);
            check(!InstagramActivityLayers.clear(f.activity)&&dialog.nativeBackCalls==0,"real other DialogFragment also retains foreground Back");
        });
        tests.put("dialog_observation_failure_stays_native",()->{
            NativeActivity121Fixture.Fixture f=NativeActivity121Fixture.create();NativeActivity121Fixture.ShowingDialog dialog=f.dialog(true,true);dialog.failShowing=true;
            check(!InstagramActivityLayers.clear(f.activity),"failed Dialog.isShowing is unknown");
        });
        tests.put("root_getter_failure_stays_native",()->{
            NativeActivity121Fixture.Fixture f=NativeActivity121Fixture.create();f.activity.failManager=true;
            check(!InstagramActivityLayers.clear(f.activity),"failed native root getter is unknown");
        });
        tests.put("missing_root_or_store_stays_native",()->{
            NativeActivity121Fixture.Fixture f=NativeActivity121Fixture.create();f.activity.manager=null;
            check(!InstagramActivityLayers.clear(f.activity),"null root manager is unknown");f.activity.manager=f.manager;
            f.manager.getClass().getField("A0V").set(f.manager,null);
            check(!InstagramActivityLayers.clear(f.activity),"null fragment store is unknown");
        });
        tests.put("missing_or_untyped_root_list_stays_native",()->{
            NativeActivity121Fixture.Fixture f=NativeActivity121Fixture.create();f.store.getClass().getField("A01").set(f.store,null);
            check(!InstagramActivityLayers.clear(f.activity),"null added list is unknown");
            f=NativeActivity121Fixture.create();f.added().add(new Object());
            check(!InstagramActivityLayers.clear(f.activity),"untyped root fragment entry is unknown");
        });
        tests.put("ordinary_fragment_and_null_list_entry_are_clear",()->{
            NativeActivity121Fixture.Fixture f=NativeActivity121Fixture.create();f.added().add(f.fragment(true));f.added().add(null);
            check(InstagramActivityLayers.clear(f.activity),"non-dialog native fragments do not become modal guards");
        });
        tests.put("unknown_modal_object_stays_native",()->{
            NativeActivity121Fixture.Fixture f=NativeActivity121Fixture.create();Object host=f.create("X.3MM");
            host.getClass().getField("A02").set(host,new WeakReference<Object>(new Object()));f.activity.getClass().getDeclaredField("A08").set(f.activity,host);
            check(!InstagramActivityLayers.clear(f.activity),"changed modal object cannot be treated as clear");
        });
        tests.put("unknown_layer_implementation_stays_native",()->{
            NativeActivity121Fixture.Fixture f=NativeActivity121Fixture.create();Object base=f.create("X.0wS");
            f.loader.loadClass(NativeActivity121Fixture.IG.replace('/','.')).getDeclaredField("A00").set(f.activity,base);
            check(!InstagramActivityLayers.clear(f.activity),"only verified final 16z implementation is accepted");
        });
        for(final String fault:new String[]{"main_field_type","main_field_private","modal_reference_type","layer_flag_type",
                "layer_getter_type","layer_getter_not_final","layer_getter_static","layer_not_final","manager_return_type",
                "store_list_type","dialog_field_type","added_return_type","missing_ig_owner"}) {
            tests.put("unverified_native_shape_"+fault,()->{
                NativeActivity121Fixture.Fixture f=NativeActivity121Fixture.create(fault);
                check(InstagramActivityLayers.inspect(f.activity)==InstagramActivityLayers.UNKNOWN,"native descriptor/access mismatch remains unknown: "+fault);
                check(f.activity.nativeBackCalls==0&&f.activity.removeCalls==0,"mismatched guard never performs native or terminal actions");
            });
        }
        return tests;
    }
    public static void main(String[] args)throws Exception {
        for(Map.Entry<String,Test> item:scenarios().entrySet()) {
            try{item.getValue().run();cases++;System.out.println("PASS "+item.getKey());}
            catch(Exception|AssertionError failure){throw new AssertionError(item.getKey(),failure);}
        }
        System.out.println("PASS Back121InstagramActivityLayersTest: "+cases+" cases, "+assertions+" assertions");
    }
}

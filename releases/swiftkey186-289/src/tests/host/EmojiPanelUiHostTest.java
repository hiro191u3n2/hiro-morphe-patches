import android.content.Context;
import android.content.SharedPreferences;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.SoftInputWindow;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;
import hiro.swiftkey.EmojiPanelUi;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Behavior scenarios for the production runtime against an explicit fake Android lifecycle.
 * This is not an Android instrumentation test. Resource IDs are the reviewed target APK IDs.
 */
public final class EmojiPanelUiHostTest {
    private static int assertions, scenarios;
    private static String searchKey, aiKey;
    private static final int ROW=0x7f0a020d, EDIT=0x7f0a0261, TOP=0x7f0a0212, ACTION=0x7f0a0048;
    private static final int SEARCH_LABEL=0x7f1401e8, AI_LABEL=0x7f14003f;
    private static final List<String> passed = new ArrayList<String>();
    private static void check(boolean yes,String detail){assertions++;if(!yes)throw new AssertionError(detail);}
    private static void equal(int a,int b,String detail){check(a==b,detail+" expected="+b+" actual="+a);}
    private static void scenario(String name,Runnable body){body.run();scenarios++;passed.add(name);}

    static final class Prefs implements SharedPreferences {
        final Map<String,Object> values=new LinkedHashMap<String,Object>();
        final Set<OnSharedPreferenceChangeListener> listeners=new HashSet<OnSharedPreferenceChangeListener>();
        public Map<String,?> getAll(){return new HashMap<String,Object>(values);}
        public String getString(String k,String d){return values.containsKey(k)?(String)values.get(k):d;}
        @SuppressWarnings("unchecked") public Set<String> getStringSet(String k,Set<String>d){return values.containsKey(k)?(Set<String>)values.get(k):d;}
        public int getInt(String k,int d){return values.containsKey(k)?(Integer)values.get(k):d;}
        public long getLong(String k,long d){return values.containsKey(k)?(Long)values.get(k):d;}
        public float getFloat(String k,float d){return values.containsKey(k)?(Float)values.get(k):d;}
        public boolean getBoolean(String k,boolean d){return values.containsKey(k)?(Boolean)values.get(k):d;}
        public boolean contains(String k){return values.containsKey(k);}
        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l){listeners.add(l);}
        public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l){listeners.remove(l);}
        public Editor edit(){return new Editor(){
            private final Map<String,Object> edits=new LinkedHashMap<String,Object>();private boolean clear;
            public Editor putString(String k,String v){edits.put(k,v);return this;}public Editor putStringSet(String k,Set<String>v){edits.put(k,v);return this;}
            public Editor putInt(String k,int v){edits.put(k,v);return this;}public Editor putLong(String k,long v){edits.put(k,v);return this;}public Editor putFloat(String k,float v){edits.put(k,v);return this;}
            public Editor putBoolean(String k,boolean v){edits.put(k,v);return this;}public Editor remove(String k){edits.put(k,null);return this;}public Editor clear(){clear=true;return this;}
            public boolean commit(){apply();return true;}
            public void apply(){Set<String> changed=new HashSet<String>();if(clear){changed.addAll(values.keySet());values.clear();}for(Map.Entry<String,Object> e:edits.entrySet()){Object old=values.get(e.getKey());Object v=e.getValue();if(v==null)values.remove(e.getKey());else values.put(e.getKey(),v);if(old==null?v!=null:!old.equals(v))changed.add(e.getKey());}for(String key:changed)for(OnSharedPreferenceChangeListener l:new ArrayList<OnSharedPreferenceChangeListener>(listeners))l.onSharedPreferenceChanged(Prefs.this,key);}
        };}
    }
    static final class Hub {
        final FrameLayout root,top,row;
        final EditText edit;
        final View arrow;
        final TextView ai;
        Hub(Context c,String hint,String text){
            root=new FrameLayout(c);top=new FrameLayout(c);top.setId(TOP);row=new FrameLayout(c);row.setId(ROW);
            ai=new TextView(c);ai.setId(ACTION);ai.setText(text);top.addView(ai);
            edit=new EditText(c);edit.setId(EDIT);edit.setHint(hint);arrow=new View(c);row.addView(edit);row.addView(arrow);
            root.addView(top);root.addView(row);
        }
    }
    static final class Fixture implements AutoCloseable {
        final Context c=new Context();final FrameLayout decor=new FrameLayout(c);final Window window=new Window(decor);
        final InputMethodService service=new InputMethodService(new SoftInputWindow(window));final Hub hub;
        Fixture(){this(true);}
        Fixture(boolean attached){hub=new Hub(c,c.getString(SEARCH_LABEL),c.getString(AI_LABEL));decor.addView(hub.root);if(attached)decor.attach();}
        void start(){EmojiPanelUi.start(service);Handler.drain();frame();}
        void frame(){for(int i=0;i<10;i++){Handler.drain();ViewTreeObserver v=decor.getViewTreeObserver();v.layout();if(v.draw()){Handler.drain();return;}}throw new AssertionError("Pre-draw cancellation did not settle");}
        public void close(){decor.detach();Handler.drain();}
    }
    private static Prefs fresh(){Prefs p=new Prefs();EmojiPanelUi.init(p);Handler.drain();return p;}
    private static void switches(Prefs p,boolean search,boolean ai){p.edit().putBoolean(searchKey,search).putBoolean(aiKey,ai).apply();Handler.drain();}
    private static Preference byTitle(PreferenceScreen s,String title){for(Preference p:s.entries)if(p.title!=null&&title.contentEquals(p.title))return p;throw new AssertionError("Missing setting: "+title);}

    public static void main(String[] args){
        scenario("two persistent independent options default off; existing settings untouched",()->{
            Prefs p=fresh();p.values.put("hiro_japanese_qwerty_glide",true);
            PreferenceScreen s=new PreferenceScreen(new Context());Preference old=new Preference(s.a,null);old.l="existing";s.G(old);
            EmojiPanelUi.install(s);equal(s.entries.size(),3,"exactly two options added");
            Preference search=byTitle(s,"絵文字の検索欄を非表示"),ai=byTitle(s,"「AI の作成」ボタンを非表示");
            searchKey=search.l;aiKey=ai.l;check(!searchKey.equals(aiKey),"independent preference keys");
            check(search instanceof SwitchPreferenceCompat && ai instanceof SwitchPreferenceCompat,"switch controls");
            check(!((SwitchPreferenceCompat)search).checked&&!((SwitchPreferenceCompat)ai).checked,"both default off");
            check(!search.s&&!ai.s,"manual existing preference persistence path");
            EmojiPanelUi.install(s);equal(s.entries.size(),3,"repeated settings install has no duplicates");
            check(search.e.i(search,Boolean.TRUE),"search toggle accepted");check(p.getBoolean(searchKey,false),"search persisted");check(!p.getBoolean(aiKey,false),"AI remains off");
            check(ai.e.i(ai,Boolean.TRUE),"AI toggle accepted");check(p.getBoolean(aiKey,false),"AI persisted");
            check(!search.e.i(search,"not a boolean"),"invalid preference value rejected");
            check(p.getBoolean("hiro_japanese_qwerty_glide",false),"prior glide option preserved");
            PreferenceScreen reopened=new PreferenceScreen(s.a);EmojiPanelUi.install(reopened);
            check(((SwitchPreferenceCompat)reopened.H(searchKey)).checked&&((SwitchPreferenceCompat)reopened.H(aiKey)).checked,"saved values restored to switches");
        });
        scenario("all four combinations and full-row collapse without child mutation",()->{
            Prefs p=fresh();try(Fixture f=new Fixture()){
                f.start();equal(f.hub.row.getVisibility(),View.VISIBLE,"default search visible");equal(f.hub.ai.getVisibility(),View.VISIBLE,"default AI visible");
                for(boolean s:new boolean[]{false,true})for(boolean a:new boolean[]{false,true}){
                    switches(p,s,a);f.frame();equal(f.hub.row.getVisibility(),s?View.GONE:View.VISIBLE,"search combination "+s+"/"+a);equal(f.hub.ai.getVisibility(),a?View.GONE:View.VISIBLE,"AI combination "+s+"/"+a);
                    equal(f.hub.edit.getVisibility(),View.VISIBLE,"edit child remains native");equal(f.hub.arrow.getVisibility(),View.VISIBLE,"arrow hidden through entire row");equal(f.hub.top.getVisibility(),View.VISIBLE,"emoji tabs remain available");
                }
                switches(p,false,false);f.frame();equal(f.hub.row.getVisibility(),View.VISIBLE,"search restored");equal(f.hub.ai.getVisibility(),View.VISIBLE,"AI restored");
            }
        });
        scenario("GIF image sticker search and Copilot are untouched; every live hub inspected",()->{
            Prefs p=fresh();switches(p,true,true);try(Fixture f=new Fixture()){
                List<Hub> other=new ArrayList<Hub>();for(String hint:new String[]{"GIFを検索","画像を検索","ステッカーを検索"}){Hub h=new Hub(f.c,hint,"Copilot");other.add(h);f.decor.addView(h.root);}
                Hub outgoing=new Hub(f.c,f.c.getString(SEARCH_LABEL),f.c.getString(AI_LABEL));f.decor.addView(outgoing.root);
                TextView unrelated=new TextView(f.c);unrelated.setId(ACTION);unrelated.setText(f.c.getString(AI_LABEL));f.decor.addView(unrelated);
                f.start();equal(f.hub.row.getVisibility(),View.GONE,"first emoji row hidden");equal(outgoing.row.getVisibility(),View.GONE,"second transition emoji row hidden");equal(outgoing.ai.getVisibility(),View.GONE,"second AI hidden");
                for(Hub h:other){equal(h.row.getVisibility(),View.VISIBLE,"other media search visible");equal(h.ai.getVisibility(),View.VISIBLE,"Copilot button visible");}
                equal(unrelated.getVisibility(),View.VISIBLE,"same ID outside hub top row untouched");
            }
        });
        scenario("locale follows native resource strings rather than Japanese literals",()->{
            Prefs p=fresh();switches(p,true,true);try(Fixture f=new Fixture()){
                f.c.getResources().setString(SEARCH_LABEL,"Search emoji");f.c.getResources().setString(AI_LABEL,"AI creations");f.hub.edit.setHint("Search emoji");f.hub.ai.setText("AI creations");f.start();
                equal(f.hub.row.getVisibility(),View.GONE,"localized search hidden");equal(f.hub.ai.getVisibility(),View.GONE,"localized AI hidden");
            }
        });
        scenario("native GONE and INVISIBLE values are preserved on re-enable",()->{
            for(int nativeValue:new int[]{View.GONE,View.INVISIBLE}){
                Prefs p=fresh();try(Fixture f=new Fixture()){
                    f.hub.row.setVisibility(nativeValue);f.hub.ai.setVisibility(nativeValue);switches(p,true,true);f.start();
                    switches(p,false,false);f.frame();equal(f.hub.row.getVisibility(),nativeValue,"search original visibility restored");equal(f.hub.ai.getVisibility(),nativeValue,"AI original visibility restored");
                }
            }
        });
        scenario("native visibility reset is suppressed before draw; view replacement is discovered",()->{
            Prefs p=fresh();switches(p,true,true);try(Fixture f=new Fixture()){
                f.start();f.hub.row.setVisibility(View.VISIBLE);f.hub.ai.setVisibility(View.VISIBLE);f.frame();equal(f.hub.row.getVisibility(),View.GONE,"native reset search rehidden");equal(f.hub.ai.getVisibility(),View.GONE,"native reset AI rehidden");
                f.decor.removeView(f.hub.root);Hub newer=new Hub(f.c,f.c.getString(SEARCH_LABEL),f.c.getString(AI_LABEL));f.decor.addView(newer.root);f.frame();equal(newer.row.getVisibility(),View.GONE,"replacement search hidden");equal(newer.ai.getVisibility(),View.GONE,"replacement AI hidden");
                switches(p,false,false);f.frame();equal(newer.row.getVisibility(),View.VISIBLE,"replacement row restored");equal(newer.ai.getVisibility(),View.VISIBLE,"replacement AI restored");
            }
        });
        scenario("native repurposing to Copilot and another search type is not kept hidden",()->{
            Prefs p=fresh();switches(p,true,true);try(Fixture f=new Fixture()){
                f.start();f.hub.ai.setText("Copilot");f.hub.ai.setVisibility(View.VISIBLE);f.hub.edit.setHint("GIFを検索");f.hub.row.setVisibility(View.VISIBLE);f.frame();
                equal(f.hub.ai.getVisibility(),View.VISIBLE,"rebound Copilot native visibility preserved");equal(f.hub.row.getVisibility(),View.VISIBLE,"rebound other search native visibility preserved");
                switches(p,false,false);f.frame();equal(f.hub.ai.getVisibility(),View.VISIBLE,"unhide cannot override rebound native visibility");
            }
        });
        scenario("repeated starts and detach reattach do not multiply listeners",()->{
            Prefs p=fresh();switches(p,true,true);try(Fixture f=new Fixture()){
                f.start();int callbacks=f.decor.getViewTreeObserver().listeners(),attaches=f.decor.attachListeners();check(callbacks>0,"watcher attached");
                for(int i=0;i<8;i++)f.start();equal(f.decor.getViewTreeObserver().listeners(),callbacks,"start listener count stable");equal(f.decor.attachListeners(),attaches,"attach listener count stable");
                ViewTreeObserver old=f.decor.getViewTreeObserver();f.decor.detach();equal(old.listeners(),0,"old observer listeners removed");f.decor.attach();f.hub.ai.setVisibility(View.VISIBLE);f.hub.row.setVisibility(View.VISIBLE);f.frame();
                equal(f.hub.ai.getVisibility(),View.GONE,"reattached AI hidden");equal(f.hub.row.getVisibility(),View.GONE,"reattached row hidden");equal(f.decor.getViewTreeObserver().listeners(),callbacks,"reattach callback count stable");
            }
        });
        scenario("start before attach and new decor window remain covered",()->{
            Prefs p=fresh();switches(p,true,true);try(Fixture f=new Fixture(false)){
                EmojiPanelUi.start(f.service);Handler.drain();f.decor.attach();f.frame();equal(f.hub.row.getVisibility(),View.GONE,"late attached row hidden");
                FrameLayout next=new FrameLayout(f.c);Hub h=new Hub(f.c,f.c.getString(SEARCH_LABEL),f.c.getString(AI_LABEL));next.addView(h.root);next.attach();f.service.setWindow(new SoftInputWindow(new Window(next)));EmojiPanelUi.start(f.service);Handler.drain();next.getViewTreeObserver().layout();next.getViewTreeObserver().draw();
                equal(h.row.getVisibility(),View.GONE,"new decor row hidden");equal(h.ai.getVisibility(),View.GONE,"new decor AI hidden");next.detach();
            }
        });
        scenario("preference notifications from background defer UI mutations to main",()->{
            Prefs p=fresh();try(Fixture f=new Fixture()){
                f.start();Looper.background(()->p.edit().putBoolean(searchKey,true).putBoolean(aiKey,true).apply());Handler.drain();f.frame();equal(f.hub.row.getVisibility(),View.GONE,"background preference search applied");equal(f.hub.ai.getVisibility(),View.GONE,"background preference AI applied");
                Looper.background(()->p.edit().putBoolean(searchKey,false).putBoolean(aiKey,false).apply());Handler.drain();f.frame();equal(f.hub.row.getVisibility(),View.VISIBLE,"background preference restores search");equal(f.hub.ai.getVisibility(),View.VISIBLE,"background preference restores AI");
            }
        });
        scenario("disabled mode does not rewrite native controls or retain duplicate preference listeners",()->{
            Prefs p=fresh();try(Fixture f=new Fixture()){
                f.start();int rowWrites=f.hub.row.visibilityWrites,aiWrites=f.hub.ai.visibilityWrites;
                for(int i=0;i<20;i++)f.frame();equal(f.hub.row.visibilityWrites,rowWrites,"disabled search no writes");equal(f.hub.ai.visibilityWrites,aiWrites,"disabled AI no writes");
                int listeners=p.listeners.size();for(int i=0;i<5;i++)EmojiPanelUi.init(p);Handler.drain();equal(p.listeners.size(),listeners,"preference init no duplicates");
                Prefs next=new Prefs();EmojiPanelUi.init(next);Handler.drain();equal(p.listeners.size(),0,"previous preference listener released");
            }
        });
        scenario("switching decor releases previous ownership and observers",()->{
            Prefs p=fresh();switches(p,true,true);try(Fixture f=new Fixture()){
                f.start();ViewTreeObserver old=f.decor.getViewTreeObserver();
                FrameLayout replacement=new FrameLayout(f.c);Hub newer=new Hub(f.c,f.c.getString(SEARCH_LABEL),f.c.getString(AI_LABEL));replacement.addView(newer.root);replacement.attach();
                f.service.setWindow(new SoftInputWindow(new Window(replacement)));EmojiPanelUi.start(f.service);Handler.drain();replacement.getViewTreeObserver().layout();replacement.getViewTreeObserver().draw();
                equal(old.listeners(),0,"previous root observers released");equal(f.decor.attachListeners(),0,"previous root attach listener released");
                equal(f.hub.row.getVisibility(),View.VISIBLE,"previous search ownership restored");equal(f.hub.ai.getVisibility(),View.VISIBLE,"previous AI ownership restored");
                equal(newer.row.getVisibility(),View.GONE,"replacement row is covered");equal(newer.ai.getVisibility(),View.GONE,"replacement AI is covered");replacement.detach();
            }
        });
        scenario("newer native INVISIBLE decision wins when filtering is disabled",()->{
            Prefs p=fresh();switches(p,true,true);try(Fixture f=new Fixture()){
                f.start();f.hub.row.setVisibility(View.INVISIBLE);f.hub.ai.setVisibility(View.INVISIBLE);
                switches(p,false,false);f.frame();equal(f.hub.row.getVisibility(),View.INVISIBLE,"native newer row visibility wins");equal(f.hub.ai.getVisibility(),View.INVISIBLE,"native newer AI visibility wins");
                equal(f.decor.getViewTreeObserver().listeners(),0,"disabled filters have no drawing observers");equal(f.decor.attachListeners(),0,"disabled filters have no attachment observer");
            }
        });
        scenario("null and missing optional surfaces do not break keyboard entry",()->{
            EmojiPanelUi.start(null);EmojiPanelUi.install(null);EmojiPanelUi.start(new InputMethodService(null));Handler.drain();check(true,"missing window and screen safely ignored");
        });
        System.out.println("PASS EmojiPanelUi production host scenarios="+scenarios+" assertions="+assertions);
        System.out.println("SCENARIOS "+String.join(" | ",passed));
        System.out.println("REPORT {\"status\":\"PASSED\",\"scenarios\":"+scenarios+",\"assertions\":"+assertions+",\"device_tested\":false,\"android_instrumentation_tested\":false}");
    }
}

package app.hiro.oneback.runtime;

import android.app.ActivityManager;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.LinkedHashMap;
import java.util.Map;
import static app.hiro.oneback.runtime.HomeBackHostTest.*;

/** Reproduces the published 120 footer/caption counterexample with an unrelated
 * retained decor branch. This is a host model; it does not identify a live tree.
 * The full native current-container dispatcher regression has its own fixture. */
public final class Back121InstagramScopeTest {
    static void unrelated(F f,int size){
        ViewGroup group=new ViewGroup(f.activity);f.root.addView(group);
        for(int i=0;i<size;i++)group.addView(new View(f.activity));
    }
    static NativeReels120Fixture.Page page(F f,Back118InstagramTest.ReelsFooter ui)throws Exception{
        NativeReels120Fixture.Page page=NativeReels120Fixture.create(f.activity,ui.page,false);
        f.activity.fragments.fragments.add(page);return page;
    }
    static boolean boundary(F f,NativeReels120Fixture.Page page,boolean main){
        OneBackExit.enterBack(f.activity);
        try{return main?OneBackExit.tryInstagramMainReelsBack(f.activity):OneBackExit.tryInstagramReelsBack(page);}
        finally{OneBackExit.leaveNative();}
    }
    static Map<String,Test> scenarios(){
        Map<String,Test> tests=new LinkedHashMap<String,Test>();
        for(final int size:new int[]{601,1801})for(final boolean main:new boolean[]{false,true}){
            tests.put("footer_local_content_"+size+"_"+(main?"main":"fragment"),()->{try(F f=new F(IG,true,false)){
                unrelated(f,size);Back118InstagramTest.ReelsFooter ui=new Back118InstagramTest.ReelsFooter(f);
                NativeReels120Fixture.Page page=page(f,ui);ActivityManager.AppTask task=ownTask(f,11);
                check(!InstagramBack.inspect(f.activity).blocked,"a retained decor branch cannot turn the permanent footer into comments");
                check(boundary(f,page,main),"actual Reels native boundary recognizes its own footer");
                check(task.excluded&&task.removeCalls==1&&f.activity.nativeBackCalls==0,"the same Back excludes and removes the task without a tab pop");
            }});
        }
        for(final int size:new int[]{601,1801}){
            tests.put("caption_local_content_"+size,()->{try(F f=new F(IG,true,false)){
                unrelated(f,size);Back118InstagramTest.ReelsFooter ui=new Back118InstagramTest.ReelsFooter(f);ui.footer.hidden=true;
                ViewGroup caption=Back118InstagramTest.group(f,"clips_caption_component",new Rect(40,1500,1000,1650),ui.viewer);
                TextView text=new TextView(f.activity);text.text="コメント 123";text.bounds=new Rect(40,1520,940,1620);caption.addView(text);
                NativeReels120Fixture.Page page=page(f,ui);ActivityManager.AppTask task=ownTask(f,11);
                check(!InstagramBack.inspect(f.activity).blocked,"caption words stay content when another retained branch is large");
                check(boundary(f,page,true)&&task.excluded&&task.removeCalls==1,"native Main boundary can exit the observed caption page");
            }});
        }
        tests.put("actual_comments_outside_local_reels_keep_precedence",()->{try(F f=new F(IG,true,false)){
            unrelated(f,601);Back118InstagramTest.ReelsFooter ui=new Back118InstagramTest.ReelsFooter(f);
            NativeReels120Fixture.Page page=page(f,ui);igPanel(f,"comments_container");
            check(InstagramBack.inspect(f.activity).blocked,"real outside comments remain in the Activity guard scan");
            check(!boundary(f,page,true),"Main classifier leaves outside comments Back native");preserved(f);
        }});
        tests.put("real_footer_editor_is_not_the_static_prompt",()->{try(F f=new F(IG,true,false)){
            unrelated(f,601);Back118InstagramTest.ReelsFooter ui=new Back118InstagramTest.ReelsFooter(f);
            NativeReels120Fixture.Page page=page(f,ui);View editor=new View(f.activity);editor.editor=true;
            editor.id=++markerId;android.content.res.Resources.IDS.put(editor.id,"comment_composer_edit_text");
            editor.bounds=new Rect(20,2000,1030,2100);ui.inner.addView(editor);
            check(InstagramBack.inspect(f.activity).blocked,"local footer exemption never exempts an editable composer");
            check(!boundary(f,page,true),"native Back leaves the editor layer in control");preserved(f);
        }});
        tests.put("unknown_oversized_current_content_stays_native",()->{try(F f=new F(IG,true,false)){
            Back118InstagramTest.ReelsFooter ui=new Back118InstagramTest.ReelsFooter(f);NativeReels120Fixture.Page page=page(f,ui);
            ViewGroup current=new ViewGroup(f.activity);ui.viewer.addView(current);
            for(int i=0;i<1801;i++)current.addView(new View(f.activity));
            check(!boundary(f,page,true),"Main classifier does not declare an uninspected current page clear");preserved(f);
            check(boundary(f,page,false)&&f.activity.removeCalls==1,"reaching the exact native Reels boundary overrides that uncertain scan");
        }});
        return tests;
    }
    public static void main(String[] args)throws Exception{
        int beforeCases=cases,beforeAssertions=assertions,failed=0;
        for(Map.Entry<String,Test> item:scenarios().entrySet()){
            try{test(item.getKey(),item.getValue());}
            catch(Exception|AssertionError failure){failed++;System.out.println("FAIL "+item.getKey()+": "+failure);}
        }
        if(failed!=0)throw new AssertionError("Back121InstagramScopeTest: "+failed+" failed");
        System.out.println("PASS Back121InstagramScopeTest: "+(cases-beforeCases)+" cases, "+(assertions-beforeAssertions)+" assertions");
    }
}

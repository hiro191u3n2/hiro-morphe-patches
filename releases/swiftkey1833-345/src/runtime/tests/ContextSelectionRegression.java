package hiro.swiftkey;

import java.util.*;
import com.microsoft.fluency.ResultsFilter.PredictionSearchType;

/** Source-bound candidate fixtures, not an execution of the Fluency language model. */
public final class ContextSelectionRegression {
    static int checks, reproduced;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static class Native extends m40.c0 {
        final String output; final l40.c request=new l40.c();
        Native(String text,String raw,final boolean prefix,final boolean verbatim){
            output=text;request.k=raw;request.d=PredictionSearchType.JAPANESE;
            testMetadata=new n40.a(){public boolean D(){return prefix;}public boolean Q(){return !prefix&&verbatim;}};
        }
        public String getUserFacingText(){return output;}
        public String getTrailingSeparator(){return " ";}
        public l40.c subrequest(){return request;}
    }
    static SpaceConversionTest.Model model(String raw,boolean visible,m40.b... candidates){
        SpaceConversionTest.Model m=SpaceConversionTest.ready();m.state.raw=raw;m.state.composition=raw;
        if(visible){m.current(raw,candidates);m.g.a.h=null;}else m.candidates((Object[])candidates);
        return m;
    }
    static void selected(SpaceConversionTest.Model m,m40.b expected,int position,String label){
        check(SpaceConversionTest.press(m),label+" handled");
        check(m.commits==1&&m.factory.last.candidate==expected,label+" original identity");
        check(m.factory.last.position==position,label+" native position");
        check(expected.getUserFacingText().equals(m.committedText),label+" no separator");
        check(" ".equals(expected.getTrailingSeparator()),label+" immutable separator");
    }
    public static void main(String[] args){
        boolean baseline=args.length>0&&"baseline".equals(args[0]);
        for(boolean visible:new boolean[]{false,true})for(String[] word:new String[][]{{"あいす","アイス","愛す"},{"おすすめ","おすすめ","お勧め"}}){
            String raw=word[0];Native preferred=new Native(word[1],raw,false,false);
            Native other=new Native(word[2],raw,false,false);
            SpaceConversionTest.Candidate echo=new SpaceConversionTest.Candidate(raw,raw);
            selected(model(raw,visible,echo,preferred,other),baseline?other:preferred,baseline?2:1,"contextual "+word[1]+" visible="+visible);
            if(baseline)reproduced++;
        }
        if(baseline){
            System.out.println("{\"status\":\"PASSED\",\"assertions\":"+checks+",\"baseline_failures_reproduced\":"+reproduced+",\"scope\":\"kana_prediction_skipped_by_kanji_filter\",\"language_model_executed\":false}");return;
        }
        String raw="あいす";Native kana=new Native("アイス",raw,false,false),kanji=new Native("愛す",raw,false,false);
        selected(model(raw,true,kanji,kana),kanji,0,"native kanji remains first in different context");
        selected(model(raw,true,new Native(raw,raw,false,true),kana,kanji),kana,1,"native verbatim echo skipped");
        selected(model(raw,true,new Native("アイス",raw,true,false),kanji),kanji,1,"prefix prediction skipped");
        selected(model(raw,true,new Native("アイス",raw,true,true),kanji),kanji,1,"prefix verbatim Q false still skipped");
        selected(model(raw,true,new m40.k0(kana),kanji),kanji,1,"native true-verbatim wrapper skipped");
        selected(model(raw,true,new m40.x(new m40.k0(kana)),kanji),kanji,1,"nested true-verbatim wrapper skipped");
        for(m40.b wrapped:new m40.b[]{new m40.g0(kana),new m40.x(kana),new m40.j0(kana),new m40.v(kana),new m40.a0(kana),new m40.b0(kana),new com.swiftkey.typeface.candidates.candidates.EmailAddressCandidate(kana),new m40.x(new m40.g0(kana))})
            selected(model(raw,true,wrapped,kanji),wrapped,0,"known native wrapper "+wrapped.getClass().getName());
        l40.c request=kana.subrequest();m40.h0 unknown=new m40.h0("アイス",raw,request);unknown.setTrailingSeparator(" ");
        selected(model(raw,true,unknown,kanji),kanji,1,"unknown synthetic metadata never promoted");
        for(String text:new String[]{"AI","ice","😊","アイスcream","ー・"})
            selected(model(raw,true,new Native(text,raw,false,false),kanji),kanji,1,"non Japanese word excluded "+text);
        Native halfwidth=new Native("ｱｲｽ",raw,false,false);
        selected(model(raw,true,halfwidth,kanji),halfwidth,0,"halfwidth katakana native prediction");
        Native absent=new Native("アイス",raw,false,false);absent.testMetadata=null;
        selected(model(raw,true,absent,kanji),kanji,1,"missing metadata keeps kanji fallback");
        Native broken=new Native("アイス",raw,false,false);broken.testMetadata=new n40.a(){public boolean D(){throw new LinkageError();}public boolean Q(){return false;}};
        selected(model(raw,true,broken,kanji),kanji,1,"unavailable metadata keeps kanji fallback");
        Native stale=new Native("アイス","きょう",false,false);
        selected(model(raw,true,stale,kanji),kanji,1,"stale kana request skipped");
        selected(model(raw,true,kana),kana,0,"contextual kana does not require a kanji alternative");
        Native unchanged=new Native(raw,raw,false,false);
        selected(model(raw,true,unchanged,kanji),unchanged,0,"native nonverbatim same-text word honored");
        SpaceConversionTest.Model m=model(raw,true,new Native(raw,raw,false,true));
        check(!SpaceConversionTest.press(m)&&m.commits==0,"only verbatim preserves ordinary space fallback");
        m=model(raw,true,kana);m.state.context="context changed";
        check(!SpaceConversionTest.press(m)&&m.commits==0,"native context guard also applies to kana");
        System.out.println("{\"status\":\"PASSED\",\"assertions\":"+checks+",\"original_objects_preserved\":true,\"synthetic_candidates_excluded\":true,\"language_model_executed\":false,\"device_tested\":false}");
    }
}

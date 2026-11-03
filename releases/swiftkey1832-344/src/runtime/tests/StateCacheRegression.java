package hiro.swiftkey;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * A narrow, source-bound translation of ib0/r.g's final cache/delivery branch.
 * This is a host fixture, not execution on Android. It invokes the actual
 * baseline or fixed SpaceConversion and actual WordCandidates sources.
 * Native evidence: ib0_r_g.smali :goto_7 -> :cond_a. The cache stores v2's raw
 * lists, whereas enhancement only changes p1 and only happens when v5 != 0.
 */
public final class StateCacheRegression {
    static int checks;
    static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    static final class Delivery {
        List<m40.b> rawCache;
        List<m40.b> visible;
        int callbacks;
        List<m40.b> run(boolean regenerated,List<m40.b> raw,l40.c request){
            // Native mb0/b.a transformation is identity for this fixture.
            List<m40.b> p1=regenerated ? raw : rawCache;
            if(regenerated){
                p1=WordCandidates.enhance(p1,request);
                // Native Emoji/English/learning leave this row unchanged here.
                visible=p1;callbacks++;
                rawCache=raw; // Lib0/n(request,v2.get(defaultRequest)), NOT p1.
            }
            return p1;
        }
    }
    static boolean contains(List<m40.b> list,String text){for(m40.b c:list)if(text.equals(c.getUserFacingText()))return true;return false;}
    static SpaceConversionTest.Model model(List<m40.b> candidates,List<m40.b> visible){
        SpaceConversionTest.Model model=SpaceConversionTest.ready();
        model.state.raw="あい";model.state.composition="あい";
        model.g.a.h=new SpaceConversionTest.Prediction(candidates);
        model.current("あい",visible.toArray(new m40.b[visible.size()]));
        return model;
    }
    public static void main(String[] args)throws Exception{
        boolean fixed=args.length<2||!"baseline".equals(args[1]);
        Field dictionary=WordCandidates.class.getDeclaredField("dictionary");dictionary.setAccessible(true);
        dictionary.set(null,WordCandidates.Index.fromBytes(Files.readAllBytes(Paths.get(args[0]))));
        l40.c request=new l40.c();request.k="あい";request.d=com.microsoft.fluency.ResultsFilter.PredictionSearchType.JAPANESE;
        List<m40.b> nativeList=Arrays.<m40.b>asList(new m40.h0("あ","あい",request),new m40.h0("あい","あい",request));
        Delivery delivery=new Delivery();
        List<m40.b> miss=delivery.run(true,nativeList,request);
        check(contains(miss,"愛"),"actual dictionary supplies 愛");
        check(delivery.visible==miss,"new-result future and UI match");
        check(!contains(delivery.rawCache,"愛"),"cache retains raw list without 愛");
        check(SpaceConversionTest.press(model(miss,delivery.visible)),"enhanced future and UI can convert");
        List<m40.b> hit=delivery.run(false,null,request);
        check(delivery.callbacks==1,"cache hit does not notify UI");
        check(contains(delivery.visible,"愛"),"visible 愛 remains after hit");
        check(hit==nativeList&&!contains(hit,"愛"),"cache-hit future lacks visible 愛");
        SpaceConversionTest.Model current=model(hit,delivery.visible);
        boolean converted=SpaceConversionTest.press(current);
        check(converted,"baseline and updated handler convert visible 愛 with raw future");
        check(current.commits==1,"exactly one conversion preserved in both versions");
        {
            check("愛".equals(current.factory.last.candidate.getUserFacingText()),"actual first eligible dictionary kanji selected");
            check(current.factory.last.candidate==delivery.visible.get(2),"visible candidate identity and position preserved");
            check(current.factory.last.position==2,"actual visible position preserved");
            check(((SpaceConversionTest.Prediction)current.g.a.h).timedGets==0,"cache-hit future is never read on UI path");
        }
        System.out.println("{\"status\":\"PASSED\",\"checks\":"+checks+",\"mode\":\""+(fixed?"fixed":"baseline")+"\",\"cache_hit_visible_candidate_case_verified\":true,\"visible_kanji\":\"愛\",\"native_cache_branch_translated\":true,\"actual_production_word_supplement\":true,\"actual_production_space_handler\":true,\"device_tested\":false}");
    }
}

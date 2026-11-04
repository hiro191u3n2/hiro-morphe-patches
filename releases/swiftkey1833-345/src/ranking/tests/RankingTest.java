package hiro.swiftkey;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import l40.c;
import m40.b;
import m40.h0;
import com.microsoft.fluency.ResultsFilter;

/** Exercises the compiled rank body; engine context preferences are test inputs. */
public final class RankingTest {
    private static int assertions, regressions;
    private static boolean baseline;
    private static Field storeField;
    private static final List<String> regressionNames=new ArrayList<String>();
    static final class Candidate implements b {
        final String text;
        Candidate(String text) { this.text=text; }
        public String getUserFacingText() { return text; }
    }
    private static void check(boolean value,String label) {
        assertions++;
        if(!value) throw new AssertionError(label);
    }
    private static void regression(boolean desired,String label) {
        if(!desired) { regressions++; regressionNames.add(label); }
        if(!baseline) check(desired,label);
    }
    private static c request(String raw) {
        c r=new c(); r.k=raw; r.d=ResultsFilter.PredictionSearchType.JAPANESE; return r;
    }
    private static FrequencyLearning$Store reset() throws Exception {
        FrequencyLearning$Store s=new FrequencyLearning$Store(); storeField.set(null,s);
        KatakanaGlossCleanup.replacements.clear(); KatakanaGlossCleanup.calls=0;
        KatakanaGlossCleanup.failure=null; KatakanaGlossCleanup.last=null;
        return s;
    }
    private static void history(FrequencyLearning$Store s,String reading,String surface,long count,long recent) {
        s.entries.add(new FrequencyLearning$Entry(reading,surface,count,recent));
    }
    private static List<b> list(b... candidates) { return new ArrayList<b>(Arrays.asList(candidates)); }
    private static String texts(List<b> candidates) {
        StringBuilder out=new StringBuilder();
        for(b candidate:candidates) {
            if(out.length()>0) out.append('|');
            out.append(candidate==null?"<null>":String.valueOf(candidate.getUserFacingText()));
        }
        return out.toString();
    }
    private static boolean prefix(List<b> output,List<b> input) {
        if(output.size()<input.size()) return false;
        for(int i=0;i<input.size();i++) if(output.get(i)!=input.get(i)) return false;
        return true;
    }
    private static void contexts() throws Exception {
        String[][] cases={{"はし","橋","箸"},{"きかん","期間","機関"},{"こうせい","構成","校正"}};
        for(String[] item:cases) {
            FrequencyLearning$Store s=reset(); history(s,item[0],item[2],1000,500);
            Candidate a=new Candidate(item[1]), b=new Candidate(item[2]);
            List<b> nativeA=list(a,b), nativeB=list(b,a);
            List<b> resultA=FrequencyLearning.rank(nativeA,request(item[0]),true);
            regression(prefix(resultA,nativeA),"native_context_"+item[0]);
            List<b> resultB=FrequencyLearning.rank(nativeB,request(item[0]),true);
            check(prefix(resultB,nativeB),"alternate context accepts its own native preference");
            check(nativeA.get(0)==a&&nativeA.get(1)==b,"input list not mutated");
            check(s.entries.get(0).count==1000&&s.entries.get(0).recent==500,"rank never records a choice");
        }
        FrequencyLearning$Store s=reset(); history(s,"はし","端",9000,100);
        List<b> input=list(new Candidate("橋"),new Candidate("箸"));
        regression(prefix(FrequencyLearning.rank(input,request("はし"),true),input),"unseen_history_after_current_candidates");

        s=reset(); history(s,"あい","藍",5000,600);
        c r=request("あい"); Candidate nativeCandidate=new Candidate("愛");
        h0 dictionary1=new h0("会い",r.k,r), dictionary2=new h0("藍",r.k,r);
        input=list(nativeCandidate,dictionary1,dictionary2);
        List<b> ranked=FrequencyLearning.rank(input,r,true);
        regression(prefix(ranked,input),"dictionary_frequency_cannot_precede_native");
        check(ranked.contains(dictionary2),"existing supplementary object reused");

        s=reset(); history(s,"なま","生",7000,700);
        input=list(new Candidate("性"),new Candidate("生"));
        regression(prefix(FrequencyLearning.rank(input,request("せい"),true),input),"other_reading_total_cannot_override_native");
    }
    private static void appends() throws Exception {
        FrequencyLearning$Store s=reset();
        history(s,"はし","端",9,1); history(s,"はし","梯",9,2);
        history(s,"はし","橋",100,3); history(s,"ほし","星",10000,4);
        c r=request("ＨＡＳＨＩ"); Candidate bridge=new Candidate("橋"), chopsticks=new Candidate("箸");
        List<b> input=list(bridge,chopsticks), output=FrequencyLearning.rank(input,r,true);
        check(texts(output).equals(baseline?"橋|梯|端|箸":"橋|箸|梯|端"),"append count then recent, irrelevant reading absent");
        check(Collections.frequency(texts(output).isEmpty()?Collections.<String>emptyList():Arrays.asList(texts(output).split("\\|")),"橋")==1,"history never duplicates current surface");
        check(output.contains(bridge)&&output.contains(chopsticks),"current candidate identities retained");
        for(b candidate:output) if(candidate instanceof h0) {
            h0 added=(h0)candidate;
            check(added.request==r,"historical candidate keeps exact current request");
            check(added.input.equals(r.k),"historical candidate keeps raw input");
            check("".equals(added.separator),"historical candidate has empty separator");
        }
        check(s.entries.size()==4&&s.entries.get(0).count==9,"history not changed by ranking");

        s=reset(); history(s,"はし","端",1,1);
        Candidate first=new Candidate("橋"), sameSurface=new Candidate("橋"), nullText=new Candidate(null);
        input=list(null,first,sameSurface,nullText);
        output=FrequencyLearning.rank(input,request("はし"),true);
        if(!baseline) check(prefix(output,input),"preexisting duplicates and null positions are preserved");
        check(output.contains(null)&&output.contains(nullText),"null candidate and null surface retained");

        s=reset(); input=list(new Candidate("橋"));
        check(FrequencyLearning.rank(input,request("はし"),true)==input,"no history returns original list");
        history(s,"ほし","星",1,1);
        check(FrequencyLearning.rank(input,request("はし"),true)==input,"only unrelated history returns original list");
    }
    private static void gates() throws Exception {
        FrequencyLearning$Store s=reset(); history(s,"はし","端",999,999);
        List<b> input=list(new Candidate("橋")); c r=request("はし");
        check(FrequencyLearning.rank(input,r,false)==input,"disabled learning preserves candidates");
        check(s.lookups==0,"disabled learning does not query history");
        check(FrequencyLearning.rank(null,r,true)==null,"null list");
        List<b> empty=new ArrayList<b>(); check(FrequencyLearning.rank(empty,r,true)==empty,"empty list");
        check(FrequencyLearning.rank(input,null,true)==input,"null request");
        r.f=true; check(FrequencyLearning.rank(input,r,true)==input,"flow request excluded"); r.f=false;
        r.d=ResultsFilter.PredictionSearchType.OTHER; check(FrequencyLearning.rank(input,r,true)==input,"non Japanese excluded");
        for(String raw:new String[]{null,"","橋","はshi",String.join("",Collections.nCopies(129,"あ"))}) {
            check(FrequencyLearning.rank(input,request(raw),true)==input,"invalid reading preserved");
        }
        check(s.lookups==0,"all ineligible calls bypass store");
        storeField.set(null,null); check(FrequencyLearning.rank(input,request("はし"),true)==input,"uninitialized store");
    }
    private static void cleanupAndFailures() throws Exception {
        FrequencyLearning$Store s=reset();
        KatakanaGlossCleanup.replacements.put("てすと\tテスト(試験)","テスト");
        Candidate nativeWord=new Candidate("試験");
        List<b> input=list(new Candidate("テスト(試験)"),nativeWord);
        List<b> cleaned=FrequencyLearning.rank(input,request("てすと"),false);
        check(texts(cleaned).equals("テスト|試験"),"existing gloss cleanup retained with learning disabled");
        check(cleaned.get(1)==nativeWord,"cleanup retains unrelated candidate identity");
        check(s.lookups==0,"cleanup does not require history access");
        s.failure=new IllegalStateException("store failure");
        List<b> recovered=FrequencyLearning.rank(input,request("てすと"),true);
        check(recovered==KatakanaGlossCleanup.last&&texts(recovered).equals("テスト|試験"),"store exception returns already cleaned list");
        s.failure=new NoSuchMethodError("ABI failure");
        recovered=FrequencyLearning.rank(input,request("てすと"),true);
        check(recovered==KatakanaGlossCleanup.last,"linkage failure returns already cleaned list");
        KatakanaGlossCleanup.failure=new IllegalStateException("cleanup failure");
        check(FrequencyLearning.rank(input,request("てすと"),true)==input,"cleanup exception returns untouched input");
        KatakanaGlossCleanup.failure=new NoClassDefFoundError("cleanup ABI failure");
        check(FrequencyLearning.rank(input,request("てすと"),true)==input,"cleanup linkage failure returns untouched input");
        reset();
        input=list(new b(){public String getUserFacingText(){throw new IllegalStateException("candidate failure");}});
        check(FrequencyLearning.rank(input,request("はし"),true)==input,"broken candidate falls back to input");
    }
    private static void sessionBoundaries() throws Exception {
        FrequencyLearning$Store s=reset(); history(s,"はし","端",99,99);
        List<b> input=list(new Candidate("橋")); c r=request("はし");
        Field session=UsageBridge.class.getDeclaredField("session"); session.setAccessible(true);
        session.set(null,new UsageBridge$Session());
        UsageBridgeTestEnvironment.eligible=true;
        UsageBridgeTestEnvironment.allowedBefore=false;
        UsageBridgeTestEnvironment.allowedAfter=true;
        UsageBridgeTestEnvironment.allowedCalls=0;
        check(UsageBridge.rank(input,r)==input,"blocked private/session boundary returns original candidates");
        check(s.lookups==0&&KatakanaGlossCleanup.calls==0,"blocked session never enters frequency rank");
        check(UsageBridgeTestEnvironment.allowedCalls==1,"blocked session checks once");
        UsageBridgeTestEnvironment.allowedBefore=true;
        UsageBridgeTestEnvironment.allowedAfter=false;
        UsageBridgeTestEnvironment.allowedCalls=0;
        check(UsageBridge.rank(input,r)==input,"session revoked during rank discards its result");
        check(s.lookups==1&&UsageBridgeTestEnvironment.allowedCalls==2,"allowed state rechecked after lookup");
        UsageBridgeTestEnvironment.eligible=false;
        UsageBridgeTestEnvironment.allowedCalls=0;
        check(UsageBridge.rank(input,r)==input,"ineligible request never enters frequency rank");
        check(s.lookups==1&&UsageBridgeTestEnvironment.allowedCalls==0,"eligibility check precedes session and lookup");
        check(s.entries.get(0).count==99,"blocked/revoked ranking never records history");
    }
    public static void main(String[] args) throws Exception {
        baseline=args.length>0&&args[0].equals("baseline");
        storeField=FrequencyLearning.class.getDeclaredField("store"); storeField.setAccessible(true);
        contexts(); appends(); gates(); cleanupAndFailures(); sessionBoundaries();
        check(regressions==(baseline?6:0),"expected exact baseline context regressions");
        System.out.println("{\"status\":\"PASSED\",\"mode\":\""+(baseline?"baseline":"fixed")+"\",\"assertions\":"+assertions+",\"context_order_regressions\":"+regressions+",\"native_preferences_are_modeled_inputs\":true,\"device_tested\":false}");
    }
}

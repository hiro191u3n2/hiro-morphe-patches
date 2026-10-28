package hiro.swiftkey;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.lang.reflect.*;
import java.util.*;
import m40.b;
import m40.h0;
import com.microsoft.fluency.Japanese;
import com.microsoft.fluency.ResultsFilter.PredictionSearchType;

/** Runs the released DEX after dex2jar conversion. Only Android/native boundaries are stubbed. */
public final class EnglishDictionaryRegressionTest {
  static int checks;
  static boolean fixed;
  static final List<String> cases = new ArrayList<>();
  static void check(boolean b, String message) { checks++; if(!b)throw new AssertionError(message); }
  static String json(String s) { return "\""+s.replace("\\","\\\\").replace("\"","\\\"")+"\""; }
  static byte[] bytes(String s) { return s.getBytes(StandardCharsets.UTF_8); }
  static void resetNative() { Japanese.lastInput=null;Japanese.calls=0; }
  static List<b> nativeList(l40.c c) { return new ArrayList<b>(Arrays.asList(new h0("native-first",c.k,c),new h0("native-second",c.k,c),new h0("native-third",c.k,c))); }
  static l40.c context(String raw) { l40.c c=new l40.c();c.k=raw;c.d=PredictionSearchType.JAPANESE;return c; }
  static void install(KatakanaEnglishCandidates.Index index) throws Exception {
    Field f=KatakanaEnglishCandidates.class.getDeclaredField("dictionary");f.setAccessible(true);f.set(null,index);
  }
  static void whole(String raw,String expected,boolean longMark,boolean romaji) {
    l40.c c=context(raw);List<b> original=nativeList(c);resetNative();
    List<b> actual=KatakanaEnglishCandidates.enhance(original,c);
    boolean shouldAdd=!longMark||fixed;
    check(Japanese.calls==((romaji&&shouldAdd)?1:0),"native conversion call count: "+raw);
    if(romaji&&shouldAdd)check(raw.equals(Japanese.lastInput),"native converter preserves raw argument: "+raw);
    if(shouldAdd) {
      check(actual.size()==4,"exact dictionary candidate count: "+raw);
      check(actual.get(0)==original.get(0)&&actual.get(1)==original.get(1)&&actual.get(3)==original.get(2),"native order and identities: "+raw);
      h0 added=(h0)actual.get(2);
      check(expected.equals(added.getUserFacingText()),"whole phrase lookup: "+raw);
      check(raw.equals(added.b)&&added.c==c&&"".equals(added.d),"commit raw, context and separator: "+raw);
    } else check(actual==original,"published mixed-romaji rejection reproduced: "+raw);
    check(original.size()==3&&"native-third".equals(original.get(2).getUserFacingText()),"native list untouched: "+raw);
    cases.add("{\"raw\":"+json(raw)+",\"expected\":"+json(expected)+",\"added\":"+shouldAdd+",\"native_calls\":"+Japanese.calls+"}");
  }
  public static void main(String[] args) throws Exception {
    fixed=args[1].equals("fixed");
    byte[] data=Files.readAllBytes(Paths.get(args[0]));
    KatakanaEnglishCandidates.Index index=KatakanaEnglishCandidates.Index.fromBytes(data);
    int rows=0,entries=0,truncated=0;
    for(String line:new String(data,StandardCharsets.UTF_8).split("\n")) {
      String[] cols=line.split("\t",-1);
      List<String> all=new ArrayList<>(new LinkedHashSet<>(Arrays.asList(cols).subList(1,cols.length)));
      List<String> expected=all.subList(0,Math.min(all.size(),12));
      List<String> actual=new ArrayList<>(KatakanaEnglishCandidates.lookup(index,cols[0],Collections.<String>emptySet()));
      check(actual.equals(expected),"published exact row intact: "+cols[0]);
      rows++;entries+=all.size();if(all.size()>12)truncated++;
    }
    check(rows==37832 && rows==index.starts.length,"all released dictionary readings loaded");
    install(index);
    String[][] examples={{"airabuyuー","あいらぶゆー","I love you"},{"sankyuー","さんきゅー","thank you"},{"guddomoーningu","ぐっどもーにんぐ","good morning"}};
    for(String[] e:examples) {
      whole(e[0],e[2],true,true);
      whole(e[1],e[2],false,false);
      whole(e[0].replace('ー','-'),e[2],false,true);
    }
    whole("kamera","camera",false,true);
    whole("カメラ","camera",false,false);
    whole("ｶﾒﾗ","camera",false,false);
    l40.c c=context("sushi");List<b> original=nativeList(c);resetNative();
    check(KatakanaEnglishCandidates.enhance(original,c)==original,"unknown exact kana has no unrelated candidates");
    check(Japanese.calls==1&&"sushi".equals(Japanese.lastInput),"unchanged plain ASCII romanization");
    Method romaji=KatakanaEnglishCandidates.class.getDeclaredMethod("isRomaji",String.class);romaji.setAccessible(true);
    for(String[] e:examples)check(((Boolean)romaji.invoke(null,e[0]))==fixed,"direct gate regression: "+e[0]);
    check((Boolean)romaji.invoke(null,"sushi"),"plain romaji accepted");
    check((Boolean)romaji.invoke(null,"shin'you"),"apostrophe retained");
    check((Boolean)romaji.invoke(null,"ko-hi-"),"legacy hyphen retained");
    for(String invalid:new String[]{"","sankyu ー","sankyu1ー","sankyuー!","sankyuあ","thank you"}) {
      check(!(Boolean)romaji.invoke(null,invalid),"invalid input remains rejected: "+invalid);
      c=context(invalid);original=nativeList(c);resetNative();
      check(KatakanaEnglishCandidates.enhance(original,c)==original&&Japanese.calls==0,"invalid input bypasses dictionary: "+invalid);
    }
    c=context("さんきゅー"); original=nativeList(c);original.add(new h0("thank you",c.k,c));
    check(KatakanaEnglishCandidates.enhance(original,c)==original,"native candidate deduplication");
    c=context("airabuyuー");original=nativeList(c);c.f=true;resetNative();
    check(KatakanaEnglishCandidates.enhance(original,c)==original&&Japanese.calls==0,"continuation input gate");
    c.f=false;c.d=PredictionSearchType.NORMAL;
    check(KatakanaEnglishCandidates.enhance(original,c)==original&&Japanese.calls==0,"non-Japanese gate");
    c.d=PredictionSearchType.JAPANESE;
    List<b> empty=new ArrayList<>();check(KatakanaEnglishCandidates.enhance(empty,c)==empty,"empty native predictions gate");
    check(KatakanaEnglishCandidates.enhance(original,null)==original,"null context gate");
    check(KatakanaEnglishCandidates.enhance(null,c)==null,"null native predictions gate");
    StringBuilder fixture=new StringBuilder();
    for(int i=0;i<80;i++)fixture.append("あいう").append((char)(0x3041+i)).append('\t').append("candidate").append(i).append('\n');
    KatakanaEnglishCandidates.Index pref=KatakanaEnglishCandidates.Index.fromBytes(bytes(fixture.toString()));
    check(KatakanaEnglishCandidates.lookup(pref,"あいう",Collections.<String>emptySet()).size()==12,"prefix suggestion cap retained");
    Set<String> existing=new HashSet<>();for(int i=0;i<64;i++)existing.add("candidate"+i);
    check(KatakanaEnglishCandidates.lookup(pref,"あいう",existing).isEmpty(),"prefix scan row limit retained");
    check(KatakanaEnglishCandidates.lookup(pref,"あい",Collections.<String>emptySet()).isEmpty(),"short prefix limit retained");
    System.out.println("{\"status\":\"PASSED\",\"mode\":"+json(args[1])+",\"runtime\":\"released DEX converted to JVM classfiles with dex2jar\",\"native_boundary\":\"stubbed; Android JNI and handset UI not executed\",\"assertions\":"+checks+",\"dictionary_readings\":"+rows+",\"dictionary_entries\":"+entries+",\"readings_with_more_than_12_entries\":"+truncated+",\"whole_long_mark_examples_verified\":"+fixed+",\"baseline_negative_examples_verified\":"+(!fixed)+",\"cases\":["+String.join(",",cases)+"]}");
  }
}

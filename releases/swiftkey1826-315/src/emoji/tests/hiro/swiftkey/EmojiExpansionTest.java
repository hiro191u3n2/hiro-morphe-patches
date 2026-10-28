package hiro.swiftkey;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.lang.reflect.Field;
import com.microsoft.fluency.Japanese;
import com.microsoft.fluency.ResultsFilter.PredictionSearchType;

public class EmojiExpansionTest {
 static int checks=0,aliases=0,entries=0,romaji=0,negative=0;
 static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
 static l40.c request(String input){l40.c r=new l40.c();r.k=input;r.d=PredictionSearchType.JAPANESE;return r;}
 static List<m40.b> nativeList(l40.c r){return new ArrayList<m40.b>(Arrays.<m40.b>asList(new m40.h0("元の先頭",r.k,r),new m40.h0("元の二番",r.k,r),new m40.h0("元の三番",r.k,r),new m40.h0("元の四番",r.k,r)));}
 static void verify(String input,List<String> expected){
  l40.c r=request(input);List<m40.b> old=nativeList(r);List<m40.b> out=EmojiCandidates.enhance(old,r);
  check(out.size()==old.size()+expected.size(),"all associations retained: "+input);
  check(out.get(0)==old.get(0)&&out.get(1)==old.get(1),"first two native identity/order: "+input);
  check(out.get(3)==old.get(2)&&out.get(4)==old.get(3),"remaining natives identity/order: "+input);
  List<m40.b> added=new ArrayList<m40.b>();added.add(out.get(2));added.addAll(out.subList(5,out.size()));
  for(int i=0;i<expected.size();i++){
   m40.h0 c=(m40.h0)added.get(i);
   check(c.getUserFacingText().equals(expected.get(i)),"association order: "+input+" / "+i);
   check(c.b.equals(input)&&c.c==r&&"".equals(c.d),"complete raw input/request/no separator commit contract: "+input);
  }
  check(old.size()==4,"native list not mutated");
 }
 static void unchanged(String input){l40.c r=request(input);List<m40.b> old=nativeList(r);check(EmojiCandidates.enhance(old,r)==old,"unrelated input unchanged: "+input);negative++;}
 public static void main(String[] args)throws Exception{
  TreeMap<String,List<String>> expected=new TreeMap<String,List<String>>();
  for(String line:Files.readAllLines(Paths.get(args[0]),StandardCharsets.UTF_8)){
   // The existing production loader treats a leading # as a comment; this legacy
   // entry is preserved in the data, never silently claimed as a reachable reading.
   if(line.isEmpty()||line.startsWith("#"))continue;
   String[] row=line.split("\\t");expected.put(EmojiCandidates.normalize(row[0]),Arrays.asList(Arrays.copyOfRange(row,1,row.length)));
  }
  System.setProperty("emoji.dictionary",args[0]);EmojiCandidates.init(new android.content.Context());
  Field f=EmojiCandidates.class.getDeclaredField("dictionary");f.setAccessible(true);
  for(int i=0;i<500&&f.get(null)==null;i++)Thread.sleep(10);
  @SuppressWarnings("unchecked") TreeMap<String,String[]> actual=(TreeMap<String,String[]>)f.get(null);
  check(actual!=null,"actual async production dictionary loader completes");check(actual.size()==expected.size(),"loaded dictionary key count");
  for(Map.Entry<String,List<String>> e:expected.entrySet()){
   check(new ArrayList<String>(EmojiCandidates.lookup(actual,e.getKey())).equals(e.getValue()),"full dictionary exact lookup/order: "+e.getKey());entries++;
  }
  Set<String> tested=new HashSet<String>();
  for(String line:Files.readAllLines(Paths.get(args[1]),StandardCharsets.UTF_8)){
   if(line.isEmpty()||line.startsWith("#"))continue;
   String[] row=line.split("\\t");
   for(String input:row[1].split("\\|")){
    String key=EmojiCandidates.normalize(input);check(expected.containsKey(key),"curated key exists "+input);
    for(String emoji:row[0].split(" "))check(expected.get(key).contains(emoji),"curated association exists "+input+" -> "+emoji);
    if(tested.add(input)){verify(input,expected.get(key));aliases++;}
   }
  }
  String[][] roman={{"arigatougozaimasu","ありがとうございます"},{"oyasuminasai","おやすみなさい"},{"sugoi","すごい"},{"yokatta","よかった"},{"kimochiii","きもちいい"},{"nyanko","にゃんこ"},{"ge-muchuu","げーむちゅう"}};
  for(String[] pair:roman){Japanese.readings.put(pair[0],pair[1]);verify(pair[0],expected.get(pair[1]));check(pair[0].equals(Japanese.lastInput),"raw romaji passed intact to native converter");romaji++;}
  String full="オヤスミナサイ";verify(full,expected.get("おやすみなさい"));
  verify("ｵﾔｽﾐﾅｻｲ",expected.get("おやすみなさい"));
  verify("オハヨー",expected.get("おはよー"));
  check(EmojiCandidates.normalize("ﾗｰﾒﾝﾀﾍﾞﾀｲ").equals("らーめんたべたい"),"halfwidth full normalization");
  verify("ラーメン食べたい",expected.get("らーめん食べたい"));
  // Prefix matching is bounded semantically by the existing dictionary prefix;
  // exact readings win, and no suffix-of-a-sentence matcher is introduced.
  TreeMap<String,String[]> tiny=new TreeMap<String,String[]>();tiny.put("はーと",new String[]{"❤️"});tiny.put("はーとまーく",new String[]{"💜"});
  check(EmojiCandidates.lookup(tiny,"はーと").equals(new LinkedHashSet<String>(Arrays.asList("❤️"))),"exact does not include longer unrelated reading");
  check(EmojiCandidates.lookup(tiny,"はー").equals(new LinkedHashSet<String>(Arrays.asList("❤️","💜"))),"two character prefix remains supported");
  check(EmojiCandidates.lookup(tiny,"は").isEmpty(),"one character broad prefix remains excluded");
  for(String input:new String[]{"", "zzqvxyz", "これはすごい", "これはありがとう", "abcありがとう", "hello猫", "999未登録の文", "猫が歩いている文章", "thanks", "happy"})unchanged(input);
  l40.c r=request("すごい");List<m40.b> old=nativeList(r);
  r.d=PredictionSearchType.NORMAL;check(EmojiCandidates.enhance(old,r)==old,"English/non-Japanese mode unchanged");negative++;
  r.d=PredictionSearchType.JAPANESE;r.f=true;check(EmojiCandidates.enhance(old,r)==old,"zero-input request untouched");negative++;
  r.f=false;r.k=null;check(EmojiCandidates.enhance(old,r)==old,"null composition untouched");negative++;
  check(EmojiCandidates.enhance(old,null)==old,"null request untouched");negative++;
  check(EmojiCandidates.enhance(null,request("すごい"))==null,"null original list untouched");negative++;
  for(int n=0;n<2;n++){List<m40.b> shortList=old.subList(0,n);check(EmojiCandidates.enhance(shortList,request("すごい"))==shortList,"short native list untouched");negative++;}
  r=request("すごい");old=nativeList(r);List<String> es=expected.get("すごい");m40.b duplicate=new m40.h0(es.get(0),r.k,r);old.add(duplicate);
  List<m40.b> dedup=EmojiCandidates.enhance(old,r);check(dedup.size()==old.size()+es.size()-1,"existing native emoji deduplicated");
  int count=0;for(m40.b c:dedup)if(c.getUserFacingText().equals(es.get(0)))count++;check(count==1&&dedup.contains(duplicate),"native duplicate retains original instance");
  List<m40.b> allExisting=nativeList(r);for(String e:es)allExisting.add(new m40.h0(e,r.k,r));check(EmojiCandidates.enhance(allExisting,r)==allExisting,"all emoji already present preserves list identity");
  check(Japanese.calls>0,"romaji uses native conversion boundary");
  // Existing feature intentionally retains every exact emoji, without a new
  // candidate count cap. This protects later variants in expanded view.
  String[] many=new String[4000];for(int i=0;i<many.length;i++)many[i]="test-"+i;actual.put("検査専用",many);verify("検査専用",Arrays.asList(many));actual.remove("検査専用");
  System.out.println("{\"status\":\"PASSED\",\"checks\":"+checks+",\"dictionary_exact_entries_tested\":"+entries+",\"curated_input_forms_tested\":"+aliases+",\"romaji_native_contract_cases\":"+romaji+",\"negative_cases\":"+negative+",\"native_top_two_and_commit_metadata_preserved\":true,\"async_production_loader_executed\":true,\"native_romaji_conversion_is_test_double\":true,\"suffix_matching_added\":false,\"english_mode_changed\":false,\"exact_candidate_cap\":null,\"device_tested\":false}");
 }
}

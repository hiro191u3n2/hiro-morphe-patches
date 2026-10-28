package hiro.swiftkey;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/** Executes the production Index and lookup against every published reading. */
public final class WordCandidatesCoverageTest {
  static final Set<String> NONE = Collections.emptySet();
  static int checks;
  static void check(boolean ok, String label) {
    checks++;
    if (!ok) throw new AssertionError(label);
  }
  static byte[] bytes(String s) { return s.getBytes(StandardCharsets.UTF_8); }
  static LinkedHashSet<String> lookup(WordCandidates.Index index, String q, Set<String> existing) {
    return WordCandidates.lookup(index, q, existing);
  }
  static String synthetic(String key, int count) {
    StringBuilder text = new StringBuilder(key);
    for (int i=0;i<count;i++) text.append('\t').append("単語").append(i);
    return text.append('\n').toString();
  }
  static String quoted(String value) { return "\""+value.replace("\\","\\\\").replace("\"","\\\"")+"\""; }
  public static void main(String[] args) throws Exception {
    boolean fixed = args[1].equals("fixed");
    long start=System.nanoTime();
    byte[] data=Files.readAllBytes(Paths.get(args[0]));
    WordCandidates.Index index=WordCandidates.Index.fromBytes(data);
    int rows=0, total=0, returned=0, incomplete=0, missing=0, unexpected=0, max=0;
    List<String> examples=new ArrayList<>();
    for (String line : new String(data,StandardCharsets.UTF_8).split("\n")) {
      String[] fields=line.split("\t",-1);
      LinkedHashSet<String> expected=new LinkedHashSet<>(Arrays.asList(fields).subList(1,fields.length));
      LinkedHashSet<String> actual=lookup(index,fields[0],NONE);
      rows++; total+=expected.size(); returned+=actual.size(); max=Math.max(max,expected.size());
      LinkedHashSet<String> absent=new LinkedHashSet<>(expected); absent.removeAll(actual);
      LinkedHashSet<String> extra=new LinkedHashSet<>(actual); extra.removeAll(expected);
      if (!absent.isEmpty()) {incomplete++;missing+=absent.size();if(examples.size()<8)examples.add(fields[0]+": "+absent);}
      unexpected+=extra.size();
      if(fixed) check(new ArrayList<>(actual).equals(new ArrayList<>(expected)),"exact full ordered row: "+fields[0]);
      else check(actual.size()<=12 && extra.isEmpty(),"published cap and exact isolation: "+fields[0]);
    }
    check(rows==index.starts.length,"all Index rows covered");
    check(rows==638044,"published dictionary reading count");
    check(total==847642,"published dictionary candidate count");
    check(unexpected==0,"no unrelated exact completions");
    if(fixed) check(missing==0,"all exact candidates retained");
    else check(incomplete==2175 && missing>0,"reproduce published 12-candidate defect");

    WordCandidates.Index exact=WordCandidates.Index.fromBytes(bytes(synthetic("こうき",40)));
    Set<String> existing=new HashSet<>();for(int i=0;i<5;i++)existing.add("単語"+i);
    LinkedHashSet<String> values=lookup(exact,"こうき",existing);
    check(values.size()==(fixed?27:12),"exact removes only existing candidates within retained 32-column parser limit");
    check(values.iterator().next().equals("単語5"),"exact cost order after native dedup");
    check(lookup(exact,"こうきあ",NONE).isEmpty(),"exact suffix does not match");

    StringBuilder prefix=new StringBuilder();
    for(int i=0;i<80;i++) prefix.append(synthetic("あいう"+(char)(0x3041+i),40));
    WordCandidates.Index pref=WordCandidates.Index.fromBytes(bytes(prefix.toString()));
    check(lookup(pref,"あいう",NONE).size()==12,"prefix still limited to 12");
    check(lookup(pref,"あい",NONE).isEmpty(),"two-character prefix still excluded");
    check(lookup(pref,"helloあいう",NONE).isEmpty(),"mixed English input still excluded");
    check(lookup(pref," あいう",NONE).isEmpty(),"leading space preserved");
    check(lookup(pref,"あいう ",NONE).isEmpty(),"trailing space preserved");
    Set<String> first64=new HashSet<>();for(int i=0;i<40;i++)first64.add("単語"+i);
    check(lookup(pref,"あいう",first64).isEmpty(),"prefix never exceeds available nonduplicate words");

    StringBuilder rowLimit=new StringBuilder();
    for(int i=0;i<80;i++)rowLimit.append("あいう").append((char)(0x3041+i)).append('\t').append("候補").append(i).append('\n');
    WordCandidates.Index prefRows=WordCandidates.Index.fromBytes(bytes(rowLimit.toString()));
    first64.clear();for(int i=0;i<64;i++)first64.add("候補"+i);
    check(lookup(prefRows,"あいう",first64).isEmpty(),"prefix still scans at most 64 reading rows");

    java.lang.reflect.Field dictionary = WordCandidates.class.getDeclaredField("dictionary");
    dictionary.setAccessible(true);
    WordCandidates.Index coffee = WordCandidates.Index.fromBytes(bytes("こーひー\tコーヒー\t珈琲\n"));
    dictionary.set(null, coffee);
    l40.c request = new l40.c(); request.k="koーhiー";
    request.d=com.microsoft.fluency.ResultsFilter.PredictionSearchType.JAPANESE;
    List<m40.b> original = new ArrayList<>();
    for(String word:Arrays.asList("native-first","native-second","native-third")) original.add(new m40.h0(word,request.k,request));
    com.microsoft.fluency.Japanese.lastInput=null; com.microsoft.fluency.Japanese.calls=0;
    List<m40.b> enhanced=WordCandidates.enhance(original,request);
    if(fixed) {
      check(com.microsoft.fluency.Japanese.calls==1 && request.k.equals(com.microsoft.fluency.Japanese.lastInput),"native converter receives unchanged raw composition with direct prolonged sound mark");
      check(enhanced.size()==5,"romaji plus prolonged mark produces Japanese dictionary candidates");
      check(enhanced.get(0)==original.get(0) && enhanced.get(1)==original.get(1) && enhanced.get(4)==original.get(2),"native top two and remaining candidates retain identities and order");
      check(enhanced.get(2).getUserFacingText().equals("コーヒー") && enhanced.get(3).getUserFacingText().equals("珈琲"),"converted reading produces expected dictionary word order");
      m40.h0 candidate=(m40.h0)enhanced.get(2);
      check(candidate.b.equals(request.k) && candidate.c==request && "".equals(candidate.d),"raw commit metadata and separator retained");
    } else {
      check(com.microsoft.fluency.Japanese.calls==0 && enhanced==original,"published bug skips converter and dictionary for romaji plus prolonged mark");
    }
    check(original.size()==3,"enrichment does not mutate native collection");
    request.k="helloこーひー";
    check(WordCandidates.enhance(original,request)==original,"unrelated mixed-script composition remains excluded");
    request.k="koーhiー";request.f=true;
    check(WordCandidates.enhance(original,request)==original,"zero-input mode remains excluded");
    request.f=false;request.d=com.microsoft.fluency.ResultsFilter.PredictionSearchType.NORMAL;
    check(WordCandidates.enhance(original,request)==original,"non-Japanese prediction mode remains excluded");

    System.out.println("{\"mode\":"+quoted(args[1])+",\"status\":\"PASSED\",\"rows\":"+rows+",\"candidates\":"+total+
      ",\"returned\":"+returned+",\"incomplete_readings\":"+incomplete+",\"missing_candidates\":"+missing+
      ",\"unexpected_candidates\":"+unexpected+",\"maximum_candidates_per_reading\":"+max+",\"assertions\":"+checks+
      ",\"elapsed_ms\":"+(System.nanoTime()-start)/1000000L+",\"examples\":["+String.join(",",examples.stream().map(WordCandidatesCoverageTest::quoted).toArray(String[]::new))+"]}");
  }
}

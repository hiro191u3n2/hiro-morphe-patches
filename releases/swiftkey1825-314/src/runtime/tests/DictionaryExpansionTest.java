package hiro.swiftkey;
import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.lang.reflect.*;
import java.security.MessageDigest;
import java.util.*;

/** Executes the production parser and lookup for every merged candidate, without a mock index. */
public final class DictionaryExpansionTest {
  static long assertions;
  static final Set<String> NONE=Collections.emptySet();
  static void check(boolean v,String m){assertions++;if(!v)throw new AssertionError(m);}
  static byte[] b(String v){return v.getBytes(StandardCharsets.UTF_8);}
  static LinkedHashSet<String> lookup(WordCandidates.Index i,String q,Set<String> existing){return WordCandidates.lookup(i,q,existing);}
  static String row(String key,int n){StringBuilder b=new StringBuilder(key);for(int i=0;i<n;i++)b.append('\t').append("単語").append(i);return b.append('\n').toString();}
  static void invalid(String data)throws Exception{try{WordCandidates.Index.fromBytes(b(data));throw new AssertionError("accepted malformed "+data);}catch(IOException expected){assertions++;}}
  static String sha(byte[] b)throws Exception{byte[] d=MessageDigest.getInstance("SHA-256").digest(b);StringBuilder s=new StringBuilder();for(byte x:d)s.append(String.format("%02x",x&255));return s.toString();}
  static String json(Object v){if(v==null)return "null";if(v instanceof Number||v instanceof Boolean)return v.toString();if(v instanceof Map){StringJoiner s=new StringJoiner(",","{","}");for(Map.Entry<?,?> e:((Map<?,?>)v).entrySet())s.add(json(e.getKey())+":"+json(e.getValue()));return s.toString();}if(v instanceof Iterable){StringJoiner s=new StringJoiner(",","[","]");for(Object x:(Iterable<?>)v)s.add(json(x));return s.toString();}return "\""+v.toString().replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n")+"\"";}
  public static void main(String[] a)throws Exception {
    boolean fixed=a[2].equals("fixed");
    long started=System.nanoTime();byte[] data=Files.readAllBytes(Paths.get(a[0]));
    // Strict UTF-8 is independently checked: production index decodes candidate text permissively.
    CharsetDecoder strict=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
    try(Reader utf8=new InputStreamReader(new ByteArrayInputStream(data),strict)){char[] buffer=new char[8192];while(utf8.read(buffer)!=-1){}}
    WordCandidates.Index index=WordCandidates.Index.fromBytes(data);
    long rows=0,candidates=0,returned=0,duplicate=0,missing=0,unexpected=0,above32=0;int max=0;
    try(BufferedReader in=Files.newBufferedReader(Paths.get(a[0]),StandardCharsets.UTF_8)){
      for(String line;(line=in.readLine())!=null;){
        String[] f=line.split("\t",-1);check(f.length>=2,"at least one value");
        LinkedHashSet<String> expected=new LinkedHashSet<String>(Arrays.asList(f).subList(1,f.length));duplicate+=f.length-1-expected.size();
        LinkedHashSet<String> actual=lookup(index,f[0],NONE);rows++;candidates+=expected.size();returned+=actual.size();max=Math.max(max,expected.size());if(expected.size()>32)above32++;
        Set<String> absent=new HashSet<String>(expected);absent.removeAll(actual);missing+=absent.size();Set<String> extra=new HashSet<String>(actual);extra.removeAll(expected);unexpected+=extra.size();
        check(new ArrayList<String>(expected).equals(new ArrayList<String>(actual)),"all ordered exact candidates for "+f[0]);
        check(f[0].equals(WordCandidates.normalize(f[0]))&&WordCandidates.isReading(f[0]),"normalized supported reading");
        for(String word:expected)for(int j=0;j<word.length();j++)check(!Character.isISOControl(word.charAt(j)),"control in surface");
      }
    }
    check(rows==index.starts.length&&duplicate==0&&missing==0&&unexpected==0,"whole dictionary census");
    // Preserve every existing candidate and its relative ranking, including old data beyond new-source overlaps.
    long oldRows=0,oldCandidates=0;
    try(BufferedReader in=Files.newBufferedReader(Paths.get(a[1]),StandardCharsets.UTF_8)){
      for(String line;(line=in.readLine())!=null;){
        String[] f=line.split("\t",-1);List<String> actual=new ArrayList<String>(lookup(index,f[0],NONE));List<String> old=Arrays.asList(f).subList(1,f.length);
        check(actual.size()>=old.size()&&actual.subList(0,old.size()).equals(old),"existing ordered prefix retained "+f[0]);oldRows++;oldCandidates+=old.size();
      }
    }
    WordCandidates.Index many=WordCandidates.Index.fromBytes(b(row("こうき",80)));Set<String> existing=new HashSet<String>();for(int i=0;i<5;i++)existing.add("単語"+i);
    check(lookup(many,"こうき",NONE).size()==(fixed?80:32),"positive/negative control 32-candidate cutoff");
    LinkedHashSet<String> filtered=lookup(many,"こうき",existing);check(filtered.size()==(fixed?75:27)&&filtered.iterator().next().equals("単語5"),"native candidate dedup and order");
    check(lookup(many,"こうきあ",NONE).isEmpty(),"exact suffix excluded");
    StringBuilder prefix=new StringBuilder();for(int i=0;i<80;i++)prefix.append(row("あいう"+(char)(0x3041+i),80));
    WordCandidates.Index pi=WordCandidates.Index.fromBytes(b(prefix.toString()));
    check(lookup(pi,"あいう",NONE).size()==12,"prefix still 12 suggestions");
    Set<String> first32=new HashSet<String>();for(int i=0;i<32;i++)first32.add("単語"+i);
    check(lookup(pi,"あいう",first32).isEmpty(),"prefix still only first32 ranks");
    check(lookup(pi,"あい",NONE).isEmpty(),"short prefix excluded");
    for(String q:Arrays.asList("helloあいう"," あいう","あいう ","", "a", "あ", "あいう\n"))check(lookup(pi,q,NONE).isEmpty(),"invalid or mixed input excluded");
    StringBuilder bounded=new StringBuilder();Set<String> first64=new HashSet<String>();for(int i=0;i<80;i++){bounded.append("あいう").append((char)(0x3041+i)).append('\t').append("候補").append(i).append('\n');if(i<64)first64.add("候補"+i);}
    check(lookup(WordCandidates.Index.fromBytes(b(bounded.toString())),"あいう",first64).isEmpty(),"prefix still64 rows");
    for(String malformed:Arrays.asList("あ\t語\n","あい\t\n","あい\t語\t\n","あい\t\t語\n","あい\t語\r\n","あい\t語\0\n","アイ\t語\n","あい\t語\nあい\t別\n","あう\t語\nあい\t別\n"))invalid(malformed);
    check(WordCandidates.Index.fromBytes(new byte[0]).starts.length==0,"empty dictionary supported");
    check(WordCandidates.Index.fromBytes(b("あい\t愛")).starts.length==1,"last row without newline supported");
    try {WordCandidates.Index.fromBytes(new byte[WordCandidates.MAX_ASSET_BYTES+1]);throw new AssertionError("asset byte guard missing");}catch(IOException expected){assertions++;}
    byte[] tooManyRows=new byte[(fixed?3000000:1000000)+1];Arrays.fill(tooManyRows,(byte)'\n');
    try {WordCandidates.Index.fromBytes(tooManyRows);throw new AssertionError("row guard missing");}catch(IOException expected){check(expected.getMessage().equals("Too many dictionary rows"),"row bound before allocation");}
    tooManyRows=null;
    check(WordCandidates.normalize("ｺｰﾋｰ").equals("こーひー"),"katakana and width normalization retained");
    Field dict=WordCandidates.class.getDeclaredField("dictionary");dict.setAccessible(true);
    WordCandidates.Index coffee=WordCandidates.Index.fromBytes(b("こーひー\tコーヒー\t珈琲\n"));dict.set(null,coffee);
    l40.c request=new l40.c();request.k="koーhiー";request.d=com.microsoft.fluency.ResultsFilter.PredictionSearchType.JAPANESE;
    List<m40.b> original=new ArrayList<m40.b>();for(String word:Arrays.asList("native-first","native-second","native-third"))original.add(new m40.h0(word,request.k,request));
    com.microsoft.fluency.Japanese.calls=0;List<m40.b> enhanced=WordCandidates.enhance(original,request);
    check(com.microsoft.fluency.Japanese.calls==1&&request.k.equals(com.microsoft.fluency.Japanese.lastInput),"1.8.24 prolonged mark native conversion preserved");
    check(enhanced.size()==5&&enhanced.get(0)==original.get(0)&&enhanced.get(1)==original.get(1)&&enhanced.get(4)==original.get(2),"native candidate order preserved");
    check(enhanced.get(2).getUserFacingText().equals("コーヒー")&&enhanced.get(3).getUserFacingText().equals("珈琲"),"exact word order preserved");
    m40.h0 c=(m40.h0)enhanced.get(2);check(c.b.equals(request.k)&&c.c==request&&"".equals(c.d),"commit metadata preserved");
    check(original.size()==3,"no mutation of original");request.f=true;check(WordCandidates.enhance(original,request)==original,"prediction-only exclusion");request.f=false;request.d=com.microsoft.fluency.ResultsFilter.PredictionSearchType.NORMAL;check(WordCandidates.enhance(original,request)==original,"language exclusion");
    // Execute production init and the two actual reads using a minimal AssetManager host double.
    dict.set(null,null);Field startedField=WordCandidates.class.getDeclaredField("started");startedField.setAccessible(true);startedField.setBoolean(null,false);
    android.content.res.AssetManager.data=data;android.content.res.AssetManager.opens=0;android.content.res.AssetManager.closes=0;
    WordCandidates.init(new android.content.Context());long deadline=System.nanoTime()+30_000_000_000L;
    while(dict.get(null)==null&&System.nanoTime()<deadline)Thread.sleep(10);
    check(dict.get(null)!=null,"async production loader accepts merged asset");check(android.content.res.AssetManager.opens==2&&android.content.res.AssetManager.closes==2,"loader performs two complete closed reads");
    WordCandidates.init(new android.content.Context());check(android.content.res.AssetManager.opens==2,"init idempotent");
    dict.set(null,null);android.content.res.AssetManager.data=null;
    if(fixed){
      startedField.setBoolean(null,false);android.content.res.AssetManager.failOutOfMemory=true;
      final Throwable[] uncaught=new Throwable[1];Thread.UncaughtExceptionHandler prior=Thread.getDefaultUncaughtExceptionHandler();
      Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler(){public void uncaughtException(Thread t,Throwable e){uncaught[0]=e;}});
      try {
        WordCandidates.init(new android.content.Context());
        for(Thread t:Thread.getAllStackTraces().keySet())if(t.getName().equals("JapaneseWordDictionary"))t.join(10000);
        check(uncaught[0]==null&&dict.get(null)==null,"optional dictionary OOME contained in loader thread");
        request.d=com.microsoft.fluency.ResultsFilter.PredictionSearchType.JAPANESE;
        check(WordCandidates.enhance(original,request)==original,"native candidates usable after allocation failure");
      }finally{Thread.setDefaultUncaughtExceptionHandler(prior);android.content.res.AssetManager.failOutOfMemory=false;}
    }
    Map<String,Object> r=new LinkedHashMap<String,Object>();r.put("status","PASSED");r.put("mode",a[2]);r.put("dictionary_sha256",sha(data));r.put("asset_bytes",data.length);r.put("rows",rows);r.put("candidates",candidates);r.put("returned",returned);r.put("missing_candidates",missing);r.put("unexpected_candidates",unexpected);r.put("duplicate_pairs",duplicate);r.put("rows_above32",above32);r.put("maximum_candidates_per_reading",max);r.put("baseline_rows_preserved",oldRows);r.put("baseline_candidates_preserved",oldCandidates);r.put("all_existing_order_preserved",true);r.put("retained_index_payload_bytes",data.length+4*rows);r.put("heap_limit_bytes",Runtime.getRuntime().maxMemory());r.put("async_full_asset_load_verified",true);r.put("prefix_bounds_preserved",true);r.put("long_mark_fix_preserved",true);r.put("assertions",assertions);r.put("elapsed_ms",(System.nanoTime()-started)/1_000_000);r.put("device_tested",false);System.out.println(json(r));
  }
}

#!/usr/bin/env python3
"""Small platform doubles for testing the unmodified upstream L10n source on a JVM.
Not an Android device test; the emitted DEX is audited separately.
"""
from pathlib import Path
import base64
STUBS={
'android/content/Context.java':'''package android.content;
public class Context { private final android.content.res.Resources resources;
public Context(java.util.Locale... locales){resources=new android.content.res.Resources(locales);}
public android.content.res.Resources getResources(){return resources;}}''',
'android/content/res/Resources.java':'''package android.content.res;
public class Resources {private final Configuration configuration;
public Resources(java.util.Locale... locales){configuration=new Configuration(locales);}
public Configuration getConfiguration(){return configuration;}}''',
'android/content/res/Configuration.java':'''package android.content.res;
public class Configuration {public java.util.Locale locale;private final android.os.LocaleList locales;
public Configuration(java.util.Locale... list){locales=new android.os.LocaleList(list);locale=list.length==0?null:list[0];}
public android.os.LocaleList getLocales(){return locales;}}''',
'android/os/LocaleList.java':'''package android.os;
public class LocaleList {private final java.util.Locale[] locales;
public LocaleList(java.util.Locale... list){locales=list;}
public int size(){return locales.length;}public java.util.Locale get(int index){return locales[index];}}''',
'android/os/Build.java':'''package android.os;public class Build {public static class VERSION {public static int SDK_INT=37;}}''',
'android/icu/text/PluralRules.java':'''package android.icu.text;
public class PluralRules {private final java.util.Locale locale;private PluralRules(java.util.Locale value){locale=value;}
public static PluralRules forLocale(java.util.Locale locale){return new PluralRules(locale);}
public String select(double count){return "ja".equals(locale.getLanguage())?"other":count==1?"one":"other";}}''',
'app/morphe/extension/shared/Utils.java':'''package app.morphe.extension.shared;
public class Utils {public static android.content.Context context;public static android.content.Context getContext(){return context;}}''',
'app/morphe/extension/tiktok/settings/L10nTranslations.java':'''package app.morphe.extension.tiktok.settings;
import java.util.*;import app.hiro.tiktokjapanese.runtime.JapaneseTranslations;
// Dispatcher double matching the additive DEX wrapper. Original dispatcher bytes
// and all original language dictionaries are checked by LocalizationAudit.
public final class L10nTranslations {static Map<String,String> of(String language){
Map<String,String> japanese=JapaneseTranslations.forLanguage(language);if(japanese!=null)return japanese;
return "de".equals(language)?Collections.singletonMap("About","Info"):null;}}''',
'app/morphe/extension/tiktok/settings/LocalizationHostTest.java':'''package app.morphe.extension.tiktok.settings;
import java.nio.file.*;import java.nio.charset.StandardCharsets;import java.util.*;
import android.content.Context;import app.hiro.tiktokjapanese.runtime.JapaneseTranslations;
public final class LocalizationHostTest {
static int assertions;static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
public static void main(String[] args)throws Exception {
Map<String,String> map=JapaneseTranslations.forLanguage("ja");check(map.size()==2181,"complete catalog");
check(map==JapaneseTranslations.forLanguage("ja"),"one immutable cached dictionary");
for(String tag:new String[]{null,"en","de","ja-rjp","ja-JP","jargon",""})check(JapaneseTranslations.forLanguage(tag)==null,"precise language dispatch");
boolean immutable=false;try{map.put("wrong","wrong");}catch(UnsupportedOperationException expected){immutable=true;}check(immutable,"immutable dictionary");
Context ja=new Context(Locale.JAPAN,Locale.ENGLISH),en=new Context(Locale.ENGLISH,Locale.JAPAN);
int rows=0;for(String line:Files.readAllLines(Paths.get(args[0]),StandardCharsets.UTF_8)){
String[] parts=line.split("\\t",-1);String key=new String(Base64.getDecoder().decode(parts[0]),StandardCharsets.UTF_8);
String value=new String(Base64.getDecoder().decode(parts[1]),StandardCharsets.UTF_8);
check(value.equals(map.get(key)),"generated dictionary matches reviewed catalog: "+key);
check(value.equals(L10n.t(ja,key)),"real L10n Japanese lookup: "+key);rows++;}
check(rows==2181,"every row exercised through L10n");
check("About".equals(L10n.t(en,"About")),"English precedence preserved");
check("ja".equals(L10n.tableTag(ja)),"region falls back to Japanese");
check("ja".equals(L10n.shownLocale(ja).getLanguage()),"Japanese formatting locale");
check("Info".equals(L10n.t(new Context(Locale.GERMAN,Locale.JAPAN),"About")),"preferred other language preserved");
check(map.get("About").equals(L10n.t(new Context(Locale.KOREAN,Locale.JAPAN),"About")),"second preferred supported language");
check("unseen-source-key".equals(L10n.t(ja,"unseen-source-key")),"unknown key stays visible");
check(L10n.t(ja,(String)null)==null&&"".equals(L10n.t(ja,"")),"null/empty preserved");
CharSequence styled=new StringBuilder("About");check(L10n.t(ja,styled)==styled,"styled text untouched");
check(L10n.f(ja,"%1$d results",42).equals(String.format(Locale.ROOT,map.get("%1$d results"),42)),"number formatting");
for(long count:new long[]{0,1,2,100}){
check("other".equals(L10n.pluralCategory(ja,count)),"Japanese ICU plural selection");
check(L10n.quantity(ja,count,"1 result","%1$d results").equals(String.format(Locale.ROOT,map.get("%1$d results"),count)),"Japanese count displayed");}
Locale previous=Locale.getDefault();try{Locale.setDefault(Locale.JAPAN);check(map.get("About").equals(L10n.t((Context)null,"About")),"default locale before context ready");}
finally{Locale.setDefault(previous);}
check("About".equals(L10n.t(en,"About")),"locale cache invalidated");
System.out.println("PASS localization host: 2181 entries through real upstream L10n; locale order/cache; null/styled/unknown fallback; formatting/plurals; assertions="+assertions);
}}'''
}
def create(out,rows):
    out=Path(out)
    for name,text in STUBS.items():
        p=out/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text+'\n',encoding='utf-8')
    encoded=[]
    for row in rows:
        encoded.append('\t'.join(base64.b64encode(row[k].replace('\\n','\n').encode()).decode() for k in ('source','target')))
    (out/'catalog.tsv').write_text('\n'.join(encoded)+'\n')

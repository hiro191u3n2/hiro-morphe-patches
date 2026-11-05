package app.hiro.amazonreviews;
import android.webkit.WebView;
import android.os.Handler;
import java.util.regex.*;
public class NativeLifecycleRegression {
 static String hint(WebView view) { Matcher m=Pattern.compile("s\\.setSearchHint\\((true|false),(null|'[a-z0-9]+')\\)").matcher(view.script);if(!m.find())throw new AssertionError("No hint");return m.group(1)+":"+m.group(2); }
 static void one(String name,Runnable test){try{Handler.reset();test.run();System.out.println("PASS "+name);}catch(Throwable e){System.out.println("FAIL "+name+" "+e.getClass().getSimpleName());}}
 static void require(boolean ok){if(!ok)throw new AssertionError();}
 public static void main(String[] args){
  one("queryless-navigation-is-no-op",()->{WebView v=new WebView();v.url="https://www.amazon.co.jp/gp/gw/ajax/mshop.html";ReviewCountRuntime.authoritativeSearch(v,"PRIVATE_PROBE");String first=hint(v);int calls=v.calls;ReviewCountRuntime.authoritativeSearch(v,null);require(v.calls==calls&&hint(v).equals(first));});
  one("tracking-and-hash-preserve-native-identity",()->{WebView v=new WebView();v.url="https://www.amazon.co.jp/gp/gw/ajax/mshop.html";ReviewCountRuntime.authoritativeSearch(v,"PRIVATE_PROBE");String first=hint(v);v.url+="?ref=tracking#fragment";ReviewCountRuntime.inject(v,null);require(hint(v).equals(first));});
  one("keywordless-cached-search-resumes",()->{WebView v=new WebView();v.url="https://www.amazon.co.jp/";ReviewCountRuntime.authoritativeSearch(v,"PRIVATE_PROBE");ReviewCountRuntime.revokeSearch(v);require(hint(v).equals("false:'n0'"));try{ReviewCountRuntime.class.getMethod("resumeSearch",WebView.class).invoke(null,v);}catch(Exception e){throw new AssertionError(e);}require(hint(v).startsWith("true:'n"));});
  one("singular-keyword-identifies-new-query",()->{WebView v=new WebView();v.url="https://www.amazon.co.jp/s?keyword=one";ReviewCountRuntime.inject(v,null);String first=hint(v);v.url="https://www.amazon.co.jp/s?keyword=two";ReviewCountRuntime.inject(v,null);require(!hint(v).equals(first));});
 }
}

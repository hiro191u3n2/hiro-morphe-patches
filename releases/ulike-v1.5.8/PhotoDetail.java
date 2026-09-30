package com.hiro.ulike;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Looper;
import android.os.SystemClock;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.widget.Toast;
import java.util.concurrent.atomic.AtomicBoolean;

/** Optional save-only processing. No permissions, networking, models or services. */
@SuppressWarnings("deprecation")
public final class PhotoDetail {
    private static final String FILE="hiro_ulike_photo_detail";
    private static final String NOISE="photo_noise_enabled",SHARP="photo_sharp_enabled";
    private static final String NOISE_LEVEL="photo_noise_level",SHARP_LEVEL="photo_sharp_level";
    private static final String TEXTURE="photo_texture_priority",HALOS="photo_halo_suppression",SHADOWS="photo_shadow_priority";
    private static final int STRIP_ROWS=64;
    private static final AtomicBoolean BUSY=new AtomicBoolean(false);
    private static volatile SharedPreferences preferences;
    private static volatile Settings settings=new Settings(false,2,false,2);
    private static volatile String last="未実行。ノイズ低減・くっきり補正の初期値は両方オフです。";
    private PhotoDetail() {}
    public static final class Settings {
        public final boolean noiseOn,sharpOn,texturePriority,haloSuppression,shadowPriority;
        public final int noiseLevel,sharpLevel;
        public Settings(boolean n,int nl,boolean s,int sl){this(n,nl,s,sl,true,true,true);}
        public Settings(boolean n,int nl,boolean s,int sl,boolean t,boolean h,boolean d){
            noiseOn=n;noiseLevel=level(nl);sharpOn=s;sharpLevel=level(sl);
            texturePriority=t;haloSuppression=h;shadowPriority=d;
        }
    }
    private static int level(Object value) {
        try{int n=Integer.parseInt(String.valueOf(value));return n>=1&&n<=4?n:2;}catch(RuntimeException e){return 2;}
    }
    private static boolean flag(SharedPreferences p,String k){try{return p.getBoolean(k,false);}catch(RuntimeException e){return false;}}
    private static boolean protection(SharedPreferences p,String k){try{return p.getBoolean(k,true);}catch(RuntimeException e){return true;}}
    private static int storedLevel(SharedPreferences p,String k){try{return level(p.getString(k,"2"));}catch(RuntimeException e){return 2;}}
    public static synchronized void init(Context context) {
        try{
            if(preferences!=null||context==null)return;
            Context app=context.getApplicationContext();if(app==null)app=context;
            SharedPreferences p=app.getSharedPreferences(FILE,Context.MODE_PRIVATE);
            Settings s=new Settings(flag(p,NOISE),storedLevel(p,NOISE_LEVEL),flag(p,SHARP),storedLevel(p,SHARP_LEVEL),protection(p,TEXTURE),protection(p,HALOS),protection(p,SHADOWS));
            try{last=p.getString("last_result",last);}catch(RuntimeException e){ }
            preferences=p;settings=s;
        }catch(RuntimeException e){ /* Keep both disabled when storage cannot be read. */ }
    }
    private static SharedPreferences.Editor snapshot(SharedPreferences p,Settings s){
        return p.edit().putBoolean(NOISE,s.noiseOn).putString(NOISE_LEVEL,String.valueOf(s.noiseLevel))
            .putBoolean(SHARP,s.sharpOn).putString(SHARP_LEVEL,String.valueOf(s.sharpLevel))
            .putBoolean(TEXTURE,s.texturePriority).putBoolean(HALOS,s.haloSuppression).putBoolean(SHADOWS,s.shadowPriority);
    }
    private static synchronized boolean save(String key,Object value) {
        SharedPreferences p=preferences;if(p==null)return false;
        Settings old=settings;boolean n=old.noiseOn,s=old.sharpOn,t=old.texturePriority,h=old.haloSuppression,d=old.shadowPriority;
        int nl=old.noiseLevel,sl=old.sharpLevel;
        if(NOISE.equals(key))n=Boolean.TRUE.equals(value);
        else if(SHARP.equals(key))s=Boolean.TRUE.equals(value);
        else if(NOISE_LEVEL.equals(key))nl=level(value);
        else if(SHARP_LEVEL.equals(key))sl=level(value);
        else if(TEXTURE.equals(key))t=Boolean.TRUE.equals(value);
        else if(HALOS.equals(key))h=Boolean.TRUE.equals(value);
        else if(SHADOWS.equals(key))d=Boolean.TRUE.equals(value);
        else return false;
        Settings next=new Settings(n,nl,s,sl,t,h,d);
        // Complete synchronous snapshot; compensate failed writes even when the
        // framework's in-memory cache was changed before commit returned false.
        boolean stored=false;
        try{stored=snapshot(p,next).commit();}catch(RuntimeException e){ }
        if(!stored){try{snapshot(p,old).commit();}catch(RuntimeException e){ }return false;}
        settings=next;return true;
    }
    private static String name(int value){return new String[]{"オフ","弱","標準","強","最強"}[value];}
    private static void status(String message) {
        last="v1.5.8 / "+message;
        try{SharedPreferences p=preferences;if(p!=null)p.edit().putString("last_result",last).apply();}catch(RuntimeException e){ }
    }
    public static void install(final PreferenceActivity activity) {
        try{
            if(activity==null)return;init(activity);
            PreferenceScreen screen=activity.getPreferenceScreen();
            if(screen==null||activity.findPreference(NOISE)!=null)return;
            PreferenceCategory category=new PreferenceCategory(activity);
            category.setKey("hiro_photo_detail_category");category.setTitle("追加設定：ノイズ低減・くっきり補正");category.setOrder(-988);
            screen.addPreference(category);
            Settings s=settings;
            addControl(activity,category,NOISE,NOISE_LEVEL,"ノイズ低減",s.noiseOn,s.noiseLevel,
                "色ノイズと明暗ノイズを分離して強化。最強ではより滑らかになりますが、細部も柔らかくなる場合があります。");
            addControl(activity,category,SHARP,SHARP_LEVEL,"くっきり補正",s.sharpOn,s.sharpLevel,
                "ノイズ低減後に細かな輪郭と周辺コントラストを2段階で調整。粒状感の再強調を抑制。失われた細部やピンぼけの復元ではありません。");
            addProtection(activity,category,TEXTURE,"肌の質感を優先",s.texturePriority,
                "肌に近い色と輪郭から推定して質感を残し、硬すぎるシャープを抑えます。顔認識ではなく、似た色の背景にも作用します。");
            addProtection(activity,category,HALOS,"輪郭の白フチを抑える",s.haloSuppression,
                "輪郭の白フチ・黒フチを抑える明暗制限。オフでも色の飽和を防ぐ上限は残します。");
            addProtection(activity,category,SHADOWS,"暗部ノイズを優先して除去",s.shadowPriority,
                "ノイズ低減がオンのとき、暗い部分ほど滑らかさを優先します。明るい部分は強めません。");
            Preference result=new Preference(activity);result.setTitle("直近の画質処理結果（タップで更新）");result.setSummary(last);
            result.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){
                @Override public boolean onPreferenceClick(Preference p){p.setSummary(last);Toast.makeText(activity,last,Toast.LENGTH_LONG).show();return true;}
            });category.addPreference(result);
            Preference help=new Preference(activity);help.setTitle("処理対象と注意");
            help.setSummary("初期値は両方オフ。個別にオン／オフ・弱／標準／強／最強を選べて、再起動後も保持します。保護3項目は初期オンで、それだけでは画素処理を行いません。保存写真のみ、プレビュー・動画・撮影原本の保管には適用しません。解像度は下げませんが保存時間とメモリ使用量が増えます。8bit sRGBを対象とし、広色域・HDR・未対応形式・メモリ不足時は、この追加処理だけを見送ります。保存済み写真の再編集・再保存では重複適用されるためオフにしてください。実機の効果・所要時間は未検証です。");
            category.addPreference(help);
        }catch(RuntimeException e){ /* Do not prevent original settings opening. */ }
    }
    private static void addControl(final PreferenceActivity a,PreferenceCategory cat,final String key,final String strengthKey,String title,boolean enabled,int strength,String text) {
        final CheckBoxPreference toggle=new CheckBoxPreference(a);toggle.setKey(key);toggle.setPersistent(false);
        toggle.setTitle(title+"（保存写真）");toggle.setChecked(enabled);toggle.setSummary("初期値オフ。"+text);
        final ListPreference select=new ListPreference(a);select.setKey(strengthKey);select.setPersistent(false);
        select.setTitle(title+"の強さ");select.setDialogTitle(title+"の強さ");
        select.setEntries(new CharSequence[]{"弱","標準","強","最強"});select.setEntryValues(new CharSequence[]{"1","2","3","4"});
        select.setValue(String.valueOf(strength));select.setSummary(name(strength));select.setEnabled(enabled);
        toggle.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
            @Override public boolean onPreferenceChange(Preference p,Object value){
                try{if(save(key,value)){select.setEnabled(Boolean.TRUE.equals(value));return true;}}catch(RuntimeException e){ }
                Toast.makeText(a,"設定を保存できませんでした。変更は反映していません",Toast.LENGTH_LONG).show();return false;
            }
        });
        select.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
            @Override public boolean onPreferenceChange(Preference p,Object value){
                try{if(save(strengthKey,value)){p.setSummary(name(level(value)));return true;}}catch(RuntimeException e){ }
                Toast.makeText(a,"設定を保存できませんでした。変更は反映していません",Toast.LENGTH_LONG).show();return false;
            }
        });cat.addPreference(toggle);cat.addPreference(select);
    }
    private static void addProtection(final PreferenceActivity a,PreferenceCategory cat,final String key,String title,boolean enabled,String text){
        CheckBoxPreference pref=new CheckBoxPreference(a);pref.setKey(key);pref.setPersistent(false);
        pref.setTitle(title);pref.setChecked(enabled);pref.setSummary(text+" 初期値オン。再起動後も保持します。");
        pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
            @Override public boolean onPreferenceChange(Preference p,Object value){
                try{if(save(key,value))return true;}catch(RuntimeException e){ }
                Toast.makeText(a,"設定を保存できませんでした。変更は反映していません",Toast.LENGTH_LONG).show();return false;
            }
        });cat.addPreference(pref);
    }
    /** Called by the retained public edge hook; the previous edge algorithm is renamed,
     * not rewritten. Both switches OFF return its result without touching pixels. */
    public static Bitmap afterEdge(Bitmap input,Bitmap original) {
        Bitmap corrected=EdgeFaceCorrection.applyEdge153(input,original);
        return applyDetail(corrected,original);
    }
    private static void recycle(Bitmap b){try{if(b!=null&&!b.isRecycled())b.recycle();}catch(RuntimeException e){ }}
    public static Bitmap applyDetail(Bitmap input,Bitmap original) {
        Settings choice=settings;int noise=choice.noiseOn?choice.noiseLevel:0,sharp=choice.sharpOn?choice.sharpLevel:0;
        if(noise==0&&sharp==0){status("両方オフ：追加画質処理なし");return input;}
        if(input==null||input.isRecycled())return input;
        if(!BUSY.compareAndSet(false,true)){status("別の写真を処理中：追加画質処理を見送り");return input;}
        Bitmap out=null;
        long start=SystemClock.elapsedRealtime();
        try{
            if(Looper.myLooper()==Looper.getMainLooper()) {status("画面の停止を避けるため追加処理を見送り（メインスレッド）");return input;}
            Bitmap.Config config=input.getConfig();
            if(config!=Bitmap.Config.ARGB_8888&&config!=Bitmap.Config.RGB_565){status("未対応の画像形式：追加画質処理を見送り");return input;}
            if(input.getColorSpace()!=null&&!input.getColorSpace().isSrgb()){status("広色域画像の色を保護：追加画質処理を見送り");return input;}
            if(Build.VERSION.SDK_INT>=34&&input.hasGainmap()){status("HDRの明るさ情報を保護：追加画質処理を見送り");return input;}
            int width=input.getWidth(),height=input.getHeight();
            if(width<1||height<1)return input;
            int rows=Math.min(height,STRIP_ROWS+2*DetailPixels.HALO);
            long workspace=(long)width*rows*16L,output=(long)width*height*4L;
            Runtime rt=Runtime.getRuntime();long free=rt.maxMemory()-(rt.totalMemory()-rt.freeMemory());
            if((long)width*rows>Integer.MAX_VALUE||output+workspace+32L*1024*1024>free){status("メモリ不足：追加画質処理を見送り");return input;}
            DetailPixels.Work work=new DetailPixels.Work(width*rows);
            out=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
            out.setDensity(input.getDensity());out.setHasAlpha(input.hasAlpha());
            for(int y=0;y<height;y+=STRIP_ROWS){
                if(Thread.currentThread().isInterrupted()){status("処理中断：追加画質処理を見送り");return input;}
                int count=Math.min(STRIP_ROWS,height-y),top=Math.max(0,y-DetailPixels.HALO),bottom=Math.min(height,y+count+DetailPixels.HALO),nrows=bottom-top;
                input.getPixels(work.source,0,width,0,top,width,nrows);
                DetailPixels.filter(work,width,nrows,y-top,count,noise,sharp,choice.texturePriority,choice.haloSuppression,choice.shadowPriority);
                out.setPixels(work.output,(y-top)*width,width,0,y,width,count);
            }
            status("ノイズ低減："+name(noise)+"／くっきり補正："+name(sharp)+"／質感保護："+(choice.texturePriority?"オン":"オフ")+"／白フチ抑制："+(choice.haloSuppression?"オン":"オフ")+"／暗部優先："+(choice.shadowPriority?"オン":"オフ")+"／"+width+"×"+height+"／追加処理 "+(SystemClock.elapsedRealtime()-start)+"ms");
            Bitmap result=out;out=null;
            // Caller owns original; only discard a superseded temporary.
            if(input!=original)recycle(input);
            return result;
        }catch(OutOfMemoryError e){status("処理用メモリ不足：追加画質処理を見送り");}
        catch(LinkageError e){status("この端末では追加画質処理を利用できません");}
        catch(RuntimeException e){status("追加画質処理を見送り："+e.getClass().getSimpleName());}
        finally{recycle(out);BUSY.set(false);}
        return input;
    }
}

package com.hiro.ulike;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.*;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;

/** Bounded diagnostic history only. This class never reads or writes GPU proof
 * stores and cannot grant admission. All preference I/O belongs to its daemon;
 * producers only replace immutable scalar snapshots in bounded memory. A kill
 * before a successful commit may lose the newest transition. */
public final class VerificationJournal19101 {
    private VerificationJournal19101() {}
    static final String VERSION=ProcessingTiming1947.VERSION, STORE="ulike_verification_journal19101", KEY="history_v1";
    static final int LIMIT=16, VALUES=59, PREVIOUS_VALUES=47, LEGACY_VALUES=37, LABELS=5, MAX_TEXT=32768;
    // No entropy provider, Android call, storage access or lock in processId().
    // This is an opaque process-session label, not an authentication identity.
    private static final String PROCESS=Long.toHexString(System.currentTimeMillis())+"-"+
        Long.toHexString(System.nanoTime())+"-"+Integer.toHexString(System.identityHashCode(new Object()));
    private static final Engine JOURNAL=new Engine(PROCESS);
    public static String processId(){return PROCESS;}
    public static String version(){return VERSION;}
    public static void initialize(Context context){
        if(context==null)return;
        try{Context app=context.getApplicationContext();JOURNAL.start(new AndroidStore(app==null?context:app));}
        catch(Throwable unavailable){JOURNAL.initializationFailed();}
    }
    /** Called by the queue's optional bridge. No probe, image, key, exception,
     * source path or device fingerprint may be passed to this fixed schema. */
    public static void offer(long[] values,String[] labels){try{JOURNAL.offer(values,labels);}catch(Throwable optional){JOURNAL.dropped();}}
    /** Historical processes only; the live queue keeps its original summary. */
    public static String summary(){try{return JOURNAL.summary();}catch(Throwable unavailable){return "検証履歴の永続記録: 読出し失敗（過去履歴の有無は不明）";}}

    interface Store {String read() throws Exception;boolean write(String value) throws Exception;}
    private static final class AndroidStore implements Store {
        final Context context;SharedPreferences preferences;
        AndroidStore(Context value){context=value;}
        private SharedPreferences preferences(){if(preferences==null)preferences=context.getSharedPreferences(STORE,Context.MODE_PRIVATE);return preferences;}
        public String read(){return preferences().getString(KEY,"");}
        public boolean write(String value){return preferences().edit().putString(KEY,value).commit();}
    }
    static final class Entry {
        final String process,version;final long[] v;final String[] labels;
        Entry(String process,String version,long[] values,String[] names){
            if(!identifier(process,64)||!identifier(version,24)||values==null||values.length!=VALUES||names==null||names.length!=LABELS)
                throw new IllegalArgumentException("journal schema");
            v=values.clone();labels=names.clone();this.process=process;this.version=version;
            for(int i=0;i<v.length;i++)if(v[i]<0&&i!=29&&i!=30||v[i]< -1)throw new IllegalArgumentException("journal scalar");
            if(v[0]<1||v[3]>2||v[4]>3||v[5]>10||v[6]>19||v[27]>5||v[28]>4||v[29]>7||v[30]>2||v[7]>v[8]||v[8]>192||v[18]>9||v[19]>2||v[20]>2||v[21]>2||v[22]>2||v[23]>2||v[24]>2||v[26]>1)
                throw new IllegalArgumentException("journal ranges");
            if(v[37]>2||v[38]>6||v[39]>v[40]||v[41]>v[45]||v[42]>v[46]||v[43]>0xffffffffL||v[44]>0xffffffffL||
                    v[45]>Integer.MAX_VALUE||v[46]>Integer.MAX_VALUE||v[40]!=v[45]*v[46]||(v[41]==0)!=(v[42]==0)||
                    v[37]==0&&(v[38]!=0||v[39]!=0||v[40]!=0)||v[37]>0&&(v[38]<1||v[40]<1)||
                    v[38]==1&&(v[39]!=v[40]||v[41]!=0)||v[38]==2&&(v[39]<1||v[41]==0||v[43]==v[44])||v[38]==6&&(v[39]!=0||v[41]!=0))
                throw new IllegalArgumentException("journal comparison ranges");
            if(!validStages19104(v))throw new IllegalArgumentException("journal stage diagnostic ranges");
            for(String label:labels)if(!label(label))throw new IllegalArgumentException("journal label");
        }
        String key(){return process+":"+v[0];}
    }
    private static boolean identifier(String s,int max){
        if(s==null||s.length()<1||s.length()>max)return false;
        for(int i=0;i<s.length();i++){char c=s.charAt(i);if(!(c>='a'&&c<='z'||c>='A'&&c<='Z'||c>='0'&&c<='9'||c=='.'||c=='-'||c=='_'))return false;}
        return true;
    }
    private static boolean label(String s){if(s==null||s.length()>40)return false;for(int i=0;i<s.length();i++)if(s.charAt(i)<32||s.charAt(i)=='/'||s.charAt(i)=='\\')return false;return true;}
    static String encode(List<Entry> entries) throws Exception {
        if(entries.size()>LIMIT)throw new IOException("journal count");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
        out.writeInt(0x564a0104);out.writeInt(entries.size());
        for(Entry e:entries){out.writeUTF(e.process);out.writeUTF(e.version);for(long v:e.v)out.writeLong(v);for(String s:e.labels)out.writeUTF(s);}
        out.close();byte[] raw=bytes.toByteArray();String result="VJ3:"+hex(MessageDigest.getInstance("SHA-256").digest(raw))+":"+Base64.getEncoder().encodeToString(raw);
        if(result.length()>MAX_TEXT)throw new IOException("journal bytes");return result;
    }
    static List<Entry> decode(String text) throws Exception {
        ArrayList<Entry> result=new ArrayList<Entry>();if(text==null||text.length()==0)return result;
        if(text.length()>MAX_TEXT)throw new IOException("journal bytes");
        int schema=text.startsWith("VJ1:")?1:text.startsWith("VJ2:")?2:text.startsWith("VJ3:")?3:0;
        if(schema==0)throw new UnsupportedOperationException("journal version");
        if(text.length()<70||text.charAt(68)!=':')throw new IOException("journal envelope");
        byte[] raw=Base64.getDecoder().decode(text.substring(69));
        if(!hex(MessageDigest.getInstance("SHA-256").digest(raw)).equals(text.substring(4,68)))throw new IOException("journal checksum");
        DataInputStream in=new DataInputStream(new ByteArrayInputStream(raw));
        if(in.readInt()!=(schema==1?0x564a0101:schema==2?0x564a0103:0x564a0104))throw new UnsupportedOperationException("journal version");
        int count=in.readInt();if(count<0||count>LIMIT)throw new IOException("journal count");
        java.util.HashSet<String> keys=new java.util.HashSet<String>();
        for(int n=0;n<count;n++){
            String process=in.readUTF(),version=in.readUTF();long[] v=new long[VALUES];String[] labels=new String[LABELS];
            for(int i=0;i<(schema==1?LEGACY_VALUES:schema==2?PREVIOUS_VALUES:VALUES);i++)v[i]=in.readLong();for(int i=0;i<labels.length;i++)labels[i]=in.readUTF();
            Entry e=new Entry(process,version,v,labels);if(!keys.add(e.key()))throw new IOException("journal duplicate");result.add(e);
        }
        if(in.read()!= -1)throw new IOException("journal trailing bytes");return result;
    }
    /** These are bounded diagnostic scalars, never qualification evidence.
     * VJ1/VJ2 zero-fill this suffix, so old observations remain unobserved. */
    private static boolean validStages19104(long[] v){
        if(v[47]>=(1L<<28)||v[48]>13||v[49]>Integer.MAX_VALUE||v[50]>8192||
                v[51]>0xffffffffL||v[52]>0xffffffffL||v[53]>3||v[54]>1||v[57]>8192||v[58]>1||
                v[55]>8192L*Integer.MAX_VALUE||v[56]>8192L*Integer.MAX_VALUE)return false;
        if(v[48]==0){for(int i=49;i<=57;i++)if(v[i]!=0)return false;}
        if(v[50]>0&&(v[49]==0||v[54]!=1||v[50]>v[57]||v[51]==v[52]))return false;
        if(v[54]>0&&(v[48]==0||v[49]==0))return false;
        if(v[49]>0&&(v[48]==0||v[57]==0||v[55]==0||v[56]==0||v[56]>v[55]||
                (v[49]-1)*v[57]>=v[55]||v[50]>0&&(v[49]-1)*v[57]+v[50]>v[55]))return false;
        return true;
    }
    private static void appendStages19104(StringBuilder text,long[] v){
        if(v[47]==0)return;
        final String[] stages={"half入力","モデルhalf","モデルquarter","モデルeighth","モデル係数","Strong出力","平滑マスク","元画像補正係数","顔・肌マスク","モアレ出力","回転・リサイズ出力","仕上げ係数","出力ノイズ係数","最終出力"};
        text.append(" / 保存後の工程診断（既存経路対CPU基準・初回／署名は認定に不使用）: ");
        for(int i=0;i<stages.length;i++){
            if(i>0)text.append('・');int state=(int)((v[47]>>>(i*2))&3);
            text.append(stages[i]).append('=').append(state==0?"未観測":state==1?(i==13?"厳密全画素一致":"署名差未検出"):state==2?(i==13?"厳密比較不一致":"署名差あり"):"未完了");
        }
        if(v[48]>0){
            text.append(" / 最初に署名差を観測した工程: ").append(stages[(int)v[48]-1]);
            if(v[49]>0)text.append("・行 ").append(v[49]-1);
            if(v[49]>0)text.append(" / 署名走査量 ").append(v[56]).append('/').append(v[55]).append("語（最後の行を含む）");
            else text.append(" / 語数: 基準 ").append(v[55]).append("・比較先 ").append(v[56]);
            if(v[54]==0)text.append(" / CPU再生成の安定性未確認・GPU原因とは未判定");
            else if(v[50]>0){
                text.append(v[53]==0?" / 同じ行の厳密差 (x,y)=(":" / 同じ行の厳密差 (列,行)=(").append(v[50]-1).append(',').append(v[49]-1).append(')');
                if(v[53]!=0)text.append("・平坦化語位置 ").append((v[49]-1)*v[57]+v[50]-1);
                text.append(v[53]==0||v[53]==3?" / ARGB":" / 生ビット").append("期待 0x").append(argb19103(v[51])).append("・実値 0x").append(argb19103(v[52]));
            }else text.append(" / 再生成の署名を確認・厳密差位置は未観測");
        }
        if(v[58]!=0)text.append(" / 診断の一部は未完了・CPU再生成不安定を含む");
        text.append("（未観測工程と署名衝突の可能性があり、絶対的な原因証明ではありません）");
    }
    private static String argb19103(long value){String s=Long.toHexString(value&0xffffffffL);return "00000000".substring(s.length())+s;}
    private static String hex(byte[] bytes){StringBuilder s=new StringBuilder(bytes.length*2);final char[] h="0123456789abcdef".toCharArray();for(byte b:bytes){s.append(h[(b>>>4)&15]);s.append(h[b&15]);}return s.toString();}

    /** Package-private engine permits real asynchronous fault/race tests without
     * exposing a flush/wait operation to capture or qualification callers. */
    static final class Engine implements Runnable {
        final String process;final Object lock=new Object();
        final LinkedHashMap<String,Entry> rows=new LinkedHashMap<String,Entry>();
        Store store;Thread writer;boolean stopping,loaded;long generation,savedGeneration;
        long failedGeneration;int dropped;String loadState="未初期化（過去履歴の有無は不明）";
        Engine(String process){if(!identifier(process,64))throw new IllegalArgumentException("process");this.process=process;}
        void start(Store target){synchronized(lock){if(writer!=null)return;store=target;loadState="読込み中（過去履歴の有無は未確認）";
            Thread thread=new Thread(this,"ulike-verification-journal19101");thread.setDaemon(true);writer=thread;
            try{thread.start();}catch(Throwable failure){writer=null;store=null;loadState="初期化失敗（過去履歴の有無は不明）";throw failure;}}}
        void initializationFailed(){synchronized(lock){if(writer==null)loadState="初期化失敗（過去履歴の有無は不明）";}}
        void dropped(){synchronized(lock){if(dropped<Integer.MAX_VALUE)dropped++;}}
        void offer(long[] values,String[] labels){
            Entry entry=new Entry(process,VERSION,values,labels);
            synchronized(lock){rows.remove(entry.key());rows.put(entry.key(),entry);trim();generation++;lock.notifyAll();}
        }
        private void trim(){while(rows.size()>LIMIT)rows.remove(rows.keySet().iterator().next());}
        public void run(){
            List<Entry> previous=new ArrayList<Entry>();String status="復元完了";
            try{previous=decode(store.read());}catch(UnsupportedOperationException unsupported){status="未対応形式のため過去履歴を復元できません";}
            catch(Throwable failure){status="読込み失敗・破損のため過去履歴の有無は不明";}
            synchronized(lock){
                // Offers made before/during loading are newer than every disk
                // snapshot. Preserve them, including their finished state.
                List<Entry> pending=new ArrayList<Entry>(rows.values());rows.clear();
                for(Entry entry:previous)rows.put(entry.key(),entry);
                for(Entry entry:pending){rows.remove(entry.key());rows.put(entry.key(),entry);}trim();
                loaded=true;loadState=status;lock.notifyAll();
            }
            long attempted= -1;int retries=0;
            for(;;){
                List<Entry> snapshot;long wanted;
                synchronized(lock){
                    while(!stopping&&(generation==savedGeneration||generation==attempted&&retries>=3))try{lock.wait();}catch(InterruptedException ignored){}
                    if(stopping)return;wanted=generation;
                    if(wanted!=attempted){attempted=wanted;retries=0;}
                    snapshot=new ArrayList<Entry>(rows.values());
                }
                boolean success=false;
                try{success=store.write(encode(snapshot));}catch(Throwable failure){}
                synchronized(lock){
                    if(success){savedGeneration=wanted;failedGeneration=0;retries=0;}
                    else{failedGeneration=wanted;retries++;if(!stopping&&generation==wanted&&retries<3)try{lock.wait(1000L);}catch(InterruptedException ignored){}}
                    lock.notifyAll();
                }
            }
        }
        String summary(){
            List<Entry> snapshot;String loadedState,save;int lost;boolean ready;
            synchronized(lock){snapshot=new ArrayList<Entry>(rows.values());loadedState=loadState;ready=loaded;lost=dropped;
                save=failedGeneration>0&&savedGeneration<generation?"保存失敗（未反映の変更あり）":savedGeneration<generation?"保存待ち（未反映の変更あり）":generation==0?"今回の保存対象なし":"直近の反映を確認";}
            StringBuilder text=new StringBuilder("検証履歴の永続記録: ").append(loadedState).append(" / ").append(save);
            if(lost>0)text.append(" / 記録を受け取れなかった更新 ").append(lost).append("件");
            int previous=0;
            for(Entry e:snapshot)if(!process.equals(e.process)){
                previous++;long[] v=e.v;
                text.append('\n').append('#').append(v[0]).append(' ').append(e.labels[0]).append(" / 撮影 ").append(v[1]>0?"#"+v[1]:"未観測")
                    .append(" / プロセス ").append(e.process).append(" / v").append(e.version).append(": ");
                if(v[3]==2)text.append("前回完了 / ").append(e.labels[2]);
                else text.append(v[3]==1?"前回実行中・結果不明":"前回待機中・結果不明");
                if(e.labels[3].length()>0)text.append(" / 種別: ").append(e.labels[3]);
                text.append(" / 最終観測工程: ").append(e.labels[1]);
                if(v[8]>0)text.append(' ').append(v[7]).append('/').append(v[8]);
                text.append(" / 写真条件の認定書込 ").append(v[9]).append("回 / 固定テストの認定書込 ").append(v[10]).append("回");
                if(v[17]>0)text.append(" / 最終観測までの実行経過 ").append(v[17]/1000000L).append("ms");
                if(v[18]>0){
                    text.append(" / 連結の詳細: ").append(e.labels[4]);
                    if(v[19]>0)text.append(' ').append(v[19]).append("/2回目");
                    text.append(v[26]==1?"・工程完了":(v[38]==2||v[38]==6)&&v[18]==(v[37]==1?5:6)?(v[38]==6?"・出力寸法不一致で終了":"・差分検出で終了"):"・工程の完了未観測");
                    if(v[25]>0)text.append("・最終観測まで ").append(v[25]/1000000L).append("ms");
                    text.append(" / CPU基準作成完了 ").append(v[20]).append("/2 / 全画素一致: 既存 ").append(v[21]).append("/2・連結 ").append(v[22])
                        .append("/2 / 速度計測完了: 既存 ").append(v[23]).append("/2・連結 ").append(v[24]).append("/2");
                }
                if(v[37]>0){
                    text.append(" / 比較結果: ").append(v[37]==1?"既存経路":"連結候補").append("対CPU基準・")
                        .append(v[38]==1?"全画素一致":v[38]==2?"差分を検出":v[38]==3?"未完了":v[38]==4?"中断":v[38]==6?"出力寸法不一致":"例外終了")
                        .append(" / 比較済み ").append(v[39]).append('/').append(v[40]).append("画素");
                    if(v[41]>0&&v[42]>0){
                        text.append(" / 最初の不一致 (x,y)=(").append(v[41]-1).append(',').append(v[42]-1)
                            .append(") / ARGB期待 0x").append(argb19103(v[43])).append("・実値 0x").append(argb19103(v[44]))
                            .append(" / 実値−期待(A,R,G,B)=(");
                        for(int shift=24;shift>=0;shift-=8){if(shift!=24)text.append(',');text.append(((v[44]>>>shift)&255)-((v[43]>>>shift)&255));}
                        text.append(')');
                    }
                }
                appendStages19104(text,v);
                if(v[34]>0&&v[35]>0)text.append(" / 速度観測: 基準 ").append(v[34]/1000000L).append("ms・候補 ").append(v[35]/1000000L).append("ms・上限 ").append(v[36]/1000000L).append("ms");
            }
            if(previous==0&&ready&&"復元完了".equals(loadedState))text.append("\n別プロセスの履歴: 保存された記録なし");
            return text.append("\n※最大16件の診断記録です。未反映のまま終了すると最新の状態が残らない場合があります。結果不明は成功・失敗・取消を意味せず、認定の復元やGPU採用には使いません。").toString();
        }
        void stopForTest(){synchronized(lock){stopping=true;lock.notifyAll();}}
    }
}

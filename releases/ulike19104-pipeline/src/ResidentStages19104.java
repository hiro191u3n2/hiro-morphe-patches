package com.hiro.ulike;

import android.graphics.Bitmap;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

/** Bounded after-save diagnostics, NEVER admission evidence. Row SHA-256 values
 * localize an observed difference; only the independent final exact comparator
 * can prove output equality. At most 2 MiB of row signatures plus bounded 32-row readback and
 * scalar/hash scratch are kept; no image, plan, model or callback is retained by this observer. */
public final class ResidentStages19104 {
    static final int HALF=0,MODEL_HALF=1,MODEL_QUARTER=2,MODEL_EIGHTH=3,MODEL_EVIDENCE=4,
        STRONG=5,SMOOTH=6,SOURCE_PLAN=7,FACE_MASK=8,MOIRE=9,GEOMETRY=10,FINISH_PLAN=11,OUTPUT_NOISE=12,FINAL=13,COUNT=14;
    static final long MAX_SIGNATURE_BYTES=2L*1024*1024;
    static final int MAX_ROW_WORDS=8192;
    private static final Comparator<Field> FIELD_ORDER=new Comparator<Field>(){public int compare(Field a,Field b){return a.getName().compareTo(b.getName());}};
    private static final ThreadLocal<Context> ACTIVE=new ThreadLocal<Context>();
    private ResidentStages19104(){}
    static final class Rows {
        final int width,height;final long words;final long[] hashes;
        Rows(int width,int height,long words){this.width=width;this.height=height;this.words=words;hashes=new long[Math.multiplyExact(height,4)];}
    }
    static final class Context {
        final long deadline=System.nanoTime()+30_000_000_000L;
        final GpuQualification1961.Cancellation cancellation;final Rows[] reference=new Rows[COUNT];
        final int[] states=new int[COUNT];int role,first=-1,row=-1,column=-1,expected,actual,kind,referenceWidth;
        long allocated,expectedWords,actualWords;int[] actualRow;boolean replayVerified,observerFailed,replayFinished;
        Context(GpuQualification1961.Cancellation c){cancellation=c;}
        void check(){if(System.nanoTime()-deadline>=0){observerFailed=true;if(role==2)throw new java.util.concurrent.CancellationException("stage replay deadline");throw new IllegalStateException("stage diagnostic deadline");}if(Thread.currentThread().isInterrupted()||cancellation!=null&&cancellation.cancelled())throw new java.util.concurrent.CancellationException("stage diagnostic cancelled");}
    }
    public static void begin(Object cancellation){ACTIVE.set(new Context((GpuQualification1961.Cancellation)cancellation));}
    public static void baseline(){Context c=ACTIVE.get();if(c!=null)c.role=1;}
    public static boolean needsReplay(){Context c=ACTIVE.get();return c!=null&&c.first>=0&&c.actualRow!=null;}
    public static boolean replayComplete(){Context c=ACTIVE.get();return c!=null&&c.replayFinished;}
    public static void replay(){Context c=ACTIVE.get();if(c!=null)c.role=2;}
    public static boolean canReplay(long imageBytes){Context c=ACTIVE.get();if(c==null)return false;
        try{c.check();long overhead=c.allocated+16L*MAX_ROW_WORDS+131072L;if(imageBytes<0||imageBytes>Long.MAX_VALUE-overhead||!GpuNoise1960.workspaceFits(imageBytes+overhead)){c.observerFailed=true;return false;}return true;}
        catch(Throwable diagnosticOnly){c.observerFailed=true;return false;}}

    public static void clear(){ACTIVE.remove();}
    public static void failed(){Context c=ACTIVE.get();if(c!=null)c.observerFailed=true;}
    public static long[] finish(int finalResult){
        Context c=ACTIVE.get();ACTIVE.remove();if(c==null)return null;
        c.states[FINAL]=finalResult==ResidentProof1978.EXACT?1:finalResult==ResidentProof1978.MISMATCH?2:3;
        long packed=0;for(int i=0;i<COUNT;i++)packed|=(long)c.states[i]<<(i*2);
        return new long[]{packed,c.first+1,c.row+1,c.column+1,c.expected&0xffffffffL,c.actual&0xffffffffL,
            c.kind,c.replayVerified?1:0,c.expectedWords,c.actualWords,c.referenceWidth,c.observerFailed?1:0};
    }
    public static void observe(int stage,Object value){
        Context c=ACTIVE.get();if(c==null||stage<0||stage>=FINAL||value==null)return;
        try{
            c.check();
            if(c.role==2&&stage!=c.first&&!(stage==MODEL_HALF&&c.first>=MODEL_HALF&&c.first<=MODEL_EVIDENCE)&&!(stage==SOURCE_PLAN&&c.first==FACE_MASK)&&!(stage==FINISH_PLAN&&c.first==OUTPUT_NOISE))return;
            if(value instanceof StrongNoise1958.Model){model(c,(StrongNoise1958.Model)value);return;}
            if(value instanceof Bitmap){Bitmap b=(Bitmap)value;scan(c,stage,b.getWidth(),b.getHeight(),0,new Read(){public void row(int row,int[] out){b.getPixels(out,0,b.getWidth(),0,row,b.getWidth(),1);}});return;}
            if(value instanceof int[]){int[] a=(int[])value;array(c,stage,a,Math.min(MAX_ROW_WORDS,Math.max(1,a.length)),stage==HALF?3:0);return;}
            if(value instanceof byte[]){byte[] a=(byte[])value;bytes(c,stage,new byte[][]{a});return;}
            if(value instanceof QualityPixels1932.Plan){QualityPixels1932.Plan p=(QualityPixels1932.Plan)value;
                primitive(c,stage,p);
                if(stage==SOURCE_PLAN)mask(c,p.faceRegions);
                if(stage==FINISH_PLAN&&p.localNoise!=null)primitive(c,OUTPUT_NOISE,p.localNoise);
            }
        }catch(java.util.concurrent.CancellationException cancelled){throw cancelled;}
        catch(Throwable unavailable){c.observerFailed=true;if(c.role==1)c.states[stage]=3;}
    }
    /** Explicit dimensions for half/mask rows; no reference to the input kept. */
    public static void observeWords(int stage,int[] value,int width){
        Context c=ACTIVE.get();if(c==null||value==null||stage<0||stage>=FINAL)return;
        try{array(c,stage,value,width,0);}catch(java.util.concurrent.CancellationException cancelled){throw cancelled;}
        catch(Throwable unavailable){c.observerFailed=true;if(c.role==1)c.states[stage]=3;}
    }
    private interface Read {void row(int y,int[] out);}
    private static void array(Context c,int stage,int[] value,int width,int kind)throws Exception{
        if(width<1||width>MAX_ROW_WORDS)throw new IllegalArgumentException("diagnostic width");
        int height=(value.length+width-1)/width;
        scan(c,stage,width,height,value.length,kind,new Read(){public void row(int y,int[] out){Arrays.fill(out,0);System.arraycopy(value,y*width,out,0,Math.min(width,value.length-y*width));}});
    }
    private static void bytes(Context c,int stage,byte[][] arrays)throws Exception{
        int length=0;for(byte[] a:arrays)length=Math.addExact(length,a.length);
        final int total=length,width=Math.min(1024,Math.max(1,total)),height=(total+width-1)/width;
        scan(c,stage,width,height,total,1,new Read(){public void row(int y,int[] out){Arrays.fill(out,0);int from=y*width,to=Math.min(total,from+width),base=0;
            for(byte[] a:arrays){int lo=Math.max(from,base),hi=Math.min(to,base+a.length);for(int at=lo;at<hi;at++)out[at-from]=a[at-base]&255;base+=a.length;}}});
    }
    private static void model(Context c,StrongNoise1958.Model model)throws Exception{
        int[][] maps=StrongNoise1958.gpuMaps1960(model);int w=model.width;
        for(int i=0;i<3;i++){w=(w+1)/2;array(c,MODEL_HALF+i,maps[i],w,0);}
        Field e=StrongNoise1958.Model.class.getDeclaredField("evidence"),r=StrongNoise1958.Model.class.getDeclaredField("runtimeEvidence");e.setAccessible(true);r.setAccessible(true);
        float[] a=(float[])e.get(model),b=(float[])r.get(model);int length=a.length+b.length;
        scan(c,MODEL_EVIDENCE,Math.min(MAX_ROW_WORDS,length),(length+Math.min(MAX_ROW_WORDS,length)-1)/Math.min(MAX_ROW_WORDS,length),2,new Read(){public void row(int y,int[] out){
            Arrays.fill(out,0);int start=y*out.length;for(int i=0;i<out.length&&start+i<length;i++)out[i]=Float.floatToRawIntBits(start+i<a.length?a[start+i]:b[start+i-a.length]);}});
    }
    private static void mask(Context c,Object mask)throws Exception{
        if(mask==null){array(c,FACE_MASK,new int[]{0},1,1);return;}
        ArrayList<byte[]> planes=new ArrayList<byte[]>();Field[] fields=mask.getClass().getDeclaredFields();Arrays.sort(fields,FIELD_ORDER);
        for(Field f:fields)if(!Modifier.isStatic(f.getModifiers())&&f.getType()==byte[].class){f.setAccessible(true);byte[] a=(byte[])f.get(mask);if(a!=null)planes.add(a);}
        if(planes.isEmpty()){c.observerFailed=true;if(c.role==1)c.states[FACE_MASK]=3;return;}
        bytes(c,FACE_MASK,planes.toArray(new byte[planes.size()][]));
    }
    private static void primitive(Context c,int stage,Object value)throws Exception{
        Field[] fields=value.getClass().getDeclaredFields();Arrays.sort(fields,FIELD_ORDER);
        int size=0;
        for(Field f:fields){if(Modifier.isStatic(f.getModifiers()))continue;Class<?> t=f.getType();f.setAccessible(true);
            int n=t==int.class||t==boolean.class||t==float.class?1:t==double.class?2:0;
            if(t==float[].class){float[] a=(float[])f.get(value);n=a==null?0:a.length;}
            if(n>MAX_ROW_WORDS-size)throw new IllegalArgumentException("diagnostic primitive bound");size+=n;
        }
        int[] words=new int[Math.max(1,size)];int at=0;
        for(Field f:fields){if(Modifier.isStatic(f.getModifiers()))continue;Class<?> t=f.getType();
            if(t==int.class)words[at++]=f.getInt(value);else if(t==boolean.class)words[at++]=f.getBoolean(value)?1:0;
            else if(t==float.class)words[at++]=Float.floatToRawIntBits(f.getFloat(value));
            else if(t==double.class){long bits=Double.doubleToRawLongBits(f.getDouble(value));words[at++]=(int)bits;words[at++]=(int)(bits>>>32);}
            else if(t==float[].class){float[] a=(float[])f.get(value);if(a!=null)for(float x:a)words[at++]=Float.floatToRawIntBits(x);}
        }
        array(c,stage,words,words.length,2);
    }

    private static long[] hash(MessageDigest digest,int[] row,byte[] bytes){
        for(int i=0;i<row.length;i++){int x=row[i],at=i*4;bytes[at]=(byte)x;bytes[at+1]=(byte)(x>>>8);bytes[at+2]=(byte)(x>>>16);bytes[at+3]=(byte)(x>>>24);}
        byte[] raw=digest.digest(bytes);long[] result=new long[4];for(int i=0;i<4;i++)for(int j=0;j<8;j++)result[i]=(result[i]<<8)|(raw[i*8+j]&255L);return result;
    }
    private static boolean matches(Rows ref,int row,long[] hash){for(int i=0;i<4;i++)if(ref.hashes[row*4+i]!=hash[i])return false;return true;}
    private static void scan(Context c,int stage,int width,int height,int kind,Read reader)throws Exception{
        scan(c,stage,width,height,(long)width*height,kind,reader);
    }
    private static void scan(Context c,int stage,int width,int height,long total,int kind,Read reader)throws Exception{
        if(stage<0||stage>=FINAL||width<1||width>MAX_ROW_WORDS||height<1)throw new IllegalArgumentException("diagnostic shape");
        if(c.role==2&&c.first!=stage)return;
        if(c.role==1&&c.first>=0&&stage>c.first)return;
        Rows ref=c.reference[stage];long charge=32L*height;
        if(c.role==0){long oldCharge=ref==null?0:32L*ref.height;
            // Until replacement succeeds the old signature vector remains owned.
            // Charge both vectors at peak, and never decrement on a failed gate.
            if(charge>MAX_SIGNATURE_BYTES-c.allocated||!GpuNoise1960.workspaceFits(c.allocated+charge+16L*width+131072L))throw new IllegalStateException("diagnostic signature budget");
            Rows replacement=new Rows(width,height,total);c.reference[stage]=replacement;ref=replacement;c.allocated=c.allocated-oldCharge+charge;}
        if(ref==null){if(c.role==1)c.states[stage]=3;return;}
        if(ref.width!=width||ref.height!=height||ref.words!=total){if(c.role==1){c.states[stage]=2;c.first=stage;c.expectedWords=ref.words;c.actualWords=total;c.row=-1;c.column=-1;c.actualRow=null;c.expected=0;c.actual=0;c.referenceWidth=width;c.kind=kind;c.replayVerified=false;}return;}
        MessageDigest digest=MessageDigest.getInstance("SHA-256");int[] row=new int[width];byte[] bytes=new byte[width*4];
        if(c.role==2){c.check();reader.row(c.row,row);if(!matches(ref,c.row,hash(digest,row,bytes))){c.observerFailed=true;c.replayFinished=true;throw new java.util.concurrent.CancellationException("stage replay unstable");}
            c.replayVerified=true;for(int x=0;x<width;x++)if(row[x]!=c.actualRow[x]){c.column=x;c.expected=row[x];c.actual=c.actualRow[x];break;}
            c.replayFinished=true;throw new java.util.concurrent.CancellationException("stage replay observed; stop before unused downstream work");}
        for(int y=0;y<height;y++){c.check();reader.row(y,row);long[] h=hash(digest,row,bytes);
            if(c.role==0){System.arraycopy(h,0,ref.hashes,y*4,4);continue;}
            if(!matches(ref,y,h)){c.states[stage]=2;c.first=stage;c.row=y;c.kind=kind;c.referenceWidth=width;c.expectedWords=total;c.actualWords=Math.min(total,(long)(y+1)*width);c.actualRow=row.clone();return;}
        }
        if(c.role==1)c.states[stage]=1;
    }
    /** Only invoked before finish-bank allocation, where slots22/23 are free.
     * Mode0 performs an exact raw-word crop. No new shader/native operation.
     * Every scratch lease and slot is released before the original graph resumes. */
    public static void observeGpu(int stage,Object value,int slot,int width,int height){
        Context c=ACTIVE.get();if(c==null||stage<0||stage>=FINAL||c.role!=1||c.first>=0||!(value instanceof GpuNoise1960.Session))return;
        GpuNoise1960.Session session=(GpuNoise1960.Session)value;GpuNoise1960.Lease1971 lease=null;
        try{
            c.check();if(slot==22||slot==23||width<1||width>MAX_ROW_WORDS)throw new IllegalArgumentException("diagnostic GPU slot/width");
            final int batchRows=Math.min(32,height);final long bytes=4L*width*batchRows;
            // Includes all retained signatures, actual mismatch row, scan row,
            // hash bytes, Java readback batch, fixed reflection/digest scratch.
            long javaPeak=c.allocated+bytes+16L*width+131072L;
            if(!GpuNoise1960.workspaceFits(bytes+javaPeak+4L)||!GpuNoise1960.supports(GpuNoise1960.GEOMETRY)){c.states[stage]=3;return;}
            lease=session.reserveCapacity1983(new int[]{22,23},new long[]{4,bytes},new long[]{0,0},javaPeak);
            if(lease==null||!lease.revalidate1971()){c.states[stage]=3;return;}
            if(!session.run(new GpuNoise1960.Batch().allocate(22,4).allocate(23,bytes)))throw new IllegalStateException("diagnostic allocation");
            final int[][] cached={null};final int[] start={-1};
            scan(c,stage,width,height,0,new Read(){public void row(int y,int[] out){c.check();
                if(start[0]<0||y>=start[0]+batchRows){
                    int rows=Math.min(batchRows,height-y);int[] u=new int[32];u[1]=width;u[2]=height;u[4]=width;u[5]=height;u[15]=y;u[16]=y+rows;u[20]=1;
                    if(!session.run(new GpuNoise1960.Batch().dispatch(GpuNoise1960.GEOMETRY,new int[]{slot,23,22,22,22,22},u,null,width*rows)))throw new IllegalStateException("diagnostic raw row copy");
                    cached[0]=null;cached[0]=session.readInts(23,width*rows);start[0]=y;
                    if(cached[0]==null||cached[0].length!=width*rows)throw new IllegalStateException("diagnostic row read");
                }
                c.check();System.arraycopy(cached[0],(y-start[0])*width,out,0,width);
            }});
        }catch(java.util.concurrent.CancellationException cancelled){throw cancelled;}
        catch(Throwable unavailable){c.observerFailed=true;c.states[stage]=3;}
        finally{if(lease!=null){lease.close();if(!session.releaseSlots1991(new int[]{22,23}))c.observerFailed=true;}}
    }
    public static boolean valid(long[] v){
        if(v==null||v.length!=12)return false;for(long x:v)if(x<0)return false;
        return v[0]<(1L<<(COUNT*2))&&v[1]<=FINAL&&v[2]<=Integer.MAX_VALUE&&v[3]<=MAX_ROW_WORDS&&
            v[4]<=0xffffffffL&&v[5]<=0xffffffffL&&v[6]<=3&&v[7]<=1&&v[10]<=MAX_ROW_WORDS&&v[11]<=1&&
            (v[1]>0||v[2]==0&&v[3]==0&&v[7]==0)&&
            (v[3]==0||v[2]>0&&v[7]==1&&v[4]!=v[5])&&
            (v[7]==0||v[1]>0&&v[2]>0);
    }
    public static void appendSummary(StringBuilder text,long[] v){
        if(!valid(v)||v[0]==0)return;
        final String[] labels={"half入力","モデルhalf","モデルquarter","モデルeighth","モデル係数","Strong出力","平滑マスク","元画像補正係数","顔・肌マスク","モアレ出力","回転・リサイズ出力","仕上げ係数","出力ノイズ係数","最終出力"};
        text.append(" / 保存後の工程診断（既存経路対CPU基準・初回／署名は認定に不使用）: ");
        for(int i=0;i<COUNT;i++){if(i>0)text.append("・");int state=(int)((v[0]>>>(i*2))&3);
            text.append(labels[i]).append('=');
            text.append(state==0?"未観測":state==1?(i==FINAL?"厳密全画素一致":"署名差未検出"):state==2?(i==FINAL?"厳密比較不一致":"署名差あり"):"未完了");
        }
        if(v[1]>0){text.append(" / 最初に署名差を観測した工程: ").append(labels[(int)v[1]-1]);
            if(v[2]>0)text.append("・行 ").append(v[2]-1);
            if(v[2]>0)text.append(" / 署名走査量 ").append(v[9]).append('/').append(v[8]).append("語（最後の行を含む）");
            else text.append(" / 語数: 基準 ").append(v[8]).append("・比較先 ").append(v[9]);
            if(v[7]==0)text.append(" / CPU再生成の安定性未確認・GPU原因とは未判定");
            else if(v[3]>0){
                text.append(v[6]==0?" / 同じ行の厳密差 (x,y)=(":" / 同じ行の厳密差 (列,行)=(").append(v[3]-1).append(',').append(v[2]-1).append(')');
                if(v[6]!=0)text.append("・平坦化語位置 ").append((v[2]-1)*v[10]+v[3]-1);
                text.append(v[6]==0||v[6]==3?" / ARGB":" / 生ビット").append("期待 0x").append(hex(v[4])).append("・実値 0x").append(hex(v[5]));
            }else text.append(" / 再生成の署名を確認・厳密差位置は未観測");
        }
        if(v[11]!=0)text.append(" / 診断の一部は未完了・CPU再生成不安定を含む");
        text.append("（未観測工程と署名衝突の可能性あり。仕上げ係数は直接保持した数値のみで、マスクの呼出し内部・GPUマスク中間値は未観測）");
    }
    private static String hex(long n){String s=Long.toHexString(n&0xffffffffL);return "00000000".substring(s.length())+s;}

}

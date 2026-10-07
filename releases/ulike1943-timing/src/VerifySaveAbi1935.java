import java.io.File;
import java.util.*;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;

/** Independent reflection ABI and effective-save-path audit. Run on an APPLIED APK,
 * not stock: the stock EXIF/global-holder path was removed by earlier releases. */
public final class VerifySaveAbi1935 {
    static final String AUTO="Li/o/a/b1/a/b/f/a;", TASK="Li/o/a/b1/a/b/f/a$a;", CALLBACK="Li/o/a/b1/a/b/f/a$c;";
    static final String CONTROL="Li/o/a/q/c/c/b/e;", LISTENER="Li/o/a/q/c/c/b/e$c;", NOTIFY="Li/o/a/q/c/c/b/e$a;";
    static final String STORE="Li/f/l/n/s/a;", BITMAP="Landroid/graphics/Bitmap;", SETTINGS="Lcom/hiro/ulike/PhotoDetail$Settings;";
    static int checks;
    static void need(boolean b,String msg){checks++;if(!b)throw new AssertionError(msg);}
    static void field(Map<String,ClassDef> all,String owner,String name,String type,boolean isStatic){
        Field f=VerifyFrontAbi1931.field(all,owner,name,type);need(((f.getAccessFlags()&8)!=0)==isStatic,"Static field contract "+f);
        System.out.println("FIELD\t"+f+"\tflags="+f.getAccessFlags());
    }
    static Method method(Map<String,ClassDef> all,String id,boolean isStatic){
        Method m=VerifyFrontAbi1931.method(all,id);need((m.getAccessFlags()&1)!=0,"getMethod/getConstructor requires public endpoint "+id);
        need(((m.getAccessFlags()&8)!=0)==isStatic,"Static method contract "+id);
        need((all.get(m.getDefiningClass()).getAccessFlags()&1)!=0,"Public reflection owner "+id);
        System.out.println("METHOD\t"+id+"\tflags="+m.getAccessFlags());return m;
    }
    static String ref(Instruction i){return VerifyFrontAbi1931.ref(i);}
    static List<String> refs(Method m){var out=new ArrayList<String>();for(var i:m.getImplementation().getInstructions()){String r=ref(i);if(!r.isEmpty())out.add(r);}return out;}
    static void storeRegister(Method m,String field,int register){
        int count=0;for(var i:m.getImplementation().getInstructions())if(ref(i).equals(field)){
            need(i.getOpcode().name().startsWith("IPUT"),"Constructor must directly assign field "+field);
            need(i instanceof TwoRegisterInstruction&&((TwoRegisterInstruction)i).getRegisterA()==register,"Constructor parameter order "+field);count++;
        }need(count==1,"One constructor assignment "+field);
    }
    static void verify(Map<String,ClassDef> all){
        field(all,AUTO,"a",CONTROL,false);field(all,AUTO,"b",CALLBACK,false);field(all,AUTO,"c",LISTENER,false);
        field(all,TASK,"k",AUTO,false);field(all,TASK,"c","I",false);field(all,TASK,"j","I",false);
        Method carrier=method(all,TASK+"-><init>("+AUTO+"II)V",false);
        storeRegister(carrier,TASK+"->k:"+AUTO,1);storeRegister(carrier,TASK+"->c:I",2);storeRegister(carrier,TASK+"->j:I",3);
        method(all,CONTROL+"-><init>("+LISTENER+")V",false);
        method(all,NOTIFY+"-><init>("+CONTROL+BITMAP+"IIZ)V",false);
        for(String n:List.of("b","g"))field(all,CONTROL,n,"Ljava/lang/String;",false);
        field(all,CONTROL,"d","J",false);
        method(all,CONTROL+"->r("+BITMAP+"IIZ)Ljava/lang/String;",false);
        method(all,NOTIFY+"->c()V",false);method(all,CALLBACK+"->a()V",false);
        method(all,STORE+"->b()"+STORE,true);method(all,STORE+"->e()"+BITMAP,false);
        method(all,"Li/f/l/n/q/y/n;->f()"+BITMAP,true);
        method(all,"Li/p/a/t/e;->d("+BITMAP+BITMAP+"IDD)"+BITMAP,true);
        field(all,"Li/o/a/p/b;","a","Li/o/a/p/b;",true);method(all,"Li/o/a/p/b;->f()V",false);
        field(all,"Li/o/a/b0/f/a;","a","Li/o/a/b0/f/a;",true);method(all,"Li/o/a/b0/f/a;->d(Z)V",false);
        method(all,"Li/o/a/b1/a/g/y;->D1()I",false);
        // Pending saves budget the configured next capture, including camera or
        // resolution switches, instead of trusting the previous photo's size.
        String yuv="Lcom/hiro/ulike/CaptureYuv;", state="Lcom/hiro/ulike/CaptureYuv$State;";
        field(all,yuv,"active","Ljava/lang/ref/WeakReference;",true);
        field(all,yuv,"states","Ljava/util/Map;",true);
        field(all,state,"ready","Z",false);field(all,state,"closed","Z",false);
        field(all,state,"original","Landroid/media/ImageReader;",false);
        field(all,state,"yuv","Landroid/media/ImageReader;",false);
        field(all,"Li/s/a/w/d0/a;","e0","Landroid/media/ImageReader;",false);
        Method readiness=method(all,yuv+"->isReady()Z",true);
        List<String> readyRefs=refs(readiness);
        need(readyRefs.contains(yuv+"->active:Ljava/lang/ref/WeakReference;")&&readyRefs.contains(yuv+"->states:Ljava/util/Map;"),"Capture readiness must use the same active owner/state map");
        need(readyRefs.contains(state+"->ready:Z"),"Capture readiness must confirm the reflected state is ready");
        for(String name:List.of("ready","closed"))need((VerifyFrontAbi1931.field(all,state,name,"Z").getAccessFlags()&0x40)!=0,"Reader lifecycle state must remain volatile "+name);
        for(String name:List.of("original","yuv"))need((VerifyFrontAbi1931.field(all,state,name,"Landroid/media/ImageReader;").getAccessFlags()&0x10)!=0,"Configured reader identity must remain final "+name);
        method(all,"Lcom/hiro/ulike/PhotoDetail;->snapshot1932()"+SETTINGS,true);
        method(all,"Lcom/hiro/ulike/ShotContext1932;->copy("+BITMAP+BITMAP+")V",true);
        method(all,"Lcom/hiro/ulike/ShotContext1932;->forBitmap("+BITMAP+")Lcom/hiro/ulike/ShotContext1932$Snapshot;",true);
        for(var f:all.get(SETTINGS).getInstanceFields())need((f.getAccessFlags()&0x10)!=0&&(f.getType().equals("I")||f.getType().equals("Z")),"Per-shot settings must remain immutable primitive fields "+f);
        for(String id:List.of(NOTIFY+"->c()V",NOTIFY+"->b()V",CONTROL+"->q(Ljava/lang/String;)V")){
            Method m=VerifyFrontAbi1931.method(all,id);for(String r:refs(m))need(!r.contains("Landroid/graphics/Bitmap;"),"Completion cannot read a recycled Bitmap "+id+" "+r);
        }
        Method n=VerifyFrontAbi1931.method(all,CONTROL+"->n("+BITMAP+"IIZLjava/lang/String;)Z");
        List<String> nr=refs(n);need(Collections.frequency(nr,"Lcom/hiro/ulike/SaveQuality2;->saveFinal("+BITMAP+"Ljava/io/File;Landroid/graphics/Bitmap$CompressFormat;I)Z")==1,"Effective save must route through current final writer");
        for(String r:nr)need(!r.startsWith(STORE)&&!r.startsWith("Li/p/a/t/l;"),"Effective save must not re-read next shot's global EXIF holder "+r);
        Method s=VerifyFrontAbi1931.method(all,CONTROL+"->s("+BITMAP+"IIZZ)Ljava/lang/String;");
        List<String> sr=refs(s);need(sr.contains("Lcom/hiro/ulike/SaveQuality2;->publishFinal(Ljava/lang/String;)Ljava/lang/String;"),"Current writer must synchronously publish before notification");
        need(sr.contains("Lcom/hiro/ulike/SaveQuality2;->savedSize(Ljava/lang/String;)[I"),"Controller dimensions must reflect its own output");
        // Native notification uses manager.a for duration; AsyncSave temporarily
        // binds its private controller before invoking this listener synchronously.
        Method callback=VerifyFrontAbi1931.method(all,"Li/o/a/b1/a/b/f/a$b;->a(ZLjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
        need(refs(callback).contains(AUTO+"->a()J"),"Native receipt duration ownership audit");
        System.out.println("FACT\tnotification_has_no_bitmap_access=true\teffective_global_exif_reads=0\tsettings_immutable=true");
    }
    public static void main(String[] args)throws Exception{
        if(args.length!=1)throw new IllegalArgumentException("APPLIED_APK");checks=0;verify(VerifyFrontAbi1931.load(args[0]));
        System.out.println("PASS save native reflection ABI: checks="+checks+", apk="+new File(args[0]).getName()+". No physical device execution.");
    }
}

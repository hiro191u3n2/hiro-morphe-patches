import hiro.swiftkey.*;
import android.view.MotionEvent;
import android.graphics.RectF;
import android.os.Handler;
import android.view.inputmethod.*;
import com.touchtype.KeyboardService;
import com.touchtype_fluency.service.j0;
import java.util.*;

public final class JapaneseGlideRuntimeTest {
    static int checks;
    static boolean enabled = true;
    static final lh0.a TOKEN = new lh0.a();
    static void check(boolean x,String message){checks++;if(!x)throw new AssertionError(message);}
    static void waitFor(java.util.concurrent.Callable<Boolean> condition)throws Exception{
        for(int i=0;i<2000;i++){Handler.drain();if(condition.call())return;Thread.sleep(1);}
        throw new AssertionError("async timeout");
    }
    static class State implements dc0.a {String text="";int selection;public String s(){return text;}public String G(){return text;}public int k(){return selection;}public int S(){return selection;}}
    static class Model extends wb0.x {
        final State state=new State();final List<String> insertions=new ArrayList<String>();
        Model(){c=new wb0.n0();c.state=state;e=new wb0.z0();e.Y=e.L=true;e.G=k40.l.b;e.c=new ah0.w0();i=new com.microsoft.tokenshare.m();vb0.b mode=new vb0.b();mode.b=vb0.c.NORMAL;i.b=mode;}
        public void P(lh0.a a,String input,String punct,ec0.m m,boolean b,Integer n){check(a==TOKEN,"original token");check(input.equals(punct)&&m==null&&!b&&n==null,"native argument contract");insertions.add(input);state.text+=input;state.selection+=input.length();}
    }
    static class Layout extends fb0.t0 {
        final wb0.x0 keys=new wb0.x0();Layout(){c=new fb0.q0();c.c=qj0.a.x4;keys.b=com.microsoft.fluency.ResultsFilter.PredictionSearchType.JAPANESE;keys.a=new ArrayList<j0>();}
        public wb0.x0 c(){return keys;}
    }
    static class View extends xd0.f0 {
        final Model model=new Model();String surrounding="original";final KeyboardService service;final List<Integer> events=new ArrayList<Integer>();
        View(int kind){super(new KeyboardService());service=(KeyboardService)getContext();service.h=new InputConnection(){public CharSequence getTextBeforeCursor(int n,int flags){return surrounding;}};service.i=new EditorInfo();z=model;j=true;A=new fz.h();I=new android.util.SparseArray();Layout layout=new Layout();a=layout;
            String[] rows={"qwertyuiop","asdfghjkl","zxcvbnm"};float[] offsets={0f,.05f,.15f};
            for(int row=0;row<3;row++)for(int col=0;col<rows[row].length();col++){
                float width=kind==1?.082f:.1f;float left=offsets[row]+col*width;
                if(kind==1)left=.0475f+col*width+offsets[row]*.82f;
                if(kind==2&&col>=rows[row].length()/2)left+=.09f;
                if(kind==2){left*=.90f;width*=.90f;}
                final RectF rect=new RectF(left,row*.20f+.05f,left+width,row*.20f+.25f);
                j0 key=new j0();key.a=new t40.d(){public RectF c(android.graphics.Matrix m){return rect;}};key.b=new z20.c();key.b.b=Arrays.asList(String.valueOf(rows[row].charAt(col)));layout.keys.a.add(key);
            }
        }
        public boolean f(lh0.a token,MotionEvent e){events.add(e.getActionMasked());if(e.getActionMasked()==1){model.state.text+="tap";model.state.selection+=3;}return true;}
        float[] key(char ch){for(Object x:((Layout)a).keys.a){j0 k=(j0)x;if(k.b.b.get(0).equals(String.valueOf(ch))){RectF r=k.a.c(null);return new float[]{r.centerX()*width,r.centerY()*height};}}throw new AssertionError();}
    }
    static long time=10000;
    static MotionEvent event(int action,float x,float y,long down,long now,int tool){MotionEvent e=new MotionEvent(action,x,y,down,now);e.tool=tool;return e;}
    static boolean dispatch(View view,MotionEvent e){boolean consumed=JapaneseGlide.handle(view,TOKEN,e);if(!consumed)view.f(TOKEN,e);return consumed;}
    static long down(View v,char first,int tool){float[] p=v.key(first);long start=time;time+=10;dispatch(v,event(0,p[0],p[1],start,start,tool));return start;}
    static void end(View v,long start,char last,int tool){float[] p=v.key(last);dispatch(v,event(2,p[0],p[1],start,time,tool));time+=10;dispatch(v,event(1,p[0],p[1],start,time,tool));time+=10;}
    static void glide(View v,char first,char last,int tool){long d=down(v,first,tool);end(v,d,last,tool);}
    static void clean()throws Exception{
        JapaneseGlideDecoder.blockDecode=false;JapaneseGlideDecoder.blockLoad=false;JapaneseGlide.reset();
        Thread.sleep(3);Handler.drain();JapaneseGlideDecoder.ready=true;JapaneseGlideDecoder.calls=0;JapaneseGlideDecoder.empty=false;JapaneseGlideDecoder.invalid=false;JapaneseGlideDecoder.loadFail=false;JapaneseGlideDecoder.starts.clear();JapaneseGlideCandidates.shown=0;android.widget.Toast.notices.clear();enabled=true;LineEnter.intercept=false;
        java.lang.reflect.Field retry=JapaneseGlide.class.getDeclaredField("retryAfter");retry.setAccessible(true);retry.setLong(null,0);
    }
    static void candidate()throws Exception{waitFor(()->JapaneseGlideCandidates.listener!=null);}
    static int pending()throws Exception{java.lang.reflect.Field f=JapaneseGlide.class.getDeclaredField("pending");f.setAccessible(true);return((Collection<?>)f.get(null)).size();}
    public static void main(String[] args)throws Exception{
        JapaneseGlide.init((k,d)->enabled);
        for(int layout=0;layout<3;layout++){
            clean();View v=new View(layout);v.model.e.c.timeout=50;glide(v,'k','a',1);candidate();
            check(v.events.equals(Arrays.asList(0,3)),"native DOWN then one CANCEL for layout "+layout);
            check(v.model.insertions.isEmpty(),"no unselected candidate insertion");JapaneseGlideCandidates.select();check(v.model.insertions.equals(Arrays.asList("ka")),"chosen native insertion");
        }
        clean();View stylus=new View(0);glide(stylus,'k','a',2);candidate();check(stylus.events.equals(Arrays.asList(0,3)),"stylus captured");JapaneseGlideCandidates.close();
        clean();View tap=new View(0);long td=down(tap,'a',1);float[] ap=tap.key('a');dispatch(tap,event(1,ap[0],ap[1],td,time,1));check(tap.events.equals(Arrays.asList(0,1)),"ordinary tap unchanged");check(pending()==0,"tap not recognized");
        clean();View slow=new View(0);long sd=down(slow,'k',1);time+=500;end(slow,sd,'a',1);candidate();check(slow.events.equals(Arrays.asList(0,3)),"late movement not blocked by configured timeout");
        clean();View popup=new View(0);long pd=down(popup,'k',1);popup.A.popup=true;end(popup,pd,'a',1);check(popup.events.equals(Arrays.asList(0,2,1)),"actual longpress popup keeps native gesture");check(pending()==0,"popup no glide");
        clean();View fifo=new View(0);JapaneseGlideDecoder.blockDecode=true;glide(fifo,'k','a',1);glide(fifo,'t','o',1);check(pending()==2,"second DOWN preserves first stroke");JapaneseGlideDecoder.blockDecode=false;candidate();JapaneseGlideCandidates.select();candidate();JapaneseGlideCandidates.select();check(fifo.model.insertions.equals(Arrays.asList("ka","ki")),"FIFO candidates inserted in order");check(pending()==0,"queue empty");
        clean();View active=new View(0);glide(active,'k','a',1);candidate();long ad=down(active,'t',1);JapaneseGlideCandidates.select();end(active,ad,'o',1);candidate();JapaneseGlideCandidates.select();check(active.model.insertions.size()==2,"own insertion rebases ongoing second stroke");
        clean();View changed=new View(0);glide(changed,'k','a',1);candidate();changed.model.state.text="external";JapaneseGlideCandidates.select();check(changed.model.insertions.isEmpty()&&pending()==0,"external edit rejects stale candidate");
        clean();View surroundings=new View(0);glide(surroundings,'k','a',1);candidate();surroundings.surrounding="modified";JapaneseGlideCandidates.select();check(surroundings.model.insertions.isEmpty()&&pending()==0,"same-cursor external text change rejects candidate");
        clean();View closeFirst=new View(0);glide(closeFirst,'k','a',1);glide(closeFirst,'t','o',1);candidate();JapaneseGlideCandidates.close();candidate();JapaneseGlideCandidates.select();check(closeFirst.model.insertions.equals(Arrays.asList("ki")),"closing first candidate preserves later queued word");
        clean();View field=new View(0);glide(field,'k','a',1);candidate();field.service.i=new EditorInfo();JapaneseGlideCandidates.select();check(field.model.insertions.isEmpty(),"field identity change rejects candidate");
        clean();View privacy=new View(0);privacy.service.i.imeOptions=0x1000000;glide(privacy,'k','a',1);check(pending()==0,"private editor waits for native private mode");
        clean();View privateReady=new View(0);privateReady.service.i.imeOptions=0x1000000;((vb0.b)privateReady.model.i.b).b=vb0.c.PRIVATE;glide(privateReady,'k','a',1);candidate();JapaneseGlideCandidates.select();check(privateReady.model.insertions.size()==1,"verified native privacy mode allowed");
        clean();View transition=new View(0);glide(transition,'k','a',1);candidate();((vb0.b)transition.model.i.b).b=vb0.c.PRIVATE;JapaneseGlideCandidates.select();check(transition.model.insertions.isEmpty(),"privacy transition discards pending");
        for(int flag:new int[]{129,145,225,0x80001}){clean();View secure=new View(0);secure.service.i.inputType=flag;glide(secure,'k','a',1);check(pending()==0,"secure/no-suggestions excluded "+flag);}
        clean();View loading=new View(0);JapaneseGlideDecoder.ready=false;JapaneseGlideDecoder.blockLoad=true;glide(loading,'k','a',1);check(pending()==1,"first stroke queued while dictionary loads");check(!android.widget.Toast.notices.isEmpty(),"loading status visible");JapaneseGlideDecoder.blockLoad=false;candidate();JapaneseGlideCandidates.select();check(loading.model.insertions.size()==1,"queued loading stroke eventually offered");
        clean();View failure=new View(0);JapaneseGlideDecoder.ready=false;JapaneseGlideDecoder.blockLoad=true;JapaneseGlideDecoder.loadFail=true;glide(failure,'k','a',1);JapaneseGlideDecoder.blockLoad=false;waitFor(()->pending()==0);check(android.widget.Toast.notices.toString().contains("読み込めません"),"load failure visible");
        clean();View limit=new View(0);JapaneseGlideDecoder.blockDecode=true;for(int i=0;i<9;i++)glide(limit,'k','a',1);check(pending()==8,"bounded pending queue");check(android.widget.Toast.notices.toString().contains("先に"),"queue overflow visible");
        clean();View boundary=new View(0);long bd=down(boundary,'q',1);dispatch(boundary,event(2,-10,80,bd,time,1));dispatch(boundary,event(2,600,80,bd,time+10,1));dispatch(boundary,event(1,600,80,bd,time+20,1));candidate();check(boundary.events.equals(Arrays.asList(0,3)),"small edge excursion retained");
        clean();View cancel=new View(0);long cd=down(cancel,'k',1);float[] kp=cancel.key('a');dispatch(cancel,event(2,kp[0],kp[1],cd,time,1));dispatch(cancel,event(3,kp[0],kp[1],cd,time+1,1));check(pending()==0,"canceled owned trace not inserted");check(cancel.events.equals(Arrays.asList(0,3)),"native cancel exactly once");
        clean();View multi=new View(0);long md=down(multi,'k',1);float[] mp=multi.key('a');dispatch(multi,event(2,mp[0],mp[1],md,time,1));MotionEvent second=event(5,mp[0],mp[1],md,time+1,1);second.count=2;dispatch(multi,second);dispatch(multi,event(1,mp[0],mp[1],md,time+2,1));check(multi.events.equals(Arrays.asList(0,3))&&pending()==0,"multitouch owned trace drains without native UP");
        clean();View reset=new View(0);JapaneseGlideDecoder.blockDecode=true;glide(reset,'k','a',1);JapaneseGlide.reset();JapaneseGlideDecoder.blockDecode=false;Thread.sleep(10);Handler.drain();check(JapaneseGlideCandidates.listener==null&&reset.model.insertions.isEmpty(),"reset invalidates worker result");
        clean();View empty=new View(0);JapaneseGlideDecoder.empty=true;glide(empty,'k','a',1);waitFor(()->pending()==0);check(android.widget.Toast.notices.toString().contains("候補が見つかりません"),"unrecognized stroke feedback");
        clean();View invalid=new View(0);JapaneseGlideDecoder.invalid=true;glide(invalid,'k','a',1);waitFor(()->pending()==0);check(JapaneseGlideCandidates.listener==null&&invalid.model.insertions.isEmpty(),"invalid native reading filtered before popup");
        clean();View line=new View(0);LineEnter.intercept=true;glide(line,'k','a',1);check(line.events.isEmpty()&&pending()==0,"LineEnter precedence preserved");LineEnter.intercept=false;
        check(MotionEvent.copies==MotionEvent.recycles,"all native cancellation MotionEvents recycled");
        System.out.println("PASSED "+checks+" production-runtime host assertions; Android/device and native engine are not emulated.");
    }
}

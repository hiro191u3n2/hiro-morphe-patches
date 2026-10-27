package hiro.swiftkey;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;
import android.view.MotionEvent;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.google.android.material.button.MaterialButton;
import com.touchtype.keyboard.view.DeleteKeyButton;
import com.touchtype.ui.SwiftKeyTabLayout;

/** Runs the unchanged production helper against isolated, explicit JVM platform fakes.
 * Checks observable view topology and dispatch, not an imitation of the helper algorithm.
 * It does not execute Android ConstraintLayout, native actor internals, or real touch timers.
 */
public final class EmojiFooterHostTest {
    private static int cases, assertions;
    private static void check(boolean ok,String message){assertions++;if(!ok)throw new AssertionError(message);}
    private static void scenario(){cases++;}
    private static final class Fixture {
        final Context context=new Context();
        final ConstraintLayout parent=new ConstraintLayout(context);
        final MaterialButton abc=new MaterialButton(context);
        final DeleteKeyButton delete=new DeleteKeyButton(context);
        final SwiftKeyTabLayout tabs=new SwiftKeyTabLayout(context);
        final wb0.x sender=new wb0.x();
        final f50.d binding=new f50.d(abc,delete,tabs);
        final ViewGroup.LayoutParams abcParams=new ViewGroup.LayoutParams(60,48);
        final ViewGroup.LayoutParams tabsParams=new ViewGroup.LayoutParams(212,48);
        final ViewGroup.LayoutParams deleteParams=new ViewGroup.LayoutParams(48,48);
        final View.OnClickListener abcListener;
        final View.OnTouchListener deleteListener;
        final Object repeat=delete.nativeRepeatController;
        final Object pager=tabs.nativePager;
        final je0.c owner;
        int abcClicks,deleteTouches;
        Fixture(){this(wa0.p.c);}
        Fixture(gg.a panel){
            abc.setId(0x7f0a0543);delete.setId(0x7f0a054a);tabs.setId(0x7f0a054e);
            abcListener=new View.OnClickListener(){public void onClick(View v){abcClicks++;}};
            deleteListener=new View.OnTouchListener(){public boolean onTouch(View v,MotionEvent e){deleteTouches++;return true;}};
            abc.setOnClickListener(abcListener);delete.setOnTouchListener(deleteListener);
            parent.addView(abc,abcParams);parent.addView(tabs,tabsParams);parent.addView(delete,deleteParams);
            owner=new je0.c(panel,binding);
        }
        void install(){EmojiFooter.install(owner,sender);}
        ViewGroup row(){check(parent.getChildCount()==1,"one wrapper replaces footer children");check(parent.getChildAt(0) instanceof ViewGroup,"wrapper is a ViewGroup");return (ViewGroup)parent.getChildAt(0);}
        View enter(){return row().getChildAt(1);}
        void original(){
            check(parent.getChildCount()==3,"original footer child count");
            check(parent.getChildAt(0)==abc&&parent.getChildAt(1)==tabs&&parent.getChildAt(2)==delete,"original footer child order");
            check(abc.getParent()==parent&&tabs.getParent()==parent&&delete.getParent()==parent,"original parent links");
            check(abc.getLayoutParams()==abcParams&&tabs.getLayoutParams()==tabsParams&&delete.getLayoutParams()==deleteParams,"original layout parameter identity");
            check(abc.clickListener==abcListener&&delete.touchListener==deleteListener,"original event listeners");
        }
    }
    private static void topology(){
        scenario();Fixture f=new Fixture();f.install();ViewGroup row=f.row();
        check(row.getChildCount()==4,"four footer children");
        check(row.getChildAt(0)==f.abc&&row.getChildAt(2)==f.delete&&row.getChildAt(3)==f.tabs,"abc, Enter, native Delete, native tabs order");
        check(f.binding.b==f.abc&&f.binding.c==f.delete&&f.binding.d==f.tabs,"native binding identities retained");
        check(f.abc.clickListener==f.abcListener&&f.delete.touchListener==f.deleteListener,"native listeners retained");
        check(f.delete.nativeRepeatController==f.repeat,"native delete repeat actor retained");
        check(f.tabs.nativePager==f.pager&&f.tabs.selectedTab==7&&f.tabs.scrollX==123&&f.tabs.tabMode==0,"native pager, selected category and scroll state retained");
        check(f.abc.performClick()&&f.abcClicks==1,"abc click still calls native listener once");
        check(f.delete.dispatchTouchEvent(new MotionEvent(MotionEvent.ACTION_DOWN))&&f.delete.dispatchTouchEvent(new MotionEvent(MotionEvent.ACTION_UP))&&f.deleteTouches==2,"native delete touch listener receives existing event stream");
        check(f.sender.enterCalls==0,"installation and other buttons do not emit Enter");
        scenario();View enter=row.getChildAt(1);f.install();
        check(f.row()==row&&row.getChildAt(1)==enter&&row.getChildCount()==4,"repeated installation is idempotent");
        check(f.abc.clickListener==f.abcListener&&f.delete.touchListener==f.deleteListener,"repeated install keeps listeners");
    }
    private static void scopeAndFailures(){
        scenario();Fixture other=new Fixture(new gg.a());other.install();other.original();check(other.sender.enterCalls==0,"non-emoji panel never emits input");
        scenario();EmojiFooter.install(null,new wb0.x());
        scenario();Fixture noSender=new Fixture();EmojiFooter.install(noSender.owner,null);noSender.original();
        scenario();Fixture noBinding=new Fixture();EmojiFooter.install(new je0.c(wa0.p.c,null),noBinding.sender);noBinding.original();
        for(int key=0;key<3;key++){
            scenario();Fixture f=new Fixture();(key==0?f.abc:key==1?f.delete:f.tabs).setId(123);f.install();f.original();
        }
        scenario();Fixture extra=new Fixture();View additional=new View(extra.context);extra.parent.addView(additional,new ViewGroup.LayoutParams(1,1));extra.install();
        check(extra.parent.getChildCount()==4&&extra.parent.getChildAt(3)==additional,"unrecognized footer children remain untouched");
        check(extra.abc.getParent()==extra.parent,"unrecognized footer never partially moved");
        scenario();Fixture split=new Fixture();split.parent.removeView(split.tabs);ConstraintLayout otherParent=new ConstraintLayout(split.context);otherParent.addView(split.tabs,split.tabsParams);split.install();
        check(split.parent.getChildCount()==2&&split.abc.getParent()==split.parent&&split.delete.getParent()==split.parent&&split.tabs.getParent()==otherParent,"split native parents fail closed");
        scenario();Fixture badType=new Fixture();badType.binding.b=new View(badType.context);badType.install();badType.original();
        scenario();Fixture insertion=new Fixture();insertion.parent.failNextAdd=true;insertion.install();insertion.original();
        scenario();Fixture removal=new Fixture();removal.parent.failNextRemove=true;removal.install();removal.original();
    }
    private static void geometry(){
        int[] widths={0,1,4,16,32,64,96,128,160,240,320,393,411,781,1080,1440};
        int[] heights={0,1,24,40,48,64,96};
        for(int direction:new int[]{View.LAYOUT_DIRECTION_LTR,View.LAYOUT_DIRECTION_RTL})for(int width:widths)for(int height:heights){
            scenario();Fixture f=new Fixture();f.parent.setLayoutDirection(direction);f.context.resources.getDisplayMetrics().density=2.5f;f.install();ViewGroup row=f.row();
            row.setLayoutDirection(direction);
            row.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));
            row.layout(0,0,width,height);
            check(row.getMeasuredWidth()==width&&row.getMeasuredHeight()==height,"honor parent exact footer bounds");
            int end=0;
            for(int i=0;i<4;i++){
                View child=row.getChildAt(i);
                check(child.getLeft()>=0&&child.getRight()>=child.getLeft()&&child.getRight()<=width,"child fits within footer width");
                check(child.getTop()>=0&&child.getBottom()>=child.getTop()&&child.getBottom()<=height,"child fits within footer height");
                check(child.getLeft()==end,"physical left-to-right controls are contiguous in LTR and RTL locales");end=child.getRight();
            }
            check(end==width,"category strip receives all remaining horizontal space");
            check(row.getChildAt(1).getWidth()==f.delete.getWidth(),"Enter and Delete have equal hit widths");
            if(width>=160&&height>=24){
                check(f.abc.getWidth()>0&&row.getChildAt(1).getWidth()>0&&f.delete.getWidth()>0&&f.tabs.getWidth()>0,"narrow phone width retains all four touch targets");
                check(f.tabs.getWidth()>=row.getChildAt(1).getWidth(),"at least one key width remains for scrolling categories");
            }
        }
        scenario();Fixture hiddenTabs=new Fixture();hiddenTabs.tabs.setVisibility(View.GONE);hiddenTabs.install();
        check(hiddenTabs.tabs.getVisibility()==View.GONE,"native category visibility retained");
    }
    private static void enterAndTheme(){
        scenario();Fixture f=new Fixture();f.install();View enter=f.enter();
        check(enter.isClickable()&&enter.isFocusable(),"new key accessible and actionable");
        check("android.widget.Button".contentEquals(enter.getAccessibilityClassName()),"button role exposed to accessibility");
        check(enter.getContentDescription()!=null&&enter.getContentDescription().toString().contains("エンター"),"Japanese Enter accessibility label");
        check(enter.performClick()&&f.sender.enterCalls==1&&f.sender.lastBreadcrumb!=null,"one click dispatches exactly one native onEnterKey event");
        lh0.a first=f.sender.lastBreadcrumb;enter.performClick();
        check(f.sender.enterCalls==2&&f.sender.lastBreadcrumb!=first,"each click receives a fresh native event breadcrumb");
        scenario();Fixture failing=new Fixture();failing.sender.throwOnEnter=true;failing.install();
        failing.enter().performClick();check(failing.sender.enterCalls==1,"native actor failure never retries or sends a second fallback newline");
        scenario();Fixture themed=new Fixture();themed.install();View key=themed.enter();key.measure(View.MeasureSpec.makeMeasureSpec(48,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(48,View.MeasureSpec.EXACTLY));key.layout(0,0,48,48);
        Canvas dark=new Canvas();themed.abc.setTextColor(0xffeeeeee);key.draw(dark);
        check(dark.lines>0&&dark.lastColor==0xffeeeeee,"Enter glyph follows native text color");
        Canvas light=new Canvas();themed.abc.setTextColor(0xff161616);key.draw(light);
        check(light.lines>0&&light.lastColor==0xff161616,"Enter glyph follows live theme color update");
    }
    private static void themeLifecycle(){
        scenario();Fixture f=new Fixture();f.install();View enter=f.enter();
        check(enter.getViewTreeObserver().hostListenerCount()==0,"detached footer has no theme observer");
        f.parent.hostAttach();check(enter.getViewTreeObserver().hostListenerCount()==1,"attached footer registers one local theme observer");
        int before=enter.invalidations;f.abc.setTextColor(0xff123456);
        check(enter.getViewTreeObserver().hostDispatch(),"theme observer never blocks window drawing");
        check(enter.invalidations==before+1,"native sibling theme change invalidates cached Enter display list");
        enter.getViewTreeObserver().hostDispatch();check(enter.invalidations==before+1,"unchanged theme does not perpetually invalidate");
        scenario();f.parent.hostDetach();check(enter.getViewTreeObserver().hostListenerCount()==0,"detachment removes local theme observer");
        f.abc.setTextColor(0xff654321);before=enter.invalidations;enter.getViewTreeObserver().hostDispatch();check(enter.invalidations==before,"detached footer receives no theme callbacks");
        scenario();f.parent.hostAttach();check(enter.getViewTreeObserver().hostListenerCount()==1,"reattachment registers exactly one observer");
        before=enter.invalidations;enter.getViewTreeObserver().hostDispatch();check(enter.invalidations>=before,"reattached observer remains operational");
        f.parent.hostDetach();check(enter.getViewTreeObserver().hostListenerCount()==0,"second detach removes observer");
    }
    public static void main(String[] args){topology();scopeAndFailures();geometry();enterAndTheme();themeLifecycle();System.out.println("PASS EmojiFooter scenarios="+cases+" assertions="+assertions);}
}

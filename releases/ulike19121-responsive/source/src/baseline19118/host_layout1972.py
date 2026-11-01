#!/usr/bin/env python3
"""Observe final startup render/control geometry without changing native layout policy."""
import argparse
import importlib.util
import json
import re
import subprocess
from pathlib import Path


TEST = r'''package com.hiro.ulike;
import com.bytedance.corecamera.ui.view.CameraShadeView;
import java.util.*;
public final class Layout1972Test {
 static int checks,textReads;
 static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
 public static class Node {
  public Object parent;public final List<Object> children=new ArrayList<>();
  public int id,left,top,right=1440,bottom=2982,visibility;public float sx=1f,sy=1f,tx,ty,rotation,alpha=1f;
  public boolean clickable,shown=true,attached=true;
  public Node(int id){this.id=id;}
  public Object getParent(){return parent;}public int getId(){return id;}
  public int getLeft(){return left;}public int getTop(){return top;}public int getRight(){return right;}public int getBottom(){return bottom;}
  public int getWidth(){return right-left;}public int getHeight(){return bottom-top;}public int getVisibility(){return visibility;}
  public float getScaleX(){return sx;}public float getScaleY(){return sy;}public float getTranslationX(){return tx;}public float getTranslationY(){return ty;}
  public float getRotation(){return rotation;}public float getAlpha(){return alpha;}public float getPivotX(){return getWidth()/2f;}public float getPivotY(){return getHeight()/2f;}
  public int getChildCount(){return children.size();}public Object getChildAt(int index){return children.get(index);}
  public boolean isClickable(){return clickable;}public boolean isShown(){return shown;}public boolean isAttachedToWindow(){return attached;}
  public String getText(){textReads++;throw new AssertionError("content must never be read");}
  public void add(Node child){children.add(child);child.parent=this;}
 }
 public static class Shade extends CameraShadeView {
  public Object parent,root;
  public Object getParent(){return parent;}public Object getRootView(){return root;}public int getId(){return 101;}
  public int getLeft(){return 0;}public int getTop(){return 0;}public int getRight(){return getWidth();}public int getBottom(){return getHeight();}public int getVisibility(){return 0;}
  public float getScaleX(){return 1f;}public float getScaleY(){return 1f;}public float getTranslationX(){return 0f;}public float getTranslationY(){return 0f;}
  public float getRotation(){return 0f;}public float getAlpha(){return 1f;}public float getPivotX(){return getWidth()/2f;}public float getPivotY(){return getHeight()/2f;}
  public String getText(){textReads++;throw new AssertionError("content must never be read");}
 }
 public static class BadShade extends Shade {public int getLeft(){throw new IllegalStateException("unavailable optional getter");}}
 public static class Render extends android.view.SurfaceView {public Render(int id){super(id);}}
 public static class Texture extends android.view.TextureView {public Texture(int id){super(id);}}
 static Shade attach(Shade shade,Node root){shade.c=1440;shade.j=2982;shade.parent=shade.root=root;root.children.add(0,shade);
  CameraTrace1965.events.clear();PreviewLayout1922.attached(shade);return shade;}
 static int count(String phase){int result=0;for(String e:CameraTrace1965.events)if(e.startsWith(phase+"|"))result++;return result;}
 static boolean has(String phase,String fragment){for(String e:CameraTrace1965.events)if(e.startsWith(phase+"|")&&e.contains(fragment))return true;return false;}
 static void sizes(){for(String e:CameraTrace1965.events)if(e.startsWith("layout_")){
  String fields=e.substring(e.indexOf('|',e.indexOf('|')+1)+1);check(fields.length()<=160,"scalar fields fit the persistent trace limit: "+fields);}}
 static void boundedAndReadOnly(){
  Node root=new Node(1),control=new Node(700);control.clickable=true;root.add(control);for(int i=0;i<80;i++)root.add(new Node(1000+i));
  Shade shade=attach(new Shade(),root);
  check(count("layout_node")==16&&count("layout_probe")==1,"emission retains the 16-node budget");
  check(has("layout_probe","max_scan=128 max_depth=10 truncated=true"),"deeper scalar search remains bounded and reports missing descendants");
  check(has("layout_node","id=101 c=Shade b=0,0,1440,2982 s=1440,2982"),"outer shade geometry is always observed");
  check(count("layout_transform")==0&&count("layout_effect")==0&&count("layout_pivot")==0,"identity transforms add no events");
  int queued=shade.queue.size();for(int i=0;i<50;i++){PreviewLayout1922.afterLayout(shade);PreviewLayout1922.ready(shade);PreviewLayout1922.schedule(shade);}
  check(count("layout_probe")==1&&shade.queue.size()==queued,"repeated notifications cannot renew observations");
  control.right=720;control.bottom=800;control.sy=.5f;control.ty=200f;control.rotation=1f;control.alpha=.75f;
  PreviewLayout1922.prepare(shade);PreviewLayout1922.ready(shade);
  shade.advance(1199);check(count("layout_probe")==1,"final child positions are delayed until the final bounded sample");
  shade.advance(1200);check(count("layout_node")==32&&count("layout_probe")==2,"same attached owner survives an ordinary geometry generation change");
  check(has("layout_node","pass=2 n=2 p=0 id=700 c=Node b=0,0,720,800 s=720,800"),"later child collapse is observable independently of the full-size shade");
  check(has("layout_transform","pass=2 n=2 sx=1.0 sy=0.5 tx=0.0 ty=200.0"),"scale and translation are scalar evidence");
  check(has("layout_effect","pass=2 n=2 rotation=1.0 alpha=0.75")&&has("layout_pivot","pass=2 n=2 x=360.0 y=400.0"),"effect causes retain pivot evidence");
  for(int i=0;i<100;i++)PreviewLayout1922.afterLayout(shade);shade.advance(5000);
  check(count("layout_probe")==2&&shade.queue.isEmpty(),"two completed observations cannot become a persistent sampler");
  check(control.sy==.5f&&control.ty==200f&&control.right==720,"diagnostics leave all child geometry unchanged");
  sizes();PreviewLayout1922.detached(shade);
 }
 static void deepRenderAndControls(){
  Node root=new Node(1),branch=new Node(900);for(int i=0;i<25;i++)root.add(new Node(1000+i));root.add(branch);
  Node parent=branch;for(int i=0;i<6;i++){Node child=new Node(910+i);parent.add(child);parent=child;}
  Render render=new Render(999);Texture texture=new Texture(998);parent.add(render);parent.add(texture);
  Node controls=new Node(800),button=new Node(777);button.clickable=true;button.right=button.bottom=0;controls.add(button);root.add(controls);
  Shade shade=attach(new Shade(),root);
  check(has("layout_node","id=999 c=Render")&&has("layout_node","id=998 c=Texture"),"subclassed SurfaceView and TextureView below depth three are selected");
  check(has("layout_view_state","render=2 clickable=0 shown=1 attached=1 available=-1 surface_valid=1"),"Surface lifecycle evidence is distinct from drawn pixels");
  check(has("layout_view_state","render=1 clickable=0 shown=1 attached=1 available=0 surface_valid=-1"),"unavailable Texture surface is observed truthfully");
  check(has("layout_node","id=777 c=Node b=0,0,0,0 s=0,0"),"visible clickable control is prioritized despite collapsed geometry");
  texture.available=true;render.valid=false;button.right=144;button.bottom=96;branch.ty=170f;
  PreviewLayout1922.prepare(shade);PreviewLayout1922.ready(shade);shade.advance(1200);
  check(has("layout_view_state","pass=2")&&has("layout_view_state","available=1 surface_valid=-1"),"final settled sample records later render availability");
  check(has("layout_view_state","available=-1 surface_valid=0"),"Surface invalidation is not reported as visible success");
  check(has("layout_node","id=777 c=Node b=0,0,144,96 s=144,96"),"final sample records measured shutter/control extent");
  check(branch.ty==170f&&button.right==144&&button.bottom==96,"render/control priorities do not modify layout");
  sizes();PreviewLayout1922.detached(shade);
 }
 static void ownershipAndFailure(){
  Shade shade=attach(new Shade(),new Node(1));ManualLens170.epoch++;shade.advance(1200);
  check(count("layout_probe")==1&&has("layout_probe_stop","reason=camera_epoch_changed"),"foreground camera epoch replacement rejects the delayed sample explicitly");
  check(shade.queue.isEmpty(),"stopped sample is not rescheduled");PreviewLayout1922.detached(shade);
  shade=attach(new Shade(),new Node(2));PreviewLayout1922.detached(shade);int before=count("layout_probe");shade.advance(2000);
  check(count("layout_probe")==before&&shade.queue.isEmpty(),"detach removes queued diagnostics");
  shade.attached=true;PreviewLayout1922.attached(shade);check(count("layout_probe")==before+1,"same-object new attachment gets its own bounded sample");
  shade.advance(3200);check(count("layout_probe")==before+2,"reattachment permits one fresh final sample");PreviewLayout1922.detached(shade);
  Shade old=attach(new Shade(),new Node(3));Shade current=attach(new Shade(),new Node(4));before=count("layout_probe");old.advance(2000);
  check(count("layout_probe")==before&&old.queue.isEmpty(),"view replacement cannot report stale child observations");PreviewLayout1922.detached(current);
  BadShade bad=new BadShade();attach(bad,new Node(2));check(bad.notifications>0&&PreviewLayout1922.viewport()!=null,"optional getter failures cannot disrupt native layout notification");
  check(has("layout_probe","unavailable=InvocationTargetException"),"failure is reported as unavailable geometry");bad.advance(1200);
  check(count("layout_probe")==1&&bad.queue.isEmpty(),"unavailable optional geometry does not queue ongoing diagnostic work");PreviewLayout1922.detached(bad);
 }
 static void depthBudgetAndSizes(){
  Node root=new Node(1),parent=root;for(int i=0;i<20;i++){Node child=new Node(10+i);parent.add(child);parent=child;}parent.add(new Render(999));
  Shade shade=attach(new Shade(),root);check(has("layout_probe","max_depth=10 truncated=true")&&!has("layout_node","id=999"),"deep search never passes the hard depth budget");PreviewLayout1922.detached(shade);
  root=new Node(1);for(int i=0;i<31;i++){Node b=new Node(100+i);b.clickable=true;b.sx=.5f;b.tx=5f;b.rotation=1f;b.alpha=.5f;root.add(b);}
  shade=attach(new Shade(),root);int records=0;for(String e:CameraTrace1965.events)if(e.startsWith("layout_node|")||e.startsWith("layout_view_state|")||e.startsWith("layout_transform|")||e.startsWith("layout_effect|")||e.startsWith("layout_pivot|")||e.startsWith("layout_probe|"))records++;
  check(records<=64,"extra render/control lifecycle state remains inside the original per-sample event budget");PreviewLayout1922.detached(shade);
  Node extremes=new Node(Integer.MIN_VALUE);extremes.left=extremes.top=extremes.right=extremes.bottom=Integer.MIN_VALUE;extremes.visibility=Integer.MIN_VALUE;
  extremes.sx=-Float.MIN_NORMAL;extremes.sy=Float.MIN_VALUE;extremes.tx=-Float.MAX_VALUE;extremes.ty=Float.MAX_VALUE;
  shade=attach(new Shade(),extremes);sizes();check(textReads==0,"all tests query zero view content or labels");PreviewLayout1922.detached(shade);
 }
 public static void main(String[] args){boundedAndReadOnly();deepRenderAndControls();ownershipAndFailure();depthBudgetAndSizes();System.out.println("LAYOUT1972_ASSERTIONS="+checks);}
}'''

SURFACE = r'''package android.view;
public class SurfaceView extends com.hiro.ulike.Layout1972Test.Node {
 public boolean valid=true;public SurfaceView(int id){super(id);}
 public SurfaceHolder getHolder(){return new Holder(this);}
 private static final class Holder implements SurfaceHolder {final SurfaceView view;Holder(SurfaceView v){view=v;}public Surface getSurface(){return new Surface(view);}}
 public static final class Surface {final SurfaceView view;Surface(SurfaceView v){view=v;}public boolean isValid(){return view.valid;}}
}'''
HOLDER = r'''package android.view;
public interface SurfaceHolder {Object getSurface();}
'''
TEXTURE = r'''package android.view;
public class TextureView extends com.hiro.ulike.Layout1972Test.Node {
 public boolean available;public TextureView(int id){super(id);}public boolean isAvailable(){return available;}
}'''



def test(root, work, jdk=None):
    root, work = Path(root).resolve(), Path(work).resolve()
    spec = importlib.util.spec_from_file_location('layout1972_compile', root / 'tests1963/ui_audit1963.py')
    runner = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(runner)
    count = runner.compile_run(root, work, 'layout1972', ['PreviewLayout1922.java'],
        ['layout1937-host'], {'com/hiro/ulike/Layout1972Test.java': TEST,'android/view/SurfaceView.java': SURFACE,'android/view/TextureView.java': TEXTURE,'android/view/SurfaceHolder.java': HOLDER},
        'com.hiro.ulike.Layout1972Test', jdk)
    java = str(Path(jdk) / 'bin/java') if jdk else 'java'
    preserved = subprocess.run([java, '-cp', str(work / 'layout1972/classes'),
        'com.hiro.ulike.LayoutHost1937'], capture_output=True, text=True, timeout=60)
    (work / 'layout-policy1972.log').write_text(preserved.stdout + preserved.stderr)
    if preserved.returncode:
        raise RuntimeError(preserved.stdout + preserved.stderr)
    match = re.search(r'HOST_LAYOUT1937_ASSERTIONS=(\d+)', preserved.stdout)
    if not match:
        raise RuntimeError('Existing layout policy suite did not complete')
    policy_count = int(match.group(1))
    report = {'status': 'passed', 'assertions': count + policy_count,
        'groups': {'bounded_scalar_diagnostic': {'status': 'passed', 'assertions': count},
            'preserved_layout_policy': {'status': 'passed', 'assertions': policy_count}},
        'scope': 'actual production helper; bounded deep scalar hierarchy, rendering lifecycle, attachment/epoch callbacks; layout policy unchanged',
        'physical_android_tested': False}
    (work / 'host-layout1972.json').write_text(json.dumps(report, indent=2) + '\n')
    return report


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--root', type=Path, default=Path(__file__).resolve().parent)
    parser.add_argument('--work', type=Path, required=True)
    parser.add_argument('--jdk', type=Path)
    args = parser.parse_args()
    print(json.dumps(test(args.root, args.work, args.jdk), indent=2))

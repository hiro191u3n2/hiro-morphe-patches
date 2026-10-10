#!/usr/bin/env python3
"""Bound the optional production layout observations and preserve native layout policy."""
import argparse
import importlib.util
import json
import re
import subprocess
from pathlib import Path


TEST = r'''package com.hiro.ulike;
import com.bytedance.corecamera.ui.view.CameraShadeView;
import java.util.*;
public final class Layout1968Test {
 static int checks,textReads;
 static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
 public static class Node {
  public Object parent;public final List<Object> children=new ArrayList<>();
  public int id,left,top,right=1440,bottom=2982,visibility;public float sx=1f,sy=1f,tx,ty,rotation,alpha=1f;
  public Node(int id){this.id=id;}
  public Object getParent(){return parent;}public int getId(){return id;}
  public int getLeft(){return left;}public int getTop(){return top;}public int getRight(){return right;}public int getBottom(){return bottom;}
  public int getWidth(){return right-left;}public int getHeight(){return bottom-top;}public int getVisibility(){return visibility;}
  public float getScaleX(){return sx;}public float getScaleY(){return sy;}public float getTranslationX(){return tx;}public float getTranslationY(){return ty;}
  public float getRotation(){return rotation;}public float getAlpha(){return alpha;}public float getPivotX(){return getWidth()/2f;}public float getPivotY(){return getHeight()/2f;}
  public int getChildCount(){return children.size();}public Object getChildAt(int index){return children.get(index);}
  public String getText(){textReads++;throw new AssertionError("content must never be read");}
  public void add(Node child){children.add(child);child.parent=this;}
 }
 public static class Shade extends CameraShadeView {
  public Object parent;
  public Object getParent(){return parent;}public int getId(){return 101;}
  public int getLeft(){return 0;}public int getTop(){return 0;}public int getRight(){return getWidth();}public int getBottom(){return getHeight();}public int getVisibility(){return 0;}
  public float getScaleX(){return 1f;}public float getScaleY(){return 1f;}public float getTranslationX(){return 0f;}public float getTranslationY(){return 0f;}
  public float getRotation(){return 0f;}public float getAlpha(){return 1f;}public float getPivotX(){return getWidth()/2f;}public float getPivotY(){return getHeight()/2f;}
  public String getText(){textReads++;throw new AssertionError("content must never be read");}
 }
 public static class BadShade extends Shade {public int getLeft(){throw new IllegalStateException("unavailable optional getter");}}
 static Shade attach(Shade shade,Node root){shade.c=1440;shade.j=2982;shade.parent=root;root.children.add(0,shade);
  CameraTrace1965.events.clear();PreviewLayout1922.attached(shade);return shade;}
 static int count(String phase){int result=0;for(String e:CameraTrace1965.events)if(e.startsWith(phase+"|"))result++;return result;}
 static boolean has(String phase,String fragment){for(String e:CameraTrace1965.events)if(e.startsWith(phase+"|")&&e.contains(fragment))return true;return false;}
 static void sizes(){for(String e:CameraTrace1965.events)if(e.startsWith("layout_node|")||e.startsWith("layout_transform|")||e.startsWith("layout_effect|")||e.startsWith("layout_pivot|")||e.startsWith("layout_probe|")){
  String fields=e.substring(e.indexOf('|',e.indexOf('|')+1)+1);check(fields.length()<=160,"all diagnostic scalar fields fit the persistent trace limit");}}
 static void boundedSnapshots(){
  Node root=new Node(1),control=new Node(700);root.add(control);for(int i=0;i<40;i++)root.add(new Node(1000+i));
  Shade shade=attach(new Shade(),root);
  check(count("layout_node")==16&&count("layout_probe")==1,"a large native container is capped at 16 nodes");
  check(has("layout_probe","truncated=true"),"bounded traversal reports missing descendants");
  check(has("layout_node","id=101 c=Shade b=0,0,1440,2982 s=1440,2982"),"local shade position and measured extent are recorded");
  check(count("layout_transform")==0&&count("layout_effect")==0&&count("layout_pivot")==0,"default transforms do not add events");
  int queued=shade.queue.size();for(int i=0;i<50;i++){PreviewLayout1922.afterLayout(shade);PreviewLayout1922.ready(shade);PreviewLayout1922.schedule(shade);}
  check(count("layout_probe")==1&&shade.queue.size()==queued,"repeated layout and frame notices cannot renew diagnostic work");
  control.right=720;control.bottom=800;control.sy=.5f;control.ty=200f;control.rotation=1f;control.alpha=.75f;
  shade.advance(299);check(count("layout_probe")==1,"the follow-up is delayed instead of a synchronous traversal loop");
  shade.advance(300);check(count("layout_node")==32&&count("layout_probe")==2,"one follow-up records the later native child geometry");
  check(has("layout_node","pass=2 n=2 p=0 id=700 c=Node b=0,0,720,800 s=720,800"),"child collapse can be distinguished from normal outer shade dimensions");
  check(has("layout_transform","pass=2 n=2 sx=1.0 sy=0.5 tx=0.0 ty=200.0"),"scale and translation are observable");
  check(has("layout_effect","pass=2 n=2 rotation=1.0 alpha=0.75")&&has("layout_pivot","pass=2 n=2 x=360.0 y=400.0"),"other scalar transform causes retain the pivot");
  for(int i=0;i<100;i++)PreviewLayout1922.afterLayout(shade);shade.advance(5000);
  check(count("layout_probe")==2&&shade.queue.isEmpty(),"completed observations cannot become a persistent sampler");
  check(control.sy==.5f&&control.ty==200f&&control.right==720,"observations do not repair speculative child geometry");
  check(textReads==0,"no view content or labels are queried");sizes();PreviewLayout1922.detached(shade);
 }
 static void staleAndTeardown(){
  Shade shade=attach(new Shade(),new Node(1));check(count("layout_probe")==1,"initial current attachment is observed");
  PreviewLayout1922.prepare(shade);PreviewLayout1922.ready(shade);shade.advance(1000);
  check(count("layout_probe")==1&&shade.queue.isEmpty(),"a changed target rejects the old delayed generation without renewing it");
  shade=attach(new Shade(),new Node(2));PreviewLayout1922.detached(shade);int before=count("layout_probe");shade.advance(1000);
  check(count("layout_probe")==before&&shade.queue.isEmpty(),"detach removes the pending optional callback");
  shade.attached=true;PreviewLayout1922.attached(shade);check(count("layout_probe")==before+1,"same-object reattachment gets one fresh bounded observation");
  shade.advance(1300);check(count("layout_probe")==before+2,"new attachment permits its own single follow-up");PreviewLayout1922.detached(shade);
  Shade old=attach(new Shade(),new Node(3));Shade current=attach(new Shade(),new Node(4));before=count("layout_probe");old.advance(1000);
  check(count("layout_probe")==before&&old.queue.isEmpty(),"old shade replacement cannot report into the new attachment");PreviewLayout1922.detached(current);
 }
 static void depthAndFailures(){
  Node root=new Node(1),parent=root;for(int i=0;i<8;i++){Node next=new Node(10+i);parent.add(next);parent=next;}
  Shade shade=attach(new Shade(),root);check(count("layout_node")==5&&has("layout_probe","truncated=true"),"deep native hierarchies stop at depth three");PreviewLayout1922.detached(shade);
  BadShade bad=new BadShade();attach(bad,new Node(2));check(bad.notifications>0&&PreviewLayout1922.viewport()!=null,"an optional getter failure cannot disrupt native layout notification");
  check(has("layout_probe","unavailable=InvocationTargetException"),"optional API failure is recorded as unavailable, never as valid geometry");PreviewLayout1922.detached(bad);
  Node extremes=new Node(Integer.MIN_VALUE);extremes.left=extremes.top=extremes.right=extremes.bottom=Integer.MIN_VALUE;extremes.visibility=Integer.MIN_VALUE;
  extremes.sx=-Float.MIN_NORMAL;extremes.sy=Float.MIN_VALUE;extremes.tx=-Float.MAX_VALUE;extremes.ty=Float.MAX_VALUE;
  shade=attach(new Shade(),extremes);sizes();check(textReads==0,"all failure and extreme cases still avoid view content");PreviewLayout1922.detached(shade);
 }
 public static void main(String[] args){boundedSnapshots();staleAndTeardown();depthAndFailures();System.out.println("LAYOUT1968_ASSERTIONS="+checks);}
}'''


def test(root, work, jdk=None):
    root, work = Path(root).resolve(), Path(work).resolve()
    spec = importlib.util.spec_from_file_location('layout1968_compile', root / 'tests1963/ui_audit1963.py')
    runner = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(runner)
    count = runner.compile_run(root, work, 'layout1968', ['PreviewLayout1922.java'],
        ['layout1937-host'], {'com/hiro/ulike/Layout1968Test.java': TEST},
        'com.hiro.ulike.Layout1968Test', jdk)
    java = str(Path(jdk) / 'bin/java') if jdk else 'java'
    preserved = subprocess.run([java, '-cp', str(work / 'layout1968/classes'),
        'com.hiro.ulike.LayoutHost1937'], capture_output=True, text=True, timeout=60)
    (work / 'layout-policy1968.log').write_text(preserved.stdout + preserved.stderr)
    if preserved.returncode:
        raise RuntimeError(preserved.stdout + preserved.stderr)
    match = re.search(r'HOST_LAYOUT1937_ASSERTIONS=(\d+)', preserved.stdout)
    if not match:
        raise RuntimeError('Existing layout policy suite did not complete')
    policy_count = int(match.group(1))
    report = {'status': 'passed', 'assertions': count + policy_count,
        'groups': {'bounded_scalar_diagnostic': {'status': 'passed', 'assertions': count},
            'preserved_layout_policy': {'status': 'passed', 'assertions': policy_count}},
        'scope': 'actual production helper; bounded view hierarchy and modeled UI callbacks',
        'physical_android_tested': False}
    (work / 'host-layout1968.json').write_text(json.dumps(report, indent=2) + '\n')
    return report


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--root', type=Path, default=Path(__file__).resolve().parent)
    parser.add_argument('--work', type=Path, required=True)
    parser.add_argument('--jdk', type=Path)
    args = parser.parse_args()
    print(json.dumps(test(args.root, args.work, args.jdk), indent=2))

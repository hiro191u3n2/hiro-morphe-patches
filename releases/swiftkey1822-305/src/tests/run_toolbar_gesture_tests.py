#!/usr/bin/env python3
"""Run production emoji graph edits against explicit native-boundary host fakes.

The small event harness models the pinned oc0.p/oc0.b interaction: LONGPRESS
marks a press as held before its actor; a held press suppresses CLICK; CANCEL
and slide-out invalidate the pending timer; LONGCLICK runs only on a held UP.
This is not an Android device or ART execution test.
"""
import argparse
import hashlib
import json
import shutil
import subprocess
from pathlib import Path

SOURCES = {
    'com/touchtype/KeyboardService.java': '''package com.touchtype;
public class KeyboardService { public boolean available=true,open,access; public int toggles,closes; public boolean fail; }
''',
    'hiro/swiftkey/CandidateToolbar.java': '''package hiro.swiftkey;
public final class CandidateToolbar {
 public static void setEmojiAccess(com.touchtype.KeyboardService s,boolean value){s.access=value;}
 public static boolean toggle(com.touchtype.KeyboardService s){
  if(s.fail)throw new IllegalStateException("controller unavailable");
  if(!s.available)return false;s.open=!s.open;s.toggles++;return true;
 }
 public static boolean closeIfOpen(com.touchtype.KeyboardService s){
  if(s.fail)throw new IllegalStateException("controller unavailable");
  if(!s.available||!s.open)return false;s.open=false;s.closes++;return true;
 }
}
''',
    'cm0/d.java': 'package cm0; public interface d { Object invoke(Object value); }',
    'lh0/a.java': 'package lh0; public class a {}',
    'fc0/k1.java': 'package fc0; public final class k1 { public com.touchtype.KeyboardService a; public int w; }',
    'fc0/h.java': 'package fc0; public final class h { public int a; public k1 b; }',
    'bf0/g.java': '''package bf0; public final class g {
 public Object a=new java.util.ArrayList(),b=new java.util.ArrayList(),c=new java.util.ArrayList(),
 d=new java.util.ArrayList(),e=new java.util.ArrayList(),f=new java.util.ArrayList(),g=new java.util.ArrayList();
}''',
    'hc0/b.java': '''package hc0; public final class b {
 public bf0.g a=new bf0.g(); public java.util.List d,e; public k f;
}''',
    'hc0/k.java': '''package hc0; public final class k {
 public final int a; public final java.util.ArrayList b=new java.util.ArrayList(),c=new java.util.ArrayList(),d=new java.util.ArrayList();
 public k(int timeout){a=timeout;}
}''',
    'hc0/o.java': '''package hc0; public final class o {
 public final cm0.d a;public final Object b; public o(cm0.d restriction,Object actor){a=restriction;b=actor;}
}''',
    'ic0/a.java': 'package ic0; public interface a { void b(lh0.a value); }',
    'ic0/e.java': 'package ic0; public final class e { public final a a;public e(a value){a=value;} }',
    'oz/i6.java': 'package oz; public enum i6 { b, other }',
    'ic0/a0.java': '''package ic0; public final class a0 implements a {
 public oz.i6 a=oz.i6.b;public int calls;public lh0.a last;public boolean fail;
 public void b(lh0.a value){if(fail)throw new IllegalArgumentException("native failure");calls++;last=value;}
}''',
    'ToolbarGestureHostTest.java': r'''import hiro.swiftkey.EmojiToolbar;
import com.touchtype.KeyboardService;
import java.util.ArrayList;
import java.util.List;

public final class ToolbarGestureHostTest {
 static int assertions,scenarios;
 static final cm0.d ALWAYS=new cm0.d(){public Object invoke(Object value){return Boolean.TRUE;}};
 static final cm0.d NEVER=new cm0.d(){public Object invoke(Object value){return Boolean.FALSE;}};
 static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
 static void execute(Object raw,lh0.a token){
  if(raw==null)return;
  for(Object value:(List)raw){hc0.o entry=(hc0.o)value;
   if(!Boolean.TRUE.equals(entry.a.invoke(null)))continue;
   Object action=entry.b;if(action instanceof ic0.e)action=((ic0.e)action).a;
   ((ic0.a)action).b(token);
  }
 }
 static final class Counter implements ic0.a {int calls;public void b(lh0.a ignored){calls++;}}
 static final class Key {
  final KeyboardService service;final fc0.h factory=new fc0.h();final hc0.b builder=new hc0.b();
  final ic0.a0 emoji=new ic0.a0();final Counter reset=new Counter();final lh0.a token=new lh0.a();
  final boolean accessible;boolean pressed,inside,held;int elapsed;
  Key(KeyboardService service,boolean accessible,int variant,int timeout){
   this.service=service;this.accessible=accessible;factory.a=variant;factory.b=new fc0.k1();
   factory.b.a=service;factory.b.w=timeout;
   ((List)builder.a.a).add(new hc0.o(ALWAYS,new ic0.e(reset)));
   if(accessible){
    ((List)builder.a.f).add(new hc0.o(ALWAYS,reset));
    ((List)builder.a.f).add(new hc0.o(ALWAYS,emoji));
    builder.e=new ArrayList();builder.e.add(new hc0.o(ALWAYS,new ic0.e(emoji)));
   }else{
    builder.d=new ArrayList();builder.d.add(new hc0.o(ALWAYS,new ic0.e(reset)));
    builder.d.add(new hc0.o(ALWAYS,new ic0.e(emoji)));
   }
  }
  Key(){this(new KeyboardService(),false,0,450);}
  void install(){EmojiToolbar.configure(factory,builder);}
  void down(){pressed=inside=true;held=false;elapsed=0;execute(builder.a.a,token);}
  void time(int ms){
   elapsed+=ms;if(pressed&&inside&&!held&&builder.f!=null&&elapsed>=builder.f.a){
    held=true;execute(builder.f.b,token);
   }
  }
  void up(){
   if(pressed&&inside){if(held)execute(builder.f.d,token);else execute(builder.d,token);}
   pressed=false;
  }
  void cancel(){pressed=inside=false;held=false;}
  void slideOut(){inside=false;}
  void accessibilityClick(){execute(builder.a.f,token);}
 }
 public static void main(String[] args){
  Key k=new Key();Object down=k.builder.a.a;hc0.o sentinel=(hc0.o)((List)down).get(0);
  k.install();check(k.service.access,"Dedicated emoji access not registered");
  check(k.builder.f!=null&&k.builder.f.a==450,"Native configured timeout not retained");
  check(k.builder.f.b.size()==1&&k.builder.f.d.size()==1&&k.builder.f.c.isEmpty(),"Wrong native longpress paths");
  check(k.builder.a.a==down&&((List)down).get(0)==sentinel,"Unrelated native DOWN actions replaced");
  k.down();k.time(449);k.up();check(k.emoji.calls==1&&k.emoji.last==k.token,"Normal emoji tap lost");
  check(k.service.toggles==0&&k.service.closes==0,"Normal tap unexpectedly toggled toolbar");scenarios++;

  k=new Key();k.install();k.down();k.time(449);
  check(!k.service.open&&k.emoji.calls==0,"Longpress triggered before threshold");
  k.time(1);check(k.service.open&&k.service.toggles==1&&k.emoji.calls==0,"Longpress must open menu only");
  k.time(5000);k.up();check(k.service.open&&k.service.toggles==1&&k.emoji.calls==0,"Held UP opened emoji or closed menu");
  k.down();k.time(100);k.up();check(!k.service.open&&k.service.closes==1&&k.emoji.calls==0,"Open-menu tap did not close only");
  k.down();k.up();check(k.emoji.calls==1,"Emoji action did not recover after closing menu");scenarios++;

  k=new Key();k.service.open=true;k.install();k.down();k.time(450);k.up();
  check(!k.service.open&&k.service.toggles==1&&k.emoji.calls==0,"Longpress while open should close menu once");scenarios++;

  k=new Key();k.install();hc0.k first=k.builder.f;Object click=k.builder.d;
  EmojiToolbar.configure(k.factory,k.builder);
  check(k.builder.f==first&&k.builder.d==click,"Repeated configure changed installed graph");
  k.down();k.time(450);k.up();check(k.service.toggles==1,"Repeated configure doubled action");scenarios++;

  Key same=k;Key combined=new Key(same.service,false,1,450);combined.install();
  check(!same.service.access,"Combined key did not disable existing access gate");
  same.install();check(same.service.access&&same.builder.f==first,"Reused dedicated key failed to restore access gate");scenarios++;

  k=new Key(new KeyboardService(),false,1,450);Object before=k.builder.d;
  k.service.access=true;k.install();check(k.builder.f==null&&k.builder.d==before&&!k.service.access,"Combined mic/emoji variant was modified or menu access hidden");
  k.down();k.up();check(k.emoji.calls==1,"Combined key native emoji action lost");scenarios++;

  k=new Key();k.install();k.down();k.time(449);k.cancel();k.time(1000);k.up();
  check(k.service.toggles==0&&k.emoji.calls==0,"CANCEL leaked hold or click");scenarios++;
  k=new Key();k.install();k.down();k.time(449);k.slideOut();k.time(1000);k.up();
  check(k.service.toggles==0&&k.emoji.calls==0,"Slide-out leaked hold or click");scenarios++;

  k=new Key();k.service.available=false;k.install();k.down();k.time(450);
  check(k.emoji.calls==0,"Unavailable-controller fallback happened before release");
  k.up();check(k.emoji.calls==1&&k.service.toggles==0,"Unavailable-controller hold fallback lost original emoji");
  execute(k.builder.f.d,k.token);check(k.emoji.calls==1,"Longrelease fallback fired twice");scenarios++;
  k=new Key();k.service.available=false;k.install();k.down();k.time(450);k.cancel();k.up();
  check(k.emoji.calls==0,"Cancelled unavailable hold opened emoji");
  k.service.available=true;k.down();k.time(450);k.up();
  check(k.service.open&&k.emoji.calls==0,"Cancelled fallback survived into next successful hold");scenarios++;

  k=new Key();k.service.available=false;k.install();k.down();k.up();
  check(k.emoji.calls==1,"Unavailable-controller short tap was swallowed");scenarios++;
  k=new Key();k.service.fail=true;k.install();k.down();k.time(450);k.up();
  check(k.emoji.calls==1,"Controller exception lost long-release fallback");scenarios++;

  k=new Key(new KeyboardService(),true,0,615);k.install();
  k.accessibilityClick();check(k.emoji.calls==1,"Accessibility click native emoji lost");
  k.service.open=true;k.accessibilityClick();check(!k.service.open&&k.emoji.calls==1,"Accessibility close also opened emoji");
  check(k.builder.f.a==615,"Accessibility native timeout changed");scenarios++;

  Key other=new Key();other.install();k=new Key();k.install();other.service.open=true;
  k.down();k.up();check(k.emoji.calls==1&&other.service.open,"Different service toolbar intercepted this key");
  other.down();other.up();check(!other.service.open&&other.emoji.calls==0,"Service-specific close not applied");scenarios++;

  k=new Key();k.factory.b.w=0;before=k.builder.d;k.service.access=true;k.install();
  check(k.builder.f==null&&before==k.builder.d&&!k.service.access,"Invalid timeout partially modified graph");scenarios++;
  k=new Key();k.builder.f=new hc0.k(900);first=k.builder.f;before=k.builder.d;k.install();
  check(k.builder.f==first&&k.builder.d==before&&!k.service.access,"Preexisting native longpress overwritten");scenarios++;

  k=new Key();k.emoji.a=oz.i6.other;before=k.builder.d;k.install();
  check(k.builder.f==null&&k.builder.d==before&&!k.service.access,"Unrecognized feature graph was rewritten");scenarios++;
  k=new Key();((List)k.builder.a.f).add(new Object());before=k.builder.d;k.install();
  check(k.builder.f==null&&k.builder.d==before&&!k.service.access,"Malformed graph partially modified other list");scenarios++;
  k=new Key();k.builder.d=java.util.Collections.unmodifiableList(k.builder.d);before=k.builder.d;k.install();
  check(k.builder.f==null&&k.builder.d==before,"Unexpected immutable list accepted");scenarios++;

  k=new Key();hc0.o nativeEntry=(hc0.o)k.builder.d.get(1);
  k.builder.d.set(1,new hc0.o(NEVER,nativeEntry.b));k.install();k.down();k.up();
  check(k.emoji.calls==0,"Native action restriction discarded");scenarios++;
  k=new Key();k.install();k.emoji.fail=true;k.down();boolean failed=false;
  try{k.up();}catch(IllegalArgumentException expected){failed=true;}
  check(failed,"Original native action exception was hidden");scenarios++;
  EmojiToolbar.configure(null,null);EmojiToolbar.configure(new fc0.h(),new Object());
  check(true,"Null boundaries");scenarios++;

  System.out.println("PASS EmojiToolbar "+scenarios+" scenarios, "+assertions+" assertions");
  System.out.println("REPORT {\"status\":\"PASSED\",\"scenarios\":"+scenarios+",\"assertions\":"+assertions+"}");
 }
}''',
}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--source-dir', type=Path, required=True)
    parser.add_argument('--work', type=Path, required=True)
    args = parser.parse_args()
    root, work = args.source_dir.resolve(), args.work.resolve()
    production = root / 'src/hiro/swiftkey/EmojiToolbar.java'
    work.mkdir(parents=True, exist_ok=True)
    source, classes = work / 'generated', work / 'classes'
    for directory in (source, classes):
        if directory.exists():
            shutil.rmtree(directory)
        directory.mkdir()
    for name, content in SOURCES.items():
        path = source / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content + '\n')
    output = ''
    for name, command in [
        ('compile', ['java', 'com.sun.tools.javac.Main', '--release', '8', '-encoding', 'UTF-8', '-g:none',
                     '-d', str(classes), str(production), *map(str, sorted(source.rglob('*.java')))]),
        ('gesture', ['java', '-ea', '-cp', str(classes), 'ToolbarGestureHostTest']),
    ]:
        result = subprocess.run(command, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        (work / (name + '.log')).write_text(result.stdout)
        print(result.stdout, end='', flush=True)
        if result.returncode:
            raise SystemExit(result.returncode)
        output = result.stdout
    report = json.loads(next(line[7:] for line in output.splitlines() if line.startswith('REPORT ')))
    report.update(
        production_source_sha256=hashlib.sha256(production.read_bytes()).hexdigest(),
        device_tested=False,
        android_instrumentation_tested=False,
        scope='Production EmojiToolbar graph edits and action delegates; explicit host controller and pinned-native timer/cancel boundary model, not ART/device execution',
        configured_native_timeout_preserved=True,
        native_touch_ownership_preserved=True,
        original_emoji_action_preserved_when_closed=True,
        toolbar_short_press_closes_without_emoji=True,
        successful_long_press_release_does_not_open_emoji=True,
        unavailable_controller_long_release_falls_back_to_original=True,
        combined_microphone_key_preserved=True,
        malformed_graph_unchanged=True,
    )
    (work / 'report.json').write_text(json.dumps(report, ensure_ascii=False, sort_keys=True, indent=2) + '\n')


if __name__ == '__main__':
    main()

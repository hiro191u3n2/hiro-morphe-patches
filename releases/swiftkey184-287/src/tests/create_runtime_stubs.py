from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
classes = {
'com/microsoft/fluency/Japanese.java':'package com.microsoft.fluency; public class Japanese { public static String romajiToHiragana(String s) { return s; } }',
'com/microsoft/fluency/ResultsFilter.java':'package com.microsoft.fluency; public class ResultsFilter { public enum PredictionSearchType { JAPANESE, OTHER } }',
'com/touchtype/KeyboardService.java':'package com.touchtype; public class KeyboardService extends android.inputmethodservice.InputMethodService { public android.view.inputmethod.InputConnection h; public android.view.inputmethod.EditorInfo i; }',
'com/touchtype_fluency/service/j0.java':'package com.touchtype_fluency.service; public class j0 { public t40.d a; public z20.c b; }',
'com/microsoft/tokenshare/m.java':'package com.microsoft.tokenshare; public class m { public Object b; }',
'z20/c.java':'package z20; public class c { public java.util.List b; }',
'ec0/m.java':'package ec0; public class m { }',
'lh0/a.java':'package lh0; public class a { }',
'k40/l.java':'package k40; public enum l { b, OTHER }',
'qj0/a.java':'package qj0; public enum a { x4, OTHER }',
'vb0/c.java':'package vb0; public enum c { NORMAL(false), PRIVATE(true); private final boolean p; c(boolean p){this.p=p;} public boolean b(){return p;} }',
'vb0/b.java':'package vb0; public class b { public vb0.c b; }',
'ah0/w0.java':'package ah0; public class w0 { public boolean changing; public int timeout=100; public boolean u0(){return changing;} public int m(){return timeout;} }',
'dc0/a.java':'package dc0; public interface a { public String s(); public String G(); public int k(); public int S(); }',
't40/d.java':'package t40; public interface d { public android.graphics.RectF c(android.graphics.Matrix m); }',
'fb0/q0.java':'package fb0; public class q0 { public qj0.a c; }',
'fb0/t0.java':'package fb0; public abstract class t0 { public fb0.q0 c; public abstract wb0.x0 c(); }',
'fz/h.java':'package fz; public class h { public boolean popup; public boolean b(){return popup;} }',
'wb0/t0.java':'package wb0; public interface t0 { }',
'wb0/z0.java':'package wb0; public class z0 { public boolean Y,L,g; public k40.l G; public ah0.w0 c; }',
'wb0/n0.java':'package wb0; public class n0 { public dc0.a state; public dc0.a C(){return state;} }',
'wb0/x0.java':'package wb0; public class x0 { public java.util.ArrayList a; public com.microsoft.fluency.ResultsFilter.PredictionSearchType b; }',
'wb0/x.java':'package wb0; public class x implements t0 { public z0 e; public com.microsoft.tokenshare.m i; public n0 c; public void P(lh0.a a,String s,String p,ec0.m m,boolean b,Integer n){} }',
'xd0/d1.java':'package xd0; public class d1 extends android.view.View { public fb0.t0 a; public d1(android.content.Context c){super(c);} public boolean f(lh0.a a,android.view.MotionEvent e){return false;} }',
'xd0/q.java':'package xd0; public class q extends d1 { public boolean j; public q(android.content.Context c){super(c);} }',
'xd0/f0.java':'package xd0; public class f0 extends q { public fz.h A; public android.util.SparseArray I; public wb0.t0 z; public f0(android.content.Context c){super(c);} }',
'hiro/swiftkey/LineEnter.java':'package hiro.swiftkey; public class LineEnter { public static boolean intercept; public static void reset(){} public static boolean handle(xd0.d1 view,lh0.a token,android.view.MotionEvent e){return intercept;} }'
}
for name,text in classes.items():
 p=ROOT/'stubs'/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text+'\n')

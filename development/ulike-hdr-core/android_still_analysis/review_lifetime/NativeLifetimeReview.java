package com.hiro.ulike.hdr.stillanalysis;

import java.lang.reflect.Field;
import java.util.Map;

/** Independently authored isolated ledger/exception review, host only. */
public final class NativeLifetimeReview {
 static int checks;
 interface Action {void run()throws Exception;}
 public static class Invoker {public long handle;public long getHandler(){return handle;}}
 public static final class ErrorInvoker extends Invoker {public final AssertionError failure=new AssertionError("original getter failure");@Override public long getHandler(){throw failure;}}
 static Field f(Class<?> c,String name)throws Exception{Field f=c.getDeclaredField(name);f.setAccessible(true);return f;}
 static Object value(String name)throws Exception{return f(RecorderAdmission.class,name).get(null);}
 static void check(boolean v,String message){checks++;if(!v)throw new AssertionError(message);}
 static void reject(Action action){checks++;try{action.run();throw new AssertionError("unsafe operation accepted");}catch(IllegalStateException expected){}catch(Exception other){throw new AssertionError(other);}}
 @SuppressWarnings("unchecked") static void reset()throws Exception {
  for(String name:new String[]{"recorders","natives","pending"})((Map<Object,Object>)value(name)).clear();
  f(RecorderAdmission.class,"installed").setBoolean(null,false);f(RecorderAdmission.class,"broken").setBoolean(null,false);f(RecorderAdmission.class,"session").set(null,null);
  ((ThreadLocal<?>)f(NativeLifetimeBoundary.class,"calls").get(null)).remove();
 }
 static int count(String name)throws Exception{return ((Map<?,?>)value(name)).size();}
 static boolean broken()throws Exception{return (Boolean)value("broken");}
 static Object stack()throws Exception{return ((ThreadLocal<?>)f(NativeLifetimeBoundary.class,"calls").get(null)).get();}
 static void install(){RecorderAdmission.installed(RecorderAdmission.HOOK_CONTRACT);}
 static void clean()throws Exception{check(count("pending")==0,"no leaked native tickets");check(stack()==null,"TLS removed after final completion");}
 static void disabled()throws Exception {
  reset();NativeLifetimeHooks.beforeInit(null);NativeLifetimeHooks.beforeUninit(null);NativeLifetimeHooks.returned(0);NativeLifetimeHooks.failed(null);
  check(!(Boolean)value("installed") && !broken(),"disabled hooks cannot install or quarantine admission");clean();
 }
 static void nestedSuccess()throws Exception {
  reset();install();Invoker outer=new Invoker(),inner=new Invoker();
  NativeLifetimeBoundary.beforeInit(outer);NativeLifetimeBoundary.beforeInit(inner);inner.handle=22;NativeLifetimeBoundary.returned(0);
  check(count("pending")==1 && count("natives")==1,"inner completion preserves outer ticket");
  outer.handle=11;NativeLifetimeBoundary.returned(0);check(count("natives")==2,"nested init both tracked");clean();
  NativeLifetimeBoundary.beforeUninit(outer);NativeLifetimeBoundary.beforeUninit(inner);inner.handle=0;NativeLifetimeBoundary.returned(0);
  check(count("pending")==1 && count("natives")==1,"nested teardown awaits outer actual return");
  outer.handle=0;check(count("natives")==1,"zero Java handle does not prove native teardown");NativeLifetimeBoundary.returned(0);check(count("natives")==0,"both actual successful teardowns tracked");clean();
 }
 static void doubleEntry()throws Exception {
  reset();install();Invoker invoker=new Invoker();NativeLifetimeBoundary.beforeInit(invoker);
  reject(()->NativeLifetimeBoundary.beforeInit(invoker));check(count("pending")==1 && !broken(),"denied nested same invoker has no side effects");
  invoker.handle=7;NativeLifetimeBoundary.returned(0);clean();
  reject(()->NativeLifetimeBoundary.beforeInit(invoker));check(count("natives")==1,"live native cannot be overwritten");
 }
 static void failedStatus()throws Exception {
  reset();install();Invoker invoker=new Invoker();NativeLifetimeBoundary.beforeInit(invoker);invoker.handle=17;NativeLifetimeBoundary.returned(-1);
  check(count("natives")==1 && !broken(),"nonzero init with real handler retained for normal cleanup");clean();
  NativeLifetimeBoundary.beforeUninit(invoker);invoker.handle=0;NativeLifetimeBoundary.returned(0);check(count("natives")==0,"failed init handle genuinely released");clean();
  reset();install();Invoker missing=new Invoker();NativeLifetimeBoundary.beforeInit(missing);reject(()->NativeLifetimeBoundary.returned(0));check(broken(),"zero-handle successful init quarantined");clean();
 }
 static void actualExceptions()throws Exception {
  reset();install();Invoker invoker=new Invoker();AssertionError original=new AssertionError("original native throw");
  NativeLifetimeBoundary.beforeInit(invoker);
  try{throw original;}catch(AssertionError failure){NativeLifetimeBoundary.failed(invoker);check(failure==original,"cleanup keeps original throwable identity");}
  check(broken(),"unknown native exception quarantined");clean();
  NativeLifetimeBoundary.failed(invoker);clean(); // repeated exceptional tail does not allocate TLS or throw
  reset();install();ErrorInvoker error=new ErrorInvoker();NativeLifetimeBoundary.beforeInit(error);
  try{NativeLifetimeBoundary.returned(0);throw new AssertionError("getter error ignored");}
  catch(IllegalStateException e){check(e.getCause().getCause()==error.failure,"reflective getter cause retained");}
  check(broken(),"reflective failure quarantined before TLS pop");clean();
 }
 static void unmatchedAndNestedFailures()throws Exception {
  reset();install();Invoker outer=new Invoker(),inner=new Invoker();NativeLifetimeBoundary.beforeInit(outer);NativeLifetimeBoundary.beforeInit(inner);
  NativeLifetimeBoundary.failed(new Object());check(broken() && count("pending")==2,"mismatched cleanup quarantines without consuming nested tickets");
  NativeLifetimeBoundary.failed(inner);check(count("pending")==1,"matching inner failure consumes only inner");NativeLifetimeBoundary.failed(outer);clean();
  reset();install();NativeLifetimeBoundary.failed(new Object());check(broken(),"unmatched exception after missing TLS remains quarantined");clean();
 }
 static void threadIsolation()throws Exception {
  reset();install();Invoker source=new Invoker();NativeLifetimeBoundary.beforeInit(source);
  final Throwable[] failure={null};Thread worker=new Thread(()->{try{NativeLifetimeBoundary.returned(0);failure[0]=new AssertionError("cross-thread completion accepted");}catch(IllegalStateException expected){}catch(Throwable t){failure[0]=t;}});
  worker.start();worker.join();check(failure[0]==null && count("pending")==1,"cross-thread return cannot pop source ticket");source.handle=55;NativeLifetimeBoundary.returned(0);clean();
 }
 public static void main(String[] args)throws Exception{disabled();nestedSuccess();doubleEntry();failedStatus();actualExceptions();unmatchedAndNestedFailures();threadIsolation();System.out.println("PASS independent native lifetime review "+checks+" checks; no device execution");}
}

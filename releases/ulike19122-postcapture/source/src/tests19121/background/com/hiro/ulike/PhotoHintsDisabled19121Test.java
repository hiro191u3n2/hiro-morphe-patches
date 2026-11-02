package com.hiro.ulike;

import android.content.Context;
import android.os.PerformanceHintManager;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

public final class PhotoHintsDisabled19121Test {
    private static int assertions;
    private static void check(boolean value,String message) { assertions++;if(!value)throw new AssertionError(message); }
    public static void main(String[] args)throws Exception {
        PerformanceHintManager manager=new PerformanceHintManager();
        Context context=new Context(manager);context.failure=new AssertionError("disabled policy must not request service");
        check(!PerformanceHints1952.photoHintsEnabled19121(),"photo ADPF policy disabled");
        PerformanceHints1952.init(null);PerformanceHints1952.init(context);PerformanceHints1952.init(context);
        check(context.requests==0,"init never requests ADPF service");
        Field hooks=SpeedWorkers1935.class.getDeclaredField("HINTS");hooks.setAccessible(true);
        check(hooks.get(null)==null,"init never installs photo performance hooks");
        check(PerformanceHints1952.begin(200000000L)==null,"begin returns no token");

        // Even an already populated API reference cannot bypass the begin guard.
        Class<?> apiClass=Class.forName("com.hiro.ulike.PerformanceHints1952$Api");
        Constructor<?> constructor=apiClass.getDeclaredConstructors()[0];constructor.setAccessible(true);
        Object api=constructor.newInstance(manager,
            PerformanceHintManager.class.getMethod("createHintSession",int[].class,long.class),
            PerformanceHintManager.Session.class.getMethod("updateTargetWorkDuration",long.class),
            PerformanceHintManager.Session.class.getMethod("reportActualWorkDuration",long.class),
            PerformanceHintManager.Session.class.getMethod("close"),(Method)null);
        Field field=PerformanceHints1952.class.getDeclaredField("api");field.setAccessible(true);field.set(null,api);
        final AtomicInteger runs=new AtomicInteger();
        PerformanceHints1952.worker(new Runnable(){public void run(){
            if(PerformanceHints1952.begin(200000000L)!=null)throw new AssertionError("cached API bypassed disabled policy");
            runs.incrementAndGet();
        }}).run();
        check(runs.get()==1,"disabled ADPF preserves worker execution");
        check(manager.attempts==0 && manager.sessions.isEmpty(),"no ADPF session created even with cached API");
        PerformanceHints1952.complete(null);PerformanceHints1952.cancel(null);
        check(context.requests==0 && manager.attempts==0,"completion and cancellation stay no-op");
        field.set(null,null);
        System.out.println("{\"status\":\"passed\",\"assertions\":"+assertions+",\"adpf_service_requests\":0,\"adpf_session_attempts\":0}");
    }
}

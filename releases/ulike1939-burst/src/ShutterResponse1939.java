package com.hiro.ulike;

import android.view.View;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/** Remove only artificial tap suppression when the actual photo pipeline is ready.
 * No queued taps, automatic capture, exposure changes or native busy overrides. */
public final class ShutterResponse1939 {
    private ShutterResponse1939() {}
    private static final WeakHashMap<View,WeakReference<Hold>> HOLDS=new WeakHashMap<View,WeakReference<Hold>>();
    public static long touchWindow(Object shutter) { return readyPhoto(shutter)?0L:200L; }
    public static long photoWindow(Object shutter) { return readyPhoto(shutter)?0L:500L; }
    private static boolean readyPhoto(Object shutter) {
        try {
            if (!(shutter instanceof View)) return false;
            View view=(View)shutter;
            if (!view.isEnabled() || !view.isShown() || !view.isAttachedToWindow() || !view.hasWindowFocus()) return false;
            // 1002 is the normal surface. Status 0 takes a photo on release
            // and retains the original 300ms video hold; status 1 takes the
            // photo on down. Active video and disabled states keep debounce.
            int status=integer(field(shutter,"f0"));
            if (integer(field(shutter,"q"))!=1002 || (status!=0 && status!=1)
                    || !Boolean.FALSE.equals(field(shutter,"J"))) return false;
            Object listener=field(shutter,"N");
            if (listener==null) return false;
            String type=listener.getClass().getName();
            if (!"i.o.a.b1.a.w.b.a.b$a".equals(type) && !"i.o.a.b1.a.w.b.c.q$b".equals(type)) return false;
            Object owner=field(listener,"a");
            Object camera=call(owner,"P");
            if (camera==null) return false;
            // This keeps D1's native lifecycle/readiness checks and the save
            // queue's real memory/ownership gate. A busy tap is never deferred.
            return AsyncSave1935.readiness(camera)==0;
        } catch (ReflectiveOperationException unavailable) { return false; }
        catch (RuntimeException unavailable) { return false; }
        catch (LinkageError unavailable) { return false; }
        catch (OutOfMemoryError unavailable) { return false; }
    }
    /** The original e() method schedules one 300ms long-video task per down.
     * After removing its 500ms debounce, an old task must never act on a newer
     * short tap. Keep that exact delay and native task, with press ownership. */
    public static boolean postHold(View view,Runnable nativeTask,long delay) {
        try {
            if (integer(field(view,"f0"))!=0) return view.postDelayed(nativeTask,delay);
            Hold next=new Hold(view,nativeTask,((Long)field(view,"L")).longValue(),field(view,"N"));
            synchronized(HOLDS) {
                WeakReference<Hold> previous=HOLDS.remove(view);
                Hold prior=previous==null?null:previous.get();
                if(prior!=null)view.removeCallbacks(prior);
                HOLDS.put(view,new WeakReference<Hold>(next));
                boolean posted=view.postDelayed(next,delay);
                if(!posted)HOLDS.remove(view);
                return posted;
            }
        }catch(ReflectiveOperationException unavailable){return false;}
        catch(RuntimeException unavailable){return false;}
        catch(LinkageError unavailable){return false;}
        catch(OutOfMemoryError unavailable){return false;}
    }
    private static final class Hold implements Runnable {
        final WeakReference<View> view;
        final Runnable nativeTask;
        final long down;
        final Object listener;
        Hold(View view,Runnable task,long down,Object listener){this.view=new WeakReference<View>(view);nativeTask=task;this.down=down;this.listener=listener;}
        public void run(){
            View button=view.get();if(button==null)return;
            synchronized(HOLDS){WeakReference<Hold> ref=HOLDS.get(button);if(ref==null||ref.get()!=this)return;HOLDS.remove(button);}
            try {
                if(!button.isAttachedToWindow()||!button.isShown()||!button.hasWindowFocus())return;
                if(integer(field(button,"f0"))!=0||integer(field(button,"q"))!=1002)return;
                if(((Long)field(button,"L")).longValue()!=down||field(button,"N")!=listener)return;
                if(!Boolean.FALSE.equals(field(button,"e0"))||!Boolean.FALSE.equals(field(button,"J")))return;
                nativeTask.run();
            }catch(ReflectiveOperationException unavailable){}
            catch(RuntimeException unavailable){}
            catch(LinkageError unavailable){}
            catch(OutOfMemoryError unavailable){}
        }
    }
    private static int integer(Object value) { return value instanceof Integer?((Integer)value).intValue():-1; }
    private static Object field(Object owner,String name)throws ReflectiveOperationException {
        if(owner==null)throw new NoSuchFieldException(name);
        for(Class<?> c=owner.getClass();c!=null;c=c.getSuperclass())try{
            Field field=c.getDeclaredField(name);field.setAccessible(true);return field.get(owner);
        }catch(NoSuchFieldException absent){}
        throw new NoSuchFieldException(name);
    }
    private static Object call(Object owner,String name)throws ReflectiveOperationException {
        Method method=owner.getClass().getMethod(name);method.setAccessible(true);return method.invoke(owner);
    }
}

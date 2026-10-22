package com.hiro.ulike;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Host checks for dynamic values, reflection semantics and concurrent ownership. */
public final class ReflectionCache1945Test {
    private static final AtomicInteger checks=new AtomicInteger();
    public interface InterfaceValue { default int inherited(){return 71;} }
    public static class Base {
        private int privateValue=41;
        private int shadow=22;
        private int hidden(){return 12;}
        public String overload(String value){return "string:"+value;}
        public String overload(int value){return "int:"+value;}
        public void fail(){throw new IllegalStateException("original target failure");}
    }
    public static final class Child extends Base implements InterfaceValue {
        private int shadow=33;
    }
    public static final class StaticValue {
        private static int value=3;
        public static int current(){return value;}
    }
    public static final class FailedInitialization {
        static int value=fail();
        private static int fail(){throw new IllegalStateException("class initialization");}
    }
    public static class LoaderValue {
        public int value;
        public int current(){return value;}
        public int plus(int delta){return value+delta;}
    }
    private static void require(boolean condition,String message){
        if(!condition)throw new AssertionError(message);checks.incrementAndGet();
    }
    private static int entries(String name)throws Exception{
        Field field=ReflectionCache1945.class.getDeclaredField(name);field.setAccessible(true);
        Map<?,?> map=(Map<?,?>)field.get(null);synchronized(map){return map.size();}
    }
    private static void semantics()throws Exception{
        Child a=new Child(),b=new Child();
        Field inherited=ReflectionCache1945.field(Child.class,"privateValue");
        require(inherited.getDeclaringClass()==Base.class,"private inherited field owner");
        require(inherited==ReflectionCache1945.field(Child.class,"privateValue"),"successful metadata reuse");
        inherited.set(a,79);require(((Integer)inherited.get(a))==79,"fresh changed value");
        require(((Integer)inherited.get(b))==41,"target instances remain separate");
        require(ReflectionCache1945.field(Child.class,"shadow").getDeclaringClass()==Child.class,"nearest shadow declaration");
        try{ReflectionCache1945.declaredField(Child.class,"privateValue");throw new AssertionError("static exact-owner semantics");}
        catch(NoSuchFieldException expected){checks.incrementAndGet();}
        Field staticField=ReflectionCache1945.declaredField(StaticValue.class,"value");
        Method staticMethod=ReflectionCache1945.method(StaticValue.class,"current",new Class<?>[0]);
        require(((Integer)staticMethod.invoke(null))==3,"initial static value");
        staticField.set(null,27);require(((Integer)staticMethod.invoke(null))==27,"static results are never cached");
        Method string=ReflectionCache1945.method(Child.class,"overload",new Class<?>[]{String.class});
        Method integer=ReflectionCache1945.method(Child.class,"overload",new Class<?>[]{int.class});
        require(string!=integer,"overload separation");
        require("string:a".equals(string.invoke(a,"a")),"reference overload invocation");
        require("int:8".equals(integer.invoke(a,8)),"primitive overload invocation");
        Class<?>[] mutable={String.class};
        require(string==ReflectionCache1945.method(Child.class,"overload",mutable),"initial parameter key");
        mutable[0]=int.class;
        require(integer==ReflectionCache1945.method(Child.class,"overload",mutable),"mutated caller array cannot alter cache key");
        require(string==ReflectionCache1945.method(Child.class,"overload",new Class<?>[]{String.class}),"original parameter key retained");
        require(((Integer)ReflectionCache1945.method(Child.class,"inherited",new Class<?>[0]).invoke(a))==71,"public interface default method");
        try{ReflectionCache1945.method(Child.class,"hidden",new Class<?>[0]);throw new AssertionError("private method became public");}
        catch(NoSuchMethodException expected){checks.incrementAndGet();}
        try{ReflectionCache1945.method(Child.class,"fail",new Class<?>[0]).invoke(a);throw new AssertionError("target failure swallowed");}
        catch(InvocationTargetException expected){require(expected.getCause() instanceof IllegalStateException,"original invocation exception wrapper");}
        require(ReflectionCache1945.type(StaticValue.class.getName())==StaticValue.class,"named class resolution");
        require(ReflectionCache1945.type(StaticValue.class.getName())==StaticValue.class,"named class cache reuse");
        int classes=entries("CLASSES"),fields=entries("FIELDS"),methods=entries("METHODS");
        for(int i=0;i<16;i++){
            try{ReflectionCache1945.type("com.hiro.ulike.DoesNotExist1945");throw new AssertionError("missing class");}
            catch(ClassNotFoundException expected){checks.incrementAndGet();}
            try{ReflectionCache1945.field(Child.class,"missing");throw new AssertionError("missing field");}
            catch(NoSuchFieldException expected){require("missing".equals(expected.getMessage()),"same missing field failure");}
            try{ReflectionCache1945.method(Child.class,"missing",new Class<?>[0]);throw new AssertionError("missing method");}
            catch(NoSuchMethodException expected){checks.incrementAndGet();}
        }
        require(entries("CLASSES")==classes&&entries("FIELDS")==fields&&entries("METHODS")==methods,"failed lookups are not retained");
        try{ReflectionCache1945.type(FailedInitialization.class.getName());throw new AssertionError("initialization failure");}
        catch(ExceptionInInitializerError expected){checks.incrementAndGet();}
        try{ReflectionCache1945.type(FailedInitialization.class.getName());throw new AssertionError("failed initialization cached");}
        catch(NoClassDefFoundError expected){checks.incrementAndGet();}
        require(entries("CLASSES")==classes,"class initialization failure is not cached");
    }
    private static void concurrent()throws Exception{
        final CountDownLatch start=new CountDownLatch(1);
        final AtomicReference<Throwable> failure=new AtomicReference<Throwable>();
        List<Thread> threads=new ArrayList<Thread>();
        for(int t=0;t<12;t++){
            final int id=t;
            Thread thread=new Thread(new Runnable(){public void run(){
                try{
                    start.await();LoaderValue own=new LoaderValue();own.value=id*1000;
                    for(int i=0;i<400;i++){
                        Field field=ReflectionCache1945.field(LoaderValue.class,"value");
                        Method method=ReflectionCache1945.method(LoaderValue.class,"plus",new Class<?>[]{int.class});
                        field.set(own,id*1000+i);
                        require(((Integer)method.invoke(own,5))==id*1000+i+5,"concurrent per-target value");
                        require(ReflectionCache1945.type(StaticValue.class.getName())==StaticValue.class,"concurrent named resolution");
                    }
                }catch(Throwable error){failure.compareAndSet(null,error);}
            }},"reflection1945-"+id);
            threads.add(thread);thread.start();
        }
        start.countDown();for(Thread thread:threads)thread.join();
        if(failure.get()!=null)throw new AssertionError("concurrent metadata lookup",failure.get());
    }
    private static byte[] fixtureBytes()throws Exception{
        String resource="/"+LoaderValue.class.getName().replace('.','/')+".class";
        try(InputStream input=ReflectionCache1945Test.class.getResourceAsStream(resource)){
            require(input!=null,"class-loader fixture bytes");ByteArrayOutputStream output=new ByteArrayOutputStream();
            byte[] buffer=new byte[2048];for(int count;(count=input.read(buffer))>=0;)output.write(buffer,0,count);
            return output.toByteArray();
        }
    }
    private static void loaderIsolationAndBounds()throws Exception{
        final byte[] fixture=fixtureBytes();final String name=LoaderValue.class.getName();
        Class<?> first=null;Object firstValue=null;
        for(int i=0;i<260;i++){
            ClassLoader loader=new ClassLoader(ReflectionCache1945Test.class.getClassLoader()){
                protected Class<?> loadClass(String className,boolean resolve)throws ClassNotFoundException{
                    if(!className.equals(name))return super.loadClass(className,resolve);
                    synchronized(getClassLoadingLock(className)){
                        Class<?> type=findLoadedClass(className);if(type==null)type=defineClass(className,fixture,0,fixture.length);
                        if(resolve)resolveClass(type);return type;
                    }
                }
            };
            Class<?> type=loader.loadClass(name);Object own=type.getConstructor().newInstance();
            require(type!=LoaderValue.class,"same binary name uses actual loader identity");
            ReflectionCache1945.field(type,"value").set(own,i);
            require(((Integer)ReflectionCache1945.method(type,"current",new Class<?>[0]).invoke(own))==i,"method belongs to its target loader");
            if(first==null){first=type;firstValue=own;}else require(type!=first,"different classes remain distinct");
        }
        require(entries("FIELDS")<=192&&entries("METHODS")<=192&&entries("CLASSES")<=64,"metadata caches remain bounded");
        require(((Integer)ReflectionCache1945.field(first,"value").get(firstValue))==0,"evicted field resolves correctly");
        require(((Integer)ReflectionCache1945.method(first,"current",new Class<?>[0]).invoke(firstValue))==0,"evicted method resolves correctly");
        String arrayName="Ljava.lang.String;";
        for(int i=0;i<80;i++){
            arrayName="["+arrayName;
            require(ReflectionCache1945.type(arrayName).isArray(),"named class cache eviction load");
        }
        require(entries("CLASSES")==64,"named class cache reaches its bound");
        require(ReflectionCache1945.type("[Ljava.lang.String;")==String[].class,"evicted named class resolves correctly");
    }
    public static void main(String[] args)throws Exception{
        semantics();concurrent();loaderIsolationAndBounds();
        System.out.println("PASS reflection metadata caching checks="+checks.get()+" fields="+entries("FIELDS")+" methods="+entries("METHODS")+" classes="+entries("CLASSES"));
    }
}

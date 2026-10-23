package app.hiro.oneback.runtime;

import java.io.*;
import java.util.*;
import android.app.*;

/** JVM fixtures use the exact APK names, which cannot be Java source identifiers. */
final class Instagram447Fixture {
 static final String OWNER="androidx/fragment/app/FragmentActivity", MANAGER="X/0dj", STORE="X/0dw";
 static final class Pool {
  final List<byte[]> values=new ArrayList<byte[]>();
  int add(int tag,Object... args)throws Exception {ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream o=new DataOutputStream(b);o.writeByte(tag);
   for(Object v:args)if(v instanceof String)o.writeUTF((String)v);else o.writeShort((Integer)v);values.add(b.toByteArray());return values.size();}
  int utf(String s)throws Exception{return add(1,s);}
  int cls(String s)throws Exception{return add(7,utf(s));}
  int ref(int tag,String owner,String name,String type)throws Exception{return add(tag,cls(owner),add(12,utf(name),utf(type)));}
 }
 static byte[] code(int... values)throws Exception {ByteArrayOutputStream b=new ByteArrayOutputStream();for(int v:values)b.write(v);return b.toByteArray();}
 static int hi(int v){return v>>>8;}static int lo(int v){return v&255;}
 static byte[] clazz(String name,String parent,String field,String fieldType,String concrete,String method,boolean activity)throws Exception {
  Pool p=new Pool();int self=p.cls(name),sup=p.cls(parent),fn=p.utf(field),ft=p.utf(fieldType);
  int initName=p.utf("<init>"),initType=p.utf(activity?"(Landroid/app/Application;IZ)V":"()V"),codeName=p.utf("Code");
  int superInit=p.ref(10,parent,"<init>",activity?"(Landroid/app/Application;IZ)V":"()V");
  int created=p.cls(concrete),createdInit=p.ref(10,concrete,"<init>","()V"),fieldRef=p.ref(9,name,field,fieldType);
  ByteArrayOutputStream cb=new ByteArrayOutputStream();DataOutputStream co=new DataOutputStream(cb);
  co.writeByte(0x2a);if(activity){co.writeByte(0x2b);co.writeByte(0x1c);co.writeByte(0x1d);}
  co.writeByte(0xb7);co.writeShort(superInit);co.writeByte(0x2a);co.writeByte(0xbb);co.writeShort(created);co.writeByte(0x59);co.writeByte(0xb7);co.writeShort(createdInit);co.writeByte(0xb5);co.writeShort(fieldRef);co.writeByte(0xb1);
  int mn=method==null?0:p.utf(method),mt=method==null?0:p.utf("()"+fieldType);
  ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream o=new DataOutputStream(b);
  o.writeInt(0xcafebabe);o.writeShort(0);o.writeShort(52);o.writeShort(p.values.size()+1);for(byte[] v:p.values)o.write(v);
  o.writeShort(0x21);o.writeShort(self);o.writeShort(sup);o.writeShort(0);o.writeShort(1);
  o.writeShort(0x11);o.writeShort(fn);o.writeShort(ft);o.writeShort(0);o.writeShort(method==null?1:2);
  method(o,initName,initType,codeName,cb.toByteArray(),activity?4:3,activity?4:1);
  if(method!=null)method(o,mn,mt,codeName,code(0x2a,0xb4,hi(fieldRef),lo(fieldRef),0xb0),1,1);
  o.writeShort(0);return b.toByteArray();
 }
 static void method(DataOutputStream o,int name,int type,int code,byte[] body,int stack,int locals)throws Exception {
  o.writeShort(1);o.writeShort(name);o.writeShort(type);o.writeShort(1);o.writeShort(code);o.writeInt(12+body.length);
  o.writeShort(stack);o.writeShort(locals);o.writeInt(body.length);o.write(body);o.writeShort(0);o.writeShort(0);
 }
 static Activity create(Application app,boolean mismatched)throws Exception {
  final Map<String,byte[]> classes=new HashMap<String,byte[]>();
  classes.put(STORE.replace('/','.'),clazz(STORE,"java/lang/Object","A01",mismatched?"Ljava/util/List;":"Ljava/util/ArrayList;","java/util/ArrayList",null,false));
  classes.put(MANAGER.replace('/','.'),clazz(MANAGER,"java/lang/Object","A0V","L"+STORE+";",STORE,null,false));
  classes.put(OWNER.replace('/','.'),clazz(OWNER,"android/app/Activity","manager","L"+MANAGER+";",MANAGER,"A1Y",true));
  final String main="com.instagram.mainactivity.InstagramMainActivity";
  classes.put(main,clazz(main.replace('.','/'),OWNER,"unused","Ljava/lang/Object;","java/lang/Object",null,true));
  ClassLoader loader=new ClassLoader(Instagram447Fixture.class.getClassLoader()){
   protected synchronized Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException {
    if(!classes.containsKey(name))return super.loadClass(name,resolve);
    Class<?> c=findLoadedClass(name);if(c==null)c=findClass(name);if(resolve)resolveClass(c);return c;
   }
   protected Class<?> findClass(String name)throws ClassNotFoundException {byte[] bytes=classes.get(name);if(bytes==null)throw new ClassNotFoundException(name);return defineClass(name,bytes,0,bytes.length);}
  };
  return (Activity)loader.loadClass(main).getConstructor(Application.class,int.class,boolean.class).newInstance(app,11,true);
 }
 @SuppressWarnings("unchecked") static List<Object> added(Activity a)throws Exception {
  Object manager=a.getClass().getMethod("A1Y").invoke(a);Object store=manager.getClass().getField("A0V").get(manager);
  return (List<Object>)store.getClass().getField("A01").get(store);
 }
}

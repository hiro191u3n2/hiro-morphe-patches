package android.content;
import android.content.pm.PackageManager;
import java.util.*;
import java.util.concurrent.CountDownLatch;

/** Instrumented, separate diagnostic/proof stores; no actual Android storage. */
public final class Context {
 public static final int MODE_PRIVATE=0;
 static final Memory JOURNAL=new Memory(true),PROOF=new Memory(false);
 public static volatile Object qualificationLock;
 public static volatile int journalIO,badJournalThread,journalUnderQueueLock,journalWrites,proofScans;
 public static volatile boolean blockCommit;
 public static volatile CountDownLatch commitEntered=new CountDownLatch(0),releaseCommit=new CountDownLatch(0);
 public Context getApplicationContext(){return this;}
 public String getPackageName(){return "test.ulike";}
 public PackageManager getPackageManager(){return new PackageManager();}
 public SharedPreferences getSharedPreferences(String name,int mode){if(name.equals("ulike_verification_journal19101")){io();return JOURNAL;}if(name.equals("ulike_gx1961_proofs"))return PROOF;throw new AssertionError("unexpected preference store");}
 static void io(){journalIO++;if(!Thread.currentThread().getName().equals("ulike-verification-journal19101"))badJournalThread++;if(qualificationLock!=null&&Thread.holdsLock(qualificationLock))journalUnderQueueLock++;}
 public static void seedJournal(String text){synchronized(JOURNAL){JOURNAL.values.put("history_v1",text);}}
 public static String journalText(){synchronized(JOURNAL){String s=JOURNAL.values.get("history_v1");return s==null?"":s;}}
 public static void seedProof(String key,String text){synchronized(PROOF){PROOF.values.put(key,text);}}
 public static Map<String,String> proofCopy(){synchronized(PROOF){return new HashMap<String,String>(PROOF.values);}}
 public static final class Memory implements SharedPreferences {
  final boolean journal;final Map<String,String> values=new HashMap<String,String>();Memory(boolean journal){this.journal=journal;}
  public synchronized String getString(String key,String fallback){if(journal)io();String value=values.get(key);return value==null?fallback:value;}
  public synchronized Map<String,?> getAll(){if(journal)throw new AssertionError("journal full scan");proofScans++;return new HashMap<String,String>(values);}
  public Editor edit(){if(journal)io();return new Editor(){final Map<String,String> changes=new HashMap<String,String>();
   public Editor putString(String key,String value){changes.put(key,value);return this;}public Editor remove(String key){changes.put(key,null);return this;}
   void publish(){synchronized(Memory.this){for(Map.Entry<String,String> e:changes.entrySet())if(e.getValue()==null)values.remove(e.getKey());else values.put(e.getKey(),e.getValue());}}
   public boolean commit(){if(journal){io();journalWrites++;if(blockCommit){commitEntered.countDown();try{releaseCommit.await();}catch(InterruptedException e){Thread.currentThread().interrupt();return false;}}}publish();return true;}
   public void apply(){if(journal)throw new AssertionError("journal must observe commit result");publish();}
  };}
 }
}

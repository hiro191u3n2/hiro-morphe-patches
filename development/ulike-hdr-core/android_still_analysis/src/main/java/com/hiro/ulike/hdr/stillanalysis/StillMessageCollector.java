package com.hiro.ulike.hdr.stillanalysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/** Bounded one-submission Lua export transport; geometry interpretation belongs to style binding. */
public final class StillMessageCollector {
    public static final int MESSAGE_ID = 0x554c5301;
    private static final int MAX_RECORD_CHARS=8192, MAX_TOTAL_CHARS=4194304, MAX_RECORDS=4096;
    private final int nonce;
    private final Map<String,List<Record>> records=new LinkedHashMap<>();
    private final Set<String> ended=new LinkedHashSet<>();
    private boolean submitted,closed;
    private int chars,count,foreignMessages;
    private String failure;
    public StillMessageCollector(int nonce,Set<String> expectedFeatures) {
        if(nonce<1 || expectedFeatures==null || expectedFeatures.size()>64)throw new IllegalArgumentException("Nonce/features");
        this.nonce=nonce;
        for(String f:expectedFeatures) {
            if(f==null || !f.matches("[A-Za-z0-9_.-]{1,64}"))throw new IllegalArgumentException("Feature name");
            records.put(f,new ArrayList<>());
        }
    }
    public synchronized void submitted() {
        if(submitted || closed || failure!=null)throw new IllegalStateException("Collector already used");
        submitted=true;notifyAll();
    }
    public synchronized void accept(int id,int packetNonce,int ordinal,String text) {
        if(closed)return;
        if(id!=MESSAGE_ID || packetNonce!=nonce) { if(foreignMessages<Integer.MAX_VALUE)foreignMessages++;return; }
        if(failure!=null)return;
        if(!submitted) { fail("Matching message before still submission");return; }
        if(text==null || text.length()>MAX_RECORD_CHARS || text.isEmpty()) { fail("Oversized/missing message");return; }
        if(chars>MAX_TOTAL_CHARS-text.length() || count==MAX_RECORDS) { fail("Message budget exceeded");return; }
        for(int i=0;i<text.length();i++)if(text.charAt(i)<32 || text.charAt(i)>126) { fail("Non-ASCII/control message");return; }
        String[] fields=text.split("\\|",5);
        if(fields.length!=5 || !fields[0].equals("S1")) { fail("Protocol version/fields");return; }
        String feature=fields[1],kind=fields[2];
        List<Record> list=records.get(feature);
        if(list==null || ended.contains(feature) || ordinal!=list.size()+1 || ordinal>512) { fail("Unknown/ended feature or unordered message");return; }
        int face;
        try { face=Integer.parseInt(fields[3]); } catch(NumberFormatException e) { fail("Invalid face index");return; }
        if(kind.equals("END")) {
            if(face!=-1 || !fields[4].equals(Integer.toString(ordinal-1))) { fail("END count mismatch");return; }
            ended.add(feature);
        } else {
            if(face<(kind.equals("uniform")?-1:0) || face>9 || !(kind.equals("uniform") || kind.equals("mvp") || kind.equals("vertices"))) { fail("Unsupported record/face");return; }
            if(fields[4].isEmpty()) { fail("Missing record payload");return; }
        }
        list.add(new Record(feature,kind,face,ordinal,fields[4]));chars+=text.length();count++;notifyAll();
    }
    public synchronized void fail(String reason) { if(!closed && failure==null)failure=reason;notifyAll(); }
    public synchronized void awaitComplete(long millis) throws InterruptedException {
        if(millis<1 || millis>30000)throw new IllegalArgumentException("Deadline");
        long end=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(millis);
        while(!closed && failure==null && (!submitted || ended.size()!=records.size())) {
            long left=end-System.nanoTime();
            if(left<=0) { fail("Still script export timeout");break; }
            TimeUnit.NANOSECONDS.timedWait(this,left);
        }
        if(closed || failure!=null)throw new IllegalStateException(failure==null?"Collector closed":failure);
    }
    public synchronized Snapshot finish() {
        if(closed)throw new IllegalStateException("Already finished");
        if(!submitted || ended.size()!=records.size())fail("Incomplete still script exports");
        closed=true;notifyAll();return new Snapshot(nonce,records,failure,foreignMessages);
    }
    public static final class Record {
        public final String feature,kind,payload;
        public final int faceIndex,ordinal;
        private Record(String f,String k,int face,int n,String p) { feature=f;kind=k;faceIndex=face;ordinal=n;payload=p; }
    }
    public static final class Snapshot {
        public final int nonce,foreignMessages;
        public final String failure;
        public final boolean allExpectedExportsObserved;
        private final Map<String,List<Record>> records;
        private Snapshot(int n,Map<String,List<Record>> source,String failure,int foreign) {
            nonce=n;this.failure=failure;foreignMessages=foreign;allExpectedExportsObserved=failure==null;
            LinkedHashMap<String,List<Record>> owned=new LinkedHashMap<>();
            for(Map.Entry<String,List<Record>> e:source.entrySet())owned.put(e.getKey(),Collections.unmodifiableList(new ArrayList<>(e.getValue())));
            records=Collections.unmodifiableMap(owned);
        }
        public Map<String,List<Record>> records() { return records; }
    }
}

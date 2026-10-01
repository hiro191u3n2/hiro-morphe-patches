package com.hiro.ulike.integration169;

/** One-way publication outcome for one owned photo. A candidate URI is never a saved URI.
 * The underlying transaction alone establishes publication; this latch prevents later UI or
 * camera failure callbacks from reclassifying a committed or uncertain publication as unsaved.
 */
public final class PublicationOutcome169 {
    public enum Kind { PROCESSING, PUBLISHING, FAILED_BEFORE_PUBLICATION, COMMITTED, PUBLICATION_UNCERTAIN }
    public static final class Snapshot {
        public final Kind kind;
        public final String committedUri;
        public final UnconfirmedPhoto unconfirmed;
        private Snapshot(Kind kind,String committedUri,UnconfirmedPhoto unconfirmed){
            this.kind=kind;this.committedUri=committedUri;this.unconfirmed=unconfirmed;
        }
    }
    /** Recovery identity only. This does not assert that the candidate exists or is visible. */
    public static final class UnconfirmedPhoto {
        public final String transactionId,candidateUri,identitySha256;
        public final long expectedBytes;
        public UnconfirmedPhoto(String transactionId,String candidateUri,long expectedBytes,String identitySha256){
            bounded(transactionId,128,"transaction ID");bounded(candidateUri,4096,"candidate URI");
            if(expectedBytes<1 || identitySha256==null || !identitySha256.matches("[0-9a-f]{64}"))
                throw new IllegalArgumentException("Exact recovery bytes and identity required");
            this.transactionId=transactionId;this.candidateUri=candidateUri;
            this.expectedBytes=expectedBytes;this.identitySha256=identitySha256;
        }
    }
    /** Identity-only authority held by the one save operation, never by failure observers. */
    public static final class SaveAttempt { private SaveAttempt(){} }
    private SaveAttempt activeAttempt;
    private volatile Snapshot value=new Snapshot(Kind.PROCESSING,null,null);
    public Snapshot snapshot(){return value;}
    public synchronized SaveAttempt beginSave(){
        requireProcessing();SaveAttempt attempt=new SaveAttempt();
        value=new Snapshot(Kind.PUBLISHING,null,null);activeAttempt=attempt;return attempt;
    }
    public synchronized void committed(SaveAttempt attempt,String uri){
        bounded(uri,4096,"committed URI");requireAttempt(attempt);value=new Snapshot(Kind.COMMITTED,uri,null);activeAttempt=null;
    }
    public synchronized void uncertain(SaveAttempt attempt,UnconfirmedPhoto photo){
        if(photo==null)throw new NullPointerException("unconfirmed photo");
        requireAttempt(attempt);value=new Snapshot(Kind.PUBLICATION_UNCERTAIN,null,photo);activeAttempt=null;
    }
    /** Called only after the save API confirmed a failure before publication. */
    public synchronized void failedSave(SaveAttempt attempt){
        requireAttempt(attempt);value=new Snapshot(Kind.FAILED_BEFORE_PUBLICATION,null,null);activeAttempt=null;
    }
    /** True only for the first failure that can truthfully be reported as not published. */
    public synchronized boolean failBeforePublication(){
        if(value.kind!=Kind.PROCESSING)return false;
        value=new Snapshot(Kind.FAILED_BEFORE_PUBLICATION,null,null);return true;
    }
    private void requireAttempt(SaveAttempt attempt){if(attempt==null || attempt!=activeAttempt || value.kind!=Kind.PUBLISHING)throw new IllegalStateException("Not the active save operation");}
    private void requireProcessing(){if(value.kind!=Kind.PROCESSING)throw new IllegalStateException("Publication outcome is already terminal");}
    private static void bounded(String s,int max,String name){
        if(s==null || s.isEmpty() || s.length()>max)throw new IllegalArgumentException("Invalid "+name);
        for(int i=0;i<s.length();i++)if(s.charAt(i)<32 || s.charAt(i)==127)throw new IllegalArgumentException("Control character in "+name);
    }
}

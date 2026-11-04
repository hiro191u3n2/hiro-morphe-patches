package com.swiftkey.typeface.candidates.candidates;
public class EmailAddressCandidate implements m40.b {
 private final m40.b wrapped;
 public EmailAddressCandidate(m40.b wrapped){this.wrapped=wrapped;}
 public m40.b getWrapped(){return wrapped;}
 public String getUserFacingText(){return wrapped.getUserFacingText();}
 public l40.c subrequest(){return wrapped.subrequest();}
 public String getTrailingSeparator(){return wrapped.getTrailingSeparator();}
 public void setTrailingSeparator(String s){wrapped.setTrailingSeparator(s);}
}

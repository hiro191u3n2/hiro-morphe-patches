package fc0;public class d1 implements ic0.a {public String b,c;public boolean d=true;public int calls;public lh0.a lastToken;public boolean fail;public Runnable callback;public void b(lh0.a token){calls++;lastToken=token;if(callback!=null)callback.run();if(fail)throw new RuntimeException("native insertion failure");}}


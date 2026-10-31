package com.ss.android.ugc.aweme.base.ui;
public class CommonPageFragment {
    public int backCalls;public boolean consume;public Runnable back;
    public boolean LJLI(){backCalls++;if(back!=null)back.run();return consume;}
}

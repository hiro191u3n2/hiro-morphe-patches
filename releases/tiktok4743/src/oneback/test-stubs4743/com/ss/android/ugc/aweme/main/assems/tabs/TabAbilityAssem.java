package com.ss.android.ugc.aweme.main.assems.tabs;
public final class TabAbilityAssem implements com.bytedance.tiktok.homepage.mainfragment.HomeTabAbility {
    public String selected="USER",lastTag;
    public int calls;
    public boolean fail,noOp;
    public Runnable transition;
    public final com.bytedance.hox.Hox tree=new com.bytedance.hox.Hox();
    public String tv2(){return selected;}
    public com.bytedance.hox.Hox jI0(){return tree;}
    public void tB0(android.os.Bundle bundle,String tag){calls++;lastTag=tag;if(fail)throw new IllegalStateException("test failure");if(noOp)return;selected=tag;tree.homeActive="HOME".equals(tag);if(transition!=null)transition.run();}
}

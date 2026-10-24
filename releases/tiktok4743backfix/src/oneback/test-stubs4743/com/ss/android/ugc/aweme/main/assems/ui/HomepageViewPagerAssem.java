package com.ss.android.ugc.aweme.main.assems.ui;
public final class HomepageViewPagerAssem implements com.ss.android.ugc.aweme.base.HomePageViewPagerAbility {
    public final com.ss.android.ugc.aweme.homepage.api.interaction.ScrollSwitchStateManager state;
    public int calls;public boolean noOp,fail;public Boolean lastAnimate;public boolean lastBackPress;
    public HomepageViewPagerAssem(com.ss.android.ugc.aweme.homepage.api.interaction.ScrollSwitchStateManager state){this.state=state;}
    public boolean Ir1(Boolean animate,boolean backPress){
        calls++;lastAnimate=animate;lastBackPress=backPress;
        if(fail)throw new IllegalStateException("fixture uninitialized outer helper");
        if(!noOp)state.selected="page_feed";
        return true;
    }
}

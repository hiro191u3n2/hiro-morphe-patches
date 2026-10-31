package com.ss.android.ugc.aweme.main;
/** Exact public bindings read from the original 47.4.3 DEX. */
public class MainActivity extends android.app.Activity {
    public com.bytedance.tiktok.homepage.mainfragment.HomeTabAbility communication_gen_field_com_bytedance_tiktok_homepage_mainfragment_HomeTabAbility;
    public com.ss.android.ugc.aweme.homepage.api.interaction.ScrollSwitchStateManager stateManager;
    public com.ss.android.ugc.aweme.base.HomePageViewPagerAbility communication_gen_field_com_ss_android_ugc_aweme_base_HomePageViewPagerAbility;
    public MainActivity(android.app.Application app,int id,boolean root){super(app,id,root);}
    public Object getAbility(Class<?> type){
        if(type==com.bytedance.tiktok.homepage.mainfragment.HomeTabAbility.class)return communication_gen_field_com_bytedance_tiktok_homepage_mainfragment_HomeTabAbility;
        if(type==com.ss.android.ugc.aweme.base.HomePageViewPagerAbility.class)return communication_gen_field_com_ss_android_ugc_aweme_base_HomePageViewPagerAbility;
        return null;
    }
}

package com.ss.android.ugc.aweme.homepage.api.interaction;
public class BaseScrollSwitchStateManager {
    public String selected="page_feed";
    public final com.ss.android.ugc.aweme.base.ui.CommonPageFragment page=new com.ss.android.ugc.aweme.base.ui.CommonPageFragment();
    public boolean noPage;
    public String i33(){return selected;}
    public boolean s33(String tag){return tag.equals(i33());}
    public com.ss.android.ugc.aweme.base.ui.CommonPageFragment j33(String tag){return noPage?null:page;}
}

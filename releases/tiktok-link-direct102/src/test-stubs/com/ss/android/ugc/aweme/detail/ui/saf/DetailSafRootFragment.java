package com.ss.android.ugc.aweme.detail.ui.saf;
/** Exact public native surface used by the runtime; navigation is audited from APK DEX separately. */
public class DetailSafRootFragment {
    public android.app.Activity activity;
    public android.content.Intent route;
    public android.view.View view;
    public android.os.Bundle arguments;
    public boolean resumed=true,visible=true;
    public android.app.Activity getActivity(){return activity;}
    public android.content.Intent eY(){return route;}
    public android.os.Bundle getArguments(){return arguments;}
    public boolean isResumed(){return resumed;}
    public boolean isVisible(){return visible;}
    public android.view.View getView(){return view;}
}

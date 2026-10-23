import java.util.*;import com.android.tools.smali.dexlib2.iface.*;
public class RuntimeContracts452{
 static Map<String,ClassDef>cs;static int count;
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);count++;}
 static void f(String c,String n,String t,int mask,int value){ClassDef x=cs.get(c);check(x!=null,"class "+c);for(Field f:x.getFields())if(f.getName().equals(n)){check(f.getType().equals(t)&&(f.getAccessFlags()&mask)==value,c+"->"+n);return;}throw new AssertionError(c+"->"+n);}
 static void m(String c,String n,String r,int flags,String...p){ClassDef x=cs.get(c);check(x!=null,"class "+c);for(Method m:x.getMethods())if(m.getName().equals(n)&&m.getParameterTypes().equals(Arrays.asList(p))){check(m.getReturnType().equals(r)&&m.getAccessFlags()==flags,c+"->"+n);return;}throw new AssertionError(c+"->"+n);}
 public static void main(String[]a)throws Exception{cs=DexDump.load(a[0]);String F="Landroidx/fragment/app/Fragment;";String M="LX/00fk;";
 m("Lcom/instagram/mainactivity/InstagramMainActivity;","A1f",F,17);m("Landroidx/fragment/app/FragmentActivity;","A1Z",M,17);
 f("Lcom/instagram/mainactivity/InstagramMainActivity;","A08","LX/00M2;",9,1);f("LX/00M2;","A02","LX/004K;",9,1);f("LX/004K;","A03","Landroidx/fragment/app/FragmentActivity;",25,17);m("LX/004K;","A06","LX/08kM;",17);
 f("LX/08kM;","A03","Ljava/lang/String;",25,17);f("LX/08kM;","A0C","LX/08kM;",25,25);
 for(String n:new String[]{"A0L"})m(M,n,"I",17);m(M,"A15","Z",17);m(M,"A0O",F,17,"I");m(M,"A0Q",F,17,"Ljava/lang/String;");f(M,"mExecutingActions","Z",9,1);f(M,"A0W","Ljava/util/ArrayList;",25,17);f(M,"A0V","LX/00fz;",25,17);f("LX/00fz;","A01","Ljava/util/ArrayList;",25,17);
 m("LX/00VS;","A05",F,17);f("LX/00VS;","A07","LX/0JWL;",25,17);f("LX/0072;","A00","Ljava/lang/Object;",9,1);m("LX/02Wh;","A03",F,17);
 m("LX/052y;","A07",F,17);f("LX/052y;","A06","LX/0mBs;",9,1);m("LX/0mBs;","A05","I",17);
 f("LX/04Bv;","A00","Landroidx/viewpager2/widget/ViewPager2;",9,1);f("LX/04Bv;","A05","LX/0IBl;",9,1);f("LX/0IBl;","A04","Ljava/util/List;",9,1);f("Landroidx/viewpager2/widget/ViewPager2;","A00","I",9,1);m("Landroidx/viewpager2/widget/ViewPager2;","getAdapter","LX/04b8;",1);
 for(String t:new String[]{"LX/0072;","LX/0IBl;","LX/00VS;","LX/02Wh;","LX/052y;","LX/04Bv;","Landroidx/viewpager2/widget/ViewPager2;"})check(cs.get(t).getAccessFlags()==17,"class flags "+t);

 m("LX/004K;","A05",F,17);check(cs.get("LX/004K;").getAccessFlags()==17,"navigation final public");
 f("LX/08kM;","A08","LX/08kM;",25,25);check((cs.get("LX/08kM;").getAccessFlags()&0x4000)!=0,"tabs enum");
 for(String n:new String[]{"isAdded","isVisible","isResumed"})m(F,n,"Z",17);
 m(F,"getActivity","Landroidx/fragment/app/FragmentActivity;",17);m(F,"getParentFragment",F,17);
 m(F,"getParentFragmentManager",M,17);m(F,"getChildFragmentManager",M,17);m(F,"getView","Landroid/view/View;",1);
 f(F,"mFragmentId","I",9,1);f(F,"mTag","Ljava/lang/String;",9,1);
 m("LX/04Bv;","A00",F,25,"LX/04Bv;");
 m("LX/02Hr;","A0o","Lcom/instagram/clips/intf/ClipsViewerConfig;",17);
 f("Lcom/instagram/clips/intf/ClipsViewerConfig;","A00","Lcom/instagram/clips/intf/ClipsViewerSource;",9,1);
 check((cs.get("Lcom/instagram/clips/intf/ClipsViewerSource;").getAccessFlags()&0x4000)!=0,"config source enum");
 f("Lcom/instagram/mainactivity/InstagramMainActivity;","A09","LX/0A4v;",9,1);
 f("LX/0A4v;","A06","Ljava/lang/ref/WeakReference;",9,1);
 check(cs.get("LX/0A4v;").getAccessFlags()==17,"modal final public");check(cs.get("LX/03CG;").getAccessFlags()==17,"modal page final public");
 f("Lcom/instagram/base/activity/IgFragmentActivity;","A00","LX/00i6;",9,1);
 check(cs.get("LX/00i7;").getAccessFlags()==17&&cs.get("LX/00i7;").getSuperclass().equals("LX/00i6;"),"native layer final subclass");
 f("LX/00i7;","A0W","Z",9,1);m("LX/00i6;","A0N",F,17);
 f("LX/007r;","A00","Landroid/app/Dialog;",9,1);
 System.out.println("PASS actual452 reflection contracts: "+count+" assertions");}
}

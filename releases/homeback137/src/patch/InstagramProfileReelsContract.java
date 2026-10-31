package app.hiro.oneback.patches;

import java.util.*;
import com.android.tools.smali.dexlib2.iface.*;

/** Read-only contracts for observing native external-profile removal and
 * selecting/resetting the real Reels tab after Back. No routing implementation
 * is copied into the patch and no fragment factory is modified. */
public final class InstagramProfileReelsContract {
    private InstagramProfileReelsContract() {}
    private static final List<String> NONE=Collections.emptyList();
    private static void require(boolean value,String why) {
        if(!value)throw new IllegalStateException("Instagram452 profile-to-Reels contract changed: "+why);
    }
    private static ClassDef clazz(Map<String,ClassDef> classes,String name,int flags,String parent) {
        ClassDef value=classes.get(name);
        require(value!=null&&value.getAccessFlags()==flags&&parent.equals(value.getSuperclass()),name);return value;
    }
    private static void field(Map<String,ClassDef> classes,String owner,String name,String type,int flags) {
        ClassDef value=classes.get(owner);require(value!=null,owner);
        for(Field field:value.getFields())if(name.equals(field.getName())&&type.equals(field.getType())&&field.getAccessFlags()==flags)return;
        throw new IllegalStateException("Instagram452 profile-to-Reels field changed: "+owner+"->"+name);
    }
    private static void method(Map<String,ClassDef> classes,String owner,String name,List<String> args,String result,int flags,int registers,String digest) {
        ClassDef value=classes.get(owner);require(value!=null,owner);
        for(Method method:value.getMethods())if(OneBackPatch.signature(method,name,args,result)) {
            require(method.getAccessFlags()==flags&&method.getImplementation()!=null
                &&method.getImplementation().getRegisterCount()==registers,owner+"->"+name+" ABI");
            Method nativeBody=method;
            // Piko's live builder appends its prefix catch after the original
            // native catch records. Validate and remove that exact addition on
            // a copy before the strict native digest examines interval order.
            // The remaining native catches are never reordered or relaxed.
            if(InstagramProfileReelsPikoContract.hasStartupPrefix(method))
                nativeBody=InstagramProfileReelsPikoContract.original(classes,method);
            else if(!digest.equals(OneBackPatch.instagramNavigationDigest(nativeBody)))
                nativeBody=InstagramProfileReelsPikoContract.original(classes,method);
            require(digest.equals(OneBackPatch.instagramNavigationDigest(nativeBody)),owner+"->"+name);return;
        }
        throw new IllegalStateException("Instagram452 profile-to-Reels method missing: "+owner+"->"+name);
    }
    public static void verifyOriginal(Map<String,ClassDef> classes) {
        clazz(classes,"LX/00VS;",17,"LX/05Jd;");clazz(classes,"LX/02Wh;",17,"LX/0CTN;");
        clazz(classes,"LX/08kM;",16401,"Ljava/lang/Enum;");
        clazz(classes,"LX/00fn;",17,"LX/00fk;");
        field(classes,"Lcom/instagram/mainactivity/InstagramMainActivity;","A08","LX/00M2;",1);
        field(classes,"LX/00M2;","A02","LX/004K;",1);
        field(classes,"LX/004K;","A03","Landroidx/fragment/app/FragmentActivity;",17);
        field(classes,"LX/08kM;","A08","LX/08kM;",16409);
        field(classes,"LX/08kM;","A06","Lkotlin/enums/EnumEntries;",4121);
        field(classes,"LX/00fk;","A0G","Z",1);
        field(classes,"LX/00fk;","mExecutingActions","Z",1);
        field(classes,"LX/00fk;","A0W","Ljava/util/ArrayList;",17);
        method(classes,"Landroidx/fragment/app/FragmentActivity;","A1Z",NONE,"LX/00fk;",17,2,
            "fc6937fa31c842df7902b5fa86687aa1d39de8465c6a6957e5acf4e028bf5d62");
        method(classes,"Lcom/instagram/mainactivity/InstagramMainActivity;","HNv",Arrays.asList("LX/08kM;"),"V",17,5,
            "4375acac9c6fa2ca5fe833c6b942213a410561345ea00c82e268613dce698a4c");
        method(classes,"Lcom/instagram/mainactivity/InstagramMainActivity;","HNw",Arrays.asList("LX/08kM;"),"V",17,2,
            "f2b0f050748b94cbc94231df3c99a6a5d58926e4a230353a30c74ea2ec254c29");
        method(classes,"Lcom/instagram/mainactivity/InstagramMainActivity;","A2Z",Arrays.asList("LX/08kM;"),"V",17,6,
            "3c82c9aaae5e4edca0f2f4f263137b83d37c465c35610d1e0c156de3adc526ba");
        method(classes,"Lcom/instagram/mainactivity/InstagramMainActivity;","BwK",NONE,"LX/00fk;",17,4,
            "64b719bbbd2e940845a909b7934870c869b857ae185527de653acf6e557523c5");
        method(classes,"Lcom/instagram/mainactivity/InstagramMainActivity;","A14",Arrays.asList("Lcom/instagram/mainactivity/InstagramMainActivity;"),"Z",25,3,
            "d5392b93c2a2a30211c0f2b11ec0617a922219cf494bd10eb9d8725af0a94061");
        method(classes,"Lcom/instagram/mainactivity/InstagramMainActivity;","A0Z",Arrays.asList("Landroid/content/Intent;"),"V",18,6,
            "a932b31d943f386c90a6b08488b576781dc705147edf3946c7f8c7ed303dba4c");
        method(classes,"Lcom/instagram/mainactivity/InstagramMainActivity;","A0d",Arrays.asList("Landroid/os/Bundle;","LX/04r3;","LX/05Jc;","Lcom/instagram/mainactivity/InstagramMainActivity;","Ljava/lang/Class;","Ljava/lang/Object;","Ljava/lang/String;"),"V",9,69,
            "212c71be803a01acc4c4a030e99f7c484b3503c502ec3ca7110298a5e6641b6d");
        method(classes,"Lcom/instagram/mainactivity/InstagramMainActivity;","A0e",Arrays.asList("Landroid/os/Bundle;","LX/05Jc;","Lcom/instagram/mainactivity/InstagramMainActivity;","Ljava/lang/Class;","Ljava/lang/Object;"),"V",9,58,
            "be3d4b79f5b5cb4dc1d4a4389cea6e4c04997a9cce3f733c1a7dee4b43b0cc98");
        method(classes,"Lcom/instagram/mainactivity/InstagramMainActivity;","onNewIntent",Arrays.asList("Landroid/content/Intent;"),"V",17,21,
            "b0e0d8e4888529a2955e925b35fadae097f07e717958dd18f59decfc09bdeafb");
        method(classes,"LX/00M2;","A08",NONE,"LX/00VS;",17,3,
            "af39543872c1e85372264e165d1708f35274da0136574c7c7841af29aaa0f5c3");
        method(classes,"LX/00M2;","A0P",Arrays.asList("LX/08kM;","Ljava/lang/String;","Z"),"V",17,6,
            "b0b56f6d77fff8e1eb121c2d892f6b008f7c94eb221bda9255db23fc2268bb00");
        method(classes,"LX/00M2;","A04",Arrays.asList("LX/00M2;","LX/08kM;","Ljava/lang/String;","Z","Z"),"V",25,36,
            "f165cc66dcd74aeaa4d8e1ebf4fb2337d6489ec033bc325034d62144df072f29");
        method(classes,"LX/00M2;","A0O",Arrays.asList("LX/08kM;","LX/08kM;","Ljava/lang/Integer;"),"V",17,32,
            "10e52b195e119667b5e032c64ddf767a3868dd9ea5caac7becc986dae26bfb3b");
        method(classes,"LX/004K;","A05",NONE,"Landroidx/fragment/app/Fragment;",17,3,
            "7dac241fd44a6159e077a7d57dde260cff075d1d4b8edb3cbc8c34baa7283c88");
        method(classes,"LX/004K;","A06",NONE,"LX/08kM;",17,3,
            "6ac0e6d0c70b5641b4ed4a8981d49ef9bcf20de66e53a777fe60403b3e814764");
        method(classes,"LX/004K;","A07",Arrays.asList("LX/08kM;"),"V",17,4,
            "ef4a8d11cb5bd82223640125862b1bb5e958aa76bc909e74f1d159c156f04cc7");
        method(classes,"LX/004K;","A09",Arrays.asList("LX/08kM;","LX/08kM;","Z","Z","Z"),"V",17,14,
            "92e94cc1e37bc13dc2cc09a64332e901d064bfd1562f928ec86ce1b367fe94f9");
        method(classes,"LX/004K;","A01",Arrays.asList("LX/00fk;","LX/08kM;","Z"),"V",18,13,
            "6ef85d7c0e6ca40f81338bb1d1ab1c0dabaaf6e21ccdbb22f4e60859bbd0f302");
        method(classes,"LX/00fl;","A01",Arrays.asList("LX/00fk;"),"Z",25,3,
            "f9cbf48a7064e15e5f5d81966238d97556065fb6d7368949d9c2b59bf8db510e");
        method(classes,"LX/00fk;","A15",NONE,"Z",17,2,
            "1604a3136e1b22d181e4f46db2d5629ac6216649d4cbe7feb4745162f64da908");
        method(classes,"LX/00fk;","A0L",NONE,"I",17,4,
            "d1d71e7c1eb0ace06d2039a94b1784107edca0409f28db719215433995b08c7c");
        method(classes,"LX/00fk;","A0d",Arrays.asList("I"),"V",17,4,
            "923bb9522f65c5d0ce81e47220f5ac7fa957e49169963bb06d3853e942d64230");
        method(classes,"LX/00fk;","A0n",Arrays.asList("Landroidx/fragment/app/Fragment;"),"V",17,4,
            "504a9922353cd41846fd89a16b96fcf84f87a5e9c0f1e0b2290e402350ae8265");
        method(classes,"Landroidx/fragment/app/Fragment;","isAdded",NONE,"Z",17,2,
            "1fdef6c24298fcffbbd6ff66f6d070c4594e2adf73b5fb51b02c6480085bcdc6");
        method(classes,"Landroidx/fragment/app/Fragment;","isResumed",NONE,"Z",17,3,
            "b52aaf00fe720ad4be507c218f529db7708caef7bce4e233b2c5257f4d5a6424");
        method(classes,"Landroidx/fragment/app/Fragment;","isRemoving",NONE,"Z",17,2,
            "94867c35f081ecb884d896d650ef2b44c60eabc6dd132935c6630313863611b6");
        method(classes,"Landroidx/fragment/app/Fragment;","getChildFragmentManager",NONE,"LX/00fk;",17,3,
            "5012d7f74d1a8f3af0cfa8e1f63066b24822ec4928cf15ac3dd2d3fc0958f9ec");
        method(classes,"LX/09s2;","A01",Arrays.asList("Landroid/content/Context;","I"),"Landroid/content/Intent;",17,7,
            "0510a95a43bbf4f526062f59d205f1ad5ee3189ae439445af5bbf417b8f0279a");
        method(classes,"LX/08pj;","A00",Arrays.asList("Landroid/content/Context;"),"Landroid/content/Intent;",17,3,
            "5a29a1b04822c6cc523aae2223200055b0b250f59b04fe2cbff9a083bf7ecaef");
        method(classes,"LX/004E;","A03",Arrays.asList("Lcom/instagram/common/session/UserSession;","Ljava/lang/String;","Ljava/lang/String;","Ljava/util/List;"),"LX/08kM;",17,8,
            "a0ffd118ba228346d0b87e891286f98faa0e9e3d6261da02eaa42c3df7f5f8a2");
        method(classes,"LX/003Q;","A07",Arrays.asList("Landroid/content/Intent;","Lcom/instagram/common/session/UserSession;","Z"),"V",18,9,
            "16d841ff1b0f1553cdc22bcb080c693ab6000f8857f4c0a1637c3ada1314ce62");
        method(classes,"LX/003Q;","A03",Arrays.asList("Landroid/content/Intent;","Lcom/instagram/common/session/UserSession;","LX/0bA2;","LX/003Q;","LX/0Bem;","Ljava/lang/String;","Z"),"Ljava/lang/Integer;",25,22,
            "1645d9035228bf7beb732a64fb3c30c4b6c1251ac507895f59e8fb496830c57c");
        method(classes,"LX/0DaR;","A00",Arrays.asList("LX/00M2;","LX/08kM;","LX/00i6;"),"V",17,8,
            "39b0169e1dee3189e18f6a0fa68c20435013f0d8b897f25db22c7cf686c056c9");
        method(classes,"LX/00fk;","A0K",Arrays.asList("Ljava/lang/String;","I","I"),"Z",2,13,
            "3f208705002b09111e1fdccbac7978a144b43dafdd8fd1af08ee8f754954837a");
        method(classes,"Landroidx/fragment/app/Fragment;","initState",NONE,"V",1,4,
            "9142a5cad34144ddffe4de6031a8b387afc7cf87943367ccb156fcb3cb9b8bf9");
        method(classes,"Landroidx/fragment/app/Fragment;","getActivity",NONE,"Landroidx/fragment/app/FragmentActivity;",17,2,
            "6f5f57dd2b4ae169c13b5c886c527ec9728816b72e73a7738987df934dc30d31");
        method(classes,"Landroidx/fragment/app/Fragment;","getParentFragment",NONE,"Landroidx/fragment/app/Fragment;",17,2,
            "936dfbbae1c392042ef08c40dab8830fc9bd2778947d7d33d2b7ebbf74b48358");
        method(classes,"Landroidx/fragment/app/Fragment;","getView",NONE,"Landroid/view/View;",1,2,
            "97aca74cf2575284eabe98dda245840242e7dd5fa1a0b6fb46e8a4562fe17c3b");
        method(classes,"LX/0A4v;","A07",NONE,"V",17,27,
            "5fcab099f22f4c5d2486b1387644f45c6c435e3daf351aee25a7aa11e223a5cd");
        method(classes,"LX/08kM;","<clinit>",NONE,"V",65545,19,
            "963b0c8570baa0c11db047f3e268dd74e22ce85e831a148a58a4c3a359c05662");
        method(classes,"LX/08kM;","valueOf",Arrays.asList("Ljava/lang/String;"),"LX/08kM;",9,2,
            "3a6a3e494b730c095199f919d543626e701f133489fdbc7ee4eab523a98a9ef8");
        method(classes,"LX/08kM;","values",NONE,"[LX/08kM;",9,1,
            "f88bc6c99f497906d105cd52e6b8aad0f00fcfb091ddbe1ef3e39f507c2211f6");
    }
}

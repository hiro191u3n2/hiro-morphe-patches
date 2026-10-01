package com.ss.android.vesdk;
import java.util.ArrayList;
/** Original test double. Only data fields are represented; constant values match independently checked public stock DEX facts. */
public final class VEEffectParams {
 public static int EFFECT_TYPE_SET_COMPOSER_WITH_TAG=0,EFFECT_TYPE_APPEND_COMPOSER_WITH_TAG=2,EFFECT_TYPE_RELOAD_COMPOSER_WITH_TAG=1,EFFECT_TYPE_REPLACE_COMPOSER_WITH_TAG=3;
 public int TYPE,intValueOne,intValueTwo,intValueThree;
 public boolean boolValueOne,boolValueTwo,boolValueThree;
 public float floatValueOne,floatValueTwo,floatValueThree;
 public String stringValueOne="",stringValueTwo="",stringValueThree="";
 public ArrayList<Boolean> boolArrayValue=new ArrayList<>();
 public ArrayList<Float> floatArrayValue=new ArrayList<>();
 public ArrayList<Integer> intArrayValue=new ArrayList<>();
 public ArrayList<String> stringArrayOne=new ArrayList<>(),stringArrayTwo=new ArrayList<>(),stringArrayThree=new ArrayList<>();
}

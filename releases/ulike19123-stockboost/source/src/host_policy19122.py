#!/usr/bin/env python3
"""Execute one-shot policy transitions; guard the unchanged normal pixel path.

The source-delta proof is deliberately not a device or encoded-image proof.
It rejects any normal pipeline edit outside the explicit diagnostic branch.
"""
from pathlib import Path
import argparse, hashlib, json, subprocess

def fixtures():
    return {
      'android/content/DialogInterface.java': 'package android.content;public interface DialogInterface{public interface OnClickListener{void onClick(DialogInterface d,int w);}}',
      'android/app/Activity.java': 'package android.app;public class Activity{public boolean finishing,destroyed,focus=true;public boolean isFinishing(){return finishing;}public boolean isDestroyed(){return destroyed;}public boolean hasWindowFocus(){return focus;}}',
      'android/preference/Preference.java': '''package android.preference;public class Preference{public interface OnPreferenceClickListener{boolean onPreferenceClick(Preference p);}public String key;public CharSequence title,summary;public OnPreferenceClickListener listener;public Preference(Object a){}public void setKey(String v){key=v;}public void setTitle(CharSequence v){title=v;}public void setSummary(CharSequence v){summary=v;}public void setOnPreferenceClickListener(OnPreferenceClickListener v){listener=v;}}''',
      'android/preference/PreferenceScreen.java': '''package android.preference;import java.util.*;public class PreferenceScreen extends Preference{public final List<Preference> rows=new ArrayList<Preference>();public PreferenceScreen(){super(null);}public boolean addPreference(Preference p){rows.add(p);return true;}}''',
      'android/preference/PreferenceActivity.java': '''package android.preference;public class PreferenceActivity extends android.app.Activity{public PreferenceScreen screen=new PreferenceScreen();public PreferenceScreen getPreferenceScreen(){return screen;}public Preference findPreference(String key){if(screen==null)return null;for(Preference p:screen.rows)if(key.equals(p.key))return p;return null;}}''',
      'android/app/AlertDialog.java': '''package android.app;import android.content.DialogInterface;public class AlertDialog{public static int shows;public static CharSequence[] items;public static DialogInterface.OnClickListener selected;public static final class Builder{public Builder(Object a){}public Builder setTitle(CharSequence t){return this;}public Builder setItems(CharSequence[] i,DialogInterface.OnClickListener s){items=i;selected=s;return this;}public Builder setNegativeButton(CharSequence t,DialogInterface.OnClickListener s){return this;}public AlertDialog show(){shows++;return new AlertDialog();}}}'''
    }

def execute(source,work,jdk,policy=None,expect=True):
    values=fixtures();paths=[]
    for name,body in values.items():
        p=work/'fixtures'/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(body);paths.append(p)
    classes=work/'classes';classes.mkdir(parents=True,exist_ok=True)
    r=subprocess.run([str(jdk/'bin/javac'),'--release','8','-encoding','UTF-8','-d',str(classes),str(policy or source/'CapturePolicy19122.java'),str(source/'tests19122/policy/CapturePolicy19122Test.java'),*map(str,paths)],capture_output=True,text=True,timeout=60)
    (work/'compile.log').write_text(r.stdout+r.stderr)
    if r.returncode:raise AssertionError(r.stderr)
    r=subprocess.run([str(jdk/'bin/java'),'-ea','-cp',str(classes),'com.hiro.ulike.CapturePolicy19122Test'],capture_output=True,text=True,timeout=60)
    (work/'execution.log').write_text(r.stdout+r.stderr)
    if expect and r.returncode:raise AssertionError(r.stdout+r.stderr)
    if not expect and not r.returncode:raise AssertionError('Policy negative control escaped')
    return json.loads(r.stdout.strip().splitlines()[-1]) if expect else None

def normal_path_guard(source):
    current=(source/'QualityPipeline1932.java').read_text()
    reference=(source/'tests19122/policy/QualityPipeline1932.reference121.java').read_text()
    expected='232b27ad67ad77c91a6fbb166a55d0a1bff20a1e35adb150f334ae819d6a2283'
    if hashlib.sha256(reference.encode()).hexdigest()!=expected:raise AssertionError('Published121 pipeline reference changed')
    start=current.index('            if (CapturePolicy19122.bypassProcessing()) {')
    end=current.index('            final PhotoDetail.Settings requested',start)
    branch=current[start:end]
    if current[:start]+current[end:]!=reference:
        raise AssertionError('Normal pipeline changed outside the explicit one-photo diagnostic guard')
    for text in ['SaveQuality2.output186(input.getWidth(),input.getHeight(),diagnosticTurn,fixed245)',
                 'working=timedResize(input,diagnosticTurn,diagnosticSize[0],diagnosticSize[1],trace)',
                 'PREPARED.put(working,new Prepared(input))','return working;',
                 '通常の画質ではありません','診断用・追加画質処理を省略']:
        if text not in branch:raise AssertionError('Diagnostic contract missing: '+text)
    if current.index('if (!readable(input))')>start:raise AssertionError('Unreadable input must retain fallback')
    return {'normal_path_source_identical_after_removing_guard':True,
      'baseline_sha256':hashlib.sha256(reference.encode()).hexdigest(),
      'diagnostic_branch_only':True,'encoded_output_equivalence_measured':False,
      'assertions':8}

def test(source,work,jdk):
    source,work,jdk=map(lambda v:Path(v).resolve(),(source,work,jdk));work.mkdir(parents=True,exist_ok=True)
    tracked=[source/'CapturePolicy19122.java',source/'QualityPipeline1932.java',source/'tests19122/policy/QualityPipeline1932.reference121.java',source/'tests19122/policy/CapturePolicy19122Test.java',source/'host_policy19122.py']
    pins={str(p.relative_to(source)):hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}
    report=execute(source,work/'production',jdk)
    original=(source/'CapturePolicy19122.java').read_text()
    mutants={'normal-bypass':('return active==6;','return active==0;'),
      'persistent-comparison':('active=pending;pending=0;','active=pending;'),
      'live-policy-mutation':('pending=mode;return true;','pending=mode;active=mode;return true;')}
    negatives=[]
    for name,(old,new) in mutants.items():
        if original.count(old)!=1:raise AssertionError('Missing policy mutant '+name)
        p=work/'mutants'/name/'CapturePolicy19122.java';p.parent.mkdir(parents=True,exist_ok=True);p.write_text(original.replace(old,new))
        execute(source,work/name,jdk,p,False);negatives.append({'name':name,'status':'rejected'})
    guard=normal_path_guard(source);report['assertions']+=guard['assertions'];report['normal_quality_guard']=guard
    if pins!={str(p.relative_to(source)):hashlib.sha256(p.read_bytes()).hexdigest() for p in tracked}:raise AssertionError('Policy test source changed during execution')
    report.update(negative_controls=negatives,source_sha256=pins,physical_android_tested=False,device_speedup_verified=False)
    (work/'result.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');return report
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk',required=True);a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk),ensure_ascii=False))

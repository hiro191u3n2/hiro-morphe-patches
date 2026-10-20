#!/usr/bin/env python3
"""Compile and exercise the actual changed timing helper with Android fixtures."""
from pathlib import Path
import argparse
import hashlib
import json
import re
import shutil
import subprocess

ROOT = Path(__file__).resolve().parent

STUBS = {
    'android/content/SharedPreferences.java': '''package android.content;
public interface SharedPreferences {
 long getLong(String key,long fallback); String getString(String key,String fallback);
 Editor edit(); interface Editor {Editor putString(String key,String value);Editor putLong(String key,long value);void apply();}
}''',
    'android/content/Context.java': '''package android.content;
public class Context {public static final int MODE_PRIVATE=0;
 public Context getApplicationContext(){return this;}
 public SharedPreferences getSharedPreferences(String name,int mode){throw new UnsupportedOperationException();}
}''',
    'android/app/Activity.java': '''package android.app;
public class Activity extends android.content.Context {public void runOnUiThread(Runnable work){work.run();}}
''',
    'android/preference/Preference.java': '''package android.preference;
public class Preference {
 public interface OnPreferenceClickListener {boolean onPreferenceClick(Preference value);}
 public String getKey(){return null;}public CharSequence getTitle(){return null;}
 public void setTitle(CharSequence value){}public void setSummary(CharSequence value){}
 public void setOnPreferenceClickListener(OnPreferenceClickListener value){}
}''',
    'android/preference/PreferenceGroup.java': '''package android.preference;
public class PreferenceGroup extends Preference {public int getPreferenceCount(){return 0;}public Preference getPreference(int index){return null;}}
''',
    'android/preference/PreferenceActivity.java': '''package android.preference;
public class PreferenceActivity extends android.app.Activity {public PreferenceGroup getPreferenceScreen(){return null;}}
''',
    'com/hiro/ulike/TimingFixtureHelpers.java': '''package com.hiro.ulike;
import android.content.Context;
final class PerformanceHints1952 {static void init(Context context){}}
final class WholeRoute1953 {
 static void initialize(Context context){}static void foregroundStarted(){}
 static void saved(ProcessingTiming1947.Trace trace,boolean success){}static void wake(){}
}
final class GpuQualification1961 {
 static volatile String status="GPU資格: 未確認";static int statusCalls;static String wakeStatus;
 static void initialize(Context context){}static void captureChanged(){}
 static String status1967(){statusCalls++;return status;}
 static void wake(){if(wakeStatus!=null)status=wakeStatus;}
}
''',
}

def _java(jdk):
    if jdk is None:
        return Path(shutil.which('java') or '/usr/lib/jvm/java-17-openjdk-amd64/bin/java')
    location = Path(jdk)
    if location.is_dir():
        return location / 'bin/java'
    return location

def test(source, work, jdk=None):
    source, work = Path(source).resolve(), Path(work).resolve()
    if source.is_dir():
        source = source / 'ProcessingTiming1947.java'
    if not source.is_file():
        raise FileNotFoundError(source)
    work.mkdir(parents=True, exist_ok=True)
    fixture = work / 'fixture'
    classes = work / 'classes'
    if classes.exists():
        shutil.rmtree(classes)
    classes.mkdir()
    paths = []
    for relative, text in STUBS.items():
        path = fixture / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding='utf-8')
        paths.append(path)
    harness = ROOT / 'Timing1970Test.java'
    java = _java(jdk)
    commands = [
        ('compile', [str(java), '-m', 'jdk.compiler/com.sun.tools.javac.Main',
                     '-source', '8', '-target', '8', '-Xlint:-options', '-encoding', 'UTF-8',
                     '-d', str(classes), str(source), str(harness), *map(str, paths)]),
        ('run', [str(java), '-XX:ActiveProcessorCount=4', '-cp', str(classes),
                 'com.hiro.ulike.Timing1970Test']),
    ]
    for name, command in commands:
        result = subprocess.run(command, capture_output=True, text=True, timeout=60)
        (work / (name + '.log')).write_text(result.stdout + result.stderr, encoding='utf-8')
        if result.returncode:
            raise RuntimeError((work / (name + '.log')).read_text(encoding='utf-8'))
    match = re.search(r'^RESULT (\{[^\n]+\})$', result.stdout, re.M)
    if not match:
        raise RuntimeError('Executed timing regression result absent')
    report = json.loads(match.group(1))
    report['production_source_sha256'] = hashlib.sha256(source.read_bytes()).hexdigest()
    report['scope'] = 'Actual ProcessingTiming1947 source with controlled Android storage/qualification fixtures; host Java tests, not Android or physical GPU execution.'
    (work / 'result.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    return report

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', default=str(ROOT / 'ProcessingTiming1947.java'))
    parser.add_argument('--work', default=str(ROOT / 'work'))
    parser.add_argument('--jdk')
    args = parser.parse_args()
    print(json.dumps(test(args.source, args.work, args.jdk), ensure_ascii=False, indent=2))

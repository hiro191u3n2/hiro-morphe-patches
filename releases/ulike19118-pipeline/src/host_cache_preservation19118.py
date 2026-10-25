#!/usr/bin/env python3
"""Exercise .117 -> .118 Strong tile8 certificate restoration.

This is a host persistence/admission test, not a physical Android GPU or speed
measurement. The native compute fingerprint and every input to it must remain
identical before a certificate written by .117 can authorize the .118 route.
"""
from __future__ import annotations

import argparse
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parent
REFERENCE = ROOT.parent.parent / "ulike19117-pipeline" / "src"


def require(ok: bool, message: str) -> None:
    if not ok:
        raise AssertionError(message)


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def fingerprint(source: Path) -> tuple[str, dict[str, str]]:
    script = source / "native1960" / "build_native1960.py"
    spec = importlib.util.spec_from_file_location("fingerprint_" + source.parent.name, script)
    assert spec and spec.loader
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module.compute_fingerprint1990(source / "native1960")


def without_class(source: str, name: str) -> str:
    marker = "final class " + name + " {"
    first = source.find(marker)
    require(first >= 0, "Missing fixture class " + name)
    opening = source.find("{", first)
    depth = 0
    for i in range(opening, len(source)):
        if source[i] == "{":
            depth += 1
        elif source[i] == "}":
            depth -= 1
            if depth == 0:
                return source[:first] + source[i + 1 :]
    raise AssertionError("Unclosed fixture class " + name)


CONTEXT = """package android.content;
public class Context {
 public static final int MODE_PRIVATE=0;
 private static SharedPreferences preferences;
 public Context getApplicationContext(){return this;}
 public SharedPreferences getSharedPreferences(String name,int mode){
  if(preferences==null)preferences=new SharedPreferences();return preferences;
 }
}
"""

PREFERENCES = """package android.content;
import java.io.*;import java.util.*;
public class SharedPreferences {
 private final Properties values=new Properties();
 private final File file=new File(System.getProperty("certificate.store"));
 public SharedPreferences(){if(file.isFile())try(FileInputStream input=new FileInputStream(file)){
  values.load(input);
 }catch(IOException failure){throw new IllegalStateException(failure);}}
 public synchronized String getString(String key,String fallback){return values.getProperty(key,fallback);}
 public synchronized Map<String,?> getAll(){Map<String,String> copy=new HashMap<>();for(String k:values.stringPropertyNames())copy.put(k,values.getProperty(k));return copy;}
 public Editor edit(){return new Editor();}
 public class Editor {
  private final Map<String,String> changes=new HashMap<>();
  public Editor putString(String key,String value){changes.put(key,value);return this;}
  public Editor remove(String key){changes.put(key,null);return this;}
  public void apply(){synchronized(SharedPreferences.this){
   for(Map.Entry<String,String> entry:changes.entrySet()){
    if(entry.getValue()==null)values.remove(entry.getKey());else values.setProperty(entry.getKey(),entry.getValue());
   }
   try(FileOutputStream output=new FileOutputStream(file)){values.store(output,"cross-release test");}
   catch(IOException failure){throw new IllegalStateException(failure);}
  }}
 }
}
"""

QUALIFICATION = """package com.hiro.ulike;
final class GpuQualification1961 {
 static String environment;
 static boolean optional,preFirst;
 static int scheduled;
 static String environment1990(){return environment;}
 static void corpusCertificate1994(){}
 static boolean preFirstStrongIdle19117(){return preFirst;}
 static boolean optionalStrongBenchmarksAllowed19117(){return optional||preFirst;}
 static boolean exactRejected(String key){return false;}
 static void outcome1984(String reason){}
 static void exception1993(Throwable problem){}
 interface Cancellation {boolean cancelled();}
 interface Probe {void run(Cancellation cancellation);void close();}
 static boolean schedule(String key,long bytes,Probe probe){scheduled++;probe.close();return false;}
}
"""

TEST = """package com.hiro.ulike;
import android.content.Context;
public final class CrossReleaseCertificate19118Test {
 static int assertions;
 static void check(boolean ok,String why){assertions++;if(!ok)throw new AssertionError(why);}
 public static void main(String[] args){
  String action=args[0],variant=args[1];GpuQualification1961.environment=args[2];
  GpuCertificates1990.initialize(new Context());
  String standard=GpuStrongDefault1990.SUITE;
  String exact=GpuStrongDefault1990.Adaptive1994.EXACT;
  String fast=GpuStrongDefault1990.Adaptive1994.FAST;
  if("write".equals(action)){
   check(GpuCertificates1990.passed(standard,68,100000,1000,800),"standard proof write");
   if(!"cancel".equals(variant)){
    check(GpuCertificates1990.passed(exact,70,200000,1000,500),"exact proof write");
    if(!"missing-fast".equals(variant))check(GpuCertificates1990.passed(fast,70,200000,1000,500),"speed proof write");
    if("reject".equals(variant))GpuCertificates1990.reject(exact,"actual pixel mismatch");
   }else{
    GpuQualification1961.optional=false;GpuQualification1961.preFirst=false;
    GpuStrongDefault1990.Adaptive1994.request();
    check(GpuQualification1961.scheduled==0,"cancelled pre-first work cannot start new proof");
   }
   System.out.println("{\\"status\\":\\"written\\",\\"assertions\\":"+assertions+"}");return;
  }
  if("environment-changed".equals(variant)){
   check(GpuCertificates1990.restore(standard)==null,"driver/app/source change rejects even standard proof");
   check(GpuStrongDefault1990.profile1994(3060,4080)==4,"changed identity cannot reuse tile8");
   System.out.println("{\\"status\\":\\"identity-rejected\\",\\"assertions\\":"+assertions+",\\"profile\\":4}");return;
  }
  check(GpuCertificates1990.restore(standard)!=null,"exact standard environment restored");
  int expected="full".equals(variant)?7:4;
  check(GpuStrongDefault1990.profile1994(3060,4080)==expected,"first portrait stage route");
  check(GpuStrongDefault1990.profile1994(4080,3060)==expected,"first landscape stage route");
  if(expected==7){
   check(GpuCertificates1990.restore(exact)!=null&&GpuCertificates1990.restore(fast)!=null,
    "both finite exact and speed records required");
   check(GpuQualification1961.scheduled==0,"restoration executed no candidate benchmark");
  }else check(GpuCertificates1990.restore(exact)==null||GpuCertificates1990.restore(fast)==null,
   "unproven candidate has no complete authority");
  System.out.println("{\\"status\\":\\"restored\\",\\"assertions\\":"+assertions+",\\"profile\\":"+expected+"}");
 }
}
"""


def write_file(root: Path, name: str, content: str) -> Path:
    path = root / name
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content)
    return path


def compile_version(source: Path, output: Path, javac: Path) -> None:
    output.mkdir(parents=True, exist_ok=True)
    fixtures = output / "fixtures"
    peers = (source / "tests1994/strong/AdaptivePeers1994.java").read_text()
    peers = without_class(without_class(peers, "GpuCertificates1990"), "GpuQualification1961")
    files = [
        write_file(fixtures, "android/content/Context.java", CONTEXT),
        write_file(fixtures, "android/content/SharedPreferences.java", PREFERENCES),
        write_file(fixtures, "com/hiro/ulike/GpuQualification1961.java", QUALIFICATION),
        write_file(fixtures, "com/hiro/ulike/AdaptivePeers1994.java", peers),
        write_file(fixtures, "com/hiro/ulike/CrossReleaseCertificate19118Test.java", TEST),
        source / "GpuCertificates1990.java",
        source / "GpuStrongDefault1990.java",
    ]
    subprocess.run([str(javac), "--release", "8", "-d", str(output / "classes"), *(str(f) for f in files)], check=True)


def run_java(java: Path, classes: Path, store: Path, action: str, variant: str, environment: str) -> dict:
    cmd = [str(java), "-Dcertificate.store=" + str(store), "-cp", str(classes),
           "com.hiro.ulike.CrossReleaseCertificate19118Test", action, variant, environment]
    done = subprocess.run(cmd, check=True, text=True, capture_output=True, timeout=30)
    return json.loads(done.stdout)


def test(reference: Path, source: Path, work: Path, jdk: Path | None, native_so: Path | None) -> dict:
    reference, source, work = reference.resolve(), source.resolve(), work.resolve()
    require(reference != source, "Cross-release source trees must differ")
    before, before_pins = fingerprint(reference)
    after, after_pins = fingerprint(source)
    require(before == after and before_pins == after_pins, "Native/numerical implementation changed: proof cannot be carried over")
    for name in ("GpuCertificates1990.java", "GpuQualification1961.java", "GpuStrongDefault1990.java"):
        require(sha(reference / name) == sha(source / name), "Qualification/suite implementation changed: " + name)
    manifest = json.loads((source / "native1960/compute_dependencies1990.json").read_text())
    require("GpuStartup1990.java" in manifest["excluded_runtime_java"] and
            "GpuStartup1990.java" not in manifest["numerical_java"], "Startup scheduling must remain outside compute identity")
    startup = (source / "GpuStartup1990.java").read_text()
    require(re.search(r"STANDARD\s*=\s*\{[^}]*\b48\b", startup) is not None and
            startup.index("supports(STANDARD[i])") < startup.index("coreReady=true"),
            "Tile8 program must be compiled before publishing standard GPU readiness")
    baseline_qa = json.loads((reference.parent / "QA_ULike_v1.9.117.json").read_text())
    expected_native = baseline_qa["gpu_native"]["sha256"]
    if native_so is not None:
        require(sha(native_so) == expected_native, "Built native GPU library changed: old device proof invalid")

    work.mkdir(parents=True, exist_ok=True)
    # Show that the inclusion boundary is real: a changed numerical class
    # invalidates the native identity, while this startup-only change does not.
    mutated = work / "mutated-source"
    if mutated.exists():shutil.rmtree(mutated)
    shutil.copytree(source, mutated)
    numerical = mutated / "GpuStrongDefault1990.java"
    numerical.write_bytes(numerical.read_bytes() + b"\n")
    changed, _ = fingerprint(mutated)
    require(changed != after, "Numerical source mutation failed to invalidate device proof")
    numerical.write_bytes((source / "GpuStrongDefault1990.java").read_bytes())
    runtime = mutated / "GpuStartup1990.java"
    runtime.write_bytes(runtime.read_bytes() + b"\n")
    excluded, _ = fingerprint(mutated)
    require(excluded == after, "Startup-only mutation incorrectly invalidated numerical proof")
    shutil.rmtree(mutated)
    javac = jdk / "bin/javac" if jdk else Path(shutil.which("javac") or "/missing-javac")
    java = jdk / "bin/java" if jdk else Path(shutil.which("java") or "/missing-java")
    require(javac.is_file() and java.is_file(), "JDK with javac required for cross-process proof test")
    old_classes, new_classes = work / "v117/classes", work / "v118/classes"
    compile_version(reference, work / "v117", javac)
    compile_version(source, work / "v118", javac)
    cases = []
    original_environment = "build-fingerprint|sdk|com.gorgeous.liteinternational|5.6.2:740|GX1961:driver|source=" + before
    for variant in ("full", "missing-fast", "reject", "cancel"):
        store = work / (variant + ".properties")
        if store.exists():store.unlink()
        written = run_java(java, old_classes, store, "write", variant, original_environment)
        restored = run_java(java, new_classes, store, "read", variant, original_environment)
        require(restored["profile"] == (7 if variant == "full" else 4), "Wrong restored route: " + variant)
        cases.append({"scenario": variant, "write": written, "restore": restored})
        if variant == "full":
            for changed in ("driver-changed", "source=" + hashlib.sha256(b"different source").hexdigest(), "app-5.6.3"):
                result = run_java(java, new_classes, store, "read", "environment-changed", original_environment + "|" + changed)
                require(result["profile"] == 4, "Changed environment reused an old proof")
                cases.append({"scenario": changed, "restore": result})
    result = {
        "status": "passed", "compute_fingerprint": before, "compute_inputs": len(before_pins),
        "expected_gpu_native_sha256": expected_native, "built_native_checked": native_so is not None,
        "assertions": 2 + sum(row.get("write", {}).get("assertions", 0) + row["restore"]["assertions"] for row in cases),
        "cases": cases, "physical_android_tested": False, "device_save_speed_verified": False,
    }
    (work / "result.json").write_text(json.dumps(result, indent=2) + "\n")
    return result


if __name__ == "__main__":
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--reference", type=Path, default=REFERENCE)
    p.add_argument("--source", type=Path, default=ROOT)
    p.add_argument("--work", type=Path)
    p.add_argument("--jdk", type=Path)
    p.add_argument("--native-so", type=Path)
    args = p.parse_args()
    if args.work:
        report = test(args.reference, args.source, args.work, args.jdk, args.native_so)
    else:
        with tempfile.TemporaryDirectory(prefix="ulike-cross-release-19118-") as temp:
            report = test(args.reference, args.source, Path(temp), args.jdk, args.native_so)
    print(json.dumps(report, ensure_ascii=False))

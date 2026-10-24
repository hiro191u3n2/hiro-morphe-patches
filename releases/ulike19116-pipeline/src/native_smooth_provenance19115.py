#!/usr/bin/env python3
"""Bind the reviewed .115 NEON gather delta to complete ARM64 JNI rebuilds.

The published .114 ELF and every frozen smooth dependency are independently
pinned. This build proof makes no numerical-equivalence or device-speed claim.
"""
from pathlib import Path
import copy, json, re, shutil, struct, subprocess, tempfile
from native_identity1995 import need, sha, sections
from native_identity19101 import exact_equal
import source_inverse19115

SCHEMA = 'ulike19115-smooth-native-source-and-rebuild-v1'
REFERENCE = 'tests19115/provenance/reference114'
LIBRARY = 'libulike_smooth1958.so'
NDK_REVISION = '27.2.12479018'
BASELINE_SHA256 = '2c7634030f2e6c2177c033f931f6b7976c76da465fdada29ea5023ce088f4974'
BASELINE_BYTES = 33128
BASELINE_MPP_SHA256 = 'b3f688b09ddeb3e2756cb68669589186fa348f7eab098255cafe80cf0328e8d3'
BASELINE_ENTRY = 'ulike1958/runtime/libulike_smooth1958.so'
BASELINE_COMMIT = 'cb901596532d0566716fc1ae00577e198c4d1c91'
BASELINE_SOURCES = {
    'StrongNoise1958.java': '0adad39df071e47c58bc47d99e39ff1725ddb11c923ed3144c9c7aaaf74e33b4',
    'native1958/build_native1958.py': '8bd306d74b637d9da9eea363f18047b94506422b8842e8819920d3e2abc8a93c',
    'native1958/neon1959.h': '0f5f589a2d641e0929541a0c509af0212ddb8ec15e2a8b87b2f11b6d9d917b04',
    'native1958/scratch1959.h': '9b61f46bc2b0b4e3c86ea19a88f2719166e0611abbb4e3dea9328a7580c5bb9e',
    'native1958/smooth_noise1958.c': 'f0d6d2cd68d5f55194d96a2268177ade0e634c79849120e1aa16061167d854d6',
}
CHANGED = ('StrongNoise1958.java', 'native1958/neon1959.h')
FLAGS = ['-O3', '-std=c11', '-Wall', '-Wextra', '-Werror', '-ffp-contract=off',
         '-fno-fast-math', '-pthread', '-fPIC', '-fvisibility=hidden', '-shared',
         '-Wl,--no-undefined', '-Wl,-z,max-page-size=16384']
EXPORTS = sorted('Java_com_hiro_ulike_StrongNoise1958_' + name for name in
                 ('nativeAbi', 'processNative', 'processNativeShared',
                  'nativeScratchBytes', 'nativeReleaseScratch'))
IMPORTS = sorted(('__cxa_finalize', '__cxa_atexit', '__register_atfork', 'memset',
                  'pthread_mutex_lock', 'pthread_mutex_unlock', 'realloc', 'free'))


def frozen_sources(source):
    source = Path(source)
    for name, digest in BASELINE_SOURCES.items():
        path = source / REFERENCE / name
        need(path.is_file() and not path.is_symlink() and sha(path.read_bytes()) == digest,
             'Frozen114 smooth dependency differs: ' + name)
    return dict(BASELINE_SOURCES)


def scope(source):
    source = Path(source); frozen_sources(source); current = {}; changed = []
    for name, digest in BASELINE_SOURCES.items():
        path = source / name
        need(path.is_file() and not path.is_symlink(), 'Missing smooth dependency: ' + name)
        raw = path.read_bytes(); current[name] = sha(raw)
        if current[name] != digest:
            need(name in CHANGED, 'Unreviewed smooth source delta: ' + name)
            need(sha(source_inverse19115.restore(source, name, raw)) == digest,
                 'Complete smooth source inverse differs')
            changed.append(name)
    need(tuple(changed) == CHANGED, 'Exact NEON gather and CPU observation deltas required')
    # Include closure is fixed by the exact C/header and builder pins above.
    return dict(current_source_sha256=current, baseline_source_sha256=dict(BASELINE_SOURCES),
                changed_sources=changed, complete_source_inverse_verified=True,
                complete_source_inverse_manifest_sha256=source_inverse19115.MANIFEST_SHA256)


def inspect(raw):
    need(type(raw) is bytes, 'Immutable smooth ELF bytes required')
    table = sections(raw)
    need(struct.unpack_from('<H', raw, 16)[0] == 3 and struct.unpack_from('<I', raw, 20)[0] == 1,
         'AArch64 shared-library ELF required')
    need(struct.unpack_from('<Q', raw, 24)[0] == 0 and struct.unpack_from('<I', raw, 48)[0] == 0,
         'Unexpected entry point or architecture flags')
    need('.text' in table and table['.text'][1] == 1 and table['.text'][2] == 6,
         'Executable read-only text required')
    need('.symtab' not in table, 'Release smooth ELF must be stripped')
    phoff = struct.unpack_from('<Q', raw, 32)[0]
    ehsize, phsize, phcount = struct.unpack_from('<HHH', raw, 52)
    need(ehsize == 64 and phsize == 56 and 0 < phcount <= 64 and
         phoff >= 64 and phoff + phsize * phcount <= len(raw), 'Invalid smooth program table')
    loads = []; stack = []; relro = []
    for i in range(phcount):
        row = struct.unpack_from('<IIQQQQQQ', raw, phoff + phsize * i)
        kind, flags, offset, address, _, filesz, memsz, align = row
        need(offset + filesz <= len(raw) and filesz <= memsz, 'Invalid smooth segment extent')
        if kind == 1:
            need(align == 16384 and offset % align == address % align,
                 'Every LOAD must preserve 16KiB page alignment')
            need(flags in (4, 5, 6), 'Writable executable or invalid LOAD flags')
            loads.append(dict(flags=flags, alignment=align))
        elif kind == 0x6474e551: stack.append(flags)
        elif kind == 0x6474e552: relro.append(flags)
    need(loads == [dict(flags=f, alignment=16384) for f in (4, 5, 6, 6)],
         'Unexpected smooth load-segment layout')
    need(stack == [6] and relro == [4], 'Non-executable stack and GNU RELRO required')
    def data(name, kind):
        need(name in table and table[name][1] == kind, 'Missing typed ELF section: ' + name)
        row = table[name]; return raw[row[4]:row[4] + row[5]]
    strings = data('.dynstr', 3)
    def string(offset):
        need(0 <= offset < len(strings), 'Dynamic string offset out of range')
        end = strings.find(b'\0', offset); need(end >= offset, 'Unterminated dynamic string')
        return strings[offset:end].decode('ascii')
    symbols = data('.dynsym', 11)
    need(table['.dynsym'][9] == 24 and len(symbols) % 24 == 0, 'Invalid dynamic symbols')
    exports = []; imports = []; text = table['.text']
    for i in range(0, len(symbols), 24):
        name, info, visibility, index, address, size = struct.unpack_from('<IBBHQQ', symbols, i)
        if not name: continue
        label = string(name)
        need(info == 18 and visibility == 0, 'Only default-visible global function imports/exports allowed')
        if index == 0: imports.append(label)
        else:
            need(size > 0 and text[3] <= address and address + size <= text[3] + text[5],
                 'JNI export outside complete executable text')
            exports.append(label)
    need(sorted(exports) == EXPORTS and sorted(imports) == IMPORTS, 'Exact JNI exports/imports changed')
    dynamic = data('.dynamic', 6); need(len(dynamic) % 16 == 0, 'Invalid dynamic entries')
    entries = [struct.unpack_from('<qQ', dynamic, i) for i in range(0, len(dynamic), 16)]
    needed = [string(value) for tag, value in entries if tag == 1]
    need(needed == ['libm.so', 'libdl.so', 'libc.so'], 'Unexpected smooth dynamic dependency')
    need((30, 8) in entries and (0x6ffffffb, 1) in entries and not any(tag == 22 for tag, _ in entries),
         'Immediate binding without text relocations required')
    note = data('.note.android.ident', 7)
    expected_note = struct.pack('<III', 8, 132, 1) + b'Android\0' + struct.pack('<I', 26)
    expected_note += b'r27c'.ljust(64, b'\0') + b'12479018'.ljust(64, b'\0')
    need(note == expected_note, 'Exact API26 / NDKr27c Android identification required')
    return dict(elf_class=64, architecture='AArch64', abi='arm64-v8a', min_sdk=26,
                jni_exports=EXPORTS, imports=IMPORTS, dynamic_libraries=needed,
                load_segments=loads, non_executable_stack=True, relro=True,
                bind_now=True, text_relocations=False, stripped=True)


def expected(source, smooth):
    graph = scope(source); shape = inspect(smooth)
    need(sha(smooth) != BASELINE_SHA256, 'Changed smooth ELF must differ from published114')
    return dict(schema=SCHEMA, status='passed', library=LIBRARY, native_abi=1958,
                baseline_release='ulike-v1.9.114', baseline_source_commit=BASELINE_COMMIT,
                baseline_mpp_sha256=BASELINE_MPP_SHA256, baseline_mpp_entry=BASELINE_ENTRY,
                baseline_sha256=BASELINE_SHA256, baseline_bytes=BASELINE_BYTES,
                current_sha256=sha(smooth), current_bytes=len(smooth),
                baseline_rebuild_sha256=BASELINE_SHA256, current_rebuild_sha256=sha(smooth),
                current_and_baseline_rebuilt_with_same_ndk=True,
                complete_baseline_elf_equal=True, complete_current_elf_equal=True,
                ndk_revision=NDK_REVISION, compiler='aarch64-linux-android26-clang',
                compile_flags=list(FLAGS), link_flags=['-lm'],
                strip_tool='llvm-strip', strip_flags=['--strip-unneeded'],
                numerical_equivalence_claimed_by_this_build_proof=False,
                physical_android_tested=False, device_speedup_verified=False,
                elf=shape, **graph)


def verify(source, smooth, report):
    need(exact_equal(report, expected(source, smooth)),
         'Smooth115 provenance differs from exact complete payload/source graph')
    return report


def build(source, ndk, native_out, work, baseline_smooth):
    source, ndk, native_out, work = map(Path, (source, ndk, native_out, work))
    need(type(baseline_smooth) is bytes and len(baseline_smooth) == BASELINE_BYTES and
         sha(baseline_smooth) == BASELINE_SHA256, 'Wrong complete published114 smooth ELF')
    inspect(baseline_smooth); graph = scope(source)
    need(not work.exists(), 'Fresh smooth115 provenance directory required'); work.mkdir(parents=True)
    need(re.search(r'^Pkg.Revision\s*=\s*27\.2\.12479018\s*$',
                   (ndk / 'source.properties').read_text(), re.M), 'Pinned NDKr27c required')
    tool = ndk / 'toolchains/llvm/prebuilt/linux-x86_64/bin'
    current = (native_out / LIBRARY).read_bytes(); inspect(current); commands = []
    def run(label, args):
        argv = list(map(str, args)); commands.append(dict(label=label, argv=argv))
        p = subprocess.run(argv, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        (work / (label + '.log')).write_text(p.stdout)
        need(p.returncode == 0, 'Smooth native rebuild failed: ' + p.stdout[-4000:])
    for label, tree, wanted in [('baseline114', source / REFERENCE, baseline_smooth),
                                ('current115', source, current)]:
        directory = work / label; directory.mkdir()
        # Copy the whole independently pinned local include closure, without
        # source slicing or normalization. Both full ELFs are rebuilt anew.
        for name in BASELINE_SOURCES:
            if name.startswith('native1958/'):
                (directory / Path(name).name).write_bytes((tree / name).read_bytes())
        target = directory / LIBRARY
        run(label + '-compile', [tool / 'aarch64-linux-android26-clang', *FLAGS,
                                directory / 'smooth_noise1958.c', '-lm', '-o', target])
        run(label + '-strip', [tool / 'llvm-strip', '--strip-unneeded', target])
        need(target.read_bytes() == wanted, label + ' complete smooth ELF rebuild differs')
        inspect(target.read_bytes())
    need(scope(source) == graph, 'Smooth sources changed while rebuilding')
    report = expected(source, current); verify(source, current, report)
    (work / 'commands.json').write_text(json.dumps(commands, indent=2) + '\n')
    (work / 'result.json').write_text(json.dumps(report, sort_keys=True, indent=2) + '\n')
    return report


def source_negative_controls(source):
    source = Path(source); scope(source); count = 0
    names = list(BASELINE_SOURCES) + [REFERENCE + '/' + name for name in BASELINE_SOURCES]
    names += [source_inverse19115.MANIFEST]
    with tempfile.TemporaryDirectory(prefix='ulike115-smooth-source-') as td:
        tree = Path(td)
        for name in names:
            target = tree / name; target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(source / name, target)
        scope(tree)
        for name in names:
            path = tree / name; old = path.read_bytes(); path.write_bytes(old + b'\n')
            try:
                try: scope(tree)
                except (AssertionError, RuntimeError, ValueError, TypeError, KeyError): count += 1
                else: raise AssertionError('Unreviewed smooth source accepted: ' + name)
            finally: path.write_bytes(old)
    return count


def negative_controls(source, smooth, report):
    verify(source, smooth, report); count = 1 + source_negative_controls(source)
    def reject(body=report, payload=smooth):
        nonlocal count
        try: verify(source, payload, body)
        except (AssertionError, RuntimeError, ValueError, TypeError, KeyError, UnicodeError, struct.error): count += 1
        else: raise AssertionError('Malformed smooth115 provenance accepted')
    for key, value in report.items():
        row = copy.deepcopy(report); row.pop(key); reject(row)
        row = copy.deepcopy(report); row[key] = None; reject(row)
        if type(value) is bool:
            row = copy.deepcopy(report); row[key] = int(value); reject(row)
        if type(value) is int:
            row = copy.deepcopy(report); row[key] = float(value); reject(row)
    row = copy.deepcopy(report); row['unexpected'] = True; reject(row)
    offsets = {0, 4, 16, 18, len(smooth) - 1}
    for row in sections(smooth).values():
        start, size = row[4:6]
        if row[1] != 8 and size: offsets.update((start, start + size // 2, start + size - 1))
    for offset in sorted(offsets):
        changed = bytearray(smooth); changed[offset] ^= 1; reject(payload=bytes(changed))
    for raw in (smooth[:-1], smooth + b'\0', b'', bytearray(smooth)): reject(payload=raw)
    return count

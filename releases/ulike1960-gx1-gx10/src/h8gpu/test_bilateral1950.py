"""Validate bilateral1950.comp against the native1950 integer formula on GLES 3.1.

Requires a GLES 3.1 EGL implementation; Mesa surfaceless works on build hosts.
This intentionally checks exact ARGB uint32 equality, including edge clamping,
transparent guide pixels, signed rounding, and arbitrary row ranges.
"""

import ctypes as C
import random
from pathlib import Path


EGL = C.CDLL("libEGL.so.1")
try:
    GL = C.CDLL("libGLESv2.so.2")
    DESKTOP_GL = False
except OSError:
    # Some Mesa hosts install only desktop OpenGL. Its ES 3.1 compatibility
    # extension can still compile and execute a #version 310 es shader.
    GL = C.CDLL("libGL.so.1")
    DESKTOP_GL = True


def bind(lib, name, result, *args):
    f = getattr(lib, name)
    f.restype = result
    f.argtypes = list(args)
    return f


def opengl():
    proc = bind(EGL, "eglGetProcAddress", C.c_void_p, C.c_char_p)
    platform = C.CFUNCTYPE(C.c_void_p, C.c_uint, C.c_void_p, C.POINTER(C.c_int))(
        proc(b"eglGetPlatformDisplayEXT")
    )
    display = platform(0x31DD, None, None)  # EGL_PLATFORM_SURFACELESS_MESA
    assert display
    assert bind(EGL, "eglInitialize", C.c_uint, C.c_void_p, C.POINTER(C.c_int), C.POINTER(C.c_int))(
        display, C.byref(C.c_int()), C.byref(C.c_int())
    )
    assert bind(EGL, "eglBindAPI", C.c_uint, C.c_uint)(0x30A2 if DESKTOP_GL else 0x30A0)
    config_attribs = (C.c_int * 9)(0x3033, 1, 0x3040, 8 if DESKTOP_GL else 0x40,
                                   0x3024, 8, 0x3025, 8, 0x3038)
    config, num = C.c_void_p(), C.c_int()
    assert bind(EGL, "eglChooseConfig", C.c_uint, C.c_void_p, C.POINTER(C.c_int),
                C.POINTER(C.c_void_p), C.c_int, C.POINTER(C.c_int))(
                    display, config_attribs, C.byref(config), 1, C.byref(num)) and num.value
    attrs = (C.c_int * 5)(0x3098, 4 if DESKTOP_GL else 3,
                         0x30FB, 3 if DESKTOP_GL else 1, 0x3038)
    context = bind(EGL, "eglCreateContext", C.c_void_p, C.c_void_p, C.c_void_p,
                   C.c_void_p, C.POINTER(C.c_int))(display, config, None, attrs)
    assert context, "EGL cannot create ES 3.1 context"
    surface = bind(EGL, "eglCreatePbufferSurface", C.c_void_p, C.c_void_p, C.c_void_p,
                   C.POINTER(C.c_int))(display, config, (C.c_int * 5)(0x3057, 1, 0x3056, 1, 0x3038))
    assert surface
    assert bind(EGL, "eglMakeCurrent", C.c_uint, C.c_void_p, C.c_void_p,
                C.c_void_p, C.c_void_p)(display, surface, surface, context)
    version = bind(GL, "glGetString", C.c_char_p, C.c_uint)(0x1F02)
    print("OpenGL" if DESKTOP_GL else "GLES", version.decode())


def program():
    shader = bind(GL, "glCreateShader", C.c_uint, C.c_uint)(0x91B9)
    code = Path(__file__).with_name("bilateral1950.comp").read_bytes()
    src = C.c_char_p(code)
    bind(GL, "glShaderSource", None, C.c_uint, C.c_int, C.POINTER(C.c_char_p),
         C.POINTER(C.c_int))(shader, 1, C.byref(src), None)
    bind(GL, "glCompileShader", None, C.c_uint)(shader)
    status = C.c_int()
    bind(GL, "glGetShaderiv", None, C.c_uint, C.c_uint, C.POINTER(C.c_int))(
        shader, 0x8B81, C.byref(status))
    if not status.value:
        log = C.create_string_buffer(8192)
        bind(GL, "glGetShaderInfoLog", None, C.c_uint, C.c_int, C.POINTER(C.c_int),
             C.c_char_p)(shader, len(log), None, log)
        raise AssertionError(log.value.decode())
    prog = bind(GL, "glCreateProgram", C.c_uint)()
    bind(GL, "glAttachShader", None, C.c_uint, C.c_uint)(prog, shader)
    bind(GL, "glLinkProgram", None, C.c_uint)(prog)
    bind(GL, "glGetProgramiv", None, C.c_uint, C.c_uint, C.POINTER(C.c_int))(
        prog, 0x8B82, C.byref(status))
    if not status.value:
        log = C.create_string_buffer(8192)
        bind(GL, "glGetProgramInfoLog", None, C.c_uint, C.c_int, C.POINTER(C.c_int),
             C.c_char_p)(prog, len(log), None, log)
        raise AssertionError(log.value.decode())
    return prog


def trunc(n, d):
    return (1 if n >= 0 else -1) * (abs(n) // d)


def rnd(n, d):
    return (n + d // 2) // d if n >= 0 else -((-n + d // 2) // d)


def clamp(n, lo, hi):
    return max(lo, min(n, hi))


def red(p):
    return (p >> 16) & 255


def green(p):
    return (p >> 8) & 255


def blue(p):
    return p & 255


def lum(p):
    return (red(p) * 77 + green(p) * 150 + blue(p) * 29 + 128) >> 8


def skin(y, cb, cr):
    a = trunc(clamp(trunc(cr * 256, 14), 0, 256) * clamp(trunc((90 - cr) * 256, 20), 0, 256), 256)
    b = trunc(clamp(trunc((20 - cb) * 256, 16), 0, 256) * clamp(trunc((cb + 80) * 256, 20), 0, 256), 256)
    return trunc(trunc(a * b, 256) * clamp(trunc((y - 10) * 256, 25), 0, 256), 256)


def reference(guide, values, width, rows, noise, horizontal, texture, shadows, begin, end, lr, cc):
    out = [0xA5A5A5A5] * (width * rows)
    offsets, spatial = [-3, -2, -1, 1, 2, 3], [1, 6, 15, 15, 6, 1]
    lm, cm = [0, 48, 68, 85, 96], [0, 78, 91, 98, 100]
    for row in range(begin, end):
        for x in range(width):
            at = row * width + x
            center = guide[at]
            if center >> 24 != 255:
                out[at] = center
                continue
            yc = lum(center)
            cb, cr = blue(center) - yc, red(center) - yc
            dark = clamp(trunc((112 - yc) * 256, 96), 0, 256) if shadows else 0
            scale = 256 - dark // 6
            vc = values[at]
            vy = yc if horizontal else lum(vc)
            sumy, sumb, sumr, wy, wc = vy * 5120, (blue(vc) - vy) * 5120, (red(vc) - vy) * 5120, 5120, 5120
            for t in range(6):
                nx = clamp(x + offsets[t], 0, width - 1) if horizontal else x
                nr = row if horizontal else clamp(row + offsets[t], 0, rows - 1)
                ix = nr * width + nx
                neighbour = guide[ix]
                if neighbour >> 24 != 255:
                    continue
                yn = lum(neighbour)
                dy = abs(yc - yn)
                dist = max(abs(cb - (blue(neighbour) - yn)), abs(cr - (red(neighbour) - yn)))
                if dy > 72 or dist > 110:
                    continue
                yw = spatial[t] * lr[noise * 256 + trunc(max(dy, dist // 3) * scale, 256)]
                cw = spatial[t] * cc[noise * 256 + trunc(max(dy * 2, dist) * scale, 256)]
                nv = values[ix]
                ny = yn if horizontal else lum(nv)
                wy += yw
                sumy += yw * ny
                wc += cw
                sumb += cw * (blue(nv) - ny)
                sumr += cw * (red(nv) - ny)
            dy = rnd(sumy, wy) - yc
            db = rnd(sumb, wc) - cb
            dr = rnd(sumr, wc) - cr
            if not horizontal:
                mixy = min(100, lm[noise] + dark * 12 // 256)
                mixc = min(100, cm[noise] + dark * 5 // 256)
                edge = (abs(lum(guide[row * width + min(width - 1, x + 1)])
                            - lum(guide[row * width + max(0, x - 1)]))
                        + abs(lum(guide[min(rows - 1, row + 1) * width + x])
                              - lum(guide[max(0, row - 1) * width + x])))
                edge = clamp(trunc((edge - 12) * 256, 72), 0, 256)
                protect = edge * 12 // 256
                if texture:
                    protect += edge * skin(yc, cb, cr) // 256 * 20 // 256
                mixy = mixy * (100 - protect) // 100
                dy, db, dr = rnd(dy * mixy, 100), rnd(db * mixc, 100), rnd(dr * mixc, 100)
            rr = clamp(red(center) + dy + dr, 0, 255)
            gg = clamp(green(center) + dy - rnd(dr * 77 + db * 29, 150), 0, 255)
            bb = clamp(blue(center) + dy + db, 0, 255)
            out[at] = 0xFF000000 | rr << 16 | gg << 8 | bb
    return out


def gpu(prog, guide, values, width, rows, noise, horizontal, texture, shadows, begin, end, lr, cc):
    n = width * rows
    contents = [guide, values, [0xA5A5A5A5] * n, lr, cc]
    ids = (C.c_uint * 5)()
    bind(GL, "glGenBuffers", None, C.c_int, C.POINTER(C.c_uint))(5, ids)
    for i, data in enumerate(contents):
        array = (C.c_uint32 if i < 3 else C.c_int32) * len(data)
        packed = array(*data)
        bind(GL, "glBindBuffer", None, C.c_uint, C.c_uint)(0x90D2, ids[i])
        bind(GL, "glBufferData", None, C.c_uint, C.c_ssize_t, C.c_void_p, C.c_uint)(
            0x90D2, C.sizeof(packed), packed, 0x88E8)
        bind(GL, "glBindBufferBase", None, C.c_uint, C.c_uint, C.c_uint)(0x90D2, i, ids[i])
    bind(GL, "glUseProgram", None, C.c_uint)(prog)
    for name, value in (("uWidth", width), ("uRows", rows), ("uNoise", noise),
                        ("uHorizontal", horizontal), ("uTexture", texture),
                        ("uShadows", shadows), ("uBegin", begin), ("uEnd", end)):
        loc = bind(GL, "glGetUniformLocation", C.c_int, C.c_uint, C.c_char_p)(prog, name.encode())
        assert loc >= 0, name
        bind(GL, "glUniform1i", None, C.c_int, C.c_int)(loc, value)
    bind(GL, "glDispatchCompute", None, C.c_uint, C.c_uint, C.c_uint)(
        (width + 15) // 16, (end - begin + 15) // 16, 1)
    bind(GL, "glMemoryBarrier", None, C.c_uint)(0x2000 | 0x200)
    bind(GL, "glFinish", None)()
    bind(GL, "glBindBuffer", None, C.c_uint, C.c_uint)(0x90D2, ids[2])
    ptr = bind(GL, "glMapBufferRange", C.c_void_p, C.c_uint, C.c_ssize_t,
               C.c_ssize_t, C.c_uint)(0x90D2, 0, n * 4, 1)
    assert ptr
    result = list(((C.c_uint32 * n).from_address(ptr)))
    assert bind(GL, "glUnmapBuffer", C.c_uint, C.c_uint)(0x90D2)
    bind(GL, "glDeleteBuffers", None, C.c_int, C.POINTER(C.c_uint))(5, ids)
    return result


def test():
    opengl()
    prog = program()
    rng = random.Random(1950)
    cases = 0
    for width, rows in ((1, 1), (2, 2), (5, 4), (23, 17)):
        n = width * rows
        for noise in range(1, 5):
            guide = [rng.getrandbits(24) | (255 if i % 13 else 128) << 24 for i in range(n)]
            values = [rng.getrandbits(24) | 255 << 24 for _ in range(n)]
            lr = [rng.randrange(257) for _ in range(1280)]
            cc = [rng.randrange(257) for _ in range(1280)]
            for horizontal in (0, 1):
                for texture in (0, 1):
                    for shadows in (0, 1):
                        begin, end = (1, rows - 1) if rows > 3 else (0, rows)
                        args = (guide, values, width, rows, noise, horizontal, texture,
                                shadows, begin, end, lr, cc)
                        expected = reference(*args)
                        actual = gpu(prog, *args)
                        differences = [(i, hex(e), hex(a)) for i, (e, a) in
                                       enumerate(zip(expected, actual)) if e != a]
                        assert not differences, (width, rows, noise, horizontal,
                                                 texture, shadows, differences[:5])
                        cases += 1
    # Smooth source pixels force the six neighbouring taps to contribute;
    # run both passes in sequence using the actual GPU scratch output.
    for width, rows in ((1, 1), (5, 4), (23, 17), (64, 34)):
        for pattern in (0, 1):
            pixels = []
            for y in range(rows):
                for x in range(width):
                    jitter = rng.randrange(-6, 7) if pattern else 0
                    r = clamp(40 + x * 2 + y + jitter, 0, 255)
                    g = clamp(47 + x * 2 + y + jitter, 0, 255)
                    b = clamp(55 + x * 2 + y + jitter, 0, 255)
                    a = 128 if (x + y * 3) % 37 == 0 else 255
                    pixels.append(a << 24 | r << 16 | g << 8 | b)
            lr = [max(0, 256 - (i % 256) * 2) for i in range(1280)]
            cc = [max(0, 256 - (i % 256) * 3) for i in range(1280)]
            for noise in range(1, 5):
                for texture in (0, 1):
                    for shadows in (0, 1):
                        basic = (width, rows, noise, 1, texture, shadows, 0, rows, lr, cc)
                        first_cpu = reference(pixels, pixels, *basic)
                        first_gpu = gpu(prog, pixels, pixels, *basic)
                        assert first_cpu == first_gpu, ("horizontal", width, rows, noise)
                        second = (width, rows, noise, 0, texture, shadows, 0, rows, lr, cc)
                        final_cpu = reference(pixels, first_cpu, *second)
                        final_gpu = gpu(prog, pixels, first_gpu, *second)
                        assert final_cpu == final_gpu, ("vertical", width, rows, noise)
                        cases += 2
            # The camera save path processes overlapping stripes. Horizontal
            # must cover three extra rows on either side, and only the vertical
            # consumed rows plus horizontal halo rows return to the CPU.
            if rows > 6:
                for begin,end in ((1,rows-1),(rows//2,rows//2+3)):
                    outer_begin=max(0,begin-3);outer_end=min(rows,end+3)
                    for noise in range(1,5):
                        for texture in (0,1):
                            for shadows in (0,1):
                                horizontal=(width,rows,noise,1,texture,shadows,
                                            outer_begin,outer_end,lr,cc)
                                cpu_middle=reference(pixels,pixels,*horizontal)
                                gpu_middle=gpu(prog,pixels,pixels,*horizontal)
                                vertical=(width,rows,noise,0,texture,shadows,
                                          begin,end,lr,cc)
                                cpu_final=reference(pixels,cpu_middle,*vertical)
                                gpu_final=gpu(prog,pixels,gpu_middle,*vertical)
                                assert cpu_final==gpu_final
                                for row in list(range(outer_begin,begin))+list(range(end,outer_end)):
                                    start=row*width
                                    assert cpu_middle[start:start+width]==gpu_middle[start:start+width]
                                cases += 2
    print(f"PASS: {cases} exact integer cases")


if __name__ == "__main__":
    test()

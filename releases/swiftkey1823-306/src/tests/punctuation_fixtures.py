"""Independent native-layout reader and baseline key-swap fixture generator.

The reader follows gg/a.z(DataInputStream)'s type-3 layout, not PunctuationKeys.
KeySwapBaseline is a reviewed host port of the pinned 1.8.16 KeySwapBinary DEX
algorithm. Its tables are decoded from the real smali fixture at test time.
It does not claim to execute DEX, ART or Android's native layout builder.
"""
import hashlib
import math
import re
import struct
import zlib
from dataclasses import dataclass
from pathlib import Path


def sha256(data):
    return hashlib.sha256(data).hexdigest()


def utf(value):
    # DataOutputStream.writeUTF: encode UTF-16 code units as modified UTF-8.
    units = struct.unpack('>' + 'H' * (len(value.encode('utf-16-be')) // 2),
                          value.encode('utf-16-be'))
    data = bytearray()
    for unit in units:
        if 1 <= unit <= 0x7f:
            data.append(unit)
        elif unit <= 0x7ff:
            data.extend((0xc0 | (unit >> 6), 0x80 | (unit & 63)))
        else:
            data.extend((0xe0 | (unit >> 12), 0x80 | ((unit >> 6) & 63),
                         0x80 | (unit & 63)))
    return struct.pack('>H', len(data)) + data


class Reader:
    def __init__(self, data):
        self.data = data
        self.pos = 0

    def read(self, fmt):
        size = struct.calcsize('>' + fmt)
        result = struct.unpack_from('>' + fmt, self.data, self.pos)
        self.pos += size
        return result[0] if len(result) == 1 else result

    def utf(self):
        size = self.read('H')
        stop = self.pos + size
        if stop > len(self.data):
            raise ValueError('truncated modified UTF-8')
        data = self.data[self.pos:stop]
        self.pos = stop
        units, pos = [], 0
        while pos < len(data):
            c = data[pos]
            if c < 128:
                units.append(c)
                pos += 1
            elif c & 0xe0 == 0xc0:
                units.append(((c & 31) << 6) | (data[pos + 1] & 63))
                pos += 2
            elif c & 0xf0 == 0xe0:
                units.append(((c & 15) << 12) | ((data[pos + 1] & 63) << 6)
                             | (data[pos + 2] & 63))
                pos += 3
            else:
                raise ValueError('unsupported modified UTF-8')
        return struct.pack('>' + 'H' * len(units), *units).decode('utf-16-be')


@dataclass
class Key:
    row: int
    column: int
    start: int
    end: int
    raw: bytes
    shorts: tuple
    doubles: tuple
    flags: tuple
    strings: tuple
    top: str
    spans: dict

    @property
    def primary(self):
        return self.strings[4]

    @property
    def popup(self):
        return self.strings[5]

    def replaced(self, **fields):
        edits = []
        for field, value in fields.items():
            first, last = self.spans[field]
            edits.append((first - self.start, last - self.start, utf(value)))
        result = bytearray()
        pos = 0
        for first, last, value in sorted(edits):
            result.extend(self.raw[pos:first])
            result.extend(value)
            pos = last
        result.extend(self.raw[pos:])
        return bytes(result)


@dataclass
class Row:
    header: bytes
    kind: int
    variant: int
    keys: list


@dataclass
class Layout:
    raw: bytes
    header: bytes
    rows: list
    tail: bytes

    @property
    def keys(self):
        return [key for row in self.rows for key in row.keys]


def parse_layout(compressed):
    raw = zlib.decompress(compressed)
    rd = Reader(raw)
    version, kind = rd.read('ii')
    if (version, kind) != (1, 3):
        raise ValueError('native fixture must have binary version 1, type 3')
    rd.read('?')
    rd.utf()
    rd.read('dd')
    rd.utf()
    row_count = rd.read('i')
    header = raw[:rd.pos]
    rows = []
    for row in range(row_count):
        start = rd.pos
        kind, variant, flag, height, count = rd.read('hh?di')
        row_header = raw[start:rd.pos]
        keys = []
        for col in range(count):
            start = rd.pos
            shorts = rd.read('6h')
            doubles = rd.read('dd')
            flags = rd.read('4?')
            strings, spans = [], {}
            for index, name in enumerate(('q', 'r', 's', 't', 'u', 'v')):
                first = rd.pos
                strings.append(rd.utf())
                spans[name] = (first, rd.pos)
            rd.read('d?')
            null_top = rd.read('?')
            if null_top:
                top = None
            else:
                first = rd.pos
                top = rd.utf()
                spans['top'] = (first, rd.pos)
            rd.read('4d')
            keys.append(Key(row, col, start, rd.pos, raw[start:rd.pos], shorts,
                            doubles, flags, tuple(strings), top, spans))
        rows.append(Row(row_header, kind, variant, keys))
    tail_start = rd.pos
    rd.read('h??')
    rd.utf()
    rd.read('4?')
    rd.utf()
    if rd.pos != len(raw):
        raise ValueError('trailing native fixture data')
    return Layout(raw, header, rows, raw[tail_start:])


class KeySwapBaseline:
    """Pinned DEX algorithm port; tables remain sourced from real DEX smali.

    The constructor executes only the small array-building class initializer.
    The transform below follows the native method's section order, width
    adjustment and arrow permutation. Every offset is supplied by the fixture.
    """
    def __init__(self, smali_path):
        source = Path(smali_path).read_text()
        method = source.split('.method static constructor <clinit>()V', 1)[1]
        method = method.split('.end method', 1)[0]
        arrays = {}
        for name, body in re.findall(r':(array_[0-9a-f]+)\s+\.array-data 4\s+(.*?)\s+\.end array-data', method, re.S):
            arrays[name] = [int(x, 0) for x in body.split()]
        registers, pending = {}, None
        self.fields = {}
        for line in method.split('return-void', 1)[0].splitlines():
            line = line.strip()
            if not line or line.startswith('.'):
                continue
            if m := re.fullmatch(r'const(?:/\w+)? (v\d+), (-?0x[0-9a-f]+)', line):
                registers[m[1]] = int(m[2], 0)
            elif m := re.fullmatch(r'const-string (v\d+), "([^"]+)"', line):
                registers[m[1]] = m[2]
            elif m := re.fullmatch(r'new-array (v\d+), (v\d+), \[+I', line):
                registers[m[1]] = [None] * registers[m[2]]
            elif m := re.fullmatch(r'fill-array-data (v\d+), :(array_[0-9a-f]+)', line):
                registers[m[1]] = arrays[m[2]][:]
            elif m := re.fullmatch(r'move-object(?:/from16)? (v\d+), (v\d+)', line):
                registers[m[1]] = registers[m[2]]
            elif m := re.fullmatch(r'filled-new-array(/range)? \{([^}]+)\}, \[+.*', line):
                if m[1]:
                    lo, hi = map(int, re.findall(r'v(\d+)', m[2]))
                    names = ['v' + str(i) for i in range(lo, hi + 1)]
                else:
                    names = m[2].split(', ')
                pending = [registers[name] for name in names]
            elif m := re.fullmatch(r'move-result-object (v\d+)', line):
                registers[m[1]] = pending
            elif m := re.fullmatch(r'sput-object (v\d+), Lhiro/swiftkey/KeySwapBinary;->(\w+):.*', line):
                self.fields[m[2]] = registers[m[1]]
            else:
                raise AssertionError('unreviewed baseline initializer instruction: ' + line)
        assert set(self.fields) == {'HASHES', 'SIZES', 'ROWS', 'ARROWS'}

    def transform(self, compressed, mask):
        if len(compressed) > 4096 or mask < 0 or mask > 255:
            raise ValueError('invalid baseline request')
        index = self.fields['HASHES'].index(sha256(compressed))
        raw = zlib.decompress(compressed)
        assert len(raw) == self.fields['SIZES'][index]
        output, pos = bytearray(), 0
        get = lambda data, at: struct.unpack_from('>d', data, at)[0]
        put = lambda data, at, value: struct.pack_into('>d', data, at, value)
        for first, middle, last, end, fw, lw, sw, bit in self.fields['ROWS'][index]:
            swap, equalize = bool(mask & bit), sw >= 0
            if not swap and not equalize:
                continue
            output.extend(raw[pos:first])
            left = bytearray(raw[first:middle])
            center = bytearray(raw[middle:last])
            right = bytearray(raw[last:end])
            left_width, right_width = get(raw, fw), get(raw, lw)
            if equalize:
                maximum = max(left_width, right_width)
                shrink = (maximum - left_width) + (maximum - right_width)
                space_width = get(raw, sw) - shrink
                assert space_width > 0 and math.isfinite(space_width)
                put(center, sw - middle, space_width)
                left_width = right_width = maximum
            put(right, lw - last, left_width if swap else right_width)
            put(left, fw - first, right_width if swap else left_width)
            output.extend(right if swap else left)
            output.extend(center)
            output.extend(left if swap else right)
            pos = end
        output.extend(raw[pos:])
        assert len(output) == len(raw)
        order = [2, 3, 0, 1]
        if index < 4 and mask & 32:
            order[0:2] = [3, 2]
        if index < 4 and mask & 16:
            order[2:4] = [1, 0]
        for table in self.fields['ARROWS'][index]:
            pos = table[0]
            for destination, source in enumerate(order):
                key = bytearray(raw[table[source]:table[source + 1]])
                key[10:12] = raw[table[destination] + 10:table[destination] + 12]
                put(key, table[source + 5] - table[source], get(raw, table[destination + 5]))
                output[pos:pos + len(key)] = key
                pos += len(key)
            assert pos == table[4]
        return zlib.compress(output)

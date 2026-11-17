"""Portable compiled-ARSC parser reused from the prior Hiro Japanese-resource audit."""
import re
import struct

class Arsc:
    def __init__(self, raw):
        self.raw = raw
        self.entries = {}
        self.packages = []
        t, h, size = self.chunk(0)
        assert t == 2 and size == len(raw), 'Not a complete compiled resource table'
        global_strings = None
        for at, typ, head, length in self.children(h, size):
            if typ == 1:
                assert global_strings is None, 'Multiple global string pools'
                global_strings = self.pool(at)
            elif typ == 0x200:
                assert global_strings is not None, 'Global string pool not found'
                self.package(at, head, length, global_strings)

    def u16(self, p):
        return struct.unpack_from('<H', self.raw, p)[0]

    def u32(self, p):
        return struct.unpack_from('<I', self.raw, p)[0]

    def chunk(self, p):
        t, h, n = struct.unpack_from('<HHI', self.raw, p)
        assert n >= h >= 8 and p + n <= len(self.raw), 'Invalid ARSC chunk bounds'
        return t, h, n

    def children(self, start, end):
        p = start
        while p < end:
            t, h, n = self.chunk(p)
            assert p + n <= end, 'Nested ARSC chunk exceeds container'
            yield p, t, h, n
            p += n
        assert p == end

    def length8(self, p):
        x = self.raw[p]
        return (((x & 127) << 8) | self.raw[p+1], p+2) if x & 128 else (x, p+1)

    def length16(self, p):
        x = self.u16(p)
        return (((x & 32767) << 16) | self.u16(p+2), p+4) if x & 32768 else (x, p+2)

    def pool(self, at):
        typ, head, length = self.chunk(at)
        assert typ == 1
        count, styles, flags, start = struct.unpack_from('<IIII', self.raw, at+8)
        values = []
        for i in range(count):
            p = at + start + self.u32(at+head+i*4)
            if flags & 0x100:
                units, p = self.length8(p)
                size, p = self.length8(p)
                assert self.raw[p+size] == 0
                # Android pools can retain Java surrogate code units as CESU-8.
                # Normalize paired surrogates to the same Python Unicode value.
                value = self.raw[p:p+size].decode('utf-8', 'surrogatepass')
                value = value.encode('utf-16le', 'surrogatepass').decode('utf-16le', 'surrogatepass')
            else:
                size, p = self.length16(p)
                assert self.u16(p+size*2) == 0
                value = self.raw[p:p+size*2].decode('utf-16le', 'surrogatepass')
            values.append(value)
        return values

    def package(self, at, head, length, global_strings):
        package_id = self.u32(at+8)
        package_name = self.raw[at+12:at+268].decode('utf-16le').split('\0', 1)[0]
        type_strings = self.pool(at+self.u32(at+268))
        key_strings = self.pool(at+self.u32(at+276))
        type_offset = self.u32(at+284) if head >= 288 else 0
        self.packages.append({'id': package_id, 'name': package_name})
        # The NewX resources belong to the main application (0x7f), while the
        # payment dynamic-feature package has unrelated duplicate names.
        if package_id != 0x7f:
            return
        for p, typ, h, n in self.children(at+head, at+length):
            if typ != 0x201:
                continue
            type_id, flags = self.raw[p+8:p+10]
            count, start = struct.unpack_from('<II', self.raw, p+12)
            if type_strings[type_id-1] != 'string':
                continue
            config_size = self.u32(p+20)
            config = self.raw[p+24:p+20+config_size]
            if not any(config):
                qualifier = ''
            else:
                # Relative to config after its size: language is bytes 4..5.
                lang = config[4:6]
                rest = config[:4] + b'\0\0' + config[6:]
                qualifier = 'ja' if lang == b'ja' and not any(rest) else 'raw:'+config.hex()
            offsets = []
            if flags & 1:
                for i in range(count):
                    index, offset = struct.unpack_from('<HH', self.raw, p+h+i*4)
                    offsets.append((index, offset*4))
            elif flags & 2:
                offsets = [(i, self.u16(p+h+i*2)*4) for i in range(count)
                           if self.u16(p+h+i*2) != 0xffff]
            else:
                offsets = [(i, self.u32(p+h+i*4)) for i in range(count)
                           if self.u32(p+h+i*4) != 0xffffffff]
            for index, offset in offsets:
                q = p+start+offset
                entry_size, entry_flags, key = struct.unpack_from('<HHI', self.raw, q)
                assert not entry_flags & 8, 'Compact entry requires explicit parser support'
                if entry_flags & 1:
                    continue
                value_size, zero, data_type, data = struct.unpack_from('<HBBI', self.raw, q+entry_size)
                assert value_size == 8 and zero == 0
                name = key_strings[key]
                value = global_strings[data] if data_type == 3 else {'type': data_type, 'data': data}
                rid = (package_id << 24) | ((type_id+type_offset) << 16) | index
                slot = self.entries.setdefault(name, {})
                assert qualifier not in slot, 'Duplicate name/config: '+name+'/'+qualifier
                slot[qualifier] = {'id': f'0x{rid:08x}', 'value': value, 'package': package_name}


def android_string(text):
    """Interpret Android source escapes/quotes without changing escaped newlines.

    These input XMLs contain simple string nodes, so no spans/references/plurals
    are handled. Fail closed on unsupported escape sequences.
    """
    out, quoted, pending, i = [], False, False, 0
    while i < len(text):
        ch = text[i]
        if ch == '\\':
            i += 1
            assert i < len(text), 'Incomplete Android string escape'
            ch = text[i]
            escapes = {'n': '\n', 't': '\t', 'r': '\r', '\\': '\\', '"': '"', "'": "'", '@': '@', '?': '?'}
            if ch == 'u':
                assert re.fullmatch('[0-9a-fA-F]{4}', text[i+1:i+5])
                ch = chr(int(text[i+1:i+5], 16)); i += 4
            else:
                assert ch in escapes, 'Unsupported Android escape: '+ch
                ch = escapes[ch]
            if pending and out:
                out.append(' ')
            pending = False
            out.append(ch)
        elif ch == '"':
            quoted = not quoted
        elif ch.isspace() and not quoted:
            pending = True
        else:
            if pending and out:
                out.append(' ')
            pending = False
            out.append(ch)
        i += 1
    assert not quoted, 'Unclosed Android source quote'
    return ''.join(out)



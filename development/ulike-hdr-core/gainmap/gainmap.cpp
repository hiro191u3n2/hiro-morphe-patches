#include "gainmap.hpp"

#include <algorithm>
#include <cmath>
#include <limits>
#include <stdexcept>
#include <vector>

namespace hiro::gainmap {
namespace {
[[noreturn]] void invalid(const char* message) { throw std::invalid_argument(message); }
bool finite(double x) { return std::isfinite(x); }
void check_samples(Samples s) {
    if (s != Samples::Float32 && s != Samples::Float64) invalid("unsupported sample storage");
}
void check_frame(const Frame& f) {
    if (!f.width || !f.height || f.width > std::numeric_limits<std::size_t>::max() / 3)
        invalid("invalid image dimensions");
    if (f.primaries != Primaries::Srgb && f.primaries != Primaries::DisplayP3 &&
        f.primaries != Primaries::Bt2020) invalid("unsupported RGB primaries");
    if (!finite(f.reference_white_nits) || f.reference_white_nits <= 0 ||
        !f.capture_id || !f.geometry_id || !f.processing_id)
        invalid("missing reference white or frame identity");
}
bool same_frame(const Frame& a, const Frame& b) {
    return a.width == b.width && a.height == b.height && a.primaries == b.primaries &&
        a.reference_white_nits == b.reference_white_nits && a.capture_id == b.capture_id &&
        a.geometry_id == b.geometry_id && a.processing_id == b.processing_id;
}
void check_buffer(std::size_t w, std::size_t h, std::size_t stride, std::size_t capacity,
                  Samples samples, const void* data) {
    check_samples(samples);
    if (!w || !h || w > std::numeric_limits<std::size_t>::max() / 3)
        invalid("invalid buffer dimensions");
    const auto row = w * 3;
    if (!data || stride < row || (h - 1) > (std::numeric_limits<std::size_t>::max() - row) / stride)
        invalid("invalid buffer or stride");
    const auto required = (h - 1) * stride + row;
    const auto bytes = samples == Samples::Float32 ? sizeof(float) : sizeof(double);
    if (capacity < required || capacity > std::numeric_limits<std::size_t>::max() / bytes)
        invalid("insufficient or overflowing storage");
    const auto alignment = samples == Samples::Float32 ? alignof(float) : alignof(double);
    if (reinterpret_cast<std::uintptr_t>(data) % alignment) invalid("unaligned sample storage");
}
void check_image(const LinearImageView& i) {
    check_frame(i.frame);
    check_buffer(i.frame.width, i.frame.height, i.row_stride, i.capacity, i.samples, i.data);
}
double read(const void* data, Samples s, std::size_t index) {
    return s == Samples::Float32 ? static_cast<double>(static_cast<const float*>(data)[index])
                                : static_cast<const double*>(data)[index];
}
double pixel(const LinearImageView& i, std::size_t y, std::size_t x) {
    const double v = read(i.data, i.samples, y * i.row_stride + x);
    if (!finite(v) || v < 0) invalid("linear RGB must be finite and nonnegative");
    return v;
}
void check_options(const Options& o) {
    if (o.base != BaseRendition::Sdr && o.base != BaseRendition::Hdr) invalid("invalid direction");
    for (int c = 0; c < 3; ++c) {
        if (!finite(o.offset_sdr[c]) || !finite(o.offset_hdr[c]) ||
            o.offset_sdr[c] < 0 || o.offset_hdr[c] < 0) invalid("invalid offsets");
        if (!finite(o.encoding_gamma[c]) || o.encoding_gamma[c] <= 0 ||
            !finite(1.0 / o.encoding_gamma[c])) invalid("invalid encoding gamma");
    }
    if (!finite(o.min_display_ratio) || !finite(o.max_display_ratio) ||
        o.min_display_ratio < 1 || o.max_display_ratio <= o.min_display_ratio)
        invalid("invalid display headroom");
}
void check_metadata(const Metadata& m) {
    check_frame(m.frame); check_options(m.options);
    for (int c = 0; c < 3; ++c)
        if (!finite(m.min_log2[c]) || !finite(m.max_log2[c]) ||
            m.max_log2[c] < m.min_log2[c] || !finite(m.max_log2[c] - m.min_log2[c]))
            invalid("invalid gain bounds");
}
void check_pair(const Pair& p) {
    check_image(p.sdr); check_image(p.hdr);
    if (!same_frame(p.sdr.frame, p.hdr.frame)) invalid("HDR/SDR frame, geometry, color or revision mismatch");
}
double log_gain(double sdr, double hdr, const Options& o, int c) {
    const double s = sdr + o.offset_sdr[c], h = hdr + o.offset_hdr[c];
    if (!finite(s) || !finite(h)) invalid("offset addition overflow");
    // Both exactly zero contain no recoverable gain information; canonical unity.
    if (s == 0 && h == 0) return 0;
    if (s <= 0 || h <= 0) invalid("zero endpoint has no finite multiplicative gain; supply positive offsets");
    return std::log2(h) - std::log2(s); // avoids overflow in h/s
}
void check_sink(const RowSink& sink) { if (!sink) invalid("missing row sink"); }
double bounded_map(double value) {
    if (!finite(value) || value < 0 || value > 1) invalid("gainmap sample outside [0,1]");
    return value;
}
double map_power(double value, double exponent) {
    const double out = bounded_map(std::pow(value, exponent));
    if (value > 0 && value < 1 && (out == 0 || out == 1))
        invalid("gamma collapses an interior sample to an endpoint at FP64 precision");
    return out;
}
std::uint32_t max_code(MapBits bits) {
    if (bits == MapBits::Ten) return 1023;
    if (bits == MapBits::Sixteen) return 65535;
    invalid("gainmap quantization must be 10 or 16 bits");
}
void reconstruct(const LinearImageView& base, const MapView& map, const Metadata& m,
                 double canonical_weight, const RowSink& sink) {
    check_metadata(m); check_image(base); check_sink(sink);
    if (!same_frame(base.frame, m.frame)) invalid("base frame does not match gainmap metadata");
    check_frame(map.frame);
    if (!same_frame(map.frame, m.frame))
        invalid("gainmap frame does not match metadata");
    check_buffer(map.frame.width, map.frame.height, map.row_stride, map.capacity, map.samples, map.data);
    const bool hdr_base = m.options.base == BaseRendition::Hdr;
    const auto& base_offset = hdr_base ? m.options.offset_hdr : m.options.offset_sdr;
    const auto& alternate_offset = hdr_base ? m.options.offset_sdr : m.options.offset_hdr;
    std::vector<double> row(m.frame.width * 3);
    for (std::size_t y = 0; y < m.frame.height; ++y) {
        for (std::size_t x = 0; x < row.size(); ++x) {
            const int c = static_cast<int>(x % 3);
            const double b = pixel(base, y, x);
            const double g = bounded_map(read(map.data, map.samples, y * map.row_stride + x));
            if (canonical_weight == 0) { row[x] = b; continue; }
            const double decoded = map_power(g, 1.0 / m.options.encoding_gamma[c]);
            const double l = m.min_log2[c] + (m.max_log2[c] - m.min_log2[c]) * decoded;
            const double shifted = b + base_offset[c];
            if (!finite(shifted)) invalid("base offset addition overflow");
            double reconstructed = 0;
            if (shifted > 0) {
                const double factor = std::exp2(l * canonical_weight);
                // Prefer direct arithmetic, but use log space when intermediate
                // gain multiplication would overflow/underflow unnecessarily.
                const double product = shifted * factor;
                reconstructed = (finite(factor) && factor > 0 && finite(product) && product > 0)
                    ? product : std::exp2(std::log2(shifted) + l * canonical_weight);
            }
            row[x] = reconstructed - alternate_offset[c];
            if (!finite(row[x])) invalid("reconstruction overflow");
        }
        sink(y, row.data(), row.size());
    }
}
} // namespace

Rgb Metadata::android_decoding_gamma() const {
    check_metadata(*this);
    return {{1.0 / options.encoding_gamma[0], 1.0 / options.encoding_gamma[1],
             1.0 / options.encoding_gamma[2]}};
}

Metadata analyze(const Pair& p, const Options& o) {
    check_pair(p); check_options(o);
    Metadata m; m.frame = p.sdr.frame; m.options = o;
    m.min_log2.fill(std::numeric_limits<double>::infinity());
    m.max_log2.fill(-std::numeric_limits<double>::infinity());
    for (std::size_t y = 0; y < m.frame.height; ++y)
        for (std::size_t x = 0; x < m.frame.width * 3; ++x) {
            const int c = static_cast<int>(x % 3);
            const double l = log_gain(pixel(p.sdr, y, x), pixel(p.hdr, y, x), o, c);
            m.min_log2[c] = std::min(m.min_log2[c], l);
            m.max_log2[c] = std::max(m.max_log2[c], l);
        }
    check_metadata(m); return m;
}

void encode_rows(const Pair& p, const Metadata& m, const RowSink& sink) {
    check_pair(p); check_metadata(m); check_sink(sink);
    if (!same_frame(p.sdr.frame, m.frame)) invalid("analysis belongs to another processed pair");
    std::vector<double> row(m.frame.width * 3);
    for (std::size_t y = 0; y < m.frame.height; ++y) {
        for (std::size_t x = 0; x < row.size(); ++x) {
            const int c = static_cast<int>(x % 3);
            const double l = log_gain(pixel(p.sdr, y, x), pixel(p.hdr, y, x), m.options, c);
            if (l < m.min_log2[c] || l > m.max_log2[c])
                invalid("processed pixels changed or gain bounds exclude the pair");
            const double span = m.max_log2[c] - m.min_log2[c];
            const double n = span == 0 ? 0 : (l - m.min_log2[c]) / span;
            row[x] = map_power(n, m.options.encoding_gamma[c]);
        }
        sink(y, row.data(), row.size());
    }
}

void reconstruct_alternate_rows(const LinearImageView& base, const MapView& map,
                                const Metadata& m, const RowSink& sink) {
    reconstruct(base, map, m, m.options.base == BaseRendition::Sdr ? 1.0 : -1.0, sink);
}
void reconstruct_display_rows(const LinearImageView& base, const MapView& map,
                              const Metadata& m, double ratio, const RowSink& sink) {
    check_metadata(m);
    if (!finite(ratio) || ratio < 1) invalid("display ratio must be finite and >= 1");
    const double min = m.options.min_display_ratio, max = m.options.max_display_ratio;
    // Endpoint comparisons avoid 0/0 when rounded logs of adjacent large
    // ratios coincide. log1p preserves a narrow but valid headroom interval.
    const double w = ratio <= min ? 0.0 : ratio >= max ? 1.0 :
        std::clamp(std::log1p((ratio - min) / min) / std::log1p((max - min) / min), 0.0, 1.0);
    reconstruct(base, map, m, m.options.base == BaseRendition::Sdr ? w : w - 1.0, sink);
}

void quantize(const double* normalized, std::uint16_t* codes, std::size_t n, MapBits bits) {
    const auto max = max_code(bits);
    if (n && (!normalized || !codes)) invalid("null quantization buffer");
    for (std::size_t i = 0; i < n; ++i)
        codes[i] = static_cast<std::uint16_t>(std::floor(bounded_map(normalized[i]) * max + 0.5));
}
void dequantize(const std::uint16_t* codes, double* normalized, std::size_t n, MapBits bits) {
    const auto max = max_code(bits);
    if (n && (!normalized || !codes)) invalid("null dequantization buffer");
    for (std::size_t i = 0; i < n; ++i) {
        if (codes[i] > max) invalid("gainmap code exceeds declared precision");
        normalized[i] = static_cast<double>(codes[i]) / max;
    }
}
} // namespace hiro::gainmap

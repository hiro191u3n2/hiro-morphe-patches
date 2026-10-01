#pragma once

#include <array>
#include <cstddef>
#include <cstdint>
#include <functional>

namespace hiro::gainmap {

using Rgb = std::array<double, 3>;
enum class Samples { Float32, Float64 };
enum class Primaries { Srgb, DisplayP3, Bt2020 };
enum class BaseRendition { Sdr, Hdr };
enum class MapBits { Ten = 10, Sixteen = 16 };

// Both renditions must describe the SAME capture, geometry, processing revision,
// linear RGB primaries, and SDR reference-white units. IDs are caller assertions,
// not proof that pixels have actually passed the declared processing stages.
struct Frame {
    std::size_t width = 0, height = 0;
    Primaries primaries = Primaries::Bt2020;
    double reference_white_nits = 0.0;
    std::uint64_t capture_id = 0, geometry_id = 0, processing_id = 0;
};

// Strides and capacity count scalar RGB samples, NOT bytes. No alpha channel.
// Rows may be padded. Pixel storage must be immutable for both analysis/encoding.
struct LinearImageView {
    Frame frame;
    Samples samples = Samples::Float32;
    const void* data = nullptr;
    std::size_t row_stride = 0, capacity = 0;
};

struct Pair { LinearImageView sdr, hdr; };

struct Options {
    BaseRendition base = BaseRendition::Sdr;
    Rgb offset_sdr{{1.0 / 64, 1.0 / 64, 1.0 / 64}};
    Rgb offset_hdr{{1.0 / 64, 1.0 / 64, 1.0 / 64}};
    // ISO/Ultra HDR ENCODING gamma. Android Gainmap.setGamma uses its reciprocal.
    Rgb encoding_gamma{{1, 1, 1}};
    // Explicit display headroom, not guessed from an image's largest pixel.
    double min_display_ratio = 1.0;
    double max_display_ratio = 0.0;  // caller must supply a value > min
};

// Mathematical metadata, NOT a serialized ISO 21496-1 / HEIF metadata block.
struct Metadata {
    Frame frame;
    Options options;
    // Canonical HDR/SDR log2 ratio, also when the base rendition is HDR.
    Rgb min_log2{{0, 0, 0}}, max_log2{{0, 0, 0}};
    Rgb android_decoding_gamma() const;
};

// Unquantized normalized G in [0,1], stored at FP32 or FP64. It is neither RGB
// color nor sRGB-encoded data: do not apply a color transfer function to it.
struct MapView {
    Frame frame;
    Samples samples = Samples::Float64;
    const void* data = nullptr;
    std::size_t row_stride = 0, capacity = 0;
};

// Row memory belongs to the callee and is only valid during the callback.
// Each operation allocates one FP64 RGB row; no full-frame copies are made.
using RowSink = std::function<void(std::size_t y, const double* rgb, std::size_t count)>;

Metadata analyze(const Pair& processed_pair, const Options& options);
void encode_rows(const Pair& processed_pair, const Metadata& metadata, const RowSink& sink);

// At full application: SDR base -> HDR alternate, HDR base -> SDR alternate.
void reconstruct_alternate_rows(const LinearImageView& base, const MapView& map,
                                const Metadata& metadata, const RowSink& sink);
// Matches the Android/Skia gainmap convention: HDR base uses W-1, SDR base W.
// At the base endpoint the base pixels pass through unchanged, including when
// offsets differ. Intermediate offsets are not interpolated or silently altered.
void reconstruct_display_rows(const LinearImageView& base, const MapView& map,
                              const Metadata& metadata, double display_ratio,
                              const RowSink& sink);

// Both formats use uint16_t containers: 10-bit codes occupy bits 0..9 (not P010).
// Callers decide the final encoder byte order/layout. 8-bit maps are rejected.
void quantize(const double* normalized, std::uint16_t* codes, std::size_t count, MapBits bits);
void dequantize(const std::uint16_t* codes, double* normalized, std::size_t count, MapBits bits);

} // namespace hiro::gainmap

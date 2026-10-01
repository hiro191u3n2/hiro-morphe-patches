#pragma once
#include <cstddef>
#include <cstdint>
#include <vector>

namespace ulike::color {
struct Rgba { double r, g, b, a; };
enum class Primaries { Bt709, DisplayP3, Bt2020 };
enum class Transfer { SrgbEncoded, Gamma22Encoded, Linear, Hlg, Pq };
enum class Range { Full, Limited };
struct Encoding {
    Primaries primaries;
    Transfer transfer;
    Range range;
};
enum class Sampling { LinearClampToEdge, NearestClampToEdge };

// Row zero is the texel row addressed near v=0. Upload/decode orientation is
// deliberately a caller contract, not an implicit PNG color/orientation transform.
class Texture {
public:
    Texture(std::size_t width, std::size_t height, std::vector<Rgba> pixels);
    Rgba sample(double u, double v, Sampling sampling) const;
    std::size_t width() const { return width_; }
    std::size_t height() const { return height_; }
private:
    std::size_t width_, height_;
    std::vector<Rgba> pixels_;
};

// Neither asset contains a sufficient standard color-space declaration. The
// integrator must explicitly supply the reference authored encoding and match it.
// HDR/linear input is not accepted: these are SDR-domain style components.
struct Contract {
    Encoding authored;
    Encoding input;
    Sampling sampling;
};

// Exact algebra of Purity2 AmazingFeature9/xshader/pass0.frag. The shader's
// constants remain 512 even for the supplied 1024x1024 LUT image.
Rgba purity_final_lut(const Rgba& encoded_sdr, const Texture& atlas,
                     double uni_alpha, const Contract& contract);

// Exact pointwise algebra of Natural AmazingFeature2 / Purity AmazingFeature8
// skinseg.frag. skin_mask_alpha must come from the matching pixel after the
// shader's vertical mask-coordinate flip. No skin mask is fabricated here.
Rgba skin_background_lut(const Rgba& encoded_sdr, const Texture& background,
                         const Texture& skin, double skin_mask_alpha,
                         double uni_alpha, const Contract& contract);
} // namespace ulike::color

#include "style_color.hpp"
#include <algorithm>
#include <cmath>
#include <limits>
#include <stdexcept>
#include <utility>

namespace ulike::color {
namespace {
bool unit(double x) { return std::isfinite(x) && x >= 0.0 && x <= 1.0; }
void unit_rgba(const Rgba& x) {
    if (!unit(x.r) || !unit(x.g) || !unit(x.b) || !unit(x.a))
        throw std::invalid_argument("finite encoded SDR RGBA in [0,1] required; no HDR clipping");
}
void validate(const Rgba& x, double alpha, const Contract& c) {
    const bool known_primaries = c.authored.primaries == Primaries::Bt709 ||
        c.authored.primaries == Primaries::DisplayP3 || c.authored.primaries == Primaries::Bt2020;
    const bool supported = c.authored.transfer == Transfer::SrgbEncoded ||
                           c.authored.transfer == Transfer::Gamma22Encoded;
    if (!known_primaries || !supported || c.authored.range != Range::Full || c.input.range != Range::Full ||
        c.authored.primaries != c.input.primaries || c.authored.transfer != c.input.transfer)
        throw std::invalid_argument("explicit matching full-range encoded SDR reference required");
    if (c.sampling != Sampling::LinearClampToEdge && c.sampling != Sampling::NearestClampToEdge)
        throw std::invalid_argument("unsupported sampler contract");
    unit_rgba(x);
    if (!unit(alpha)) throw std::invalid_argument("uniAlpha must be finite in [0,1]");
}
void atlas_shape(const Texture& t) {
    if (t.width() != t.height() || t.width() < 8 || t.width() % 8)
        throw std::invalid_argument("square 8x8 atlas required");
}
Rgba mix(const Rgba& a, const Rgba& b, double t) {
    return {a.r + (b.r-a.r)*t, a.g + (b.g-a.g)*t,
            a.b + (b.b-a.b)*t, a.a + (b.a-a.a)*t};
}
Rgba lookup_slice(const Rgba& x, const Texture& t, double slice, Sampling s) {
    const double row = std::floor(slice / 8.0);
    const double column = slice - row * 8.0;
    // Do not replace 512 by texture width: these are authored shader constants.
    const double u = column * 0.125 + 0.5 / 512.0 + (0.125 - 1.0 / 512.0) * x.r;
    const double v = row * 0.125 + 0.5 / 512.0 + (0.125 - 1.0 / 512.0) * x.g;
    return t.sample(u, v, s);
}
}

Texture::Texture(std::size_t width, std::size_t height, std::vector<Rgba> pixels)
    : width_(width), height_(height), pixels_(std::move(pixels)) {
    if (!width_ || !height_ || width_ > std::numeric_limits<std::size_t>::max()/height_ ||
        pixels_.size() != width_*height_ || width_ > 65536 || height_ > 65536)
        throw std::invalid_argument("invalid or oversized texture dimensions");
    for (const auto& p : pixels_) unit_rgba(p);
}

Rgba Texture::sample(double u, double v, Sampling sampling) const {
    if (!std::isfinite(u) || !std::isfinite(v)) throw std::invalid_argument("finite UV required");
    u=std::clamp(u,0.0,1.0); v=std::clamp(v,0.0,1.0);
    if (sampling == Sampling::NearestClampToEdge) {
        const auto x=std::min(width_-1,static_cast<std::size_t>(std::floor(u*width_)));
        const auto y=std::min(height_-1,static_cast<std::size_t>(std::floor(v*height_)));
        return pixels_[y*width_+x];
    }
    if (sampling != Sampling::LinearClampToEdge) throw std::invalid_argument("unsupported sampling");
    const double x=u*width_-0.5, y=v*height_-0.5;
    const auto x0=static_cast<std::int64_t>(std::floor(x));
    const auto y0=static_cast<std::int64_t>(std::floor(y));
    const auto at=[&](std::int64_t ix,std::int64_t iy) {
        ix=std::clamp(ix,std::int64_t{0},static_cast<std::int64_t>(width_-1));
        iy=std::clamp(iy,std::int64_t{0},static_cast<std::int64_t>(height_-1));
        return pixels_[static_cast<std::size_t>(iy)*width_+static_cast<std::size_t>(ix)];
    };
    return mix(mix(at(x0,y0),at(x0+1,y0),x-x0),
               mix(at(x0,y0+1),at(x0+1,y0+1),x-x0),y-y0);
}

Rgba purity_final_lut(const Rgba& x,const Texture& atlas,double alpha,const Contract& c) {
    validate(x,alpha,c);atlas_shape(atlas);
    const double blue=x.b*63.0;
    auto mapped=mix(lookup_slice(x,atlas,std::floor(blue),c.sampling),
                    lookup_slice(x,atlas,std::ceil(blue),c.sampling),blue-std::floor(blue));
    mapped.a=x.a;
    return mix(x,mapped,alpha);
}

Rgba skin_background_lut(const Rgba& x,const Texture& background,const Texture& skin,
                         double mask,double alpha,const Contract& c) {
    validate(x,alpha,c);atlas_shape(background);atlas_shape(skin);
    if (!unit(mask)) throw std::invalid_argument("matching external skin mask alpha required in [0,1]");
    // The source shader clamps here. Domain validation makes that clamp an identity
    // and prevents callers from silently throwing away HDR highlights.
    const double slice=std::floor(x.b*63.0);
    auto mapped=mix(lookup_slice(x,background,slice,c.sampling),
                    lookup_slice(x,skin,slice,c.sampling),mask);
    mapped.a=x.a;
    return mix(x,mapped,alpha);
}
} // namespace ulike::color

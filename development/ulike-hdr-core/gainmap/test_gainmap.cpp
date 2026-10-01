#include "gainmap.hpp"

#include <algorithm>
#include <cmath>
#include <iostream>
#include <limits>
#include <set>
#include <stdexcept>
#include <string>
#include <vector>

using namespace hiro::gainmap;
namespace {
int checks = 0;
void require(bool ok, const std::string& what) {
    ++checks; if (!ok) throw std::runtime_error(what);
}
void near(double got, double wanted, double tolerance, const std::string& what) {
    require(std::isfinite(got) && std::abs(got - wanted) <= tolerance, what);
}
template<class F> void rejects(F f, const std::string& what) {
    bool thrown = false; try { f(); } catch (const std::invalid_argument&) { thrown = true; }
    require(thrown, what);
}
Frame frame(std::size_t w, std::size_t h = 1) {
    return {w, h, Primaries::Bt2020, 203.0, 42, 7, 1};
}
LinearImageView view(const std::vector<double>& v, Frame f, std::size_t stride = 0) {
    return {f, Samples::Float64, v.data(), stride ? stride : f.width * 3, v.size()};
}
MapView map_view(const std::vector<double>& v, const Metadata& m) {
    return {m.frame, Samples::Float64, v.data(), m.frame.width * 3, v.size()};
}
Options options() { Options o; o.max_display_ratio = 16; return o; }
std::vector<double> encode(const Pair& p, const Metadata& m) {
    std::vector<double> out;
    encode_rows(p, m, [&](std::size_t, const double* row, std::size_t n) { out.insert(out.end(), row, row + n); });
    return out;
}
std::vector<double> reconstruct(const LinearImageView& b, const std::vector<double>& g,
                                const Metadata& m, double display = 0) {
    std::vector<double> out;
    const auto sink = [&](std::size_t, const double* row, std::size_t n) { out.insert(out.end(), row, row + n); };
    if (display == 0) reconstruct_alternate_rows(b, map_view(g, m), m, sink);
    else reconstruct_display_rows(b, map_view(g, m), m, display, sink);
    return out;
}
void compare(const std::vector<double>& got, const std::vector<double>& want, double eps,
             const std::string& what) {
    require(got.size() == want.size(), what + " count");
    for (std::size_t i = 0; i < got.size(); ++i) near(got[i], want[i], eps, what);
}

void independent_formula_and_direction() {
    // Known desired gains include attenuation and per-channel, non-unity gamma.
    const std::vector<double> s{0.0, .15, .4, .2, .35, .6, .4, .7, 1.0};
    const std::vector<double> h{.1, .07, 2.0, .9, .5, 1.2, .3, 3.7, .8};
    Options o = options(); o.encoding_gamma = {{.5, 2.0, 1.3}};
    o.offset_sdr = {{.015625, .03, .1}}; o.offset_hdr = {{.04, .02, .07}};
    Pair p{view(s, frame(3)), view(h, frame(3))};
    auto m = analyze(p, o); auto g = encode(p, m);
    compare(reconstruct(p.sdr, g, m), h, 3e-14, "HDR reconstructed with gamma and unequal offsets");
    for (int c = 0; c < 3; ++c) near(m.android_decoding_gamma()[c], 1.0 / o.encoding_gamma[c], 0, "Android gamma is reciprocal");
    // Independent formula, rather than roundtrip-only validation.
    for (std::size_t i = 0; i < s.size(); ++i) {
        const auto c = i % 3;
        const double l = std::log2((h[i] + o.offset_hdr[c]) / (s[i] + o.offset_sdr[c]));
        // Independent log(ratio) and log(h)-log(s) can differ by a last bit at
        // an exact endpoint; normalize that rounding before applying gamma.
        double n = (l - m.min_log2[c]) / (m.max_log2[c] - m.min_log2[c]);
        if (std::abs(n) < 1e-14) n = 0;
        if (std::abs(n - 1) < 1e-14) n = 1;
        const double expected = std::pow(n, o.encoding_gamma[c]);
        near(g[i], expected, 2e-15, "independent encoding formula");
    }
    auto reverse = m; reverse.options.base = BaseRendition::Hdr;
    compare(reconstruct(p.hdr, g, reverse), s, 1e-14, "HDR base inverse reconstruction");
    compare(reconstruct(p.sdr, g, m, 1), s, 0, "SDR base endpoint identity with unequal offsets");
    compare(reconstruct(p.hdr, g, reverse, 16), h, 0, "HDR base endpoint identity with unequal offsets");
    compare(reconstruct(p.sdr, g, m, 16), h, 3e-14, "display full HDR");
    compare(reconstruct(p.hdr, g, reverse, 1), s, 1e-14, "display full SDR from HDR base");
    const auto mid = reconstruct(p.sdr, g, m, 4); // half the log-headroom
    const auto mid_reverse = reconstruct(p.hdr, g, reverse, 4);
    for (std::size_t i = 0; i < s.size(); ++i) {
        const auto c = i % 3;
        const double gain = (h[i] + o.offset_hdr[c]) / (s[i] + o.offset_sdr[c]);
        near(mid[i], (s[i] + o.offset_sdr[c]) * std::sqrt(gain) - o.offset_hdr[c], 2e-14, "mid-headroom positive weight");
        near(mid_reverse[i], (h[i] + o.offset_hdr[c]) / std::sqrt(gain) - o.offset_sdr[c], 2e-14, "mid-headroom negative weight");
    }
}

void quantization_quality() {
    constexpr std::size_t width = 4096;
    std::vector<double> s(width * 3), h(s.size());
    const auto o = options();
    for (std::size_t x = 0; x < width; ++x) for (int c = 0; c < 3; ++c) {
        const auto i = x * 3 + c; s[i] = .05 + .9 * ((x * 101 + c * 19) % width) / (width - 1.0);
        const double log_gain = -0.5 + (3.0 + .25 * c) * x / (width - 1.0);
        h[i] = (s[i] + o.offset_sdr[c]) * std::exp2(log_gain) - o.offset_hdr[c];
    }
    Pair p{view(s, frame(width)), view(h, frame(width))};
    const auto m = analyze(p, o); const auto g = encode(p, m);
    compare(reconstruct(p.sdr, g, m), h, 3e-14, "FP64 unquantized reconstruction");
    double previous_mse = 1e99;
    for (const auto bits : {MapBits::Ten, MapBits::Sixteen}) {
        const unsigned levels = bits == MapBits::Ten ? 1023 : 65535;
        std::vector<std::uint16_t> codes(g.size()); std::vector<double> decoded(g.size());
        quantize(g.data(), codes.data(), g.size(), bits);
        dequantize(codes.data(), decoded.data(), g.size(), bits);
        auto output = reconstruct(p.sdr, decoded, m); double mse = 0, maxerr = 0;
        std::set<std::uint16_t> distinct;
        for (std::size_t i = 0; i < g.size(); ++i) {
            near(decoded[i], g[i], .5 / levels + 2e-16, "round-to-nearest quantization bound");
            const auto c = i % 3;
            const double max_log_error = (m.max_log2[c] - m.min_log2[c]) * .5 / levels;
            const double bound = (h[i] + o.offset_hdr[c]) * std::expm1(std::log(2.0) * max_log_error);
            near(output[i], h[i], bound + 3e-14, "independent reconstructed HDR error bound");
            const double error = output[i] - h[i]; mse += error * error; maxerr = std::max(maxerr, std::abs(error));
            if (c == 0) distinct.insert(codes[i]);
        }
        mse /= g.size(); require(distinct.size() > 256, "map not silently quantized to eight bits");
        require(mse < previous_mse / 100, "16-bit quantization measurably improves over ten-bit");
        previous_mse = mse;
        std::cout << "quantization bits=" << static_cast<int>(bits) << " levels_observed=" << distinct.size()
                  << " HDR_max_abs_error=" << maxerr << " HDR_rmse=" << std::sqrt(mse) << '\n';
    }
    std::vector<float> sf(s.begin(), s.end()), hf(h.begin(), h.end());
    Pair pf{{frame(width), Samples::Float32, sf.data(), width * 3, sf.size()},
            {frame(width), Samples::Float32, hf.data(), width * 3, hf.size()}};
    const auto mf = analyze(pf, o); const auto gf = encode(pf, mf);
    compare(reconstruct(pf.sdr, gf, mf), std::vector<double>(hf.begin(), hf.end()), 3e-14, "FP32 input preserved in FP64 math");
    std::vector<float> float_map(g.begin(), g.end());
    const MapView fm{m.frame, Samples::Float32, float_map.data(), width * 3, float_map.size()};
    reconstruct_alternate_rows(p.sdr, fm, m, [&](std::size_t y, const double* row, std::size_t n) {
        for (std::size_t x = 0; x < n; ++x) {
            const auto i = y * width * 3 + x, c = x % 3;
            const double max_log_error = (m.max_log2[c] - m.min_log2[c]) * std::exp2(-25.0);
            const double bound = (h[i] + o.offset_hdr[c]) * std::expm1(std::log(2.0) * max_log_error);
            near(row[x], h[i], bound + 3e-14, "FP32 map reconstruction error bound");
        }
    });
}

void edited_content_requires_new_map() {
    const Frame before = frame(32, 2); Frame after = before; after.processing_id++; after.geometry_id++;
    std::vector<double> h(192), s(192), edited_h(192), edited_s(192);
    for (std::size_t i = 0; i < h.size(); ++i) { h[i] = .1 + 5.0 * ((i * 37) % 191) / 191.0; s[i] = h[i] / (1 + h[i]); }
    // Reproject pixels and apply channel/local exposure changes, representing
    // post-beauty geometry/color edits, then derive its matching SDR rendition.
    for (std::size_t i = 0; i < h.size(); ++i) {
        edited_h[i] = h[(i + 9) % h.size()] * (i % 3 == 0 ? .55 : 1.6) + (i < 90 ? .3 : 0);
        edited_s[i] = edited_h[i] / (1 + edited_h[i]);
    }
    Pair original{view(s, before), view(h, before)}, edited{view(edited_s, after), view(edited_h, after)};
    const auto old_m = analyze(original, options()); const auto old_g = encode(original, old_m);
    rejects([&] { reconstruct(edited.sdr, old_g, old_m); }, "old map rejected after geometry/processing edit");
    // Deliberately lie about identity to measure the actual visual error of the
    // stale-map anti-pattern, not merely whether metadata validation is present.
    auto wrongly_relabelled = edited.sdr; wrongly_relabelled.frame = before;
    const auto stale = reconstruct(wrongly_relabelled, old_g, old_m);
    double old_error = 0; for (std::size_t i = 0; i < stale.size(); ++i) old_error = std::max(old_error, std::abs(stale[i] - edited_h[i]));
    require(old_error > 1.0, "old gainmap cannot recover processed HDR");
    const auto new_m = analyze(edited, options()); const auto new_g = encode(edited, new_m);
    compare(reconstruct(edited.sdr, new_g, new_m), edited_h, 5e-14, "recomputed map reproduces edited HDR");
    std::cout << "post_edit stale_map_max_abs_error=" << old_error << " recomputed_tolerance=5e-14\n";
}

void boundaries_and_rejections() {
    std::vector<double> s{0, .2, .6}, h{0, .4, 1.2}; Pair p{view(s, frame(1)), view(h, frame(1))};
    auto o = options(); o.offset_hdr.fill(0); o.offset_sdr.fill(0);
    const auto m = analyze(p, o); const auto g = encode(p, m);
    require(g == std::vector<double>({0, 0, 0}), "constant gain encodes without division by zero");
    compare(reconstruct(p.sdr, g, m), h, 3e-16, "black/zero-offset and constant gain");
    auto bad = p; bad.hdr.frame.primaries = Primaries::DisplayP3;
    rejects([&] { analyze(bad, o); }, "mismatched primaries rejected");
    bad = p; bad.hdr.frame.geometry_id++;
    rejects([&] { analyze(bad, o); }, "mismatched geometry rejected");
    bad = p; bad.hdr.frame.reference_white_nits = 80;
    rejects([&] { analyze(bad, o); }, "mismatched luminance units rejected");
    bad = p; bad.hdr.frame.primaries = static_cast<Primaries>(999);
    rejects([&] { analyze(bad, o); }, "unknown color rejected");
    bad = p; bad.hdr.capacity = 2;
    rejects([&] { analyze(bad, o); }, "short buffer rejected");
    bad = p; bad.hdr.row_stride = 2;
    rejects([&] { analyze(bad, o); }, "short row rejected");
    bad = p; bad.hdr.frame.width = std::numeric_limits<std::size_t>::max();
    rejects([&] { analyze(bad, o); }, "dimension overflow rejected");
    auto saved = h[0];
    for (const auto invalid : {-1.0, std::numeric_limits<double>::infinity(), std::numeric_limits<double>::quiet_NaN()}) {
        h[0] = invalid; rejects([&] { analyze(p, o); }, "invalid RGB rejected");
    }
    h[0] = .1; rejects([&] { analyze(p, o); }, "one zero endpoint without offset rejected"); h[0] = saved;
    auto bad_o = o; bad_o.encoding_gamma[0] = 0;
    rejects([&] { analyze(p, bad_o); }, "zero gamma rejected");
    bad_o = o; bad_o.offset_hdr[0] = -.01;
    rejects([&] { analyze(p, bad_o); }, "negative offset rejected");
    bad_o = o; bad_o.max_display_ratio = 1;
    rejects([&] { analyze(p, bad_o); }, "invalid headroom rejected");
    auto bad_m = m; bad_m.max_log2[0] = std::numeric_limits<double>::infinity();
    rejects([&] { reconstruct(p.sdr, g, bad_m); }, "invalid metadata rejected");
    auto wrong_map = map_view(g, m); wrong_map.frame.capture_id++;
    rejects([&] { reconstruct_alternate_rows(p.sdr, wrong_map, m, [](std::size_t, const double*, std::size_t) {}); }, "mismatched map identity rejected");
    std::uint16_t code = 0; double value = .5;
    rejects([&] { quantize(&value, &code, 1, static_cast<MapBits>(8)); }, "eight-bit map rejected");
    value = 1.01; rejects([&] { quantize(&value, &code, 1, MapBits::Ten); }, "map range rejected");
    code = 1024; rejects([&] { dequantize(&code, &value, 1, MapBits::Ten); }, "out-of-range ten-bit code rejected");
    rejects([&] { reconstruct(p.sdr, g, m, .5); }, "display below SDR white rejected");
    auto narrow = m; narrow.options.min_display_ratio = 1e308;
    narrow.options.max_display_ratio = std::nextafter(1e308, std::numeric_limits<double>::infinity());
    compare(reconstruct(p.sdr, g, narrow, narrow.options.min_display_ratio), s, 0, "adjacent high-headroom base endpoint");
    compare(reconstruct(p.sdr, g, narrow, narrow.options.max_display_ratio), h, 3e-16, "adjacent high-headroom HDR endpoint");
    // Very large gain, where naive HDR/SDR division and exp2(gain) overflow.
    std::vector<double> tiny(3, 1e-200), huge(3, 1e200);
    Pair extremes{view(tiny, frame(1)), view(huge, frame(1))};
    const auto em = analyze(extremes, o); const auto eg = encode(extremes, em);
    for (double v : reconstruct(extremes.sdr, eg, em)) near(v / 1e200, 1, 1e-12, "extreme finite ratio reconstruction");
    // Extreme gamma must not silently collapse a representable middle sample.
    std::vector<double> flat(9, 1), varying{1,1,1,2,2,2,4,4,4};
    Pair gp{view(flat, frame(3)), view(varying, frame(3))}; auto go = o; go.encoding_gamma.fill(1e308);
    const auto gm = analyze(gp, go); rejects([&] { encode(gp, gm); }, "gamma underflow rejected");
    // Padding is deliberately NaN; only valid samples may be touched.
    const double nan = std::numeric_limits<double>::quiet_NaN();
    std::vector<double> padded_s{.2,.3,.4,nan,nan,.5,.6,.7};
    std::vector<double> padded_h{.4,.9,1.6,nan,nan,1,1.8,2.8};
    Pair pp{view(padded_s, frame(1,2),5), view(padded_h, frame(1,2),5)};
    const auto pm=analyze(pp,options()); const auto pg=encode(pp,pm);
    compare(reconstruct(pp.sdr,pg,pm), {.4,.9,1.6,1,1.8,2.8}, 2e-14, "padded rows streamed correctly");
}
} // namespace

int main() {
    try {
        independent_formula_and_direction(); quantization_quality();
        edited_content_requires_new_map(); boundaries_and_rejections();
        std::cout << "PASS checks=" << checks << " scope=CPU_gainmap_math_only\n";
    } catch (const std::exception& e) { std::cerr << "FAIL " << e.what() << '\n'; return 1; }
}

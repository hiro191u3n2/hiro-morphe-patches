#include "style_color.hpp"
#include <cmath>
#include <iostream>
#include <limits>
#include <stdexcept>
#include <functional>
using namespace ulike::color;
static int checks=0;
static void expect(bool b) { ++checks; if(!b) throw std::runtime_error("test assertion failed"); }
static void near(double a,double b) { expect(std::abs(a-b)<1e-12); }
static void reject(const std::function<void()>& f) {
    bool threw=false;try { f(); }catch(const std::invalid_argument&) { threw=true; }expect(threw);
}
int main() {
    const Encoding ref{Primaries::Bt709,Transfer::SrgbEncoded,Range::Full};
    const Contract c{ref,ref,Sampling::LinearClampToEdge};
    Texture corners(2,2,{{0,0,0,1},{1,0,0,1},{0,1,0,1},{1,1,1,1}});
    auto q=corners.sample(.5,.5,c.sampling);near(q.r,.5);near(q.g,.5);near(q.b,.25);
    q=corners.sample(-3,2,c.sampling);near(q.r,0);near(q.g,1);
    q=corners.sample(.9,.1,Sampling::NearestClampToEdge);near(q.r,1);near(q.g,0);
    reject([&]{corners.sample(std::nan(""),.5,c.sampling);});
    reject([&]{Texture(0,2,{});});
    reject([&]{Texture(2,2,{{0,0,0,1}});});
    reject([&]{Texture(1,1,{{0,std::nan(""),0,1}});});
    std::vector<Rgba> identity(512*512), constant(512*512,{.2,.4,.8,1});
    for(std::size_t y=0;y<512;++y)for(std::size_t x=0;x<512;++x)
        identity[y*512+x]={double(x%64)/63,double(y%64)/63,double((y/64)*8+x/64)/63,1};
    Texture id(512,512,std::move(identity)), flat(512,512,std::move(constant));
    // A conventional identity atlas is an independent oracle for the two-slice pass.
    for(int i=0;i<1024;++i) {
        Rgba p{double((i*37)%1024)/1023,double((i*59)%1024)/1023,double(i)/1023,.37};
        for(double alpha:{0.,.37,1.}) {
            const auto r=purity_final_lut(p,id,alpha,c);
            near(r.r,p.r);near(r.g,p.g);near(r.b,p.b);near(r.a,p.a);
        }
        const auto r=skin_background_lut(p,id,flat,0.,1.,c);
        near(r.r,p.r);near(r.g,p.g);near(r.b,std::floor(p.b*63)/63);near(r.a,p.a);
    }
    Rgba p{.21,.43,.65,.8};
    auto r=purity_final_lut(p,flat,.5,c);near(r.r,.205);near(r.g,.415);near(r.b,.725);near(r.a,.8);
    r=skin_background_lut(p,id,flat,1.,1.,c);near(r.r,.2);near(r.g,.4);near(r.b,.8);near(r.a,p.a);
    for(auto tr:{Transfer::Linear,Transfer::Hlg,Transfer::Pq}) {
        Contract bad=c;bad.input.transfer=tr;
        reject([&]{purity_final_lut(p,id,1,bad);});
        bad.authored.transfer=tr;
        reject([&]{purity_final_lut(p,id,1,bad);});
    }
    Contract bad=c;bad.input.primaries=Primaries::DisplayP3;
    reject([&]{purity_final_lut(p,id,1,bad);});
    bad=c;bad.input.range=Range::Limited;reject([&]{purity_final_lut(p,id,1,bad);});
    bad=c;bad.authored.primaries=bad.input.primaries=static_cast<Primaries>(99);
    reject([&]{purity_final_lut(p,id,1,bad);});
    reject([&]{purity_final_lut({1.001,.5,.5,1},id,1,c);});
    reject([&]{skin_background_lut({2,.5,.5,1},id,flat,1,1,c);});
    reject([&]{purity_final_lut({-.001,.5,.5,1},id,1,c);});
    reject([&]{purity_final_lut({.5,.5,std::nan(""),1},id,1,c);});
    reject([&]{purity_final_lut(p,id,std::numeric_limits<double>::infinity(),c);});
    reject([&]{purity_final_lut(p,id,1.1,c);});
    reject([&]{skin_background_lut(p,id,flat,std::nan(""),1,c);});
    reject([&]{skin_background_lut(p,id,flat,-.1,1,c);});
    reject([&]{purity_final_lut(p,corners,1,c);});
    std::cout<<"PASS "<<checks<<" numeric/contract assertions\n";
}

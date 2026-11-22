package app.hiro.berry.links;

import android.net.Uri;

public final class ProfileLinkCases {
    public static void main(String[] args) {
        String base = "https://www.instagram.com/";
        String profile = args.length == 0 ? base + "profile.example_3/?hl=ja" : args[0];
        String[] yes = {profile, base + "_example", base + "p/aBc_12/", base + "reel/a-B/", base + "tv/X/",
                "https://instagram.com/test.1/?igsh=sample"};
        for (String url : yes) expect(url, url);
        for (String host : new String[]{"www.google.com", "www.google.co.jp"}) {
            for (String param : new String[]{"url","q"}) {
                expect("https://"+host+"/url?sa=t&"+param+"="+Uri.encode(profile), profile);
            }
        }
        String[] no = {base,base+"accounts/",base+"direct/", base+"explore/",base+"p/",base+"reels/",
                base+"two/paths/",base+"a%2Fb/",base+"a..b/", base+"a./",base+".",base+"../",base+"robots.txt",
                base+"1234567890123456789012345678901/", "https://www.instagram.com.invalid/account/",
                "https://www.instagram.com@example.org/user/", "http://instagram.com/user/",
                "https://www.google.com/url?q="+Uri.encode("https://example.org/name"),
                "https://example.org/url?q="+Uri.encode(profile)};
        for (String url : no) expect(url, null);
        System.out.println("Passed "+(yes.length+4+no.length)+" URL cases on Android.");
    }
    private static void expect(String input, String expected) {
        Uri result = InstagramPostLinks.instagramUrl(Uri.parse(input));
        if (expected==null ? result!=null : !expected.equals(String.valueOf(result)))
            throw new AssertionError(input+" returned "+result);
    }
}

package app.hiro.mapsstrict;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Host-side behavioral tests, no Maps or Android mocks. */
public final class StrictNameMatcherTest {
    private static int checks;

    public static void main(String[] args) {
        screenshotRegression();
        aliasesAndBranches();
        wordBoundaries();
        ordinaryCategorySearches();
        addressesAndNavigation();
        unknownNamesAndAreas();
        missingDataAndNormalization();
        System.out.println("PASS StrictNameMatcherTest: " + checks + " behavioral assertions");
    }

    private static void screenshotRegression() {
        StrictNameMatcher.Query query = StrictNameMatcher.classify("ランダ");
        check(query.enabled, "RANDA search must be strict");
        List<String> results = Arrays.asList("ランダ銀座インズ店", "RANDA 有楽町マルイ店", "リランドチュール", "シューファンタジー 有楽町マルイ店", "GINZA WASHINGTON 銀座本店", "ダイアナ 銀座本店");
        List<String> kept = new ArrayList<String>();
        for (String name : results) if (StrictNameMatcher.matches(query, name)) kept.add(name);
        check(kept.equals(Arrays.asList("ランダ銀座インズ店", "RANDA 有楽町マルイ店")), "Screenshot must retain exactly both RANDA branches: " + kept);
        // New page/pan result batches must use the identical plan.
        check(!StrictNameMatcher.matches(query, "ABC-MART 銀座店"), "Later result must still be filtered");
        check(StrictNameMatcher.matches(query, "RANDA 新宿店"), "Later RANDA branch must stay");
    }

    private static void aliasesAndBranches() {
        keep("RANDA", "ランダ銀座インズ店");
        keep("ランダ", "RANDA 有楽町マルイ店");
        keep("ＲＡＮＤＡ", "randa 銀座店");
        keep("らんだ", "RANDA 銀座店");
        keep("ﾗﾝﾀﾞ", "ランダ 銀座店");
        keep(" ランダ　", "ＲＡＮＤＡ　有楽町マルイ店");
        keep("銀座 ランダ", "RANDA 有楽町マルイ店");
        keep("ランダ 銀座", "ランダ銀座インズ店");
        keep("銀座のランダ", "RANDA 銀座インズ店");
        keep("ランダ銀座インズ店", "RANDA 銀座インズ店");
        keep("RANDA near Ginza", "ランダ銀座店");
        keep("ランダ 福生店", "RANDA 福生店");
        keep("ランダ", "ランダルミネエスト新宿店");
        keep("ランダ", "ランダららぽーと船橋店");
        keep("ランダ", "ランダ アミュプラザ博多店");
        keep("RANDA", "株式会社RANDA 銀座店");
        keep("スタバ", "STARBUCKS COFFEE 銀座店");
        keep("スターバックス", "スターバックスコーヒー銀座店");
        keep("タリーズ", "TULLY'S COFFEE 有楽町店");
        keep("ドトール", "ドトールコーヒーショップ 銀座店");
        keep("ABCマート", "ABC-MART 銀座店");
        keep("アンミール", "An MILLE 新宿店");
        keep("オリーブデオリーブ", "OLIVE des OLIVE 新宿店");
        keep("ジルバイジルスチュアート", "JILL by JILL STUART 新宿店");
        keep("GU", "ジーユー 銀座店");
        keep("セブンイレブン", "7-ELEVEN 銀座店");
        keep("ファミマ", "FamilyMart 銀座店");
        reject("ランダ", "ダイアナ 銀座店");
        reject("GU", "UNIQLO 銀座店");
        reject("スタバ", "タリーズコーヒー 有楽町店");
    }

    private static void wordBoundaries() {
        reject("RANDA", "MIRANDA GINZA");
        reject("RANDA", "RANDALL SHOES");
        reject("RANDA", "RANDAHARA");
        reject("ランダ", "グランダ 銀座");
        reject("ランダ", "オランダ料理店");
        reject("ランダ", "ランダムカフェ");
        reject("ランダ", "ランダム店");
        reject("GU", "GUCCI 銀座店");
        keep("RANDA", "RANDA / 銀座店");
        keep("RANDA", "RANDA（有楽町マルイ店）");
        keep("ランダ", "銀座ランダ");
        reject("JILL by JILL STUART", "JILL STUART 新宿店");
        // No unsupported automatic Japanese/English transliteration is invented.
        reject("架空ブランド", "Imaginary Brand");
    }

    private static void ordinaryCategorySearches() {
        String[] categories = {
            "靴屋", "靴", "銀座 靴屋", "銀座の靴屋", "東京で靴屋", "靴屋 銀座", "喫煙所", "有楽町 喫煙所", "近くの喫煙所", "新宿駅 周辺 喫煙所",
            "カフェ", "喫煙できるカフェ", "おしゃれなカフェ", "安い 居酒屋", "ホテル 銀座", "近くのホテル", "ラーメン", "銀座 ラーメン", "猫 カフェ",
            "個室 焼肉", "メイドカフェ", "靴 修理", "子供 靴", "新宿駅近くのレストラン", "レディース 靴", "100円ショップ", "ATM", "駐車場",
            "shoe stores near me", "restaurants in Ginza", "coffee shops", "smoking area", "hotels near Tokyo", "free parking"
        };
        for (String category : categories) {
            StrictNameMatcher.Query query = StrictNameMatcher.classify(category);
            check(!query.enabled, "Category must not become name-only: " + category + " -> " + query.reason);
            check(StrictNameMatcher.matches(query, "店名に検索語が含まれていないお店"), "Category must retain native result: " + category);
        }
        bypass("ランダ おすすめ", "Ambiguous brand + advice term");
        bypass("ランダ 靴", "Brand + category intent remains conservative");
        bypass("ランダ ダイアナ", "Multiple brand request remains conservative");
    }

    private static void addressesAndNavigation() {
        String[] queries = {"東京", "銀座", "オランダ", "京都市", "横芝光町", "東京駅", "羽田空港", "東京都千代田区1-1", "銀座4丁目", "〒104-0061", "35.6717,139.7650", "9Q8C+7R 東京", "東京から銀座まで", "ランダ 行き方", "ランダへの経路", "新宿→銀座", "directions to RANDA", "https://maps.google.com/?q=RANDA", "geo:35.67,139.76?q=RANDA"};
        for (String query : queries) bypass(query, "Address/navigation query");
    }

    private static void unknownNamesAndAreas() {
        keep("しゃぶ耀", "しゃぶ耀 銀座店");
        keep("銀座 しゃぶ耀", "しゃぶ耀 銀座本店");
        keep("しゃぶ耀 銀座", "銀座しゃぶ耀");
        reject("しゃぶ耀", "銀座しゃぶしゃぶ専門店");
        keep("架空珈琲", "架空珈琲 銀座店");
        keep("架空珈琲", "架空珈琲銀座店");
        keep("架空珈琲", "架空珈琲菊川店");
        reject("架空珈琲", "普通の喫茶店");
        keep("Acme Boutique", "ACME BOUTIQUE Ginza");
        keep("Acme Boutique", "Acme-Boutique Ginza");
        keep("Acme Boutique Ginza", "ACME BOUTIQUE 銀座店");
        reject("Acme Boutique", "MyAcmeBoutique2");
        keep("カフェねこ", "カフェねこ 銀座店");
        reject("カフェねこ", "カフェいぬ 銀座店");
        keep("帝国ホテル", "帝国ホテル 東京");
        reject("帝国ホテル", "別のホテル");
        // Do not mistake a substring of an unrelated name for the RANDA alias.
        keep("グランダ", "グランダ 成城");
        reject("グランダ", "ランダ 銀座店");
        bypass("どこで靴を買える", "Natural-language search");
        bypass("where can I buy shoes", "Natural-language search");
        bypass("A", "Single letter would hide almost all results");
        bypass("109", "Bare number is ambiguous");
    }

    private static void missingDataAndNormalization() {
        bypass(null, "Null query");
        bypass("", "Empty query");
        bypass("　 ", "Whitespace query");
        check(StrictNameMatcher.shouldKeep("ランダ", null), "Null name remains visible");
        check(StrictNameMatcher.shouldKeep("ランダ", "　"), "Missing name remains visible");
        check(StrictNameMatcher.matches(null, "ダイアナ"), "Missing plan keeps original data");
        keep("ＲＡＮＤＡ", "R\u200bANDA 銀座店");
        check("ランダ randa".equals(StrictNameMatcher.normalize(" らんだ　ＲＡＮＤＡ ")), "Unicode normalization");
        StrictNameMatcher.Query randa = StrictNameMatcher.classify("ランダ");
        StrictNameMatcher.Query diana = StrictNameMatcher.classify("ダイアナ");
        check(StrictNameMatcher.matches(randa, "RANDA 銀座店"), "A subsequent classification must not mutate previous plan");
        check(!StrictNameMatcher.matches(diana, "RANDA 銀座店"), "Different search must not reuse prior aliases");
    }

    private static void keep(String query, String name) {
        StrictNameMatcher.Query plan = StrictNameMatcher.classify(query);
        check(plan.enabled, "Expected strict name plan: " + query + " -> " + plan.reason);
        check(StrictNameMatcher.matches(plan, name), "Must keep [" + name + "] for [" + query + "]");
    }

    private static void reject(String query, String name) {
        StrictNameMatcher.Query plan = StrictNameMatcher.classify(query);
        check(plan.enabled, "Expected strict name plan: " + query + " -> " + plan.reason);
        check(!StrictNameMatcher.matches(plan, name), "Must reject [" + name + "] for [" + query + "]");
    }

    private static void bypass(String query, String label) {
        StrictNameMatcher.Query plan = StrictNameMatcher.classify(query);
        check(!plan.enabled, label + ": " + query + " -> " + plan.reason);
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}

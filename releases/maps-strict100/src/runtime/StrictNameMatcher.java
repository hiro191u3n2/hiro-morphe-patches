package app.hiro.mapsstrict;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Name-only filtering policy. This class deliberately has no Android or Maps
 * dependencies: the caller supplies the committed query and primary place name.
 * A disabled query, or a missing primary name, is always retained.
 */
public final class StrictNameMatcher {
    private StrictNameMatcher() { }

    /** Immutable, reusable plan for one committed search. */
    public static final class Query {
        public final boolean enabled;
        public final String reason;
        public final String normalizedQuery;
        private final String[] names;

        private Query(boolean enabled, String reason, String normalizedQuery, String[] names) {
            this.enabled = enabled;
            this.reason = reason;
            this.normalizedQuery = normalizedQuery;
            this.names = names.clone();
        }
    }

    private static final class Brand {
        final String[] names;
        Brand(String... names) {
            this.names = new String[names.length];
            for (int i = 0; i < names.length; i++) this.names[i] = compact(names[i]);
        }
    }

    /* Explicit identity aliases, not arbitrary phonetic guesses. */
    private static final Brand[] BRANDS = {
        new Brand("RANDA", "ランダ"),
        new Brand("DIANA", "ダイアナ"),
        new Brand("ABC-MART", "ABCマート", "エービーシーマート"),
        new Brand("evelyn", "エブリン"),
        new Brand("An MILLE", "AnMILLE", "アンミール"),
        new Brand("ROJITA", "ロジータ"),
        new Brand("INGNI", "イング"),
        new Brand("LIZ LISA", "リズリサ"),
        new Brand("SNIDEL", "スナイデル"),
        new Brand("FRAY I.D", "フレイアイディー"),
        new Brand("MERCURYDUO", "マーキュリーデュオ"),
        new Brand("dazzlin", "ダズリン"),
        new Brand("OLIVE des OLIVE", "オリーブデオリーブ"),
        new Brand("JILL by JILL STUART", "ジルバイジルスチュアート"),
        new Brand("JILL STUART", "ジルスチュアート"),
        new Brand("MARY QUANT", "マリークヮント", "マリークワント"),
        new Brand("SAMANTHA THAVASA", "サマンサタバサ"),
        new Brand("CHARLES & KEITH", "チャールズアンドキース"),
        new Brand("earth music&ecology", "アースミュージックアンドエコロジー"),
        new Brand("WEGO", "ウィゴー"),
        new Brand("GU", "ジーユー"),
        new Brand("UNIQLO", "ユニクロ"),
        new Brand("ZARA", "ザラ"),
        new Brand("H&M", "エイチアンドエム"),
        new Brand("MUJI", "無印良品"),
        new Brand("IKEA", "イケア"),
        new Brand("NITORI", "ニトリ"),
        new Brand("STARBUCKS", "STARBUCKS COFFEE", "スターバックス", "スターバックスコーヒー", "スタバ"),
        new Brand("TULLY'S", "TULLYS", "TULLY'S COFFEE", "タリーズ", "タリーズコーヒー"),
        new Brand("DOUTOR", "DOUTOR COFFEE", "ドトール", "ドトールコーヒー", "ドトールコーヒーショップ"),
        new Brand("EXCELSIOR CAFFE", "エクセルシオールカフェ"),
        new Brand("CAFE de CRIE", "カフェドクリエ"),
        new Brand("BLUE BOTTLE COFFEE", "BLUE BOTTLE", "ブルーボトルコーヒー", "ブルーボトル"),
        new Brand("KOMEDA'S COFFEE", "コメダ珈琲店", "コメダ珈琲", "コメダ"),
        new Brand("McDonald's", "McDonalds", "マクドナルド", "マクド"),
        new Brand("MOS BURGER", "モスバーガー", "モス"),
        new Brand("KFC", "KENTUCKY FRIED CHICKEN", "ケンタッキーフライドチキン", "ケンタッキー"),
        new Brand("BURGER KING", "バーガーキング"),
        new Brand("WENDY'S FIRST KITCHEN", "ウェンディーズファーストキッチン"),
        new Brand("Saizeriya", "サイゼリヤ", "サイゼ"),
        new Brand("GUSTO", "ガスト"),
        new Brand("Bamiyan", "バーミヤン"),
        new Brand("SEVEN ELEVEN", "7 ELEVEN", "7-ELEVEN", "セブンイレブン"),
        new Brand("FamilyMart", "ファミリーマート", "ファミマ"),
        new Brand("LAWSON", "ローソン"),
        new Brand("MINISTOP", "ミニストップ"),
        new Brand("SUKIYA", "すき家"),
        new Brand("YOSHINOYA", "吉野家"),
        new Brand("KURA SUSHI", "くら寿司", "無添くら寿司"),
        new Brand("SUSHIRO", "スシロー"),
        new Brand("HAMA SUSHI", "はま寿司"),
        new Brand("TORIKIZOKU", "鳥貴族"),
        new Brand("DON QUIJOTE", "DONKI", "ドンキホーテ", "ドンキ"),
        new Brand("MATSUMOTO KIYOSHI", "マツモトキヨシ", "マツキヨ"),
        new Brand("SUND RUG", "SUNDRUG", "サンドラッグ"),
        new Brand("WELCIA", "ウエルシア"),
        new Brand("BIC CAMERA", "ビックカメラ"),
        new Brand("YODOBASHI CAMERA", "ヨドバシカメラ"),
        new Brand("YAMADA DENKI", "ヤマダ電機", "ヤマダデンキ"),
        new Brand("TSUTAYA", "ツタヤ"),
        new Brand("DAISO", "ダイソー"),
        new Brand("Seria", "セリア"),
        new Brand("Can Do", "キャンドゥ"),
        new Brand("LUMINE", "ルミネ"),
        new Brand("PARCO", "パルコ"),
        new Brand("MARUI", "マルイ", "丸井")
    };

    private static final String[] LOCATIONS = keys(
        "日本|北海道|東京|東京都|神奈川|神奈川県|千葉|千葉県|埼玉|埼玉県|茨城|栃木|群馬|" +
        "青森|岩手|宮城|秋田|山形|福島|新潟|富山|石川|福井|山梨|長野|岐阜|静岡|愛知|三重|" +
        "滋賀|京都|京都府|大阪|大阪府|兵庫|奈良|和歌山|鳥取|島根|岡山|広島|山口|徳島|香川|愛媛|高知|" +
        "福岡|佐賀|長崎|熊本|大分|宮崎|鹿児島|沖縄|札幌|仙台|金沢|名古屋|神戸|那覇|" +
        "銀座|有楽町|日比谷|丸の内|大手町|日本橋|京橋|新橋|汐留|新宿|渋谷|原宿|表参道|青山|" +
        "恵比寿|代官山|目黒|五反田|品川|大崎|田町|浜松町|池袋|大塚|巣鴨|駒込|高田馬場|" +
        "中野|高円寺|阿佐ヶ谷|荻窪|吉祥寺|三鷹|立川|八王子|町田|下北沢|自由が丘|二子玉川|" +
        "赤坂|六本木|麻布|上野|御徒町|秋葉原|神田|浅草|浅草橋|押上|錦糸町|北千住|南千住|" +
        "亀戸|葛西|西葛西|豊洲|お台場|台場|板橋|練馬|赤羽|蒲田|大森|大井町|" +
        "浦安|新浦安|舞浜|船橋|西船橋|本八幡|市川|津田沼|松戸|柏|流山|成田|海浜幕張|" +
        "横浜|川崎|武蔵小杉|溝の口|新横浜|みなとみらい|桜木町|関内|藤沢|辻堂|鎌倉|大船|" +
        "大宮|浦和|川口|越谷|川越|所沢|梅田|難波|なんば|心斎橋|天王寺|京橋|三宮|烏丸|四条|" +
        "天神|博多|小倉|中洲|太宰府|栄|金山|熱田|大須|" +
        "港区|中央区|千代田区|新宿区|渋谷区|豊島区|台東区|墨田区|江東区|品川区|目黒区|世田谷区|" +
        "中野区|杉並区|北区|荒川区|板橋区|練馬区|足立区|葛飾区|江戸川区|大田区|文京区|" +
        "tokyo|ginza|yurakucho|shinjuku|shibuya|harajuku|ikebukuro|yokohama|osaka|kyoto|fukuoka|hakata|" +
        "sapporo|nagoya|sendai|kobe|japan|オランダ|韓国|中国|台湾|香港|アメリカ|フランス|イギリス|" +
        "イタリア|ドイツ|パリ|ロンドン|ニューヨーク|ソウル|台北|ホノルル|バンコク"
    );
    private static final String[] VENUES = keys(
        "ルミネ|ルミネエスト|ルミネストリート|マルイ|丸井|パルコ|インズ|アトレ|アトレヴィ|エキュート|" +
        "グランスタ|ララポート|ララガーデン|ラゾーナ|ララスクエア|ダイバーシティ|ソラマチ|" +
        "イオン|イオンモール|イオンタウン|イトーヨーカドー|アリオ|ユメタウン|ユメシティ|" +
        "ジョイナス|モディ|ミロード|サンシャイン|サンシャインシティ|ペリエ|シャポー|ビーンズ|" +
        "タカシマヤ|高島屋|伊勢丹|三越|大丸|松坂屋|松屋|そごう|西武|東武|阪急|阪神|東急|近鉄|京王|小田急|" +
        "アウトレット|プレミアムアウトレット|ゲートタワー|アミュプラザ|ステラプレイス|ステーション|" +
        "ショッピングセンター|ショッピングモール|モール|百貨店|デパート|ビル|タワー|シティ|テラス|" +
        "lumine|lumineest|marui|parco|atre|aeon|mall|outlet|plaza|station"
    );
    private static final String[] LOCATION_SUFFIXES = keys(
        "都|道|府|県|市|区|郡|町|村|駅|空港|国際空港|店|本店|支店|営業所|店内|店舗|支社|本社|" +
        "東|西|南|北|東口|西口|南口|北口|中央口|駅前|駅構内|駅ナカ|駅ビル|中央|中央店|駅前店|" +
        "通り|通リ|大通り|通|街|横丁|新館|本館|別館|新店|本|支|内|前|館|シンカン|ホンカン"
    );
    private static final String[] QUALIFIERS = keys(
        "の|で|にある|近く|近くの|近所|周辺|付近|附近|最寄り|現在地|ここ|この辺|このあたり|このエリア|" +
        "in|at|near|nearby|near me|around|around me|the|a|an|and|or|" +
        "おすすめ|オススメ|人気|安い|安く|格安|おしゃれ|おしゃれな|かわいい|かわいい|可愛い|可愛い|" +
        "美味しい|おいしい|有名|評判|評判の|評判がいい|営業中|現在営業中|今開いている|24時間|" +
        "喫煙可|喫煙可能|喫煙できる|禁煙|個室|個室あり|予約可能|予約|テイクアウト|持ち帰り|" +
        "子供|子ども|こども|キッズ|子連れ|女性|男性|レディース|メンズ|ファミリー|駅近|駅チカ|" +
        "無料|有料|大型|大きい|小さい|専門|専門店|中古|新品|販売|購入|買える|あり|ある|のある|できる|" +
        "open now|open|best|cheap|free|24h|24 hours|women|womens|men|mens|kids"
    );
    private static final String[] CATEGORIES = keys(
        "靴|靴屋|靴店|靴販売店|靴修理|シューズ|シューズショップ|スニーカー|ブーツ|パンプス|" +
        "服|洋服|服屋|洋服店|衣料品|衣料品店|衣料店|婦人服|紳士服|古着|古着屋|アパレル|" +
        "バッグ|鞄|かばん|カバン|バッグ店|アクセサリー|アクセサリー店|雑貨|雑貨屋|雑貨店|" +
        "コスメ|化粧品|化粧品店|ネイル|ネイルサロン|美容院|美容室|理容室|床屋|ヘアサロン|エステ|" +
        "喫煙所|喫煙室|喫煙場所|喫煙スペース|喫煙スポット|喫煙|トイレ|公衆トイレ|お手洗い|" +
        "レストラン|飲食店|食事|食べ物|グルメ|ランチ|ディナー|朝食|昼食|夕食|ご飯|ごはん|" +
        "カフェ|喫茶店|喫茶|コーヒー|珈琲|カフェバー|猫カフェ|メイドカフェ|パン|パン屋|ベーカリー|" +
        "ケーキ|ケーキ屋|スイーツ|甘味処|和菓子|洋菓子|タピオカ|アイス|" +
        "居酒屋|酒場|バー|飲み屋|立ち飲み|ビアホール|ビール|焼き鳥|焼鳥|串焼き|焼肉|焼き肉|" +
        "寿司|すし|鮨|回転寿司|ラーメン|らーめん|中華|中華料理|餃子|カレー|うどん|そば|蕎麦|" +
        "和食|洋食|日本料理|フレンチ|フランス料理|イタリアン|イタリア料理|韓国料理|タイ料理|" +
        "インド料理|ベトナム料理|エスニック|ハンバーガー|ファストフード|ファミレス|ファミリーレストラン|" +
        "ステーキ|ハンバーグ|しゃぶしゃぶ|すき焼き|鍋|お好み焼き|たこ焼き|もんじゃ|食堂|定食|" +
        "鰻|うなぎ|ひつまぶし|海鮮|魚料理|魚|肉料理|豚カツ|とんかつ|牛丼|丼|弁当|惣菜|" +
        "コンビニ|コンビニエンスストア|スーパー|スーパーマーケット|食料品|食料品店|ドラッグストア|" +
        "薬局|薬店|病院|医院|クリニック|内科|外科|歯科|歯医者|眼科|耳鼻科|皮膚科|整形外科|" +
        "ATM|銀行|郵便局|金融機関|ホテル|旅館|宿|宿泊|宿泊施設|カプセルホテル|民泊|" +
        "駐車場|駐輪場|ガソリンスタンド|充電|充電スポット|EV充電|レンタカー|レンタサイクル|" +
        "駅|バス停|空港|港|フェリー|電車|鉄道|地下鉄|公園|広場|遊園地|動物園|水族館|美術館|博物館|" +
        "観光|観光地|観光スポット|名所|展望台|神社|寺|寺院|温泉|銭湯|スーパー銭湯|サウナ|" +
        "映画館|劇場|ライブハウス|カラオケ|ゲームセンター|ゲーセン|ゲーム|ボウリング|スポーツ|" +
        "ジム|フィットネス|プール|学校|大学|図書館|役所|区役所|市役所|警察|交番|消防署|" +
        "家具|家具店|家電|家電量販店|電気屋|ホームセンター|本屋|書店|CDショップ|花屋|花店|" +
        "携帯ショップ|携帯電話|スマホ修理|パソコン修理|修理|クリーニング|コインランドリー|" +
        "マッサージ|整体|耳かき|ペットショップ|不動産|会社|企業|店舗|店|ショップ|お店|商店街|" +
        "ショッピング|ショッピングモール|百貨店|デパート|アウトレット|100円ショップ|100均|" +
        "restaurant|restaurants|cafe|cafes|coffee|coffee shop|coffee shops|bakery|bakeries|bar|bars|" +
        "shoe|shoes|shoe shop|shoe shops|shoe store|shoe stores|clothing|clothes|clothing store|clothing stores|" +
        "smoking area|smoking areas|smoking room|smoking rooms|toilet|toilets|restroom|restrooms|" +
        "hotel|hotels|parking|parking lot|parking lots|gas station|gas stations|pharmacy|pharmacies|" +
        "convenience store|convenience stores|supermarket|supermarkets|atm|bank|banks|hospital|hospitals|" +
        "shopping mall|shopping malls|mall|malls|store|stores|shop|shops|park|parks|museum|museums"
    );
    private static final Set<String> CATEGORY_SET = new HashSet<String>(Arrays.asList(CATEGORIES));
    private static final String[] LOCATION_CONTEXT = concat(LOCATIONS, LOCATION_SUFFIXES, keys("の|近く|周辺|付近|near|nearby|in|at"));
    private static final String[] BRANCH_CONTEXT = concat(LOCATION_CONTEXT, VENUES);
    private static final String[] CATEGORY_CONTEXT = concat(LOCATIONS, LOCATION_SUFFIXES, QUALIFIERS, VENUES);
    private static final Pattern COORDINATES = Pattern.compile("^[+-]?\\d{1,3}(?:\\.\\d+)?\\s*[,、]\\s*[+-]?\\d{1,3}(?:\\.\\d+)?$");
    private static final Pattern PLUS_CODE = Pattern.compile(".*[23456789cfghjmpqrvwx]{4,}\\+[23456789cfghjmpqrvwx]{2,}.*");
    private static final Pattern POSTCODE = Pattern.compile(".*(?:〒\\s*)?\\d{3}[-ー]\\d{4}.*");
    private static final Pattern STREET_NUMBER = Pattern.compile(".*(?:\\d+\\s*(?:丁目|番地|番|号)|\\d+[-ー]\\d+(?:[-ー]\\d+)?).*");
    private static final Pattern LOCATION_ENDING = Pattern.compile("^.{2,}(?:都|道|府|県|市|区|郡|町|村|駅|空港)$");
    private static final String[] ROUTE_HINTS = keys("行き方|経路|ルート|ナビ|道順|乗換|乗り換え|徒歩で|車で|自転車で|directions to|route to|navigate to");
    private static final String[] SENTENCE_HINTS = keys("どこ|何処|どれ|どんな|探して|教えて|行きたい|where is|where can|where to|how to|find me");

    /** Build once when the committed query changes, not once per place. */
    public static Query classify(String input) {
        String normalized = normalize(input);
        Text text = new Text(normalized);
        String key = text.compact;
        if (key.length() == 0) return bypass("empty", normalized);
        if (normalized.length() > 160) return bypass("long-query", normalized);
        if (isNavigationOrAddress(normalized, key)) return bypass("address-or-navigation", normalized);

        Brand selected = null;
        int selectedLength = -1;
        boolean hasBrandTerm = false;
        for (Brand brand : BRANDS) {
            for (String alias : brand.names) {
                if (containsName(text, alias)) hasBrandTerm = true;
                int start = -1;
                while ((start = key.indexOf(alias, start + 1)) >= 0) {
                    String before = key.substring(0, start);
                    String after = key.substring(start + alias.length());
                    if (isLocationContext(before) && isQueryBranchContext(after)) {
                        if (alias.length() > selectedLength) {
                            selected = brand;
                            selectedLength = alias.length();
                        }
                    }
                }
            }
        }
        if (selected != null) return new Query(true, "known-name", normalized, selected.names);

        if (isCategoryQuery(normalized, key)) return bypass("category", normalized);
        if (isLocationContext(key) || LOCATION_ENDING.matcher(normalized).matches()) {
            return bypass("location", normalized);
        }
        if (hasBrandTerm) return bypass("ambiguous-name-context", normalized);
        for (String sentence : SENTENCE_HINTS) {
            if (key.contains(sentence)) return bypass("natural-language-query", normalized);
        }

        // A separated area qualifier does not have to occur in a business name.
        // Do not strip arbitrary concatenated prefixes: e.g. "青山フラワー..."
        // may itself be the business name.
        List<String> remaining = new ArrayList<String>();
        String[] words = normalized.split("[\\s\\p{Z}]+", -1);
        for (String word : words) {
            String wordKey = compact(word);
            if (wordKey.length() != 0 && !isLocationContext(wordKey)) remaining.add(wordKey);
        }
        if (remaining.size() > 4) return bypass("ambiguous-long-name", normalized);
        StringBuilder name = new StringBuilder();
        for (String word : remaining) name.append(word);
        String nameKey = name.toString();
        if (nameKey.length() < 2 || !hasLetter(nameKey)) return bypass("ambiguous-short-name", normalized);
        if (CATEGORY_SET.contains(nameKey)) return bypass("category", normalized);
        return new Query(true, "literal-name", normalized, new String[] {nameKey});
    }

    /** True means retain this result. Only the primary business name belongs here. */
    public static boolean matches(Query query, String primaryName) {
        if (query == null || !query.enabled) return true;
        Text title = new Text(normalize(primaryName));
        // Missing metadata is not evidence that a result is unrelated.
        if (title.compact.length() == 0) return true;
        for (String name : query.names) if (containsName(title, name)) return true;
        return false;
    }

    /** Convenience API; prefer caching classify() for a whole result update. */
    public static boolean shouldKeep(String committedQuery, String primaryName) {
        return matches(classify(committedQuery), primaryName);
    }

    /** NFKC, locale-independent case folding, and hiragana -> katakana. */
    public static String normalize(String input) {
        if (input == null || input.length() == 0) return "";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        StringBuilder out = new StringBuilder(normalized.length());
        boolean space = false;
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (c >= '\u3041' && c <= '\u3096') c = (char) (c + 0x60);
            if (Character.getType(c) == Character.FORMAT) continue;
            if (Character.isWhitespace(c) || Character.isSpaceChar(c)) {
                if (out.length() != 0) space = true;
            } else {
                if (space) out.append(' ');
                out.append(c);
                space = false;
            }
        }
        return Normalizer.normalize(out.toString(), Normalizer.Form.NFC);
    }

    private static Query bypass(String reason, String query) {
        return new Query(false, reason, query, new String[0]);
    }

    private static boolean isNavigationOrAddress(String normalized, String key) {
        if (normalized.contains("://") || normalized.startsWith("geo:") || normalized.startsWith("google.navigation:")) return true;
        if (COORDINATES.matcher(normalized).matches() || PLUS_CODE.matcher(normalized).matches() || POSTCODE.matcher(normalized).matches() || STREET_NUMBER.matcher(normalized).matches()) return true;
        if (normalized.contains("→") || normalized.contains("⇒") || normalized.contains("->")) return true;
        if (key.contains("カラ") && key.contains("マデ")) return true;
        for (String hint : ROUTE_HINTS) if (key.contains(hint)) return true;
        return false;
    }

    private static boolean isCategoryQuery(String normalized, String key) {
        if (CATEGORY_SET.contains(key) || covered(key, CATEGORY_CONTEXT, CATEGORIES, true)) return true;
        // Separated category + unknown qualifier is ambiguous. Prefer Maps'
        // ordinary semantic search over suppressing useful results.
        for (String word : normalized.split("[\\s\\p{Z}]+")) {
            if (CATEGORY_SET.contains(compact(word))) return true;
        }
        return false;
    }

    private static boolean isLocationContext(String key) {
        return key.length() == 0 || covered(key, LOCATION_CONTEXT, null, false);
    }

    private static boolean isQueryBranchContext(String key) {
        return key.length() == 0 || covered(key, BRANCH_CONTEXT, null, false) || isKanjiBranchSuffix(key);
    }

    private static boolean isKanjiBranchSuffix(String key) {
        if (key.length() < 3 || !isHan(key.charAt(0))) return false;
        if (!(key.endsWith("店") || key.endsWith("営業所") || key.endsWith("支社"))) return false;
        // Do not turn explicit category searches into a brand-only request.
        for (String category : CATEGORIES) {
            if (category.length() > 1 && key.startsWith(category)) return false;
        }
        return true;
    }

    /** Dynamic programming avoids order-dependent greedy vocabulary matching. */
    private static boolean covered(String key, String[] neutral, String[] required, boolean requireHit) {
        if (key.length() == 0) return !requireHit;
        if (key.length() > 160) return false;
        boolean[][] reachable = new boolean[key.length() + 1][2];
        reachable[0][0] = true;
        for (int i = 0; i < key.length(); i++) {
            for (int flag = 0; flag < 2; flag++) {
                if (!reachable[i][flag]) continue;
                for (String word : neutral) {
                    if (key.startsWith(word, i)) reachable[i + word.length()][flag] = true;
                }
                if (required != null) for (String word : required) {
                    if (key.startsWith(word, i)) reachable[i + word.length()][1] = true;
                }
            }
        }
        return reachable[key.length()][1] || (!requireHit && reachable[key.length()][0]);
    }

    private static boolean containsName(Text title, String name) {
        int start = -1;
        while ((start = title.compact.indexOf(name, start + 1)) >= 0) {
            int end = start + name.length();
            if (leftBoundary(title, start, name.charAt(0)) && rightBoundary(title, end, name.charAt(name.length() - 1))) return true;
        }
        return false;
    }

    private static boolean leftBoundary(Text title, int start, char first) {
        if (start == 0 || title.positions[start] > title.positions[start - 1] + 1) return true;
        char previous = title.compact.charAt(start - 1);
        if (isLatinWord(first)) return !isLatinWord(previous);
        if (isKana(first)) return !isKana(previous) || isLocationContext(title.compact.substring(0, start));
        if (isHan(first) && isHan(previous)) return isLocationContext(title.compact.substring(0, start));
        return true;
    }

    private static boolean rightBoundary(Text title, int end, char last) {
        if (end == title.compact.length() || title.positions[end] > title.positions[end - 1] + 1) return true;
        char next = title.compact.charAt(end);
        if (isLatinWord(last)) return !isLatinWord(next);
        if (isKana(last) && !isKana(next)) return true;
        if (isHan(last) || isKana(last)) return isQueryBranchContext(title.compact.substring(end));
        return true;
    }

    private static boolean isLatinWord(char c) {
        if (c >= 'a' && c <= 'z' || c >= '0' && c <= '9') return true;
        return c >= '\u00c0' && c <= '\u024f' && Character.isLetter(c);
    }

    private static boolean isKana(char c) {
        return c >= '\u3040' && c <= '\u30ff' && (Character.isLetter(c) || c == '\u30fc');
    }

    private static boolean isHan(char c) {
        return c >= '\u3400' && c <= '\u9fff' || c >= '\uf900' && c <= '\ufaff';
    }

    private static boolean hasLetter(String key) {
        for (int i = 0; i < key.length(); i++) if (Character.isLetter(key.charAt(i))) return true;
        return false;
    }

    private static String compact(String input) { return new Text(normalize(input)).compact; }

    private static String[] keys(String source) {
        Set<String> result = new HashSet<String>();
        for (String word : source.split("\\|")) {
            String value = compact(word);
            if (value.length() > 0) result.add(value);
        }
        return result.toArray(new String[result.size()]);
    }

    private static String[] concat(String[]... arrays) {
        Set<String> values = new HashSet<String>();
        for (String[] array : arrays) values.addAll(Arrays.asList(array));
        return values.toArray(new String[values.size()]);
    }

    private static final class Text {
        final String compact;
        final int[] positions;
        Text(String normalized) {
            StringBuilder out = new StringBuilder(normalized.length());
            int[] positions = new int[normalized.length()];
            for (int i = 0; i < normalized.length(); i++) {
                char c = normalized.charAt(i);
                if (Character.isLetterOrDigit(c) || Character.getType(c) == Character.NON_SPACING_MARK) {
                    positions[out.length()] = i;
                    out.append(c);
                }
            }
            this.compact = out.toString();
            this.positions = Arrays.copyOf(positions, out.length());
        }
    }
}

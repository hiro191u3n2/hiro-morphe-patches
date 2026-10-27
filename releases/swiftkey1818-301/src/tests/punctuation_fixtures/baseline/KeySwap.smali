.class public final Lhiro/swiftkey/KeySwap;
.super Ljava/lang/Object;
.source "KeySwap.java"


# annotations
.annotation system Ldalvik/annotation/MemberClasses;
    value = {
        Lhiro/swiftkey/KeySwap$ChangeListener;,
        Lhiro/swiftkey/KeySwap$Wrapped;
    }
.end annotation


# static fields
.field public static final GLIDE_KEY:Ljava/lang/String; = "hiro_japanese_qwerty_glide"

.field public static final KEY:Ljava/lang/String; = "hiro_japanese_swap_123_enter"

.field public static final LATIN_123_KEY:Ljava/lang/String; = "hiro_latin_qwerty_swap_123_enter"

.field public static final LATIN_SHIFT_KEY:Ljava/lang/String; = "hiro_latin_qwerty_swap_shift_backspace"

.field public static final LEFT_RIGHT_KEY:Ljava/lang/String; = "hiro_japanese_swap_left_right"

.field public static final LONG_VOWEL_KEY:Ljava/lang/String; = "hiro_japanese_swap_long_vowel_backspace"

.field public static final SYMBOLS_KEY:Ljava/lang/String; = "hiro_japanese_swap_abc_enter"

.field public static final SYMBOL_PAGE_KEY:Ljava/lang/String; = "hiro_japanese_swap_symbol_page_backspace"

.field public static final UP_DOWN_KEY:Ljava/lang/String; = "hiro_japanese_swap_up_down"

.field private static volatile preferences:Landroid/content/SharedPreferences;

.field private static final replacements:[[[B


# direct methods
.method static constructor <clinit>()V
    .locals 1

    const/16 v0, 0x10

    new-array v0, v0, [[[B

    sput-object v0, Lhiro/swiftkey/KeySwap;->replacements:[[[B

    return-void
.end method

.method private constructor <init>()V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method static synthetic access$000()Landroid/content/SharedPreferences;
    .locals 1

    sget-object v0, Lhiro/swiftkey/KeySwap;->preferences:Landroid/content/SharedPreferences;

    return-object v0
.end method

.method static synthetic access$100(Ljava/lang/String;)Z
    .locals 0

    invoke-static {p0}, Lhiro/swiftkey/KeySwap;->isOption(Ljava/lang/String;)Z

    move-result p0

    return p0
.end method

.method private static addDictionarySources(Landroidx/preference/PreferenceScreen;)V
    .locals 4

    nop

    const-string v0, "hiro_dictionary_sources"

    invoke-virtual {p0, v0}, Landroidx/preference/PreferenceScreen;->H(Ljava/lang/CharSequence;)Landroidx/preference/Preference;

    move-result-object v1

    if-eqz v1, :cond_0

    return-void

    :cond_0
    new-instance v1, Landroidx/preference/Preference;

    iget-object v2, p0, Landroidx/preference/PreferenceScreen;->a:Landroid/content/Context;

    const/4 v3, 0x0

    invoke-direct {v1, v2, v3}, Landroidx/preference/Preference;-><init>(Landroid/content/Context;Landroid/util/AttributeSet;)V

    iput-object v0, v1, Landroidx/preference/Preference;->l:Ljava/lang/String;

    const/4 v0, 0x0

    iput-boolean v0, v1, Landroidx/preference/Preference;->s:Z

    iput-boolean v0, v1, Landroidx/preference/Preference;->q:Z

    const-string v0, "\u8ffd\u52a0\u8f9e\u66f8\u306e\u51fa\u5178"

    invoke-virtual {v1, v0}, Landroidx/preference/Preference;->B(Ljava/lang/CharSequence;)V

    const-string v0, "JMdict \u00a9 James William BREEN / EDRDG (CC BY-SA 4.0)\nhttps://www.edrdg.org/wiki/index.php/JMdict-EDICT_Dictionary_Project\nhttps://www.edrdg.org/edrdg/licence.html\n\u305d\u306e\u4ed6\u306e\u8ffd\u52a0\u8f9e\u66f8\uff1aMozc\u30fbSudachiDict\u30fbSKK\u30fbKEINOS \u30ab\u30bf\u30ab\u30ca\u8a9e\u8f9e\u66f8"

    invoke-virtual {v1, v0}, Landroidx/preference/Preference;->A(Ljava/lang/CharSequence;)V

    invoke-virtual {p0, v1}, Landroidx/preference/PreferenceScreen;->G(Landroidx/preference/Preference;)V

    return-void
.end method

.method private static addOption(Landroidx/preference/PreferenceScreen;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
    .locals 3

    invoke-virtual {p0, p1}, Landroidx/preference/PreferenceScreen;->H(Ljava/lang/CharSequence;)Landroidx/preference/Preference;

    move-result-object v0

    if-eqz v0, :cond_0

    return-void

    :cond_0
    new-instance v0, Landroidx/preference/SwitchPreferenceCompat;

    iget-object v1, p0, Landroidx/preference/PreferenceScreen;->a:Landroid/content/Context;

    const/4 v2, 0x0

    invoke-direct {v0, v1, v2}, Landroidx/preference/SwitchPreferenceCompat;-><init>(Landroid/content/Context;Landroid/util/AttributeSet;)V

    iput-object p1, v0, Landroidx/preference/SwitchPreferenceCompat;->l:Ljava/lang/String;

    const/4 v1, 0x0

    iput-boolean v1, v0, Landroidx/preference/SwitchPreferenceCompat;->s:Z

    invoke-virtual {v0, p2}, Landroidx/preference/SwitchPreferenceCompat;->B(Ljava/lang/CharSequence;)V

    invoke-virtual {v0, p3}, Landroidx/preference/SwitchPreferenceCompat;->A(Ljava/lang/CharSequence;)V

    sget-object p2, Lhiro/swiftkey/KeySwap;->preferences:Landroid/content/SharedPreferences;

    invoke-interface {p2, p1, v1}, Landroid/content/SharedPreferences;->getBoolean(Ljava/lang/String;Z)Z

    move-result p2

    invoke-virtual {v0, p2}, Landroidx/preference/SwitchPreferenceCompat;->G(Z)V

    new-instance p2, Lhiro/swiftkey/KeySwap$ChangeListener;

    invoke-direct {p2, p1}, Lhiro/swiftkey/KeySwap$ChangeListener;-><init>(Ljava/lang/String;)V

    iput-object p2, v0, Landroidx/preference/SwitchPreferenceCompat;->e:Loa/j;

    invoke-virtual {p0, v0}, Landroidx/preference/PreferenceScreen;->G(Landroidx/preference/Preference;)V

    return-void
.end method

.method public static init(Landroid/content/SharedPreferences;)V
    .locals 0

    sput-object p0, Lhiro/swiftkey/KeySwap;->preferences:Landroid/content/SharedPreferences;

    :try_start_0
    invoke-static {p0}, Lhiro/swiftkey/FlickSymbols;->init(Landroid/content/SharedPreferences;)V

    invoke-static {p0}, Lhiro/swiftkey/EmojiPanelUi;->init(Landroid/content/SharedPreferences;)V

    invoke-static {p0}, Lhiro/swiftkey/JapaneseGlide;->init(Landroid/content/SharedPreferences;)V
    :try_end_0
    .catch Ljava/lang/RuntimeException; {:try_start_0 .. :try_end_0} :catch_1
    .catch Ljava/lang/LinkageError; {:try_start_0 .. :try_end_0} :catch_0

    :goto_0
    goto :goto_1

    :catch_0
    move-exception p0

    goto :goto_0

    :catch_1
    move-exception p0

    goto :goto_0

    :goto_1
    return-void
.end method

.method public static install(Landroidx/preference/PreferenceScreen;)V
    .locals 3

    if-eqz p0, :cond_1

    :try_start_0
    sget-object v0, Lhiro/swiftkey/KeySwap;->preferences:Landroid/content/SharedPreferences;

    if-nez v0, :cond_0

    goto :goto_1

    :cond_0
    const-string v0, "hiro_japanese_swap_123_enter"

    const-string v1, "\u65e5\u672c\u8a9eQWERTY\u306e\u300c123\u300d\u3068\u6539\u884c\u30ad\u30fc\u3092\u5165\u308c\u66ff\u3048"

    const-string v2, "\u6a19\u6e96\u30fb\u7247\u624b\u914d\u5217\u3067\u5de6\u53f3\u3092\u4ea4\u63db\u3057\u307e\u3059\u3002\u6539\u884c\u30ad\u30fc\u306f123\u3068\u540c\u3058\u5e45\u3067\u3059\u3002\u691c\u7d22\u30fb\u9001\u4fe1\u3082\u5de6\u5074\u306b\u306a\u308a\u307e\u3059\u3002PC\u914d\u5217\u306f\u5bfe\u8c61\u5916\u3067\u3059\u3002"

    invoke-static {p0, v0, v1, v2}, Lhiro/swiftkey/KeySwap;->addOption(Landroidx/preference/PreferenceScreen;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V

    const-string v0, "hiro_japanese_swap_long_vowel_backspace"

    const-string v1, "\u65e5\u672c\u8a9eQWERTY\u306e\u300c\u30fc\u300d\u3068\u524a\u9664\u30ad\u30fc\u3092\u5165\u308c\u66ff\u3048"

    const-string v2, "\u6587\u5b57\u6bb5\u306e\u5de6\u7aef\u306e\u9577\u97f3\u30ad\u30fc\u3068\u53f3\u7aef\u306e\u524a\u9664\u30ad\u30fc\u3092\u4ea4\u63db\u3057\u307e\u3059\u3002\u6a19\u6e96\u30fb\u7247\u624b\u914d\u5217\u304c\u5bfe\u8c61\u3067\u3059\u3002PC\u914d\u5217\u306f\u5bfe\u8c61\u5916\u3067\u3059\u3002"

    invoke-static {p0, v0, v1, v2}, Lhiro/swiftkey/KeySwap;->addOption(Landroidx/preference/PreferenceScreen;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V

    const-string v0, "hiro_japanese_swap_abc_enter"

    const-string v1, "\u65e5\u672c\u8a9e\u306e\u8a18\u53f7\u753b\u9762\u3067\u300cabc\u300d\u3068\u6539\u884c\u30ad\u30fc\u3092\u5165\u308c\u66ff\u3048"

    const-string v2, "\u65e5\u672c\u8a9e\u306e\u6570\u5b57\u30fb\u8a18\u53f7\u753b\u9762\u3067\u4e0b\u6bb5\u306e\u5de6\u53f3\u3092\u4ea4\u63db\u3057\u307e\u3059\u3002\u691c\u7d22\u30fb\u9001\u4fe1\u3082\u5de6\u5074\u306b\u306a\u308a\u307e\u3059\u3002"

    invoke-static {p0, v0, v1, v2}, Lhiro/swiftkey/KeySwap;->addOption(Landroidx/preference/PreferenceScreen;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V

    const-string v0, "hiro_japanese_swap_symbol_page_backspace"

    const-string v1, "\u65e5\u672c\u8a9e\u306e\u8a18\u53f7\u753b\u9762\u3067\u8a18\u53f7\u5207\u66ff\u3068\u524a\u9664\u30ad\u30fc\u3092\u5165\u308c\u66ff\u3048"

    const-string v2, "\u6587\u5b57\u6bb5\u306e\u5de6\u7aef\u306e\u8a18\u53f7\u30da\u30fc\u30b8\u5207\u66ff\u30ad\u30fc\u3068\u53f3\u7aef\u306e\u524a\u9664\u30ad\u30fc\u3092\u4ea4\u63db\u3057\u307e\u3059\u3002"

    invoke-static {p0, v0, v1, v2}, Lhiro/swiftkey/KeySwap;->addOption(Landroidx/preference/PreferenceScreen;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V

    const-string v0, "hiro_japanese_swap_up_down"

    const-string v1, "\u65e5\u672c\u8a9e\u30ad\u30fc\u30dc\u30fc\u30c9\u306e\u300c\u2191\u300d\u3068\u300c\u2193\u300d\u3092\u5165\u308c\u66ff\u3048"

    const-string v2, "\u4e0b\u6bb5\u306e\u4e0a\u4e0b\u77e2\u5370\u30ad\u30fc\u3092\u4ea4\u63db\u3057\u307e\u3059\u3002\u6a19\u6e96\u30fb\u7247\u624b\u914d\u5217\u306e\u65e5\u672c\u8a9eQWERTY\u3068\u65e5\u672c\u8a9e\u306e\u6570\u5b57\u30fb\u8a18\u53f7\u753b\u9762\u304c\u5bfe\u8c61\u3067\u3059\u3002"

    invoke-static {p0, v0, v1, v2}, Lhiro/swiftkey/KeySwap;->addOption(Landroidx/preference/PreferenceScreen;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V

    const-string v0, "hiro_japanese_swap_left_right"

    const-string v1, "\u65e5\u672c\u8a9e\u30ad\u30fc\u30dc\u30fc\u30c9\u306e\u300c\u2190\u300d\u3068\u300c\u2192\u300d\u3092\u5165\u308c\u66ff\u3048"

    const-string v2, "\u4e0b\u6bb5\u306e\u5de6\u53f3\u77e2\u5370\u30ad\u30fc\u3092\u4ea4\u63db\u3057\u307e\u3059\u3002\u6a19\u6e96\u30fb\u7247\u624b\u914d\u5217\u306e\u65e5\u672c\u8a9eQWERTY\u3068\u65e5\u672c\u8a9e\u306e\u6570\u5b57\u30fb\u8a18\u53f7\u753b\u9762\u304c\u5bfe\u8c61\u3067\u3059\u3002"

    invoke-static {p0, v0, v1, v2}, Lhiro/swiftkey/KeySwap;->addOption(Landroidx/preference/PreferenceScreen;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V

    const-string v0, "hiro_japanese_qwerty_glide"

    const-string v1, "\u65e5\u672c\u8a9eQWERTY\u3067\u30b0\u30e9\u30a4\u30c9\u5165\u529b\uff08\u8a66\u9a13\u6a5f\u80fd\uff09"

    const-string v2, "\u30ed\u30fc\u30de\u5b57\u306e\u30ad\u30fc\u3092\u306a\u305e\u308a\u3001\u8868\u793a\u3055\u308c\u305f\u8aad\u307f\u3092\u9078\u3076\u3068\u901a\u5e38\u306e\u65e5\u672c\u8a9e\u5909\u63db\u3078\u6e21\u3057\u307e\u3059\u3002\u6a19\u6e96\u30fb\u7247\u624b\u30fbPC\u30fb\u5206\u5272QWERTY\u914d\u5217\u306b\u5bfe\u5fdc\u3057\u307e\u3059\u3002\u521d\u671f\u8a2d\u5b9a\u306f\u30aa\u30d5\u3067\u3059\u3002"

    invoke-static {p0, v0, v1, v2}, Lhiro/swiftkey/KeySwap;->addOption(Landroidx/preference/PreferenceScreen;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V

    const-string v0, "hiro_latin_qwerty_swap_123_enter"

    const-string v1, "\u82f1\u5b57QWERTY\u306e\u300c123\u300d\u3068\u6539\u884c\u30ad\u30fc\u3092\u5165\u308c\u66ff\u3048"

    const-string v2, "\u82f1\u8a9e\u306a\u3069\u3067\u4f7f\u3046\u6a19\u6e96\u30fb\u7247\u624bQWERTY\u914d\u5217\u3067\u4e0b\u6bb5\u306e\u5de6\u53f3\u3092\u4ea4\u63db\u3057\u307e\u3059\u3002\u6539\u884c\u30ad\u30fc\u306f123\u3068\u540c\u3058\u5e45\u306b\u306a\u308a\u3001\u5897\u3048\u305f\u5206\u3060\u3051\u30b9\u30da\u30fc\u30b9\u3092\u72ed\u3081\u307e\u3059\u3002\u691c\u7d22\u30fb\u9001\u4fe1\u3082\u5de6\u5074\u306b\u306a\u308a\u307e\u3059\u3002PC\u914d\u5217\u306f\u5bfe\u8c61\u5916\u3067\u3059\u3002"

    invoke-static {p0, v0, v1, v2}, Lhiro/swiftkey/KeySwap;->addOption(Landroidx/preference/PreferenceScreen;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V

    const-string v0, "hiro_latin_qwerty_swap_shift_backspace"

    const-string v1, "\u82f1\u5b57QWERTY\u306e\u30b7\u30d5\u30c8\u3068\u524a\u9664\u30ad\u30fc\u3092\u5165\u308c\u66ff\u3048"

    const-string v2, "\u82f1\u8a9e\u306a\u3069\u3067\u4f7f\u3046\u6a19\u6e96\u30fb\u7247\u624bQWERTY\u914d\u5217\u3067\u6587\u5b57\u6bb5\u306e\u5de6\u53f3\u3092\u4ea4\u63db\u3057\u307e\u3059\u3002PC\u914d\u5217\u306f\u5bfe\u8c61\u5916\u3067\u3059\u3002"

    invoke-static {p0, v0, v1, v2}, Lhiro/swiftkey/KeySwap;->addOption(Landroidx/preference/PreferenceScreen;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V

    invoke-static {p0}, Lhiro/swiftkey/FlickSymbols;->install(Landroidx/preference/PreferenceScreen;)V

    invoke-static {p0}, Lhiro/swiftkey/EmojiPanelUi;->install(Landroidx/preference/PreferenceScreen;)V

    invoke-static {p0}, Lhiro/swiftkey/KeySwap;->addDictionarySources(Landroidx/preference/PreferenceScreen;)V
    :try_end_0
    .catch Ljava/lang/RuntimeException; {:try_start_0 .. :try_end_0} :catch_1
    .catch Ljava/lang/LinkageError; {:try_start_0 .. :try_end_0} :catch_0

    goto :goto_0

    :catch_0
    move-exception p0

    goto :goto_0

    :catch_1
    move-exception p0

    :goto_0
    nop

    return-void

    :cond_1
    :goto_1
    return-void
.end method

.method private static isLayoutOption(Ljava/lang/String;)Z
    .locals 1

    const-string v0, "hiro_japanese_swap_123_enter"

    invoke-virtual {v0, p0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_1

    const-string v0, "hiro_japanese_swap_long_vowel_backspace"

    invoke-virtual {v0, p0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_1

    const-string v0, "hiro_japanese_swap_abc_enter"

    invoke-virtual {v0, p0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_1

    const-string v0, "hiro_japanese_swap_symbol_page_backspace"

    invoke-virtual {v0, p0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_1

    const-string v0, "hiro_japanese_swap_up_down"

    invoke-virtual {v0, p0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_1

    const-string v0, "hiro_japanese_swap_left_right"

    invoke-virtual {v0, p0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_1

    const-string v0, "hiro_latin_qwerty_swap_123_enter"

    invoke-virtual {v0, p0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_1

    const-string v0, "hiro_latin_qwerty_swap_shift_backspace"

    invoke-virtual {v0, p0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result p0

    if-eqz p0, :cond_0

    goto :goto_0

    :cond_0
    const/4 p0, 0x0

    goto :goto_1

    :cond_1
    :goto_0
    const/4 p0, 0x1

    :goto_1
    return p0
.end method

.method private static isOption(Ljava/lang/String;)Z
    .locals 1

    const-string v0, "hiro_japanese_qwerty_glide"

    invoke-virtual {v0, p0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_1

    invoke-static {p0}, Lhiro/swiftkey/KeySwap;->isLayoutOption(Ljava/lang/String;)Z

    move-result p0

    if-eqz p0, :cond_0

    goto :goto_0

    :cond_0
    const/4 p0, 0x0

    goto :goto_1

    :cond_1
    :goto_0
    const/4 p0, 0x1

    :goto_1
    return p0
.end method

.method public static layoutPreferenceKey(Ljava/lang/String;)Ljava/lang/String;
    .locals 1

    invoke-static {p0}, Lhiro/swiftkey/KeySwap;->isLayoutOption(Ljava/lang/String;)Z

    move-result v0

    if-eqz v0, :cond_0

    const-string p0, "pref_arrows_key"

    :cond_0
    return-object p0
.end method

.method private static resourceIndex(Ljava/lang/String;)I
    .locals 1

    const-string v0, "keyboard_layout_standard_romaji"

    invoke-virtual {v0, p0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_0

    const/4 p0, 0x0

    return p0

    :cond_0
    const-string v0, "keyboard_layout_standard_romaji_compact"

    invoke-virtual {v0, p0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_1

    const/4 p0, 0x1

    return p0

    :cond_1
    const-string v0, "keyboard_layout_symbols_japanese"

    invoke-virtual {v0, p0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_2

    const/4 p0, 0x2

    return p0

    :cond_2
    const-string v0, "keyboard_layout_symbols_japanese_compact"

    invoke-virtual {v0, p0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_3

    const/4 p0, 0x3

    return p0

    :cond_3
    const-string v0, "keyboard_layout_symbols_top_row_japanese"

    invoke-virtual {v0, p0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_4

    const/4 p0, 0x4

    return p0

    :cond_4
    const-string v0, "keyboard_layout_symbols_top_row_japanese_compact"

    invoke-virtual {v0, p0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_5

    const/4 p0, 0x5

    return p0

    :cond_5
    const-string v0, "keyboard_layout_symbols_alt_japanese"

    invoke-virtual {v0, p0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_6

    const/4 p0, 0x6

    return p0

    :cond_6
    const-string v0, "keyboard_layout_symbols_alt_japanese_compact"

    invoke-virtual {v0, p0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_7

    const/4 p0, 0x7

    return p0

    :cond_7
    const-string v0, "keyboard_layout_standard_qwerty"

    invoke-virtual {v0, p0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_8

    const/16 p0, 0x8

    return p0

    :cond_8
    const-string v0, "keyboard_layout_standard_qwerty_compact"

    invoke-virtual {v0, p0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_9

    const/16 p0, 0x9

    return p0

    :cond_9
    const-string v0, "keyboard_layout_symbols"

    invoke-virtual {v0, p0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_a

    const/16 p0, 0xa

    return p0

    :cond_a
    const-string v0, "keyboard_layout_symbols_compact"

    invoke-virtual {v0, p0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_b

    const/16 p0, 0xb

    return p0

    :cond_b
    const-string v0, "keyboard_layout_symbols_top_row"

    invoke-virtual {v0, p0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_c

    const/16 p0, 0xc

    return p0

    :cond_c
    const-string v0, "keyboard_layout_symbols_top_row_compact"

    invoke-virtual {v0, p0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_d

    const/16 p0, 0xd

    return p0

    :cond_d
    const-string v0, "keyboard_layout_symbols_alt"

    invoke-virtual {v0, p0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_e

    const/16 p0, 0xe

    return p0

    :cond_e
    const-string v0, "keyboard_layout_symbols_alt_compact"

    invoke-virtual {v0, p0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result p0

    if-eqz p0, :cond_f

    const/16 p0, 0xf

    return p0

    :cond_f
    const/4 p0, -0x1

    return p0
.end method

.method public static wrap(Lsc0/c;Landroid/content/res/Resources;)Lhiro/swiftkey/KeySwap$Wrapped;
    .locals 11

    sget-object v0, Lhiro/swiftkey/KeySwap;->preferences:Landroid/content/SharedPreferences;

    const/4 v1, 0x0

    if-eqz p0, :cond_13

    if-nez p1, :cond_0

    goto/16 :goto_e

    :cond_0
    :try_start_0
    iget v2, p0, Lsc0/c;->a:I

    invoke-virtual {p1, v2}, Landroid/content/res/Resources;->getResourceEntryName(I)Ljava/lang/String;

    move-result-object v2

    invoke-static {v2}, Lhiro/swiftkey/KeySwap;->resourceIndex(Ljava/lang/String;)I

    move-result v2

    if-gez v2, :cond_1

    return-object v1

    :cond_1
    const/16 v3, 0x8

    const/4 v4, 0x2

    const/4 v5, 0x0

    if-ge v2, v3, :cond_c

    if-nez v0, :cond_2

    move v6, v5

    goto :goto_2

    :cond_2
    const-string v6, "hiro_japanese_swap_up_down"

    invoke-interface {v0, v6, v5}, Landroid/content/SharedPreferences;->getBoolean(Ljava/lang/String;Z)Z

    move-result v6

    if-eqz v6, :cond_3

    const/16 v6, 0x10

    goto :goto_0

    :cond_3
    move v6, v5

    :goto_0
    const-string v7, "hiro_japanese_swap_left_right"

    invoke-interface {v0, v7, v5}, Landroid/content/SharedPreferences;->getBoolean(Ljava/lang/String;Z)Z

    move-result v7

    if-eqz v7, :cond_4

    const/16 v7, 0x20

    goto :goto_1

    :cond_4
    move v7, v5

    :goto_1
    or-int/2addr v6, v7

    :goto_2
    if-ge v2, v4, :cond_8

    if-nez v0, :cond_5

    goto :goto_4

    :cond_5
    const-string v3, "hiro_japanese_swap_123_enter"

    invoke-interface {v0, v3, v5}, Landroid/content/SharedPreferences;->getBoolean(Ljava/lang/String;Z)Z

    move-result v3

    if-eqz v3, :cond_6

    const/4 v3, 0x1

    goto :goto_3

    :cond_6
    move v3, v5

    :goto_3
    const-string v7, "hiro_japanese_swap_long_vowel_backspace"

    invoke-interface {v0, v7, v5}, Landroid/content/SharedPreferences;->getBoolean(Ljava/lang/String;Z)Z

    move-result v0

    if-eqz v0, :cond_7

    move v5, v4

    :cond_7
    or-int/2addr v5, v3

    :goto_4
    or-int v0, v6, v5

    move v5, v0

    goto :goto_8

    :cond_8
    if-nez v0, :cond_9

    goto :goto_7

    :cond_9
    const-string v7, "hiro_japanese_swap_abc_enter"

    invoke-interface {v0, v7, v5}, Landroid/content/SharedPreferences;->getBoolean(Ljava/lang/String;Z)Z

    move-result v7

    if-eqz v7, :cond_a

    const/4 v7, 0x4

    goto :goto_5

    :cond_a
    move v7, v5

    :goto_5
    const-string v8, "hiro_japanese_swap_symbol_page_backspace"

    invoke-interface {v0, v8, v5}, Landroid/content/SharedPreferences;->getBoolean(Ljava/lang/String;Z)Z

    move-result v0

    if-eqz v0, :cond_b

    goto :goto_6

    :cond_b
    move v3, v5

    :goto_6
    or-int v5, v7, v3

    :goto_7
    or-int v0, v6, v5

    move v5, v0

    :goto_8
    goto :goto_b

    :cond_c
    const/16 v3, 0xa

    if-ge v2, v3, :cond_10

    if-nez v0, :cond_d

    goto :goto_a

    :cond_d
    const-string v3, "hiro_latin_qwerty_swap_123_enter"

    invoke-interface {v0, v3, v5}, Landroid/content/SharedPreferences;->getBoolean(Ljava/lang/String;Z)Z

    move-result v3

    if-eqz v3, :cond_e

    const/16 v3, 0x40

    goto :goto_9

    :cond_e
    move v3, v5

    :goto_9
    const-string v6, "hiro_latin_qwerty_swap_shift_backspace"

    invoke-interface {v0, v6, v5}, Landroid/content/SharedPreferences;->getBoolean(Ljava/lang/String;Z)Z

    move-result v0

    if-eqz v0, :cond_f

    const/16 v5, 0x80

    :cond_f
    or-int v0, v3, v5

    move v5, v0

    :goto_a
    goto :goto_b

    :cond_10
    nop

    :goto_b
    nop

    const-class v0, Lhiro/swiftkey/KeySwap;

    monitor-enter v0
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_2
    .catch Ljava/lang/LinkageError; {:try_start_0 .. :try_end_0} :catch_1

    :try_start_1
    sget-object v3, Lhiro/swiftkey/KeySwap;->replacements:[[[B

    aget-object v3, v3, v2

    if-nez v3, :cond_11

    sget-object v3, Lhiro/swiftkey/KeySwap;->replacements:[[[B

    const/16 v6, 0x100

    new-array v6, v6, [[B

    aput-object v6, v3, v2

    move-object v3, v6

    :cond_11
    aget-object v6, v3, v5

    if-nez v6, :cond_12

    iget v6, p0, Lsc0/c;->a:I

    invoke-virtual {p1, v6}, Landroid/content/res/Resources;->openRawResource(I)Ljava/io/InputStream;

    move-result-object p1
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_1

    const/16 v6, 0x1000

    :try_start_2
    invoke-static {p1, v6}, Lhiro/swiftkey/KeySwapBinary;->read(Ljava/io/InputStream;I)[B

    move-result-object v6

    invoke-static {v6, v5}, Lhiro/swiftkey/KeySwapBinary;->transform([BI)[B

    move-result-object v6
    :try_end_2
    .catchall {:try_start_2 .. :try_end_2} :catchall_0

    :try_start_3
    invoke-virtual {p1}, Ljava/io/InputStream;->close()V

    aput-object v6, v3, v5

    goto :goto_c

    :catchall_0
    move-exception p0

    invoke-virtual {p1}, Ljava/io/InputStream;->close()V

    throw p0

    :cond_12
    :goto_c
    nop

    monitor-exit v0
    :try_end_3
    .catchall {:try_start_3 .. :try_end_3} :catchall_1

    int-to-long v7, v5

    const/16 p1, 0x30

    shl-long/2addr v7, p1

    const-wide/high16 v9, 0x2000000000000000L

    or-long/2addr v7, v9

    :try_start_4
    div-int/2addr v2, v4

    int-to-long v2, v2

    const/16 p1, 0x38

    shl-long/2addr v2, p1

    or-long/2addr v2, v7

    new-instance p1, Lhiro/swiftkey/KeySwap$Wrapped;

    new-instance v0, Ljava/io/ByteArrayInputStream;

    invoke-direct {v0, v6}, Ljava/io/ByteArrayInputStream;-><init>([B)V

    iget-wide v4, p0, Lsc0/c;->b:J

    xor-long/2addr v2, v4

    invoke-direct {p1, v0, v2, v3}, Lhiro/swiftkey/KeySwap$Wrapped;-><init>(Ljava/io/InputStream;J)V
    :try_end_4
    .catch Ljava/lang/Exception; {:try_start_4 .. :try_end_4} :catch_2
    .catch Ljava/lang/LinkageError; {:try_start_4 .. :try_end_4} :catch_1

    :try_start_5
    iget-object p0, p0, Lsc0/c;->c:Ljava/io/InputStream;

    invoke-virtual {p0}, Ljava/io/InputStream;->close()V
    :try_end_5
    .catch Ljava/io/IOException; {:try_start_5 .. :try_end_5} :catch_0
    .catch Ljava/lang/Exception; {:try_start_5 .. :try_end_5} :catch_2
    .catch Ljava/lang/LinkageError; {:try_start_5 .. :try_end_5} :catch_1

    goto :goto_d

    :catch_0
    move-exception p0

    :goto_d
    return-object p1

    :catchall_1
    move-exception p0

    :try_start_6
    monitor-exit v0
    :try_end_6
    .catchall {:try_start_6 .. :try_end_6} :catchall_1

    :try_start_7
    throw p0
    :try_end_7
    .catch Ljava/lang/Exception; {:try_start_7 .. :try_end_7} :catch_2
    .catch Ljava/lang/LinkageError; {:try_start_7 .. :try_end_7} :catch_1

    :catch_1
    move-exception p0

    return-object v1

    :catch_2
    move-exception p0

    return-object v1

    :cond_13
    :goto_e
    return-object v1
.end method

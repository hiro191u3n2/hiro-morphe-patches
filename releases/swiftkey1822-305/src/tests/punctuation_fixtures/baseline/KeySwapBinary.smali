.class public final Lhiro/swiftkey/KeySwapBinary;
.super Ljava/lang/Object;
.source "KeySwapBinary.java"


# static fields
.field private static final ARROWS:[[[I

.field private static final HASHES:[Ljava/lang/String;

.field private static final ROWS:[[[I

.field private static final SIZES:[I

.field public static final SWAP_123_ENTER:I = 0x1

.field public static final SWAP_ABC_ENTER:I = 0x4

.field public static final SWAP_LEFT_RIGHT:I = 0x20

.field public static final SWAP_LONG_VOWEL_BACKSPACE:I = 0x2

.field public static final SWAP_QWERTY_123_ENTER:I = 0x40

.field public static final SWAP_QWERTY_SHIFT_BACKSPACE:I = 0x80

.field public static final SWAP_SYMBOL_PAGE_BACKSPACE:I = 0x8

.field public static final SWAP_UP_DOWN:I = 0x10


# direct methods
.method static constructor <clinit>()V
    .locals 23

    const-string v6, "bc222a57ee897f3ef58694387ada8158b71315bad39f67a0ad00f2e0035fa957"

    const-string v7, "1596055b8ec46615304e2b524dabd65990baf72c89b38c7fc86fe835696aa383"

    const-string v0, "681a6141e3e71a5e07e9b406ec38e9388d080cee428ed7927936a2a3521fa2f9"

    const-string v1, "fd2dfab0c0ab6012e38dcfe9347cde4780571a2ab9b410acf110cbeb43a166c1"

    const-string v2, "d044b2f6880ca94e7084fc745cc4b39eeb1bd547ef26c213f0c0d7b14a7c0683"

    const-string v3, "c63e3d6987d3069b41f006f4fb2ee1656f2a1c6fafde64242cdec05c4cac83f8"

    const-string v4, "1a0aff577848210025cf4ceedae2b395957a5e0ec558a974980bd54a119b90c8"

    const-string v5, "c9fc39a3c992f966d91ea6d6bce6534230aeb0504f9e177306743832c2382aa2"

    filled-new-array/range {v0 .. v7}, [Ljava/lang/String;

    move-result-object v0

    sput-object v0, Lhiro/swiftkey/KeySwapBinary;->HASHES:[Ljava/lang/String;

    const/16 v0, 0x8

    new-array v1, v0, [I

    fill-array-data v1, :array_0

    sput-object v1, Lhiro/swiftkey/KeySwapBinary;->SIZES:[I

    new-array v2, v0, [I

    fill-array-data v2, :array_1

    new-array v3, v0, [I

    fill-array-data v3, :array_2

    new-array v4, v0, [I

    fill-array-data v4, :array_3

    new-array v5, v0, [I

    fill-array-data v5, :array_4

    new-array v6, v0, [I

    fill-array-data v6, :array_5

    new-array v7, v0, [I

    fill-array-data v7, :array_6

    new-array v8, v0, [I

    fill-array-data v8, :array_7

    new-array v9, v0, [I

    fill-array-data v9, :array_8

    new-array v10, v0, [I

    fill-array-data v10, :array_9

    new-array v11, v0, [I

    fill-array-data v11, :array_a

    new-array v12, v0, [I

    fill-array-data v12, :array_b

    new-array v13, v0, [I

    fill-array-data v13, :array_c

    new-array v14, v0, [I

    fill-array-data v14, :array_d

    filled-new-array/range {v2 .. v14}, [[I

    move-result-object v15

    new-array v1, v0, [I

    fill-array-data v1, :array_e

    new-array v2, v0, [I

    fill-array-data v2, :array_f

    filled-new-array {v1, v2}, [[I

    move-result-object v16

    new-array v1, v0, [I

    fill-array-data v1, :array_10

    new-array v2, v0, [I

    fill-array-data v2, :array_11

    new-array v3, v0, [I

    fill-array-data v3, :array_12

    new-array v4, v0, [I

    fill-array-data v4, :array_13

    filled-new-array {v1, v2, v3, v4}, [[I

    move-result-object v17

    new-array v1, v0, [I

    fill-array-data v1, :array_14

    new-array v2, v0, [I

    fill-array-data v2, :array_15

    filled-new-array {v1, v2}, [[I

    move-result-object v18

    new-array v1, v0, [I

    fill-array-data v1, :array_16

    new-array v2, v0, [I

    fill-array-data v2, :array_17

    new-array v3, v0, [I

    fill-array-data v3, :array_18

    new-array v4, v0, [I

    fill-array-data v4, :array_19

    new-array v5, v0, [I

    fill-array-data v5, :array_1a

    new-array v6, v0, [I

    fill-array-data v6, :array_1b

    new-array v7, v0, [I

    fill-array-data v7, :array_1c

    new-array v8, v0, [I

    fill-array-data v8, :array_1d

    new-array v9, v0, [I

    fill-array-data v9, :array_1e

    new-array v10, v0, [I

    fill-array-data v10, :array_1f

    new-array v11, v0, [I

    fill-array-data v11, :array_20

    new-array v12, v0, [I

    fill-array-data v12, :array_21

    new-array v13, v0, [I

    fill-array-data v13, :array_22

    filled-new-array/range {v1 .. v13}, [[I

    move-result-object v19

    const/4 v0, 0x0

    new-array v1, v0, [[I

    new-array v2, v0, [[I

    new-array v0, v0, [[I

    move-object/from16 v20, v1

    move-object/from16 v21, v2

    move-object/from16 v22, v0

    filled-new-array/range {v15 .. v22}, [[[I

    move-result-object v0

    sput-object v0, Lhiro/swiftkey/KeySwapBinary;->ROWS:[[[I

    const/16 v0, 0x9

    new-array v1, v0, [I

    fill-array-data v1, :array_23

    filled-new-array {v1}, [[I

    move-result-object v2

    new-array v1, v0, [I

    fill-array-data v1, :array_24

    filled-new-array {v1}, [[I

    move-result-object v3

    new-array v1, v0, [I

    fill-array-data v1, :array_25

    new-array v4, v0, [I

    fill-array-data v4, :array_26

    filled-new-array {v1, v4}, [[I

    move-result-object v4

    new-array v1, v0, [I

    fill-array-data v1, :array_27

    filled-new-array {v1}, [[I

    move-result-object v5

    new-array v1, v0, [I

    fill-array-data v1, :array_28

    filled-new-array {v1}, [[I

    move-result-object v6

    new-array v1, v0, [I

    fill-array-data v1, :array_29

    new-array v7, v0, [I

    fill-array-data v7, :array_2a

    filled-new-array {v1, v7}, [[I

    move-result-object v7

    new-array v1, v0, [I

    fill-array-data v1, :array_2b

    filled-new-array {v1}, [[I

    move-result-object v8

    new-array v0, v0, [I

    fill-array-data v0, :array_2c

    filled-new-array {v0}, [[I

    move-result-object v9

    filled-new-array/range {v2 .. v9}, [[[I

    move-result-object v0

    sput-object v0, Lhiro/swiftkey/KeySwapBinary;->ARROWS:[[[I

    return-void

    nop

    :array_0
    .array-data 4
        0x3078
        0x106a
        0x2094
        0xe24
        0x3010
        0x2094
        0x106a
        0xe25
    .end array-data

    :array_1
    .array-data 4
        0xaf0
        0xb4b
        0xdd5
        0xe39
        0xb1f
        0xe0f
        -0x1
        0x2
    .end array-data

    :array_2
    .array-data 4
        0xe4a
        0xea3
        0xfca
        0x103f
        0xe79
        0x1003
        0xf37
        0x1
    .end array-data

    :array_3
    .array-data 4
        0x1050
        0x10a9
        0x1231
        0x12a6
        0x107f
        0x126a
        0x119e
        0x1
    .end array-data

    :array_4
    .array-data 4
        0x12b7
        0x1310
        0x14a4
        0x1519
        0x12e6
        0x14dd
        0x13a4
        0x1
    .end array-data

    :array_5
    .array-data 4
        0x152a
        0x1583
        0x1778
        0x17ed
        0x1559
        0x17b1
        0x1678
        0x1
    .end array-data

    :array_6
    .array-data 4
        0x17fe
        0x1857
        0x19d9
        0x1a4e
        0x182d
        0x1a12
        0x18da
        0x1
    .end array-data

    :array_7
    .array-data 4
        0x1a5f
        0x1ab8
        0x1c9b
        0x1d10
        0x1a8e
        0x1cd4
        0x1b9c
        0x1
    .end array-data

    :array_8
    .array-data 4
        0x1d21
        0x1d7a
        0x1f69
        0x1fde
        0x1d50
        0x1fa2
        0x1dfd
        0x1
    .end array-data

    :array_9
    .array-data 4
        0x1fef
        0x2048
        0x2298
        0x230d
        0x201e
        0x22d1
        0x212c
        0x1
    .end array-data

    :array_a
    .array-data 4
        0x231e
        0x2377
        0x251c
        0x2591
        0x234d
        0x2555
        0x240b
        0x1
    .end array-data

    :array_b
    .array-data 4
        0x25a2
        0x25fb
        0x2801
        0x2876
        0x25d1
        0x283a
        0x26f0
        0x1
    .end array-data

    :array_c
    .array-data 4
        0x2887
        0x28e0
        0x2af2
        0x2b67
        0x28b6
        0x2b2b
        0x2974
        0x1
    .end array-data

    :array_d
    .array-data 4
        0x2b78
        0x2bd1
        0x2e44
        0x2eb9
        0x2ba7
        0x2e7d
        0x2cc6
        0x1
    .end array-data

    :array_e
    .array-data 4
        0x88a
        0x8e3
        0xc2c
        0xc90
        0x8b9
        0xc66
        -0x1
        0x8
    .end array-data

    :array_f
    .array-data 4
        0xca1
        0xd00
        0xe36
        0xeab
        0xcd6
        0xe6f
        -0x1
        0x4
    .end array-data

    :array_10
    .array-data 4
        0x7af
        0x808
        0xaf9
        0xb5d
        0x7de
        0xb33
        -0x1
        0x8
    .end array-data

    :array_11
    .array-data 4
        0xb6e
        0xbcd
        0xe36
        0xeab
        0xba3
        0xe6f
        -0x1
        0x4
    .end array-data

    :array_12
    .array-data 4
        0x1639
        0x1692
        0x1983
        0x19e7
        0x1668
        0x19bd
        -0x1
        0x8
    .end array-data

    :array_13
    .array-data 4
        0x19f8
        0x1a57
        0x1cc0
        0x1d35
        0x1a2d
        0x1cf9
        -0x1
        0x4
    .end array-data

    :array_14
    .array-data 4
        0x68e
        0x6e7
        0x951
        0x9b5
        0x6bd
        0x98b
        -0x1
        0x8
    .end array-data

    :array_15
    .array-data 4
        0x9c6
        0xa25
        0xbf0
        0xc65
        0x9fb
        0xc29
        -0x1
        0x4
    .end array-data

    :array_16
    .array-data 4
        0xaf0
        0xb53
        0xddd
        0xe41
        0xb29
        0xe17
        -0x1
        0x80
    .end array-data

    :array_17
    .array-data 4
        0xe52
        0xeab
        0xfc8
        0x103d
        0xe81
        0x1001
        0xf3d
        0x40
    .end array-data

    :array_18
    .array-data 4
        0x104e
        0x10a7
        0x1225
        0x129a
        0x107d
        0x125e
        0x119a
        0x40
    .end array-data

    :array_19
    .array-data 4
        0x12ab
        0x1304
        0x148e
        0x1503
        0x12da
        0x14c7
        0x1396
        0x40
    .end array-data

    :array_1a
    .array-data 4
        0x1514
        0x156d
        0x1758
        0x17cd
        0x1543
        0x1791
        0x1660
        0x40
    .end array-data

    :array_1b
    .array-data 4
        0x17de
        0x1837
        0x19b1
        0x1a26
        0x180d
        0x19ea
        0x18ba
        0x40
    .end array-data

    :array_1c
    .array-data 4
        0x1a37
        0x1a90
        0x1c6b
        0x1ce0
        0x1a66
        0x1ca4
        0x1b74
        0x40
    .end array-data

    :array_1d
    .array-data 4
        0x1cf1
        0x1d4a
        0x1f31
        0x1fa6
        0x1d20
        0x1f6a
        0x1dcd
        0x40
    .end array-data

    :array_1e
    .array-data 4
        0x1fb7
        0x2010
        0x2258
        0x22cd
        0x1fe6
        0x2291
        0x20f4
        0x40
    .end array-data

    :array_1f
    .array-data 4
        0x22de
        0x2337
        0x24d2
        0x2547
        0x230d
        0x250b
        0x23c9
        0x40
    .end array-data

    :array_20
    .array-data 4
        0x2558
        0x25b1
        0x27ad
        0x2822
        0x2587
        0x27e6
        0x26a4
        0x40
    .end array-data

    :array_21
    .array-data 4
        0x2833
        0x288c
        0x2a94
        0x2b09
        0x2862
        0x2acd
        0x291e
        0x40
    .end array-data

    :array_22
    .array-data 4
        0x2b1a
        0x2b73
        0x2ddc
        0x2e51
        0x2b49
        0x2e15
        0x2c66
        0x40
    .end array-data

    :array_23
    .array-data 4
        0x2eca
        0x2f2c
        0x2f90
        0x2ff4
        0x3059
        0x2f02
        0x2f66
        0x2fca
        0x302f
    .end array-data

    :array_24
    .array-data 4
        0xebc
        0xf1e
        0xf82
        0xfe6
        0x104b
        0xef4
        0xf58
        0xfbc
        0x1021
    .end array-data

    :array_25
    .array-data 4
        0x1d46
        0x1da8
        0x1e0c
        0x1e70
        0x1ed5
        0x1d7e
        0x1de2
        0x1e46
        0x1eab
    .end array-data

    :array_26
    .array-data 4
        0x1ee6
        0x1f48
        0x1fac
        0x2010
        0x2075
        0x1f1e
        0x1f82
        0x1fe6
        0x204b
    .end array-data

    :array_27
    .array-data 4
        0xc76
        0xcd8
        0xd3c
        0xda0
        0xe05
        0xcae
        0xd12
        0xd76
        0xddb
    .end array-data

    :array_28
    .array-data 4
        0x2e62
        0x2ec4
        0x2f28
        0x2f8c
        0x2ff1
        0x2e9a
        0x2efe
        0x2f62
        0x2fc7
    .end array-data

    :array_29
    .array-data 4
        0x1d46
        0x1da8
        0x1e0c
        0x1e70
        0x1ed5
        0x1d7e
        0x1de2
        0x1e46
        0x1eab
    .end array-data

    :array_2a
    .array-data 4
        0x1ee6
        0x1f48
        0x1fac
        0x2010
        0x2075
        0x1f1e
        0x1f82
        0x1fe6
        0x204b
    .end array-data

    :array_2b
    .array-data 4
        0xebc
        0xf1e
        0xf82
        0xfe6
        0x104b
        0xef4
        0xf58
        0xfbc
        0x1021
    .end array-data

    :array_2c
    .array-data 4
        0xc77
        0xcd9
        0xd3d
        0xda1
        0xe06
        0xcaf
        0xd13
        0xd77
        0xddc
    .end array-data
.end method

.method private constructor <init>()V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method private static getDouble([BI)D
    .locals 7

    nop

    const-wide/16 v0, 0x0

    const/4 v2, 0x0

    :goto_0
    const/16 v3, 0x8

    if-ge v2, v3, :cond_0

    shl-long/2addr v0, v3

    add-int v3, p1, v2

    aget-byte v3, p0, v3

    int-to-long v3, v3

    const-wide/16 v5, 0xff

    and-long/2addr v3, v5

    or-long/2addr v0, v3

    add-int/lit8 v2, v2, 0x1

    goto :goto_0

    :cond_0
    invoke-static {v0, v1}, Ljava/lang/Double;->longBitsToDouble(J)D

    move-result-wide p0

    return-wide p0
.end method

.method static hex([B)Ljava/lang/String;
    .locals 6

    const-string v0, "0123456789abcdef"

    invoke-virtual {v0}, Ljava/lang/String;->toCharArray()[C

    move-result-object v0

    new-instance v1, Ljava/lang/StringBuilder;

    array-length v2, p0

    mul-int/lit8 v2, v2, 0x2

    invoke-direct {v1, v2}, Ljava/lang/StringBuilder;-><init>(I)V

    array-length v2, p0

    const/4 v3, 0x0

    :goto_0
    if-ge v3, v2, :cond_0

    aget-byte v4, p0, v3

    ushr-int/lit8 v5, v4, 0x4

    and-int/lit8 v5, v5, 0xf

    aget-char v5, v0, v5

    invoke-virtual {v1, v5}, Ljava/lang/StringBuilder;->append(C)Ljava/lang/StringBuilder;

    and-int/lit8 v4, v4, 0xf

    aget-char v4, v0, v4

    invoke-virtual {v1, v4}, Ljava/lang/StringBuilder;->append(C)Ljava/lang/StringBuilder;

    add-int/lit8 v3, v3, 0x1

    goto :goto_0

    :cond_0
    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    return-object p0
.end method

.method private static putDouble([BID)V
    .locals 3

    invoke-static {p2, p3}, Ljava/lang/Double;->doubleToLongBits(D)J

    move-result-wide p2

    const/4 v0, 0x7

    :goto_0
    if-ltz v0, :cond_0

    add-int v1, p1, v0

    long-to-int v2, p2

    int-to-byte v2, v2

    aput-byte v2, p0, v1

    const/16 v1, 0x8

    ushr-long/2addr p2, v1

    add-int/lit8 v0, v0, -0x1

    goto :goto_0

    :cond_0
    return-void
.end method

.method static read(Ljava/io/InputStream;I)[B
    .locals 4
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/io/IOException;
        }
    .end annotation

    new-instance v0, Ljava/io/ByteArrayOutputStream;

    invoke-direct {v0}, Ljava/io/ByteArrayOutputStream;-><init>()V

    const/16 v1, 0x800

    new-array v1, v1, [B

    :goto_0
    invoke-virtual {p0, v1}, Ljava/io/InputStream;->read([B)I

    move-result v2

    const/4 v3, -0x1

    if-eq v2, v3, :cond_1

    invoke-virtual {v0}, Ljava/io/ByteArrayOutputStream;->size()I

    move-result v3

    add-int/2addr v3, v2

    if-gt v3, p1, :cond_0

    const/4 v3, 0x0

    invoke-virtual {v0, v1, v3, v2}, Ljava/io/ByteArrayOutputStream;->write([BII)V

    goto :goto_0

    :cond_0
    new-instance p0, Ljava/io/IOException;

    const-string p1, "Layout is too large"

    invoke-direct {p0, p1}, Ljava/io/IOException;-><init>(Ljava/lang/String;)V

    throw p0

    :cond_1
    invoke-virtual {v0}, Ljava/io/ByteArrayOutputStream;->toByteArray()[B

    move-result-object p0

    return-object p0
.end method

.method public static transform([B)[B
    .locals 1
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/lang/Exception;
        }
    .end annotation

    const/4 v0, 0x1

    invoke-static {p0, v0}, Lhiro/swiftkey/KeySwapBinary;->transform([BI)[B

    move-result-object p0

    return-object p0
.end method

.method public static transform([BI)[B
    .locals 23
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/lang/Exception;
        }
    .end annotation

    move-object/from16 v0, p0

    move/from16 v1, p1

    if-eqz v0, :cond_11

    array-length v2, v0

    const/16 v3, 0x1000

    if-gt v2, v3, :cond_11

    and-int/lit16 v2, v1, -0x100

    if-nez v2, :cond_11

    const-string v2, "SHA-256"

    invoke-static {v2}, Ljava/security/MessageDigest;->getInstance(Ljava/lang/String;)Ljava/security/MessageDigest;

    move-result-object v2

    invoke-virtual {v2, v0}, Ljava/security/MessageDigest;->digest([B)[B

    move-result-object v2

    invoke-static {v2}, Lhiro/swiftkey/KeySwapBinary;->hex([B)Ljava/lang/String;

    move-result-object v2

    nop

    const/4 v3, 0x0

    move v4, v3

    :goto_0
    sget-object v5, Lhiro/swiftkey/KeySwapBinary;->HASHES:[Ljava/lang/String;

    array-length v5, v5

    if-ge v4, v5, :cond_1

    sget-object v5, Lhiro/swiftkey/KeySwapBinary;->HASHES:[Ljava/lang/String;

    aget-object v5, v5, v4

    invoke-virtual {v5, v2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v5

    if-eqz v5, :cond_0

    goto :goto_1

    :cond_0
    add-int/lit8 v4, v4, 0x1

    goto :goto_0

    :cond_1
    const/4 v4, -0x1

    :goto_1
    if-ltz v4, :cond_10

    new-instance v2, Ljava/util/zip/InflaterInputStream;

    new-instance v5, Ljava/io/ByteArrayInputStream;

    invoke-direct {v5, v0}, Ljava/io/ByteArrayInputStream;-><init>([B)V

    invoke-direct {v2, v5}, Ljava/util/zip/InflaterInputStream;-><init>(Ljava/io/InputStream;)V

    const/16 v5, 0x4000

    invoke-static {v2, v5}, Lhiro/swiftkey/KeySwapBinary;->read(Ljava/io/InputStream;I)[B

    move-result-object v2

    array-length v5, v2

    sget-object v6, Lhiro/swiftkey/KeySwapBinary;->SIZES:[I

    aget v6, v6, v4

    if-ne v5, v6, :cond_f

    new-instance v5, Ljava/io/ByteArrayOutputStream;

    array-length v6, v2

    invoke-direct {v5, v6}, Ljava/io/ByteArrayOutputStream;-><init>(I)V

    nop

    sget-object v6, Lhiro/swiftkey/KeySwapBinary;->ROWS:[[[I

    aget-object v6, v6, v4

    array-length v7, v6

    move v8, v3

    move v9, v8

    :goto_2
    const/4 v10, 0x3

    const/4 v12, 0x2

    const/4 v13, 0x1

    if-ge v8, v7, :cond_8

    aget-object v14, v6, v8

    const/4 v15, 0x7

    aget v15, v14, v15

    and-int/2addr v15, v1

    if-eqz v15, :cond_2

    move v15, v13

    goto :goto_3

    :cond_2
    move v15, v3

    :goto_3
    const/16 v16, 0x6

    aget v17, v14, v16

    if-ltz v17, :cond_3

    move/from16 v17, v13

    goto :goto_4

    :cond_3
    move/from16 v17, v3

    :goto_4
    if-nez v15, :cond_4

    if-nez v17, :cond_4

    move-object v12, v6

    move/from16 v19, v7

    goto/16 :goto_7

    :cond_4
    aget v18, v14, v3

    sub-int v11, v18, v9

    invoke-virtual {v5, v2, v9, v11}, Ljava/io/ByteArrayOutputStream;->write([BII)V

    aget v9, v14, v3

    aget v11, v14, v13

    invoke-static {v2, v9, v11}, Ljava/util/Arrays;->copyOfRange([BII)[B

    move-result-object v9

    aget v11, v14, v13

    aget v3, v14, v12

    invoke-static {v2, v11, v3}, Ljava/util/Arrays;->copyOfRange([BII)[B

    move-result-object v3

    aget v11, v14, v12

    aget v12, v14, v10

    invoke-static {v2, v11, v12}, Ljava/util/Arrays;->copyOfRange([BII)[B

    move-result-object v11

    const/4 v12, 0x4

    aget v10, v14, v12

    move-object v12, v6

    move/from16 v19, v7

    invoke-static {v2, v10}, Lhiro/swiftkey/KeySwapBinary;->getDouble([BI)D

    move-result-wide v6

    const/4 v10, 0x5

    aget v13, v14, v10

    move-object/from16 v20, v11

    invoke-static {v2, v13}, Lhiro/swiftkey/KeySwapBinary;->getDouble([BI)D

    move-result-wide v10

    if-eqz v17, :cond_6

    invoke-static {v6, v7, v10, v11}, Ljava/lang/Math;->max(DD)D

    move-result-wide v21

    sub-double v6, v21, v6

    sub-double v10, v21, v10

    add-double/2addr v6, v10

    aget v10, v14, v16

    invoke-static {v2, v10}, Lhiro/swiftkey/KeySwapBinary;->getDouble([BI)D

    move-result-wide v10

    sub-double/2addr v10, v6

    const-wide/16 v6, 0x0

    cmpg-double v6, v10, v6

    if-lez v6, :cond_5

    invoke-static {v10, v11}, Ljava/lang/Double;->isNaN(D)Z

    move-result v6

    if-nez v6, :cond_5

    invoke-static {v10, v11}, Ljava/lang/Double;->isInfinite(D)Z

    move-result v6

    if-nez v6, :cond_5

    aget v6, v14, v16

    const/4 v7, 0x1

    aget v7, v14, v7

    sub-int/2addr v6, v7

    invoke-static {v3, v6, v10, v11}, Lhiro/swiftkey/KeySwapBinary;->putDouble([BID)V

    move-wide/from16 v6, v21

    move-wide v10, v6

    goto :goto_5

    :cond_5
    new-instance v0, Ljava/io/IOException;

    const-string v1, "Invalid space width"

    invoke-direct {v0, v1}, Ljava/io/IOException;-><init>(Ljava/lang/String;)V

    throw v0

    :cond_6
    :goto_5
    if-eqz v15, :cond_7

    const/4 v13, 0x5

    aget v13, v14, v13

    const/4 v15, 0x2

    aget v15, v14, v15

    sub-int/2addr v13, v15

    move-object/from16 v15, v20

    invoke-static {v15, v13, v6, v7}, Lhiro/swiftkey/KeySwapBinary;->putDouble([BID)V

    const/4 v13, 0x4

    aget v6, v14, v13

    const/16 v16, 0x0

    aget v7, v14, v16

    sub-int/2addr v6, v7

    invoke-static {v9, v6, v10, v11}, Lhiro/swiftkey/KeySwapBinary;->putDouble([BID)V

    invoke-virtual {v5, v15}, Ljava/io/ByteArrayOutputStream;->write([B)V

    invoke-virtual {v5, v3}, Ljava/io/ByteArrayOutputStream;->write([B)V

    invoke-virtual {v5, v9}, Ljava/io/ByteArrayOutputStream;->write([B)V

    goto :goto_6

    :cond_7
    move-object/from16 v15, v20

    const/4 v13, 0x4

    const/16 v16, 0x0

    aget v13, v14, v13

    aget v17, v14, v16

    sub-int v13, v13, v17

    invoke-static {v9, v13, v6, v7}, Lhiro/swiftkey/KeySwapBinary;->putDouble([BID)V

    const/4 v6, 0x5

    aget v6, v14, v6

    const/4 v7, 0x2

    aget v7, v14, v7

    sub-int/2addr v6, v7

    invoke-static {v15, v6, v10, v11}, Lhiro/swiftkey/KeySwapBinary;->putDouble([BID)V

    invoke-virtual {v5, v9}, Ljava/io/ByteArrayOutputStream;->write([B)V

    invoke-virtual {v5, v3}, Ljava/io/ByteArrayOutputStream;->write([B)V

    invoke-virtual {v5, v15}, Ljava/io/ByteArrayOutputStream;->write([B)V

    :goto_6
    const/4 v3, 0x3

    aget v9, v14, v3

    :goto_7
    add-int/lit8 v8, v8, 0x1

    move-object v6, v12

    move/from16 v7, v19

    const/4 v3, 0x0

    goto/16 :goto_2

    :cond_8
    array-length v3, v2

    sub-int/2addr v3, v9

    invoke-virtual {v5, v2, v9, v3}, Ljava/io/ByteArrayOutputStream;->write([BII)V

    invoke-virtual {v5}, Ljava/io/ByteArrayOutputStream;->size()I

    move-result v3

    array-length v6, v2

    if-ne v3, v6, :cond_e

    invoke-virtual {v5}, Ljava/io/ByteArrayOutputStream;->toByteArray()[B

    move-result-object v3

    const/4 v5, 0x4

    new-array v6, v5, [I

    const/4 v7, 0x2

    const/4 v8, 0x0

    aput v7, v6, v8

    const/4 v9, 0x3

    const/4 v10, 0x1

    aput v9, v6, v10

    aput v8, v6, v7

    aput v10, v6, v9

    if-ge v4, v5, :cond_9

    and-int/lit8 v5, v1, 0x20

    if-eqz v5, :cond_9

    aput v9, v6, v8

    aput v7, v6, v10

    :cond_9
    const/4 v5, 0x4

    if-ge v4, v5, :cond_a

    and-int/lit8 v1, v1, 0x10

    if-eqz v1, :cond_a

    const/4 v1, 0x2

    const/4 v5, 0x1

    aput v5, v6, v1

    const/4 v1, 0x3

    const/4 v5, 0x0

    aput v5, v6, v1

    :cond_a
    sget-object v1, Lhiro/swiftkey/KeySwapBinary;->ARROWS:[[[I

    aget-object v1, v1, v4

    array-length v4, v1

    const/4 v5, 0x0

    :goto_8
    if-ge v5, v4, :cond_d

    aget-object v7, v1, v5

    const/4 v8, 0x0

    aget v9, v7, v8

    const/4 v8, 0x0

    :goto_9
    const/4 v10, 0x4

    if-ge v8, v10, :cond_b

    aget v10, v6, v8

    aget v11, v7, v10

    add-int/lit8 v12, v10, 0x1

    aget v12, v7, v12

    invoke-static {v2, v11, v12}, Ljava/util/Arrays;->copyOfRange([BII)[B

    move-result-object v11

    aget v12, v7, v8

    const/16 v13, 0xa

    add-int/2addr v12, v13

    const/4 v14, 0x2

    invoke-static {v2, v12, v11, v13, v14}, Ljava/lang/System;->arraycopy(Ljava/lang/Object;ILjava/lang/Object;II)V

    add-int/lit8 v12, v10, 0x5

    aget v12, v7, v12

    aget v10, v7, v10

    sub-int/2addr v12, v10

    add-int/lit8 v10, v8, 0x5

    aget v10, v7, v10

    invoke-static {v2, v10}, Lhiro/swiftkey/KeySwapBinary;->getDouble([BI)D

    move-result-wide v14

    invoke-static {v11, v12, v14, v15}, Lhiro/swiftkey/KeySwapBinary;->putDouble([BID)V

    array-length v10, v11

    const/4 v12, 0x0

    invoke-static {v11, v12, v3, v9, v10}, Ljava/lang/System;->arraycopy(Ljava/lang/Object;ILjava/lang/Object;II)V

    array-length v10, v11

    add-int/2addr v9, v10

    add-int/lit8 v8, v8, 0x1

    goto :goto_9

    :cond_b
    const/4 v12, 0x0

    const/4 v8, 0x4

    aget v7, v7, v8

    if-ne v9, v7, :cond_c

    add-int/lit8 v5, v5, 0x1

    goto :goto_8

    :cond_c
    new-instance v0, Ljava/io/IOException;

    const-string v1, "Arrow row length changed"

    invoke-direct {v0, v1}, Ljava/io/IOException;-><init>(Ljava/lang/String;)V

    throw v0

    :cond_d
    new-instance v1, Ljava/io/ByteArrayOutputStream;

    array-length v0, v0

    add-int/lit8 v0, v0, 0x40

    invoke-direct {v1, v0}, Ljava/io/ByteArrayOutputStream;-><init>(I)V

    new-instance v0, Ljava/util/zip/DeflaterOutputStream;

    invoke-direct {v0, v1}, Ljava/util/zip/DeflaterOutputStream;-><init>(Ljava/io/OutputStream;)V

    invoke-virtual {v0, v3}, Ljava/util/zip/DeflaterOutputStream;->write([B)V

    invoke-virtual {v0}, Ljava/util/zip/DeflaterOutputStream;->close()V

    invoke-virtual {v1}, Ljava/io/ByteArrayOutputStream;->toByteArray()[B

    move-result-object v0

    return-object v0

    :cond_e
    new-instance v0, Ljava/io/IOException;

    const-string v1, "Layout length changed"

    invoke-direct {v0, v1}, Ljava/io/IOException;-><init>(Ljava/lang/String;)V

    throw v0

    :cond_f
    new-instance v0, Ljava/io/IOException;

    const-string v1, "Layout size differs"

    invoke-direct {v0, v1}, Ljava/io/IOException;-><init>(Ljava/lang/String;)V

    throw v0

    :cond_10
    new-instance v0, Ljava/io/IOException;

    const-string v1, "Unrecognized keyboard layout"

    invoke-direct {v0, v1}, Ljava/io/IOException;-><init>(Ljava/lang/String;)V

    throw v0

    :cond_11
    new-instance v0, Ljava/io/IOException;

    const-string v1, "Invalid keyboard layout request"

    invoke-direct {v0, v1}, Ljava/io/IOException;-><init>(Ljava/lang/String;)V

    throw v0
.end method

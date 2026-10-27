.class public final Lje0/c;
.super Ljava/lang/Object;
.source "SourceFile"

# interfaces
.implements Lkd0/n1;


# instance fields
.field public final a:Lgd0/i;

.field public final b:Landroidx/lifecycle/q0;

.field public final c:Lgg/a;

.field public final d:Z

.field public final e:Loz/i6;

.field public final f:Lf40/a;

.field public final g:Lfb0/g0;

.field public final h:Lfb0/d;

.field public final i:Lfb0/b4;

.field public final j:Lwa0/q0;

.field public final k:Landroid/view/LayoutInflater;

.field public final l:Lf50/d;


# direct methods
.method public constructor <init>(Landroid/view/ContextThemeWrapper;Lma0/q;Lgd0/i;Landroidx/lifecycle/q0;Lgg/a;ZLoz/i6;Lf40/a;Lah0/w0;Lfz/h;Lwb0/x;Lfb0/n;Lfb0/g0;Lfb0/d;Lfb0/b4;Lwa0/q0;Z)V
    .locals 17

    move-object/from16 v0, p0

    move-object/from16 v1, p2

    move-object/from16 v2, p3

    move-object/from16 v3, p4

    move-object/from16 v4, p5

    move-object/from16 v5, p7

    move-object/from16 v6, p11

    move-object/from16 v7, p16

    const-string v8, "toolbarPanelLayoutBinding"

    invoke-static {v1, v8}, Lkotlin/jvm/internal/m;->f(Ljava/lang/Object;Ljava/lang/String;)V

    const-string v8, "themeViewModel"

    invoke-static {v2, v8}, Lkotlin/jvm/internal/m;->f(Ljava/lang/Object;Ljava/lang/String;)V

    iget-object v8, v2, Lgd0/i;->c:Landroidx/lifecycle/c1;

    const-string v9, "overlayTrigger"

    invoke-static {v5, v9}, Lkotlin/jvm/internal/m;->f(Ljava/lang/Object;Ljava/lang/String;)V

    const-string v9, "superlayController"

    invoke-static {v7, v9}, Lkotlin/jvm/internal/m;->f(Ljava/lang/Object;Ljava/lang/String;)V

    invoke-direct {v0}, Ljava/lang/Object;-><init>()V

    iput-object v2, v0, Lje0/c;->a:Lgd0/i;

    iput-object v3, v0, Lje0/c;->b:Landroidx/lifecycle/q0;

    iput-object v4, v0, Lje0/c;->c:Lgg/a;

    move/from16 v2, p6

    iput-boolean v2, v0, Lje0/c;->d:Z

    iput-object v5, v0, Lje0/c;->e:Loz/i6;

    move-object/from16 v2, p8

    iput-object v2, v0, Lje0/c;->f:Lf40/a;

    move-object/from16 v2, p13

    iput-object v2, v0, Lje0/c;->g:Lfb0/g0;

    move-object/from16 v2, p14

    iput-object v2, v0, Lje0/c;->h:Lfb0/d;

    move-object/from16 v2, p15

    iput-object v2, v0, Lje0/c;->i:Lfb0/b4;

    iput-object v7, v0, Lje0/c;->j:Lwa0/q0;

    invoke-static/range {p1 .. p1}, Landroid/view/LayoutInflater;->from(Landroid/content/Context;)Landroid/view/LayoutInflater;

    move-result-object v2

    invoke-static {v2}, Lkotlin/jvm/internal/m;->c(Ljava/lang/Object;)V

    iput-object v2, v0, Lje0/c;->k:Landroid/view/LayoutInflater;

    iget-object v1, v1, Lma0/q;->e:Landroid/widget/FrameLayout;

    const v5, 0x7f0d010e

    const/4 v7, 0x0

    invoke-virtual {v2, v5, v1, v7}, Landroid/view/LayoutInflater;->inflate(ILandroid/view/ViewGroup;Z)Landroid/view/View;

    move-result-object v2

    if-nez p17, :cond_0

    invoke-virtual {v1, v2}, Landroid/view/ViewGroup;->addView(Landroid/view/View;)V

    :cond_0
    const v1, 0x7f0a0543

    invoke-static {v2, v1}, La/a;->y(Landroid/view/View;I)Landroid/view/View;

    move-result-object v5

    check-cast v5, Lcom/google/android/material/button/MaterialButton;

    if-eqz v5, :cond_a

    const v1, 0x7f0a054a

    invoke-static {v2, v1}, La/a;->y(Landroid/view/View;I)Landroid/view/View;

    move-result-object v9

    check-cast v9, Lcom/touchtype/keyboard/view/DeleteKeyButton;

    if-eqz v9, :cond_a

    const v1, 0x7f0a054e

    invoke-static {v2, v1}, La/a;->y(Landroid/view/View;I)Landroid/view/View;

    move-result-object v10

    check-cast v10, Lcom/touchtype/ui/SwiftKeyTabLayout;

    if-eqz v10, :cond_a

    new-instance v1, Lf50/d;

    check-cast v2, Landroidx/constraintlayout/widget/ConstraintLayout;

    invoke-direct {v1, v2, v5, v9, v10}, Lf50/d;-><init>(Landroidx/constraintlayout/widget/ConstraintLayout;Lcom/google/android/material/button/MaterialButton;Lcom/touchtype/keyboard/view/DeleteKeyButton;Lcom/touchtype/ui/SwiftKeyTabLayout;)V

    iput-object v1, v0, Lje0/c;->l:Lf50/d;

    invoke-virtual/range {p12 .. p12}, Lfb0/n;->a()Lqj0/a;

    move-result-object v1

    invoke-virtual {v1}, Lqj0/a;->d()Lcr/t;

    move-result-object v1

    sget-object v2, Ljava/util/Locale;->ENGLISH:Ljava/util/Locale;

    invoke-virtual {v1, v2}, Lcr/t;->g(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v1

    const-string v2, "or(...)"

    invoke-static {v1, v2}, Lkotlin/jvm/internal/m;->e(Ljava/lang/Object;Ljava/lang/String;)V

    check-cast v1, Ljava/util/Locale;

    const/4 v2, 0x0

    invoke-static {v1, v2, v2}, Lfc0/w1;->a(Ljava/util/Locale;Ljava/lang/String;Ljava/util/ArrayList;)Lfc0/w1;

    move-result-object v1

    sget-object v10, Lfc0/y1;->b:Lyd/b;

    const-string v10, "label_ABC"

    invoke-virtual {v1, v10, v10}, Lfc0/w1;->c(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v1

    invoke-virtual {v5, v1}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

    new-instance v1, Lae0/d;

    const/4 v10, 0x2

    invoke-direct {v1, v0, v10}, Lae0/d;-><init>(Ljava/lang/Object;I)V

    invoke-virtual {v5, v1}, Landroid/view/View;->setOnClickListener(Landroid/view/View$OnClickListener;)V

    invoke-virtual {v9, v7}, Landroid/view/View;->setVisibility(I)V

    new-instance v1, Lfc0/g2;

    invoke-direct {v1, v6}, Lfc0/g2;-><init>(Lwb0/t0;)V

    new-instance v5, Landroidx/credentials/playservices/j;

    invoke-direct {v5, v0}, Landroidx/credentials/playservices/j;-><init>(Ljava/lang/Object;)V

    iput-object v5, v1, Lfc0/g2;->j:Lfc0/n1;

    sget-object v5, Lwa0/p;->c:Lwa0/p;

    invoke-virtual {v4, v5}, Ljava/lang/Object;->equals(Ljava/lang/Object;)Z

    move-result v5

    if-eqz v5, :cond_1

    sget-object v4, Loz/o1;->a:Loz/o1;

    goto :goto_0

    :cond_1
    sget-object v5, Lwa0/a0;->c:Lwa0/a0;

    invoke-virtual {v4, v5}, Ljava/lang/Object;->equals(Ljava/lang/Object;)Z

    move-result v5

    if-eqz v5, :cond_2

    sget-object v4, Loz/o1;->c:Loz/o1;

    goto :goto_0

    :cond_2
    sget-object v5, Lwa0/n0;->c:Lwa0/n0;

    invoke-virtual {v4, v5}, Ljava/lang/Object;->equals(Ljava/lang/Object;)Z

    move-result v5

    if-eqz v5, :cond_3

    sget-object v4, Loz/o1;->d:Loz/o1;

    goto :goto_0

    :cond_3
    sget-object v5, Lwa0/d;->c:Lwa0/d;

    invoke-virtual {v4, v5}, Ljava/lang/Object;->equals(Ljava/lang/Object;)Z

    move-result v4

    if-eqz v4, :cond_9

    sget-object v4, Loz/o1;->b:Loz/o1;

    :goto_0
    new-instance v5, Ld70/p;

    const/16 v11, 0xa

    invoke-direct {v5, v11}, Ld70/p;-><init>(I)V

    new-instance v12, Lcom/touchtype_fluency/service/o;

    const/4 v13, 0x5

    invoke-direct {v12, v13}, Lcom/touchtype_fluency/service/o;-><init>(I)V

    iget-object v13, v9, Lcom/touchtype/keyboard/view/DeleteKeyButton;->c:Lxd0/l;

    iput-object v5, v9, Lcom/touchtype/keyboard/view/DeleteKeyButton;->b:Ld70/p;

    invoke-virtual/range {p9 .. p9}, Lah0/w0;->m()I

    move-result v5

    new-instance v14, Lhc0/m;

    add-int/lit16 v15, v5, 0x1f4

    invoke-virtual/range {p10 .. p10}, Lfz/h;->b()Z

    move-result v16

    move-object/from16 p1, v2

    move/from16 p2, v7

    const/4 v7, 0x1

    if-eqz v16, :cond_4

    const/4 v2, 0x3

    goto :goto_1

    :cond_4
    move v2, v7

    :goto_1
    invoke-direct {v14, v15, v2}, Lhc0/m;-><init>(II)V

    new-instance v2, Lic0/g;

    const/4 v15, -0x5

    invoke-direct {v2, v1, v15}, Lic0/g;-><init>(Lfc0/d2;I)V

    new-instance v15, Lhc0/b;

    invoke-direct {v15}, Lhc0/b;-><init>()V

    new-array v10, v7, [Lic0/a;

    aput-object v2, v10, p2

    new-instance v7, Lb40/a;

    invoke-direct {v7, v11}, Lb40/a;-><init>(I)V

    invoke-virtual {v15, v7, v10}, Lhc0/b;->l(Lcm0/d;[Lic0/a;)V

    sget-object v7, Lhc0/c;->c:Lhc0/c;

    sget-object v10, Lxd0/m;->a:[I

    invoke-virtual {v13}, Ljava/lang/Enum;->ordinal()I

    move-result v16

    aget v11, v10, v16

    move-object/from16 p8, v2

    const/4 v2, 0x1

    if-ne v11, v2, :cond_5

    new-instance v11, Lxd0/h;

    invoke-direct {v11, v6, v7, v4}, Lxd0/h;-><init>(Lwb0/x;Lhc0/c;Loz/o1;)V

    goto :goto_2

    :cond_5
    new-instance v11, Lxd0/i;

    invoke-direct {v11, v6, v4}, Lxd0/i;-><init>(Lwb0/x;Loz/o1;)V

    :goto_2
    new-array v7, v2, [Lic0/c0;

    aput-object v11, v7, p2

    new-instance v11, Laa0/m;

    const/16 v2, 0xd

    invoke-direct {v11, v2}, Laa0/m;-><init>(I)V

    invoke-virtual {v15, v11, v7}, Lhc0/b;->j(Lcm0/d;[Lic0/c0;)V

    invoke-virtual {v13}, Ljava/lang/Enum;->ordinal()I

    move-result v7

    aget v7, v10, v7

    const/4 v11, 0x1

    if-ne v7, v11, :cond_6

    new-instance v7, Lxd0/f;

    invoke-direct {v7, v6, v4}, Lxd0/f;-><init>(Lwb0/x;Loz/o1;)V

    goto :goto_3

    :cond_6
    new-instance v7, Lxd0/g;

    invoke-direct {v7, v6, v4}, Lxd0/g;-><init>(Lwb0/x;Loz/o1;)V

    :goto_3
    new-array v13, v11, [Lic0/a;

    aput-object v7, v13, p2

    new-instance v7, La2/a;

    move/from16 p5, v11

    const/16 v11, 0x9

    invoke-direct {v7, v11}, La2/a;-><init>(I)V

    invoke-virtual {v15, v7, v13}, Lhc0/b;->f(Lcm0/d;[Lic0/a;)V

    new-instance v7, Lxd0/j;

    invoke-direct {v7}, Ljava/lang/Object;-><init>()V

    const/4 v11, 0x2

    new-array v13, v11, [Lic0/a;

    sget-object v16, Lic0/d;->a:Lic0/b;

    aput-object v16, v13, p2

    aput-object v7, v13, p5

    invoke-virtual {v15, v13}, Lhc0/b;->k([Lic0/a;)V

    new-instance v7, Lic0/b;

    invoke-direct {v7, v12}, Lic0/b;-><init>(Lcom/touchtype_fluency/service/o;)V

    new-array v12, v11, [Lic0/a;

    aput-object p8, v12, p2

    aput-object v7, v12, p5

    new-instance v7, Lay/l;

    invoke-direct {v7, v2}, Lay/l;-><init>(I)V

    invoke-virtual {v15, v5, v7, v12}, Lhc0/b;->q(ILcm0/d;[Lic0/a;)V

    iget-object v2, v9, Lcom/touchtype/keyboard/view/DeleteKeyButton;->d:Lxd0/l;

    sget-object v7, Lhc0/c;->d:Lhc0/c;

    invoke-virtual {v2}, Ljava/lang/Enum;->ordinal()I

    move-result v2

    aget v2, v10, v2

    move/from16 v11, p5

    if-ne v2, v11, :cond_7

    new-instance v2, Lxd0/h;

    invoke-direct {v2, v6, v7, v4}, Lxd0/h;-><init>(Lwb0/x;Lhc0/c;Loz/o1;)V

    goto :goto_4

    :cond_7
    new-instance v2, Lxd0/i;

    invoke-direct {v2, v6, v4}, Lxd0/i;-><init>(Lwb0/x;Loz/o1;)V

    :goto_4
    new-array v10, v11, [Lic0/c0;

    aput-object v2, v10, p2

    new-instance v2, Laa0/f;

    const/16 v11, 0xf

    invoke-direct {v2, v11}, Laa0/f;-><init>(I)V

    invoke-virtual {v15, v5, v2, v10}, Lhc0/b;->r(ILcm0/d;[Lic0/c0;)V

    new-instance v2, Lic0/j;

    invoke-direct {v2, v6, v7, v4}, Lic0/j;-><init>(Lwb0/t0;Lhc0/c;Loz/o1;)V

    new-instance v4, Lxd0/k;

    invoke-direct {v4}, Ljava/lang/Object;-><init>()V

    const/4 v5, 0x3

    new-array v5, v5, [Lic0/x;

    aput-object v2, v5, p2

    const/4 v11, 0x1

    aput-object p8, v5, v11

    const/4 v11, 0x2

    aput-object v4, v5, v11

    new-instance v2, La60/t;

    const/16 v4, 0xb

    invoke-direct {v2, v4}, La60/t;-><init>(I)V

    invoke-virtual {v15, v14, v2, v5}, Lhc0/b;->t(Lhc0/l;Lcm0/d;[Lic0/x;)V

    invoke-virtual {v15, v1}, Lhc0/b;->e(Lfc0/d2;)Lhc0/a;

    move-result-object v2

    iput-object v2, v9, Lcom/touchtype/keyboard/view/DeleteKeyButton;->a:Lhc0/a;

    new-instance v5, Lxd0/m0;

    move-object/from16 v6, p10

    invoke-direct {v5, v1, v2, v6}, Lxd0/m0;-><init>(Lfc0/g2;Lhc0/a;Lfz/h;)V

    invoke-virtual {v9, v5}, Landroid/view/View;->setOnTouchListener(Landroid/view/View$OnTouchListener;)V

    new-instance v1, Lfz/e;

    invoke-direct {v1}, Lfz/e;-><init>()V

    invoke-virtual {v9}, Landroid/view/View;->getResources()Landroid/content/res/Resources;

    move-result-object v2

    const v5, 0x7f14019c

    invoke-virtual {v2, v5}, Landroid/content/res/Resources;->getString(I)Ljava/lang/String;

    move-result-object v2

    const-string v5, "getString(...)"

    invoke-static {v2, v5}, Lkotlin/jvm/internal/m;->e(Ljava/lang/Object;Ljava/lang/String;)V

    iput-object v2, v1, Lfz/e;->a:Ljava/lang/String;

    invoke-virtual {v9}, Landroid/view/View;->getResources()Landroid/content/res/Resources;

    move-result-object v2

    const v6, 0x7f14019b

    invoke-virtual {v2, v6}, Landroid/content/res/Resources;->getString(I)Ljava/lang/String;

    move-result-object v2

    invoke-static {v2, v5}, Lkotlin/jvm/internal/m;->e(Ljava/lang/Object;Ljava/lang/String;)V

    invoke-virtual {v1, v2}, Lfz/e;->b(Ljava/lang/String;)V

    invoke-virtual {v9}, Landroid/view/View;->getResources()Landroid/content/res/Resources;

    move-result-object v2

    const v6, 0x7f14019a

    invoke-virtual {v2, v6}, Landroid/content/res/Resources;->getString(I)Ljava/lang/String;

    move-result-object v2

    invoke-static {v2, v5}, Lkotlin/jvm/internal/m;->e(Ljava/lang/Object;Ljava/lang/String;)V

    invoke-virtual {v1, v2}, Lfz/e;->c(Ljava/lang/String;)V

    invoke-virtual {v1, v9}, Lfz/e;->a(Landroid/view/View;)V

    iget-object v1, v9, Lcom/touchtype/keyboard/view/DeleteKeyButton;->a:Lhc0/a;

    if-eqz v1, :cond_8

    invoke-static {v9, v1}, Lc50/p;->r(Landroid/view/View;Lhc0/a;)V

    new-instance v1, La60/t;

    const/16 v2, 0xa

    invoke-direct {v1, v2}, La60/t;-><init>(I)V

    invoke-static {v8, v1}, Landroidx/lifecycle/z1;->m(Landroidx/lifecycle/y0;Lcm0/d;)Landroidx/lifecycle/b1;

    move-result-object v1

    new-instance v2, Lae0/w;

    invoke-direct {v2, v0, v4}, Lae0/w;-><init>(Ljava/lang/Object;I)V

    new-instance v4, Lje0/b;

    invoke-direct {v4, v2}, Lje0/b;-><init>(Lcm0/d;)V

    invoke-virtual {v1, v3, v4}, Landroidx/lifecycle/y0;->e(Landroidx/lifecycle/q0;Landroidx/lifecycle/d1;)V

    new-instance v1, Leb0/c;

    const/16 v2, 0x8

    invoke-direct {v1, v2}, Leb0/c;-><init>(I)V

    invoke-static {v8, v1}, Landroidx/lifecycle/z1;->m(Landroidx/lifecycle/y0;Lcm0/d;)Landroidx/lifecycle/b1;

    move-result-object v1

    new-instance v2, Laa0/h0;

    const/16 v4, 0xc

    invoke-direct {v2, v0, v4}, Laa0/h0;-><init>(Ljava/lang/Object;I)V

    new-instance v4, Lje0/b;

    invoke-direct {v4, v2}, Lje0/b;-><init>(Lcm0/d;)V

    invoke-virtual {v1, v3, v4}, Landroidx/lifecycle/y0;->e(Landroidx/lifecycle/q0;Landroidx/lifecycle/d1;)V

    return-void

    :cond_8
    const-string v1, "action"

    invoke-static {v1}, Lkotlin/jvm/internal/m;->l(Ljava/lang/String;)V

    throw p1

    :cond_9
    new-instance v1, Ljava/lang/IllegalStateException;

    const-string v2, "Shouldn\'t be reached here"

    invoke-direct {v1, v2}, Ljava/lang/IllegalStateException;-><init>(Ljava/lang/String;)V

    throw v1

    :cond_a
    invoke-virtual {v2}, Landroid/view/View;->getResources()Landroid/content/res/Resources;

    move-result-object v2

    invoke-virtual {v2, v1}, Landroid/content/res/Resources;->getResourceName(I)Ljava/lang/String;

    move-result-object v1

    new-instance v2, Ljava/lang/NullPointerException;

    const-string v3, "Missing required view with ID: "

    invoke-virtual {v3, v1}, Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v1

    invoke-direct {v2, v1}, Ljava/lang/NullPointerException;-><init>(Ljava/lang/String;)V

    throw v2
.end method


# virtual methods
.method public final K()V
    .locals 0

    return-void
.end method

.method public final L(Lfb0/t2;)V
    .locals 1

    const-string v0, "overlayController"

    invoke-static {p1, v0}, Lkotlin/jvm/internal/m;->f(Ljava/lang/Object;Ljava/lang/String;)V

    sget-object v0, Loz/i6;->j:Loz/i6;

    invoke-interface {p1, v0}, Lfb0/t2;->a(Loz/i6;)V

    return-void
.end method

.method public final U(Ldd0/e0;)V
    .locals 1

    iget-object v0, p0, Lje0/c;->l:Lf50/d;

    iget-object v0, v0, Lf50/d;->d:Ljava/lang/Object;

    check-cast v0, Lcom/touchtype/ui/SwiftKeyTabLayout;

    invoke-virtual {v0, p1}, Lcom/touchtype/ui/SwiftKeyTabLayout;->q(Ldd0/e0;)V

    return-void
.end method

.method public final d(Landroidx/lifecycle/q0;)V
    .locals 5

    const-string v0, "owner"

    invoke-static {p1, v0}, Lkotlin/jvm/internal/m;->f(Ljava/lang/Object;Ljava/lang/String;)V

    new-instance p1, Ltz/i4;

    iget-object v0, p0, Lje0/c;->f:Lf40/a;

    invoke-interface {v0}, Lg40/d;->g()Ljz/a;

    move-result-object v1

    sget-object v2, Lwa0/p;->c:Lwa0/p;

    iget-object v3, p0, Lje0/c;->c:Lgg/a;

    invoke-virtual {v3, v2}, Ljava/lang/Object;->equals(Ljava/lang/Object;)Z

    move-result v2

    if-eqz v2, :cond_0

    sget-object v2, Loz/a3;->a:Loz/a3;

    goto :goto_0

    :cond_0
    sget-object v2, Lwa0/a0;->c:Lwa0/a0;

    invoke-virtual {v3, v2}, Ljava/lang/Object;->equals(Ljava/lang/Object;)Z

    move-result v2

    if-eqz v2, :cond_1

    sget-object v2, Loz/a3;->b:Loz/a3;

    goto :goto_0

    :cond_1
    sget-object v2, Lwa0/n0;->c:Lwa0/n0;

    invoke-virtual {v3, v2}, Ljava/lang/Object;->equals(Ljava/lang/Object;)Z

    move-result v2

    if-eqz v2, :cond_2

    sget-object v2, Loz/a3;->d:Loz/a3;

    goto :goto_0

    :cond_2
    sget-object v2, Lwa0/d;->c:Lwa0/d;

    invoke-virtual {v3, v2}, Ljava/lang/Object;->equals(Ljava/lang/Object;)Z

    move-result v2

    if-eqz v2, :cond_3

    sget-object v2, Loz/a3;->c:Loz/a3;

    :goto_0
    iget-boolean v3, p0, Lje0/c;->d:Z

    invoke-static {v3}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;

    move-result-object v3

    iget-object v4, p0, Lje0/c;->e:Loz/i6;

    invoke-direct {p1, v1, v2, v3, v4}, Ltz/i4;-><init>(Ljz/a;Loz/a3;Ljava/lang/Boolean;Loz/i6;)V

    invoke-interface {v0, p1}, Lg40/c;->s(Lorg/apache/avro/generic/GenericRecord;)Z

    return-void

    :cond_3
    new-instance p1, Ljava/lang/IllegalStateException;

    const-string v0, "Shouldn\'t be reached here"

    invoke-direct {p1, v0}, Ljava/lang/IllegalStateException;-><init>(Ljava/lang/String;)V

    throw p1
.end method

.method public final f()V
    .locals 0

    return-void
.end method

.method public final onDestroy(Landroidx/lifecycle/q0;)V
    .locals 0

    iget-object p1, p0, Lje0/c;->l:Lf50/d;

    iget-object p1, p1, Lf50/d;->d:Ljava/lang/Object;

    check-cast p1, Lcom/touchtype/ui/SwiftKeyTabLayout;

    iget-object p1, p1, Lcom/google/android/material/tabs/TabLayout;->L:Ljava/util/ArrayList;

    invoke-virtual {p1}, Ljava/util/ArrayList;->clear()V

    return-void
.end method

.method public final w()V
    .locals 0

    return-void
.end method

.method public final x()V
    .locals 0

    return-void
.end method

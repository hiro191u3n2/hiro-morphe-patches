.class public final Lob0/d1;
.super Landroidx/constraintlayout/widget/ConstraintLayout;

.method public constructor <init>(Landroid/view/ContextThemeWrapper;Lfb0/t1;Ldd0/i0;Lkd0/e0;Lfb0/d;)V
    .locals 7

    invoke-direct {p0, p1}, Landroidx/constraintlayout/widget/ConstraintLayout;-><init>(Landroid/content/Context;)V

    new-instance v0, Lob0/b1;

    new-instance v6, Landroidx/room/k0;

    const/16 v1, 0x11

    invoke-direct {v6, p0, v1}, Landroidx/room/k0;-><init>(Ljava/lang/Object;I)V

    move-object v1, p1

    move-object v2, p2

    move-object v3, p3

    move-object v4, p4

    move-object v5, p5

    invoke-direct/range {v0 .. v6}, Lob0/b1;-><init>(Landroid/view/ContextThemeWrapper;Lfb0/t1;Ldd0/i0;Lkd0/e0;Lfb0/d;Landroidx/room/k0;)V

    iput-object v0, p0, Lob0/d1;->a:Lob0/b1;

    const p1, 0x7f0a0537

    invoke-virtual {p0, p1}, Landroidx/constraintlayout/widget/ConstraintLayout;->setId(I)V

    invoke-virtual {v1}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;

    move-result-object p1

    const p2, 0x7f0704f3

    invoke-virtual {p1, p2}, Landroid/content/res/Resources;->getDimension(I)F

    move-result p1

    float-to-int p1, p1

    invoke-virtual {p0, p1, p1, p1, p1}, Landroid/view/View;->setPadding(IIII)V

    invoke-virtual {p0, v0}, Landroid/view/ViewGroup;->addView(Landroid/view/View;)V

    new-instance p1, Leg0/t;

    const/4 p2, 0x6

    invoke-direct {p1, p0, p2}, Leg0/t;-><init>(Ljava/lang/Object;I)V

    invoke-virtual {p0, p1}, Landroid/view/View;->setOnClickListener(Landroid/view/View$OnClickListener;)V

    return-void
.end method

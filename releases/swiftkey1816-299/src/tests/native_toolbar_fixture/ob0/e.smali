.class public final Lob0/e;
.super Landroidx/constraintlayout/widget/ConstraintLayout;

.method public constructor <init>(Landroid/view/ContextThemeWrapper;Lfb0/n0;Lfb0/i0;Lde0/g;Lcm0/d;Lcm0/a;Ldd0/i0;Lob0/d1;)V
    .locals 0

    invoke-direct {p0, p1}, Landroidx/constraintlayout/widget/ConstraintLayout;-><init>(Landroid/content/Context;)V

    iput-object p2, p0, Lob0/e;->a:Lfb0/n0;

    iput-object p3, p0, Lob0/e;->b:Lfb0/i0;

    iput-object p4, p0, Lob0/e;->c:Lde0/g;

    iput-object p6, p0, Lob0/e;->d:Lcm0/a;

    iput-object p7, p0, Lob0/e;->e:Ldd0/i0;

    invoke-static {p1}, Landroid/view/LayoutInflater;->from(Landroid/content/Context;)Landroid/view/LayoutInflater;

    move-result-object p1

    const p2, 0x7f0d0027

    invoke-virtual {p1, p2, p0}, Landroid/view/LayoutInflater;->inflate(ILandroid/view/ViewGroup;)Landroid/view/View;

    const p1, 0x7f0a01a8

    invoke-static {p0, p1}, La/a;->y(Landroid/view/View;I)Landroid/view/View;

    move-result-object p2

    check-cast p2, Landroid/widget/FrameLayout;

    if-eqz p2, :cond_0

    const p1, 0x7f0a0281

    invoke-static {p0, p1}, La/a;->y(Landroid/view/View;I)Landroid/view/View;

    move-result-object p3

    check-cast p3, Landroid/widget/FrameLayout;

    if-eqz p3, :cond_0

    const p1, 0x7f0a04b8

    invoke-static {p0, p1}, La/a;->y(Landroid/view/View;I)Landroid/view/View;

    move-result-object p4

    check-cast p4, Landroid/widget/FrameLayout;

    if-eqz p4, :cond_0

    const p1, 0x7f0a0500

    invoke-static {p0, p1}, La/a;->y(Landroid/view/View;I)Landroid/view/View;

    move-result-object p6

    check-cast p6, Landroid/widget/FrameLayout;

    if-eqz p6, :cond_0

    new-instance p1, Lfb0/t1;

    invoke-direct {p1, p2, p3, p4, p6}, Lfb0/t1;-><init>(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V

    invoke-interface {p5, p0}, Lcm0/d;->invoke(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object p2

    check-cast p2, Landroid/view/View;

    invoke-virtual {p6, p2}, Landroid/view/ViewGroup;->addView(Landroid/view/View;)V

    iput-object p1, p0, Lob0/e;->f:Lfb0/t1;

    invoke-virtual {p4, p8}, Landroid/view/ViewGroup;->addView(Landroid/view/View;)V

    invoke-virtual {p0}, Lob0/e;->n()V

    new-instance p1, Lob0/d;

    invoke-direct {p1, p0}, Lob0/d;-><init>(Lob0/e;)V

    iput-object p1, p0, Lob0/e;->g:Lob0/d;

    new-instance p1, Lob0/c;

    const/4 p2, 0x0

    invoke-direct {p1, p0, p2}, Lob0/c;-><init>(Ljava/lang/Object;I)V

    iput-object p1, p0, Lob0/e;->h:Lob0/c;

    new-instance p1, Lad0/m;

    const/4 p2, 0x1

    invoke-direct {p1, p0, p2}, Lad0/m;-><init>(Ljava/lang/Object;I)V

    iput-object p1, p0, Lob0/e;->i:Lad0/m;

    return-void

    :cond_0
    invoke-virtual {p0}, Landroid/view/View;->getResources()Landroid/content/res/Resources;

    move-result-object p2

    invoke-virtual {p2, p1}, Landroid/content/res/Resources;->getResourceName(I)Ljava/lang/String;

    move-result-object p1

    new-instance p2, Ljava/lang/NullPointerException;

    const-string p3, "Missing required view with ID: "

    invoke-virtual {p3, p1}, Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;

    move-result-object p1

    invoke-direct {p2, p1}, Ljava/lang/NullPointerException;-><init>(Ljava/lang/String;)V

    throw p2
.end method

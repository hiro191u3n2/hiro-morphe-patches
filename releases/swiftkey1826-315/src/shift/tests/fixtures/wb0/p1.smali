.class public final Lwb0/p1;
.super Ljava/lang/Object;
.source "SourceFile"

# interfaces
.implements Lwb0/q1;


# instance fields
.field public final a:Lj70/a;

.field public final b:Lwf/h;

.field public c:Lwb0/r1;

.field public d:Lwb0/r1;

.field public e:I

.field public f:Z

.field public g:I

.field public h:Z

.field public i:Z

.field public j:Z

.field public k:Z

.field public l:Z

.field public m:I

.field public final n:Lwb0/b2;

.field public o:Lwb0/r1;

.field public final p:Lwm0/x1;


# direct methods
.method public constructor <init>(Lj70/a;Lwf/h;Lwb0/b2;)V
    .locals 1

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    sget-object v0, Lwb0/j1;->a:Lwb0/j1;

    invoke-static {v0}, Lwm0/w;->c(Ljava/lang/Object;)Lwm0/x1;

    move-result-object v0

    iput-object v0, p0, Lwb0/p1;->p:Lwm0/x1;

    iput-object p1, p0, Lwb0/p1;->a:Lj70/a;

    iput-object p2, p0, Lwb0/p1;->b:Lwf/h;

    sget-object p1, Lwb0/r1;->a:Lwb0/r1;

    iput-object p1, p0, Lwb0/p1;->c:Lwb0/r1;

    iput-object p1, p0, Lwb0/p1;->d:Lwb0/r1;

    const/4 p1, 0x1

    iput p1, p0, Lwb0/p1;->e:I

    const/4 p2, 0x0

    iput-boolean p2, p0, Lwb0/p1;->f:Z

    iput p1, p0, Lwb0/p1;->g:I

    iput-boolean p2, p0, Lwb0/p1;->h:Z

    iput-boolean p2, p0, Lwb0/p1;->i:Z

    iput-boolean p2, p0, Lwb0/p1;->j:Z

    iput-boolean p2, p0, Lwb0/p1;->k:Z

    iput-boolean p2, p0, Lwb0/p1;->l:Z

    iput p2, p0, Lwb0/p1;->m:I

    iput-object p3, p0, Lwb0/p1;->n:Lwb0/b2;

    return-void
.end method


# virtual methods
.method public final a(Ljava/lang/String;)Lcom/microsoft/fluency/ResultsFilter$CapitalizationHint;
    .locals 5

    iget-boolean v0, p0, Lwb0/p1;->k:Z

    if-eqz v0, :cond_0

    sget-object p1, Lcom/microsoft/fluency/ResultsFilter$CapitalizationHint;->DEFAULT:Lcom/microsoft/fluency/ResultsFilter$CapitalizationHint;

    return-object p1

    :cond_0
    const/4 v0, 0x0

    if-eqz p1, :cond_1

    invoke-virtual {p1}, Ljava/lang/String;->length()I

    move-result v1

    if-nez v1, :cond_1

    iput v0, p0, Lwb0/p1;->m:I

    :cond_1
    invoke-virtual {p0}, Lwb0/p1;->u()Lwb0/r1;

    move-result-object v1

    sget-object v2, Lwb0/r1;->a:Lwb0/r1;

    const/4 v3, 0x1

    const/4 v4, 0x2

    if-ne v1, v2, :cond_4

    iget v1, p0, Lwb0/p1;->m:I

    rem-int/2addr v1, v4

    if-ne v1, v3, :cond_2

    sget-object p1, Lcom/microsoft/fluency/ResultsFilter$CapitalizationHint;->FORCE_LOWER_CASE:Lcom/microsoft/fluency/ResultsFilter$CapitalizationHint;

    return-object p1

    :cond_2
    invoke-static {p1}, Lk50/x;->m(Ljava/lang/CharSequence;)Z

    move-result v1

    if-nez v1, :cond_3

    invoke-virtual {p1, v0}, Ljava/lang/String;->charAt(I)C

    move-result p1

    invoke-static {p1}, Ljava/lang/Character;->isUpperCase(C)Z

    move-result p1

    if-eqz p1, :cond_3

    sget-object p1, Lcom/microsoft/fluency/ResultsFilter$CapitalizationHint;->INITIAL_UPPER_CASE:Lcom/microsoft/fluency/ResultsFilter$CapitalizationHint;

    return-object p1

    :cond_3
    sget-object p1, Lcom/microsoft/fluency/ResultsFilter$CapitalizationHint;->DEFAULT:Lcom/microsoft/fluency/ResultsFilter$CapitalizationHint;

    return-object p1

    :cond_4
    sget-object v2, Lwb0/r1;->c:Lwb0/r1;

    if-eq v1, v2, :cond_9

    invoke-virtual {p0}, Lwb0/p1;->e()I

    move-result v2

    if-ne v2, v4, :cond_5

    goto :goto_1

    :cond_5
    sget-object v2, Lwb0/r1;->b:Lwb0/r1;

    if-ne v1, v2, :cond_8

    iget-object v1, p0, Lwb0/p1;->a:Lj70/a;

    iget-object v1, v1, Lj70/a;->b:Ljava/lang/Object;

    check-cast v1, Landroid/text/SpannableStringBuilder;

    invoke-static {v1, v3}, Landroid/text/method/MetaKeyKeyListener;->getMetaState(Ljava/lang/CharSequence;I)I

    move-result v1

    if-ne v1, v3, :cond_7

    invoke-static {p1}, Lk50/x;->m(Ljava/lang/CharSequence;)Z

    move-result v1

    if-nez v1, :cond_6

    invoke-virtual {p1, v0}, Ljava/lang/String;->charAt(I)C

    move-result p1

    invoke-static {p1}, Ljava/lang/Character;->isUpperCase(C)Z

    move-result p1

    if-eqz p1, :cond_6

    goto :goto_0

    :cond_6
    sget-object p1, Lcom/microsoft/fluency/ResultsFilter$CapitalizationHint;->DEFAULT:Lcom/microsoft/fluency/ResultsFilter$CapitalizationHint;

    return-object p1

    :cond_7
    :goto_0
    sget-object p1, Lcom/microsoft/fluency/ResultsFilter$CapitalizationHint;->INITIAL_UPPER_CASE:Lcom/microsoft/fluency/ResultsFilter$CapitalizationHint;

    return-object p1

    :cond_8
    sget-object p1, Lcom/microsoft/fluency/ResultsFilter$CapitalizationHint;->DEFAULT:Lcom/microsoft/fluency/ResultsFilter$CapitalizationHint;

    return-object p1

    :cond_9
    :goto_1
    sget-object p1, Lcom/microsoft/fluency/ResultsFilter$CapitalizationHint;->UPPER_CASE:Lcom/microsoft/fluency/ResultsFilter$CapitalizationHint;

    return-object p1
.end method

.method public final b()Lwm0/x1;
    .locals 1

    iget-object v0, p0, Lwb0/p1;->p:Lwm0/x1;

    return-object v0
.end method

.method public final c(IZ)Lwb0/r1;
    .locals 2

    iget-boolean v0, p0, Lwb0/p1;->h:Z

    if-eqz v0, :cond_0

    goto :goto_0

    :cond_0
    invoke-virtual {p0}, Lwb0/p1;->e()I

    move-result v0

    invoke-static {v0}, Ld0/y;->d(I)I

    move-result v0

    if-eqz v0, :cond_3

    const/4 v1, 0x1

    if-eq v0, v1, :cond_2

    if-lez p1, :cond_1

    if-nez p2, :cond_1

    goto :goto_1

    :cond_1
    sget-object p1, Lwb0/r1;->b:Lwb0/r1;

    return-object p1

    :cond_2
    :goto_0
    sget-object p1, Lwb0/r1;->c:Lwb0/r1;

    return-object p1

    :cond_3
    :goto_1
    sget-object p1, Lwb0/r1;->a:Lwb0/r1;

    return-object p1
.end method

.method public final d(Llh0/a;)V
    .locals 2

    sget-object p1, Lwb0/j1;->c:Lwb0/j1;

    iget-object v0, p0, Lwb0/p1;->p:Lwm0/x1;

    invoke-virtual {v0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    const/4 v1, 0x0

    invoke-virtual {v0, v1, p1}, Lwm0/x1;->j(Ljava/lang/Object;Ljava/lang/Object;)Z

    return-void
.end method

.method public final e()I
    .locals 1

    iget-boolean v0, p0, Lwb0/p1;->l:Z

    if-eqz v0, :cond_0

    const/4 v0, 0x1

    return v0

    :cond_0
    iget v0, p0, Lwb0/p1;->e:I

    return v0
.end method

.method public final f(Llh0/a;Lwb0/r1;)V
    .locals 2

    iget-object v0, p0, Lwb0/p1;->c:Lwb0/r1;

    iput-object v0, p0, Lwb0/p1;->d:Lwb0/r1;

    iget-boolean v1, p0, Lwb0/p1;->h:Z

    if-eqz v1, :cond_0

    sget-object p2, Lwb0/r1;->c:Lwb0/r1;

    :cond_0
    iput-object p2, p0, Lwb0/p1;->c:Lwb0/r1;

    iget-boolean v1, p0, Lwb0/p1;->k:Z

    if-nez v1, :cond_1

    if-eq v0, p2, :cond_1

    iget-object v0, p0, Lwb0/p1;->n:Lwb0/b2;

    invoke-interface {v0, p2}, Lwb0/b2;->j(Lwb0/r1;)V

    iget-object p2, p0, Lwb0/p1;->c:Lwb0/r1;

    iget-object v0, p0, Lwb0/p1;->b:Lwf/h;

    invoke-virtual {v0, p1, p2}, Lwf/h;->l0(Llh0/a;Lwb0/r1;)V

    iget-boolean p1, p0, Lwb0/p1;->j:Z

    if-nez p1, :cond_1

    invoke-virtual {v0, p2}, Lwf/h;->n0(Lwb0/r1;)V

    :cond_1
    return-void
.end method

.method public final g(Llh0/a;)V
    .locals 3

    iget-object v0, p0, Lwb0/p1;->c:Lwb0/r1;

    invoke-virtual {v0}, Ljava/lang/Enum;->ordinal()I

    move-result v0

    if-eqz v0, :cond_2

    const/4 v1, 0x1

    const/4 v2, 0x2

    if-eq v0, v1, :cond_1

    if-eq v0, v2, :cond_0

    return-void

    :cond_0
    iput v2, p0, Lwb0/p1;->g:I

    return-void

    :cond_1
    iput v2, p0, Lwb0/p1;->g:I

    sget-object v0, Lwb0/r1;->c:Lwb0/r1;

    invoke-virtual {p0, p1, v0}, Lwb0/p1;->f(Llh0/a;Lwb0/r1;)V

    return-void

    :cond_2
    const/4 p1, 0x3

    iput p1, p0, Lwb0/p1;->g:I

    return-void
.end method

.method public final h(Llh0/a;)V
    .locals 5

    const/4 v0, 0x0

    iput-boolean v0, p0, Lwb0/p1;->f:Z

    iget-object v0, p0, Lwb0/p1;->c:Lwb0/r1;

    invoke-virtual {v0}, Ljava/lang/Enum;->ordinal()I

    move-result v0

    const/4 v1, 0x2

    sget-object v2, Lwb0/r1;->a:Lwb0/r1;

    sget-object v3, Lwb0/r1;->c:Lwb0/r1;

    if-eqz v0, :cond_3

    const/4 v4, 0x1

    if-eq v0, v4, :cond_2

    if-eq v0, v1, :cond_0

    goto :goto_0

    :cond_0
    invoke-virtual {p0}, Lwb0/p1;->e()I

    move-result v0

    if-ne v0, v1, :cond_1

    iput-object v3, p0, Lwb0/p1;->o:Lwb0/r1;

    goto :goto_0

    :cond_1
    iput-object v2, p0, Lwb0/p1;->o:Lwb0/r1;

    goto :goto_0

    :cond_2
    iput-object v2, p0, Lwb0/p1;->o:Lwb0/r1;

    goto :goto_0

    :cond_3
    iput-object v3, p0, Lwb0/p1;->o:Lwb0/r1;

    :goto_0
    iget-object v0, p0, Lwb0/p1;->c:Lwb0/r1;

    if-ne v0, v3, :cond_4

    invoke-virtual {p0}, Lwb0/p1;->e()I

    move-result v0

    if-eq v0, v1, :cond_4

    invoke-virtual {p0, p1, v2}, Lwb0/p1;->f(Llh0/a;Lwb0/r1;)V

    :cond_4
    sget-object p1, Lwb0/j1;->b:Lwb0/j1;

    iget-object v0, p0, Lwb0/p1;->p:Lwm0/x1;

    invoke-virtual {v0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    const/4 v1, 0x0

    invoke-virtual {v0, v1, p1}, Lwm0/x1;->j(Ljava/lang/Object;Ljava/lang/Object;)Z

    return-void
.end method

.method public final k(Llh0/a;ILandroid/view/KeyEvent;)V
    .locals 6

    iget-boolean v0, p0, Lwb0/p1;->h:Z

    invoke-virtual {p3}, Landroid/view/KeyEvent;->getKeyCode()I

    move-result v1

    const/16 v2, 0x52

    const/4 v3, 0x1

    if-ne v1, v2, :cond_1

    invoke-virtual {p3}, Landroid/view/KeyEvent;->getAction()I

    move-result v1

    if-nez v1, :cond_0

    move v1, v3

    goto :goto_0

    :cond_0
    const/4 v1, 0x0

    :goto_0
    iput-boolean v1, p0, Lwb0/p1;->i:Z

    :cond_1
    invoke-virtual {p3}, Landroid/view/KeyEvent;->getKeyCode()I

    move-result v1

    const/16 v2, 0x73

    if-ne v1, v2, :cond_2

    iget-boolean v1, p0, Lwb0/p1;->h:Z

    xor-int/2addr v1, v3

    iput-boolean v1, p0, Lwb0/p1;->h:Z

    goto :goto_1

    :cond_2
    invoke-virtual {p3}, Landroid/view/KeyEvent;->isCapsLockOn()Z

    move-result v1

    iput-boolean v1, p0, Lwb0/p1;->h:Z

    :goto_1
    invoke-virtual {p0}, Lwb0/p1;->u()Lwb0/r1;

    move-result-object v1

    iget-boolean v2, p0, Lwb0/p1;->h:Z

    sget-object v4, Lwb0/r1;->c:Lwb0/r1;

    if-eqz v2, :cond_3

    :goto_2
    move-object v1, v4

    goto :goto_4

    :cond_3
    invoke-static {p2}, Landroid/view/KeyEvent;->isModifierKey(I)Z

    move-result v2

    if-nez v2, :cond_4

    iget-boolean v2, p0, Lwb0/p1;->h:Z

    if-eq v0, v2, :cond_7

    :cond_4
    iget-object v0, p0, Lwb0/p1;->a:Lj70/a;

    iget-object v1, v0, Lj70/a;->a:Ljava/lang/Object;

    check-cast v1, Lbd0/m;

    iget-object v2, v0, Lj70/a;->b:Ljava/lang/Object;

    check-cast v2, Landroid/text/SpannableStringBuilder;

    const/4 v5, 0x0

    invoke-virtual {v1, v5, v2, p2, p3}, Landroid/text/method/MetaKeyKeyListener;->onKeyDown(Landroid/view/View;Landroid/text/Editable;ILandroid/view/KeyEvent;)Z

    iget-object p2, v0, Lj70/a;->b:Ljava/lang/Object;

    check-cast p2, Landroid/text/SpannableStringBuilder;

    invoke-static {p2, v3}, Landroid/text/method/MetaKeyKeyListener;->getMetaState(Ljava/lang/CharSequence;I)I

    move-result p2

    if-eqz p2, :cond_6

    if-eq p2, v3, :cond_5

    goto :goto_2

    :cond_5
    sget-object p2, Lwb0/r1;->b:Lwb0/r1;

    :goto_3
    move-object v1, p2

    goto :goto_4

    :cond_6
    sget-object p2, Lwb0/r1;->a:Lwb0/r1;

    goto :goto_3

    :cond_7
    :goto_4
    invoke-virtual {p0}, Lwb0/p1;->u()Lwb0/r1;

    move-result-object p2

    if-eq v1, p2, :cond_8

    invoke-virtual {p0, p1, v1}, Lwb0/p1;->f(Llh0/a;Lwb0/r1;)V

    :cond_8
    return-void
.end method

.method public final l(Llh0/a;ILandroid/view/KeyEvent;Lwb0/o0;)V
    .locals 6

    invoke-virtual {p3}, Landroid/view/KeyEvent;->getKeyCode()I

    move-result v0

    const/16 v1, 0x52

    const/4 v2, 0x1

    const/4 v3, 0x0

    if-ne v0, v1, :cond_1

    invoke-virtual {p3}, Landroid/view/KeyEvent;->getAction()I

    move-result v0

    if-nez v0, :cond_0

    move v0, v2

    goto :goto_0

    :cond_0
    move v0, v3

    :goto_0
    iput-boolean v0, p0, Lwb0/p1;->i:Z

    :cond_1
    invoke-virtual {p0}, Lwb0/p1;->u()Lwb0/r1;

    move-result-object v0

    invoke-static {p2}, Landroid/view/KeyEvent;->isModifierKey(I)Z

    move-result v1

    iget-object v4, p0, Lwb0/p1;->a:Lj70/a;

    if-eqz v1, :cond_4

    iget-object v0, v4, Lj70/a;->a:Ljava/lang/Object;

    check-cast v0, Lbd0/m;

    iget-object v1, v4, Lj70/a;->b:Ljava/lang/Object;

    check-cast v1, Landroid/text/SpannableStringBuilder;

    const/4 v5, 0x0

    invoke-virtual {v0, v5, v1, p2, p3}, Landroid/text/method/MetaKeyKeyListener;->onKeyUp(Landroid/view/View;Landroid/text/Editable;ILandroid/view/KeyEvent;)Z

    iget-object p2, v4, Lj70/a;->b:Ljava/lang/Object;

    check-cast p2, Landroid/text/SpannableStringBuilder;

    invoke-static {p2, v2}, Landroid/text/method/MetaKeyKeyListener;->getMetaState(Ljava/lang/CharSequence;I)I

    move-result p2

    if-eqz p2, :cond_3

    if-eq p2, v2, :cond_2

    sget-object p2, Lwb0/r1;->c:Lwb0/r1;

    :goto_1
    move-object v0, p2

    goto :goto_2

    :cond_2
    sget-object p2, Lwb0/r1;->b:Lwb0/r1;

    goto :goto_1

    :cond_3
    sget-object p2, Lwb0/r1;->a:Lwb0/r1;

    goto :goto_1

    :cond_4
    iget-object p2, v4, Lj70/a;->b:Ljava/lang/Object;

    check-cast p2, Landroid/text/SpannableStringBuilder;

    invoke-static {p2}, Landroid/text/method/MetaKeyKeyListener;->adjustMetaAfterKeypress(Landroid/text/Spannable;)V

    :goto_2
    invoke-virtual {p0}, Lwb0/p1;->u()Lwb0/r1;

    move-result-object p2

    if-eq v0, p2, :cond_5

    invoke-virtual {p0, p1, v0}, Lwb0/p1;->f(Llh0/a;Lwb0/r1;)V

    :cond_5
    iget-object p1, v4, Lj70/a;->b:Ljava/lang/Object;

    check-cast p1, Landroid/text/SpannableStringBuilder;

    iget-object p2, v4, Lj70/a;->b:Ljava/lang/Object;

    check-cast p2, Landroid/text/SpannableStringBuilder;

    invoke-static {p1, v2}, Landroid/text/method/MetaKeyKeyListener;->getMetaState(Ljava/lang/CharSequence;I)I

    move-result p1

    if-nez p1, :cond_6

    goto :goto_3

    :cond_6
    move v2, v3

    :goto_3
    const/4 p1, 0x2

    invoke-static {p2, p1}, Landroid/text/method/MetaKeyKeyListener;->getMetaState(Ljava/lang/CharSequence;I)I

    move-result p1

    if-nez p1, :cond_7

    or-int/lit8 v2, v2, 0x2

    :cond_7
    const/4 p1, 0x4

    invoke-static {p2, p1}, Landroid/text/method/MetaKeyKeyListener;->getMetaState(Ljava/lang/CharSequence;I)I

    move-result p1

    if-nez p1, :cond_8

    or-int/lit8 v2, v2, 0x4

    :cond_8
    invoke-interface {p4, v2}, Lwb0/o0;->clearMetaKeyStates(I)Z

    return-void
.end method

.method public final n(Llh0/a;)V
    .locals 5

    iget-boolean v0, p0, Lwb0/p1;->h:Z

    if-eqz v0, :cond_0

    const/4 v0, 0x0

    iput-boolean v0, p0, Lwb0/p1;->h:Z

    :cond_0
    iget-object v0, p0, Lwb0/p1;->c:Lwb0/r1;

    const/4 v1, 0x1

    iput-boolean v1, p0, Lwb0/p1;->f:Z

    sget-object v2, Lwb0/j1;->a:Lwb0/j1;

    iget-object v3, p0, Lwb0/p1;->p:Lwm0/x1;

    invoke-virtual {v3}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    const/4 v4, 0x0

    invoke-virtual {v3, v4, v2}, Lwm0/x1;->j(Ljava/lang/Object;Ljava/lang/Object;)Z

    invoke-virtual {v0}, Ljava/lang/Enum;->ordinal()I

    move-result v0

    const/4 v2, 0x2

    sget-object v3, Lwb0/r1;->c:Lwb0/r1;

    if-eqz v0, :cond_3

    if-eq v0, v1, :cond_2

    if-eq v0, v2, :cond_1

    return-void

    :cond_1
    sget-object v0, Lwb0/r1;->a:Lwb0/r1;

    invoke-virtual {p0, p1, v0}, Lwb0/p1;->f(Llh0/a;Lwb0/r1;)V

    return-void

    :cond_2
    invoke-virtual {p0, p1, v3}, Lwb0/p1;->f(Llh0/a;Lwb0/r1;)V

    return-void

    :cond_3
    iget v0, p0, Lwb0/p1;->m:I

    add-int/2addr v0, v1

    iput v0, p0, Lwb0/p1;->m:I

    invoke-virtual {p0}, Lwb0/p1;->e()I

    move-result v0

    if-ne v0, v2, :cond_4

    invoke-virtual {p0, p1, v3}, Lwb0/p1;->f(Llh0/a;Lwb0/r1;)V

    return-void

    :cond_4
    sget-object v0, Lwb0/r1;->b:Lwb0/r1;

    invoke-virtual {p0, p1, v0}, Lwb0/p1;->f(Llh0/a;Lwb0/r1;)V

    return-void
.end method

.method public final p(Llh0/a;)V
    .locals 2

    iget-boolean v0, p0, Lwb0/p1;->f:Z

    if-eqz v0, :cond_0

    iget v0, p0, Lwb0/p1;->g:I

    const/4 v1, 0x1

    if-ne v0, v1, :cond_0

    invoke-virtual {p0, p1}, Lwb0/p1;->g(Llh0/a;)V

    :cond_0
    return-void
.end method

.method public final q(Landroid/view/inputmethod/EditorInfo;Llh0/a;Z)V
    .locals 5

    const/4 v0, 0x0

    iput-boolean v0, p0, Lwb0/p1;->j:Z

    invoke-static {p1}, Lwb0/r;->e(Landroid/view/inputmethod/EditorInfo;)Z

    move-result v1

    const/4 v2, 0x1

    if-eqz v1, :cond_7

    iget v1, p1, Landroid/view/inputmethod/EditorInfo;->inputType:I

    and-int/lit8 v3, v1, 0xf

    and-int/lit16 v4, v1, 0xff0

    if-eqz p3, :cond_5

    if-ne v3, v2, :cond_5

    and-int/lit16 p3, v1, 0x1000

    if-eqz p3, :cond_0

    const/4 p3, 0x2

    goto :goto_2

    :cond_0
    const/16 p3, 0x80

    if-eq v4, p3, :cond_5

    const/16 p3, 0xe0

    if-ne v4, p3, :cond_1

    goto :goto_1

    :cond_1
    and-int/lit16 p3, v1, 0x2000

    if-nez p3, :cond_4

    const/16 p3, 0x60

    if-eq v4, p3, :cond_4

    const/16 p3, 0x70

    if-ne v4, p3, :cond_2

    goto :goto_0

    :cond_2
    and-int/lit16 p3, v1, 0x4000

    if-nez p3, :cond_3

    const/16 p3, 0x40

    if-eq v4, p3, :cond_3

    const/16 p3, 0x50

    if-eq v4, p3, :cond_3

    const/16 p3, 0x30

    if-eq v4, p3, :cond_3

    const/16 p3, 0xa0

    if-eq v4, p3, :cond_3

    const/high16 p3, 0x20000

    and-int/2addr p3, v1

    if-nez p3, :cond_3

    const/high16 p3, 0x40000

    and-int/2addr p3, v1

    if-eqz p3, :cond_5

    :cond_3
    const/4 p3, 0x4

    goto :goto_2

    :cond_4
    :goto_0
    const/4 p3, 0x3

    goto :goto_2

    :cond_5
    :goto_1
    move p3, v2

    :goto_2
    iput p3, p0, Lwb0/p1;->e:I

    iget p3, p1, Landroid/view/inputmethod/EditorInfo;->initialSelStart:I

    iget p1, p1, Landroid/view/inputmethod/EditorInfo;->initialCapsMode:I

    if-eqz p1, :cond_6

    move v0, v2

    :cond_6
    invoke-virtual {p0, p3, v0}, Lwb0/p1;->c(IZ)Lwb0/r1;

    move-result-object p1

    invoke-virtual {p0, p2, p1}, Lwb0/p1;->f(Llh0/a;Lwb0/r1;)V

    return-void

    :cond_7
    iput v2, p0, Lwb0/p1;->e:I

    sget-object p1, Lwb0/r1;->a:Lwb0/r1;

    invoke-virtual {p0, p2, p1}, Lwb0/p1;->f(Llh0/a;Lwb0/r1;)V

    return-void
.end method

.method public final u()Lwb0/r1;
    .locals 1

    iget-boolean v0, p0, Lwb0/p1;->k:Z

    if-eqz v0, :cond_0

    sget-object v0, Lwb0/r1;->a:Lwb0/r1;

    return-object v0

    :cond_0
    iget-object v0, p0, Lwb0/p1;->c:Lwb0/r1;

    return-object v0
.end method

.class public Ltj0/e;
.super Ljava/lang/Object;
.source "SourceFile"

# interfaces
.implements Lir/k0;
.implements Ljn0/m1;
.implements Lrc0/e;
.implements Lag/a;
.implements Ls40/a;
.implements Lt90/h;
.implements Lah0/s;


# instance fields
.field public final synthetic a:I

.field public b:Ljava/lang/Object;

.field public c:Ljava/lang/Object;




.method public b0(Lec0/n;Llh0/a;)V
    .locals 6

    iget-object v0, p0, Ltj0/e;->b:Ljava/lang/Object;

    check-cast v0, Lwb0/p1;

    const-string v1, "breadcrumb"

    invoke-static {p2, v1}, Lkotlin/jvm/internal/m;->f(Ljava/lang/Object;Ljava/lang/String;)V

    iget-object v1, p0, Ltj0/e;->c:Ljava/lang/Object;

    check-cast v1, Lzb0/e;

    iget-object v1, v1, Lzb0/e;->b:Ljava/lang/Object;

    check-cast v1, Lwm0/i1;

    iget-object v1, v1, Lwm0/i1;->a:Lwm0/v1;

    invoke-interface {v1}, Lwm0/v1;->getValue()Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Lzc0/k;

    sget-object v2, Lzc0/j;->a:Lzc0/j;

    invoke-static {v1, v2}, Lkotlin/jvm/internal/m;->b(Ljava/lang/Object;Ljava/lang/Object;)Z

    move-result v1

    if-nez v1, :cond_a

    iget-boolean v1, v0, Lwb0/p1;->h:Z

    sget-object v2, Lwb0/r1;->c:Lwb0/r1;

    if-eqz v1, :cond_0

    goto/16 :goto_1

    :cond_0
    invoke-virtual {v0}, Lwb0/p1;->u()Lwb0/r1;

    move-result-object v1

    if-ne v1, v2, :cond_1

    goto :goto_1

    :cond_1
    invoke-virtual {v0}, Lwb0/p1;->e()I

    move-result v1

    const/4 v3, 0x1

    sget-object v4, Lwb0/r1;->a:Lwb0/r1;

    if-ne v1, v3, :cond_3

    :cond_2
    move-object v2, v4

    goto :goto_1

    :cond_3
    invoke-virtual {v0}, Lwb0/p1;->e()I

    move-result v1

    const/4 v5, 0x2

    if-ne v1, v5, :cond_4

    goto :goto_1

    :cond_4
    invoke-interface {p1}, Ldc0/a;->h()I

    move-result v1

    if-nez v1, :cond_5

    const/4 p1, 0x0

    invoke-virtual {v0, p1, p1}, Lwb0/p1;->c(IZ)Lwb0/r1;

    move-result-object v2

    goto :goto_1

    :cond_5
    invoke-virtual {v0}, Lwb0/p1;->e()I

    move-result v1

    invoke-static {v1}, Ld0/y;->d(I)I

    move-result v2

    if-eqz v2, :cond_2

    if-eq v2, v3, :cond_9

    invoke-interface {p1}, Ldc0/a;->y()I

    move-result v2

    invoke-interface {p1}, Ldc0/a;->h()I

    move-result p1

    if-eqz p1, :cond_9

    const/16 v3, 0xa

    if-ne p1, v3, :cond_6

    goto :goto_0

    :cond_6
    invoke-static {v2}, Lu50/c;->g(I)Z

    move-result v3

    const/4 v5, -0x1

    if-nez v3, :cond_7

    const-string v3, "\u00bf\u00a1"

    invoke-virtual {v3, v2}, Ljava/lang/String;->indexOf(I)I

    move-result v3

    if-ne v3, v5, :cond_7

    invoke-static {v2}, Lp40/d;->a(I)Z

    move-result v2

    if-eqz v2, :cond_2

    :cond_7
    const-string v2, ".\u3002!?\n\t"

    invoke-virtual {v2, p1}, Ljava/lang/String;->indexOf(I)I

    move-result p1

    if-eq p1, v5, :cond_8

    goto :goto_0

    :cond_8
    const/4 p1, 0x3

    if-ne v1, p1, :cond_2

    :cond_9
    :goto_0
    sget-object v2, Lwb0/r1;->b:Lwb0/r1;

    :goto_1
    invoke-virtual {v0}, Lwb0/p1;->u()Lwb0/r1;

    move-result-object p1

    if-eq v2, p1, :cond_a

    invoke-virtual {v0, p2, v2}, Lwb0/p1;->f(Llh0/a;Lwb0/r1;)V

    :cond_a
    return-void
.end method

.method public c0(Lec0/n;Llh0/a;)V
    .locals 1

    const-string v0, "bc"

    invoke-static {p2, v0}, Lkotlin/jvm/internal/m;->f(Ljava/lang/Object;Ljava/lang/String;)V

    iget-object v0, p0, Ltj0/e;->b:Ljava/lang/Object;

    check-cast v0, Lwb0/p1;

    iget-boolean v0, v0, Lwb0/p1;->f:Z

    if-nez v0, :cond_0

    invoke-virtual {p0, p1, p2}, Ltj0/e;->b0(Lec0/n;Llh0/a;)V

    :cond_0
    return-void
.end method

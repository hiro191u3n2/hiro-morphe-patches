.class public final Lwb0/x;
.super Ljava/lang/Object;
.source "SourceFile"

# interfaces
.implements Lwb0/t0;
.implements Lcc0/c;
.implements Lwb0/a1;
.implements Lec0/k;
.implements Lzc0/a;
.implements Lwb0/o1;
.implements Lwb0/q1;


.method public final P(Llh0/a;Ljava/lang/String;Ljava/lang/String;Lec0/m;ZLjava/lang/Integer;)V
    .locals 10

    const-string v0, "breadcrumb"

    invoke-static {p1, v0}, Lkotlin/jvm/internal/m;->f(Ljava/lang/Object;Ljava/lang/String;)V

    const-string v0, "textForInput"

    invoke-static {p2, v0}, Lkotlin/jvm/internal/m;->f(Ljava/lang/Object;Ljava/lang/String;)V

    const-string v0, "textForPunctuation"

    invoke-static {p3, v0}, Lkotlin/jvm/internal/m;->f(Ljava/lang/Object;Ljava/lang/String;)V

    :try_start_0
    iget-object v0, p0, Lwb0/x;->s:Lnb0/m;

    invoke-virtual {v0}, Lnb0/m;->b()V

    iget-object v0, p0, Lwb0/x;->d:Lwf/h;

    invoke-virtual {v0}, Lwf/h;->j0()V

    iget-object v0, p0, Lwb0/x;->n:Lwb0/p1;

    invoke-virtual {v0, p1}, Lwb0/p1;->p(Llh0/a;)V

    new-instance v1, Lyb0/o0;

    const/4 v7, 0x1

    const/4 v8, 0x1

    move-object v2, p1

    move-object v3, p2

    move-object v4, p3

    move-object v5, p4

    move v6, p5

    move-object/from16 v9, p6

    invoke-direct/range {v1 .. v9}, Lyb0/o0;-><init>(Llh0/a;Ljava/lang/String;Ljava/lang/String;Lec0/m;ZZZLjava/lang/Integer;)V

    invoke-virtual {p0, v1}, Lwb0/x;->i0(Lyb0/a;)V
    :try_end_0
    .catch Lwb0/u0; {:try_start_0 .. :try_end_0} :catch_0

    return-void

    :catch_0
    move-exception v0

    move-object p1, v0

    const-string p2, "onKey"

    invoke-virtual {p0, p2, p1}, Lwb0/x;->p0(Ljava/lang/String;Lwb0/u0;)V

    return-void
.end method

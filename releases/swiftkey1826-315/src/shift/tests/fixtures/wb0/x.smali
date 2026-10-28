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


# instance fields
.field public final A:Lub0/a;

.field public final B:Lho/h;

.field public final C:Lai0/b;

.field public final D:Landroidx/lifecycle/l0;

.field public final E:Luf/j;

.field public final F:Ljava/lang/String;

.field public final G:Ljava/util/LinkedHashSet;

.field public final H:Ljava/util/LinkedHashSet;

.field public final I:Ljava/util/LinkedHashSet;

.field public J:Z

.field public final K:Ljava/util/HashSet;

.field public L:Lwb0/x1;

.field public M:Z

.field public final a:Lwb0/q;

.field public final b:Lc5/m;

.field public final c:Lwb0/n0;

.field public final d:Lwf/h;

.field public final e:Lwb0/z0;

.field public final f:Lec0/x;

.field public final g:Lwb0/h1;

.field public final h:Lcom/touchtype/report/TouchTypeStats;

.field public final i:Lcom/microsoft/tokenshare/m;

.field public final j:Lwb0/t1;

.field public final k:Lln0/y;

.field public final l:Lq40/b;

.field public final m:Lzb0/i;

.field public final n:Lwb0/p1;

.field public final o:La3/r;

.field public final p:La50/a;

.field public final q:Lfb0/b0;

.field public final r:Lm40/d0;

.field public final s:Lnb0/m;

.field public final t:Lfb0/b0;

.field public final u:Lbd0/s;

.field public final v:Lrc0/g;

.field public final w:Lft/c;

.field public final x:Lb90/d;

.field public final y:Lal0/c;

.field public final z:Lda0/d;




.method public final A(Llh0/a;)V
    .locals 4

    const-string v0, "breadcrumb"

    invoke-static {p1, v0}, Lkotlin/jvm/internal/m;->f(Ljava/lang/Object;Ljava/lang/String;)V

    const/4 v0, 0x0

    iget-object v1, p0, Lwb0/x;->n:Lwb0/p1;

    iput-boolean v0, v1, Lwb0/p1;->f:Z

    iget-object v0, v1, Lwb0/p1;->p:Lwm0/x1;

    sget-object v2, Lwb0/j1;->a:Lwb0/j1;

    invoke-virtual {v0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    const/4 v3, 0x0

    invoke-virtual {v0, v3, v2}, Lwm0/x1;->j(Ljava/lang/Object;Ljava/lang/Object;)Z

    iget v0, v1, Lwb0/p1;->g:I

    const/4 v2, 0x1

    if-ne v0, v2, :cond_0

    return-void

    :cond_0
    const/4 v3, 0x2

    if-ne v0, v3, :cond_1

    sget-object v0, Lwb0/r1;->a:Lwb0/r1;

    invoke-virtual {v1, p1, v0}, Lwb0/p1;->f(Llh0/a;Lwb0/r1;)V

    goto :goto_0

    :cond_1
    sget-object v0, Lwb0/r1;->c:Lwb0/r1;

    invoke-virtual {v1, p1, v0}, Lwb0/p1;->f(Llh0/a;Lwb0/r1;)V

    :goto_0
    iput v2, v1, Lwb0/p1;->g:I

    return-void
.end method

.method public final O(Llh0/a;)V
    .locals 4

    const-string v0, "breadcrumb"

    invoke-static {p1, v0}, Lkotlin/jvm/internal/m;->f(Ljava/lang/Object;Ljava/lang/String;)V

    const/4 v0, 0x0

    iget-object v1, p0, Lwb0/x;->n:Lwb0/p1;

    iput-boolean v0, v1, Lwb0/p1;->f:Z

    iget v0, v1, Lwb0/p1;->g:I

    const/4 v2, 0x1

    if-ne v0, v2, :cond_0

    iget-object v0, v1, Lwb0/p1;->d:Lwb0/r1;

    invoke-virtual {v1, p1, v0}, Lwb0/p1;->f(Llh0/a;Lwb0/r1;)V

    return-void

    :cond_0
    const/4 v3, 0x2

    if-ne v0, v3, :cond_1

    sget-object v0, Lwb0/r1;->a:Lwb0/r1;

    invoke-virtual {v1, p1, v0}, Lwb0/p1;->f(Llh0/a;Lwb0/r1;)V

    goto :goto_0

    :cond_1
    sget-object v0, Lwb0/r1;->c:Lwb0/r1;

    invoke-virtual {v1, p1, v0}, Lwb0/p1;->f(Llh0/a;Lwb0/r1;)V

    :goto_0
    iput v2, v1, Lwb0/p1;->g:I

    return-void
.end method

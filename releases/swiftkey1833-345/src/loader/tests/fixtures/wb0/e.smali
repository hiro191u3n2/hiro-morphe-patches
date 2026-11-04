.class public final Lwb0/e;
.super Ljava/lang/Object;
.source "SourceFile"

.method public final d(Lm40/b;Lwb0/j;ILec0/n;Z)Z
    .locals 6

    sget-object p3, Lwb0/j;->A:Lwb0/j;

    if-ne p2, p3, :cond_0

    const/4 p1, 0x1

    return p1

    :cond_0
    invoke-interface {p1}, Lm40/b;->getUserFacingText()Ljava/lang/String;

    move-result-object v1

    invoke-static {p1}, Lm40/r;->c(Lm40/b;)Ljava/lang/String;

    move-result-object v3

    invoke-interface {p1}, Lm40/b;->getTrailingSeparator()Ljava/lang/String;

    move-result-object v4

    move-object v0, p0

    move-object v2, p4

    move v5, p5

    invoke-virtual/range {v0 .. v5}, Lwb0/e;->e(Ljava/lang/String;Lec0/n;Ljava/lang/String;Ljava/lang/String;Z)Z

    move-result p1

    return p1
.end method

.class public final Lec0/x;
.super Ljava/lang/Object;
.source "SourceFile"

.method public static A(Lec0/x;Lm40/b;Lwb0/j;Ljava/lang/String;Lec0/y;I)V
    .locals 0

    invoke-virtual {p0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    invoke-virtual {p4, p1, p2, p5}, Lec0/y;->i(Lm40/b;Lwb0/j;I)V

    iget-object p0, p0, Lec0/x;->a:Lec0/f;

    invoke-interface {p1}, Lm40/b;->getTokens()Ljava/util/List;

    move-result-object p2

    invoke-interface {p1}, Lm40/b;->getTrailingSeparator()Ljava/lang/String;

    move-result-object p1

    invoke-virtual {p0, p3, p4, p2, p1}, Lec0/f;->l0(Ljava/lang/String;Lec0/y;Ljava/util/List;Ljava/lang/String;)V

    return-void
.end method

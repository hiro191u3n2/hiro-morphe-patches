.method public final a(Lef0/p;)V
    .locals 1

    iget-object p1, p1, Lef0/p;->b:Ljava/lang/Object;

    check-cast p1, Lkj0/f;

    iget-object p1, p1, Lkj0/f;->b:Ljava/lang/Object;

    check-cast p1, Llh0/a;

    const-string v0, "getBreadcrumb(...)"

    invoke-static {p1, v0}, Lkotlin/jvm/internal/m;->e(Ljava/lang/Object;Ljava/lang/String;)V

    iget-object v0, p0, Lic0/e;->a:Lic0/a;

    invoke-interface {v0, p1}, Lic0/a;->b(Llh0/a;)V

    return-void
.end method

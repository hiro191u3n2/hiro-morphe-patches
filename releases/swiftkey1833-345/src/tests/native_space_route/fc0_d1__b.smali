.method public final b(Llh0/a;)V
    .locals 8

    const-string v0, "breadcrumb"

    invoke-static {p1, v0}, Lkotlin/jvm/internal/m;->f(Ljava/lang/Object;Ljava/lang/String;)V

    iget-object v0, p0, Lfc0/d1;->a:Lfc0/k1;

    iget-object v1, v0, Lfc0/k1;->d:Ljava/lang/Object;

    const/4 v7, 0x0

    const/4 v5, 0x0

    iget-object v3, p0, Lfc0/d1;->b:Ljava/lang/String;

    iget-object v4, p0, Lfc0/d1;->c:Ljava/lang/String;

    iget-boolean v6, p0, Lfc0/d1;->d:Z

    move-object v2, p1

    invoke-interface/range {v1 .. v7}, Lwb0/t0;->P(Llh0/a;Ljava/lang/String;Ljava/lang/String;Lec0/m;ZLjava/lang/Integer;)V

    return-void
.end method

.method public final b(Llh0/a;)V
    .locals 2

    iget v0, p0, Lfc0/q0;->a:I

    packed-switch v0, :pswitch_data_0

    const-string v0, "it"

    invoke-static {p1, v0}, Lkotlin/jvm/internal/m;->f(Ljava/lang/Object;Ljava/lang/String;)V

    iget-object p1, p0, Lfc0/q0;->b:Lfc0/k1;

    iget-object p1, p1, Lfc0/k1;->d:Ljava/lang/Object;

    invoke-interface {p1}, Lwb0/t0;->X()V

    return-void

    :pswitch_0
    const-string v0, "it"

    invoke-static {p1, v0}, Lkotlin/jvm/internal/m;->f(Ljava/lang/Object;Ljava/lang/String;)V

    iget-object p1, p0, Lfc0/q0;->b:Lfc0/k1;

    iget-object p1, p1, Lfc0/k1;->l:Lbp/f;

    iget-object p1, p1, Lbp/f;->a:Ljava/lang/Object;

    check-cast p1, Lp10/a;

    invoke-virtual {p1}, Lp10/a;->iterator()Ljava/util/Iterator;

    move-result-object p1

    :goto_0
    invoke-interface {p1}, Ljava/util/Iterator;->hasNext()Z

    move-result v0

    if-eqz v0, :cond_0

    invoke-interface {p1}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Ls40/d;

    invoke-virtual {v0}, Ls40/d;->b()V

    const/4 v1, 0x0

    iput v1, v0, Ls40/d;->d:I

    goto :goto_0

    :cond_0
    return-void

    nop

    :pswitch_data_0
    .packed-switch 0x0
        :pswitch_0
    .end packed-switch
.end method

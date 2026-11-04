.method public final b()V
    .locals 3

    iget-boolean v0, p0, Ls40/d;->a:Z

    const/4 v1, 0x0

    if-eqz v0, :cond_0

    iget-object v0, p0, Ls40/d;->b:La40/n;

    invoke-virtual {v0}, La40/n;->c()Z

    move-result v0

    if-eqz v0, :cond_0

    iput v1, p0, Ls40/d;->d:I

    :cond_0
    iget v0, p0, Ls40/d;->d:I

    iget v2, p0, Ls40/d;->c:I

    if-ne v0, v2, :cond_1

    iput v1, p0, Ls40/d;->d:I

    :cond_1
    return-void
.end method

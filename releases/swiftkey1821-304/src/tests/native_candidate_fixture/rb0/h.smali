.class public final Lrb0/h;
.super Lra/q1;

.method public final d(Z)V
    .locals 0

    if-eqz p1, :cond_0

    const/4 p1, 0x1

    iput-boolean p1, p0, Lrb0/h;->n:Z

    const/4 p1, 0x0

    iput-boolean p1, p0, Lrb0/h;->o:Z

    iput p1, p0, Lrb0/h;->p:I

    iput p1, p0, Lrb0/h;->q:I

    invoke-virtual {p0}, Lrb0/h;->H()V

    return-void

    :cond_0
    invoke-virtual {p0}, Lrb0/h;->H()V

    return-void
.end method

.method public final H()V
    .locals 4

    invoke-virtual {p0}, Lrb0/h;->G()V

    iget-boolean v0, p0, Lrb0/h;->o:Z

    if-nez v0, :cond_1

    const/4 v0, 0x1

    iput-boolean v0, p0, Lrb0/h;->o:Z

    const/4 v0, 0x0

    :goto_0
    iget-object v1, p0, Lrb0/h;->l:Landroidx/recyclerview/widget/GridLayoutManager;

    iget v2, v1, Landroidx/recyclerview/widget/GridLayoutManager;->G:I

    if-ge v0, v2, :cond_0

    iget v2, p0, Lrb0/h;->p:I

    invoke-virtual {p0}, Lrb0/h;->j()I

    move-result v3

    if-ge v2, v3, :cond_0

    iget-object v1, v1, Landroidx/recyclerview/widget/GridLayoutManager;->L:Lce0/a;

    iget v2, p0, Lrb0/h;->q:I

    add-int/lit8 v3, v2, 0x1

    iput v3, p0, Lrb0/h;->q:I

    invoke-virtual {v1, v2}, Lce0/a;->s(I)I

    move-result v1

    add-int/2addr v0, v1

    goto :goto_0

    :cond_0
    iget v0, p0, Lrb0/h;->q:I

    add-int/lit8 v0, v0, -0x1

    iput v0, p0, Lrb0/h;->q:I

    :cond_1
    invoke-virtual {p0}, Lra/q1;->n()V

    return-void
.end method

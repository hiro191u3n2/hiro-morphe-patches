.class public final Lrb0/k;
.super Lrb0/d;

.method public final onAttachedToWindow()V
    .locals 4

    invoke-super {p0}, Lrb0/d;->onAttachedToWindow()V

    iget-object v0, p0, Lrb0/k;->h:Lfb0/u;

    invoke-virtual {v0, p0}, Lfb0/u;->i(Lfb0/s;)V

    iget-object v1, p0, Lrb0/k;->m:Lrb0/h;

    invoke-virtual {v0, v1}, Lfb0/u;->i(Lfb0/s;)V

    iget-object v0, p0, Lrb0/k;->l:Lcom/touchtype/ui/AccessibilityEmptyRecyclerView;

    iget-object v2, p0, Lrb0/k;->n:Lrb0/l;

    invoke-virtual {v0, v2}, Landroidx/recyclerview/widget/RecyclerView;->r(Lra/f2;)V

    const/4 v3, 0x1

    iput-boolean v3, v1, Lrb0/h;->r:Z

    invoke-virtual {v1}, Lrb0/h;->G()V

    invoke-virtual {v1}, Lra/q1;->n()V

    iget-object v1, p0, Lrb0/k;->o:Lyd0/e;

    invoke-virtual {v1}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    iget-object v1, p0, Lrb0/k;->i:Lwb0/t0;

    invoke-interface {v1, p0}, Lwb0/a1;->f(Lcc0/g;)V

    const/4 v1, 0x0

    iput v1, v2, Lrb0/l;->c:I

    invoke-virtual {v0, v1}, Landroidx/recyclerview/widget/RecyclerView;->w0(I)V

    return-void
.end method

.class public final Lcom/touchtype/keyboard/candidates/view/SequentialCandidateBarLayoutWithECWButton;
.super Landroid/widget/FrameLayout;

.method public final onFinishInflate()V
    .locals 2

    invoke-super {p0}, Landroid/view/View;->onFinishInflate()V

    const v0, 0x7f0a0473

    invoke-virtual {p0, v0}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v0

    const-string v1, "findViewById(...)"

    invoke-static {v0, v1}, Lkotlin/jvm/internal/m;->e(Ljava/lang/Object;Ljava/lang/String;)V

    check-cast v0, Lcom/touchtype/keyboard/candidates/view/SequentialCandidatesRecyclerView;

    iput-object v0, p0, Lcom/touchtype/keyboard/candidates/view/SequentialCandidateBarLayoutWithECWButton;->b:Lcom/touchtype/keyboard/candidates/view/SequentialCandidatesRecyclerView;

    const v0, 0x7f0a0474

    invoke-virtual {p0, v0}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v0

    invoke-static {v0, v1}, Lkotlin/jvm/internal/m;->e(Ljava/lang/Object;Ljava/lang/String;)V

    check-cast v0, Lcom/touchtype/keyboard/expandedcandidate/ExpandedResultsOverlayOpenButton;

    iput-object v0, p0, Lcom/touchtype/keyboard/candidates/view/SequentialCandidateBarLayoutWithECWButton;->g:Lcom/touchtype/keyboard/expandedcandidate/ExpandedResultsOverlayOpenButton;

    return-void
.end method

.method public final onMeasure(II)V
    .locals 2

    invoke-super {p0, p1, p2}, Landroid/widget/FrameLayout;->onMeasure(II)V

    iget-object p1, p0, Lcom/touchtype/keyboard/candidates/view/SequentialCandidateBarLayoutWithECWButton;->b:Lcom/touchtype/keyboard/candidates/view/SequentialCandidatesRecyclerView;

    if-eqz p1, :cond_1

    invoke-virtual {p1}, Landroid/view/View;->getLayoutParams()Landroid/view/ViewGroup$LayoutParams;

    move-result-object p1

    const-string p2, "null cannot be cast to non-null type android.widget.FrameLayout.LayoutParams"

    invoke-static {p1, p2}, Lkotlin/jvm/internal/m;->d(Ljava/lang/Object;Ljava/lang/String;)V

    check-cast p1, Landroid/widget/FrameLayout$LayoutParams;

    invoke-virtual {p0}, Landroid/view/View;->getResources()Landroid/content/res/Resources;

    move-result-object p2

    const v0, 0x7f070065

    invoke-virtual {p2, v0}, Landroid/content/res/Resources;->getDimensionPixelSize(I)I

    move-result p2

    invoke-virtual {p0}, Landroid/view/View;->getLayoutDirection()I

    move-result v0

    const/4 v1, 0x1

    if-ne v0, v1, :cond_0

    const/4 v0, 0x0

    invoke-virtual {p1, p2, v0, v0, v0}, Landroid/view/ViewGroup$MarginLayoutParams;->setMargins(IIII)V

    :cond_0
    return-void

    :cond_1
    const-string p1, "sequentialCandidatesRecyclerView"

    invoke-static {p1}, Lkotlin/jvm/internal/m;->l(Ljava/lang/String;)V

    const/4 p1, 0x0

    throw p1
.end method

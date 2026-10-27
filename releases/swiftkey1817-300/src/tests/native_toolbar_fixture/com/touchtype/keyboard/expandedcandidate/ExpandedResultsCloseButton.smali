.class public final Lcom/touchtype/keyboard/expandedcandidate/ExpandedResultsCloseButton;
.super Lcom/google/android/material/button/MaterialButton;

.method public final onAttachedToWindow()V
    .locals 2

    invoke-super {p0}, Lcom/google/android/material/button/MaterialButton;->onAttachedToWindow()V

    iget-object v0, p0, Lcom/touchtype/keyboard/expandedcandidate/ExpandedResultsCloseButton;->b:Lgd0/b;

    const/4 v1, 0x0

    if-eqz v0, :cond_0

    invoke-interface {v0}, Lgd0/b;->a()Led0/f;

    move-result-object v0

    invoke-interface {v0, p0}, Led0/f;->b(Ldd0/u;)V

    invoke-virtual {p0}, Landroid/view/View;->requestFocus()Z

    const/16 v0, 0x40

    invoke-virtual {p0, v0, v1}, Landroid/view/View;->performAccessibilityAction(ILandroid/os/Bundle;)Z

    return-void

    :cond_0
    const-string v0, "themeProvider"

    invoke-static {v0}, Lkotlin/jvm/internal/m;->l(Ljava/lang/String;)V

    throw v1
.end method

.method public final onMeasure(II)V
    .locals 0

    iget-object p1, p0, Lcom/touchtype/keyboard/expandedcandidate/ExpandedResultsCloseButton;->a:Lui0/k;

    if-eqz p1, :cond_1

    if-eqz p1, :cond_0

    invoke-virtual {p1}, Lui0/k;->d()I

    move-result p1

    const/high16 p2, 0x40000000    # 2.0f

    invoke-static {p1, p2}, Landroid/view/View$MeasureSpec;->makeMeasureSpec(II)I

    move-result p1

    invoke-super {p0, p1, p1}, Landroid/view/View;->onMeasure(II)V

    return-void

    :cond_0
    const-string p1, "keyHeightProvider"

    invoke-static {p1}, Lkotlin/jvm/internal/m;->l(Ljava/lang/String;)V

    const/4 p1, 0x0

    throw p1

    :cond_1
    new-instance p1, Ljava/lang/IllegalStateException;

    const-string p2, "ExpandedResultsCloseButton not initialised"

    invoke-direct {p1, p2}, Ljava/lang/IllegalStateException;-><init>(Ljava/lang/String;)V

    throw p1
.end method

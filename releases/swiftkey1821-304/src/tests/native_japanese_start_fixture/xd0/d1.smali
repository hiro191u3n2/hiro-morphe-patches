.class public abstract Lxd0/d1;
.super Landroid/widget/FrameLayout;
.implements Ldd0/u;

.method public final onTouchEvent(Landroid/view/MotionEvent;)Z
    .locals 5

    new-instance v0, Llh0/a;

    invoke-direct {v0}, Llh0/a;-><init>()V

    invoke-virtual {p1}, Landroid/view/MotionEvent;->getActionMasked()I

    move-result v1

    const/4 v2, 0x1

    if-eq v1, v2, :cond_0

    const/4 v3, 0x6

    if-ne v1, v3, :cond_1

    :cond_0
    new-instance v1, Lrh0/m;

    invoke-virtual {p1}, Landroid/view/MotionEvent;->getEventTime()J

    move-result-wide v3

    invoke-direct {v1, v0, v3, v4}, Lrh0/m;-><init>(Llh0/a;J)V

    new-array v2, v2, [Lnh0/r;

    const/4 v3, 0x0

    aput-object v1, v2, v3

    iget-object v1, p0, Lxd0/d1;->c:Lg40/c;

    invoke-interface {v1, v2}, Lg40/c;->f([Lnh0/r;)Z

    :cond_1
    invoke-virtual {p0, v0, p1}, Lxd0/d1;->f(Llh0/a;Landroid/view/MotionEvent;)Z

    move-result p1

    return p1
.end method

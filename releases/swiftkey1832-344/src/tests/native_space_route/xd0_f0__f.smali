.method public final f(Llh0/a;Landroid/view/MotionEvent;)Z
    .locals 12

    iget-boolean v0, p0, Lxd0/q;->j:Z

    const/4 v1, 0x1

    if-nez v0, :cond_1

    :cond_0
    move-object v6, p0

    goto/16 :goto_5

    :cond_1
    new-instance v0, Lkj0/f;

    iget-object v2, p0, Lxd0/q;->i:Landroid/graphics/Matrix;

    iget-object v3, p0, Lxd0/f0;->y:Lfb0/j;

    invoke-direct {v0, p1, p2, v2, v3}, Lkj0/f;-><init>(Llh0/a;Landroid/view/MotionEvent;Landroid/graphics/Matrix;Lfb0/j;)V

    const/4 p1, 0x0

    move p2, p1

    :goto_0
    iget-object v2, v0, Lkj0/f;->c:Ljava/lang/Object;

    check-cast v2, Landroid/view/MotionEvent;

    iget-object v3, v0, Lkj0/f;->b:Ljava/lang/Object;

    check-cast v3, Llh0/a;

    invoke-virtual {v2}, Landroid/view/MotionEvent;->getPointerCount()I

    move-result v4

    if-ge p2, v4, :cond_0

    invoke-virtual {v2, p2}, Landroid/view/MotionEvent;->getPointerId(I)I

    move-result v4

    iget-object v5, p0, Lxd0/f0;->I:Landroid/util/SparseArray;

    invoke-virtual {v5, v4}, Landroid/util/SparseArray;->get(I)Ljava/lang/Object;

    move-result-object v5

    check-cast v5, Lxd0/q;

    if-eqz v5, :cond_2

    move-object v9, v5

    goto :goto_1

    :cond_2
    move-object v9, p0

    :goto_1
    if-ne v9, p0, :cond_4

    move-object v6, p0

    :cond_3
    move-object v7, v0

    goto :goto_2

    :cond_4
    const/4 v5, 0x2

    new-array v10, v5, [I

    new-array v11, v5, [I

    invoke-virtual {v9, v10}, Landroid/view/View;->getLocationOnScreen([I)V

    invoke-virtual {p0, v11}, Landroid/view/View;->getLocationOnScreen([I)V

    invoke-virtual {v0}, Lkj0/f;->q()F

    move-result v7

    invoke-virtual {v0, p1}, Lkj0/f;->o(I)Landroid/graphics/PointF;

    move-result-object v5

    iget v8, v5, Landroid/graphics/PointF;->y:F

    move-object v6, p0

    invoke-virtual/range {v6 .. v11}, Lxd0/f0;->q(FFLxd0/q;[I[I)Z

    move-result v5

    if-eqz v5, :cond_3

    new-instance v5, Landroid/graphics/Matrix;

    invoke-direct {v5}, Landroid/graphics/Matrix;-><init>()V

    aget v7, v11, p1

    aget v8, v10, p1

    sub-int/2addr v7, v8

    int-to-float v7, v7

    aget v8, v11, v1

    aget v10, v10, v1

    sub-int/2addr v8, v10

    int-to-float v8, v8

    invoke-virtual {v5, v7, v8}, Landroid/graphics/Matrix;->postTranslate(FF)Z

    invoke-virtual {v9}, Landroid/view/View;->getWidth()I

    move-result v7

    int-to-float v7, v7

    const/high16 v8, 0x3f800000    # 1.0f

    div-float v7, v8, v7

    invoke-virtual {v9}, Landroid/view/View;->getHeight()I

    move-result v10

    int-to-float v10, v10

    div-float/2addr v8, v10

    invoke-virtual {v5, v7, v8}, Landroid/graphics/Matrix;->postScale(FF)Z

    new-instance v7, Lkj0/f;

    invoke-direct {v7, v3, v2, v5}, Lkj0/f;-><init>(Llh0/a;Landroid/view/MotionEvent;Landroid/graphics/Matrix;)V

    :goto_2
    iget-boolean v2, v6, Lxd0/f0;->J:Z

    if-nez v2, :cond_5

    iput v4, v6, Lxd0/f0;->K:I

    goto :goto_3

    :cond_5
    iget v2, v6, Lxd0/f0;->L:I

    if-eq v4, v2, :cond_6

    iget-object v2, v9, Lxd0/q;->h:Ltj0/e;

    invoke-virtual {v2, v3, v4}, Ltj0/e;->D(Llh0/a;I)V

    goto :goto_4

    :cond_6
    :goto_3
    iget-object v2, v9, Lxd0/q;->g:Lef0/d;

    invoke-virtual {v9, v7, p2}, Lxd0/q;->l(Lkj0/f;I)Lfc0/f;

    move-result-object v3

    invoke-virtual {v2, p2, v3, v7}, Lef0/d;->l(ILfc0/f;Lkj0/f;)V

    :goto_4
    add-int/lit8 p2, p2, 0x1

    goto/16 :goto_0

    :goto_5
    return v1
.end method

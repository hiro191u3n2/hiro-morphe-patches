.method private static permitsLearning(Landroid/view/inputmethod/EditorInfo;)Z
    .locals 5

    iget v0, p0, Landroid/view/inputmethod/EditorInfo;->inputType:I

    and-int/lit8 v1, v0, 0xf

    const/4 v2, 0x0

    const/4 v3, 0x1

    if-eq v1, v3, :cond_0

    return v2

    :cond_0
    and-int/lit16 v1, v0, 0xff0

    const/16 v4, 0x80

    if-eq v1, v4, :cond_1

    const/16 v4, 0x90

    if-eq v1, v4, :cond_1

    const/16 v4, 0xe0

    if-eq v1, v4, :cond_1

    const/high16 v1, 0x80000

    and-int/2addr v0, v1

    if-nez v0, :cond_1

    iget p0, p0, Landroid/view/inputmethod/EditorInfo;->imeOptions:I

    const/high16 v0, 0x1000000

    and-int/2addr p0, v0

    if-nez p0, :cond_1

    move v2, v3

    :cond_1
    return v2
.end method

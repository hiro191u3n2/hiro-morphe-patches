.method public static rank(Ljava/util/List;Ll40/c;)Ljava/util/List;
    .locals 3
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/util/List<",
            "Lm40/b;",
            ">;",
            "Ll40/c;",
            ")",
            "Ljava/util/List<",
            "Lm40/b;",
            ">;"
        }
    .end annotation

    :try_start_0
    sget-object v0, Lhiro/swiftkey/UsageBridge;->session:Lhiro/swiftkey/UsageBridge$Session;

    invoke-static {p1}, Lhiro/swiftkey/UsageBridge;->eligible(Ll40/c;)Z

    move-result v1

    if-eqz v1, :cond_2

    const/4 v1, 0x0

    invoke-static {v0, v1}, Lhiro/swiftkey/UsageBridge;->allowed(Lhiro/swiftkey/UsageBridge$Session;Lwb0/n0;)Z

    move-result v2

    if-nez v2, :cond_0

    goto :goto_0

    :cond_0
    const/4 v2, 0x1

    invoke-static {p0, p1, v2}, Lhiro/swiftkey/FrequencyLearning;->rank(Ljava/util/List;Ll40/c;Z)Ljava/util/List;

    move-result-object p1

    invoke-static {v0, v1}, Lhiro/swiftkey/UsageBridge;->allowed(Lhiro/swiftkey/UsageBridge$Session;Lwb0/n0;)Z

    move-result v0
    :try_end_0
    .catch Ljava/lang/RuntimeException; {:try_start_0 .. :try_end_0} :catch_1
    .catch Ljava/lang/LinkageError; {:try_start_0 .. :try_end_0} :catch_0

    if-eqz v0, :cond_1

    move-object p0, p1

    :cond_1
    return-object p0

    :cond_2
    :goto_0
    return-object p0

    :catch_0
    move-exception p1

    goto :goto_1

    :catch_1
    move-exception p1

    :goto_1
    return-object p0
.end method

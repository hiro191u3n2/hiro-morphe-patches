.class public final Lrb0/l;
.super Lra/f2;
.source "SourceFile"


# instance fields
.field public final a:Lwb0/t0;

.field public final b:Landroidx/recyclerview/widget/GridLayoutManager;

.field public c:I


# direct methods
.method public constructor <init>(Lwb0/t0;Landroidx/recyclerview/widget/GridLayoutManager;)V
    .locals 1

    const-string v0, "inputEventModel"

    invoke-static {p1, v0}, Lkotlin/jvm/internal/m;->f(Ljava/lang/Object;Ljava/lang/String;)V

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    iput-object p1, p0, Lrb0/l;->a:Lwb0/t0;

    iput-object p2, p0, Lrb0/l;->b:Landroidx/recyclerview/widget/GridLayoutManager;

    return-void
.end method


# virtual methods
.method public final b(Landroidx/recyclerview/widget/RecyclerView;II)V
    .locals 2

    iget-object p1, p0, Lrb0/l;->b:Landroidx/recyclerview/widget/GridLayoutManager;

    invoke-virtual {p1}, Landroidx/recyclerview/widget/LinearLayoutManager;->W0()I

    move-result p1

    iget p2, p0, Lrb0/l;->c:I

    if-le p1, p2, :cond_0

    new-instance p2, Llh0/a;

    invoke-direct {p2}, Llh0/a;-><init>()V

    const/4 p3, 0x1

    sget-object v0, Lib0/m;->g:Lib0/m;

    iget-object v1, p0, Lrb0/l;->a:Lwb0/t0;

    invoke-interface {v1, p2, p3, v0}, Lwb0/t0;->Y(Llh0/a;ZLib0/m;)V

    iput p1, p0, Lrb0/l;->c:I

    :cond_0
    return-void
.end method

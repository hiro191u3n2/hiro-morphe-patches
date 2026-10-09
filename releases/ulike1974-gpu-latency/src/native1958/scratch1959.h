/* H43: every staging buffer has an exclusive lease. Four idle workspaces
 * may retain bounded capacity; trim never frees a busy worker's memory. */
#ifndef ULIKE_SCRATCH1959_H
#define ULIKE_SCRATCH1959_H
#define SCRATCH_BLOCKS1959 9
#define SCRATCH_RETAIN1959 ((size_t)48*1024*1024)
#define SCRATCH_TOTAL_IDLE1959 ((size_t)96*1024*1024)
typedef struct {
    void *blocks[SCRATCH_BLOCKS1959];
    size_t capacities[SCRATCH_BLOCKS1959],bytes;
    int pooled,busy,discard;
} Scratch1959;
static pthread_mutex_t scratch_mutex1959=PTHREAD_MUTEX_INITIALIZER;
static Scratch1959 scratch_pool1959[4];

static void scratch_free1959(Scratch1959 *s) {
    for(int i=0;i<SCRATCH_BLOCKS1959;i++){free(s->blocks[i]);s->blocks[i]=NULL;s->capacities[i]=0;}
    s->bytes=0;s->discard=0;
}
static Scratch1959 *scratch_acquire1959(Scratch1959 *temporary) {
    (void)temporary;Scratch1959 *s=NULL;
    pthread_mutex_lock(&scratch_mutex1959);
    for(int i=0;i<4;i++)if(!scratch_pool1959[i].busy){s=&scratch_pool1959[i];s->pooled=1;s->busy=1;break;}
    pthread_mutex_unlock(&scratch_mutex1959);return s;
}
static void *scratch_reserve1959(Scratch1959 *s,int block,size_t bytes) {
    if(!s||block<0||block>=SCRATCH_BLOCKS1959||!bytes)return NULL;
    pthread_mutex_lock(&scratch_mutex1959);
    if(s->capacities[block]<bytes){
        void *replacement=realloc(s->blocks[block],bytes);
        if(!replacement){pthread_mutex_unlock(&scratch_mutex1959);return NULL;}
        s->bytes+=bytes-s->capacities[block];s->blocks[block]=replacement;s->capacities[block]=bytes;
        if(s->bytes>SCRATCH_RETAIN1959)s->discard=1;
    }
    void *result=s->blocks[block];
    pthread_mutex_unlock(&scratch_mutex1959);return result;
}
static void scratch_release1959(Scratch1959 *s) {
    pthread_mutex_lock(&scratch_mutex1959);
    if(s->discard)scratch_free1959(s);
    s->busy=0;
    size_t idle=0;for(int i=0;i<4;i++)if(!scratch_pool1959[i].busy)idle+=scratch_pool1959[i].bytes;
    for(int i=0;i<4&&idle>SCRATCH_TOTAL_IDLE1959;i++)if(!scratch_pool1959[i].busy&& &scratch_pool1959[i]!=s){idle-=scratch_pool1959[i].bytes;scratch_free1959(&scratch_pool1959[i]);}
    pthread_mutex_unlock(&scratch_mutex1959);
}
static uint64_t scratch_bytes1959(void) {
    uint64_t bytes=0;pthread_mutex_lock(&scratch_mutex1959);
    for(int i=0;i<4;i++)bytes+=scratch_pool1959[i].bytes;
    pthread_mutex_unlock(&scratch_mutex1959);return bytes;
}
static void scratch_trim1959(void) {
    pthread_mutex_lock(&scratch_mutex1959);
    for(int i=0;i<4;i++){if(scratch_pool1959[i].busy)scratch_pool1959[i].discard=1;else scratch_free1959(&scratch_pool1959[i]);}
    pthread_mutex_unlock(&scratch_mutex1959);
}
#endif

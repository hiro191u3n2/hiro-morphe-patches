/* Host-only link-time fault fixture; never included in the ARM64 artifact.
 * The real map is always unmapped first. One requested false return exercises
 * production handling of an unmap failure after staging/direct copy completed. */
#include <jni.h>
#include <GLES3/gl31.h>
#include <stdatomic.h>
static atomic_int fail_unmap;
extern GLboolean __real_glUnmapBuffer(GLenum target);
GLboolean __wrap_glUnmapBuffer(GLenum target) {
    GLboolean actual=__real_glUnmapBuffer(target);
    return atomic_exchange(&fail_unmap,0) ? GL_FALSE : actual;
}
JNIEXPORT void JNICALL Java_com_hiro_ulike_DispatchReadback1954Test_failNextUnmap(
        JNIEnv *env,jclass cls) {
    (void)env;(void)cls;atomic_store(&fail_unmap,1);
}

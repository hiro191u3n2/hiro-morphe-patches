/* Execute the actual program/cache/getter implementation with a controlled
 * driver boundary. These cases prove diagnostics, not shader mathematics or
 * physical-device capability. All unmocked GL/EGL functions link normally. */
#include <jni.h>
#include <EGL/egl.h>
#include <GLES3/gl31.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static int assertions, gl_calls, egl_calls, compile_calls, link_calls, string_calls;
static int shader_object, program_object, compile_ok, link_ok, uniform_count;
static int local_x, local_y, local_z;
static GLenum pending_error;
static const char *extensions;
static char returned_string[256];

static void check(int value,const char *message){
 assertions++;if(!value){fprintf(stderr,"FAIL %s\n",message);exit(1);}
}
static void text_result(const char *text,GLsizei capacity,GLsizei *length,GLchar *output){
 size_t size=strlen(text);if(capacity<=0){if(length)*length=0;return;}
 if(size>=(size_t)capacity)size=(size_t)capacity-1;
 memcpy(output,text,size);output[size]=0;if(length)*length=(GLsizei)size;
}
static GLuint GL_APIENTRY fixtureCreateShader(GLenum type){(void)type;gl_calls++;return shader_object?11u:0u;}
static void GL_APIENTRY fixtureShaderSource(GLuint shader,GLsizei count,const GLchar *const *parts,const GLint *lengths){(void)shader;(void)count;(void)parts;(void)lengths;gl_calls++;}
static void GL_APIENTRY fixtureCompileShader(GLuint shader){(void)shader;gl_calls++;compile_calls++;}
static void GL_APIENTRY fixtureGetShaderiv(GLuint shader,GLenum name,GLint *value){(void)shader;(void)name;gl_calls++;*value=compile_ok;}
static void GL_APIENTRY fixtureGetShaderInfoLog(GLuint shader,GLsizei capacity,GLsizei *length,GLchar *output){(void)shader;gl_calls++;text_result("controlled compile failure",capacity,length,output);}
static const GLubyte *GL_APIENTRY fixtureGetString(GLenum name){(void)name;gl_calls++;return (const GLubyte *)extensions;}
static GLuint GL_APIENTRY fixtureCreateProgram(void){gl_calls++;return program_object?22u:0u;}
static void GL_APIENTRY fixtureAttachShader(GLuint program_id,GLuint shader){(void)program_id;(void)shader;gl_calls++;}
static void GL_APIENTRY fixtureLinkProgram(GLuint program_id){(void)program_id;gl_calls++;link_calls++;}
static void GL_APIENTRY fixtureGetProgramiv(GLuint program_id,GLenum name,GLint *value){
 (void)program_id;gl_calls++;
 if(name==GL_LINK_STATUS)*value=link_ok;
 else if(name==GL_ACTIVE_UNIFORMS)*value=1;
 else if(name==GL_COMPUTE_WORK_GROUP_SIZE){value[0]=local_x;value[1]=local_y;value[2]=local_z;}
 else *value=0;
}
static void GL_APIENTRY fixtureGetProgramInfoLog(GLuint program_id,GLsizei capacity,GLsizei *length,GLchar *output){(void)program_id;gl_calls++;text_result("controlled link failure",capacity,length,output);}
static void GL_APIENTRY fixtureDeleteShader(GLuint shader){(void)shader;gl_calls++;}
static void GL_APIENTRY fixtureDeleteProgram(GLuint program_id){(void)program_id;gl_calls++;}
static GLenum GL_APIENTRY fixtureGetError(void){GLenum error=pending_error;gl_calls++;pending_error=GL_NO_ERROR;return error;}
static GLint GL_APIENTRY fixtureGetUniformLocation(GLuint program_id,const GLchar *name){(void)program_id;gl_calls++;return !strcmp(name,"u[0]")?0:1;}
static void GL_APIENTRY fixtureGetActiveUniform(GLuint program_id,GLuint index,GLsizei capacity,GLsizei *length,GLint *size,GLenum *type,GLchar *name){
 (void)program_id;(void)index;gl_calls++;*size=uniform_count;*type=GL_INT;text_result("u[0]",capacity,length,name);
}
static EGLContext EGLAPIENTRY fixtureGetCurrentContext(void){egl_calls++;return EGL_NO_CONTEXT;}
static EGLDisplay EGLAPIENTRY fixtureGetDisplay(EGLNativeDisplayType native){(void)native;egl_calls++;return EGL_NO_DISPLAY;}
static jstring JNICALL fixtureNewStringUTF(JNIEnv *env,const char *text){
 (void)env;string_calls++;snprintf(returned_string,sizeof(returned_string),"%s",text);return (jstring)(void *)returned_string;
}

/* Rename only after the real headers have declared their ABI types. */
#define glCreateShader fixtureCreateShader
#define glShaderSource fixtureShaderSource
#define glCompileShader fixtureCompileShader
#define glGetShaderiv fixtureGetShaderiv
#define glGetShaderInfoLog fixtureGetShaderInfoLog
#define glGetString fixtureGetString
#define glCreateProgram fixtureCreateProgram
#define glAttachShader fixtureAttachShader
#define glLinkProgram fixtureLinkProgram
#define glGetProgramiv fixtureGetProgramiv
#define glGetProgramInfoLog fixtureGetProgramInfoLog
#define glDeleteShader fixtureDeleteShader
#define glDeleteProgram fixtureDeleteProgram
#define glGetError fixtureGetError
#define glGetUniformLocation fixtureGetUniformLocation
#define glGetActiveUniform fixtureGetActiveUniform
#define eglGetCurrentContext fixtureGetCurrentContext
#define eglGetDisplay fixtureGetDisplay
#include "engine1960.c"

static void reset(void){
 memset(&state,0,sizeof(state));state.initialized=1;state.maxShared=1024*1024;
 state.maxLocal=128;state.maxInvocations=128;
 shader_object=program_object=compile_ok=link_ok=1;uniform_count=32;
 local_x=64;local_y=local_z=1;pending_error=GL_NO_ERROR;
 extensions="GL_EXT_gpu_shader5 GL_EXT_shader_explicit_arithmetic_types_float64";
 gl_calls=egl_calls=compile_calls=link_calls=string_calls=0;returned_string[0]=0;
}
static void expected_failure(int id,const char *reason){
 check(program(id)==0,"unsupported program rejected");
 check(state.attempted[id]==1,"failed build is marked attempted");
 check(state.programs[id]==0,"failed program is not cached as usable");
 check(strncmp(state.programFailure19100[id],reason,strlen(reason))==0,"exact diagnostic category");
 int calls=gl_calls,compiled=compile_calls,linked=link_calls;
 char saved[256];memcpy(saved,state.programFailure19100[id],sizeof(saved));
 compile_ok=link_ok=shader_object=program_object=1;pending_error=GL_NO_ERROR;
 check(JNI1960(supportedNative)(NULL,NULL,id)==JNI_FALSE,"cached unsupported stays unsupported");
 check(JNI1960(workgroupNative)(NULL,NULL,id)==0,"cached unsupported has no workgroup");
 check(gl_calls==calls&&compile_calls==compiled&&link_calls==linked,"cached unsupported performs no GL or rebuild");
 check(memcmp(saved,state.programFailure19100[id],sizeof(saved))==0,"cached rejection preserves original evidence");
}
static void getter_is_observational(void){
 struct JNINativeInterface_ table={0};table.NewStringUTF=fixtureNewStringUTF;JNIEnv env=&table;
 reset();state.initialized=0;
 snprintf(state.programFailure19100[2],sizeof(state.programFailure19100[2]),"shader_compile_failed: retained evidence");
 state.failureCode1971=FAILURE_UPLOAD1971;state.failed=1;state.token=73;
 State before=state;
 check(JNI1960(programFailureNative19100)(&env,NULL,2)==(jstring)(void *)returned_string,"JNI getter returns NewStringUTF result");
 check(strcmp(returned_string,"shader_compile_failed: retained evidence")==0,"JNI getter returns program-specific evidence");
 check(JNI1960(programFailureNative19100)(&env,NULL,-1)==(jstring)(void *)returned_string&&returned_string[0]==0,"negative id returns empty");
 check(JNI1960(programFailureNative19100)(&env,NULL,PROGRAMS)==(jstring)(void *)returned_string&&returned_string[0]==0,"upper-bound id returns empty");
 check(JNI1960(programFailureNative19100)(&env,NULL,3)==(jstring)(void *)returned_string&&returned_string[0]==0,"unobserved program returns empty");
 check(string_calls==4,"one JNI string per getter");
 check(gl_calls==0&&egl_calls==0&&compile_calls==0,"getter initializes or compiles nothing");
 check(memcmp(&state,&before,sizeof(state))==0,"getter preserves all owner/cache/failure state");
}
int main(void){
 reset();compile_ok=0;expected_failure(2,"shader_compile_failed:");
 check(strstr(state.programFailure19100[2],"controlled compile failure")!=NULL,"driver compile detail retained");
 reset();compile_ok=0;extensions="GL_EXT_gpu_shader5";expected_failure(1,"fp64_extension_missing:");
 reset();compile_ok=0;extensions="GL_EXT_shader_explicit_arithmetic_types_float64";expected_failure(2,"shader5_extension_missing:");
 reset();compile_ok=0;extensions="";expected_failure(7,"shader_compile_failed:");
 reset();compile_ok=0;extensions="";expected_failure(8,"shader_compile_failed:");
 reset();link_ok=0;expected_failure(2,"program_link_failed:");
 check(strstr(state.programFailure19100[2],"controlled link failure")!=NULL,"driver link detail retained");
 reset();state.maxShared=1;expected_failure(0,"shared_memory_limit:");check(compile_calls==0,"shared limit rejects before compilation");
 reset();uniform_count=31;expected_failure(2,"uniform_contract_failed:");
 reset();local_x=256;expected_failure(2,"workgroup_contract_failed:");
 reset();shader_object=0;expected_failure(2,"shader_object_unavailable:");
 reset();program_object=0;expected_failure(2,"program_object_unavailable:");
 reset();pending_error=GL_INVALID_OPERATION;expected_failure(2,"program_gl_error:");

 reset();compile_ok=0;check(program(2)==0,"session test builds an unsupported program");
 char saved[256];memcpy(saved,state.programFailure19100[2],sizeof(saved));
 int calls=gl_calls;state.failed=1;state.failureCode1971=FAILURE_UPLOAD1971;
 snprintf(state.failure,sizeof(state.failure),"previous session upload failure");state.poolRequested=1;state.used[0]=123;state.sequence=4;
 check(JNI1960(openNative)(NULL,NULL)==5,"new session receives a fresh owner token");
 check(!state.failed&&!state.failure[0]&&!state.failureCode1971,"session failure reset remains intact");
 check(!state.poolRequested&&!state.used[0],"session working-use reset remains intact");
 check(memcmp(saved,state.programFailure19100[2],sizeof(saved))==0&&state.attempted[2],"open preserves program failure and attempted cache");
 compile_ok=1;check(program(2)==0&&gl_calls==calls,"open does not rebuild cached unsupported program");

 reset();snprintf(state.programFailure19100[2],sizeof(state.programFailure19100[2]),"stale diagnostic");
 check(program(2)==1,"valid build still succeeds");
 check(state.programFailure19100[2][0]==0&&state.programs[2]==22&&state.localX[2]==64,"success clears only program evidence and stores capability");
 calls=gl_calls;check(program(2)==1&&gl_calls==calls,"successful program cache remains lazy");
 getter_is_observational();
 printf("RESULT {\"status\":\"passed\",\"assertions\":%d,\"native_program_failure_cache\":true,\"physical_android_tested\":false}\n",assertions);
 return 0;
}

/* Controlled host transport for the original, unedited C program() body and
 * dispatch topology. This inspects compiler inputs; it is not an EGL/JNI run.
 * Shader arithmetic is checked separately after preprocessing, and real native
 * shader execution is the responsibility of host_gpu_programs1978. */
#include <stdint.h>
#include <limits.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "shader_sources1960.h"
typedef unsigned int GLuint,GLenum;
typedef int GLint,jint;
#define JNI_FALSE 0
#define JNI_TRUE 1
#define GL_COMPUTE_SHADER 1
#define GL_COMPILE_STATUS 2
#define GL_LINK_STATUS 3
#define GL_ACTIVE_UNIFORMS 4
#define GL_COMPUTE_WORK_GROUP_SIZE 5
#define GL_INT 6
#define GL_FLOAT 7
#define GL_SHADER_STORAGE_BARRIER_BIT 8
@DECLARATIONS@
static struct {
 unsigned char attempted[PROGRAMS];GLuint programs[PROGRAMS];
 GLint uLocation[PROGRAMS],fLocation[PROGRAMS],uCount[PROGRAMS],fCount[PROGRAMS],localX[PROGRAMS];
 int maxShared,maxLocal,maxInvocations,maxGroups;unsigned long submitted;
 char failure[256];
} state;
static const char *directory;
static int current_id,current_base,current_local,current_mode,current_tile,current_ieee,current_pow2;
static unsigned captured,launch_count,launches[4096][2],current_offset;
static void require(int condition,const char *message){if(!condition){fprintf(stderr,"%s\n",message);exit(2);}}
static int macro(const char *text,const char *name,int fallback){
 const char *p=strstr(text,name);return p?atoi(p+strlen(name)):fallback;
}
static int initialize(void){return 1;}
static int clean_gl(void){return 1;}
static void fail(const char *message){fprintf(stderr,"%s\n",message);}
static GLuint glCreateShader(GLenum kind){require(kind==GL_COMPUTE_SHADER,"wrong shader stage");return 1;}
static void glDeleteShader(GLuint shader){(void)shader;}
static void glDeleteProgram(GLuint program){(void)program;}
static GLuint glCreateProgram(void){return 1;}
static void glCompileShader(GLuint shader){(void)shader;}
static void glAttachShader(GLuint program,GLuint shader){(void)program;(void)shader;}
static void glLinkProgram(GLuint program){(void)program;}
static void glGetShaderiv(GLuint shader,GLenum query,GLint *value){(void)shader;require(query==GL_COMPILE_STATUS,"wrong shader query");*value=1;}
static void glGetShaderInfoLog(GLuint shader,int size,void *length,char *message){(void)shader;(void)length;if(size)message[0]=0;}
static GLint glGetUniformLocation(GLuint program,const char *name){(void)program;return !strcmp(name,"u[0]")?0:1;}
static void glGetActiveUniform(GLuint program,GLuint index,int capacity,void *length,GLint *size,GLenum *type,char *name){
 (void)program;(void)length;require(index<2&&capacity>5,"wrong uniform inspection");
 *size=32;*type=index?GL_FLOAT:GL_INT;strcpy(name,index?"f[0]":"u[0]");
}
static void glGetProgramiv(GLuint program,GLenum query,GLint *value){
 (void)program;
 if(query==GL_LINK_STATUS)*value=1;
 else if(query==GL_ACTIVE_UNIFORMS)*value=2;
 else if(query==GL_COMPUTE_WORK_GROUP_SIZE){value[0]=current_local;value[1]=1;value[2]=1;}
 else require(0,"unknown program query");
}
static void glShaderSource(GLuint shader,int count,const char *const *parts,const GLint *lengths){
 (void)shader;require(count==3,"changed shader source part count");current_base=-1;
 for(int i=0;i<BASE_PROGRAMS;i++)if(parts[0]==sources[i])current_base=i;
 require(current_base>=0,"shader source is outside the engine source table");
 current_local=macro(parts[1],"#define GX_LOCAL_SIZE ",-999);
 current_mode=macro(parts[1],"#define GX_STRONG_MODE ",-999);
 current_tile=macro(parts[1],"#define GX_TILE_WIDTH ",-999);
 current_ieee=macro(parts[1],"#define GX_IEEE_DIV73 ",-999);
 current_pow2=macro(parts[1],"#define GX_POW2_EXACT78 ",0);
 char path[4096];int n=snprintf(path,sizeof(path),"%s/program-%02d.glsl",directory,current_id);
 require(n>0&&(size_t)n<sizeof(path),"shader path too long");FILE *out=fopen(path,"wb");require(out!=NULL,"shader output failed");
 for(int i=0;i<count;i++)require(lengths[i]>=0&&fwrite(parts[i],1,(size_t)lengths[i],out)==(size_t)lengths[i],"shader write failed");
 require(fclose(out)==0,"shader close failed");captured++;
}
static void glUniform1iv(GLint location,int count,const jint *values){(void)location;require(count==32,"changed integer uniforms");current_offset=(unsigned)values[31];}
static void glDispatchCompute(GLuint x,GLuint y,GLuint z){
 require(y==1&&z==1&&launch_count<4096,"invalid launch topology");
 launches[launch_count][0]=x;launches[launch_count][1]=current_offset;launch_count++;
}
static void glMemoryBarrier(GLenum bits){require(bits==GL_SHADER_STORAGE_BARRIER_BIT,"changed dispatch barrier");}
@PROGRAM_BODY@
static int dispatch_shape(int id,jint *u,jint invocations){
@MODE_GUARDS@
@DISPATCH_BODY@
}
int main(int argc,char **argv){
 require(argc==2,"output directory required");directory=argv[1];
 state.maxShared=1024*1024;state.maxLocal=1024;state.maxInvocations=1024;state.maxGroups=3;
 require(!program(-1)&&!program(PROGRAMS)&&captured==0,"invalid program was accepted");
 char path[4096];snprintf(path,sizeof(path),"%s/dispatch.tsv",directory);FILE *dispatch=fopen(path,"wb");require(dispatch!=NULL,"dispatch report failed");
 for(int id=0;id<PROGRAMS;id++){
  current_id=id;unsigned before=captured;require(program(id),"declared program rejected");
  require(captured==before+1&&program(id)&&captured==before+1,"program cache changed");
  printf("%d\t%s\t%d\t%d\t%d\t%d\t%d\t%d\t%d\n",id,source_names[current_base],current_base,current_local,current_mode,current_tile,current_ieee,current_pow2,state.localX[id]);
  const int geometry[4][3]={{1,1,0},{7,5,3},{128,32,4},{129,33,9}};
  for(int g=0;g<4;g++){
   jint u[32]={0};u[0]=geometry[g][0];u[2]=geometry[g][2];u[3]=u[2]+geometry[g][1];u[10]=current_mode<0?3:current_mode;
   launch_count=0;require(dispatch_shape(id,u,u[0]*(u[3]-u[2]))==JNI_TRUE,"valid dispatch rejected");
   fprintf(dispatch,"%d\t%d\t%d\t%d\t%d",id,u[0],u[3]-u[2],u[2],u[10]);
   for(unsigned k=0;k<launch_count;k++)fprintf(dispatch,"\t%u:%u",launches[k][0],launches[k][1]);
   fputc('\n',dispatch);
  }
  if(id>=STRONG_PROGRAM_BASE){
   jint u[32]={0};u[0]=7;u[3]=5;u[10]=(current_mode+1)%4;launch_count=0;
   require(dispatch_shape(id,u,35)==JNI_FALSE&&launch_count==0,"specialized mode guard bypass");
  }
 }
 require(fclose(dispatch)==0,"dispatch report close failed");return 0;
}

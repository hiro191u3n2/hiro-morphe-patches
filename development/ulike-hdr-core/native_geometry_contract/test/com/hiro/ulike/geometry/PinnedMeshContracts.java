package com.hiro.ulike.geometry;
import com.hiro.ulike.binding.AuthoredMesh;
import java.util.zip.*;
import java.io.*;
public final class PinnedMeshContracts {
 static byte[] read(InputStream in)throws Exception{ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] p=new byte[4096];int n;while((n=in.read(p))!=-1)b.write(p,0,n);return b.toByteArray();}
 public static void main(String[] a)throws Exception {
  String[][] specs={
   {"0","AmazingFeature1/mesh/mask_faceuv22995_mesh.mesh","6104a8128030b43bd93c1eebe23fd0c795bd6af44ba7cd95a1a6c8e76f85fbdc"},
   {"1","AmazingFeature1/mesh/lips_keypoint_faceu2996_mesh.mesh","e645b4dfec7d374d8c224247108d59ceab9297c60a0d1831079911c4894f8863"},
   {"1","AmazingFeature3/mesh/mask_faceuv22994_mesh.mesh","fd8dee385cb47d4aba35360c52a87d1b29560c37213585680152bf4d5586b0e3"},
   {"1","AmazingFeature5/mesh/eye_part_faceu2988_mesh.mesh","f9559d495c6af95af811bc32eabffe4338a58f5c6d928be2d4072d0f97ebd110"},
   {"1","AmazingFeature6/mesh/jiemaoFaceU_V2_vwwo_1669694037.mesh","2529c347728a63bf6f59c6cfc7aa965f93d62ece20252d2aa75f1d8618434a68"},
   {"1","AmazingFeature7/mesh/eye_part_faceu2992_mesh.mesh","f0bd06d5f2b43e6b9dcd019a69ff9d740a659a72ae401749dd9646394cb39c27"}};
  for(String[] s:specs)try(ZipFile z=new ZipFile(a[Integer.parseInt(s[0])])) {
   AuthoredMesh m=AuthoredMesh.readPinned(read(z.getInputStream(z.getEntry("materials/016/"+s[1]))),s[2]);
   if(m.positionSemantic!=(s[1].startsWith("AmazingFeature6/")?0:13)||m.positionComponents!=(s[1].startsWith("AmazingFeature6/")?3:2)||m.vertexCount>8192)throw new AssertionError("unsupported pinned position layout");
   System.out.println(s[1]+" semantic="+m.positionSemantic+" components="+m.positionComponents+" vertices="+m.vertexCount);
  }
 }
}

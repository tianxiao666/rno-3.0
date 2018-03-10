package com.iscreate.plat.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.zip.*;
import java.io.*;
public class ZipCommand {
     private static int ziplevel = 7;
     private static File sourceFile = null;
     private static File  zipFile = null;
     private static ZipOutputStream jos = null;
     private static ZipEntry sourEntry = null;
     private static byte[] buf = new byte[1024];
      
     public static void main(String[] s){
    	 List<String> sourcefilelist = new ArrayList<String>();
    	 sourcefilelist.add("f:/a.txt");
    	 sourcefilelist.add("f:/b.txt");
         AddtoZip(sourcefilelist,"bb.zip");
       // System.out.println( tz.AddtoZip("E:/cis-2000/document/CPoPUserSManual_CPoP_User_s_Manual.doc"));
     }
      
    public static String AddtoZip(List<String> sourcefilelist,String zipFileName){
        if(sourcefilelist == null || sourcefilelist.size()<1){
            return null;
        }
        sourceFile = new File(sourcefilelist.get(0).toString());
        if(!sourceFile.isFile()){
            return null;
        }else{
            //sourceFileName = sourceFile.getName();
            //this.setZipfileName(sourceFileName.substring(0,sourceFileName.lastIndexOf("."))+".zip");
            
            try{
            zipFile = new File(sourceFile.getParent(),zipFileName);
 
            if(zipFile.exists()){
                int i = 1 ;
                while(true){
                    zipFile = new File(sourceFile.getParent(),zipFileName.substring(0, zipFileName.lastIndexOf(".zip")) + i + ".zip");
                    if(!zipFile.exists()) break ;
                    i++ ;
                }
            }
            //System.out.println(zipFile.getPath());
             if(zipFile.exists()){
                 zipFile.deleteOnExit();
             }
             zipFile.createNewFile();
             jos = new ZipOutputStream(new FileOutputStream(zipFile));
             jos.setLevel(ziplevel);
             for(int i=0;i<sourcefilelist.size();i++){
             
             Stzip(jos,new File(sourcefilelist.get(i).toString()));
             }
             jos.finish();
            }catch(Exception e){
                return null;
            }
            if(zipFile !=null)
            return zipFile.getPath();
            else{
                return null;
            }
        }
    }
     
     private static  void Stzip(ZipOutputStream jos, File file)
     throws IOException, FileNotFoundException
 {
          
         sourEntry= new ZipEntry(file.getName());
         FileInputStream fin = new FileInputStream(file);
         BufferedInputStream in = new BufferedInputStream(fin);
         jos.putNextEntry(sourEntry);
         int len;
         while ((len = in.read(buf)) >= 0) 
             jos.write(buf, 0, len);
         in.close();
         jos.closeEntry();
          
      
 }
 
}
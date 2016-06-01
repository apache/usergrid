/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.usergrid.apm.util;

import japa.parser.JavaParser;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.ImportDeclaration;
import japa.parser.ast.PackageDeclaration;
import japa.parser.ast.body.BodyDeclaration;
import japa.parser.ast.body.ClassOrInterfaceDeclaration;
import japa.parser.ast.body.FieldDeclaration;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.body.TypeDeclaration;
import japa.parser.ast.expr.NameExpr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class JPADependencyRemover
{

   public static void main(String[] args) throws Exception {

      String destFolder = "/home/alanho/projects/WebManagerClientCore/src/com/ideawheel/webmanager/client/model/";
      //String destFolder = "/tmp/com/ideawheel/webmanager/client/model/";
      String destPackageName ="com.ideawheel.webmanager.client.model";
      String originFolder = "/home/alanho/workspace/InstaOps-portal/portal-model/src/main/java/com/ideawheel/portal/model/";
      //String originFolder = "/home/prabhat/ideawheel/workspace/trunk/portal-model/src/main/java/com/ideawheel/portal/model/";

      if (!new File(originFolder).exists())
         throw new RuntimeException("WTF! Not even a valid folder where source files are?");
      if (!new File(destFolder).exists()) {
         boolean success = new File (destFolder).mkdirs();
         if (success)
            System.out.println ("Created non existent destination directory");
      }
         
      
         
      List<String> fileNames = getFileNames (originFolder);

      for (String fileName: fileNames) {

         FileInputStream in = new FileInputStream(originFolder+ fileName);
         CompilationUnit cu;
         try {
            // parse the file
            cu = JavaParser.parse(in);
         } finally {
            in.close();
         }
         cu.setPackage(new PackageDeclaration(new NameExpr(destPackageName)));

         //clean hibernate and jpa dependency from imports and also update import with dest package name
         removeJPAImports(cu,destPackageName);

         //it currently remove all class level annotation
         removeAnnotationFromClass(cu);  
         
         //it currently removes all annotations from fields
         removeAnnotationFromVariables(cu);

         //it currently removes all annotations from methods 
         removeAnnotationFromMethods(cu);

         FileWriter writer = new FileWriter (destFolder+ fileName);
         writer.write(cu.toString());
         writer.close();
         System.out.println ("Just massaged : " + destFolder+fileName);
      }

      /* FileOutputStream out = new FileOutputStream(folder+ "/"+"ClientLog.java");
      out.write(cu.toString().toCharArray());
       */
      // prints the changed compilation unit
      //System.out.println(cu.toString());
   }

   private static void removeAnnotationFromClass(CompilationUnit cu) {

      List<TypeDeclaration> types = cu.getTypes();
      for (TypeDeclaration type : types) {
         if (type instanceof ClassOrInterfaceDeclaration) {
            type.setAnnotations(null);
         }
      }

   }

   private static void removeJPAImports(CompilationUnit cu, String destPackageName)  {
      List<ImportDeclaration> imports = cu.getImports();
      if (imports == null)
         return; //nothing to do
      List<ImportDeclaration> newImports = new ArrayList <ImportDeclaration> ();
      for (ImportDeclaration imp: imports) {
         if (!(imp.getName().toString().contains("persistence")
               || imp.getName().toString().contains("hibernate") 
               || imp.getName().toString().contains("seam")))
            newImports.add (imp);
         if(imp.getName().toString().contains("com.ideawheel.portal.model")) {            
            String temp = imp.getName().toString();
            System.out.println ("old import " + temp.toString());
            String temp1 = temp.replace("com.ideawheel.portal.model", destPackageName);
            imp.setName(new NameExpr(temp1.toString()));
            System.out.println ("New import " + imp.getName().toString());
         }
            

      }
      cu.setImports(newImports);

   }
   
   private static void removeAnnotationFromVariables(CompilationUnit cu) {
      
      List<TypeDeclaration> types = cu.getTypes();
      for (TypeDeclaration type : types) {
         List<BodyDeclaration> members = type.getMembers();
         for (BodyDeclaration member : members) {
            if (member instanceof FieldDeclaration) {
               member.setAnnotations(null);
            }
         }
      }
   }

   private static void removeAnnotationFromMethods(CompilationUnit cu) {
      List<TypeDeclaration> types = cu.getTypes();
      for (TypeDeclaration type : types) {
         List<BodyDeclaration> members = type.getMembers();
         for (BodyDeclaration member : members) {
            if (member instanceof MethodDeclaration) {
               MethodDeclaration method = (MethodDeclaration) member;
               removeAnnotationFromMethod(method);
            }
         }
      }
   }

   private static void removeAnnotationFromMethod(MethodDeclaration n) {
      n.setAnnotations(null);
      // change the name of the method to upper case
      /*n.setName(n.getName().toUpperCase());

      // create the new parameter
      Parameter newArg = ASTHelper.createParameter(ASTHelper.INT_TYPE, "value");

      // add the parameter to the method
      ASTHelper.addParameter(n, newArg);
       */  }

   private static List<String> getFileNames (String originFolder) {
      List<String> fileNames = new ArrayList<String>();
      File folder = new File(originFolder);
      File[] listOfFiles = folder.listFiles();     
      for (int i = 0; i < listOfFiles.length; i++) {
         if (listOfFiles[i].isFile() && listOfFiles[i].getName().contains(".java")) {         
            fileNames.add(listOfFiles[i].getName());

         }
      }
      return fileNames;

   }


}

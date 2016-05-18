package org.apache.usergrid.apm.model;


import java.io.Serializable;

public class User implements Serializable
{
   //this is a temp object for prototyping purposes which will be a table soon...
   private Long id;
   private String userName;
   private String firstName;

   public Long getId()
   {
      return id;
   }

   public void setId(Long id)
   {
      this.id = id;
   }

   public String getUserName()
   {
      return userName;
   }

   public void setUserName(String userName)
   {
      this.userName = userName;
   }

   public String getFirstName()
   {
      return firstName;
   }

   public void setFirstName(String firstName)
   {
      this.firstName = firstName;
   }

}

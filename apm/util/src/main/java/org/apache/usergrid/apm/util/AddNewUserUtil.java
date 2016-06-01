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



public class AddNewUserUtil {

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		String email;
		String firstName;
		String lastName;
		System.out.println();
		System.out.println ("Expects following params in order : emailAddress firstName lastName");
		System.out.println ("Number of params " + args.length);

		if (args.length < 3) {
			System.out.println ("Wrong inputs");
			return;
		}
		else
		{
			System.out.println("Going to add following user");
			email = args[0];
			firstName = args[1];
			lastName = args[2];
			

			System.out.println("with email: " + email + " having first name " + firstName + " last name " + lastName);			



		/*	UserService userService = ServiceFactory.getUserService();
			User user1 = null;
			if (userService.getUserByEmail(email) == null) {
				user1 = new User();
				user1.setEmail(email);
				user1.setFirstName(firstName);
				user1.setLastName(lastName);
				user1.setPasswordHash("InstaOpsAgent");
				userService.createInternalUser(user1);	
				System.out.println("User with email " + email + " created");
			}
			else {
				System.out.println ("User with that name already exists !!");
			}*/
		} 

	}
}

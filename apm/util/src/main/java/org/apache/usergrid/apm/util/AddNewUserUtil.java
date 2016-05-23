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
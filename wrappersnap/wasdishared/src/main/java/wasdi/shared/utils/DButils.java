package wasdi.shared.utils;


import java.util.ArrayList;

import wasdi.shared.business.User;
import wasdi.shared.data.UserRepository;

/**
 * Created by Cristiano Nattero on 2018.10.29
 */

public class DButils {

	//used for updating users so that the validation flag for the registration is enabled
	public static void addRegistrationValidationFlag() {
		UserRepository oUrepo = new UserRepository();
		ArrayList<User> aoExistingUsers = oUrepo.getAllUsers();
		for (User oUser : aoExistingUsers) {
			if( null == oUser.getFirstAccessValidated() ) {
				oUser.setFirstAccessValidated(true);
			}
		}
		oUrepo.UpdateAllUsers(aoExistingUsers);
	}
	
}

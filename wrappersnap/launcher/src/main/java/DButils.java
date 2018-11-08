import java.util.ArrayList;

//import it.fadeout.business.PasswordAuthentication;
import wasdi.ConfigReader;
import wasdi.shared.business.User;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.UserRepository;
import java.util.ArrayList;

public class DButils {

	
	//PasswordAuthentication m_oPasswordAuthentication = new PasswordAuthentication();
	
	
	/*
	private void updatePasswordInDB(UserRepository oUserRepository)
	{
		//update password
		ArrayList<User> aoUsers = oUserRepository.getAllUsers();
		aoUsers = UpdateHashUsersPassword(aoUsers);
		oUserRepository.UpdateAllUsers(aoUsers);
	}
	*/
	
	/*
	private ArrayList<User> UpdateHashUsersPassword(ArrayList<User> aoUsers)
	{
		for (int i = 0; i < aoUsers.size(); i++) 
		{
			User oUser = aoUsers.get(i);
			if( oUser.getAuthServiceProvider() == null || oUser.getAuthServiceProvider().contains("google") == false)
			{
				oUser.setPassword(m_oPasswordAuthentication.hash(oUser.getPassword().toCharArray()));
			}
			
		}
		return aoUsers;
	}
	*/

	
	public static void main(String[] args) {
		
        //call methods from here
	}

}

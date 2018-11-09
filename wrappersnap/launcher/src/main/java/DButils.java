import java.io.IOException;
import java.util.ArrayList;

//import it.fadeout.business.PasswordAuthentication;
import wasdi.ConfigReader;
import wasdi.shared.business.PasswordAuthentication;
import wasdi.shared.business.User;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.UserRepository;
import java.util.ArrayList;

public class DButils {

	
	PasswordAuthentication m_oPasswordAuthentication = new PasswordAuthentication();
	
	
	
	private void updatePasswordInDB(UserRepository oUserRepository)
	{
		//update password
		ArrayList<User> aoUsers = oUserRepository.getAllUsers();
		aoUsers = UpdateHashUsersPassword(aoUsers);
		oUserRepository.UpdateAllUsers(aoUsers);
	}
	
	
	
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
	

	
	public static void main(String[] args) {
		
        //call methods from here
		
		try {
			MongoRepository.SERVER_ADDRESS = ConfigReader.getPropValue("MONGO_ADDRESS");
	        MongoRepository.SERVER_PORT = Integer.parseInt(ConfigReader.getPropValue("MONGO_PORT"));
	        MongoRepository.DB_NAME = ConfigReader.getPropValue("MONGO_DBNAME");
	        MongoRepository.DB_USER = ConfigReader.getPropValue("MONGO_DBUSER");
	        MongoRepository.DB_PWD = ConfigReader.getPropValue("MONGO_DBPWD");
	        
			DButils oDbUtils = new DButils();
			// P.Campanella 09/11/2018: lo ho fatto girare su ufficiale. Ora è allienato
			// Commento per sicurezza
			//oDbUtils.updatePasswordInDB(new UserRepository());
			
		} catch (Exception e) {
			e.printStackTrace();
		}
				
	}

}

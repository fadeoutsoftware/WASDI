import wasdi.ConfigReader;
import wasdi.shared.data.MongoRepository;

public class ZZZTemplateMain {

	
	public static void sampleMethod() {
		System.out.println("sample method running");
	}
		
	public static void main(String[] args) {
		
        try {
        	//this is how you read parameters:
			MongoRepository.SERVER_ADDRESS = ConfigReader.getPropValue("MONGO_ADDRESS");
	        MongoRepository.SERVER_PORT = Integer.parseInt(ConfigReader.getPropValue("MONGO_PORT"));
	        MongoRepository.DB_NAME = ConfigReader.getPropValue("MONGO_DBNAME");
	        MongoRepository.DB_USER = ConfigReader.getPropValue("MONGO_DBUSER");
	        MongoRepository.DB_PWD = ConfigReader.getPropValue("MONGO_DBPWD");
			
	        sampleMethod();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}

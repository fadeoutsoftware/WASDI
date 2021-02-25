/**
 * 
 */
package stats;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import org.json.JSONObject;

import wasdi.ConfigReader;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.data.UserRepository;

/**
 * @author c.nattero
 * 
 * This class calculates some basic stats about the usage of WASDI
 *
 */
public class WasdiStats {


	/**
	 * Entry point
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			MongoRepository.SERVER_ADDRESS = ConfigReader.getPropValue("MONGO_ADDRESS");
			MongoRepository.SERVER_PORT = Integer.parseInt(ConfigReader.getPropValue("MONGO_PORT"));
			MongoRepository.DB_NAME = ConfigReader.getPropValue("MONGO_DBNAME");
			MongoRepository.DB_USER = ConfigReader.getPropValue("MONGO_DBUSER");
			MongoRepository.DB_PWD = ConfigReader.getPropValue("MONGO_DBPWD");
		} catch (IOException e) {
			e.printStackTrace();
		}


		WasdiStats oStatsCalculator = new WasdiStats();		
		JSONObject oStats = new JSONObject();
		
		//stats on processors
		oStatsCalculator.measureApplications(oStats);
		
		//number of users
		oStatsCalculator.measureUsers(oStats);
		
	
		
		//running time:
		//total duration of computation
		//running minutes per application
		//running time per application per server
		//running time per workspace
		//running time per user
		//running time per user per server
		//...
		
		//used storage:
		//total | downloaded | created
		//total | downloaded | created per server
		//total | downloaded | created per workspace
		//total | downloaded | created per user
		//total | downloaded | created per user per server

		
		//download images per application (if possible, using parent id)
		//number of unique users that used each application

		
		System.out.println(oStats.toString(2));
		
		File oFile = new File("stats.json");
		try(Writer output = new BufferedWriter(new FileWriter(oFile));){
			output.write(oStats.toString());
		} catch (IOException oE) {
			oE.printStackTrace();
		}
	}


	private void measureApplications(JSONObject oStats) {
		try {
			ProcessorRepository oProcessorRepository = new ProcessorRepository();
			JSONObject oProcessorsMain = new JSONObject();

			JSONObject oProcessors = new JSONObject();
			long lTotalProcessors = oProcessorRepository.countProcessors();
			oProcessors.put("total", lTotalProcessors);

			long lPublicProcessors = oProcessorRepository.countProcessors(true);
			oProcessors.put("public", lPublicProcessors);

			long lPrivateProcessors = lTotalProcessors - lPublicProcessors;
			oProcessors.put("private", lPrivateProcessors);

			JSONObject oInStoreProcessors = new JSONObject();
			long lInAppStore = oProcessorRepository.countProcessors(true, false);
			oInStoreProcessors.put("total", lInAppStore);

			long lPublicInAppStore = oProcessorRepository.countProcessors(true, true);
			oInStoreProcessors.put("public", lPublicInAppStore);

			long lPrivateInAppStore = lInAppStore - lPublicInAppStore;
			oInStoreProcessors.put("private", lPrivateInAppStore);

			oProcessorsMain.put("processors", oProcessors);
			oProcessorsMain.put("in app store", oInStoreProcessors);

			oStats.put("processors", oProcessorsMain);
		}
		catch (Exception oE) {
			System.out.println("calculateApplications: " + oE);
		}
	}

	private void measureUsers(JSONObject oStats) {
		JSONObject oUsers = new JSONObject();
		UserRepository oUserRepository = new UserRepository();
		oUsers.put("users", oUserRepository.countUsers());
		oStats.put("users", oUsers);
	}

}

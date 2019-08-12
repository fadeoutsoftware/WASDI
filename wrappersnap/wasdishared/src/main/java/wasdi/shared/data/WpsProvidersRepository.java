/*
 * WPS providers entity
 * 
 * Created by Cristiano Nattero on 2018.11.16
 * 
 * Fadeout software
 * 
 */


package wasdi.shared.data;

import java.util.ArrayList;

import org.bson.Document;

import com.mongodb.client.FindIterable;

import wasdi.shared.business.WpsProvider;
import wasdi.shared.utils.AuthenticationCredentials;
import wasdi.shared.utils.Utils;

public class WpsProvidersRepository extends MongoRepository {

	public ArrayList<WpsProvider> getWpsList(){
		try {
			FindIterable<Document> aoWPSprovidersDocumentList = getCollection("wpsProviders").find();
			if (aoWPSprovidersDocumentList != null) {
				ArrayList<WpsProvider> aoResult = new ArrayList<WpsProvider>();
				for (Document oDocument : aoWPSprovidersDocumentList) {
					if(null!=oDocument) {
						String sJSON = oDocument.toJson();
						if(null!=sJSON) {
							WpsProvider oWPS = null;
							try {
								//FIXME null values
								oWPS = s_oMapper.readValue(sJSON, WpsProvider.class);
							} catch (Exception e) {
								e.printStackTrace();
							}
							if(null!=oWPS) {
								aoResult.add(oWPS);
							}
						}
					}
				}
				return aoResult;
			}

		} catch (Exception oEx) {
			oEx.printStackTrace();
		}

		return null;
	}

	public WpsProvider getProvider(String sProviderName) {
		Utils.debugLog("WpsProviderRepository.getProvider");
		if(null==sProviderName) {
			throw new NullPointerException("WpsProviderRepository.getProvider: null String passed");
		}
		WpsProvider oWpsProvider = null;
		try {
			Document oWpsProviderDocument = getCollection("wpsProviders").find(new Document("provider", sProviderName)).first();
			String sJson = oWpsProviderDocument.toJson();
			oWpsProvider = s_oMapper.readValue(sJson, WpsProvider.class); 
		}catch (Exception e) {
			e.printStackTrace();
		}
		return oWpsProvider;
	}
	
	public String getProviderUrl(String sProviderName) {
		Utils.debugLog("WpsProviderRepository.getProviderUrl");
		if(null==sProviderName) {
			throw new NullPointerException("WpsProviderRepository.getProviderUrl: null String passed");
		}
		WpsProvider oProvider = getProvider(sProviderName);
		if(null!=oProvider) {
			return oProvider.getProviderUrl();
		} else {
			return null;
		}
	}
	
	public AuthenticationCredentials getCredentials(String sProviderName) {
		Utils.debugLog("WpsProvidersRepository");
		if(null==sProviderName) {
			throw new NullPointerException("WpsProviderRepository.getProviderUrl: null String passed");
		}
		WpsProvider oProvider = getProvider(sProviderName);
		if(null == oProvider) {
			return null;
		}
		if(null == oProvider.getUsername() || null == oProvider.getPassword()) {
			return null;
		}
		AuthenticationCredentials oCredentials = new AuthenticationCredentials(oProvider.getUsername(), oProvider.getPassword());
		return oCredentials;
	}
}

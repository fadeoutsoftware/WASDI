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
}

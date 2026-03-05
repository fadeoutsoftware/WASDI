package wasdi.shared.data.no2;

import static org.dizitart.no2.filters.FluentFilter.where;

import java.util.ArrayList;
import java.util.List;

import org.dizitart.no2.collection.Document;
import org.dizitart.no2.collection.NitriteCollection;

import wasdi.shared.business.PublishedBand;
import wasdi.shared.data.interfaces.IPublishedBandsRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * NO2 backend implementation for published bands repository.
 */
public class No2PublishedBandsRepositoryBackend extends No2Repository implements IPublishedBandsRepositoryBackend {

	private static final String s_sCollectionName = "publishedbands";

	@Override
	public boolean insertPublishedBand(PublishedBand oFile) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null || oFile == null) {
				return false;
			}
			oCollection.insert(toDocument(oFile));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2PublishedBandsRepositoryBackend.insertPublishedBand", oEx);
		}
		return false;
	}

	@Override
	public PublishedBand getPublishedBand(String sProductName, String sBandName) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return null;
			}
			for (Document oDocument : oCollection.find(where("productName").eq(sProductName))) {
				PublishedBand oBand = fromDocument(oDocument, PublishedBand.class);
				if (oBand != null && equalsSafe(oBand.getBandName(), sBandName)) {
					return oBand;
				}
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2PublishedBandsRepositoryBackend.getPublishedBand", oEx);
		}
		return null;
	}

	@Override
	public List<PublishedBand> getPublishedBandsByProductName(String sProductName) {
		List<PublishedBand> aoReturnList = new ArrayList<>();
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection != null) {
				aoReturnList = toList(oCollection.find(where("productName").eq(sProductName)), PublishedBand.class);
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2PublishedBandsRepositoryBackend.getPublishedBandsByProductName", oEx);
		}
		return aoReturnList;
	}

	@Override
	public List<PublishedBand> getList() {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			return toList(oCollection != null ? oCollection.find() : null, PublishedBand.class);
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2PublishedBandsRepositoryBackend.getList", oEx);
		}
		return new ArrayList<>();
	}

	@Override
	public int deleteByProductName(String sProductName) {
		if (Utils.isNullOrEmpty(sProductName)) {
			return 0;
		}

		int iCount = 0;
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return 0;
			}
			for (Document oDoc : oCollection.find(where("productName").eq(sProductName))) {
				if (oDoc != null) {
					iCount++;
				}
			}
			oCollection.remove(where("productName").eq(sProductName));
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2PublishedBandsRepositoryBackend.deleteByProductName", oEx);
		}
		return iCount;
	}

	@Override
	public int deleteByProductNameLayerId(String sProductName, String sLayerId) {
		if (Utils.isNullOrEmpty(sProductName) || Utils.isNullOrEmpty(sLayerId)) {
			return 0;
		}

		int iCount = 0;
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return 0;
			}
			for (Document oDoc : oCollection.find(where("productName").eq(sProductName))) {
				PublishedBand oBand = fromDocument(oDoc, PublishedBand.class);
				if (oBand != null && equalsSafe(oBand.getLayerId(), sLayerId)) {
					iCount = 1;
					Object oId = oDoc.get("_id");
					if (oId != null) {
						oCollection.remove(where("_id").eq(oId));
					}
					break;
				}
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2PublishedBandsRepositoryBackend.deleteByProductNameLayerId", oEx);
		}
		return iCount;
	}

	private boolean equalsSafe(String sA, String sB) {
		return sA == null ? sB == null : sA.equals(sB);
	}
}

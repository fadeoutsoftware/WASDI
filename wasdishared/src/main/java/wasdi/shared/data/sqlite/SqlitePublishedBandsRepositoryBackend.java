package wasdi.shared.data.sqlite;

import java.util.ArrayList;
import java.util.List;

import wasdi.shared.business.PublishedBand;
import wasdi.shared.data.interfaces.IPublishedBandsRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * SQLite backend implementation for published bands repository.
 */
public class SqlitePublishedBandsRepositoryBackend extends SqliteRepository implements IPublishedBandsRepositoryBackend {

	public SqlitePublishedBandsRepositoryBackend() {
		m_sThisCollection = "publishedbands";
		this.ensureTable(m_sThisCollection);
	}

	@Override
	public boolean insertPublishedBand(PublishedBand oFile) {
		try {
			String sKey = oFile.getProductName() + "_" + oFile.getBandName();
			insert(sKey, oFile);
			return true;
		} catch (Exception oEx) {
			WasdiLog.errorLog("PublishedBandsRepository.insertPublishedBand: exception ", oEx);
		}
		return false;
	}

	@Override
	public PublishedBand getPublishedBand(String sProductName, String sBandName) {
		try {
			return queryOne(
					"SELECT data FROM " + m_sThisCollection +
					" WHERE json_extract(data,'$.productName') = ? AND json_extract(data,'$.bandName') = ?",
					new Object[]{sProductName, sBandName}, PublishedBand.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("PublishedBandsRepository.getPublishedBand: exception ", oEx);
		}
		return null;
	}

	@Override
	public List<PublishedBand> getPublishedBandsByProductName(String sProductName) {
		try {
			return findAllWhere("productName", sProductName, PublishedBand.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("PublishedBandsRepository.getPublishedBandsByProductName: exception ", oEx);
		}
		return new ArrayList<>();
	}

	@Override
	public List<PublishedBand> getList() {
		try {
			return findAll(PublishedBand.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("PublishedBandsRepository.getList: exception ", oEx);
		}
		return new ArrayList<>();
	}

	@Override
	public int deleteByProductName(String sProductName) {
		if (Utils.isNullOrEmpty(sProductName)) {
			return 0;
		}
		try {
			return deleteWhere("productName", sProductName);
		} 
		catch (Exception oEx) {
			WasdiLog.errorLog("PublishedBandsRepository.deleteByProductName: exception ", oEx);
		}
		return 0;
	}

	@Override
	public int deleteByProductNameLayerId(String sProductName, String sLayerId) {
		if (Utils.isNullOrEmpty(sProductName) || Utils.isNullOrEmpty(sLayerId)) {
			return 0;
		}
		try {
			return execute(
					"DELETE FROM " + m_sThisCollection +
					" WHERE json_extract(data,'$.productName') = ? AND json_extract(data,'$.layerId') = ?",
					new Object[]{sProductName, sLayerId});
		} catch (Exception oEx) {
			WasdiLog.errorLog("PublishedBandsRepository.deleteByProductNameLayerId: exception ", oEx);
		}
		return 0;
	}
}

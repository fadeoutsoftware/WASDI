package wasdi.shared.data.sqlite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import wasdi.shared.business.processors.Processor;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.interfaces.IProcessorRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class SqliteProcessorRepositoryBackend extends SqliteRepository implements IProcessorRepositoryBackend {

	public SqliteProcessorRepositoryBackend() {
		m_sThisCollection = "processors";
		this.ensureTable(m_sThisCollection);
	}

	@Override
	public boolean insertProcessor(Processor oProcessor) {
		try {
			return insert(m_sThisCollection, oProcessor.getProcessorId(), oProcessor);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorRepository.insertProcessor :error ", oEx);
		}

		return false;
	}

	@Override
	public Processor getProcessor(String sProcessorId) {
		try {
			return findOneWhere(m_sThisCollection, "processorId", sProcessorId, Processor.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorRepository.getProcessor :error ", oEx);
		}

		return null;
	}

	@Override
	public Processor getProcessorByName(String sName) {
		if (Utils.isNullOrEmpty(sName)) {
			return null;
		}

		try {
			return findOneWhere(m_sThisCollection, "name", sName, Processor.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorRepository.getProcessorByName :error ", oEx);
		}

		return null;
	}

	@Override
	public boolean updateProcessor(Processor oProcessor) {
		try {
			return updateWhere(m_sThisCollection, "processorId", oProcessor.getProcessorId(), oProcessor);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorRepository.updateProcessor :error ", oEx);
		}

		return false;
	}

	@Override
	public List<Processor> getProcessorByUser(String sUserId) {
		try {
			return findAllWhere(m_sThisCollection, "userId", sUserId, Processor.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorRepository.getProcessorByUser :error ", oEx);
		}

		return new ArrayList<>();
	}

	@Override
	public List<Processor> findProcessorsByPartialName(String sPartialName) {
		List<Processor> aoReturnList = new ArrayList<>();

		if (Utils.isNullOrEmpty(sPartialName) || sPartialName.length() < 3) {
			return aoReturnList;
		}

		try {
			String sPattern = "%" + sPartialName + "%";
			String sQuery = "SELECT data FROM " + m_sThisCollection
					+ " WHERE LOWER(json_extract(data,'$.processorId')) LIKE LOWER(?)"
					+ " OR LOWER(json_extract(data,'$.name')) LIKE LOWER(?)"
					+ " OR LOWER(json_extract(data,'$.friendlyName')) LIKE LOWER(?)"
					+ " OR LOWER(json_extract(data,'$.description')) LIKE LOWER(?)"
					+ " ORDER BY json_extract(data,'$.name') ASC";
			return queryList(sQuery, Arrays.asList(sPattern, sPattern, sPattern, sPattern), Processor.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorRepository.findProcessorsByPartialName :error ", oEx);
		}

		return aoReturnList;
	}

	@Override
	public int getNextProcessorPort() {
		int iPort = -1;

		try {
			String sQuery = "SELECT data FROM " + m_sThisCollection
					+ " ORDER BY json_extract(data,'$.port') DESC LIMIT 1";
			Processor oProcessor = queryOne(sQuery, Arrays.asList(), Processor.class);
			if (oProcessor != null) {
				iPort = oProcessor.getPort();
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorRepository.getNextProcessorPort :error ", oEx);
		}

		if (iPort == -1) {
			iPort = WasdiConfig.Current.dockers.processorsInternalPort;
		} else {
			iPort++;
		}

		return iPort;
	}

	@Override
	public boolean deleteProcessor(String sProcessorId) {
		if (Utils.isNullOrEmpty(sProcessorId)) {
			return false;
		}

		try {
			int iDeleteCount = deleteWhere(m_sThisCollection, "processorId", sProcessorId);
			return iDeleteCount == 1;
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorRepository.deleteProcessor :error ", oEx);
		}

		return false;
	}

	@Override
	public int deleteProcessorByUser(String sUserId) {
		if (Utils.isNullOrEmpty(sUserId)) {
			return 0;
		}

		try {
			int iCount = (int) countWhere(m_sThisCollection, "userId", sUserId);
			deleteWhere(m_sThisCollection, "userId", sUserId);
			return iCount;
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorRepository.deleteProcessorByUser :error ", oEx);
		}

		return 0;
	}

	@Override
	public List<Processor> getDeployedProcessors() {
		return getDeployedProcessors("_id");
	}

	@Override
	public List<Processor> getDeployedProcessors(String sOrderBy) {
		return getDeployedProcessors(sOrderBy, 1);
	}

	@Override
	public List<Processor> getDeployedProcessors(String sOrderBy, int iDirection) {
		try {
			String sDir = (iDirection >= 0) ? "ASC" : "DESC";
			String sSortExpr = "_id".equals(sOrderBy) ? "id" : "json_extract(data,'$." + sOrderBy + "')";
			String sQuery = "SELECT data FROM " + m_sThisCollection + " ORDER BY " + sSortExpr + " " + sDir;
			return queryList(sQuery, Arrays.asList(), Processor.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorRepository.getDeployedProcessors :error ", oEx);
		}

		return new ArrayList<>();
	}

	@Override
	public long countProcessors() {
		return count(m_sThisCollection);
	}

	@Override
	public long countProcessors(boolean bPublicOnly) {
		return countProcessors(false, bPublicOnly);
	}

	@Override
	public long countProcessors(boolean bInAppStoreOnly, boolean bPublicOnly) {
		try {
			if (!bPublicOnly && !bInAppStoreOnly) {
				return count(m_sThisCollection);
			}

			StringBuilder oSb = new StringBuilder("SELECT COUNT(*) FROM " + m_sThisCollection + " WHERE 1=1");
			List<Object> aoParams = new ArrayList<>();

			if (bPublicOnly) {
				oSb.append(" AND json_extract(data,'$.isPublic') = 1");
			}
			if (bInAppStoreOnly) {
				oSb.append(" AND json_extract(data,'$.showInStore') = 1");
			}

			String sResult = queryOne(oSb.toString(), aoParams, String.class);
			return sResult != null ? Long.parseLong(sResult) : -1L;
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorRepository.countProcessors( " + bInAppStoreOnly + ", " + bPublicOnly + " ): ", oEx);
		}
		return -1L;
	}
}

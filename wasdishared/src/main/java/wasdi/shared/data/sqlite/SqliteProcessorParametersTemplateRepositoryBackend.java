package wasdi.shared.data.sqlite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import wasdi.shared.business.processors.ProcessorParametersTemplate;
import wasdi.shared.data.interfaces.IProcessorParametersTemplateRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class SqliteProcessorParametersTemplateRepositoryBackend extends SqliteRepository implements IProcessorParametersTemplateRepositoryBackend {

	public SqliteProcessorParametersTemplateRepositoryBackend() {
		m_sThisCollection = "processorParametersTemplates";
		this.ensureTable(m_sThisCollection);
	}

	@Override
	public List<ProcessorParametersTemplate> getProcessorParametersTemplatesByUser(String sUserId) {
		try {
			String sQuery = "SELECT data FROM " + m_sThisCollection
					+ " WHERE json_extract(data,'$.userId') = ?"
					+ " ORDER BY json_extract(data,'$.name') ASC";
			return queryList(sQuery, Arrays.asList(sUserId), ProcessorParametersTemplate.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorParametersTemplateRepository.getProcessorParametersTemplatesByUser :error ", oEx);
		}

		return new ArrayList<>();
	}

	@Override
	public List<ProcessorParametersTemplate> getProcessorParametersTemplatesByUserAndProcessor(String sUserId, String sProcessorId) {
		try {
			String sQuery = "SELECT data FROM " + m_sThisCollection
					+ " WHERE json_extract(data,'$.userId') = ?"
					+ " AND json_extract(data,'$.processorId') = ?"
					+ " ORDER BY json_extract(data,'$.name') ASC";
			return queryList(sQuery, Arrays.asList(sUserId, sProcessorId), ProcessorParametersTemplate.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorParametersTemplateRepository.getProcessorParametersTemplatesByUserAndProcessor :error ", oEx);
		}

		return new ArrayList<>();
	}

	@Override
	public List<ProcessorParametersTemplate> getProcessorParametersTemplatesByProcessor(String sProcessorId) {
		try {
			String sQuery = "SELECT data FROM " + m_sThisCollection
					+ " WHERE json_extract(data,'$.processorId') = ?"
					+ " ORDER BY json_extract(data,'$.name') ASC";
			return queryList(sQuery, Arrays.asList(sProcessorId), ProcessorParametersTemplate.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorParametersTemplateRepository.getProcessorParametersTemplatesByProcessor :error ", oEx);
		}

		return new ArrayList<>();
	}

	@Override
	public ProcessorParametersTemplate getProcessorParametersTemplateByTemplateId(String sTemplateId) {
		try {
			return findOneWhere(m_sThisCollection, "templateId", sTemplateId, ProcessorParametersTemplate.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorParametersTemplateRepository.getProcessorParametersTemplateByTemplateId :error ", oEx);
		}

		return null;
	}

	@Override
	public ProcessorParametersTemplate getProcessorParametersTemplatesByUserAndProcessorAndName(String sUserId, String sProcessorId, String sName) {
		try {
			LinkedHashMap<String, Object> oFilter = new LinkedHashMap<>();
			oFilter.put("userId", sUserId);
			oFilter.put("processorId", sProcessorId);
			oFilter.put("name", sName);
			return findOneWhere(m_sThisCollection, oFilter, ProcessorParametersTemplate.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorParametersTemplateRepository.getProcessorParametersTemplatesByUserAndProcessorAndName :error ", oEx);
		}

		return null;
	}

	@Override
	public String insertProcessorParametersTemplate(ProcessorParametersTemplate oProcessorParametersTemplate) {
		try {
			if (oProcessorParametersTemplate == null) {
				WasdiLog.debugLog("ProcessorParametersTemplateRepository.InsertProcessorParametersTemplate: oProcessorParametersTemplate is null");
				return null;
			}

			insert(m_sThisCollection, oProcessorParametersTemplate.getTemplateId(), oProcessorParametersTemplate);
			return oProcessorParametersTemplate.getTemplateId();
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorParametersTemplateRepository.InsertProcessorParametersTemplate: ", oEx);
		}

		return "";
	}

	@Override
	public int deleteByTemplateId(String sTemplateId) {
		if (Utils.isNullOrEmpty(sTemplateId)) {
			return 0;
		}

		try {
			int iCount = (int) countWhere(m_sThisCollection, "templateId", sTemplateId);
			deleteWhere(m_sThisCollection, "templateId", sTemplateId);
			return iCount;
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorParametersTemplateRepository.deleteByTemplateId:error ", oEx);
			return -1;
		}
	}

	@Override
	public boolean updateProcessorParametersTemplate(ProcessorParametersTemplate oProcessorParametersTemplate) {
		try {
			return updateWhere(m_sThisCollection, "templateId", oProcessorParametersTemplate.getTemplateId(), oProcessorParametersTemplate);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorParametersTemplateRepository.updateProcessorParametersTemplate:error ", oEx);
			return false;
		}
	}

	@Override
	public boolean isTheOwnerOfTheTemplate(String sTemplateId, String sUserId) {
		try {
			LinkedHashMap<String, Object> oFilter = new LinkedHashMap<>();
			oFilter.put("templateId", sTemplateId);
			oFilter.put("userId", sUserId);
			return countWhere(m_sThisCollection, oFilter) > 0;
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorParametersTemplateRepository.isTheOwnerOfTheTemplate :error ", oEx);
		}

		return false;
	}

	@Override
	public int deleteByUserId(String sUserId) {
		if (Utils.isNullOrEmpty(sUserId)) {
			return 0;
		}

		try {
			int iCount = (int) countWhere(m_sThisCollection, "userId", sUserId);
			deleteWhere(m_sThisCollection, "userId", sUserId);
			return iCount;
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorParametersTemplateRepository.deleteByUserId: error ", oEx);
			return -1;
		}
	}
}


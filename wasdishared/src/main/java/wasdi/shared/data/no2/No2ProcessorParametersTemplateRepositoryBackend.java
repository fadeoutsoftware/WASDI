package wasdi.shared.data.no2;

import static org.dizitart.no2.filters.FluentFilter.where;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.dizitart.no2.collection.Document;
import org.dizitart.no2.collection.NitriteCollection;

import wasdi.shared.business.processors.ProcessorParametersTemplate;
import wasdi.shared.data.interfaces.IProcessorParametersTemplateRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * NO2 backend implementation for processor parameters template repository.
 */
public class No2ProcessorParametersTemplateRepositoryBackend extends No2Repository implements IProcessorParametersTemplateRepositoryBackend {

	private static final String s_sCollectionName = "processorParametersTemplates";

	@Override
	public List<ProcessorParametersTemplate> getProcessorParametersTemplatesByUser(String sUserId) {
		List<ProcessorParametersTemplate> aoReturnList = new ArrayList<>();
		for (ProcessorParametersTemplate oTemplate : getAllTemplates()) {
			if (oTemplate != null && equalsSafe(oTemplate.getUserId(), sUserId)) {
				aoReturnList.add(oTemplate);
			}
		}
		aoReturnList.sort(Comparator.comparing(ProcessorParametersTemplate::getName, Comparator.nullsLast(String::compareToIgnoreCase)));
		return aoReturnList;
	}

	@Override
	public List<ProcessorParametersTemplate> getProcessorParametersTemplatesByUserAndProcessor(String sUserId, String sProcessorId) {
		List<ProcessorParametersTemplate> aoReturnList = new ArrayList<>();
		for (ProcessorParametersTemplate oTemplate : getAllTemplates()) {
			if (oTemplate != null && equalsSafe(oTemplate.getUserId(), sUserId) && equalsSafe(oTemplate.getProcessorId(), sProcessorId)) {
				aoReturnList.add(oTemplate);
			}
		}
		aoReturnList.sort(Comparator.comparing(ProcessorParametersTemplate::getName, Comparator.nullsLast(String::compareToIgnoreCase)));
		return aoReturnList;
	}

	@Override
	public List<ProcessorParametersTemplate> getProcessorParametersTemplatesByProcessor(String sProcessorId) {
		List<ProcessorParametersTemplate> aoReturnList = new ArrayList<>();
		for (ProcessorParametersTemplate oTemplate : getAllTemplates()) {
			if (oTemplate != null && equalsSafe(oTemplate.getProcessorId(), sProcessorId)) {
				aoReturnList.add(oTemplate);
			}
		}
		aoReturnList.sort(Comparator.comparing(ProcessorParametersTemplate::getName, Comparator.nullsLast(String::compareToIgnoreCase)));
		return aoReturnList;
	}

	@Override
	public ProcessorParametersTemplate getProcessorParametersTemplateByTemplateId(String sTemplateId) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return null;
			}
			for (Document oDoc : oCollection.find(where("templateId").eq(sTemplateId))) {
				return fromDocument(oDoc, ProcessorParametersTemplate.class);
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProcessorParametersTemplateRepositoryBackend.getProcessorParametersTemplateByTemplateId", oEx);
		}
		return null;
	}

	@Override
	public ProcessorParametersTemplate getProcessorParametersTemplatesByUserAndProcessorAndName(String sUserId, String sProcessorId, String sName) {
		for (ProcessorParametersTemplate oTemplate : getAllTemplates()) {
			if (oTemplate != null
					&& equalsSafe(oTemplate.getUserId(), sUserId)
					&& equalsSafe(oTemplate.getProcessorId(), sProcessorId)
					&& equalsSafe(oTemplate.getName(), sName)) {
				return oTemplate;
			}
		}
		return null;
	}

	@Override
	public String insertProcessorParametersTemplate(ProcessorParametersTemplate oProcessorParametersTemplate) {
		try {
			if (oProcessorParametersTemplate == null) {
				WasdiLog.debugLog("No2ProcessorParametersTemplateRepositoryBackend.insert: template is null");
				return null;
			}
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return "";
			}
			Document oDocument = toDocument(oProcessorParametersTemplate);
			oCollection.insert(oDocument);
			Object oId = oDocument.get("_id");
			return oId != null ? oId.toString() : "";
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProcessorParametersTemplateRepositoryBackend.insert", oEx);
		}
		return "";
	}

	@Override
	public int deleteByTemplateId(String sTemplateId) {
		if (Utils.isNullOrEmpty(sTemplateId)) {
			return 0;
		}
		try {
			int iCount = 0;
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return -1;
			}
			for (Document oDoc : oCollection.find(where("templateId").eq(sTemplateId))) {
				if (oDoc != null) {
					iCount++;
				}
			}
			oCollection.remove(where("templateId").eq(sTemplateId));
			return iCount > 0 ? 1 : 0;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProcessorParametersTemplateRepositoryBackend.deleteByTemplateId", oEx);
			return -1;
		}
	}

	@Override
	public boolean updateProcessorParametersTemplate(ProcessorParametersTemplate oProcessorParametersTemplate) {
		try {
			if (oProcessorParametersTemplate == null) {
				return false;
			}
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return false;
			}
			oCollection.update(where("templateId").eq(oProcessorParametersTemplate.getTemplateId()), toDocument(oProcessorParametersTemplate));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProcessorParametersTemplateRepositoryBackend.update", oEx);
			return false;
		}
	}

	@Override
	public boolean isTheOwnerOfTheTemplate(String sTemplateId, String sUserId) {
		for (ProcessorParametersTemplate oTemplate : getAllTemplates()) {
			if (oTemplate != null && equalsSafe(oTemplate.getTemplateId(), sTemplateId) && equalsSafe(oTemplate.getUserId(), sUserId)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int deleteByUserId(String sUserId) {
		if (Utils.isNullOrEmpty(sUserId)) {
			return 0;
		}
		try {
			int iCount = 0;
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return -1;
			}
			for (Document oDoc : oCollection.find(where("userId").eq(sUserId))) {
				if (oDoc != null) {
					iCount++;
				}
			}
			oCollection.remove(where("userId").eq(sUserId));
			return iCount;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProcessorParametersTemplateRepositoryBackend.deleteByUserId", oEx);
			return -1;
		}
	}

	private List<ProcessorParametersTemplate> getAllTemplates() {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			return toList(oCollection != null ? oCollection.find() : null, ProcessorParametersTemplate.class);
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProcessorParametersTemplateRepositoryBackend.getAllTemplates", oEx);
			return new ArrayList<>();
		}
	}

	private boolean equalsSafe(String sA, String sB) {
		return sA == null ? sB == null : sA.equals(sB);
	}
}

package wasdi.shared.data.no2;

import static org.dizitart.no2.filters.FluentFilter.where;

import org.dizitart.no2.collection.Document;
import org.dizitart.no2.collection.NitriteCollection;

import wasdi.shared.business.processors.ProcessorUI;
import wasdi.shared.data.interfaces.IProcessorUIRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * NO2 backend implementation for processor UI repository.
 */
public class No2ProcessorUIRepositoryBackend extends No2Repository implements IProcessorUIRepositoryBackend {

	private static final String s_sCollectionName = "processorsui";

	@Override
	public boolean insertProcessorUI(ProcessorUI oProcUI) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null || oProcUI == null) {
				return false;
			}

			oCollection.insert(toDocument(oProcUI));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProcessorUIRepositoryBackend.insertProcessorUI", oEx);
		}

		return false;
	}

	@Override
	public ProcessorUI getProcessorUI(String sProcessorId) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return null;
			}

			for (Document oDoc : oCollection.find(where("processorId").eq(sProcessorId))) {
				return fromDocument(oDoc, ProcessorUI.class);
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProcessorUIRepositoryBackend.getProcessorUI", oEx);
		}

		return null;
	}

	@Override
	public boolean updateProcessorUI(ProcessorUI oProcessorUI) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null || oProcessorUI == null) {
				return false;
			}

			oCollection.update(where("processorId").eq(oProcessorUI.getProcessorId()), toDocument(oProcessorUI));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProcessorUIRepositoryBackend.updateProcessorUI", oEx);
		}

		return false;
	}

	@Override
	public int deleteProcessorUIByProcessorId(String sProcessorId) {
		if (Utils.isNullOrEmpty(sProcessorId)) {
			return 0;
		}
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return -1;
			}
			int iCount = 0;
			for (Document oDoc : oCollection.find(where("processorId").eq(sProcessorId))) {
				if (oDoc != null) {
					iCount++;
				}
			}
			oCollection.remove(where("processorId").eq(sProcessorId));
			return iCount;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProcessorUIRepositoryBackend.deleteProcessorUIByProcessorId", oEx);
			return -1;
		}
	}
}

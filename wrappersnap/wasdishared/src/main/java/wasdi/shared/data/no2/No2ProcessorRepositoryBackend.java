package wasdi.shared.data.no2;

import static org.dizitart.no2.filters.FluentFilter.where;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import org.dizitart.no2.collection.Document;
import org.dizitart.no2.collection.DocumentCursor;
import org.dizitart.no2.collection.NitriteCollection;

import wasdi.shared.business.processors.Processor;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.interfaces.IProcessorRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * NO2 backend implementation for processor repository.
 */
public class No2ProcessorRepositoryBackend extends No2Repository implements IProcessorRepositoryBackend {

	private static final String s_sCollectionName = "processors";

	@Override
	public boolean insertProcessor(Processor oProcessor) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null || oProcessor == null) {
				return false;
			}

			oCollection.insert(toDocument(oProcessor));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProcessorRepositoryBackend.insertProcessor: error", oEx);
		}

		return false;
	}

	@Override
	public Processor getProcessor(String sProcessorId) {
		if (Utils.isNullOrEmpty(sProcessorId)) {
			return null;
		}

		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return null;
			}

			for (Document oDocument : oCollection.find(where("processorId").eq(sProcessorId))) {
				return fromDocument(oDocument, Processor.class);
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProcessorRepositoryBackend.getProcessor: error", oEx);
		}

		return null;
	}

	@Override
	public Processor getProcessorByName(String sName) {
		if (Utils.isNullOrEmpty(sName)) {
			return null;
		}

		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return null;
			}

			for (Document oDocument : oCollection.find(where("name").eq(sName))) {
				return fromDocument(oDocument, Processor.class);
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProcessorRepositoryBackend.getProcessorByName: error", oEx);
		}

		return null;
	}

	@Override
	public boolean updateProcessor(Processor oProcessor) {
		if (oProcessor == null || Utils.isNullOrEmpty(oProcessor.getProcessorId())) {
			return false;
		}

		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return false;
			}

			oCollection.update(where("processorId").eq(oProcessor.getProcessorId()), toDocument(oProcessor));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProcessorRepositoryBackend.updateProcessor: error", oEx);
		}

		return false;
	}

	@Override
	public List<Processor> getProcessorByUser(String sUserId) {
		List<Processor> aoReturnList = new ArrayList<>();
		try {
			for (Processor oProcessor : getAllProcessors()) {
				if (oProcessor != null && equalsSafe(oProcessor.getUserId(), sUserId)) {
					aoReturnList.add(oProcessor);
				}
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProcessorRepositoryBackend.getProcessorByUser: error", oEx);
		}
		return aoReturnList;
	}

	@Override
	public List<Processor> findProcessorsByPartialName(String sPartialName) {
		List<Processor> aoReturnList = new ArrayList<>();

		if (Utils.isNullOrEmpty(sPartialName) || sPartialName.length() < 3) {
			return aoReturnList;
		}

		Pattern oRegex = Pattern.compile(Pattern.quote(sPartialName), Pattern.CASE_INSENSITIVE);

		for (Processor oProcessor : getAllProcessors()) {
			if (oProcessor == null) {
				continue;
			}

			if (matches(oRegex, oProcessor.getProcessorId())
					|| matches(oRegex, oProcessor.getName())
					|| matches(oRegex, oProcessor.getFriendlyName())
					|| matches(oRegex, oProcessor.getDescription())) {
				aoReturnList.add(oProcessor);
			}
		}

		aoReturnList.sort(Comparator.comparing(Processor::getName, Comparator.nullsLast(String::compareToIgnoreCase)));
		return aoReturnList;
	}

	@Override
	public int getNextProcessorPort() {
		int iPort = -1;

		try {
			for (Processor oProcessor : getAllProcessors()) {
				if (oProcessor != null && oProcessor.getPort() > iPort) {
					iPort = oProcessor.getPort();
				}
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProcessorRepositoryBackend.getNextProcessorPort: error", oEx);
		}

		if (iPort == -1) {
			if (WasdiConfig.Current != null && WasdiConfig.Current.dockers != null) {
				return WasdiConfig.Current.dockers.processorsInternalPort;
			}
			return 8080;
		}

		return iPort + 1;
	}

	@Override
	public boolean deleteProcessor(String sProcessorId) {
		if (Utils.isNullOrEmpty(sProcessorId)) {
			return false;
		}

		boolean bExisted = getProcessor(sProcessorId) != null;

		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection != null) {
				oCollection.remove(where("processorId").eq(sProcessorId));
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProcessorRepositoryBackend.deleteProcessor: error", oEx);
			return false;
		}

		return bExisted;
	}

	@Override
	public int deleteProcessorByUser(String sUserId) {
		if (Utils.isNullOrEmpty(sUserId)) {
			return 0;
		}

		int iDeleted = 0;
		for (Processor oProcessor : getAllProcessors()) {
			if (oProcessor != null && equalsSafe(oProcessor.getUserId(), sUserId)) {
				iDeleted++;
			}
		}

		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection != null) {
				oCollection.remove(where("userId").eq(sUserId));
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProcessorRepositoryBackend.deleteProcessorByUser: error", oEx);
		}

		return iDeleted;
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
		List<Processor> aoReturnList = new ArrayList<>(getAllProcessors());
		Comparator<Processor> oComparator = comparatorForOrderBy(sOrderBy);
		if (iDirection < 0) {
			oComparator = oComparator.reversed();
		}
		aoReturnList.sort(oComparator);
		return aoReturnList;
	}

	@Override
	public long countProcessors() {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			return oCollection != null ? oCollection.size() : 0;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProcessorRepositoryBackend.countProcessors: error", oEx);
		}
		return -1;
	}

	@Override
	public long countProcessors(boolean bPublicOnly) {
		return countProcessors(false, bPublicOnly);
	}

	@Override
	public long countProcessors(boolean bInAppStoreOnly, boolean bPublicOnly) {
		try {
			return getAllProcessors().stream()
					.filter(oProcessor -> !bPublicOnly || oProcessor.getIsPublic() == 1)
					.filter(oProcessor -> !bInAppStoreOnly || oProcessor.getShowInStore())
					.count();
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProcessorRepositoryBackend.countProcessors(filters): error", oEx);
		}
		return -1L;
	}

	private List<Processor> getAllProcessors() {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			DocumentCursor oCursor = oCollection != null ? oCollection.find() : null;
			return toList(oCursor, Processor.class);
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProcessorRepositoryBackend.getAllProcessors: error", oEx);
			return new ArrayList<>();
		}
	}

	private Comparator<Processor> comparatorForOrderBy(String sOrderBy) {
		String sField = Utils.isNullOrEmpty(sOrderBy) ? "_id" : sOrderBy;

		switch (sField) {
			case "name":
				return Comparator.comparing(Processor::getName, Comparator.nullsLast(String::compareToIgnoreCase));
			case "friendlyName":
				return Comparator.comparing(Processor::getFriendlyName, Comparator.nullsLast(String::compareToIgnoreCase));
			case "port":
				return Comparator.comparingInt(Processor::getPort);
			case "uploadDate":
				return Comparator.comparing(Processor::getUploadDate, Comparator.nullsLast(Double::compareTo));
			case "updateDate":
				return Comparator.comparing(Processor::getUpdateDate, Comparator.nullsLast(Double::compareTo));
			case "userId":
				return Comparator.comparing(Processor::getUserId, Comparator.nullsLast(String::compareToIgnoreCase));
			case "_id":
			default:
				return Comparator.comparing(Processor::getProcessorId, Comparator.nullsLast(String::compareToIgnoreCase));
		}
	}

	private boolean matches(Pattern oPattern, String sValue) {
		return sValue != null && oPattern.matcher(sValue).find();
	}

	private boolean equalsSafe(String sA, String sB) {
		return sA == null ? sB == null : sA.equals(sB);
	}
}

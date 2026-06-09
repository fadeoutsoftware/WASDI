package wasdi.shared.data.no2;

import static org.dizitart.no2.filters.FluentFilter.where;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

import org.dizitart.no2.collection.Document;
import org.dizitart.no2.collection.DocumentCursor;
import org.dizitart.no2.collection.FindOptions;
import org.dizitart.no2.collection.NitriteCollection;
import org.dizitart.no2.common.SortOrder;
import org.dizitart.no2.filters.Filter;

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
	public List<Processor> getDeployedProcessorsLightweight() {
		List<Processor> aoReturnList = new ArrayList<>();

		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return aoReturnList;
			}

			DocumentCursor oCursor = oCollection.find(Filter.ALL, FindOptions.orderBy("processorId", SortOrder.Ascending));
			for (Document oDocument : oCursor) {
				Processor oProcessor = toLightweightProcessor(oDocument);
				if (oProcessor != null) {
					aoReturnList.add(oProcessor);
				}
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProcessorRepositoryBackend.getDeployedProcessorsLightweight: error", oEx);
		}

		return aoReturnList;
	}

	@Override
	public List<Processor> getMarketplaceProcessors(String sOrderBy, int iDirection) {
		List<Processor> aoReturnList = getAllProcessors().stream()
				.filter(oProcessor -> oProcessor != null && oProcessor.getShowInStore())
				.collect(Collectors.toList());

		Comparator<Processor> oComparator = comparatorForOrderBy(sOrderBy);
		if (iDirection < 0) {
			oComparator = oComparator.reversed();
		}
		aoReturnList.sort(oComparator);
		return aoReturnList;
	}

	@Override
	public List<Processor> getMarketplaceProcessorsPage(
			String sUserId,
			List<String> asSharedProcessorIds,
			String sName,
			List<String> asCategories,
			List<String> asPublishers,
			float fMaxPrice,
			String sOrderBy,
			int iDirection,
			int iPage,
			int iItemsPerPage) {
		if (iPage < 0) {
			iPage = 0;
		}
		if (iItemsPerPage <= 0) {
			iItemsPerPage = 12;
		}

		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return new ArrayList<>();
			}

			List<Filter> aoAndFilters = new ArrayList<>();
			aoAndFilters.add(where("showInStore").eq(true));

			List<Filter> aoAccessFilters = new ArrayList<>();
			aoAccessFilters.add(where("isPublic").eq(1));
			if (!Utils.isNullOrEmpty(sUserId)) {
				aoAccessFilters.add(where("userId").eq(sUserId));
			}
			if (asSharedProcessorIds != null && !asSharedProcessorIds.isEmpty()) {
				if (asSharedProcessorIds.size() == 1) {
					aoAccessFilters.add(where("processorId").eq(asSharedProcessorIds.get(0)));
				}
				else {
					aoAccessFilters.add(where("processorId").in(asSharedProcessorIds.toArray(new String[0])));
				}
			}
			aoAndFilters.add(combineOr(aoAccessFilters));

			if (!Utils.isNullOrEmpty(sName)) {
				String sRegex = "(?i).*" + Pattern.quote(sName) + ".*";
				aoAndFilters.add(Filter.or(
						where("name").regex(sRegex),
						where("friendlyName").regex(sRegex)));
			}

			if (asCategories != null && !asCategories.isEmpty()) {
				List<Filter> aoCategoriesFilters = new ArrayList<>();
				for (String sCategory : asCategories) {
					aoCategoriesFilters.add(where("categories").elemMatch(where("$").eq(sCategory)));
				}
				aoAndFilters.add(combineOr(aoCategoriesFilters));
			}

			if (asPublishers != null && !asPublishers.isEmpty()) {
				if (asPublishers.size() == 1) {
					aoAndFilters.add(where("userId").eq(asPublishers.get(0)));
				}
				else {
					aoAndFilters.add(where("userId").in(asPublishers.toArray(new String[0])));
				}
			}

			if (fMaxPrice >= 0) {
				aoAndFilters.add(where("ondemandPrice").lte(fMaxPrice));
			}

			String sSortField = normalizeNo2SortField(sOrderBy);
			SortOrder eSortOrder = iDirection < 0 ? SortOrder.Descending : SortOrder.Ascending;
			FindOptions oFindOptions = FindOptions.orderBy(sSortField, eSortOrder)
					.skip((long) iPage * iItemsPerPage)
					.limit(iItemsPerPage);

			Filter oFinalFilter = combineAnd(aoAndFilters);
			DocumentCursor oCursor = oCollection.find(oFinalFilter, oFindOptions);
			return toList(oCursor, Processor.class);
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProcessorRepositoryBackend.getMarketplaceProcessorsPage: error", oEx);
		}

		return new ArrayList<>();
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

	private String normalizeNo2SortField(String sOrderBy) {
		if (Utils.isNullOrEmpty(sOrderBy) || "_id".equals(sOrderBy)) {
			return "processorId";
		}

		switch (sOrderBy) {
			case "friendlyName":
			case "updateDate":
			case "ondemandPrice":
			case "name":
				return sOrderBy;
			default:
				return "friendlyName";
		}
	}

	private Filter combineAnd(List<Filter> aoFilters) {
		if (aoFilters == null || aoFilters.isEmpty()) {
			return Filter.ALL;
		}
		if (aoFilters.size() == 1) {
			return aoFilters.get(0);
		}
		return Filter.and(aoFilters.toArray(new Filter[0]));
	}

	private Filter combineOr(List<Filter> aoFilters) {
		if (aoFilters == null || aoFilters.isEmpty()) {
			return Filter.ALL;
		}
		if (aoFilters.size() == 1) {
			return aoFilters.get(0);
		}
		return Filter.or(aoFilters.toArray(new Filter[0]));
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

	private Processor toLightweightProcessor(Document oDocument) {
		if (oDocument == null) {
			return null;
		}

		Processor oProcessor = new Processor();
		oProcessor.setProcessorId(asString(oDocument.get("processorId")));
		oProcessor.setUserId(asString(oDocument.get("userId")));
		oProcessor.setIsPublic(asInt(oDocument.get("isPublic"), 0));
		return oProcessor;
	}

	private String asString(Object oValue) {
		return oValue == null ? null : oValue.toString();
	}

	private int asInt(Object oValue, int iDefaultValue) {
		if (oValue instanceof Number) {
			return ((Number) oValue).intValue();
		}

		if (oValue != null) {
			try {
				return Integer.parseInt(oValue.toString());
			}
			catch (NumberFormatException oEx) {
				WasdiLog.warnLog("No2ProcessorRepositoryBackend.asInt: invalid integer value " + oValue);
			}
		}

		return iDefaultValue;
	}

	private boolean equalsSafe(String sA, String sB) {
		return sA == null ? sB == null : sA.equals(sB);
	}

	@Override
	public boolean deleteProcessorByName(String sProcessorName) {
		if (Utils.isNullOrEmpty(sProcessorName)) {
			return false;
		}

		boolean bExisted = getProcessor(sProcessorName) != null;

		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection != null) {
				oCollection.remove(where("name").eq(sProcessorName));
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ProcessorRepositoryBackend.deleteProcessorByName: error", oEx);
			return false;
		}

		return bExisted;
	}
}

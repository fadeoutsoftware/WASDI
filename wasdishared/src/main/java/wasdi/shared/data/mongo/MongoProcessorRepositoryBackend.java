package wasdi.shared.data.mongo;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

import wasdi.shared.business.processors.Processor;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.interfaces.IProcessorRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * Mongo backend implementation for processor repository.
 */
public class MongoProcessorRepositoryBackend extends MongoRepository implements IProcessorRepositoryBackend {

	public MongoProcessorRepositoryBackend() {
		m_sThisCollection = "processors";
	}

	@Override
	public boolean insertProcessor(Processor oProcessor) {

		try {
			String sJSON = s_oMapper.writeValueAsString(oProcessor);
			getCollection(m_sThisCollection).insertOne(Document.parse(sJSON));

			return true;

		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorRepository.insertProcessor :error ", oEx);
		}

		return false;
	}

	@Override
	public Processor getProcessor(String sProcessorId) {

		try {
			Document oWSDocument = getCollection(m_sThisCollection).find(new Document("processorId", sProcessorId)).first();

			if (null != oWSDocument) {
				String sJSON = oWSDocument.toJson();
				return s_oMapper.readValue(sJSON, Processor.class);
			}
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
			Document oWSDocument = getCollection(m_sThisCollection).find(new Document("name", sName)).first();
			if (null != oWSDocument) {
				String sJSON = oWSDocument.toJson();
				return s_oMapper.readValue(sJSON, Processor.class);
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorRepository.getProcessorByName :error ", oEx);
		}

		return null;
	}

	@Override
	public boolean updateProcessor(Processor oProcessor) {
		try {
			String sJSON = s_oMapper.writeValueAsString(oProcessor);

			Bson oFilter = new Document("processorId", oProcessor.getProcessorId());
			Bson oUpdateOperationDocument = new Document("$set", new Document(Document.parse(sJSON)));

			UpdateResult oResult = getCollection(m_sThisCollection).updateOne(oFilter, oUpdateOperationDocument);

			if (oResult.getModifiedCount() == 1) {
				return true;
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorRepository.updateProcessor :error ", oEx);
		}

		return false;
	}

	@Override
	public List<Processor> getProcessorByUser(String sUserId) {

		final ArrayList<Processor> aoReturnList = new ArrayList<Processor>();
		try {

			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(new Document("userId", sUserId));

			fillList(aoReturnList, oWSDocuments, Processor.class);

		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorRepository.getProcessorByUser :error ", oEx);
		}

		return aoReturnList;
	}

	@Override
	public List<Processor> findProcessorsByPartialName(String sPartialName) {
		List<Processor> aoReturnList = new ArrayList<>();

		if (Utils.isNullOrEmpty(sPartialName) || sPartialName.length() < 3) {
			return aoReturnList;
		}

		Pattern regex = Pattern.compile(Pattern.quote(sPartialName), Pattern.CASE_INSENSITIVE);

		Bson oFilterLikeProcessorId = Filters.eq("processorId", regex);
		Bson oFilterLikeName = Filters.eq("name", regex);
		Bson oFilterLikeFriendlyName = Filters.eq("friendlyName", regex);
		Bson oFilterLikeDescription = Filters.eq("description", regex);

		Bson oFilter = Filters.or(oFilterLikeProcessorId, oFilterLikeName, oFilterLikeFriendlyName, oFilterLikeDescription);

		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(oFilter).sort(new Document("name", 1));

			fillList(aoReturnList, oWSDocuments, Processor.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorRepository.findProcessorsByPartialName :error ", oEx);
		}

		return aoReturnList;
	}

	@Override
	public int getNextProcessorPort() {

		int iPort = -1;

		try {
			Document oWSDocument = getCollection(m_sThisCollection).find().sort(new Document("port", -1)).first();
			String sJSON = oWSDocument.toJson();
			Processor oProcessor = s_oMapper.readValue(sJSON, Processor.class);
			iPort = oProcessor.getPort();
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

			DeleteResult oDeleteResult = getCollection(m_sThisCollection).deleteOne(new Document("processorId", sProcessorId));

			if (oDeleteResult != null) {
				if (oDeleteResult.getDeletedCount() == 1) {
					return true;
				}
			}

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

			DeleteResult oDeleteResult = getCollection(m_sThisCollection).deleteMany(new Document("userId", sUserId));

			if (oDeleteResult != null) {
				return (int) oDeleteResult.getDeletedCount();
			}

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

		final ArrayList<Processor> aoReturnList = new ArrayList<Processor>();
		try {

			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find().sort(new Document(sOrderBy, iDirection));

			fillList(aoReturnList, oWSDocuments, Processor.class);

		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorRepository.getDeployedProcessors :error ", oEx);
		}

		return aoReturnList;
	}

	@Override
	public List<Processor> getMarketplaceProcessors(String sOrderBy, int iDirection) {

		final ArrayList<Processor> aoReturnList = new ArrayList<Processor>();
		try {
			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection)
					.find(new Document("showInStore", true))
					.projection(Projections.include(
							"processorId",
							"userId",
							"name",
							"friendlyName",
							"description",
							"isPublic",
							"ondemandPrice",
							"pricePerSquareKm",
							"showInStore",
							"logo",
							"categories"
					))
					.sort(new Document(sOrderBy, iDirection));

			fillList(aoReturnList, oWSDocuments, Processor.class);

		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorRepository.getMarketplaceProcessors :error ", oEx);
		}

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

		final ArrayList<Processor> aoReturnList = new ArrayList<Processor>();
		try {
			if (iPage < 0) {
				iPage = 0;
			}
			if (iItemsPerPage <= 0) {
				iItemsPerPage = 12;
			}

			List<Bson> aoAndFilters = new ArrayList<>();
			aoAndFilters.add(Filters.eq("showInStore", true));

			List<Bson> aoAccessFilters = new ArrayList<>();
			aoAccessFilters.add(Filters.eq("isPublic", 1));
			if (!Utils.isNullOrEmpty(sUserId)) {
				aoAccessFilters.add(Filters.eq("userId", sUserId));
			}
			if (asSharedProcessorIds != null && !asSharedProcessorIds.isEmpty()) {
				aoAccessFilters.add(Filters.in("processorId", asSharedProcessorIds));
			}
			aoAndFilters.add(Filters.or(aoAccessFilters));

			if (!Utils.isNullOrEmpty(sName)) {
				Pattern oRegex = Pattern.compile(Pattern.quote(sName), Pattern.CASE_INSENSITIVE);
				aoAndFilters.add(Filters.or(
						Filters.regex("name", oRegex),
						Filters.regex("friendlyName", oRegex)));
			}

			if (asCategories != null && !asCategories.isEmpty()) {
				aoAndFilters.add(Filters.in("categories", asCategories));
			}

			if (asPublishers != null && !asPublishers.isEmpty()) {
				aoAndFilters.add(Filters.in("userId", asPublishers));
			}

			if (fMaxPrice >= 0) {
				aoAndFilters.add(Filters.lte("ondemandPrice", fMaxPrice));
			}

			Bson oFinalFilter = Filters.and(aoAndFilters);

			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection)
					.find(oFinalFilter)
					.projection(Projections.include(
							"processorId",
							"userId",
							"name",
							"friendlyName",
							"description",
							"isPublic",
							"ondemandPrice",
							"pricePerSquareKm",
							"showInStore",
							"logo",
							"categories"))
					.sort(new Document(sOrderBy, iDirection))
					.skip(iPage * iItemsPerPage)
					.limit(iItemsPerPage);

			fillList(aoReturnList, oWSDocuments, Processor.class);

		} catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorRepository.getMarketplaceProcessorsPage :error ", oEx);
		}

		return aoReturnList;
	}

	@Override
	public long countProcessors() {
		return getCollection(m_sThisCollection).countDocuments();
	}

	@Override
	public long countProcessors(boolean bPublicOnly) {
		return countProcessors(false, bPublicOnly);
	}

	@Override
	public long countProcessors(boolean bInAppStoreOnly, boolean bPublicOnly) {
		try {
			BasicDBObject oQuery = new BasicDBObject();

			BasicDBObject oPublicOnlyQuery = null;
			if (bPublicOnly) {
				oPublicOnlyQuery = new BasicDBObject("isPublic", 1);

			}

			BasicDBObject oInStoreOnlyQuery = null;
			if (bInAppStoreOnly) {
				oInStoreOnlyQuery = new BasicDBObject("showInStore", true);
			}

			if (null == oPublicOnlyQuery && null == oInStoreOnlyQuery) {
				return getCollection(m_sThisCollection).countDocuments();
			}

			if (null != oPublicOnlyQuery && null == oInStoreOnlyQuery) {
				return getCollection(m_sThisCollection).countDocuments(oPublicOnlyQuery);
			}

			if (null == oPublicOnlyQuery && null != oInStoreOnlyQuery) {
				return getCollection(m_sThisCollection).countDocuments(oInStoreOnlyQuery);
			}

			if (null != oPublicOnlyQuery && null != oInStoreOnlyQuery) {
				List<BasicDBObject> aoAndList = new ArrayList<>();
				aoAndList.add(oPublicOnlyQuery);
				aoAndList.add(oInStoreOnlyQuery);
				oQuery.put("$and", aoAndList);
				return getCollection(m_sThisCollection).countDocuments(oQuery);
			}

		} catch (Exception oE) {
			WasdiLog.errorLog("ProcessorRepository.countProcessors( " + bInAppStoreOnly + ", " + bPublicOnly + " ): ", oE);
		}
		return -1L;
	}
}

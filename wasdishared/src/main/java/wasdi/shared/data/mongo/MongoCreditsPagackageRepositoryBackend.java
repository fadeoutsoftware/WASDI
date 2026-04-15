package wasdi.shared.data.mongo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.result.UpdateResult;

import wasdi.shared.business.CreditsPackage;
import wasdi.shared.data.interfaces.ICreditsPagackageRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * Mongo backend implementation for credits package repository.
 */
public class MongoCreditsPagackageRepositoryBackend extends MongoRepository implements ICreditsPagackageRepositoryBackend {

	public MongoCreditsPagackageRepositoryBackend() {
		m_sThisCollection = "creditspackages";
	}

	@Override
	public CreditsPackage getCreditPackageById(String sCreditPackageId) {
		try {
			Document oWSDocument = getCollection(m_sThisCollection).find(new Document("creditPackageId", sCreditPackageId)).first();

			if (oWSDocument != null) {
				String sJSON = oWSDocument.toJson();

				return s_oMapper.readValue(sJSON, CreditsPackage.class);
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("CreditsPagackageRepository.getCreditPackageById: error ", oEx);
		}

		return null;
	}

	@Override
	public CreditsPackage getCreditPackageByNameAndUserId(String sCreditPackageName, String sUserId) {
		try {
			Document oWSDocument = getCollection(m_sThisCollection)
					.find(Filters.and(Filters.eq("userId", sUserId), Filters.eq("name", sCreditPackageName))).first();

			if (oWSDocument != null) {
				String sJSON = oWSDocument.toJson();

				return s_oMapper.readValue(sJSON, CreditsPackage.class);
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("CreditsPagackageRepository.getCreditPackageByNameAndUserId: error ", oEx);
		}

		return null;
	}

	@Override
	public Double getTotalCreditsByUser(String sUserId) {

		if (Utils.isNullOrEmpty(sUserId)) {
			WasdiLog.warnLog("CreditsPackageRepository.getTotalCreditsByUser: user id is null or empty");
			return null;
		}

		try {
			AggregateIterable<Document> oResult = getCollection(m_sThisCollection).aggregate(Arrays.asList(
					Aggregates.match(Filters.and(Filters.eq("userId", sUserId), Filters.eq("buySuccess", true))),
					Aggregates.group(null, Accumulators.sum("credits", "$creditsRemaining"))));

			if (oResult != null) {
				if (oResult.first() != null) {
					return oResult.first().getDouble("credits");
				}
			}

		} catch (Exception oEx) {
			WasdiLog.errorLog("CreditsPagackageRepository.getTotalCreditsByUser: error", oEx);
		}

		return 0.0;

	}

	@Override
	public List<CreditsPackage> listByUser(String sUserId, boolean bAscendingOrder) {

		if (Utils.isNullOrEmpty(sUserId)) {
			WasdiLog.warnLog("CreditsPackageRepository.listByUser: user id is null or empty");
			return null;
		}

		try {
			Bson oSortingOder = bAscendingOrder ? Sorts.ascending("buyDate") : Sorts.descending("buyDate");
			List<CreditsPackage> aoReturnList = new ArrayList<>();
			FindIterable<Document> aoRetrievedDocs = getCollection(m_sThisCollection).find(new Document("userId", sUserId)).sort(oSortingOder);
			fillList(aoReturnList, aoRetrievedDocs, CreditsPackage.class);
			return aoReturnList;

		} catch (Exception oEx) {
			WasdiLog.errorLog("CreditsPagackageRepository.listByUser: error", oEx);
		}
		return null;
	}

	@Override
	public boolean insertCreditPackage(CreditsPackage oCreditPackage) {

		try {
			String sJSON = s_oMapper.writeValueAsString(oCreditPackage);
			getCollection(m_sThisCollection).insertOne(Document.parse(sJSON));

			return true;
		} catch (Exception oEx) {
			WasdiLog.errorLog("CreditsPagackageRepository.getCreditPackageByNameAndUserId: error ", oEx);
		}

		return false;
	}

	@Override
	public boolean updateCreditPackage(CreditsPackage oCreditPackage) {

		try {
			String sJSON = s_oMapper.writeValueAsString(oCreditPackage);

			Bson oFilter = new Document("creditPackageId", oCreditPackage.getCreditPackageId());
			Bson oUpdateOperationDocument = new Document("$set", new Document(Document.parse(sJSON)));

			UpdateResult oResult = getCollection(m_sThisCollection).updateOne(oFilter, oUpdateOperationDocument);

			if (oResult.getModifiedCount() == 1)
				return true;
		} catch (Exception oEx) {
			WasdiLog.errorLog("SubscriptionRepository.updateSubscription: error ", oEx);
		}

		return false;
	}
}

package wasdi.shared.data;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.client.FindIterable;

import wasdi.shared.viewmodels.monitoring.MetricsEntry;

public class MetricsEntryRepository extends MongoRepository {

	public MetricsEntryRepository() {
		m_sThisCollection = "metricsentries";
	}

	public boolean insertMetricsEntry(MetricsEntry oMetricsEntry) {
		try {
			String sJSON = s_oMapper.writeValueAsString(oMetricsEntry);
			getCollection(m_sThisCollection).insertOne(Document.parse(sJSON));

			return true;
		} catch (Exception oEx) {
			oEx.printStackTrace();
		}

		return false;
	}

	public List<MetricsEntry> getMetricsEntries() {
		final List<MetricsEntry> aoReturnList = new ArrayList<>();

		try {
			FindIterable<Document> oDocuments = getCollection(m_sThisCollection).find();

			fillList(aoReturnList, oDocuments, MetricsEntry.class);
		} catch (Exception oEx) {
			oEx.printStackTrace();
		}

		return aoReturnList;
	}

	public List<MetricsEntry> getMetricsEntryByNode(String sNode) {
		final List<MetricsEntry> aoReturnList = new ArrayList<>();

		try {
			FindIterable<Document> oDocuments = getCollection(m_sThisCollection)
					.find(new Document("node", sNode));

			fillList(aoReturnList, oDocuments, MetricsEntry.class);
		} catch (Exception oEx) {
			oEx.printStackTrace();
		}

		return aoReturnList;
	}

	public MetricsEntry getLatestMetricsEntryByNode(String sNode) {
		MetricsEntry oMetricsEntry = null;

		try {
			Document oDocument = getCollection(m_sThisCollection)
					.find(new Document("node", sNode))
					.sort(new Document("timestamp", -1))
					.first();

			if (oDocument != null) {
				String sJSON = oDocument.toJson();
				oMetricsEntry = s_oMapper.readValue(sJSON, MetricsEntry.class);
			}
		} catch (Exception oEx) {
			oEx.printStackTrace();
		}

		return oMetricsEntry;
	}

}

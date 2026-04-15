package wasdi.shared.data.no2;

import static org.dizitart.no2.filters.FluentFilter.where;

import org.dizitart.no2.collection.Document;
import org.dizitart.no2.collection.DocumentCursor;
import org.dizitart.no2.collection.NitriteCollection;

import wasdi.shared.business.Counter;
import wasdi.shared.data.interfaces.ICounterRepositoryBackend;
import wasdi.shared.utils.log.WasdiLog;

/**
 * First native NO2 backend port target (CounterRepository).
 */
public class No2CounterRepositoryBackend implements ICounterRepositoryBackend {

    private static final String s_sCounterCollectionName = "counter";
    private static final String s_sSequenceFieldName = "sequence";
    private static final String s_sValueFieldName = "value";

    public No2CounterRepositoryBackend() {
    }

    private NitriteCollection getCounterCollection() {
        return No2Connection.getCollection(s_sCounterCollectionName);
    }

    private Document getCounterDocumentBySequence(String sSequence) {
        NitriteCollection oCounterCollection = getCounterCollection();

        if (oCounterCollection == null) {
            return null;
        }

        DocumentCursor oCursor = oCounterCollection.find(where(s_sSequenceFieldName).eq(sSequence));

        for (Document oCounterDocument : oCursor) {
            return oCounterDocument;
        }

        return null;
    }

    private Counter fromDocument(Document oCounterDocument) {
        if (oCounterDocument == null) {
            return null;
        }

        Counter oCounter = new Counter();
        oCounter.setSequence(oCounterDocument.get(s_sSequenceFieldName, String.class));

        Integer iCounterValue = oCounterDocument.get(s_sValueFieldName, Integer.class);
        oCounter.setValue(iCounterValue != null ? iCounterValue.intValue() : 0);

        return oCounter;
    }

	@Override
	public int getNextValue(String sSequence) {
		Counter oCounter = getCounterBySequence(sSequence);

		if (oCounter == null) {
			oCounter = new Counter();
			oCounter.setSequence(sSequence);
			oCounter.setValue(1);
			insertCounter(oCounter);
			return 1;
		}

		oCounter.setValue(oCounter.getValue() + 1);

		if (updateCounter(oCounter)) {
			return oCounter.getValue();
		}

		return 0;
	}

	@Override
	public String insertCounter(Counter oCounter) {
		try {
			NitriteCollection oCounterCollection = getCounterCollection();

			if (oCounterCollection == null || oCounter == null) {
				return "";
			}

			Document oCounterDocument = Document.createDocument(s_sSequenceFieldName, oCounter.getSequence())
					.put(s_sValueFieldName, oCounter.getValue());

			oCounterCollection.insert(oCounterDocument);
			return oCounter.getSequence();
		}
		catch (Exception oException) {
			WasdiLog.errorLog("No2CounterRepositoryBackend.insertCounter exception", oException);
			return "";
		}
	}

	@Override
	public Counter getCounterBySequence(String sSequence) {
		try {
			Document oCounterDocument = getCounterDocumentBySequence(sSequence);
			return fromDocument(oCounterDocument);
		}
		catch (Exception oException) {
			WasdiLog.errorLog("No2CounterRepositoryBackend.getCounterBySequence exception", oException);
			return null;
		}
	}

	@Override
	public boolean updateCounter(Counter oCounter) {
		try {
			NitriteCollection oCounterCollection = getCounterCollection();

			if (oCounterCollection == null || oCounter == null) {
				return false;
			}

			Document oUpdateDocument = Document.createDocument(s_sValueFieldName, oCounter.getValue());
			oCounterCollection.update(where(s_sSequenceFieldName).eq(oCounter.getSequence()), oUpdateDocument);
			return true;
		}
		catch (Exception oException) {
			WasdiLog.errorLog("No2CounterRepositoryBackend.updateCounter exception", oException);
			return false;
		}
	}
}

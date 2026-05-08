package wasdi.shared.data.no2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.dizitart.no2.collection.Document;
import org.dizitart.no2.collection.DocumentCursor;
import org.dizitart.no2.collection.NitriteCollection;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import wasdi.shared.utils.log.WasdiLog;

/**
 * Base helper for NO2 repository implementations.
 */
public class No2Repository {

	protected static final ObjectMapper s_oMapper = new ObjectMapper();

	static {
		s_oMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	protected NitriteCollection getCollection(String sCollectionName) {
		return No2Connection.getCollection(sCollectionName);
	}

	protected Document toDocument(Object oEntity) throws Exception {
		Map<String, Object> aoFields = s_oMapper.convertValue(oEntity, Map.class);
		Document oDocument = Document.createDocument();

		for (Map.Entry<String, Object> oField : aoFields.entrySet()) {
			oDocument.put(oField.getKey(), oField.getValue());
		}

		return oDocument;
	}

	protected <T> T fromDocument(Document oDocument, Class<T> oClass) {
		if (oDocument == null) {
			return null;
		}

		try {
			return s_oMapper.convertValue(oDocument, oClass);
		}
		catch (Exception oException) {
			WasdiLog.errorLog("No2Repository.fromDocument: conversion error", oException);
			return null;
		}
	}

	protected <T> List<T> toList(DocumentCursor oCursor, Class<T> oClass) {
		List<T> aoResults = new ArrayList<>();

		if (oCursor == null) {
			return aoResults;
		}

		for (Document oDocument : oCursor) {
			T oEntity = fromDocument(oDocument, oClass);

			if (oEntity != null) {
				aoResults.add(oEntity);
			}
		}

		return aoResults;
	}
}

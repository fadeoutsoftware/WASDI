package wasdi.shared.data.no2;

import static org.dizitart.no2.filters.FluentFilter.where;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.dizitart.no2.collection.Document;
import org.dizitart.no2.collection.DocumentCursor;
import org.dizitart.no2.collection.NitriteCollection;

import wasdi.shared.business.Style;
import wasdi.shared.data.interfaces.IStyleRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * NO2 backend implementation for style repository.
 */
public class No2StyleRepositoryBackend extends No2Repository implements IStyleRepositoryBackend {

	private static final String s_sCollectionName = "styles";

	@Override
	public boolean insertStyle(Style oStyle) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null || oStyle == null) {
				return false;
			}

			oCollection.insert(toDocument(oStyle));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2StyleRepositoryBackend.insertStyle: error", oEx);
		}

		return false;
	}

	@Override
	public Style getStyle(String sStyleId) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return null;
			}

			for (Document oDocument : oCollection.find(where("styleId").eq(sStyleId))) {
				return fromDocument(oDocument, Style.class);
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2StyleRepositoryBackend.getStyle: error", oEx);
		}

		return null;
	}

	@Override
	public Style getStyleByName(String sName) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return null;
			}

			for (Document oDocument : oCollection.find(where("name").eq(sName))) {
				return fromDocument(oDocument, Style.class);
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2StyleRepositoryBackend.getStyleByName: error", oEx);
		}

		return null;
	}

	@Override
	public boolean isStyleNameTaken(String sName) {
		return getStyleByName(sName) != null;
	}

	@Override
	public List<Style> getStylePublicAndByUser(String sUserId) {
		List<Style> aoResults = new ArrayList<>();

		for (Style oStyle : getList()) {
			if (oStyle == null) {
				continue;
			}

			if (oStyle.getIsPublic() || (sUserId != null && sUserId.equals(oStyle.getUserId()))) {
				aoResults.add(oStyle);
			}
		}

		aoResults.sort(Comparator.comparing(Style::getName, Comparator.nullsLast(String::compareToIgnoreCase)));
		return aoResults;
	}

	@Override
	public List<Style> getList() {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			DocumentCursor oCursor = oCollection != null ? oCollection.find() : null;
			List<Style> aoStyles = toList(oCursor, Style.class);
			aoStyles.sort(Comparator.comparing(Style::getName, Comparator.nullsLast(String::compareToIgnoreCase)));
			return aoStyles;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2StyleRepositoryBackend.getList: error", oEx);
		}

		return new ArrayList<>();
	}

	@Override
	public boolean updateStyle(Style oStyle) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null || oStyle == null) {
				return false;
			}

			oCollection.update(where("styleId").eq(oStyle.getStyleId()), toDocument(oStyle));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2StyleRepositoryBackend.updateStyle: error", oEx);
		}

		return false;
	}

	@Override
	public boolean deleteStyle(String sStyleId) {
		if (Utils.isNullOrEmpty(sStyleId)) {
			return false;
		}

		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return false;
			}

			oCollection.remove(where("styleId").eq(sStyleId));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2StyleRepositoryBackend.deleteStyle: error", oEx);
		}

		return false;
	}

	@Override
	public int deleteStyleByUser(String sUserId) {
		if (Utils.isNullOrEmpty(sUserId)) {
			return 0;
		}

		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return 0;
			}

			int iCount = 0;
			for (Style oStyle : getList()) {
				if (oStyle != null && sUserId.equals(oStyle.getUserId())) {
					iCount++;
				}
			}

			oCollection.remove(where("userId").eq(sUserId));
			return iCount;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2StyleRepositoryBackend.deleteStyleByUser: error", oEx);
		}

		return 0;
	}

	@Override
	public boolean isOwnedByUser(String sUserId, String sStyleId) {
		Style oStyle = getStyle(sStyleId);
		return oStyle != null && sUserId != null && sUserId.equals(oStyle.getUserId());
	}

	@Override
	public List<Style> findStylesByPartialName(String sPartialName) {
		List<Style> aoResults = new ArrayList<>();

		if (Utils.isNullOrEmpty(sPartialName) || sPartialName.length() < 3) {
			return aoResults;
		}

		String sLookup = sPartialName.toLowerCase();

		for (Style oStyle : getList()) {
			if (oStyle == null) {
				continue;
			}

			String sStyleId = oStyle.getStyleId() != null ? oStyle.getStyleId().toLowerCase() : "";
			String sName = oStyle.getName() != null ? oStyle.getName().toLowerCase() : "";
			String sDescription = oStyle.getDescription() != null ? oStyle.getDescription().toLowerCase() : "";

			if (sStyleId.contains(sLookup) || sName.contains(sLookup) || sDescription.contains(sLookup)) {
				aoResults.add(oStyle);
			}
		}

		aoResults.sort(Comparator.comparing(Style::getName, Comparator.nullsLast(String::compareToIgnoreCase)));
		return aoResults;
	}
}

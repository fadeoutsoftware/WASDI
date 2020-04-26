package wasdi.shared.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.Block;
import com.mongodb.client.FindIterable;

import wasdi.shared.business.Catalog;

/**
 * Catalog Repo
 * Created by s.adamo on 02/03/2017.
 */
public class CatalogRepository extends MongoRepository{
	
	public CatalogRepository() {
		m_sThisCollection = "catalog";
	}

	/**
	 * Insert new Catalog
	 * @param oCatalog
	 * @return
	 */
    public boolean insertCatalogEntry(Catalog oCatalog) {
        try {
            String sJSON = s_oMapper.writeValueAsString(oCatalog);
            getCollection(m_sThisCollection).insertOne(Document.parse(sJSON));

            return true;

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return false;
    }

    /**
     * Get All catalogues
     * @return
     */
    public List<Catalog> getCatalogs() {

        final ArrayList<Catalog> aoReturnList = new ArrayList<Catalog>();
        try {

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find();

            oWSDocuments.forEach(new Block<Document>() {
                public void apply(Document document) {
                    String sJSON = document.toJson();
                    Catalog oCatalog = null;
                    try {
                        oCatalog = s_oMapper.readValue(sJSON,Catalog.class);
                        aoReturnList.add(oCatalog);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            });

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }

    /**
     * Get by date
     * @param sDate
     * @return
     */
    public Catalog getCatalogsByDate(String sDate) {

        Catalog oCatalog = null;
        try {

            Document oWSDocument = getCollection(m_sThisCollection).find(new Document("date", sDate)).first();
            String sJSON = oWSDocument.toJson();
            try {
                oCatalog = s_oMapper.readValue(sJSON, Catalog.class);

            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return oCatalog;
    }

}

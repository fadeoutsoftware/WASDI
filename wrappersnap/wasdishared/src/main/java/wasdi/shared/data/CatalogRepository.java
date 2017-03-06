package wasdi.shared.data;

import org.bson.Document;
import wasdi.shared.business.Catalog;
import wasdi.shared.business.DownloadedFile;

/**
 * Created by s.adamo on 02/03/2017.
 */
public class CatalogRepository extends MongoRepository{

    public boolean InsertCatalogEntry(Catalog oCatalog) {
        try {
            String sJSON = s_oMapper.writeValueAsString(oCatalog);
            getCollection("catalog").insertOne(Document.parse(sJSON));

            return true;

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return false;
    }

}

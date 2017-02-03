package wasdi.shared.data;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import wasdi.shared.business.DownloadedFile;
import wasdi.shared.business.UserSession;

import java.util.Date;

/**
 * Created by p.campanella on 11/11/2016.
 */
public class DownloadedFilesRepository extends MongoRepository {

    public boolean InsertDownloadedFile(DownloadedFile oFile) {
        try {
            String sJSON = s_oMapper.writeValueAsString(oFile);
            getCollection("downloadedfiles").insertOne(Document.parse(sJSON));

            return true;

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return false;
    }

    public boolean UpdateDownloadedFile(DownloadedFile oFile) {
        try {
            UpdateResult oResult = getCollection("downloadedfiles").updateOne(Filters.eq("fileName", oFile.getFileName()), Updates.set("productViewModel", oFile.getProductViewModel()));

            if (oResult.getModifiedCount()==1) return  true;
        }
        catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return  false;
    }

    public DownloadedFile GetDownloadedFile(String sFileName) {
        try {
            Document oSessionDocument = getCollection("downloadedfiles").find(new Document("fileName", sFileName)).first();

            if (oSessionDocument==null) return  null;

            String sJSON = oSessionDocument.toJson();

            DownloadedFile oFile = s_oMapper.readValue(sJSON,DownloadedFile.class);

            return oFile;
        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return  null;
    }
}

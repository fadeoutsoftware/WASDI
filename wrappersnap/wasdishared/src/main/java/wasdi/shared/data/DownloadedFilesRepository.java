package wasdi.shared.data;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

import wasdi.shared.business.DownloadedFile;

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
            String sJSON = s_oMapper.writeValueAsString(oFile);
            
            Bson oFilter = new Document("fileName", oFile.getFileName());
            Bson oUpdateOperationDocument = new Document("$set", new Document(Document.parse(sJSON)));
            
            //UpdateResult oResult = getCollection("downloadedfiles").updateOne(Filters.eq("fileName", oFile.getFileName()), new Document(Document.parse(sJSON)));
            UpdateResult oResult = getCollection("downloadedfiles").updateOne(oFilter, oUpdateOperationDocument);

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

    public int DeleteByFilePath(String sFilePath) {

        try {

            DeleteResult oDeleteResult = getCollection("downloadedfiles").deleteOne(Filters.eq("filePath", sFilePath));

            if (oDeleteResult != null)
            {
                return  (int) oDeleteResult.getDeletedCount();
            }

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return 0;
    }
}

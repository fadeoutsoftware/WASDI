package wasdi.shared.data;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

import wasdi.shared.business.DownloadedFile;
import wasdi.shared.business.ProductWorkspace;

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

    public List<DownloadedFile> Search(Date from, Date to, String freeText, String category) {
    	List<DownloadedFile> files = new ArrayList<DownloadedFile>();    	
    	List<Bson> filters = new ArrayList<Bson>();
    	
    	if (from!=null && to!=null) {
    		
    		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
    		
    		filters.add(Filters.and(Filters.gte("refDate", df.format(from)), Filters.lte("refDate", df.format(to))));
    	}
    	
    	if (freeText!=null && !freeText.isEmpty()) {
    		Document regQuery = new Document();
    		regQuery.append("$regex", Pattern.quote(freeText));
    		regQuery.append("$options", "i");
    		Document findQuery = new Document();
    		findQuery.append("fileName", regQuery);
    		filters.add(findQuery);
    	}
    	
    	if (category!=null && !category.isEmpty()) {
    		filters.add(Filters.eq("category", category));
    	}
    	
    	Bson filter = Filters.and(filters);
    	FindIterable<Document> docs = filters.isEmpty() ? getCollection("downloadedfiles").find() : getCollection("downloadedfiles").find(filter);
    	
    	docs.forEach(new Block<Document>() {
            public void apply(Document document) {
                String json = document.toJson();
                try {
                    DownloadedFile df = s_oMapper.readValue(json, DownloadedFile.class);
                    files.add(df);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
    	
		return files ;
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

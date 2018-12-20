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
import wasdi.shared.viewmodels.AttributeViewModel;
import wasdi.shared.viewmodels.MetadataViewModel;

/**
 * Downloaded File Repo
 * Created by p.campanella on 11/11/2016.
 */
public class DownloadedFilesRepository extends MongoRepository {

	/**
	 * Insert new Downloaded File
	 * @param oFile
	 * @return
	 */
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

    /**
     * Update Entity
     * @param oFile
     * @return
     */
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

    /**
     * Get Downloaded file by name
     * @param sFileName
     * @return
     */
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

    /**
     * Search files
     * @param oDateFrom start date
     * @param oDateTo end date
     * @param sFreeText free text to search in name
     * @param sCategory category
     * @return
     */
    public List<DownloadedFile> Search(Date oDateFrom, Date oDateTo, String sFreeText, String sCategory) {
    	final List<DownloadedFile> aoFiles = new ArrayList<DownloadedFile>();    	
    	List<Bson> aoFilters = new ArrayList<Bson>();
    	
    	if (oDateFrom!=null && oDateTo!=null) {
    		
    		DateFormat oDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
    		
    		aoFilters.add(Filters.and(Filters.gte("refDate", oDateFormat.format(oDateFrom)), Filters.lte("refDate", oDateFormat.format(oDateTo))));
    	}
    	
    	if (sFreeText!=null && !sFreeText.isEmpty()) {
    		Document oRegQuery = new Document();
    		oRegQuery.append("$regex", Pattern.quote(sFreeText));
    		oRegQuery.append("$options", "i");
    		Document oFindQuery = new Document();
    		oFindQuery.append("fileName", oRegQuery);
    		aoFilters.add(oFindQuery);
    	}
    	
    	if (sCategory!=null && !sCategory.isEmpty()) {
    		aoFilters.add(Filters.eq("category", sCategory));
    	}
    	
    	Bson filter = Filters.and(aoFilters);
    	FindIterable<Document> aoDocs = aoFilters.isEmpty() ? getCollection("downloadedfiles").find() : getCollection("downloadedfiles").find(filter);
    	
    	aoDocs.forEach(new Block<Document>() {
            public void apply(Document oDocument) {
                String sJson = oDocument.toJson();
                try {
                    DownloadedFile oDownloadedFile = s_oMapper.readValue(sJson, DownloadedFile.class);
                    aoFiles.add(oDownloadedFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
    	
		return aoFiles ;
    }
    
    /**
     * Delete by file path
     * @param sFilePath
     * @return
     */
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
    
    /**
     * Get full list
     * @return
     */
    public List<DownloadedFile> getList() {
        final ArrayList<DownloadedFile> aoReturnList = new ArrayList<DownloadedFile>();
        try {

            FindIterable<Document> oDFDocuments = getCollection("downloadedfiles").find();

            oDFDocuments.forEach(new Block<Document>() {
                public void apply(Document document) {
                    String sJSON = document.toJson();
                    DownloadedFile oDwFile = null;
                    try {
                        oDwFile = s_oMapper.readValue(sJSON,DownloadedFile.class);
                        aoReturnList.add(oDwFile);
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
    
    public static void main(String[] args) {
	    String productName = "S2B_MSIL1C_20171102T100139_N0206_R122_T32TQP_20171102T121017.zip";
		DownloadedFilesRepository repo = new DownloadedFilesRepository();
		DownloadedFile df = repo.GetDownloadedFile(productName);
		MetadataViewModel m = df.getProductViewModel().getMetadata();
		if (m!=null) {
			showMetadata(m.getName(), m);
		} else {
			System.out.println("No metadata found!");
		}
		
    } 
   
    private static void showMetadata(String pre, MetadataViewModel m) {
//	   System.out.println(pre + " NAME: " + m.getName());
	   ArrayList<AttributeViewModel> attrs = m.getAttributes();
	   if (attrs!=null) {
		   for (AttributeViewModel attr : attrs) {
			   System.out.println(pre + ": " + attr.getDescription() + " = " + attr.getData());
			   //System.out.println(pre + " - DATA: " + attr.getData());
			   //System.out.println(pre + " - DESCR: " + attr.getDescription());
			   //System.out.println(pre + " - TYPE: " + attr.getDataType());
			   //System.out.println(pre + " - NUM_EL: " + attr.getNumElems());
		   }
	   } else {
		   System.out.println(pre + " - NO ATTRIBUTES");
	   }
	   String newPre = (pre.isEmpty()) ? m.getName() : pre + "." + m.getName();
	   ArrayList<MetadataViewModel> childs = m.getElements();
	   if (childs != null) {
		   for (MetadataViewModel anotherM : childs) {
			   showMetadata(newPre, anotherM);
		   }		   
	   }
    }
    
}

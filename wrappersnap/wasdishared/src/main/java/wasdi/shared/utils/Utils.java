package wasdi.shared.utils;

import static org.apache.commons.lang.SystemUtils.IS_OS_UNIX;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

// email, IP addresses (v4 and v6), domains and URL validators:
import org.apache.commons.validator.routines.DomainValidator;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.apache.commons.validator.routines.UrlValidator;

import wasdi.shared.business.UserSession;

/**
 * Created by p.campanella on 14/10/2016.
 */
public class Utils {
	
	public static int m_iSessionValidityMinutes = 24*60;
	
    public static boolean isNullOrEmpty(String sString) {
        if (sString == null) return true;
        if (sString.isEmpty()) return  true;

        return  false;
    }

    public static String GetRandomName()
    {
        return UUID.randomUUID().toString();
    }

    public static Date getDate(Double oDouble) {
        double dDate = oDouble;
        long lLong = (long)dDate;
        return new Date(lLong);
    }

    public static  boolean isValidSession(UserSession oSession) {

        if (oSession == null) return false;
        if (isNullOrEmpty(oSession.getUserId()) ) return false;

        Date oLastTouch = getDate(oSession.getLastTouch());

        long lNow = new Date().getTime();
        long lLastTouch = oLastTouch.getTime();

        if ((lNow-lLastTouch)> m_iSessionValidityMinutes*60*1000) return  false;

        return  true;
    }

    //FIXME it may not work as expected in the following case:
    //the filename contains one or more dots ('.'):
    //  /home/username/my.lovely.file.name.zip
    public static String GetFileNameWithoutExtension(String sInputFile) {
        String sReturn = "";
        File oFile = new File(sInputFile);
        String sInputFileNameOnly = oFile.getName();

        // Create a clean layer id: the file name without any extension
        String [] asLayerIdSplit = sInputFileNameOnly.split("\\.");
        if (asLayerIdSplit!=null) {
            if (asLayerIdSplit.length>0){
                sReturn = asLayerIdSplit[0];
            }
        }

        return sReturn;
    }

    public static String GetFileNameExtension(String sInputFile) {
        String sReturn = "";
        File oFile = new File(sInputFile);
        String sInputFileNameOnly = oFile.getName();

        // Create a clean layer id: the file name without any extension
        String [] asLayerIdSplit = sInputFileNameOnly.split("\\.");
        if (asLayerIdSplit!=null) {
            if (asLayerIdSplit.length>0){
                sReturn = asLayerIdSplit[asLayerIdSplit.length-1];
            }
        }

        return sReturn;
    }

    public static void fixUpPermissions(Path destPath) throws IOException {
        Stream<Path> files = Files.list(destPath);
        files.forEach(path -> {
            if (Files.isDirectory(path)) {
                try {
                    fixUpPermissions(path);
                } catch (IOException e) {
                    e.printStackTrace();

                }
            }
            else {
                setExecutablePermissions(path);
            }
        });
        files.close();
    }

    private static void setExecutablePermissions(Path executablePathName) {
        if (IS_OS_UNIX) {
            Set<PosixFilePermission> permissions = new HashSet<>(Arrays.asList(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE,
                    PosixFilePermission.GROUP_READ,
                    PosixFilePermission.GROUP_EXECUTE,
                    PosixFilePermission.OTHERS_READ,
                    PosixFilePermission.OTHERS_EXECUTE));
            try {
                Files.setPosixFilePermissions(executablePathName, permissions);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    //XXX remove if not used
    public static boolean isValidEmail(String sEmail)
    {
    	boolean bIsValid = false;
    	if(!isNullOrEmpty(sEmail)) {
    		bIsValid = EmailValidator.getInstance().isValid(sEmail);
    	}
		return bIsValid;
    	
    }
    
	public static String GetFormatDate(Date oDate){
		
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(oDate);
	}

	public static String GetFormatFileDimension(long bytes) {
		 int unit = 1024;
		 if (bytes < unit) return bytes + " B";
		 int exp = (int) (Math.log(bytes) / Math.log(unit));
		 String pre = ("KMGTPE").charAt(exp-1) + "";
		 return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}

	private static char randomChar() {
		Random r = new Random();
		char c = (char)(r.nextInt(26) + 'a');
		return c;
	}
	
	
	public static String generateRandomPassword() {
		//String sPassword = UUID.randomUUID().toString().split("-")[0];
		String sPassword = new String(UUID.randomUUID().toString());
		sPassword = sPassword.replace('-', randomChar());
		//XXX shuffle string before returning
		return sPassword;
	}
	
	public static Boolean isFilePathPlausible(String sFullPath) {
		if(isNullOrEmpty(sFullPath)) {
			return false;
		}
		
		return true;
	}
	
	
	public static Boolean isServerNamePlausible(String sServer) {
		if(isNullOrEmpty(sServer)) {
			return false;
		}
		//Ok, let's inspect the server...
		Boolean bRes = false;
		bRes = InetAddressValidator.getInstance().isValid(sServer);
		if(!bRes) {
			//then maybe it's a domain
			bRes = DomainValidator.getInstance().isValid(sServer);
			if(!bRes) {
				//then maybe it's an URL
				bRes = UrlValidator.getInstance().isValid(sServer);
			}
			if(!bRes) {
				//then maybe it's localhost
				if(sServer.equals("localhost")) {
					bRes = true;
				}
			}
		}
		return bRes;
	}
	
	public static Boolean isPortNumberPlausible(Integer iPort) {
		if(null == iPort) {
			return false;
		}
		if( 0 <= iPort && iPort <= 65535 ){
			return true;
		}
		return false;
	}
	
	public static String[] convertPolygonToArray(String sArea){
		if(sArea.isEmpty()== true)
		{
			return null;
		}
		String sCleanedArea = sArea.replaceAll("[POLYGN()]", "");
		String[] asAreaPoints = sCleanedArea.split(",");
		return asAreaPoints;
	}

	public static boolean doesThisStringMeansTrue(String sString) {
		//default value is arbitrary!
		if(isNullOrEmpty(sString)) {
			return true;
		} else if(sString.equalsIgnoreCase("true")) {
			return true;
		} else if(sString.equalsIgnoreCase("1")) {
			return true;
		}
		return false;
	}
	
	public static void printToFile(String sFilePath, String sToBePrinted) {
		FileWriter oFileWeriter;
		try {
			oFileWeriter = new FileWriter(sFilePath);
			oFileWeriter.write(sToBePrinted);
			oFileWeriter.flush();
			oFileWeriter.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Confert a Polygon WKT String in a set of Lat Lon Points comma separated
	 * @param sContent 
	 * @return
	 */
	public static String polygonToBounds (String sContent) {
        sContent = sContent.replace("MULTIPOLYGON ","");
        sContent = sContent.replace("MULTIPOLYGON","");
        sContent = sContent.replace("POLYGON ","");
        sContent = sContent.replace("POLYGON","");
        sContent = sContent.replace("(((","");
        sContent = sContent.replace(")))","");
        sContent = sContent.replace("((","");
        sContent = sContent.replace("))","");

        String [] asContent = sContent.split(",");
        
        String sOutput = "";
        
        for (int iIndexBounds = 0; iIndexBounds < asContent.length; iIndexBounds++)
        {
            String sBounds = asContent[iIndexBounds];
            sBounds = sBounds.trim();
            String [] asNewBounds = sBounds.split(" ");

            if (iIndexBounds > 0) sOutput += ", ";
            
            try{
            	sOutput += asNewBounds[1] + "," + asNewBounds[0];
            }catch(Exception oEx){
                oEx.printStackTrace();
            }
                        
        }
        return sOutput;

	}	

	public static boolean isPlausibleHttpUrl(String sUrl) {
		if(isNullOrEmpty(sUrl)) {
			return false;
		}
		if(sUrl.startsWith("https://") || sUrl.startsWith("http://")) {
			return true;
		}
		return false;
	}
}

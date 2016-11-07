package wasdi.shared.utils;

import wasdi.shared.business.UserSession;

import java.io.File;
import java.util.Date;
import java.util.UUID;

/**
 * Created by p.campanella on 14/10/2016.
 */
public class Utils {
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
        if (isNullOrEmpty(oSession.getUserId()) ) return  false;

        Date oLastTouch = getDate(oSession.getLastTouch());

        long lNow = new Date().getTime();
        long lLastTouch = oLastTouch.getTime();

        if ((lNow-lLastTouch)> 24*60*60*1000) return  false;

        return  true;
    }

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

}

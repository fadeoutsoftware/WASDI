package wasdi.shared.utils;

import wasdi.shared.business.UserSession;

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

}

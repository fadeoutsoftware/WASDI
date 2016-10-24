package wasdi.shared.utils;

/**
 * Created by p.campanella on 14/10/2016.
 */
public class Utils {
    public static boolean isNullOrEmpty(String sString) {
        if (sString == null) return true;
        if (sString.isEmpty()) return  true;

        return  false;
    }

}

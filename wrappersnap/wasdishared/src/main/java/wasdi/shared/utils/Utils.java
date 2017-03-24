package wasdi.shared.utils;

import wasdi.shared.business.UserSession;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;
import java.util.stream.Stream;
import static org.apache.commons.lang.SystemUtils.IS_OS_UNIX;

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

}

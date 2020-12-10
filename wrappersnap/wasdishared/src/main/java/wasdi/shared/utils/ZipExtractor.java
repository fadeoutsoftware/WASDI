/*
 * Copyright (c) Fadeout Software 2020. Marco Menapace
 *
 */

package wasdi.shared.utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Utility class for zip extraction operation with some security considerations.
 *
 * @see <a href="http://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2018-1263">CVE-2018-1263</a>
 * @see <a href="http://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2018-16131">CVE-2018-16131</a>
 */
public class ZipExtractor {
    // These parameters must be instantiated using configurations values
    static final int BUFFER = 512;
    long m_lToobigtotal = 1024L * 1024L * 1024L; // Max size of unzipped total Data, 1 GB
    long m_lToobigsingle = 1024L * 1024L * 512L; // Max size of unzipped total Data, 512 MB
    int m_lToomany = 1024; // Maximum number of files that can be extracted

    /**
     * Safer version of the unzip method.
     * This method take in account the total dimension extracted and the dimension for each file.
     * If the extraction fails a clean method should be invoked, removing all the files extracted till
     * that point.
     * Try-with-resources approach for the ZipInputStream.
     * The extraction creates a temporary directory which is programmatically deleted if
     * - errors occurs (I.E. a file is bigger than the limit imposed)
     * - the unzip is completed without errors
     *
     * @param filename the filename of the Zip file to be extracted
     * @param path     The intended path in which the file should be extracted
     * @throws java.io.IOException Throws IO exception in case the zip file is not founded
     */
    public String unzip(String filename, String path) throws java.io.IOException {
        FileInputStream fis = new FileInputStream(filename);
        ZipEntry entry;
        int entries = 0;
        long total = 0;
        long single = 0;
        int iRandom = Math.abs(new SecureRandom().nextInt());
        String sTemp = "tmp-" + iRandom + File.separator;
        String sTempPath = path + sTemp;

        if (new File(sTempPath).mkdir()) System.out.println("Temp directory created");
        else throw new IOException("Can't create temporary dir " + sTempPath);

        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis))) {
            while ((entry = zis.getNextEntry()) != null) {
                System.out.println("Extracting: " + entry);
                int count;
                byte[] data = new byte[BUFFER];
                // Write the files to the disk, but ensure that the filename is valid,
                // and that the file is not insanely big
                String name = validateFilename(sTempPath + entry.getName(), sTempPath); // throws exception in case
                // Random used to mitigate attacks
                //File oTempDir = new File(path + "/tmp-" +iRandom+"/");
                if (entry.isDirectory()) {
                    System.out.println("Creating directory " + name);
                    if (new File(name).mkdir()) System.out.println("Directory created");
                    entries++; // count also a directory creation as an entry
                    continue;
                }
                FileOutputStream fos = new FileOutputStream(name);
                BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER);
                while (total + BUFFER <= m_lToobigtotal &&
                        single + BUFFER <= m_lToobigsingle &&
                        (count = zis.read(data, 0, BUFFER)) != -1) {
                    dest.write(data, 0, count);
                    total += count;
                    single += count;
                }
                dest.flush();
                dest.close();
                zis.closeEntry();
                entries++;
                if (single + BUFFER > m_lToobigsingle) {
                    cleanTempDir(sTempPath, sTemp);
                    throw new IllegalStateException("File being unzipped is too big. The limit is " + humanReadableByteCountSI(m_lToobigsingle));
                }
                if (total + BUFFER > m_lToobigtotal) {
                    cleanTempDir(sTempPath, sTemp);
                    throw new IllegalStateException("File extraction interrupted because total dimension is over extraction limits. The limit is " + humanReadableByteCountSI(m_lToobigtotal));
                }
                if (entries > m_lToomany) {
                    cleanTempDir(sTempPath, sTemp);
                    throw new IllegalStateException("Too many files to unzip.");
                }
                single = 0; // resets single file byte-counter
            }
            /// IF everything went well cp temp content to original folder (overwrite it's fine) and delete temp dir
            cleanTempDir(sTempPath, sTemp);
        }
        return sTempPath;
    }

    /**
     * Instantiates a ZipExtractor with default parameters
     */
    public ZipExtractor() {
    }

    /**
     * Instantiates a ZipExtractor passing 3 critical parameters (in bytes)
     *
     * @param toobigtotal     the total maximum size allowed for extraction
     * @param m_lToobigsingle the maximum single size for each file
     * @param m_lToomany      the maximum number of files allowed to be extracted
     */
    public ZipExtractor(long toobigtotal, long m_lToobigsingle, int m_lToomany) {
        this.m_lToobigtotal = toobigtotal;
        this.m_lToobigsingle = m_lToobigsingle;
        this.m_lToomany = m_lToomany;
    }

    /**
     * Checks that the the output dir is coherent with the current dir.
     * This methods filters out directory traversal attempts.
     *
     * @param filename    the file name of the zip file to be extracted
     * @param intendedDir the intended directory where the extraction must be done
     * @return the canonical path of the file
     * @throws java.io.IOException in case of the file is outside target extraction directory
     */
    private static String validateFilename(String filename, String intendedDir)
            throws java.io.IOException {
        File f = new File(filename);
        String canonicalPath = f.getCanonicalPath();

        File iD = new File(intendedDir);
        String canonicalID = iD.getCanonicalPath();

        if (canonicalPath.startsWith(canonicalID)) {
            return canonicalPath;
        } else {
            throw new IllegalStateException("File is outside extraction target directory.");
        }
    }

    /**
     * Util methods to obtain a human readable byte count(GB,MB,KB) from
     * a byte count.
     *
     * @param bytes the number of the bytes that should be considered
     * @return String with the human readable string (e.g. kB, MB, GB)
     */
    public static String humanReadableByteCountSI(long bytes) {
        if (-1000 < bytes && bytes < 1000) {
            return bytes + " B";
        }
        CharacterIterator ci = new StringCharacterIterator("kMGTPE");
        while (bytes <= -999_950 || bytes >= 999_950) {
            bytes /= 1000;
            ci.next();
        }
        return String.format("%.1f %cB", bytes / 1000.0, ci.current());
    }

    /**
     * After a successful extraction this method is invoked in order to move the
     * temporary folder content and cleanup the directory.
     * @param tempPath the temp path where to clean up
     * @param sTemp the folder name of the temp path [temp-{random-generated-id}]
     * @return True if the operation is done without errors nor exceptions. False instead
     */
    private boolean cleanTempDir(String tempPath, String sTemp) {
        File dir = new File(tempPath); // point one dir

        try {
            Files.walk(dir.toPath())
                    .sorted(Comparator.naturalOrder()). // this make the dir before other files
                    map(Path::toFile).
                    forEach(f -> {
                        try {
                            File dest = new File(f.getCanonicalPath().replace(sTemp, "")); // removes the tmp-part from the destination files
                            if (dest.isDirectory()) return; // checks the existence of the dir
                            /*System.out.println("moving " +
                                    f.getCanonicalFile().toPath() +
                                    " to " +
                                    dest.getCanonicalFile().toPath());*/

                            Files.copy(f.getCanonicalFile().toPath(), dest.getCanonicalFile().toPath(), StandardCopyOption.REPLACE_EXISTING);

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        try {
            deleteDirectory(dir.toPath());
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;

    }

    /**
     * Util method that recursively delete the path passed as input
     * On unix systems is equivalent to "rm -rf [path]"
     * @param toBeDeleted path to be deleted
     * @throws IOException
     */
    protected void deleteDirectory(Path toBeDeleted) throws IOException {
        Files.walk(toBeDeleted)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    /**
     * Setter of the parameter that checks the total size of the extracted files
     *
     * @param TOOBIGTOTAL size, in bytes, of the limit
     */
    public void setTOOBIGTOTAL(long TOOBIGTOTAL) {
        m_lToobigtotal = TOOBIGTOTAL;
    }

    /**
     * Setter of the parameter that checks the size of each single file
     *
     * @param TOOBIGSINGLE size, in bytes, of the limit
     */
    public void setTOOBIGSINGLE(long TOOBIGSINGLE) {
        m_lToobigsingle = TOOBIGSINGLE;
    }

    /**
     * Setter of the parameter that checks the number of files being extracted
     *
     * @param TOOMANY count of the files that must not be exceed
     */
    public void setTOOMANY(int TOOMANY) {
        m_lToomany = TOOMANY;
    }
}

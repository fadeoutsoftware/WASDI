/*
 * Copyright (c) Fadeout Software 2020. Marco Menapace
 *
 */
package wasdi.shared.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import wasdi.shared.utils.ZipExtractor;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Test class for safe extraction using it.fadeout.utils.zipextractor.ZipExtractor
 * Checks different condition. Before every single test the directory of the extraction is
 * cleaned. Tests also several settings of zip extractor itself.
 */
public class TestZipExtractor {

    private ZipExtractor zipExtractor;
    // final string pointing the path for testing
    private final static String sExtractionPath = "." + File.separator + "src" + File.separator + "test" + File.separator + "java" + File.separator + "extractionFolder" + File.separator;
    private final static String sExtractionFileName = "test.zip";

    /**
     * This methods cleans the out directory before and after each test.
     */
    @BeforeEach
    @AfterEach
    private void deleteExtractedFolder() {

        zipExtractor = new ZipExtractor();
        zipExtractor.setTOOBIGTOTAL(1024L * 1024L * 1024L * 1L); // Max size of unzipped total Data, 1 GB
        zipExtractor.setTOOBIGSINGLE(1024L * 1024L * 512L); // Max size of single file, 512 MB
        zipExtractor.setTOOMANY(30); // Max count of unzipped files

        // Directory clean
        // for each file
        for (File f : new File(sExtractionPath).listFiles()) {
            try {
                if (!f.getCanonicalFile().toString().contains(sExtractionFileName)) { // exclude test.zip
                    if (f.isDirectory()) zipExtractor.deleteDirectory(f.toPath());
                    else {
                        if (!f.delete()) {
                            System.err.println("Before Test extraction - can't delete file " + f.getCanonicalFile());
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * Test that the extraction is done by checking that the files list contained
     * in the original zip file are on the file system.
     */
    @Test
    void testExtractionDone() {
        // first unzip
        String sTempDir = "";
        try {

            sTempDir = zipExtractor.unzip(sExtractionPath + sExtractionFileName, sExtractionPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // then check files in temp dir
        File here = new File(sTempDir);
        ArrayList<String> path = new ArrayList<>();
        try {
            Files.walk(here.toPath().getParent()) // get the parent of the temp dir
                    .forEach(s -> path.add(s.toString()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        String[] fileListInTestZip = getFileListInTestZip();
        // this checks is done on temp dir but at this point the temp dir is deleted
        for (String s : fileListInTestZip) {
            Assertions.assertTrue(path.contains(sExtractionPath + s));
        }


    }

    /**
     * Checks that extraction fails for a single file being too big
     */
    @Test
    void testExtractionSingleFileTooBig() {
        zipExtractor.setTOOBIGSINGLE(1024 * 20); // aka 20 KB
        // with the current test must be checked that Big L script shouldn't be extracted
        Exception exception = Assertions.assertThrows(IllegalStateException.class, () -> {
            zipExtractor.unzip(sExtractionPath + sExtractionFileName, sExtractionPath);
        });
        // first unzip
        Assertions.assertTrue(exception.getMessage().contains("File being unzipped is too big"));

    }

    /**
     * Checks that extraction fails for total size exceeding the imposed limits
     */
    @Test
    void testExtractionAllFilesTooBig() {
        zipExtractor.setTOOBIGTOTAL(1); // aka 1 Byte
        // with the current test must be checked that Big L script shouldn't be extracted
        Exception exception = Assertions.assertThrows(IllegalStateException.class, () -> {
            zipExtractor.unzip(sExtractionPath + sExtractionFileName, sExtractionPath);
        });
        // first unzip
        Assertions.assertTrue(exception.getMessage().contains("File extraction interrupted because total dimension is over extraction limits"));

    }

    /**
     * Checks that extraction fails for total number of files exceed the imposed limit
     */
    @Test
    void testExtractionTooMany() {
        zipExtractor.setTOOMANY(5); // aka 5 files
        // with the current test must be checked that Big L script shouldn't be extracted
        Exception exception = Assertions.assertThrows(IllegalStateException.class, () -> {
            zipExtractor.unzip(sExtractionPath + sExtractionFileName, sExtractionPath);
        });
        // first unzip
        Assertions.assertTrue(exception.getMessage().contains("Too many files to unzip"));

    }


    /**
     * Utils methods used that returns a String array containing all the
     * entries inside the zip file.
     *
     * @return String array with the entries of the zip file used in these test
     */
    static String[] getFileListInTestZip() {
        try {
            FileInputStream fis = new FileInputStream(sExtractionPath + sExtractionFileName);
            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
            ArrayList<String> returnArray = new ArrayList<>();
            ZipEntry ze = zis.getNextEntry();
            while (ze != null) {
                //if (!ze.getName().endsWith(".zip")) {
                if (ze.getName().endsWith(File.separator)) {
                    returnArray.add(ze.getName().substring(0, ze.getName().length() - 1));
                } else {
                    returnArray.add(ze.getName());
                }
                //}
                ze = zis.getNextEntry();
            }
            return returnArray.toArray(new String[0]);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

    }
}

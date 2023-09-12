package wasdi.shared.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.FileUtils;

import wasdi.shared.utils.log.WasdiLog;

/**
 * Utility Class to work with Tar Files
 * @author p.campanella
 *
 */
public class TarUtils {
	
	/**
	 * Creates a tar file with inside the list of files in the input list.
	 * 
	 * This method put all the input files in the tar, assuming the subfolder structure obtained by
	 * the path of each input file "subtracted" by so-called sInputFilesBaseFolder.
	 * 
	 * @param asInputFilesFullPath List of Input Files. Each must be a full path included the name of the file
	 * @param sInputFilesBaseFolder Base Path of the input files. the relative path in the TAR will be input_full_path - input_base_path.
	 * @param sOutputTarFileFullPath Name of Output file. Must be a full path included the name of the file
	 * @return True if the file is created
	 */
	public static boolean tarFiles(List<String> asInputFilesFullPath, String sInputFilesBaseFolder, String sOutputTarFileFullPath) {
		try {
			// Stream to the output file
			OutputStream oOutputStream = Files.newOutputStream(Paths.get(sOutputTarFileFullPath));
			
			// Lets tar this stream
			BufferedOutputStream oBufferedOutputStream = new BufferedOutputStream(oOutputStream);
			TarArchiveOutputStream oTarOutputStream = new TarArchiveOutputStream(oBufferedOutputStream);
			
			// For all the files to add to the TAR
			for (String sInputFile : asInputFilesFullPath) {
				
				// This is the input file
				File oFile = new File(sInputFile);
				// The relative path is the actual path starting from the base folder
				String sRelativePath = sInputFile.replace(sInputFilesBaseFolder, "");
				
				// Create the Tar Entry
				TarArchiveEntry oTarArchiveEntry = new TarArchiveEntry(sRelativePath);
				
				// Write in the tar
				oTarArchiveEntry.setSize(Files.size(oFile.toPath()));
				oTarOutputStream.putArchiveEntry(oTarArchiveEntry);
				oTarOutputStream.write(FileUtils.readFileToByteArray(oFile));
				
				// Close it
				oTarOutputStream.closeArchiveEntry();
			}
			
			// Close the tar stream
			oTarOutputStream.close();
			
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("TarUtils.tarFiles: exception ", oEx);
			return false;
		}
	}

}

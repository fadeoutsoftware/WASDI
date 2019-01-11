/**
 * Created by Cristiano Nattero on 2018-12-19
 * 
 * Fadeout software
 *
 */
package wasdi.filebuffer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import wasdi.shared.utils.Utils;

/**
 * @author c.nattero
 *
 */
public class FileDownloaderSupplier {

	private static final Map<String, Supplier<FileDownloader>> s_aoDownloadSupplier;

	static {
		final Map<String, Supplier<FileDownloader>> aoDownloaders = new HashMap<>();
		aoDownloaders.put("SENTINEL", DhUSFileDownloader::new);
		aoDownloaders.put("MATERA", DhUSFileDownloader::new);
		aoDownloaders.put("FEDEO", DhUSFileDownloader::new);
		aoDownloaders.put("PROBAV", PROBAVFileDownloader::new);
		aoDownloaders.put("ONDA", ONDAFileDownloader::new);

		s_aoDownloadSupplier = Collections.unmodifiableMap(aoDownloaders);
	}

	public FileDownloader supplyFileDownloader(String sFileDownloaderType) {
		FileDownloader oResult = null;
		if(!Utils.isNullOrEmpty(sFileDownloaderType)) {
			Supplier<FileDownloader> oDownloadFile = s_aoDownloadSupplier.get(sFileDownloaderType);
			if(oDownloadFile != null ) {
				oResult = oDownloadFile.get();
			}
		}

		return oResult;
	}

}

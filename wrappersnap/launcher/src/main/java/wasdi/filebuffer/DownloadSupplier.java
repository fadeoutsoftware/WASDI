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
public class DownloadSupplier {

	private static final Map<String, Supplier<DownloadFile>> s_aoDownloadSupplier;

	static {
		final Map<String, Supplier<DownloadFile>> aoDownloaders = new HashMap<>();
		aoDownloaders.put("SENTINEL", DhUSDownloadFile::new);
		aoDownloaders.put("MATERA", DhUSDownloadFile::new);
		aoDownloaders.put("FEDEO", DhUSDownloadFile::new);
		aoDownloaders.put("PROBAV", PROBAVDownloadFile::new);
		aoDownloaders.put("ONDA", ONDADownloadFile::new);

		s_aoDownloadSupplier = Collections.unmodifiableMap(aoDownloaders);
	}

	public DownloadFile supplyDownloader(String sDownloadFileType) {
		DownloadFile oResult = null;
		if(!Utils.isNullOrEmpty(sDownloadFileType)) {
			Supplier<DownloadFile> oDownloadFile = s_aoDownloadSupplier.get(sDownloadFileType);
			if(oDownloadFile != null ) {
				oResult = oDownloadFile.get();
			}
		}

		return oResult;
	}

}

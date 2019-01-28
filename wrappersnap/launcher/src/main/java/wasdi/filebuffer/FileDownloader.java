/**
 * Created by Cristiano Nattero on 2019-01-11
 * 
 * Fadeout software
 *
 */
package wasdi.filebuffer;

/**
 * @author c.nattero
 *
 */
public class FileDownloader {
	
	ProviderAdapter m_oProviderAdapter;
	
	FileDownloader( ProviderAdapter oProviderAdapter ){
		if(null!=oProviderAdapter) {
			m_oProviderAdapter = oProviderAdapter;
		} else {
			throw new NullPointerException();
		}
	}
	
	protected void notify(int iPercent) {
		m_oProviderAdapter.UpdateProcessProgress(iPercent);
	}
	
	//TODO some abstract void download, to be overridden in derived classes (http: vs file:)
	//TODO derived class httpFileDownloader
	//TODO derived class localFileDownloader

}

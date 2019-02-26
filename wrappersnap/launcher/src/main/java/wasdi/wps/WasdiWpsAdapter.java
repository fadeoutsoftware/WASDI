/**
 * Created by Cristiano Nattero on 2019-02-25
 * 
 * Fadeout software
 *
 */
package wasdi.wps;

/**
 * @author c.nattero
 *
 */
public class WasdiWpsAdapter extends WpsAdapter {
	//TODO implement ProcessWorkspaceUpdateNotifier
	
	static {
		s_sWpsHost = "http://178.22.66.96/geoserver/wps"; 
		s_sVersion = "2.0.0";
	}


	@Override
	public String getStatus() {
		//TODO query status and update last status
		
		return m_sResponse;
	}

	@Override
	public String getResult() {
		// TODO Auto-generated method stub
		return null;
	}

}

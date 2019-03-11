/**
 * Created by Cristiano Nattero on 2019-02-26
 * 
 * Fadeout software
 *
 */
package wasdi.wps;

/**
 * @author c.nattero
 *
 */
public class N52DemoWpsAdapter extends WpsAdapter {
	
	static {
		s_sWpsHost = "http://geoprocessing.demo.52north.org:8080/wps/WebProcessingService";
		s_sVersion = "2.0.0";
	}
	

	/* (non-Javadoc)
	 * @see wasdi.wps.WpsExecutionClient#getStatus()
	 */
	@Override
	public String getStatus() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getResult() {
		// TODO Auto-generated method stub
		return null;
	}

}

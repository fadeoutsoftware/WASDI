/**
 * Created by Cristiano Nattero on 2019-02-20
 * 
 * Fadeout software
 *
 */
package wasdi.wps;

/**
 * @author c.nattero
 *
 */
public abstract class WpsExecutionClient {
	
	protected String m_sWpsHost;
	
	 
	public abstract int execute();
	//TODO getStatus
	//TODO getResult
}

/**
 * Created by Cristiano Nattero on 2019-02-25
 * 
 * Fadeout software
 *
 */
package wasdi;

import wasdi.wps.WpsExecutionClient;
import wasdi.wps.WpsFactory;

/**
 * @author c.nattero
 *
 */
public class TestWps {
	
	public static void main(String[] args) throws Exception {
		WpsFactory oFactory = new WpsFactory();
		WpsExecutionClient oClient = oFactory.supply("wasdi");

//		ProcessWorkspaceRepository oRepo = new ProcessWorkspaceRepository();
//		ProcessWorkspace oProcessWorkspace = new ProcessWorkspace();
		
		oClient.execute();
	}
}

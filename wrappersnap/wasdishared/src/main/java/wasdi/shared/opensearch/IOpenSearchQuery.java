package wasdi.shared.opensearch;

import java.net.URISyntaxException;

public interface IOpenSearchQuery {
	
	String ExecuteQuery(String sQuery) throws URISyntaxException;
}

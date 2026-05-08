package wasdi.shared.config;

import java.util.ArrayList;

/**
 * Configuration of a catalogues: represents a supported platform 
 * and indicates the list of catalogues that support that platform in
 * order of priority of query.
 * 
 * @author p.campanella
 *
 */
public class CatalogueConfig {
	
	/**
	 * Code of the Platform Type as defined by the Platform class
	 */
	public String platform;
	
	/**
	 * List of catalogues supporting the Platform Type. 
	 * Each string is the code of a QueryExecutor/ProviderAdapter.
	 * The first element is the one with highest priority.
	 */
	public ArrayList<String> catalogues;

}

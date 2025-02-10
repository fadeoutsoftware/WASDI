package wasdi.shared.config;

import java.util.List;

/**
 * Data Provider Configuration
 * 
 * All Data Providers has:
 * 	.name
 * 	.description
 * 	.link
 * 	.user
 * 	.password
 * 
 * Different data providers uses also different parameters. This config has been re-written after that
 * many data providers were already avaiable and is done to support also legacy objects.
 * 
 * Is due to control for Each Data Provider witch params are really needed
 * 
 * @author p.campanella
 *
 */
public class DataProviderConfig {
	
	/**
	 * Name/Code of the data Provider.
	 * This name is required to get QueryExecutors and ProviderAdapters
	 */
	public String name;
	/**
	 * Full Class Name and Path of the class implementing this Query Executor
	 */
	public String classpath;
	/**
	 * Description of the Data Provider
	 */
	public String description;
	/**
	 * Link of Data Provider web site
	 */
	public String link;
	/**
	 * Size required when making multiple queries to make a WASDI searchList operation.
	 * We make paginated request to the provider and collect all results for WASDI Client 
	 */
	public String searchListPageSize;
	/**
	 * Default protocol for data fetch in this node.
	 * Can be
	 * "https://"
	 * "file://"
	 */
	public String defaultProtocol;
	/**
	 * Path of a parser config util json file that can be used
	 * by QueryExecutor (and or QueryTranslator) to convert a WASDI query
	 * in equivalent provider query 
	 */
	public String parserConfig;
	/**
	 * User of the Data Provider
	 */
	public String user;
	/**
	 * Password of the Data Provider
	 */
	public String password;
	/**
	 * API Key of the Data Provider
	 */
	public String apiKey;
	/**
	 * Local base folder where the data archive is.
	 * It can be used by Data Providers that allows direct file access
	 * when the defaultProtocol is file://
	 */
	public String localFilesBasePath;
	/**
	 * API address of the data provider
	 */
	public String urlDomain;
	/**
	 * Specific connection timeout for this data provider
	 */
	public String connectionTimeout;
	/**
	 * Specific read timeout for this data provider
	 */
	public String readTimeout;
	/**
	 * Path to a file that can be used to store specific Provider Adapter configs
	 */
	public String adapterConfig;
	/**
	 * Code of the cloud provider where the Data Provider is hosted. The code must 
	 * be one of the codes of the entities in the cloudproviders Database table
	 */
	public String cloudProvider;
	/**
	 * List of the platforms supported by this data provider 
	 */
	public List<String> supportedPlatforms;
}

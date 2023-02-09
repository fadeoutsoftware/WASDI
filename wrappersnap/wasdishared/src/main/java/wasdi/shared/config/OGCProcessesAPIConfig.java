package wasdi.shared.config;

/**
 * Specific configuration of the OGC Processes API WASDI Server Implementation
 * @author p.campanella
 *
 */
public class OGCProcessesAPIConfig {
	/**
	 * Base address of the OGC Rest API Endpoint
	 */
	public String baseAddress;
	/**
	 * Title of the server: this will be returned by the landing page from the core resource
	 */
	public String landingTitle;
	/**
	 * Description of the server: this will be returned by the landing page from the core resource
	 */
	public String landingDescription;
	/**
	 * Default type to use for links (ie application/json)
	 */
	public String defaultLinksType;
	/**
	 * Default language to use for links
	 */
	public String defaultLinksLang;
	/**
	 * Link to the API specification: has the format httplink;relationtype;title
	 */
	public String landingLinkServiceDefinition;
	/**
	 * Link to the conformance api: has the format relativepath;relationtype;title. 
	 */
	public String landingLinkConformance;
	/**
	 * Link to the processes api: has the format relativepath;relationtype;title
	 */	
	public String landingLinkProcesses;
	/**
	 * Link to the jobs api: has the format relativepath;relationtype;title
	 */	
	public String landingLinkJobs;	
	/**
	 * List of links to add to the conforms to api response, separated by ;
	 */
	public String conformsTo;
	/**
	 * Link to the service description: has the format httplink;relationtype;title
	 */
	public String landingLinkServiceDescription;
	/**
	 * Flat to activate the validation mode. DANGEROUS: it can disable authentication 
	 */
	public boolean validationModeOn = false;
	/**
	 * User Id to use for validation
	 */
	public String validationUserId = "";
	/**
	 * Session Id to use for validation
	 */
	public String validationSessionId = "";
	/**
	 * Default application to use for validation mode
	 */
	public String validationEchoProcessId = "hellowasdi";
}

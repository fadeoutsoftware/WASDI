package wasdi.shared.business;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Reprents a cloud provider where a WASDI node can be installed
 * 
 * @author p.campanella
 *
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CloudProvider {
	
	/**
	 * Unique Cloud Provider Guid 
	 */
	private String cloudProviderId;
	
	/**
	 * Cloud Provider Code as declared in the Node entity
	 */
	private String code;
	
	/**
	 * main link to the cloud provider site
	 */
	private String mainLink;
	
	/**
	 * Mail to contact the cloud provider
	 */
	private String contact;
	
	/**
	 * Direct link to the the SLA of the cloud provider
	 */
	private String slaLink;
	
	/**
	 * Brief description
	 */
	private String description;

}

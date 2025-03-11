package wasdi.shared.config;

/**
 * Configuration of the Multi Cloud Load Balancer. Contains the parameters
 * that can be used to configure the Multi Cloud Load Balancer
 * @author p.campanella
 *
 */
public class LoadBalancerConfig {
	
	/**
	 * Flag to decide if the main node should be included in the list of nodes to evaluate to decide where to create a workspace
	 * or where to run an application. By default is false, meaning the main node is NOT included as a Computational Node
	 */
	public boolean includeMainClusterAsNode = false;
	
	/**
	 * Max percentage of disk space occupied to consider a node avaiable 
	 */
	public int diskOccupiedSpaceMaxPercentage = 90;
	
	/**
	 * Each node sends metrics. Here we define how many seconds are the "age" limit of the metrics.
	 * It means that if a node has metrics older than this parameters in seconds, it is considered down
	 */
	public int metricsMaxAgeSeconds = 600;
	
	/**
	 * Min RAM required for a Node: under this limit, it is considered a low performance mode and so penalized in the final ranking
	 */
	public int minTotalMemoryGBytes = 30;

}

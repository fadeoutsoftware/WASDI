package wasdi.shared.config;

public class StorageUsageControl {
	
	/**
	 * Maximum storage space (in bytes) for FREE subscription plans
	 */
	public Long storageSizeFreeSubscription = 20000000000L;
	
	/**
	 * Maximum storage space (in bytes) for STANDARD subscription plans
	 */
	public Long storageSizeStandardSubscription = 107374182400L;
	
	/**
	 * Number of days to wait for, before proceeding to the deletion of a workspace, after the warning has been sent by email to the user
	 */
	public int deletionDelayFromWarning = 10;
	
	/**
	 * If the flag is true, the workspaces belonging to users with invalid subscription will not be actually deleted, but a mail will be sent to the admins.
	 */
	public boolean isDeletionInTestMode = true;
	
	/**
	 * Configuration of the warning email to be sent to the users before deleting their workspaces, when their subscriptions are no longer valid
	 */
	public WarningEmailConfig warningEmailConfig = new WarningEmailConfig();
}

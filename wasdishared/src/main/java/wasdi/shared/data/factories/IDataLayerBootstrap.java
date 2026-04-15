package wasdi.shared.data.factories;

/**
 * Data layer lifecycle abstraction.
 */
public interface IDataLayerBootstrap {
	/**
	 * Initialize data layer connections and configuration.
	 */
	void init();

	/**
	 * Shutdown data layer resources.
	 */
	void shutdown();
}

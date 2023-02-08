package wasdi.shared.config;

import java.util.List;

/**
 * Stripe configuration
 * @author PetruPetrescu
 *
 */
public class StripeConfig {

	/**
	 * Api Key
	 */
	public String apiKey;

	/**
	 * List of product-related configuration-entries
	 */
	public List<StripeProductConfig> products;

}

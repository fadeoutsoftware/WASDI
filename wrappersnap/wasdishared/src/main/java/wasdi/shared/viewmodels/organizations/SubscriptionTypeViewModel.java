package wasdi.shared.viewmodels.organizations;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * View model class for Subscription Type
 * @author PetruPetrescu on 19/01/2023
 *
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionTypeViewModel {

	private String typeId;
	private String name;
	private String description;

}

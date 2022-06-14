package wasdi.shared.business.ecostress;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class EcoStressLocation {

	private String type;
	private List<List<List<Double>>> coordinates;

}

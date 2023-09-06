package wasdi.shared.business.modis11a2;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class ModisLocation {
	private String type;
	private List<List<List<Double>>> coordinates;

}

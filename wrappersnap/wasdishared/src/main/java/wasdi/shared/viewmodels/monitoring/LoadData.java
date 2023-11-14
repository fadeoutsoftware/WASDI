package wasdi.shared.viewmodels.monitoring;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class LoadData {

	private Double load1;
	private Double load5;
	private Double load15;

}
package wasdi.shared.viewmodels.processors;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Image Import View Model
 * 
 * Represents the parameters of an image import operation
 * 
 * @author PetruPetrescu
 *
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ImageImportViewModel {

	private String fileUrl;
	private String name;
	private String provider;
	private String workspace;
	private String bbox;
	private String parent;

}

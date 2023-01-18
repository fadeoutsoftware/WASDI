package wasdi.shared.viewmodels.ogcprocesses;

import java.util.ArrayList;
import java.util.List;

public class DescriptionType {
	private String title = null;
	private String description = null;
	private List<String> keywords = new ArrayList<String>();
	private List<Metadata> metadata = new ArrayList<Metadata>();
	private AllOfdescriptionTypeAdditionalParameters additionalParameters = null;
	
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public List<String> getKeywords() {
		return keywords;
	}
	public void setKeywords(List<String> keywords) {
		this.keywords = keywords;
	}
	public List<Metadata> getMetadata() {
		return metadata;
	}
	public void setMetadata(List<Metadata> metadata) {
		this.metadata = metadata;
	}
	public AllOfdescriptionTypeAdditionalParameters getAdditionalParameters() {
		return additionalParameters;
	}
	public void setAdditionalParameters(AllOfdescriptionTypeAdditionalParameters additionalParameters) {
		this.additionalParameters = additionalParameters;
	}
}

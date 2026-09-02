package wasdi.shared.viewmodels.labelling.templates;

import java.util.ArrayList;

import wasdi.shared.viewmodels.labelling.attributes.AttributeViewModel;

public class TemplateViewModel {
	public String id;
	public String name;
	public String description;
	public boolean hasPolygons;
	public boolean hasLines;
	public boolean hasPoints;
	public ArrayList<AttributeViewModel> attributes= new ArrayList<>();
	public boolean isFixedColour;
	public Integer fixedColour;
	public String colourAttributeName;
	public String creator;
	public long created;
	public boolean canEdit;
}

package wasdi.shared.viewmodels.labelling.attributes;

import java.util.ArrayList;

import wasdi.shared.business.labelling.Attribute;
import wasdi.shared.business.labelling.AttributeType;

public class AttributeViewModel {
	public String name;
	public String type;
	public ArrayList<String> categories;
	public ArrayList<Integer> colours;
	public boolean isMandatory;
	
	public static AttributeViewModel getFromEntity(Attribute oAttribute) {
		if (oAttribute==null) return null;
		
		AttributeViewModel oAttributeViewModel = new AttributeViewModel();
		oAttributeViewModel.name = oAttribute.getName();
		oAttributeViewModel.type = oAttribute.getType().name();
		oAttributeViewModel.categories.addAll(oAttribute.getCategories());
		oAttributeViewModel.colours.addAll(oAttribute.getColours());
		
		return oAttributeViewModel;
		
	}
	
	public static Attribute convertToEntity(AttributeViewModel oAttributeViewModel) {
		if (oAttributeViewModel==null) return null;
		
		Attribute oAttribute = new Attribute();
		oAttribute.setName(oAttributeViewModel.name) ;
		oAttribute.setType(AttributeType.valueOf(oAttributeViewModel.type));
		oAttribute.getCategories().addAll(oAttributeViewModel.categories);
		oAttribute.getColours().addAll(oAttributeViewModel.colours);
		
		return oAttribute;
		
	}	
}

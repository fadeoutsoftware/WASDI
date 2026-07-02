package wasdi.shared.viewmodels.labelling.attributes;

import java.util.ArrayList;

import wasdi.shared.business.labelling.Attribute;
import wasdi.shared.business.labelling.AttributeType;

public class AttributeViewModel {
	public String name;
	public String type;
	public ArrayList<String> categories  = new ArrayList<>(); 
	public ArrayList<Integer> colours  = new ArrayList<>();
	public String value;
	public boolean isMandatory;
	
	public static AttributeViewModel getFromEntity(Attribute oAttribute) {
	    if (oAttribute == null) return null;

	    AttributeViewModel oVM = new AttributeViewModel();
	    oVM.name = oAttribute.getName();
	    oVM.type = oAttribute.getType().name();
	    oVM.categories = new ArrayList<>();
	    oVM.colours = new ArrayList<>();
	    oVM.value = oAttribute.getValue();

	    if (oAttribute.getCategories() != null) {
	        oVM.categories.addAll(oAttribute.getCategories());
	    }
	    if (oAttribute.getColours() != null) {
	        oVM.colours.addAll(oAttribute.getColours());
	    }

	    return oVM;
	}
	
	public static Attribute convertToEntity(AttributeViewModel oAttributeViewModel) {
	    if (oAttributeViewModel == null) return null;

	    Attribute oAttribute = new Attribute();
	    oAttribute.setName(oAttributeViewModel.name);
	    oAttribute.setType(AttributeType.valueOf(oAttributeViewModel.type.toUpperCase()));
	    oAttribute.setValue(oAttributeViewModel.value);

	    // Guard against null lists
	    if (oAttributeViewModel.categories != null) {
	        oAttribute.getCategories().addAll(oAttributeViewModel.categories);
	    }
	    if (oAttributeViewModel.colours != null) {
	        oAttribute.getColours().addAll(oAttributeViewModel.colours);
	    }

	    return oAttribute;
	}
}

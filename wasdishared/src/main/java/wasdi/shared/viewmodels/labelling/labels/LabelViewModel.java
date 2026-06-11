package wasdi.shared.viewmodels.labelling.labels;

import java.util.ArrayList;

import wasdi.shared.viewmodels.labelling.attributes.AttributeViewModel;

public class LabelViewModel {
	public String id;
	public String geometry;
	public boolean isPoint;
	public boolean isLine;
	public boolean isPolygon;
	public boolean isMultiPolygon;
	public String annotator;
	public String image;
	public ArrayList<String> reviewers = new ArrayList<>();
	public ArrayList<ReviewNoteViewModel> reviewNotes = new ArrayList<>();
	public ArrayList<AttributeViewModel> attributes = new ArrayList<>();
	public int reviewCount;
	public boolean isValidated;
	public String creatorId;
	public String datasetId;
}
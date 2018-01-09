package wasdi.shared.viewmodels;

import java.util.HashSet;

import org.esa.snap.rcp.imgfilter.model.Filter;
import org.esa.snap.rcp.imgfilter.model.Filter.Operation;

public class FilterBandViewModel {
	
    String name;
    String shorthand;
    Operation operation;
    boolean editable;
    HashSet<String> tags;
    double[] kernelElements;
    int kernelWidth;
    int kernelHeight;
    double kernelQuotient;
    int kernelOffsetX;
    int kernelOffsetY;
    
    public Filter getFilter() {
    	Filter filter = new Filter(name, shorthand, operation, kernelWidth, kernelHeight, kernelElements, kernelQuotient, tags.toArray(new String[tags.size()]));
    	filter.setEditable(editable);
    	filter.setKernelOffset(kernelOffsetX, kernelOffsetY);
		return filter;
    }
    
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getShorthand() {
		return shorthand;
	}
	public void setShorthand(String shorthand) {
		this.shorthand = shorthand;
	}
	public Operation getOperation() {
		return operation;
	}
	public void setOperation(Operation operation) {
		this.operation = operation;
	}
	public boolean isEditable() {
		return editable;
	}
	public void setEditable(boolean editable) {
		this.editable = editable;
	}
	public HashSet<String> getTags() {
		return tags;
	}
	public void setTags(HashSet<String> tags) {
		this.tags = tags;
	}
	public double[] getKernelElements() {
		return kernelElements;
	}
	public void setKernelElements(double[] kernelElements) {
		this.kernelElements = kernelElements;
	}
	public int getKernelWidth() {
		return kernelWidth;
	}
	public void setKernelWidth(int kernelWidth) {
		this.kernelWidth = kernelWidth;
	}
	public int getKernelHeight() {
		return kernelHeight;
	}
	public void setKernelHeight(int kernelHeight) {
		this.kernelHeight = kernelHeight;
	}
	public double getKernelQuotient() {
		return kernelQuotient;
	}
	public void setKernelQuotient(double kernelQuotient) {
		this.kernelQuotient = kernelQuotient;
	}
	public int getKernelOffsetX() {
		return kernelOffsetX;
	}
	public void setKernelOffsetX(int kernelOffsetX) {
		this.kernelOffsetX = kernelOffsetX;
	}
	public int getKernelOffsetY() {
		return kernelOffsetY;
	}
	public void setKernelOffsetY(int kernelOffsetY) {
		this.kernelOffsetY = kernelOffsetY;
	}    
}

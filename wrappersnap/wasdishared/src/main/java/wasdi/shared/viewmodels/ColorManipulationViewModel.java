package wasdi.shared.viewmodels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.esa.snap.core.datamodel.ImageInfo.HistogramMatching;

public class ColorManipulationViewModel {

	ColorWithValueViewModel[] colors;
	
	int[] histogramBins;
	float histogramWidth;
	float histogramMin;
	float histogramMax;
	
	ColorViewModel noDataColor;
	
	HistogramMatching histogramMatching;
	HistogramMatching[] histogramMathcingValues;
	
	boolean discreteColor;

	public ColorViewModel getNoDataColor() {
		return noDataColor;
	}

	public void setNoDataColor(ColorViewModel noDataColor) {
		this.noDataColor = noDataColor;
	}

	public HistogramMatching getHistogramMatching() {
		return histogramMatching;
	}

	public void setHistogramMatching(HistogramMatching histogramMatching) {
		this.histogramMatching = histogramMatching;
	}

	public boolean isDiscreteColor() {
		return discreteColor;
	}

	public void setDiscreteColor(boolean discreteColor) {
		this.discreteColor = discreteColor;
	}

	public int[] getHistogramBins() {
		return histogramBins;
	}

	public void setHistogramBins(int[] histogramBins) {
		this.histogramBins = histogramBins;
	}

	public float getHistogramWidth() {
		return histogramWidth;
	}

	public void setHistogramWidth(float histogramWidth) {
		this.histogramWidth = histogramWidth;
	}

	public float getHistogramMin() {
		return histogramMin;
	}

	public void setHistogramMin(float histogramMin) {
		this.histogramMin = histogramMin;
	}

	public float getHistogramMax() {
		return histogramMax;
	}

	public void setHistogramMax(float histogramMax) {
		this.histogramMax = histogramMax;
	}

	public HistogramMatching[] getHistogramMathcingValues() {
		return histogramMathcingValues;
	}

	public void setHistogramMathcingValues(HistogramMatching[] histogramMathcingValues) {
		this.histogramMathcingValues = histogramMathcingValues;
	}

	public ColorWithValueViewModel[] getColors() {
		return colors;
	}

	public void setColors(ColorWithValueViewModel[] colors) {
		this.colors = colors;
	}
	
}
package wasdi.shared.viewmodels;

import java.awt.Color;

public class ColorWithValueViewModel extends ColorViewModel {
	float value;

	public ColorWithValueViewModel() {
	}
	
	public ColorWithValueViewModel(float value, Color color) {
		super(color);
		this.value = value;
	}

	public float getValue() {
		return value;
	}

	public void setValue(float value) {
		this.value = value;
	}	
}

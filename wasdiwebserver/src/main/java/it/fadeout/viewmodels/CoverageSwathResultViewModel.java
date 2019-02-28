package it.fadeout.viewmodels;

import java.util.ArrayList;
import java.util.Date;

public class CoverageSwathResultViewModel {
	public int IdCoverageSwathResultViewModel;
	public String SwathName;
	public String SatelliteName;
	public String SensorName;
	public String SensorLookDirection;
	public String SensorMode;
	public String Angle;
	public String SensorType;
	public String CoveredAreaName;
	public double Coverage;
	public Date AcquisitionStartTime;
	public Date AcquisitionEndTime;
	public double AcquisitionDuration;
	public double CoverageWidth;
	public double CoverageLength;
	public String SwathFootPrint;
	public String FrameFootPrint;
	public String CoveredArea;
	public int IdTriggerType;
	public String FrameFootPrintGeometry;
	public Boolean IsAscending;
	public ArrayList<CoverageSwathResultViewModel> aoChilds;
	
	public CoverageSwathResultViewModel(){
			
	}
	
	public CoverageSwathResultViewModel(CoverageSwathResultViewModel oCopy) {
		this.AcquisitionDuration = oCopy.AcquisitionDuration;
		this.AcquisitionEndTime = oCopy.AcquisitionEndTime;
		this.AcquisitionStartTime = oCopy.AcquisitionStartTime;
		this.Angle = oCopy.Angle;
		this.Coverage = oCopy.Coverage;
		this.CoverageLength = oCopy.CoverageLength;
		this.CoverageWidth = oCopy.CoverageWidth;
		this.CoveredAreaName = oCopy.CoveredAreaName;
		this.SwathFootPrint = oCopy.SwathFootPrint;
		this.SatelliteName = oCopy.SatelliteName;
		this.SensorLookDirection = oCopy.SensorLookDirection;
		this.SensorMode = oCopy.SensorMode;
		this.SensorName = oCopy.SensorName;
		this.SwathName = oCopy.SwathName;
		this.SensorType = oCopy.SensorType;
		this.IdTriggerType = oCopy.IdTriggerType;
		this.IsAscending = oCopy.IsAscending;
		this.aoChilds = oCopy.aoChilds;
	}
}

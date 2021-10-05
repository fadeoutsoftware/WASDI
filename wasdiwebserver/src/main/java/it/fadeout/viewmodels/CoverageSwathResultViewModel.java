package it.fadeout.viewmodels;

import java.util.ArrayList;
import java.util.Date;

/**
 * Result of an orbit search
 * @author p.campanella
 *
 */
public class CoverageSwathResultViewModel {
	/**
	 * Unique id
	 */
	public int IdCoverageSwathResultViewModel;
	/**
	 * Name of the swath
	 */
	public String SwathName;
	/**
	 * Name of the satellite
	 */
	public String SatelliteName;
	/**
	 * Name of the sensor
	 */
	public String SensorName;
	/**
	 * Look direction
	 */
	public String SensorLookDirection;
	/**
	 * Sensor Mode
	 */
	public String SensorMode;
	/**
	 * View Angle
	 */
	public String Angle;
	/**
	 * Sensor Type
	 */
	public String SensorType;
	/**
	 * Name of the Covered Area
	 */
	public String CoveredAreaName;
	/**
	 * Percentage of Coverage
	 */
	public double Coverage;
	/**
	 * Acquisition start time
	 */
	public Date AcquisitionStartTime;
	/**
	 * Acquisition end time
	 */
	public Date AcquisitionEndTime;
	/**
	 * Acquisition duration
	 */
	public double AcquisitionDuration;
	/**
	 * Width of the coverage
	 */
	public double CoverageWidth;
	/**
	 * Length of the coverage
	 */
	public double CoverageLength;
	/**
	 * Footprint of the swath
	 */
	public String SwathFootPrint;
	/**
	 * Footprint of the frame
	 */
	public String FrameFootPrint;
	/**
	 * Covered Area
	 */
	public String CoveredArea;
	/**
	 * Trigger Id
	 */
	public int IdTriggerType;
	/**
	 * Geometry of the frame
	 */
	public String FrameFootPrintGeometry;
	/**
	 * True if ascendig, false otherwise
	 */
	public Boolean IsAscending;
	/**
	 * List of child coverages
	 */
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

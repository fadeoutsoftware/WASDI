package wasdi.shared;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.esa.s1tbx.calibration.gpf.CalibrationOp;
import org.esa.s1tbx.sar.gpf.MultilookOp;
import org.esa.s1tbx.sar.gpf.geometric.RangeDopplerGeocodingOp;
import org.esa.s1tbx.sar.gpf.orbits.ApplyOrbitFileOp;
import org.esa.s2tbx.radiometry.TndviOp;
import org.esa.snap.core.gpf.OperatorSpi;

public class SnapOperatorFactory {

	public static Class getOperatorClass(String sOperation) {
		switch (sOperation) {
		case LauncherOperations.APPLYORBIT:			
			return ApplyOrbitFileOp.class;
		case LauncherOperations.CALIBRATE:
			return CalibrationOp.class;
		case LauncherOperations.MULTILOOKING:
			return MultilookOp.class;
		case LauncherOperations.TERRAIN:
			return RangeDopplerGeocodingOp.class;
		case LauncherOperations.NDVI:
			return TndviOp.class;			
		default:
			return null;			
		}
	}
	
	public static OperatorSpi getOperatorSpi(String sOperation) {

		Class oOperatorClass = getOperatorClass(sOperation);
		
		try {
			Method oMethod = oOperatorClass.getMethod("Spi");
			Object oObj = oMethod.invoke(oOperatorClass);
			if (oObj instanceof OperatorSpi) {
				return (OperatorSpi) oObj;				
			}
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		
		return null;
		
	}
	
	
}

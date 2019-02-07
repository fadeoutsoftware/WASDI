package org.geoserver.wpasdi;

import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geoserver.wps.gs.GeoServerProcess;

@DescribeProcess(title="helloWASDI", description="WASDI Hello WPS Sample")
public class WASDIHello implements GeoServerProcess {
	
	@DescribeResult(name="result", description="output result")
	public String execute(@DescribeParameter(name="name", description="name of who is calling") String name) {
		return "Hello from WASDI dear " + name;
	}
}

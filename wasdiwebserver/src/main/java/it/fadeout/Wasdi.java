package it.fadeout;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;

import org.apache.catalina.tribes.util.UUIDGenerator;

import it.fadeout.rest.resources.DownloadResource;
import it.fadeout.rest.resources.OpenSearchResource;
import it.fadeout.rest.resources.WasdiResource;

public class Wasdi extends Application {
	@Context
	ServletConfig m_oServletConfig;
	
	@Context
	ServletContext m_oContext;	

	@Override
	public Set<Class<?>> getClasses() {
		final Set<Class<?>> classes = new HashSet<Class<?>>();
		// register resources and features
		classes.add(DownloadResource.class);
		classes.add(OpenSearchResource.class);
		classes.add(WasdiResource.class);
		return classes;
	}
	
	
	@PostConstruct
	public void initWasdi() {
		
	}
	
	public static String GetSerializationFileName()
	{
		return UUID.randomUUID().toString();
	}
	
}

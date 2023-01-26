package ogc.wasdi.processes.providers;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.ogcprocesses.OgcProcessesViewModel;

@Provider
@Produces(MediaType.TEXT_HTML)
public class OgcProcessesViewModelBodyWriter implements MessageBodyWriter<OgcProcessesViewModel> {

	@Override
	public boolean isWriteable(Class<?> oInputType, Type oGenericType, Annotation[] aoAnnotations, MediaType oMediaType) {
		
		try {
			if (oMediaType.toString().contentEquals(MediaType.TEXT_HTML)) {
				
				boolean bReturn = OgcProcessesViewModel.class.isAssignableFrom(oInputType); 
				return bReturn;
			}			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("OgcProcessesViewModelBodyWriter.isWriteable: exception " + oEx.toString());
		}
		
		return false;
	}

	@Override
	public void writeTo(OgcProcessesViewModel oViewModel, Class<?> oType, Type oGenericType, Annotation[] aoAnnotations,
			MediaType oMediaType, MultivaluedMap<String, Object> oHttpHeaders, OutputStream oEntityStream)
			throws IOException, WebApplicationException {
		
		try {
			
	        Writer oWriter = new PrintWriter(oEntityStream);
	        oWriter.write("<html>");
	        oWriter.write("<body>");
	        oWriter.write("<p>" + oViewModel.toString() + "</p>");
	        oWriter.write("</body>");
	        oWriter.write("</html>");

	        oWriter.flush();
	        oWriter.close();		
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("OgcProcessesViewModelBodyWriter.writeTo: exception " + oEx.toString());
		}		
		
	}

	@Override
	public long getSize(OgcProcessesViewModel t, Class<?> type, Type genericType, Annotation[] annotations,
			MediaType mediaType) {
		return -1l;
	}

}

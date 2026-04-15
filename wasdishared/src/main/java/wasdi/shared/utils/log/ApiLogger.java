package wasdi.shared.utils.log;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.message.EntryMessage;
import org.apache.logging.log4j.message.FlowMessageFactory;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.util.MessageSupplier;
import org.apache.logging.log4j.util.Supplier;

import wasdi.shared.config.WasdiConfig;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.Utils;

public class ApiLogger implements Logger {
	
	protected String m_sProcessWorkspaceId = "";
	
	/**
	 * Call the API fo WASDI to log. NOTE this does not uses HttpUtils because the class itself 
	 * uses logs => we create a stack overflow. 
	 * @param sMessage
	 */
	protected void apiLog(String sMessage) {
		
		if (Utils.isNullOrEmpty(sMessage)) return;
		
		try {
			
			String sUrl = WasdiConfig.Current.baseUrl;
			
			// Safe programming
			if (!sUrl.endsWith("/")) sUrl += "/";
			// Compose the API string
			sUrl += "processors/logs/add?processworkspace="+m_sProcessWorkspaceId;
			
			try {
				byte [] ayBytes = sMessage.getBytes();
				
				Map<String, String> asHeaders=HttpUtils.getStandardHeaders("");
				
				// Create the Url and relative Connection
				URL oURL = new URL(sUrl);
				HttpURLConnection oConnection = (HttpURLConnection) oURL.openConnection();
				
				// Set Read Timeout
				oConnection.setReadTimeout(WasdiConfig.Current.readTimeout);
				// Set Connection Timeout
				oConnection.setConnectTimeout(WasdiConfig.Current.connectionTimeout);				
				
				// Optional: default is GET
				oConnection.setRequestMethod("POST");
				// We accept all
				oConnection.setRequestProperty("Accept", "*/*");
				oConnection.setDoOutput(true);
				
				
				// Do we have input headers?
				if (asHeaders != null) {
					// Yes: add all to our request
					for (Entry<String, String> asEntry : asHeaders.entrySet()) {
						oConnection.setRequestProperty(asEntry.getKey(), asEntry.getValue());
					}
				}
				
				oConnection.setFixedLengthStreamingMode(ayBytes.length);
				oConnection.connect();
				
				try (OutputStream oOutputStream = oConnection.getOutputStream()) {
					oOutputStream.write(ayBytes);
				}				

				try {
					// Read server response code
					int iResponseCode = oConnection.getResponseCode();
									
				} catch (Exception oEint) {
					System.err.println("HttpUtils.httpGet: Exception " + oEint);
				} finally {
					oConnection.disconnect();
				}
			} catch (Exception oE) {
				System.err.println("HttpUtils.httpGet: Exception " + oE);
			}
		}
		catch (Exception oEx) {
			System.err.println("ApiLogger.apiLog: error " + oEx.getMessage());
		}		
	}

	@Override
	public void catching(Level level, Throwable throwable) {
		return;
	}

	@Override
	public void catching(Throwable throwable) {
		return;
	}

	@Override
	public void debug(Marker marker, Message message) {
		return;
	}

	@Override
	public void debug(Marker marker, Message message, Throwable throwable) {
		return;
	}

	@Override
	public void debug(Marker marker, MessageSupplier messageSupplier) {
		return;
	}

	@Override
	public void debug(Marker marker, MessageSupplier messageSupplier, Throwable throwable) {
		return;
	}

	@Override
	public void debug(Marker marker, CharSequence message) {
		return;
	}

	@Override
	public void debug(Marker marker, CharSequence message, Throwable throwable) {
		return;
		
	}

	@Override
	public void debug(Marker marker, Object message) {
		return;
	}

	@Override
	public void debug(Marker marker, Object message, Throwable throwable) {
		return;
	}

	@Override
	public void debug(Marker marker, String message) {
		return;
	}

	@Override
	public void debug(Marker marker, String message, Object... params) {
		return;
		
	}

	@Override
	public void debug(Marker marker, String message, Supplier<?>... paramSuppliers) {
		return;
		
	}

	@Override
	public void debug(Marker marker, String message, Throwable throwable) {
		return;
		
	}

	@Override
	public void debug(Marker marker, Supplier<?> messageSupplier) {
		return;
		
	}

	@Override
	public void debug(Marker marker, Supplier<?> messageSupplier, Throwable throwable) {
		return;
		
	}

	@Override
	public void debug(Message message) {
		return;
		
	}

	@Override
	public void debug(Message message, Throwable throwable) {
		return;
		
	}

	@Override
	public void debug(MessageSupplier messageSupplier) {
		return;
		
	}

	@Override
	public void debug(MessageSupplier messageSupplier, Throwable throwable) {
		return;
		
	}

	@Override
	public void debug(CharSequence message) {
		return;
		
	}

	@Override
	public void debug(CharSequence message, Throwable throwable) {
		return;
		
	}

	@Override
	public void debug(Object message) {
		return;
		
	}

	@Override
	public void debug(Object message, Throwable throwable) {
		return;
		
	}

	@Override
	public void debug(String message) {
		apiLog(message);
	}

	@Override
	public void debug(String message, Object... params) {
		return;
		
	}

	@Override
	public void debug(String message, Supplier<?>... paramSuppliers) {
		return;
		
	}

	@Override
	public void debug(String message, Throwable throwable) {
		return;
		
	}

	@Override
	public void debug(Supplier<?> messageSupplier) {
		return;
		
	}

	@Override
	public void debug(Supplier<?> messageSupplier, Throwable throwable) {
		return;
		
	}

	@Override
	public void debug(Marker marker, String message, Object p0) {
		return;
		
	}

	@Override
	public void debug(Marker marker, String message, Object p0, Object p1) {
		return;
		
	}

	@Override
	public void debug(Marker marker, String message, Object p0, Object p1, Object p2) {
		return;
		
	}

	@Override
	public void debug(Marker marker, String message, Object p0, Object p1, Object p2, Object p3) {
		return;
		
	}

	@Override
	public void debug(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4) {
		return;
		
	}

	@Override
	public void debug(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {
		return;
		
	}

	@Override
	public void debug(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5,
			Object p6) {
		return;
		
	}

	@Override
	public void debug(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5,
			Object p6, Object p7) {
		return;
		
	}

	@Override
	public void debug(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5,
			Object p6, Object p7, Object p8) {
		return;
		
	}

	@Override
	public void debug(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5,
			Object p6, Object p7, Object p8, Object p9) {
		return;
		
	}

	@Override
	public void debug(String message, Object p0) {
		return;
		
	}

	@Override
	public void debug(String message, Object p0, Object p1) {
		return;
		
	}

	@Override
	public void debug(String message, Object p0, Object p1, Object p2) {
		return;
		
	}

	@Override
	public void debug(String message, Object p0, Object p1, Object p2, Object p3) {
		return;
		
	}

	@Override
	public void debug(String message, Object p0, Object p1, Object p2, Object p3, Object p4) {
		return;
		
	}

	@Override
	public void debug(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {
		return;
		
	}

	@Override
	public void debug(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {
		return;
		
	}

	@Override
	public void debug(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
			Object p7) {
		return;
		
	}

	@Override
	public void debug(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
			Object p7, Object p8) {
		return;
		
	}

	@Override
	public void debug(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
			Object p7, Object p8, Object p9) {
		return;
		
	}

	@Override
	public void entry() {
		return;
		
	}

	@Override
	public void entry(Object... params) {
		return;
		
	}

	@Override
	public void error(Marker marker, Message message) {
		return;
		
	}

	@Override
	public void error(Marker marker, Message message, Throwable throwable) {
		return;
		
	}

	@Override
	public void error(Marker marker, MessageSupplier messageSupplier) {
		return;
		
	}

	@Override
	public void error(Marker marker, MessageSupplier messageSupplier, Throwable throwable) {
		return;
		
	}

	@Override
	public void error(Marker marker, CharSequence message) {
		return;
		
	}

	@Override
	public void error(Marker marker, CharSequence message, Throwable throwable) {
		return;
		
	}

	@Override
	public void error(Marker marker, Object message) {
		return;
		
	}

	@Override
	public void error(Marker marker, Object message, Throwable throwable) {
		return;
		
	}

	@Override
	public void error(Marker marker, String message) {
		return;
		
	}

	@Override
	public void error(Marker marker, String message, Object... params) {
		return;
		
	}

	@Override
	public void error(Marker marker, String message, Supplier<?>... paramSuppliers) {
		return;
		
	}

	@Override
	public void error(Marker marker, String message, Throwable throwable) {
		return;
		
	}

	@Override
	public void error(Marker marker, Supplier<?> messageSupplier) {
		return;
		
	}

	@Override
	public void error(Marker marker, Supplier<?> messageSupplier, Throwable throwable) {
		return;
		
	}

	@Override
	public void error(Message message) {
		return;
		
	}

	@Override
	public void error(Message message, Throwable throwable) {
		return;
		
	}

	@Override
	public void error(MessageSupplier messageSupplier) {
		return;
		
	}

	@Override
	public void error(MessageSupplier messageSupplier, Throwable throwable) {
		return;
		
	}

	@Override
	public void error(CharSequence message) {
		return;
		
	}

	@Override
	public void error(CharSequence message, Throwable throwable) {
		return;
		
	}

	@Override
	public void error(Object message) {
		return;
		
	}

	@Override
	public void error(Object message, Throwable throwable) {
		return;
		
	}

	@Override
	public void error(String message) {
		apiLog(message);
		
	}

	@Override
	public void error(String message, Object... params) {
		return;
		
	}

	@Override
	public void error(String message, Supplier<?>... paramSuppliers) {
		return;
		
	}

	@Override
	public void error(String message, Throwable throwable) {
		String sExceptionText = "";
		if (throwable!=null) {
			sExceptionText = throwable.getMessage();
		}
		apiLog(message + " - " + sExceptionText);
		return;
	}

	@Override
	public void error(Supplier<?> messageSupplier) {
		return;
		
	}

	@Override
	public void error(Supplier<?> messageSupplier, Throwable throwable) {
		return;
		
	}

	@Override
	public void error(Marker marker, String message, Object p0) {
		return;
		
	}

	@Override
	public void error(Marker marker, String message, Object p0, Object p1) {
		return;
		
	}

	@Override
	public void error(Marker marker, String message, Object p0, Object p1, Object p2) {
		return;
		
	}

	@Override
	public void error(Marker marker, String message, Object p0, Object p1, Object p2, Object p3) {
		return;
		
	}

	@Override
	public void error(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4) {
		return;
		
	}

	@Override
	public void error(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {
		return;
		
	}

	@Override
	public void error(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5,
			Object p6) {
		return;
		
	}

	@Override
	public void error(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5,
			Object p6, Object p7) {
		return;
		
	}

	@Override
	public void error(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5,
			Object p6, Object p7, Object p8) {
		return;
		
	}

	@Override
	public void error(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5,
			Object p6, Object p7, Object p8, Object p9) {
		return;
		
	}

	@Override
	public void error(String message, Object p0) {
		return;
		
	}

	@Override
	public void error(String message, Object p0, Object p1) {
		return;
		
	}

	@Override
	public void error(String message, Object p0, Object p1, Object p2) {
		return;
		
	}

	@Override
	public void error(String message, Object p0, Object p1, Object p2, Object p3) {
		return;
		
	}

	@Override
	public void error(String message, Object p0, Object p1, Object p2, Object p3, Object p4) {
		return;
		
	}

	@Override
	public void error(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {
		return;
		
	}

	@Override
	public void error(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {
		return;
		
	}

	@Override
	public void error(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
			Object p7) {
		return;
		
	}

	@Override
	public void error(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
			Object p7, Object p8) {
		return;
		
	}

	@Override
	public void error(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
			Object p7, Object p8, Object p9) {
		return;
		
	}

	@Override
	public void exit() {
		return;
		
	}

	@Override
	public <R> R exit(R result) {
		return null;
	}

	@Override
	public void fatal(Marker marker, Message message) {
		return;
		
	}

	@Override
	public void fatal(Marker marker, Message message, Throwable throwable) {
		return;
		
	}

	@Override
	public void fatal(Marker marker, MessageSupplier messageSupplier) {
		return;
		
	}

	@Override
	public void fatal(Marker marker, MessageSupplier messageSupplier, Throwable throwable) {
		return;
		
	}

	@Override
	public void fatal(Marker marker, CharSequence message) {
		return;
		
	}

	@Override
	public void fatal(Marker marker, CharSequence message, Throwable throwable) {
		return;
		
	}

	@Override
	public void fatal(Marker marker, Object message) {
		return;
		
	}

	@Override
	public void fatal(Marker marker, Object message, Throwable throwable) {
		return;
		
	}

	@Override
	public void fatal(Marker marker, String message) {
		return;
		
	}

	@Override
	public void fatal(Marker marker, String message, Object... params) {
		return;
		
	}

	@Override
	public void fatal(Marker marker, String message, Supplier<?>... paramSuppliers) {
		return;
		
	}

	@Override
	public void fatal(Marker marker, String message, Throwable throwable) {
		return;
		
	}

	@Override
	public void fatal(Marker marker, Supplier<?> messageSupplier) {
		return;
		
	}

	@Override
	public void fatal(Marker marker, Supplier<?> messageSupplier, Throwable throwable) {
		return;
		
	}

	@Override
	public void fatal(Message message) {
		return;
		
	}

	@Override
	public void fatal(Message message, Throwable throwable) {
		return;
		
	}

	@Override
	public void fatal(MessageSupplier messageSupplier) {
		return;
		
	}

	@Override
	public void fatal(MessageSupplier messageSupplier, Throwable throwable) {
		return;
		
	}

	@Override
	public void fatal(CharSequence message) {
		return;
		
	}

	@Override
	public void fatal(CharSequence message, Throwable throwable) {
		return;
		
	}

	@Override
	public void fatal(Object message) {
		return;
		
	}

	@Override
	public void fatal(Object message, Throwable throwable) {
		return;
		
	}

	@Override
	public void fatal(String message) {
		return;
		
	}

	@Override
	public void fatal(String message, Object... params) {
		return;
		
	}

	@Override
	public void fatal(String message, Supplier<?>... paramSuppliers) {
		return;
		
	}

	@Override
	public void fatal(String message, Throwable throwable) {
		return;
		
	}

	@Override
	public void fatal(Supplier<?> messageSupplier) {
		return;
		
	}

	@Override
	public void fatal(Supplier<?> messageSupplier, Throwable throwable) {
		return;
		
	}

	@Override
	public void fatal(Marker marker, String message, Object p0) {
		return;
		
	}

	@Override
	public void fatal(Marker marker, String message, Object p0, Object p1) {
		return;
		
	}

	@Override
	public void fatal(Marker marker, String message, Object p0, Object p1, Object p2) {
		return;
		
	}

	@Override
	public void fatal(Marker marker, String message, Object p0, Object p1, Object p2, Object p3) {
		return;
		
	}

	@Override
	public void fatal(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4) {
		return;
		
	}

	@Override
	public void fatal(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {
		return;
		
	}

	@Override
	public void fatal(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5,
			Object p6) {
		return;
		
	}

	@Override
	public void fatal(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5,
			Object p6, Object p7) {
		return;
		
	}

	@Override
	public void fatal(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5,
			Object p6, Object p7, Object p8) {
		return;
		
	}

	@Override
	public void fatal(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5,
			Object p6, Object p7, Object p8, Object p9) {
		return;
		
	}

	@Override
	public void fatal(String message, Object p0) {
		return;
		
	}

	@Override
	public void fatal(String message, Object p0, Object p1) {
		return;
		
	}

	@Override
	public void fatal(String message, Object p0, Object p1, Object p2) {
		return;
		
	}

	@Override
	public void fatal(String message, Object p0, Object p1, Object p2, Object p3) {
		return;
		
	}

	@Override
	public void fatal(String message, Object p0, Object p1, Object p2, Object p3, Object p4) {
		return;
		
	}

	@Override
	public void fatal(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {
		return;
		
	}

	@Override
	public void fatal(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {
		return;
		
	}

	@Override
	public void fatal(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
			Object p7) {
		return;
		
	}

	@Override
	public void fatal(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
			Object p7, Object p8) {
		return;
		
	}

	@Override
	public void fatal(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
			Object p7, Object p8, Object p9) {
		return;
		
	}

	@Override
	public Level getLevel() {
		return null;
	}

	@Override
	public <MF extends MessageFactory> MF getMessageFactory() {
		return null;
	}

	@Override
	public FlowMessageFactory getFlowMessageFactory() {
		return null;
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public void info(Marker marker, Message message) {
		return;
		
	}

	@Override
	public void info(Marker marker, Message message, Throwable throwable) {
		return;
		
	}

	@Override
	public void info(Marker marker, MessageSupplier messageSupplier) {
		return;
		
	}

	@Override
	public void info(Marker marker, MessageSupplier messageSupplier, Throwable throwable) {
		return;
		
	}

	@Override
	public void info(Marker marker, CharSequence message) {
		return;
		
	}

	@Override
	public void info(Marker marker, CharSequence message, Throwable throwable) {
		return;
		
	}

	@Override
	public void info(Marker marker, Object message) {
		return;
		
	}

	@Override
	public void info(Marker marker, Object message, Throwable throwable) {
		return;
		
	}

	@Override
	public void info(Marker marker, String message) {
		return;
		
	}

	@Override
	public void info(Marker marker, String message, Object... params) {
		return;
		
	}

	@Override
	public void info(Marker marker, String message, Supplier<?>... paramSuppliers) {
		return;
		
	}

	@Override
	public void info(Marker marker, String message, Throwable throwable) {
		return;
		
	}

	@Override
	public void info(Marker marker, Supplier<?> messageSupplier) {
		return;
		
	}

	@Override
	public void info(Marker marker, Supplier<?> messageSupplier, Throwable throwable) {
		return;
		
	}

	@Override
	public void info(Message message) {
		return;
		
	}

	@Override
	public void info(Message message, Throwable throwable) {
		return;
		
	}

	@Override
	public void info(MessageSupplier messageSupplier) {
		return;
		
	}

	@Override
	public void info(MessageSupplier messageSupplier, Throwable throwable) {
		return;
		
	}

	@Override
	public void info(CharSequence message) {
		return;
		
	}

	@Override
	public void info(CharSequence message, Throwable throwable) {
		return;
		
	}

	@Override
	public void info(Object message) {
		return;
		
	}

	@Override
	public void info(Object message, Throwable throwable) {
		return;
		
	}

	@Override
	public void info(String message) {
		apiLog(message);
	}

	@Override
	public void info(String message, Object... params) {
		return;
		
	}

	@Override
	public void info(String message, Supplier<?>... paramSuppliers) {
		return;
		
	}

	@Override
	public void info(String message, Throwable throwable) {
		return;
		
	}

	@Override
	public void info(Supplier<?> messageSupplier) {
		return;
		
	}

	@Override
	public void info(Supplier<?> messageSupplier, Throwable throwable) {
		return;
		
	}

	@Override
	public void info(Marker marker, String message, Object p0) {
		return;
		
	}

	@Override
	public void info(Marker marker, String message, Object p0, Object p1) {
		return;
		
	}

	@Override
	public void info(Marker marker, String message, Object p0, Object p1, Object p2) {
		return;
		
	}

	@Override
	public void info(Marker marker, String message, Object p0, Object p1, Object p2, Object p3) {
		return;
		
	}

	@Override
	public void info(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4) {
		return;
		
	}

	@Override
	public void info(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {
		return;
		
	}

	@Override
	public void info(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5,
			Object p6) {
		return;
		
	}

	@Override
	public void info(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5,
			Object p6, Object p7) {
		return;
		
	}

	@Override
	public void info(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5,
			Object p6, Object p7, Object p8) {
		return;
		
	}

	@Override
	public void info(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5,
			Object p6, Object p7, Object p8, Object p9) {
		return;
		
	}

	@Override
	public void info(String message, Object p0) {
		return;
		
	}

	@Override
	public void info(String message, Object p0, Object p1) {
		return;
		
	}

	@Override
	public void info(String message, Object p0, Object p1, Object p2) {
		return;
		
	}

	@Override
	public void info(String message, Object p0, Object p1, Object p2, Object p3) {
		return;
		
	}

	@Override
	public void info(String message, Object p0, Object p1, Object p2, Object p3, Object p4) {
		return;
		
	}

	@Override
	public void info(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {
		return;
		
	}

	@Override
	public void info(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {
		return;
		
	}

	@Override
	public void info(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
			Object p7) {
		return;
		
	}

	@Override
	public void info(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
			Object p7, Object p8) {
		return;
		
	}

	@Override
	public void info(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
			Object p7, Object p8, Object p9) {
		return;
		
	}

	@Override
	public boolean isDebugEnabled() {
		return false;
	}

	@Override
	public boolean isDebugEnabled(Marker marker) {
		return false;
	}

	@Override
	public boolean isEnabled(Level level) {
		return false;
	}

	@Override
	public boolean isEnabled(Level level, Marker marker) {
		return false;
	}

	@Override
	public boolean isErrorEnabled() {
		return false;
	}

	@Override
	public boolean isErrorEnabled(Marker marker) {
		return false;
	}

	@Override
	public boolean isFatalEnabled() {
		return false;
	}

	@Override
	public boolean isFatalEnabled(Marker marker) {
		return false;
	}

	@Override
	public boolean isInfoEnabled() {
		return false;
	}

	@Override
	public boolean isInfoEnabled(Marker marker) {
		return false;
	}

	@Override
	public boolean isTraceEnabled() {
		return false;
	}

	@Override
	public boolean isTraceEnabled(Marker marker) {
		return false;
	}

	@Override
	public boolean isWarnEnabled() {
		return false;
	}

	@Override
	public boolean isWarnEnabled(Marker marker) {
		return false;
	}

	@Override
	public void log(Level level, Marker marker, Message message) {
		return;
		
	}

	@Override
	public void log(Level level, Marker marker, Message message, Throwable throwable) {
		return;
		
	}

	@Override
	public void log(Level level, Marker marker, MessageSupplier messageSupplier) {
		return;
		
	}

	@Override
	public void log(Level level, Marker marker, MessageSupplier messageSupplier, Throwable throwable) {
		return;
		
	}

	@Override
	public void log(Level level, Marker marker, CharSequence message) {
		return;
		
	}

	@Override
	public void log(Level level, Marker marker, CharSequence message, Throwable throwable) {
		return;
		
	}

	@Override
	public void log(Level level, Marker marker, Object message) {
		return;
		
	}

	@Override
	public void log(Level level, Marker marker, Object message, Throwable throwable) {
		return;
		
	}

	@Override
	public void log(Level level, Marker marker, String message) {
		return;
		
	}

	@Override
	public void log(Level level, Marker marker, String message, Object... params) {
		return;
		
	}

	@Override
	public void log(Level level, Marker marker, String message, Supplier<?>... paramSuppliers) {
		return;
		
	}

	@Override
	public void log(Level level, Marker marker, String message, Throwable throwable) {
		return;
		
	}

	@Override
	public void log(Level level, Marker marker, Supplier<?> messageSupplier) {
		return;
		
	}

	@Override
	public void log(Level level, Marker marker, Supplier<?> messageSupplier, Throwable throwable) {
		return;
		
	}

	@Override
	public void log(Level level, Message message) {
		return;
		
	}

	@Override
	public void log(Level level, Message message, Throwable throwable) {
		return;
		
	}

	@Override
	public void log(Level level, MessageSupplier messageSupplier) {
		return;
		
	}

	@Override
	public void log(Level level, MessageSupplier messageSupplier, Throwable throwable) {
		return;
		
	}

	@Override
	public void log(Level level, CharSequence message) {
		return;
		
	}

	@Override
	public void log(Level level, CharSequence message, Throwable throwable) {
		return;
		
	}

	@Override
	public void log(Level level, Object message) {
		return;
		
	}

	@Override
	public void log(Level level, Object message, Throwable throwable) {
		return;
		
	}

	@Override
	public void log(Level level, String message) {
		return;
		
	}

	@Override
	public void log(Level level, String message, Object... params) {
		return;
		
	}

	@Override
	public void log(Level level, String message, Supplier<?>... paramSuppliers) {
		return;
		
	}

	@Override
	public void log(Level level, String message, Throwable throwable) {
		return;
		
	}

	@Override
	public void log(Level level, Supplier<?> messageSupplier) {
		return;
		
	}

	@Override
	public void log(Level level, Supplier<?> messageSupplier, Throwable throwable) {
		return;
		
	}

	@Override
	public void log(Level level, Marker marker, String message, Object p0) {
		return;
		
	}

	@Override
	public void log(Level level, Marker marker, String message, Object p0, Object p1) {
		return;
		
	}

	@Override
	public void log(Level level, Marker marker, String message, Object p0, Object p1, Object p2) {
		return;
		
	}

	@Override
	public void log(Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3) {
		return;
		
	}

	@Override
	public void log(Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4) {
		return;
		
	}

	@Override
	public void log(Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4,
			Object p5) {
		return;
		
	}

	@Override
	public void log(Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4,
			Object p5, Object p6) {
		return;
		
	}

	@Override
	public void log(Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4,
			Object p5, Object p6, Object p7) {
		return;
		
	}

	@Override
	public void log(Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4,
			Object p5, Object p6, Object p7, Object p8) {
		return;
		
	}

	@Override
	public void log(Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4,
			Object p5, Object p6, Object p7, Object p8, Object p9) {
		return;
		
	}

	@Override
	public void log(Level level, String message, Object p0) {
		return;
		
	}

	@Override
	public void log(Level level, String message, Object p0, Object p1) {
		return;
		
	}

	@Override
	public void log(Level level, String message, Object p0, Object p1, Object p2) {
		return;
		
	}

	@Override
	public void log(Level level, String message, Object p0, Object p1, Object p2, Object p3) {
		return;
		
	}

	@Override
	public void log(Level level, String message, Object p0, Object p1, Object p2, Object p3, Object p4) {
		return;
		
	}

	@Override
	public void log(Level level, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {
		return;
		
	}

	@Override
	public void log(Level level, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5,
			Object p6) {
		return;
		
	}

	@Override
	public void log(Level level, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5,
			Object p6, Object p7) {
		return;
		
	}

	@Override
	public void log(Level level, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5,
			Object p6, Object p7, Object p8) {
		return;
		
	}

	@Override
	public void log(Level level, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5,
			Object p6, Object p7, Object p8, Object p9) {
		return;
		
	}

	@Override
	public void printf(Level level, Marker marker, String format, Object... params) {
		return;
		
	}

	@Override
	public void printf(Level level, String format, Object... params) {
		return;
		
	}

	@Override
	public <T extends Throwable> T throwing(Level level, T throwable) {
		return null;
	}

	@Override
	public <T extends Throwable> T throwing(T throwable) {
		return null;
	}

	@Override
	public void trace(Marker marker, Message message) {
		return;
		
	}

	@Override
	public void trace(Marker marker, Message message, Throwable throwable) {
		return;
		
	}

	@Override
	public void trace(Marker marker, MessageSupplier messageSupplier) {
		return;
		
	}

	@Override
	public void trace(Marker marker, MessageSupplier messageSupplier, Throwable throwable) {
		return;
		
	}

	@Override
	public void trace(Marker marker, CharSequence message) {
		return;
		
	}

	@Override
	public void trace(Marker marker, CharSequence message, Throwable throwable) {
		return;
		
	}

	@Override
	public void trace(Marker marker, Object message) {
		return;
		
	}

	@Override
	public void trace(Marker marker, Object message, Throwable throwable) {
		return;
		
	}

	@Override
	public void trace(Marker marker, String message) {
		return;
		
	}

	@Override
	public void trace(Marker marker, String message, Object... params) {
		return;
		
	}

	@Override
	public void trace(Marker marker, String message, Supplier<?>... paramSuppliers) {
		return;
		
	}

	@Override
	public void trace(Marker marker, String message, Throwable throwable) {
		return;
		
	}

	@Override
	public void trace(Marker marker, Supplier<?> messageSupplier) {
		return;
		
	}

	@Override
	public void trace(Marker marker, Supplier<?> messageSupplier, Throwable throwable) {
		return;
		
	}

	@Override
	public void trace(Message message) {
		return;
		
	}

	@Override
	public void trace(Message message, Throwable throwable) {
		return;
		
	}

	@Override
	public void trace(MessageSupplier messageSupplier) {
		return;
		
	}

	@Override
	public void trace(MessageSupplier messageSupplier, Throwable throwable) {
		return;
		
	}

	@Override
	public void trace(CharSequence message) {
		return;
		
	}

	@Override
	public void trace(CharSequence message, Throwable throwable) {
		return;
		
	}

	@Override
	public void trace(Object message) {
		return;
		
	}

	@Override
	public void trace(Object message, Throwable throwable) {
		return;
		
	}

	@Override
	public void trace(String message) {
		return;
		
	}

	@Override
	public void trace(String message, Object... params) {
		return;
		
	}

	@Override
	public void trace(String message, Supplier<?>... paramSuppliers) {
		return;
		
	}

	@Override
	public void trace(String message, Throwable throwable) {
		return;
		
	}

	@Override
	public void trace(Supplier<?> messageSupplier) {
		return;
		
	}

	@Override
	public void trace(Supplier<?> messageSupplier, Throwable throwable) {
		return;
		
	}

	@Override
	public void trace(Marker marker, String message, Object p0) {
		return;
		
	}

	@Override
	public void trace(Marker marker, String message, Object p0, Object p1) {
		return;
		
	}

	@Override
	public void trace(Marker marker, String message, Object p0, Object p1, Object p2) {
		return;
		
	}

	@Override
	public void trace(Marker marker, String message, Object p0, Object p1, Object p2, Object p3) {
		return;
		
	}

	@Override
	public void trace(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4) {
		return;
		
	}

	@Override
	public void trace(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {
		return;
		
	}

	@Override
	public void trace(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5,
			Object p6) {
		return;
		
	}

	@Override
	public void trace(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5,
			Object p6, Object p7) {
		return;
		
	}

	@Override
	public void trace(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5,
			Object p6, Object p7, Object p8) {
		return;
		
	}

	@Override
	public void trace(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5,
			Object p6, Object p7, Object p8, Object p9) {
		return;
		
	}

	@Override
	public void trace(String message, Object p0) {
		return;
		
	}

	@Override
	public void trace(String message, Object p0, Object p1) {
		return;
		
	}

	@Override
	public void trace(String message, Object p0, Object p1, Object p2) {
		return;
		
	}

	@Override
	public void trace(String message, Object p0, Object p1, Object p2, Object p3) {
		return;
		
	}

	@Override
	public void trace(String message, Object p0, Object p1, Object p2, Object p3, Object p4) {
		return;
		
	}

	@Override
	public void trace(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {
		return;
		
	}

	@Override
	public void trace(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {
		return;
		
	}

	@Override
	public void trace(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
			Object p7) {
		return;
		
	}

	@Override
	public void trace(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
			Object p7, Object p8) {
		return;
		
	}

	@Override
	public void trace(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
			Object p7, Object p8, Object p9) {
		return;
		
	}

	@Override
	public EntryMessage traceEntry() {
		return null;
	}

	@Override
	public EntryMessage traceEntry(String format, Object... params) {
		return null;
	}

	@Override
	public EntryMessage traceEntry(Supplier<?>... paramSuppliers) {
		return null;
	}

	@Override
	public EntryMessage traceEntry(String format, Supplier<?>... paramSuppliers) {
		return null;
	}

	@Override
	public EntryMessage traceEntry(Message message) {
		return null;
	}

	@Override
	public void traceExit() {
		return;
		
	}

	@Override
	public <R> R traceExit(R result) {
		return null;
	}

	@Override
	public <R> R traceExit(String format, R result) {
		return null;
	}

	@Override
	public void traceExit(EntryMessage message) {
		return;
	}

	@Override
	public <R> R traceExit(EntryMessage message, R result) {
		return null;
	}

	@Override
	public <R> R traceExit(Message message, R result) {
		return null;
	}

	@Override
	public void warn(Marker marker, Message message) {
		return;
		
	}

	@Override
	public void warn(Marker marker, Message message, Throwable throwable) {
		return;
		
	}

	@Override
	public void warn(Marker marker, MessageSupplier messageSupplier) {
		return;
		
	}

	@Override
	public void warn(Marker marker, MessageSupplier messageSupplier, Throwable throwable) {
		return;
		
	}

	@Override
	public void warn(Marker marker, CharSequence message) {
		return;
		
	}

	@Override
	public void warn(Marker marker, CharSequence message, Throwable throwable) {
		return;
		
	}

	@Override
	public void warn(Marker marker, Object message) {
		return;
		
	}

	@Override
	public void warn(Marker marker, Object message, Throwable throwable) {
		return;
		
	}

	@Override
	public void warn(Marker marker, String message) {
		return;
		
	}

	@Override
	public void warn(Marker marker, String message, Object... params) {
		return;
		
	}

	@Override
	public void warn(Marker marker, String message, Supplier<?>... paramSuppliers) {
		return;
		
	}

	@Override
	public void warn(Marker marker, String message, Throwable throwable) {
		return;
		
	}

	@Override
	public void warn(Marker marker, Supplier<?> messageSupplier) {
		return;
		
	}

	@Override
	public void warn(Marker marker, Supplier<?> messageSupplier, Throwable throwable) {
		return;
		
	}

	@Override
	public void warn(Message message) {
		return;
		
	}

	@Override
	public void warn(Message message, Throwable throwable) {
		return;
		
	}

	@Override
	public void warn(MessageSupplier messageSupplier) {
		return;
		
	}

	@Override
	public void warn(MessageSupplier messageSupplier, Throwable throwable) {
		return;
		
	}

	@Override
	public void warn(CharSequence message) {
		return;
		
	}

	@Override
	public void warn(CharSequence message, Throwable throwable) {
		return;
		
	}

	@Override
	public void warn(Object message) {
		return;
		
	}

	@Override
	public void warn(Object message, Throwable throwable) {
		return;
		
	}

	@Override
	public void warn(String message) {
		apiLog(message);
	}

	@Override
	public void warn(String message, Object... params) {
		return;
		
	}

	@Override
	public void warn(String message, Supplier<?>... paramSuppliers) {
		return;
		
	}

	@Override
	public void warn(String message, Throwable throwable) {
		return;
		
	}

	@Override
	public void warn(Supplier<?> messageSupplier) {
		return;
		
	}

	@Override
	public void warn(Supplier<?> messageSupplier, Throwable throwable) {
		return;
		
	}

	@Override
	public void warn(Marker marker, String message, Object p0) {
		return;
		
	}

	@Override
	public void warn(Marker marker, String message, Object p0, Object p1) {
		return;
		
	}

	@Override
	public void warn(Marker marker, String message, Object p0, Object p1, Object p2) {
		return;
		
	}

	@Override
	public void warn(Marker marker, String message, Object p0, Object p1, Object p2, Object p3) {
		return;
		
	}

	@Override
	public void warn(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4) {
		return;
		
	}

	@Override
	public void warn(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {
		return;
		
	}

	@Override
	public void warn(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5,
			Object p6) {
		return;
		
	}

	@Override
	public void warn(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5,
			Object p6, Object p7) {
		return;
		
	}

	@Override
	public void warn(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5,
			Object p6, Object p7, Object p8) {
		return;
		
	}

	@Override
	public void warn(Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5,
			Object p6, Object p7, Object p8, Object p9) {
		return;
		
	}

	@Override
	public void warn(String message, Object p0) {
		return;
		
	}

	@Override
	public void warn(String message, Object p0, Object p1) {
		return;
		
	}

	@Override
	public void warn(String message, Object p0, Object p1, Object p2) {
		return;
		
	}

	@Override
	public void warn(String message, Object p0, Object p1, Object p2, Object p3) {
		return;
		
	}

	@Override
	public void warn(String message, Object p0, Object p1, Object p2, Object p3, Object p4) {
		return;
		
	}

	@Override
	public void warn(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {
		return;
		
	}

	@Override
	public void warn(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {
		return;
		
	}

	@Override
	public void warn(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
			Object p7) {
		return;
		
	}

	@Override
	public void warn(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
			Object p7, Object p8) {
		return;
		
	}

	@Override
	public void warn(String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6,
			Object p7, Object p8, Object p9) {
		return;
		
	}

	public String getProcessWorkspaceId() {
		return m_sProcessWorkspaceId;
	}

	public void setProcessWorkspaceId(String sProcessWorkspaceId) {
		this.m_sProcessWorkspaceId = sProcessWorkspaceId;
	}

}

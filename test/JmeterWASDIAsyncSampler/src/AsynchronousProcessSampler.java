import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.TimeoutException;

import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;



public class AsynchronousProcessSampler extends AbstractJavaSamplerClient implements Serializable {
	
	private static final long serialVersionUID = 1L;

	 @Override
	    public Arguments getDefaultParameters() {
		 	Arguments defaultParameters = new Arguments();
	        defaultParameters.addArgument("WorkspaceID", "");
	        defaultParameters.addArgument("API", "DOWNLOAD");
	        defaultParameters.addArgument("Rhost", "localhost");
	        defaultParameters.addArgument("Rport", "19999");
	        defaultParameters.addArgument("Ruser", "fadeout");
	        defaultParameters.addArgument("Rpwd", "fadeout");
	        return defaultParameters;
	 }
	 
	 @Override
		public SampleResult runTest(JavaSamplerContext context) {
		    String sWsID=context.getParameter( "WorkspaceID" );
	    	String sAPI=context.getParameter( "API" );
	    	
	    	String sRhost=context.getParameter( "Rhost" );
	    	String sRPort = context.getParameter("Rport");
	    	String sRuser = context.getParameter("Ruser");
	    	String sRpwd = context.getParameter("Rpwd");
	    	
	    	
	    	RabbitConsumer rc = null;
		 SampleResult sampleResult = new SampleResult();
		 
		 try {
			rc = new RabbitConsumer(sRhost,sRPort,sRuser,sRpwd,sAPI,sWsID);
		 }
		 catch (IOException  | TimeoutException e) {
			 this.getLogger().error("Problem connecting to Rabbit Server");
			 sampleResult.setSuccessful(false);
			 sampleResult.setResponseMessage("Problem connecting to RabbitMQ server");
			 e.printStackTrace();
			 return sampleResult;
		 }

		 sampleResult.sampleStart();
		 boolean result = rc.runTest();
		 sampleResult.sampleEnd();
		 
		 if (result) {
			 sampleResult.setResponseCodeOK();
		 	 sampleResult.setResponseMessage(sAPI +"Async Process Terminated");
		 }
		 else {
			 sampleResult.setResponseCode("KO");
		 }
		 sampleResult.setSuccessful(result);
		 return sampleResult;

	 }	 
}

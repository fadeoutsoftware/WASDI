package wasdi.jwasdilib;

import java.io.File;

/**
 * Hello world!<6
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        System.out.println( "JWasdiLib Test Start" );
        WasdiLib oLib = new WasdiLib();
                
        String sWorkingDirectory = System.getProperty("user.dir");
        oLib.init(sWorkingDirectory + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "myConfig.properties");

        oLib.printStatus();
        
        System.out.println(oLib.getProcessorPath());
        
        System.out.println("JWasdiLib Test Done");
        oLib.updateStatus("DONE");
        
    } 
    
}

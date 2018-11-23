import java.io.IOException;

public class UglyTests {


		
	public static void main(String[] args) throws IOException {
		
		String sShellExString = new String("java -jar C:\\Users\\c.nattero\\.m2\\repository\\fadeout\\software\\launcher\\launcher-1.0-SNAPSHOT.jar -operation UPLOADVIAFTP -parameter c:\\temp\\wasdi\\03b754a8-3bde-4462-a25a-1892b29ff921");
		Process oSystemProc = Runtime.getRuntime().exec(sShellExString);
		
		/*
		String sWinPath = new String("c:\\a\\kind\\of\\path");
		System.out.println(sWinPath);
		String sUnixPath = new String(sWinPath.replace('\\', '/'));
		System.out.println(sWinPath);
		System.out.println(sUnixPath);
		*/
	}

}

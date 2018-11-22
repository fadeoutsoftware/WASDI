public class UglyTests {


		
	public static void main(String[] args) {
		
		String sWinPath = new String("c:\\a\\kind\\of\\path");
		System.out.println(sWinPath);
		String sUnixPath = new String(sWinPath.replace('\\', '/'));
		System.out.println(sWinPath);
		System.out.println(sUnixPath);
	}

}

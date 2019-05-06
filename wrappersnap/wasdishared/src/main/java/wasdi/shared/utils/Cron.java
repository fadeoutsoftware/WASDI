package wasdi.shared.utils;

public class Cron {
	public void addTask(String sUser, String sMinute, String sHour, String sDayOfMonth, String sMonth, String sDayOfWeek, String sCommand) {
		
		String sPlan = sMinute + " " + sHour + " " + sDayOfMonth + " " + sMonth + " " + sDayOfWeek; 
		String sShellExec = "crontab -u "+sUser + " -l ; echo \"" + sPlan + " " + sCommand + "\" | crontab -u "+ sUser + " -";
		
		/*
		 * 
		 * (crontab -u sUser -l ; echo "sPlan sCommand") | crontab -u sUser -
		 */
	}
	
	public void removeTask(String sUser, String sCommand) {
		//crontab -u sUser -l | grep -v sCommand  | crontab -u sUser -
	}
}

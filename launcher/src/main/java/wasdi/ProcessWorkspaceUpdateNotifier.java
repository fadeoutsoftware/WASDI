/**
 * Created by Cristiano Nattero on 2019-01-11
 * 
 * Fadeout software
 *
 */
package wasdi;

/**
 * @author c.nattero
 *
 */
public interface ProcessWorkspaceUpdateNotifier {
	public void subscribe(ProcessWorkspaceUpdateSubscriber oSubscriber);
	public void unsubscribe(ProcessWorkspaceUpdateSubscriber oSubscriber);
}

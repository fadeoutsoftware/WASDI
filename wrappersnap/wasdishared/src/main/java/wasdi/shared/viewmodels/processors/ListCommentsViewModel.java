package wasdi.shared.viewmodels.processors;

import java.util.ArrayList;
import java.util.List;

/**
 * List Comments View Model
 * 
 * Wraps a list of comments view models
 * 
 * @author p.campanella
 *
 */
public class ListCommentsViewModel {

	private List<CommentViewModel> comments = new ArrayList<CommentViewModel>();

	public List<CommentViewModel> getComments() {
		return comments;
	}

	public void setComments(List<CommentViewModel> comments) {
		this.comments = comments;
	}

}

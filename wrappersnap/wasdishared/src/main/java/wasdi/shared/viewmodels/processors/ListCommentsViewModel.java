package wasdi.shared.viewmodels.processors;

import java.util.ArrayList;
import java.util.List;

public class ListCommentsViewModel {

	private List<CommentViewModel> comments = new ArrayList<CommentViewModel>();

	public List<CommentViewModel> getComments() {
		return comments;
	}

	public void setComments(List<CommentViewModel> comments) {
		this.comments = comments;
	}

}

package wasdi.shared.viewmodels.processors;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The Comment DetailViewModel
 * 
 * Represents a comment to a review
 * 
 * @author PetruPetrescu
 *
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommentDetailViewModel {

	private String commentId;
	private String reviewId;
	private String userId;
	private Date date;
	private String text;

}

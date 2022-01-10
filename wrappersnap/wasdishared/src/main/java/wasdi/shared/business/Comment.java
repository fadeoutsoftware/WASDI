package wasdi.shared.business;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Comment that users can make to an existing review.
 * Reviews and comment are available in the space market
 * 
 * @author PetruPetrescu
 *
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Comment {
	
	/** Comment Id: guid */
	private String commentId;
	
	/** Id of the parent review */
	private String reviewId;
	
	/** User that made the review */
	private String userId;
	
	/** Date of the review (timestamp) */
	private Double date;
	
	/** Text of the comment */
	private String text;

}

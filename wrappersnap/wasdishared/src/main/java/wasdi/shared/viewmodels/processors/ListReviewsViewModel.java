package wasdi.shared.viewmodels.processors;

import java.util.ArrayList;
import java.util.List;

public class ListReviewsViewModel {
	private List<ReviewViewModel> reviews = new ArrayList<ReviewViewModel>();
	float avgVote=-1.0f;
	int numberOfOneStarVotes = 0;
	int numberOfTwoStarVotes = 0;
	int numberOfThreeStarVotes = 0;
	int numberOfFourStarVotes = 0;
	int numberOfFiveStarVotes = 0;
	boolean alreadyVoted = false;
	
	public List<ReviewViewModel> getReviews() {
		return reviews;
	}
	public void setReviews(List<ReviewViewModel> reviews) {
		this.reviews = reviews;
	}
	public float getAvgVote() {
		return avgVote;
	}
	public void setAvgVote(float avgVote) {
		this.avgVote = avgVote;
	}
	public int getNumberOfOneStarVotes() {
		return numberOfOneStarVotes;
	}
	public void setNumberOfOneStarVotes(int numberOfOneStarVotes) {
		this.numberOfOneStarVotes = numberOfOneStarVotes;
	}
	public int getNumberOfTwoStarVotes() {
		return numberOfTwoStarVotes;
	}
	public void setNumberOfTwoStarVotes(int numberOfTwoStarVotes) {
		this.numberOfTwoStarVotes = numberOfTwoStarVotes;
	}
	public int getNumberOfThreeStarVotes() {
		return numberOfThreeStarVotes;
	}
	public void setNumberOfThreeStarVotes(int numberOfThreeStarVotes) {
		this.numberOfThreeStarVotes = numberOfThreeStarVotes;
	}
	public int getNumberOfFourStarVotes() {
		return numberOfFourStarVotes;
	}
	public void setNumberOfFourStarVotes(int numberOfFourStarVotes) {
		this.numberOfFourStarVotes = numberOfFourStarVotes;
	}
	public int getNumberOfFiveStarVotes() {
		return numberOfFiveStarVotes;
	}
	public void setNumberOfFiveStarVotes(int numberOfFiveStarVotes) {
		this.numberOfFiveStarVotes = numberOfFiveStarVotes;
	}
	public boolean getAlreadyVoted() {
		return alreadyVoted;
	}
	public void setAlreadyVoted(boolean alreadyVoted) {
		this.alreadyVoted = alreadyVoted;
	}
	
}

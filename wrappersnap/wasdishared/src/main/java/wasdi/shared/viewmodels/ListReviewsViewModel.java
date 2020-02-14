package wasdi.shared.viewmodels;

import java.util.List;

public class ListReviewsViewModel {
	private List<ReviewViewModel> reviews;
	long avgVote;
	int numberOfOneStarVotes;
	int numberOfTwoStarVotes;
	int numberOfThreeStarVotes;
	int numberOfFourStarVotes;
	int numberOfFiveStarVotes;
	public List<ReviewViewModel> getReviews() {
		return reviews;
	}
	public void setReviews(List<ReviewViewModel> reviews) {
		this.reviews = reviews;
	}
	public long getAvgVote() {
		return avgVote;
	}
	public void setAvgVote(long avgVote) {
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
	
}

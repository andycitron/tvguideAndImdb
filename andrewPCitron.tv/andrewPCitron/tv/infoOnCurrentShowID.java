package andrewPCitron.tv;

public class infoOnCurrentShowID {
	boolean isAMovie;
	String typeOfMovie;
	Integer channel;
	String movieName;
	Integer  yearMade;

	public infoOnCurrentShowID() {
		super();
		reset();
	}
	public void reset() {
		isAMovie=false;
		typeOfMovie=null;
		channel=0;
		movieName=null;
		yearMade=0;

	}
	public boolean isAMovie() {
		return isAMovie;
	}
	public void setAMovie(boolean isAMovie) {
		this.isAMovie = isAMovie;
	}
	public String getTypeOfMovie() {
		return typeOfMovie;
	}
	public void setTypeOfMovie(String typeOfMovie) {
		this.typeOfMovie = typeOfMovie;
	}
	public Integer getChannel() {
		return channel;
	}
	public void setChannel(Integer channel) {
		this.channel = channel;
	}
	public String getMovieName() {
		return movieName;
	}
	public void setMovieName(String movieName) {
		this.movieName = movieName;
	}
	public Integer getYearMade() {
		return yearMade;
	}
	public void setYearMade(Integer yearMade) {
		this.yearMade = yearMade;
	}

}

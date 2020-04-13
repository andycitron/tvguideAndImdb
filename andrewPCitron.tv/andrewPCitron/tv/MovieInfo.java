package andrewPCitron.tv;

public class MovieInfo {
	private String channel;
	private String oneMoviesJson;

	private String title;

	private String titleForRegexSearch;
	// stuff from here down gets figured out later
	private String criticsRating;
	private String nonCriticsRating;
	private String description;
	private String yearMade;
	private String typeOfMovie;

	public MovieInfo(String channel, String title, String yearMade, String typeOfMovie) {
		super();
		this.channel = channel.trim();
		
		String processedTitle = title.trim();
		this.setTitleForRegexSearch(processedTitle);  	

		processedTitle = processedTitle.replaceAll("&#039;", "'");

		this.title = processedTitle;
		this.yearMade=yearMade;
		this.typeOfMovie=typeOfMovie;
		// this stuff gets figured out later
		this.criticsRating ="";
		this.nonCriticsRating="";
		this.description="";

	}

	public String getChannel() {
		return channel;
	}
	public void setChannel(String channel) {
		this.channel = channel.trim();
	}
	public String getMovieJson() {
		return oneMoviesJson;
	}
	public void setMovieJson(String movieHref) {
		movieHref = movieHref.trim();
		// jan 2016 / is escaped in the url
		this.oneMoviesJson = movieHref.replaceAll("\\\\", ""); // replacing all back slashes
		
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		title = title.trim();

		this.title = title.replaceAll("\u00c1", "a"); // spanish with accent messes up IMDB search
	}
	public String getCriticsRating() {
		return criticsRating;
	}
	public void setCriticsRating(String criticsRating) {
		this.criticsRating = criticsRating.trim();
	}
	public String getNonCriticsRating() {
		return nonCriticsRating;
	}
	public void setNonCriticsRating(String nonCriticsRating) {
		this.nonCriticsRating = nonCriticsRating.trim();
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		String processedDescription = description.trim();
		if (processedDescription.contains(",")) {    // if it has a comma enclose the whole field in quotes
			// I'm going to add quotes around the string, but embedded quotes are messing up excel.
			// so first replace existing quotes
			processedDescription = processedDescription.replaceAll("\"", "'");

// maybe this isn't needed as I'll put quotes around it upon output			processedDescription = "\"" + processedDescription + "\"";	
		}
		processedDescription = processedDescription.replaceAll("&#039;", "'");

		// they like to put external links (hrefs) in the descriptions, remove those



		this.description = processedDescription.trim();

	}
	public String getYearMade() {
		return yearMade;
	}
	public void setYearMade(String yearMade) {
		this.yearMade = yearMade.trim();
	}

	public String toString() {

		String printableVersionInRightOrder = ""; // assume we're going to
		// filter out low rated
		// movie
		int iYearMade = -1;
		boolean displayMovie = false;

		try {
			iYearMade = Integer.parseInt(getYearMade());
			//int critsRating = 0;// no critics vs. non-critics on IMDB Integer.parseInt(getCriticsRating());  I used to have 2 different ratings, but now there's only 1
			// value from IMDB is like 7.2
			Double nonCritsRating = Double.parseDouble(getNonCriticsRating());

			if (/* (critsRating > 60) || */ (nonCritsRating > 70)) {
				displayMovie = true;
			} 
		} catch (Exception e) {                           // probably integer parsing exception
			// System.out.println("MovieInfo.toString threw exception " + e.toString());
			if (iYearMade > 2016) {                       // display newer movie just to be safe
				displayMovie = true;
			}        
		}

		if (displayMovie) {
			String titleWithEnclosedComma = getTitle();
			if (titleWithEnclosedComma.contains(",")) {   // if it has a comma enclose the whole field in quotes
	            
				titleWithEnclosedComma = "\"" + titleWithEnclosedComma + "\"";	
			}
			
			String typeWithEnclosedComma = getTypeOfMovie().trim();
			if (typeWithEnclosedComma.contains(",")) {    // if it has a comma enclose the whole field in quotes
	            
				typeWithEnclosedComma = "\"" + typeWithEnclosedComma + "\"";	
			}

			String descriptionWithEnclosedComma = getDescription();
			if (descriptionWithEnclosedComma.contains(",")) {    // if it has a comma enclose the whole field in quotes

				descriptionWithEnclosedComma = "\"" + descriptionWithEnclosedComma + "\"";	
			}
			

			printableVersionInRightOrder = "\n" + getChannel() + ","
					+ titleWithEnclosedComma + "," + getYearMade() + ","
					+ getNonCriticsRating() + ","
					+ descriptionWithEnclosedComma + "," 
					+ typeWithEnclosedComma;
		}

		return printableVersionInRightOrder;
	}
		
	public String getTypeOfMovie() {
		return typeOfMovie;
	}

	public void setTypeOfMovie(String typeOfMovie) {
		this.typeOfMovie = typeOfMovie;
	}
	public String getOneMoviesJson() {
		return oneMoviesJson;
	}

	public void setOneMoviesJson(String oneMoviesJson) {
		this.oneMoviesJson = oneMoviesJson;
	}

	public String getTitleForRegexSearch() {
		return titleForRegexSearch;
	}

	public void setTitleForRegexSearch(String inputTitle) {
		// replace all special characters:  \.[]{}()*+-?^$|
		// this threw exception inputTitle = inputTitle.replaceAll("\\", "\\\\");
		//if (inputTitle.contains("?")) {
		//	System.out.println("InputTitle= " + inputTitle);
		//}
		this.titleForRegexSearch = inputTitle;
				
		try {
			this.titleForRegexSearch = this.titleForRegexSearch.replaceAll("\\.", "\\\\.");
			this.titleForRegexSearch = this.titleForRegexSearch.replaceAll("\\[", "\\\\[");
			this.titleForRegexSearch = this.titleForRegexSearch.replaceAll("\\]", "\\\\]");
			this.titleForRegexSearch = this.titleForRegexSearch.replaceAll("\\{", "\\\\{");
			this.titleForRegexSearch = this.titleForRegexSearch.replaceAll("\\}", "\\\\}");
			this.titleForRegexSearch = this.titleForRegexSearch.replaceAll("\\(", "\\\\(");
			this.titleForRegexSearch = this.titleForRegexSearch.replaceAll("\\)", "\\\\)");
			this.titleForRegexSearch = this.titleForRegexSearch.replaceAll("\\*", "\\\\*");
			this.titleForRegexSearch = this.titleForRegexSearch.replaceAll("\\+", "\\\\+");
			this.titleForRegexSearch = this.titleForRegexSearch.replaceAll("\\-", "\\\\-");
			this.titleForRegexSearch = this.titleForRegexSearch.replaceAll("\\?", "\\\\?");
			this.titleForRegexSearch = this.titleForRegexSearch.replaceAll("\\^", "\\\\^");
		    this.titleForRegexSearch = this.titleForRegexSearch.replaceAll("\\$", "\\\\$");  // I've seen this throw an exception when$ was a end.  No time to figure out why that is rignt now
		} catch (Exception e) {
			System.out.println("setTitleForRegexSearch threw exception on " +this.titleForRegexSearch);
			e.printStackTrace();
		}
		
		if (this.titleForRegexSearch.compareTo(inputTitle)!= 0) {  // we did change some characters...output info for debug
			// for debug System.out.println("regex changed chars.  inputTitle= " + inputTitle + "  modified title =" + this.titleForRegexSearch);
			
		}
	}



}

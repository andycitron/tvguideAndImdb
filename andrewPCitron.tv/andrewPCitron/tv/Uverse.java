package andrewPCitron.tv;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

public class Uverse {
	private static Connection mainConn;      // holds vod2_2m main db connection
	private Connection dailyConn;            // holds info about one day that's being processed
	private HashMap<String, MovieInfo>  movieInfoHolder;         // holds hashmap of all movies
    private List<String> categoriesThatMightBeMovies=new ArrayList<String>();
	private infoOnCurrentShowID infoOnCurrentShow;
    private WebSavePageParser webSavePageParser;
    private UrlAccess utilityClass;

	// needs to know if we get these channels
	private int showtime;                // user has showtime?
	private int epix;                    // user has epix movie channel
	private int starz;                   // user has starz movie channel
	private int sundance;                // user has sundance movie channel
	private int ifc;                     // user has ifc movie channel: no checks for this in code TBD
	private int tcm;                     // user has epix movie channel
	private int tmc;                     // user has the movie channel
	private int hbo;                     // user has hbo
	private int cinemax;                 // user has cinemax	
	private String zipCode;              // Uverse uses this in some urls 
	
	public Uverse(HashMap<String, MovieInfo> movieInfo, String zipCode) {
	   getvod2_2_m_s3db();  // fetch initial database
	   infoOnCurrentShow = new infoOnCurrentShowID();   // initial class that gets reused on each movie
	   movieInfoHolder=movieInfo;
	   utilityClass = new UrlAccess();
	   webSavePageParser = new WebSavePageParser();
	   
	   // build list of all show types that might be movies
	   categoriesThatMightBeMovies.add("Movies");
	   categoriesThatMightBeMovies.add("All Movies");
	   categoriesThatMightBeMovies.add("Comedy");
	   categoriesThatMightBeMovies.add("Action");
	   categoriesThatMightBeMovies.add("Sci-Fi & Horror");
	   categoriesThatMightBeMovies.add("Drama");
	   categoriesThatMightBeMovies.add("Westerns");
	   categoriesThatMightBeMovies.add("Documentaries");
	   categoriesThatMightBeMovies.add("Horror/Sci-Fi");
	   categoriesThatMightBeMovies.add("Independent");
	   categoriesThatMightBeMovies.add("Thriller/Suspense");
	   categoriesThatMightBeMovies.add("All Comedy");
	   categoriesThatMightBeMovies.add("Reality/Docs");
	   categoriesThatMightBeMovies.add("All Reality/Docs");
	   categoriesThatMightBeMovies.add("After Hours");
	   categoriesThatMightBeMovies.add("Big Originals");
	   categoriesThatMightBeMovies.add("Big Movies");
	   categoriesThatMightBeMovies.add("Documentary");
	   categoriesThatMightBeMovies.add("Horror & SciFi");

	}

	/**
	 * 
	 */
	private static void getvod2_2_m_s3db() {
		// fetch initial database
		try {
			mainConn = DriverManager 
				.getConnection("jdbc:sqlite::resource:http://vsapps.asp.att.net/mra2files/vodcatalog/vod2_2_m.s3db");
			
			// now print all the data if debugging
			String sql = "SELECT SortOrder, CategoryID, ParentCategoryID, CategoryName, NetworkID  FROM Category";
		//	[Category] 
		//			   ( [SortOrder] INTEGER(8), 
		//			   [CategoryID] INTEGER(8) NOT NULL, 
		//			   [ParentCategoryID] INTEGER(8), 
		//			   [CategoryName] VARCHAR(25), 
		//			   [NetworkID] INTEGER(10),
	        
	        try {
	             Statement stmt  = mainConn.createStatement();
//	             ResultSet rs    = stmt.executeQuery(sql);
//	            
//	            // loop through the result set
//	            while (rs.next()) {
//	                System.out.println(rs.getInt("SortOrder") +  " " + 
//	                		           rs.getInt("CategoryID") +  " " +
//	                		           rs.getInt("ParentCategoryID") +  " " +
//	                                   rs.getString("CategoryName") + " " +
//	                                   rs.getInt("NetworkID"));
//	            }
//	            
	            // check version info
 				String sql2 = "SELECT VODCatLastUpdated  FROM Catalog";

 				ResultSet rs    = stmt.executeQuery(sql2);
		            
	            // sanity check that db version hasn't changed
	            while (rs.next()) {
	            	if (rs.getInt("VODCatLastUpdated")!= 1513024806 )
	                System.out.println("****Warning VODCatLastUpdated version has changed to " +
	                		rs.getInt("VODCatLastUpdated"));
	            }
	            
	        } catch (SQLException e) {
	            System.out.println(e.getMessage());
	        }
		} catch (SQLException e) {
		
			System.out.println("getvod2_2m connect threw an exception " + e.getMessage());
			e.printStackTrace();
		} 

		
	}
	
	public void rememberPackagesWeGet(	
			int showtime,                
			int epix,                
			int starz,                   
			int sundance,                
			int ifc,                     
			int tcm,                     
			int tmc,                 
			int hbo, 	
			int cinemax  ) {
		this.showtime=showtime;
		this.epix=epix;
		this.starz=starz;
		this.sundance=sundance;
		this.ifc=ifc;
		this.tcm=tcm;
		this.tmc=tmc;
		this.hbo=hbo;
		this.cinemax=cinemax;

	}

	public void readOneDay(String year, String month, String day) {
		System.out.println("Year, Month, Day " + year + " " + month + " " + day);

		String allShowsTodaySql = "Select TunerPosition, ShowID, StartTime, EndTime, ChannelID from Broadcast";

		String urlToFetch = "http://vsapps.asp.att.net/mra2files/epgdigest2/epg2_92899_" + year + month + day + "_m.s3db";
		System.out.println("daily db urlToFetch: "+ urlToFetch);

		// For now, use this to read one day and figure out how data found maps to show
		try {
			dailyConn = DriverManager 
					.getConnection("jdbc:sqlite::resource:" + urlToFetch);  
			Statement stmt  = dailyConn.createStatement();
			ResultSet allShowsTodayRs    = stmt.executeQuery(allShowsTodaySql);

			PreparedStatement lookForMatchOnShowID = dailyConn.prepareStatement("Select ShowID, Title, SubTitle, ShowFlags2, SeriesID, ShowFlags from Show WHERE ShowID=?");

			// loop through the result set
			while (allShowsTodayRs.next()) 
			{
				int tunerPosition = allShowsTodayRs.getInt("TunerPosition");
				if (weGetThisChannel(tunerPosition)) {
					infoOnCurrentShow.reset();                    //   reuse this to gather info on one show

					int broadCastShowID =  allShowsTodayRs.getInt("ShowID");
					int broadCastChannelID = allShowsTodayRs.getInt("ChannelID");
					int startTime = allShowsTodayRs.getInt("StartTime");
					int endTime = allShowsTodayRs.getInt("EndTime");
					int runningTime = (endTime - startTime)/60;  // number of minutes in show 

					if (runningTime > 75) {                      // rule out as movie if it's less that an hour and 15 minutes
						//System.out.println("Another Show: " + tunerPosition +  " " + 
						//		broadCastShowID +  " " +
						//		broadCastChannelID);

						// for now, go through every show and try to find match 
						lookForMatchOnShowID.setInt(1, broadCastShowID);
						ResultSet matchRs = lookForMatchOnShowID.executeQuery();
						while (matchRs.next()) {
							// System.out.println("ShowID = " + matchRs.getInt("ShowID") + " Title = " + matchRs.getString("Title"));
							
						   

							// now go to global database and look for match on Title returns true if its a movie
							boolean isAMovie = SearchGlobalShowDatabaseForMatchOnTitle(matchRs.getString("Title"));

							if (!isAMovie && (( infoOnCurrentShow.getTypeOfMovie()!=null && infoOnCurrentShow.getTypeOfMovie() == "Not found in Category DB"))) {                           // not all movies are in category table.  try to use flags2 as indicator
								int showFlags = matchRs.getInt("ShowFlags");
								// int forDebug = showFlags>>2;
								if ( getBit(showFlags,2)) {          //check for bit on in 4 position
									//System.out.println("ShowFlags for " + matchRs.getString("Title") + " is " + showFlags); 
									isAMovie = true;                   // just a guess
								}
							}
							// if a movie, get year made and write to hashmap
							if (isAMovie) {
								// need to get release year from SearchGlobalShowetcetc
								System.out.println("Movie added to hashmap tuner position= "+ tunerPosition + " "
										+ matchRs.getString("Title") );
								
								infoOnCurrentShow.setMovieName(matchRs.getString("Title"));
								infoOnCurrentShow.setChannel(tunerPosition);
								// at this point infoOnCurrentShow should have all the info we can save
								MovieInfo currentShow = new MovieInfo(infoOnCurrentShow.getChannel().toString(),
										infoOnCurrentShow.getMovieName(),
										infoOnCurrentShow.getYearMade().toString(),
										infoOnCurrentShow.getTypeOfMovie());
								// insert into hashmap
								movieInfoHolder.put(infoOnCurrentShow.getMovieName(), currentShow); 
								//if (infoOnCurrentShow.getMovieName().contains("Holmes") || infoOnCurrentShow.getMovieName().contains("Justice") || infoOnCurrentShow.getMovieName().contains("of Stalin")) {
				                //    // convenient point to add breakpoint
								//	System.out.println(infoOnCurrentShow.getMovieName());
								//}
									

							} else {  // for debug
								System.out.println(matchRs.getString("Title")+ " is not a movie." +
										           " Subtitle is " + matchRs.getString("SubTitle") +
										           " Showflags2 is " + matchRs.getInt("ShowFlags2") +  
										           " Series ID is " + matchRs.getInt("SeriesID") +
										           " ShowFlags is " + matchRs.getInt("ShowFlags"));
							   
								
							}
						}

						matchRs.close();
					}
				}

			}
			dailyConn.close();

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	boolean getBit(int n, int k) {
		
	    return (((n >> k) & 1) == 1 ? true:false);
	}

	private boolean SearchGlobalShowDatabaseForMatchOnTitle(String ShowTitle) {
		boolean isAMovie=false;
		String sql = "Select VODShowID, ShowTitle, releaseYear from Show WHERE ShowTitle=?";

		try {
			//Statement stmt  = mainConn.createStatement();
			PreparedStatement lookForMatchOnShowTitle = mainConn.prepareStatement(sql);

			///  added this
			lookForMatchOnShowTitle.setString(1, ShowTitle);
			ResultSet rs = lookForMatchOnShowTitle.executeQuery();

            boolean empty=true;
			// loop through the result set
			while (rs.next()) {
				//System.out.println("VODShowID = "+ rs.getInt("VODShowID") +  " " +						
				//		rs.getString("ShowTitle") +  " " + rs.getInt("releaseYear") );
				
				// here get show type from ShowCategory using VODShowID
			    isAMovie = getShowCategoryBasedOnVODShowID(rs.getString("VODShowID"));
			    if (isAMovie) {
			    	// save release year
			    	infoOnCurrentShow.setYearMade(rs.getInt("releaseYear"));
			    }
			    empty=false;
			}
			if( empty ) {
			    // System.out.println("No match for " +ShowTitle + " in Show table"  ); 
			    infoOnCurrentShow.setTypeOfMovie("Not found in Category DB");
		    }
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
        return (isAMovie);

	}

	private boolean getShowCategoryBasedOnVODShowID(String VODShowID) {
		boolean isAMovie=false;
		String query = "SELECT CategoryID, VODShowID, NetworkID from ShowCategory where VODShowID=?";

		try {
			// set all the prepared statement parameters
			PreparedStatement st = mainConn.prepareStatement(query);
			st.setString(1, VODShowID);

			// execute the prepared statement insert
			st.executeQuery();
			// stuff I copied
			ResultSet rs = st.executeQuery();

			// loop through the result set
			while (rs.next()) {
				//System.out.println("VODShowID, NetworkID, CategoryID = " +rs.getString("VODShowID") +  " " + 
				//		rs.getInt("NetworkID") +  " " +
				//		rs.getInt("CategoryID")  );

				// at this point we have ShowTitle Channel its on, ShowCategory and network
				// next check show category for this network to see if it is a movie or check subcategory as well
				if(isItAMovie2(rs.getInt("NetworkID"), rs.getInt("CategoryID")) ) {
					isAMovie=true;
				}
			}
			//

			st.close();
		} 
		catch (SQLException e)
		{
			// log exception
			System.out.println(e.getMessage());
		}
		return (isAMovie);

	}

//	private boolean isItAMovie(int NetworkID, int CategoryID) {
//		boolean isMovie = false;
//		String query ="Select CategoryID, CategoryName,ParentCategoryID,  NetworkID from Category Where NetworkID=?";
//		String movieCategoryForThisChannel ="Select CategoryID, CategoryName from Category Where CategoryName LIKE \"%Movies%\" AND NetworkID=?";
//		int movieCategory=-1;
//		try {
//			// get the movie category for this channel
//			PreparedStatement movieSt = mainConn.prepareStatement(movieCategoryForThisChannel);
//			movieSt.setInt(1, NetworkID);
//
//			// execute the prepared statement insert
//			movieSt.executeQuery();
//
//			ResultSet movieRs = movieSt.executeQuery();
//
//			// loop through the result set should only be 1
//            boolean empty = true;
//			while (movieRs.next()) {
//					System.out.println("CategoryID = " + movieRs.getInt("CategoryID") );
//					movieCategory = movieRs.getInt("CategoryID");
//					empty = false;
//			}
//			if( empty ) {
//				    System.out.println("CategoryID not found for "  + CategoryID
//				    		        + " For NetworkID " +NetworkID  );
//			}
//
//			movieSt.close();
//
//			// end of get movie category for this channel
//			// set all the prepared statement parameters
//			PreparedStatement st = mainConn.prepareStatement(query);
//			st.setInt(1, NetworkID);
//
//			// execute the prepared statement 
//			ResultSet rs = st.executeQuery();
//
//			// loop through the result set
//			while (rs.next() &&!isMovie) {
//				//System.out.println("CategoryID, CategoryName, ParentCategoryID, NetworkID =" + rs.getInt("CategoryID") +  " " + 
//				//		rs.getString("CategoryName") +  " " +
//				//		rs.getInt("ParentCategoryID") + " " +
//				//		rs.getInt("NetworkID"));
//
//				// at this point we have ShowTitle Channel its on, ShowCategory and network
//				// next check show category for this network to see if it is a movie or check subcategory as well
//				if( (rs.getInt("CategoryID") == movieCategory)|| (rs.getInt("ParentCategoryID")== movieCategory)) {
//					isMovie=true;
//					System.out.println(movieCategory + " is a movie");
//					infoOnCurrentShow.setAMovie(true);
//					infoOnCurrentShow.setTypeOfMovie(rs.getString("CategoryName"));
//				}
//			}
//			//
//
//			st.close();
//		} 
//		catch (SQLException e)
//		{
//			// log exception
//			System.out.println(e.getMessage());
//		}			
//
//		return isMovie;
//	}
	
	// original approach (above) didn't work, try different approach
	private boolean isItAMovie2(int NetworkID, int CategoryID) {
		boolean isMovie = false;
		String query ="Select CategoryID, CategoryName,ParentCategoryID,  NetworkID from Category Where NetworkID=? AND CategoryID=?";

		try {
			// get the movie category for this channel

			// end of get movie category for this channel
			// set all the prepared statement parameters
			PreparedStatement st = mainConn.prepareStatement(query);
			st.setInt(1, NetworkID);
			st.setInt(2, CategoryID);

			// execute the prepared statement 
			ResultSet rs = st.executeQuery();
			boolean empty = true;
			// loop through the result set
			while (rs.next() &&!isMovie) {
				//System.out.println("CategoryID, CategoryName, ParentCategoryID, NetworkID =" + rs.getInt("CategoryID") +  " " + 
				//		rs.getString("CategoryName") +  " " +
				//		rs.getInt("ParentCategoryID") + " " +
				//		rs.getInt("NetworkID"));
                empty=false;
				// at this point we have ShowTitle Channel its on, ShowCategory and network
				// next check show category for this network to see if it is a movie 
                String CategoryName = rs.getString("CategoryName");                
                
				if( mightBeMovie(CategoryName)) {
					isMovie=true;
					System.out.println(CategoryName + " is a movie");
					infoOnCurrentShow.setAMovie(true);
					infoOnCurrentShow.setTypeOfMovie(rs.getString("CategoryName"));
				}
			}
			if( empty ) {
			    System.out.println("CategoryID not found for "  + CategoryID
			    		        + " For NetworkID " +NetworkID  );
	    	}
			//

			st.close();
		} 
		catch (SQLException e)
		{
			// log exception
			System.out.println(e.getMessage());
		}			

		return isMovie;
	}

	private boolean mightBeMovie(String search) {
	
		for(String s : categoriesThatMightBeMovies)
		    if(s.contains(search)) return true;
		return false;
		
	}

	private boolean weGetThisChannel(int iChannel) {
		boolean thisChannelInList = false;

		if ( ((iChannel < 800 ) && ((iChannel != 102) && (iChannel != 346) && (iChannel != 360) && (iChannel != 365))&& (iChannel != 366) ) ||  // base channels other than lifetime or hallmark
				(iChannel== 960 ) ||
				((iChannel >= 852) && (iChannel <= 866) && (showtime !=0) )  ||
				((iChannel >= 802) && (iChannel <= 815) && (hbo !=0) )  ||
				((iChannel >= 832) && (iChannel <= 846) && (cinemax !=0) )  ||
				((iChannel >= 882) && (iChannel <= 885) && (tmc != 0)) ||
				((iChannel >= 902) && (iChannel <= 944) && (starz !=0) )  ||	      
				((iChannel >= 891) && (iChannel < 896) && (epix != 0)) || 
				((iChannel == 1798) && (sundance !=0) )  ||
				((iChannel == 790) && (tcm !=0) )  ||
				(((iChannel >= 1004)&& (iChannel < 1800) ) && (iChannel != 1360) && (iChannel != 1365)&& (iChannel != 1366) && (iChannel != 1107) && (iChannel != 1793))  // hd channels other than lifetime
				) {
			
			thisChannelInList = true;	              // this is a channel we get
		}	
		
		return thisChannelInList;
	}
	public HashMap<String, MovieInfo> getMovieInfoHolder() {
		return movieInfoHolder;
	}

	public void setMovieInfoHolder(HashMap<String, MovieInfo> movieInfoHolder) {
		this.movieInfoHolder = movieInfoHolder;
	}
	
	public String figureOutWhichYearIsPlayingOnUverse(MovieInfo listingInfo,
			String[] arrayOfIMDBentries, String movieInfo) {
		String outputUrl = arrayOfIMDBentries[0];  // if worse comes to worse, use 1st

		// check to see if new need to go to uverse details to get the year
		if (listingInfo.getYearMade().length() == 0) {  // this won't work for uverse ap version
			// first fetch detail entry from uverse.  It contains the year of the movie that's on the schedule
			// get programId, seriesId and internalID from movieInfo json data
			String programId[] = webSavePageParser.parsePage(listingInfo.getMovieJson() ,"programId\":", ",", null);		
			String seriesId[] = webSavePageParser.parsePage(listingInfo.getMovieJson() ,"seriesId\":", ",", null);		
			String internalId[] = webSavePageParser.parsePage(listingInfo.getMovieJson() ,"internalId\":", ",", null);		

			if ((programId != null) && (seriesId != null) && (internalId != null)) {

				String uverseDetailUrl = "http://uverse.com/epg-item-details.html?programId=" + programId[0] + "&seriesId=" +seriesId[0]+"&internalId=" + internalId[0]+ "&zipcode=" + zipCode;
				// get details from uverse
				String uverseDetailsPage = utilityClass.readUrlAsAjax(uverseDetailUrl, 2);
				if (uverseDetailsPage!=null) {
					// pull as much info as possible off the details page
					//first get year made
					String yearOnSchedule[] = webSavePageParser.parsePage(uverseDetailsPage,"Released in ", " </h2>", null);
					if ((yearOnSchedule != null) && (yearOnSchedule.length > 0)) {
						// save the year
						listingInfo.setYearMade(yearOnSchedule[0].trim());			  
					}
					// as long as we fetched it, let's get description
					String descriptionSchedule[] = webSavePageParser.parsePage(uverseDetailsPage,"<p class=\"episode-description\">", "</p>", null);
					if ((descriptionSchedule != null) && (descriptionSchedule.length > 0)) {
						// save the year
						listingInfo.setDescription(descriptionSchedule[0].trim());			  
					}
				}
			}

		}	
		// now go through all of the versions on IMDB to find the one that is for the year that's actually showing

		boolean found=false;
		for (int i=0;( i < arrayOfIMDBentries.length && !found); i++) {

			int startPos = movieInfo.indexOf(arrayOfIMDBentries[i] + "\" >" + listingInfo.getTitle());
			if (startPos >= 0)  {             // we have a match
				int stopPos = movieInfo.substring(startPos).indexOf("</td>")+startPos;
				if (movieInfo.substring(startPos, stopPos).contains(listingInfo.getYearMade())) {
					outputUrl = arrayOfIMDBentries[i];
					found=true;
				}
			}
		}

		return outputUrl;
	}

	
}	
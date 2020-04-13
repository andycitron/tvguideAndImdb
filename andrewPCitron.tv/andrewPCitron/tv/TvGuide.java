/**
 * 
 *  @author Andrew P. Citron
 *  various methods to fetch information from tvguide.com
 */ 
package andrewPCitron.tv;

import java.util.HashMap;
import java.util.Iterator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;



public class TvGuide {
	private HashMap<String, MovieInfo>  movieInfoHolder;         // holds hashmap of all movies
	private infoOnCurrentShowID infoOnCurrentShow;

	// needs to know if we get these channels
	private int showtime;                // user has showtime?
	private int epix;                    // user has epix movie channel
	private int starz;                   // user has starz movie channel
	private int sundance;                // user has sundance movie channel
	private int ifc;                     // user has ifc movie channel (obsolete as I don't currently know what ifc is)
	private int tcm;                     // user has epix movie channel
	private int tmc;                     // user has the movie channel
	private int hbo;                     // user has hbo
	private int cinemax;                 // user has cinemax	
	private String provider;             // special number that means TimeWarner in Raleigh for TV Guide
	private UrlAccess utilityClass;	     // http access helpers
    private JSONParser jsonParser;       // JSON parser object to parse read file

	public TvGuide(HashMap<String, MovieInfo> movieInfo, String providerFromPropertyFile) {
	   infoOnCurrentShow = new infoOnCurrentShowID();   // initial class that gets reused on each movie
	   movieInfoHolder=movieInfo;
	   provider=providerFromPropertyFile;
	   utilityClass = new UrlAccess();                  // we use this over and over, so make it a class object
	   jsonParser = new JSONParser();
	   
	}

	public void rememberPackagesWeGet(int showtime, int epix, int starz,
			int sundance, int ifc, int tcm, int tmc, int hbo, int cinemax) {
				
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

	public void readOneDay(String valueOfDayAtMidnightInMilleseconds) {
		String URLToCheck = "https://mobilelistings.tvguide.com/Listingsweb/ws/rest/schedules/" + provider + "/start/" + valueOfDayAtMidnightInMilleseconds + "/duration/1440?ChannelFields=Name%2CFullName%2CNumber%2CSourceId&ScheduleFields=ProgramId%2CEndTime%2CStartTime%2CTitle%2CAiringAttrib%2CCatId&formattype=json&disableChannels=music%2Cppv%2C24hr";
		String movieInfo = utilityClass.readUrl(URLToCheck, 2);      // 2 is retry count
		// parse json and saving it to array 
		// System.out.println(movieInfo);		
      
        Object obj;
		try {
			obj = jsonParser.parse(movieInfo);
			
	        JSONArray channelList = (JSONArray) obj;

	        // Iterate over channels array
	        channelList.forEach((n) -> parseScheduleObject((JSONObject) n));
		} catch (ParseException e) {
			System.out.println("exception thrown reading one day from tvguide.");
			e.printStackTrace();
		}
        
	}

	private Object parseScheduleObject(JSONObject oneChannel) {
		// System.out.println ("Json object is " + oneChannel.toString());

		JSONObject channelInfo = (JSONObject) oneChannel.get("Channel");
		// System.out.println ("oneChannel object is " + channelInfo);
		String channelNumber = (String) channelInfo.get("Number");

		String channelName = (String) channelInfo.get("FullName");        // this could be used to determine which channels are hbo, showtime etc
		                                                                  // right now channel numbers are hardcoded in ThisIsAChannelWeGet.  Not very flexible.
		                                                                  // but knowing how to classify all channels seems difficult/impossible
		if (thisIsAChannelWeGet(channelNumber)) {
			// System.out.println ("We do get channelNumber " + channelNumber + " " + channelName);

			// loop array
			JSONArray programObject = (JSONArray) oneChannel.get("ProgramSchedules");  

			@SuppressWarnings("unchecked")
			Iterator <JSONObject>iterator =  programObject.iterator();
			while (iterator.hasNext()) {

				try {

					String oneShow = iterator.next().toJSONString();
					if (oneShow.length() <= 2) {                                    // I don't know why this would happen, seems like server bug
						// System.out.println("oneShow is empty " + oneShow);

					} else {

						//System.out.println(oneShow);
						JSONObject jsonObject = (JSONObject) jsonParser.parse(oneShow);						
						String title;
						Long catId;
						Long programId;
						Long endTime;
						Long startTime;
						try {
							title = (String) jsonObject.get("Title");
							catId = (Long) jsonObject.get("CatId");
							programId = (Long) jsonObject.get("ProgramId");
							endTime = (Long) jsonObject.get("EndTime");
							startTime = (Long) jsonObject.get("StartTime");
							if (catId == 1 && (((endTime - startTime)/60) >= 75))  {  // seems that 1 means movie, and its long enough to be a movie

								MovieInfo currentShow = new MovieInfo(channelNumber,
										title,
										"",     // we don't have year made from tvguide...yet
										"");    // we don't have type of movie from tvguide ... yet
								// for tvguide, we need to save programId so we can find details later
								currentShow.setMovieJson(programId.toString());
								// insert into hashmap
								movieInfoHolder.put(title, currentShow); 
								//if (infoOnCurrentShow.getMovieName().contains("Holmes") || infoOnCurrentShow.getMovieName().contains("Justice") || infoOnCurrentShow.getMovieName().contains("of Stalin")) {
								//    // convenient point to add breakpoint
								//	System.out.println(infoOnCurrentShow.getMovieName());
								//}

								//					   System.out.println("Added to movieInfoHOlder title is "+ title + " catId is " + catId + " programId is " + programId + 
								//							" startTime is " + startTime + " endTime is " + endTime);
							} else {
								if ( ((endTime - startTime)/60) > 75) {                   // sanity check to make sure we're not missing any movies
									// for debug seems to be working correctly  System.out.println(title + " is flagged as not being a movie.  catId " + catId);
								}

							}


						} catch (Exception e) {
							
							System.out.println("exception parsing one show " + oneShow);
							e.printStackTrace();
						}
					}

				} catch (ParseException e) {
					System.out.println("exception parsing one program object");
					e.printStackTrace();
				}

			}
		} else {
			// System.out.println ("We don't get channelNumber " + channelNumber + " " + channelName);
		}


		return null;
	}

	private boolean thisIsAChannelWeGet(String channelNumber) {
		// this routine should be parameterized and channels we get should be read from a table.  But for now, that's too hard and I've tailored it for my needs
		
		int iChannel = Integer.parseInt(channelNumber); 
		boolean thisChannelInList = true;   // assume we do get the channel

		if  ( ((iChannel == 33 ) || (iChannel == 47) || (iChannel == 170) || (iChannel == 174)) ||  // filter out lifetime 
			 ((iChannel == 55 ) || (iChannel == 78) || (iChannel == 123)) ||                        // filter out hallmark 	
		     ((iChannel == 66 ) || (iChannel == 79) || (iChannel == 86)) ||                         // filter out channels we don't get
		     ((iChannel == 88 ) || (iChannel == 99) || (iChannel == 119) || (iChannel == 120)) ||   // filter out channels we don't get
		     ((iChannel >= 135) && iChannel <= 137)  ||                                             // filter out channels we don't get
		     ((iChannel == 140 ) || (iChannel == 141) || (iChannel == 179) || (iChannel == 182)|| (iChannel == 186) ||  // filter out channels we don't get
			 ((iChannel == 307 ) || (iChannel == 311) || (iChannel == 312) || (iChannel == 330) || 
			 ((iChannel >= 331) && iChannel <= 342))  ||
			 ((iChannel >= 371) && iChannel <= 382))  ||
			 (iChannel == 386)  ||  
			 ((iChannel >= 392) && (iChannel <= 399))  ||
			 (iChannel == 402)  || 
			 ((iChannel >= 408) && (iChannel <= 440))  ||
			 (iChannel == 443)  ||
			 (iChannel == 462)  || 
			 ((iChannel >= 468) && (iChannel <= 469))  ||
			 ((iChannel >= 500) && (iChannel <= 510))  ||   // movies on demand			
			 ((iChannel >= 511) && (iChannel <= 521) && (hbo ==0) )  ||
			 (iChannel == 530)  ||                          // cinemax on demand
			 ((iChannel >= 531) && (iChannel <= 542) && (cinemax ==0) )  ||
			 (iChannel == 550)  ||                          // showtime on demand
			 ((iChannel >= 551) && (iChannel <= 561) && (showtime ==0) )  ||
			 (iChannel == 570)  ||                          // tmc on demand
			 ((iChannel >= 571) && (iChannel <= 572) && (tmc == 0)) ||
			 (iChannel == 580)  ||                          // starz on demand
			 ((iChannel >= 581) && (iChannel <= 586) && (starz ==0) )  ||
			 (iChannel == 594)  || (iChannel == 601) ||      // epix on demand
			 ((iChannel >= 595) && (iChannel < 599) && (epix == 0)) || 
			 (iChannel == 601)  ||                          // starz encore on demand
			 ((iChannel >= 602) && (iChannel <= 609) )  ||  // starz encore (no property file for this
			 ((iChannel >= 620) && (iChannel <= 624) )  ||  // other movie channels
			 ((iChannel == 625) || (iChannel == 626) && (sundance ==0) )  ||
			 ((iChannel >= 629) && (iChannel <= 630) )  ||  // hallmark and lifetime...never watch those even if we get them
			 ((iChannel == 631) && (tcm ==0) )          ||  // turner classic movies
			 ((iChannel >= 633) && (iChannel <= 640) )  ||  // other movies
			 ((iChannel >= 651) && (iChannel <= 661) )  ||  // pay per view
			 ((iChannel >= 700) && (iChannel <= 783) )  ||  // sports
			 (iChannel == 800)  ||                          // on demand
			 ((iChannel >= 804) && (iChannel <= 811) )  ||  // spanish
			 ((iChannel >= 834) && (iChannel <= 896) )  ||  // spanish
			 (iChannel == 899)  ||                          // spanish   
			 ((iChannel >= 910) && (iChannel <= 950) )  ||  // spanish
			 (iChannel == 958)  ||                          // spanish   
			 ((iChannel >= 960) && (iChannel <= 985) )  ||  // spanish 
			 ((iChannel >= 1000) && (iChannel <= 1020) )||  // on demand
			 (iChannel == 1226)  ||                         // on demand
			 (iChannel == 1256)  ||                         // wral protected 
			 (iChannel == 1304)  ||                         // local
			 ((iChannel >= 1400) && (iChannel <= 1849) )||  // foreign
			 ((iChannel >= 2001) && (iChannel <= 2495) )    // on demand
				) {
			
			thisChannelInList = false;	                    // this is a channel we do not get
		}	
		
		return thisChannelInList;
	}


	public HashMap<String, MovieInfo> getMovieInfoHolder() {
		
		return movieInfoHolder;
	}

	public void fetchMovieDetails(MovieInfo listingInfo) {
		String movieURL = "https://mapi.tvguide.com/listings/expanded_details?v=1.5&program=" + listingInfo.getMovieJson() + "&deviceOs=web";

		String movieDetails = utilityClass.readUrl(movieURL, 2);

		JSONObject obj;
		try {
			obj = (JSONObject) jsonParser.parse(movieDetails);

			String strDescription = (String) obj.get("description");
			if (strDescription != null) {
				listingInfo.setDescription(strDescription);
			}
			listingInfo.setTitleForRegexSearch((String) obj.get("title"));
			
			if (obj.containsKey("release_year")) {
				
				listingInfo.setYearMade(Long.toString((Long)(obj.get("release_year"))) );    // convert to string
			} else {
//				System.out.println("TV Guide detail didn't include release year");
				listingInfo.setYearMade("0");                                                // if year is 0, then IMDB search will try to fill in
			}
			
			  

			//  these next 2 require more logic as they are members of an array
			//  these are inside is a tvobject need to fetch that first

			JSONObject tvJSONObject = (JSONObject) obj.get("tvobject");
			if (tvJSONObject != null)  {                              // sometimes this isn't present.  We can fetch later from IMDB
	
				// seems there should be a more direct way of fetching json sub objects.  there is, I figured that out elsewhere.
				String tvObjectString = tvJSONObject.toJSONString();
				JSONObject tvObject = (JSONObject) jsonParser.parse(tvObjectString);

				JSONObject metacriticJSONObject = (JSONObject) tvObject.get("metacritic");
				if (metacriticJSONObject != null) {
						
		     		String metacriticString = metacriticJSONObject.toJSONString();
			    	JSONObject metacriticObject = (JSONObject) jsonParser.parse(metacriticString);
					String tvGuideRating = Long.toString((long)metacriticObject.get("score"));
					if (tvGuideRating != null) {
						listingInfo.setNonCriticsRating(tvGuideRating);  // need to also get int score
					}
				}
				
				JSONArray genreArray= (JSONArray) tvObject.get("genres");
				if (genreArray != null)  {                              // sometimes this isn't present.  We can fetch later from IMDB

					// loop through all
					String allGenres = "";
					Iterator<String> iterator = genreArray.iterator();
					while (iterator.hasNext()) {

						if (allGenres != "") {
								allGenres = allGenres + ", ";            // comma separate
						}
						String nextGenre = iterator.next();
						// System.out.println(nextGenre);
						
						allGenres = allGenres + nextGenre;						  
					}

					listingInfo.setTypeOfMovie(allGenres);               // need to also loop because could be more than one
				}

			}

		} catch (ParseException e) {
			System.out.println("Exception caught trying to parse TvGuide JSON");
			e.printStackTrace();
		}

	}  


}

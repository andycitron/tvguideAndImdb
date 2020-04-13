/**
 * 
 *  @author Andrew P. Citron
 *  methods to coordinate the fetching of data from tvguide.com and IMDB.com
 */ 

package andrewPCitron.tv;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class GetDataAndProduceSpreadSheet {
	// class variables
	private HashMap<String, MovieInfo> showMap;
	private HashMap<String, String> moviesWeveSeenMap;
	private TvGuide tvGuideAccess;

	private int showtime;                // user has showtime?
	private int epix;                    // user has epix movie channel
	private int starz;                   // user has starz movie channel
	private int sundance;                // user has sundance movie channel
	private int ifc;                     // user has ifc movie channel
	private int tcm;                     // user has epix movie channel
	private int tmc;                     // user has the movie channel
	private int hbo;                     // user has hbo
	private int cinemax;                 // user has cinemax
	private int seinfeldOnly;            // not looking for movies.  looking for seinfeld shows, not  currently implemented in this version
	private String zipCodeProviderMapping;  // uVerse uses zipCode, other services have a mapping to zipCode and provider
	private int omitMoviesWeveSeen;
	private Properties prop;             // to access property files

	

	public GetDataAndProduceSpreadSheet() {
		super();
		
		showMap = new HashMap<String, MovieInfo>(300);                 // start at 300, it'll grow if needed           
		moviesWeveSeenMap = new HashMap<String, String>(300);          

		// until I get a gui, set control variables here
		showtime = 0;                // user has showtime?
		epix = 0;                    // user has epix movie channel
		starz = 0;                   // user has starz movie channel
		sundance = 0;                // user has sundance movie channel
		ifc = 0;                     // user has ifc movie channel
		tcm = 0;                     // user has epix movie channel
		tmc = 0;                     // user has the movie channel
		hbo = 0;                     // user has hbo
		cinemax = 0;                 // user has cinemax
		seinfeldOnly = 0;            // not looking for seinfeld
		omitMoviesWeveSeen = 0;  // optional feature to omit movies that we've already seen  

		prop = new Properties();

		try {
			// if using this code from a web ap, this info should be queried from a user and stored in a database or some other persistant storage.
			// getting it from a property file works fine if dealing with just one user.  Otherwise, something more robust is needed
			FileInputStream fis = new FileInputStream("tvlistings.Properties");
			prop.load(fis);

			showtime = new Integer(prop.getProperty("showtime", "0"));
			epix = new Integer(prop.getProperty("epix", "0"));
			starz = new Integer(prop.getProperty("starz", "0"));
			sundance = new Integer(prop.getProperty("sundance", "0"));
			ifc = new Integer(prop.getProperty("ifc", "0"));
			tcm = new Integer(prop.getProperty("tcm", "0"));
			tmc = new Integer(prop.getProperty("tmc", "0"));
			hbo = new Integer(prop.getProperty("hbo", "0"));
			cinemax = new Integer(prop.getProperty("cinemax", "0"));
			seinfeldOnly = new Integer(prop.getProperty("seinfeldOnly", "0"));
			// property file should have either valueThatMapsToZipCode or zipcode and provider name
			zipCodeProviderMapping = prop.getProperty("valueThatMapsToZipCode", "");   
			String zipCode = prop.getProperty("zipCode", "");
			String provider = prop.getProperty("providerName", "");
			
			if (zipCodeProviderMapping == "") {    // not provided by property file, go figure it out
				zipCodeProviderMapping = figureOutProviderStringFromTvGuide(zipCode, provider);
				if (zipCodeProviderMapping =="" ) {
					System.out.println("We're screwed, unable to determine provider information from zipCode/provider " + zipCode + "/" + provider);
				}
				
			}
			
			omitMoviesWeveSeen = new Integer(prop.getProperty("omitMoviesWeveSeen", "0"));      

			tvGuideAccess = new TvGuide(showMap, zipCodeProviderMapping); 
			tvGuideAccess.rememberPackagesWeGet(showtime, epix, starz, sundance, ifc, tcm, tmc, hbo, cinemax);

			System.out.println("TvGuide TV Ap version April 11, 2020.  Starz " + starz + " Showtime = " + showtime + " omitMoviesWeveSeen = " + omitMoviesWeveSeen); 

			fis.close();


		} catch(IOException e) {
			e.printStackTrace();
		}

	}


	


	public void getAllInfoAndProduceCSV () {
		
		String yearStart = null;
		String monthStart = null;
		String dayStart= null;
		String twcCurrentDay = "";
		long   currentDayAsLong = 0;
		
		long oneDay = (long) 1000.0 * 60 * 60 * 24;

		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

		String today = getTodayForTVGuide();

		twcCurrentDay = today;
		System.out.println("Today for TvGuide is " + twcCurrentDay);
		currentDayAsLong = Long.parseLong(today);        

		// see if we need to set up hashmap of movies we've seen
		if (omitMoviesWeveSeen >= 0) {
			setUpListOfMoviesWeveSeen();        	  
		}

		for (int dayOfWeek = 0; dayOfWeek<=7 ; dayOfWeek++) {   // actually process 8 days just to be safe
			java.util.Date date = new java.util.Date(System.currentTimeMillis()+(oneDay*dayOfWeek));
			String datetime = dateFormat.format(date);

			String year =  datetime.substring(0, 4);
			String month = datetime.substring(5, 7);
			String day =   datetime.substring(8, 10);	

			if (dayOfWeek == 0) {                            // first day of list
				yearStart = year;
				monthStart = month;
				dayStart = day;

			}

			System.out.println("Processing Date: " + datetime);                 

			processOneDay(twcCurrentDay, year, month, day);
			// figure out tomorrow
			currentDayAsLong=currentDayAsLong+86400;      // added 3 zeros at end for uVerse, but not needed for TvGuide
			twcCurrentDay=Long.toString(currentDayAsLong);


		}
		
		//showMap = sqlLiteAccess.getMovieInfoHolder();  // Uverse not needed for TV Guide, left here for informational purposes
		showMap = tvGuideAccess.getMovieInfoHolder();
		String outputData= postProcessAllShows(showMap, yearStart, monthStart, dayStart, prop);

		System.out.println("Done");
		try {

			String  spreadSheetFnAndPath = prop.getProperty("spreadSheetFnAndPath"  , "C:\\Program Files\\Microsoft Office\\OFFICE11\\EXCEL.EXE");
			Runtime.getRuntime().exec(spreadSheetFnAndPath +" " + outputData);
		}
		catch (Exception err) {
			err.printStackTrace();
		}

	}
private void setUpListOfMoviesWeveSeen() {
		
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(
					"moviesWeveSeen.txt"));
			String line = reader.readLine();
			
			while (line != null) {
				line.trim();
				
				if ((line.equals("")) || (line.startsWith("//")) || (line.startsWith("/*"))) {
					line = reader.readLine();         // skip empty lines and comments
				} else {
					// System.out.println("Movie we've seen added to list " + line);
					moviesWeveSeenMap.put(line, "1");    // key is movie name
					// read next line
					line = reader.readLine();
					
				}

			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}


	private String getTodayForTVGuide() {
		
		Long currentTime = System.currentTimeMillis();
		String curMillisString = Long.toString(currentTime);
		
		curMillisString = curMillisString.substring(0, 8) + "00";   // only 2 00 need to be added at end for TvGuide.  was 5 for uVerse
		
		// System.out.println("curMillisString =" + curMillisString);
		return curMillisString;
	}
    



	public void processOneDay(String valueOfDayAtMidnight, String year, String month, String day)  {

		tvGuideAccess.readOneDay(valueOfDayAtMidnight);  
		// could print out movie hashmap here for debug

	}

		
/* this next method is obsolete.  was used before TimeWarner was bought by Spectrum */	
//private static String getYearOfMovieUsingShowInfo(String zipCode,
//		String airDateTime, String episodeId, String lineupId) {
	
	// http://tvlistings.timewarnercable.com/data/Json/detailEpisodeInfoJson/?headendId=NC32421X&airDateTime=201408032000&episodeId=MV001322650000&lineupId=11640
//	String requestUrl = "http://tvlistings.timewarnercable.com/data/Json/detailEpisodeInfoJson/?headendId=" + zipCode + "&airDateTime=" + airDateTime + "&episodeId=" + episodeId + "&lineupId=" + lineupId;
//	String jsonReply = utilityClass.readUrlAsAjax(requestUrl, 2);
//	String array[] = parsePage(jsonReply, "\"year\": \"" , "\"", null);
//	String year = array[0];  //TBD
//	return year;
//}
	
	
	
	private String postProcessAllShows( HashMap<String, MovieInfo> allMovies, String year, String month, String day, Properties prop){
		BufferedWriter writer = null; 
		String workingDir = prop.getProperty("workingDirectory", "c:\\andys\tv\\");
		//System.out.println("postProcessAllShows workingDirectory is " + workingDir);
		String outputFn = "WeeklyMovieListing.csv";
		String outputSpreadsheet = workingDir + year + month + day + outputFn;
		int countMoviesWeveSeen = 0;
		
		try {

			writer = new BufferedWriter(new FileWriter(outputSpreadsheet));
			writer.write("Channel,Movie Title,Year made,Rating,description,Movie Category");

			Iterator<String> iterator = allMovies.keySet().iterator();

			while( iterator.hasNext() ){

				String key  = (String) iterator.next();
				MovieInfo listingInfo = allMovies.get(key);

				String movieInList = null;
				if (omitMoviesWeveSeen != 0) {
					movieInList = moviesWeveSeenMap.get(listingInfo.getTitle());
					
				}

				if ((movieInList == null) ){                // no reason to omit this movie
					getMovieDetails(listingInfo);

					String forSpreadsheet = listingInfo.toString();
					if (forSpreadsheet.length() != 0) {
						//System.out.println("Writing listing entry to csv " + forSpreadsheet);
						//System.out.println("listingInfo.getMovieHref length = " + listingInfo.getMovieHref().toString().length());
						// TBD pull info out of listingInfo
						writer.write(forSpreadsheet);
					}  // if low rated, we don't write to spreadsheet  

					// free up some memory associated with listing info, getting oom on 32 bit machine
					listingInfo.setMovieJson("");        // free up string since we're done with it
					listingInfo.setTitle("");            // free up string since we're done with it
					listingInfo.setDescription("");      // free up string since we're done with it
					listingInfo.setNonCriticsRating(""); // free up string since we're done with it
					listingInfo.setYearMade("");         // free up string since we're done with it
					
				} else {
					countMoviesWeveSeen++;               // keep a count of movies we're ignoring because we've seen them.	
				}
			}


			writer.close();        

		} catch (Exception E) {
			System.out.println("Exception generating spreadsheet " + E.getMessage());
			E.printStackTrace();
			if (writer != null) {
				try{
					writer.close();
				} catch (Exception E1){};

			}

		}
		System.out.println("We've already seen " + countMoviesWeveSeen + " that have been omitted from spreadsheet");
		return (outputSpreadsheet);

	}
    
	private void getMovieDetails(MovieInfo listingInfo) {
		
		tvGuideAccess.fetchMovieDetails(listingInfo);                            // fetch the movie info from tvguide.com

		// if TV Guide doesn't have rating info, or synopsis, we have to get that from IMDB

		if (listingInfo.getTitle().contains("The Secrets of Jonathan Sperry")) {  // for debug of a particular show
		// convenient point to add breakpoint when trying to figure out issue with specific show
		//	System.out.println(listingInfo.getTitle());
		}

		// only look in IMDB if some information is missing
		if ((listingInfo.getDescription() == "") || (listingInfo.getNonCriticsRating() == "") || (listingInfo.getTypeOfMovie() == "") || (listingInfo.getYearMade() == "0") ) {
			IMDBInfo imdbAccess = new IMDBInfo();
			imdbAccess.getYearOfMovieUsingTitle(listingInfo.getTitle(), listingInfo);   
			if ((listingInfo.getNonCriticsRating() == "") && (imdbAccess.getIMDBRating() != ""))  {   // don't have tvguide rating and do have imdb rating

				// imdb rates from 1-10.  tv guide goes from 1 - 100 so scale imdb rating to match tv guide	
				float ratingAsLong = Float.parseFloat( imdbAccess.getIMDBRating() );
				ratingAsLong = ratingAsLong * 10;	

				DecimalFormat format = new DecimalFormat("0.##"); // Choose the number of decimal places to work with in case they are different than zero and zero value will be removed
				format.setRoundingMode(RoundingMode.DOWN);        // choose your Rounding Mode
			
				String ratingNoDecimals = format.format(ratingAsLong);

				listingInfo.setNonCriticsRating( ratingNoDecimals);  
			}

			// if we didn't have year of movie, but we do now....I'm not sure this will happen with tv guide
			if (listingInfo.getYearMade().contentEquals("0")) {
				if ( imdbAccess.getIMDBYear().length() >=3) {
					listingInfo.setYearMade(imdbAccess.getIMDBYear());
				} else {
					System.out.println("Year of movie not found for " + listingInfo.getTitle());
					// if spanish unicode chars in title, fetch of IMDB fails.  Remove those chars from title, we'll try again later if we don't have reviewer ratings
					if (imdbAccess.getIMDBRating().contentEquals("")){   // no  year or rating, maybe imdb fetch failed for illegal chars
						if (getRidOfUnicodeInTitle(listingInfo)) {       // if title changed, try to get info again, with changed title
							getMovieDetails(listingInfo);                // shouldn't recurse more than once as the title should already be changed, so next time it'll return false if title conversion occurs again
						}	  
					}
				}
			}	

			// check if TvGuide didn't have description, but IMDB did:
			if (listingInfo.getDescription().length() == 0) {
				try {
					if (imdbAccess.getIMDBDescription().length() != 0) {          // this threw an exception  
						String imdbDescription = imdbAccess.getIMDBDescription();
						// imdb has director and actor first, move description first
						// look for "  With "
						int pos = imdbDescription.indexOf("  With ");
						int endOfWith = -1;  
						if (pos >=0 ) {    // if With isn't found, don't look for end
							endOfWith = imdbDescription.substring(pos).indexOf('.');
						}
						StringBuffer alteredDescription;

						if (pos<0 || endOfWith<0) {   // if we didn't find those strings, just use original description
							alteredDescription=new StringBuffer(imdbDescription);
						} else {
							alteredDescription = new StringBuffer(imdbDescription.substring(pos+endOfWith+1));
							alteredDescription.append(" "+imdbDescription.substring(0, pos+endOfWith));
						}

						listingInfo.setDescription(alteredDescription.toString());
					}
				} catch (Exception e) {
					System.out.println("exception getting IMDBDescription for " + listingInfo.getTitle());
					e.printStackTrace();
				}
			} else { 				             // even if tvguide had description, it doesn't always list actors, so lets append that to description
				if (!imdbAccess.getIMDBActorsList().isEmpty() || !imdbAccess.getIMDBDirectorsList().isEmpty()) {                                            // **** make sure actors not already present in description????
                    String currentDescription = listingInfo.getDescription();
                    
                    // go through list and make sure description doesn't already have that actor name
                    List<String> actorsList = imdbAccess.getIMDBActorsList();
                    int actorCount = 0;
                    for (String oneActor : actorsList) {
                        if (!currentDescription.contains(oneActor)) {    // if description doesn't include this actor already
                        	if (actorCount == 0) {
                        		
                        		currentDescription+=" With:";
                        	}
                        	String padding = " ";
                        	if (actorCount > 0) {
                        	  padding =", ";                             // separate actors with a comma	
                        	}
                        	currentDescription+= padding + oneActor;
                        	actorCount++;
                        	
                        }
                    }
                    // now look for directors
                    List<String> directorsList = imdbAccess.getIMDBDirectorsList();
                    int directorCount = 0;
                    for (String oneDirector : directorsList) {
                        if (!currentDescription.contains(oneDirector)) {    // if description doesn't include this director already
                        	if (directorCount == 0) {
                        		
                        		currentDescription+=" Directed by:";
                        	}
                        	
                        	String padding = " ";
                        	if (directorCount > 0) {
                        	  padding =", ";                                // separate actors with a comma	
                        	}
                        	currentDescription+= padding + oneDirector;
                        	directorCount++;
                        	
                        }
                    }
                    
					listingInfo.setDescription(currentDescription);         // append list of actors to description
				}
			}
			
			// see if we don't already have type of movie
			if ( ((listingInfo.getTypeOfMovie()== "") || ((listingInfo.getTypeOfMovie()== "Not found in Category DB"))) && (imdbAccess.getIMDBTypeOfMovie()!= "") ) {
				listingInfo.setTypeOfMovie(imdbAccess.getIMDBTypeOfMovie());
			}
			
			if (imdbAccess.getIMDBAlsoKnownAs() != "") {                    // if also known as something, save that info as part of title.
				listingInfo.setTitle(listingInfo.getTitle() + " aka " + imdbAccess.getIMDBAlsoKnownAs());
			}
		}
	}
	
	private String figureOutProviderStringFromTvGuide(String zipCode, String specifiedProvider) {
		UrlAccess utilityClass = new UrlAccess();
		String URLToCheck = "https://mobilelistings.tvguide.com/Listingsweb/ws/rest/serviceproviders/zipcode/"+ zipCode + "?formattype=json";
		String providersInThisAreaJson = utilityClass.readUrl(URLToCheck, 2);
		String zipProviderResult = "";                   // assume we can't figure it out
		
		JSONParser jsonParser = new JSONParser();        // JSON parser object to parse read file
		try {
			// extract the json data 
			Object jsonObject = jsonParser.parse(providersInThisAreaJson);
						
			JSONArray providerList = (JSONArray) jsonObject;

			for (Object oneProvider :  providerList) {   
			 	 String extractedInfo = parseProviderElement(oneProvider, specifiedProvider);
			     if (extractedInfo != "") {              // could quit after found but I want look through all of them to know what they might be
			    	 zipProviderResult = extractedInfo;
			  	
			     }
			}
			 	
			
		} catch (ParseException e) {
			System.out.println("unable to determine provider code for " + specifiedProvider);
			e.printStackTrace();
		}	
		System.out.println("provider string for " + specifiedProvider + " is " + zipProviderResult);
		return zipProviderResult;
	}
	
	private String parseProviderElement(Object providersObject, String specifiedProvider) {
		String providerCode = "";
		JSONObject providerJSON = (JSONObject) providersObject;

		String providerName = (String) providerJSON.get("Name");
		String providerId = Long.toString((Long) providerJSON.get("Id"));

		// System.out.println("looking at provider " + providerName + " Id " + providerId);

		if (providerName.contentEquals(specifiedProvider)) {                                  // is this the TV service that was specified in the properties file?

			// get list or element that contains the related device flag
			Object obj = providerJSON.get("Devices");

			if (obj != null) {


				for (Object deviceObject : (JSONArray) obj) {
					JSONObject deviceJSONObject = (JSONObject) deviceObject; 

					String deviceName = (String) deviceJSONObject.get("DeviceName");
					String deviceFlag = Long.toString((Long) deviceJSONObject.get("DeviceFlag"));

					if (deviceName.contentEquals("Digital (non-rebuild)")) {  // I'm not sure how this works exactly, but I think this is the one we want
						// if its present
						providerCode = providerId + "." + deviceFlag;         // use this one

					} else {                                                  // 2nd best
						if (providerCode == "") {                             // use if we don't hav something better
							providerCode = providerId + "." + deviceFlag;     // I'm not certain how tv guide actually works, but try to use this other isn't found
						}
					}

				}

			}                                                                      // devices element not present 
		}
		//System.out.println("provider code is " + providerCode);

		return providerCode;
	}





	private boolean getRidOfUnicodeInTitle(MovieInfo listingInfo) {
		// @output true if title was changed, false not changed
		String strInput = listingInfo.getTitle();
        strInput = strInput.replace("\u00c0", "À");
        strInput = strInput.replace("\u00c1", "Á");
        strInput = strInput.replace("\u00c2", "Â");
        strInput = strInput.replace("\u00c3", "Ã");
        strInput = strInput.replace("\u00c4", "Ä");
        strInput = strInput.replace("\u00c5", "Å");
        strInput = strInput.replace("\u00c6", "Æ");
        strInput = strInput.replace("\u00c7", "Ç");
        strInput = strInput.replace("\u00c8", "È");
        strInput = strInput.replace("\u00c9", "É");
        strInput = strInput.replace("\u00ca", "Ê");
        strInput = strInput.replace("\u00cb", "Ë");
        strInput = strInput.replace("\u00cc", "Ì");
        strInput = strInput.replace("\u00cd", "Í");
        strInput = strInput.replace("\u00ce", "Î");
        strInput = strInput.replace("\u00cf", "Ï");
        strInput = strInput.replace("\u00d1", "Ñ");
        strInput = strInput.replace("\u00d2", "Ò");
        strInput = strInput.replace("\u00d3", "Ó");
        strInput = strInput.replace("\u00d4", "Ô");
        strInput = strInput.replace("\u00d5", "Õ");
        strInput = strInput.replace("\u00d6", "Ö");
        strInput = strInput.replace("\u00d8", "Ø");
        strInput = strInput.replace("\u00d9", "Ù");
        strInput = strInput.replace("\u00da", "Ú");
        strInput = strInput.replace("\u00db", "Û");
        strInput = strInput.replace("\u00dc", "Ü");
        strInput = strInput.replace("\u00dd", "Ý");

        // Now lower case accents
        strInput = strInput.replace("\u00df", "ß");
        strInput = strInput.replace("\u00e0", "à");
        strInput = strInput.replace("\u00e1", "á");
        strInput = strInput.replace("\u00e2", "â");
        strInput = strInput.replace("\u00e3", "ã");
        strInput = strInput.replace("\u00e4", "ä");
        strInput = strInput.replace("\u00e5", "å");
        strInput = strInput.replace("\u00e6", "æ");
        strInput = strInput.replace("\u00e7", "ç");
        strInput = strInput.replace("\u00e8", "è");
        strInput = strInput.replace("\u00e9", "é");
        strInput = strInput.replace("\u00ea", "ê");
        strInput = strInput.replace("\u00eb", "ë");
        strInput = strInput.replace("\u00ec", "ì");
        strInput = strInput.replace("\u00ed", "í");
        strInput = strInput.replace("\u00ee", "î");
        strInput = strInput.replace("\u00ef", "ï");
        strInput = strInput.replace("\u00f0", "ð");
        strInput = strInput.replace("\u00f1", "ñ");
        strInput = strInput.replace("\u00f2", "ò");
        strInput = strInput.replace("\u00f3", "ó");
        strInput = strInput.replace("\u00f4", "ô");
        strInput = strInput.replace("\u00f5", "õ");
        strInput = strInput.replace("\u00f6", "ö");
        strInput = strInput.replace("\u00f8", "ø");
        strInput = strInput.replace("\u00f9", "ù");
        strInput = strInput.replace("\u00fa", "ú");
        strInput = strInput.replace("\u00fb", "û");
        strInput = strInput.replace("\u00fc", "ü");
        strInput = strInput.replace("\u00fd", "ý");
        strInput = strInput.replace("\u00ff", "ÿ");

        if (listingInfo.getTitle().contentEquals(strInput))  {  // no changes made
          return(false) ;
        } else {      	        
          listingInfo.setTitle(strInput);      // get rid of unincode in title
          return(true);                        // indicate changes were made
        }
		
	}




}

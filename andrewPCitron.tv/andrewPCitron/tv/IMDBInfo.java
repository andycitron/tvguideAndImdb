/**
 * 
 *  @author Andrew P. Citron
 *  various methods to fetch info from IMDB.com
 */ 
package andrewPCitron.tv;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.util.*;

public class IMDBInfo {
    private String year = "";
    private String rating = "";
    private String description ="";    // new for uVerse
    private String typeOfMovie="";     // added when I added specific movie search of IMDB
	private List<String> actorsList;
    private List<String> directorsList;
    private String alsoKnownAs = "";

	private UrlAccess utilityClass;
    private WebSavePageParser webSavePageParser;
    
    public IMDBInfo () {               // constructor
       utilityClass = new UrlAccess();
 	   webSavePageParser = new WebSavePageParser();   	
 	   actorsList = new ArrayList<String>();
 	   directorsList = new ArrayList<String>();
    	
    }
    	
	/* This fills in a lot of fields in imdbInfo */
	public void getYearOfMovieUsingTitle(String titleFromListing, MovieInfo listingInfo  ) {

		//IMDBInfo fetchedIMDBinfo = new IMDBInfo(); 
		String outputYear = new String("");
		String URLToCheck;

		titleFromListing = titleFromListing.replaceAll("&amp;", "&"); // actual listing result seems to have & instead of url encoding
		
		String alteredTitle = titleFromListing;                       // urlencode this.  I probably should have used a helper class for this instead of doing it myself
		// I've seen this fail with targeted searchString AlteredTitle = titleFromListing.replaceAll("-", "_");  // no dashes allowed in url use underscore		
		alteredTitle = alteredTitle.replaceAll("%", "\\%25");         // first encode % sign.  gotta do this first as we're about to add some % into string 
		alteredTitle = alteredTitle.replaceAll(" ", "\\%20");         // no spaces in url, use _ underscore if using typeahead, otherwise use + (%20) for blank		
		alteredTitle = alteredTitle.replaceAll("&", "\\%26");         // & needs to be url encoded for a url  **** double check this as I used to change it in titleFromListing
		alteredTitle = alteredTitle.replaceAll("'", "\\%27");         // no single quotes in url, use url encoding
		alteredTitle = alteredTitle.replaceAll(",", "\\%2C");         // encode comma
		alteredTitle = alteredTitle.replaceAll("#", "\\%23");         // encode pound sign 

		// new for uverse ap need to remove from input title
		alteredTitle = alteredTitle.replaceAll("\"", "");             // remove double quotes

		String titleToUseForRegex = listingInfo.getTitleForRegexSearch();

		String stage = "0th find, specific movie search";             // only used if debugging stmts uncommmented

		getYearOfMovieWithTargetedSearch(alteredTitle, listingInfo);

		if ((getIMDBDescription().equals("")) || getIMDBRating().equals("")) {   // direct search didn't find everything we need

			// probably not found, try full search
		
			// AlteredTitle = AlteredTitle.replaceAll(" ", "+");     // no blanks allowed in url use + (url encoding)
			URLToCheck = "https://www.imdb.com/find?q=" + alteredTitle;
			readMoviePageAndParseIt(titleFromListing, listingInfo, outputYear,
					URLToCheck, alteredTitle, titleToUseForRegex);

		}                                                            // endif direct search worked

	}

	/**
	 * @param titleFromListing
	 * @param listingInfo
	 * @param outputYear
	 * @param URLToCheck
	 * @param alteredTitle
	 * @param titleToUseForRegex
	 */
	private void readMoviePageAndParseIt(String titleFromListing,
			MovieInfo listingInfo, String outputYear, String URLToCheck,
			String alteredTitle, String titleToUseForRegex) {

		boolean successfulFindOfAkaMovie = false;                    // in case we have to search for alsoKnownAs title                                  
		String stage;
		String[] yearOfMoviePlus;

		stage = "first find";                                        // for debug
		//System.out.println("IMDB full movie url is " + URLToCheck);
		try {
			
			String movieHTMLPage = utilityClass.readUrl(URLToCheck, 2);      // 2 is retry count

			String leftBracket = "\" >" + titleToUseForRegex +"[\\s]*+</a> \\(";
			String rightBracket = "\\) </td";
			yearOfMoviePlus = webSavePageParser.parsePageCaseInsensitive(movieHTMLPage, leftBracket , rightBracket, "I\\)");  // exclude I) which is in some listing
			if ((yearOfMoviePlus != null) && yearOfMoviePlus.length != 0) {
				//System.out.println("yearOfMoviePlus is " + yearOfMoviePlus[0]);

				outputYear = yearOfMoviePlus[0];      // will this ever be hit? Yes
				outputYear = outputYear.trim();

			} else {
				stage = "parse 1.5";                  // new for uverse ap. but I think parse 1 is wrong
				leftBracket = "\" >" + titleToUseForRegex +"[\\s]* </a> \\(";
				rightBracket = "\\) </td";
				yearOfMoviePlus = webSavePageParser.parsePageCaseInsensitive(movieHTMLPage, leftBracket , rightBracket, "\\(");
				if ((yearOfMoviePlus != null) && yearOfMoviePlus.length != 0) {
					//System.out.println("yearOfMoviePlus is " + yearOfMoviePlus[0]);

					outputYear = yearOfMoviePlus[0];
					outputYear = outputYear.trim();

				} else {

					stage = "second parse";
					// seems there might be extra chars in result (I), try another parse
					leftBracket = "\" >" 	+ titleToUseForRegex + "[\\s]*</a> \\([I]+\\) \\(";
					rightBracket = "\\) </td";
					yearOfMoviePlus = webSavePageParser.parsePageCaseInsensitive(movieHTMLPage, leftBracket , rightBracket, "\\(");
					if ((yearOfMoviePlus != null) && yearOfMoviePlus.length != 0) {
						//System.out.println("yearOfMoviePlus is " + yearOfMoviePlus[0]);

						outputYear = yearOfMoviePlus[0];
						outputYear = outputYear.trim();

					} else {
						stage = "third parse";
						// seems there might be extra chars in result (I), try another parse
						leftBracket = "\" >" 	+ titleToUseForRegex + "[\\s]*</a> \\(";
						rightBracket = "\\) \\(TV Episode\\) <";
						yearOfMoviePlus = webSavePageParser.parsePageCaseInsensitive(movieHTMLPage, leftBracket , rightBracket, "[()<>]");  // exclude was: "\\("
						if ((yearOfMoviePlus != null) && yearOfMoviePlus.length != 0) {
							//System.out.println("yearOfMoviePlus is " + yearOfMoviePlus[0]);
							outputYear = outputYear.trim();
							outputYear = yearOfMoviePlus[0] + " TV Episode";                // flag it as something we don't want to record

						} else {                                        					// going to page for full name failed
							stage = "forth parse";
							rightBracket = "\\) \\(TV Movie\\) <";
							yearOfMoviePlus = webSavePageParser.parsePageCaseInsensitive(movieHTMLPage, leftBracket , rightBracket, ">");
							//                  can't figure out right regex for this
							String toLookFor = "\" >" + titleFromListing +"</a> (";
							int pos = movieHTMLPage.indexOf(toLookFor);
							if (pos > -1){
								String yearString = movieHTMLPage.substring(pos+toLookFor.length(), pos+toLookFor.length()+4);
								outputYear = yearString;


								if (movieHTMLPage.substring(pos+toLookFor.length()+4, pos+toLookFor.length()+20).contains("TV Movie") ) {
									setIMDBTypeOfMovie("TV Movie");	
								} else if (movieHTMLPage.substring(pos+toLookFor.length()+4, pos+toLookFor.length()+20).contains("Video") ) {
									setIMDBTypeOfMovie("Video");	
								} else if (movieHTMLPage.substring(pos+toLookFor.length()+4, pos+toLookFor.length()+20).contains("TV Episode") ) {
									setIMDBTypeOfMovie("TV Episode");	
								} else if (movieHTMLPage.substring(pos+toLookFor.length()+4, pos+toLookFor.length()+20).contains("TV Series") ) {
									setIMDBTypeOfMovie("TV Series");	
								} else if (movieHTMLPage.substring(pos+toLookFor.length()+4, pos+toLookFor.length()+20).contains("Short") ) {
									setIMDBTypeOfMovie("Short");	
								} else if (movieHTMLPage.substring(pos+toLookFor.length()+4, pos+toLookFor.length()+20).contains("TV Special") ) {
									setIMDBTypeOfMovie("TV Special");	
								} else   {

									// for debug see what else might be there
									// System.out.println("type of movie might be "+ movieInfo.substring(pos+toLookFor.length()+4, pos+toLookFor.length()+20) );
								}

							} else { 	
								stage = "fifth parse";
								rightBracket = "\\) </td";                        
								yearOfMoviePlus = webSavePageParser.parsePage(movieHTMLPage, leftBracket , rightBracket, "\\)");  // try case sensitive, no trailing ) allowed in year
								if ((yearOfMoviePlus != null) && yearOfMoviePlus.length != 0) {
									//System.out.println("yearOfMoviePlus is " + yearOfMoviePlus[0]);

									outputYear = yearOfMoviePlus[0];
									outputYear = outputYear.trim();

								} else {
									//System.out.println("movieInfo " +movieInfo);
									stage = "sixth parse"; // added during uverse ap processing probably dead code now

									rightBracket = "\\) \\(Video\\) <";                       
									yearOfMoviePlus = webSavePageParser.parsePage(movieHTMLPage, leftBracket , rightBracket, "\\)");  // try case sensitive, no trailing ) allowed in year
									if ((yearOfMoviePlus != null) && yearOfMoviePlus.length != 0) {
										//System.out.println("yearOfMoviePlus is " + yearOfMoviePlus[0]);

										outputYear = yearOfMoviePlus[0];
										outputYear = outputYear.trim();
										setIMDBTypeOfMovie("Video");

									} else {

										// look for an 'also known as' aka string
										stage = "akaString";
										String akaString = "aka <i>\"" + titleFromListing + "\"</i>";  
										if (movieHTMLPage.contains(akaString)) {

											// System.out.println(titleFromListing + " is also known as something");

											// try to isolate the other title it is known as
											// href="/title/tt6622186/?ref_=fn_al_tt_1" >FERRARI-Race to Immortality</a> (2017) <br/>aka <i>"Ferrari: Race to Immortality"</i>

											int endPos = movieHTMLPage.indexOf(akaString);   // aka info is after the other title
											int startPos = endPos -1;
											// loop backward looking for 'h' then see if its href
											setIMDBTypeOfMovie("aka something else");    // just in case we don't find anything, make sure this is set
											boolean hrefFound = false;

											while ((!hrefFound) && (startPos>0)) {
												// char forDebug = movieInfo.charAt(startPos);      // used only for debug 
												if (movieHTMLPage.charAt(startPos) == 'h') {            // now see if what follows is the href
													// String forDebug2 = movieInfo.substring(startPos, startPos+4);  // used only for debug
													if (movieHTMLPage.substring(startPos,startPos+4).contentEquals("href")) {
														hrefFound = true;                           // to get out of loop

														String akaInfo = movieHTMLPage.substring(startPos, endPos);	      // get url out of aka information
														String[]akaURLarray = webSavePageParser.parsePage(akaInfo , "href=\"", "\"", null);
														if (((akaURLarray != null) && akaURLarray.length != 0))  {

															String akaURL = "https://www.imdb.com" + akaURLarray[0];

															int akaNamePos = akaInfo.indexOf(">")+1;;                 // get beyond href="url
															int endAkaMoviePos = akaInfo.indexOf("</a>");
															setIMDBAlsoKnownAs( akaInfo.substring(akaNamePos, endAkaMoviePos));

															successfulFindOfAkaMovie = readJSONFromPage(akaURL);     // try to find movie on aka page 
																	    								
														} else {                                                     // aka web page not successfully fetched
															successfulFindOfAkaMovie = false; 
															setIMDBTypeOfMovie("aka " + movieHTMLPage.substring(startPos, endPos));     // we know the aka url, could fetch that info to populate data about movie.  For now, its not worth the effort
														}

													} else {
														startPos = startPos-1;                      // look at prior character
													}													
												} else {
													startPos = startPos-1;                          // look at prior character
												}
											}

											if (successfulFindOfAkaMovie) {
												outputYear = getIMDBYear();

											} else {


												leftBracket = "</a> \\(";
												rightBracket = "\\) <br/>";
												yearOfMoviePlus = webSavePageParser.parsePage(movieHTMLPage, leftBracket , rightBracket, null /*"[()<>]" */);  // try case sensitive
												if ((yearOfMoviePlus != null) && yearOfMoviePlus.length != 0) {
													//System.out.println("yearOfMoviePlus is " + yearOfMoviePlus[0]);

													outputYear = yearOfMoviePlus[0];
													outputYear = outputYear.trim();
												} else {                             // try one more thing
													rightBracket = "\\) \\(";
													yearOfMoviePlus = webSavePageParser.parsePage(movieHTMLPage, leftBracket , rightBracket, null/* "[()<>]" */);  // try case sensitive
													if ((yearOfMoviePlus != null) && yearOfMoviePlus.length != 0) {
														//System.out.println("yearOfMoviePlus is " + yearOfMoviePlus[0]);

														outputYear = yearOfMoviePlus[0];
														outputYear = outputYear.trim();
													} else {
														System.out.println("Aka string for " + titleFromListing + " not found in page: " + movieHTMLPage); 
													}
												}
											}
										} 	
									}
									// still no hit, try search
									if (((yearOfMoviePlus == null) || (yearOfMoviePlus.length == 0)) && !successfulFindOfAkaMovie ) {

										// for debug, print out page that we couldn't find name in
										//System.out.println("Page with no movie info found for " + titleFromListing + " Url is "+ URLToCheck + "\n" +movieInfo);
										stage = "first search";
										//System.out.println("Page with no movie year.  Now searching for " + titleFromListing);
										alteredTitle = alteredTitle.replaceAll("\\%20", "_");             // _ underscore if using typeahead, otherwise use + for blank

										String firstLetterInName = alteredTitle.substring(0, 1); 
										firstLetterInName = firstLetterInName.toLowerCase();

										// first search for 6 chars
										int titleLen = alteredTitle.length();
										if (titleLen > 7) {                                           // or full title if not 6 chars long
											titleLen = 6;
										}

										URLToCheck = "http://sg.media-imdb.com/suggests/" +  firstLetterInName  + "/" + alteredTitle.substring(0, titleLen).toLowerCase() + ".json";

										outputYear = lookAheadIMDB(URLToCheck, titleFromListing);      // output is "" if lookahead failed
										if (outputYear.contentEquals("")) {                            // remove 2 (or less) chars from search and try again
											titleLen = titleLen - 2;            // try 2 chars shorter
											if (titleLen <=0) {
												titleLen = 1;
											}
											stage = "second search";
											URLToCheck = "http://sg.media-imdb.com/suggests/" +  firstLetterInName  + "/" + alteredTitle.substring(0, titleLen).toLowerCase() + ".json";
											outputYear = lookAheadIMDB(URLToCheck, titleFromListing);  // output is "" if look ahead failed
											if (!outputYear.contentEquals("")) { 
												System.out.println("LookAheadIMDB got a hit for " + titleFromListing + " in "  + URLToCheck);
											}


										} else {                                                        // for debug
											System.out.println("LookAheadIMDB got a for " + titleFromListing + " in " + URLToCheck);
										}
									}

								}
							}

							if (outputYear.contentEquals("")) {
								// never gets hits System.out.println(stage + " LookAheadIMDB no hits " + URLToCheck);
							}
						}  
					}

				}

			}	
			if (!successfulFindOfAkaMovie) {                             // if we didn't find all the data in json on aka page
				// as long as we went to IMDB, let's get other potentially useful info
				// rating looks like this <span itemprop="ratingValue">7.8</span>7.8</span>
				String[] rating = webSavePageParser.parsePage(movieHTMLPage, "<span itemprop=\"ratingValue\">\"", "</span>", "<");

				if ((rating!= null) && (rating.length>0)) {
					setIMDBRating(rating[0]);
				} 

				// make sure fetchedIMDBinfo.setYear is called before figureOutWhichYearIsPlayingOnUverse fixed in uverse ap version
				int pos = outputYear.indexOf(',');      //  search imdb sometimes has a comma
				if (pos > 0 ) {
					outputYear = outputYear.substring(0, pos);  // remove comma, but keep checking
				}	

				// the also 'known as path' doesn't successfully remove ), so do it here to cover all cases
				pos = outputYear.indexOf(')');     

				if (pos > 0 ) {
					outputYear = outputYear.substring(0, pos);
				}

				setIMDBYear(outputYear);

				// year made is used when figuring out which details url is from the year of the movie
				if ((listingInfo.getYearMade()== null) || (listingInfo.getYearMade().contentEquals("0")) ) {
					if (outputYear.contentEquals("")) {        // we don't know the year
						System.out.println("yearMade Not set before calling figureOutWhichYearIsPlaying for " + listingInfo.getTitle() );
					}
				}

				// <a href="/title/tt1723127/?ref_=fn_al_tt_1" >Leaving Limbo</a> (2013) </td>
				// System.out.println("movieInfo is: \n" + movieInfo);
				String[] detailUrls = webSavePageParser.parsePageCaseInsensitive(movieHTMLPage,"<a href=\"" , "\"\\s*>"+listingInfo.getTitleForRegexSearch(), "<"); // **** changed 11/19 to add \\s* and getTitleForRegexSearch 
				if (detailUrls.length != 0) {
					// tbd check for year if more than one item on page, plus error check readUrl
					String IMDBdetailsUrl = detailUrls[0];  // assume we'll fetch the 1st
					if (detailUrls.length > 1) {     // more than one version of this movie
						IMDBdetailsUrl = figureOutWhichYearIsPlaying(listingInfo, detailUrls, movieHTMLPage);				
					} 

					String IMDBDetails = utilityClass.readUrl("https://www.imdb.com"+IMDBdetailsUrl, 2);
					//                               <span itemprop="ratingValue">6.6</span></strong>
					// this page could have json on it
					boolean foundJsonInPage = parseIMDBJson(IMDBDetails/*, listingInfo*/);	
					
					if ((!foundJsonInPage) && IMDBDetails.length() != 0) {
						// changed Aug 2018 rating = parsePage(IMDBDetails, "<span itemprop=\"ratingValue\">", "</span>", "<");
						String[] ratingSection = webSavePageParser.parsePage(IMDBDetails, "aggregateRating", "\\},", null );
						if ((ratingSection != null) && (ratingSection.length!=0)) {
							rating = webSavePageParser.parsePage(ratingSection[0], "ratingValue\": \"", "\"", null);
						} else { 
							// happens when no ratings have been posted System.out.println("RatingSection null: IMDBDetails: " + "https://www.imdb.com"+IMDBdetailsUrl);
							rating = null;
						}

						if ((rating!= null) && (rating.length>0)) {
							setIMDBRating(rating[0]);
						} 

						// save IMDB description
						// <meta name="description" content="Directed by Sandy Boikian.  With Mandy Brown, Elias Cecil, David Fruechting, Luke Barnett. A young woman on the brink of a bright future whose dreams are destroyed by a car wreck that leaves her in a coma for nineteen years. Her attempt to &quot;fix shambles&quot; at age 38 only leads to further heartbreak." />

						String[] description  = webSavePageParser.parsePage(IMDBDetails, "<meta name=\"description\" content=\"", "\" />", null);
						if ((description!= null) && (description.length>0)) {
							setIMDBDescription(description[0]);
						} else {
							System.out.println("no description on" + "https://www.imdb.com"+IMDBdetailsUrl);
						}

						if (listingInfo.getTypeOfMovie().contains("aka ") || listingInfo.getTypeOfMovie().contains("Not found in Category DB")) { // see if we need to check genre on this page

							leftBracket = "\"genre\": \"";
							rightBracket = "\",";
							String[] genreArray = webSavePageParser.parsePage(IMDBDetails, leftBracket , rightBracket, null /*"[()<>]" */);
							// look into genre

							if ((genreArray != null) && (genreArray.length != 0) ) {

								setIMDBTypeOfMovie(genreArray[0].trim() + " " + getIMDBTypeOfMovie());
							}
						}
					}	
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("full search from imdb threw exception " +  URLToCheck);
		}
		return;
	}
	

	private boolean readJSONFromPage(String URLToCheck) {
		boolean foundJsonInPage = false;
		String aKaMovieInfo = utilityClass.readUrl(URLToCheck, 2);              // 2 is retry count
		foundJsonInPage = parseIMDBJson(aKaMovieInfo);		
		
		return foundJsonInPage;
	}

	private boolean parseIMDBJson(String htmlPage) {
		boolean foundJsonInPage = false;                    // assume not found
		String movieJson = "";
		if (htmlPage.length()>0) {                          // is there something to check
			String scriptStart = "<script type=\"application/ld+json\">";
			int startPos = htmlPage.indexOf(scriptStart);
			if (startPos != -1) {
				startPos+=scriptStart.length();              // get past start of script
				movieJson = htmlPage.substring(startPos);    // movie json now starts with start of json script
				int endPos = movieJson.indexOf("</script>");
				movieJson = movieJson.substring(0, endPos);  // this should now be isolated json
				// System.out.println("json on page is " + movieJson);

				JSONParser jsonParser = new JSONParser();    // JSON parser object to parse read file
				try {
					// extract the json data and store it in listingInfo
					JSONObject jsonObject = (JSONObject) jsonParser.parse(movieJson);
					foundJsonInPage = true; 

					// any particular object might not be present, so put try/catch around each
					if (getIMDBDescription()=="") {     // we don't already have a description
						try {
							setIMDBDescription( (String) jsonObject.get("description") );
						} catch (Exception e) {
							System.out.println("description not present in json");
						}
					}

					if (getIMDBYear()=="") {     // we don't already have a description
						// I think datePublished must be the year it came out				    				    
						try {
							String yearMonthDay = (String) jsonObject.get("datePublished");
							if (yearMonthDay != null) {
							  if (yearMonthDay.length() > 4) {       // if day and year are there, just take year   
								setIMDBYear(yearMonthDay.substring(0, 4));
							  } else { 
								setIMDBYear(yearMonthDay);          // if its that short, just set from the first part
							  }
							}
						} catch (Exception e) {
							System.out.println("datePublished not present in json");
						}
					}

					if (getIMDBRating()=="") {     // we don't already have a description
						// get aggregate rating.  rating value is embedded in that
						try {
							JSONObject aggregateRatingObject = (JSONObject)jsonObject.get("aggregateRating");
							//JSONObject jsonRatingObject = (JSONObject) jsonParser.parse(aggregateRating);
							setIMDBRating((String)aggregateRatingObject.get("ratingValue"));
						} catch (Exception e) {
							// System.out.println("aggregateRating not present in json");
						}
					}

					Object obj;
					if (getIMDBActorsList().isEmpty()) {     // we don't already have a list of actors

						JSONArray actorList;
						
						try {
							// get actors
							obj = jsonObject.get("actor");

							if (obj != null) {
								if (obj instanceof java.util.List) {

									actorList = (JSONArray) jsonObject.get("actor");                   // if there's only 1 actor this throws an exception

									// Iterate over actors array
									actorList.forEach((n) -> parsePersonObject((JSONObject) n, getIMDBActorsList()) );
								} else {                                                               // not a list, only 1
									// System.out.println("actor is a single element TBD process single element");
									JSONObject actorElement =  (JSONObject) jsonObject.get("actor");							
									getIMDBActorsList().add((String)actorElement.get("name"));         // element to list

								}
							}                                                                          // actors element not present 

						} catch (Exception e) {
							System.out.println("actors not present in json");
							e.printStackTrace();
						}

						try {
							// get director 
							obj = jsonObject.get("director");
							if (obj != null) {
								if (obj instanceof java.util.Map) {
									JSONObject  directorList = (JSONObject) jsonObject.get("director");    // if there's more than one this seem to throw exception

									parsePersonObject(directorList, getIMDBDirectorsList()) ;
								} else {                                                                   // not a list, only 1
									// System.out.println("director is a list TBD process list");								

									JSONArray directorArray = (JSONArray) jsonObject.get("director");
									for (Object temp: directorArray) {

										JSONObject directorElement = (JSONObject)temp;
										getIMDBDirectorsList().add((String)directorElement.get("name")); 
									}
								}
							}                                                                             // director element null
						} catch (Exception e) {
							System.out.println("directors not present in json");
							System.out.println(e.getMessage());
						}
					}

					if (getIMDBTypeOfMovie()=="") {     // we don't already have a genre
						try {
							// get all genres	
							obj = jsonObject.get("genre");
							if (obj != null) {
								if (obj instanceof java.util.List) { 

									JSONArray genreArray = (JSONArray) jsonObject.get("genre");
									for (Object temp: genreArray) {

										String genreElement = (String)temp;
										setIMDBTypeOfMovie(getIMDBTypeOfMovie() + " " + genreElement); 
									}
								}else {                                                                 // not a list, only 1 and its string

									setIMDBTypeOfMovie((String) obj);

								}
							}                                                                       // genre null
						} catch (Exception e) {
							System.out.println("genres not present in json " );
							e.printStackTrace();
						}
					}

				} catch (ParseException e) {
					System.out.println("exception caught when accessing IMDB json " );
					e.printStackTrace();
				}	

			}

		}

		return foundJsonInPage;
	}
	
	private void parsePersonObject(JSONObject actorElement, List<String> listToAddTo) {
		String personsName = (String)actorElement.get("name");
		listToAddTo.add(personsName);
		return;
			
	}
	

	private void getYearOfMovieWithTargetedSearch(String urlEncodedTitle, MovieInfo listingInfo) {

		String URLToCheck;

		//IMDBInfo fetchedIMDBinfo = new IMDBInfo();

		Integer yearOfMovieMinus1;
		String yearMadeFromTVGuide = listingInfo.getYearMade();
		String finalSearchYear;

		if (!yearMadeFromTVGuide.contentEquals("0")) {                           // did tvguide have a year for the movie?
			yearOfMovieMinus1 = Integer.parseInt(listingInfo.getYearMade())-1;	
			finalSearchYear = Integer.toString(Integer.parseInt(listingInfo.getYearMade())+1);  // years don't always seem to match
		} else {
			yearOfMovieMinus1 = 1900;
			finalSearchYear = "2100";                                            // find any movie...ever

		}

		// https://www.imdb.com/search/title/?title=Justice&title_type=feature,tv_movie,documentary&release_date=2017-01-01,2017-12-31                  // seems dates mismatch between imdb and uverse.  so give it an extra 6 months
		URLToCheck = "https://www.imdb.com/search/title/?title=" + urlEncodedTitle +"&title_type=feature,tv_movie,documentary&release_date="+yearOfMovieMinus1.toString()+"-07-01,"+finalSearchYear+"-07-01";
		// for debug System.out.println("IMDB full movie url is " + URLToCheck);
		try {

			String movieInfo = utilityClass.readUrl(URLToCheck, 2);              // 2 is retry count
			String movieInfoUpperCase = "";                                      // for use if title search doesn't work
			int startPos = movieInfo.indexOf("<span>No results.</span>");
			if (startPos == -1) {                                                // make sure it doesn't say no results
				if (true /* I don't think this page has any json on it, so don't look for now !parseIMDBJson(movieInfo, listingInfo)*/ ) {
					startPos = movieInfo.indexOf(">"+listingInfo.getTitle()+"</a>"); 

					if (startPos == -1)  {                        					//  sometimes no hit because case doesn't match completely.  tbd see how to fix that
						String movieTitleUpperCase = listingInfo.getTitle().toUpperCase();
						movieInfoUpperCase = movieInfo.toUpperCase();               //  not very efficient.  would be nice to have a better way to do this
						// for debug System.out.println("no hit on IMDB search for title.  Convert to upper case and try that " + movieTitleUpperCase);
						startPos = movieInfoUpperCase.indexOf(">"+movieTitleUpperCase+"</A>"); 

					}

					if (startPos == -1 ) {                                          // if still no match try substitution &amp; for and
						if (listingInfo.getTitle().contains(" and ")) {
							String titleWithandReplace = listingInfo.getTitle().replaceAll(" and ", " &amp; ");
							startPos = movieInfo.indexOf(">"+titleWithandReplace+"</a>");  
						}
					}

					if (startPos == -1) {                                           // still can't find title?
						if (listingInfo.getTitle().contains("\"")) {
							String titleWithQuoteReplace = listingInfo.getTitle().replaceAll("\"", "");
							startPos = movieInfo.indexOf(">"+titleWithQuoteReplace+"</a>");  
						}

					}

					// if startPos isn't found, its usually because the movie name from TV Guide doesn't match what IMDB has.
					// Frequently it is because of punctuation.  Its hard to predict how they'll differ, so we might not 
					// find any info about this movie in IMDB
					if (startPos >= 0) {                                             // found movie

						String isolatedMovie = movieInfo.substring(startPos);
						int endPos = isolatedMovie.indexOf("<img alt=");
						if (endPos == -1) {
							isolatedMovie = isolatedMovie.substring(0);               // must be last movie, so grab full string		
						} else {
							isolatedMovie = isolatedMovie.substring(0, endPos);       // we now have isolated the movie
						}  

						// now pick out the IMDB year of the movie...which might be different from Uverse by 1 year...not sure why
						String movieSpan = "<span class=\"lister-item-year text-muted unbold\">"+"(";
						startPos = isolatedMovie.indexOf(movieSpan);

						endPos = isolatedMovie.indexOf(")");                        // have to handle this:  (II) (2017)
						
						String isolatedYear = "";
						if (startPos+movieSpan.length() < endPos) {                 // there's a case where the wrong movie is found
							isolatedYear = isolatedMovie.substring(startPos+movieSpan.length(), endPos);
							isolatedYear = isolatedYear.replace(" TV Movie", "").trim();       // make sure no extra junk
						} 
						// System.out.println("year string:" + isolatedYear);

						if (( (endPos!=-1 && startPos !=-1)) && isInteger(isolatedYear)) {
							setIMDBYear(isolatedYear);
						} else {                                                   // look past first set of parens
							startPos = endPos+1;
							isolatedYear = isolatedMovie.substring(startPos);
							startPos = isolatedYear.indexOf("(");
							endPos = isolatedYear.indexOf(")");
							isolatedYear = isolatedYear.substring(startPos, endPos);
							isolatedYear = isolatedYear.replace("(", "");         // sometimes extra chars cause ( to not be removed

							// for debug System.out.println("year string try 2:" + isolatedYear);
							if (( (endPos!=-1 && startPos !=-1)) && isInteger(isolatedYear)) {
								setIMDBYear(isolatedYear);
							} else {
								System.out.println("getYearOfMovieWithTargetedSearch didn't find year in " + isolatedYear);
							}				
						}

						/* now find genre
	                     <span class="genre">
	                         Western            </span>
	                     </p>
						*/    
						String genreSpan = "<span class=\"genre\">";
						startPos = isolatedMovie.indexOf(genreSpan);
						String isolatedGenre = isolatedMovie.substring(startPos+genreSpan.length());
						endPos = isolatedGenre.indexOf("</span>");

						setIMDBTypeOfMovie(isolatedGenre.substring(0, endPos).trim());

						// next get rating:   <meta itemprop="ratingValue" content="4" />

						String ratingSpan = "<div class=\"inline-block ratings-imdb-rating\" name=\"ir\" data-value=\"";
						startPos = isolatedMovie.indexOf(ratingSpan);
						if (startPos != -1) {                                  // movie has some ratings
							String isolatedRating = isolatedMovie.substring(startPos+ratingSpan.length());
							endPos = isolatedRating.indexOf("\">");
							setIMDBRating(isolatedRating.substring(0, endPos).trim());
						}  

						// now find title and actors
						/*
				<p class="text-muted">
			    A U.S. Marshal seeking justice for his brother's murder defends a small town from a corrupt Mayor and his henchmen with intents to revive the civil war.</p>
						 */

						String descriptionDelim = "<p class=\"text-muted\">";
						startPos = isolatedMovie.indexOf(descriptionDelim);
						String isolatedDescription = isolatedMovie.substring(startPos+descriptionDelim.length());
						endPos = isolatedDescription.indexOf("</p>");

						// some titles have hrefs in them.  remove those
						// <a href='/name/nm0035842'>Neil Armstrong</a>
						String descriptionWithHrefsRemoved = isolatedDescription.substring(0, endPos).trim();
						String rebuildDescription = ""; 
						startPos = descriptionWithHrefsRemoved.indexOf("<a href=");
						if (startPos == -1) {
							rebuildDescription = descriptionWithHrefsRemoved; 
						} else {
							while (startPos != -1) {
								if (startPos == 1 || startPos == 0)  {  
									// System.out.println("startPos is " + startPos + " " + descriptionWithHrefsRemoved);
								}
								if ((descriptionWithHrefsRemoved.contains("Add a Plot") || startPos == 0)) {
									// System.out.println("Element doesn't have a Plot or nothing before href startPos = " + startPos);
									startPos = -1;                        // get out of loop
								} else {
									rebuildDescription += descriptionWithHrefsRemoved.substring(0, startPos-1) + " ";  // grab part of string before href
									startPos = descriptionWithHrefsRemoved.indexOf(">");
									String endAnchor = "</a>";
									endPos = descriptionWithHrefsRemoved.indexOf(endAnchor);
									rebuildDescription += descriptionWithHrefsRemoved.substring(startPos+1, endPos) + " ";     // get actors name and everything past it
									descriptionWithHrefsRemoved = descriptionWithHrefsRemoved.substring(endPos+endAnchor.length());
									startPos = descriptionWithHrefsRemoved.indexOf("<a href=");
									if (startPos == -1) {
										rebuildDescription +=descriptionWithHrefsRemoved; 
									}
								}
							}	  
						}
						// remove extra junk from title
						rebuildDescription = rebuildDescription.replace("See full summary &nbsp;&raquo;", "");

						setIMDBDescription(rebuildDescription);
						if (getIMDBDescription()!= "") {        // we did find a description
							// next concatenate list of actors and director to description which is a list of these:
							/*		
					<a href="/name/nm0299923/?ref_=adv_li_dr_0"
							>Richard Gabai</a>	
							 */
							String actorDelim = "<a href=\"/name/";
							startPos = isolatedMovie.indexOf(actorDelim);
							String isolatedActor = isolatedMovie.substring(startPos+actorDelim.length());

							// before looking for actors, make sure there are some listed
							if (isolatedMovie.contains("Director:") || isolatedMovie.contains("Stars:") || isolatedMovie.contains("Directors:") || isolatedMovie.contains("Directed by") || isolatedMovie.contains("Star:")) {
								startPos = isolatedActor.indexOf(">");
								isolatedActor = isolatedActor.substring(startPos+">".length());             // get past other markup
								endPos = isolatedActor.indexOf("</a>");
								while (startPos != -1 && endPos !=-1) {
									String actor = isolatedActor.substring(0, endPos).trim();
									// don't know if its an actor or director, so add to actors list.
									getIMDBActorsList().add(actor);

									startPos = isolatedActor.indexOf(actorDelim);                           // get next actor
									isolatedActor = isolatedActor.substring(startPos+descriptionDelim.length());   // get past other markup
									startPos = isolatedActor.indexOf(">");
									isolatedActor = isolatedActor.substring(startPos+">".length()).trim();   // get past other markup

									endPos = isolatedActor.indexOf("</a>");
									if ((isolatedActor.startsWith("</p>")) || (isolatedActor.startsWith("<p class")) || (isolatedActor.startsWith("<span class")) || (isolatedActor.startsWith("</div>"))  ){
										endPos = -1;                                                         // we got to the end of the list
									} 
								}
							} else {
								// for debug System.out.println("No director or stars found in " + isolatedActor);
							}

						}

					} else {                                                                     // didn't find title, typically because of punctuation differences between tvguide.com and IMDB.com
						// System.out.println("targeted search found no title " + URLToCheck);                    
					}

				}                                                                              // we did find JSON for movie
			} else {
				// System.out.println("targeted search found no results for " + URLToCheck);  // this usually happens because punctuation differences between TV Guide and IMDB websites
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("targeted search from imdb threw exception " +  URLToCheck);
		}
		return;
	}
	
	public String lookAheadIMDB (String URLToCheck, String titleFromListing) {

		String outputYear = "";
		String movieInfo  = "";
		
		if(titleFromListing.startsWith("\"")) {                //leading and trailing quotes will cause search failures
			titleFromListing = titleFromListing.substring(1);  // remove leading quote
			int lastPos = titleFromListing.lastIndexOf("\"");  // find last quote
			titleFromListing = titleFromListing.substring(0, lastPos-1);  // removes last quote
			
		}
				
				

		try {
			movieInfo = utilityClass.readUrlWithHeaders(URLToCheck);

			if ((movieInfo != null) && (movieInfo.length()!=0) ) {
				//next 2 for debug only
				//System.out.println("Year of movie URL is http://tvlistings.zap2it.com/tv/chisum/" + episodeID );
				// String movieInfo = fullPageOfMovieInfo.replaceAll(" ", "");   // remove blanks, it'll make pulling year out easier...nope		
				//System.out.println("MovieInfo is " + movieInfo);

				String yearOfMoviePlus[] = webSavePageParser.parsePage(movieInfo, "\\{\"y\":", "\"l\":\"" + titleFromListing + "!*\",\"q\"", null);		

				if ((yearOfMoviePlus != null) && yearOfMoviePlus.length != 0) {
					//System.out.println("LookAheadIMDB is " + yearOfMoviePlus[0]);


					outputYear = yearOfMoviePlus[0];
					outputYear = outputYear.trim();
					
					// as long as we're looking at IMDB
					


				}  else {
					//System.out.println("LookAheadIMDB no hits for " + titleFromListing + " " +  URLToCheck);
				}
			}
		} catch (Exception e) {
			System.out.println("fetch from imdb threw exception for " + titleFromListing + " " +  URLToCheck);
			e.printStackTrace();
		}

		return outputYear;

	}
	
	private String figureOutWhichYearIsPlaying(MovieInfo listingInfo,
			String[] arrayOfIMDBentries, String movieInfo) {
		String outputUrl = arrayOfIMDBentries[0];  // if worse comes to worse, use 1st
		
		// when used with TV Guide, year should already be present in listingInfo

		// check to see if new need to go to uverse details to get the year
		if (listingInfo.getYearMade().length() == 0) {  // this won't work for uverse ap version
			
		   System.out.println("Year of movie not know for " + listingInfo.getOneMoviesJson());

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

	private boolean isInteger( String input ) {
	    try {
	        Integer.parseInt( input );
	        return true;
	    }
	    catch( Exception e ) {
	        return false;
	    }
	}


	public String getIMDBAlsoKnownAs() {
	    return alsoKnownAs;
	}

	public void setIMDBAlsoKnownAs(String alsoKnownAs) {
		this.alsoKnownAs = alsoKnownAs;
	}
    public String getIMDBTypeOfMovie() {
		return typeOfMovie;
	}
	public void setIMDBTypeOfMovie(String typeOfMovie) {
		this.typeOfMovie = typeOfMovie;
	}
	public String getIMDBDescription() {
		return description;
	}
	public void setIMDBDescription(String description) {
		if (description != null) {                  // null comes out of json parsing when an element isn't present
		  this.description = description;
		}  
	}
	public String getIMDBYear() {
		return year;
	}
	public void setIMDBYear(String year) {
		this.year = year;
	}
	public String getIMDBRating() {
		return rating;
	}
	public void setIMDBRating(String rating) {
		if (rating != null) {                       // null comes out of json parsing when an element isn't present
		  this.rating = rating;
		}
	}
	
    public List<String> getIMDBActorsList() {
		return actorsList;
	}

	public List<String> getIMDBDirectorsList() {
		return directorsList;
	}
		

}

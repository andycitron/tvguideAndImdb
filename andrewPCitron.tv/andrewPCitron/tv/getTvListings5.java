/**
 * 
 *  * @author Andrew P. Citron 
 * 
 * The purpose of this package is to answer the question:  what good movies are on TV this week on channels that I get?  
 * Optionally it can filter out movies that we've already seen.  If filtering is desired tvlistings.properties should indicate: omitMoviesWeveSeen = 1 and moviesWeveSeen.txt should be present and populated with the names of movies you've already seen
 * 
 * Good movies are defined by a rating over over 70 on tvguide.com or 7.0 on IMDB.com.  If a movie rating isn't found, then the movie is considered good.  The assumption is the movie is too new to be rated, or this code simply couldn't figure out the rating and its better to display information than to hide it.
 *
 * Things you must do to modify this code for your own use:
 * 1) update tvlistings.properties to match your tv provider and services you get
 * 2) 2) figure out you mapping from service provider to url to use with tvguide.com.  To do this, go to tvguide.com -> listings then 'change provider'.  Specify your zipCode and get the string that signifies your provider.  put zip code and provider in tvlistings.properties
 * 3) I can't figure out which channels you get for your provider and area.  I can only figure out my own channels.  
 *    In TvGuide.java change thisIsAChannelWeGet() to match the channels in your area.
 *    Best would be to somehow figure this out dynamically, but I don't work for tvguide.com or your tv service provider so I don't know how they're figuring that out.
 * 4) Limitation:  The program is dependent on html pages from tvguide.com and IMDB.com.  If that html changes, this code can break and would need to be updated.  
 *     A lot of the information comes from json on those websites.  That json is less likely to change in an incompatible manner.  
 * It would be nice if this was implemented by a service you log into so it could remember your preferences instead of storing the info files and code
 * 
 * @return The output of this program is a comma separated vector (.csv) file that can be understood by any spreadsheet or widget that understands .csv files.  The spreadsheet is opened if you have defined a program in tvlistings.Properies that can open .csv files
 *
 * External packages used:  
 * 1) json-simple-1.1.1.jar
 * 2) joda-time-2.9.3.jar
 * 3) sqlite-jdbc-3.21.0.jar (actually not currently used, but code is included that does use this)

 * 
 * Bundle-License: http://www.apache.org/licenses/LICENSE-2.0.txt
 */
package andrewPCitron.tv;


// import andrewPCitron.tv.Uverse;     // this was used to reverse engineer the uVerse iPad ap.  no longer used, and might not need to be imported but kept for educational purposes




public class getTvListings5 {	
	
	/**
	 * @param args aren't used
	 */
	public static void main(String[] args) {
		
		GetDataAndProduceSpreadSheet doItAll = new GetDataAndProduceSpreadSheet();
		doItAll.getAllInfoAndProduceCSV();		

	}
		

}

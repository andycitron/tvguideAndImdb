# tvguideAndImdb
This program uses tvguide.com and IMDB.com to answer the question "What good movies are on this week, on channels I get, that I have not already seen"

Optionally it can filter out movies that we've already seen.  If filtering is desired tvlistings.properties should indicate: omitMoviesWeveSeen = 1 and moviesWeveSeen.txt should be present and populated with the names of movies you've already seen
  
Good movies are defined by a rating over over 70 on tvguide.com or 7.0 on IMDB.com.  If a movie rating isn't found, then the movie is considered good.  The assumption is the movie is too new to be rated, or this code simply couldn't figure out the rating and its better to display information than to hide it.
 
Things you must do to modify this code for your own use:
1) update tvlistings.properties to match your tv provider and services you get
2) figure out service provider to use with tvguide.com.  To do this, go to tvguide.com -> listings then 'change provider'.  Specify your zipCode and get the string that signifies your providerName.  put zipcode and providerName in tvlistings.properties
Alternatively, you can fetch this url to see the json that includes your providerName:  (replace ZIPCODE with your zipCode)
https://mobilelistings.tvguide.com/Listingsweb/ws/rest/serviceproviders/ZIPCODE/zipCode?formattype=json

3) I can't figure out which channels you get for your provider and area.  I can only figure out my own channels.  
      In TvGuide.java change thisIsAChannelWeGet() to match the channels from your provider in your area.
      Best would be to somehow figure this out dynamically, but I don't work for tvguide.com or your tv service provider so I don't know how they're figuring that out. 
      
4) The program is dependent on html pages from tvguide.com and IMDB.com.  If that html changes, this code can break and would need to be updated.  
      A lot of the information comes from json on those websites.  That json is less likely to change in an incompatible manner.       
  
It would be nice if this was implemented by a service you log into so it could remember your preferences instead of storing the info in files and code.  For example this would be a useful function for tvguide.com to use on their site.
  
The output of this program is a comma separated vector (.csv) file that can be understood by any spreadsheet or widget that understands .csv files.  The spreadsheet is opened if you have defined a program in tvlistings.Properies that can open .csv files
 
External packages used:  
 1) json-simple-1.1.1.jar
 2) joda-time-2.9.3.jar
 3) sqlite-jdbc-3.21.0.jar (actually not currently used, but code is included that does use this)
  
 Bundle-License: http://www.apache.org/licenses/LICENSE-2.0.txt

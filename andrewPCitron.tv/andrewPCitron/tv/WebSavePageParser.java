package andrewPCitron.tv;


/**
 * @author Andrew Citron.  
 * 
 * The intent of this code is to parse a set of values out of a page based on the input 'right boundary' and 'left boundary'
 * Sorta like what web_reg_save_param does in load runner.  It uses regular expressions to produce an array of matching items
 * 
 * input is right delimiter, left delimiter, and optional string to exclude 
 * 
 * @return an array of strings that are enclosed in the specified delimiters.  If none are found, the returned array is null
 *
 */

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebSavePageParser {
	
	// constructor
	public WebSavePageParser() {    
    } 
	
	
	// ParsePage is a method that mimics LoadRunner's web reg find with the 'all' option.
	// input:  an html page (or any other string)
	//         a left delimiter.  it can include regular expression syntax (which is better than load runner
	//         a right delimiter.   can also include regular expression syntax
	
	public String[] parsePage(String HTML, String leftBracket, String rightBracket, String exclude) {
		Vector<Object>  v = new Vector<Object>();        // store results in this ordered list
		String matchString;
		int matchCounter = 0;		                     // used for debugging

		if (exclude == null) {
			matchString = ".*?";                         // grabs everything
		}  else {
			matchString = "[^" + exclude + "]*?";        // grabs everything but the exclude pattern
		}

		String matchPattern = leftBracket + "(" + matchString + ")" + rightBracket; 

		// for debug System.out.println("parsePage pattern:" +  matchPattern);

		Pattern itemFind = Pattern.compile(matchPattern, Pattern.DOTALL);

		Matcher itemMatch = itemFind.matcher(HTML);


		while (itemMatch.find()) {			             // save elements in a vector.  This preserves the order in which they were found
			//System.out.println(itemMatch.group(1));
			// debug System.out.println("\nElement " + ++matchCounter + " Found****:  " + itemMatch.group(1));
			v.addElement((Object)itemMatch.group(1));
		}

		String[] s = new String[v.size()];               // now turn them into an array of strings for external consumption
		for(int i=0, cnt=s.length; i<cnt; i++) {
			s[i] = (String) v.elementAt(i);
		}

		if (s.length == 0) {
			// for debug System.out.println("Parser didn't find hits for LB=: " + leftBracket + " and RB=" + rightBracket );
		}
		// debug System.out.println("Total matches found is " + matchCounter);
		return s;                                       // return array of strings to caller

	}
	
	/**
	 * Maybe have standard input be a file, the HTML file for testing purposes.
	 * @param argv
	 */
	
		
	public String[] parsePageCaseInsensitive(String HTML, String leftBracket, String rightBracket, String exclude ) {
		Vector<Object> v = new Vector<Object>();        // store results in this ordered list
		
		String matchString;
		
		if (exclude == null) {
		    matchString = ".*?";                        // grabs everything
		}  else {
			matchString = "[^" + exclude + "]*?";       // grabs everything but the exclude pattern
		}
		
		String patternString = leftBracket+ "(" + matchString + ")" +rightBracket;
		
		//  system.out.println("parsePage() pattern: " + patternString);
			  
		Pattern itemFind = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);
		
		Matcher itemMatch = itemFind.matcher(HTML);
		
		while (itemMatch.find()) {			            // save elements in a vector.  This preserves the order in which they were found
			//System.out.println(itemMatch.group(1));

			v.addElement((Object)itemMatch.group(1));
		}
		
		String[] s = new String[v.size()];              // now turn them into an array of strings for external consumption
		for(int i=0, cnt=s.length; i<cnt; i++) {
			s[i] = (String) v.elementAt(i);
		}
		
		//if ( s.length == 0)  {
		//	system.out.println("parsePage didn't find hits for pattern string " + patternString );
		//}
		
		return s;                                       // return array of strings to caller
		

	}

	public static void main(String argv[]) {
		// Simple framework which reads file in argv[0]
                
		int b;
		try {
			FileInputStream fileIn = new FileInputStream(argv[0]);
			ByteArrayOutputStream out = new ByteArrayOutputStream(); 
			b = fileIn.read();
			while (b != -1) {
				out.write(b);
				b = fileIn.read();
			}
			
			String html = out.toString();
			WebSavePageParser parser = new WebSavePageParser();

			// ReportMessage.getReporter(tes) for RPT
			// ReportMessage.getDevNull() for nothing
			// ReportMessage.getSystemOutReporter() for printf
			//parser.parsePage(html, "href=\"", "#[/s/w]\">Headline for story", ReportMessage.getSystemOutReporter() );
			//parser.parsePage(html, "<a href=\"", "#[^>]*>info", "#" );
                        parser.parsePage(html, "\"asynchDoFormSubmit('"  , "');\",\""  ,"#"  );
                        
            fileIn.close();            
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}


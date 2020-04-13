/**
 * 
 *  @author Andrew P. Citron
 *  various methods to fetch pages using http protocols
 */ 

package andrewPCitron.tv;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class UrlAccess {

	public String readUrl(String requestUrl, int retryCount) {
		StringBuffer fullData = new StringBuffer(512);
		try {
			URL url = new URL(requestUrl.toString());

			HttpURLConnection hc = (HttpURLConnection)url.openConnection();
			// seems if user agent isn't set, some servers treat device as a smart phone
			hc.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64; Trident/7.0; rv:11.0) like Gecko");
			
			hc.setRequestProperty("X-Requested-With", "XMLHttpRequest");
			hc.setRequestProperty("Accept", "text/html, application/xhtml+xml, image/jxr, */*");
			// added May 2016
			// content is zipped if I do this:  hc.setRequestProperty("Accept-Encoding", "gzip, deflate");
			hc.setRequestProperty("Accept-Language", "en-US, en; q=0.5");
			
			hc.connect();   
			int code = hc.getResponseCode();  
			if (code != 200) {
				// System.out.println("Http response code is " + code + " for " + requestUrl.toString());
				if (retryCount > 0 && (code == 503)) {  // I've seen 503's work if retried
					// didn't help hc.disconnect();                    // apc trying to avoid oom, not sure if this does it ****
					Thread.sleep(1000);
					return readUrl(requestUrl, retryCount-1);  
				}
			}  else {                         // don't try to read input if return code isn't 200 
				
				BufferedReader in = new BufferedReader(new InputStreamReader(
						hc.getInputStream()));
				String inputLine;
				//System.out.println("-----Read URL RESPONSE START-----" + requestUrl.toString());
				boolean abort = false;
				int length = 0;                   // to help report large pages being read
				while ((abort == false) && (inputLine = in.readLine()) != null) {
					fullData.append(inputLine);
					//System.out.println(inputLine);
			
					if (fullData.length() > 1000000) {
					//	abort = true;  **** let's not abort for now, tvguide json is about 1.5MB
						length = fullData.length(); 
						//System.out.println("Reading " + fullData.length() + " " + requestUrl);
					}
				}
				if (length > 2000000) {           // sanity check to flag really large gets                    
					System.out.println("Reading " + fullData.length() + " bytes from " + requestUrl);
				}
				in.close();
			}
			//System.out.println("-----RESPONSE END-----");

		} catch (IOException e) {
			//  for debug	
			System.out.println("read url threw an exception for " + requestUrl);
			e.printStackTrace();
		} catch (InterruptedException e) {
			System.out.println("Sleep threw an exception.  Shouldn't happen.");
			e.printStackTrace();
		}
		return (fullData.toString());

	}

	public String readUrlWithCookie(String requestUrl, String cookie) {
		StringBuffer fullData = new StringBuffer(512);
		try {
			URL url = new URL(requestUrl.toString());

			// try to add user agent 10/2012.  seems that if its not sent, date of movie site treats it as a smart phone
			HttpURLConnection hc = (HttpURLConnection) url.openConnection();
			hc.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.2; en-US; rv:1.9.2.8) Gecko/20100722 Firefox/3.6.8 ( .NET CLR 3.5.30729)");
			hc.setRequestProperty("Cookie", cookie);
			hc.connect();
			int code = hc.getResponseCode();
			// debug
			if (code != 200) {
				// debug System.out.println("readUrlWithCookie response code = " + code + "for url " + requestUrl);
			}

			BufferedReader in = new BufferedReader(new InputStreamReader(
					hc.getInputStream()));

			String inputLine;
			//System.out.println("-----RESPONSE START-----");
			while ((inputLine = in.readLine()) != null) {
				fullData.append(inputLine);
				//System.out.println(inputLine);
			}
			in.close();
			//System.out.println("-----RESPONSE END-----");

		} catch (IOException e) {
			e.printStackTrace();
		}
		return (fullData.toString());

	}

	public  String readUrlAsAjax(String requestUrl, int retryCount) {
		StringBuffer fullData = new StringBuffer(512);
		try {
			URL url = new URL(requestUrl.toString());

			// try to add user agent 10/2012.  seems that if its not sent, date of movie site treats it as a smart phone
			HttpURLConnection hc = (HttpURLConnection) url.openConnection();
			hc.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.2; en-US; rv:1.9.2.8) Gecko/20100722 Firefox/3.6.8 ( .NET CLR 3.5.30729)");
			hc.setRequestProperty("X-Requested-With", "XMLHttpRequest");
			//hc.setRequestProperty("Accept", "text/javascript, text/html, application/xml, text/xml, */*");
			hc.setRequestProperty("Accept", "application/json");
			
			hc.connect();
			int code = hc.getResponseCode();
			// debug
			if (code != 200) {
				System.out.println("readUrlAsAjax response code = " + code + " for url " + requestUrl);
			}   
			if (retryCount > 0 && ((code >= 500) && (code<=600)  )) {  // I've seen 503's work if retried
				// didn't help hc.disconnect();
				Thread.sleep(1000);
				return readUrlAsAjax(requestUrl, retryCount-1);  
			}  else {                         // don't try to read input if return code isn't 200 

				BufferedReader in = new BufferedReader(new InputStreamReader(
						hc.getInputStream()));

				String inputLine;
				//System.out.println("-----RESPONSE START-----");
				while ((inputLine = in.readLine()) != null) {
					fullData.append(inputLine);
					//System.out.println(inputLine);
				}
				in.close();
				//System.out.println("-----RESPONSE END-----");
			} 

		} catch (IOException e) {
			e.printStackTrace();
		}
		catch (InterruptedException e) {
			System.out.println("Sleep threw an exception.  Shouldn't happen.");
			e.printStackTrace();
		}
		return (fullData.toString());

	}


	public String readUrlWithHeaders(String requestUrl) {
		StringBuffer fullData = new StringBuffer(512);
		try {
			URL url = new URL(requestUrl.toString());

			// try to add user agent 10/2012.  seems that if its not sent, date of movie site treats it as a smart phone
			HttpURLConnection hc = (HttpURLConnection) url.openConnection();
			hc.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.2; en-US; rv:1.9.2.8) Gecko/20100722 Firefox/3.6.8 ( .NET CLR 3.5.30729)");
			hc.setRequestProperty("Accept", "*/*");
			hc.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
			hc.setRequestProperty("Accept-Encoding", "gzip, deflate");
			hc.setRequestProperty("Host", "sg.media-imdb.com");
			hc.setRequestProperty("Referer", "http://www.imdb.com/find?q=The+Perfect+Child&s=all");
			hc.connect();
			int code = hc.getResponseCode();

			// debug
			if (code != 200) {
				// System.out.println("readUrlWithHeaders response code = " + code + " for url " + requestUrl);
			}

			BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));

			String inputLine;
			//System.out.println("-----RESPONSE START-----");
			while ((inputLine = in.readLine()) != null) {
				fullData.append(inputLine);
				//System.out.println(inputLine);
			}
			in.close();
			//System.out.println("-----RESPONSE END-----");

		} catch (IOException e) {
			// e.printStackTrace();   too much output produced with uverse
		}
		return (fullData.toString());

	}

}

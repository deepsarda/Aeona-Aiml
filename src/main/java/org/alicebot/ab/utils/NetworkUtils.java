package org.alicebot.ab.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Enumeration;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class NetworkUtils {
	private static final Logger log = LoggerFactory
			.getLogger(NetworkUtils.class);

    public static String localIPAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        String ipAddress =  inetAddress.getHostAddress().toString();
                        int p = ipAddress.indexOf("%");
                        if (p > 0) ipAddress = ipAddress.substring(0, p);
                        log.info("--> localIPAddress = {}", ipAddress);
                        return ipAddress;
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return "127.0.0.1";
    }


	public static String responseContent(String url) throws Exception {
		HttpClient client = new DefaultHttpClient();
		HttpGet request = new HttpGet();
		request.setURI(new URI(url));
		InputStream is = client.execute(request).getEntity().getContent();
		BufferedReader inb = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder("");
		String line;
		String NL = System.getProperty("line.separator");
		while ((line = inb.readLine()) != null) {
			sb.append(line).append(NL);
		}
		inb.close();
		return sb.toString();
	}


	public static String spec(String host, String botid, String custid, String input) {
		log.trace("--> custid = {}", custid);
		String spec = "";
		if (custid.equals("0"))      // get custid on first transaction with Pandorabots
			spec =    String.format("%s?botid=%s&input=%s",
									"http://" + host + "/pandora/talk-xml",
									botid,
									URLEncoder.encode(input));
		else spec =                 // re-use custid on each subsequent interaction
				 String.format("%s?botid=%s&custid=%s&input=%s",
							   "http://" + host + "/pandora/talk-xml",
							   botid,
							   custid,
							   URLEncoder.encode(input));
		return spec;
	}


}

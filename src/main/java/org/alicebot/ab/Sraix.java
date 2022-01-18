package org.alicebot.ab;
/* Program AB Reference AIML 2.0 implementation
        Copyright (C) 2013 ALICE A.I. Foundation
        Contact: info@alicebot.org

        This library is free software; you can redistribute it and/or
        modify it under the terms of the GNU Library General Public
        License as published by the Free Software Foundation; either
        version 2 of the License, or (at your option) any later version.

        This library is distributed in the hope that it will be useful,
        but WITHOUT ANY WARRANTY; without even the implied warranty of
        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
        Library General Public License for more details.

        You should have received a copy of the GNU Library General Public
        License along with this library; if not, write to the
        Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
        Boston, MA  02110-1301, USA.
*/
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alicebot.ab.utils.CalendarUtils;
import org.alicebot.ab.utils.NetworkUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Sraix {

	private static final Logger log = LoggerFactory.getLogger(Sraix.class);
    public static HashMap<String, String> custIdMap = new HashMap<String, String>();

    private static String custid = "0"; // customer ID number for Pandorabots

    public static String sraix(Chat chatSession, String input, String defaultResponse, String hint, String host, String botid, String apiKey, String limit) {
        String response;
        if (host != null && botid != null) {
           response = sraixPandorabots(input, chatSession, host, botid);
        }
        else response = sraixPannous(input, hint, chatSession);
        if (response.equals(MagicStrings.sraix_failed)) {
          if (chatSession != null && defaultResponse == null) response = AIMLProcessor.respond(MagicStrings.sraix_failed, "nothing", "nothing", chatSession);
          else if (defaultResponse != null) response = defaultResponse;
        }
        return response;
    }
    public static String sraixPandorabots(String input, Chat chatSession, String host, String botid) {
        //log.info("Entering SRAIX with input="+input+" host ="+host+" botid="+botid);
        String responseContent = pandorabotsRequest(input, host, botid);
        if (responseContent == null) return MagicStrings.sraix_failed;
        else return pandorabotsResponse(responseContent, chatSession, host, botid);
    }
    public static String pandorabotsRequest(String input, String host, String botid) {
        try {
            custid = "0";
            String key = host+":"+botid;
            if (custIdMap.containsKey(key)) custid = custIdMap.get(key);
            //log.info("--> custid = "+custid);
            String spec = NetworkUtils.spec(host, botid, custid, input);
            //log.info("Spec = "+spec);
			String responseContent = NetworkUtils.responseContent(spec);
            return responseContent;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
    public static String pandorabotsResponse (String sraixResponse, Chat chatSession, String host, String botid) {
        int n1 = sraixResponse.indexOf("<that>");
        int n2 = sraixResponse.indexOf("</that>");
        String botResponse = MagicStrings.sraix_failed;
        if (n2 > n1)
            botResponse = sraixResponse.substring(n1+"<that>".length(), n2);
        n1 = sraixResponse.indexOf("custid=");
        if (n1 > 0) {
            custid = sraixResponse.substring(n1+"custid=\"".length(), sraixResponse.length());
            n2 = custid.indexOf("\"");
            if (n2 > 0) custid = custid.substring(0, n2);
            else custid = "0";
            String key = host+":"+botid;
            //log.info("--> Map "+key+" --> "+custid);
            custIdMap.put(key, custid);
        }
        if (botResponse.endsWith(".")) botResponse = botResponse.substring(0, botResponse.length()-1);   // snnoying Pandorabots extra "."
        return botResponse;
    }

    public static String sraixPannous(String input, String hint, Chat chatSession)  {
        try {
            if (hint == null) hint = MagicStrings.sraix_no_hint;
            input = " "+input+" ";
            input = input.replace(" point ", ".");
            input = input.replace(" rparen ", ")");
            input = input.replace(" lparen ","(");
            input = input.replace(" slash ","/");
            input = input.replace(" star ","*");
            input = input.replace(" dash ","-");
            input = input.trim();
            input = input.replace(" ","+");
            int offset = CalendarUtils.timeZoneOffset();
            //log.info("OFFSET = "+offset);
            String locationString = "";
            if (chatSession.locationKnown) {
                locationString = "&location="+chatSession.latitude+","+chatSession.longitude;
            }
            // https://weannie.pannous.com/api?input=when+is+daylight+savings+time+in+the+us&locale=en_US&login=pandorabots&ip=169.254.178.212&botid=0&key=CKNgaaVLvNcLhDupiJ1R8vtPzHzWc8mhIQDFSYWj&exclude=Dialogues,ChatBot&out=json
            String url = "https://weannie.pannous.com/api?input="+input+"&locale=en_US&timeZone="+offset+locationString+"&login="+MagicStrings.pannous_login+"&ip="+NetworkUtils.localIPAddress()+"&botid=0&key="+MagicStrings.pannous_api_key+"&exclude=Dialogues,ChatBot&out=json";
            log.debug("Sraix url='"+url+"'");
            String page = NetworkUtils.responseContent(url);
            log.debug( "Sraix: "+page);
            String text="";
            String imgRef="";
            if (page == null || page.length() == 0) {
                text = MagicStrings.sraix_failed;
            }
            else {
                JSONArray outputJson = new JSONObject(page).getJSONArray("output");
                if (outputJson.length() == 0) {
                    text = MagicStrings.sraix_failed;
                }
                else {
                    JSONObject firstHandler = outputJson.getJSONObject(0);
                    JSONObject actions = firstHandler.getJSONObject("actions");
                    if (actions.has("reminder")) {
                        Object obj = actions.get("reminder");
                        if (obj instanceof JSONObject) {
                            JSONObject sObj = (JSONObject) obj;
                            String date = sObj.getString("date");
                            date = date.substring(0, "2012-10-24T14:32".length());
                            //log.info("date="+date);
                            String duration = sObj.getString("duration");
                            //log.info("duration="+duration);

                            Pattern datePattern = Pattern.compile("(.*)-(.*)-(.*)T(.*):(.*)");
                            Matcher m = datePattern.matcher(date);
                            String year="", month="", day="", hour="", minute="";
                            if (m.matches())  {
                                year = m.group(1);
                                month = String.valueOf(Integer.parseInt(m.group(2))-1);
                                day = m.group(3);

                                hour = m.group(4);
                                minute = m.group(5);
                                text =  "<year>"+year+"</year>" +
                                        "<month>"+month+"</month>" +
                                        "<day>"+day+"</day>" +
                                        "<hour>"+hour+"</hour>" +
                                        "<minute>"+minute+"</minute>" +
                                        "<duration>"+duration+"</duration>";

                            }
                            else text = MagicStrings.schedule_error;
                        }
                    }
                    else if (actions.has("say")  && !hint.equals(MagicStrings.sraix_pic_hint)) {
                        Object obj = actions.get("say");
                        if (obj instanceof JSONObject) {
                            JSONObject sObj = (JSONObject) obj;
                            text = sObj.getString("text");
                            if (sObj.has("moreText")) {
                            JSONArray arr = sObj.getJSONArray("moreText");
                            for (int i = 0; i < arr.length(); i++) {
                                text += " " + arr.getString(i);
                            }
                            }
                        } else {
                            text = obj.toString();
                        }
                    }
                    if (actions.has("show") && !text.contains("Wolfram")
                            && actions.getJSONObject("show").has("images")) {
                        JSONArray arr = actions.getJSONObject("show").getJSONArray(
                                "images");
                        int i = (int)(arr.length() * Math.random());
                        //for (int j = 0; j < arr.length(); j++) log.info(arr.getString(j));
                        imgRef = arr.getString(i);
                        if (imgRef.startsWith("//")) imgRef = "http:"+imgRef;
                        imgRef = "<a href=\""+imgRef+"\"><img src=\""+imgRef+"\"/></a>";
                        //log.info("IMAGE REF="+imgRef);

                    }
                }
                if (hint.equals(MagicStrings.sraix_event_hint) && !text.startsWith("<year>")) return MagicStrings.sraix_failed;
                else if (text.equals(MagicStrings.sraix_failed)) return AIMLProcessor.respond(MagicStrings.sraix_failed, "nothing", "nothing", chatSession);
                else {
                    text = text.replace("&#39;","'");
                    text = text.replace("&apos;","'");
                    text = text.replaceAll("\\[(.*)\\]", "");
                    String[] sentences;
                    sentences = text.split("\\. ");
                    //log.info("Sraix: text has "+sentences.length+" sentences:");
                    String clippedPage = sentences[0];
                    for (int i = 1; i < sentences.length; i++) {
                        if (clippedPage.length() < 500) clippedPage = clippedPage + ". "+sentences[i];
                        //log.info(i+". "+sentences[i]);
                    }

                    clippedPage = clippedPage + " " + imgRef;
                    return clippedPage;
                }
            }
        } catch (Exception ex)   {
            ex.printStackTrace();
            log.info("Sraix '" + input + "' failed");
        }
        return MagicStrings.sraix_failed;
    } // sraixPannous
}


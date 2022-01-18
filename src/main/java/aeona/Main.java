package aeona;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.*;  
import org.alicebot.ab.AIMLProcessor;
import org.alicebot.ab.Bot;
import org.alicebot.ab.Chat;
import org.alicebot.ab.MagicBooleans;
import org.alicebot.ab.MagicStrings;
import org.alicebot.ab.PCAIMLProcessorExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
	private static final Logger log = LoggerFactory.getLogger(Main.class);
    public static Bot bot ; //
    public  static File file = new File("save");
    public static int totalServers=1;
    public static int serverNumber=0;
    public static boolean mainServer=true;
    public static String[] otherServers;
    public static void main (String[] args)  throws IOException, ClassNotFoundException{
        Scanner sc= new Scanner(System.in);
        System.out.println("Enter total number of servers!");
        totalServers=sc.nextInt();
        otherServers=new String[totalServers];
        System.out.println("Enter number of current server (starts from 0!)");
        serverNumber=sc.nextInt();

        if(serverNumber!=0){
            mainServer=false;
        }
        System.out.println("Auto fill other servers? \n 0:Yes, 1:No \n works by adding -1,-2,-3 etc to a url which will be asked!");
        int autoFill=sc.nextInt();
        if(autoFill==0){
            System.out.println("Enter main server url!");
            String baseUrl=sc.nextLine();
            for(int i=0;i<totalServers;i++){
                if(i!=serverNumber){
                    if(i==0){
                        otherServers[i]=baseUrl;
                    }else{
                        otherServers[i]=baseUrl+"-"+i;
                    }
                }
            }
        }else{

        }
        MagicStrings.root_path = System.getProperty("user.dir");
        log.info("Working Directory = " + MagicStrings.root_path);
        AIMLProcessor.extension =  new PCAIMLProcessorExtension();
        bot= new Bot("Aeona", MagicStrings.root_path, "chat");
       
        MagicBooleans.trace_mode = true;
        if (!file.exists()) {
            file.mkdir();
        }
        
        HttpServer server = HttpServer.create(new InetSocketAddress(5000), 0);
        server.createContext("/test", new Handler());
        server.createContext("/alive", new Alive());
        server.setExecutor(null);
        server.start();
    }

    static class Handler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            Map<String, String> query = Handler.getQueryMap(t.getRequestURI().toString().replaceAll("%20", " ").trim());
            Gson gson = new Gson();
            System.out.println(query.keySet().toArray().toString());
            Response response =null;
            if(Main.mainServer){ 
                File file = new File("save/" + query.get("id").trim() + ".json");
                
                if (file.exists()) {
                    
                    JsonReader reader = new JsonReader(new FileReader(file));
                    Chat chat = (Chat)gson.fromJson(reader, Chat.class);
                    chat.setBot(Main.bot);
                    response= new Response(chat,chat.multisentenceRespond(query.get("text")));
                    FileWriter fileWriter = new FileWriter(file);
                    gson.toJson(chat,fileWriter);
                    fileWriter.flush();
                    fileWriter.close();
                }else{
                    Chat chat= new Chat(Main.bot, query.get("id").trim());
                    response=new Response(chat,chat.multisentenceRespond(query.get("text")));
                    FileWriter fileWriter = new FileWriter(file);
                    gson.toJson(chat,fileWriter);
                    fileWriter.flush();
                    fileWriter.close();
                }
            }else{
                InputStreamReader isReader = new InputStreamReader(t.getRequestBody());
                Response request=(Response)gson.fromJson(isReader, Response.class);
                response=new Response(request.chat,request.chat.multisentenceRespond(query.get("text")));
            }
            
            System.out.println("closing");
            t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            t.getResponseHeaders().add("Content-Type", "text/html");
            String responsString=gson.toJson(response);
            t.sendResponseHeaders(200, responsString.length());
            OutputStream os = t.getResponseBody();
            os.write(responsString.getBytes());
            os.flush();
            os.close();
            t.close();
        }
    
        public static Map<String, String> getQueryMap(String query) {
          String[] params = query.split("&");
          Map<String, String> map = new HashMap<String, String>();
    
          for (String param : params) {
            String name = param.split("=")[0];
            String value = param.split("=")[1];
            map.put(name, value.replaceAll("%20", " "));
          }
          return map;
        }
    }
    static class Alive implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            if(Main.mainServer){
                for(String server :otherServers){
                    URL url = new URL(server+"/alive");
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("GET");
                    con.connect();
                }
            }
            t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            t.getResponseHeaders().add("Content-Type", "text/html");
            String responsString="I am alive!";
            t.sendResponseHeaders(200, responsString.length());
            OutputStream os = t.getResponseBody();
            os.write(responsString.getBytes());
            os.flush();
            os.close();
            t.close();
        }

    }
}

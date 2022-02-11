package aeona;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

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
    public static void main (String[] args)  throws IOException, ClassNotFoundException{
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
        server.setExecutor(null);
        server.start();
    }

    static class Handler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            Map<String, String> query = Handler.getQueryMap(t.getRequestURI().toString());
            Gson gson = new Gson();
            System.out.println(query.keySet().toArray().toString());
            File file = new File("save/" + query.get("id").trim() + ".json");
            String response ="";
            if (file.exists()) {
                
                JsonReader reader = new JsonReader(new FileReader(file));
                Chat chat = (Chat)gson.fromJson(reader, Chat.class);
                chat.setBot(Main.bot);
                response=chat.multisentenceRespond(query.get("text"));
                FileWriter fileWriter = new FileWriter(file);
                gson.toJson(chat,fileWriter);
                fileWriter.flush();
                fileWriter.close();
            }else{
                Chat chat= new Chat(Main.bot, query.get("id").trim());
                response=chat.multisentenceRespond(query.get("text"));
                FileWriter fileWriter = new FileWriter(file);
                gson.toJson(chat,fileWriter);
                fileWriter.flush();
                fileWriter.close();
            }
           
            
            System.out.println("closing");
            t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            t.getResponseHeaders().add("Content-Type", "text/html");
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.flush();
            os.close();
            t.close();
        }
    
        public static Map<String, String> getQueryMap(String query) {
         
            try {
                query= URLDecoder.decode(query, StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
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
}

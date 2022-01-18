package aeona;

import org.alicebot.ab.Chat;

public class Response {
    public Chat chat;
    public String response;
    public Response(Chat chat,String response){
        this.chat=chat;
        this.response=response;
    }
}

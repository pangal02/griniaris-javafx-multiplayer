package gr.uop.server;

public class PlayerSession {
    public final String name;
    public String color = "NONE";
    public boolean ready = false;

    public PlayerSession(String name){ this.name = name; }

    public String token(){ // name:color:ready
        return name + ":" + color + ":" + (ready ? "1" : "0");
    }

}

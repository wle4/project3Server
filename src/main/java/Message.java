import java.io.Serializable;

public class Message implements Serializable {
    static final long serialVersionUID = 42L;

    private final String receiver; // who the message is being sent to
    private final String sender; // who the message came from
    private final String msg; // the content in the message

    public Message(String receiver, String sender, String msg){
        this.receiver = receiver;
        this.sender = sender;
        this.msg = msg;
    }

//    //specifically used when message is sent
//    public Message(String sender, String msg){
//        this.receiver = "";
//        this.sender = sender;
//        this.msg = msg;
//    }

    public String getReceiver(){
        // returns the username of the person that the message is being sent to
        return receiver;
    }

    public String getSender(){
        // returns the username of the person who sent the message
        return sender;
    }

    public String getMsg(){
        // returns the content in the message
        return msg;
    }
}

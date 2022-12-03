import java.io.Serializable;
import java.util.Comparator;

public class User implements Serializable {
    String ip;
    String port;

    String userKey;
    User(String ip, String port, String publicKey){
        this.ip=ip;
        this.port=port;
        this.userKey =publicKey;
    }


    class UserComparator implements Comparator<User> {

        public int compare(User u1, User u2)
        {
            return u1.ip.compareTo(u2.ip);
        }
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }
}

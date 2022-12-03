import java.io.Serializable;
import java.security.*;
import java.util.*;

public class User implements Serializable {
    private static final long serialVersionUID = -7748296967295940465L;

    public String name;
    public String ip;
    public String port;
    public PublicKey publicKey;
    private String eKey;
    private List<String> groups;

    User(String name, String ip, String port, PublicKey publicKey, String eKey) {
        this.name = name;
        this.ip = ip;
        this.port = port;
        this.publicKey = publicKey;
        this.eKey = eKey;
        this.groups = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public PublicKey getPKey() {
        return publicKey;
    }

    public void setPKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public String getEKey() {
        return eKey;
    }

    public void setEKey(String eKey) {
        this.eKey = eKey;
    }

    public List<String> getGroups() {
        return groups;
    }

    public void addGroup(String group) {
        this.groups.add(group);
    }

    public void removeGroup(String group) {
        this.groups.remove(group);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null)
            return false;
        if (getClass() != o.getClass())
            return false;
        User u = (User) o;
        return this.name.equals(u.name);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + (this.name != null ? this.name.hashCode() : 0);
        return hash;
    }
}

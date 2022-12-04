import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class P2PFile implements Serializable {
    private String name;
    private String owner;
    private Set<String> groups;
    private List<User> locations;

    P2PFile(String name, String owner, List<User> locations) {
        this.name = name;
        this.owner = owner;
        this.groups = new HashSet<>();
        this.locations = locations;
    }

    public Set<String> getGroups() {
        return groups;
    }

    public List<User> getLocations() {
        return locations;
    }

    public String getName() {
        return name;
    }

    public String getOwner() {
        return owner;
    }

    public void setGroups(Set<String> groups) {
        this.groups = groups;
    }

    public void addGroup(String group) {
        System.out.println(this.groups);
        this.groups.add(group);
    }

    public void removeGroup(String group) {
        System.out.println(this.groups);
        this.groups.remove(group);
    }

    public void setLocations(List<User> locations) {
        this.locations = locations;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }
}

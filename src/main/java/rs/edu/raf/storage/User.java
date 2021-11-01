package rs.edu.raf.storage;

public class User {

    private String username;
    private String password;
    // TODO: Privilegije!
    // TODO: Apstraktna klasa User, koju nasledjuju Superuser i RegularUser?
    // TODO: mozda da User ima polje gde pisemo kom Skladistu pripada?
    // TODO: polje tipa boolean isSuperuser

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}

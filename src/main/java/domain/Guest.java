package domain;

public class Guest {
    private Long id;
    private String fullName;
    private String phone;

    public Guest() {}

    public Guest(Long id, String fullName, String phone){
        this.id = id;
        this.fullName = fullName;
        this.phone = phone;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}

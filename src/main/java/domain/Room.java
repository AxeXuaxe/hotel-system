package domain;

public class Room {
    private Long id;
    private String number;
    private int capacity;
    private boolean occupied;

    public Room() {}

    public Room(Long id, String number, int capacity) {
        this.id = id;
        this.number = number;
        this.capacity = capacity;
        this.occupied = false;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public boolean isOccupied() { return occupied; }
    public void setOccupied(boolean occupied) { this.occupied = occupied; }
}

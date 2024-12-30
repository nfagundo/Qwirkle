public class Piece {
    public String[] name;
    public int xCoord;
    public int yCoord;
    public Piece(String name, int xCoord, int yCoord) {
        this.name = name.split(" ");
        this.xCoord = xCoord;
        this.yCoord = yCoord;
    }

    public Piece(String name) {
        this(name, 0, 0);
    }

    public boolean check(Piece other) {
        return (!this.name[0].equals(other.name[0]) && this.name[1].equals(other.name[1]));
    }
}
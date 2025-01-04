import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Piece {
    public String[] name;
    public int xCoord;
    public int yCoord;

    @JsonCreator
    public Piece(
        @JsonProperty("name") String name,
        @JsonProperty("x") int x,
        @JsonProperty("y") int y
    ) {
        this.name = name.split(" ");
        this.xCoord = x;
        this.yCoord = y;
    }

    public Piece(String name) {
        this(name, 0, 0);
    }

    public Piece(String[] name) {
        this.name = name;
        this.xCoord = 0;
        this.yCoord = 0;
    }

    public boolean check(Piece other) {
        return (!this.name[0].equals(other.name[0]) && this.name[1].equals(other.name[1]));
    }
    
}
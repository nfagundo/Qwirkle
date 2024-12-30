import java.util.*;
public class Player {
    private List<Piece> hand;
    private int score;

    public Player(List<Piece> pieces) {
        this.hand = pieces;
        this.score = 0;
    }

    public List<Piece> getHand() {
        return new ArrayList<>(hand);
    }

    public void place(Piece piece) {
        hand.remove(piece);
    }
    
    public void setScore(int score) {
        this.score = score;
    }

    public int getScore() {
        return score;
    }
}
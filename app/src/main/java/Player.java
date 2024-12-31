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
    
    public void addScore(int score) {
        this.score += score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getScore() {
        return score;
    }

    public void addPieces(List<Piece> Piece) {
        for(int i = 0; i < Piece.size(); i++) {
            hand.add(Piece.get(i));
        }
    }

    public void removePieces(List<Piece> Piece) {
        for(int i = 0; i < Piece.size(); i++) {
            hand.remove(Piece.get(i));
        }
    }

    public void addPiece(Piece Piece) {
        hand.add(Piece);
    }

    public void removePiece(Piece Piece) {
        hand.remove(Piece);
    }
}
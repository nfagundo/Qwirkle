import java.util.*;
public class Player {
    private List<Piece> hand;
    private int score;

    public Player(List<Piece> pieces) {
        this.hand = new ArrayList<>(pieces);
        this.score = 0;
    }

    public List<Piece> getHand() {
        return new ArrayList<>(hand);
    }

    public void place(Piece piece) {
        hand.remove(piece);
    }

    public void setHand(List<Piece> pieces) {
        this.hand = new ArrayList<>(pieces);
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

    public void addPieces(List<Piece> Pieces) {
        hand.addAll(Pieces);
    }

    public void removePieces(List<Piece> Pieces) {
        for(int i = 0; i < Pieces.size(); i++) {
            for(int j = 0; j < hand.size(); j++) {
                if(hand.get(j).name[0].equalsIgnoreCase(Pieces.get(i).name[0]) &&
                    hand.get(j).name[1].equalsIgnoreCase(Pieces.get(i).name[1])) {
                    hand.remove(j);
                    break;
                }
            }
        }
    }

    public void addPiece(Piece Piece) {
        hand.add(Piece);
    }

    public void removePiece(Piece Piece) {
        for(int i = 0; i < hand.size(); i++) {
            if(hand.get(i).name[0].equalsIgnoreCase(Piece.name[0]) && 
                hand.get(i).name[1].equalsIgnoreCase(Piece.name[1])) {
                hand.remove(i);
                break;
            }
        }
    }

    public boolean hasPiece(Piece Piece) {
        for(int i = 0; i < hand.size(); i++) {
            if(hand.get(i).name[0].equalsIgnoreCase(Piece.name[0]) &&
                hand.get(i).name[1].equalsIgnoreCase(Piece.name[1])) {
                return true;
            }
        }
        return false;
    }
}
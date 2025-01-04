import java.util.*;
import java.util.stream.Collectors;
public class Bag {
    private Map<Piece, Integer> pieces;
    
    public Bag() {
        pieces = new HashMap<>();
        pieces.put(new Piece("Red Clover"), 3);
        pieces.put(new Piece("Red Square"), 3);
        pieces.put(new Piece("Red Diamond"), 3);
        pieces.put(new Piece("Red 8pt-Star"), 3);
        pieces.put(new Piece("Red 4pt-Star"), 3);
        pieces.put(new Piece("Red Circle"), 3);
        pieces.put(new Piece("Orange Clover"), 3);
        pieces.put(new Piece("Orange Square"), 3);
        pieces.put(new Piece("Orange Diamond"), 3);
        pieces.put(new Piece("Orange 8pt-Star"), 3);
        pieces.put(new Piece("Orange 4pt-Star"), 3);
        pieces.put(new Piece("Orange Circle"), 3);
        pieces.put(new Piece("Yellow Clover"), 3);
        pieces.put(new Piece("Yellow Square"), 3);
        pieces.put(new Piece("Yellow Diamond"), 3);
        pieces.put(new Piece("Yellow 8pt-Star"), 3);
        pieces.put(new Piece("Yellow 4pt-Star"), 3);
        pieces.put(new Piece("Yellow Circle"), 3);
        pieces.put(new Piece("Green Clover"), 3);
        pieces.put(new Piece("Green Square"), 3);
        pieces.put(new Piece("Green Diamond"), 3);
        pieces.put(new Piece("Green 8pt-Star"), 3);
        pieces.put(new Piece("Green 4pt-Star"), 3);
        pieces.put(new Piece("Green Circle"), 3);
        pieces.put(new Piece("Blue Clover"), 3);
        pieces.put(new Piece("Blue Square"), 3);
        pieces.put(new Piece("Blue Diamond"), 3);
        pieces.put(new Piece("Blue 8pt-Star"), 3);
        pieces.put(new Piece("Blue 4pt-Star"), 3);
        pieces.put(new Piece("Blue Circle"), 3);
        pieces.put(new Piece("Purple Clover"), 3);
        pieces.put(new Piece("Purple Square"), 3);
        pieces.put(new Piece("Purple Diamond"), 3);
        pieces.put(new Piece("Purple 8pt-Star"), 3);
        pieces.put(new Piece("Purple 4pt-Star"), 3);
        pieces.put(new Piece("Purple Circle"), 3);
    }

    public List<Piece> draw(int numPieces) {
        if(numPieces <= 0 || numPieces > 6) {
            throw new IllegalArgumentException("Invalid Number of Pieces");
        }
        
        List<Piece> available = pieces.entrySet().stream()
            .filter(e -> e.getValue() > 0)
            .flatMap(e -> Collections.nCopies(e.getValue(), e.getKey()).stream())
            .collect(Collectors.toList());
            
        if(available.size() < numPieces) {
            throw new IllegalStateException("Not enough pieces in bag");
        }
        
        List<Piece> drawn = new ArrayList<>();
        Random random = new Random();
        
        for(int i = 0; i < numPieces; i++) {
            int index = random.nextInt(available.size());
            Piece selectedPiece = available.remove(index);
            drawn.add(selectedPiece);
            pieces.merge(selectedPiece, -1, Integer::sum);
        }
        
        pieces.entrySet().removeIf(entry -> entry.getValue() <= 0);
        return drawn;
    }


    public boolean isEmpty() {
        return pieces.isEmpty();
    }

    public int size() {
        int count = 0;
        for(Piece piece : pieces.keySet()) {
            count += pieces.get(piece);
        }
        return count;
    }
}
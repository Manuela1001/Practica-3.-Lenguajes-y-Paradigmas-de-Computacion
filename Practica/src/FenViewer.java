import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;
import java.util.regex.*;

public class FenViewer {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FenViewer().createAndShowGui());
    }

    private JFrame frame;
    private JPanel boardPanel;
    private JLabel statusLabel;
    private JTextField fenField;
    private JPanel infoPanel;

    private void createAndShowGui() {
        frame = new JFrame("FEN Viewer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(8,8));
        frame.setSize(760, 640);
        frame.setLocationRelativeTo(null);

        JPanel top = new JPanel(new BorderLayout(6,6));
        top.setBorder(new EmptyBorder(8,8,0,8));
        fenField = new JTextField();
        fenField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        fenField.setText("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w - - 1 1");

        JButton parseBtn = new JButton("Parse & Show");
        parseBtn.addActionListener(e -> onParse());

        top.add(new JLabel("FEN:"), BorderLayout.WEST);
        top.add(fenField, BorderLayout.CENTER);
        top.add(parseBtn, BorderLayout.EAST);

        frame.add(top, BorderLayout.NORTH);

        boardPanel = new JPanel(new GridLayout(8,8));
        boardPanel.setBorder(new EmptyBorder(8,8,8,8));
        frame.add(boardPanel, BorderLayout.CENTER);

        infoPanel = new JPanel(new GridLayout(5,1));
        infoPanel.setBorder(new EmptyBorder(0,8,8,8));
        statusLabel = new JLabel("Enter FEN and press Parse & Show");
        infoPanel.add(statusLabel);

        frame.add(infoPanel, BorderLayout.SOUTH);

        renderEmptyBoard();
        frame.setVisible(true);
    }

    private void renderEmptyBoard() {
        boardPanel.removeAll();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                JLabel sq = makeSquareLabel(null, r, c);
                boardPanel.add(sq);
            }
        }
        boardPanel.revalidate();
        boardPanel.repaint();
    }

    private JLabel makeSquareLabel(String pieceUnicode, int row, int col) {
        JLabel label = new JLabel();
        label.setOpaque(true);
        boolean light = (row + col) % 2 == 0; // top-left (0,0) light
        label.setBackground(light ? new Color(240, 217, 181) : new Color(181, 136, 99));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);
        label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 36));
        if (pieceUnicode != null) label.setText(pieceUnicode);
        else label.setText("");
        label.setBorder(BorderFactory.createLineBorder(new Color(0,0,0,30)));
        return label;
    }

    private void onParse() {
        String fen = fenField.getText().trim();
        try {
            FenData data = FenParser.parse(fen);
            showFenData(data);
            statusLabel.setText("FEN OK. Side to move: " + (data.sideToMove == 'w' ? "White" : "Black"));
        } catch (FenParseException ex) {
            showError(ex.getMessage());
        }
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(frame, "Invalid FEN string:\n" + msg, "FEN Error", JOptionPane.ERROR_MESSAGE);
        statusLabel.setText("Invalid FEN: " + msg);
    }

    private void showFenData(FenData data) {
        boardPanel.removeAll();
        for (int r = 0; r < 8; r++) {
            for (int f = 0; f < 8; f++) {
                String piece = data.board[r][f];
                JLabel sq = makeSquareLabel(piece, r, f);
                boardPanel.add(sq);
            }
        }
        boardPanel.revalidate();
        boardPanel.repaint();

        infoPanel.removeAll();
        infoPanel.add(new JLabel("Side to move: " + (data.sideToMove == 'w' ? "White" : "Black")));
        infoPanel.add(new JLabel("Castling: " + (data.castling.equals("-") ? "None" : data.castling)));
        infoPanel.add(new JLabel("En-passant: " + (data.enPassant.equals("-") ? "None" : data.enPassant)));
        infoPanel.add(new JLabel("Halfmove clock: " + data.halfmoveClock));
        infoPanel.add(new JLabel("Fullmove number: " + data.fullmoveNumber));
        infoPanel.revalidate();
        infoPanel.repaint();
    }

    static class FenData {
        String[][] board = new String[8][8];
        char sideToMove;
        String castling;
        String enPassant;
        int halfmoveClock;
        int fullmoveNumber;
    }

    static class FenParseException extends Exception {
        FenParseException(String msg) { super(msg); }
    }


    static class FenParser {
        private static final Pattern FEN_SPLIT = Pattern.compile("\\s+");
        private static final Set<Character> WHITE_PIECES = new HashSet<>(Arrays.asList('P','N','B','R','Q','K'));
        private static final Set<Character> BLACK_PIECES = new HashSet<>(Arrays.asList('p','n','b','r','q','k'));
        private static final Map<Character, String> PIECE_TO_UNICODE = new HashMap<>();
        static {

            PIECE_TO_UNICODE.put('K', "\u2654");
            PIECE_TO_UNICODE.put('Q', "\u2655");
            PIECE_TO_UNICODE.put('R', "\u2656");
            PIECE_TO_UNICODE.put('B', "\u2657");
            PIECE_TO_UNICODE.put('N', "\u2658");
            PIECE_TO_UNICODE.put('P', "\u2659");

            PIECE_TO_UNICODE.put('k', "\u265A");
            PIECE_TO_UNICODE.put('q', "\u265B");
            PIECE_TO_UNICODE.put('r', "\u265C");
            PIECE_TO_UNICODE.put('b', "\u265D");
            PIECE_TO_UNICODE.put('n', "\u265E");
            PIECE_TO_UNICODE.put('p', "\u265F");
        }


        public static FenData parse(String fen) throws FenParseException {
            if (fen == null) throw new FenParseException("FEN string is empty");
            fen = fen.trim();
            if (fen.isEmpty()) throw new FenParseException("FEN string is empty");


            String[] tokens = FEN_SPLIT.split(fen);
            if (tokens.length != 6) {
                throw new FenParseException("FEN must have 6 fields (piece placement, side to move, castling, en-passant, halfmove, fullmove). Found: " + tokens.length);
            }

            FenData data = new FenData();

            parsePiecePlacement(tokens[0], data);
            parseSideToMove(tokens[1], data);
            parseCastling(tokens[2], data);
            parseEnPassant(tokens[3], data);
            parseHalfmove(tokens[4], data);
            parseFullmove(tokens[5], data);


            return data;
        }

        private static void parsePiecePlacement(String placement, FenData out) throws FenParseException {
            String[] ranks = placement.split("/");
            if (ranks.length != 8) {
                throw new FenParseException("Piece placement must contain 8 ranks separated by '/'. Found: " + ranks.length);
            }

            for (int r = 0; r < 8; r++) {
                String rankStr = ranks[r];
                int file = 0;
                for (int i = 0; i < rankStr.length(); i++) {
                    char ch = rankStr.charAt(i);
                    if (Character.isDigit(ch)) {
                        int empties = ch - '0';
                        if (empties < 1 || empties > 8) {
                            throw new FenParseException("Digit in rank must be between 1 and 8. Found '" + ch + "' in rank " + (8-r));
                        }
                        // place empties
                        for (int k = 0; k < empties; k++) {
                            if (file >= 8) {
                                throw new FenParseException("Too many squares in rank " + (8 - r) + ".");
                            }
                            out.board[r][file] = null;
                            file++;
                        }
                    } else if (isPieceChar(ch)) {
                        if (file >= 8) {
                            throw new FenParseException("Too many squares in rank " + (8 - r) + " (extra piece '" + ch + "').");
                        }
                        out.board[r][file] = PIECE_TO_UNICODE.get(ch);
                        file++;
                    } else {
                        throw new FenParseException("Invalid character '" + ch + "' in piece placement (allowed: pnbrqkPNBRQK and digits 1-8).");
                    }
                }
                if (file != 8) {
                    throw new FenParseException("Rank " + (8 - r) + " does not have exactly 8 squares (has " + file + ").");
                }
            }
        }

        private static boolean isPieceChar(char ch) {
            return WHITE_PIECES.contains(ch) || BLACK_PIECES.contains(ch);
        }

        private static void parseSideToMove(String s, FenData out) throws FenParseException {
            if (s.length() != 1) throw new FenParseException("Side-to-move field must be a single character 'w' or 'b'. Found: '" + s + "'");
            char c = s.charAt(0);
            if (c != 'w' && c != 'b') throw new FenParseException("Side-to-move must be 'w' or 'b'. Found: '" + c + "'");
            out.sideToMove = c;
        }

        private static void parseCastling(String s, FenData out) throws FenParseException {
            if (s.equals("-")) {
                out.castling = "-";
                return;
            }

            if (s.length() < 1 || s.length() > 4) throw new FenParseException("Castling availability must be '-' or between 1 and 4 characters from [KQkq]. Found: '" + s + "'");
            Set<Character> seen = new HashSet<>();
            for (char c : s.toCharArray()) {
                if ("KQkq".indexOf(c) == -1) {
                    throw new FenParseException("Invalid castling character '" + c + "'. Allowed: K Q k q or '-'");
                }
                if (seen.contains(c)) throw new FenParseException("Duplicate castling character '" + c + "' in castling field.");
                seen.add(c);
            }
            out.castling = s;
        }

        private static void parseEnPassant(String s, FenData out) throws FenParseException {
            if (s.equals("-")) {
                out.enPassant = "-";
                return;
            }

            if (s.length() != 2) throw new FenParseException("En-passant field must be '-' or a square like 'e3' or 'd6'. Found: '" + s + "'");
            char file = s.charAt(0);
            char rank = s.charAt(1);
            if (file < 'a' || file > 'h') throw new FenParseException("En-passant file must be between 'a' and 'h'. Found: '" + file + "'");
            if (rank != '3' && rank != '6') throw new FenParseException("En-passant rank must be '3' or '6' (square of pawn that could be captured). Found: '" + rank + "'");
            out.enPassant = s;
        }

        private static void parseHalfmove(String s, FenData out) throws FenParseException {

            if (!s.matches("\\d+")) throw new FenParseException("Halfmove clock must be a non-negative integer (digits only). Found: '" + s + "'");
            try {
                int val = Integer.parseInt(s);
                if (val < 0) throw new FenParseException("Halfmove clock must be >= 0.");
                out.halfmoveClock = val;
            } catch (NumberFormatException nfe) {
                throw new FenParseException("Halfmove clock number too large or invalid: " + s);
            }
        }

        private static void parseFullmove(String s, FenData out) throws FenParseException {

            if (!s.matches("\\d+")) throw new FenParseException("Fullmove number must be a positive integer. Found: '" + s + "'");
            try {
                int val = Integer.parseInt(s);
                if (val < 1) throw new FenParseException("Fullmove number must be >= 1.");
                out.fullmoveNumber = val;
            } catch (NumberFormatException nfe) {
                throw new FenParseException("Fullmove number too large or invalid: " + s);
            }
        }
    }
}

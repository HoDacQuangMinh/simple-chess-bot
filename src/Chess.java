import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * To compile and run:
 * 1. Save the code as Chess.java
 * 2. Compile: javac Chess.java
 * 3. Run: java Chess
 */
public class Chess extends JFrame {

    // --- GAME MODE ---
    enum GameMode { PVP, PVE }
    private GameMode gameMode;
    private Bot bot;

    // --- CONSTANTS ---
    private static final int INITIAL_TIME_SECONDS = 600; // 10 minutes

    // --- GUI COMPONENTS ---
    private Board chessBoard;
    private JLabel statusLabel;
    private JLabel whiteTimerLabel;
    private JLabel blackTimerLabel;
    private JLayeredPane layeredPane;
    private JPanel gameOverPanel;
    private JLabel gameOverLabel;
    private CapturedPiecesPanel whiteCapturedPanel;
    private CapturedPiecesPanel blackCapturedPanel;


    // --- TIMER ---
    private Timer gameTimer;
    private int whiteTimeLeft = INITIAL_TIME_SECONDS;
    private int blackTimeLeft = INITIAL_TIME_SECONDS;

    // --- CAPTURED PIECES ---
    private List<Piece> whiteCaptured = new ArrayList<>();
    private List<Piece> blackCaptured = new ArrayList<>();


    /**
     * Main constructor to set up the game window.
     */
    public Chess() {
        setTitle("Chess Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        int boardHeight = Board.TILE_SIZE * Board.BOARD_SIZE;
        int topPanelHeight = 30;
        int capturedPanelHeight = 40;
        int statusPanelHeight = 40;
        int totalHeight = boardHeight + topPanelHeight + capturedPanelHeight * 2 + statusPanelHeight;


        // Use JLayeredPane to overlay a "Game Over" panel
        layeredPane = new JLayeredPane();
        layeredPane.setPreferredSize(new Dimension(Board.TILE_SIZE * Board.BOARD_SIZE, totalHeight));
        setContentPane(layeredPane);

        // Main container
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBounds(0,0, Board.TILE_SIZE * Board.BOARD_SIZE, totalHeight);
        layeredPane.add(mainPanel, JLayeredPane.DEFAULT_LAYER);

        // --- Top Panel for Black's info ---
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setPreferredSize(new Dimension(Board.TILE_SIZE * Board.BOARD_SIZE, topPanelHeight));
        blackTimerLabel = new JLabel(formatTime(blackTimeLeft), SwingConstants.RIGHT);
        blackTimerLabel.setFont(new Font("Arial", Font.BOLD, 20));
        blackTimerLabel.setBorder(new EmptyBorder(5, 10, 5, 10));
        topPanel.add(new JLabel("Black"), BorderLayout.WEST);
        topPanel.add(blackTimerLabel, BorderLayout.EAST);

        // --- Bottom Panel for White's info ---
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setPreferredSize(new Dimension(Board.TILE_SIZE * Board.BOARD_SIZE, topPanelHeight));
        whiteTimerLabel = new JLabel(formatTime(whiteTimeLeft), SwingConstants.RIGHT);
        whiteTimerLabel.setFont(new Font("Arial", Font.BOLD, 20));
        whiteTimerLabel.setBorder(new EmptyBorder(5, 10, 5, 10));
        bottomPanel.add(new JLabel("White"), BorderLayout.WEST);
        bottomPanel.add(whiteTimerLabel, BorderLayout.EAST);

        // --- Captured Pieces Panels ---
        whiteCapturedPanel = new CapturedPiecesPanel();
        blackCapturedPanel = new CapturedPiecesPanel();

        // --- Status Label ---
        statusLabel = new JLabel("White's Turn", SwingConstants.CENTER);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Info Panel to hold status and timers ---
        JPanel infoPanel = new JPanel(new GridLayout(4, 1));
        infoPanel.add(topPanel);
        infoPanel.add(blackCapturedPanel);
        infoPanel.add(whiteCapturedPanel);
        infoPanel.add(bottomPanel);

        // --- Main Layout ---
        mainPanel.add(infoPanel, BorderLayout.NORTH);
        chessBoard = new Board(this);
        mainPanel.add(chessBoard, BorderLayout.CENTER);
        mainPanel.add(statusLabel, BorderLayout.SOUTH);

        // Game over panel (initially invisible)
        setupGameOverPanel();

        pack();
        setLocationRelativeTo(null); // Center the window

        // Setup the timer BEFORE prompting for game mode
        setupGameTimer();

        // Prompt for game mode before starting everything
        promptForGameMode();
    }

    private void promptForGameMode() {
        String[] options = {"Player vs. Player", "Player vs. Bot"};
        int choice = JOptionPane.showOptionDialog(this, "Choose a game mode:", "New Game",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

        if (choice == 1) {
            gameMode = GameMode.PVE;
            bot = new Bot(Piece.Color.BLACK);
            statusLabel.setText("Your Turn (White)");
        } else {
            gameMode = GameMode.PVP;
            bot = null; // No bot needed
            statusLabel.setText("White's Turn");
        }

        // If the user closes the dialog, exit the application
        if (choice == JOptionPane.CLOSED_OPTION) {
            System.exit(0);
        }

        // Now that mode is set, start the game
        restartGame();
        setVisible(true);
    }

    public void triggerBotMove() {
        // Disable board interaction while bot is thinking
        chessBoard.setEnabled(false);
        // Use a short timer to make the bot's move feel less instant
        Timer botTimer = new Timer(500, e -> {
            Move botMove = bot.findBestMove(chessBoard);
            if (botMove != null) {
                chessBoard.movePiece(botMove);
                if (chessBoard.isGameOver()) return; // Stop if the move ended the game
                chessBoard.switchTurn();
                chessBoard.updateGameStatus();
                chessBoard.repaint();
            }
            // Re-enable board
            chessBoard.setEnabled(true);
        });
        botTimer.setRepeats(false);
        botTimer.start();
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    private void setupGameOverPanel() {
        gameOverPanel = new JPanel();
        gameOverPanel.setLayout(new BoxLayout(gameOverPanel, BoxLayout.Y_AXIS));
        gameOverPanel.setBackground(new Color(0,0,0,180));
        gameOverPanel.setBounds(0,0, layeredPane.getWidth(), layeredPane.getHeight());
        gameOverPanel.setVisible(false);

        gameOverLabel = new JLabel();
        gameOverLabel.setForeground(Color.WHITE);
        gameOverLabel.setFont(new Font("Arial", Font.BOLD, 36));
        gameOverLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton playAgainButton = new JButton("Play Again");
        playAgainButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        playAgainButton.addActionListener(e -> {
            setVisible(false); // Hide the old window
            promptForGameMode(); // Ask for new game mode
        });

        gameOverPanel.add(Box.createVerticalGlue());
        gameOverPanel.add(gameOverLabel);
        gameOverPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        gameOverPanel.add(playAgainButton);
        gameOverPanel.add(Box.createVerticalGlue());

        layeredPane.add(gameOverPanel, JLayeredPane.PALETTE_LAYER);
    }

    private void setupGameTimer() {
        gameTimer = new Timer(1000, e -> {
            if (chessBoard.isGameOver()) {
                gameTimer.stop();
                return;
            }
            if (chessBoard.isWhiteTurn()) {
                whiteTimeLeft--;
                whiteTimerLabel.setText(formatTime(whiteTimeLeft));
                if (whiteTimeLeft <= 0) {
                    endGame("Black wins on time!");
                }
            } else {
                blackTimeLeft--;
                blackTimerLabel.setText(formatTime(blackTimeLeft));
                if (blackTimeLeft <= 0) {
                    endGame("White wins on time!");
                }
            }
        });
    }

    public void endGame(String message) {
        gameTimer.stop();
        chessBoard.setGameOver(true);
        gameOverLabel.setText("<html><div style='text-align: center;'>" + message + "</div></html>");
        gameOverPanel.setVisible(true);
        statusLabel.setText("Game Over");
    }

    public void restartGame() {
        whiteTimeLeft = INITIAL_TIME_SECONDS;
        blackTimeLeft = INITIAL_TIME_SECONDS;
        whiteTimerLabel.setText(formatTime(whiteTimeLeft));
        blackTimerLabel.setText(formatTime(blackTimeLeft));

        whiteCaptured.clear();
        blackCaptured.clear();
        updateCapturedPieces();

        chessBoard.restart();
        if (gameMode == GameMode.PVP) {
            statusLabel.setText("White's Turn");
        } else {
            statusLabel.setText("Your Turn (White)");
        }
        gameOverPanel.setVisible(false);
        gameTimer.start();
    }


    private String formatTime(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    public void addCapturedPiece(Piece piece) {
        if (piece.isWhite()) {
            blackCaptured.add(piece);
        } else {
            whiteCaptured.add(piece);
        }
        updateCapturedPieces();
    }

    private void updateCapturedPieces() {
        whiteCapturedPanel.update(whiteCaptured);
        blackCapturedPanel.update(blackCaptured);
    }


    /**
     * Updates the status label at the bottom of the window.
     * @param text The text to display.
     */
    public void setStatus(String text) {
        statusLabel.setText(text);
    }

    /**
     * The main method to start the application.
     */
    public static void main(String[] args) {
        // Run the GUI creation on the Event Dispatch Thread for thread safety
        SwingUtilities.invokeLater(Chess::new);
    }
}

/**
 * Represents the 8x8 chessboard panel. Handles drawing the board, pieces,
 * and user interactions.
 */
class Board extends JPanel implements MouseListener {
    // --- CONSTANTS ---
    public static final int TILE_SIZE = 80;
    public static final int BOARD_SIZE = 8;
    private final Color LIGHT_COLOR = new Color(240, 217, 181);
    private final Color DARK_COLOR = new Color(181, 136, 99);
    private final Color HIGHLIGHT_COLOR = new Color(130, 151, 105, 192);
    private final Color SELECT_COLOR = new Color(255, 255, 0, 128);
    private final Color CHECK_COLOR = new Color(255, 0, 0, 128);


    // --- GAME STATE ---
    private final Piece[][] board = new Piece[BOARD_SIZE][BOARD_SIZE];
    private Piece selectedPiece = null;
    private boolean isWhiteTurn = true;
    private final Chess game; // Reference to the main game frame
    private Move lastMove = null;
    private boolean isGameOver = false;

    /**
     * Board constructor.
     * @param game The main Chess frame.
     */
    public Board(Chess game) {
        this.game = game;
        setPreferredSize(new Dimension(TILE_SIZE * BOARD_SIZE, TILE_SIZE * BOARD_SIZE));
        addMouseListener(this);
        setupInitialPieces();
    }

    // --- GETTERS & SETTERS for game state ---
    public Piece getPiece(int row, int col) { return board[row][col]; }
    public Move getLastMove() { return lastMove; }
    public Piece[][] getBoardState() { return board; }
    public boolean isWhiteTurn() { return isWhiteTurn; }
    public boolean isGameOver() { return isGameOver; }
    public void setGameOver(boolean gameOver) { this.isGameOver = gameOver; }
    public void switchTurn() { this.isWhiteTurn = !this.isWhiteTurn; }

    public void restart() {
        setupInitialPieces();
        selectedPiece = null;
        isWhiteTurn = true;
        lastMove = null;
        isGameOver = false;
        repaint();
    }

    /**
     * Sets up the initial positions of all pieces on the board.
     */
    private void setupInitialPieces() {
        for(int r = 0; r < BOARD_SIZE; r++) {
            for(int c = 0; c < BOARD_SIZE; c++) {
                board[r][c] = null;
            }
        }

        // Black pieces
        board[0][0] = new Rook(0, 0, Piece.Color.BLACK);
        board[0][1] = new Knight(0, 1, Piece.Color.BLACK);
        board[0][2] = new Bishop(0, 2, Piece.Color.BLACK);
        board[0][3] = new Queen(0, 3, Piece.Color.BLACK);
        board[0][4] = new King(0, 4, Piece.Color.BLACK);
        board[0][5] = new Bishop(0, 5, Piece.Color.BLACK);
        board[0][6] = new Knight(0, 6, Piece.Color.BLACK);
        board[0][7] = new Rook(0, 7, Piece.Color.BLACK);
        for (int col = 0; col < BOARD_SIZE; col++) {
            board[1][col] = new Pawn(1, col, Piece.Color.BLACK);
        }

        // White pieces
        board[7][0] = new Rook(7, 0, Piece.Color.WHITE);
        board[7][1] = new Knight(7, 1, Piece.Color.WHITE);
        board[7][2] = new Bishop(7, 2, Piece.Color.WHITE);
        board[7][3] = new Queen(7, 3, Piece.Color.WHITE);
        board[7][4] = new King(7, 4, Piece.Color.WHITE);
        board[7][5] = new Bishop(7, 5, Piece.Color.WHITE);
        board[7][6] = new Knight(7, 6, Piece.Color.WHITE);
        board[7][7] = new Rook(7, 7, Piece.Color.WHITE);
        for (int col = 0; col < BOARD_SIZE; col++) {
            board[6][col] = new Pawn(6, col, Piece.Color.WHITE);
        }
    }

    /**
     * Custom painting method to draw the board and pieces.
     * @param g The Graphics object to draw on.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Draw the checkered board
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                Color tileColor = (row + col) % 2 == 0 ? LIGHT_COLOR : DARK_COLOR;
                g2d.setColor(tileColor);
                g2d.fillRect(col * TILE_SIZE, row * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            }
        }

        // Highlight king in check
        King checkedKing = findKing(isWhiteTurn ? Piece.Color.WHITE : Piece.Color.BLACK);
        if (!isGameOver && checkedKing != null && isKingInCheck(checkedKing.getColor())) {
            g2d.setColor(CHECK_COLOR);
            g2d.fillRect(checkedKing.getCol() * TILE_SIZE, checkedKing.getRow() * TILE_SIZE, TILE_SIZE, TILE_SIZE);
        }

        // Highlight the selected piece's square
        if (selectedPiece != null && !isGameOver) {
            g2d.setColor(SELECT_COLOR);
            g2d.fillRect(selectedPiece.getCol() * TILE_SIZE, selectedPiece.getRow() * TILE_SIZE, TILE_SIZE, TILE_SIZE);

            // Highlight possible moves
            g2d.setColor(HIGHLIGHT_COLOR);
            for (Move move : selectedPiece.getValidMoves(this)) {
                g2d.fillOval(move.getEndCol() * TILE_SIZE + TILE_SIZE / 4,
                        move.getEndRow() * TILE_SIZE + TILE_SIZE / 4,
                        TILE_SIZE / 2, TILE_SIZE / 2);
            }
        }

        // Draw all the pieces
        g2d.setFont(new Font("Serif", Font.BOLD, 56));
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                Piece piece = board[row][col];
                if (piece != null) {
                    piece.draw(g2d);
                }
            }
        }
    }

    /**
     * Handles mouse click events on the board.
     * @param e The MouseEvent.
     */
    @Override
    public void mousePressed(MouseEvent e) {
        if (isGameOver || (game.getGameMode() == Chess.GameMode.PVE && !isWhiteTurn)) {
            return;
        }

        int col = e.getX() / TILE_SIZE;
        int row = e.getY() / TILE_SIZE;

        Piece clickedPiece = getPiece(row, col);

        if (selectedPiece == null) {
            // --- SELECT A PIECE ---
            if (clickedPiece != null && (isWhiteTurn ? clickedPiece.isWhite() : clickedPiece.isBlack())) {
                selectedPiece = clickedPiece;
            }
        } else {
            // --- MOVE A PIECE ---
            List<Move> validMoves = selectedPiece.getValidMoves(this);
            Move targetMove = findMoveInList(row, col, validMoves);

            if (targetMove != null) {
                movePiece(targetMove);
                if (isGameOver) return; // Stop if movePiece ended the game

                switchTurn();
                updateGameStatus();
                selectedPiece = null;

                // If now it's the bot's turn, trigger its move
                if (!isGameOver && game.getGameMode() == Chess.GameMode.PVE && !isWhiteTurn) {
                    game.triggerBotMove();
                }

            } else if (clickedPiece != null && (isWhiteTurn ? clickedPiece.isWhite() : clickedPiece.isBlack())) {
                // Select another piece of the same color
                selectedPiece = clickedPiece;
            } else {
                // Deselect
                selectedPiece = null;
            }
        }
        repaint(); // Redraw the board to show changes
    }

    public void updateGameStatus() {
        Piece.Color currentPlayer = isWhiteTurn ? Piece.Color.WHITE : Piece.Color.BLACK;
        boolean inCheck = isKingInCheck(currentPlayer);
        boolean hasLegalMoves = hasAnyLegalMoves(currentPlayer);

        if (inCheck && !hasLegalMoves) {
            game.endGame("Checkmate! " + (isWhiteTurn ? "Black" : "White") + " wins.");
        } else if (!inCheck && !hasLegalMoves) {
            game.endGame("Stalemate! Game is a draw.");
        } else if (inCheck) {
            game.setStatus((isWhiteTurn ? "White" : "Black") + "'s Turn (in Check!)");
        } else {
            if (game.getGameMode() == Chess.GameMode.PVE) {
                game.setStatus(isWhiteTurn ? "Your Turn (White)" : "Bot is thinking...");
            } else {
                game.setStatus((isWhiteTurn ? "White" : "Black") + "'s Turn");
            }
        }
    }

    public boolean hasAnyLegalMoves(Piece.Color color) {
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                Piece p = board[r][c];
                if (p != null && p.getColor() == color) {
                    if (!p.getValidMoves(this).isEmpty()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


    /**
     * Finds a move in a list that targets the given row and column.
     * This is necessary to get the correct Move object with its type (e.g., CASTLE).
     */
    private Move findMoveInList(int row, int col, List<Move> validMoves) {
        for (Move move : validMoves) {
            if (move.getEndRow() == row && move.getEndCol() == col) {
                return move;
            }
        }
        return null;
    }


    /**
     * Executes a move, updating the board state.
     * @param move The move to be made.
     */
    public void movePiece(Move move) {
        Piece pieceToMove = board[move.getStartRow()][move.getStartCol()];
        Piece capturedPiece = board[move.getEndRow()][move.getEndCol()];

        if (capturedPiece != null) {
            game.addCapturedPiece(capturedPiece);
        }
        move.setCapturedPiece(capturedPiece);

        // Check if the captured piece is a king
        if (move.getCapturedPiece() instanceof King) {
            board[move.getEndRow()][move.getEndCol()] = pieceToMove;
            board[move.getStartRow()][move.getStartCol()] = null;
            repaint();
            String winner = pieceToMove.isWhite() ? "White" : "Black";
            game.endGame("Checkmate! " + winner + " wins.");
            return;
        }

        // Handle special move types
        switch(move.getType()) {
            case EN_PASSANT:
                Piece enPassantPawn = board[move.getStartRow()][move.getEndCol()];
                game.addCapturedPiece(enPassantPawn);
                board[move.getStartRow()][move.getEndCol()] = null; // Remove the captured pawn
                break;
            case CASTLE:
                // Move the rook
                if (move.getEndCol() == 6) { // Kingside
                    Piece rook = board[move.getStartRow()][7];
                    board[move.getStartRow()][5] = rook;
                    board[move.getStartRow()][7] = null;
                    rook.setPos(move.getStartRow(), 5);
                    rook.setHasMoved(true);
                } else { // Queenside
                    Piece rook = board[move.getStartRow()][0];
                    board[move.getStartRow()][3] = rook;
                    board[move.getStartRow()][0] = null;
                    rook.setPos(move.getStartRow(), 3);
                    rook.setHasMoved(true);
                }
                break;
            case NORMAL:
                // No special action needed
                break;
        }

        // Move the primary piece
        board[move.getEndRow()][move.getEndCol()] = pieceToMove;
        board[move.getStartRow()][move.getStartCol()] = null;
        pieceToMove.setPos(move.getEndRow(), move.getEndCol());

        // Mark piece as having moved (for castling rights)
        if (pieceToMove instanceof King || pieceToMove instanceof Rook) {
            pieceToMove.setHasMoved(true);
        }

        // Handle pawn promotion
        if (pieceToMove instanceof Pawn) {
            if ((pieceToMove.isWhite() && move.getEndRow() == 0) || (pieceToMove.isBlack() && move.getEndRow() == 7)) {
                board[move.getEndRow()][move.getEndCol()] = new Queen(move.getEndRow(), move.getEndCol(), pieceToMove.getColor());
            }
        }

        this.lastMove = move; // Store this move for en passant checks
    }

    /**
     * Finds the King of a specific color.
     * @param color The color of the king to find.
     * @return The King piece, or null if not found.
     */
    public King findKing(Piece.Color color) {
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                Piece p = board[r][c];
                if (p instanceof King && p.getColor() == color) {
                    return (King) p;
                }
            }
        }
        return null; // Should not happen in a normal game
    }

    /**
     * Checks if the king of a given color is currently under attack.
     * @param kingColor The color of the king to check.
     * @return True if the king is in check, false otherwise.
     */
    public boolean isKingInCheck(Piece.Color kingColor) {
        King king = findKing(kingColor);
        if (king == null) return false;
        return isSquareUnderAttack(king.getRow(), king.getCol(), kingColor.opposite());
    }

    /**
     * Checks if a square is under attack by the opponent. This is crucial for
     * check, checkmate, and castling validation.
     *
     * @param row The row of the square.
     * @param col The column of the square.
     * @param attackerColor The color of the attacking pieces.
     * @return True if the square is under attack, false otherwise.
     */
    public boolean isSquareUnderAttack(int row, int col, Piece.Color attackerColor) {
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                Piece p = board[r][c];
                if (p != null && p.getColor() == attackerColor) {
                    for (Move move : p.getAttackMoves(this)) {
                        if (move.getEndRow() == row && move.getEndCol() == col) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Simulates a move and checks if it results in the king being in check.
     * @param move The move to test.
     * @return True if the move is legal (does not result in check), false otherwise.
     */
    public boolean isMoveLegal(Move move) {
        Piece pieceToMove = getPiece(move.getStartRow(), move.getStartCol());
        Piece capturedPiece;

        // Determine the captured piece based on move type
        if (move.getType() == Move.MoveType.EN_PASSANT) {
            capturedPiece = getPiece(move.getStartRow(), move.getEndCol());
        } else {
            capturedPiece = getPiece(move.getEndRow(), move.getEndCol());
        }

        // Simulate the move
        board[move.getEndRow()][move.getEndCol()] = pieceToMove;
        board[move.getStartRow()][move.getStartCol()] = null;
        if(move.getType() == Move.MoveType.EN_PASSANT) {
            board[move.getStartRow()][move.getEndCol()] = null;
        }

        boolean isLegal = !isKingInCheck(pieceToMove.getColor());

        // Undo the move
        board[move.getStartRow()][move.getStartCol()] = pieceToMove;
        board[move.getEndRow()][move.getEndCol()] = (move.getType() == Move.MoveType.EN_PASSANT) ? null : capturedPiece;

        if(move.getType() == Move.MoveType.EN_PASSANT) {
            board[move.getStartRow()][move.getEndCol()] = capturedPiece;
        }

        return isLegal;
    }


    // Unused mouse listener methods
    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
}


/**
 * Abstract base class for all chess pieces.
 */
abstract class Piece {
    protected int row, col;
    protected final Color color;
    protected boolean hasMoved = false;

    enum Color {
        WHITE, BLACK;
        public Color opposite() {
            return this == WHITE ? BLACK : WHITE;
        }
    }

    public Piece(int row, int col, Color color) {
        this.row = row;
        this.col = col;
        this.color = color;
    }

    public int getRow() { return row; }
    public int getCol() { return col; }
    public Color getColor() { return color; }
    public boolean isWhite() { return color == Color.WHITE; }
    public boolean isBlack() { return color == Color.BLACK; }
    public void setPos(int row, int col) {
        this.row = row;
        this.col = col;
    }
    public boolean hasMoved() { return hasMoved; }
    public void setHasMoved(boolean hasMoved) { this.hasMoved = hasMoved; }


    /**
     * Draws the piece on the board.
     * @param g2d Graphics context.
     */
    public void draw(Graphics2D g2d) {
        g2d.setColor(isWhite() ? java.awt.Color.WHITE : java.awt.Color.BLACK);
        // Center the piece in the tile
        FontMetrics fm = g2d.getFontMetrics();
        int stringWidth = fm.stringWidth(getSymbol());
        int stringHeight = fm.getAscent();
        int x = col * Board.TILE_SIZE + (Board.TILE_SIZE - stringWidth) / 2;
        int y = row * Board.TILE_SIZE + (Board.TILE_SIZE + stringHeight) / 2;
        g2d.drawString(getSymbol(), x, y);
    }

    /**
     * Gets the Unicode symbol for the piece.
     */
    public abstract String getSymbol();

    /**
     * Gets the value of the piece for AI evaluation.
     */
    public abstract int getValue();

    /**
     * Calculates and returns a list of all moves this piece can make,
     * regardless of whether it puts the king in check.
     * @param board The current board object.
     */
    public abstract List<Move> getPseudoLegalMoves(Board board);

    /**
     * Calculates the squares this piece attacks, ignoring special rules like castling.
     * This is used to break the recursion in check detection.
     * @param board The current board object.
     */
    public abstract List<Move> getAttackMoves(Board board);

    /**
     * Gets all fully legal moves by filtering pseudo-legal moves.
     */
    public List<Move> getValidMoves(Board board) {
        return getPseudoLegalMoves(board).stream()
                .filter(board::isMoveLegal)
                .collect(Collectors.toList());
    }


    /**
     * Helper method to add valid moves in a straight or diagonal line.
     */
    protected void addLineMoves(List<Move> moves, Piece[][] boardState, int dRow, int dCol) {
        for (int i = 1; i < Board.BOARD_SIZE; i++) {
            int newRow = row + i * dRow;
            int newCol = col + i * dCol;

            if (newRow < 0 || newRow >= Board.BOARD_SIZE || newCol < 0 || newCol >= Board.BOARD_SIZE) {
                break; // Off the board
            }

            Piece targetPiece = boardState[newRow][newCol];
            if (targetPiece == null) {
                moves.add(new Move(row, col, newRow, newCol, this));
            } else {
                // *** ACCURACY FIX: The piece can move to capture an opponent piece. ***
                // The loop must break after finding any piece (friend or foe).
                if (targetPiece.getColor() != this.color) {
                    moves.add(new Move(row, col, newRow, newCol, this));
                }
                break;
            }
        }
    }
}

/**
 * Represents a single move from a start to an end square.
 */
class Move {
    enum MoveType { NORMAL, CASTLE, EN_PASSANT }
    private final int startRow, startCol, endRow, endCol;
    private final MoveType type;
    private final Piece piece;
    private Piece capturedPiece;

    public Move(int startRow, int startCol, int endRow, int endCol, Piece piece, MoveType type) {
        this.startRow = startRow;
        this.startCol = startCol;
        this.endRow = endRow;
        this.endCol = endCol;
        this.piece = piece;
        this.type = type;
        this.capturedPiece = null;
    }

    public Move(int startRow, int startCol, int endRow, int endCol, Piece piece) {
        this(startRow, startCol, endRow, endCol, piece, MoveType.NORMAL);
    }

    public int getStartRow() { return startRow; }
    public int getStartCol() { return startCol; }
    public int getEndRow() { return endRow; }
    public int getEndCol() { return endCol; }
    public MoveType getType() { return type; }
    public Piece getPiece() { return piece; }
    public Piece getCapturedPiece() { return capturedPiece; }
    public void setCapturedPiece(Piece p) { this.capturedPiece = p; }
}


// --- CONCRETE PIECE IMPLEMENTATIONS ---

class King extends Piece {
    public King(int row, int col, Color color) { super(row, col, color); }
    @Override public String getSymbol() { return isWhite() ? "♔" : "♚"; }
    @Override public int getValue() { return 10000; } // Effectively infinite value

    private List<Move> getStandardKingMoves() {
        List<Move> moves = new ArrayList<>();
        int[] dRows = {-1, -1, -1, 0, 0, 1, 1, 1};
        int[] dCols = {-1, 0, 1, -1, 1, -1, 0, 1};

        for (int i = 0; i < 8; i++) {
            int newRow = row + dRows[i];
            int newCol = col + dCols[i];
            if (newRow >= 0 && newRow < Board.BOARD_SIZE && newCol >= 0 && newCol < Board.BOARD_SIZE) {
                moves.add(new Move(row, col, newRow, newCol, this));
            }
        }
        return moves;
    }

    @Override
    public List<Move> getAttackMoves(Board board) {
        return getStandardKingMoves();
    }

    @Override
    public List<Move> getPseudoLegalMoves(Board board) {
        List<Move> moves = new ArrayList<>();
        for(Move move : getStandardKingMoves()) {
            Piece targetPiece = board.getPiece(move.getEndRow(), move.getEndCol());
            if (targetPiece == null || targetPiece.getColor() != this.color) {
                moves.add(move);
            }
        }

        // --- Castling Logic ---
        if (!hasMoved && !board.isSquareUnderAttack(row, col, color.opposite())) {
            // Kingside
            Piece kingsideRook = board.getPiece(row, 7);
            if (kingsideRook instanceof Rook && !kingsideRook.hasMoved()) {
                if (board.getPiece(row, 5) == null && board.getPiece(row, 6) == null) {
                    if (!board.isSquareUnderAttack(row, 5, color.opposite()) && !board.isSquareUnderAttack(row, 6, color.opposite())) {
                        moves.add(new Move(row, col, row, 6, this, Move.MoveType.CASTLE));
                    }
                }
            }
            // Queenside
            Piece queensideRook = board.getPiece(row, 0);
            if (queensideRook instanceof Rook && !queensideRook.hasMoved()) {
                if (board.getPiece(row, 1) == null && board.getPiece(row, 2) == null && board.getPiece(row, 3) == null) {
                    if (!board.isSquareUnderAttack(row, 2, color.opposite()) && !board.isSquareUnderAttack(row, 3, color.opposite())) {
                        moves.add(new Move(row, col, row, 2, this, Move.MoveType.CASTLE));
                    }
                }
            }
        }
        return moves;
    }
}

class Queen extends Piece {
    public Queen(int row, int col, Color color) { super(row, col, color); }
    @Override public String getSymbol() { return isWhite() ? "♕" : "♛"; }
    @Override public int getValue() { return 90; }

    @Override
    public List<Move> getPseudoLegalMoves(Board board) {
        List<Move> moves = new ArrayList<>();
        Piece[][] boardState = board.getBoardState();
        addLineMoves(moves, boardState, 1, 0);
        addLineMoves(moves, boardState, -1, 0);
        addLineMoves(moves, boardState, 0, 1);
        addLineMoves(moves, boardState, 0, -1);
        addLineMoves(moves, boardState, 1, 1);
        addLineMoves(moves, boardState, 1, -1);
        addLineMoves(moves, boardState, -1, 1);
        addLineMoves(moves, boardState, -1, -1);
        return moves;
    }

    @Override
    public List<Move> getAttackMoves(Board board) {
        return getPseudoLegalMoves(board);
    }
}

class Rook extends Piece {
    public Rook(int row, int col, Color color) { super(row, col, color); }
    @Override public String getSymbol() { return isWhite() ? "♖" : "♜"; }
    @Override public int getValue() { return 50; }

    @Override
    public List<Move> getPseudoLegalMoves(Board board) {
        List<Move> moves = new ArrayList<>();
        Piece[][] boardState = board.getBoardState();
        addLineMoves(moves, boardState, 1, 0);
        addLineMoves(moves, boardState, -1, 0);
        addLineMoves(moves, boardState, 0, 1);
        addLineMoves(moves, boardState, 0, -1);
        return moves;
    }

    @Override
    public List<Move> getAttackMoves(Board board) {
        return getPseudoLegalMoves(board);
    }
}

class Bishop extends Piece {
    public Bishop(int row, int col, Color color) { super(row, col, color); }
    @Override public String getSymbol() { return isWhite() ? "♗" : "♝"; }
    @Override public int getValue() { return 30; }

    @Override
    public List<Move> getPseudoLegalMoves(Board board) {
        List<Move> moves = new ArrayList<>();
        Piece[][] boardState = board.getBoardState();
        addLineMoves(moves, boardState, 1, 1);
        addLineMoves(moves, boardState, 1, -1);
        addLineMoves(moves, boardState, -1, 1);
        addLineMoves(moves, boardState, -1, -1);
        return moves;
    }

    @Override
    public List<Move> getAttackMoves(Board board) {
        return getPseudoLegalMoves(board);
    }
}

class Knight extends Piece {
    public Knight(int row, int col, Color color) { super(row, col, color); }
    @Override public String getSymbol() { return isWhite() ? "♘" : "♞"; }
    @Override public int getValue() { return 30; }

    @Override
    public List<Move> getPseudoLegalMoves(Board board) {
        List<Move> moves = new ArrayList<>();
        Piece[][] boardState = board.getBoardState();
        int[] dRows = {-2, -2, -1, -1, 1, 1, 2, 2};
        int[] dCols = {-1, 1, -2, 2, -2, 2, -1, 1};

        for (int i = 0; i < 8; i++) {
            int newRow = row + dRows[i];
            int newCol = col + dCols[i];

            if (newRow >= 0 && newRow < Board.BOARD_SIZE && newCol >= 0 && newCol < Board.BOARD_SIZE) {
                Piece targetPiece = boardState[newRow][newCol];
                if (targetPiece == null || targetPiece.getColor() != this.color) {
                    moves.add(new Move(row, col, newRow, newCol, this));
                }
            }
        }
        return moves;
    }

    @Override
    public List<Move> getAttackMoves(Board board) {
        return getPseudoLegalMoves(board);
    }
}

class Pawn extends Piece {
    public Pawn(int row, int col, Color color) { super(row, col, color); }
    @Override public String getSymbol() { return isWhite() ? "♙" : "♟"; }
    @Override public int getValue() { return 10; }

    @Override
    public List<Move> getAttackMoves(Board board) {
        List<Move> moves = new ArrayList<>();
        int direction = isWhite() ? -1 : 1;
        int newRow = row + direction;

        // Pawns only attack diagonally.
        for (int dCol = -1; dCol <= 1; dCol += 2) {
            int newCol = col + dCol;
            if (newRow >= 0 && newRow < Board.BOARD_SIZE && newCol >= 0 && newCol < Board.BOARD_SIZE) {
                moves.add(new Move(row, col, newRow, newCol, this));
            }
        }
        return moves;
    }

    @Override
    public List<Move> getPseudoLegalMoves(Board board) {
        List<Move> moves = new ArrayList<>();
        Piece[][] boardState = board.getBoardState();
        int direction = isWhite() ? -1 : 1;
        int startRow = isWhite() ? 6 : 1;

        // 1. Move one step forward
        int newRow = row + direction;
        if (newRow >= 0 && newRow < Board.BOARD_SIZE && boardState[newRow][col] == null) {
            moves.add(new Move(row, col, newRow, col, this));
            // 2. Move two steps forward from start
            if (row == startRow && boardState[newRow + direction][col] == null) {
                moves.add(new Move(row, col, newRow + direction, col, this));
            }
        }

        // 3. Capture diagonally
        for (int dCol = -1; dCol <= 1; dCol += 2) {
            int newCol = col + dCol;
            if (newRow >= 0 && newRow < Board.BOARD_SIZE && newCol >= 0 && newCol < Board.BOARD_SIZE) {
                Piece target = boardState[newRow][newCol];
                if (target != null && target.getColor() != this.color) {
                    moves.add(new Move(row, col, newRow, newCol, this));
                }
            }
        }

        // 4. En Passant
        Move lastMove = board.getLastMove();
        if (lastMove != null) {
            Piece lastMovedPiece = lastMove.getPiece();
            if (lastMovedPiece instanceof Pawn && Math.abs(lastMove.getStartRow() - lastMove.getEndRow()) == 2) {
                if (lastMove.getEndRow() == this.row && Math.abs(lastMove.getEndCol() - this.col) == 1) {
                    moves.add(new Move(this.row, this.col, this.row + direction, lastMove.getEndCol(), this, Move.MoveType.EN_PASSANT));
                }
            }
        }
        return moves;
    }
}

/**
 * An improved bot that uses a 1-ply lookahead with material evaluation.
 */
class Bot {
    private final Piece.Color botColor;
    private final Random random = new Random();

    public Bot(Piece.Color botColor) {
        this.botColor = botColor;
    }

    /**
     * Finds the best move by looking one step ahead and evaluating the material balance.
     * This is an improvement over the original greedy algorithm as it considers all moves,
     * not just captures, and evaluates the board state after the move.
     */
    public Move findBestMove(Board board) {
        List<Move> allPossibleMoves = new ArrayList<>();
        for (int r = 0; r < Board.BOARD_SIZE; r++) {
            for (int c = 0; c < Board.BOARD_SIZE; c++) {
                Piece piece = board.getPiece(r, c);
                if (piece != null && piece.getColor() == botColor) {
                    allPossibleMoves.addAll(piece.getValidMoves(board));
                }
            }
        }

        if (allPossibleMoves.isEmpty()) {
            return null; // No legal moves
        }

        int bestScore = Integer.MIN_VALUE;
        List<Move> bestMoves = new ArrayList<>();

        for (Move move : allPossibleMoves) {
            // --- Temporarily simulate the move ---
            Piece movingPiece = board.getPiece(move.getStartRow(), move.getStartCol());
            Piece capturedPiece = null;
            Piece[][] boardState = board.getBoardState();

            // Handle special case for en passant capture
            if (move.getType() == Move.MoveType.EN_PASSANT) {
                capturedPiece = boardState[move.getStartRow()][move.getEndCol()];
                boardState[move.getStartRow()][move.getEndCol()] = null;
            } else {
                capturedPiece = board.getPiece(move.getEndRow(), move.getEndCol());
            }

            // Make the move on the board state array
            boardState[move.getEndRow()][move.getEndCol()] = movingPiece;
            boardState[move.getStartRow()][move.getStartCol()] = null;

            // --- Evaluate the resulting board ---
            int score = evaluate(board);

            // --- Undo the move on the board state array ---
            boardState[move.getStartRow()][move.getStartCol()] = movingPiece;
            boardState[move.getEndRow()][move.getEndCol()] = capturedPiece;
            if (move.getType() == Move.MoveType.EN_PASSANT) {
                // The landing square was empty, so restore it and the captured pawn
                boardState[move.getStartRow()][move.getEndCol()] = capturedPiece;
                boardState[move.getEndRow()][move.getEndCol()] = null;
            }

            // --- Compare scores ---
            if (score > bestScore) {
                bestScore = score;
                bestMoves.clear();
                bestMoves.add(move);
            } else if (score == bestScore) {
                bestMoves.add(move);
            }
        }

        // Pick one of the best moves at random to add variety
        return bestMoves.get(random.nextInt(bestMoves.size()));
    }

    /**
     * A simple evaluation function based on material count.
     * Positive score is good for the bot, negative is good for the opponent.
     * Note: Piece values have been scaled up to allow for more nuanced evaluation in the future.
     */
    private int evaluate(Board board) {
        int totalScore = 0;
        for (int r = 0; r < Board.BOARD_SIZE; r++) {
            for (int c = 0; c < Board.BOARD_SIZE; c++) {
                Piece piece = board.getPiece(r, c);
                if (piece != null) {
                    int value = piece.getValue();
                    if (piece.getColor() == botColor) {
                        totalScore += value;
                    } else {
                        totalScore -= value;
                    }
                }
            }
        }
        return totalScore;
    }
}

/**
 * A panel to display captured pieces and material score.
 */
class CapturedPiecesPanel extends JPanel {
    private List<Piece> capturedPieces = new ArrayList<>();
    private JLabel scoreLabel = new JLabel();

    public CapturedPiecesPanel() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));
        add(scoreLabel);
        scoreLabel.setFont(new Font("Arial", Font.BOLD, 14));
    }

    public void update(List<Piece> pieces) {
        this.capturedPieces = new ArrayList<>(pieces);
        // Sort pieces by value for a consistent display
        this.capturedPieces.sort(Comparator.comparingInt(Piece::getValue));

        int whiteScore = 0;
        int blackScore = 0;

        for (Piece p : pieces) {
            if(p.isWhite()) whiteScore += p.getValue();
            else blackScore += p.getValue();
        }

        int scoreDifference = whiteScore - blackScore;
        if(scoreDifference > 0) {
            scoreLabel.setText("+" + (scoreDifference/10));
        } else {
            scoreLabel.setText("");
        }

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setFont(new Font("Serif", Font.PLAIN, 28));

        int x = 80; // Start drawing pieces after the score label
        for (Piece piece : capturedPieces) {
            g2d.setColor(piece.isWhite() ? new Color(200,200,200) : new Color(50,50,50));
            g2d.drawString(piece.getSymbol(), x, 25);
            x += 20;
        }
    }
}
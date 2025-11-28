package com.example.puzzle_assemble_picture;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import android.os.Vibrator;
import android.os.VibrationEffect;

public class PuzzleView extends View {

    private static final String TAG = "PuzzleView";

    private PuzzlePiece[][] grid;
    private List<PuzzlePiece> allPieces;
    private PuzzleConfig config;
    private PuzzleListener listener;

    private float gridX, gridY;
    private int gridWidth, gridHeight;
    private int cellWidth, cellHeight; // THAY ĐỔI: Riêng biệt width và height

    private PuzzlePiece draggedPiece;
    private int draggedFromRow, draggedFromCol;
    private float dragOffsetX, dragOffsetY;
    private float draggedPieceX, draggedPieceY;
    private boolean isDragging = false;

    private Paint paint;
    private Paint dimPaint;
    private Paint gridPaint;
    private Paint borderPaint;

    public interface PuzzleListener {
        void onPieceConnected();
        void onPuzzleCompleted();
        void onProgressChanged();
    }

    public PuzzleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        dimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dimPaint.setAlpha(128);
        dimPaint.setColorFilter(new PorterDuffColorFilter(Color.argb(100, 255, 255, 255), PorterDuff.Mode.SRC_ATOP));

        gridPaint = new Paint();
        gridPaint.setColor(Color.argb(100, 255, 255, 255));
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(2);

        borderPaint = new Paint();
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(3);

        allPieces = new ArrayList<>();
    }

    public void initPuzzle(Bitmap image, PuzzleConfig config, PuzzleListener listener) {
        this.config = config;
        this.listener = listener;
        allPieces.clear();

        int screenWidth = getWidth();
        int screenHeight = getHeight();

        Log.d(TAG, "Initializing puzzle: " + config.gridSize + "x" + config.gridSize);
        Log.d(TAG, "Screen size: " + screenWidth + "x" + screenHeight);
        Log.d(TAG, "Image size: " + image.getWidth() + "x" + image.getHeight());

        // FIX: Grid theo tỷ lệ ảnh gốc, mảnh có thể là hình chữ nhật
        int padding = 40;
        int availableWidth = screenWidth - (padding * 2);
        int availableHeight = screenHeight - (padding * 2);

        // Tính aspect ratio của ảnh gốc
        float imageAspectRatio = (float) image.getHeight() / image.getWidth();

        // Grid width = 90% màn hình
        gridWidth = (int) (availableWidth * 0.9f);

        // Grid height theo tỷ lệ ảnh gốc
        gridHeight = (int) (gridWidth * imageAspectRatio);

        // Đảm bảo không vượt quá màn hình
        if (gridHeight > availableHeight) {
            gridHeight = availableHeight;
            gridWidth = (int) (gridHeight / imageAspectRatio);
        }

        // Center grid
        gridX = (screenWidth - gridWidth) / 2f;
        gridY = (screenHeight - gridHeight) / 2f;

        // QUAN TRỌNG: Cell width và height RIÊNG BIỆT
        cellWidth = gridWidth / config.gridSize;
        cellHeight = gridHeight / config.gridSize;

        Log.d(TAG, "Grid: " + gridWidth + "x" + gridHeight);
        Log.d(TAG, "Cell: " + cellWidth + "x" + cellHeight);

        // Scale ảnh theo EXACT grid size
        Bitmap scaledImage = Bitmap.createScaledBitmap(
                image,
                gridWidth,
                gridHeight,
                true
        );

        grid = new PuzzlePiece[config.gridSize][config.gridSize];

        // Tạo pieces với kích thước hình chữ nhật chính xác
        for (int row = 0; row < config.gridSize; row++) {
            for (int col = 0; col < config.gridSize; col++) {
                int x = col * cellWidth;
                int y = row * cellHeight;

                int width = Math.min(cellWidth, scaledImage.getWidth() - x);
                int height = Math.min(cellHeight, scaledImage.getHeight() - y);

                if (width <= 0 || height <= 0) {
                    Log.e(TAG, "Invalid piece size at [" + row + "," + col + "]");
                    continue;
                }

                try {
                    Bitmap pieceBitmap = Bitmap.createBitmap(
                            scaledImage,
                            x, y,
                            width, height
                    );

                    // Piece với width/height riêng biệt
                    PuzzlePiece piece = new PuzzlePiece(pieceBitmap, row, col, cellWidth, cellHeight);
                    allPieces.add(piece);

                    Log.d(TAG, "Created piece [" + row + "," + col + "] size " + width + "x" + height);
                } catch (Exception e) {
                    Log.e(TAG, "Error creating piece [" + row + "," + col + "]", e);
                }
            }
        }

        if (allPieces.size() != config.gridSize * config.gridSize) {
            Log.e(TAG, "Piece count mismatch! Expected: " +
                    (config.gridSize * config.gridSize) + ", Got: " + allPieces.size());
        }

        shufflePieces();
        checkAllConnections();
        invalidate();
    }

    private void shufflePieces() {
        List<PuzzlePiece> shuffled = new ArrayList<>(allPieces);
        Collections.shuffle(shuffled);

        int index = 0;
        for (int row = 0; row < config.gridSize; row++) {
            for (int col = 0; col < config.gridSize; col++) {
                grid[row][col] = shuffled.get(index++);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (config == null) return;

        // Background
        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.argb(50, 0, 0, 0));
        canvas.drawRect(gridX, gridY, gridX + gridWidth, gridY + gridHeight, bgPaint);

        // Grid lines với cellWidth và cellHeight
        for (int i = 0; i <= config.gridSize; i++) {
            float x = gridX + i * cellWidth;
            float y = gridY + i * cellHeight;
            canvas.drawLine(x, gridY, x, gridY + gridHeight, gridPaint);
            canvas.drawLine(gridX, y, gridX + gridWidth, y, gridPaint);
        }

        // Draw pieces
        for (int row = 0; row < config.gridSize; row++) {
            for (int col = 0; col < config.gridSize; col++) {
                if (grid[row][col] != null && grid[row][col] != draggedPiece) {
                    drawPieceAt(canvas, grid[row][col], row, col);
                }
            }
        }

        // Draw dragged piece
        if (draggedPiece != null && isDragging) {
            RectF destRect = new RectF(
                    draggedPieceX,
                    draggedPieceY,
                    draggedPieceX + cellWidth,
                    draggedPieceY + cellHeight
            );

            Paint dragPaint = draggedPiece.isLocked() && config.dimLockedPieces ? dimPaint : paint;
            canvas.drawBitmap(draggedPiece.getBitmap(), null, destRect, dragPaint);

            borderPaint.setColor(Color.YELLOW);
            borderPaint.setStrokeWidth(5);
            canvas.drawRect(destRect, borderPaint);
        }

        // Outer border
        Paint outerBorder = new Paint();
        outerBorder.setColor(Color.WHITE);
        outerBorder.setStyle(Paint.Style.STROKE);
        outerBorder.setStrokeWidth(5);
        canvas.drawRect(gridX, gridY, gridX + gridWidth, gridY + gridHeight, outerBorder);
    }

    private void drawPieceAt(Canvas canvas, PuzzlePiece piece, int row, int col) {
        float x = gridX + col * cellWidth;
        float y = gridY + row * cellHeight;

        RectF destRect = new RectF(x, y, x + cellWidth, y + cellHeight);

        Paint currentPaint = piece.isLocked() && config.dimLockedPieces ? dimPaint : paint;
        canvas.drawBitmap(piece.getBitmap(), null, destRect, currentPaint);

        boolean isCorrect = (piece.getCorrectRow() == row && piece.getCorrectCol() == col);

        // FIX: Không vẽ viền cho locked pieces (chỉ dim)
        if (piece.isLocked()) {
            // Không vẽ viền, chỉ để dim
            return;
        }

        borderPaint.setStrokeWidth(3);
        if (!piece.getConnectedPieces().isEmpty()) {
            borderPaint.setColor(Color.YELLOW);
        } else {
            borderPaint.setColor(Color.argb(150, 255, 255, 255));
        }

        canvas.drawRect(destRect, borderPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                return handleTouchDown(x, y);

            case MotionEvent.ACTION_MOVE:
                return handleTouchMove(x, y);

            case MotionEvent.ACTION_UP:
                return handleTouchUp(x, y);
        }

        return super.onTouchEvent(event);
    }

    private boolean handleTouchDown(float x, float y) {
        if (x < gridX || x >= gridX + gridWidth || y < gridY || y >= gridY + gridHeight) {
            return false;
        }

        int col = (int) Math.floor((x - gridX) / cellWidth);
        int row = (int) Math.floor((y - gridY) / cellHeight);

        if (row < 0 || row >= config.gridSize || col < 0 || col >= config.gridSize) {
            return false;
        }

        PuzzlePiece piece = grid[row][col];

        if (piece == null) {
            return false;
        }

        // FIX: Không cho kéo mảnh locked
        if (piece.isLocked()) {
            Log.d(TAG, "Piece at [" + row + "," + col + "] is locked - cannot drag");
            return false;
        }

        float cellLeft = gridX + col * cellWidth;
        float cellTop = gridY + row * cellHeight;
        float cellRight = cellLeft + cellWidth;
        float cellBottom = cellTop + cellHeight;

        if (x >= cellLeft && x < cellRight && y >= cellTop && y < cellBottom) {
            draggedPiece = piece;
            draggedFromRow = row;
            draggedFromCol = col;
            dragOffsetX = x - cellLeft;
            dragOffsetY = y - cellTop;
            draggedPieceX = cellLeft;
            draggedPieceY = cellTop;
            isDragging = true;

            Log.d(TAG, "Started dragging piece[" + piece.getCorrectRow() + "," +
                    piece.getCorrectCol() + "] from grid[" + row + "," + col + "]");

            invalidate();
            return true;
        }

        return false;
    }

    private boolean handleTouchMove(float x, float y) {
        if (!isDragging || draggedPiece == null) {
            return false;
        }

        draggedPieceX = x - dragOffsetX;
        draggedPieceY = y - dragOffsetY;
        invalidate();
        return true;
    }

    private boolean handleTouchUp(float x, float y) {
        if (!isDragging || draggedPiece == null) {
            return false;
        }

        try {
            float centerX = draggedPieceX + cellWidth / 2f;
            float centerY = draggedPieceY + cellHeight / 2f;

            int dropCol = (int) Math.floor((centerX - gridX) / cellWidth);
            int dropRow = (int) Math.floor((centerY - gridY) / cellHeight);

            // Clamp to valid range
            dropCol = Math.max(0, Math.min(dropCol, config.gridSize - 1));
            dropRow = Math.max(0, Math.min(dropRow, config.gridSize - 1));

            Log.d(TAG, "Dropped piece from [" + draggedFromRow + "," + draggedFromCol +
                    "] to [" + dropRow + "," + dropCol + "]");

            // Only move if position changed
            if (dropRow != draggedFromRow || dropCol != draggedFromCol) {
                movePiece(draggedFromRow, draggedFromCol, dropRow, dropCol);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in handleTouchUp", e);
        } finally {
            // Always cleanup
            draggedPiece = null;
            isDragging = false;
            invalidate();
        }

        // Check completion
        try {
            if (isPuzzleComplete()) {
                if (listener != null) {
                    listener.onPuzzleCompleted();
                }
            } else if (listener != null) {
                listener.onProgressChanged();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking puzzle completion", e);
        }

        return true;
    }

    private void movePiece(int fromRow, int fromCol, int toRow, int toCol) {
        try {
            // Validate bounds
            if (fromRow < 0 || fromRow >= config.gridSize ||
                    fromCol < 0 || fromCol >= config.gridSize ||
                    toRow < 0 || toRow >= config.gridSize ||
                    toCol < 0 || toCol >= config.gridSize) {
                Log.e(TAG, "Invalid move indices!");
                return;
            }

            PuzzlePiece movingPiece = grid[fromRow][fromCol];
            PuzzlePiece targetPiece = grid[toRow][toCol];

            if (movingPiece == null) {
                Log.w(TAG, "No piece at source position");
                return;
            }

            // Check if moving piece is locked
            if (movingPiece.isLocked()) {
                Log.d(TAG, "Cannot move locked piece");
                return;
            }

            // Check if target piece is locked
            if (targetPiece != null && targetPiece.isLocked()) {
                Log.d(TAG, "Cannot swap with locked piece at [" + toRow + "," + toCol + "]");
                return;
            }

            Log.d(TAG, "Moving piece[" + movingPiece.getCorrectRow() + "," +
                    movingPiece.getCorrectCol() + "] from grid[" + fromRow + "," + fromCol +
                    "] to grid[" + toRow + "," + toCol + "]");

            swapPieces(fromRow, fromCol, toRow, toCol);
            checkAllConnections();
            checkLocking();

            if (listener != null) {
                listener.onPieceConnected();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in movePiece", e);
            e.printStackTrace();
        }
    }

    private void swapPieces(int fromRow, int fromCol, int toRow, int toCol) {
        try {
            // Validate indices
            if (fromRow < 0 || fromRow >= config.gridSize ||
                    fromCol < 0 || fromCol >= config.gridSize ||
                    toRow < 0 || toRow >= config.gridSize ||
                    toCol < 0 || toCol >= config.gridSize) {
                Log.e(TAG, "Invalid swap indices: from[" + fromRow + "," + fromCol +
                        "] to[" + toRow + "," + toCol + "]");
                return;
            }

            // Perform swap
            PuzzlePiece temp = grid[fromRow][fromCol];
            grid[fromRow][fromCol] = grid[toRow][toCol];
            grid[toRow][toCol] = temp;

            Log.d(TAG, "Swapped grid[" + fromRow + "," + fromCol + "] <-> grid[" +
                    toRow + "," + toCol + "]");

        } catch (Exception e) {
            Log.e(TAG, "Error in swapPieces", e);
            e.printStackTrace();
        }
    }

    private void checkAllConnections() {
        try {
            // Clear all connections
            for (PuzzlePiece piece : allPieces) {
                if (piece != null) {
                    piece.getConnectedPieces().clear();
                }
            }

            // Check horizontal and vertical adjacency
            for (int row = 0; row < config.gridSize; row++) {
                for (int col = 0; col < config.gridSize; col++) {
                    PuzzlePiece piece = grid[row][col];
                    if (piece == null) continue;

                    // Check right neighbor
                    if (col < config.gridSize - 1) {
                        PuzzlePiece right = grid[row][col + 1];
                        if (right != null && shouldConnect(piece, right, row, col, row, col + 1)) {
                            piece.addConnectedPiece(right);
                            right.addConnectedPiece(piece);
                        }
                    }

                    // Check bottom neighbor
                    if (row < config.gridSize - 1) {
                        PuzzlePiece bottom = grid[row + 1][col];
                        if (bottom != null && shouldConnect(piece, bottom, row, col, row + 1, col)) {
                            piece.addConnectedPiece(bottom);
                            bottom.addConnectedPiece(piece);
                        }
                    }
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in checkAllConnections", e);
            e.printStackTrace();
        }
    }

    private boolean shouldConnect(PuzzlePiece piece1, PuzzlePiece piece2,
                                  int row1, int col1, int row2, int col2) {
        if (!config.autoConnectCorrectPieces) return false;

        int correctRowDiff = piece2.getCorrectRow() - piece1.getCorrectRow();
        int correctColDiff = piece2.getCorrectCol() - piece1.getCorrectCol();

        int currentRowDiff = row2 - row1;
        int currentColDiff = col2 - col1;

        return correctRowDiff == currentRowDiff && correctColDiff == currentColDiff;
    }

    private void checkLocking() {
        try {
            if (!config.autoLockCorrectPieces) return;

            for (int row = 0; row < config.gridSize; row++) {
                for (int col = 0; col < config.gridSize; col++) {
                    PuzzlePiece piece = grid[row][col];
                    if (piece != null && !piece.isLocked()) {
                        if (piece.getCorrectRow() == row && piece.getCorrectCol() == col) {
                            piece.setLocked(true);
                            vibratePiece();
                            Log.d(TAG, "Locked piece at correct position [" + row + "," + col + "]");
                        }
                    }
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in checkLocking", e);
            e.printStackTrace();
        }
    }

    private boolean isPuzzleComplete() {
        for (int row = 0; row < config.gridSize; row++) {
            for (int col = 0; col < config.gridSize; col++) {
                PuzzlePiece piece = grid[row][col];
                if (piece == null || piece.getCorrectRow() != row || piece.getCorrectCol() != col) {
                    return false;
                }
            }
        }
        return true;
    }

    public int getCorrectPiecesCount() {
        int count = 0;
        for (int row = 0; row < config.gridSize; row++) {
            for (int col = 0; col < config.gridSize; col++) {
                PuzzlePiece piece = grid[row][col];
                if (piece != null && piece.getCorrectRow() == row && piece.getCorrectCol() == col) {
                    count++;
                }
            }
        }
        return count;
    }

    public boolean isPuzzleCompleted() {
        return isPuzzleComplete();
    }

    public GameSaveData getSaveData() {
        GameSaveData saveData = new GameSaveData();

        for (int row = 0; row < config.gridSize; row++) {
            for (int col = 0; col < config.gridSize; col++) {
                PuzzlePiece piece = grid[row][col];
                if (piece != null) {
                    float x = gridX + col * cellWidth;
                    float y = gridY + row * cellHeight;
                    saveData.piecePositions.add(new GameSaveData.PiecePosition(
                            piece.getCorrectRow(),
                            piece.getCorrectCol(),
                            x, y,
                            piece.isLocked()
                    ));
                }
            }
        }

        return saveData;
    }

    public void loadGameState(GameSaveData saveData) {
        if (saveData == null || saveData.piecePositions == null) return;

        for (GameSaveData.PiecePosition pos : saveData.piecePositions) {
            for (PuzzlePiece piece : allPieces) {
                if (piece.getCorrectRow() == pos.correctRow &&
                        piece.getCorrectCol() == pos.correctCol) {

                    int col = Math.round((pos.x - gridX) / cellWidth);
                    int row = Math.round((pos.y - gridY) / cellHeight);

                    if (row >= 0 && row < config.gridSize && col >= 0 && col < config.gridSize) {
                        grid[row][col] = piece;
                        piece.setLocked(pos.isLocked);
                    }
                    break;
                }
            }
        }

        checkAllConnections();
        invalidate();
    }

    public void cleanup() {
        if (allPieces != null) {
            for (PuzzlePiece piece : allPieces) {
                if (piece != null && piece.getBitmap() != null && !piece.getBitmap().isRecycled()) {
                    piece.getBitmap().recycle();
                }
            }
            allPieces.clear();
        }
        grid = null;
        draggedPiece = null;
        isDragging = false;
        Log.d(TAG, "Cleanup completed");
    }

    private void vibratePiece() {
        if (SettingsActivity.isVibrationEnabled(getContext())) {
            Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(50);
                }
            }
        }
    }

    /**
     * AUTO-SOLVE ONE PIECE
     * Tìm 1 mảnh sai vị trí và đổi về đúng chỗ
     */
    public boolean autoSolveOnePiece() {
        try {
            // Tìm mảnh đầu tiên chưa đúng vị trí và chưa locked
            for (int row = 0; row < config.gridSize; row++) {
                for (int col = 0; col < config.gridSize; col++) {
                    PuzzlePiece piece = grid[row][col];

                    if (piece != null && !piece.isLocked()) {
                        // Kiểm tra nếu piece này chưa đúng vị trí
                        if (piece.getCorrectRow() != row || piece.getCorrectCol() != col) {
                            // Tìm vị trí đúng của piece này
                            int correctRow = piece.getCorrectRow();
                            int correctCol = piece.getCorrectCol();

                            // Kiểm tra piece ở vị trí đúng có locked không
                            PuzzlePiece pieceAtCorrectPos = grid[correctRow][correctCol];
                            if (pieceAtCorrectPos != null && pieceAtCorrectPos.isLocked()) {
                                continue; // Skip, không swap với locked piece
                            }

                            // Swap
                            Log.d(TAG, "Auto-solving: Moving piece[" + piece.getCorrectRow() + "," +
                                    piece.getCorrectCol() + "] from [" + row + "," + col +
                                    "] to [" + correctRow + "," + correctCol + "]");

                            swapPieces(row, col, correctRow, correctCol);
                            checkAllConnections();
                            checkLocking();
                            invalidate();

                            if (listener != null) {
                                listener.onPieceConnected();
                            }

                            return true; // Success
                        }
                    }
                }
            }

            Log.d(TAG, "No piece to auto-solve (all correct or locked)");
            return false;

        } catch (Exception e) {
            Log.e(TAG, "Error in autoSolveOnePiece", e);
            return false;
        }
    }

    /**
     * SHUFFLE REMAINING PIECES
     * Xáo trộn tất cả mảnh chưa đúng vị trí (không động locked pieces)
     */
    public boolean shuffleRemainingPieces() {
        try {
            // Thu thập tất cả pieces chưa đúng vị trí và chưa locked
            List<PuzzlePiece> incorrectPieces = new ArrayList<>();
            List<int[]> incorrectPositions = new ArrayList<>();

            for (int row = 0; row < config.gridSize; row++) {
                for (int col = 0; col < config.gridSize; col++) {
                    PuzzlePiece piece = grid[row][col];

                    if (piece != null && !piece.isLocked()) {
                        if (piece.getCorrectRow() != row || piece.getCorrectCol() != col) {
                            incorrectPieces.add(piece);
                            incorrectPositions.add(new int[]{row, col});
                        }
                    }
                }
            }

            if (incorrectPieces.isEmpty()) {
                Log.d(TAG, "No pieces to shuffle (all correct or locked)");
                return false;
            }

            // Shuffle pieces
            Collections.shuffle(incorrectPieces);

            // Đặt lại vào grid
            for (int i = 0; i < incorrectPieces.size(); i++) {
                int[] pos = incorrectPositions.get(i);
                grid[pos[0]][pos[1]] = incorrectPieces.get(i);
            }

            Log.d(TAG, "Shuffled " + incorrectPieces.size() + " pieces");

            checkAllConnections();
            invalidate();

            if (listener != null) {
                listener.onProgressChanged();
            }

            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error in shuffleRemainingPieces", e);
            return false;
        }
    }

    /**
     * GET STATS
     */
    public int getLockedPiecesCount() {
        int count = 0;
        for (int row = 0; row < config.gridSize; row++) {
            for (int col = 0; col < config.gridSize; col++) {
                PuzzlePiece piece = grid[row][col];
                if (piece != null && piece.isLocked()) {
                    count++;
                }
            }
        }
        return count;
    }

    public int getRemainingPiecesCount() {
        int totalPieces = config.gridSize * config.gridSize;
        int correctPieces = getCorrectPiecesCount();
        return totalPieces - correctPieces;
    }
}
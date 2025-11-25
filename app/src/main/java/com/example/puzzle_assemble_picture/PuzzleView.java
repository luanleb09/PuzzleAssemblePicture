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
    private static final float CONNECTION_TOLERANCE = 0.1f;

    private PuzzlePiece[][] grid;
    private List<PuzzlePiece> allPieces;
    private PuzzleConfig config;
    private PuzzleListener listener;

    private float gridX, gridY;
    private int gridWidth, gridHeight;
    private int cellSize;

    private PuzzlePiece draggedPiece;
    private int draggedFromRow, draggedFromCol;
    private float dragOffsetX, dragOffsetY;
    private float draggedPieceX, draggedPieceY;
    private boolean isDragging = false;

    private Paint paint;
    private Paint dimPaint;
    private Paint gridPaint;
    private Paint borderPaint;
    private Paint highlightPaint;

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

        highlightPaint = new Paint();
        highlightPaint.setColor(Color.argb(100, 255, 255, 0));
        highlightPaint.setStyle(Paint.Style.FILL);

        allPieces = new ArrayList<>();
    }

    public void initPuzzle(Bitmap image, PuzzleConfig config, PuzzleListener listener) {
        this.config = config;
        this.listener = listener;
        allPieces.clear();

        int screenWidth = getWidth();
        int screenHeight = getHeight();

        Log.d(TAG, "Initializing puzzle: " + config.gridSize + "x" + config.gridSize);

        int gridSize = Math.min(screenWidth, screenHeight) - 40;
        gridWidth = gridSize;
        gridHeight = gridSize;
        gridX = (screenWidth - gridWidth) / 2f;
        gridY = (screenHeight - gridHeight) / 2f;
        cellSize = gridWidth / config.gridSize;

        Bitmap scaledImage = Bitmap.createScaledBitmap(image, gridWidth, gridHeight, true);

        grid = new PuzzlePiece[config.gridSize][config.gridSize];

        for (int row = 0; row < config.gridSize; row++) {
            for (int col = 0; col < config.gridSize; col++) {
                Bitmap pieceBitmap = Bitmap.createBitmap(
                        scaledImage,
                        col * cellSize,
                        row * cellSize,
                        cellSize,
                        cellSize
                );

                PuzzlePiece piece = new PuzzlePiece(pieceBitmap, row, col, cellSize, cellSize);
                allPieces.add(piece);
            }
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
                Log.d(TAG, "Grid[" + row + "][" + col + "] = piece[" +
                        grid[row][col].getCorrectRow() + "," + grid[row][col].getCorrectCol() + "]");
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (config == null) return;

        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.argb(50, 0, 0, 0));
        canvas.drawRect(gridX, gridY, gridX + gridWidth, gridY + gridHeight, bgPaint);

        for (int i = 0; i <= config.gridSize; i++) {
            float pos = i * cellSize;
            canvas.drawLine(gridX + pos, gridY, gridX + pos, gridY + gridHeight, gridPaint);
            canvas.drawLine(gridX, gridY + pos, gridX + gridWidth, gridY + pos, gridPaint);
        }

        for (int row = 0; row < config.gridSize; row++) {
            for (int col = 0; col < config.gridSize; col++) {
                if (grid[row][col] != null && grid[row][col] != draggedPiece) {
                    drawPieceAt(canvas, grid[row][col], row, col);
                }
            }
        }

        if (draggedPiece != null && isDragging) {
            RectF destRect = new RectF(
                    draggedPieceX,
                    draggedPieceY,
                    draggedPieceX + cellSize,
                    draggedPieceY + cellSize
            );

            Paint dragPaint = draggedPiece.isLocked() && config.dimLockedPieces ? dimPaint : paint;
            canvas.drawBitmap(draggedPiece.getBitmap(), null, destRect, dragPaint);

            borderPaint.setColor(Color.YELLOW);
            borderPaint.setStrokeWidth(5);
            canvas.drawRect(destRect, borderPaint);
        }

        Paint outerBorder = new Paint();
        outerBorder.setColor(Color.WHITE);
        outerBorder.setStyle(Paint.Style.STROKE);
        outerBorder.setStrokeWidth(5);
        canvas.drawRect(gridX, gridY, gridX + gridWidth, gridY + gridHeight, outerBorder);
    }

    private void drawPieceAt(Canvas canvas, PuzzlePiece piece, int row, int col) {
        float x = gridX + col * cellSize;
        float y = gridY + row * cellSize;

        RectF destRect = new RectF(x, y, x + cellSize, y + cellSize);

        Paint currentPaint = piece.isLocked() && config.dimLockedPieces ? dimPaint : paint;
        canvas.drawBitmap(piece.getBitmap(), null, destRect, currentPaint);

        boolean isCorrect = (piece.getCorrectRow() == row && piece.getCorrectCol() == col);

        borderPaint.setStrokeWidth(3);
        if (piece.isLocked()) {
            borderPaint.setColor(Color.GREEN);
        } else if (!piece.getConnectedPieces().isEmpty()) {
            borderPaint.setColor(Color.YELLOW);
        } else if (isCorrect) {
            borderPaint.setColor(Color.argb(255, 0, 200, 0));
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
                if (x >= gridX && x < gridX + gridWidth && y >= gridY && y < gridY + gridHeight) {
                    int col = (int) ((x - gridX) / cellSize);
                    int row = (int) ((y - gridY) / cellSize);

                    if (row >= 0 && row < config.gridSize && col >= 0 && col < config.gridSize) {
                        draggedPiece = grid[row][col];
                        if (draggedPiece != null && !draggedPiece.isLocked()) {
                            draggedFromRow = row;
                            draggedFromCol = col;
                            dragOffsetX = x - (gridX + col * cellSize);
                            dragOffsetY = y - (gridY + row * cellSize);
                            draggedPieceX = gridX + col * cellSize;
                            draggedPieceY = gridY + row * cellSize;
                            isDragging = true;

                            Log.d(TAG, "Started dragging from [" + row + "," + col + "]");
                            invalidate();
                        }
                    }
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                if (isDragging && draggedPiece != null) {
                    draggedPieceX = x - dragOffsetX;
                    draggedPieceY = y - dragOffsetY;
                    invalidate();
                }
                return true;

            case MotionEvent.ACTION_UP:
                if (isDragging && draggedPiece != null) {
                    int dropCol = Math.round((x - gridX - dragOffsetX) / cellSize);
                    int dropRow = Math.round((y - gridY - dragOffsetY) / cellSize);

                    dropCol = Math.max(0, Math.min(dropCol, config.gridSize - 1));
                    dropRow = Math.max(0, Math.min(dropRow, config.gridSize - 1));

                    Log.d(TAG, "Dropped at [" + dropRow + "," + dropCol + "]");

                    if (dropRow != draggedFromRow || dropCol != draggedFromCol) {
                        movePiece(draggedFromRow, draggedFromCol, dropRow, dropCol);
                    }

                    draggedPiece = null;
                    isDragging = false;
                    invalidate();

                    if (isPuzzleComplete()) {
                        if (listener != null) {
                            listener.onPuzzleCompleted();
                        }
                    } else if (listener != null) {
                        listener.onProgressChanged();
                    }
                }
                return true;
        }

        return super.onTouchEvent(event);
    }

    private void movePiece(int fromRow, int fromCol, int toRow, int toCol) {
        PuzzlePiece movingPiece = grid[fromRow][fromCol];

        if (movingPiece == null) return;

        Log.d(TAG, "Moving piece from [" + fromRow + "," + fromCol + "] to [" + toRow + "," + toCol + "]");

        List<PuzzlePiece> connectedGroup = getConnectedGroup(movingPiece);

        if (connectedGroup.size() > 1) {
            if (canMoveGroup(connectedGroup, fromRow, fromCol, toRow, toCol)) {
                moveGroup(connectedGroup, fromRow, fromCol, toRow, toCol);
            } else {
                breakConnectionsForPiece(movingPiece);
                swapPieces(fromRow, fromCol, toRow, toCol);
            }
        } else {
            swapPieces(fromRow, fromCol, toRow, toCol);
        }

        checkAllConnections();
        checkLocking();

        if (listener != null) {
            listener.onPieceConnected();
        }
    }

    private List<PuzzlePiece> getConnectedGroup(PuzzlePiece piece) {
        List<PuzzlePiece> group = new ArrayList<>();
        buildConnectedGroup(piece, group);
        return group;
    }

    private void buildConnectedGroup(PuzzlePiece piece, List<PuzzlePiece> group) {
        if (group.contains(piece)) return;
        group.add(piece);

        for (PuzzlePiece connected : piece.getConnectedPieces()) {
            buildConnectedGroup(connected, group);
        }
    }

    private boolean canMoveGroup(List<PuzzlePiece> group, int fromRow, int fromCol, int toRow, int toCol) {
        return group.size() == 1;
    }

    private void moveGroup(List<PuzzlePiece> group, int fromRow, int fromCol, int toRow, int toCol) {
        swapPieces(fromRow, fromCol, toRow, toCol);
    }

    private void swapPieces(int fromRow, int fromCol, int toRow, int toCol) {
        PuzzlePiece temp = grid[fromRow][fromCol];
        grid[fromRow][fromCol] = grid[toRow][toCol];
        grid[toRow][toCol] = temp;

        Log.d(TAG, "Swapped [" + fromRow + "," + fromCol + "] <-> [" + toRow + "," + toCol + "]");
    }

    private void breakConnectionsForPiece(PuzzlePiece piece) {
        List<PuzzlePiece> connected = new ArrayList<>(piece.getConnectedPieces());
        for (PuzzlePiece other : connected) {
            piece.removeConnectedPiece(other);
            other.removeConnectedPiece(piece);
            Log.d(TAG, "Broke connection between pieces");
        }
    }

    private void checkAllConnections() {
        for (PuzzlePiece piece : allPieces) {
            piece.getConnectedPieces().clear();
        }

        for (int row = 0; row < config.gridSize; row++) {
            for (int col = 0; col < config.gridSize; col++) {
                PuzzlePiece piece = grid[row][col];
                if (piece == null) continue;

                if (col < config.gridSize - 1) {
                    PuzzlePiece right = grid[row][col + 1];
                    if (right != null && shouldConnect(piece, right, row, col, row, col + 1)) {
                        piece.addConnectedPiece(right);
                        right.addConnectedPiece(piece);
                    }
                }

                if (row < config.gridSize - 1) {
                    PuzzlePiece bottom = grid[row + 1][col];
                    if (bottom != null && shouldConnect(piece, bottom, row, col, row + 1, col)) {
                        piece.addConnectedPiece(bottom);
                        bottom.addConnectedPiece(piece);
                    }
                }
            }
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
        if (!config.autoLockCorrectPieces) return;

        for (int row = 0; row < config.gridSize; row++) {
            for (int col = 0; col < config.gridSize; col++) {
                PuzzlePiece piece = grid[row][col];
                if (piece != null && !piece.isLocked()) {
                    if (piece.getCorrectRow() == row && piece.getCorrectCol() == col) {
                        piece.setLocked(true);
                        Log.d(TAG, "Locked piece at correct position [" + row + "," + col + "]");
                    }
                }
            }
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
                    float x = gridX + col * cellSize;
                    float y = gridY + row * cellSize;
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

                    int col = Math.round((pos.x - gridX) / cellSize);
                    int row = Math.round((pos.y - gridY) / cellSize);

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
        Log.d(TAG, "Cleanup completed");
    }

    // Trong checkCorrectPositions() hoặc khi piece snap:
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
}
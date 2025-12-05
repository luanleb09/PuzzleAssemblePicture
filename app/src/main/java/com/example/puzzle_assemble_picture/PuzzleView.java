package com.example.puzzle_assemble_picture;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.os.Vibrator;
import android.os.VibrationEffect;
import android.view.ScaleGestureDetector;

public class PuzzleView extends View {

    private static final String TAG = "PuzzleView";

    private PuzzlePiece[][] grid;
    private List<PuzzlePiece> allPieces;
    private PuzzleConfig config;
    private PuzzleListener listener;
    private Bitmap fullImage; // Store full image for completion

    private float gridX, gridY;
    private int gridWidth, gridHeight;
    private int cellWidth, cellHeight;

    private PuzzlePiece draggedPiece;
    private int draggedFromRow, draggedFromCol;
    private float dragOffsetX, dragOffsetY;
    private float draggedPieceX, draggedPieceY;
    private boolean isDragging = false;

    private Paint paint;
    private Paint dimPaint;
    private Paint gridPaint;
    private Paint borderPaint;

    // Animation states
    private boolean isAnimating = false;
    private boolean showingCompletion = false;

    // Animated positions for pieces during swap/shuffle
    private Map<PuzzlePiece, PointF> animatedPositions = new HashMap<>();
    private float animationProgress = 0f;

    private ScaleGestureDetector scaleGestureDetector;
    private float scaleFactor = 1.0f;
    private float focusX = 0f;
    private float focusY = 0f;
    private static final float MIN_ZOOM = 1.0f;
    private static final float MAX_ZOOM = 3.0f;

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

        scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleListener());
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(MIN_ZOOM, Math.min(scaleFactor, MAX_ZOOM));

            focusX = detector.getFocusX();
            focusY = detector.getFocusY();

            invalidate();
            return true;
        }
    }


    public void initPuzzle(Bitmap image, PuzzleConfig config, PuzzleListener listener) {
        this.config = config;
        this.listener = listener;
        this.fullImage = image; // Store full image
        allPieces.clear();

        int screenWidth = getWidth();
        int screenHeight = getHeight();

        Log.d(TAG, "Initializing puzzle: " + config.gridSize + "x" + config.gridSize);

        int padding = 40;
        int availableWidth = screenWidth - (padding * 2);
        int availableHeight = screenHeight - (padding * 2);

        float imageAspectRatio = (float) image.getHeight() / image.getWidth();

        gridWidth = (int) (availableWidth * 0.9f);
        gridHeight = (int) (gridWidth * imageAspectRatio);

        if (gridHeight > availableHeight) {
            gridHeight = availableHeight;
            gridWidth = (int) (gridHeight / imageAspectRatio);
        }

        gridX = (screenWidth - gridWidth) / 2f;
        gridY = (screenHeight - gridHeight) / 2f;

        cellWidth = gridWidth / config.gridSize;
        cellHeight = gridHeight / config.gridSize;

        Bitmap scaledImage = Bitmap.createScaledBitmap(image, gridWidth, gridHeight, true);

        grid = new PuzzlePiece[config.gridSize][config.gridSize];

        for (int row = 0; row < config.gridSize; row++) {
            for (int col = 0; col < config.gridSize; col++) {
                int x = col * cellWidth;
                int y = row * cellHeight;

                int width = Math.min(cellWidth, scaledImage.getWidth() - x);
                int height = Math.min(cellHeight, scaledImage.getHeight() - y);

                if (width <= 0 || height <= 0) continue;

                try {
                    Bitmap pieceBitmap = Bitmap.createBitmap(scaledImage, x, y, width, height);
                    PuzzlePiece piece = new PuzzlePiece(pieceBitmap, row, col, cellWidth, cellHeight);
                    allPieces.add(piece);
                } catch (Exception e) {
                    Log.e(TAG, "Error creating piece [" + row + "," + col + "]", e);
                }
            }
        }

        shufflePieces();
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

        canvas.save();
        canvas.scale(scaleFactor, scaleFactor, focusX, focusY);

        // If showing completion, draw full image instead of pieces
        if (showingCompletion && fullImage != null) {
            RectF destRect = new RectF(gridX, gridY, gridX + gridWidth, gridY + gridHeight);
            canvas.drawBitmap(fullImage, null, destRect, paint);

            // Outer border
            Paint outerBorder = new Paint();
            outerBorder.setColor(Color.WHITE);
            outerBorder.setStyle(Paint.Style.STROKE);
            outerBorder.setStrokeWidth(5);
            canvas.drawRect(gridX, gridY, gridX + gridWidth, gridY + gridHeight, outerBorder);
            return;
        }

        // Background
        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.argb(50, 0, 0, 0));
        canvas.drawRect(gridX, gridY, gridX + gridWidth, gridY + gridHeight, bgPaint);

        // Grid lines
        for (int i = 0; i <= config.gridSize; i++) {
            float x = gridX + i * cellWidth;
            float y = gridY + i * cellHeight;
            canvas.drawLine(x, gridY, x, gridY + gridHeight, gridPaint);
            canvas.drawLine(gridX, y, gridX + gridWidth, y, gridPaint);
        }

        // Draw pieces (with animation if active)
        for (int row = 0; row < config.gridSize; row++) {
            for (int col = 0; col < config.gridSize; col++) {
                PuzzlePiece piece = grid[row][col];
                if (piece != null && piece != draggedPiece) {
                    // Check if piece has animated position
                    if (animatedPositions.containsKey(piece)) {
                        PointF animPos = animatedPositions.get(piece);
                        drawPieceAtPosition(canvas, piece, animPos.x, animPos.y);
                    } else {
                        drawPieceAt(canvas, piece, row, col);
                    }
                }
            }
        }

        // Draw dragged piece
        if (draggedPiece != null && isDragging) {
            drawPieceAtPosition(canvas, draggedPiece, draggedPieceX, draggedPieceY);

            borderPaint.setColor(Color.YELLOW);
            borderPaint.setStrokeWidth(5);
            RectF destRect = new RectF(
                    draggedPieceX,
                    draggedPieceY,
                    draggedPieceX + cellWidth,
                    draggedPieceY + cellHeight
            );
            canvas.drawRect(destRect, borderPaint);
        }

        // Outer border
        Paint outerBorder = new Paint();
        outerBorder.setColor(Color.WHITE);
        outerBorder.setStyle(Paint.Style.STROKE);
        outerBorder.setStrokeWidth(5);
        canvas.drawRect(gridX, gridY, gridX + gridWidth, gridY + gridHeight, outerBorder);

        canvas.restore();
    }

    private void drawPieceAtPosition(Canvas canvas, PuzzlePiece piece, float x, float y) {
        RectF destRect = new RectF(x, y, x + cellWidth, y + cellHeight);

        Paint currentPaint = piece.isLocked() && config.dimLockedPieces ? dimPaint : paint;
        canvas.drawBitmap(piece.getBitmap(), null, destRect, currentPaint);

        if (!piece.isLocked()) {
            borderPaint.setStrokeWidth(2);
            borderPaint.setColor(Color.argb(100, 255, 255, 255)); // Viền trắng mờ
            canvas.drawRect(destRect, borderPaint);
        }
    }

    private void drawPieceAt(Canvas canvas, PuzzlePiece piece, int row, int col) {
        float x = gridX + col * cellWidth;
        float y = gridY + row * cellHeight;
        drawPieceAtPosition(canvas, piece, x, y);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isAnimating || showingCompletion) return false;

        // Handle zoom gesture
        scaleGestureDetector.onTouchEvent(event);

        // If zooming, don't process drag
        if (event.getPointerCount() > 1) {
            return true;
        }

        float x = event.getX();
        float y = event.getY();

        // Convert touch coordinates to puzzle coordinates (accounting for zoom)
        float puzzleX = (x - focusX) / scaleFactor + focusX;
        float puzzleY = (y - focusY) / scaleFactor + focusY;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                return handleTouchDown(puzzleX, puzzleY);
            case MotionEvent.ACTION_MOVE:
                return handleTouchMove(puzzleX, puzzleY);
            case MotionEvent.ACTION_UP:
                return handleTouchUp(puzzleX, puzzleY);
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
        if (piece == null || piece.isLocked()) {
            return false;
        }

        float cellLeft = gridX + col * cellWidth;
        float cellTop = gridY + row * cellHeight;

        draggedPiece = piece;
        draggedFromRow = row;
        draggedFromCol = col;
        dragOffsetX = x - cellLeft;
        dragOffsetY = y - cellTop;
        draggedPieceX = cellLeft;
        draggedPieceY = cellTop;
        isDragging = true;

        invalidate();
        return true;
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

            dropCol = Math.max(0, Math.min(dropCol, config.gridSize - 1));
            dropRow = Math.max(0, Math.min(dropRow, config.gridSize - 1));

            if (dropRow != draggedFromRow || dropCol != draggedFromCol) {
                movePiece(draggedFromRow, draggedFromCol, dropRow, dropCol);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in handleTouchUp", e);
        } finally {
            draggedPiece = null;
            isDragging = false;
            invalidate();
        }

        try {
            if (isPuzzleComplete()) {
                showCompletionImage();
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
            PuzzlePiece movingPiece = grid[fromRow][fromCol];
            PuzzlePiece targetPiece = grid[toRow][toCol];

            if (movingPiece == null || movingPiece.isLocked()) {
                return;
            }

            if (targetPiece != null && targetPiece.isLocked()) {
                return;
            }

            swapPieces(fromRow, fromCol, toRow, toCol);
            checkLocking();

            if (listener != null) {
                listener.onPieceConnected();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in movePiece", e);
        }
    }

    private void swapPieces(int fromRow, int fromCol, int toRow, int toCol) {
        PuzzlePiece temp = grid[fromRow][fromCol];
        grid[fromRow][fromCol] = grid[toRow][toCol];
        grid[toRow][toCol] = temp;
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
        if (!config.autoLockCorrectPieces) {
            Log.d(TAG, "Auto-lock DISABLED");
            return;
        }

        boolean anyLocked = false;
        for (int row = 0; row < config.gridSize; row++) {
            for (int col = 0; col < config.gridSize; col++) {
                PuzzlePiece piece = grid[row][col];
                if (piece != null && !piece.isLocked()) {
                    if (piece.getCorrectRow() == row && piece.getCorrectCol() == col) {
                        piece.setLocked(true);
                        anyLocked = true;
                        Log.d(TAG, "✓ LOCKED piece at [" + row + "," + col + "]"); // ← Thêm log
                    }
                }
            }
        }

        if (anyLocked) {
            Log.d(TAG, "Pieces locked, vibrating..."); // ← Thêm log
            vibratePiece();
            invalidate();
        } else {
            Log.d(TAG, "No pieces to lock"); // ← Thêm log
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

    /**
     * Show full completion image (not pieces)
     */
    private void showCompletionImage() {
        showingCompletion = true;
        invalidate();

        // Notify listener immediately
        if (listener != null) {
            listener.onPuzzleCompleted();
        }
    }

    public void hideCompletionImage() {
        showingCompletion = false;
        invalidate();
    }

    /**
     * AUTO-SOLVE with clear animation
     */
    public boolean autoSolveOnePiece() {
        if (isAnimating || showingCompletion) {
            Log.d(TAG, "Cannot auto-solve: isAnimating=" + isAnimating + ", showingCompletion=" + showingCompletion);
            return false;
        }

        try {
            // Find first incorrect piece
            for (int row = 0; row < config.gridSize; row++) {
                for (int col = 0; col < config.gridSize; col++) {
                    PuzzlePiece piece = grid[row][col];

                    if (piece != null && !piece.isLocked()) {
                        int correctRow = piece.getCorrectRow();
                        int correctCol = piece.getCorrectCol();

                        if (correctRow != row || correctCol != col) {
                            // Check if target position is locked
                            if (correctRow >= 0 && correctRow < config.gridSize &&
                                    correctCol >= 0 && correctCol < config.gridSize) {

                                PuzzlePiece pieceAtCorrectPos = grid[correctRow][correctCol];

                                if (pieceAtCorrectPos != null && pieceAtCorrectPos.isLocked()) {
                                    Log.d(TAG, "Target position [" + correctRow + "," + correctCol + "] is locked, skip");
                                    continue;
                                }

                                Log.d(TAG, "Auto-solving: piece from [" + row + "," + col + "] to [" + correctRow + "," + correctCol + "]");
                                animateSwap(row, col, correctRow, correctCol);
                                return true;
                            }
                        }
                    }
                }
            }

            Log.d(TAG, "No piece to auto-solve");
            return false;

        } catch (Exception e) {
            Log.e(TAG, "Error in autoSolveOnePiece", e);
            isAnimating = false;
            return false;
        }
    }

    /**
     * Animate swap with CLEAR visual movement
     */
    private void animateSwap(int fromRow, int fromCol, int toRow, int toCol) {
        isAnimating = true;
        animatedPositions.clear();

        PuzzlePiece piece1 = grid[fromRow][fromCol];
        PuzzlePiece piece2 = grid[toRow][toCol];

        float fromX = gridX + fromCol * cellWidth;
        float fromY = gridY + fromRow * cellHeight;
        float toX = gridX + toCol * cellWidth;
        float toY = gridY + toRow * cellHeight;

        Log.d(TAG, "animateSwap: [" + fromRow + "," + fromCol + "] <-> [" + toRow + "," + toCol + "]");

        // Swap in grid immediately
        swapPieces(fromRow, fromCol, toRow, toCol);

        // Animate visual positions
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(800);
        animator.setInterpolator(new OvershootInterpolator(0.8f));

        animator.addUpdateListener(animation -> {
            float progress = (float) animation.getAnimatedValue();

            // Piece1 moves from fromPos to toPos
            if (piece1 != null) {
                float currentX = fromX + (toX - fromX) * progress;
                float currentY = fromY + (toY - fromY) * progress;
                animatedPositions.put(piece1, new PointF(currentX, currentY));
            }

            // Piece2 moves from toPos to fromPos
            if (piece2 != null) {
                float currentX = toX + (fromX - toX) * progress;
                float currentY = toY + (fromY - toY) * progress;
                animatedPositions.put(piece2, new PointF(currentX, currentY));
            }

            invalidate();
        });

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                animatedPositions.clear();
                isAnimating = false;

                // Check connections and locking AFTER swap
                checkLocking();

                // Force redraw to show dimmed/locked state
                invalidate();

                // Notify listener
                if (listener != null) {
                    listener.onPieceConnected();
                }

                // Check if puzzle is completed
                if (isPuzzleComplete()) {
                    showCompletionImage();
                } else if (listener != null) {
                    // Update progress if not completed
                    listener.onProgressChanged();
                }
            }
        });

        animator.start();
    }

    /**
     * SHUFFLE with clear stagger animation
     */
    public boolean shuffleRemainingPieces() {
        if (isAnimating || showingCompletion) return false;

        List<PuzzlePiece> incorrectPieces = new ArrayList<>();
        List<int[]> incorrectPositions = new ArrayList<>();

        for (int row = 0; row < config.gridSize; row++) {
            for (int col = 0; col < config.gridSize; col++) {
                PuzzlePiece piece = grid[row][col];

                if (piece != null) {
                    // ✅ ĐIỀU KIỆN ĐẦY ĐỦ:
                    // 1. Không locked
                    // 2. Không đúng vị trí
                    boolean isLocked = piece.isLocked();
                    boolean isCorrectPosition = (piece.getCorrectRow() == row && piece.getCorrectCol() == col);

                    // Chỉ shuffle nếu cả 2 điều kiện: không locked VÀ không đúng vị trí
                    if (!isLocked && !isCorrectPosition) {
                        incorrectPieces.add(piece);
                        incorrectPositions.add(new int[]{row, col});
                        Log.d(TAG, "Will shuffle piece at [" + row + "," + col + "]");
                    }
                }
            }
        }

        Log.d(TAG, "Found " + incorrectPieces.size() + " pieces to shuffle");

        if (incorrectPieces.isEmpty()) {
            return false;
        }

        animateShuffle(incorrectPieces, incorrectPositions);
        return true;
    }

    /**
     * Animate shuffle with stagger effect
     */
    private void animateShuffle(List<PuzzlePiece> pieces, List<int[]> oldPositions) {
        isAnimating = true;
        animatedPositions.clear();

        // Store old positions
        Map<PuzzlePiece, PointF> startPositions = new HashMap<>();
        for (int i = 0; i < pieces.size(); i++) {
            int[] pos = oldPositions.get(i);
            float x = gridX + pos[1] * cellWidth;
            float y = gridY + pos[0] * cellHeight;
            startPositions.put(pieces.get(i), new PointF(x, y));
        }

        // Shuffle
        Collections.shuffle(pieces);

        // Put back in grid
        for (int i = 0; i < pieces.size(); i++) {
            int[] pos = oldPositions.get(i);
            grid[pos[0]][pos[1]] = pieces.get(i);
        }

        // Calculate new positions
        Map<PuzzlePiece, PointF> endPositions = new HashMap<>();
        for (int i = 0; i < pieces.size(); i++) {
            int[] pos = oldPositions.get(i);
            float x = gridX + pos[1] * cellWidth;
            float y = gridY + pos[0] * cellHeight;
            endPositions.put(pieces.get(i), new PointF(x, y));
        }

        // Animate
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(1000); // Longer = more visible
        animator.setInterpolator(new AccelerateDecelerateInterpolator());

        animator.addUpdateListener(animation -> {
            float progress = (float) animation.getAnimatedValue();

            for (PuzzlePiece piece : pieces) {
                PointF start = startPositions.get(piece);
                PointF end = endPositions.get(piece);

                if (start != null && end != null) {
                    float currentX = start.x + (end.x - start.x) * progress;
                    float currentY = start.y + (end.y - start.y) * progress;
                    animatedPositions.put(piece, new PointF(currentX, currentY));
                }
            }

            invalidate();
        });

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                animatedPositions.clear();
                isAnimating = false;

                checkLocking();
                invalidate();

                if (listener != null) {
                    listener.onProgressChanged();
                }
            }
        });

        animator.start();
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
        return showingCompletion;
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

        invalidate();
    }

    public void cleanup() {
        animatedPositions.clear();

        if (allPieces != null) {
            for (PuzzlePiece piece : allPieces) {
                if (piece != null && piece.getBitmap() != null && !piece.getBitmap().isRecycled()) {
                    piece.getBitmap().recycle();
                }
            }
            allPieces.clear();
        }

        if (fullImage != null && !fullImage.isRecycled()) {
            fullImage.recycle();
            fullImage = null;
        }

        grid = null;
        draggedPiece = null;
        isDragging = false;
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
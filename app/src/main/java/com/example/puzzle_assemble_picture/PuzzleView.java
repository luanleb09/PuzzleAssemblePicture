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
import android.widget.Scroller;
import android.view.VelocityTracker;

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
    private Bitmap fullImage;

    private float gridX, gridY;
    private int gridWidth, gridHeight;
    private int cellWidth, cellHeight;

    private PuzzlePiece draggedPiece;
    private int draggedFromRow, draggedFromCol;
    private float dragOffsetX, dragOffsetY;
    private float draggedPieceX, draggedPieceY;
    private boolean isDragging = false;

    // Selection mode for zoomed swap
    private PuzzlePiece selectedPiece;
    private int selectedRow = -1;
    private int selectedCol = -1;

    private Paint paint;
    private Paint dimPaint;
    private Paint gridPaint;
    private Paint borderPaint;
    private Paint selectedPaint;
    private Paint resetButtonPaint;
    private Paint resetIconPaint;

    private boolean isAnimating = false;
    private boolean showingCompletion = false;

    private Map<PuzzlePiece, PointF> animatedPositions = new HashMap<>();

    // Reset zoom button
    private RectF resetZoomButtonRect;
    private static final float RESET_BUTTON_SIZE = 50f;
    private static final float RESET_BUTTON_WIDTH = 110f;
    private static final float RESET_BUTTON_MARGIN = 10f;



    // Completion animation
    private ValueAnimator completionAnimator;
    private float completionScale = 1.0f;

    // Pan & Zoom
    private ScaleGestureDetector scaleGestureDetector;
    private float scaleFactor = 1.0f;
    private static final float MIN_ZOOM = 1.0f;
    private static final float MAX_ZOOM = 4.0f;
    private static final float ZOOM_THRESHOLD = 1.2f; // Threshold để chuyển sang swap mode

    private float panX = 0f;
    private float panY = 0f;
    private float lastTouchX = 0f;
    private float lastTouchY = 0f;
    private boolean isPanning = false;

    private Scroller scroller;
    private VelocityTracker velocityTracker;
    private static final int MINIMUM_VELOCITY = 50;
    private static final int MAXIMUM_VELOCITY = 8000;

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

        // Paint for selected piece highlight
        selectedPaint = new Paint();
        selectedPaint.setStyle(Paint.Style.STROKE);
        selectedPaint.setStrokeWidth(6);
        selectedPaint.setColor(Color.rgb(255, 215, 0)); // Gold color

        // Paint for reset button
        resetButtonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        resetButtonPaint.setColor(0xAA000000);

        resetIconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        resetIconPaint.setColor(0xFFFFFFFF);
        resetIconPaint.setStyle(Paint.Style.STROKE);
        resetIconPaint.setStrokeWidth(3);
        resetIconPaint.setStrokeCap(Paint.Cap.ROUND);

        allPieces = new ArrayList<>();

        scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleListener());
        scroller = new Scroller(getContext());

        setLayerType(View.LAYER_TYPE_HARDWARE, null);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float oldScale = scaleFactor;
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(MIN_ZOOM, Math.min(scaleFactor, MAX_ZOOM));

            if (scaleFactor != oldScale) {
                float focusX = detector.getFocusX();
                float focusY = detector.getFocusY();

                float contentX = (focusX - getWidth() / 2f - panX) / oldScale;
                float contentY = (focusY - getHeight() / 2f - panY) / oldScale;

                float newContentX = (focusX - getWidth() / 2f - panX) / scaleFactor;
                float newContentY = (focusY - getHeight() / 2f - panY) / scaleFactor;

                panX += (newContentX - contentX) * scaleFactor;
                panY += (newContentY - contentY) * scaleFactor;

                float[] bounds = getPanBounds();
                panX = Math.max(bounds[0], Math.min(bounds[1], panX));
                panY = Math.max(bounds[2], Math.min(bounds[3], panY));
            }

            invalidate();
            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            if (draggedPiece != null && isDragging) {
                isDragging = false;
                draggedPiece = null;
            }
            // Clear selection when starting to zoom
            clearSelection();
            return true;
        }
    }

    public void initPuzzle(Bitmap image, PuzzleConfig config, PuzzleListener listener) {
        this.config = config;
        this.listener = listener;
        this.fullImage = image;
        allPieces.clear();

        int screenWidth = getWidth();
        int screenHeight = getHeight();

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

//        Bitmap scaledImage = Bitmap.createScaledBitmap(image, gridWidth, gridHeight, true);

        float imageAspect = (float) image.getWidth() / image.getHeight();
        float gridAspect = (float) gridWidth / gridHeight;

        Bitmap scaledImage;
        if (Math.abs(imageAspect - gridAspect) < 0.01f) {
            // Aspect ratio gần giống nhau, scale trực tiếp
            scaledImage = Bitmap.createScaledBitmap(image, gridWidth, gridHeight, true);
        } else {
            // Aspect ratio khác nhau, cần crop hoặc fit
            int scaledWidth, scaledHeight;
            int offsetX = 0, offsetY = 0;

            if (imageAspect > gridAspect) {
                // Image rộng hơn, scale theo height
                scaledHeight = gridHeight;
                scaledWidth = (int) (scaledHeight * imageAspect);
                offsetX = (scaledWidth - gridWidth) / 2;
            } else {
                // Image cao hơn, scale theo width
                scaledWidth = gridWidth;
                scaledHeight = (int) (scaledWidth / imageAspect);
                offsetY = (scaledHeight - gridHeight) / 2;
            }

            Bitmap tempScaled = Bitmap.createScaledBitmap(image, scaledWidth, scaledHeight, true);
            scaledImage = Bitmap.createBitmap(tempScaled, offsetX, offsetY, gridWidth, gridHeight);

            if (tempScaled != scaledImage) {
                tempScaled.recycle();
            }
        }

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

        if (grid == null || allPieces == null || allPieces.isEmpty()) {
            return;
        }

        // Update fling animation
        if (scroller.computeScrollOffset()) {
            panX = scroller.getCurrX();
            panY = scroller.getCurrY();
            postInvalidateOnAnimation();
        }

        // If showing completion, draw full image with animation
        if (showingCompletion && fullImage != null) {
            drawCompletionImage(canvas);
            return;
        }

        // Save canvas state
        canvas.save();

        // Apply zoom and pan transformations
        canvas.translate(getWidth() / 2f + panX, getHeight() / 2f + panY);
        canvas.scale(scaleFactor, scaleFactor);
        canvas.translate(-getWidth() / 2f, -getHeight() / 2f);

        // Draw grid outline
        canvas.drawRect(gridX, gridY, gridX + gridWidth, gridY + gridHeight, gridPaint);

        // Draw all pieces in grid
        for (int row = 0; row < config.gridSize; row++) {
            for (int col = 0; col < config.gridSize; col++) {
                PuzzlePiece piece = grid[row][col];
                if (piece != null && piece != draggedPiece) {
                    // Check if piece has animated position
                    if (animatedPositions.containsKey(piece)) {
                        PointF pos = animatedPositions.get(piece);
                        drawPieceAtPosition(canvas, piece, pos.x, pos.y, false);
                    } else {
                        boolean isSelected = (piece == selectedPiece);
                        drawPieceAt(canvas, piece, row, col, isSelected);
                    }
                }
            }
        }

        // Draw dragged piece on top
        if (isDragging && draggedPiece != null) {
            drawPieceAtPosition(canvas, draggedPiece, draggedPieceX, draggedPieceY, false);
        }

        // Restore canvas
        canvas.restore();

        // Draw zoom indicator and reset button
        if (scaleFactor > 1.1f) {
            drawZoomIndicator(canvas);
            drawResetZoomButton(canvas);
            if (scaleFactor > ZOOM_THRESHOLD) {
                drawSwapModeIndicator(canvas);
            }
        }
    }

    private void drawZoomIndicator(Canvas canvas) {
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0xAAFFFFFF);
        textPaint.setTextSize(32);
        textPaint.setTextAlign(Paint.Align.RIGHT);

        Paint bgPaint = new Paint();
        bgPaint.setColor(0x88000000);

        String zoomText = String.format("%.1fx", scaleFactor);
        float textWidth = textPaint.measureText(zoomText);

        // Calculate positions considering the reset button
        float resetButtonEstimatedWidth = textPaint.measureText("Reset") + 30 + RESET_BUTTON_MARGIN * 2;
        float rightEdge = getWidth() - resetButtonEstimatedWidth - 10;

        canvas.drawRoundRect(
                rightEdge - textWidth - 30, 10,
                rightEdge, 60,
                10, 10, bgPaint
        );
        canvas.drawText(zoomText, rightEdge - 15, 45, textPaint);

    }

    private void drawResetZoomButton(Canvas canvas) {
        // Tạo paint cho text trước
        Paint buttonTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        buttonTextPaint.setColor(0xFFFFFFFF);
        buttonTextPaint.setTextSize(26);
        buttonTextPaint.setTextAlign(Paint.Align.CENTER);

        String buttonText = "Reset";
        float textWidth = buttonTextPaint.measureText(buttonText);

        // Tính button size dựa trên text width + padding
        float buttonWidth = textWidth + 30; // 15px padding mỗi bên
        float buttonHeight = 50;

        // Button position
        float right = getWidth() - RESET_BUTTON_MARGIN;
        float left = right - buttonWidth;
        float top = RESET_BUTTON_MARGIN;
        float bottom = top + buttonHeight;

        resetZoomButtonRect = new RectF(left, top, right, bottom);

        // Draw button background
        canvas.drawRoundRect(resetZoomButtonRect, 10, 10, resetButtonPaint);

        // Draw text (XÓA toàn bộ code vẽ icon)
        float centerX = (left + right) / 2;
        float centerY = (top + bottom) / 2;
        float textY = centerY - ((buttonTextPaint.descent() + buttonTextPaint.ascent()) / 2);

        canvas.drawText(buttonText, centerX, textY, buttonTextPaint);
    }

    private void drawCompletionImage(Canvas canvas) {
        // Draw semi-transparent dark background
        canvas.drawColor(0xE0000000);

        // Calculate scaled image dimensions maintaining aspect ratio
        int padding = 80;
        int availableWidth = getWidth() - (padding * 2);
        int availableHeight = getHeight() - (padding * 2);

        float imageAspectRatio = (float) fullImage.getHeight() / fullImage.getWidth();

        int displayWidth = availableWidth;
        int displayHeight = (int) (displayWidth * imageAspectRatio);

        if (displayHeight > availableHeight) {
            displayHeight = availableHeight;
            displayWidth = (int) (displayHeight / imageAspectRatio);
        }

        // Apply completion scale animation
        displayWidth = (int) (displayWidth * completionScale);
        displayHeight = (int) (displayHeight * completionScale);

        float left = (getWidth() - displayWidth) / 2f;
        float top = (getHeight() - displayHeight) / 2f;

        RectF destRect = new RectF(left, top, left + displayWidth, top + displayHeight);

        // Draw the full image (crystal clear, no dimming)
        canvas.drawBitmap(fullImage, null, destRect, paint);

        // Draw border around image
        Paint completionBorderPaint = new Paint();
        completionBorderPaint.setStyle(Paint.Style.STROKE);
        completionBorderPaint.setStrokeWidth(6);
        completionBorderPaint.setColor(0xFFFFD700); // Gold
        canvas.drawRect(destRect, completionBorderPaint);

        // Draw "Puzzle Completed!" text
        Paint completionTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        completionTextPaint.setColor(0xFFFFD700);
        completionTextPaint.setTextSize(48);
        completionTextPaint.setTextAlign(Paint.Align.CENTER);
        completionTextPaint.setFakeBoldText(true);

//        canvas.drawText("Puzzle Completed!", getWidth() / 2f, top - 30, completionTextPaint);
    }

    private void drawSwapModeIndicator(Canvas canvas) {
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0xFFFFD700); // Gold
        textPaint.setTextSize(28);
        textPaint.setTextAlign(Paint.Align.LEFT);

        Paint bgPaint = new Paint();
        bgPaint.setColor(0x88000000);

        String modeText = selectedPiece != null ? "Tap to swap" : "Swap Mode";
        float textWidth = textPaint.measureText(modeText);

        canvas.drawRoundRect(
                10, 10,
                textWidth + 40, 55,
                10, 10, bgPaint
        );
        canvas.drawText(modeText, 25, 40, textPaint);
    }

    private void drawPieceAtPosition(Canvas canvas, PuzzlePiece piece, float x, float y, boolean isSelected) {
        RectF destRect = new RectF(x, y, x + cellWidth, y + cellHeight);

        Paint currentPaint = piece.isLocked() && config.dimLockedPieces ? dimPaint : paint;
        canvas.drawBitmap(piece.getBitmap(), null, destRect, currentPaint);

        if (isSelected) {
            // Draw gold border for selected piece
            canvas.drawRect(destRect, selectedPaint);
        } else if (!piece.isLocked()) {
            borderPaint.setStrokeWidth(2);
            borderPaint.setColor(Color.argb(100, 255, 255, 255));
            canvas.drawRect(destRect, borderPaint);
        }
    }

    private void drawPieceAt(Canvas canvas, PuzzlePiece piece, int row, int col, boolean isSelected) {
        float x = gridX + col * cellWidth;
        float y = gridY + row * cellHeight;
        drawPieceAtPosition(canvas, piece, x, y, isSelected);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (grid == null || allPieces == null || allPieces.isEmpty()) {
            return super.onTouchEvent(event);
        }

        scaleGestureDetector.onTouchEvent(event);

        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }
        velocityTracker.addMovement(event);

        int action = event.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                scroller.forceFinished(true);
                lastTouchX = event.getX();
                lastTouchY = event.getY();

                // Check if reset zoom button is clicked
                if (scaleFactor > 1.1f && resetZoomButtonRect != null &&
                        resetZoomButtonRect.contains(lastTouchX, lastTouchY)) {
                    animateResetZoom();
                    return true;
                }

                // Determine interaction mode based on zoom level
                if (scaleFactor > ZOOM_THRESHOLD) {
                    // Swap mode when zoomed
                    handleSwapModeTouch(getTouchX(lastTouchX), getTouchY(lastTouchY));
                } else {
                    // Drag mode when not zoomed
                    handleTouchDown(getTouchX(lastTouchX), getTouchY(lastTouchY));
                }
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                if (isDragging && draggedPiece != null) {
                    isDragging = false;
                    draggedPiece = null;
                }
                clearSelection();
                isPanning = false;
                break;

            case MotionEvent.ACTION_MOVE:
                float dx = event.getX() - lastTouchX;
                float dy = event.getY() - lastTouchY;

                // Priority 1: If dragging a piece, continue dragging (works at any zoom level < threshold)
                if (isDragging && draggedPiece != null && scaleFactor <= ZOOM_THRESHOLD) {
                    handleTouchMove(getTouchX(event.getX()), getTouchY(event.getY()));
                }
                // Priority 2: Pan mode when zoomed above threshold
                else if (scaleFactor > ZOOM_THRESHOLD && event.getPointerCount() == 1 &&
                        (Math.abs(dx) > 10 || Math.abs(dy) > 10)) {
                    isPanning = true;
                    clearSelection(); // Clear selection when panning

                    float newPanX = panX + dx;
                    float newPanY = panY + dy;

                    float[] bounds = getPanBounds();
                    panX = Math.max(bounds[0], Math.min(bounds[1], newPanX));
                    panY = Math.max(bounds[2], Math.min(bounds[3], newPanY));

                    invalidate();
                }
                // Priority 3: Normal drag when not zoomed much and not already panning
                else if (!isPanning && scaleFactor <= ZOOM_THRESHOLD && !isDragging) {
                    handleTouchMove(getTouchX(event.getX()), getTouchY(event.getY()));
                }

                lastTouchX = event.getX();
                lastTouchY = event.getY();
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // Handle fling for pan mode
                if (isPanning && scaleFactor > ZOOM_THRESHOLD) {
                    velocityTracker.computeCurrentVelocity(1000, MAXIMUM_VELOCITY);
                    float velocityX = velocityTracker.getXVelocity();
                    float velocityY = velocityTracker.getYVelocity();

                    if (Math.abs(velocityX) > MINIMUM_VELOCITY || Math.abs(velocityY) > MINIMUM_VELOCITY) {
                        fling(velocityX, velocityY);
                    }
                    isPanning = false;
                }
                // Handle piece drop for drag mode
                else if (isDragging && draggedPiece != null && scaleFactor <= ZOOM_THRESHOLD) {
                    handleTouchUp(getTouchX(event.getX()), getTouchY(event.getY()));
                }

                if (velocityTracker != null) {
                    velocityTracker.recycle();
                    velocityTracker = null;
                }
                break;

            case MotionEvent.ACTION_POINTER_UP:
                if (event.getPointerCount() > 1) {
                    isPanning = false;
                }
                break;
        }

        return true;
    }

    private void handleSwapModeTouch(float x, float y) {
        if (x < gridX || x >= gridX + gridWidth || y < gridY || y >= gridY + gridHeight) {
            clearSelection();
            return;
        }

        int col = (int) Math.floor((x - gridX) / cellWidth);
        int row = (int) Math.floor((y - gridY) / cellHeight);

        if (row < 0 || row >= config.gridSize || col < 0 || col >= config.gridSize) {
            clearSelection();
            return;
        }

        PuzzlePiece tappedPiece = grid[row][col];
        if (tappedPiece == null) {
            clearSelection();
            return;
        }

        // If no piece selected yet, select this piece
        if (selectedPiece == null) {
            // Cannot select locked pieces
            if (tappedPiece.isLocked()) {
                return;
            }

            selectedPiece = tappedPiece;
            selectedRow = row;
            selectedCol = col;
            vibratePieceShort();
            invalidate();
        } else {
            // A piece is already selected
            if (tappedPiece == selectedPiece) {
                // Tapped same piece, deselect
                clearSelection();
            } else {
                // Swap with selected piece
                if (!tappedPiece.isLocked() && !selectedPiece.isLocked()) {
                    swapPieces(selectedRow, selectedCol, row, col);
                    checkLocking();

                    if (listener != null) {
                        listener.onPieceConnected();
                    }

                    clearSelection();

                    if (isPuzzleComplete()) {
                        showCompletionImage();
                    } else if (listener != null) {
                        listener.onProgressChanged();
                    }
                }
            }
        }
    }

    private void clearSelection() {
        selectedPiece = null;
        selectedRow = -1;
        selectedCol = -1;
        invalidate();
    }

    private float[] getPanBounds() {
        float viewWidth = getWidth();
        float viewHeight = getHeight();

        float scaledWidth = viewWidth * scaleFactor;
        float scaledHeight = viewHeight * scaleFactor;

        float maxPanX = (scaledWidth - viewWidth) / 2f;
        float maxPanY = (scaledHeight - viewHeight) / 2f;

        return new float[] {
                -maxPanX, maxPanX,
                -maxPanY, maxPanY
        };
    }

    private float getTouchX(float screenX) {
        return (screenX - getWidth() / 2f - panX) / scaleFactor + getWidth() / 2f;
    }

    private float getTouchY(float screenY) {
        return (screenY - getHeight() / 2f - panY) / scaleFactor + getHeight() / 2f;
    }

    private void fling(float velocityX, float velocityY) {
        float[] bounds = getPanBounds();

        scroller.fling(
                (int) panX, (int) panY,
                (int) velocityX, (int) velocityY,
                (int) bounds[0], (int) bounds[1],
                (int) bounds[2], (int) bounds[3]
        );

        postInvalidateOnAnimation();
    }

    public void resetPanZoom() {
        scaleFactor = 1.0f;
        panX = 0f;
        panY = 0f;
        clearSelection();
        invalidate();
    }

    private void animateResetZoom() {
        if (isAnimating) return;

        final float startScale = scaleFactor;
        final float startPanX = panX;
        final float startPanY = panY;

        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(300);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());

        animator.addUpdateListener(animation -> {
            float progress = (float) animation.getAnimatedValue();

            scaleFactor = startScale + (1.0f - startScale) * progress;
            panX = startPanX * (1f - progress);
            panY = startPanY * (1f - progress);

            invalidate();
        });

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                scaleFactor = 1.0f;
                panX = 0f;
                panY = 0f;
                clearSelection();
                invalidate();
            }
        });

        clearSelection();
        animator.start();
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

    private void checkLocking() {
        if (!config.autoLockCorrectPieces) {
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
                    }
                }
            }
        }

        if (anyLocked) {
            vibratePiece();
            invalidate();
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

    private void showCompletionImage() {
        showingCompletion = true;
        clearSelection();

        // Start completion zoom animation
        startCompletionAnimation();

        if (listener != null) {
            listener.onPuzzleCompleted();
        }
    }

    private void startCompletionAnimation() {
        if (completionAnimator != null && completionAnimator.isRunning()) {
            completionAnimator.cancel();
        }

        completionAnimator = ValueAnimator.ofFloat(0.8f, 1.05f, 1.0f);
        completionAnimator.setDuration(1200);
        completionAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        completionAnimator.setRepeatCount(ValueAnimator.INFINITE);
        completionAnimator.setRepeatMode(ValueAnimator.REVERSE);

        completionAnimator.addUpdateListener(animation -> {
            completionScale = (float) animation.getAnimatedValue();
            invalidate();
        });

        completionAnimator.start();
    }

    public void hideCompletionImage() {
        showingCompletion = false;
        if (completionAnimator != null && completionAnimator.isRunning()) {
            completionAnimator.cancel();
        }
        completionScale = 1.0f;
        invalidate();
    }

    public boolean autoSolveOnePiece() {
        if (isAnimating || showingCompletion) {
            return false;
        }

        try {
            for (int row = 0; row < config.gridSize; row++) {
                for (int col = 0; col < config.gridSize; col++) {
                    PuzzlePiece piece = grid[row][col];

                    if (piece != null && !piece.isLocked()) {
                        int correctRow = piece.getCorrectRow();
                        int correctCol = piece.getCorrectCol();

                        if (correctRow != row || correctCol != col) {
                            if (correctRow >= 0 && correctRow < config.gridSize &&
                                    correctCol >= 0 && correctCol < config.gridSize) {

                                PuzzlePiece pieceAtCorrectPos = grid[correctRow][correctCol];

                                if (pieceAtCorrectPos != null && pieceAtCorrectPos.isLocked()) {
                                    continue;
                                }

                                clearSelection();
                                animateSwap(row, col, correctRow, correctCol);
                                return true;
                            }
                        }
                    }
                }
            }

            return false;

        } catch (Exception e) {
            Log.e(TAG, "Error in autoSolveOnePiece", e);
            isAnimating = false;
            return false;
        }
    }

    private void animateSwap(int fromRow, int fromCol, int toRow, int toCol) {
        isAnimating = true;
        animatedPositions.clear();

        PuzzlePiece piece1 = grid[fromRow][fromCol];
        PuzzlePiece piece2 = grid[toRow][toCol];

        float fromX = gridX + fromCol * cellWidth;
        float fromY = gridY + fromRow * cellHeight;
        float toX = gridX + toCol * cellWidth;
        float toY = gridY + toRow * cellHeight;

        swapPieces(fromRow, fromCol, toRow, toCol);

        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(800);
        animator.setInterpolator(new OvershootInterpolator(0.8f));

        animator.addUpdateListener(animation -> {
            float progress = (float) animation.getAnimatedValue();

            if (piece1 != null) {
                float currentX = fromX + (toX - fromX) * progress;
                float currentY = fromY + (toY - fromY) * progress;
                animatedPositions.put(piece1, new PointF(currentX, currentY));
            }

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

                checkLocking();
                invalidate();

                if (listener != null) {
                    listener.onPieceConnected();
                }

                if (isPuzzleComplete()) {
                    showCompletionImage();
                } else if (listener != null) {
                    listener.onProgressChanged();
                }
            }
        });

        animator.start();
    }

    public boolean shuffleRemainingPieces() {
        if (isAnimating || showingCompletion) return false;

        clearSelection();

        List<PuzzlePiece> incorrectPieces = new ArrayList<>();
        List<int[]> incorrectPositions = new ArrayList<>();

        for (int row = 0; row < config.gridSize; row++) {
            for (int col = 0; col < config.gridSize; col++) {
                PuzzlePiece piece = grid[row][col];

                if (piece != null) {
                    boolean isLocked = piece.isLocked();
                    boolean isCorrectPosition = (piece.getCorrectRow() == row && piece.getCorrectCol() == col);

                    if (!isLocked && !isCorrectPosition) {
                        incorrectPieces.add(piece);
                        incorrectPositions.add(new int[]{row, col});
                    }
                }
            }
        }

        if (incorrectPieces.isEmpty()) {
            return false;
        }

        animateShuffle(incorrectPieces, incorrectPositions);
        return true;
    }

    private void animateShuffle(List<PuzzlePiece> pieces, List<int[]> oldPositions) {
        isAnimating = true;
        animatedPositions.clear();

        Map<PuzzlePiece, PointF> startPositions = new HashMap<>();
        for (int i = 0; i < pieces.size(); i++) {
            int[] pos = oldPositions.get(i);
            float x = gridX + pos[1] * cellWidth;
            float y = gridY + pos[0] * cellHeight;
            startPositions.put(pieces.get(i), new PointF(x, y));
        }

        Collections.shuffle(pieces);

        for (int i = 0; i < pieces.size(); i++) {
            int[] pos = oldPositions.get(i);
            grid[pos[0]][pos[1]] = pieces.get(i);
        }

        Map<PuzzlePiece, PointF> endPositions = new HashMap<>();
        for (int i = 0; i < pieces.size(); i++) {
            int[] pos = oldPositions.get(i);
            float x = gridX + pos[1] * cellWidth;
            float y = gridY + pos[0] * cellHeight;
            endPositions.put(pieces.get(i), new PointF(x, y));
        }

        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(1000);
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

        clearSelection();

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
        clearSelection();

        if (completionAnimator != null && completionAnimator.isRunning()) {
            completionAnimator.cancel();
            completionAnimator = null;
        }

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

        if (velocityTracker != null) {
            velocityTracker.recycle();
            velocityTracker = null;
        }

        if (scroller != null) {
            scroller.forceFinished(true);
        }
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

    private void vibratePieceShort() {
        if (SettingsActivity.isVibrationEnabled(getContext())) {
            Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(25);
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
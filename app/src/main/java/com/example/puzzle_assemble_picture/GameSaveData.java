package com.example.puzzle_assemble_picture;

import java.util.ArrayList;
import java.util.List;

public class GameSaveData {
    public int level;
    public long timestamp;
    public List<PiecePosition> piecePositions;
    public List<PieceConnection> connections;

    public GameSaveData() {
        piecePositions = new ArrayList<>();
        connections = new ArrayList<>();
        timestamp = System.currentTimeMillis();
    }

    public static class PiecePosition {
        public int correctRow;
        public int correctCol;
        public float x;
        public float y;
        public boolean isLocked;

        public PiecePosition(int correctRow, int correctCol, float x, float y, boolean isLocked) {
            this.correctRow = correctRow;
            this.correctCol = correctCol;
            this.x = x;
            this.y = y;
            this.isLocked = isLocked;
        }
    }

    public static class PieceConnection {
        public int piece1Row;
        public int piece1Col;
        public int piece2Row;
        public int piece2Col;

        public PieceConnection(int piece1Row, int piece1Col, int piece2Row, int piece2Col) {
            this.piece1Row = piece1Row;
            this.piece1Col = piece1Col;
            this.piece2Row = piece2Row;
            this.piece2Col = piece2Col;
        }
    }
}
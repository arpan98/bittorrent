package com.arpan.model;

public class FilePiece {
    private byte[] data;

    public FilePiece(byte[] data) {
        this.data = data;
    }

    public FilePiece() {
        this.data = null;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}

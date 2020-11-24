package com.arpan;

import java.io.*;
import java.nio.file.Files;

public class FileUtil {
    private static final String FOLDER_PATH = "peers/";
    private static final String DATA_FILE= "cfg/Project_Logistics.pdf";
    private String destination;

    FileUtil(String peerId){
        this.destination = FOLDER_PATH+peerId+"/Project_Logistics.pdf";
    }

    public boolean copyContent(String peerId){
        File source = new File(DATA_FILE);
        File dest = new File(destination);
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(source);
            os = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
            return true;

        } catch (Exception ex) {
            System.out.println("Unable to copy file:" + ex.getMessage());
        } finally {
            try {
                is.close();
                os.close();

            } catch (Exception ex) {
            }
            return false;
        }
    }

    public FilePiece[] getPieces(String peerId, int num_pieces, int pieceSize) throws Exception {
        FilePiece[] filePieces = new FilePiece[num_pieces];
        File dataFile = new File(destination);

        if(!dataFile.exists()){
            return filePieces;
        }
        byte[] data = Files.readAllBytes(dataFile.toPath());

        for (int i = 0; i < filePieces.length; i++)
            filePieces[i] = new FilePiece();
        byte[] piece;
        int index = 0;
        for (int i = 0; i < filePieces.length; i++) {
            piece = new byte[pieceSize];
            for (int j = 0; j < pieceSize && index < data.length; j++) {
                piece[j] = data[index++];
            }
            filePieces[i] = new FilePiece(piece);
        }
        return filePieces;
    }

    public void constructFile(Peer peer) throws IOException {
        //writing all bits to file and creating the file

            File outFile = new File(destination);
            FileOutputStream fos = new FileOutputStream(outFile);
            FilePiece[] filePieces = peer.getFilePieces();
            for (int i = 0; i < filePieces.length; i++) {
                fos.write(filePieces[i].getData());

            fos.flush();
            fos.close();

        }

    }
}

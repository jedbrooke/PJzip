package pjzip.src.main.java.pjzip;

import java.io.*;
import java.util.zip.*;

/**
 * PJzip
 */
public class PJzip {

    public final static int BLOCK_SIZE = 131072; // 2^17
    public final static int DICT_SIZE = 32768; // 2^15
    private final static int GZIP_MAGIC = 0x8b1f;
    private final static int TRAILER_SIZE = 8;

    private int num_threads;

    private CRC32 crc = new CRC32();
    private ByteArrayOutputStream out;

    public PJzip(int _threads) {
        num_threads = _threads;
        out = new ByteArrayOutputStream();
    }

    /*
     * Writes GZIP member trailer to a byte array, starting at a given offset.
     */
    private void writeTrailer(long totalBytes, byte[] buf, int offset) throws IOException {
        writeInt((int) crc.getValue(), buf, offset); // CRC-32 of uncompr. data
        writeInt((int) totalBytes, buf, offset + 4); // Number of uncompr. bytes
    }

    /*
     * Writes integer in Intel byte order to a byte array, starting at a given
     * offset.
     */
    private void writeInt(int i, byte[] buf, int offset) throws IOException {
        writeShort(i & 0xffff, buf, offset);
        writeShort((i >> 16) & 0xffff, buf, offset + 2);
    }

    /*
     * Writes short integer in Intel byte order to a byte array, starting at a given
     * offset
     */
    private void writeShort(int s, byte[] buf, int offset) throws IOException {
        buf[offset] = (byte) (s & 0xff);
        buf[offset + 1] = (byte) ((s >> 8) & 0xff);
    }

    private void writeHeader() throws IOException {
        out.write(new byte[] { (byte) GZIP_MAGIC, // Magic number (short)
                (byte) (GZIP_MAGIC >> 8), // Magic number (short)
                Deflater.DEFLATED, // Compression method (CM)
                0, // Flags (FLG)
                0, // Modification time MTIME (int)
                0, // Modification time MTIME (int)
                0, // Modification time MTIME (int)
                0, // Modification time MTIME (int)Sfil
                0, // Extra flags (XFLG)
                0 // Operating system (OS)
        });
    }

    public void compressFile() throws IOException, InterruptedException {
        writeHeader();
        crc.reset();

        long totalBytesRead = 0;

        byte[] blockBuf = new byte[BLOCK_SIZE];

        int num_blocks = 0;

        BlockManager BM = new BlockManager();
        Worker.setBlockManager(BM);

        // start worker threads
        if (num_threads > 1) {
            for (int i = 0; i < num_threads; i++) {
                Thread w = new Thread(new Worker());
                w.start();
            }
        }
        
        int nbytes = System.in.read(blockBuf);

        while (nbytes > 0) {
            
            BM.newFreeBlock(blockBuf, nbytes, num_blocks);
            crc.update(blockBuf, 0, nbytes);

            totalBytesRead += nbytes;
            num_blocks++;
            // System.err.println("read " + num_blocks + " blocks");

            nbytes = System.in.read(blockBuf);
        }
        
        // System.err.println("finished reading " + num_blocks + " blocks");
        BM.finishedReading();
        
        Deflater compressor = new Deflater(Deflater.DEFAULT_COMPRESSION, true);

        if (num_threads < 2) {
            // System.err.println("performing single threaded case");
            Worker w = new Worker();
            w.compressBlocks();
        }

        // start writing the output
        for(int i = 0; i < num_blocks - 1; i++ ) { 
            //will block if data is not ready yet
            Block b = BM.getCompletedBlock(i);
            // System.err.println("writing block " + i);
            // System.err.println(b.getCompressed_size() + " bytes");
            out.write(b.getCompressed(), 0, b.getCompressed_size());
        }
    

        //do the last block on the main thread so we can end cleanly
        Block b = BM.getCompletedBlock(num_blocks - 1);
        // System.err.println("writing block " + b.getSequence_number());

        compressor.reset();
        byte[] dict = BM.getDict(b);
        if (dict != null) {
            compressor.setDictionary(dict);
        }
        compressor.setInput(b.getData(), 0, b.getSize());

        byte[] compressedBlockBuf = new byte[BLOCK_SIZE * 2];

        if (!compressor.finished()) {
            compressor.finish();
            while (!compressor.finished()) {
                int deflatedBytes = compressor.deflate(compressedBlockBuf, 0, compressedBlockBuf.length, Deflater.NO_FLUSH);
                if (deflatedBytes > 0) {
                    out.write(compressedBlockBuf, 0, deflatedBytes);
                }
            }
        }
        
        // System.err.println("crc: " + crc.getValue());
        // System.err.println("total bytes read: " + totalBytesRead);

        byte[] trailerBuf = new byte[TRAILER_SIZE];
        writeTrailer(totalBytesRead, trailerBuf, 0);
        out.write(trailerBuf);
        out.writeTo(System.out);

        out.flush();
        out.close();
        System.out.flush();
        System.out.close();
    }

    public static int test() {
        return 5;
    }

    public static void main(String[] args) {
        int num_threads = Runtime.getRuntime().availableProcessors();
        if (args.length > 0) {
            if (args[0].equals("-p")) {
                if (args.length != 2) {
                    System.err.println("Error: -p argument requires exactly 1 argument");
                    System.exit(1);
                }
                num_threads = Integer.parseInt(args[1]);
                if (num_threads < 1) {
                    num_threads = 1;
                }
            }
        }

        // System.err.println("using " + num_threads + " threads");
        long mem = Runtime.getRuntime().maxMemory();
        System.err.println("max mem: " + (double)mem / (1 << 30) + "G");

        PJzip compressor = new PJzip(num_threads);

        // do some basic zlib compression
        try {
            compressor.compressFile();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.exit(3);
        }

    }

}
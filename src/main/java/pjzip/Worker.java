package pjzip;

import java.util.zip.Deflater;

/**
 * Worker
 */
public class Worker implements Runnable {

    private static BlockManager BM;

    public static void setBlockManager(BlockManager _BM) {
        BM = _BM;
    }

    private Deflater compressor;

    public Worker() {
        compressor = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
    }

    public Worker(Deflater deflater) {
        compressor = deflater;
    }

    public void compressBlocks() {
        boolean there_are_blocks_left = true;
        while (there_are_blocks_left) {
            Block b;
            try {
                b = BM.getNextFreeBlock();
                if (b == null) {
                    there_are_blocks_left = false;
                } else {
                    // do the compression
                    compressor.reset();

                    byte[] dict = BM.getDict(b);
                    if (dict != null) {
                        compressor.setDictionary(dict);
                    }
                    compressor.setInput(b.getData(), 0, b.getSize());

                    // System.err.println("compressing block " + b.getSequence_number());
                    int deflated_bytes = compressor.deflate(b.getCompressed(), 0, b.getCompressed().length,
                            Deflater.SYNC_FLUSH);
                    if(deflated_bytes > 0) {
                        b.setCompressed_size(deflated_bytes);
                    }
                    
                    BM.submitCompletedBlock(b);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        // System.err.println(Thread.currentThread().getName() + " is done");
    }

    public void run() {
        compressBlocks();
    }

}
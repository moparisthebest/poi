/* ====================================================================
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */
package com.moparisthebest.poi.poifs.crypt;

import static com.moparisthebest.poi.poifs.crypt.Decryptor.DEFAULT_POIFS_ENTRY;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.util.BitSet;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.ShortBufferException;

import com.moparisthebest.poi.EncryptedDocumentException;
import com.moparisthebest.poi.poifs.filesystem.DirectoryNode;
import com.moparisthebest.poi.poifs.filesystem.POIFSWriterEvent;
import com.moparisthebest.poi.poifs.filesystem.POIFSWriterListener;
import com.moparisthebest.poi.util.IOUtils;
import com.moparisthebest.poi.util.Internal;
import com.moparisthebest.poi.util.LittleEndian;
import com.moparisthebest.poi.util.LittleEndianConsts;
import com.moparisthebest.poi.util.POILogFactory;
import com.moparisthebest.poi.util.POILogger;
import com.moparisthebest.poi.util.TempFile;

@Internal
public abstract class ChunkedCipherOutputStream extends FilterOutputStream {
    private static final POILogger LOG = POILogFactory.getLogger(ChunkedCipherOutputStream.class);
    private static final int STREAMING = -1;

    private final int chunkSize;
    private final int chunkBits;

    private final byte[] chunk;
    private final BitSet plainByteFlags;
    private final File fileOut;
    private final DirectoryNode dir;

    private long pos;
    private long totalPos;
    private long written;
    
    // the cipher can't be final, because for the last chunk we change the padding
    // and therefore need to change the cipher too
    private Cipher cipher;
    private boolean isClosed = false;

    public ChunkedCipherOutputStream(DirectoryNode dir, int chunkSize) throws IOException, GeneralSecurityException {
        super(null);
        this.chunkSize = chunkSize;
        int cs = chunkSize == STREAMING ? 4096 : chunkSize;
        this.chunk = new byte[cs];
        this.plainByteFlags = new BitSet(cs);
        this.chunkBits = Integer.bitCount(cs-1);
        this.fileOut = TempFile.createTempFile("encrypted_package", "crypt");
        this.fileOut.deleteOnExit();
        this.out = new FileOutputStream(fileOut);
        this.dir = dir;
        this.cipher = initCipherForBlock(null, 0, false);
    }

    public ChunkedCipherOutputStream(OutputStream stream, int chunkSize) throws IOException, GeneralSecurityException {
        super(stream);
        this.chunkSize = chunkSize;
        int cs = chunkSize == STREAMING ? 4096 : chunkSize;
        this.chunk = new byte[cs];
        this.plainByteFlags = new BitSet(cs);
        this.chunkBits = Integer.bitCount(cs-1);
        this.fileOut = null;
        this.dir = null;
        this.cipher = initCipherForBlock(null, 0, false);
    }

    public final Cipher initCipherForBlock(int block, boolean lastChunk) throws IOException, GeneralSecurityException {
        return initCipherForBlock(cipher, block, lastChunk);
    }

    protected abstract Cipher initCipherForBlock(Cipher existing, int block, boolean lastChunk)
    throws IOException, GeneralSecurityException;

    protected abstract void calculateChecksum(File fileOut, int oleStreamSize)
    throws GeneralSecurityException, IOException;

    protected abstract void createEncryptionInfoEntry(DirectoryNode dir, File tmpFile)
    throws IOException, GeneralSecurityException;

    @Override
    public void write(int b) throws IOException {
        write(new byte[]{(byte)b});
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        write(b, off, len, false);
    }

    public void writePlain(byte[] b, int off, int len) throws IOException {
        write(b, off, len, true);
    }
    
    protected void write(byte[] b, int off, int len, boolean writePlain) throws IOException {
        if (len == 0) {
            return;
        }

        if (len < 0 || b.length < off+len) {
            throw new IOException("not enough bytes in your input buffer");
        }

        final int chunkMask = getChunkMask();
        while (len > 0) {
            int posInChunk = (int)(pos & chunkMask);
            int nextLen = Math.min(chunk.length-posInChunk, len);
            System.arraycopy(b, off, chunk, posInChunk, nextLen);
            if (writePlain) {
                plainByteFlags.set(posInChunk, posInChunk+nextLen);
            }
            pos += nextLen;
            totalPos += nextLen;
            off += nextLen;
            len -= nextLen;
            if ((pos & chunkMask) == 0) {
                writeChunk(len > 0);
            }
        }
    }

    protected int getChunkMask() {
        return chunk.length-1;
    }

    protected void writeChunk(boolean continued) throws IOException {
        if (pos == 0 || totalPos == written) {
            return;
        }

        int posInChunk = (int)(pos & getChunkMask());

        // normally posInChunk is 0, i.e. on the next chunk (-> index-1)
        // but if called on close(), posInChunk is somewhere within the chunk data
        int index = (int)(pos >> chunkBits);
        boolean lastChunk;
        if (posInChunk==0) {
            index--;
            posInChunk = chunk.length;
            lastChunk = false;
        } else {
            // pad the last chunk
            lastChunk = true;
        }

        int ciLen;
        try {
            boolean doFinal = true;
            long oldPos = pos;
            // reset stream (not only) in case we were interrupted by plain stream parts
            // this also needs to be set to prevent an endless loop
            pos = 0;
            if (chunkSize == STREAMING) {
                if (continued) {
                    doFinal = false;
                }
            } else {
                cipher = initCipherForBlock(cipher, index, lastChunk);
                // restore pos - only streaming chunks will be reset
                pos = oldPos;
            }
            ciLen = invokeCipher(posInChunk, doFinal);
        } catch (GeneralSecurityException e) {
            throw new IOException("can't re-/initialize cipher", e);
        }

        out.write(chunk, 0, ciLen);
        plainByteFlags.clear();
        written += ciLen;
    }

    /**
     * Helper function for overriding the cipher invocation, i.e. XOR doesn't use a cipher
     * and uses it's own implementation
     *
     * @throws BadPaddingException 
     * @throws IllegalBlockSizeException 
     * @throws ShortBufferException
     */
    protected int invokeCipher(int posInChunk, boolean doFinal) throws GeneralSecurityException {
        byte plain[] = (plainByteFlags.isEmpty()) ? null : chunk.clone();

        int ciLen = (doFinal)
            ? cipher.doFinal(chunk, 0, posInChunk, chunk)
            : cipher.update(chunk, 0, posInChunk, chunk);
        
        for (int i = plainByteFlags.nextSetBit(0); i >= 0 && i < posInChunk; i = plainByteFlags.nextSetBit(i+1)) {
            chunk[i] = plain[i];
        }
        
        return ciLen;
    }
    
    @Override
    public void close() throws IOException {
        if (isClosed) {
            LOG.log(POILogger.DEBUG, "ChunkedCipherOutputStream was already closed - ignoring");
            return;
        }

        isClosed = true;

        try {
            writeChunk(false);

            super.close();

            if (fileOut != null) {
                int oleStreamSize = (int)(fileOut.length()+LittleEndianConsts.LONG_SIZE);
                calculateChecksum(fileOut, (int)pos);
                dir.createDocument(DEFAULT_POIFS_ENTRY, oleStreamSize, new EncryptedPackageWriter());
                createEncryptionInfoEntry(dir, fileOut);
            }
        } catch (GeneralSecurityException e) {
            throw new IOException(e);
        }
    }
    
    protected byte[] getChunk() {
        return chunk;
    }

    protected BitSet getPlainByteFlags() {
        return plainByteFlags;
    }

    protected long getPos() {
        return pos;
    }

    protected long getTotalPos() {
        return totalPos;
    }

    /**
     * Some ciphers (actually just XOR) are based on the record size,
     * which needs to be set before encryption
     *
     * @param recordSize the size of the next record
     * @param isPlain {@code true} if the record is unencrypted
     */
    public void setNextRecordSize(int recordSize, boolean isPlain) {
    }
    
    private class EncryptedPackageWriter implements POIFSWriterListener {
        @Override
        public void processPOIFSWriterEvent(POIFSWriterEvent event) {
            try {
                OutputStream os = event.getStream();

                // StreamSize (8 bytes): An unsigned integer that specifies the number of bytes used by data
                // encrypted within the EncryptedData field, not including the size of the StreamSize field.
                // Note that the actual size of the \EncryptedPackage stream (1) can be larger than this
                // value, depending on the block size of the chosen encryption algorithm
                byte buf[] = new byte[LittleEndianConsts.LONG_SIZE];
                LittleEndian.putLong(buf, 0, pos);
                os.write(buf);

                FileInputStream fis = new FileInputStream(fileOut);
                try {
                    IOUtils.copy(fis, os);
                } finally {
                    fis.close();
                }

                os.close();

                if (!fileOut.delete()) {
                    LOG.log(POILogger.ERROR, "Can't delete temporary encryption file: "+fileOut);
                }
            } catch (IOException e) {
                throw new EncryptedDocumentException(e);
            }
        }
    }
}

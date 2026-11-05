package divstar.ico4a.codec.bmp;

import android.graphics.Bitmap;
import android.graphics.Color;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import divstar.ico4a.io.CountingInputStream;
import divstar.ico4a.io.LittleEndianInputStream;

/**
 * Decodes images in BMP format.
 *
 * @author Ian McDonagh
 * @author Igor Tykhyy
 */
public class BMPDecoder {

    private Bitmap img;
    private InfoHeader infoHeader;

    /**
     * Creates a new instance of BMPDecoder and reads the BMP data from the source.
     *
     * @param in the source <tt>InputStream</tt> from which to read the BMP data
     * @throws IOException if an error occurs
     */
    public BMPDecoder(InputStream in) throws IOException {
        LittleEndianInputStream lis = new LittleEndianInputStream(new CountingInputStream(in));
            
    /* header [14] */

        //signature "BM" [2]
        byte[] bsignature = new byte[2];
        lis.read(bsignature);
        String signature = new String(bsignature, "UTF-8");

        if (!signature.equals("BM")) {
            throw new IOException("Invalid signature '" + signature + "' for BMP format");
        }

        //file size [4]
        lis.readIntLE();

        //reserved = 0 [4]
        lis.readIntLE();

        //DataOffset [4] file offset to raster data
        lis.readIntLE();
    
    /* info header [40] */

        infoHeader = readInfoHeader(lis);
    
    /* Color table and Raster data */

        img = read(infoHeader, lis);
    }

    /**
     * Retrieves a bit from the lowest order byte of the given integer.
     *
     * @param bits  the source integer, treated as an unsigned byte
     * @param index the index of the bit to retrieve, which must be in the range <tt>0..7</tt>.
     * @return the bit at the specified index, which will be either <tt>0</tt> or <tt>1</tt>.
     */
    private static int getBit(int bits, int index) {
        return (bits >> (7 - index)) & 1;
    }

    /**
     * Retrieves a nibble (4 bits) from the lowest order byte of the given integer.
     *
     * @param nibbles the source integer, treated as an unsigned byte
     * @param index   the index of the nibble to retrieve, which must be in the range <tt>0..1</tt>.
     * @return the nibble at the specified index, as an unsigned byte.
     */
    private static int getNibble(int nibbles, int index) {
        return (nibbles >> (4 * (1 - index))) & 0xF;
    }

    private static void getColorTable(ColorEntry[] colorTable, int[] ar, int[] ag, int[] ab) {
        for (int i = 0; i < colorTable.length; i++) {
            ar[i] = colorTable[i].bRed & 0xFF;
            ag[i] = colorTable[i].bGreen & 0xFF;
            ab[i] = colorTable[i].bBlue & 0xFF;
        }
    }

    /**
     * Reads the BMP info header structure from the given <tt>InputStream</tt>.
     *
     * @param lis the <tt>InputStream</tt> to read
     * @return the <tt>InfoHeader</tt> structure
     * @throws IOException if an error occurred
     */
    public static InfoHeader readInfoHeader(LittleEndianInputStream lis) throws IOException {
        return new InfoHeader(lis);
    }

    public static InfoHeader readInfoHeader(LittleEndianInputStream lis, int infoSize) throws IOException {
        return new InfoHeader(lis, infoSize);
    }

    /**
     * Reads the BMP data from the given <tt>InputStream</tt> using the information
     * contained in the <tt>InfoHeader</tt>.
     *
     * @param lis        the source input
     * @param infoHeader an <tt>InfoHeader</tt> that was read by a call to
     *                   {@link #readInfoHeader(net.sf.image4j.io.LittleEndianInputStream) readInfoHeader()}.
     * @return the decoded image read from the source input
     * @throws IOException if an error occurs
     */
    public static Bitmap read(InfoHeader infoHeader, LittleEndianInputStream lis) throws IOException {
        Bitmap img;

    /* Color table (palette) */

        ColorEntry[] colorTable = null;

        //color table is only present for 1, 4 or 8 bit (indexed) images
        if (infoHeader.sBitCount <= 8) {
            colorTable = readColorTable(infoHeader, lis);
        }

        img = read(infoHeader, lis, colorTable);

        return img;
    }

    /**
     * Reads the BMP data from the given <tt>InputStream</tt> using the information
     * contained in the <tt>InfoHeader</tt>.
     *
     * @param colorTable <tt>ColorEntry</tt> array containing palette
     * @param infoHeader an <tt>InfoHeader</tt> that was read by a call to
     *                   {@link #readInfoHeader(net.sf.image4j.io.LittleEndianInputStream) readInfoHeader()}.
     * @param lis        the source input
     * @return the decoded image read from the source input
     * @throws IOException if any error occurs
     */
    public static Bitmap read(InfoHeader infoHeader, LittleEndianInputStream lis, ColorEntry[] colorTable) throws IOException {

        switch(infoHeader.iCompression) {

            case BMPConstants.BI_RGB:

                switch(infoHeader.sBitCount) {
                    case 1: return read1(infoHeader, lis, colorTable);
                    case 4: return read4(infoHeader, lis, colorTable);
                    case 8: return read8(infoHeader, lis, colorTable);
                    case 16: return read16(infoHeader, lis);
                    case 24: return read24(infoHeader, lis);
                    case 32: return read32(infoHeader, lis);
                    default: throw new IOException("Unrecognized bitmap format: bit count=" + infoHeader.sBitCount);
                }

            case BMPConstants.BI_BITFIELDS:

                switch(infoHeader.sBitCount) {
                    case 16: return read16BitFields(infoHeader, lis);
                    case 32: return read32BitFields(infoHeader, lis);
                    default: throw new IOException("Unrecognized bitmap format: bit count=" + infoHeader.sBitCount);
                }

            default:

                throw new IOException("Unsupported bitmap format: compression=" + infoHeader.iCompression);
        }
    }

    /**
     * Reads the <tt>ColorEntry</tt> table from the given <tt>InputStream</tt> using
     * the information contained in the given <tt>infoHeader</tt>.
     *
     * @param infoHeader the <tt>InfoHeader</tt> structure, which was read using
     *                   {@link #readInfoHeader(net.sf.image4j.io.LittleEndianInputStream) readInfoHeader()}
     * @param lis        the <tt>InputStream</tt> to read
     * @return the decoded image read from the source input
     * @throws IOException if an error occurs
     */
    public static ColorEntry[] readColorTable(InfoHeader infoHeader, LittleEndianInputStream lis) throws IOException {
        ColorEntry[] colorTable = new ColorEntry[infoHeader.iNumColors];
        for (int i = 0; i < infoHeader.iNumColors; i++) {
            ColorEntry ce = new ColorEntry(lis);
            colorTable[i] = ce;
        }
        return colorTable;
    }

    /**
     * Reads 1-bit uncompressed bitmap raster data, which may be monochrome depending on the
     * palette entries in <tt>colorTable</tt>.
     *
     * @param infoHeader the <tt>InfoHeader</tt> structure, which was read using
     *                   {@link #readInfoHeader(net.sf.image4j.io.LittleEndianInputStream) readInfoHeader()}
     * @param lis        the source input
     * @param colorTable <tt>ColorEntry</tt> array specifying the palette, which
     *                   must not be <tt>null</tt>.
     * @return the decoded image read from the source input
     * @throws IOException if an error occurs
     */
    public static Bitmap read1(InfoHeader infoHeader, LittleEndianInputStream lis, ColorEntry[] colorTable) throws IOException {
        //1 bit per pixel or 8 pixels per byte
        //each pixel specifies the palette index

        int[] ar = new int[colorTable.length];
        int[] ag = new int[colorTable.length];
        int[] ab = new int[colorTable.length];

        getColorTable(colorTable, ar, ag, ab);

        // Create indexed image
        Bitmap img = Bitmap.createBitmap(infoHeader.iWidth, infoHeader.iHeight, Bitmap.Config.ARGB_8888);
        // We'll use the raster to set samples instead of RGB values.
        // The SampleModel of an indexed image interprets samples as
        // the index of the colour for a pixel, which is perfect for use here.

        //padding
        int bitsPerLine = infoHeader.iWidth;
        if (bitsPerLine % 32 != 0) {
            bitsPerLine = (bitsPerLine / 32 + 1) * 32;
        }

        int bytesPerLine = bitsPerLine / 8;
        int[] line = new int[bytesPerLine];

        for (int y = infoHeader.iHeight - 1; y >= 0; y--) {
            for (int i = 0; i < bytesPerLine; i++) {
                line[i] = lis.readUnsignedByte();
            }

            for (int x = 0; x < infoHeader.iWidth; x++) {
                int i = x / 8;
                int v = line[i];
                int b = x % 8;
                int index = getBit(v, b);
                img.setPixel(x, y, Color.rgb(ar[index], ag[index], ab[index]));
            }
        }

        return img;
    }

    /**
     * Reads 4-bit uncompressed bitmap raster data, which is interpreted based on the colours
     * specified in the palette.
     *
     * @param infoHeader the <tt>InfoHeader</tt> structure, which was read using
     *                   {@link #readInfoHeader(net.sf.image4j.io.LittleEndianInputStream) readInfoHeader()}
     * @param lis        the source input
     * @param colorTable <tt>ColorEntry</tt> array specifying the palette, which
     *                   must not be <tt>null</tt>.
     * @return the decoded image read from the source input
     * @throws IOException if an error occurs
     */
    public static Bitmap read4(InfoHeader infoHeader, LittleEndianInputStream lis, ColorEntry[] colorTable) throws IOException {

        // 2 pixels per byte or 4 bits per pixel.
        // Colour for each pixel specified by the color index in the pallette.

        int[] ar = new int[colorTable.length];
        int[] ag = new int[colorTable.length];
        int[] ab = new int[colorTable.length];

        getColorTable(colorTable, ar, ag, ab);

        Bitmap img = Bitmap.createBitmap(infoHeader.iWidth, infoHeader.iHeight, Bitmap.Config.ARGB_8888);

        //padding
        int bitsPerLine = infoHeader.iWidth * 4;
        if (bitsPerLine % 32 != 0) {
            bitsPerLine = (bitsPerLine / 32 + 1) * 32;
        }
        int bytesPerLine = bitsPerLine / 8;

        int[] line = new int[bytesPerLine];

        for (int y = infoHeader.iHeight - 1; y >= 0; y--) {
            //scan line
            for (int i = 0; i < bytesPerLine; i++) {
                int b = lis.readUnsignedByte();
                line[i] = b;
            }

            //get pixels
            for (int x = 0; x < infoHeader.iWidth; x++) {
                //get byte index for line
                int b = x / 2; // 2 pixels per byte
                int i = x % 2;
                int n = line[b];
                int index = getNibble(n, i);
                img.setPixel(x, y, Color.rgb(ar[index], ag[index], ab[index]));
            }
        }

        return img;
    }

    /**
     * Reads 8-bit uncompressed bitmap raster data, which is interpreted based on the colours
     * specified in the palette.
     *
     * @param infoHeader the <tt>InfoHeader</tt> structure, which was read using
     *                   {@link #readInfoHeader(net.sf.image4j.io.LittleEndianInputStream) readInfoHeader()}
     * @param lis        the source input
     * @param colorTable <tt>ColorEntry</tt> array specifying the palette, which
     *                   must not be <tt>null</tt>.
     * @return the decoded image read from the source input
     * @throws IOException if an error occurs
     */
    public static Bitmap read8(InfoHeader infoHeader, LittleEndianInputStream lis, ColorEntry[] colorTable) throws IOException {
        //1 byte per pixel
        //  color index 1 (index of color in palette)
        //lines padded to nearest 32bits
        //no alpha

        int[] ar = new int[colorTable.length];
        int[] ag = new int[colorTable.length];
        int[] ab = new int[colorTable.length];

        getColorTable(colorTable, ar, ag, ab);

        Bitmap img = Bitmap.createBitmap(infoHeader.iWidth, infoHeader.iHeight, Bitmap.Config.ARGB_8888);

        //create color pallette
        int[] c = new int[infoHeader.iNumColors];
        for (int i = 0; i < c.length; i++) {
            int r = colorTable[i].bRed;
            int g = colorTable[i].bGreen;
            int b = colorTable[i].bBlue;
            c[i] = (r << 16) | (g << 8) | (b);
        }

        //padding
        int dataPerLine = infoHeader.iWidth;
        int bytesPerLine = dataPerLine;
        if (bytesPerLine % 4 != 0) {
            bytesPerLine = (bytesPerLine / 4 + 1) * 4;
        }
        int padBytesPerLine = bytesPerLine - dataPerLine;

        for (int y = infoHeader.iHeight - 1; y >= 0; y--) {
            for (int x = 0; x < infoHeader.iWidth; x++) {
                int b = lis.readUnsignedByte();
                img.setPixel(x, y, Color.argb(255, Color.red(c[b]), Color.green(c[b]), Color.blue(c[b])));
            }

            lis.skip(padBytesPerLine);
        }

        return img;
    }

    /**
     * Reads 16-bit uncompressed bitmap raster data.
     *
     * @param lis        the source input
     * @param infoHeader the <tt>InfoHeader</tt> structure, which was read using
     *                   {@link #readInfoHeader(net.sf.image4j.io.LittleEndianInputStream) readInfoHeader()}
     * @return the decoded image read from the source input
     * @throws IOException if an error occurs
     */
    public static Bitmap read16(InfoHeader infoHeader, LittleEndianInputStream lis) throws IOException {

        if(infoHeader.iCompression != BMPConstants.BI_RGB)
            throw new IOException("Expected BI_RGB compression but got " + infoHeader.iCompression + ".");

        Bitmap img = Bitmap.createBitmap(infoHeader.iWidth, infoHeader.iHeight, Bitmap.Config.ARGB_8888);

        int rowPadding = (4 - infoHeader.iWidth * 2 % 4) % 4;

        for (int y = infoHeader.iHeight - 1; y >= 0; --y) {

            for (int x = 0; x < infoHeader.iWidth; ++x) {

                int pixel = lis.readUnsignedShortLE();

                int r = Math.round((float) ((pixel & 0b0111110000000000) >> 10) / 31 * 255);
                int g = Math.round((float) ((pixel & 0b0000001111100000) >> 05) / 31 * 255);
                int b = Math.round((float) ((pixel & 0b0000000000011111) >> 00) / 31 * 255);

                img.setPixel(x, y, Color.argb(255, r, g, b));
            }

            lis.skip(rowPadding, true);
        }

        return img;
    }

    /**
     * Reads 24-bit uncompressed bitmap raster data.
     *
     * @param lis        the source input
     * @param infoHeader the <tt>InfoHeader</tt> structure, which was read using
     *                   {@link #readInfoHeader(net.sf.image4j.io.LittleEndianInputStream) readInfoHeader()}
     * @return the decoded image read from the source input
     * @throws IOException if an error occurs
     */
    public static Bitmap read24(InfoHeader infoHeader, LittleEndianInputStream lis) throws IOException {
        //3 bytes per pixel
        //  blue 1
        //  green 1
        //  red 1
        // lines padded to nearest 32 bits
        // no alpha

        Bitmap img = Bitmap.createBitmap(infoHeader.iWidth, infoHeader.iHeight, Bitmap.Config.ARGB_8888);

        //padding to nearest 32 bits
        int dataPerLine = infoHeader.iWidth * 3;
        int bytesPerLine = dataPerLine;
        if (bytesPerLine % 4 != 0) {
            bytesPerLine = (bytesPerLine / 4 + 1) * 4;
        }
        int padBytesPerLine = bytesPerLine - dataPerLine;

        for (int y = infoHeader.iHeight - 1; y >= 0; y--) {
            for (int x = 0; x < infoHeader.iWidth; x++) {
                int b = lis.readUnsignedByte();
                int g = lis.readUnsignedByte();
                int r = lis.readUnsignedByte();

                img.setPixel(x, y, Color.rgb(r, g, b));
            }
            lis.skip(padBytesPerLine);
        }

        return img;
    }

    /**
     * Reads 32-bit uncompressed bitmap raster data, with transparency.
     *
     * @param lis        the source input
     * @param infoHeader the <tt>InfoHeader</tt> structure, which was read using
     *                   {@link #readInfoHeader(net.sf.image4j.io.LittleEndianInputStream) readInfoHeader()}
     * @return the decoded image read from the source input
     * @throws IOException if an error occurs
     */
    public static Bitmap read32(InfoHeader infoHeader, LittleEndianInputStream lis) throws IOException {
        //4 bytes per pixel
        // blue 1
        // green 1
        // red 1
        // alpha 1
        //No padding since each pixel = 32 bits

        Bitmap img = Bitmap.createBitmap(infoHeader.iWidth, infoHeader.iHeight, Bitmap.Config.ARGB_8888);

        for (int y = infoHeader.iHeight - 1; y >= 0; y--) {
            for (int x = 0; x < infoHeader.iWidth; x++) {
                int b = lis.readUnsignedByte();
                int g = lis.readUnsignedByte();
                int r = lis.readUnsignedByte();
                int a = lis.readUnsignedByte();
                img.setPixel(x, y, Color.argb(a, r, g, b));
            }
        }

        return img;
    }

    /**
     * Reads 16-bit uncompressed bitmap raster data, with a bit mask for each color channel.
     *
     * @param lis        the source input
     * @param infoHeader the <tt>InfoHeader</tt> structure, which was read using
     *                   {@link #readInfoHeader(net.sf.image4j.io.LittleEndianInputStream) readInfoHeader()} and
     *                   has {@link InfoHeader#iCompression} set to {@link BMPConstants#BI_BITFIELDS}.
     * @return the decoded image read from the source input
     * @throws IOException if an error occurs
     */
    public static Bitmap read16BitFields(InfoHeader infoHeader, LittleEndianInputStream lis) throws IOException {

        if(infoHeader.iCompression != BMPConstants.BI_BITFIELDS)
            throw new IOException("Expected BI_BITFIELDS compression but got " + infoHeader.iCompression + ".");

        Bitmap img = Bitmap.createBitmap(infoHeader.iWidth, infoHeader.iHeight, Bitmap.Config.ARGB_8888);

        // Read masks.

        long rm = lis.readUnsignedIntLE();
        long gm = lis.readUnsignedIntLE();
        long bm = lis.readUnsignedIntLE();

        // Offset of mask (at which bit its color channel starts in a pixel).

        int rs = Long.numberOfTrailingZeros(rm);
        int gs = Long.numberOfTrailingZeros(gm);
        int bs = Long.numberOfTrailingZeros(bm);

        // Length of mask (the number of bits in its color channel).

        int rl = Long.bitCount(rm);
        int gl = Long.bitCount(gm);
        int bl = Long.bitCount(bm);

        // Range of mask (the highest possible number in its color channel).

        int rr = (1 << rl) - 1;
        int gr = (1 << gl) - 1;
        int br = (1 << bl) - 1;

        int rowPadding = (4 - infoHeader.iWidth * 2 % 4) % 4;

        for (int y = infoHeader.iHeight - 1; y >= 0; --y) {

            for (int x = 0; x < infoHeader.iWidth; ++x) {

                int pixel = lis.readUnsignedShortLE();

                // Mask and shift the color channel from the pixel and then scale it into 8-bit representation.

                int r = Math.round((float) ((pixel & rm) >> rs) / rr * 255);
                int g = Math.round((float) ((pixel & gm) >> gs) / gr * 255);
                int b = Math.round((float) ((pixel & bm) >> bs) / br * 255);

                img.setPixel(x, y, Color.argb(255, r, g, b));
            }

            lis.skip(rowPadding, true);
        }

        return img;
    }

    /**
     * Reads 32-bit uncompressed bitmap raster data, with transparency and a bit mask for each color channel.
     *
     * @param lis        the source input
     * @param infoHeader the <tt>InfoHeader</tt> structure, which was read using
     *                   {@link #readInfoHeader(net.sf.image4j.io.LittleEndianInputStream) readInfoHeader()} and
     *                   has {@link InfoHeader#iCompression} set to {@link BMPConstants#BI_BITFIELDS}.
     * @return the decoded image read from the source input
     * @throws IOException if an error occurs
     */
    public static Bitmap read32BitFields(InfoHeader infoHeader, LittleEndianInputStream lis) throws IOException {

        if(infoHeader.iCompression != BMPConstants.BI_BITFIELDS)
            throw new IOException("Expected BI_BITFIELDS compression but got " + infoHeader.iCompression + ".");

        Bitmap img = Bitmap.createBitmap(infoHeader.iWidth, infoHeader.iHeight, Bitmap.Config.ARGB_8888);

        // Read masks.

        long rm = lis.readUnsignedIntLE();
        long gm = lis.readUnsignedIntLE();
        long bm = lis.readUnsignedIntLE();
        long am = ~(rm | gm | bm) & 0xffffffffl;

        // Offset of mask (at which bit its color channel starts in a pixel).

        int rs = Long.numberOfTrailingZeros(rm);
        int gs = Long.numberOfTrailingZeros(gm);
        int bs = Long.numberOfTrailingZeros(bm);
        int as = Long.numberOfTrailingZeros(am);

        // Length of mask (the number of bits in its color channel).

        int rl = Long.bitCount(rm);
        int gl = Long.bitCount(gm);
        int bl = Long.bitCount(bm);
        int al = Long.bitCount(am);

        // Range of mask (the highest possible number in its color channel).

        int rr = (1 << rl) - 1;
        int gr = (1 << gl) - 1;
        int br = (1 << bl) - 1;
        int ar = (1 << al) - 1;

        for (int y = infoHeader.iHeight - 1; y >= 0; --y) {

            for (int x = 0; x < infoHeader.iWidth; ++x) {

                long pixel = lis.readUnsignedIntLE();

                // Mask and shift the color channel from the pixel and then scale it into 8-bit representation.

                int r = Math.round((float) ((pixel & rm) >> rs) / rr * 255);
                int g = Math.round((float) ((pixel & gm) >> gs) / gr * 255);
                int b = Math.round((float) ((pixel & bm) >> bs) / br * 255);
                int a = Math.round((float) ((pixel & am) >> as) / ar * 255);

                img.setPixel(x, y, Color.argb(a, r, g, b));
            }
        }

        return img;
    }

    /**
     * Reads and decodes BMP data from the source file.
     *
     * @param file the source file
     * @return the decoded image read from the source file
     * @throws IOException if an error occurs
     */
    public static Bitmap read(File file) throws IOException {
        FileInputStream fin = new FileInputStream(file);
        try {
            return read(new BufferedInputStream(fin));
        } finally {
            try {
                fin.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Reads and decodes BMP data from the source input.
     *
     * @param in the source input
     * @return the decoded image read from the source file
     * @throws IOException if an error occurs
     */
    public static Bitmap read(InputStream in) throws IOException {
        BMPDecoder d = new BMPDecoder(in);
        return d.getBitmap();
    }

    /**
     * Reads and decodes BMP data from the source file, together with metadata.
     *
     * @param file the source file
     * @return the decoded image read from the source file
     * @throws IOException if an error occurs
     */
    public static BMPImage readExt(File file) throws IOException {
        FileInputStream fin = new FileInputStream(file);
        try {
            return readExt(new BufferedInputStream(fin));
        } finally {
            try {
                fin.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Reads and decodes BMP data from the source input, together with metadata.
     *
     * @param in the source input
     * @return the decoded image read from the source file
     * @throws IOException if an error occurs
     */
    public static BMPImage readExt(InputStream in) throws IOException {
        BMPDecoder d = new BMPDecoder(in);
        return new BMPImage(d.getBitmap(), d.getInfoHeader());
    }

    /**
     * The <tt>InfoHeader</tt> structure, which provides information about the BMP data.
     *
     * @return the <tt>InfoHeader</tt> structure that was read from the source data when this <tt>BMPDecoder</tt>
     * was created.
     */
    public InfoHeader getInfoHeader() {
        return infoHeader;
    }

    /**
     * The decoded image read from the source input.
     *
     * @return the <tt>BufferedImage</tt> representing the BMP image.
     */
    public Bitmap getBitmap() {
        return img;
    }
}

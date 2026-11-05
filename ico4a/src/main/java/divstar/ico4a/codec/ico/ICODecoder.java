package divstar.ico4a.codec.ico;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import divstar.ico4a.codec.bmp.BMPConstants;
import divstar.ico4a.codec.bmp.BMPDecoder;
import divstar.ico4a.codec.bmp.ColorEntry;
import divstar.ico4a.codec.bmp.InfoHeader;
import divstar.ico4a.io.CountingInputStream;
import divstar.ico4a.io.LittleEndianInputStream;

/**
 * Decodes images in ICO format.
 *
 * @author Ian McDonagh
 * @author Igor Tykhyy
 */
public class ICODecoder {

    private static final int PNG_MAGIC = 0x89504E47;
    private static final int PNG_MAGIC_LE = 0x474E5089;
    private static final int PNG_MAGIC2 = 0x0D0A1A0A;
    private static final int PNG_MAGIC2_LE = 0x0A1A0A0D;
    private static Logger log = Logger.getLogger(ICODecoder.class.getName());

    // private java.util.List<BufferedImage> img;

    private ICODecoder() {
    }

    /**
     * Reads and decodes the given ICO file. Convenience method equivalent to
     * {@link #read(InputStream) read(new
     * java.io.FileInputStream(file))}.
     *
     * @param file the source file to read
     * @return the list of images decoded from the ICO data
     * @throws IOException if an error occurs
     */
    public static List<Bitmap> read(File file)
            throws IOException {
        FileInputStream fin = new FileInputStream(file);
        try {
            return read(new BufferedInputStream(fin));
        } finally {
            try {
                fin.close();
            } catch (IOException ex) {
                Log.e("ICODecoder", "read: " + "Failed to close file input for file " + file, ex);
            }
        }
    }

    /**
     * Reads and decodes the given ICO file, together with all metadata.
     * Convenience method equivalent to {@link #readExt(InputStream)
     * readExt(new java.io.FileInputStream(file))}.
     *
     * @param file the source file to read
     * @return the list of images decoded from the ICO data
     * @throws IOException if an error occurs
     * @since 0.7
     */
    public static List<ICOImage> readExt(File file)
            throws IOException {
        FileInputStream fin = new FileInputStream(file);
        try {
            return readExt(new BufferedInputStream(fin));
        } finally {
            try {
                fin.close();
            } catch (IOException ex) {
                Log.w("ICODecoder", "read: " + "Failed to close file input for file " + file, ex);
            }
        }
    }

    /**
     * Reads and decodes ICO data from the given source. The returned list of
     * images is in the order in which they appear in the source ICO data.
     *
     * @param is the source <tt>InputStream</tt> to read
     * @return the list of images decoded from the ICO data
     * @throws IOException if an error occurs
     */
    public static List<Bitmap> read(InputStream is)
            throws IOException {
        List<ICOImage> list = readExt(is);
        List<Bitmap> ret = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            ICOImage icoImage = list.get(i);
            Bitmap image = icoImage.getImage();
            ret.add(image);
        }
        return ret;
    }

    private static IconEntry[] sortByFileOffset(IconEntry[] entries) {
        List<IconEntry> list = Arrays.asList(entries);
        Collections.sort(list, new Comparator<IconEntry>() {

            @Override
            public int compare(IconEntry o1, IconEntry o2) {
                return o1.iFileOffset - o2.iFileOffset;
            }
        });
        return list.toArray(new IconEntry[list.size()]);
    }

    /**
     * Reads and decodes ICO data from the given source, together with all
     * metadata. The returned list of images is in the order in which they
     * appear in the source ICO data.
     *
     * @param is the source <tt>InputStream</tt> to read
     * @return the list of images decoded from the ICO data
     * @throws IOException if an error occurs
     * @since 0.7
     */
    public static List<ICOImage> readExt(InputStream is)
            throws IOException {
        // long t = System.currentTimeMillis();

        LittleEndianInputStream in = new LittleEndianInputStream(new CountingInputStream(is));

        // Reserved 2 byte =0
        in.readShortLE();
        // Type 2 byte =1
        in.readShortLE();
        // Count 2 byte Number of Icons in this file
        short sCount = in.readShortLE();

        // Entries Count * 16 list of icons
        IconEntry[] entries = new IconEntry[sCount];
        for (short s = 0; s < sCount; s++) {
            entries[s] = new IconEntry(in);
        }
        // Seems like we don't need this, but you never know!
        // entries = sortByFileOffset(entries);

        int i = 0;
        // images list of bitmap structures in BMP/PNG format
        List<ICOImage> ret = new ArrayList<>(sCount);

        try {
            for (i = 0; i < sCount; i++) {
                // Make sure we're at the right file offset!
                int fileOffset = in.getCount();
                if (fileOffset != entries[i].iFileOffset) { // entries[10].iFileOffset = 173650
                    throw new IOException("Cannot read image #" + i + " starting at unexpected file offset.");
                }

                int info = in.readIntLE();
                if (info == 40) {

                    // read XOR bitmap
                    // BMPDecoder bmp = new BMPDecoder(is);
                    InfoHeader infoHeader = BMPDecoder.readInfoHeader(in, info);
                    InfoHeader andHeader = new InfoHeader(infoHeader);
                    andHeader.iHeight = infoHeader.iHeight / 2;
                    InfoHeader xorHeader = new InfoHeader(infoHeader);
                    xorHeader.iHeight = andHeader.iHeight;

                    andHeader.sBitCount = 1;
                    andHeader.iNumColors = 2;
                    andHeader.iCompression = BMPConstants.BI_RGB;

                    // for now, just read all the raster data (img + and)
                    // and store as separate images

                    Bitmap img = BMPDecoder.read(xorHeader, in);

                    ColorEntry[] andColorTable = new ColorEntry[]{
                            new ColorEntry(255, 255, 255, 255),
                            new ColorEntry(0, 0, 0, 0)};

                    if (infoHeader.sBitCount == 32) {
                        // transparency from alpha
                        // ignore bytes after XOR bitmap
                        int size = entries[i].iSizeInBytes;
                        int infoHeaderSize = infoHeader.iSize;
                        // data size = w * h * 4
                        int dataSize = xorHeader.iWidth * xorHeader.iHeight * 4;
                        int skip = size - infoHeaderSize - dataSize;

                        if (in.skip(skip, false) < skip && i < sCount - 1) {
                            throw new EOFException("Unexpected end of input");
                        }
                    } else if (infoHeader.sBitCount <= 24) {
                        Bitmap and = BMPDecoder.read(andHeader, in, andColorTable);
                        for (int y = xorHeader.iHeight - 1; y >= 0; y--) {
                            for (int x = 0; x < xorHeader.iWidth; x++) {
                                int baseColor = img.getPixel(x, y);
                                img.setPixel(x, y, Color.argb(and.getPixel(x, y), Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor)));
                            }
                        }
                    }
                    // create ICOImage
                    IconEntry iconEntry = entries[i];
                    ICOImage icoImage = new ICOImage(img, infoHeader, iconEntry);
                    icoImage.setPngCompressed(false);
                    icoImage.setIconIndex(i);
                    ret.add(icoImage);
                }
                // check for PNG magic header and that image height and width =
                // 0 = 256 -> Vista format
                else if (info == PNG_MAGIC_LE) {
                    int info2 = in.readIntLE();

                    if (info2 != PNG_MAGIC2_LE) {
                        throw new IOException(
                                "Unrecognized icon format for image #" + i);
                    }

                    IconEntry e = entries[i];
                    int size = e.iSizeInBytes - 8;
                    byte[] pngData = new byte[size];
                    /* int count = */
                    in.readFully(pngData);
                    // if (count != pngData.length) {
                    // throw new
                    // IOException("Unable to read image #"+i+" - incomplete PNG compressed data");
                    // }
                    ByteArrayOutputStream bout = new ByteArrayOutputStream();
                    DataOutputStream dout = new DataOutputStream(bout);
                    dout.writeInt(PNG_MAGIC);
                    dout.writeInt(PNG_MAGIC2);
                    dout.write(pngData);
                    byte[] pngData2 = bout.toByteArray();
                    ByteArrayInputStream bin = new ByteArrayInputStream(pngData2);
                    Bitmap img = BitmapFactory.decodeStream(bin);

                    bin.close();
                    bout.close();
                    dout.close();

                    // create ICOImage
                    IconEntry iconEntry = entries[i];
                    ICOImage icoImage = new ICOImage(img, null, iconEntry);
                    icoImage.setPngCompressed(true);
                    icoImage.setIconIndex(i);
                    ret.add(icoImage);
                } else {
                    throw new IOException(
                            "Unrecognized icon format for image #" + i);
                }
            }
        } catch (IOException ex) {
            throw new IOException("Failed to read image # " + i, ex);
        }

        return ret;
    }
}

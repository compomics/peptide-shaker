/*
 * Java class "BinaryArrayImpl.java" generated from Poseidon for UML.
 * Poseidon for UML is developed by <A HREF="http://www.gentleware.com">Gentleware</A>.
 * Generated with <A HREF="http://jakarta.apache.org/velocity/">velocity</A> template engine.
 */
package eu.isas.peptideshaker.pride.util;

import org.apache.xerces.impl.dv.util.Base64;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * <p>Persistence independent object model implementation class:</p>
 * <p>Implements a binary array of the kind found in
 * mzData/spectrumList/spectrum/mzArrayBinary or
 * mzData/spectrumList/spectrum/intenArrayBinary.&nbsp; Holds both the data
 * itself and meta data describing the encoding of the data.</p>
 *
 * @author Philip Jones
 */
public class BinaryArrayImpl {

    /**
     * Defines the valid String indicating big endian byte order.
     */
    public static final String BIG_ENDIAN_LABEL = "big";
    /**
     * Defines the valid String indicating little endian byte order.
     */
    public static final String LITTLE_ENDIAN_LABEL = "little";
    /**
     * Defines the valid String indicating the correct precision for encoded
     * floats.
     */
    public static final String FLOAT_PRECISION = "32";
    /**
     * Defines the valid String indicating the correct precision for encoded
     * doubles.
     */
    public static final String DOUBLE_PRECISION = "64";
    /**
     * Defines the number of bytes required in an UNENCODED byte array to hold a
     * single float value.
     */
    public static final int BYTES_TO_HOLD_FLOAT = 4;
    /**
     * Defines the number of bytes required in an UNENCODED byte array to hold a
     * dingle double value.
     */
    public static final int BYTES_TO_HOLD_DOUBLE = 8;
    ///////////////////////////////////////
    // attributes
    /**
     * <p>Represents the binary contents of the array as an array of bytes.
     * (mzData element .../binaryDataGroup.)</p> <p>Note that the contents of
     * this field can be obtained from a database by calling the
     * <code>java.sql.Blob.getBytes():byte[]</code> method.</p>
     */
    protected String iBase64String = null;
    /**
     * <p>Represents the length of the binary array (mzData element
     * .../data/length).</p>
     */
    protected long iDataLength = -1;
    /**
     * <p>Represents&nbsp;the endian value of the binary array (mzData element
     * .../data/endian).</p>
     */
    protected String iDataEndian = null;
    /**
     * <p>Represents the precision of the binary array (mzData element
     * .../data/precision).</p>
     */
    protected String iDataPrecision = null;

    ///////////////////////////////////////
    // operations
    /**
     * <p>Default constructor with protected access.</p>
     */
    protected BinaryArrayImpl() {
    } // end BinaryArrayImpl

    /**
     * <p>Creates an instance of this BinaryArray object, setting all fields as
     * per description below.</p>
     *
     * @param aBase64String the binary contents of the array as an array of
     * bytes. (mzData element .../binaryDataGroup.) Note that the contents of
     * this field can be obtained from a database by calling the
     * java.sql.Blob.getBytes():byte[] method.
     * @param aDataLength the length of the binary array (mzData element
     * .../data/length).
     * @param aDataEndian The byte order is used when reading or writing
     * multibyte values stored as mzData element .../data/endian. Only possible
     * values are defined by the static String members of this class
     * 'BIG_ENDIAN_LABEL' (or "big") and 'LITTLE_ENDIAN_LABEL' (or "little").
     * @param aDataPrecision the precision of the binary array (mzData element
     * .../data/precision) that indicates if the array contains encoded double
     * values or encoded float values. Only possible values for this parameter
     * are defined byt he static String members of this class 'FLOAT_PRECISION'
     * (or "32") and 'DOUBLE_PRECISION' (or "64").
     */
    public BinaryArrayImpl(String aBase64String,
            long aDataLength,
            String aDataEndian,
            String aDataPrecision) {
        this.iDataEndian = aDataEndian;
        constructorCommon(aBase64String, aDataLength, aDataPrecision);
    } // end BinaryArrayImpl

    /**
     * Constructor that allows the creation of a BinaryArray object using an
     * array of double values.
     *
     * @param doubleArray being the array of double values to be converted to
     * base64 and placed in the mzData element .../binaryDataGroup.
     * @param aDataEndian The byte order is used when reading or writing
     * multibyte values stored as mzData element .../data/endian. Only possible
     * values are defined by the static String members of this class
     * 'BIG_ENDIAN_LABEL' (or "big") and 'LITTLE_ENDIAN_LABEL' (or "little").
     */
    public BinaryArrayImpl(double[] doubleArray,
            String aDataEndian) {
        this.iDataEndian = aDataEndian;
        if (doubleArray == null) {
            throw new IllegalArgumentException("The double[] 'doubleArray' that has been passed into the method BinaryArrayImpl is null.  This is not valid.");
        }

        ByteBuffer buffer = ByteBuffer.allocate(doubleArray.length * BYTES_TO_HOLD_DOUBLE);
        buffer.order(getByteOrder());
        for (double aDoubleArray : doubleArray) {
            buffer.putDouble(aDoubleArray);
        }
        String base64String = Base64.encode(buffer.array());
        constructorCommon(base64String, doubleArray.length, DOUBLE_PRECISION);
    }

    /**
     * Constructor that allows the creation of a BinaryArray object using an
     * array of float values.
     *
     * @param floatArray being the array of float values to be converted to
     * base64 and placed in the mzData element .../binaryDataGroup.
     * @param aDataEndian The byte order is used when reading or writing
     * multibyte values stored as mzData element .../data/endian. Only possible
     * values are defined by the static String members of this class
     * 'BIG_ENDIAN_LABEL' (or "big") and 'LITTLE_ENDIAN_LABEL' (or "little").
     */
    public BinaryArrayImpl(float[] floatArray, String aDataEndian) {
        this.iDataEndian = aDataEndian;
        if (floatArray == null) {
            throw new IllegalArgumentException("The float[] 'floatArray' that has been passed into the BinaryArrayImpl is null.  This is not valid.");
        }
        ByteBuffer buffer = ByteBuffer.allocate(floatArray.length * BYTES_TO_HOLD_FLOAT);
        buffer.order(getByteOrder());
        for (float aFloatArray : floatArray) {
            buffer.putFloat(aFloatArray);
        }
        String base64String = Base64.encode(buffer.array());
        constructorCommon(base64String, floatArray.length, FLOAT_PRECISION);
    }

    /**
     * Private helper to factor out common behaviour of the two constructors
     * that take arrays of numbers to be encoded.
     *
     * @param aBase64String the binary contents of the array as an array of
     * bytes.
     * @param aDataLength the length of the binary array (mzData element
     * .../data/length). stored as mzData element .../data/endian. Only possible
     * values are defined by the static String members of this class
     * 'BIG_ENDIAN_LABEL' (or "big") and 'LITTLE_ENDIAN_LABEL' (or "little").
     * @param aDataPrecision the precision of the binary array (mzData element
     * .../data/precision) that indicates if the array contains encoded double
     * values or encoded float values. Only possible values for this parameter
     * are defined byt he static String members of this class 'FLOAT_PRECISION'
     * (or "32") and 'DOUBLE_PRECISION' (or "64").
     */
    private void constructorCommon(String aBase64String, long aDataLength, String aDataPrecision) {
        if (aBase64String == null) {
            throw new IllegalArgumentException("Attempting to instantiate a BinaryArrayImpl without specifying a valid value for aBase64String");
        }
        if (aBase64String.indexOf('\n') > -1) {
            // Strip out white space if any present.
            this.iBase64String = aBase64String.trim().replaceAll("\\n", "");
        } else {
            this.iBase64String = aBase64String.trim();
        }
        this.iDataLength = aDataLength;
        this.iDataPrecision = aDataPrecision;
    }

    public String getBase64String() {
        return iBase64String;
    } // end getBase64String

    /**
     * <p>Returns the contents of the binary array <i>decoded</i> using the
     * Base64 algorithm.</p>
     *
     * @return the contents of the binary array <i>decoded</i> using the Base64
     * algorithm.
     */
    public byte[] getDecodedByteArray() {
        if (this.getBase64String() == null) {
            return null;
        } else {
            return Base64.decode(this.getBase64String());
        }
    }

    /**
     * Returns the number of bytes used for each element in the (NON-encoded)
     * byte array depending on the precision of the data.
     *
     * @return an int value being either 4 (for storage of floats) or 8 (for
     * storage of doubles).
     */
    private int getStep() {
        if (FLOAT_PRECISION.equals(this.getDataPrecision())) {
            return BYTES_TO_HOLD_FLOAT;
        } else if (DOUBLE_PRECISION.equals(this.getDataPrecision())) {
            return BYTES_TO_HOLD_DOUBLE;
        } else {
            throw new IllegalStateException("The value for data precision for this binary array must be either 32 or 64.  In this case it is: " + this.getDataPrecision());
        }
    }

    /**
     * Returns the appropriate ByteOrder object depending on whether big endian
     * or little endian byte order is being used.
     *
     * @return the appropriate ByteOrder object depending on whether big endian
     * or little endian byte order is being used.
     */
    private ByteOrder getByteOrder() {
        if (BIG_ENDIAN_LABEL.equals(this.getDataEndian())) {
            return ByteOrder.BIG_ENDIAN;
        } else if (LITTLE_ENDIAN_LABEL.equals(this.getDataEndian())) {
            return ByteOrder.LITTLE_ENDIAN;
        } else {
            throw new IllegalStateException("The value for data endian for this binary array must be either 'big' or 'little'.  In this case it is: " + this.getDataPrecision());
        }
    }

    /**
     * Checks if all the necessary information is provided and then converts the
     * decoded binary array into an array of double values (that for example
     * could be used to draw a spectra).
     *
     * @return the decoded binary array converted into an array of double
     * values.
     */
    public double[] getDoubleArray() {
        // Note the 'precision' value is constrained to be only "32" or "64".
        int step = getStep();

        byte[] fullArray = this.getDecodedByteArray();
        if (fullArray == null || fullArray.length == 0) {
            // No data array set - return null.
            return null;
        }
        if (fullArray.length % step != 0) {
            throw new IllegalStateException("Error caused by attempting to split a byte array of length " + fullArray.length + " into pieces of length " + step);
        }

        double[] doubleArray = new double[fullArray.length / step];
        ByteBuffer bb = ByteBuffer.wrap(fullArray);
        // Set the order to BIG or LITTLE ENDIAN
        bb.order(getByteOrder());

        for (int indexOut = 0; indexOut < fullArray.length; indexOut += step) {
            /*
             * Note that the 'getFloat(index)' method gets the next 4 bytes and
             * the 'getDouble(index)' method gets the next 8 bytes.
             */
            doubleArray[indexOut / step] = (step == BYTES_TO_HOLD_FLOAT) ? (double) bb.getFloat(indexOut)
                    : bb.getDouble(indexOut);
        }

        return doubleArray;
    }

    /**
     * <p>Returns the length of the binary array (mzData element
     * .../data/length).</p>
     *
     * @return Returns the length of the binary array (mzData element
     * .../data/length).
     */
    public long getDataLength() {
        return iDataLength;
    } // end getDataLength

    /**
     * <p>Returns the endian value of the binary array (mzData element
     * .../data/endian).</p>
     *
     * @return Returns the endian value of the binary array (mzData element
     * .../data/endian).
     */
    public String getDataEndian() {
        return iDataEndian;
    } // end getDataEndian

    /**
     * <p>Returns the precision of the binary array (mzData element
     * .../data/precision).</p>
     *
     * @return Returns the precision of the binary array (mzData element
     * .../data/precision).
     */
    public String getDataPrecision() {
        return iDataPrecision;
    } // end getDataPrecision

    /**
     * Returns a useful String representation of this Imlplementation instance
     * that includes details of all fields.
     *
     * @return a useful String representation of this Imlplementation instance.
     */
    public String toString() {
        final StringBuffer buf = new StringBuffer();
        buf.append("    BinaryArrayImpl:").append(" DataLength=").append(iDataLength).append(", DataEndian=").append(iDataEndian).append(", DataPrecision=").append(iDataPrecision).append("\n, Base64 String='").append(iBase64String).append("'");

        buf.append("}\n");
        if (iBase64String != null) {
            byte[] decoded = this.getDecodedByteArray();
            buf.append("\n...DECODED: ");
            for (int i = 0; i < decoded.length; ++i) {
                buf.append(i == 0 ? "" : ", ").append(decoded[i]);
            }
            buf.append("}\n");
            double[] decodedDouble = this.getDoubleArray();
            buf.append("\n...TO DOUBLE ARRAY: ");
            for (int i = 0; i < decodedDouble.length; i++) {
                buf.append(i == 0 ? "" : ", ").append(decodedDouble[i]);
            }
            buf.append("}\n");
        }
        buf.append("}\n");
        return buf.toString();
    }

    /**
     * <p>Performs equals methods dependent on values of instance variables and
     * class of Object o.</p>
     *
     * @param o Object that this is being compared with.
     * @return boolean indicating equality of this object with the parameter
     * Object o
     */
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final BinaryArrayImpl that = (BinaryArrayImpl) o;

        if (this.getDataLength() != that.getDataLength()) {
            return false;
        }
        if (!this.getDataEndian().equals(that.getDataEndian())) {
            return false;
        }
        if (!this.getDataPrecision().equals(that.getDataPrecision())) {
            return false;
        }
        if (this.getBase64String() != null ? !this.getBase64String().equals(that.getBase64String()) : that.getBase64String() != null) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result;
        result = (this.getBase64String() != null ? this.getBase64String().hashCode() : 0);
        result = 29 * result + (int) (this.getDataLength() ^ (this.getDataLength() >>> 32));
        result = 29 * result + this.getDataEndian().hashCode();
        result = 29 * result + this.getDataPrecision().hashCode();
        return result;
    }
} // end BinaryArrayImpl


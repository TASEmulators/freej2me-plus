/*======================================================================

 CLASS       : DrmResourceInputStream.java
 SUMMARY     : Template Java source file for use in ThomasSCF
 COPYRIGHT   : (c) 2011 Nokia Corporation. All rights reserved.

 ========================================================================

 CCM DETAILS : %name:          DrmResourceInputStream.java %
 %version:       1 %
 %date_modified: Fri Jun 01 18:22:45 2012 %
 
 ========================================================================
 
VERSION     : 3        May 6, 2011        Guangjing Zhang       
REASON      : Initial version - Accept 360 ID - 1384755
REFERENCE   : sd1iam3#71755
DESCRIPTION : Make the constructor package private as a part of domain protection

 ========================================================================

VERSION     : 2        08-Mar-2011        Guangjing Zhang      
REASON      : Initial version. Accept 360 ID: 1227661
REFERENCE   : sd1iam3#67496
DESCRIPTION : Create class DrmResourceInputStream
 
 ========================================================================

VERSION     : 1        Jan 20, 2010        Sonia Sarkar       
REASON      : Initial version - Accept 360 ID - 1082785
REFERENCE   : sd1iam3#63909
DESCRIPTION : API support for In Application Payment

 ======================================================================*/

package com.nokia.mid.payment;

import java.io.*;
import java.util.*;

public class DrmResourceInputStream extends InputStream {

    // This field must be an Object, since it will point to
    // something that needs to be protected from the garbage
    // collector.
    private Object handle;
    private int pos;    // position we are currently accessing
    private int size;   // Size of the whole stream
    private int mark;   // Position where a reset will move pos to in the stream

    /**
     * Fixes the resource name to be conformant with the CLDC 1.0
     * specification. We are not allowed to use "../" to get outside
     * of the .jar file.
     *
     * @param name the name of the resource in classpath to access.
     * @return     the fixed string.
     * @exception  IOException if the resource name points to a
     *              classfile, as determined by the resource name's
     *              extension.
     */
    private static String fixResourceName(String name) throws IOException {
        Vector dirVector = new Vector();
        int    startIdx = 0;
        int    endIdx = 0;
        String curDir;

        while ((endIdx = name.indexOf('/', startIdx)) != -1) {
            if (endIdx == startIdx) {
                // We have a leading '/' or two consecutive '/'s
                startIdx++;
                continue;
            }

            curDir = name.substring(startIdx, endIdx);
            startIdx = endIdx + 1;

            if (curDir.equals(".")) {
                // Ignore a single '.' directory
                continue;
            }
            if (curDir.equals("..")) {
                // Go up a level
                try {
                    dirVector.removeElementAt(dirVector.size()-1);
                } catch (ArrayIndexOutOfBoundsException aioobe) {
                     // "/../resource" Not allowed!
                     throw new IOException();
                }
                continue;
            }
            dirVector.addElement(curDir);
        }

        // save directory structure
        StringBuffer dirName = new StringBuffer();

        int nelements = dirVector.size();
        for (int i = 0; i < nelements; ++i) {
          dirName.append((String)dirVector.elementAt(i));
          dirName.append("/");
        }

        // save filename
        if (startIdx < name.length()) {
            String filename = name.substring(startIdx);
            // Throw IOE if the resource ends with ".class", but, not
            //  if the entire name is ".class"
            if ((filename.endsWith(".class")) &&
                (! ".class".equals(filename))) {
                throw new IOException();
            }
            dirName.append(name.substring(startIdx));
        }
        return dirName.toString();
    }

    /**
     * Construct a resource input stream for accessing objects in the jar file.
     *
     * @param name the name of the resource in classpath to access. The
     *              name must not have a leading '/'.
     * @exception  IOException  if an I/O error occurs.
     */
    DrmResourceInputStream(String name) throws IOException {
        String fixedName = fixResourceName(name);

        handle = open(fixedName);
        if (handle == null) {
            throw new IOException();
        }
        size = size(handle);
        pos = 0;
        mark = 0;
    }

    /**
     * Reads the next byte of data from the input stream.
     *
     * @return     the next byte of data, or <code>-1</code>
     *             if the end of the stream is reached.
     * @exception  IOException  if an I/O error occurs.
     */
    public int read() throws IOException {
        int result;
        if ((result = read(handle)) != -1) {
            pos++;
        }
        return result;
    }

    /**
     * Closes this input stream and releases any system resources associated
     * with the stream.
     *
     * @exception  IOException  if an I/O error occurs.
     */
    public synchronized void close() throws IOException {
        close(handle);
        handle = null;
    }

    /**
     * Returns the number of bytes that can be read (or skipped over) from
     * this input stream without blocking by the next caller of a method for
     * this input stream.  The next caller might be the same thread or
     * another thread.
     *
     * @return     the number of bytes that can be read from this input stream
     *             without blocking.
     * @exception  IOException  if an I/O error occurs.
     */
    public int available() throws IOException {
        return size - pos;
    }

    /**
     * Reads up to <code>len</code> bytes of data from the input stream into
     * an array of bytes.  An attempt is made to read as many as
     * <code>len</code> bytes, but a smaller number may be read, possibly
     * zero. The number of bytes actually read is returned as an integer.
     *
     * <p> This method blocks until input data is available, end of file is
     * detected, or an exception is thrown.
     *
     * <p> If <code>b</code> is <code>null</code>, a
     * <code>NullPointerException</code> is thrown.
     *
     * <p> If <code>off</code> is negative, or <code>len</code> is negative, or
     * <code>off+len</code> is greater than the length of the array
     * <code>b</code>, then an <code>IndexOutOfBoundsException</code> is
     * thrown.
     *
     * <p> If <code>len</code> is zero, then no bytes are read and
     * <code>0</code> is returned; otherwise, there is an attempt to read at
     * least one byte. If no byte is available because the stream is at end of
     * file, the value <code>-1</code> is returned; otherwise, at least one
     * byte is read and stored into <code>b</code>.
     *
     * <p> The first byte read is stored into element <code>b[off]</code>, the
     * next one into <code>b[off+1]</code>, and so on. The number of bytes read
     * is, at most, equal to <code>len</code>. Let <i>k</i> be the number of
     * bytes actually read; these bytes will be stored in elements
     * <code>b[off]</code> through <code>b[off+</code><i>k</i><code>-1]</code>,
     * leaving elements <code>b[off+</code><i>k</i><code>]</code> through
     * <code>b[off+len-1]</code> unaffected.
     *
     * <p> In every case, elements <code>b[0]</code> through
     * <code>b[off]</code> and elements <code>b[off+len]</code> through
     * <code>b[b.length-1]</code> are unaffected.
     *
     * <p> If the first byte cannot be read for any reason other than end of
     * file, then an <code>IOException</code> is thrown. In particular, an
     * <code>IOException</code> is thrown if the input stream has been closed.
     *
     * @param      b     the buffer into which the data is read.
     * @param      off   the start offset in array <code>b</code>
     *                   at which the data is written.
     * @param      len   the maximum number of bytes to read.
     * @return     the total number of bytes read into the buffer, or
     *             <code>-1</code> if there is no more data because the end of
     *             the stream has been reached.
     * @exception  IOException  if an I/O error occurs.
     * @see        java.io.InputStream#read()
     */
    public int read(byte b[], int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        } else if ((off < 0) || (off > b.length) || (len < 0) ||
                   ((off + len) > b.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        }
        if (pos >= size) {
            return -1;
        }
        if (pos + len > size) {
            len = size - pos;
        }
        if (len <= 0) {
            return 0;
        }

        int readLength;
        if ((readLength = readBytes(handle, b, off, pos, len)) != -1) {
            pos += readLength;
        }
        return readLength;
    }

    /**
     * Marks the current position in this input stream. A subsequent call to
     * the <code>reset</code> method repositions this stream at the last marked
     * position so that subsequent reads re-read the same bytes.
     *
     * <p> The <code>readlimit</code> arguments tells this input stream to
     * allow that many bytes to be read before the mark position gets
     * invalidated. This is not used in this implementation.
     *
     * <p> The general contract of <code>mark</code> is that, if the method
     * <code>markSupported</code> returns <code>true</code>, the stream somehow
     * remembers all the bytes read after the call to <code>mark</code> and
     * stands ready to supply those same bytes again if and whenever the method
     * <code>reset</code> is called.  However, the stream is not required to
     * remember any data at all if more than <code>readlimit</code> bytes are
     * read from the stream before <code>reset</code> is called.
     *
     * @param   readlimit   the maximum limit of bytes that can be read before
     *                      the mark position becomes invalid.
     * @see     java.io.InputStream#reset()
     */
    public synchronized void mark(int readlimit) {
        // Simply remember the current position ready for the next reset() 
        // NOTE that readlimit is ignored as we read the whole file.
        mark = pos;
    }

    /**
     * Tests if this input stream supports the <code>mark</code> and
     * <code>reset</code> methods. The <code>markSupported</code> method of
     * <code>InputStream</code> returns <code>true</code>.
     *
     * @return  <code>true</code> if this true type supports the mark and reset
     *          method; <code>false</code> otherwise.
     * @see     java.io.InputStream#mark(int)
     * @see     java.io.InputStream#reset()
     */
    public boolean markSupported() {
        return true;
    }

    /**
     * Repositions this stream to the position at the time the
     * <code>mark</code> method was last called on this input stream.
     *
     * <p> The general contract of <code>reset</code> is:
     *
     * <p><ul>
     *
     * <li> If the method <code>markSupported</code> returns
     * <code>true</code>, then:
     *
     *     <ul><li> If the method <code>mark</code> has not been called since
     *     the stream was created, or the number of bytes read from the stream
     *     since <code>mark</code> was last called is larger than the argument
     *     to <code>mark</code> at that last call, then an
     *     <code>IOException</code> might be thrown.
     *
     *     <li> If such an <code>IOException</code> is not thrown, then the
     *     stream is reset to a state such that all the bytes read since the
     *     most recent call to <code>mark</code> (or since the start of the
     *     file, if <code>mark</code> has not been called) will be resupplied
     *     to subsequent callers of the <code>read</code> method, followed by
     *     any bytes that otherwise would have been the next input data as of
     *     the time of the call to <code>reset</code>. </ul>
     *
     * <li> If the method <code>markSupported</code> returns
     * <code>false</code>, then:
     *
     *     <ul><li> The call to <code>reset</code> may throw an
     *     <code>IOException</code>.
     *
     *     <li> If an <code>IOException</code> is not thrown, then the stream
     *     is reset to a fixed state that depends on the particular type of the
     *     input stream and how it was created. The bytes that will be supplied
     *     to subsequent callers of the <code>read</code> method depend on the
     *     particular type of the input stream. </ul></ul>
     *
     * @exception  IOException  if this stream has not been marked or if the
     *                          mark has been invalidated.
     * @see     java.io.InputStream#mark(int)
     * @see     java.io.IOException
     */
    public synchronized void reset() throws IOException {
        resetToPos( handle, mark );
        pos = mark;
    }

    /**
     * Skips over and discards <code>n</code> bytes of data from this input
     * stream. The <code>skip</code> method may, for a variety of reasons, end
     * up skipping over some smaller number of bytes, possibly <code>0</code>.
     * This may result from any of a number of conditions; reaching end of file
     * before <code>n</code> bytes have been skipped is only one possibility.
     * The actual number of bytes skipped is returned.  If <code>n</code> is
     * negative, no bytes are skipped.
     *
     * @param      n   the number of bytes to be skipped.
     * @return     the actual number of bytes skipped.
     * @exception  IOException  if an I/O error occurs.
     */
    public long skip(long n) throws IOException {

        int result = skipN( handle, (int) n );
        pos += result;

        // Return the actual amount of bytes we managed to skip
        return ( long ) result;

    }

    private static native Object open(String name) throws IOException;
    private static native void close(Object handle) throws IOException;
    private static native int size(Object handle) throws IOException;
    private static native int read(Object handle) throws IOException;
    private static native int readBytes(Object handle, byte[] b, int offset,
                                        int pos, int len) throws IOException;
    private static native void resetToPos( Object handle, int pos );
    private static native int skipN( Object handle, int n );
}



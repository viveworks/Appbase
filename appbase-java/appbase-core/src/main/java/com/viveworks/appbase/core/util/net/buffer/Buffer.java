/*
 * Copyright 2019 The original author or authors of Appbase
 *
 * This project includes software developed by the author or authors of
 * Appbase (https://github.com/viveworks/Appbase).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.viveworks.appbase.core.util.net.buffer;

import com.viveworks.appbase.core.util.objectpool.Poolable;
import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;

/**
 * Wrapper for {@link io.netty.buffer.ByteBuf}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@SuppressWarnings("checkstyle:MethodCount")
public interface Buffer extends Poolable {
    /**
     * Get a <code>byte</code> from the buffer at the specified <code>index</code>.
     *
     * @param index Index in the buffer (where index starts from 0)
     * @return byte
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    byte getByte(int index) throws IndexOutOfBoundsException;

    /**
     * Get an unsigned <code>byte</code> from the buffer at the specified <code>index</code>.
     *
     * @param index Index in the buffer (where index starts from 0)
     * @return byte
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    short getUnsignedByte(int index) throws IndexOutOfBoundsException;

    /**
     * Get a 2-byte UTF-16 <code>char</code> from the buffer at the specified <code>index</code>.
     *
     * @param index Index in the buffer (where index starts from 0)
     * @return char
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    char getChar(int index) throws IndexOutOfBoundsException;

    /**
     * Get a <code>boolean</code> from the buffer at the specified <code>index</code>.
     *
     * @param index Index in the buffer (where index starts from 0)
     * @return boolean
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    boolean getBoolean(int index) throws IndexOutOfBoundsException;

    /**
     * Get a 16-bit <code>short</code> from the buffer at the specified <code>index</code>.
     *
     * @param index Index in the buffer (where index starts from 0)
     * @return short
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    short getShort(int index) throws IndexOutOfBoundsException;

    /**
     * Get a 16-bit <code>short</code> from the buffer at the specified <code>index</code> in little endian order.
     *
     * @param index Index in the buffer (where index starts from 0)
     * @return short
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    short getShortLE(int index) throws IndexOutOfBoundsException;

    /**
     * Get a 16-bit unsigned <code>short</code> from the buffer at the specified <code>index</code>.
     *
     * @param index Index in the buffer (where index starts from 0)
     * @return short
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    int getUnsignedShort(int index) throws IndexOutOfBoundsException;

    /**
     * Get a 16-bit unsigned <code>short</code> from the buffer at the specified <code>index</code> in little endian
     * order.
     *
     * @param index Index in the buffer (where index starts from 0)
     * @return short
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    int getUnsignedShortLE(int index) throws IndexOutOfBoundsException;

    /**
     * Get a 24-bit <code>medium integer</code> from the buffer at the specified <code>index</code>.
     *
     * @param index Index in the buffer (where index starts from 0)
     * @return short
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    int getMedium(int index) throws IndexOutOfBoundsException;

    /**
     * Get a 24-bit <code>medium integer</code> from the buffer at the specified <code>index</code> in little endian
     * order.
     *
     * @param index Index in the buffer (where index starts from 0)
     * @return short
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    int getMediumLE(int index) throws IndexOutOfBoundsException;

    /**
     * Get a 24-bit unsigned <code>unsigned medium</code> from the buffer at the specified <code>index</code>.
     *
     * @param index Index in the buffer (where index starts from 0)
     * @return short
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    int getUnsignedMedium(int index) throws IndexOutOfBoundsException;

    /**
     * Get a 24-bit unsigned <code>medium</code> from the buffer at the specified <code>index</code> in little endian
     * order.
     *
     * @param index Index in the buffer (where index starts from 0)
     * @return short
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    int getUnsignedMediumLE(int index) throws IndexOutOfBoundsException;

    /**
     * Get a 32-bit <code>int</code> from the buffer at the specified <code>index</code>.
     *
     * @param index Index in the buffer (where index starts from 0)
     * @return int
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    int getInt(int index) throws IndexOutOfBoundsException;

    /**
     * Get an 32-bit <code>int</code> from the buffer at the specified <code>index</code> in little endian order.
     *
     * @param index Index in the buffer (where index starts from 0)
     * @return int
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    int getIntLE(int index) throws IndexOutOfBoundsException;

    /**
     * Get a 32-bit <code>unsigned int</code> from the buffer at the specified <code>index</code>.
     *
     * @param index Index in the buffer (where index starts from 0)
     * @return int
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    long getUnsignedInt(int index) throws IndexOutOfBoundsException;

    /**
     * Get a 32-bit <code>unsigned int</code> from the buffer at the specified <code>index</code> in little endian
     * order.
     *
     * @param index Index in the buffer (where index starts from 0)
     * @return int
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    long getUnsignedIntLE(int index) throws IndexOutOfBoundsException;

    /**
     * Get a 64-bit <code>long</code> from the buffer at the specified <code>index</code>.
     *
     * @param index Index in the buffer (where index starts from 0)
     * @return long
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    long getLong(int index) throws IndexOutOfBoundsException;

    /**
     * Get a 64-bit <code>long</code> from the buffer at the specified <code>index</code> in little endian order.
     *
     * @param index Index in the buffer (where index starts from 0)
     * @return long
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    long getLongLE(int index) throws IndexOutOfBoundsException;

    /**
     * Get a 32-bit <code>float</code> from the buffer at the specified <code>index</code>.
     *
     * @param index Index in the buffer (where index starts from 0)
     * @return long
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    float getFloat(int index) throws IndexOutOfBoundsException;

    /**
     * Get a 32-bit <code>float</code> from the buffer at the specified <code>index</code> in little endian order.
     *
     * @param index Index in the buffer (where index starts from 0)
     * @return long
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    float getFloatLE(int index) throws IndexOutOfBoundsException;

    /**
     * Get a 64-bit <code>double</code> from the buffer at the specified <code>index</code>.
     *
     * @param index Index in the buffer (where index starts from 0)
     * @return long
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    double getDouble(int index) throws IndexOutOfBoundsException;

    /**
     * Get a 64-bit <code>double</code> from the buffer at the specified <code>index</code> in little endian order.
     *
     * @param index Index in the buffer (where index starts from 0)
     * @return long
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    double getDoubleLE(int index) throws IndexOutOfBoundsException;

    /**
     * Get a <code>byte array</code> from the buffer at the specified <code>index</code>.
     *
     * @param index Index in the buffer (where index starts from 0)
     * @return byte array
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    byte[] getBytes(int index) throws IndexOutOfBoundsException;

    /**
     * Get a <code>byte array</code> from the buffer at a <code>starting index inclusively</code> to
     * <code>ending index exclusively</code>.
     *
     * @param startIndexInclusive Start index (inclusively, where index starts from 0)
     * @param endIndexExclusive End index (exclusively, where {@link #length()} is the last index)
     * @return byte array
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    byte[] getBytes(int startIndexInclusive, int endIndexExclusive) throws IndexOutOfBoundsException;

    /**
     * Get bytes into the <code>destination byte array</code> from the <code>starting index inclusively</code>
     * to <code>ending index exclusively</code>.
     *
     * @param startIndexInclusive Start index (inclusively, where index starts from 0)
     * @param endIndexExclusive End index (exclusively, where {@link #length()} is the last index)
     * @param destination Destination byte array
     * @param destinationStartIndex Destination start index (inclusively, where index starts from 0)
     * @throws IndexOutOfBoundsException if either indices are invalid
     */
    void getBytes(
            int startIndexInclusive,
            int endIndexExclusive,
            byte[] destination,
            int destinationStartIndex) throws IndexOutOfBoundsException;

    /**
     * Get a {@link CharSequence} from the <code>starting index inclusively</code> to
     * <code>ending index exclusively</code>.
     *
     * @param startIndexInclusive Start index (inclusively, where index starts from 0)
     * @param endIndexExclusive End index (exclusively, where {@link #length()} is the last index)
     * @param charset {@link Charset} to use to getInstance the char sequence from the buffer
     * @return Char sequence
     * @throws IndexOutOfBoundsException if either indices are invalid
     */
    CharSequence getCharSequence(
            int startIndexInclusive,
            int endIndexExclusive,
            Charset charset) throws IndexOutOfBoundsException;

    /**
     * Get a {@link String} in the <code>default charset (UTF16)</code>.
     *
     * @return String
     * @throws IndexOutOfBoundsException if either indices are invalid
     */
    String getString();

    /**
     * Get a {@link String} in the specified <code>charset</code>.
     *
     * @param charset Charset of the returned string
     * @return String
     * @throws IndexOutOfBoundsException if either indices are invalid
     */
    String getString(Charset charset);

    /**
     * Get a {@link String} in the <code>default charset (UTF16)</code>from the <code>starting index inclusively</code>
     * to <code>ending index exclusively</code>.
     *
     * @param startIndexInclusive Start index (inclusively, where index starts from 0)
     * @param endIndexExclusive End index (exclusively, where {@link #length()} is the last index)
     * @return String
     * @throws IndexOutOfBoundsException if either indices are invalid
     */
    String getString(int startIndexInclusive,
                     int endIndexExclusive) throws IndexOutOfBoundsException;

    /**
     * Get a {@link String} in the <code>default charset (UTF16)</code>.
     *
     * @param startIndexInclusive Start index (inclusively, where index starts from 0)
     * @param endIndexExclusive End index (exclusively, where {@link #length()} is the last index)
     * @param charset Charset of the returned string
     * @return String
     * @throws IndexOutOfBoundsException if either indices are invalid
     */
    String getString(int startIndexInclusive,
                     int endIndexExclusive,
                     Charset charset) throws IndexOutOfBoundsException;

    /**
     * Set a <code>byte</code> value at the specified <code>index</code>.
     * 
     * @param index Index in the buffer (where index starts from 0)
     * @param value byte value to set
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    void setByte(int index, byte value) throws IndexOutOfBoundsException;

    /**
     * Set a <code>-2-byte UTF16 character</code> value at the specified <code>index</code>.
     *
     * @param index Index in the buffer (where index starts from 0)
     * @param value byte value to set
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    void setChar(int index, char value) throws IndexOutOfBoundsException;

    /**
     * Set a <code>boolean</code> value at the specified <code>index</code>.
     *
     * @param index Index in the buffer (where index starts from 0)
     * @param value byte value to set
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    void setBoolean(int index, boolean value) throws IndexOutOfBoundsException;

    /**
     * Set a 16-bit <code>short</code> value at the specified <code>index</code>.
     *
     * @param index Index in the buffer (where index starts from 0)
     * @param value byte value to set
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    void setShort(int index, short value) throws IndexOutOfBoundsException;

    /**
     * Set a 16-bit <code>short</code> value at the specified <code>index</code> with little endian order.
     *
     * @param index Index in the buffer (where index starts from 0)
     * @param value byte value to set
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    void setShortLE(int index, short value) throws IndexOutOfBoundsException;

    /**
     * Set a 24-bit <code>medium int</code> value at the specified <code>index</code>.
     *
     * @param index Index in the buffer (where index starts from 0)
     * @param value byte value to set
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    void setMedium(int index, int value) throws IndexOutOfBoundsException;

    /**
     * Set a 24-bit <code>medium int</code> value at the specified <code>index</code> with little endian order.
     *
     * @param index Index in the buffer (where index starts from 0)
     * @param value byte value to set
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    void setMediumLE(int index, int value) throws IndexOutOfBoundsException;

    /**
     * Set a 32-bit <code>int</code> value at the specified <code>index</code>.
     *
     * @param index Index in the buffer (where index starts from 0)
     * @param value byte value to set
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    void setInt(int index, int value) throws IndexOutOfBoundsException;

    /**
     * Set a 32-bit <code>int</code> value at the specified <code>index</code> with little endian order.
     *
     * @param index Index in the buffer (where index starts from 0)
     * @param value byte value to set
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    void setIntLE(int index, int value) throws IndexOutOfBoundsException;

    /**
     * Set a 64-bit <code>long</code> value at the specified <code>index</code>.
     *
     * @param index Index in the buffer (where index starts from 0)
     * @param value byte value to set
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    void setLong(int index, long value) throws IndexOutOfBoundsException;

    /**
     * Set a 64-bit <code>long</code> value at the specified <code>index</code> with little endian order.
     *
     * @param index Index in the buffer (where index starts from 0)
     * @param value byte value to set
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    void setLongLE(int index, long value) throws IndexOutOfBoundsException;

    /**
     * Set a 32-bit <code>float</code> value at the specified <code>index</code>.
     *
     * @param index Index in the buffer (where index starts from 0)
     * @param value byte value to set
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    void setFloat(int index, float value) throws IndexOutOfBoundsException;

    /**
     * Set a 32-bit <code>float</code> value at the specified <code>index</code> with little endian order.
     *
     * @param index Index in the buffer (where index starts from 0)
     * @param value byte value to set
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    void setFloatLE(int index, float value) throws IndexOutOfBoundsException;

    /**
     * Set a 64-bit <code>double</code> value at the specified <code>index</code>.
     *
     * @param index Index in the buffer (where index starts from 0)
     * @param value byte value to set
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    void setDouble(int index, double value) throws IndexOutOfBoundsException;

    /**
     * Set a 64-bit <code>double</code> value at the specified <code>index</code> with little endian order.
     *
     * @param index Index in the buffer (where index starts from 0)
     * @param value byte value to set
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    void setDoubleLE(int index, double value) throws IndexOutOfBoundsException;

    /**
     * Set <code>all bytes</code> from a source at the specified <code>index</code>.
     *
     * @param source Source byte array
     * @param index Index in this buffer (where index starts from 0)
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    void setBytes(int index, byte[] source) throws IndexOutOfBoundsException;

    /**
     * Set <code>bytes</code> from a source starting from a specified <code>index inclusively</code> to
     * the <code>ending index exclusively</code> at the specified <code>index</code> in this buffer.
     *
     * @param index Index in this buffer (where index starts from 0)
     * @param source Source byte array
     * @param sourceStartIndexInclusive Source start index (inclusively, where index starts from 0)
     * @param sourceEndIndexExclusive Source end index (exclusively, where {@link #length()} is the last index)
     * @throws IndexOutOfBoundsException if either indices are invalid
     */
    void setBytes(
            int index,
            byte[] source,
            int sourceStartIndexInclusive,
            int sourceEndIndexExclusive)
            throws IndexOutOfBoundsException;

    /**
     * Set {@link CharSequence} at the specified <code>index</code> of this buffer using a specific {@link Charset}.
     *
     * @param index Index in this buffer (where index starts from 0)
     * @param charSequence {@link CharSequence} source
     * @param charset {@link Charset} to use
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    void setCharSequence(int index, CharSequence charSequence, Charset charset) throws IndexOutOfBoundsException;

    /**
     * Set a {@link String} at the specified <code>index</code> of this buffer using the default charset (UTF16).
     *
     * @param index Index in this buffer (where index starts from 0)
     * @param string {@link String} source
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    void setString(int index, String string) throws IndexOutOfBoundsException;

    /**
     * Set a {@link String} at the specified <code>index</code> of this buffer using the specific {@link Charset}.
     *
     * @param index Index in this buffer (where index starts from 0)
     * @param string {@link String} source
     * @param charset {@link Charset} to use
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    void setString(int index, String string, Charset charset) throws IndexOutOfBoundsException;

    /**
     * Append a <code>byte</code> at the end of this buffer.
     *
     * @param value byte
     */
    void appendByte(byte value);

    /**
     * Append a <code>2-byte UTF-16 character</code> at the end of this buffer.
     *
     * @param value char
     */
    void appendChar(char value);

    /**
     * Append a <code>boolean</code> at the end of this buffer.
     *
     * @param value boolean
     */
    void appendBoolean(boolean value);

    /**
     * Append a 16-bit <code>short</code> at the end of this buffer.
     *
     * @param value short
     */
    void appendShort(short value);

    /**
     * Append a 16-bit <code>short</code> at the end of this buffer with little endian order.
     *
     * @param value short
     */
    void appendShortLE(short value);

    /**
     * Append a 24-bit <code>medium int</code> at the end of this buffer.
     *
     * @param value short
     */
    void appendMedium(int value);

    /**
     * Append a 24-bit <code>medium int</code> at the end of this buffer with little endian order.
     *
     * @param value short
     */
    void appendMediumLE(int value);

    /**
     * Append a 32-bit <code>int</code> at the end of this buffer.
     *
     * @param value int
     */
    void appendInt(int value);

    /**
     * Append a 32-bit <code>int</code> at the end of this buffer with little endian order.
     *
     * @param value int
     */
    void appendIntLE(int value);

    /**
     * Append a 64-bit <code>long</code> at the end of this buffer.
     *
     * @param value long
     */
    void appendLong(long value);

    /**
     * Append a 64-bit <code>long</code> at the end of this buffer with little endian order.
     *
     * @param value long
     */
    void appendLongLE(long value);

    /**
     * Append a 32-bit <code>float</code> at the end of this buffer.
     *
     * @param value float
     */
    void appendFloat(float value);

    /**
     * Append a 32-bit <code>float</code> at the end of this buffer with little endian order.
     *
     * @param value float
     */
    void appendFloatLE(float value);

    /**
     * Append a 64-bit <code>double</code> at the end of this buffer.
     *
     * @param value double
     */
    void appendDouble(double value);

    /**
     * Append a 64-bit <code>double</code> at the end of this buffer with little endian order.
     *
     * @param value double
     */
    void appendDoubleLE(double value);

    /**
     * Append a <code>double</code> at the end of this buffer.
     *
     * @param value Source <code>byte array</code>
     */
    void appendBytes(byte[] value);

    /**
     * Append a <code>byte array</code> at the end of this buffer.
     *
     * @param source Source <code>byte array</code>
     * @param sourceStartIndexInclusive Source start index (inclusively, where index starts from 0)
     * @param sourceEndIndexExclusive Source end index (exclusively, where {@link #length()} is the last index)
     */
    void appendBytes(byte[] source, int sourceStartIndexInclusive, int sourceEndIndexExclusive);

    /**
     * Append a {@link CharSequence} at the end of this buffer.
     *
     * @param charSequence {@link CharSequence} source
     * @param charset {@link Charset} to use
     */
    void appendCharSequence(CharSequence charSequence, Charset charset);

    /**
     * Append a {@link String} at the end of this buffer using the default charset (UTF6).
     *
     * @param string {@link String} value
     */
    void appendString(String string);

    /**
     * Append a {@link CharSequence} at the end of this buffer.
     *
     * @param string {@link String} value
     * @param charset {@link Charset} to use
     */
    void appendString(String string, Charset charset);

    /**
     * Make a copy of this buffer from a specified <code>index</code>.
     *
     * @return Buffer with a copy of this buffer
     */
    Buffer copy();

    /**
     * Make a copy of this buffer starting from a specified <code>index inclusively</code> to
     * the <code>ending index exclusively</code>.
     *
     * @param startIndexInclusive Start index (inclusively, where index starts from 0)
     * @param endIndexExclusive End index (exclusively, where {@link #length()} is the last index)
     * @return Buffer with a copy of this buffer within specified indices
     * @throws IndexOutOfBoundsException if either indices are invalid
     */
    Buffer copy(int startIndexInclusive, int endIndexExclusive) throws IndexOutOfBoundsException;

    /**
     * Copy (and replace) the contents of this buffer from the <code>source</code> buffer.
     *
     * @param index Index of this buffer to copy to (where index starts from 0)
     * @param source Source buffer to copy from
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    void copyFrom(int index, Buffer source) throws IndexOutOfBoundsException;

    /**
     * Copy (and replace) the contents of this buffer from the <code>source</code> buffer from a specified
     * <code>index inclusively</code> to the <code>ending index exclusively</code>.
     *
     * @param index Index of this buffer to copy to (where index starts from 0)
     * @param source Source buffer to copy from
     * @param sourceStartIndexInclusive Source buffer start index (inclusively, where index starts from 0)
     * @param sourceEndIndexExclusive Source buffer end index (exclusively, where {@link #length()} is the last index)
     * @throws IndexOutOfBoundsException if either indices are invalid
     */
    void copyFrom(
            int index,
            Buffer source,
            int sourceStartIndexInclusive,
            int sourceEndIndexExclusive) throws IndexOutOfBoundsException;

    /**
     * Returns a slice of this buffer. The sliced buffer maintains independent buffer indices, but, the content is
     * shared between this and the sliced buffer.
     *
     * @return Sliced buffer
     */
    Buffer slice();

    /**
     * Returns a slice of this buffer from a specified <code>index inclusively</code> to the <code>ending index
     * exclusively</code>.
     *
     * @param startIndexInclusive Start index (inclusively, where index starts from 0)
     * @param endIndexExclusive End index (exclusively, where {@link #length()} is the last index)
     * @return Sliced buffer
     * @throws IndexOutOfBoundsException if either indices are invalid
     */
    Buffer slice(int startIndexInclusive, int endIndexExclusive) throws IndexOutOfBoundsException;

    /**
     * Make a slice of the <code>source buffer</code> into this buffer. This buffer will maintain independent buffer
     * indices, but, the content will be shared between the <code>source</code> and this buffer.
     *
     * @param source Source buffer
     */
    void sliceFrom(Buffer source);

    /**
     * Make a slice of the <code>source buffer</code> into this buffer starting from a specified <code>index
     * inclusively</code> to the <code>ending index exclusively</code>. This buffer will maintain independent buffer
     * indices, but, the content will be shared between this and the <code>source</code> buffer.
     *
     * @param source Source buffer
     * @param sourceStartIndexInclusive Source buffer start index (inclusively, where index starts from 0)
     * @param sourceEndIndexExclusive Source buffer end index (exclusively, where {@link #length()} is the last index)
     * @throws IndexOutOfBoundsException if either indices are invalid
     */
    void sliceFrom(
            Buffer source,
            int sourceStartIndexInclusive,
            int sourceEndIndexExclusive)
            throws IndexOutOfBoundsException;

    /**
     * Read all bytes from {@link InputStream} into the buffer at the specified <code>index</code>.
     *
     * @param index Index of this buffer to read to (where index starts from 0)
     * @param inputStream {@link InputStream} to read from
     * @throws IndexOutOfBoundsException if the index is invalid
     * @throws IOException if the read fails
     */
    void read(int index, InputStream inputStream) throws IndexOutOfBoundsException, IOException;

    /**
     * Read a total number of <code>lengthByIndices</code> bytes into this buffer from {@link InputStream} at the specified
     * <code>index</code>.
     *
     * @param index Index of this buffer to copy to (where index starts from 0)
     * @param inputStream {@link InputStream} to read from
     * @param length Number of bytes to read
     * @throws IndexOutOfBoundsException if the index is invalid
     * @throws IOException if the read fails
     */
    void read(int index, InputStream inputStream, int length) throws IndexOutOfBoundsException, IOException;

    /**
     * Read all bytes from {@link ScatteringByteChannel} into this buffer at the specified <code>index</code>.
     *
     * @param index Index of this buffer to read to (where index starts from 0)
     * @param scatteringByteChannel {@link ScatteringByteChannel} to read from
     * @throws IndexOutOfBoundsException if the index is invalid
     * @throws IOException if the read fails
     */
    void read(int index, ScatteringByteChannel scatteringByteChannel) throws IndexOutOfBoundsException, IOException;

    /**
     * Read a total number of <code>lengthByIndices</code> bytes into this buffer from {@link ScatteringByteChannel} at the
     * specified <code>index</code>.
     *
     * @param index Index of this buffer to read to (where index starts from 0)
     * @param scatteringByteChannel {@link ScatteringByteChannel} to read from
     * @param length Number of bytes to read
     * @throws IndexOutOfBoundsException if the index is invalid
     * @throws IOException if the read fails
     */
    void read(
            int index,
            ScatteringByteChannel scatteringByteChannel,
            int length)
            throws IndexOutOfBoundsException, IOException;

    /**
     * Read all <code>bytes</code> into this buffer from {@link FileChannel} at the specified <code>index</code>.
     *
     * @param index Index of this buffer to read to (where index starts from 0)
     * @param fileChannel {@link FileChannel} to read from
     * @throws IndexOutOfBoundsException if the index is invalid
     * @throws IOException if the read fails
     */
    void read(int index, FileChannel fileChannel) throws IndexOutOfBoundsException, IOException;

    /**
     * Read a total number of <code>lengthByIndices</code> bytes into this buffer from {@link FileChannel} at the
     * specified <code>index</code>.
     *
     * @param index Index of this buffer to read to (where index starts from 0)
     * @param fileChannel {@link FileChannel} to read from
     * @param length Number of bytes to read
     * @throws IndexOutOfBoundsException if the index is invalid
     * @throws IOException if the read fails
     */
    void read(int index, FileChannel fileChannel, int length) throws IndexOutOfBoundsException, IOException;

    /**
     * Write all bytes of this buffer starting from the specified <code>index</code> to {@link OutputStream}.
     *
     * @param index Index of this buffer to write from (where index starts from 0)
     * @param outputStream {@link OutputStream} to write to
     * @throws IndexOutOfBoundsException if the index is invalid
     * @throws IOException if the read fails
     */
    void write(int index, OutputStream outputStream) throws IndexOutOfBoundsException, IOException;

    /**
     * Write a total number of <code>lengthByIndices</code> bytes from the specified <code>index</code> to
     * {@link OutputStream}.
     *
     * @param index Index of this buffer to write from (where index starts from 0)
     * @param startIndexInclusive Start index (inclusively, where index starts from 0)
     * @param endIndexExclusive End index (exclusively, where {@link #length()} is the last index)
     * @param outputStream {@link OutputStream} to write to
     * @throws IndexOutOfBoundsException if the index is invalid
     * @throws IOException if the read fails
     */
    void write(
            int index,
            int startIndexInclusive,
            int endIndexExclusive,
            OutputStream outputStream)
            throws IndexOutOfBoundsException, IOException;

    /**
     * Write all bytes of this buffer starting from the specified <code>index</code> to {@link GatheringByteChannel}.
     *
     * @param index Index of this buffer to write from (where index starts from 0)
     * @param gatheringByteChannel {@link GatheringByteChannel} to write to
     * @throws IndexOutOfBoundsException if the index is invalid
     * @throws IOException if the read fails
     */
    void write(int index, GatheringByteChannel gatheringByteChannel) throws IndexOutOfBoundsException, IOException;

    /**
     * Write a total number of <code>lengthByIndices</code> bytes of this buffer starting from the specified
     * <code>index</code> to {@link GatheringByteChannel}.
     *
     * @param index Index of this buffer to write from (where index starts from 0)
     * @param startIndexInclusive Start index (inclusively, where index starts from 0)
     * @param endIndexExclusive End index (exclusively, where {@link #length()} is the last index)
     * @param gatheringByteChannel {@link GatheringByteChannel} to write to
     * @throws IndexOutOfBoundsException if the index is invalid
     * @throws IOException if the read fails
     */
    void write(
            int index,
            int startIndexInclusive,
            int endIndexExclusive,
            GatheringByteChannel gatheringByteChannel)
            throws IndexOutOfBoundsException, IOException;

    /**
     * Write all bytes of this buffer starting from the specified <code>index</code> to {@link FileChannel}.
     *
     * @param index Index of this buffer to write from (where index starts from 0)
     * @param outFileChannel {@link FileChannel} to write to
     * @throws IndexOutOfBoundsException if the index is invalid
     * @throws IOException if the read fails
     */
    void write(int index, FileChannel outFileChannel) throws IndexOutOfBoundsException, IOException;

    /**
     * Write a total number of <code>lengthByIndices</code> bytes of this buffer starting from the
     * specified <code>index</code> to {@link FileChannel}.
     *
     * @param index Index of this buffer to write from (where index starts from 0)
     * @param startIndexInclusive Start index (inclusively, where index starts from 0)
     * @param endIndexExclusive End index (exclusively, where {@link #length()} is the last index)
     * @param outFileChannel {@link FileChannel} to write to
     * @throws IndexOutOfBoundsException if the index is invalid
     * @throws IOException if the read fails
     */
    void write(
            int index,
            int startIndexInclusive,
            int endIndexExclusive,
            FileChannel outFileChannel)
            throws IndexOutOfBoundsException, IOException;

    /**
     * Check if this buffer is backed by a <code>byte array</code> (usually for heap buffers).
     *
     * @return true if this buffer is backed by a <code>byte array</code>, false otherwise
     */
    boolean hasBackingByteArray();

    /**
     * Get the backing <code>byte array</code> of this buffer, if {@link #hasBackingByteArray()} is true.
     *
     * @return Backing <code>byte array</code>
     * @throws UnsupportedOperationException if {@link #hasBackingByteArray()} is false
     */
    byte[] backingByteArray() throws UnsupportedOperationException;

    /**
     * Get the offset of the first byte within the backing byte array of this buffer.
     *
     * @return First byte offset
     * @throws UnsupportedOperationException if {@link #hasBackingByteArray()} is false
     */
    int backingArrayOffset() throws UnsupportedOperationException;

    /**
     * Check if this buffer is backed by a <code>direct memory address</code> (usually for direct buffers).
     *
     * @return true if this buffer is backed by a <code>direct memory address</code>, false otherwise
     */
    boolean hasMemoryAddress();

    /**
     * Get the <code>memory address</code> that this buffer is using, if {@link #hasMemoryAddress()} returns true.
     *
     * @return Backing <code>memory address</code>
     * @throws UnsupportedOperationException if {@link #hasMemoryAddress()} is false
     */
    long memoryAddress() throws UnsupportedOperationException;


    /**
     * Check if this buffer is backed or can be converted to {@link java.nio.ByteBuffer}s.
     *
     * @return true if this buffer is backed by {@link java.nio.ByteBuffer}, false otherwise
     */
    boolean hasNioByteBuffers();

    /**
     * Convert this buffer to an array of {@link ByteBuffer}s.
     *
     * @return Array of {@link ByteBuffer}s
     * @throws UnsupportedOperationException if {@link #hasNioByteBuffers()} is false
     * @see ByteBuf#nioBuffers()
     */
    ByteBuffer[] nioByteBuffers() throws UnsupportedOperationException;


    /**
     * Get {@code netty} {@link ByteBuf}.
     *
     * @return {@link ByteBuf}
     */
    ByteBuf getNettyByteBuf();


    /**
     * Set {@code netty} {@link ByteBuf}.
     *
     * @param byteBuf {@link ByteBuf}
     */
    void setNettyByteBuf(ByteBuf byteBuf);

    /**
     * Get the lengthByIndices of the content in this buffer.
     *
     * @return Content lengthByIndices
     */
    int length();

    /**
     * Get the current capacity of this buffer.
     *
     * @return Capacity
     */
    int capacity();

    /**
     * Get the {@link BufferPoolingStrategy} of this buffer.
     *
     * @return {@link BufferPoolingStrategy}
     */
    BufferPoolingStrategy getBufferPoolingStrategy();

    /**
     * Get the {@link BufferSource} of this buffer.
     *
     * @return {@link BufferSource}
     */
    BufferSource getBufferSource();
}

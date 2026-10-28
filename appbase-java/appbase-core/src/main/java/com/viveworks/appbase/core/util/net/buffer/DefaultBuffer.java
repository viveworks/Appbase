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

import com.viveworks.appbase.core.AppbaseRuntimeException;
import com.viveworks.appbase.core.util.AppbaseUtil;
import com.viveworks.appbase.core.util.net.AppbaseNetUtil;
import io.netty.buffer.ByteBuf;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Default implementation of {@link Buffer}.
 *
 * @author Navaneeth Mysore G
 * @since 1.0.0
 */
@NotThreadSafe
@SuppressWarnings("checkstyle:MethodCount")
public class DefaultBuffer implements Buffer {
    private volatile ByteBuf byteBuf;
    private final BufferAllocator bufferAllocator;
    private final BufferPoolingStrategy bufferPoolingStrategy;
    private final BufferSource bufferSource;
    private final AtomicBoolean reset;

    public DefaultBuffer(
            final ByteBuf byteBuf,
            final BufferAllocator bufferAllocator,
            final BufferPoolingStrategy bufferPoolingStrategy,
            final BufferSource bufferSource)
    {
        AppbaseUtil.notNull(byteBuf, "byteBuf");
        AppbaseUtil.notNull(bufferAllocator, "bufferAllocator");
        AppbaseUtil.notNull(bufferPoolingStrategy, "bufferPoolingStrategy");
        AppbaseUtil.notNull(bufferSource, "bufferSource");

        this.byteBuf = byteBuf;
        this.bufferAllocator = bufferAllocator;
        this.bufferPoolingStrategy = bufferPoolingStrategy;
        this.bufferSource = bufferSource;
        this.reset = new AtomicBoolean(false);
    }

    /**
     * Set all fields.
     */
    public void setAll() {
        // No-op, all fields are set during construction. Just defined for consistency.
    }

    @Override
    public byte getByte(final int index) throws IndexOutOfBoundsException {
        return byteBuf.getByte(index);
    }

    @Override
    public short getUnsignedByte(final int index) throws IndexOutOfBoundsException {
        return byteBuf.getUnsignedByte(index);
    }

    @Override
    public char getChar(final int index) throws IndexOutOfBoundsException {
        return byteBuf.getChar(index);
    }

    @Override
    public boolean getBoolean(final int index) throws IndexOutOfBoundsException {
        return byteBuf.getBoolean(index);
    }

    @Override
    public short getShort(final int index) throws IndexOutOfBoundsException {
        return byteBuf.getShort(index);
    }

    @Override
    public short getShortLE(final int index) throws IndexOutOfBoundsException {
        return byteBuf.getShortLE(index);
    }

    @Override
    public int getUnsignedShort(final int index) throws IndexOutOfBoundsException {
        return byteBuf.getUnsignedShort(index);
    }

    @Override
    public int getUnsignedShortLE(final int index) throws IndexOutOfBoundsException {
        return byteBuf.getUnsignedShortLE(index);
    }

    @Override
    public int getMedium(final int index) throws IndexOutOfBoundsException {
        return byteBuf.getMedium(index);
    }

    @Override
    public int getMediumLE(final int index) throws IndexOutOfBoundsException {
        return byteBuf.getMediumLE(index);
    }

    @Override
    public int getUnsignedMedium(final int index) throws IndexOutOfBoundsException {
        return byteBuf.getUnsignedMedium(index);
    }

    @Override
    public int getUnsignedMediumLE(final int index) throws IndexOutOfBoundsException {
        return byteBuf.getUnsignedMediumLE(index);
    }

    @Override
    public int getInt(final int index) throws IndexOutOfBoundsException {
        return byteBuf.getInt(index);
    }

    @Override
    public int getIntLE(final int index) throws IndexOutOfBoundsException {
        return byteBuf.getIntLE(index);
    }

    @Override
    public long getUnsignedInt(final int index) throws IndexOutOfBoundsException {
        return byteBuf.getUnsignedInt(index);
    }

    @Override
    public long getUnsignedIntLE(final int index) throws IndexOutOfBoundsException {
        return byteBuf.getUnsignedIntLE(index);
    }

    @Override
    public long getLong(final int index) throws IndexOutOfBoundsException {
        return byteBuf.getLong(index);
    }

    @Override
    public long getLongLE(final int index) throws IndexOutOfBoundsException {
        return byteBuf.getLongLE(index);
    }

    @Override
    public float getFloat(final int index) throws IndexOutOfBoundsException {
        return byteBuf.getFloat(index);
    }

    @Override
    public float getFloatLE(final int index) throws IndexOutOfBoundsException {
        return byteBuf.getFloatLE(index);
    }

    @Override
    public double getDouble(final int index) throws IndexOutOfBoundsException {
        return byteBuf.getDouble(index);
    }

    @Override
    public double getDoubleLE(final int index) throws IndexOutOfBoundsException {
        return byteBuf.getDoubleLE(index);
    }

    @Override
    public byte[] getBytes(final int index) throws IndexOutOfBoundsException {
        return getBytes(index, length());
    }

    @Override
    public byte[] getBytes(final int startIndexInclusive,
                           final int endIndexExclusive) throws IndexOutOfBoundsException
    {
        AppbaseUtil.positiveOrZero(startIndexInclusive, "startIndexInclusive");
        AppbaseUtil.positiveOrZero(endIndexExclusive, "endIndexExclusive");

        final int capacity = capacity();
        if (startIndexInclusive > capacity) {
            throw new IndexOutOfBoundsException("Buffer overflow [startIndexInclusive=" +
                    startIndexInclusive + ", capacity=" + capacity + "]");
        }
        if (endIndexExclusive > capacity) {
            throw new IndexOutOfBoundsException("Buffer overflow [endIndexExclusive=" +
                    endIndexExclusive + ", capacity=" + capacity + "]");
        }

        final byte[] bytes = new byte[capacity];
        byteBuf.getBytes(
                startIndexInclusive,
                bytes,
                0,
                AppbaseNetUtil.lengthByIndices(startIndexInclusive, endIndexExclusive));
        return bytes;
    }

    @Override
    public void getBytes(final int startIndexInclusive,
                         final int endIndexExclusive,
                         final byte[] destination,
                         final int destinationStartIndex) throws IndexOutOfBoundsException
    {
        byteBuf.getBytes(
                startIndexInclusive,
                destination,
                destinationStartIndex,
                AppbaseNetUtil.lengthByIndices(startIndexInclusive, endIndexExclusive));
    }

    @Override
    public CharSequence getCharSequence(final int startIndexInclusive,
                                        final int endIndexExclusive,
                                        final Charset charset) throws IndexOutOfBoundsException
    {
        return byteBuf.getCharSequence(
                startIndexInclusive, AppbaseNetUtil.lengthByIndices(startIndexInclusive, endIndexExclusive), charset);
    }

    @Override
    public String getString() {
        return getString(0, length());
    }

    @Override
    public String getString(final Charset charset) {
        AppbaseUtil.notNull(charset, "charset");

        return getString(0, length(), charset);
    }

    @Override
    public String getString(final int startIndexInclusive,
                            final int endIndexExclusive) throws IndexOutOfBoundsException
    {
        AppbaseNetUtil.checkIndices(startIndexInclusive, endIndexExclusive);

        return getString(startIndexInclusive, endIndexExclusive, StandardCharsets.UTF_8);
    }

    @Override
    public String getString(final int startIndexInclusive,
                            final int endIndexExclusive,
                            final Charset charset) throws IndexOutOfBoundsException
    {
        AppbaseNetUtil.checkIndices(startIndexInclusive, endIndexExclusive);
        AppbaseUtil.notNull(charset, "charset");

        final byte[] bytes = getBytes(startIndexInclusive, endIndexExclusive);
        return new String(bytes, charset);
    }

    @Override
    public void setByte(final int index, final byte value) throws IndexOutOfBoundsException {
        AppbaseUtil.positiveOrZero(index, "index");

        ensureCapacity(index + AppbaseNetUtil.SIZE_OF_BYTE);
        byteBuf.setByte(index, value);
    }

    @Override
    public void setChar(final int index, final char value) throws IndexOutOfBoundsException {
        AppbaseUtil.positiveOrZero(index, "index");

        ensureCapacity(index + AppbaseNetUtil.SIZE_OF_CHAR);
        byteBuf.setChar(index, value);
    }

    @Override
    public void setBoolean(final int index, final boolean value) throws IndexOutOfBoundsException {
        AppbaseUtil.positiveOrZero(index, "index");

        ensureCapacity(index + AppbaseNetUtil.SIZE_OF_BOOLEAN);
        byteBuf.setBoolean(index, value);
    }

    @Override
    public void setShort(final int index, final short value) throws IndexOutOfBoundsException {
        AppbaseUtil.positiveOrZero(index, "index");

        ensureCapacity(index + AppbaseNetUtil.SIZE_OF_SHORT);
        byteBuf.setShort(index, value);
    }

    @Override
    public void setShortLE(final int index, final short value) throws IndexOutOfBoundsException {
        AppbaseUtil.positiveOrZero(index, "index");

        ensureCapacity(index + AppbaseNetUtil.SIZE_OF_SHORT);
        byteBuf.setShortLE(index, value);
    }

    @Override
    public void setMedium(final int index, final int value) throws IndexOutOfBoundsException {
        AppbaseUtil.positiveOrZero(index, "index");

        ensureCapacity(index + AppbaseNetUtil.SIZE_OF_MEDIUM);
        byteBuf.setMedium(index, value);
    }

    @Override
    public void setMediumLE(final int index, final int value) throws IndexOutOfBoundsException {
        AppbaseUtil.positiveOrZero(index, "index");

        ensureCapacity(index + AppbaseNetUtil.SIZE_OF_MEDIUM);
        byteBuf.setMediumLE(index, value);
    }

    @Override
    public void setInt(final int index, final int value) throws IndexOutOfBoundsException {
        AppbaseUtil.positiveOrZero(index, "index");

        ensureCapacity(index + AppbaseNetUtil.SIZE_OF_INT);
        byteBuf.setInt(index, value);
    }

    @Override
    public void setIntLE(final int index, final int value) throws IndexOutOfBoundsException {
        AppbaseUtil.positiveOrZero(index, "index");

        ensureCapacity(index + AppbaseNetUtil.SIZE_OF_INT);
        byteBuf.setIntLE(index, value);
    }

    @Override
    public void setLong(final int index, final long value) throws IndexOutOfBoundsException {
        AppbaseUtil.positiveOrZero(index, "index");

        ensureCapacity(index + AppbaseNetUtil.SIZE_OF_LONG);
        byteBuf.setLong(index, value);
    }

    @Override
    public void setLongLE(final int index, final long value) throws IndexOutOfBoundsException {
        AppbaseUtil.positiveOrZero(index, "index");

        ensureCapacity(index + AppbaseNetUtil.SIZE_OF_LONG);
        byteBuf.setLongLE(index, value);
    }

    @Override
    public void setFloat(final int index, final float value) throws IndexOutOfBoundsException {
        AppbaseUtil.positiveOrZero(index, "index");

        ensureCapacity(index + AppbaseNetUtil.SIZE_OF_FLOAT);
        byteBuf.setFloat(index, value);
    }

    @Override
    public void setFloatLE(final int index, final float value) throws IndexOutOfBoundsException {
        AppbaseUtil.positiveOrZero(index, "index");

        ensureCapacity(index + AppbaseNetUtil.SIZE_OF_FLOAT);
        byteBuf.setFloatLE(index, value);
    }

    @Override
    public void setDouble(final int index, final double value) throws IndexOutOfBoundsException {
        AppbaseUtil.positiveOrZero(index, "index");

        ensureCapacity(index + AppbaseNetUtil.SIZE_OF_DOUBLE);
        byteBuf.setDouble(index, value);
    }

    @Override
    public void setDoubleLE(final int index, final double value) throws IndexOutOfBoundsException {
        AppbaseUtil.positiveOrZero(index, "index");

        ensureCapacity(index + AppbaseNetUtil.SIZE_OF_DOUBLE);
        byteBuf.setDoubleLE(index, value);
    }

    @Override
    public void setBytes(final int index, final byte[] source) throws IndexOutOfBoundsException {
        AppbaseUtil.positiveOrZero(index, "index");
        AppbaseUtil.notNull(source, "source");

        setBytes(index, source, 0, source.length);
    }

    @Override
    public void setBytes(final int index,
                         final byte[] source,
                         final int sourceStartIndexInclusive,
                         final int sourceEndIndexExclusive) throws IndexOutOfBoundsException
    {
        AppbaseUtil.positiveOrZero(index, "index");
        AppbaseUtil.notNull(source, "source");
        AppbaseNetUtil.checkIndices(sourceStartIndexInclusive, sourceEndIndexExclusive);

        ensureCapacity(source.length);
        byteBuf.setBytes(
                index,
                source,
                sourceStartIndexInclusive,
                AppbaseNetUtil.lengthByIndices(sourceStartIndexInclusive, sourceEndIndexExclusive));
    }

    @Override
    public void setCharSequence(final int index,
                                final CharSequence charSequence,
                                final Charset charset) throws IndexOutOfBoundsException
    {
        AppbaseUtil.positiveOrZero(index, "index");
        AppbaseUtil.notNull(charSequence, "charSequence");
        AppbaseUtil.notNull(charset, "charset");

        ensureCapacity(charSequence.length() * AppbaseNetUtil.SIZE_OF_CHAR);
        byteBuf.setCharSequence(index, charSequence, charset);
    }

    @Override
    public void setString(final int index, final String string) throws IndexOutOfBoundsException {
        setString(index, string, StandardCharsets.UTF_8);
    }

    @Override
    public void setString(final int index,
                          final String string,
                          final Charset charset) throws IndexOutOfBoundsException
    {
        AppbaseUtil.positiveOrZero(index, "index");
        AppbaseUtil.notNull(string, "string");
        AppbaseUtil.notNull(charset, "charset");

        final byte[] bytes = string.getBytes(charset);
        setBytes(index, bytes, 0, bytes.length);
    }

    @Override
    public void appendByte(final byte value) {
        ensureExpandableBy(AppbaseNetUtil.SIZE_OF_BYTE);
        byteBuf.writeByte(value);
    }

    @Override
    public void appendChar(final char value) {
        ensureExpandableBy(AppbaseNetUtil.SIZE_OF_CHAR);
        byteBuf.writeChar(value);
    }

    @Override
    public void appendBoolean(final boolean value) {
        ensureExpandableBy(AppbaseNetUtil.SIZE_OF_BOOLEAN);
        byteBuf.writeBoolean(value);
    }

    @Override
    public void appendShort(final short value) {
        ensureExpandableBy(AppbaseNetUtil.SIZE_OF_SHORT);
        byteBuf.writeShort(value);
    }

    @Override
    public void appendShortLE(final short value) {
        ensureExpandableBy(AppbaseNetUtil.SIZE_OF_SHORT);
        byteBuf.writeShortLE(value);
    }

    @Override
    public void appendMedium(final int value) {
        ensureExpandableBy(AppbaseNetUtil.SIZE_OF_MEDIUM);
        byteBuf.writeMedium(value);
    }

    @Override
    public void appendMediumLE(final int value) {
        ensureExpandableBy(AppbaseNetUtil.SIZE_OF_MEDIUM);
        byteBuf.writeMediumLE(value);
    }

    @Override
    public void appendInt(final int value) {
        ensureExpandableBy(AppbaseNetUtil.SIZE_OF_INT);
        byteBuf.writeInt(value);
    }

    @Override
    public void appendIntLE(final int value) {
        ensureExpandableBy(AppbaseNetUtil.SIZE_OF_INT);
        byteBuf.writeIntLE(value);
    }

    @Override
    public void appendLong(final long value) {
        ensureExpandableBy(AppbaseNetUtil.SIZE_OF_LONG);
        byteBuf.writeLong(value);
    }

    @Override
    public void appendLongLE(final long value) {
        ensureExpandableBy(AppbaseNetUtil.SIZE_OF_LONG);
        byteBuf.writeLongLE(value);
    }

    @Override
    public void appendFloat(final float value) {
        ensureExpandableBy(AppbaseNetUtil.SIZE_OF_FLOAT);
        byteBuf.writeFloat(value);
    }

    @Override
    public void appendFloatLE(final float value) {
        ensureExpandableBy(AppbaseNetUtil.SIZE_OF_FLOAT);
        byteBuf.writeFloatLE(value);
    }

    @Override
    public void appendDouble(final double value) {
        ensureExpandableBy(AppbaseNetUtil.SIZE_OF_DOUBLE);
        byteBuf.writeDouble(value);
    }

    @Override
    public void appendDoubleLE(final double value) {
        ensureExpandableBy(AppbaseNetUtil.SIZE_OF_DOUBLE);
        byteBuf.writeDoubleLE(value);
    }

    @Override
    public void appendBytes(final byte[] value) {
        AppbaseUtil.notNull(value, "value");

        ensureExpandableBy(value.length);
        byteBuf.writeBytes(value);
    }

    @Override
    public void appendBytes(final byte[] source,
                            final int sourceStartIndexInclusive,
                            final int sourceEndIndexExclusive)
    {
        AppbaseUtil.notNull(source, "source");
        AppbaseNetUtil.checkIndices(sourceStartIndexInclusive, sourceEndIndexExclusive);

        if (sourceEndIndexExclusive > source.length) {
            throw new AppbaseRuntimeException("Source end index " +
                    sourceEndIndexExclusive + " must be less than the lengthByIndices " + source.length);
        }

        final int length = AppbaseNetUtil.lengthByIndices(sourceStartIndexInclusive, sourceEndIndexExclusive);
        ensureExpandableBy(length);
        byteBuf.writeBytes(source, sourceStartIndexInclusive, length);
    }

    @Override
    public void appendCharSequence(final CharSequence charSequence, final Charset charset) {
        AppbaseUtil.notNull(charSequence, "charSequence");
        AppbaseUtil.notNull(charset, "charset");

        ensureExpandableBy(charSequence.length() * AppbaseNetUtil.SIZE_OF_CHAR);
        byteBuf.writeCharSequence(charSequence, charset);
    }

    @Override
    public void appendString(final String string) {
        AppbaseUtil.notNull(string, "string");

        appendString(string, StandardCharsets.UTF_8);
    }

    @Override
    public void appendString(final String string, final Charset charset) {
        AppbaseUtil.notNull(string, "string");
        AppbaseUtil.notNull(charset, "charset");

        final byte[] bytes = string.getBytes(charset);
        ensureExpandableBy(bytes.length);
        byteBuf.writeCharSequence(string, charset);
    }

    private Buffer copy(final ByteBuf copy) {
        return new DefaultBuffer(copy, bufferAllocator, bufferPoolingStrategy, bufferSource);
    }

    @Override
    public Buffer copy() {
        final ByteBuf copy = byteBuf.copy();
        return copy(copy);
    }

    @Override
    public Buffer copy(final int startIndexInclusive, final int endIndexExclusive) throws IndexOutOfBoundsException {
        AppbaseNetUtil.checkIndices(startIndexInclusive, endIndexExclusive);

        final int length = AppbaseNetUtil.lengthByIndices(startIndexInclusive, endIndexExclusive);
        final ByteBuf copy = byteBuf.copy(startIndexInclusive, length);
        return copy(copy);
    }

    @Override
    public void copyFrom(final int index, final Buffer source) throws IndexOutOfBoundsException {
        AppbaseUtil.positiveOrZero(index, "index");
        AppbaseUtil.notNull(source, "source");

        copyFrom(index, source, 0, length());
    }

    @Override
    public void copyFrom(final int index,
                         final Buffer source,
                         final int sourceStartIndexInclusive,
                         final int sourceEndIndexExclusive) throws IndexOutOfBoundsException
    {
        AppbaseUtil.positiveOrZero(index, "index");
        AppbaseUtil.notNull(source, "source");
        AppbaseNetUtil.checkIndices(sourceStartIndexInclusive, sourceEndIndexExclusive);

        final int length = AppbaseNetUtil.lengthByIndices(sourceStartIndexInclusive, sourceEndIndexExclusive);
        final ByteBuf sourceByteBuf = source.getNettyByteBuf();
        final ByteBuf copy = sourceByteBuf.copy(sourceStartIndexInclusive, length);
        copy.clear();

        this.byteBuf.clear();
        this.byteBuf.release();
        this.byteBuf = copy;
    }

    @Override
    public Buffer slice() {
        return slice(0, length());
    }

    @Override
    public Buffer slice(final int startIndexInclusive, final int endIndexExclusive) throws IndexOutOfBoundsException {
        AppbaseNetUtil.checkIndices(startIndexInclusive, endIndexExclusive);

        final int length = AppbaseNetUtil.lengthByIndices(startIndexInclusive, endIndexExclusive);
        return new DefaultBuffer(
                this.byteBuf.slice(startIndexInclusive, length),
                bufferAllocator,
                bufferPoolingStrategy,
                bufferSource);
    }

    @Override
    public void sliceFrom(final Buffer source) {
        AppbaseUtil.notNull(source, "source");

        sliceFrom(source, 0, source.length());
    }

    @Override
    public void sliceFrom(final Buffer source,
                          final int sourceStartIndexInclusive,
                          final int sourceEndIndexExclusive) throws IndexOutOfBoundsException
    {
        AppbaseUtil.notNull(source, "source");
        AppbaseNetUtil.checkIndices(sourceStartIndexInclusive, sourceEndIndexExclusive);

        final int length = AppbaseNetUtil.lengthByIndices(sourceStartIndexInclusive, sourceEndIndexExclusive);
        final ByteBuf nettyByteBuf = source.getNettyByteBuf();
        final ByteBuf slice = nettyByteBuf.slice(sourceStartIndexInclusive, length);

        this.byteBuf.clear();
        this.byteBuf.release();
        this.byteBuf = slice;
    }

    @Override
    public void read(final int index,
                     final InputStream inputStream) throws IndexOutOfBoundsException, IOException
    {
        read(index, inputStream, length());
    }

    @Override
    public void read(final int index,
                     final InputStream inputStream,
                     final int length) throws IndexOutOfBoundsException, IOException
    {
        AppbaseUtil.positiveOrZero(index, "index");
        AppbaseUtil.notNull(inputStream, "inputStream");
        AppbaseUtil.positiveOrZero(length, "lengthByIndices");

        this.byteBuf.setBytes(index, inputStream, length);
    }

    @Override
    public void read(final int index,
                     final ScatteringByteChannel scatteringByteChannel) throws IndexOutOfBoundsException, IOException
    {
        read(index, scatteringByteChannel, length());
    }

    @Override
    public void read(final int index,
                     final ScatteringByteChannel scatteringByteChannel,
                     final int length) throws IndexOutOfBoundsException, IOException
    {
        AppbaseUtil.positiveOrZero(index, "index");
        AppbaseUtil.notNull(scatteringByteChannel, "scatteringByteChannel");
        AppbaseUtil.notNull(length, "lengthByIndices");

        this.byteBuf.setBytes(index, scatteringByteChannel, length);
    }

    @Override
    public void read(final int index, final FileChannel fileChannel) throws IndexOutOfBoundsException, IOException {
        read(index, fileChannel, length());
    }

    @Override
    public void read(final int index,
                     final FileChannel fileChannel,
                     final int length) throws IndexOutOfBoundsException, IOException
    {
        AppbaseUtil.positiveOrZero(index, "index");
        AppbaseUtil.notNull(fileChannel, "fileChannel");
        AppbaseUtil.positiveOrZero(length, "lengthByIndices");

        this.byteBuf.setBytes(index, fileChannel, length);
    }

    @Override
    public void write(final int index, final OutputStream outputStream) throws IndexOutOfBoundsException, IOException {
        write(index, 0, length(), outputStream);
    }

    @Override
    public void write(final int index,
                      final int startIndexInclusive,
                      final int endIndexExclusive,
                      final OutputStream outputStream) throws IndexOutOfBoundsException, IOException
    {
        AppbaseUtil.positiveOrZero(index, "index");
        AppbaseUtil.positiveOrZero(startIndexInclusive, "startIndexInclusive");
        AppbaseUtil.positiveOrZero(endIndexExclusive, "endIndexExclusive");
        AppbaseUtil.notNull(outputStream, "outputStream");

        final int length = AppbaseNetUtil.lengthByIndices(startIndexInclusive, endIndexExclusive);
        this.byteBuf.getBytes(index, outputStream, length);
    }

    @Override
    public void write(
            final int index,
            final GatheringByteChannel gatheringByteChannel)
            throws IndexOutOfBoundsException, IOException
    {
        write(index, 0, length(), gatheringByteChannel);
    }

    @Override
    public void write(
            final int index,
            final int startIndexInclusive,
            final int endIndexExclusive,
            final GatheringByteChannel gatheringByteChannel) throws IndexOutOfBoundsException, IOException
    {
        AppbaseUtil.positiveOrZero(index, "index");
        AppbaseUtil.positiveOrZero(startIndexInclusive, "startIndexInclusive");
        AppbaseUtil.positiveOrZero(endIndexExclusive, "endIndexExclusive");
        AppbaseUtil.notNull(gatheringByteChannel, "gatheringByteChannel");

        final int length = AppbaseNetUtil.lengthByIndices(startIndexInclusive, endIndexExclusive);
        this.byteBuf.getBytes(index, gatheringByteChannel, length);
    }

    @Override
    public void write(final int index, final FileChannel outFileChannel) throws IndexOutOfBoundsException, IOException {
        write(index, 0, length(), outFileChannel);
    }

    @Override
    public void write(
            final int index,
            final int startIndexInclusive,
            final int endIndexExclusive,
            final FileChannel outFileChannel)
            throws IndexOutOfBoundsException, IOException
    {
        AppbaseUtil.positiveOrZero(index, "index");
        AppbaseUtil.positiveOrZero(startIndexInclusive, "startIndexInclusive");
        AppbaseUtil.positiveOrZero(endIndexExclusive, "endIndexExclusive");
        AppbaseUtil.notNull(outFileChannel, "outFileChannel");

        final int length = AppbaseNetUtil.lengthByIndices(startIndexInclusive, endIndexExclusive);
        this.byteBuf.getBytes(index, outFileChannel, length);
    }

    @Override
    public boolean hasBackingByteArray() {
        return byteBuf.hasArray();
    }

    @Override
    public byte[] backingByteArray() throws UnsupportedOperationException {
        return byteBuf.array();
    }

    @Override
    public int backingArrayOffset() throws UnsupportedOperationException {
        return byteBuf.arrayOffset();
    }

    @Override
    public boolean hasMemoryAddress() {
        return byteBuf.hasMemoryAddress();
    }

    @Override
    public long memoryAddress() throws UnsupportedOperationException {
        return byteBuf.memoryAddress();
    }

    @Override
    public boolean hasNioByteBuffers() {
        return byteBuf.nioBufferCount() > -1;
    }

    @Override
    public ByteBuffer[] nioByteBuffers() throws UnsupportedOperationException {
        return byteBuf.nioBuffers();
    }

    @Override
    public ByteBuf getNettyByteBuf() {
        return byteBuf;
    }

    @Override
    public void setNettyByteBuf(final ByteBuf byteBuf) {
        AppbaseUtil.notNull(byteBuf, "byteBuf");

        this.byteBuf = byteBuf;
    }

    @Override
    public int length() {
        return byteBuf.readableBytes();
    }

    @Override
    public int capacity() {
        return byteBuf.capacity();
    }

    @Override
    public BufferPoolingStrategy getBufferPoolingStrategy() {
        return bufferPoolingStrategy;
    }

    @Override
    public BufferSource getBufferSource() {
        return bufferSource;
    }

    @Override
    public boolean isValid() {
        // All state is set in the constructor, this object is valid at construction.
        return true;
    }

    @Override
    public boolean isClean() {
        return reset.get();
    }

    @Override
    public void clean() {
        if (reset.compareAndSet(false, true)) {
            byteBuf.clear();
            byteBuf.release();
            byteBuf = null;
        }
    }

    private void ensureCapacity(final int newLength) {
        AppbaseUtil.positiveOrZero(newLength, "newLength");

        final int capacity = byteBuf.capacity();
        final int over = newLength - capacity;
        final int writerIndex = byteBuf.writerIndex();

        if (over > 0) {
            final int maxCapacity = byteBuf.maxCapacity();
            if (capacity + over > maxCapacity) {
                maxBufferCopy(capacity + over);
            }
            byteBuf.ensureWritable(newLength - writerIndex);
        }
        if (newLength > writerIndex) {
            byteBuf.writerIndex(newLength);
        }
    }

    private void ensureExpandableBy(final int amount) {
        AppbaseUtil.positiveOrZero(amount, "amount");

        final int newLength = byteBuf.writerIndex() + amount;
        if (newLength > byteBuf.maxCapacity()) {
            maxBufferCopy(newLength);
        }
    }

    private void maxBufferCopy(final int capacity) {
        AppbaseUtil.positiveOrZero(capacity, "capacity");

        final Buffer newBuffer = bufferAllocator.buffer(capacity, AppbaseNetUtil.MAX_BUFFER_LENGTH);
        final ByteBuf newByteBuf = newBuffer.getNettyByteBuf();
        newByteBuf.writeBytes(this.byteBuf);
        this.byteBuf = newByteBuf;
    }
}

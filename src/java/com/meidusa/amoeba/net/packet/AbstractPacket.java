package com.meidusa.amoeba.net.packet;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.meidusa.amoeba.net.Connection;

/**
 * 数据包抽象类
 * @author struct
 */
public abstract class AbstractPacket implements Packet {
	 /**
     * 从buffer(含包头) 中初始化数据包
     * @param buffer buffer是从socketChannel的流读取头n个字节计算数据包长度 并且读取相应的长度所形成的buffer
     */
    public void init(byte[] buffer, Connection conn) {
        AbstractPacketBuffer packetBuffer = constractorBuffer(buffer);
        packetBuffer.init(conn);
        init(packetBuffer);
        afterInit(packetBuffer);
    }

    /**
     * 分析数据包(分析包头+数据区域,分析完包头以后应该将Buffer的postion设置到数据区)
     */
    protected abstract void init(AbstractPacketBuffer buffer);

    /**
     * 做完初始化以后
     */
    protected void afterInit(AbstractPacketBuffer buffer) {
    }
    /**
     * 将数据包转化成ByteBuffer,byteBuffer中包含有包头信息
     */
    public ByteBuffer toByteBuffer(Connection conn) {
        try {
            int bufferSize = calculatePacketSize();
            AbstractPacketBuffer packetBuffer = constractorBuffer(bufferSize);
            packetBuffer.init(conn);
            return toBuffer(packetBuffer).toByteBuffer();
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    private AbstractPacketBuffer constractorBuffer(int bufferSize) {
        AbstractPacketBuffer buffer = null;
        try {
            Constructor<? extends AbstractPacketBuffer> constractor = getPacketBufferClass().getConstructor(int.class);
            buffer = constractor.newInstance(bufferSize);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return buffer;
    }

    /**
     * <pre>
     *  该方法调用了{@link #write2Buffer(PacketBuffer)} 写入到指定的buffer， 
     *  并且调用了{@link #afterPacketWritten(PacketBuffer)}
     * </pre>
     */
    private AbstractPacketBuffer toBuffer(AbstractPacketBuffer buffer) throws UnsupportedEncodingException {
        write2Buffer(buffer);
        afterPacketWritten(buffer);
        return buffer;
    }

    /**
     * 包含头的消息封装
     */
    protected abstract void write2Buffer(AbstractPacketBuffer buffer) throws UnsupportedEncodingException;

    /**
     * <pre>
     * 写完之后一定需要调用这个方法，buffer的指针位置指向末尾的下一个位置（包总长度位置）。
     * 这儿一般是计算数据包总长度,或者其他需要数据包写完才能完成的数据
     * </pre>
     */
    protected abstract void afterPacketWritten(AbstractPacketBuffer buffer);

    /**
     * 估算packet的大小，估算的太大浪费内存，估算的太小会影响性能
     */
    protected abstract int calculatePacketSize();

    private AbstractPacketBuffer constractorBuffer(byte[] buffer) {
        AbstractPacketBuffer packetbuffer = null;
        try {
            Constructor<? extends AbstractPacketBuffer> constractor = getPacketBufferClass().getConstructor(byte[].class);
            packetbuffer = constractor.newInstance(buffer);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return packetbuffer;
    }

    protected abstract Class<? extends AbstractPacketBuffer> getPacketBufferClass();

    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}

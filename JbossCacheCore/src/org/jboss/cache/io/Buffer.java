package org.jboss.cache.io;


public class Buffer {
    private final byte[] buf;
    private final int offset;
    private final int length;

    public Buffer(byte[] buf, int offset, int length) {
        this.buf=buf;
        this.offset=offset;
        this.length=length;
    }

    public byte[] getBuf() {
        return buf;
    }

    public int getOffset() {
        return offset;
    }

    public int getLength() {
        return length;
    }

    public Buffer copy() {
        byte[] new_buf=buf != null? new byte[length] : null;
        int new_length=new_buf != null? new_buf.length : 0;
        if(new_buf != null)
            System.arraycopy(buf, offset, new_buf, 0, length);
        return new Buffer(new_buf, 0, new_length);
    }

    public String toString() {
        StringBuilder sb=new StringBuilder();
        sb.append(length).append(" bytes");
        if(offset > 0)
            sb.append(" (offset=").append(offset).append(")");
        return sb.toString();
    }

}
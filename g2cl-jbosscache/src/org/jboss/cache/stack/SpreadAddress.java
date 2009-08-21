package org.jboss.cache.stack;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import net.sf.jgcs.spread.SpGroup;

import org.jgroups.Address;
import org.jgroups.Global;

public class SpreadAddress extends SpGroup implements Address {
	int size = -1;
	private byte[] additional_data;
	
	// Used only by Externalization
	public SpreadAddress(){
		super("");
	}
		
    public SpreadAddress(String group) {
    	super(group);
    }

	

	public boolean isMulticastAddress() {
		return true;
	}

	public int size() {
		if (size >= 0)
			return size;
		// length (1 bytes) + 4 bytes for port + 1 for additional_data available
		int tmp_size = Global.BYTE_SIZE + Global.SHORT_SIZE + Global.BYTE_SIZE;
		size = tmp_size;

		return tmp_size;
	}

	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		int len = in.readByte();
		if (len > 0) {
			// read the four bytes
			byte[] a = new byte[len];
			// in theory readFully(byte[]) should be faster
			// than read(byte[]) since latter reads
			// 4 bytes one at a time
			in.readFully(a);
			// look up an instance in the cache
		}
		// then read the port

		if (in.readBoolean() == false)
			return;
		len = in.readShort();
		if (len > 0) {
			additional_data = new byte[len];
			in.readFully(additional_data, 0, additional_data.length);
		}

	}

	public void writeExternal(ObjectOutput out) throws IOException {

		out.writeByte(0);

		if (additional_data != null) {
			out.writeBoolean(true);
			out.writeShort(additional_data.length);
			out.write(additional_data, 0, additional_data.length);
		} else
			out.writeBoolean(false);

	}

	public void readFrom(DataInputStream in) throws IOException,
			IllegalAccessException, InstantiationException {
		int len = in.readByte();
		if (len > 0 && (len != Global.IPV4_SIZE && len != Global.IPV6_SIZE))
			throw new IOException("length has to be " + Global.IPV4_SIZE
					+ " or " + Global.IPV6_SIZE + " bytes (was " + len
					+ " bytes)");
		byte[] a = new byte[len]; // 4 bytes (IPv4) or 16 bytes (IPv6)
		in.readFully(a);

		// changed from readShort(): we need the full 65535, with a short we'd
		// only get up to 32K !

		if (in.readBoolean() == false)
			return;
		len = in.readUnsignedShort();
		if (len > 0) {
			additional_data = new byte[len];
			in.readFully(additional_data, 0, additional_data.length);
		}

	}

	public void writeTo(DataOutputStream out) throws IOException {

		out.writeByte(0);

		if (additional_data != null) {
			out.writeBoolean(true); // 1 byte
			out.writeShort(additional_data.length);
			out.write(additional_data, 0, additional_data.length);
		} else {
			out.writeBoolean(false);
		}

	}

	
	public int compareTo(Object o) {
        if(this == o) return 0;
        if(!(o instanceof SpreadAddress))
            throw new ClassCastException("comparison between different classes: the other object is " +
                    (o != null? o.getClass() : o));
        SpreadAddress other = (SpreadAddress) o;
        
        return this.getGroup().compareTo(other.getGroup());
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getGroup() == null) ? 0 : getGroup().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SpreadAddress other = (SpreadAddress) obj;
		if (getGroup() == null) {
			if (other.getGroup() != null)
				return false;
		} else if (!getGroup().equals(other.getGroup()))
			return false;
		return true;
	}
}

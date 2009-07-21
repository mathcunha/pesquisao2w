package org.jboss.cache.marshall;

import org.jgroups.blocks.RpcDispatcher.Marshaller2;
import org.jgroups.blocks.RpcDispatcher.MarshallerAdapter;

public class MarshallerWrapper implements br.unifor.g2cl.Marshaller {
	
	private org.jgroups.blocks.RpcDispatcher.Marshaller2 marshaller;
	
	public org.jgroups.blocks.RpcDispatcher.Marshaller2 getMarshaller() {
		return marshaller;
	}

	public MarshallerWrapper(org.jgroups.blocks.RpcDispatcher.Marshaller m){
		
		if(m == null)
            this.marshaller=null;
        else if(m instanceof Marshaller2)
            this.marshaller=(Marshaller2)m;
        else
            this.marshaller=new MarshallerAdapter(m);
		 
	}
	
	public byte[] getArrayFromObject(Object o) throws Exception {
		return marshaller.objectToByteBuffer(o);
	}

	public Object getObjectFromByte(byte[] arr, int offset, int length)
			throws Exception {
		
		return marshaller.objectFromByteBuffer(arr, offset, length);
	}
}

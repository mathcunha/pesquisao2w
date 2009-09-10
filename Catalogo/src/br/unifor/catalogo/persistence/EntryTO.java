package br.unifor.catalogo.persistence;

import java.util.Map.Entry;

public class EntryTO implements Entry<Object, Object> {
	private Object key; 
	private Object value;
	
	
	public EntryTO(Object key, Object value){
		this.key = key;
		this.value = value;
	}

	@Override
	public Object getKey() {
		return key;
	}

	@Override
	public Object getValue() {
		return value;
	}

	@Override
	public Object setValue(Object arg0) {
		Object aux = value;
		value = arg0;
		return aux;
	}

}

package br.unifor.onaga.occi.xml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.digester.Digester;
import org.xml.sax.SAXException;

import br.unifor.onaga.occi.xml.Disk;

public class Storage {
	public List<Disk> disks;

	public List<Disk> getDisks() {
		return disks;
	}

	public void setDisks(List<Disk> disks) {
		this.disks = disks;
	}

	public void addDisk(Disk disk) {
		if (disks == null) {
			disks = new ArrayList<Disk>();
		}
		disks.add(disk);
	}

	public static void config(Digester digester) {
		digester.addObjectCreate("STORAGE", Storage.class);
		digester.addObjectCreate("STORAGE/DISK", Disk.class);		
		digester.addSetProperties("STORAGE/DISK","href","href");
		digester.addSetNext("STORAGE/DISK", "addDisk");
	}

	public static Storage loadFromFile(File file) throws IOException,
			SAXException {
		Digester digester = new Digester();
		config(digester);
		return (Storage) digester.parse(file);
	}

	public static Storage loadFromInputStream(InputStream input)
			throws IOException, SAXException {
		Digester digester = new Digester();
		config(digester);
		return (Storage) digester.parse(input);
	}
}

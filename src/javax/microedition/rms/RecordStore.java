/*
	This file is part of FreeJ2ME.

	FreeJ2ME is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	FreeJ2ME is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with FreeJ2ME.  If not, see http://www.gnu.org/licenses/
*/
package javax.microedition.rms;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.Vector;
import java.util.Arrays;

import org.recompile.mobile.Mobile;

public class RecordStore
{

	public static final int AUTHMODE_ANY = 1;
	public static final int AUTHMODE_PRIVATE = 0;


	private String name;

	private String appname;

	private static String rmsPath;

	private String rmsFile;

	private File file;

	private int version = 0;

	private int nextid = 0;

	private Vector<byte[]> records;

	private Vector<RecordListener> listeners;

	private long lastModified = 0;

	private static int recordsOpened = 0;

	protected static boolean recordStoreIsOpen = false;

	private RecordStore(String recordStoreName, boolean createIfNecessary) throws RecordStoreException, RecordStoreNotFoundException
	{
		if(recordStoreName == null) { throw new NullPointerException("RecordStore received a null argument"); }

		Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "> RecordStore "+recordStoreName);

		records = new Vector<byte[]>();
		listeners = new Vector<RecordListener>();

		records.add(new byte[]{}); // dummy record (record ids start at 1)

		int count = 0;
		int offset = 0;
		int reclen;

		name = recordStoreName.replaceAll("[/\\\\:*?\"<>|]", "");

		if(name == "") { throw(new RecordStoreException("The record name:'"+ name +"' is not valid")); }

		appname = Mobile.getPlatform().loader.suitename;

		rmsPath = Mobile.getPlatform().dataPath + "./rms/"+appname;
		rmsFile = Mobile.getPlatform().dataPath + "./rms/"+appname+"/"+name;

		try
		{
			Files.createDirectories(Paths.get(rmsPath));
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + e.getMessage());
			throw(new RecordStoreException("Problem Creating Record Store Path "+rmsPath));
		}
		file = new File(rmsFile);
		if(!file.exists())
		{
			if(!createIfNecessary)
			{
				throw (new RecordStoreNotFoundException("Record Store Doesn't Exist: " + rmsFile));
			}

			try // Check Record Store File
			{
				Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "> Creating New Record Store "+appname+"/"+recordStoreName);
				file.createNewFile();
				version = 1;
				nextid = 1;
				count = 0;
				save();
				nextid = 1;
			}
			catch (Exception e)
			{
				Mobile.log(Mobile.LOG_ERROR, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + e.getMessage());
				throw(new RecordStoreException("Problem Opening Record Store (createIfNecessary "+createIfNecessary+"): "+rmsFile));
			}
		}

		try // Read Records
		{
			Path path = Paths.get(file.getAbsolutePath());
			byte[] data = Files.readAllBytes(path);

			if(data.length>=4)
			{
				version = getUInt16(data, offset); offset+=2;
				nextid = getUInt16(data, offset); offset+=2;
				count = getUInt16(data, offset); offset+=2;
				Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "Record count in "+rmsFile + ": " + count);
				for(int i=0; i<count; i++)
				{
					reclen = getUInt16(data, offset);
					offset+=2;

					loadRecord(data, offset, reclen);
					offset+=reclen;
				}

				if(data.length - offset < 8) { lastModified = 0; } // For compatibility, as FreeJ2ME was saving records without the lastModified data for quite some time.
				else { lastModified = getLong(data, offset); }
			}
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "Problem Reading Record Store: "+rmsFile);
			Mobile.log(Mobile.LOG_ERROR, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + e.getMessage());
			throw(new RecordStoreException("Problem Reading Record Store: "+rmsFile));
		}
	}

	private void save()
	{
		byte[] temp = new byte[2];
		try
		{
			FileOutputStream fout = new FileOutputStream(rmsFile);

			// version //
			setUInt16(temp, 0, version);
			fout.write(temp);
			// next record id //
			setUInt16(temp, 0, nextid);
			fout.write(temp);
			// record count //
			setUInt16(temp, 0, records.size()-1);
			fout.write(temp);

			// records //
			for(int i=1; i<records.size(); i++)
			{
				setUInt16(temp, 0, records.get(i).length);
				fout.write(temp);
				fout.write(records.get(i));
			}

			// last modified //
			byte[] lastMod = new byte[8];
			setLong(lastMod, 0, lastModified);
			fout.write(lastMod);

			fout.close();
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "Problem Saving RecordStore");
			e.printStackTrace();
		}
	}

	private void loadRecord(byte[] data, int offset, int numBytes)
	{
		Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "loading Record...");
		byte[] rec = Arrays.copyOfRange(data, offset, offset+numBytes);
		if(rec==null) { rec = new byte[]{}; }
		records.addElement(rec);
	}

	private int getUInt16(byte[] data, int offset)
	{
		int out = 0;

		out |= (((int)data[offset])   & 0xFF) << 8;
		out |= (((int)data[offset+1]) & 0xFF);

		return out | 0x00000000;
	}

	private void setUInt16(byte[] data, int offset, int val)
	{
		data[offset]   = (byte)((val>>8) & 0xFF);
		data[offset+1] = (byte)((val)    & 0xFF);
	}

	private long getLong(byte[] data, int offset)
	{
		long out = 0;
		
		out |= (((long)data[offset])   & 0xFF) << 56;
		out |= (((long)data[offset+1]) & 0xFF) << 48;
		out |= (((long)data[offset+2]) & 0xFF) << 40;
		out |= (((long)data[offset+3]) & 0xFF) << 32;
		out |= (((long)data[offset+4]) & 0xFF) << 24;
		out |= (((long)data[offset+5]) & 0xFF) << 16;
		out |= (((long)data[offset+6]) & 0xFF) << 8;
		out |= (((long)data[offset+7]) & 0xFF);

		return out | 0x00000000;
	}
	
	private void setLong(byte[] data, int offset, long val)
	{
		data[offset]   = (byte)((val>>56) & 0xFF);
		data[offset+1] = (byte)((val>>48) & 0xFF);
		data[offset+2] = (byte)((val>>40) & 0xFF);
		data[offset+3] = (byte)((val>>32) & 0xFF);
		data[offset+4] = (byte)((val>>24) & 0xFF);
		data[offset+5] = (byte)((val>>16) & 0xFF);
		data[offset+6] = (byte)((val>>8)  & 0xFF);
		data[offset+7] = (byte)((val)     & 0xFF);
	}

	public int addRecord(byte[] data, int offset, int numBytes) throws RecordStoreException, RecordStoreFullException
	{
		if(!recordStoreIsOpen) { throw new RecordStoreNotOpenException("Cannot add record, as Record Store is not open"); }
		if (data == null && numBytes > 0) { throw new NullPointerException("Cannot add record, as it is null"); }

		Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "> Add Record "+nextid+ " to "+name);
		try
		{
			
			byte[] rec = new byte[]{};

			// Only try to copy data if there's data to begin with, as some apps may try to store a record with zero-length data
			if(data != null && data.length != 0)
			{
				if(offset < 0 || numBytes < 0 || offset + numBytes > data.length) { throw new ArrayIndexOutOfBoundsException("Tried to access invalid record data position"); }
				rec = Arrays.copyOfRange(data, offset, offset+numBytes);
			}
			else { records.addElement(rec); } // offset and numBytes aren't even taken into account in this case, since the data will have zero-length anyway.

			records.addElement(rec);

			lastModified = System.currentTimeMillis();
			version++;
			
			save();

			for(int i=0; i<listeners.size(); i++) { listeners.get(i).recordAdded(this, nextid); }

			return nextid++;
		}
		catch (Exception e) { throw(new RecordStoreException("Can't Add RMS Record: " + e.getMessage())); }
	}

	public int addRecord(byte[] data, int offset, int numBytes, int tag) throws RecordStoreException, RecordStoreFullException
	{
		Mobile.log(Mobile.LOG_WARNING, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "> Add Record with tag not implemented, adding record without tag instead.");

		return addRecord(data, offset, numBytes);
	}

	public void addRecordListener(RecordListener listener)
	{
		listeners.add(listener);
	}

	public void closeRecordStore() throws RecordStoreNotOpenException
	{ 
		if (!recordStoreIsOpen) { throw new RecordStoreNotOpenException("Record Store is not open at this time"); }

		if (--recordsOpened > 0) { return; }

		if (listeners != null) { listeners.removeAllElements(); }

		records.clear();

		recordStoreIsOpen = false;
	}

	public void deleteRecord(int recordId)
	{
		version++;
		Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "> Delete Record");
		records.remove(recordId);
		save();
		for(int i=0; i<listeners.size(); i++)
		{
			listeners.get(i).recordDeleted(this, recordId);
		}
	}

	public static void deleteRecordStore(String recordStoreName)
	{
		try
		{
			File fstore = new File(Mobile.getPlatform().dataPath + "./rms/"+Mobile.getPlatform().loader.suitename+"/"+recordStoreName);
			fstore.delete();
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "Problem deleting RecordStore "+recordStoreName);
			e.printStackTrace();
		}
		System.gc();
	}

	public RecordEnumeration enumerateRecords(RecordFilter filter, RecordComparator comparator, boolean keepUpdated)
	{
		Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "RecordStore.enumerateRecords");
		return new enumeration(filter, comparator, keepUpdated);
	}

	public RecordEnumeration enumerateRecords(RecordFilter filter, RecordComparator comparator, boolean keepUpdated, int[] tags)
	{
		Mobile.log(Mobile.LOG_WARNING, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "RecordStore.enumerateRecords with tags not implemented. Enumerating without tags...");
		return new enumeration(filter, comparator, keepUpdated);
	}

	public long getLastModified() { return lastModified; }

	public String getName() { return name; }

	public int getNextRecordID()
	{
		Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "> getNextRecordID");
		return nextid;
	}

	// As noted in the RecordStore Constructor, Record IDs start from 1, so the very first position (0) of the record vector is just padding, hence why this returns size-1;
	public int getNumRecords()
	{
		Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "> getNumRecords:" + (records.size()-1));
		return records.size()-1;
	}

	public byte[] getRecord(int recordId) throws InvalidRecordIDException, RecordStoreException
	{
		if (!recordStoreIsOpen) { throw new RecordStoreNotOpenException("Cannot get the record of a closed Record Store"); }
		if(recordId > records.size()-1) { throw new InvalidRecordIDException("setRecord: Invalid Record ID: "+recordId); }
		
		Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "> getRecord("+recordId+")");

		try
		{
			byte[] t = records.get(recordId);
			return t.length < 1 ? null : t.clone();
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "(getRecord) Record Store Exception: "+recordId);
			throw new RecordStoreException();
		}
	}

	public int getRecord(int recordId, byte[] buffer, int offset) throws InvalidRecordIDException, RecordStoreException
	{
		if (!recordStoreIsOpen) { throw new RecordStoreNotOpenException("Cannot get the record of a closed Record Store"); }
		if(recordId > records.size()-1) { throw new InvalidRecordIDException("setRecord: Invalid Record ID: "+recordId); }

		Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "> getRecord(id, buffer, offset)");
		byte[] temp = getRecord(recordId);

		if(temp == null) { return 0; }

		int len = temp.length;

		while (offset+len > buffer.length) { len--; }

		for(int i=0; i<len; i++) { buffer[offset+i] = temp[i]; }

		return len;
	}

	public int getRecordSize(int recordId) throws InvalidRecordIDException, RecordStoreException
	{
		if (!recordStoreIsOpen) { throw new RecordStoreNotOpenException("Cannot get the record's size on a closed Record Store"); }
		if(recordId > records.size()-1) { throw new InvalidRecordIDException("setRecord: Invalid Record ID: "+recordId); }

		Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "> Get Record Size");
		
		return records.get(recordId).length;
	}

	public int getSize() throws RecordStoreNotOpenException
	{ 
		if (!recordStoreIsOpen) { throw new RecordStoreNotOpenException("Cannot get the size of a closed Record Store"); }

		int size = 0;
		for(int i = 1; i < records.size(); i++) {size += records.get(i).length; }

		return size;
	}

	// 16MiB minus whatever size the RecordStore is currently occupying. Whould be more than enough for everything given how limited those devices were.
	public int getSizeAvailable() throws RecordStoreNotOpenException
	{
		if (!recordStoreIsOpen) { throw new RecordStoreNotOpenException("Cannot get the size of a closed Record Store"); }

		int size = 0;
		for(int i = 1; i < records.size(); i++) {size += records.get(i).length; }

		return 16777216 - size; 
	}

	public int getVersion() { return version; }

	public static String[] listRecordStores()
	{
		Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "List Record Stores");
		if(rmsPath==null)
		{
			rmsPath = Mobile.getPlatform().dataPath + "./rms/"+Mobile.getPlatform().loader.name;
			try
			{
				Files.createDirectories(Paths.get(rmsPath));
			}
			catch (Exception e) { }
		}
		try
		{
			File folder = new File(rmsPath);
			File[] files = folder.listFiles();

			String[] out = new String[files.length];

			for(int i=0; i<files.length; i++)
			{
				Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + (files[i].toString()).substring(rmsPath.length()+1));
				out[i] = (files[i].toString()).substring(rmsPath.length()+1);
			}

			return out;
		}
		catch (Exception e) { }
		return null;
	}

	public static RecordStore openRecordStore(String recordStoreName, boolean createIfNecessary) throws RecordStoreException, RecordStoreNotFoundException
	{
		if(!recordStoreIsOpen) { recordStoreIsOpen = true; }
		Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "Open Record Store A "+ createIfNecessary + ": " + recordStoreName.replaceAll("[/\\\\:*?\"<>|]", ""));
		recordsOpened++;
		return new RecordStore(recordStoreName.replaceAll("[/\\\\:*?\"<>|]", ""), createIfNecessary);
	}

	public static RecordStore openRecordStore(String recordStoreName, boolean createIfNecessary, int authmode, boolean writable) throws RecordStoreException, RecordStoreNotFoundException
	{
		if(!recordStoreIsOpen) { recordStoreIsOpen = true; }
		Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "Open Record Store B "+ createIfNecessary + ": " + recordStoreName.replaceAll("[/\\\\:*?\"<>|]", ""));
		recordsOpened++;
		return new RecordStore(recordStoreName.replaceAll("[/\\\\:*?\"<>|]", ""), createIfNecessary);
	}

	public static RecordStore openRecordStore(String recordStoreName, boolean createIfNecessary, int authmode, boolean writable, String password) throws RecordStoreException, RecordStoreNotFoundException//, SecureRecordStoreException
	{
		//if(!recordStoreIsOpen) { recordStoreIsOpen = true; }
		Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "Open Record Store with authmode and password not implemented.");
		return null;
	}

	public static RecordStore openRecordStore(String recordStoreName, String vendorName, String suiteName) throws RecordStoreException, RecordStoreNotFoundException
	{
		if(!recordStoreIsOpen) { recordStoreIsOpen = true; }
		Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "Open Record Store C:" + recordStoreName.replaceAll("[/\\\\:*?\"<>|]", ""));
		recordsOpened++;
		return new RecordStore(recordStoreName.replaceAll("[/\\\\:*?\"<>|]", ""), false);
	}

	public static RecordStore openRecordStore(String recordStoreName, String vendorName, String suiteName, String password) throws RecordStoreException, RecordStoreNotFoundException//, SecureRecordStoreException
	{
		//if(!recordStoreIsOpen) { recordStoreIsOpen = true; }
		Mobile.log(Mobile.LOG_WARNING, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "Open Record Store with password not implemented:.");
		return null;
	}

	public void removeRecordListener(RecordListener listener)
	{
		listeners.remove(listener);
	}

	public void setMode(int authmode, boolean writable) {  }

	public void setRecord(int recordId, byte[] newData, int offset, int numBytes) throws RecordStoreException, InvalidRecordIDException
	{
		if (!recordStoreIsOpen) { throw new RecordStoreNotOpenException("Cannot set record on a closed Record Store"); }

		Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "> Set Record "+recordId+" in "+name);
		if(recordId > records.size()-1) { throw new InvalidRecordIDException("setRecord: Invalid Record ID: "+recordId); }
		
		if (recordId == records.size()-1) 
		{
			Mobile.log(Mobile.LOG_INFO, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "> "+recordId+" in "+name + " has no data yet, creating it now as a workaround.");
			addRecord(newData, offset, numBytes);
			return;
		}
		
		try
		{
			byte[] rec = new byte[]{};
			// As for addRecord, only try to copy data if there's data to begin with
			if(newData != null && newData.length != 0)
			{
				if(offset < 0 || numBytes < 0 || offset + numBytes > newData.length) { throw new ArrayIndexOutOfBoundsException("Tried to access invalid record data position"); }
				
				rec = Arrays.copyOfRange(newData, offset, offset+numBytes);
			}

			records.set(recordId, rec);
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "Problem in Set Record");
			e.printStackTrace();
		}
		lastModified = System.currentTimeMillis();
		save();
		for(int i=0; i<listeners.size(); i++)
		{
			listeners.get(i).recordChanged(this, recordId);
		}
	}

	public void setRecord(int recordId, byte[] newData, int offset, int numBytes, int tag) throws RecordStoreException, InvalidRecordIDException
	{
		Mobile.log(Mobile.LOG_INFO, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "> Set Record with tag not implemented yet, setting record without tag instead");
		setRecord(recordId, newData, offset, numBytes);
	}


	/* ************************************************************
				RecordEnumeration implementation
	    *********************************************************** */

	private class enumeration implements RecordEnumeration
	{
		private int index;
		private int[] elements;
		private int count;
		private boolean keepupdated;
		RecordFilter filter;
		RecordComparator comparator;

		public enumeration(RecordFilter filter, RecordComparator comparator, boolean keepUpdated)
		{
			keepupdated = keepUpdated;
			index = 0;
			count = 0;

			this.filter = filter;
			this.comparator = comparator;

			this.filter = filter;

			build();
		}

		private void build()
		{
			elements = new int[records.size()+1];
			for(int i=0; i<records.size()+1; i++) { elements[i] = 1; }
			count = 0;
			if(filter==null)
			{
				Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "Not Filtered");
				for(int i=1; i<records.size(); i++)
				{
					if(records.get(i).length>0) // not deleted
					{
						elements[count] = i;
						count++;
					}
				}
			}
			else
			{
				Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "Filtered");
				for(int i=1; i<records.size(); i++)
				{
					if(filter.matches(records.get(i)))
					{
						if(records.get(i).length>0) // not deleted
						{
							elements[count] = i;
							count++;
						}
					}
				}
			}

			int result = 0;
			int temp;
			if(comparator!=null)
			{
				Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "Comparator");
				for(int i=0; i<count-1; i++)
				{
					for(int j=0; j<count-(1+i); j++)
					{
						result = comparator.compare(records.get(elements[j]), records.get(elements[j+1]));
						if(result==RecordComparator.FOLLOWS)
						{
							temp = elements[j];
							elements[j] = elements[j+1];
							elements[j+1] = temp;
						}

					}
				}
			}
		}

		public void destroy() { }

		public int getRecordId(int index) throws IllegalArgumentException, RecordStoreNotOpenException
		{
			if (!recordStoreIsOpen) { throw new RecordStoreNotOpenException("Cannot get Record ID of a closed Record Store"); }

			if(index < 0 || index >= count) {throw new IllegalArgumentException("Cannot get Record ID, as the received index is out of bounds"); }

			return elements[index];
		}

		public boolean hasNextElement()
		{
			if(keepupdated) { rebuild(); }
			if (index<count) { return true; }
			return false;
		}

		public boolean hasPreviousElement()
		{
			if(keepupdated) { rebuild(); }
			if(index>0) { return true; }
			return false;
		}

		public boolean isKeptUpdated() { return keepupdated; }

		public void keepUpdated(boolean keepUpdated) { keepupdated = keepUpdated; }

		public byte[] nextRecord() throws InvalidRecordIDException, RecordStoreNotOpenException
		{
			if (!recordStoreIsOpen) { throw new RecordStoreNotOpenException("Cannot get the next record of a closed Record Store"); }
			if(index>=count) { throw(new InvalidRecordIDException("Next Record ID is out of bounds")); }

			Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "> Next Record");
			if(keepupdated) { rebuild(); }
			index++;
			return records.get(elements[index-1]).clone();
		}

		public int nextRecordId() throws InvalidRecordIDException, RecordStoreNotOpenException	
		{
			if (!recordStoreIsOpen) { throw new RecordStoreNotOpenException("Cannot get the next record ID of a closed Record Store"); }
			if(index>=count) { throw(new InvalidRecordIDException("Next Record ID is out of bounds")); }

			Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "> Next Record ID (idx:"+index+" cnt:"+count+")");
			if(keepupdated) { rebuild(); }
			return elements[index++];
		}

		public int numRecords()
		{
			Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "> numRecords()");
			if(keepupdated) { rebuild(); }
			return count;
		}

		public byte[] previousRecord() throws InvalidRecordIDException, RecordStoreNotOpenException
		{
			if (!recordStoreIsOpen) { throw new RecordStoreNotOpenException("Cannot get the previous record of a closed Record Store"); }
			if(index-1 < 0) { throw new InvalidRecordIDException("Previous Record is out of bounds"); }
			Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "> Previous Record");
			if(keepupdated) { rebuild(); }
			return records.get(elements[--index]).clone();
		}

		public int previousRecordId() throws InvalidRecordIDException, RecordStoreNotOpenException
		{
			if (!recordStoreIsOpen) { throw new RecordStoreNotOpenException("Cannot get the previous record ID of a closed Record Store"); }
			if(index < 0) { throw new InvalidRecordIDException("Previous Record is out of bounds"); }
			Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "> Previous Record ID");
			if(keepupdated) { rebuild(); }
			index--;
			if(index<0) { throw(new InvalidRecordIDException("Cannot return previous Record ID, as it goes out of bounds")); }
			return elements[index];
		}

		public void rebuild()
		{
			build();
			if(index > count) { index = count; }
			if(index < 0) { index = 0; }
		}

		public void reset()
		{
			if(keepupdated) { rebuild(); }
			index = 0;
		}
	}
}

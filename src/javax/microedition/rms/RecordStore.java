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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Vector;

import org.recompile.mobile.Base64Util;
import org.recompile.mobile.Mobile;

public class RecordStore
{

	public static final int AUTHMODE_APPLEVEL = 2; // This probably won't ever be used, but since RecordStore now has quite a few bits from MIDP 3.0, having this defined wouldn't hurt
	public static final int AUTHMODE_ANY = 1;
	public static final int AUTHMODE_PRIVATE = 0;

	protected RecordStore thisStore;

	private final String RMS_VERSION = "1.0.0";

	private String name;

	private String basename, suitename, vendorname, password;
	private boolean writable, writablebyothers;
	private int authmode;

	private static String rmsPath;

	private String rmsFile;

	private File file;

	private int version = 0;

	private int nextid = 0;

	private Vector<byte[]> records; // Records contains the actual record data (on DoJa, this saves the scratchPad data for the given index)
	private Vector<Integer> recordIds; // recordIds contains the id of the record data in the respective "records" position (recordIds[4] = recordId value of records[4] which is != its actual position in the vector)
	private Vector<Integer> recordTags; // recordTags contains the tags tied to each position of recordIDs

	private int scratchPadIndex = 0; // DoJa-only, used to differentiate between multiple scratchpads when writing

	private Vector<RecordListener> listeners;
	private static Vector<String> openedStores = new Vector<String>();

	private long lastModified = 0;

	private static int recordsOpened = 0;

	protected static boolean recordStoreIsOpen = false;

	private RecordStore(String recordStoreName, boolean createIfNecessary, String vendorname, String suitename, int authmode, boolean writable, String password) throws RecordStoreException, RecordStoreNotFoundException, SecurityException
	{
		if(recordStoreName == null) { throw new NullPointerException("RecordStore received a null argument"); }

		basename = generateBaseName(vendorname, recordStoreName);

		Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "> RecordStore "+basename);

		records = new Vector<byte[]>();
		recordIds = new Vector<Integer>();
		recordTags = new Vector<Integer>();
		listeners = new Vector<RecordListener>();

		records.add(new byte[]{}); // dummy record (record ids start at 1)
		recordIds.add(0);
		recordTags.add(0);

		name = recordStoreName;

		if(name == "") { throw(new RecordStoreException("The record name:'"+ name +"' is not valid")); }

		this.password = password; // We don't really encrypt anything, so password is worthless at the moment
		this.writable = writable;
		this.writablebyothers = (writable && authmode == AUTHMODE_ANY); // Sets if this record can be written to by other suites
		this.authmode = authmode;
		this.vendorname = vendorname;
		this.suitename = suitename;

		try
		{
			// For ISO-8859-1 encodings, we'll use UTF-8 for save paths, helps with chinese and special characters
			rmsPath = new String((Mobile.getPlatform().dataPath + "./rms/"+suitename).getBytes(System.getProperty("file.encoding")), System.getProperty("file.encoding").equals(Mobile.supportedEncodings[Mobile.ISO_8859_1]) ? "UTF-8" : Mobile.textEncoding);
			rmsFile = rmsPath+"/"+basename+".rms";
		}
		catch (UnsupportedEncodingException e) { } // Shouldn't really happen

		// Check if the record directory exists, if not, create it.
		try
		{
			File rmsDir = new File(rmsPath);
			if (!rmsDir.exists()) { rmsDir.mkdirs(); }
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + e.getMessage());
			throw(new RecordStoreException("Problem Creating Record Store Path "+rmsPath));
		}

		// Load actual record data
		file = new File(rmsFile);

		if(!file.exists())
		{
			Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": New recordStore file format not found, checking for legacy one...");
			file = new File(rmsPath+"/"+name);
			if(!file.exists())
			{
				Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": Legacy recordStore file not found either, will create if necessary...");
				loadRecordStore(createIfNecessary);
			}
			else
			{
				Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": Legacy recordStore file found! Converting to new format...");
				loadLegacyRecordStore(rmsPath+"/"+name, createIfNecessary);
			}

		}
		else { loadRecordStore(createIfNecessary); }

		// If no exceptions were thrown, the record was loaded, set the recordStoreIsOpen flag and increase the counter of opened stores
		if(!recordStoreIsOpen)
		{
			recordStoreIsOpen = true;
			openedStores.add(this.name);
		}
		recordsOpened++;

		thisStore = this;
	}

	// We don't add anything to recordIds here, as all this does is load records when a recordStore is opened (recordIds are loaded right after lastModified)
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

		return out;
	}

	private int getUint32(byte[] data, int offset)
	{
		int out = 0;

		out |= (((int)data[offset])   & 0xFF) << 24;
		out |= (((int)data[offset+1]) & 0xFF) << 16;
		out |= (((int)data[offset+2]) & 0xFF) << 8;
		out |= (((int)data[offset+3]) & 0xFF);

		return out;
	}

	private void setUInt16(byte[] data, int offset, int val)
	{
		data[offset]   = (byte)((val>>8) & 0xFF);
		data[offset+1] = (byte)((val)    & 0xFF);
	}

	private byte[] setUInt32(int offset, int val)
	{
		byte[] data = new byte[4];

		data[offset]   = (byte)((val>>24)  & 0xFF);
		data[offset+1] = (byte)((val>>16)  & 0xFF);
		data[offset+2] = (byte)((val>>8)   & 0xFF);
		data[offset+3] = (byte)((val)      & 0xFF);

		return data;
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

		return out;
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

	public int addRecord(byte[] data, int offset, int numBytes) throws RecordStoreException, RecordStoreFullException, SecurityException
	{
		return addRecord(data, offset, numBytes, 0);
	}

	public int addRecord(byte[] data, int offset, int numBytes, int tag) throws RecordStoreException, RecordStoreFullException, SecurityException
	{
		Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "> Add Record "+nextid+ " to "+name + " with tag " + tag + ", length " + numBytes + " and data " + (data != null? Arrays.toString(data) : "null"));

		if(!recordStoreIsOpen) { throw new RecordStoreNotOpenException("Cannot add record, as Record Store is not open"); }
		if(!Mobile.getPlatform().loader.suitename.equals(this.suitename) && !writablebyothers) { throw new SecurityException("This suite does not have write access to this RecordStore"); }
		if (data == null && numBytes > 0) { throw new NullPointerException("Cannot add record, as it is null"); }

		try
		{

			byte[] rec = new byte[]{};

			// Only try to copy data if there's data to begin with, as some apps may try to store a record with zero-length data
			if(data != null && data.length != 0)
			{
				if(offset < 0 || numBytes < 0 || offset + numBytes > data.length) { throw new ArrayIndexOutOfBoundsException("Tried to access invalid record data position"); }
				rec = Arrays.copyOfRange(data, offset, offset+numBytes);
			}

			records.addElement(rec);
			recordIds.addElement(nextid);
			recordTags.addElement(tag); // Tag will be tied to the current ID in recordIDs

			for(int i=0; i<listeners.size(); i++) { listeners.get(i).recordAdded(this, nextid); }

			lastModified = System.currentTimeMillis();
			version++;
			nextid++;

			saveRecordStore();

			return nextid-1; // Return the new record's id, not the next one's.
		}
		catch (Exception e) { throw(new RecordStoreException("Can't Add RMS Record: " + e.getMessage())); }
	}

	public void closeRecordStore() throws RecordStoreNotOpenException
	{
		if (!recordStoreIsOpen) { throw new RecordStoreNotOpenException("Record Store is not open at this time"); }

		Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "> Close Record");
		if (--recordsOpened > 0) { return; }

		Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "> No more stores opened for " + name + ", cleaning up.");

		if (listeners != null) { listeners.removeAllElements(); }

		records.clear();
		recordTags.clear();
		recordIds.clear();

		recordStoreIsOpen = false;
		openedStores.remove(this.name);
	}

	public void deleteRecord(int recordId) throws RecordStoreException, SecurityException
	{
		if(!recordStoreIsOpen) { throw new RecordStoreNotOpenException("Cannot add record, as Record Store is not open"); }
		if(!Mobile.getPlatform().loader.suitename.equals(this.suitename) && !writablebyothers) { throw new SecurityException("This suite does not have write access to this RecordStore"); }
		version++;
		Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "> Delete Record " + recordId);
		records.remove(recordIds.indexOf(recordId));
		recordTags.remove(recordIds.indexOf(recordId));
		recordIds.remove(recordIds.indexOf(recordId));
		saveRecordStore();
		for(int i=0; i<listeners.size(); i++)
		{
			listeners.get(i).recordDeleted(this, recordId);
		}
	}

	// This should only delete records that are tied to the current MIDlet suite
	public static void deleteRecordStore(String recordStoreName) throws RecordStoreException, RecordStoreNotFoundException
	{
		if(openedStores.contains(recordStoreName)) { throw new RecordStoreException("Cannot delete an open record store"); }

		try
		{
			Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "Deleting RecordStore "+recordStoreName);

			// Just like for saving, we use UTF-8 ONLY IF Mobile encoding is ISO_8859_1, handling Shift_JIS and EUC_KR as normal.
			String encoding = System.getProperty("file.encoding").equals(Mobile.supportedEncodings[Mobile.ISO_8859_1]) ? "UTF-8" : Mobile.textEncoding;

			String folderPath = Mobile.getPlatform().dataPath + "./rms/" + Mobile.getPlatform().loader.suitename;
			folderPath = new String(folderPath.getBytes(System.getProperty("file.encoding")), encoding);

			File folder = new File(folderPath);

			// If the directory doesn't exist, don't even bother looking.
			if (!folder.exists() || !folder.isDirectory()) { throw new RecordStoreNotFoundException("RecordStore directory does not exist: " + recordStoreName); }

			String[] fileNames = folder.list();

			if (fileNames == null || fileNames.length == 0) { throw new RecordStoreNotFoundException("RecordStore not found: " + recordStoreName); }

			String base64prefix = generateBaseName(Mobile.getPlatform().loader.vendorname, recordStoreName);
			String prefix = new String(base64prefix.getBytes(System.getProperty("file.encoding")), encoding);
			boolean exists = false;

			for (int i = 0; i < fileNames.length; i++)
			{
				File currentFile = new File(folder, fileNames[i]);

				if (currentFile.isFile() && fileNames[i].startsWith(prefix))
				{
					exists = true;
					if (currentFile.delete())
					{
						Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." +
							RecordStore.class.getSimpleName() + ": Deleted " + fileNames[i]);
					}
					else
					{
						Mobile.log(Mobile.LOG_ERROR, RecordStore.class.getPackage().getName() + "." +
							RecordStore.class.getSimpleName() + ": Failed to delete " + fileNames[i]);
					}
				}
			}

			// Doesn't exist, throw the exception (gets caught below and re-thrown).
			if (!exists) { throw new RecordStoreNotFoundException("RecordStore not found: " + recordStoreName); }
		}
		catch (RecordStoreNotFoundException e) { throw e; } // Throw it to the app
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": Problem deleting RecordStore " + recordStoreName);
			e.printStackTrace();
			throw new RecordStoreNotFoundException("Could not delete the requested RecordStore");
		}
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

	public byte[] getRecord(int recordId) throws InvalidRecordIDException, RecordStoreNotOpenException, RecordStoreException
	{
		Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "> getRecord("+recordId+")");
		if (!recordStoreIsOpen) { throw new RecordStoreNotOpenException("Cannot get the record of a closed Record Store"); }

		if(recordId <= 0 || !recordIds.contains(recordId)) { throw new InvalidRecordIDException("getRecord: Invalid Record ID: "+recordId); }

		byte[] t = records.get(recordIds.indexOf(recordId));

		return (t == null || t.length == 0) ? null : t.clone();
	}

	public int getRecord(int recordId, byte[] buffer, int offset) throws InvalidRecordIDException, RecordStoreNotOpenException, RecordStoreException
	{
		Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "> getRecord(" + recordId + ", " + buffer + ", " + offset + ")");
		if (!recordStoreIsOpen) { throw new RecordStoreNotOpenException("Cannot get the record of a closed Record Store"); }
		if(recordId <= 0 || !recordIds.contains(recordId)) { throw new InvalidRecordIDException("getRecord: Invalid Record ID: "+recordId); }
		if(getRecord(recordIds.indexOf(recordId)).length > buffer.length-offset) { throw new ArrayIndexOutOfBoundsException("Record data won't fit on the provided buffer"); }

		byte[] temp = getRecord(recordIds.indexOf(recordId));

		System.arraycopy(temp, 0, buffer, offset, temp.length);

		return temp.length;
	}

	public int getTag(int recordId) throws InvalidRecordIDException, RecordStoreNotOpenException
	{
		Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "> getTag("+recordId+")");
		if (!recordStoreIsOpen) { throw new RecordStoreNotOpenException("Cannot get the record of a closed Record Store"); }
		if(!recordIds.contains(recordId)) { throw new InvalidRecordIDException("getRecord: Invalid Record ID: "+recordId); }

		return recordTags.get(recordIds.indexOf(recordId));
	}

	public int getRecordSize(int recordId) throws InvalidRecordIDException, RecordStoreNotOpenException, RecordStoreException
	{
		Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "> Get Record Size");
		if (!recordStoreIsOpen) { throw new RecordStoreNotOpenException("Cannot get the record's size on a closed Record Store"); }
		if(!recordIds.contains(recordId)) { throw new InvalidRecordIDException("getRecord: Invalid Record ID: "+recordId); }

		return records.get(recordIds.indexOf(recordId)).length;
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
			try
			{
				rmsPath = new String((Mobile.getPlatform().dataPath + "./rms/"+Mobile.getPlatform().loader.suitename).getBytes(System.getProperty("file.encoding")), System.getProperty("file.encoding").equals(Mobile.supportedEncodings[Mobile.ISO_8859_1]) ? "UTF-8" : Mobile.textEncoding);
				File rmsDir = new File(rmsPath);
				if (!rmsDir.exists()) { rmsDir.mkdirs(); }
			}
			catch (Exception e) { }
		}

		try
		{
			File folder = new File(rmsPath);
			File[] files = folder.listFiles();

			// Filter for .rms files only, otherwise this will return a longer array than expected since binary data is saved separately with the same name
			if (files != null)
			{
				List<String> outList = new ArrayList<String>();
				for (File file : files)
				{
					if (file.isFile() && file.getName().endsWith(".rms"))
					{
						Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ":   > '" + returnRecordStoreName(rmsPath+"/"+file.toString().substring(rmsPath.length() + 1)) +"'");
						outList.add(returnRecordStoreName(rmsPath+"/"+file.toString().substring(rmsPath.length() + 1)));
					}
				}

				return outList.toArray(new String[0]);
			}
		}
		catch (Exception e) { e.printStackTrace(); }

		return null;
	}

	public static RecordStore openRecordStore(String recordStoreName, boolean createIfNecessary) throws RecordStoreException, RecordStoreNotFoundException, SecurityException
	{
		Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "Open Record Store A "+ createIfNecessary + ": " + recordStoreName);
		return new RecordStore(recordStoreName, createIfNecessary, Mobile.getPlatform().loader.vendorname, Mobile.getPlatform().loader.suitename, AUTHMODE_PRIVATE, true, "");
	}

	public static RecordStore openRecordStore(String recordStoreName, boolean createIfNecessary, int authmode, boolean writable) throws RecordStoreException, RecordStoreNotFoundException, SecurityException
	{
		Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "Open Record Store B "+ createIfNecessary + ": " + recordStoreName);
		return new RecordStore(recordStoreName, createIfNecessary, Mobile.getPlatform().loader.vendorname, Mobile.getPlatform().loader.suitename, authmode, writable, "");
	}

	public static RecordStore openRecordStore(String recordStoreName, boolean createIfNecessary, int authmode, boolean writable, String password) throws RecordStoreException, RecordStoreNotFoundException, SecurityException
	{
		Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "Open Record Store C + pass,auth "+ createIfNecessary + ": " + recordStoreName);
		return new RecordStore(recordStoreName, createIfNecessary, Mobile.getPlatform().loader.vendorname, Mobile.getPlatform().loader.suitename, authmode, writable, password);
	}

	/*
	 * These can open a record store from another vendor and suite, so default their access modes to PRIVATE and writable to false as these tokens will change in the constructor,
	 * based on the writable and authentication flags that the file was last saved with.
	 */
	public static RecordStore openRecordStore(String recordStoreName, String vendorName, String suiteName) throws RecordStoreException, RecordStoreNotFoundException, SecurityException
	{
		Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "Open Record Store D:" + recordStoreName);
		return new RecordStore(recordStoreName, false, vendorName, suiteName, AUTHMODE_PRIVATE, false, "");
	}

	public static RecordStore openRecordStore(String recordStoreName, String vendorName, String suiteName, String password) throws RecordStoreException, RecordStoreNotFoundException, SecurityException
	{
		Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "Open Record Store E + pass:" + recordStoreName);
		return new RecordStore(recordStoreName, false, vendorName, suiteName, AUTHMODE_PRIVATE, false, password);
	}

	public void addRecordListener(RecordListener listener) { listeners.add(listener); }

	public void removeRecordListener(RecordListener listener) { listeners.remove(listener); }

	public void setMode(int authmode, boolean writable) throws SecurityException
	{
		if(authmode != AUTHMODE_ANY && authmode != AUTHMODE_PRIVATE) { throw new IllegalArgumentException("Invalid authentication mode"); }
		if(!Mobile.getPlatform().loader.suitename.equals(this.suitename)) { throw new SecurityException("Cannot change another suite's recordStore mode"); }
		this.authmode = authmode;
		this.writable = writable;
	}

	public void setRecord(int recordId, byte[] newData, int offset, int numBytes) throws RecordStoreException, InvalidRecordIDException, SecurityException
	{
		setRecord(recordId, newData, offset, numBytes, 0);
	}

	public void setRecord(int recordId, byte[] newData, int offset, int numBytes, int tag) throws RecordStoreException, InvalidRecordIDException, SecurityException
	{
		Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "> Set Record "+recordId+" in "+name + " from " + offset + " to " + (offset+numBytes) +  " with tag " + tag);
		if (!recordStoreIsOpen) { throw new RecordStoreNotOpenException("Cannot set record on a closed Record Store"); }
		if(!Mobile.getPlatform().loader.suitename.equals(this.suitename) && !writablebyothers) { throw new SecurityException("This suite does not have write access to this RecordStore"); }

		if(recordId == 0) { recordId++; } // Records should always start at ID 1
		if(!recordIds.contains(recordId)) { throw new InvalidRecordIDException("setRecord: Invalid Record ID: "+recordId); }
		if(offset < 0 || numBytes < 0 || offset + numBytes > newData.length) { throw new ArrayIndexOutOfBoundsException("Tried to access invalid record data position"); }

		try
		{
			byte[] temp = new byte[numBytes];

			System.arraycopy(newData, offset, temp, 0, numBytes);

			records.set(recordIds.indexOf(recordId), temp);
			recordTags.set(recordIds.indexOf(recordId), tag);
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "Problem in Set Record");
			e.printStackTrace();
		}
		lastModified = System.currentTimeMillis();
		version++;
		saveRecordStore();
		for(int i=0; i<listeners.size(); i++) { listeners.get(i).recordChanged(this, recordId); }
	}


	/* ************************************************************
				RecordEnumeration implementation
		*********************************************************** */

	// TODO: Implement tag handling for enumeration, although it might not be needed for MIDP up to 3.0
	private class enumeration implements RecordEnumeration
	{
		private int index;
		private int[] elements, tagsToMatch;
		private int count;
		private boolean keepUpdated;
		RecordFilter filter;
		RecordComparator comparator;

		private final RecordListener recordListener = new RecordListener()
		{
			public void recordAdded(RecordStore recordStore, int recordId) { rebuild(); }

			public void recordChanged(RecordStore recordStore, int recordId) { rebuild(); }

			public void recordDeleted(RecordStore recordStore, int recordId) { rebuild(); }

		};

		public enumeration(RecordFilter filter, RecordComparator comparator, boolean keepUpdated)
		{
			this(filter, comparator, keepUpdated, null);
		}

		public enumeration(RecordFilter filter, RecordComparator comparator, boolean keepUpdated, int[] tags)
		{
			this.keepUpdated = keepUpdated;

			this.filter = filter;
			this.comparator = comparator;
			this.filter = filter;
			this.tagsToMatch = tags;

			rebuild();

			if (keepUpdated)
			{
				thisStore.addRecordListener(recordListener);
			}
		}

		public void destroy()
		{
			Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "> Enum Destroy called (not implemented)");
		}

		public int getRecordId(int index) throws IllegalArgumentException, RecordStoreNotOpenException
		{
			if (!recordStoreIsOpen) { throw new RecordStoreNotOpenException("Cannot get Record ID of a closed Record Store"); }

			if(index < 0 || index >= count) {throw new IllegalArgumentException("Cannot get Record ID, as the received index is out of bounds"); }

			return elements[index];
		}

		public boolean hasNextElement() { return count > 0 && index < count; }

		public boolean hasPreviousElement() { return index != 0 && count > 0; }

		public boolean isKeptUpdated() { return keepUpdated; }

		public void keepUpdated(boolean keepUpdated)
		{
			if (keepUpdated)
			{
				if (!this.keepUpdated)
				{
					rebuild();
					thisStore.addRecordListener(recordListener);
				}
			}
			else { thisStore.removeRecordListener(recordListener); }

			this.keepUpdated = keepUpdated;
		}

		public byte[] nextRecord() throws InvalidRecordIDException, RecordStoreNotOpenException
		{
			if (!recordStoreIsOpen) { throw new RecordStoreNotOpenException("Cannot get the next record of a closed Record Store"); }
			if(index < 0) { index = 0; }
			if(index >= count) { throw(new InvalidRecordIDException("Next Record ID is out of bounds")); }
			Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "> Enum Next Record " + index);
			return records.get(elements[index++]).clone();
		}

		public int nextRecordId() throws InvalidRecordIDException, RecordStoreNotOpenException
		{
			if (!recordStoreIsOpen) { throw new RecordStoreNotOpenException("Cannot get the next record ID of a closed Record Store"); }
			if(index < 0) { index = 0; }
			if(index >= count) { throw(new InvalidRecordIDException("Next Record ID is out of bounds")); }
			Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "> Enum Next Record ID " + elements[index]);
			return elements[index++];
		}

		public int numRecords()
		{
			Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "> Enum numRecords()");
			return count;
		}

		public byte[] previousRecord() throws InvalidRecordIDException, RecordStoreNotOpenException
		{
			if (!recordStoreIsOpen) { throw new RecordStoreNotOpenException("Cannot get the previous record of a closed Record Store"); }
			if(index == 0 || count == 0) { throw new InvalidRecordIDException("Previous Record is out of bounds"); }

			if(index < 0) { index = records.size(); }

			Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "> Enum Previous Record " + (index-1));

			return records.get(elements[--index]).clone();
		}

		public int previousRecordId() throws InvalidRecordIDException, RecordStoreNotOpenException
		{
			if (!recordStoreIsOpen) { throw new RecordStoreNotOpenException("Cannot get the previous record ID of a closed Record Store"); }
			if(index == 0 || count == 0) { throw new InvalidRecordIDException("Previous Record is out of bounds"); }

			if(index < 0) { index = records.size(); }

			Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "> Enum Previous Record ID " + elements[index-1]);

			return elements[--index];
		}

		public void rebuild()
		{
			reset();
			elements = new int[records.size()];
			count = 0;

			Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": Enumerator > " + (filter == null ? "Not Filtered" : "Filtered") + " Size:" + records.size());

			for (int i = 1; i < records.size(); i++)
			{
				boolean matchesFilter = filter == null || filter.matches(records.get(i));
				// If the tags array is null, return all records, if it exists but has length zero, basically return an empty enumeration (as there are no tags to match), else, match against available tags
				boolean matchesTag = tagsToMatch == null || matchesTag(recordTags.get(i), tagsToMatch);

				if (records.get(i).length > 0 && matchesFilter && matchesTag)
				{
					elements[count++] = recordIds.get(i);
				}
			}

			if(comparator!=null)
			{
				Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "Comparator");
				for (int i = 0; i < count - 1; i++)
				{
					for (int j = 0; j < count - 1 - i; j++)
					{
						if (comparator.compare(records.get(elements[j]), records.get(elements[j + 1])) == RecordComparator.FOLLOWS)
						{
							int temp = elements[j];
							elements[j] = elements[j + 1];
							elements[j + 1] = temp;
						}
					}
				}
			}
		}

		private boolean matchesTag(int recordTag, int[] tags)
		{
			for (int tag : tags)
			{
				if (recordTag == tag) { return true; }
			}
			return false;
		}

		public void reset() { index = -1; }
	}

	/* ************************************************************
				DoJa-specific methods
		*********************************************************** */

	public void setScratchPadIndex(int index) { scratchPadIndex = index; }

	/* ************************************************************
				Saving to and loading from disk
		*********************************************************** */

	public void saveRecordStore()
	{
		final String ownerVersion = Mobile.isDoJa ? Mobile.getPlatform().loader.getProperty("AppVer") : Mobile.getPlatform().loader.getProperty("MIDlet-Version");
		String recordName = name; // TODO: For doja, get the sp index

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String lastModifiedDate = dateFormat.format(new Date(lastModified));

		int[] validRecords = new int[recordIds.size()-1];

		// Building JSON string
		StringBuilder jsonBuilder = new StringBuilder();
		jsonBuilder.append("{\n")
			.append("  \"rmsVersion\": ").append("\""+RMS_VERSION+"\"").append(",\n")
			.append("  \"rmsDate\": ").append("\""+lastModifiedDate+"\"").append(",\n")
			.append("  \"ownerVersion\": \"").append(ownerVersion).append("\",\n")
			.append("  \"otherWrite\": ").append(writablebyothers ? 1 : 0).append(",\n")
			.append("  \"lastModified\": ").append(lastModified).append(",\n")
			.append("  \"modificationCount\": ").append(getVersion()).append(",\n")
			.append("  \"authentication\": ").append(authmode).append(",\n")
			.append("  \"ownerVendor\": \"").append(vendorname).append("\",\n")
			.append("  \"password\": \"").append(password).append("\",\n")
			.append("  \"recordName\": \"").append(recordName).append("\",\n")
			.append("  \"baseName\": \"").append(basename).append("\",\n")
			.append("  \"ownerName\": \"").append(suitename).append("\",\n")
			.append("  \"compatibleLastId\": ").append(nextid).append(",\n");

		// Write tags, followed by the record IDs
		for(int i = 1; i < recordIds.size(); i++) // Tags
		{
			jsonBuilder.append("  \"tag:").append(recordIds.get(i)).append("\": ").append(recordTags.get(i)).append(",\n");
			validRecords[i-1] = recordIds.get(i);
		}
		jsonBuilder.append("  \"ids\": ").append(Arrays.toString(validRecords)); // IDs

		jsonBuilder.append("\n}"); // JSON has been properly created in memory, now save it to disk

		// TODO: Delete any previously saved data before writing the updated records
		deleteOutdatedRecords(rmsPath + "/", basename);

		// Write the JSON data to disk, as well as the records' binary data
		try
		{
			FileOutputStream fos = new FileOutputStream(rmsPath + "/" + basename + ".rms");
			fos.write(jsonBuilder.toString().getBytes());
			fos.close();

			for(int i = 1; i < recordIds.size(); i++) // Write Binary Data
			{
				if(records.get(i) == null) { continue; } // Skip records that have been deleted and whose IDs are not to be used anymore
				fos = new FileOutputStream(rmsPath + "/" + basename + "." + recordIds.get(i));
				fos.write(records.get(i));
				fos.close();
			}

		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "> Couldn't save RecordStore " + name + " :" + e.getMessage());
			e.printStackTrace();
		}
	}

	public void loadRecordStore(boolean createIfNecessary) throws RecordStoreException, RecordStoreNotFoundException, SecurityException
	{
		file = new File(rmsFile);
		if(!file.exists())
		{
			if(!createIfNecessary)
			{
				throw (new RecordStoreNotFoundException("Record Store Doesn't Exist: " +suitename+"/"+basename+".rms"));
			}

			try // Check Record Store File
			{
				Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "> Creating New Record Store "+suitename+"/"+basename+".rms");
				file.createNewFile();
				version = 1;
				nextid = records.size(); // Since "records" always receives a dummy record on start, this will safely be 1 as it should.
				lastModified = System.currentTimeMillis(); // When creating a new empty record, we should save the creation date
				saveRecordStore();
			}
			catch (Exception e)
			{
				Mobile.log(Mobile.LOG_ERROR, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + e.getMessage());
				throw(new RecordStoreException("Problem Opening Record Store (createIfNecessary "+createIfNecessary+"): "+rmsFile));
			}
		}

		try
		{
			Map<String, Object> jsonMap = new HashMap<String, Object>();
			StringBuilder jsonBuilder = new StringBuilder();
			FileInputStream fis = new FileInputStream(rmsFile);
			Scanner scanner = new Scanner(fis);
			while (scanner.hasNextLine()) { jsonBuilder.append(scanner.nextLine().trim()); }

			scanner.close();
			fis.close();

			String jsonString = jsonBuilder.toString();
			// Remove outer braces
			jsonString = jsonString.substring(1, jsonString.length() - 1).trim();

			// Split by commas to get each entry
			String[] entries = jsonString.split(",(?![^\\[]*\\])");

			for (String entry : entries)
			{
				// Split at the first colon
				int colonIndex = entry.indexOf("\":");
				if (colonIndex != -1)
				{
					String key = entry.substring(1, colonIndex).trim().replace("\"", "");
					String value = entry.substring(colonIndex+2).trim();

					// Handle different value types
					if (value.startsWith("\"") && value.endsWith("\""))
					{
						// String values

						if(key.equals("lastModified"))  { lastModified = Integer.parseInt(value); } // lastModified date
						else if(key.equals("password")) { password = value.substring(1, value.length() - 1); } // Retrieve password without quotes
						// If the json representation expands further, this might have more keys being matched
					}
					else if (value.startsWith("[") && value.endsWith("]"))
					{
						// Array values

						String arrayContent = value.endsWith("]") ? value.substring(1, value.length() - 1) : value.substring(1, value.length() - 2);

						if(arrayContent.equals("")) { continue; }

						String[] arrayItems = arrayContent.split(",");
						int[] intArray = new int[arrayItems.length];
						for (int i = 0; i < intArray.length; i++) { intArray[i] = Integer.parseInt(arrayItems[i].trim()); }
						if(key.contains("ids"))
						{
							for(int i = 0; i < intArray.length; i++) { recordIds.add(intArray[i]); }
						}
					}
					else
					{
						// Numerical/boolean values
						if(key.contains("tag:")) { recordTags.add(Integer.parseInt(value)); }
						else if(key.equals("otherWrite")) { writablebyothers = (Integer.parseInt(value) == 1); }
						else if(key.equals("authentication")) { authmode = Integer.parseInt(value); }
						else if(key.equals("modificationCount")) { version = Integer.parseInt(value); }
						else if(key.equals("compatibleLastId")) { nextid = Integer.parseInt(value); }
					}
				}
			}

			// Throw a security exception if the record is from a different suite and is set to not be accessed by others
			if(!Mobile.getPlatform().loader.suitename.equals(this.suitename) && authmode != AUTHMODE_ANY) { throw new SecurityException("This suite does not have authorization to access the requested RecordStore:" + name); }

			for(int i = 1; i < recordIds.size(); i++) // Read Binary Data
			{
				FileInputStream binfis = new FileInputStream(rmsFile.substring(0, rmsFile.length()-4) + "." + recordIds.get(i));
				byte[] binData = new byte[binfis.available()];
				binfis.read(binData);
				records.add(binData);
				binfis.close();
			}
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": Couldn't load recordStore:" + name + " :" + e.getMessage());
			e.printStackTrace();
		}
	}

	// The legacy format had no handling of access modes, authorization, etc. For original FreeJ2ME it's even worse, it didn't even save the lastModified date or any recordIDs.
	public void loadLegacyRecordStore(String filePath, boolean createIfNecessary) throws RecordStoreException, RecordStoreNotFoundException
	{
		int offset = 0;
		int reclen;

		FileInputStream fis = null;
		ByteArrayOutputStream bos = null;

		file = new File(filePath);
		try // Read Records
		{
			fis = new FileInputStream(file);
			bos = new ByteArrayOutputStream();

			byte[] buffer = new byte[1024];
			int bytesRead;

			while ((bytesRead = fis.read(buffer)) != -1) { bos.write(buffer, 0, bytesRead); }

			byte[] data = bos.toByteArray();

			if(data.length>=4)
			{
				version = getUInt16(data, offset); offset+=2;
				nextid = getUInt16(data, offset); offset+=2;
				int recordcount = getUInt16(data, offset); offset+=2;

				Mobile.log(Mobile.LOG_DEBUG, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "Record count in "+filePath + ": " + recordcount);

				// get each record's data
				for(int i=0; i<recordcount; i++)
				{
					reclen = getUInt16(data, offset);
					offset+=2;

					loadRecord(data, offset, reclen);
					offset+=reclen;
				}

				if(data.length - offset < 8) // Doesn't have the lastModified field, it's a (very) old, original FreeJ2ME recordStore
				{
					lastModified = System.currentTimeMillis();

					for(int i = 0; i < recordcount; i++)
					{
						recordIds.addElement(i+1);
						recordTags.addElement(0);
					}
				}
				else // Save appears to have lastModified field, so treat it as a FreeJ2ME-Plus legacy recordStore
				{
					// Get last modified date
					lastModified = getLong(data, offset); offset+=8;

					// get record Ids
					if(data.length - offset >= 4) // Good, we already have record ids properly saved, load them up
					{
						for(int i = 0; i < recordcount; i++)
						{
							recordIds.addElement(getUint32(data, offset));
							recordTags.addElement(0);
							offset+=4;
						}
					}
					else // For compatibility with older saves, we'll populate recordIds with the records vector positions (hopefully a new save will correct the data)
					{
						for(int i = 0; i < recordcount; i++)
						{
							recordIds.addElement(i+1);
							recordTags.addElement(0);
						}
					}
				}
			}
			saveRecordStore(); // Save in order to write the converted record on disk
			file.delete(); // Delete this legacy record, as the new one is already in place
		}
		catch (Exception e)
		{
			Mobile.log(Mobile.LOG_ERROR, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + "Problem Reading Record Store: "+filePath);
			Mobile.log(Mobile.LOG_ERROR, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": " + e.getMessage());
			throw(new RecordStoreException("Problem Reading Record Store: "+filePath));
		}
		finally
		{
			try
			{
				if (fis != null) { fis.close(); }
				if (bos != null) { bos.close(); }
			}
			catch (IOException e) { e.printStackTrace(); }
		}
	}


	private static String returnRecordStoreName(String filePath) throws RecordStoreException, RecordStoreNotFoundException, SecurityException
	{
		try
		{
			Map<String, Object> jsonMap = new HashMap<String, Object>();
			StringBuilder jsonBuilder = new StringBuilder();
			FileInputStream fis = new FileInputStream(filePath);
			Scanner scanner = new Scanner(fis);
			while (scanner.hasNextLine()) { jsonBuilder.append(scanner.nextLine().trim()); }

			scanner.close();
			fis.close();

			String jsonString = jsonBuilder.toString();
			jsonString = jsonString.substring(1, jsonString.length() - 1).trim();

			String[] entries = jsonString.split(",(?![^\\[]*\\])");

			for (String entry : entries)
			{
				// Split at the first colon
				int colonIndex = entry.indexOf("\":");
				if (colonIndex != -1)
				{
					String key = entry.substring(1, colonIndex).trim().replace("\"", "");
					String value = entry.substring(colonIndex+2).trim();

					// Found the actual recordName inside the Store, retrieve it
					if(key.equals("recordName")) { return value.substring(1, value.length() - 1).trim(); }
				}
			}
		}
		catch(Exception e) { Mobile.log(Mobile.LOG_ERROR, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": Couldn't return the record Store Name:" + e.getMessage()); }

		Mobile.log(Mobile.LOG_WARNING, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": Record does not have a recordName field. Expect bugs!");
		return null;
	}

	public final void deleteOutdatedRecords(String rmsPath, String basename)
	{
		File directory = new File(rmsPath);

		if (directory.exists() && directory.isDirectory())
		{
			// Get all files in the directory to then delete any that match the current record's basename
			File[] files = directory.listFiles();

			if (files != null)
			{
				for (File file : files)
				{
					if (file.getName().startsWith(basename)) { file.delete(); }
				}
			}
		}
		else { } // Dir does not exist, nothing to delete
	}


	// These two are used so that FreeJ2ME-Plus matches SquirrelJME's save layout
	public static final String generateBaseName(String owner, String name)
	{
		String base64Encoded = "";

		try
		{
			base64Encoded = Base64Util.encode(name.getBytes("UTF-8"))
				.toLowerCase()
				.replace('=', '_');
		}
		catch (Exception e) { Mobile.log(Mobile.LOG_ERROR, RecordStore.class.getPackage().getName() + "." + RecordStore.class.getSimpleName() + ": Failed to properly encode the recordStores disk name!"); }

		return String.format("%08x%02d%s", ownerHashcode(owner, Mobile.getPlatform().loader.suitename), name.length(), base64Encoded);
	}

	public static final int ownerHashcode(String owner, String name) { return name.hashCode() ^ owner.hashCode(); }
}

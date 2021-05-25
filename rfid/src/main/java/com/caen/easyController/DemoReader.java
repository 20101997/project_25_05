package com.caen.easyController;

import android.os.Parcel;
import android.os.Parcelable;

import com.caen.RFIDLibrary.CAENRFIDException;
import com.caen.RFIDLibrary.CAENRFIDPort;
import com.caen.RFIDLibrary.CAENRFIDRFRegulations;
import com.caen.RFIDLibrary.CAENRFIDReader;
public class DemoReader implements Parcelable{
	private CAENRFIDReader reader;
	private String name;
	private String serial;
	private String firmwareRelease;
	private String regulation;
	private CAENRFIDPort connectionType;
	
	public DemoReader(CAENRFIDReader caenReader, String readerName, String serialNum, String fwRel, CAENRFIDPort connType){
		this.setReader(caenReader);
		this.setReaderName(readerName);
		this.setSerialNumber(serialNum);
		this.setFirmwareRelease(fwRel);
		try {
			this.setRegulation(caenReader.GetRFRegulation());
		} catch (CAENRFIDException e) {
			e.printStackTrace();
		}
		this.setConnectionType(connType);
	}


	public void setFirmwareRelease(String firmwareRelease) {
		this.firmwareRelease = firmwareRelease;
	}

	public String getSerialNumber() {
		return serial;
	}

	public void setSerialNumber(String serial) {
		this.serial = serial;
	}

	public String getReaderName() {
		return name;
	}

	public void setReaderName(String name) {
		this.name = name;
	}

	public CAENRFIDReader getReader() {
		return reader;
	}

	public void setReader(CAENRFIDReader reader) {
		this.reader = reader;
	}


	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		Object[] array=new Object[4];
		array[0]=this.reader;
		array[1]=this.name;
		array[2]=this.serial;
		array[3]=this.firmwareRelease;
		dest.writeArray(array);
	}
	
	public static final Parcelable.Creator<DemoReader> CREATOR = new Parcelable.Creator<DemoReader>() {
		public DemoReader createFromParcel(Parcel in) {
			return new DemoReader(in);
		}
		public DemoReader[] newArray(int size) {
			return new DemoReader[size];
		}
	};

	private DemoReader(Parcel in) {
		this.reader=(CAENRFIDReader) in.readValue(DemoReader.class.getClassLoader());
		this.name=in.readString();
		this.serial=in.readString();
		this.firmwareRelease=in.readString();
	}


	public void setRegulation(CAENRFIDRFRegulations reg){
		if(CAENRFIDRFRegulations.AUSTRALIA.getshortValue() == reg.getshortValue())
			this.regulation="AUSTRALIA";
		else if(CAENRFIDRFRegulations.BRAZIL.getshortValue() == reg.getshortValue())
			this.regulation="BRAZIL";
		else if(CAENRFIDRFRegulations.CHINA.getshortValue() == reg.getshortValue())
			this.regulation="CHINA";
		else if(CAENRFIDRFRegulations.ETSI_300220.getshortValue() == reg.getshortValue())
			this.regulation="ETSI 300220";
		else if(CAENRFIDRFRegulations.ETSI_302208.getshortValue() == reg.getshortValue())
			this.regulation="ETSI 302208";
		else if(CAENRFIDRFRegulations.FCC_US.getshortValue() == reg.getshortValue())
			this.regulation="FCC_US";
		else if(CAENRFIDRFRegulations.JAPAN.getshortValue() == reg.getshortValue())
			this.regulation="JAPAN";
		else if(CAENRFIDRFRegulations.KOREA.getshortValue() == reg.getshortValue())
			this.regulation="KOREA";
		else if(CAENRFIDRFRegulations.MALAYSIA.getshortValue() == reg.getshortValue())
			this.regulation="MALAYSIA";
		else if(CAENRFIDRFRegulations.SINGAPORE.getshortValue() == reg.getshortValue())
			this.regulation="SINGAPORE";
		else if(CAENRFIDRFRegulations.TAIWAN.getshortValue() == reg.getshortValue())
			this.regulation="TAIWAN";
		else if(CAENRFIDRFRegulations.CHILE.getshortValue() == reg.getshortValue())
			this.regulation="CHILE";
		else if(CAENRFIDRFRegulations.HONG_KONG.getshortValue() == reg.getshortValue())
			this.regulation="HONG_KONG";
		else if(CAENRFIDRFRegulations.INDONESIA.getshortValue() == reg.getshortValue())
			this.regulation="INDONESIA";
		else if(CAENRFIDRFRegulations.ISRAEL.getshortValue() == reg.getshortValue())
			this.regulation="ISRAEL";
		else if(CAENRFIDRFRegulations.PERU.getshortValue() == reg.getshortValue())
			this.regulation="PERU";
		else if(CAENRFIDRFRegulations.JAPAN_STD_T106.getshortValue() == reg.getshortValue())
			this.regulation="JAPAN_STD_T106";
		else if(CAENRFIDRFRegulations.JAPAN_STD_T106_L.getshortValue() == reg.getshortValue())
			this.regulation="JAPAN_STD_T106_L";
		else if(CAENRFIDRFRegulations.JAPAN_STD_T107.getshortValue() == reg.getshortValue())
			this.regulation="JAPAN_STD_T107";
		else if(CAENRFIDRFRegulations.SOUTH_AFRICA.getshortValue() == reg.getshortValue())
			this.regulation="SOUTH_AFRICA";
		else
			this.regulation="UNKNOWN";

	}

	public CAENRFIDPort getConnectionType() {
		return connectionType;
	}

	public void setConnectionType(CAENRFIDPort connectionType) {
		this.connectionType = connectionType;
	}

}

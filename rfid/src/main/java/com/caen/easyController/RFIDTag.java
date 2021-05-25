package com.caen.easyController;


import com.caen.RFIDLibrary.CAENRFIDException;
import com.caen.RFIDLibrary.CAENRFIDNotify;
import com.caen.RFIDLibrary.CAENRFIDTag;
import java.nio.charset.StandardCharsets;

public class RFIDTag {
	private CAENRFIDNotify mTag;
	private String mId;
	private short mRssi;
	private int counter;
	private String ascii;


	private static final int MAX_RETRY = 3;


	static final char[] hexChar = { '0', '1', '2', '3', '4', '5', '6', '7',
			'8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

	public static String toHexString(byte[] b) {
		StringBuilder sb = new StringBuilder(b.length * 2);
		for (byte aB : b) {
			sb.append(hexChar[(aB & 0xf0) >>> 4]);
			sb.append(hexChar[aB & 0x0f]);
		}
		return sb.toString();
	}

	public static byte[] hexStringToByteArray(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
		}
		return data;
	}

	public static String toASCII(byte[] b) {

		return new String(b, StandardCharsets.ISO_8859_1);
	}





	public static byte[] ReadWithRetry(CAENRFIDTag tag, short MemBank,
			short Address, short Length) {
		int retry_count = 0;
		boolean retry = true;
		byte[] result = null;
		while (retry)
			try {
				result = tag.GetSource().ReadTagData_EPC_C1G2(tag, MemBank,
						Address, Length);
				retry = false;
			} catch (CAENRFIDException err) {
				retry_count++;
				if (retry_count == MAX_RETRY) {
					retry = false;
				}
			}
		return result;
	}


	public RFIDTag(CAENRFIDNotify tag,String Id, short rssi) {
		this.mTag = tag;

		this.mId = Id;
		this.setmRssi(rssi);
		this.setCounter(1);
		this.setAscii(toASCII(mTag.getTagID()));
	}

	public CAENRFIDNotify getTag() {
		return mTag;
	}

	public void setTag(CAENRFIDNotify tag) {
		this.mTag = tag;
	}

	@Override
	public String toString() {
		return this.mId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((mId == null) ? 0 : mId.hashCode());
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
		RFIDTag other = (RFIDTag) obj;
		if (mId == null) {
			return other.mId == null;
		} else return mId.equals(other.mId);
	}



	public String getId() {
		return this.mId;
	}

	public void setId(String mId) {
		this.mId = mId;
	}

	public void setmRssi(short mRssi) {
		this.mRssi = mRssi;
	}

	public int getCounter() {
		return counter;
	}

	public void setCounter(int counter) {
		this.counter = counter;
	}

	public String getAscii() {
		return ascii;
	}

	public void setAscii(String ascii) {
		this.ascii = ascii;
	}
}

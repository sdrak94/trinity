package luna;

import java.util.Arrays;

public class HexUtil
{
	public static <T> T[] concat(T[] first, T[] second)
	{
		T[] result = Arrays.copyOf(first, first.length + second.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}
	
	public static byte[] concat(byte[] first, byte[] second)
	{
		byte[] result = Arrays.copyOf(first, first.length + second.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}
	
	public static final byte[] writeInt(int value)
	{
		return new byte[]
		{
			(byte) (value >> 24),
			(byte) (value >> 16),
			(byte) (value >> 8),
			(byte) value
		};
	}
	
	public static final byte[] rwriteInt(int value)
	{
		return new byte[]
		{
			(byte) value,
			(byte) (value >> 8),
			(byte) (value >> 16),
			(byte) (value >> 24),
		};
	}
	
	public static final byte[] writeLong(long value)
	{
		return new byte[]
		{
			(byte) (value >> 56),
			(byte) (value >> 48),
			(byte) (value >> 40),
			(byte) (value >> 32),
			(byte) (value >> 24),
			(byte) (value >> 16),
			(byte) (value >> 8),
			(byte) value
		};
	}
	
	public static void writeInt(long value, byte[] buffer, int offset)
	{
		buffer[offset] = (byte) (value >> 24);
		buffer[offset + 1] = (byte) (value >> 16);
		buffer[offset + 2] = (byte) (value >> 8);
		buffer[offset + 3] = (byte) value;
	}
	
	public static void writeLong(long value, byte[] buffer, int offset)
	{
		buffer[offset] = (byte) (value >> 56);
		buffer[offset + 1] = (byte) (value >> 48);
		buffer[offset + 2] = (byte) (value >> 40);
		buffer[offset + 3] = (byte) (value >> 32);
		buffer[offset + 4] = (byte) (value >> 24);
		buffer[offset + 5] = (byte) (value >> 16);
		buffer[offset + 6] = (byte) (value >> 8);
		buffer[offset + 7] = (byte) value;
	}
	
	public static int readInt(byte[] buffer)
	{
		return readInt(buffer, 0);
	}
	
	public static int readInt(byte[] buffer, int offset)
	{
		return buffer[offset + 3] & 0xFF | (buffer[offset + 2] & 0xFF) << 8 | (buffer[offset + 1] & 0xFF) << 16 | (buffer[offset] & 0xFF) << 24;
	}
	
	public static int rreadInt(byte[] buffer, int offset)
	{
		return (buffer[offset + 3] & 0xFF) << 24 | (buffer[offset + 2] & 0xFF) << 16 | (buffer[offset + 1] & 0xFF) << 8 | buffer[offset + 0] & 0xFF;
	}
	
	public static long readLong(byte[] buffer, int offset)
	{
		return buffer[offset + 7] & 0xFF | (buffer[offset + 6] & 0xFF) << 8 | (buffer[offset + 5] & 0xFF) << 16 | (buffer[offset + 4] & 0xFF) << 24 | (buffer[offset + 3] & 0xFF) << 32 | (buffer[offset + 2] & 0xFF) << 40 | (buffer[offset + 1] & 0xFF) << 48 | (buffer[offset] & 0xFF) << 56;
	}
	
	public static void printAsHex(byte[] data)
	{
		for (byte b : data)
			System.out.print("0x" + String.format("%02x", b) + " ");
		System.out.println();
	}
	
	public static void hexdump(byte[] data, int len, int offset, char delim, int incr)
	{
		for (int i = offset; i < len; i++)
		{
			System.out.printf("%02X%c", data[i], delim);
			if (i + 1 % incr == 0)
				System.out.println();
		}
		System.out.println();
	}
	
	public static void hexdump(byte[] data, char delim, int incr)
	{
		hexdump(data, data.length, 0, delim, incr);
	}
	
	public static void hexdump(byte[] data, int incr)
	{
		hexdump(data, ' ', incr);
	}
	
	public static String hexstr(byte[] data)
	{
		StringBuilder sb = new StringBuilder();
		if (data == null)
			return "";
		for (byte b : data)
		{
			sb.append(String.format("%02X", b));
		}
		//System.out.println(sb.toString());
		String hex = sb.toString();
		return hex;
	}
	
	public static void mencdec(byte[] _keys, byte[] in)
	{
		for (int i = 0; i < in.length; i++)
			in[i] = (byte) (in[i] ^ (_keys[i % _keys.length]));
	}
}

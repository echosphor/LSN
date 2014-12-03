package util;

import java.io.UnsupportedEncodingException;

import messages.AbstractMessage;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.filter.codec.demux.MessageDecoderResult;
import org.slf4j.LoggerFactory;
import java.io.*;
import com.jcraft.jzlib.*;


import codec.ConnectEncoder;


public class Utils {
	
	public static final int MAX_LENGTH_LIMIT = 268435455;
	 private static final int MAXLENGTH = 102400;
	 private static final int BUFFERSIZE = 1024;
	
    public static int readWord(IoBuffer in) {
        int msb = in.get() & 0x00FF; 
        int lsb = in.get() & 0x00FF;
        msb = (msb << 8) | lsb ;
        return msb;
    }
    
    public static void writeWord(IoBuffer out, int value) {
        out.put((byte) ((value & 0xFF00) >> 8)); //msb
        out.put((byte) (value & 0x00FF)); //lsb
    }
    
    public static IoBuffer encodeString(String str) {
        IoBuffer out = IoBuffer.allocate(2).setAutoExpand(true);
        byte[] raw;
        try {
            raw = str.getBytes("UTF-8");
        } catch (UnsupportedEncodingException ex) {
            LoggerFactory.getLogger(ConnectEncoder.class).error(null, ex);
            return null;
        }
        Utils.writeWord(out, raw.length);//长度
        out.put(raw).flip();//内容
        return out;
    }
    
    public static String decodeString(IoBuffer in) throws UnsupportedEncodingException {
        if (in.remaining() < 2) {
            return null;
        }
        int strLen = Utils.readWord(in);
        if (in.remaining() < strLen) {
            return null;
        }
        byte[] strRaw = new byte[strLen];
        in.get(strRaw);

        return new String(strRaw, "UTF-8");
    }
    
	public static int decodeRemainingLenght(IoBuffer in) {
        int multiplier = 1;
        int value = 0;
        byte digit;
        do {
            if (in.remaining() < 1) {
                return -1;
            }
            digit = in.get();
            value += (digit & 0x7F) * multiplier;
            multiplier *= 128;
        } while ((digit & 0x80) != 0);
        return value;
    }
    
	public static IoBuffer encodeRemainingLength(int value) throws IllegalAccessException {
        if (value > MAX_LENGTH_LIMIT || value < 0) {
            throw new IllegalAccessException("Value should in range 0.." + MAX_LENGTH_LIMIT + " found " + value);
        }

        IoBuffer encoded = IoBuffer.allocate(4);
        byte digit;
        do {
            digit = (byte) (value % 128);
            value = value / 128;
            if (value > 0) {
                digit = (byte) (digit | 0x80);
            }
            encoded.put(digit);
        } while (value > 0);
        encoded.flip();
        return encoded;
    }
	
    public static MessageDecoderResult checkDecodable(byte type, IoBuffer in) {
        if (in.remaining() < 1) {
            return MessageDecoderResult.NEED_DATA;
        }
        byte h1 = in.get();
        byte messageType = (byte) ((h1 & 0x00F0) >> 4);
        
        int remainingLength = Utils.decodeRemainingLenght(in);
        if (remainingLength == -1) {
            return MessageDecoderResult.NEED_DATA;
        }
        
        if (in.remaining() < remainingLength) {
            return MessageDecoderResult.NEED_DATA;
        }
        
        return messageType == type ? MessageDecoderResult.OK : MessageDecoderResult.NOT_OK;
    }
    
    public static byte encodeFlags(AbstractMessage message) {
        byte flags = 0;
        if (message.isDupFlag()) {
            flags |= 0x08;
        }
        if (message.isRetainFlag()) {
            flags |= 0x01;
        }
        
        flags |= ((message.getQos().ordinal() & 0x03) << 1);
        return flags;
    }
    
    //获取表示剩余长度需要的字节数
    public static int numBytesToEncode(int len) {
        if (0 <= len && len <= 127) return 1;
        if (128 <= len && len <= 16383) return 2;
        if (16384 <= len && len <= 2097151) return 3;
        if (2097152 <= len && len <= 268435455) return 4;
        throw new IllegalArgumentException("value shoul be in the range [0..268435455]");
    }
    
    /**
     * Converts MQTT message type to a textual description.
     * */
    public static String msgType2String(int type) {
        switch (type) {
            case AbstractMessage.CONNECT: return "CONNECT";
            case AbstractMessage.CONNACK: return "CONNACK";
            case AbstractMessage.PUSH: return "PUSH";
            case AbstractMessage.PUSHACK: return "PUSHACK";
            case AbstractMessage.POLL: return "POLL";
            case AbstractMessage.POLLRESP: return "POLLRESP";
            case AbstractMessage.PINGREQ: return "PINGREQ";
            case AbstractMessage.PINGRESP: return "PINGRESP";
            case AbstractMessage.DISCONNECT: return "DISCONNECT";
            default: throw  new RuntimeException("Can't decode message type " + type);
        }
    }
    
    public static byte[] Compress(byte[] object) throws IOException{
    	  byte[] data = null;
    	  try {
    	   ByteArrayOutputStream out = new ByteArrayOutputStream();
    	   ZOutputStream zOut = new ZOutputStream(out,
    	     JZlib.Z_BEST_COMPRESSION);
    	   DataOutputStream objOut = new DataOutputStream(zOut);
    	   objOut.write(object);
    	   objOut.flush();
    	   zOut.close();
    	   data = out.toByteArray();
    	   out.close();

    	  } catch (IOException e) {
    	   e.printStackTrace();
    	   throw e;
    	  }
    	  return data;
    }
    
    public static byte[] UnCompress(byte[] object) throws IOException {
    	  byte[] data = new byte[MAXLENGTH];
    	  try {
    	   ByteArrayInputStream in = new ByteArrayInputStream(object);
    	   ZInputStream zIn = new ZInputStream(in);
    	   DataInputStream objIn = new DataInputStream(zIn);

    	   int len = 0;
    	   int count = 0;
    	   while ((count = objIn.read(data, len, len + BUFFERSIZE)) != -1) {
    	    len = len + count;
    	   }

    	   byte[] trueData = new byte[len];
    	   System.arraycopy(data, 0, trueData, 0, len);

    	   objIn.close();
    	   zIn.close();
    	   in.close();

    	   return trueData;

    	  } catch (IOException e) {
    	   e.printStackTrace();
    	   throw e;
    	  }
    }
}

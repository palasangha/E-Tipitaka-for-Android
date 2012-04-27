package org.yuttadhammo.tipitaka;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import android.content.res.Resources;
import biz.source_code.base64Coder.Base64Coder;

public class Utils {
	public static String toStringBase64(Object o) throws IOException {
    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
    	ObjectOutputStream oos = new ObjectOutputStream( baos );
    	oos.writeObject(o);
    	oos.close();
        String s = new String( Base64Coder.encode( baos.toByteArray() ) );
        return s;
	}
	
	public static Object fromStringBase64(String s) throws ClassNotFoundException, IOException {
        byte [] data = Base64Coder.decode(s);
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
        Object o   = (ArrayList<String>) ois.readObject();
        ois.close();
		return o;		
	}
	
	public static String arabic2thai(String number, Resources res) {

		return number;	
	}
	
	public static String arabic2thai(int number, Resources res) {
		return Integer.toString(number);
	}
}
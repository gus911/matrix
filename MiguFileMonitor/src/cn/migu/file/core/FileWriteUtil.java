package cn.migu.file.core;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class FileWriteUtil {

	// private static SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd
	// HH:mm:ss");

	public static void Write(String path, int count) throws IOException, InterruptedException {

		BufferedOutputStream buff = new BufferedOutputStream(new FileOutputStream(path));

		for (int i = 0; i < count; i++) {

			// buff.write(
			// (format.format(new Date()) + " INFO " + i + " " +
			// getRandomString(16) + " " + getIntervalNum(10, 20)
			// + " " + getRandomString(32) + " " + getIntervalNum(1, 50000) + "
			// " + getRandomString(16) + "\n")
			// .getBytes());

			//Thread.sleep(1000);
			buff.write(
					(i + "\037" + getRandomString(16) + "\037" + getIntervalNum(10, 20) + "\037" + getRandomString(32)
							+ "\037" + getIntervalNum(1, 50000) + "\037" + getRandomString(16) + "\n").getBytes());

			buff.flush();
		}

		
		buff.close();
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		Write("/10m.a", 1000000);

	}

	public static String getLongDateString(Long time) {

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		if (time == null) {
			return sdf.format(new Date());
		}

		Date date = new Date(time);
		return sdf.format(date);
	}

	public static String getRandomString(int len) {
		String seed = "abcdefghijklmnopqrstuvwxyz0123456789";
		Random random = new Random();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < len; i++) {
			int number = random.nextInt(seed.length());
			sb.append(seed.charAt(number));
		}
		return sb.toString();
	}

	public static String noRepeatNum(int count) {
		if (count > 10) {
			count = 10;
		}
		StringBuffer sb = new StringBuffer();
		String str = "0123456789";
		Random r = new Random();
		for (int i = 0; i < count; i++) {
			int num = r.nextInt(str.length());
			sb.append(str.charAt(num));
			str = str.replace((str.charAt(num) + ""), "");
		}
		return sb.toString();
	}

	public static int getIntervalNum(int min, int max) {

		Random random = new Random();

		int num = random.nextInt(max) % (max - min + 1) + min;

		return num;
	}
}

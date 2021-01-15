package com.zhuandian.bluetooth;

import android.content.Context;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.write.Label;
import jxl.write.WritableCell;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;

/**
 * @description: Excel 工具类
 * @author: ODM
 */
public class ExcelUtil {

	public static WritableFont arial12font = null;
	public static WritableCellFormat arial12format = null;

	public final static String UTF8_ENCODING = "UTF-8";
	public final static String GBK_ENCODING = "GBK";


	public static void format() {
		try {
			arial12font = new WritableFont(WritableFont.ARIAL, 12);
			arial12format = new WritableCellFormat(arial12font);
			arial12format.setBorder(jxl.format.Border.ALL, jxl.format.BorderLineStyle.THIN);
		} catch (WriteException e) {
			e.printStackTrace();
		}
	}


	/**
	 * 初始化表格，包括文件名、sheet名、各列的名字
	 *
	 * @param filePath  文件路径
	 * @param sheetName sheet名
	 * @param colName   各列的名字
	 */
	public static void initExcel(String filePath, String sheetName, String[] colName) {
		format();
		WritableWorkbook workbook = null;
		try {
			File file = new File(filePath);
			if (!file.exists()) {
				file.createNewFile();
			}
			workbook = Workbook.createWorkbook(file);
			WritableSheet sheet = workbook.createSheet(sheetName, 0);
			sheet.addCell((WritableCell) new Label(0, 0, filePath, arial12format));
			for (int col = 0; col < colName.length; col++) {
				sheet.addCell(new Label(col, 0, colName[col], arial12format));
			}
			workbook.write();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (workbook != null) {
				try {
					workbook.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

	}

	/**
	 * 将数据写入Excel表格
	 *
	 * @param objList  要写的列表数据
	 * @param filePath 文件路径
	 * @param c        上下文
	 * @param <T>
	 */
	public static <T> void writeObjListToExcel(List<T> objList, String filePath, Context c) {
		if (objList != null && objList.size() > 0) {
			WritableWorkbook writebook = null;
			InputStream in = null;
			try {
				WorkbookSettings setEncode = new WorkbookSettings();
				setEncode.setEncoding(UTF8_ENCODING);
				in = new FileInputStream(new File(filePath));
				Workbook workbook = Workbook.getWorkbook(in);
				writebook = Workbook.createWorkbook(new File(filePath), workbook);
				WritableSheet sheet = writebook.getSheet(0);
				for (int j = 0; j < objList.size(); j++) {
					ArrayList<String> list = (ArrayList<String>) objList.get(j);
					for (int i = 0; i < list.size(); i++) {
						sheet.addCell(new Label(i, j + 1, list.get(i), arial12format));
					}
				}
				writebook.write();
				Toast.makeText(c, " 导出成功 ", Toast.LENGTH_SHORT).show();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (writebook != null) {
					try {
						writebook.close();
					} catch (Exception e) {
						e.printStackTrace();
					}

				}
				if (in != null) {
					try {
						in.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

		}
	}

}

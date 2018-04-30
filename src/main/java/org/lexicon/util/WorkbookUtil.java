package org.lexicon.util;

import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.poi.ss.usermodel.Workbook;

public class WorkbookUtil {

    // static methods only
    private WorkbookUtil() {}

    /**
     * @param workbook
     *            Workbook to write
     * @param filePath
     *            FilePath the workbook is to write in
     * @return <code>true</code> if successful
     */
    public static boolean writeWorkbookToFile(Workbook workbook, String filePath) {
        try {
            FileOutputStream out = new FileOutputStream(filePath);
            workbook.write(out);
            workbook.close();
            out.close();
            return true;
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}

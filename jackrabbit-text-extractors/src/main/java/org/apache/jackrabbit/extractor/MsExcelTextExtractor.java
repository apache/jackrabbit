/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.extractor;

import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.InputStream;
import java.io.IOException;
import java.io.CharArrayWriter;
import java.io.CharArrayReader;
import java.io.StringReader;
import java.util.Iterator;

/**
 * Text extractor for Microsoft Excel sheets.
 */
public class MsExcelTextExtractor extends AbstractTextExtractor {

    /**
     * Logger instance.
     */
    private static final Logger logger =
        LoggerFactory.getLogger(MsExcelTextExtractor.class);

    /**
     * Force loading of dependent class.
     */
    static {
        POIFSFileSystem.class.getName();
    }

    /**
     * Creates a new <code>MsExcelTextExtractor</code> instance.
     */
    public MsExcelTextExtractor() {
        super(new String[]{"application/vnd.ms-excel", "application/msexcel"});
    }

    //-------------------------------------------------------< TextExtractor >

    /**
     * {@inheritDoc}
     */
    public Reader extractText(InputStream stream,
                              String type,
                              String encoding) throws IOException {
        CharArrayWriter writer = new CharArrayWriter();
        try {
            POIFSFileSystem fs = new POIFSFileSystem(stream);
            HSSFWorkbook workbook = new HSSFWorkbook(fs);

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                HSSFSheet sheet = workbook.getSheetAt(i);

                Iterator rows = sheet.rowIterator();
                while (rows.hasNext()) {
                    HSSFRow row = (HSSFRow) rows.next();

                    Iterator cells = row.cellIterator();
                    while (cells.hasNext()) {
                        HSSFCell cell = (HSSFCell) cells.next();
                        switch (cell.getCellType()) {
                        case HSSFCell.CELL_TYPE_NUMERIC:
                            String num = Double.toString(cell.getNumericCellValue()).trim();
                            if (num.length() > 0) {
                                writer.write(num + " ");
                            }
                            break;
                        case HSSFCell.CELL_TYPE_STRING:
                            String text = cell.getStringCellValue().trim();
                            if (text.length() > 0) {
                                writer.write(text + " ");
                            }
                            break;
                        }
                    }
                }
            }

            return new CharArrayReader(writer.toCharArray());
        } catch (RuntimeException e) {
            logger.warn("Failed to extract Excel text content", e);
            return new StringReader("");
        } finally {
            stream.close();
        }
    }
}

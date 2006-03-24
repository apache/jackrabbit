/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.query;

import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.query.lucene.FieldNames;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.BLOBFileValue;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

/**
 * Extracts texts from MS Excel document binary data.
 * Taken from Jakarta Slide class
 * <code>org.apache.slide.extractor.MSExcelExtractor</code>
 */
public class MsExcelTextFilter implements TextFilter {

    /**
     * @return <code>true</code> for <code>application/vnd.ms-excel</code>, <code>false</code> otherwise.
     */
    public boolean canFilter(String mimeType) {
        return "application/vnd.ms-excel".equalsIgnoreCase(mimeType);
    }

    /**
     * Returns a map with a single entry for field {@link FieldNames#FULLTEXT}.
     * @param data object containing MS Excel document data.
     * @param encoding text encoding is not used, since it is specified in the data.
     * @return a map with a single Reader value for field {@link FieldNames#FULLTEXT}.
     * @throws RepositoryException if data is a multi-value property or it does not
     * contain valid MS Excel document.
     */
    public Map doFilter(PropertyState data, String encoding) throws RepositoryException {
        InternalValue[] values = data.getValues();
        if (values.length > 0) {
            BLOBFileValue blob = (BLOBFileValue) values[0].internalValue();
                
            try {
                CharArrayWriter writer = new CharArrayWriter();
    
                POIFSFileSystem fs = new POIFSFileSystem(blob.getStream());
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
                
                Map result = new HashMap();
                result.put(FieldNames.FULLTEXT, new CharArrayReader(writer.toCharArray()));
                return result;
            } 
            catch (IOException ex) {
                throw new RepositoryException(ex);
            }
        } 
        else {
            // multi value not supported
            throw new RepositoryException("Multi-valued binary properties not supported.");
        }
    }
}
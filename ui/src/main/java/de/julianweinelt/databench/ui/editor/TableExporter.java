package de.julianweinelt.databench.ui.editor;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.io.*;

@Slf4j
public class TableExporter {
    private final File file;
    private final String extension;
    private final JTable resultTable;
    private final EditorCallBack callBack;

    public TableExporter(File file, String extension, EditorCallBack callBack, JTable resultTable) {
        this.file = file;
        this.extension = extension;
        this.callBack = callBack;
        this.resultTable = resultTable;
    }

    public void exportWithProgress(ProgressCallback callback) {
        TableModel model = resultTable.getModel();

        switch (extension) {
            case ".csv" -> exportCsv(model, file, callback);
            case ".xlsx" -> exportExcel(model, file, callback);
        }
    }

    private void exportCsv(TableModel model, File file, ProgressCallback callback) {
        File save = new File(file.getAbsolutePath().replace(".csv", "") + ".csv");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(save))) {

            int columnCount = model.getColumnCount();

            for (int col = 0; col < columnCount; col++) {
                writer.write(escapeCsv(model.getColumnName(col)));
                if (col < columnCount - 1) writer.write(",");
            }
            writer.newLine();

            int rowCount = model.getRowCount();

            for (int row = 0; row < rowCount; row++) {

                for (int col = 0; col < columnCount; col++) {
                    Object value = model.getValueAt(row, col);
                    writer.write(escapeCsv(value == null ? "" : value.toString()));
                    if (col < columnCount - 1) writer.write(",");
                }

                writer.newLine();
                callback.update(row + 1);
            }

        } catch (IOException ex) {
            this.callBack.call(ex.getMessage(), "error");
            log.error(ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }

    private String escapeCsv(String value) {
        boolean needsQuotes = value.contains(",")
                || value.contains("\"")
                || value.contains("\n")
                || value.contains("\r");

        value = value.replace("\"", "\"\"");

        return needsQuotes ? "\"" + value + "\"" : value;
    }

    private void exportExcel(TableModel model, File file, ProgressCallback callback) {
        File save = new File(file.getAbsolutePath().replace(".xlsx", "") + ".xlsx");
        try (SXSSFWorkbook workbook = new SXSSFWorkbook();
             FileOutputStream out = new FileOutputStream(save)) {

            Sheet sheet = workbook.createSheet("Results");

            Row header = sheet.createRow(0);
            int columnCount = model.getColumnCount();

            for (int col = 0; col < columnCount; col++) {
                Cell cell = header.createCell(col);
                cell.setCellValue(model.getColumnName(col));
            }

            int rowCount = model.getRowCount();

            for (int rowIdx = 0; rowIdx < rowCount; rowIdx++) {
                Row row = sheet.createRow(rowIdx + 1);

                for (int col = 0; col < columnCount; col++) {
                    Cell cell = row.createCell(col);
                    Object value = model.getValueAt(rowIdx, col);

                    if (value instanceof Number n) {
                        cell.setCellValue(n.doubleValue());
                    } else if (value instanceof Boolean b) {
                        cell.setCellValue(b);
                    } else {
                        cell.setCellValue(value == null ? "" : value.toString());
                    }
                }
                callback.update(rowIdx + 1);
            }

            workbook.write(out);
            workbook.dispose();

        } catch (Exception ex) {
            callBack.call(ex.getMessage(), "error");
            log.error(ex.getMessage(), ex);
        }
    }
}
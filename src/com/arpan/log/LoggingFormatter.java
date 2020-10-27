package com.arpan.log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class LoggingFormatter extends Formatter {
    @Override
    public String format(LogRecord record) {
        return calcDate(record.getMillis()) + ": " + record.getMessage() + "\n";
    }

    private String calcDate(long millis) {
        SimpleDateFormat date_format = new SimpleDateFormat("MMM dd, HH:mm:ss");
        Date resultDate = new Date(millis);
        return date_format.format(resultDate);
    }
}

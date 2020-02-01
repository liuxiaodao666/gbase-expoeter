package cn.com.zte.util;


import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.RolloverFailure;
import ch.qos.logback.core.rolling.helper.CompressionMode;
import ch.qos.logback.core.rolling.helper.Compressor;
import ch.qos.logback.core.rolling.helper.RenameUtil;


import java.io.File;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogSetting extends FixedWindowRollingPolicy {
    //    int maxIndex = super.getMaxIndex();
//    int minIndex = super.getMinIndex();
    Compressor compressor;
    RenameUtil util = new RenameUtil();
    private static String sixBitRandomNumber;

    static {
        sixBitRandomNumber = Integer.toString((int) ((Math.random() * 9 + 1) * 100000));
        //System.out.println(((int) ((Math.random() * 9 + 1) * 100000)));
    }


    public void start() {
        compressor = new Compressor(CompressionMode.GZ);
        compressor.setContext(this.context);
        super.start();
    }

    @Override
    public String getFileNamePattern() {
        Date date = new Date();
        DateFormat format = new SimpleDateFormat("yyyyMMddHHmmsss");
        String t = format.format(date);
        return fileNamePatternStr.replace("%i", t + "-[" + sixBitRandomNumber + "]");
    }

    public String getActiveFileName() {
        String s = getParentsRawFileProperty();
        Date date = new Date();
        DateFormat format = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        String t = format.format(date);
        //System.out.println(String.format(s, t, "123456"));
        CustomRollingFileAppender.FileName = String.format(s, t, sixBitRandomNumber);
        CustomRollingFileAppender.currentlyActiveFile=new File(CustomRollingFileAppender.FileName);
        return CustomRollingFileAppender.FileName;
    }

    public void rollover() throws RolloverFailure {
        // Inside this method it is guaranteed that the hereto active log file is
        // closed.
        // If maxIndex <= 0, then there is no file renaming to be done.
//        if (maxIndex >= 0) {
//            // Delete the oldest file, to keep Windows happy.
//            File file = new File(fileNamePattern.convertInt(maxIndex));
//
//            if (file.exists()) {
//                file.delete();
//            }
//
//            // Map {(maxIndex - 1), ..., minIndex} to {maxIndex, ..., minIndex+1}
//            for (int i = maxIndex - 1; i >= minIndex; i--) {
//                String toRenameStr = fileNamePattern.convertInt(i);
//                File toRename = new File(toRenameStr);
//                // no point in trying to rename an inexistent file
//                if (toRename.exists()) {
//                    util.rename(toRenameStr, fileNamePattern.convertInt(i + 1));
//                } else {
//                    addInfo("Skipping roll-over for inexistent file " + toRenameStr);
//                }
//            }

        // move active file name to min
        String targetFileNamePattern = getFileNamePattern();
        switch (compressionMode) {
            case NONE:
                util.rename(CustomRollingFileAppender.FileName, targetFileNamePattern);
                break;
            case GZ:
//                System.out.println("dddddddddddddd");
//                System.out.println(CustomRollingFileAppender.FileName);
//                System.out.println(getFileNamePattern());
                compressor.compress(CustomRollingFileAppender.FileName, targetFileNamePattern, null);
                break;
//                case ZIP:
//                    compressor.compress(getActiveFileName(), fileNamePattern.convertInt(minIndex), zipEntryFileNamePattern.convert(new Date()));
//                    break;
        }

        System.out.println("sssss");
        File f = new File(targetFileNamePattern);
        System.out.println(f.getParent());

//        File f = new File(this.getClass().getResource(getFileNamePattern()).getPath());
//        System.out.println(f);

        File file = new File(f.getParent());
        String[] fileList = file.list();
        // System.out.println(fileList[0]);
        // System.out.println(new File(f.getParent() + File.separator + fileList[0]).exists());

        // Pattern p = Pattern.compile(fileNamePatternStr.replace("%i",".*"));
//        System.out.println(f.getParent() + File.separator + fileList[0]);
//        System.out.println(fileNamePatternStr.replace("%i", "(.*)"));
        ArrayList<String> logName = new ArrayList<String>();
        for (String s : fileList) {
            String a = f.getParent() + File.separator + fileList[0];
            String b = fileNamePatternStr.replace("%i", "(.*)");
            a = a.replace("\\", "/");
            b = b.replace("\\", "/");
            if (a.matches(b)) {
                logName.add(s);
            }
        }
        System.out.println(logName);
        ArrayList<Long> logTime = new ArrayList<Long>();
        String pattern = "[0-9]{15}";
        Pattern p = Pattern.compile(pattern);

        for (String s : logName) {
            Matcher m = p.matcher(s);
            if (m.find()) {
                //System.out.println(m.group());
                logTime.add(Long.parseLong(m.group()));
            }
        }
        //logTime.add(new Long("200002010025001"));
        System.out.println(logTime);
        if (logName.size() > getMaxIndex()) {
            Collections.sort(logTime);
            for (int i = 0; i < logTime.size() - getMaxIndex(); i++) {
                for (String s : logName) {
                    if (s.contains(Long.toString(logTime.get(i)))) {
                        File logFile = new File(f.getParent() + File.separator + s);
                        System.out.println(logFile.getPath());
                        if (logFile.exists()) {
                            logFile.delete();
                        }
                    }
                }
            }
        }
        //System.out.println(logTime);

    }
//}
}

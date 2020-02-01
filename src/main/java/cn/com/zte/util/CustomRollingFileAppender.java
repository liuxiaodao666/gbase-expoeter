package cn.com.zte.util;


import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.RolloverFailure;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import ch.qos.logback.core.status.ErrorStatus;


import java.io.File;
import java.io.IOException;

public class CustomRollingFileAppender<E> extends RollingFileAppender<E> {

    public static File currentlyActiveFile;
    public static String FileName = null;
//    private TriggeringPolicy<E> triggeringPolicy;
//    private RollingPolicy rollingPolicy;

    @Override
    public String getFile() {
        if (FileName == null) {
            FileName = getRollingPolicy().getActiveFileName();
            currentlyActiveFile = new File(FileName);
        }
        return FileName;
    }

    public void rollover() {
        lock.lock();
        try {
            // Note: This method needs to be synchronized because it needs exclusive
            // access while it closes and then re-opens the target file.
            //
            // make sure to close the hereto active log file! Renaming under windows
            // does not work for open files.
            this.closeOutputStream();
            attemptRollover();
            attemptOpenFile();
        } finally {
            lock.unlock();
        }
    }

    private void attemptOpenFile() {
        try {
            // update the currentlyActiveFile LOGBACK-64
            currentlyActiveFile = new File(getRollingPolicy().getActiveFileName());

            // This will also close the file. This is OK since multiple close operations are safe.
            this.openFile(getRollingPolicy().getActiveFileName());
        } catch (IOException e) {
            addError("setFile(" + fileName + ", false) call failed.", e);
            System.out.println("error1");
        }
    }

    private void attemptRollover() {
        try {
            getRollingPolicy().rollover();
        } catch (RolloverFailure rf) {
            addWarn("RolloverFailure occurred. Deferring roll-over.");
            // we failed to roll-over, let us not truncate and risk data loss
            this.append = true;
        }
    }

    @Override
    protected void subAppend(E event) {

        // The roll-over check must precede actual writing. This is the
        // only correct behavior for time driven triggers.

        // We need to synchronize on triggeringPolicy so that only one rollover
        // occurs at a time
        synchronized (getTriggeringPolicy()) {
            if (getTriggeringPolicy().isTriggeringEvent(currentlyActiveFile,event)){
                rollover();
            }
        }
        if (!isStarted()) {
            return;
        }
        try {
            // this step avoids LBCLASSIC-139
            if (event instanceof DeferredProcessingAware) {
                ((DeferredProcessingAware) event).prepareForDeferredProcessing();
            }
            // the synchronization prevents the OutputStream from being closed while we
            // are writing. It also prevents multiple threads from entering the same
            // converter. Converters assume that they are in a synchronized block.
            // lock.lock();

            writeOut(event);

        } catch (IOException ioe) {
            // as soon as an exception occurs, move to non-started state
            // and add a single ErrorStatus to the SM.
            this.started = false;
            addStatus(new ErrorStatus("IO failure in appender", this, ioe));
        }
    }

}

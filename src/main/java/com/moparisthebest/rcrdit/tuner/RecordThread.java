/*
 * rcrdit records TV programs from TV tuners
 * Copyright (C) 2017  Travis Burtrum
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.moparisthebest.rcrdit.tuner;

import com.moparisthebest.rcrdit.autorec.ProgramAutoRec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.function.Function;

/**
 * Created by mopar on 2/25/17.
 */
public class RecordThread extends Thread {

    private static final Logger log = LoggerFactory.getLogger(RecordThread.class);

    private volatile boolean running = true;
    private final ProgramAutoRec recording;
    private final String ext;
    private final Function<ProgramAutoRec, InputStream> getInputStream;

    private final int bufferSize;

    RecordThread(final ProgramAutoRec recording, final String ext, final int bufferSize, final Function<ProgramAutoRec, InputStream> getInputStream) {
        this.recording = recording;
        this.ext = ext;
        this.getInputStream = getInputStream;
        this.bufferSize = bufferSize;
    }

    @Override
    public void interrupt() {
        log.debug("interrupted");
        running = false;
        super.interrupt();
    }

    @Override
    public void run() {
        while (running) {
            log.debug("recording started {}", recording);
            File outputFile = null;
            try (OutputStream os = new FileOutputStream(outputFile = recording.getNewFileToWrite(ext), true);
                 WritableByteChannel oc = Channels.newChannel(os);
            ) {
                // output file exists now, start onStart events
                new ExecThread(true, outputFile, recording).start();
                /*
                // old io
                final byte[] buffer = new byte[bufferSize];
                int len;
                while ((len = is.read(buffer)) >= 0) {
                    os.write(buffer, 0, len);
                }
                */
                // new io from https://thomaswabner.wordpress.com/2007/10/09/fast-stream-copy-using-javanio-channels/
                final ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);

                while (running) {
                    try (InputStream is = getInputStream.apply(recording);
                         ReadableByteChannel ic = Channels.newChannel(is);
                    ) {
                        while (ic.read(buffer) != -1) {
                            // prepare the buffer to be drained
                            buffer.flip();
                            // write to the channel, may block
                            oc.write(buffer);
                            // If partial transfer, shift remainder down
                            // If buffer is empty, same as doing clear()
                            buffer.compact();
                        }
                        // EOF will leave buffer in fill state
                        buffer.flip();
                        // make sure the buffer is fully drained.
                        while (buffer.hasRemaining()) {
                            oc.write(buffer);
                        }
                    } catch (InterruptedIOException | java.nio.channels.ClosedByInterruptException e) {
                        // ignore
                    } catch (Throwable e) {
                        log.error("unknown read exception", e);
                    }
                }
            } catch (InterruptedIOException | java.nio.channels.ClosedByInterruptException e) {
                // ignore
            } catch (Throwable e) {
                log.error("unknown exception", e);
            }
            log.debug("recording finished {}", recording);
            if (outputFile != null)
                new ExecThread(false, outputFile, recording).start();
        }
        log.debug("thread finished {}", recording);
    }

}

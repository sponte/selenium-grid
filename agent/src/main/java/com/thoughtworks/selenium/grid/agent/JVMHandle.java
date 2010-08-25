package com.thoughtworks.selenium.grid.agent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

/**
 * Handle to a process started with @{link JVM
 */
public class JVMHandle {
    private final Process process;

    public JVMHandle(Process process) {
        this.process = process;
    }

    public int waitForProg(final PrintStream outputStream) throws IOException {
        final InputStream is = process.getInputStream();
        final InputStreamReader isr = new InputStreamReader(is);

        BufferedReader br = new BufferedReader(isr);

        try {
            String line;
            while ((line = br.readLine()) != null) {
                outputStream.println(line);
            }
        } finally {
            br.close();
        }

        return process.exitValue();
    }

    public void kill() {
        process.destroy();
    }

    public boolean alive() {
        try {
            process.exitValue();
            return false;
        } catch (IllegalThreadStateException e) {
            return true;
        }
    }

}

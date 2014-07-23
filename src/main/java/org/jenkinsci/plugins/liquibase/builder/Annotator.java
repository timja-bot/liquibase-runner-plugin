package org.jenkinsci.plugins.liquibase.builder;

import hudson.console.LineTransformationOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.jenkinsci.plugins.liquibase.external.ChangeSetNote;

public class Annotator extends LineTransformationOutputStream {
    private final OutputStream out;
    private final Charset charset;

    public Annotator(OutputStream out, Charset charset) {
        this.out = out;
        this.charset = charset;
    }
    @Override
    protected void eol(byte[] b, int len) throws IOException {
        String line = trimEOL(charset.decode(ByteBuffer.wrap(b, 0, len)).toString());

        if (ChangeSetNote.doesLineHaveChangeset(line)) {
            new ChangeSetNote().encodeTo(out);

        }
        out.write(b, 0, len);
    }}

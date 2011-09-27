package org.geogit.repository.remote;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.http.entity.AbstractHttpEntity;
import org.geogit.repository.remote.payload.IPayload;

public class PayloadEntity extends AbstractHttpEntity {

    protected final IPayload payload;

    public PayloadEntity(final IPayload payload) {
        super();
        if (payload == null) {
            throw new IllegalArgumentException("Payload may not be null");
        }
        this.payload = payload;
        setContentType("binary/octet-stream");
    }

    public boolean isRepeatable() {
        return false;
    }

    public long getContentLength() {
        return -1;
    }

    public InputStream getContent() throws IOException {
        return null;
    }

    public void writeTo(final OutputStream outstream) throws IOException {
        try {
            NetworkIO.sendPayload(payload, outstream);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public boolean isStreaming() {
        return true;
    }
}

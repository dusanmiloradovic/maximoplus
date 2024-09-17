package maximoplus;

import java.io.InputStream;

public class BinaryOutputT {
    InputStream inputStream;
    String fileName;
    String contentType;
    String additionalHttpHeader;
    private boolean download = false;

    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getAdditionalHttpHeader() {
        return additionalHttpHeader;
    }

    public void setAdditionalHttpHeader(String additionalHttpHeader) {
        this.additionalHttpHeader = additionalHttpHeader;
    }

    public boolean isDownload() {
        return download;
    }

    public void setDownload(boolean download) {
        this.download = download;
    }
}

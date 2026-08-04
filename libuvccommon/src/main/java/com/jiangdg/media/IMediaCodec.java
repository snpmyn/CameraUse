package com.jiangdg.media;

import java.io.IOException;

public interface IMediaCodec {
    // 10[msec]
    static final int TIMEOUT_USEC = 10000;

    public void prepare() throws IOException;

    public void start();

    public void stop();

    public void release() throws IOException;

    public boolean isPrepared();

    public boolean isRunning();
}
package org.pygisserver.render.util;

import java.awt.Graphics;
import java.util.Timer;
import java.util.TimerTask;

import org.geotools.renderer.GTRenderer;

/**
 * @Description: 渲染超时处理 
 * @author:  
 * @date: 2021-03-18
 */
public class PyRenderingTimeout {

	long timeout;
    GTRenderer renderer;
    Graphics graphics;
    Timer timer;
    boolean timedOut = false;
    boolean saveMap;

    public PyRenderingTimeout(long timeout, GTRenderer renderer, Graphics graphics) {
        this.timeout = timeout;
        this.renderer = renderer;
        this.graphics = graphics;
    }

    /**
     * 开始检查渲染超时
     */
    public void start() {
        if (timer != null)
            throw new IllegalStateException("The timeout enforcer has already been started");

        if (timeout > 0) {
            timedOut = false;
            timer = new Timer();
            timer.schedule(new StopRenderingTask(), timeout);
        }
    }

    public void stop() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }

    public boolean isTimedOut() {
        return timedOut;
    }

    class StopRenderingTask extends TimerTask {

        @Override
        public void run() {
            timedOut = true;
            renderer.stopRendering();
            graphics.dispose();
        }
    }
	
}

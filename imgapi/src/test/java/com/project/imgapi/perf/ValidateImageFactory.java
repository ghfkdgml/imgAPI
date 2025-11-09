package com.project.imgapi.perf;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

public class ValidateImageFactory {
    public static byte[] jpeg(int w, int h) throws Exception {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0,0,w,h);
            g.setColor(Color.BLUE);
            g.fillOval(w/4, h/4, w/2, h/2);
            g.setColor(Color.RED);
            g.drawString("perf", 10, 20);
        } finally { g.dispose(); }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        return baos.toByteArray();
    }
}

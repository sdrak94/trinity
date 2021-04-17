/*
 * Decompiled with CFR 0.139.
 * 
 * Could not load the following classes:
 *  l2r.Config
 *  l2r.gameserver.model.actor.instance.L2PcInstance
 *  l2r.gameserver.network.serverpackets.L2GameServerPacket
 *  l2r.gameserver.network.serverpackets.PledgeCrest
 */
package luna.custom.antibot;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;

import javax.imageio.ImageIO;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.L2GameServerPacket;
import net.sf.l2j.gameserver.network.serverpackets.PledgeCrest;

public class CaptchaImageGenerator {
    public StringBuilder finalString = new StringBuilder();

    public StringBuilder getFinalString() {
        return this.finalString;
    }

    public BufferedImage generateCaptcha() {
        Color textColor = new Color(98, 213, 43);
        Color circleColor = new Color(98, 213, 43);
        Font textFont = new Font("comic sans ms", 1, 24);
        int charsToPrint = 5;
        int width = 256;
        int height = 64;
        int circlesToDraw = 8;
        float horizMargin = 20.0f;
        double rotationRange = 0.7;
        BufferedImage bufferedImage = new BufferedImage(width, height, 1);
        Graphics2D g = (Graphics2D)bufferedImage.getGraphics();
        g.setColor(new Color(30, 31, 31));
        g.fillRect(0, 0, width, height);
        g.setColor(circleColor);
        for (int i = 0; i < circlesToDraw; ++i) {
            int circleRadius = (int)(Math.random() * (double)height / 2.0);
            int circleX = (int)(Math.random() * (double)width - (double)circleRadius);
            int circleY = (int)(Math.random() * (double)height - (double)circleRadius);
            g.drawOval(circleX, circleY, circleRadius * 2, circleRadius * 2);
        }
        g.setColor(textColor);
        g.setFont(textFont);
        FontMetrics fontMetrics = g.getFontMetrics();
        int maxAdvance = fontMetrics.getMaxAdvance();
        int fontHeight = fontMetrics.getHeight();
        String elegibleChars = "0123456789";
        char[] chars = elegibleChars.toCharArray();
        float spaceForLetters = -horizMargin * 2.0f + (float)width;
        float spacePerChar = spaceForLetters / ((float)charsToPrint - 1.0f);
        for (int i = 0; i < charsToPrint; ++i) {
            double randomValue = Math.random();
            int randomIndex = (int)Math.round(randomValue * (double)(chars.length - 1));
            char characterToShow = chars[randomIndex];
            this.finalString.append(characterToShow);
            int charWidth = fontMetrics.charWidth(characterToShow);
            int charDim = Math.max(maxAdvance, fontHeight);
            int halfCharDim = charDim / 2;
            BufferedImage charImage = new BufferedImage(charDim, charDim, 2);
            Graphics2D charGraphics = charImage.createGraphics();
            charGraphics.translate(halfCharDim, halfCharDim);
            double angle = (Math.random() - 0.5) * rotationRange;
            charGraphics.transform(AffineTransform.getRotateInstance(angle));
            charGraphics.translate(-halfCharDim, -halfCharDim);
            charGraphics.setColor(textColor);
            charGraphics.setFont(textFont);
            int charX = (int)(0.5 * (double)charDim - 0.5 * (double)charWidth);
            charGraphics.drawString("" + characterToShow, charX, (charDim - fontMetrics.getAscent()) / 2 + fontMetrics.getAscent());
            float x = horizMargin + spacePerChar * (float)i - (float)charDim / 2.0f;
            int y = (height - charDim) / 2;
            g.drawImage(charImage, (int)x, y, charDim, charDim, null, null);
            charGraphics.dispose();
        }
        g.dispose();
        return bufferedImage;
    }

    public void captchaLogo(L2PcInstance player, int imgId) {
        block2 : {
            try {
                File captcha = new File("data/luna/images/captcha.png");
                ImageIO.write((RenderedImage)CaptchaImageGenerator.getInstance().generateCaptcha(), "png", captcha);
                PledgeCrest packet = new PledgeCrest(imgId, DDSConverter.convertToDDS(captcha).array());
                player.sendPacket((L2GameServerPacket)packet);
            }
            catch (Exception e) {
                if (!Config.DEBUG) break block2;
                e.printStackTrace();
            }
        }
    }

    public static CaptchaImageGenerator getInstance() {
        return SingletonHolder._instance;
    }

    private static class SingletonHolder {
        protected static final CaptchaImageGenerator _instance = new CaptchaImageGenerator();

        private SingletonHolder() {
        }
    }

}


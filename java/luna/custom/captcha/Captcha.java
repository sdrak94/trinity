package luna.custom.captcha;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import luna.custom.captcha.instancemanager.BotsPreventionManager.PlayerData;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.util.Rnd;

/**
 * Class that handles Generating and Sending Captcha Image to the Player
 */
public class Captcha
{
   private static final char[] CAPTCHA_TEXT_POSSIBILITIES =
   {
       'A',
       'B',
       'C',
       'D',
       'E',
       'F',
       'G',
       'H',
       'K',
       'L',
       'M',
       'P',
       'R',
       'S',
       'T',
       'U',
       'W',
       'X',
       'Y',
       'Z'
   };
   private static final int CAPTCHA_WORD_LENGTH = 5;
  
   private static final int CAPTCHA_MIN_ID = 19000;
   private static final int CAPTCHA_MAX_ID = 25000;
  
   Captcha()
   {
   }
  
   public static Captcha getInstance()
   {
       return SingletonHolder._instance;
   }
  
   private static class SingletonHolder
   {
       protected static final Captcha _instance = new Captcha();
   }
  
    public void generateCaptcha(PlayerData container, L2PcInstance target)
    {
    int captchaId = generateRandomCaptchaId();
    char[] captchaText = generateCaptchaText();
  
    container.image = generateCaptcha(captchaText);
    // PledgeCrest packet = new PledgeCrest(captchaId, DDSConverter.convertToDDS(image).array());
    // target.sendPacket(packet);
  
    container.captchaID = captchaId;
    container.captchaText = String.valueOf(captchaText);
    }
   char[] captchaText;
   int captchaId;
  
   public synchronized Map<Integer, ImageData> createImageList()
   {
       Map<Integer, ImageData> _imageMap = new ConcurrentHashMap<>(); 
       for (int i = 0; i < 1000; i++)
       {
           do
           {
               captchaId = generateRandomCaptchaId();
               captchaText = generateCaptchaText();
           }
           while (!_imageMap.isEmpty() && _imageMap.values().stream().anyMatch(txt -> txt.captchaText.equals(String.valueOf(captchaText))) || _imageMap.values().stream().anyMatch(s -> s.captchaID == captchaId));
           ImageData dt = new ImageData();
           dt.captchaID = captchaId;
           dt.captchaText = String.valueOf(captchaText);
           dt.image = generateCaptcha(captchaText);
           _imageMap.put(i, dt);
       }
       return _imageMap;
   }
  
   private static char[] generateCaptchaText()
   {
       char[] text = new char[CAPTCHA_WORD_LENGTH];
       for (int i = 0; i < CAPTCHA_WORD_LENGTH; i++)
           text[i] = CAPTCHA_TEXT_POSSIBILITIES[Rnd.get(CAPTCHA_TEXT_POSSIBILITIES.length)];
       return text;
   }
  
   private static int generateRandomCaptchaId()
   {
       return Rnd.get(CAPTCHA_MIN_ID, CAPTCHA_MAX_ID);
   }
  
   public static BufferedImage generateCaptcha(char[] text)
   {
       Color textColor = new Color(179, 56, 56);
       Color circleColor = new Color(73, 100, 151);
       Font textFont = new Font("Lineage 2 Font Regular", Font.BOLD, 24);
       int charsToPrint = 5;
       int width = 256;
       int height = 64;
       int circlesToDraw = 10;
       float horizMargin = 20.0f;
       double rotationRange = 1; // this is radians
       BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
      
       Graphics2D g = (Graphics2D) bufferedImage.getGraphics();
      
       // Draw an oval
       g.setColor(new Color(47, 47, 47));
       g.fillRect(0, 0, width, height);
      
       g.setColor(circleColor);
       for (int i = 0; i < circlesToDraw; i++)
       {
           int circleRadius = (int) (Math.random() * height / 2.0);
           int circleX = (int) (Math.random() * width - circleRadius);
           int circleY = (int) (Math.random() * height - circleRadius);
           g.drawOval(circleX, circleY, circleRadius * 2, circleRadius * 2);
       }
      
       g.setColor(textColor);
       g.setFont(textFont);
      
       FontMetrics fontMetrics = g.getFontMetrics();
      int maxAdvance = fontMetrics.getMaxAdvance();
       int fontHeight = fontMetrics.getHeight();
      
       float spaceForLetters = -horizMargin * 2.0F + width;
       float spacePerChar = spaceForLetters / (charsToPrint - 1.0f);
      
      for (int i = 0; i < charsToPrint; i++)
       {
           char characterToShow = text[i];
          
          // this is a separate canvas used for the character so that
           // we can rotate it independently
           int charWidth = fontMetrics.charWidth(characterToShow);
           int charDim = Math.max(maxAdvance, fontHeight);
           int halfCharDim = charDim / 2;
          
           BufferedImage charImage = new BufferedImage(charDim, charDim, BufferedImage.TYPE_INT_ARGB);
           Graphics2D charGraphics = charImage.createGraphics();
           charGraphics.translate(halfCharDim, halfCharDim);
           double angle = (Math.random() - 0.5) * rotationRange;
           charGraphics.transform(AffineTransform.getRotateInstance(angle));
           charGraphics.translate(-halfCharDim, -halfCharDim);
           charGraphics.setColor(textColor);
           charGraphics.setFont(textFont);
          
           int charX = (int) (0.5 * charDim - 0.5 * charWidth);
           charGraphics.drawString(String.valueOf(characterToShow), charX, (charDim - fontMetrics.getAscent()) / 2 + fontMetrics.getAscent());
          
           float x = horizMargin + spacePerChar * i - charDim / 2.0f;
           int y = (height - charDim) / 2;
           g.drawImage(charImage, (int) x, y, charDim, charDim, null, null);
          
           charGraphics.dispose();
       }
      
       g.dispose();
      
       return bufferedImage;
   }
}

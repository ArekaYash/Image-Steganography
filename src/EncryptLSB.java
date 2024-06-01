import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class EncryptLSB {
    public static void Encrypt(File imageFile, String message) {
        String directory = new JFileChooser().getFileSystemView().getDefaultDirectory().toString();
        String newImageFileString = directory + "\\export.png";
        File newImageFile = new File(newImageFileString);

        BufferedImage image;
        try {
            image = ImageIO.read(imageFile);
            BufferedImage imageToEncrypt = GetImageToEncrypt(image);
            Pixel[] pixels = GetPixelArray(imageToEncrypt);
            String[] messageBinary = ConvertMessageToBinary(message);
            EncodeMessageBinaryInPixels(pixels, messageBinary);
            ReplacePixelsInNewBufferedImage(pixels, imageToEncrypt);
            SaveNewFile(imageToEncrypt, newImageFile);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private static BufferedImage GetImageToEncrypt(BufferedImage image) {
        ColorModel colorModel = image.getColorModel();
        boolean isAlphaPremultiplied = colorModel.isAlphaPremultiplied();
        WritableRaster raster = image.copyData(null);
        return new BufferedImage(colorModel, raster, isAlphaPremultiplied, null);
    }

    private static Pixel[] GetPixelArray(BufferedImage imageToEncrypt) {
        int height = imageToEncrypt.getHeight();
        int width = imageToEncrypt.getWidth();
        Pixel[] pixels = new Pixel[width * height];

        int count = 0;
        for(int x = 0; x < width; x++) {
            for(int y = 0; y < height; y++) {
                Color colorToAdd = new Color(imageToEncrypt.getRGB(x, y));
                pixels[count] = new Pixel(x, y, colorToAdd);
                count++;
            }
        }
        return pixels;
    }

    private static String[] ConvertMessageToBinary(String message) {
        int[] messageAscii = convertMessageToAscii(message);
        return ConvertAsciiToBinary(messageAscii);
    }

    private static int[] convertMessageToAscii(String message) {
        int[] messageAscii = new int[message.length()];
        for(int i = 0; i < message.length(); i++) {
            messageAscii[i] = (int)message.charAt(i);
        }
        return messageAscii;
    }

    private static String[] ConvertAsciiToBinary(int[] asciiValues) {
        String[] messageBinary = new String[asciiValues.length];
        for(int i = 0; i < asciiValues.length; i++) {
            String binary = LeftPadZeroes(Integer.toBinaryString(asciiValues[i]));
            messageBinary[i] = binary;
        }
        return messageBinary;
    }

    private static String LeftPadZeroes(String binary) {
        StringBuilder sb = new StringBuilder("00000000");
        int offset = 8 - binary.length();
        for(int i = 0; i < binary.length(); i++) {
            sb.setCharAt(i + offset, binary.charAt(i));
        }
        return sb.toString();
    }

    private static void EncodeMessageBinaryInPixels(Pixel[] pixels, String[] messageBinary) {
        int pixelIndex = 0;
        boolean isLastCharacter = false;

        for(int i = 0; i < messageBinary.length; i++) {
            Pixel[] currentPixels = new Pixel[] {pixels[pixelIndex], pixels[pixelIndex + 1], pixels[pixelIndex + 2]};
            if(i + 1 == messageBinary.length) {
                isLastCharacter = true;
            }
            ChangePixelsColor(messageBinary[i], currentPixels, isLastCharacter);
            pixelIndex += 3;
        }
    }

    private static void ChangePixelsColor(String messageBinary, Pixel[] pixels, boolean isLastCharacter) {
        int messageIndex = 0;
        for(int i = 0; i < pixels.length - 1; i++) {
            char[] messageBinaryCharacter = new char[] {messageBinary.charAt(messageIndex), messageBinary.charAt(messageIndex + 1), messageBinary.charAt(messageIndex + 2)};
            String[] pixelRGBBinary = GetPixelsRGBBinary(pixels[i], messageBinaryCharacter);
            pixels[i].setColor(GetNewPixelColor(pixelRGBBinary));
            messageIndex += 3;
        }
        if(!isLastCharacter) {
            char[] messageBinaryChars = new char[] {messageBinary.charAt(messageIndex), messageBinary.charAt(messageIndex + 1), '1'};
            String[] pixelRGBBinary = GetPixelsRGBBinary(pixels[pixels.length - 1], messageBinaryChars);
            pixels[pixels.length - 1].setColor(GetNewPixelColor(pixelRGBBinary));
        } else {
            char[] messageBinaryChars = new char[] {messageBinary.charAt(messageIndex), messageBinary.charAt(messageIndex + 1), '0'};
            String[] pixelRGBBinary = GetPixelsRGBBinary(pixels[pixels.length - 1], messageBinaryChars);
            pixels[pixels.length - 1].setColor(GetNewPixelColor(pixelRGBBinary));
        }
    }

    private static String[] GetPixelsRGBBinary(Pixel pixel, char[] messageBinaryChars) {
        String[] pixelRGBBinary = new String[3];
        pixelRGBBinary[0] = ChangePixelBinary(Integer.toBinaryString(pixel.getColor().getRed()), messageBinaryChars[0]);
        pixelRGBBinary[1] = ChangePixelBinary(Integer.toBinaryString(pixel.getColor().getGreen()), messageBinaryChars[1]);
        pixelRGBBinary[2] = ChangePixelBinary(Integer.toBinaryString(pixel.getColor().getBlue()), messageBinaryChars[2]);
        return pixelRGBBinary;
    }

    private static String ChangePixelBinary(String pixelBinary, char messageBinaryChar) {
        StringBuilder sb = new StringBuilder(pixelBinary);
        sb.setCharAt(sb.length() - 1, messageBinaryChar);
        return sb.toString();
    }

    private static Color GetNewPixelColor(String[] colorBinary) {
        return new Color(Integer.parseInt(colorBinary[0], 2), Integer.parseInt(colorBinary[1], 2), Integer.parseInt(colorBinary[2], 2));
    }

    private static void ReplacePixelsInNewBufferedImage(Pixel[] pixels, BufferedImage imageToEncrypt) {
        for (Pixel pixel : pixels) {
            imageToEncrypt.setRGB(pixel.getX(), pixel.getY(), pixel.getColor().getRGB());
        }
    }

    private static void SaveNewFile(BufferedImage newImage, File newImageFile) {
        try{
            ImageIO.write(newImage, "png", newImageFile);
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}

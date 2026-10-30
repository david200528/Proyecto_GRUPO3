package sigelab.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ReaderException;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.qrcode.QRCodeWriter;
import sigelab.util.BusinessException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Genera y lee códigos QR. El QR solo contiene el código de inventario y solo sirve para identificar
 * el equipo; leerlo no cambia ningún estado (RN-19).
 */
public class QrService {

    public BufferedImage generar(String codigoInventario, int tamano) {
        if (codigoInventario == null || codigoInventario.isBlank()) {
            throw new BusinessException("No hay código de inventario para generar el QR.");
        }
        try {
            BitMatrix matriz = new QRCodeWriter().encode(codigoInventario, BarcodeFormat.QR_CODE, tamano, tamano);
            return MatrixToImageWriter.toBufferedImage(matriz);
        } catch (WriterException e) {
            throw new BusinessException("No se pudo generar el código QR.", e);
        }
    }

    public void guardarPng(String codigoInventario, File destino) {
        try {
            ImageIO.write(generar(codigoInventario, 400), "png", destino);
        } catch (IOException e) {
            throw new BusinessException("No se pudo guardar la imagen del QR.", e);
        }
    }

    /** Lee el texto de un QR contenido en una imagen. */
    public String leer(File imagen) {
        try {
            BufferedImage buffer = ImageIO.read(imagen);
            if (buffer == null) throw new BusinessException("El archivo seleccionado no es una imagen válida.");
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(buffer)));
            return new QRCodeReader().decode(bitmap).getText().trim();
        } catch (ReaderException e) {
            throw new BusinessException("No se encontró un código QR legible en la imagen.", e);
        } catch (IOException e) {
            throw new BusinessException("No se pudo abrir la imagen seleccionada.", e);
        }
    }
}
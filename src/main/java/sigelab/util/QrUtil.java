package sigelab.util;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class QrUtil {

    /**
     * 1. Genera un código QR a partir de un texto (ej: "CAM-001") y lo guarda como imagen PNG.
     */
    public static void generarQr(String texto, int ancho, int alto, String rutaSalida) {
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            BitMatrix bitMatrix = new MultiFormatWriter().encode(
                    texto, BarcodeFormat.QR_CODE, ancho, alto, hints
            );

            Path path = FileSystems.getDefault().getPath(rutaSalida);
            MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);
            System.out.println("QR generado exitosamente en: " + rutaSalida);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 2. Lee un código QR desde un archivo de imagen y retorna el texto decodificado (ej: "CAM-001").
     */
    public static String leerQr(String rutaImagen) {
        try {
            File archivo = new File(rutaImagen);
            BufferedImage imagen = ImageIO.read(archivo);
            if (imagen == null) {
                return null;
            }

            LuminanceSource source = new BufferedImageLuminanceSource(imagen);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

            Map<DecodeHintType, Object> hints = new HashMap<>();
            hints.put(DecodeHintType.CHARACTER_SET, "UTF-8");

            Result resultado = new MultiFormatReader().decode(bitmap, hints);
            return resultado.getText(); // Retorna el código de inventario limpio
        } catch (NotFoundException e) {
            // Significa que la imagen no contiene un QR válido o legible
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
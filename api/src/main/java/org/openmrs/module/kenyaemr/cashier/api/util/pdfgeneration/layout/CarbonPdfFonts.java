package org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.layout;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class CarbonPdfFonts {

    private static final String REGULAR_FONT = "/fonts/ibm-plex-sans/IBMPlexSans-Regular.woff";
    private static final String SEMIBOLD_FONT = "/fonts/ibm-plex-sans/IBMPlexSans-SemiBold.woff";
    private static final byte[] REGULAR_FONT_BYTES = readFontResource(REGULAR_FONT);
    private static final byte[] SEMIBOLD_FONT_BYTES = readFontResource(SEMIBOLD_FONT);
    private static final ThreadLocal<FontPair> DOCUMENT_FONTS = new ThreadLocal<FontPair>();

    private CarbonPdfFonts() {
    }

    public static FontScope openDocumentScope() {
        FontPair previous = DOCUMENT_FONTS.get();
        DOCUMENT_FONTS.set(new FontPair(
                createFont(REGULAR_FONT_BYTES, StandardFonts.HELVETICA),
                createFont(SEMIBOLD_FONT_BYTES, StandardFonts.HELVETICA_BOLD)));
        return new FontScope(previous);
    }

    public static PdfFont regular() {
        FontPair fonts = DOCUMENT_FONTS.get();
        return fonts != null ? fonts.regular : createFont(REGULAR_FONT_BYTES, StandardFonts.HELVETICA);
    }

    public static PdfFont semibold() {
        FontPair fonts = DOCUMENT_FONTS.get();
        return fonts != null ? fonts.semibold : createFont(SEMIBOLD_FONT_BYTES, StandardFonts.HELVETICA_BOLD);
    }

    private static PdfFont createFont(byte[] fontBytes, String fallbackFont) {
        try {
            if (fontBytes != null) {
                return PdfFontFactory.createFont(fontBytes, PdfEncodings.IDENTITY_H,
                        PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED, true);
            }
            return PdfFontFactory.createFont(fallbackFont);
        } catch (Exception e) {
            try {
                return PdfFontFactory.createFont(fallbackFont);
            } catch (IOException fallbackException) {
                throw new IllegalStateException("Failed to load fallback PDF font " + fallbackFont, fallbackException);
            }
        }
    }

    private static byte[] readFontResource(String resourcePath) {
        try (InputStream inputStream = CarbonPdfFonts.class.getResourceAsStream(resourcePath)) {
            return inputStream != null ? readAllBytes(inputStream) : null;
        } catch (IOException e) {
            return null;
        }
    }

    private static byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, read);
        }
        return outputStream.toByteArray();
    }

    private static final class FontPair {

        private final PdfFont regular;
        private final PdfFont semibold;

        private FontPair(PdfFont regular, PdfFont semibold) {
            this.regular = regular;
            this.semibold = semibold;
        }
    }

    public static final class FontScope implements AutoCloseable {

        private final FontPair previous;
        private boolean closed;

        private FontScope(FontPair previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            if (previous == null) {
                DOCUMENT_FONTS.remove();
            } else {
                DOCUMENT_FONTS.set(previous);
            }
            closed = true;
        }
    }
}

/*
 * Firma Digital: Servicio
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ec.gob.firmadigital.servicio;

import static ec.gob.firmadigital.libreria.utils.CheckPDF.checkPDF;
import static ec.gob.firmadigital.libreria.utils.Utils.pdfToDocumento;
import ec.gob.firmadigital.libreria.exceptions.XploitException;
import ec.gob.firmadigital.libreria.exceptions.SignatureVerificationException;
import ec.gob.firmadigital.servicio.exception.ServicioVersionException;
import ec.gob.firmadigital.servicio.exception.TokenExpiradoException;
import ec.gob.firmadigital.servicio.exception.TokenInvalidoException;
import ec.gob.firmadigital.servicio.token.ServicioToken;
import ec.gob.firmadigital.libreria.certificate.to.Documento;
import ec.gob.firmadigital.libreria.sign.SignInfo;
import ec.gob.firmadigital.libreria.sign.Signer;
import ec.gob.firmadigital.libreria.sign.pdf.BasePdfSigner;
import ec.gob.firmadigital.libreria.utils.Json;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import jakarta.ejb.Stateless;
import jakarta.ejb.EJB;
import jakarta.json.JsonReader;
import jakarta.json.stream.JsonParsingException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import jakarta.validation.constraints.NotNull;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Christian Espinosa, Misael Fernández
 */
@Stateless
public class ServicioAppVerificarDocumento {

    @EJB
    private ServicioToken servicioToken;

    @EJB
    private ServicioVersion servicioVersion;

    private static final Logger LOGGER = Logger.getLogger(ec.gob.firmadigital.servicio.ServicioAppVerificarDocumento.class.getName());

    /**
     * appVerificarDocumento
     *
     * @param jwt
     * @param base64Documento
     * @return json
     */
    public String appVerificarDocumento(@NotNull String jwt, @NotNull String base64Documento, @NotNull String base64) {
        String retorno = null;
        Documento documento = null;
        byte[] byteDocumento = java.util.Base64.getDecoder().decode(base64Documento);
        try {
            // Validar JWT
            servicioToken.parseToken(jwt);
            // Validar Version
            String version = buscarVersion(base64);
            if (version.contains("Version enabled")) {
                String mensajeAnalisisDocumento;
                try (InputStream inputStreamDocumento = new ByteArrayInputStream(byteDocumento); PdfReader pdfReader = new PdfReader(inputStreamDocumento); PdfDocument pdfDocument = new PdfDocument(pdfReader)) {
                    mensajeAnalisisDocumento = checkPDF(pdfDocument);
                }
                if (mensajeAnalisisDocumento != null) {
                    LOGGER.log(Level.WARNING, "XploitException: {0}", mensajeAnalisisDocumento);
                    throw new XploitException(mensajeAnalisisDocumento);
                } else {
                    try (InputStream inputStreamDocumento = new ByteArrayInputStream(byteDocumento); PdfReader pdfReader = new PdfReader(inputStreamDocumento)) {
                        Signer signer = new BasePdfSigner();
                        java.util.List<SignInfo> signInfos;
                        signInfos = signer.getSigners(byteDocumento);
                        documento = pdfToDocumento(pdfReader, signInfos);
                    }
                }
            } else {
                retorno = version;
            }
        } catch (TokenInvalidoException ex) {
            retorno = "JWT Inválido";
            return retorno;
        } catch (TokenExpiradoException ex) {
            retorno = "JWT expirado";
            return retorno;
        } catch (java.lang.UnsupportedOperationException uoe) {
            retorno = "No es posible procesar el documento desde dispositivo móvil\nIntentar en FirmaEC de Escritorio";
        } catch (com.itextpdf.io.IOException ioe) {
            retorno = "El archivo no es PDF";
        } catch (SignatureVerificationException sve) {
            retorno = sve.toString();
        } catch (Exception ex) {
            retorno = ex.toString();
        }
        if (documento == null) {
            documento = new Documento(false, false, new ArrayList<>(), retorno);
        }
        return Json.generarJsonDocumento(documento);
    }

    private String buscarVersion(String base64) {
        if (base64 == null || base64.isEmpty()) {
            return "Se debe generar en Base64";
        }
        String jsonParameter;
        try {
            jsonParameter = new String(Base64.getDecoder().decode(base64));
        } catch (IllegalArgumentException e) {
            return getClass().getSimpleName() + "::Error al decodificar base64: \"" + e.getMessage();
        }
        if (jsonParameter == null || jsonParameter.isEmpty()) {
            return "Se debe incluir JSON con los parámetros: sistemaOperativo, aplicacion y versionApp";
        }
        jakarta.json.JsonObject json;
        try {
            JsonReader jsonReader = jakarta.json.Json.createReader(new StringReader(URLDecoder.decode(jsonParameter, "UTF-8")));
            json = (jakarta.json.JsonObject) jsonReader.read();
        } catch (JsonParsingException | UnsupportedEncodingException e) {
            return getClass().getSimpleName() + "::Error al decodificar JSON: " + e.getMessage();
        }

        String sistemaOperativo;
        String aplicacion;
        String versionApp;
        try {
            sistemaOperativo = json.getString("sistemaOperativo");
        } catch (NullPointerException e) {
            return getClass().getSimpleName() + "::Error al decodificar JSON: Se debe incluir \"sistemaOperativo\"";
        }
        try {
            aplicacion = json.getString("aplicacion");
        } catch (NullPointerException e) {
            return getClass().getSimpleName() + "::Error al decodificar JSON: Se debe incluir \"aplicacion\"";
        }
        try {
            versionApp = json.getString("versionApp");
        } catch (NullPointerException e) {
            return getClass().getSimpleName() + "::Error al decodificar JSON: Se debe incluir \"versionApp\"";
        }
        try {
            return servicioVersion.validarVersion(sistemaOperativo, aplicacion, versionApp);
        } catch (ServicioVersionException e) {
            return "versión no encontrada";
        }
    }
}

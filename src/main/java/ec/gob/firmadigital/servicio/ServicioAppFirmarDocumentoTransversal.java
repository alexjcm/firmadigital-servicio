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
import ec.gob.firmadigital.libreria.exceptions.XploitException;
import ec.gob.firmadigital.servicio.exception.ServicioVersionException;
import ec.gob.firmadigital.libreria.exceptions.CertificadoInvalidoException;
import ec.gob.firmadigital.libreria.exceptions.ConexionException;
import ec.gob.firmadigital.libreria.exceptions.EntidadCertificadoraNoValidaException;
import ec.gob.firmadigital.libreria.exceptions.HoraServidorException;
import ec.gob.firmadigital.libreria.exceptions.RubricaException;
import ec.gob.firmadigital.libreria.utils.X509CertificateUtils;
import ec.gob.firmadigital.servicio.util.FirmaDigital;
import ec.gob.firmadigital.servicio.util.JsonProcessor;
import ec.gob.firmadigital.servicio.util.Pkcs12;
import ec.gob.firmadigital.servicio.util.Propiedades;
import com.itextpdf.kernel.crypto.BadPasswordException;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import ec.gob.firmadigital.libreria.model.Document;
import ec.gob.firmadigital.libreria.model.InMemoryDocument;
import jakarta.ejb.EJB;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.ejb.Stateless;
import jakarta.json.JsonReader;
import jakarta.json.stream.JsonParsingException;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.Response;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Base64;

/**
 * Buscar en una lista de URLs permitidos para utilizar como API. Esto permite
 * federar la utilización de FirmaEC sobre otra infraestructura, consultando en
 * una lista de servidores permitidos.
 *
 * @author Christian Espinosa, Misael Fernández
 */
@Stateless
public class ServicioAppFirmarDocumentoTransversal {

    @EJB
    private ServicioVersion servicioVersion;

    /**
     * Nombre de la propiedad de sistema que contiene el archivo de
     * configuracion del servidor WildFly (standalone.xml)
     */
    private static final String WS_SYSTEM_PROPERTY_PREPRODUCCION = "firmadigital-servicio-mobile-preproduccion";
    private static final String WS_SYSTEM_PROPERTY_DESARROLLO = "firmadigital-servicio-mobile-desarrollo";
    private static final String WS_SYSTEM_PROPERTY_PRODUCCION = "firmadigital-servicio-mobile-produccion";

    private String restServiceUrl;
    private static final Logger LOGGER = Logger.getLogger(ServicioAppFirmarDocumentoTransversal.class.getName());

    private String resultado = null;
    private String sistema = null;
    private String versionFirmaEC = null;
    private String formatoDocumento = null;
    private String llx = null;
    private String lly = null;
    private String tipoEstampado = null;
    private String razon = null;
    private String pagina = null;
    private boolean pre = false;
    private boolean des = false;
    private String base64 = null;
    private String url = null;

    private String cedula;

    public String firmarTransversal(@NotNull String pkcs12, @NotNull String password,
            @NotNull String sistema, @NotNull String operacion, String url,
            @NotNull String versionFirmaEC, String formatoDocumento, @NotNull String tokenJwt,
            String llx, String lly, String pagina, String tipoEstampado, String razon,
            boolean pre, boolean des, @NotNull String base64) throws Exception {
        // Validar Version
        String version = buscarVersion(base64);
        if (version.contains("Version enabled")) {
            // Parametros opcionales
            this.sistema = sistema;
            this.versionFirmaEC = versionFirmaEC;
            this.formatoDocumento = formatoDocumento;
            this.llx = llx;
            this.lly = lly;
            this.tipoEstampado = tipoEstampado;
            this.razon = razon;
            this.pagina = pagina;
            this.url = url;
            this.pre = pre;
            this.des = des;
            this.base64 = base64;
            ambiente();
            //en caso de ser firma descentralizada
            if (url != null) {
                this.restServiceUrl = url;
            }
            Map<Long, byte[]> documentosFirmados;
            try {
                //bajar documentos a firmar
                String json = bajarDocumentos(tokenJwt);
                if (json != null) {
                    //firmando documentos descargados
                    String decodedPassword = new String(Base64.getDecoder().decode(password));
                    documentosFirmados = firmarDocumentos(json, pkcs12, decodedPassword);
                    // Actualizar documentos
                    actualizarDocumentos(tokenJwt, documentosFirmados, cedula);
                }
            } finally {
                return resultado;
            }
        } else {
            return version;
        }
    }

    private void ambiente() {
        // Invocar el servicio de Preproduccio o Produccion?
        if (pre) {
            restServiceUrl = System.getProperty(WS_SYSTEM_PROPERTY_PREPRODUCCION);
        } else if (des) {
            restServiceUrl = System.getProperty(WS_SYSTEM_PROPERTY_DESARROLLO);
        } else {
            restServiceUrl = System.getProperty(WS_SYSTEM_PROPERTY_PRODUCCION);
        }
    }

    private Map<Long, byte[]> firmarDocumentos(String json, String pkcs12, String password)
            throws Exception {
        Map<Long, byte[]> documentos = JsonProcessor.parseJsonDocumentos(json);
        Map<Long, byte[]> documentosFirmados = new HashMap<>();
        String fechaHora = JsonProcessor.parseJsonFechaHora(json);
        // Firmar!
        for (Long id : documentos.keySet()) {
            byte[] documento = documentos.get(id);
            byte[] documentoFirmado = null;
            FirmaDigital firmador = new FirmaDigital();
            try {
                // Obtener keyStore
                KeyStore keyStore = Pkcs12.getKeyStore(pkcs12, password);
                String alias = Pkcs12.getAlias(keyStore);

                // Cedula de identidad contenida en el certificado:
                cedula = X509CertificateUtils.getCedula(keyStore, alias);

                if ("xml".equalsIgnoreCase(formatoDocumento)) {
                    documentoFirmado = firmador.firmarXML(keyStore, alias, documento, password.toCharArray(), null, url, base64);
                }
                if ("pdf".equalsIgnoreCase(formatoDocumento)) {
                    Document document = new InMemoryDocument(documento);
                    try (InputStream is = document.openStream()) {
                        PdfReader pdfReader = new PdfReader(is);
                        PdfDocument pdfDocument = new PdfDocument(pdfReader);
                        String mensajeAnalisisDocumento = checkPDF(pdfDocument);
                        if (mensajeAnalisisDocumento != null) {
                            throw new XploitException(mensajeAnalisisDocumento);
                        } else {
                            Properties properties = Propiedades.propiedades(versionFirmaEC, llx, lly, pagina, tipoEstampado, razon, null, fechaHora, base64);
                            documentoFirmado = firmador.firmarPDF(keyStore, alias, documento, password.toCharArray(), properties, url, base64);
                        }
                    } catch (XploitException xe) {
                        throw new XploitException(xe.getMessage());
                    }
                }
            } catch (ConexionException ce) {
                resultado = "Servidor FirmaEC: " + ce.getMessage();
                throw ce;
            } catch (XploitException xe) {
                resultado = xe.getMessage();
                LOGGER.log(Level.WARNING, "XploitException: {0}", resultado);
                throw xe;
            } catch (BadPasswordException bpe) {
                resultado = "Documento protegido con contraseña";
                throw bpe;
            } catch (InvalidKeyException ie) {
                resultado = "Problemas al abrir el documento";
                throw ie;
            } catch (EntidadCertificadoraNoValidaException | CertificadoInvalidoException ecnve) {
                resultado = "Certificado no válido";
                throw ecnve;
            } catch (HoraServidorException hse) {
                resultado = "Problemas en la red\nIntente nuevamente o verifique su conexión";
                throw hse;
            } catch (UnrecoverableKeyException uke) {
                resultado = "Certificado Corrupto";
                throw uke;
            } catch (KeyStoreException kse) {
                resultado = "La contraseña es inválida";
                throw kse;
            } catch (RubricaException re) {
                resultado = "No es posible procesar el documento";
                throw re;
            } catch (IOException | NoSuchAlgorithmException e) {
                resultado = "Excepción no conocida: " + e.getMessage();
                System.out.println("resultado: " + resultado);
            }
            documentosFirmados.put(id, documentoFirmado);
        }
        return documentosFirmados;
    }

    private String bajarDocumentos(String tokenJwt) throws Exception {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(restServiceUrl + tokenJwt);
        Invocation.Builder builder = target.request();
        Invocation invocation = builder.buildGet();
        Response response = invocation.invoke();
        // Leer la respuesta
        int statusCode = response.getStatus();
        String body = null;
        body = response.readEntity(String.class);
        resultado = leerBodyErrores(statusCode, body);
        return body;
    }

    private void actualizarDocumentos(String tokenJwt, Map<Long, byte[]> documentosFirmados, String cedula)
            throws Exception {
        String json = JsonProcessor.buildJson(documentosFirmados, cedula);

        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(restServiceUrl + tokenJwt);
        Invocation.Builder builder = target.request();
        Form form = new Form();
        form.param("json", json);
        form.param("base64", base64);
        Invocation invocation = builder.buildPut(Entity.form(form));
        String body = null;
        try {
            Response response = invocation.invoke();
            // Leer la respuesta
            int statusCode = response.getStatus();
            body = response.readEntity(String.class);
            resultado = leerBodyErrores(statusCode, body);
            if (resultado.isEmpty()) {
                resultado = JsonProcessor.parseJsonDocumentoFirmado(body);
            }
        } catch (BadRequestException e) {
            LOGGER.log(Level.SEVERE, "BadRequestException: {0}", e.getResponse().readEntity(String.class));
        } catch (WebApplicationException e) {
            LOGGER.log(Level.SEVERE, "WebApplicationException: {0}", e.getResponse().readEntity(String.class));
        }
    }

    private String leerBodyErrores(int statusCode, String body) {
        String error = "";
        if (statusCode != HttpURLConnection.HTTP_OK) {
            if (body.contains("Token expirado")) {
                error = "El tiempo de vida del documento en el servidor, se encuentra expirado";
            }
            if (body.contains("Token gestionado")) {
                error = "El/Los documento(s) fueron gestionados";
            }
            if (body.contains("Token invalido")
                    || body.contains("No se encuentran documentos")
                    || body.contains("Error al invocar servicio de obtencion de documentos")
                    || body.contains("Base 64 inválido")) {
                error = "No se encontraron documentos para firmar.";
            }
            if (body.contains("Cedula invalida")) {
                error = "Certificado no corresponde al usuario.\nVuelva a intentarlo.";
            }
            if (body.contains("Certificado revocado")) {
                error = "Certificado puede estar expirado o revocado.\nVuelva a intentarlo.";
            }
            if (body.contains("Request Entity Too Large")) {
                error = "Problemas con los servicios web.\nComuníquese con el administrador de su sistema.";
            }
        }
        return error;
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

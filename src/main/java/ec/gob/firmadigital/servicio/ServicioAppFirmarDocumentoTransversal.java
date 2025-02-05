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

import com.itextpdf.kernel.crypto.BadPasswordException;
import ec.gob.firmadigital.servicio.util.FirmaDigital;
import ec.gob.firmadigital.servicio.util.JsonProcessor;
import ec.gob.firmadigital.servicio.util.Pkcs12;
import ec.gob.firmadigital.servicio.util.Propiedades;
import ec.gob.firmadigital.libreria.exceptions.CertificadoInvalidoException;
import ec.gob.firmadigital.libreria.exceptions.ConexionException;
import ec.gob.firmadigital.libreria.exceptions.EntidadCertificadoraNoValidaException;
import ec.gob.firmadigital.libreria.exceptions.HoraServidorException;
import ec.gob.firmadigital.libreria.exceptions.RubricaException;
import ec.gob.firmadigital.libreria.utils.X509CertificateUtils;
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

/**
 * Buscar en una lista de URLs permitidos para utilizar como API. Esto permite
 * federar la utilización de FirmaEC sobre otra infraestructura, consultando en
 * una lista de servidores permitidos.
 *
 * @author Christian Espinosa <christian.espinosa@mintel.gob.ec>, Misael
 * Fernández
 */
@Stateless
public class ServicioAppFirmarDocumentoTransversal {

    /**
     * Nombre de la propiedad de sistema que contiene el servicio web
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
            @NotNull String sistema, @NotNull String operacion, @NotNull String url, 
            @NotNull String versionFirmaEC, String formatoDocumento, @NotNull String tokenJwt, 
            String llx, String lly, String pagina, String tipoEstampado, String razon, 
            boolean pre, boolean des, @NotNull String base64) throws Exception {
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
        System.out.println("restServiceUrl:  " + restServiceUrl);
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
                documentosFirmados = firmarDocumentos(json, pkcs12, password);
                // Actualizar documentos
                actualizarDocumentos(tokenJwt, documentosFirmados, cedula);
            }
        } finally {
            return resultado;
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
                    Properties properties = Propiedades.propiedades(versionFirmaEC, llx, lly, pagina, tipoEstampado, razon, null, fechaHora, base64);
                    documentoFirmado = firmador.firmarPDF(keyStore, alias, documento, password.toCharArray(), properties, url, base64);
                }
            } catch (ConexionException ce) {
                resultado = "Servidor FirmaEC: " + ce.getMessage();
                throw ce;
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
}

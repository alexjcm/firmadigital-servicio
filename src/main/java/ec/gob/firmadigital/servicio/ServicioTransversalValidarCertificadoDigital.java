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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ec.gob.firmadigital.libreria.certificate.CertEcUtils;
import ec.gob.firmadigital.libreria.certificate.to.Certificado;
import ec.gob.firmadigital.libreria.certificate.to.DatosUsuario;
import ec.gob.firmadigital.libreria.core.Util;
import ec.gob.firmadigital.libreria.exceptions.CertificadoInvalidoException;
import ec.gob.firmadigital.libreria.utils.TiempoUtils;
import ec.gob.firmadigital.libreria.utils.Utils;
import ec.gob.firmadigital.libreria.utils.UtilsCrlOcsp;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import ec.gob.firmadigital.libreria.utils.Json;
import ec.gob.firmadigital.servicio.token.ServicioToken;
import ec.gob.firmadigital.servicio.token.TokenExpiradoException;
import ec.gob.firmadigital.servicio.token.TokenInvalidoException;
import ec.gob.firmadigital.servicio.util.Pkcs12;
import jakarta.ejb.EJB;

import jakarta.ejb.Stateless;
import jakarta.validation.constraints.NotNull;
import java.util.Base64;

/**
 * REST Web Service
 *
 * @author Misael Fernández
 */
@Stateless
public class ServicioTransversalValidarCertificadoDigital {

    @EJB
    private ServicioToken servicioToken;

    /**
     * Busca un ApiUrl por URL.
     *
     * @param jwt
     * @param pkcs12
     * @param password
     * @param base64
     * @return json
     */
    public String transversalValidarCertificadoDigital(@NotNull String jwt, @NotNull String pkcs12, @NotNull String password, @NotNull String base64) {
        Certificado certificado = null;
        String retorno = null;
        boolean caducado = true, revocado = true;

        try {
            String decodedPassword = new String(Base64.getDecoder().decode(password));

            // Validar JWT y obtener info
            servicioToken.parseToken(jwt);

            // Obtener keyStore
            KeyStore keyStore = Pkcs12.getKeyStore(pkcs12, decodedPassword);
            String alias = Pkcs12.getAlias(keyStore);

            X509Certificate x509Certificate = (X509Certificate) keyStore.getCertificate(alias);
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
            TemporalAccessor accessor = dateTimeFormatter.parse(TiempoUtils.getFechaHoraServidor(null, base64));
            Date fechaHoraISO = Date.from(Instant.from(accessor));
            //Validad certificado revocado
            //Date fechaRevocado = fechaString_Date("2022-06-01 10:00:16");
            Date fechaRevocado = UtilsCrlOcsp.validarFechaRevocado(x509Certificate, null);
            if (fechaRevocado != null && fechaRevocado.compareTo(fechaHoraISO) <= 0) {
                retorno = "Certificado revocado: " + fechaRevocado;
                revocado = true;
            } else {
                revocado = false;
            }
            //if (fechaHoraISO.compareTo(x509Certificate.getNotBefore()) <= 0 || fechaHoraISO.compareTo(fechaString_Date("2022-06-21 10:00:16")) >= 0) {
            if (fechaHoraISO.compareTo(x509Certificate.getNotBefore()) <= 0 || fechaHoraISO.compareTo(x509Certificate.getNotAfter()) >= 0) {
                retorno = "Certificado caducado";
                caducado = true;
            } else {
                caducado = false;
            }
            DatosUsuario datosUsuario = CertEcUtils.getDatosUsuarios(x509Certificate);
            certificado = new Certificado(
                    Util.getCN(x509Certificate),
                    CertEcUtils.getNombreCA(x509Certificate),
                    Utils.dateToCalendar(x509Certificate.getNotBefore()),
                    Utils.dateToCalendar(x509Certificate.getNotAfter()),
                    null,
                    //Utils.dateToCalendar(fechaString_Date("2022-06-01 10:00:16")),
                    Utils.dateToCalendar(UtilsCrlOcsp.validarFechaRevocado(x509Certificate, null)),
                    caducado,
                    datosUsuario);
            certificado.setKeyUsages(Utils.validacionKeyUsages(x509Certificate));
        } catch (TokenInvalidoException ex) {
            retorno = "JWT Inválido";
            return retorno;
        } catch (TokenExpiradoException ex) {
            retorno = "JWT expirado";
            return retorno;
        } catch (KeyStoreException kse) {
            if (kse.getCause().toString().contains("Invalid keystore format")) {
                retorno = "Certificado digital es inválido.";
            }
            if (kse.getCause().toString().contains("keystore password was incorrect")) {
                retorno = "La contraseña es inválida.";
            }
        } catch (CertificadoInvalidoException | IOException ex) {
            retorno = "Excepción no conocida: " + ex;
            ex.printStackTrace();
        } finally {
            JsonObject jsonObject = new JsonObject();
            boolean certificateValidate = true;
            if (certificado != null) {
                //TODO reparar al verificar un certificado no encontrado
                if (revocado || certificado.getValidated() || !certificado.getDatosUsuario().isCertificadoDigitalValido()) {
                    certificateValidate = false;
                } else {
                    certificateValidate = true;
                }
                jsonObject.addProperty("validarCertificado", certificateValidate);
                jsonObject.addProperty("error", retorno);
                String jsonCertificado = Json.generarJsonCertificadoTransversal(certificado);
                JsonParser jsonParser = new JsonParser();
                jsonObject.add("certificado", (JsonArray) jsonParser.parse(jsonCertificado));
            } else {
                jsonObject.addProperty("validarCertificado", false);
                jsonObject.addProperty("error", retorno);
                jsonObject.add("certificado", null);
            }
            JsonArray jsonArray = new JsonArray();
            jsonArray.add(jsonObject);
            return jsonArray.toString();
        }
    }
}

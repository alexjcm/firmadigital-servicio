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
package ec.gob.firmadigital.servicio.rest;

import ec.gob.firmadigital.servicio.ServicioAppFirmarDocumento;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

/**
 * REST Web Service
 *
 * @author Christian Espinosa, Misael Fernández
 */
@Stateless
@Path("/appfirmardocumento")
public class ServicioAppFirmarDocumentoRest extends RequestSizeFilter {

    @EJB
    private ServicioAppFirmarDocumento servicioAppFirmarDocumento;

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public String firmarDocumento(@FormParam("jwt") String jwt, @FormParam("pkcs12") String pkcs12,
            @FormParam("password") String password, @FormParam("documento") String documento,
            @FormParam("json") String json, @FormParam("base64") String base64) throws Exception {
        if (jwt == null || jwt.isEmpty()) {
            return "Se debe incluir el parametro jwt";
        }
        if (pkcs12 == null || pkcs12.isEmpty()) {
            return "Se debe incluir el parametro pkcs12";
        }
        if (password == null || password.isEmpty()) {
            return "Se debe incluir el parametro password";
        }
        if (documento == null || documento.isEmpty()) {
            return "Se debe incluir el parametro documento";
        }
        if (json == null || json.isEmpty()) {
            return "Se debe incluir el parametro json";
        }
        JsonObject jsonObject;
        try {
            jsonObject = new JsonParser().parse(json).getAsJsonObject();
        } catch (JsonSyntaxException e) {
            return getClass().getSimpleName() + "::Error al decodificar JSON: \"" + e.getMessage();
        }
        String versionFirmaEC = null;
        String formatoDocumento = null;
        String llx = null;
        String lly = null;
        String pagina = null;
        String tipoEstampado = null;
        String razon = null;
        try {
            versionFirmaEC = jsonObject.get("versionFirmaEC").getAsString();
        } catch (NullPointerException npe) {
            return "Error al decodificar JSON: Se debe incluir \"versionFirmaEC\"";
        } catch (java.lang.UnsupportedOperationException | ClassCastException e) {
            return "Error al decodificar JSON: No coincide el tipo de dato \"versionFirmaEC\"";
        }
        try {
            formatoDocumento = jsonObject.get("formatoDocumento").getAsString();
        } catch (NullPointerException npe) {
            formatoDocumento = "pdf";
        } catch (java.lang.UnsupportedOperationException | ClassCastException e) {
            return "Error al decodificar JSON: No coincide el tipo de dato \"formatoDocumento\"";
        }
        try {
            if (jsonObject.get("llx") != null) {
                llx = jsonObject.get("llx").getAsString();
            }
        } catch (java.lang.UnsupportedOperationException | ClassCastException e) {
            return "Error al decodificar JSON: No coincide el tipo de dato \"llx\"";
        }
        try {
            if (jsonObject.get("lly") != null) {
                lly = jsonObject.get("lly").getAsString();
            }
        } catch (java.lang.UnsupportedOperationException | ClassCastException e) {
            return "Error al decodificar JSON: No coincide el tipo de dato \"lly\"";
        }
        try {
            if (jsonObject.get("pagina") != null) {
                pagina = jsonObject.get("pagina").getAsString();
            }
        } catch (java.lang.UnsupportedOperationException | ClassCastException e) {
            return "Error al decodificar JSON: No coincide el tipo de dato \"pagina\"";
        }
        try {
            if (jsonObject.get("tipoEstampado") != null) {
                tipoEstampado = jsonObject.get("tipoEstampado").getAsString();
            }
        } catch (java.lang.UnsupportedOperationException | ClassCastException e) {
            return "Error al decodificar JSON: No coincide el tipo de dato \"tipoEstampado\"";
        }
        try {
            if (jsonObject.get("razon") != null) {
                razon = jsonObject.get("razon").getAsString();
            }
        } catch (java.lang.UnsupportedOperationException | ClassCastException e) {
            return "Error al decodificar JSON: No coincide el tipo de dato \"razon\"";
        }

        if (base64 == null || base64.isEmpty()) {
            return "Se debe incluir el parametro base64";
        }
        return servicioAppFirmarDocumento.firmarDocumento(jwt, pkcs12, password, documento, versionFirmaEC, formatoDocumento, llx, lly, pagina, tipoEstampado, razon, base64);
    }
}

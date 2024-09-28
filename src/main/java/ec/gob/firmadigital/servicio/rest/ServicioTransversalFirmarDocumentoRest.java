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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import ec.gob.firmadigital.servicio.ServicioTransversalFirmarDocumento;
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
 * @author Misael Fernández
 */
@Stateless
@Path("/transversalfirmardocumento")
public class ServicioTransversalFirmarDocumentoRest {

    @EJB
    private ServicioTransversalFirmarDocumento servicioTransversalFirmarDocumento;

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public String transversalFirmarDocumento(@FormParam("jwt") String jwt, @FormParam("pkcs12") String pkcs12, @FormParam("password") String password,
            @FormParam("documento") String documento, @FormParam("json") String json, @FormParam("base64") String base64) throws Exception {

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

        String formatoDocumento = null;
        String llx = null;
        String lly = null;
        String pagina = null;
        String tipoEstampado = null;
        String razon = null;

        try {
            formatoDocumento = jsonObject.get("formatoDocumento").getAsString();
        } catch (NullPointerException npe) {
            formatoDocumento = "pdf";
        } catch (ClassCastException cce) {
            return "Error al decodificar JSON: No coincide el tipo de dato \"formatoDocumento\"";
        }
        try {
            if (jsonObject.get("llx") != null) {
                llx = jsonObject.get("llx").getAsString();
            }
        } catch (ClassCastException cce) {
            return "Error al decodificar JSON: No coincide el tipo de dato \"llx\"";
        }
        try {
            if (jsonObject.get("lly") != null) {
                lly = jsonObject.get("lly").getAsString();
            }
        } catch (ClassCastException cce) {
            return "Error al decodificar JSON: No coincide el tipo de dato \"lly\"";
        }
        try {
            if (jsonObject.get("pagina") != null) {
                pagina = jsonObject.get("pagina").getAsString();
            }
        } catch (ClassCastException cce) {
            return "Error al decodificar JSON: No coincide el tipo de dato \"pagina\"";
        }
        try {
            if (jsonObject.get("tipoEstampado") != null) {
                tipoEstampado = jsonObject.get("tipoEstampado").getAsString();
            }
        } catch (ClassCastException cce) {
            return "Error al decodificar JSON: No coincide el tipo de dato \"tipoEstampado\"";
        }
        try {
            if (jsonObject.get("razon") != null) {
                razon = jsonObject.get("razon").getAsString();
            }
        } catch (ClassCastException cce) {
            return "Error al decodificar JSON: No coincide el tipo de dato \"razon\"";
        }
        
        if (base64 == null || base64.isEmpty()) {
            return "Se debe incluir el parametro base64";
        }
        
        return servicioTransversalFirmarDocumento.transversalFirmarDocumento(jwt, pkcs12, password, documento, formatoDocumento, llx, lly, pagina, tipoEstampado, razon, base64);
    }
}

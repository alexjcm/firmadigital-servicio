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

import ec.gob.firmadigital.servicio.ServicioAppVerificarDocumento;
import jakarta.ejb.EJB;
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
@Path("/appverificardocumento")
public class ServicioAppVerificarDocumentoRest extends RequestSizeFilter {

    @EJB
    private ServicioAppVerificarDocumento servicioAppVerificarDocumento;

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public String validarDocumento(@FormParam("jwt") String jwt,
            @FormParam("documento") String documento, @FormParam("base64") String base64) throws Exception {
        if (jwt == null || jwt.isEmpty()) {
            return "Se debe incluir el parametro jwt";
        }
        if (documento == null || documento.isEmpty()) {
            return "Se debe incluir el parametro documento";
        }
        if (base64 == null || base64.isEmpty()) {
            return "Se debe incluir el parametro base64";
        }
        return servicioAppVerificarDocumento.appVerificarDocumento(jwt, documento, base64);
    }
}

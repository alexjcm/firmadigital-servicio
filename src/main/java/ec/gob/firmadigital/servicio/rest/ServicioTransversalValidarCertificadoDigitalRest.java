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

import ec.gob.firmadigital.servicio.ServicioTransversalValidarCertificadoDigital;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

/**
 * REST Web Service
 *
 * @author Misael Fernández
 */
@Stateless
@Path("/transversalvalidarcertificadodigital")
public class ServicioTransversalValidarCertificadoDigitalRest {

    @EJB
    private ServicioTransversalValidarCertificadoDigital servicioTransversalValidarCertificadoDigital;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public String validarCertificadoDigital(@FormParam("jwt") String jwt, @FormParam("pkcs12") String pkcs12, @FormParam("password") String password, @FormParam("base64") String base64) {

        if (jwt == null || jwt.isEmpty()) {
            return "Se debe incluir el parametro jwt";
        }
        
        if (pkcs12 == null || pkcs12.isEmpty()) {
            return "Se debe incluir el parametro pkcs12";
        }

        if (password == null || password.isEmpty()) {
            return "Se debe incluir el parametro password";
        }
        
        if (base64 == null || base64.isEmpty()) {
            return "Se debe incluir el parametro base64";
        }
        return servicioTransversalValidarCertificadoDigital.transversalValidarCertificadoDigital(jwt, pkcs12, password, base64);
    }

}
